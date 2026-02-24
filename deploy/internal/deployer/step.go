package deployer

import (
	"context"

	"deploy/internal/config"
)

// Step represents a single deployment step that can be executed and rolled back
type Step interface {
	// Name returns the human-readable name of this step
	Name() string

	// Execute runs the deployment step
	// Should be idempotent - safe to run multiple times
	Execute(ctx context.Context, cfg config.Config) error

	// Rollback undoes the changes made by Execute
	// Called if a subsequent step fails
	Rollback(ctx context.Context, cfg config.Config) error
}

// stepResult captures the result of executing a step
type stepResult struct {
	step   Step
	failed bool
	err    error
}

// stepRegistry holds all deployment steps in order
type stepRegistry struct {
	steps []Step
}

func newStepRegistry() *stepRegistry {
	return &stepRegistry{
		steps: make([]Step, 0),
	}
}

func (r *stepRegistry) Register(step Step) {
	r.steps = append(r.steps, step)
}

func (r *stepRegistry) GetAll() []Step {
	return r.steps
}

func (r *stepRegistry) Count() int {
	return len(r.steps)
}
