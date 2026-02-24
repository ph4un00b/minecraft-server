package utils

import (
	"context"
	"fmt"
	"log/slog"
	"os/exec"
	"strings"
)

// RunCommand executes a command with logging and error handling
func RunCommand(ctx context.Context, name string, args ...string) error {
	cmd := exec.CommandContext(ctx, name, args...)

	slog.Debug("executing command", "command", name, "args", args)

	output, err := cmd.CombinedOutput()
	if err != nil {
		slog.Error("command failed",
			"command", name,
			"args", args,
			"error", err,
			"output", string(output))
		return fmt.Errorf("command failed: %s %s: %w (output: %s)",
			name, strings.Join(args, " "), err, string(output))
	}

	return nil
}

// RunCommandWithOutput executes a command and returns output
func RunCommandWithOutput(ctx context.Context, name string, args ...string) (string, error) {
	cmd := exec.CommandContext(ctx, name, args...)

	output, err := cmd.Output()
	if err != nil {
		return "", fmt.Errorf("command failed: %w", err)
	}

	return strings.TrimSpace(string(output)), nil
}

// CommandExists checks if a command exists in PATH
func CommandExists(name string) bool {
	_, err := exec.LookPath(name)
	return err == nil
}

// RunAsUser executes a command as a specific user
func RunAsUser(ctx context.Context, user, dir, command string, args ...string) error {
	fullCommand := fmt.Sprintf("cd %s && %s %s", dir, command, strings.Join(args, " "))
	cmd := exec.CommandContext(ctx, "su", "-", user, "-c", fullCommand)

	output, err := cmd.CombinedOutput()
	if err != nil {
		slog.Error("command as user failed",
			"user", user,
			"dir", dir,
			"command", command,
			"error", err,
			"output", string(output))
		return fmt.Errorf("command as %s failed: %w", user, err)
	}

	return nil
}
