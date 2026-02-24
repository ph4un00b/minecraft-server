package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/exec"
	"path/filepath"

	"deploy/internal/config"
	"deploy/internal/templates"
)

type SystemdServiceStep struct {
	serviceCreated bool
}

func NewSystemdServiceStep() *SystemdServiceStep {
	return &SystemdServiceStep{}
}

func (s *SystemdServiceStep) Name() string {
	return "Create Systemd Service"
}

func (s *SystemdServiceStep) Execute(ctx context.Context, cfg config.Config) error {
	serviceFile := "/etc/systemd/system/minecraft.service"

	startScript := filepath.Join(cfg.ProjectDir, "scripts", "start-server.sh")

	// Render the service file
	data := struct {
		User        string
		ProjectDir  string
		StartScript string
	}{
		User:        cfg.User,
		ProjectDir:  cfg.ProjectDir,
		StartScript: startScript,
	}

	serviceContent, err := templates.RenderTemplate(templates.MinecraftService, data)
	if err != nil {
		return fmt.Errorf("failed to render service template: %w", err)
	}

	if err := os.WriteFile(serviceFile, []byte(serviceContent), 0644); err != nil {
		return fmt.Errorf("failed to write service file: %w", err)
	}

	s.serviceCreated = true

	// Generate start-server.sh
	if err := s.generateStartScript(cfg); err != nil {
		return err
	}

	// Reload systemd
	cmd := exec.CommandContext(ctx, "systemctl", "daemon-reload")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to reload systemd: %w", err)
	}

	// Enable service
	cmd = exec.CommandContext(ctx, "systemctl", "enable", "minecraft")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to enable service: %w", err)
	}

	slog.Info("systemd service created and enabled")
	return nil
}

func (s *SystemdServiceStep) generateStartScript(cfg config.Config) error {
	scriptPath := filepath.Join(cfg.ProjectDir, "scripts", "start-server.sh")

	data := struct {
		RAM     string
		JVMArgs string
	}{
		RAM:     cfg.RAM,
		JVMArgs: cfg.GetJVMArgs(),
	}

	content, err := templates.RenderTemplate(templates.StartServer, data)
	if err != nil {
		return fmt.Errorf("failed to render start script template: %w", err)
	}

	if err := os.WriteFile(scriptPath, []byte(content), 0755); err != nil {
		return fmt.Errorf("failed to write start script: %w", err)
	}

	// Set ownership
	os.Chown(scriptPath, 0, 0) // Will be owned by root, but that's ok for execution

	return nil
}

func (s *SystemdServiceStep) Rollback(ctx context.Context, cfg config.Config) error {
	if !s.serviceCreated {
		return nil
	}

	// Stop and disable service
	exec.CommandContext(ctx, "systemctl", "stop", "minecraft").Run()
	exec.CommandContext(ctx, "systemctl", "disable", "minecraft").Run()

	// Remove service file
	os.Remove("/etc/systemd/system/minecraft.service")

	// Reload systemd
	exec.CommandContext(ctx, "systemctl", "daemon-reload").Run()

	return nil
}
