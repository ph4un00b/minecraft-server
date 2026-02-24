package steps

import (
	"context"
	"fmt"
	"log/slog"
	"net"
	"os/exec"
	"time"

	"deploy/internal/config"
)

type StartServerStep struct{}

func NewStartServerStep() *StartServerStep {
	return &StartServerStep{}
}

func (s *StartServerStep) Name() string {
	return "Start Server"
}

func (s *StartServerStep) Execute(ctx context.Context, cfg config.Config) error {
	if cfg.Redeploy {
		slog.Info("redeploy complete, starting server")
	}

	// Start the service
	cmd := exec.CommandContext(ctx, "systemctl", "start", "minecraft")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to start server: %w", err)
	}

	// Wait a moment and check status
	time.Sleep(3 * time.Second)

	statusCmd := exec.CommandContext(ctx, "systemctl", "is-active", "--quiet", "minecraft")
	if err := statusCmd.Run(); err != nil {
		slog.Warn("server may not have started properly, checking logs")
		logsCmd := exec.CommandContext(ctx, "journalctl", "-u", "minecraft", "-n", "20", "--no-pager")
		logs, _ := logsCmd.Output()
		fmt.Println(string(logs))
	} else {
		slog.Info("server started successfully")
	}

	// Get server IP
	addrs, err := net.InterfaceAddrs()
	var ip string
	if err == nil {
		for _, addr := range addrs {
			if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
				if ipnet.IP.To4() != nil {
					ip = ipnet.IP.String()
					break
				}
			}
		}
	}

	if ip == "" {
		ip = "127.0.0.1"
	}

	// Print completion banner
	fmt.Println("\n========================================")
	fmt.Println("  DEPLOYMENT COMPLETE!")
	fmt.Println("========================================")
	fmt.Println("")
	fmt.Printf("Server IP: %s:%d\n", ip, cfg.Port)
	fmt.Printf("User: %s\n", cfg.User)
	fmt.Printf("Project: %s\n", cfg.ProjectDir)
	fmt.Printf("RAM: %s\n", cfg.RAM)
	fmt.Println("")
	fmt.Println("Management Commands:")
	fmt.Println("  mcserver start       - Start server")
	fmt.Println("  mcserver stop        - Stop server")
	fmt.Println("  mcserver restart     - Restart server")
	fmt.Println("  mcserver logs        - View logs")
	fmt.Println("  mcserver backup      - Backup world")
	fmt.Println("  mcserver fix         - Fix locks/crashes")
	fmt.Println("  mcserver redeploy    - Full redeploy")
	fmt.Println("  mcserver update      - Quick plugin update")
	fmt.Println("")
	fmt.Printf("Connect in Minecraft: %s:%d\n", ip, cfg.Port)
	fmt.Println("")
	fmt.Println("========================================")
	fmt.Println("  Installation Complete!")
	fmt.Println("========================================")
	fmt.Println("")
	fmt.Println("Next steps:")
	fmt.Println("1. Connect to console: mcserver console")
	fmt.Println("2. OP yourself: op <your-minecraft-username>")
	fmt.Println("3. Start playing!")
	fmt.Println("")
	fmt.Println("To redeploy after code changes:")
	fmt.Printf("  cd /home/%s/colosseum-arena\n", cfg.User)
	fmt.Println("  sudo ./deploy --redeploy")
	fmt.Println("")
	fmt.Println("Or use: mcserver redeploy")
	fmt.Println("")

	return nil
}

func (s *StartServerStep) Rollback(ctx context.Context, cfg config.Config) error {
	// Stop the server
	exec.CommandContext(ctx, "systemctl", "stop", "minecraft").Run()
	return nil
}
