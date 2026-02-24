package main

import (
	"context"
	"flag"
	"fmt"
	"log/slog"
	"os"
	"path/filepath"

	"deploy/internal/config"
	"deploy/internal/deployer"
)

func main() {
	cfg := parseFlags()

	logger := setupLogger()
	slog.SetDefault(logger)

	ctx := context.Background()

	d, err := deployer.New(cfg)
	if err != nil {
		slog.Error("failed to create deployer", "error", err)
		os.Exit(1)
	}

	if err := d.Run(ctx); err != nil {
		slog.Error("deployment failed", "error", err)
		os.Exit(1)
	}

	slog.Info("deployment completed successfully")
}

func parseFlags() config.Config {
	var cfg config.Config

	flag.StringVar(&cfg.RAM, "ram", "2G", "Server RAM (512M, 1G, 2G, 4G)")
	flag.IntVar(&cfg.Port, "port", 25565, "Server port")
	flag.StringVar(&cfg.User, "user", "minecraft", "Service user")
	flag.BoolVar(&cfg.SkipJava, "skip-java", false, "Skip Java installation")
	flag.BoolVar(&cfg.SkipFirewall, "skip-firewall", false, "Skip firewall configuration")
	flag.BoolVar(&cfg.Redeploy, "redeploy", false, "Force redeployment")

	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, "Usage: %s [OPTIONS]\n\n", filepath.Base(os.Args[0]))
		fmt.Fprintf(os.Stderr, "Minecraft Colosseum Arena Server Deployer\n\n")
		fmt.Fprintf(os.Stderr, "Options:\n")
		flag.PrintDefaults()
		fmt.Fprintf(os.Stderr, "\nExamples:\n")
		fmt.Fprintf(os.Stderr, "  sudo ./deploy                    # Fresh install with 2GB RAM\n")
		fmt.Fprintf(os.Stderr, "  sudo ./deploy --ram=4G           # Install with 4GB RAM\n")
		fmt.Fprintf(os.Stderr, "  sudo ./deploy --redeploy         # Redeploy/update existing\n")
	}

	flag.Parse()

	if err := cfg.Validate(); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n\n", err)
		flag.Usage()
		os.Exit(1)
	}

	return cfg
}

func setupLogger() *slog.Logger {
	opts := &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}
	handler := slog.NewTextHandler(os.Stdout, opts)
	return slog.New(handler)
}
