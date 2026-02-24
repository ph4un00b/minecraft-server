package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/exec"
	"strconv"
	"strings"

	"deploy/internal/config"
)

type SetupSwapStep struct {
	swapCreated bool
	swapSize    string
}

func NewSetupSwapStep() *SetupSwapStep {
	return &SetupSwapStep{}
}

func (s *SetupSwapStep) Name() string {
	return "Setup Swap"
}

func (s *SetupSwapStep) Execute(ctx context.Context, cfg config.Config) error {
	// Check current swap
	cmd := exec.CommandContext(ctx, "free", "-m")
	output, err := cmd.Output()
	if err != nil {
		return fmt.Errorf("failed to check swap: %w", err)
	}

	lines := strings.Split(string(output), "\n")
	for _, line := range lines {
		if strings.HasPrefix(line, "Swap:") {
			fields := strings.Fields(line)
			if len(fields) >= 2 {
				swapTotal, _ := strconv.Atoi(fields[1])
				if swapTotal > 0 {
					slog.Info("swap already configured", "size_mb", swapTotal)
					return nil
				}
			}
		}
	}

	// No swap, create it
	s.swapSize = cfg.GetSwapSize()
	slog.Warn("no swap configured, creating swap file", "size", s.swapSize)

	// Create swap file
	cmd = exec.CommandContext(ctx, "fallocate", "-l", s.swapSize, "/swapfile")
	if err := cmd.Run(); err != nil {
		// Fallback to dd if fallocate fails
		cmd = exec.CommandContext(ctx, "dd", "if=/dev/zero", "of=/swapfile", "bs=1G", fmt.Sprintf("count=%s", strings.TrimSuffix(s.swapSize, "G")))
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("failed to create swap file: %w", err)
		}
	}

	// Set permissions
	cmd = exec.CommandContext(ctx, "chmod", "600", "/swapfile")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to set swap file permissions: %w", err)
	}

	// Setup swap
	cmd = exec.CommandContext(ctx, "mkswap", "/swapfile")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to setup swap: %w", err)
	}

	// Enable swap
	cmd = exec.CommandContext(ctx, "swapon", "/swapfile")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to enable swap: %w", err)
	}

	// Make permanent in fstab
	fstabContent, _ := os.ReadFile("/etc/fstab")
	if !strings.Contains(string(fstabContent), "/swapfile") {
		f, _ := os.OpenFile("/etc/fstab", os.O_APPEND|os.O_WRONLY, 0644)
		f.WriteString("/swapfile none swap sw 0 0\n")
		f.Close()
	}

	s.swapCreated = true
	slog.Info("swap file created and enabled", "size", s.swapSize)

	return nil
}

func (s *SetupSwapStep) Rollback(ctx context.Context, cfg config.Config) error {
	if !s.swapCreated {
		return nil
	}

	slog.Info("removing swap file")
	exec.CommandContext(ctx, "swapoff", "/swapfile").Run()
	os.Remove("/swapfile")

	// Remove from fstab
	content, _ := os.ReadFile("/etc/fstab")
	lines := strings.Split(string(content), "\n")
	var newLines []string
	for _, line := range lines {
		if !strings.Contains(line, "/swapfile") {
			newLines = append(newLines, line)
		}
	}
	os.WriteFile("/etc/fstab", []byte(strings.Join(newLines, "\n")), 0644)

	return nil
}
