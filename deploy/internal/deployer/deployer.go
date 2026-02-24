package deployer

import (
	"context"
	"fmt"
	"log/slog"

	"deploy/internal/config"
	"deploy/internal/steps"
)

// Deployer orchestrates the deployment process
type Deployer struct {
	cfg      config.Config
	registry *stepRegistry
	rollback *RollbackManager
}

// New creates a new Deployer instance
func New(cfg config.Config) (*Deployer, error) {
	d := &Deployer{
		cfg:      cfg,
		registry: newStepRegistry(),
		rollback: NewRollbackManager(),
	}

	if err := d.registerSteps(); err != nil {
		return nil, fmt.Errorf("failed to register steps: %w", err)
	}

	return d, nil
}

// registerSteps registers all deployment steps
func (d *Deployer) registerSteps() error {
	// Register all steps in order
	steps := []Step{
		steps.NewCheckRootStep(),
		steps.NewCheckOSStep(),
		steps.NewUpdateSystemStep(),
		steps.NewInstallJavaStep(),
		steps.NewCreateUserStep(),
		steps.NewCheckProjectStep(),
		steps.NewSetupProjectStep(),
		steps.NewEnsureVersionStep(),
		steps.NewVerifyVersionStep(),
		steps.NewBuildProjectStep(),
		steps.NewConfigureServerStep(),
		steps.NewSetupFirewallStep(),
		steps.NewSetupSwapStep(),
		steps.NewSystemdServiceStep(),
		steps.NewHelperScriptsStep(),
		steps.NewStartServerStep(),
	}

	for _, step := range steps {
		d.registry.Register(step)
	}

	return nil
}

// Run executes all deployment steps
func (d *Deployer) Run(ctx context.Context) error {
	totalSteps := d.registry.Count()
	slog.Info("starting deployment", "total_steps", totalSteps, "ram", d.cfg.RAM, "port", d.cfg.Port, "user", d.cfg.User)

	if d.cfg.Redeploy {
		slog.Warn("redeploy mode enabled - will rebuild and restart server")
	}

	allSteps := d.registry.GetAll()

	for i, step := range allSteps {
		stepNum := i + 1
		slog.Info("executing step", "step", stepNum, "total", totalSteps, "name", step.Name())

		if err := step.Execute(ctx, d.cfg); err != nil {
			slog.Error("step failed", "step", step.Name(), "error", err)

			// Rollback completed steps
			d.rollback.Rollback(ctx, d.cfg)

			return fmt.Errorf("step %d (%s) failed: %w", stepNum, step.Name(), err)
		}

		// Track successful step for potential rollback
		d.rollback.Push(step)
		slog.Info("step completed successfully", "step", step.Name())
	}

	slog.Info("all steps completed", "total_steps", totalSteps)
	return nil
}
