package utils

import (
	"fmt"
	"os"
	"os/exec"
)

// EnsureDir creates a directory if it doesn't exist
func EnsureDir(path string, perm os.FileMode) error {
	if err := os.MkdirAll(path, perm); err != nil {
		return fmt.Errorf("failed to create directory %s: %w", path, err)
	}
	return nil
}

// CopyFile copies a file from src to dst
func CopyFile(src, dst string) error {
	content, err := os.ReadFile(src)
	if err != nil {
		return fmt.Errorf("failed to read source file %s: %w", src, err)
	}

	if err := os.WriteFile(dst, content, 0644); err != nil {
		return fmt.Errorf("failed to write destination file %s: %w", dst, err)
	}

	return nil
}

// SetOwnership sets the ownership of a file/directory
func SetOwnership(path, user, group string) error {
	cmd := exec.Command("chown", fmt.Sprintf("%s:%s", user, group), path)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to set ownership of %s: %w", path, err)
	}
	return nil
}

// SetOwnershipRecursive sets ownership recursively
func SetOwnershipRecursive(path, user, group string) error {
	cmd := exec.Command("chown", "-R", fmt.Sprintf("%s:%s", user, group), path)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to set ownership of %s: %w", path, err)
	}
	return nil
}

// IsDir checks if a path is a directory
func IsDir(path string) bool {
	info, err := os.Stat(path)
	if err != nil {
		return false
	}
	return info.IsDir()
}

// IsFile checks if a path is a file
func IsFile(path string) bool {
	info, err := os.Stat(path)
	if err != nil {
		return false
	}
	return !info.IsDir()
}

// SafeRemove removes a file or directory if it exists
func SafeRemove(path string) error {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return nil
	}
	return os.RemoveAll(path)
}
