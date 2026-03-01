#!/bin/bash

# Minecraft Colosseum Arena Server - Automated Ubuntu Deployment Script
# Usage: sudo ./deploy.sh [OPTIONS]
# 
# This script deploys the Colosseum Arena server on Ubuntu
# It handles Java installation, server setup, firewall, and systemd service
#
# Options:
#   --skip-java        Skip Java installation (if already installed)
#   --skip-firewall    Skip firewall configuration
#   --ram=SIZE         Set server RAM (default: 2G, options: 512M, 1G, 2G, 4G)
#   --port=25565       Set server port (default: 25565)
#   --user=minecraft   Set service user (default: minecraft)
#   --redeploy         Force redeployment (stop, clean, rebuild, restart)

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
RAM="2G"                              # Default: 2GB
PORT="25565"
USER="minecraft"
SKIP_JAVA=false
SKIP_FIREWALL=false
REDEPLOY=false
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
        --redeploy)
            REDEPLOY=true
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
            echo "  --redeploy         Force redeployment (stop, clean, rebuild, restart)"
            echo "  --help             Show this help message"
            echo ""
            echo "Examples:"
            echo "  sudo ./deploy.sh                    # Fresh install with 2GB RAM"
            echo "  sudo ./deploy.sh --ram=4G           # Install with 4GB RAM"
            echo "  sudo ./deploy.sh --redeploy         # Redeploy/update existing server"
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
    log_step "Step 1/11: Updating system packages..."
    
    apt-get update
    apt-get upgrade -y
    apt-get install -y wget curl git screen htop zip unzip lsb-release ufw rsync
    
    log_info "System updated successfully"
}

# Step 2: Install Java 21
step2_install_java() {
    if [ "$SKIP_JAVA" = true ]; then
        log_step "Step 2/11: Skipping Java installation (--skip-java flag set)"
        return
    fi
    
    log_step "Step 2/11: Installing Java 21..."
    
    # Check if Java 21 is already installed
    if command -v java &> /dev/null && java -version 2>&1 | grep -q "21"; then
        log_info "Java 21 is already installed"
        java -version 2>&1 | head -1
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
        java -version 2>&1 | head -1
    else
        log_error "Java 21 installation failed"
        exit 1
    fi
}

