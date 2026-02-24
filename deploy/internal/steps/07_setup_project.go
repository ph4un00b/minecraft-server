package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/exec"
	"path/filepath"
	"time"

	"deploy/internal/config"
)

type SetupProjectStep struct {
	backupCreated bool
	backupPath    string
}

func NewSetupProjectStep() *SetupProjectStep {
	return &SetupProjectStep{}
}

func (s *SetupProjectStep) Name() string {
	return "Setup Project Directory"
}

func (s *SetupProjectStep) Execute(ctx context.Context, cfg config.Config) error {
	// Handle redeploy
	if cfg.Redeploy {
		if err := s.handleRedeploy(ctx, cfg); err != nil {
			return err
		}
	}

	// Create project directory
	if err := os.MkdirAll(cfg.ProjectDir, 0755); err != nil {
		return fmt.Errorf("failed to create project directory: %w", err)
	}

	// Copy project files using rsync or fallback to cp
	if err := s.copyProjectFiles(ctx, cfg); err != nil {
		return err
	}

	// Create necessary subdirectories
	dirs := []string{
		filepath.Join(cfg.ProjectDir, "server"),
		filepath.Join(cfg.ProjectDir, "plugins"),
		filepath.Join(cfg.ProjectDir, "external"),
	}

	for _, dir := range dirs {
		if err := os.MkdirAll(dir, 0755); err != nil {
			return fmt.Errorf("failed to create directory %s: %w", dir, err)
		}
	}

	// Set ownership
	cmd := exec.CommandContext(ctx, "chown", "-R", fmt.Sprintf("%s:%s", cfg.User, cfg.User), cfg.ProjectDir)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to set ownership: %w", err)
	}

	// Make scripts executable
	gradlew := filepath.Join(cfg.ProjectDir, "gradlew")
	os.Chmod(gradlew, 0755)

	scripts, _ := filepath.Glob(filepath.Join(cfg.ProjectDir, "scripts", "*.sh"))
	for _, script := range scripts {
		os.Chmod(script, 0755)
	}

	return nil
}

func (s *SetupProjectStep) handleRedeploy(ctx context.Context, cfg config.Config) error {
	if _, err := os.Stat(cfg.ProjectDir); os.IsNotExist(err) {
		return nil // Nothing to redeploy
	}

	slog.Warn("redeploy mode: stopping server and cleaning")

	// Stop server
	exec.CommandContext(ctx, "systemctl", "stop", "minecraft").Run()
	exec.CommandContext(ctx, "pkill", "-9", "-f", "paper-1.21.4.jar").Run()
	time.Sleep(2 * time.Second)

	// Backup world if exists
	worldDir := filepath.Join(cfg.ProjectDir, "server", "world")
	if _, err := os.Stat(worldDir); err == nil {
		backupDir := filepath.Join("/home", cfg.User, "backups")
		os.MkdirAll(backupDir, 0755)

		s.backupPath = filepath.Join(backupDir, fmt.Sprintf("world-redeploy-%s.tar.gz", time.Now().Format("20060102-150405")))

		cmd := exec.CommandContext(ctx, "tar", "-czf", s.backupPath, "-C", filepath.Join(cfg.ProjectDir, "server"), "world/")
		if err := cmd.Run(); err == nil {
			s.backupCreated = true
			slog.Info("world backup created", "path", s.backupPath)
		}
	}

	// Clean build artifacts
	os.RemoveAll(filepath.Join(cfg.ProjectDir, "build"))
	os.RemoveAll(filepath.Join(cfg.ProjectDir, ".gradle"))
	os.RemoveAll(filepath.Join(cfg.ProjectDir, "plugins"))

	return nil
}

func (s *SetupProjectStep) copyProjectFiles(ctx context.Context, cfg config.Config) error {
	// Try rsync first
	if _, err := exec.LookPath("rsync"); err == nil {
		cmd := exec.CommandContext(ctx, "rsync", "-av", "--delete",
			"--exclude=.gradle",
			"--exclude=build",
			"--exclude=.kotlin",
			"--exclude=.idea",
			"--exclude=*.tar.gz",
			".", cfg.ProjectDir+"/")
		return cmd.Run()
	}

	// Fallback to cp
	slog.Warn("rsync not available, using cp")
	os.RemoveAll(cfg.ProjectDir)
	os.MkdirAll(cfg.ProjectDir, 0755)

	cmd := exec.CommandContext(ctx, "cp", "-r", ".", cfg.ProjectDir+"/")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to copy project files: %w", err)
	}

	// Clean up
	os.RemoveAll(filepath.Join(cfg.ProjectDir, ".gradle"))
	os.RemoveAll(filepath.Join(cfg.ProjectDir, "build"))

	return nil
}

func (s *SetupProjectStep) Rollback(ctx context.Context, cfg config.Config) error {
	if s.backupCreated && s.backupPath != "" {
		slog.Info("restoring world backup", "path", s.backupPath)
		worldDir := filepath.Join(cfg.ProjectDir, "server", "world")
		os.RemoveAll(worldDir)

		cmd := exec.CommandContext(ctx, "tar", "-xzf", s.backupPath, "-C", filepath.Join(cfg.ProjectDir, "server"))
		cmd.Run()
	}

	// Remove project directory
	os.RemoveAll(cfg.ProjectDir)
	return nil
}
