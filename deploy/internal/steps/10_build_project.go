package steps

import (
	"context"
	"fmt"
	"log/slog"
	"os/exec"

	"deploy/internal/config"
)

type BuildProjectStep struct{}

func NewBuildProjectStep() *BuildProjectStep {
	return &BuildProjectStep{}
}

func (s *BuildProjectStep) Name() string {
	return "Build Project"
}

func (s *BuildProjectStep) Execute(ctx context.Context, cfg config.Config) error {
	slog.Info("running Gradle setup")

	cmd := exec.CommandContext(ctx, "su", "-", cfg.User, "-c",
		fmt.Sprintf("cd %s && ./gradlew setup --no-daemon --console=plain", cfg.ProjectDir))

	output, err := cmd.CombinedOutput()
	if err != nil {
		slog.Error("build failed", "output", string(output))
		return fmt.Errorf("build failed: %w\nCommon issues:\n- Network connectivity for downloading Paper\n- Java version mismatch\n- Permission issues", err)
	}

	slog.Info("project built successfully")
	return nil
}

func (s *BuildProjectStep) Rollback(ctx context.Context, cfg config.Config) error {
	return nil
}