# Step 3: Create minecraft user
step3_create_user() {
    log_step "Step 3/11: Creating service user '$USER'..."
    
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
    log_step "Step 4/11: Checking project files..."
    
    if [ ! -f "build.gradle.kts" ]; then
        log_error "Project files not found! Please run this script from the project root directory"
        log_error "Expected files: build.gradle.kts, gradlew, scripts/, src/, etc."
        log_error "Current directory: $(pwd)"
        exit 1
    fi
    
    if [ ! -f "gradlew" ]; then
        log_error "gradlew not found! This should be included in the project."
        exit 1
    fi
    
    if [ ! -d "scripts" ]; then
        log_error "scripts/ directory not found!"
        exit 1
    fi
    
    # Make scripts executable
    chmod +x scripts/*.sh 2>/dev/null || true
    chmod +x gradlew 2>/dev/null || true
    
    log_info "Project files validated"
}

# Step 5: Handle redeployment or fresh install
step5_setup_project() {
    log_step "Step 5/11: Setting up project directory..."
    
    PROJECT_DIR="/home/${USER}/colosseum-arena"
    
    if [ "$REDEPLOY" = true ] && [ -d "$PROJECT_DIR" ]; then
        log_warn "Redeploy mode: Stopping server and cleaning old build..."
        
        # Stop server if running
        systemctl stop minecraft 2>/dev/null || true
        pkill -9 -f "paper-1.21.11.jar" 2>/dev/null || true
        sleep 2
        
        # Backup world if it exists
        if [ -d "$PROJECT_DIR/server/world" ]; then
            BACKUP_DIR="/home/$USER/backups"
            mkdir -p "$BACKUP_DIR"
            BACKUP_FILE="$BACKUP_DIR/world-redeploy-$(date +%Y%m%d-%H%M%S).tar.gz"
            log_info "Backing up world to $BACKUP_FILE..."
            tar -czf "$BACKUP_FILE" -C "$PROJECT_DIR/server" world/ 2>/dev/null || true
        fi
        
        # Clean old build artifacts but keep server configs
        log_info "Cleaning old build files..."
        rm -rf "$PROJECT_DIR/build" 2>/dev/null || true
        rm -rf "$PROJECT_DIR/.gradle" 2>/dev/null || true
        rm -rf "$PROJECT_DIR/plugins" 2>/dev/null || true
    fi
    
    # Create project directory if it doesn't exist
    mkdir -p "$PROJECT_DIR"
    
    # Copy project files using rsync (preserves permissions, excludes unnecessary dirs)
    log_info "Copying project files to $PROJECT_DIR..."
    
    if command -v rsync &> /dev/null; then
        rsync -av --delete \
            --exclude='.gradle' \
            --exclude='build' \
            --exclude='.kotlin' \
            --exclude='.idea' \
            --exclude='*.tar.gz' \
            . "$PROJECT_DIR/"
    else
        # Fallback if rsync not available
        log_warn "rsync not available, using cp method..."
        rm -rf "$PROJECT_DIR"
        mkdir -p "$PROJECT_DIR"
        cp -r . "$PROJECT_DIR/"
        # Keep .git but remove other build dirs
        rm -rf "$PROJECT_DIR/.gradle" "$PROJECT_DIR/build" 2>/dev/null || true
    fi
    
    # Create necessary directories
    mkdir -p "$PROJECT_DIR/server"
    mkdir -p "$PROJECT_DIR/plugins"
    mkdir -p "$PROJECT_DIR/external"
    
    # Set ownership
    chown -R "$USER:$USER" "$PROJECT_DIR"
    
    # Make gradlew executable for the user
    chmod +x "$PROJECT_DIR/gradlew"
    chmod +x "$PROJECT_DIR/scripts/"*.sh
    
    log_info "Project files copied to $PROJECT_DIR"
}

# Step 5a: Ensure version.properties exists (recreate if missing)
step5a_ensure_version_file() {
    log_step "Step 5a/11: Ensuring version.properties exists..."
    
    local version_file="$PROJECT_DIR/src/main/resources/version.properties"
    
    # Check if version.properties exists
    if [ -f "$version_file" ]; then
        log_info "version.properties found"
        return 0
    fi
    
    log_warn "version.properties not found! Recreating..."
    
    # Ensure directory exists
    mkdir -p "$PROJECT_DIR/src/main/resources"
    
    # Check if git is available and we're in a git repo
    if command -v git &> /dev/null && [ -d "$PROJECT_DIR/.git" ]; then
        log_info "Git available. Recreating from git data..."
        
        cd "$PROJECT_DIR"
        
        # Get git info
        local git_hash=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
        local git_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
        local build_time=$(date +"%Y-%m-%d %H:%M:%S")
        
        # Create version.properties with git data
        cat > "$version_file" << EOF
# Colosseum Arena Version Info
# This file is the SOURCE OF TRUTH for version information
# It is committed to git and serves as the base template
# Gradle updates these values during build if git is available

version=1.0+${git_hash}
build.time=${build_time}
git.hash=${git_hash}
git.branch=${git_branch}
EOF
        
        log_info "✓ Created version.properties from git data: 1.0+${git_hash}"
        
    else
        log_warn "Git not available. Using default template..."
        
        # Create default template
        cat > "$version_file" << 'EOF'
# Colosseum Arena Version Info
# This file is the SOURCE OF TRUTH for version information
# It is committed to git and serves as the base template
# Gradle updates these values during build if git is available

version=1.0+unknown
build.time=unknown
git.hash=unknown
git.branch=unknown
EOF
        
        log_warn "✓ Created version.properties with default values (no git)"
        log_warn "Plugin will show version: 1.0+unknown"
    fi
    
    # Set ownership
    chown "$USER:$USER" "$version_file"
}

# Step 5b: Verify Plugin Version in JAR
step5b_verify_version() {
    log_step "Step 5b/11: Verifying plugin version..."
    
    cd "$PROJECT_DIR"
    
    # Build just the plugin JAR (not full setup)
    log_info "Building plugin to generate version info..."
    su - "$USER" -c "cd $PROJECT_DIR && ./gradlew jar --no-daemon --console=plain" || {
        log_error "Build failed! Cannot verify version."
        return 1
    }
    
    # Check if JAR was created
    if [ ! -f "$PROJECT_DIR/plugins/colosseum-arena-1.0.jar" ]; then
        log_error "Plugin JAR not found at $PROJECT_DIR/plugins/colosseum-arena-1.0.jar"
        return 1
    fi
    
    # Verify JAR contains version.properties
    if ! unzip -l "$PROJECT_DIR/plugins/colosseum-arena-1.0.jar" | grep -q "version.properties"; then
        log_error "JAR missing version.properties!"
        return 1
    fi
    
    log_info "✓ JAR contains version.properties"
    
    # Extract and display version info from JAR
    local version_info=$(unzip -p "$PROJECT_DIR/plugins/colosseum-arena-1.0.jar" version.properties 2>/dev/null)
    local version=$(echo "$version_info" | grep "^version=" | cut -d'=' -f2)
    local build_time=$(echo "$version_info" | grep "^build.time=" | cut -d'=' -f2)
    local git_hash=$(echo "$version_info" | grep "^git.hash=" | cut -d'=' -f2)
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Plugin Version Info${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "  Version: ${GREEN}${version:-unknown}${NC}"
    echo -e "  Build Time: ${GREEN}${build_time:-unknown}${NC}"
    echo -e "  Git Hash: ${GREEN}${git_hash:-unknown}${NC}"
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo ""
    
    log_info "Plugin version verified: ${version:-unknown}"
}

# Step 6: Build project
step6_build_project() {
    log_step "Step 6/11: Building project..."
    
    cd "$PROJECT_DIR"
    
    # Run gradle setup as the user
    log_info "Running Gradle setup..."
    
    # Use su to run as the minecraft user
    su - "$USER" -c "cd $PROJECT_DIR && ./gradlew setup --no-daemon --console=plain" || {
        log_error "Build failed! Check the output above for errors."
        log_error "Common issues:"
        log_error "  - Network connectivity for downloading Paper"
        log_error "  - Java version mismatch"
        log_error "  - Permission issues"
        exit 1
    }
    
    log_info "Project built successfully"
}

# Step 7: Configure server.properties
step7_configure_server() {
    log_step "Step 7/11: Configuring server..."
    
    SERVER_PROPS="$PROJECT_DIR/server/server.properties"
    
    # Set view distance based on RAM
    case $RAM in
        512M|1G)
            VIEW_DISTANCE=5
            MAX_PLAYERS=5
            ;;
        2G)
            VIEW_DISTANCE=8
            MAX_PLAYERS=10
            ;;
        4G)
            VIEW_DISTANCE=12
            MAX_PLAYERS=20
            ;;
        *)
            VIEW_DISTANCE=8
            MAX_PLAYERS=10
            ;;
    esac
    
    # Create or update server.properties
    cat > "$SERVER_PROPS" << EOF
# Colosseum Arena Server Properties
# Auto-generated by deployment script
# RAM: $RAM, View Distance: $VIEW_DISTANCE, Max Players: $MAX_PLAYERS

server-port=$PORT
server-ip=0.0.0.0
gamemode=survival
difficulty=peaceful
level-type=flat
max-players=$MAX_PLAYERS
spawn-protection=0
view-distance=$VIEW_DISTANCE
simulation-distance=$VIEW_DISTANCE
motd=Colosseum Arena - Gothic Battleground
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
    log_info "Created server.properties (port: $PORT, view-distance: $VIEW_DISTANCE)"
    
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
        log_step "Step 8/11: Skipping firewall configuration (--skip-firewall flag set)"
        return
    fi
    
    log_step "Step 8/11: Configuring firewall..."
    
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

# Step 9: Setup swap
step9_setup_swap() {
    log_step "Step 9/11: Checking swap configuration..."
    
    # Check current swap
    CURRENT_SWAP=$(free -m | awk '/^Swap:/ {print $2}')
    
    if [ "$CURRENT_SWAP" -eq 0 ]; then
        # Determine swap size based on RAM
        case $RAM in
            512M)
                SWAP_SIZE="2G"
                ;;
            1G)
                SWAP_SIZE="3G"
                ;;
            2G)
                SWAP_SIZE="4G"
                ;;
            4G)
                SWAP_SIZE="4G"
                ;;
            *)
                SWAP_SIZE="3G"
                ;;
        esac
        
        log_warn "No swap configured. Creating $SWAP_SIZE swap file..."
        
        fallocate -l $SWAP_SIZE /swapfile
        chmod 600 /swapfile
        mkswap /swapfile
        swapon /swapfile
        
        # Make permanent
        if ! grep -q '/swapfile' /etc/fstab; then
            echo '/swapfile none swap sw 0 0' >> /etc/fstab
        fi
        
        log_info "$SWAP_SIZE swap file created and enabled"
    else
        log_info "Swap already configured: ${CURRENT_SWAP}MB"
    fi
}

# Step 10: Create systemd service
step10_systemd_service() {
    log_step "Step 10/11: Creating systemd service..."
    
    SERVICE_FILE="/etc/systemd/system/minecraft.service"
    
    # Generate the service file that uses the project's start-server.sh
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
SyslogIdentifier=minecraft

[Install]
WantedBy=multi-user.target
EOF

    # Make sure start-server.sh uses the correct JVM args based on RAM
    generate_start_script
    
    # Reload systemd
    systemctl daemon-reload
    
    # Enable auto-start
    systemctl enable minecraft
    
    log_info "Systemd service created and enabled"
}

# Generate start-server.sh with correct JVM args
generate_start_script() {
    local START_SCRIPT="$PROJECT_DIR/scripts/start-server.sh"
    
    # Calculate JVM memory settings
    case $RAM in
        512M)
            JVM_ARGS="-Xms400M -Xmx450M"
            ;;
        1G)
            JVM_ARGS="-Xms900M -Xmx1G"
            ;;
        2G)
            JVM_ARGS="-Xms2G -Xmx2G"
            ;;
        4G)
            JVM_ARGS="-Xms4G -Xmx4G"
            ;;
        *)
            JVM_ARGS="-Xms2G -Xmx2G"
            ;;
    esac
    
    cat > "$START_SCRIPT" << EOF
#!/bin/bash

# Minecraft Server Start Script
# Generated by deploy.sh with RAM=$RAM

cd \$(dirname "\$0")/../server || exit 1

if [ ! -f "eula.txt" ]; then
    echo "[ERROR] Server not initialized. EULA not accepted."
    exit 1
fi

echo "[INFO] Starting PaperMC server with $RAM RAM..."
echo "[INFO] Type 'help' for commands, 'stop' to shutdown"
echo ""

exec java $JVM_ARGS \\
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
    -jar paper-1.21.11.jar \\
    --nogui
EOF

    chmod +x "$START_SCRIPT"
    chown "$USER:$USER" "$START_SCRIPT"
}

# Step 11: Create helper scripts
step11_helper_scripts() {
    log_step "Step 11/11: Creating management scripts..."
    
    # Create mcserver management script
    cat > "/usr/local/bin/mcserver" << 'EOF'
#!/bin/bash
# Minecraft Server Management Script

USER="minecraft"
PROJECT_DIR="/home/$USER/colosseum-arena"

show_help() {
    echo "Usage: mcserver [command]"
    echo ""
    echo "Commands:"
    echo "  start       Start the Minecraft server"
    echo "  stop        Stop the Minecraft server"
    echo "  restart     Restart the Minecraft server"
    echo "  status      Check server status"
    echo "  logs        View server logs (follow mode)"
    echo "  log         View last 50 log lines"
    echo "  console     Attach to server console (screen)"
    echo "  backup      Backup world data"
    echo "  fix         Kill server processes and fix locks"
    echo "  redeploy    Stop, clean, rebuild, and restart"
    echo "  update      Update plugin only (quick redeploy)"
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
        sleep 2
        echo "Server stopped"
        ;;
    restart)
        echo "Restarting server..."
        sudo systemctl restart minecraft
        echo "Server restarted"
        ;;
    status)
        sudo systemctl status minecraft --no-pager
        ;;
    logs)
        echo "Following logs (Ctrl+C to exit)..."
        sudo journalctl -u minecraft -f
        ;;
    log)
        sudo journalctl -u minecraft -n 50 --no-pager
        ;;
    console)
        if sudo -u "$USER" screen -list | grep -q "minecraft"; then
            echo "Attaching to console... (Ctrl+A then D to detach)"
            sleep 1
            sudo -u "$USER" screen -r minecraft
        else
            echo "Starting console in screen..."
            sudo -u "$USER" screen -S minecraft -d -m bash -c "cd $PROJECT_DIR && ./scripts/start-server.sh"
            sleep 2
            sudo -u "$USER" screen -r minecraft
        fi
        ;;
    backup)
        BACKUP_DIR="/home/$USER/backups"
        mkdir -p "$BACKUP_DIR"
        BACKUP_FILE="$BACKUP_DIR/world-$(date +%Y%m%d-%H%M%S).tar.gz"
        echo "Creating backup: $BACKUP_FILE"
        if sudo systemctl is-active --quiet minecraft; then
            echo "Server is running. Creating live backup..."
        fi
        sudo tar -czf "$BACKUP_FILE" -C "$PROJECT_DIR/server" world/ 2>/dev/null || {
            echo "Warning: Some files may have changed during backup"
        }
        echo "Backup complete: $BACKUP_FILE"
        du -h "$BACKUP_FILE"
        ;;
    fix)
        echo "Killing server processes and fixing locks..."
        sudo systemctl stop minecraft 2>/dev/null || true
        sudo pkill -9 -f "paper-1.21.11.jar" 2>/dev/null || true
        sudo pkill -9 java 2>/dev/null || true
        sleep 2
        sudo rm -f "$PROJECT_DIR/server/world/session.lock" 2>/dev/null || true
        sudo rm -f "$PROJECT_DIR/server/world_nether/session.lock" 2>/dev/null || true
        sudo rm -f "$PROJECT_DIR/server/world_the_end/session.lock" 2>/dev/null || true
        echo "Locks cleared. You can now start the server with: mcserver start"
        ;;
    redeploy)
        echo "=== REDEPLOYING SERVER ==="
        echo "This will stop the server, clean build files, rebuild, and restart."
        read -p "Are you sure? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "Stopping server..."
            sudo systemctl stop minecraft
            sudo pkill -9 java 2>/dev/null || true
            sleep 2
            
            echo "Cleaning and rebuilding..."
            cd "$PROJECT_DIR" || exit 1
            sudo -u "$USER" rm -rf build .gradle 2>/dev/null || true
            sudo -u "$USER" ./gradlew clean setup --no-daemon --console=plain || {
                echo "Build failed!"
                exit 1
            }
            
            echo "Starting server..."
            sudo systemctl start minecraft
            sleep 3
            sudo systemctl status minecraft --no-pager
        else
            echo "Redeploy cancelled"
        fi
        ;;
    update)
        echo "=== QUICK UPDATE (Plugin Only) ==="
        echo "This updates just the plugin JAR and restarts."
        read -p "Continue? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "Stopping server..."
            sudo systemctl stop minecraft
            sleep 2
            
            echo "Rebuilding plugin..."
            cd "$PROJECT_DIR" || exit 1
            sudo -u "$USER" ./gradlew clean jar --no-daemon --console=plain || {
                echo "Build failed!"
                exit 1
            }
            
            echo "Copying new JAR..."
            sudo cp "$PROJECT_DIR/plugins/colosseum-arena-1.0.jar" "$PROJECT_DIR/server/plugins/"
            sudo chown "$USER:$USER" "$PROJECT_DIR/server/plugins/colosseum-arena-1.0.jar"
            
            echo "Starting server..."
            sudo systemctl start minecraft
            echo "Update complete!"
        else
            echo "Update cancelled"
        fi
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
    
    # Create redeploy helper
    cat > "/usr/local/bin/mc-redeploy" << EOF
#!/bin/bash
# Quick redeploy shortcut

if [ ! -f "deploy.sh" ]; then
    echo "Error: deploy.sh not found in current directory"
    echo "Please run this from the project root"
    exit 1
fi

sudo ./deploy.sh --redeploy --ram=$RAM --port=$PORT
EOF
    chmod +x /usr/local/bin/mc-redeploy
    
    log_info "Management commands installed:"
    log_info "  mcserver start|stop|restart|status|logs|backup|fix|redeploy|update"
}

# Step 12: Start server
step12_start_server() {
    log_step "Final Step: Starting Minecraft server..."
    
    # Check if it's a redeploy and server was already running
    if [ "$REDEPLOY" = true ]; then
        log_info "Redeploy complete. Starting server..."
    fi
    
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
    echo "  mcserver redeploy    - Full redeploy"
    echo "  mcserver update      - Quick plugin update"
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
    
    if [ "$REDEPLOY" = true ]; then
        echo -e "${YELLOW}REDEPLOY MODE: Will rebuild and restart server${NC}"
        echo ""
    fi
    
    check_root
    check_os
    
    log_info "Configuration:"
    log_info "  User: $USER"
    log_info "  RAM: $RAM"
    log_info "  Port: $PORT"
    log_info "  Skip Java: $SKIP_JAVA"
    log_info "  Skip Firewall: $SKIP_FIREWALL"
    log_info "  Redeploy Mode: $REDEPLOY"
    echo ""
    
    # Run all steps
    step1_update_system
    step2_install_java
    step3_create_user
    step4_check_project
    step5_setup_project
    step5a_ensure_version_file
    step5b_verify_version
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
    echo "1. Connect to console: mcserver console"
    echo "2. OP yourself: op <your-minecraft-username>"
    echo "3. Start playing!"
    echo ""
    echo "To redeploy after code changes:"
    echo "  cd /home/$USER/colosseum-arena"
    echo "  sudo ./deploy.sh --redeploy"
    echo ""
    echo "Or use: mcserver redeploy"
    echo ""
}

# Run main function
main "$@"
