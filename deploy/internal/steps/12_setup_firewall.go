package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os/exec"
	"strings"

	"deploy/internal/config"
)

type SetupFirewallStep struct {
	ufwEnabled bool
}

func NewSetupFirewallStep() *SetupFirewallStep {
	return &SetupFirewallStep{}
}

func (s *SetupFirewallStep) Name() string {
	return "Setup Firewall"
}

func (s *SetupFirewallStep) Execute(ctx context.Context, cfg config.Config) error {
	if cfg.SkipFirewall {
		slog.Info("skipping firewall configuration (--skip-firewall flag set)")
		return nil
	}

	// Check if UFW is installed
	if _, err := exec.LookPath("ufw"); err != nil {
		slog.Info("installing UFW")
		cmd := exec.CommandContext(ctx, "apt-get", "install", "-y", "ufw")
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("failed to install UFW: %w", err)
		}
	}

	// Allow Minecraft port
	cmd := exec.CommandContext(ctx, "ufw", "allow", fmt.Sprintf("%d/tcp", cfg.Port))
	cmd.Args = append(cmd.Args, "comment", "Minecraft Server")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to allow TCP port: %w", err)
	}

	cmd = exec.CommandContext(ctx, "ufw", "allow", fmt.Sprintf("%d/udp", cfg.Port))
	cmd.Args = append(cmd.Args, "comment", "Minecraft Server")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to allow UDP port: %w", err)
	}

	// Allow SSH
	cmd = exec.CommandContext(ctx, "ufw", "allow", "22/tcp")
	cmd.Args = append(cmd.Args, "comment", "SSH Access")
	cmd.Run()

	// Check if already enabled
	statusCmd := exec.CommandContext(ctx, "ufw", "status")
	output, _ := statusCmd.Output()
	if !strings.Contains(string(output), "Status: active") {
		// Enable firewall
		cmd = exec.CommandContext(ctx, "bash", "-c", "echo 'y' | ufw enable")
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("failed to enable firewall: %w", err)
		}
		s.ufwEnabled = true
	}

	slog.Info("firewall configured", "port", cfg.Port)
	return nil
}

func (s *SetupFirewallStep) Rollback(ctx context.Context, cfg config.Config) error {
	if s.ufwEnabled {
		// Don't disable firewall, just remove our rules
		exec.CommandContext(ctx, "ufw", "delete", "allow", fmt.Sprintf("%d/tcp", cfg.Port)).Run()
		exec.CommandContext(ctx, "ufw", "delete", "allow", fmt.Sprintf("%d/udp", cfg.Port)).Run()
	}
	return nil
}
