#!/bin/bash

# Minecraft Colosseum Arena Server - Automated Ubuntu Deployment Script
# Usage: sudo ./deploy.sh [OPTIONS]
# Options:
#   --skip-java        Skip Java installation (if already installed)
#   --skip-firewall    Skip firewall configuration
#   --ram=SIZE         Set server RAM (default: 1G, options: 512M, 1G, 2G, 4G)
#   --port=25565       Set server port (default: 25565)
#   --user=minecraft   Set service user (default: minecraft)

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
RAM="1G"                              # Default: 1GB (optimized for VPS)
PORT="25565"
USER="minecraft"
SKIP_JAVA=false
SKIP_FIREWALL=false
PROJECT_DIR="/home/${USER}/colosseum-arena"

# Parse arguments
for arg in "$@"; do
    case $arg in
        --skip-java)
            SKIP_JAVA=true
            shift
            ;;
        --skip-firewall)
            SKIP_FIREWALL=true
            shift
            ;;
        --ram=*)
            RAM="${arg#*=}"
            shift
            ;;
        --port=*)
            PORT="${arg#*=}"
            shift
            ;;
        --user=*)
            USER="${arg#*=}"
            shift
            ;;
        --help)
            echo "Usage: sudo ./deploy.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --skip-java        Skip Java installation"
            echo "  --skip-firewall    Skip firewall configuration"
            echo "  --ram=SIZE         Set server RAM (default: 2G, options: 1G, 2G, 4G)"
            echo "  --port=PORT        Set server port (default: 25565)"
            echo "  --user=USER        Set service user (default: minecraft)"
            echo "  --help             Show this help message"
            exit 0
            ;;
    esac
done

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_error "This script must be run as root (use sudo)"
        exit 1
    fi
}

# Check Ubuntu version
check_os() {
    if ! command -v lsb_release &> /dev/null; then
        apt-get install -y lsb-release
    fi
    
    OS=$(lsb_release -si)
    VERSION=$(lsb_release -sr)
    
    if [[ "$OS" != "Ubuntu" ]]; then
        log_warn "This script is designed for Ubuntu. Detected: $OS $VERSION"
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        log_info "Detected: Ubuntu $VERSION"
    fi
}

# Step 1: Update system
step1_update_system() {
    log_step "Step 1/12: Updating system packages..."
    
    apt-get update
    apt-get upgrade -y
    apt-get install -y wget curl git screen htop zip micro unzip lsb-release ufw
    
    log_info "System updated successfully"
}

# Step 2: Install Java 21
step2_install_java() {
    if [ "$SKIP_JAVA" = true ]; then
        log_step "Step 2/12: Skipping Java installation (--skip-java flag set)"
        return
    fi
    
    log_step "Step 2/12: Installing Java 21..."
    
    # Check if Java 21 is already installed
    if command -v java &> /dev/null && java -version 2>&1 | grep -q "21"; then
        log_info "Java 21 is already installed"
        java -version
        return
    fi
    
    # Try to install OpenJDK 21
    if apt-cache show openjdk-21-jdk &> /dev/null; then
        log_info "Installing OpenJDK 21..."
        apt-get install -y openjdk-21-jdk
    else
        log_warn "OpenJDK 21 not available in default repos"
        log_info "Installing Eclipse Temurin JDK 21..."
        
        # Install Temurin JDK 21
        apt-get install -y wget apt-transport-https gnupg
        
        # Add Adoptium repository
        wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add -
        echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
        
        apt-get update
        apt-get install -y temurin-21-jdk
    fi
    
    # Verify installation
    if java -version 2>&1 | grep -q "21"; then
        log_info "Java 21 installed successfully"
        java -version
    else
        log_error "Java 21 installation failed"
        exit 1
    fi
}

# Step 3: Create minecraft user
step3_create_user() {
    log_step "Step 3/12: Creating service user '$USER'..."
    
    if id "$USER" &>/dev/null; then
        log_warn "User '$USER' already exists, skipping creation"
    else
        useradd -r -m -s /bin/bash "$USER"
        log_info "User '$USER' created"
    fi
    
    # Create necessary directories
    mkdir -p "/home/$USER"
    chown "$USER:$USER" "/home/$USER"
}

