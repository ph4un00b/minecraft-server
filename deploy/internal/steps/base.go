package steps

import (
	"deploy/internal/config"
)

// baseStep provides common functionality for all steps
type baseStep struct {
	name string
}

func (s *baseStep) Name() string {
	return s.name
}

// NoopRollback is a rollback that does nothing (for steps that don't need rollback)
type NoopRollback struct{}

func (n *NoopRollback) Rollback(ctx interface{}, cfg config.Config) error {
	return nil
}
