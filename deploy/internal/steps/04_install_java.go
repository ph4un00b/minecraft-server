package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os/exec"
	"strings"

	"deploy/internal/config"
)

type InstallJavaStep struct{}

func NewInstallJavaStep() *InstallJavaStep {
	return &InstallJavaStep{}
}

func (s *InstallJavaStep) Name() string {
	return "Install Java 21"
}

func (s *InstallJavaStep) Execute(ctx context.Context, cfg config.Config) error {
	if cfg.SkipJava {
		slog.Info("skipping Java installation (--skip-java flag set)")
		return nil
	}

	// Check if Java 21 is already installed
	if s.hasJava21(ctx) {
		slog.Info("Java 21 is already installed")
		return nil
	}

	// Try to install OpenJDK 21
	if s.isOpenJDK21Available(ctx) {
		slog.Info("installing OpenJDK 21")
		cmd := exec.CommandContext(ctx, "apt-get", "install", "-y", "openjdk-21-jdk")
		if err := cmd.Run(); err != nil {
			slog.Warn("failed to install OpenJDK 21, trying Temurin", "error", err)
		} else {
			if s.hasJava21(ctx) {
				slog.Info("Java 21 installed successfully")
				return nil
			}
		}
	}

	// Install Temurin JDK 21
	slog.Info("installing Eclipse Temurin JDK 21")

	// Install prerequisites
	cmd := exec.CommandContext(ctx, "apt-get", "install", "-y", "wget", "apt-transport-https", "gnupg")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to install prerequisites: %w", err)
	}

	// Add Adoptium GPG key
	cmd = exec.CommandContext(ctx, "bash", "-c",
		`wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add -`)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to add Adoptium GPG key: %w", err)
	}

	// Add Adoptium repository
	cmd = exec.CommandContext(ctx, "bash", "-c",
		`echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list`)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to add Adoptium repository: %w", err)
	}

	// Update and install Temurin
	cmd = exec.CommandContext(ctx, "apt-get", "update")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to update after adding repository: %w", err)
	}

	cmd = exec.CommandContext(ctx, "apt-get", "install", "-y", "temurin-21-jdk")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to install Temurin JDK 21: %w", err)
	}

	if !s.hasJava21(ctx) {
		return fmt.Errorf("Java 21 installation verification failed")
	}

	slog.Info("Java 21 installed successfully")
	return nil
}

func (s *InstallJavaStep) hasJava21(ctx context.Context) bool {
	cmd := exec.CommandContext(ctx, "java", "-version")
	output, err := cmd.CombinedOutput()
	if err != nil {
		return false
	}
	return strings.Contains(string(output), "21")
}

func (s *InstallJavaStep) isOpenJDK21Available(ctx context.Context) bool {
	cmd := exec.CommandContext(ctx, "apt-cache", "show", "openjdk-21-jdk")
	err := cmd.Run()
	return err == nil
}

func (s *InstallJavaStep) Rollback(ctx context.Context, cfg config.Config) error {
	// Java installation is not rolled back
	return nil
}
