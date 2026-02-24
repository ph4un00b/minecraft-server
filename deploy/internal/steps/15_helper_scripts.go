package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os"

	"deploy/internal/config"
	"deploy/internal/templates"
)

type HelperScriptsStep struct {
	scriptsInstalled []string
}

func NewHelperScriptsStep() *HelperScriptsStep {
	return &HelperScriptsStep{
		scriptsInstalled: make([]string, 0),
	}
}

func (s *HelperScriptsStep) Name() string {
	return "Create Helper Scripts"
}

func (s *HelperScriptsStep) Execute(ctx context.Context, cfg config.Config) error {
	// Install mcserver script
	if err := s.installMCServerScript(cfg); err != nil {
		return err
	}

	// Install mc-redeploy script
	if err := s.installMCRedeployScript(cfg); err != nil {
		return err
	}

	slog.Info("management commands installed")
	return nil
}

func (s *HelperScriptsStep) installMCServerScript(cfg config.Config) error {
	scriptPath := "/usr/local/bin/mcserver"

	data := struct {
		User string
	}{
		User: cfg.User,
	}

	content, err := templates.RenderTemplate(templates.MCServer, data)
	if err != nil {
		return fmt.Errorf("failed to render mcserver script: %w", err)
	}

	if err := os.WriteFile(scriptPath, []byte(content), 0755); err != nil {
		return fmt.Errorf("failed to write mcserver script: %w", err)
	}

	s.scriptsInstalled = append(s.scriptsInstalled, scriptPath)
	return nil
}

func (s *HelperScriptsStep) installMCRedeployScript(cfg config.Config) error {
	scriptPath := "/usr/local/bin/mc-redeploy"

	data := struct {
		RAM  string
		Port int
	}{
		RAM:  cfg.RAM,
		Port: cfg.Port,
	}

	content, err := templates.RenderTemplate(templates.MCRedeploy, data)
	if err != nil {
		return fmt.Errorf("failed to render mc-redeploy script: %w", err)
	}

	if err := os.WriteFile(scriptPath, []byte(content), 0755); err != nil {
		return fmt.Errorf("failed to write mc-redeploy script: %w", err)
	}

	s.scriptsInstalled = append(s.scriptsInstalled, scriptPath)
	return nil
}

func (s *HelperScriptsStep) Rollback(ctx context.Context, cfg config.Config) error {
	for _, script := range s.scriptsInstalled {
		os.Remove(script)
	}
	return nil
}