# Step 4: Check project files
step4_check_project() {
    log_step "Step 4/12: Checking project files..."
    
    if [ ! -f "build.gradle.kts" ]; then
        log_error "Project files not found! Please run this script from the project root directory"
        log_error "Expected files: build.gradle.kts, gradlew, scripts/, etc."
        exit 1
    fi
    
    if [ ! -d "scripts" ]; then
        log_error "Scripts directory not found!"
        exit 1
    fi
    
    # Make scripts executable
    chmod +x scripts/*.sh 2>/dev/null || true
    chmod +x gradlew 2>/dev/null || true
    
    log_info "Project files validated"
}

# Step 5: Setup project directory
step5_setup_project() {
    log_step "Step 5/12: Setting up project directory..."
    
    PROJECT_DIR="/home/${USER}/colosseum-arena"
    
    # Create project directory
    mkdir -p "$PROJECT_DIR"
    
    # Copy project files
    log_info "Copying project files to $PROJECT_DIR..."
    
    # Copy all files except excluded directories
    rsync -av --exclude='.git' --exclude='.gradle' --exclude='build' --exclude='server' . "$PROJECT_DIR/" 2>/dev/null || {
        # Fallback if rsync not available
        log_warn "rsync not available, using cp -r (slower)..."
        cp -r . "$PROJECT_DIR/"
        rm -rf "$PROJECT_DIR/.git" "$PROJECT_DIR/.gradle" "$PROJECT_DIR/build" "$PROJECT_DIR/server" 2>/dev/null || true
    }
    
    # Create necessary directories
    mkdir -p "$PROJECT_DIR/server"
    mkdir -p "$PROJECT_DIR/plugins"
    mkdir -p "$PROJECT_DIR/external"
    
    # Set ownership
    chown -R "$USER:$USER" "$PROJECT_DIR"
    
    log_info "Project files copied to $PROJECT_DIR"
}

# Step 6: Build project
step6_build_project() {
    log_step "Step 6/12: Building project..."
    
    cd "$PROJECT_DIR"
    
    # Run as minecraft user
    su - "$USER" -c "cd $PROJECT_DIR && ./gradlew setup --no-daemon" || {
        log_error "Build failed!"
        exit 1
    }
    
    log_info "Project built successfully"
}

# Step 7: Configure server.properties
step7_configure_server() {
    log_step "Step 7/12: Configuring server.properties..."
    
    SERVER_PROPS="$PROJECT_DIR/server/server.properties"
    
    if [ ! -f "$SERVER_PROPS" ]; then
        log_warn "server.properties not found, creating default..."
        
        cat > "$SERVER_PROPS" << EOF
# Colosseum Arena Server Properties
# Auto-generated by deployment script
# Optimized for 1GB RAM (view-distance=5, max-players=5)

server-port=$PORT
server-ip=0.0.0.0
gamemode=survival
difficulty=peaceful
level-type=flat
max-players=5
spawn-protection=0
view-distance=5
simulation-distance=5
motd=Gothic Battleground
enable-command-block=false
generate-structures=false
spawn-npcs=false
spawn-animals=false
spawn-monsters=false
online-mode=false
enforce-secure-profile=false
entity-broadcast-range-percentage=50
EOF
        
        chown "$USER:$USER" "$SERVER_PROPS"
        log_info "Created server.properties with port $PORT"
    else
        # Update port if different
        sed -i "s/^server-port=.*/server-port=$PORT/" "$SERVER_PROPS"
        sed -i 's/^gamemode=.*/gamemode=survival/' "$SERVER_PROPS"
        log_info "Updated server.properties"
    fi
    
    # Copy phau.properties template if not exists
    if [ -f "$PROJECT_DIR/templates/phau.properties.defaults" ] && [ ! -f "$PROJECT_DIR/server/phau.properties" ]; then
        cp "$PROJECT_DIR/templates/phau.properties.defaults" "$PROJECT_DIR/server/phau.properties"
        chown "$USER:$USER" "$PROJECT_DIR/server/phau.properties"
        log_info "Copied phau.properties template"
    fi
}

# Step 8: Configure firewall
step8_firewall() {
    if [ "$SKIP_FIREWALL" = true ]; then
        log_step "Step 8/12: Skipping firewall configuration (--skip-firewall flag set)"
        return
    fi
    
    log_step "Step 8/12: Configuring firewall..."
    
    # Check if UFW is installed
    if ! command -v ufw &> /dev/null; then
        apt-get install -y ufw
    fi
    
    # Allow Minecraft port
    ufw allow "$PORT/tcp" comment 'Minecraft Server'
    ufw allow "$PORT/udp" comment 'Minecraft Server'
    
    # Allow SSH (important!)
    ufw allow 22/tcp comment 'SSH Access'
    
    # Enable firewall (only if not already enabled)
    if ! ufw status | grep -q "Status: active"; then
        echo "y" | ufw enable
    fi
    
    ufw status
    log_info "Firewall configured for port $PORT"
}

