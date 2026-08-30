-- Database Schema for POS Toko Akbar Media Group
-- Optimized for MySQL 8.0+ / MariaDB 10.6+ on Ubuntu 2GB RAM

CREATE DATABASE IF NOT EXISTS pos_akbar CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pos_akbar;

-- 1. Users Table (Cashiers, Admins, Superadmins/Owners)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role ENUM('superadmin', 'admin', 'kasir', 'owner') NOT NULL DEFAULT 'kasir',
    store_name VARCHAR(100) DEFAULT 'Toko Akbar Media Group',
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_username (username)
) ENGINE=InnoDB;

-- 2. Products Table (Inventory)
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    barcode VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'Umum',
    sell_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    cost_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    min_stock INT NOT NULL DEFAULT 5,
    unit VARCHAR(20) NOT NULL DEFAULT 'pcs',
    image_url VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_barcode (barcode),
    INDEX idx_product_category (category)
) ENGINE=InnoDB;

-- 3. Transactions Table (Header)
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(50) NOT NULL UNIQUE,
    cashier_name VARCHAR(100) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    final_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'TUNAI',
    amount_paid DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    change_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tx_invoice (invoice_no),
    INDEX idx_tx_created_at (created_at)
) ENGINE=InnoDB;

-- 4. Transaction Items Table (Details)
CREATE TABLE IF NOT EXISTS transaction_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    barcode VARCHAR(64) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    cost_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    quantity INT NOT NULL DEFAULT 1,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item_tx (transaction_id),
    INDEX idx_item_product (product_id),
    CONSTRAINT fk_item_tx FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Initial Seed Data: Default Users
-- Default Password for user 'akbar' & 'superadmin' is '08Delapan'
INSERT INTO users (username, password_hash, name, role, store_name)
VALUES 
('akbar', '$2a$10$tZlhR9g38lU8wZ4bJ9vPve8Fv9s7iX6u1D4Pz8eO7qZ0aX3s7mQea', 'Akbar Maulana (Owner)', 'superadmin', 'Toko Akbar Media Group'),
('superadmin', '$2a$10$tZlhR9g38lU8wZ4bJ9vPve8Fv9s7iX6u1D4Pz8eO7qZ0aX3s7mQea', 'Super Administrator', 'superadmin', 'Toko Akbar Media Group'),
('admin', '$2a$10$tZlhR9g38lU8wZ4bJ9vPve8Fv9s7iX6u1D4Pz8eO7qZ0aX3s7mQea', 'Budi Santoso (Manajer)', 'admin', 'Toko Akbar Media Group'),
('kasir1', '$2a$10$tZlhR9g38lU8wZ4bJ9vPve8Fv9s7iX6u1D4Pz8eO7qZ0aX3s7mQea', 'Siti Rahmawati (Kasir)', 'kasir', 'Toko Akbar Media Group')
ON DUPLICATE KEY UPDATE name=VALUES(name), role=VALUES(role);

-- Initial Seed Data: Products
INSERT INTO products (barcode, name, category, sell_price, cost_price, stock, min_stock, unit)
VALUES
('899999900101', 'Beras Raja Platinum 5kg', 'Sembako', 68000, 62000, 45, 10, 'sak'),
('899999900102', 'Minyak Goreng Bimoli 2L', 'Sembako', 34000, 30500, 30, 8, 'pouch'),
('899999900103', 'Gula Pasir Gulaku 1kg', 'Sembako', 18500, 16000, 50, 15, 'bks'),
('899999900104', 'Tepung Terigu Segitiga Biru 1kg', 'Sembako', 13000, 11000, 40, 10, 'bks'),
('899999900105', 'Telur Ayam Negeri 1kg', 'Sembako', 28000, 25000, 25, 5, 'kg'),
('899999900201', 'Indomie Goreng Original 85g', 'Makanan', 3500, 2900, 120, 24, 'bks'),
('899999900202', 'Indomie Kuah Soto Mie 75g', 'Makanan', 3500, 2900, 100, 24, 'bks'),
('899999900203', 'Biskuit Roma Kelapa 300g', 'Makanan', 11500, 9500, 35, 10, 'bks'),
('899999900204', 'Oreo Vanilla 133g', 'Makanan', 9500, 7800, 40, 10, 'bks'),
('899999900301', 'Teh Botol Sosro Kotak 250ml', 'Minuman', 4000, 3100, 60, 12, 'kotak'),
('899999900302', 'Le Minerale 600ml', 'Minuman', 3500, 2500, 80, 20, 'btl'),
('899999900303', 'Kopi Kapal Api Special Mix (10x24g)', 'Minuman', 14000, 11800, 45, 10, 'renceng'),
('899999900304', 'Susu Ultra Milk Cokelat 250ml', 'Minuman', 6500, 5300, 50, 15, 'kotak'),
('899999900401', 'Sabun Mandi Lifebuoy Red 85g', 'Kebersihan', 4500, 3600, 40, 10, 'pcs'),
('899999900402', 'Sampo Pantene Anti Dandruff 160ml', 'Kebersihan', 26000, 22000, 20, 5, 'btl'),
('899999900403', 'Deterjen Rinso Molto 770g', 'Kebersihan', 21000, 17800, 30, 8, 'bks'),
('899999900404', 'Pasta Gigi Pepsodent 190g', 'Kebersihan', 16500, 13800, 25, 8, 'pcs')
ON DUPLICATE KEY UPDATE sell_price=VALUES(sell_price), stock=VALUES(stock);
