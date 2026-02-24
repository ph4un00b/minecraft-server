# Minecraft Colosseum Arena Deployer

A Go-based deployment tool for the Minecraft Colosseum Arena server. Replaces the 960-line bash script with a type-safe, testable, and maintainable Go application.

## Features

- **Type-safe configuration** - No more string parsing errors
- **Idempotent operations** - Safe to run multiple times
- **Automatic rollback** - Failed deployments undo changes
- **Structured logging** - Uses Go's slog for consistent output
- **Zero dependencies** - Single binary deployment
- **Testable** - Each step can be unit tested

## Building

```bash
# Build for local machine
cd deploy
go build -o deploy cmd/deploy/main.go

# Cross-compile for Linux (most common)
GOOS=linux GOARCH=amd64 go build -o deploy-linux cmd/deploy/main.go

# Cross-compile for ARM64 (Raspberry Pi, etc.)
GOOS=linux GOARCH=arm64 go build -o deploy-linux-arm64 cmd/deploy/main.go
```

## Usage

```bash
# Fresh install with defaults (2GB RAM, port 25565)
sudo ./deploy

# Install with 4GB RAM
sudo ./deploy --ram=4G

# Use custom port
sudo ./deploy --port=25566

# Redeploy after code changes
sudo ./deploy --redeploy

# Skip Java installation (if already installed)
sudo ./deploy --skip-java

# Skip firewall configuration
sudo ./deploy --skip-firewall
```

## Command Line Options

| Option | Default | Description |
|--------|---------|-------------|
| `--ram` | `2G` | Server RAM (512M, 1G, 2G, 4G) |
| `--port` | `25565` | Server port |
| `--user` | `minecraft` | Service user |
| `--skip-java` | `false` | Skip Java installation |
| `--skip-firewall` | `false` | Skip firewall configuration |
| `--redeploy` | `false` | Force redeployment |

## Architecture

```
deploy/
├── cmd/deploy/        # Entry point
├── internal/
│   ├── config/        # Configuration validation
│   ├── deployer/      # Deployment orchestration
│   ├── steps/         # Individual deployment steps
│   └── utils/         # Helper utilities
└── templates/         # Template files (optional)
```

### Step System

Each deployment step implements the `Step` interface:

```go
type Step interface {
    Name() string
    Execute(ctx context.Context, cfg Config) error
    Rollback(ctx context.Context, cfg Config) error
}
```

Steps are executed in order. If any step fails, all completed steps are rolled back in reverse order.

## Development

```bash
# Run tests
go test ./...

# Run with verbose logging
./deploy 2>&1 | jq -R 'fromjson?'

# Build and run locally
go run cmd/deploy/main.go --help
```

## Migration from Bash

The Go version provides:

1. **Better error handling** - Proper error propagation vs `set -e`
2. **Type safety** - Port is an `int`, validated at startup
3. **Testability** - Each step can be tested independently
4. **Rollback support** - Failed deployments clean up after themselves
5. **Structured logging** - Consistent, parseable output
6. **Cross-compilation** - Build on macOS, deploy to Linux

## Management Commands

After deployment, use these commands:

```bashnmcserver start       # Start server
mcserver stop        # Stop server
mcserver restart     # Restart server
mcserver logs        # View logs
mcserver backup      # Backup world
mcserver fix         # Fix locks/crashes
mcserver redeploy    # Full redeploy
mcserver update      # Quick plugin update
```

## License

Same as the main project.