# Step 9: Setup swap (essential for 1GB RAM systems)
step9_setup_swap() {
    log_step "Step 9/12: Checking swap configuration..."
    
    # Check current swap
    CURRENT_SWAP=$(free -m | awk '/^Swap:/ {print $2}')
    
    if [ "$CURRENT_SWAP" -eq 0 ]; then
        log_warn "No swap configured. Creating 3GB swap file for 1GB RAM system..."
        
        # Create swap file (3GB for 1GB systems)
        fallocate -l 3G /swapfile
        chmod 600 /swapfile
        mkswap /swapfile
        swapon /swapfile
        
        # Make permanent
        echo '/swapfile none swap sw 0 0' >> /etc/fstab
        
        log_info "3GB swap file created and enabled"
    else
        log_info "Swap already configured: ${CURRENT_SWAP}MB"
    fi
}

# Step 10: Create systemd service
step10_systemd_service() {
    log_step "Step 10/12: Creating systemd service..."
    
    SERVICE_FILE="/etc/systemd/system/minecraft.service"
    
    # Calculate JVM memory settings with optimization profiles
    case $RAM in
        512M)
            # Ultra-low memory: SerialGC for minimal overhead
            JVM_RAM="-Xms400M -Xmx450M -XX:+UseSerialGC -XX:+AlwaysPreTouch"
            ;;
        1G)
            # Default: 1GB with G1GC optimized for short pauses
            JVM_RAM="-Xms768M -Xmx900M -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:G1HeapRegionSize=4M"
            ;;
        2G)
            # Comfortable: Standard G1GC settings
            JVM_RAM="-Xms2G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=8M"
            ;;
        4G)
            # High-performance: Large heap regions
            JVM_RAM="-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16M"
            ;;
        *)
            # Fallback to 1GB default
            JVM_RAM="-Xms768M -Xmx900M -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:G1HeapRegionSize=4M"
            ;;
    esac
    
    cat > "$SERVICE_FILE" << EOF
[Unit]
Description=Minecraft Colosseum Arena Server
After=network.target

[Service]
Type=simple
User=$USER
Group=$USER
WorkingDirectory=$PROJECT_DIR/server
ExecStart=$PROJECT_DIR/scripts/start-server.sh
Restart=on-failure
RestartSec=10
StandardInput=null
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

    # Create wrapper script for JVM args
    cat > "$PROJECT_DIR/scripts/start-server.sh" << EOF
#!/bin/bash
# Minecraft Server Start Script (Auto-generated)
cd $PROJECT_DIR/server

exec java $JVM_RAM \\
    -XX:+UseG1GC \\
    -XX:+ParallelRefProcEnabled \\
    -XX:MaxGCPauseMillis=200 \\
    -XX:+UnlockExperimentalVMOptions \\
    -XX:+DisableExplicitGC \\
    -XX:G1NewSizePercent=30 \\
    -XX:G1MaxNewSizePercent=40 \\
    -XX:G1HeapRegionSize=8M \\
    -XX:G1ReservePercent=20 \\
    -XX:G1HeapWastePercent=5 \\
    -Dpaper.disableWatchdog=true \\
    -Djava.awt.headless=true \\
    -jar paper-1.21.4.jar \\
    --nogui
EOF

    chmod +x "$PROJECT_DIR/scripts/start-server.sh"
    chown -R "$USER:$USER" "$PROJECT_DIR"
    
    # Reload systemd
    systemctl daemon-reload
    
    # Enable auto-start
    systemctl enable minecraft
    
    log_info "Systemd service created and enabled"
    log_info "Service config: RAM=$RAM, Port=$PORT"
}

