package steps

import (
	"context"
	"fmt"
	"os/exec"

	"deploy/internal/config"
)

type UpdateSystemStep struct{}

func NewUpdateSystemStep() *UpdateSystemStep {
	return &UpdateSystemStep{}
}

func (s *UpdateSystemStep) Name() string {
	return "Update System Packages"
}

func (s *UpdateSystemStep) Execute(ctx context.Context, cfg config.Config) error {
	// Update package list
	cmd := exec.CommandContext(ctx, "apt-get", "update")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to update package list: %w", err)
	}

	// Upgrade packages
	cmd = exec.CommandContext(ctx, "apt-get", "upgrade", "-y")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to upgrade packages: %w", err)
	}

	// Install required packages
	packages := []string{
		"wget", "curl", "git", "screen", "htop",
		"zip", "unzip", "lsb-release", "ufw", "rsync",
	}

	cmd = exec.CommandContext(ctx, "apt-get", "install", "-y")
	cmd.Args = append(cmd.Args, packages...)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to install packages: %w", err)
	}

	return nil
}

func (s *UpdateSystemStep) Rollback(ctx context.Context, cfg config.Config) error {
	// System updates cannot be rolled back
	return nil
}
