package config

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
)

type Config struct {
	RAM          string
	Port         int
	User         string
	SkipJava     bool
	SkipFirewall bool
	Redeploy     bool
	ProjectDir   string
}

var validRAM = map[string]bool{
	"512M": true,
	"1G":   true,
	"2G":   true,
	"4G":   true,
}

var validUserPattern = regexp.MustCompile(`^[a-z_][a-z0-9_-]*$`)

func (c *Config) Validate() error {
	if !validRAM[c.RAM] {
		return fmt.Errorf("invalid RAM size: %s (must be 512M, 1G, 2G, or 4G)", c.RAM)
	}

	if c.Port < 1 || c.Port > 65535 {
		return fmt.Errorf("invalid port: %d (must be 1-65535)", c.Port)
	}

	if !validUserPattern.MatchString(c.User) {
		return fmt.Errorf("invalid username: %s", c.User)
	}

	if c.User == "root" {
		return fmt.Errorf("cannot use root as service user")
	}

	c.ProjectDir = filepath.Join("/home", c.User, "colosseum-arena")

	return nil
}

func (c *Config) GetViewDistance() int {
	switch c.RAM {
	case "512M", "1G":
		return 5
	case "2G":
		return 8
	case "4G":
		return 12
	default:
		return 8
	}
}

func (c *Config) GetMaxPlayers() int {
	switch c.RAM {
	case "512M", "1G":
		return 5
	case "2G":
		return 10
	case "4G":
		return 20
	default:
		return 10
	}
}

func (c *Config) GetJVMArgs() string {
	switch c.RAM {
	case "512M":
		return "-Xms400M -Xmx450M"
	case "1G":
		return "-Xms900M -Xmx1G"
	case "2G":
		return "-Xms2G -Xmx2G"
	case "4G":
		return "-Xms4G -Xmx4G"
	default:
		return "-Xms2G -Xmx2G"
	}
}

func (c *Config) GetSwapSize() string {
	switch c.RAM {
	case "512M":
		return "2G"
	case "1G":
		return "3G"
	case "2G", "4G":
		return "4G"
	default:
		return "3G"
	}
}

func (c *Config) IsRunningAsRoot() bool {
	return os.Geteuid() == 0
}
