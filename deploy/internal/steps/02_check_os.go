package steps

import (
	"bufio"
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/exec"
	"strings"

	"deploy/internal/config"
)

type CheckOSStep struct{}

func NewCheckOSStep() *CheckOSStep {
	return &CheckOSStep{}
}

func (s *CheckOSStep) Name() string {
	return "Check Operating System"
}

func (s *CheckOSStep) Execute(ctx context.Context, cfg config.Config) error {
	// Check if lsb_release is available
	if _, err := exec.LookPath("lsb_release"); err != nil {
		slog.Info("lsb_release not found, attempting to install")
		cmd := exec.CommandContext(ctx, "apt-get", "install", "-y", "lsb-release")
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("failed to install lsb-release: %w", err)
		}
	}

	// Get OS info
	cmd := exec.CommandContext(ctx, "lsb_release", "-si")
	osOut, err := cmd.Output()
	if err != nil {
		return fmt.Errorf("failed to get OS: %w", err)
	}
	osName := strings.TrimSpace(string(osOut))

	cmd = exec.CommandContext(ctx, "lsb_release", "-sr")
	versionOut, err := cmd.Output()
	if err != nil {
		return fmt.Errorf("failed to get version: %w", err)
	}
	version := strings.TrimSpace(string(versionOut))

	if osName != "Ubuntu" {
		slog.Warn("this script is designed for Ubuntu", "detected_os", osName, "version", version)

		// Ask user to continue
		fmt.Printf("Continue anyway? (y/N): ")
		reader := bufio.NewReader(os.Stdin)
		response, _ := reader.ReadString('\n')
		response = strings.TrimSpace(strings.ToLower(response))

		if response != "y" && response != "yes" {
			return fmt.Errorf("user chose not to continue on non-Ubuntu system")
		}
	} else {
		slog.Info("detected operating system", "os", "Ubuntu", "version", version)
	}

	return nil
}

func (s *CheckOSStep) Rollback(ctx context.Context, cfg config.Config) error {
	return nil
}
