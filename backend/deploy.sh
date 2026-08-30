#!/usr/bin/env bash

# ==============================================================================
# Auto-Deployment Script for POS Toko Akbar Media Group
# Target: Ubuntu Server 20.04 / 22.04 / 24.04 LTS (Optimized for 2GB RAM VPS)
# ==============================================================================

set -e

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}==============================================================${NC}"
echo -e "${GREEN}  🚀 POS Toko Akbar Media Group - Ubuntu Server Installer   ${NC}"
echo -e "${BLUE}==============================================================${NC}"
echo -e "Tuning environment for 2GB RAM VPS..."

# 1. Ensure script is run with sudo/root
if [ "$EUID" -ne 0 ]; then
  echo -e "${RED}❌ Harap jalankan script ini sebagai root atau dengan sudo:${NC}"
  echo "   sudo bash deploy.sh"
  exit 1
fi

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR"

# 2. Setup 2GB Swap Memory (Prevent OOM on 2GB RAM servers)
echo -e "\n${YELLOW}[1/8] Memeriksa dan mengatur Swap Memory (2GB)...${NC}"
SWAP_EXISTS=$(swapon --show | wc -l)
if [ "$SWAP_EXISTS" -le 1 ]; then
    echo "Membuat file swap 2GB di /swapfile..."
    fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    if ! grep -q "/swapfile" /etc/fstab; then
        echo '/swapfile none swap sw 0 0' >> /etc/fstab
    fi
    sysctl vm.swappiness=10
    echo 'vm.swappiness=10' >> /etc/sysctl.conf
    echo -e "${GREEN}✅ Swap memory 2GB berhasil diaktifkan!${NC}"
else
    echo -e "${GREEN}✅ Swap memory sudah aktif.${NC}"
fi

# 3. Update Package Repositories and Install Core Dependencies
echo -e "\n${YELLOW}[2/8] Mengupdate sistem dan menginstal dependensi...${NC}"
apt-get update -y
apt-get install -y curl wget git ufw nginx mariadb-server software-properties-common

# 4. Install Node.js 20 LTS and PM2
echo -e "\n${YELLOW}[3/8] Menginstal Node.js 20 LTS & PM2...${NC}"
if ! command -v node &> /dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
    apt-get install -y nodejs
fi
echo "Versi Node.js: $(node -v)"
echo "Versi NPM: $(npm -v)"

npm install -g pm2 --silent
echo -e "${GREEN}✅ Node.js & PM2 berhasil dipasang.${NC}"

# 5. Optimize MySQL / MariaDB for 2GB RAM
echo -e "\n${YELLOW}[4/8] Mengonfigurasi MySQL / MariaDB (Low-RAM Optimization)...${NC}"
mkdir -p /etc/mysql/mariadb.conf.d/
cp "$APP_DIR/mysql-low-ram.cnf" /etc/mysql/mariadb.conf.d/99-low-ram.cnf 2>/dev/null || \
cp "$APP_DIR/mysql-low-ram.cnf" /etc/mysql/conf.d/low-ram.cnf

systemctl restart mariadb || systemctl restart mysql
systemctl enable mariadb || systemctl enable mysql
echo -e "${GREEN}✅ Konfigurasi database hemat memori diterapkan.${NC}"

# 6. Setup Database, User, and Schema
echo -e "\n${YELLOW}[5/8] Menginisialisasi Database pos_akbar & Tabel...${NC}"
DB_NAME="pos_akbar"
DB_USER="pos_user"
DB_PASS="pos_secure_password_2026"

mysql -u root <<EOF
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
EOF

# Import schema
mysql -u root ${DB_NAME} < "$APP_DIR/schema.sql"
echo -e "${GREEN}✅ Database & skema tabel berhasil dibuat & diisi data awal!${NC}"

# 7. Configure Environment & NPM Install
echo -e "\n${YELLOW}[6/8] Memasang dependensi Node.js & Konfigurasi .env...${NC}"
if [ ! -f "$APP_DIR/.env" ]; then
    cp "$APP_DIR/.env.example" "$APP_DIR/.env"
    # Update default database credentials in .env
    sed -i "s/DB_USER=.*/DB_USER=${DB_USER}/g" "$APP_DIR/.env"
    sed -i "s/DB_PASSWORD=.*/DB_PASSWORD=${DB_PASS}/g" "$APP_DIR/.env"
    sed -i "s/DB_NAME=.*/DB_NAME=${DB_NAME}/g" "$APP_DIR/.env"
fi

npm install --production --silent

# 8. Start Backend Service with PM2
echo -e "\n${YELLOW}[7/8] Menjalankan Service Backend dengan PM2...${NC}"
pm2 delete pos-akbar-api 2>/dev/null || true
pm2 start ecosystem.config.js
pm2 save
pm2 startup systemd -u root --hp /root || true
echo -e "${GREEN}✅ Backend aktif berjalan di background via PM2.${NC}"

# 9. Configure Nginx Reverse Proxy
echo -e "\n${YELLOW}[8/8] Mengonfigurasi Nginx Web Server...${NC}"
cp "$APP_DIR/nginx-pos.conf" /etc/nginx/sites-available/pos.akbarmediagroup.me
ln -sf /etc/nginx/sites-available/pos.akbarmediagroup.me /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true

nginx -t
systemctl restart nginx
systemctl enable nginx

# Configure Firewall
ufw allow 22/tcp 2>/dev/null || true
ufw allow 80/tcp 2>/dev/null || true
ufw allow 443/tcp 2>/dev/null || true
ufw allow 4750/tcp 2>/dev/null || true
ufw allow 4760/tcp 2>/dev/null || true
ufw --force enable 2>/dev/null || true

# Summary
echo -e "\n${BLUE}==============================================================${NC}"
echo -e "${GREEN}  🎉 DEPLOYMENT BERHASIL SELESAI!                             ${NC}"
echo -e "${BLUE}==============================================================${NC}"
echo -e "📍 Backend Internal  : http://127.0.0.1:4750"
echo -e "🌐 Domain Backend/API: http://pos.akbarmediagroup.me"
echo -e "🌐 Web Toko / Portal : http://toko.akbarmediagroup.me:4760"
echo -e "👤 Akun Login Default: Username: ${YELLOW}akbar${NC} | Password: ${YELLOW}08Delapan${NC}"
echo -e "💾 RAM Usage Total   : ~300MB - 450MB (Aman untuk server 2GB RAM)"
echo -e "\n${YELLOW}Langkah Berikutnya untuk SSL / HTTPS Gratis (Let's Encrypt):${NC}"
echo "   sudo apt-get install -y certbot python3-certbot-nginx"
echo "   sudo certbot --nginx -d pos.akbarmediagroup.me -d toko.akbarmediagroup.me"
echo -e "${BLUE}==============================================================${NC}\n"
