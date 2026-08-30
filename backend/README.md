# 🚀 Backend POS Toko Akbar Media Group (Deployment Guide - Ubuntu 2GB RAM)

Paket backend ini dirancang khusus dengan konsumsi memori sangat rendah (**RAM total < 450 MB**), sehingga sangat stabil, cepat, dan anti-crash untuk VPS Ubuntu dengan spesifikasi **RAM 2 GB**.

---

## 📁 Struktur File Pendukung Backend

| File | Keterangan |
| :--- | :--- |
| **`server.js`** | Express REST API server lengkap dengan endpoint Auth JWT, CRUD Produk, Transaksi POS (auto cut stock), Laporan Harian, & Landing Page Toko. |
| **`schema.sql`** | Database schema MySQL & data awal produk sembako, makanan, minuman, dan akun kasir. |
| **`ecosystem.config.js`** | Konfigurasi PM2 Process Manager dengan memory auto-restart limit & V8 engine tuning. |
| **`mysql-low-ram.cnf`** | Optimasi MySQL/MariaDB agar buffer pool dan memory footprint hemat RAM (< 300MB). |
| **`nginx-pos.conf`** | Konfigurasi Nginx reverse proxy untuk domain `pos.akbarmediagroup.me` & `toko.akbarmediagroup.me`. |
| **`deploy.sh`** | **Script otomatis 1-Klik**: otomatis setup Swap 2GB, pasang Node.js, MariaDB/MySQL, PM2, Nginx, firewall, dan migrasi database. |
| **`docker-compose.yml`** | Konfigurasi Docker alternatif jika ingin menggunakan container dengan batasan limit memori. |

---

## ⚡ Cara Deploy Cepat di Ubuntu (Metode 1-Klik)

### 1. Upload Folder `backend` ke Server VPS Anda
Bisa menggunakan `scp`, `rsync`, FileZilla, atau clone via Git:
```bash
# Contoh upload dari komputer lokal:
scp -r ./backend root@IP_SERVER_ANDA:/var/www/pos-akbar
```

### 2. Masuk ke Server Ubuntu dan Jalankan `deploy.sh`
```bash
ssh root@IP_SERVER_ANDA
cd /var/www/pos-akbar

# Berikan izin eksekusi dan jalankan script:
chmod +x deploy.sh
sudo ./deploy.sh
```

Script akan secara otomatis:
1. Menambahkan **Swap Memory 2GB** di `/swapfile` untuk mencegah Out-Of-Memory (OOM).
2. Memasang **Node.js 20 LTS**, **MariaDB Server**, **PM2**, dan **Nginx**.
3. Menerapkan tuning MySQL hemat memori (`innodb_buffer_pool_size = 256M`, `performance_schema = OFF`).
4. Membuat database `pos_akbar` dan mengimpor tabel serta data awal.
5. Memasang dependensi Node.js (`npm install --production`).
6. Menjalankan backend dengan PM2 dan mengaturnya agar **otomatis aktif kembali jika server restart**.
7. Menghubungkan Nginx reverse proxy ke port 4750 (API) & 4760 (Web Portal) serta mengaktifkan UFW Firewall (port 22, 80, 443, 4750, 4760).

---

## 🔒 Pasang SSL HTTPS Gratis (Let's Encrypt)

Setelah domain `pos.akbarmediagroup.me` dan `toko.akbarmediagroup.me` diarahkan (A-Record DNS) ke IP server Anda, pasang sertifikat SSL gratis dengan Certbot:

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d pos.akbarmediagroup.me -d toko.akbarmediagroup.me
```

---

## 🛠️ Perintah Berguna untuk Manajemen Server

### Cek Status & Log Backend (PM2):
```bash
pm2 status                  # Melihat status service
pm2 logs pos-akbar-api      # Melihat log realtime aplikasi
pm2 restart pos-akbar-api   # Me-restart backend
```

### Cek Pemakaian Memori RAM VPS:
```bash
free -h                     # Cek sisa RAM & Swap
htop                        # Monitor CPU & RAM interaktif
```

### Cek Status Nginx:
```bash
sudo systemctl status nginx
sudo nginx -t               # Test validasi file config
sudo systemctl reload nginx
```

---

## 🔑 Kredensial & Endpoint Default

- **URL API / POS Backend**: `http://pos.akbarmediagroup.me` (Port 80/HTTP standar) atau `http://IP_SERVER`
- **URL Web Portal Toko**: `http://toko.akbarmediagroup.me:4760` atau `http://IP_SERVER:4760`
- **Username Default**: `akbar`
- **Password Default**: `08Delapan`
- **Role**: `admin`
