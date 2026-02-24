package steps

import (
	"context"
	"fmt"
	"os"

	"deploy/internal/config"
)

type CheckRootStep struct{}

func NewCheckRootStep() *CheckRootStep {
	return &CheckRootStep{}
}

func (s *CheckRootStep) Name() string {
	return "Check Root Privileges"
}

func (s *CheckRootStep) Execute(ctx context.Context, cfg config.Config) error {
	if os.Geteuid() != 0 {
		return fmt.Errorf("this script must be run as root (use sudo)")
	}
	return nil
}

func (s *CheckRootStep) Rollback(ctx context.Context, cfg config.Config) error {
	return nil
}
