package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"deploy/internal/config"
)

type VerifyVersionStep struct{}

func NewVerifyVersionStep() *VerifyVersionStep {
	return &VerifyVersionStep{}
}

func (s *VerifyVersionStep) Name() string {
	return "Verify Plugin Version"
}

func (s *VerifyVersionStep) Execute(ctx context.Context, cfg config.Config) error {
	// Build plugin JAR
	slog.Info("building plugin to verify version")

	cmd := exec.CommandContext(ctx, "su", "-", cfg.User, "-c",
		fmt.Sprintf("cd %s && ./gradlew jar --no-daemon --console=plain", cfg.ProjectDir))

	if err := cmd.Run(); err != nil {
		return fmt.Errorf("build failed: %w", err)
	}

	// Check if JAR was created
	jarPath := filepath.Join(cfg.ProjectDir, "plugins", "colosseum-arena-1.0.jar")
	if _, err := os.Stat(jarPath); os.IsNotExist(err) {
		return fmt.Errorf("plugin JAR not found: %s", jarPath)
	}

	// Verify JAR contains version.properties
	cmd = exec.CommandContext(ctx, "unzip", "-l", jarPath)
	output, err := cmd.Output()
	if err != nil {
		return fmt.Errorf("failed to list JAR contents: %w", err)
	}

	if !strings.Contains(string(output), "version.properties") {
		return fmt.Errorf("JAR missing version.properties")
	}

	slog.Info("JAR contains version.properties")

	// Extract and display version info
	cmd = exec.CommandContext(ctx, "unzip", "-p", jarPath, "version.properties")
	versionContent, err := cmd.Output()
	if err != nil {
		return fmt.Errorf("failed to extract version.properties: %w", err)
	}

	version := s.extractValue(string(versionContent), "version")
	buildTime := s.extractValue(string(versionContent), "build.time")
	gitHash := s.extractValue(string(versionContent), "git.hash")

	fmt.Println("\n========================================")
	fmt.Println("  Plugin Version Info")
	fmt.Println("========================================")
	fmt.Printf("\n  Version: %s\n", version)
	fmt.Printf("  Build Time: %s\n", buildTime)
	fmt.Printf("  Git Hash: %s\n", gitHash)
	fmt.Println("\n========================================")
	fmt.Println()

	slog.Info("plugin version verified", "version", version)

	return nil
}

func (s *VerifyVersionStep) extractValue(content, key string) string {
	lines := strings.Split(content, "\n")
	for _, line := range lines {
		if strings.HasPrefix(line, key+"=") {
			return strings.TrimPrefix(line, key+"=")
		}
	}
	return "unknown"
}

func (s *VerifyVersionStep) Rollback(ctx context.Context, cfg config.Config) error {
	return nil
}
