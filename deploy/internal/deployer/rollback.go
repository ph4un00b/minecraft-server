package deployer

import (
	"context"
	"log/slog"

	"deploy/internal/config"
)

// RollbackManager tracks completed steps for rollback
type RollbackManager struct {
	completed []Step
}

func NewRollbackManager() *RollbackManager {
	return &RollbackManager{
		completed: make([]Step, 0),
	}
}

// Push adds a successfully completed step to the stack
func (rm *RollbackManager) Push(step Step) {
	rm.completed = append(rm.completed, step)
}

// Rollback executes rollback for all completed steps in reverse order
func (rm *RollbackManager) Rollback(ctx context.Context, cfg config.Config) {
	if len(rm.completed) == 0 {
		return
	}

	slog.Warn("initiating rollback", "steps_to_rollback", len(rm.completed))

	// Roll back in reverse order
	for i := len(rm.completed) - 1; i >= 0; i-- {
		step := rm.completed[i]
		slog.Info("rolling back step", "step", step.Name())

		if err := step.Rollback(ctx, cfg); err != nil {
			slog.Error("rollback failed for step", "step", step.Name(), "error", err)
			// Continue rolling back other steps even if one fails
		}
	}

	slog.Info("rollback completed")
}

// Clear removes all tracked steps (call after successful deployment)
func (rm *RollbackManager) Clear() {
	rm.completed = rm.completed[:0]
}
