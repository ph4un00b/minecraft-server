package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"

	"deploy/internal/config"
)

type EnsureVersionStep struct {
	versionCreated bool
	versionPath    string
}

func NewEnsureVersionStep() *EnsureVersionStep {
	return &EnsureVersionStep{}
}

func (s *EnsureVersionStep) Name() string {
	return "Ensure Version File"
}

func (s *EnsureVersionStep) Execute(ctx context.Context, cfg config.Config) error {
	s.versionPath = filepath.Join(cfg.ProjectDir, "src", "main", "resources", "version.properties")

	// Check if file exists
	if _, err := os.Stat(s.versionPath); err == nil {
		slog.Info("version.properties already exists")
		return nil
	}

	slog.Warn("version.properties not found, recreating")

	// Ensure directory exists
	dir := filepath.Dir(s.versionPath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return fmt.Errorf("failed to create resources directory: %w", err)
	}

	// Try to get git info
	var gitHash, gitBranch string

	if s.hasGit(ctx, cfg) {
		slog.Info("git available, extracting version info")

		// Get git hash
		cmd := exec.CommandContext(ctx, "git", "-C", cfg.ProjectDir, "rev-parse", "--short", "HEAD")
		if out, err := cmd.Output(); err == nil {
			gitHash = strings.TrimSpace(string(out))
		}

		// Get git branch
		cmd = exec.CommandContext(ctx, "git", "-C", cfg.ProjectDir, "rev-parse", "--abbrev-ref", "HEAD")
		if out, err := cmd.Output(); err == nil {
			gitBranch = strings.TrimSpace(string(out))
		}
	}

	// Build version file content
	buildTime := time.Now().Format("2006-01-02 15:04:05")

	if gitHash == "" {
		gitHash = "unknown"
	}
	if gitBranch == "" {
		gitBranch = "unknown"
	}

	content := fmt.Sprintf(`# Colosseum Arena Version Info
# This file is the SOURCE OF TRUTH for version information

version=1.0+%s
build.time=%s
git.hash=%s
git.branch=%s
`, gitHash, buildTime, gitHash, gitBranch)

	if err := os.WriteFile(s.versionPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write version.properties: %w", err)
	}

	// Set ownership
	cmd := exec.CommandContext(ctx, "chown", fmt.Sprintf("%s:%s", cfg.User, cfg.User), s.versionPath)
	cmd.Run()

	s.versionCreated = true
	slog.Info("version.properties created", "version", "1.0+"+gitHash)

	return nil
}

func (s *EnsureVersionStep) hasGit(ctx context.Context, cfg config.Config) bool {
	if _, err := exec.LookPath("git"); err != nil {
		return false
	}

	gitDir := filepath.Join(cfg.ProjectDir, ".git")
	if _, err := os.Stat(gitDir); os.IsNotExist(err) {
		return false
	}

	return true
}

func (s *EnsureVersionStep) Rollback(ctx context.Context, cfg config.Config) error {
	if s.versionCreated {
		os.Remove(s.versionPath)
	}
	return nil
}
