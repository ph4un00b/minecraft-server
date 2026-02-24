package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/exec"
	"os/user"

	"deploy/internal/config"
)

type CreateUserStep struct {
	userCreated bool
}

func NewCreateUserStep() *CreateUserStep {
	return &CreateUserStep{}
}

func (s *CreateUserStep) Name() string {
	return "Create Service User"
}

func (s *CreateUserStep) Execute(ctx context.Context, cfg config.Config) error {
	// Check if user already exists
	_, err := user.Lookup(cfg.User)
	if err == nil {
		slog.Info("user already exists", "user", cfg.User)
	} else {
		// Create user
		slog.Info("creating service user", "user", cfg.User)
		cmd := exec.CommandContext(ctx, "useradd", "-r", "-m", "-s", "/bin/bash", cfg.User)
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("failed to create user: %w", err)
		}
		s.userCreated = true
		slog.Info("user created", "user", cfg.User)
	}

	// Ensure home directory exists with correct ownership
	homeDir := fmt.Sprintf("/home/%s", cfg.User)
	if err := os.MkdirAll(homeDir, 0755); err != nil {
		return fmt.Errorf("failed to create home directory: %w", err)
	}

	cmd := exec.CommandContext(ctx, "chown", fmt.Sprintf("%s:%s", cfg.User, cfg.User), homeDir)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to set home directory ownership: %w", err)
	}

	return nil
}

func (s *CreateUserStep) Rollback(ctx context.Context, cfg config.Config) error {
	if !s.userCreated {
		return nil
	}

	slog.Info("rolling back user creation", "user", cfg.User)
	cmd := exec.CommandContext(ctx, "userdel", "-r", cfg.User)
	return cmd.Run()
}
