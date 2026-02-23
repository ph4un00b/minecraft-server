# Ubuntu Server Deployment Guide

One-command automated deployment script for Ubuntu servers.

## Quick Start

```bash
# 1. Upload the deploy.sh script to your Ubuntu server
scp deploy.sh root@your-server-ip:/root/

# 2. SSH into your server
ssh root@your-server-ip

# 3. Run the deployment script
chmod +x deploy.sh
./deploy.sh
```

## Script Options

```bash
./deploy.sh [OPTIONS]

Options:
  --skip-java        Skip Java installation (if already installed)
  --skip-firewall    Skip firewall configuration  
  --ram=SIZE         Set server RAM: 512M, 1G, 2G, or 4G (default: 1G)
  --port=PORT        Set server port (default: 25565)
  --user=USER        Set service user (default: minecraft)
  --help             Show help message
```

### Examples

```bash
# Default deployment (1GB RAM, port 25565) - Optimized for VPS
./deploy.sh

# Ultra low-memory VPS (512MB RAM) - Minimal settings
./deploy.sh --ram=512M

# More memory (2GB RAM) - Better for multiple players
./deploy.sh --ram=2G

# Different port
./deploy.sh --port=25566

# Skip Java install (if you already have Java 21)
./deploy.sh --skip-java
```

## What the Script Does

The deployment script performs these 12 steps automatically:

1. **Update System** - Updates all Ubuntu packages
2. **Install Java 21** - Downloads and installs OpenJDK or Eclipse Temurin
3. **Create User** - Creates a dedicated `minecraft` user (security)
4. **Check Project** - Validates project files are present
5. **Setup Project** - Copies all files to `/home/minecraft/colosseum-arena/`
6. **Build Project** - Compiles the plugin and downloads Paper
7. **Configure Server** - Creates server.properties with your settings
8. **Setup Firewall** - Opens port 25565 (or your custom port)
9. **Setup Swap** - Creates 2GB swap file if none exists (for low RAM)
10. **Systemd Service** - Creates auto-starting service with your RAM settings
11. **Helper Scripts** - Installs `mcserver` command for easy management
12. **Start Server** - Launches the Minecraft server

## Management Commands

After deployment, use these commands to manage your server:

```bash
# Start/stop/restart
mcserver start
mcserver stop
mcserver restart

# View real-time logs
mcserver logs

# Check status
mcserver status

# Backup world data
mcserver backup

# Fix crashes/locks
mcserver fix

# Update server files
mcserver update
```

## Connecting to Your Server

Players connect using your server's IP address:

```
Server Address: your-server-ip:25565
```

If using a custom port:
```
Server Address: your-server-ip:YOUR_PORT
```

## Prerequisites

### Server Requirements
- Ubuntu 20.04 LTS, 22.04 LTS, or 24.04 LTS
- **512MB+ RAM** (512M minimum, 1GB default, 2GB+ recommended)
- 10GB+ disk space
- Root access (sudo)
- Internet connection

**RAM Recommendations:**
- **512MB**: Ultra-low budget VPS, 1-2 players max, minimal settings
- **1GB (Default)**: Standard VPS, 3-5 players, optimized settings, view-distance=5
- **2GB+**: Comfortable for 5-10 players with higher view distance

### Before Running

1. **Copy project files to server:**
```bash
# On your local machine, create archive (excluding large dirs)
tar --exclude='.git' --exclude='.gradle' --exclude='server' --exclude='build' -czvf colosseum-arena.tar.gz .

# Upload to server
scp colosseum-arena.tar.gz root@your-server-ip:/root/

# On server, extract
ssh root@your-server-ip
tar -xzvf colosseum-arena.tar.gz
```

2. **Cloud Provider Setup** (AWS, DigitalOcean, etc.):
   - Open port 25565 (TCP and UDP) in your cloud firewall/security group
   - Script configures UFW firewall, but cloud firewall must also allow the port

## Troubleshooting

### Server won't start
```bash
# Check logs
mcserver logs

# Fix locks/crashes
mcserver fix
mcserver start
```

### Permission denied
```bash
# Make sure you ran as root
sudo ./deploy.sh
```

### Port already in use
```bash
# Find what's using the port
sudo lsof -i :25565
# Kill the process or change port in server.properties
```

### Java not found
```bash
# Run without skip flag
./deploy.sh  # (don't use --skip-java)
```

## Security Notes

1. **Dedicated User**: Script creates a `minecraft` user - server never runs as root
2. **Firewall**: Port 25565 is opened, but only for Minecraft (SSH remains on port 22)
3. **Swap**: Created for stability on low-RAM VPS instances
4. **Auto-start**: Server starts automatically on system boot

## Files Created

- `/home/minecraft/colosseum-arena/` - Server files
- `/etc/systemd/system/minecraft.service` - Auto-start service
- `/usr/local/bin/mcserver` - Management helper script
- `/swapfile` - 2GB swap (if not already present)
- `/etc/ufw/` - Firewall rules for port 25565

## Updating the Server

To update plugin or server files:

```bash
# Method 1: Use the update command
mcserver update

# Method 2: Manual update
cd /home/minecraft/colosseum-arena
sudo -u minecraft git pull  # if using git
sudo -u minecraft ./gradlew setup --no-daemon
mcserver restart
```

## Support

If deployment fails:
1. Check the error message at the failing step
2. Run `mcserver logs` to see server output
3. Try `mcserver fix` to clear any locks
4. Check disk space: `df -h`
5. Check memory: `free -h`
