package steps

import (
	"context"
	"fmt"
	"os"
	"path/filepath"

	"deploy/internal/config"
)

type CheckProjectStep struct{}

func NewCheckProjectStep() *CheckProjectStep {
	return &CheckProjectStep{}
}

func (s *CheckProjectStep) Name() string {
	return "Check Project Files"
}

func (s *CheckProjectStep) Execute(ctx context.Context, cfg config.Config) error {
	requiredFiles := []string{
		"build.gradle.kts",
		"gradlew",
	}

	for _, file := range requiredFiles {
		if _, err := os.Stat(file); os.IsNotExist(err) {
			return fmt.Errorf("required file not found: %s (run from project root)", file)
		}
	}

	if _, err := os.Stat("scripts"); os.IsNotExist(err) {
		return fmt.Errorf("scripts/ directory not found")
	}

	// Make scripts executable
	scripts, _ := filepath.Glob("scripts/*.sh")
	for _, script := range scripts {
		os.Chmod(script, 0755)
	}
	os.Chmod("gradlew", 0755)

	return nil
}

func (s *CheckProjectStep) Rollback(ctx context.Context, cfg config.Config) error {
	return nil
}