# Step 11: Create helper scripts
step11_helper_scripts() {
    log_step "Step 11/12: Creating helper scripts..."
    
    # Create management script
    cat > "/usr/local/bin/mcserver" << 'EOF'
#!/bin/bash
# Minecraft Server Management Script

show_help() {
    echo "Usage: mcserver [command]"
    echo ""
    echo "Commands:"
    echo "  start       Start the Minecraft server"
    echo "  stop        Stop the Minecraft server"
    echo "  restart     Restart the Minecraft server"
    echo "  status      Check server status"
    echo "  logs        View server logs"
    echo "  console     Attach to server console (Ctrl+A then D to detach)"
    echo "  backup      Backup world data"
    echo "  fix         Kill server processes and fix locks"
    echo "  update      Update server files and restart"
    echo "  help        Show this help message"
}

case "$1" in
    start)
        sudo systemctl start minecraft
        echo "Server started"
        ;;
    stop)
        echo "Stopping server..."
        sudo systemctl stop minecraft
        ;;
    restart)
        echo "Restarting server..."
        sudo systemctl restart minecraft
        ;;
    status)
        sudo systemctl status minecraft
        ;;
    logs)
        sudo journalctl -u minecraft -f
        ;;
    console)
        sudo -u minecraft screen -r minecraft 2>/dev/null || {
            echo "Starting console..."
            sudo -u minecraft screen -S minecraft -d -m bash -c "cd /home/minecraft/colosseum-arena && ./scripts/start-server.sh"
            sleep 2
            sudo -u minecraft screen -r minecraft
        }
        ;;
    backup)
        BACKUP_DIR="/home/minecraft/backups"
        mkdir -p "$BACKUP_DIR"
        BACKUP_FILE="$BACKUP_DIR/world-$(date +%Y%m%d-%H%M%S).tar.gz"
        echo "Creating backup: $BACKUP_FILE"
        sudo tar -czf "$BACKUP_FILE" -C /home/minecraft/colosseum-arena/server world/
        echo "Backup complete!"
        ;;
    fix)
        echo "Killing server processes and fixing locks..."
        sudo pkill -9 -f "paper-1.21.4.jar" 2>/dev/null || true
        sudo pkill -9 java 2>/dev/null || true
        sleep 2
        rm -f /home/minecraft/colosseum-arena/server/world/session.lock 2>/dev/null || true
        echo "Locks cleared. You can now start the server."
        ;;
    update)
        echo "Updating server..."
        cd /home/minecraft/colosseum-arena
        sudo -u minecraft git pull 2>/dev/null || echo "Not a git repo, skipping pull"
        sudo -u minecraft ./gradlew setup --no-daemon
        sudo systemctl restart minecraft
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        show_help
        exit 1
        ;;
esac
EOF

    chmod +x /usr/local/bin/mcserver
    
    log_info "Helper script 'mcserver' installed"
    log_info "Usage: mcserver [start|stop|restart|logs|backup|fix]"
}

# Step 12: Start server
step12_start_server() {
    log_step "Step 12/12: Starting Minecraft server..."
    
    systemctl start minecraft
    sleep 3
    
    # Check if started successfully
    if systemctl is-active --quiet minecraft; then
        log_info "Server started successfully!"
    else
        log_warn "Server may not have started properly. Checking logs..."
        journalctl -u minecraft -n 20 --no-pager
    fi
    
    # Get server IP
    IP=$(hostname -I | awk '{print $1}')
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  DEPLOYMENT COMPLETE!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "Server IP: ${GREEN}$IP:$PORT${NC}"
    echo -e "User: ${GREEN}$USER${NC}"
    echo -e "Project: ${GREEN}$PROJECT_DIR${NC}"
    echo -e "RAM: ${GREEN}$RAM${NC}"
    echo ""
    echo "Management Commands:"
    echo "  mcserver start       - Start server"
    echo "  mcserver stop        - Stop server"
    echo "  mcserver restart     - Restart server"
    echo "  mcserver logs        - View logs"
    echo "  mcserver backup      - Backup world"
    echo "  mcserver fix         - Fix locks/crashes"
    echo ""
    echo -e "Connect in Minecraft: ${GREEN}$IP:$PORT${NC}"
    echo ""
}

# Main deployment flow
main() {
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Minecraft Colosseum Arena Server${NC}"
    echo -e "${GREEN}  Ubuntu Deployment Script${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    
    check_root
    check_os
    
    log_info "Starting deployment with settings:"
    log_info "  User: $USER"
    log_info "  RAM: $RAM (1GB default, optimized for VPS)"
    log_info "  Port: $PORT"
    log_info "  View Distance: 5 chunks"
    log_info "  Max Players: 5"
    log_info "  Swap: 3GB (auto-created for 1GB systems)"
    log_info "  Skip Java: $SKIP_JAVA"
    log_info "  Skip Firewall: $SKIP_FIREWALL"
    echo ""
    
    # Run all steps
    step1_update_system
    step2_install_java
    step3_create_user
    step4_check_project
    step5_setup_project
    step6_build_project
    step7_configure_server
    step8_firewall
    step9_setup_swap
    step10_systemd_service
    step11_helper_scripts
    step12_start_server
    
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Installation Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Connect to your server: mcserver console"
    echo "2. In console, OP yourself: op <your-minecraft-username>"
    echo "3. Start playing!"
    echo ""
    echo "For troubleshooting:"
    echo "  mcserver logs      - View recent logs"
    echo "  mcserver fix       - Kill stuck processes"
    echo "  mcserver backup    - Backup world data"
}

# Run main function
main "$@"
