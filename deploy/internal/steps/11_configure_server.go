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

type ConfigureServerStep struct{}

func NewConfigureServerStep() *ConfigureServerStep {
	return &ConfigureServerStep{}
}

func (s *ConfigureServerStep) Name() string {
	return "Configure Server"
}

func (s *ConfigureServerStep) Execute(ctx context.Context, cfg config.Config) error {
	serverPropsPath := filepath.Join(cfg.ProjectDir, "server", "server.properties")

	data := struct {
		RAM          string
		Port         int
		ViewDistance int
		MaxPlayers   int
	}{
		RAM:          cfg.RAM,
		Port:         cfg.Port,
		ViewDistance: cfg.GetViewDistance(),
		MaxPlayers:   cfg.GetMaxPlayers(),
	}

	content, err := templates.RenderTemplate(templates.ServerProperties, data)
	if err != nil {
		return fmt.Errorf("failed to render server.properties template: %w", err)
	}

	if err := os.WriteFile(serverPropsPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write server.properties: %w", err)
	}

	// Set ownership
	cmd := exec.CommandContext(ctx, "chown", fmt.Sprintf("%s:%s", cfg.User, cfg.User), serverPropsPath)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to set server.properties ownership: %w", err)
	}

	slog.Info("created server.properties", "port", cfg.Port, "view_distance", data.ViewDistance)

	// Copy phau.properties template if not exists
	templatePath := filepath.Join(cfg.ProjectDir, "templates", "phau.properties.defaults")
	phauPath := filepath.Join(cfg.ProjectDir, "server", "phau.properties")

	if _, err := os.Stat(templatePath); err == nil {
		if _, err := os.Stat(phauPath); os.IsNotExist(err) {
			content, _ := os.ReadFile(templatePath)
			os.WriteFile(phauPath, content, 0644)
			exec.CommandContext(ctx, "chown", fmt.Sprintf("%s:%s", cfg.User, cfg.User), phauPath).Run()
			slog.Info("copied phau.properties template")
		}
	}

	return nil
}

func (s *ConfigureServerStep) Rollback(ctx context.Context, cfg config.Config) error {
	serverPropsPath := filepath.Join(cfg.ProjectDir, "server", "server.properties")
	os.Remove(serverPropsPath)
	return nil
}
