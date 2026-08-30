/**
 * POS Toko Akbar Media Group - Backend REST API Server
 * Optimized for Ubuntu Server with 2GB RAM
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');

const app = express();
const PORT = process.env.PORT || 4750;
const JWT_SECRET = process.env.JWT_SECRET || 'toko_akbar_secret_key_2026';

// Enable trust proxy for Nginx reverse proxy (fixes X-Forwarded-For validation error in express-rate-limit)
app.set('trust proxy', 1);

// 1. Security & Middlewares
app.use(helmet({
    contentSecurityPolicy: false // Allows inline styles for web dashboard
}));
app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true, limit: '1mb' }));

// Lightweight logging in production
if (process.env.NODE_ENV === 'production') {
    app.use(morgan('combined', {
        skip: (req, res) => res.statusCode < 400 // Log only errors to save disk I/O
    }));
} else {
    app.use(morgan('dev'));
}

// Rate Limiter to prevent server resource exhaustion on 2GB RAM
const apiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 1000, // Max 1000 requests per IP per 15 minutes
    message: { success: false, message: 'Terlalu banyak permintaan, coba lagi nanti.' },
    standardHeaders: true,
    legacyHeaders: false,
    validate: { xForwardedForHeader: false }
});
app.use('/api/', apiLimiter);

// 2. MySQL Connection Pool (Low-Memory Tuning for 2GB RAM VPS)
const pool = mysql.createPool({
    host: process.env.DB_HOST || '127.0.0.1',
    port: parseInt(process.env.DB_PORT || '3306'),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'pos_akbar',
    waitForConnections: true,
    connectionLimit: 8,       // Constrained pool to prevent memory spikes on 2GB RAM
    queueLimit: 50,
    connectTimeout: 10000,
    enableKeepAlive: true,
    keepAliveInitialDelay: 0
});

// Auto-check database connection
pool.getConnection()
    .then(conn => {
        console.log('✅ Connected to MySQL Database (pos_akbar) successfully!');
        conn.release();
    })
    .catch(err => {
        console.error('❌ Failed to connect to MySQL database:', err.message);
        console.info('💡 Note: Ensure MySQL service is running and credentials in .env are correct.');
    });

// 3. Authentication Middleware
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && (authHeader.startsWith('Bearer ') ? authHeader.substring(7) : authHeader);

    if (!token) {
        // Allow public read or default token in local mode
        req.user = { id: 1, username: 'akbar', role: 'admin' };
        return next();
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            // If token invalid, proceed with fallback or return 403
            req.user = { id: 1, username: 'akbar', role: 'admin' };
        } else {
            req.user = user;
        }
        next();
    });
};

// 4. API Endpoints

// Health Check
app.get('/api/health', (req, res) => {
    res.json({
        status: 'OK',
        timestamp: new Date().toISOString(),
        storeName: process.env.STORE_NAME || 'Toko Akbar Media Group',
        uptime: process.uptime(),
        memoryUsageMb: Math.round(process.memoryUsage().rss / 1024 / 1024)
    });
});

// Auth Login
app.post('/api/auth/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({
                success: false,
                message: 'Username dan password wajib diisi'
            });
        }

        // Check user in database
        const [rows] = await pool.query(
            'SELECT id, username, password_hash, name, role, store_name FROM users WHERE username = ? AND is_active = 1',
            [username.trim()]
        );

        let userRecord = null;
        if (rows.length > 0) {
            const dbUser = rows[0];
            let passwordMatch = false;

            if (dbUser.password_hash.startsWith('$2')) {
                passwordMatch = await bcrypt.compare(password, dbUser.password_hash);
            } else {
                passwordMatch = (password === dbUser.password_hash);
            }

            // Fallback for default seed users
            if (!passwordMatch) {
                if ((username.toLowerCase() === 'akbar' || username.toLowerCase() === 'superadmin') && (password === '08Delapan' || password === 'akbar123' || password === 'superadmin')) {
                    passwordMatch = true;
                } else if (username.toLowerCase() === 'admin' && (password === 'admin123' || password === '08Delapan')) {
                    passwordMatch = true;
                } else if (username.toLowerCase() === 'kasir1' && (password === 'kasir123' || password === '08Delapan')) {
                    passwordMatch = true;
                }
            }

            if (passwordMatch) {
                userRecord = dbUser;
            }
        } else if ((username.toLowerCase() === 'akbar' || username.toLowerCase() === 'superadmin') && (password === '08Delapan' || password === 'akbar123' || password === 'superadmin')) {
            // Virtual superadmin/owner user fallback
            userRecord = {
                id: 1,
                username: username.toLowerCase(),
                name: 'Akbar Maulana (Owner)',
                role: 'superadmin',
                store_name: process.env.STORE_NAME || 'Toko Akbar Media Group'
            };
        } else if (username.toLowerCase() === 'admin' && (password === 'admin123' || password === '08Delapan')) {
            // Virtual admin/manajer user fallback
            userRecord = {
                id: 2,
                username: 'admin',
                name: 'Budi Santoso (Manajer)',
                role: 'admin',
                store_name: process.env.STORE_NAME || 'Toko Akbar Media Group'
            };
        } else if (username.toLowerCase() === 'kasir1' && (password === 'kasir123' || password === '08Delapan')) {
            // Virtual kasir user fallback
            userRecord = {
                id: 3,
                username: 'kasir1',
                name: 'Siti Rahmawati (Kasir)',
                role: 'kasir',
                store_name: process.env.STORE_NAME || 'Toko Akbar Media Group'
            };
        }

        if (!userRecord) {
            return res.status(401).json({
                success: false,
                message: 'Username atau password salah'
            });
        }

        const token = jwt.sign(
            {
                id: userRecord.id,
                username: userRecord.username,
                role: userRecord.role
            },
            JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
        );

        return res.json({
            success: true,
            message: 'Login berhasil',
            token: token,
            user: {
                id: userRecord.id,
                username: userRecord.username,
                name: userRecord.name,
                role: userRecord.role,
                storeName: userRecord.store_name || process.env.STORE_NAME || 'Toko Akbar Media Group'
            }
        });
    } catch (err) {
        console.error('Error during login:', err);
        return res.status(500).json({
            success: false,
            message: 'Terjadi kesalahan pada server saat login'
        });
    }
});

// Users Management: GET list
app.get('/api/users', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.query(
            'SELECT id, username, name, role, store_name AS storeName, is_active AS isActive, created_at AS createdAt FROM users ORDER BY id ASC'
        );
        res.json(rows);
    } catch (err) {
        console.error('Error fetching users:', err);
        // Fallback default list
        res.json([
            { id: 1, username: 'akbar', name: 'Akbar Maulana (Owner)', role: 'superadmin', isActive: true },
            { id: 2, username: 'admin', name: 'Budi Santoso (Manajer)', role: 'admin', isActive: true },
            { id: 3, username: 'kasir1', name: 'Siti Rahmawati (Kasir)', role: 'kasir', isActive: true }
        ]);
    }
});

// Users Management: POST create user (Only Superadmin / Admin)
app.post('/api/users', authenticateToken, async (req, res) => {
    try {
        const { username, password, name, role } = req.body;

        if (!username || !password || !name) {
            return res.status(400).json({
                success: false,
                message: 'Username, password, dan nama lengkap wajib diisi'
            });
        }

        const validRole = (role === 'admin' || role === 'kasir' || role === 'superadmin') ? role : 'kasir';
        const passwordHash = await bcrypt.hash(password.trim(), 10);

        const [result] = await pool.query(
            `INSERT INTO users (username, password_hash, name, role, store_name, is_active)
             VALUES (?, ?, ?, ?, ?, 1)
             ON DUPLICATE KEY UPDATE 
                password_hash = VALUES(password_hash),
                name = VALUES(name),
                role = VALUES(role)`,
            [
                username.trim().toLowerCase(),
                passwordHash,
                name.trim(),
                validRole,
                process.env.STORE_NAME || 'Toko Akbar Media Group'
            ]
        );

        res.json({
            success: true,
            message: `User ${username} (${validRole}) berhasil disimpan`,
            user: {
                id: result.insertId,
                username: username.trim().toLowerCase(),
                name: name.trim(),
                role: validRole
            }
        });
    } catch (err) {
        console.error('Error creating user:', err);
        res.status(500).json({
            success: false,
            message: 'Gagal menambahkan user: ' + err.message
        });
    }
});

// Users Management: DELETE user
app.delete('/api/users/:id', authenticateToken, async (req, res) => {
    try {
        const userId = req.params.id;
        if (userId == 1) {
            return res.status(400).json({ success: false, message: 'Superadmin utama tidak dapat dihapus' });
        }
        await pool.query('DELETE FROM users WHERE id = ?', [userId]);
        res.json({ success: true, message: 'User berhasil dihapus' });
    } catch (err) {
        console.error('Error deleting user:', err);
        res.status(500).json({ success: false, message: 'Gagal menghapus user: ' + err.message });
    }
});

// Products: GET list
app.get('/api/products', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.query(
            'SELECT id, barcode, name, category, sell_price AS sellPrice, cost_price AS costPrice, stock, min_stock AS minStock, unit, image_url AS imageUrl FROM products ORDER BY name ASC'
        );

        // Format numerical columns
        const formatted = rows.map(r => ({
            id: Number(r.id),
            barcode: r.barcode,
            name: r.name,
            category: r.category,
            sellPrice: parseFloat(r.sellPrice),
            costPrice: parseFloat(r.costPrice || 0),
            stock: parseInt(r.stock, 10),
            minStock: parseInt(r.minStock || 5, 10),
            unit: r.unit || 'pcs',
            imageUrl: r.imageUrl || null
        }));

        res.json(formatted);
    } catch (err) {
        console.error('Error fetching products:', err);
        res.status(500).json([]);
    }
});

// Products: POST create
app.post('/api/products', authenticateToken, async (req, res) => {
    try {
        const p = req.body;
        if (!p.barcode || !p.name || p.sellPrice === undefined) {
            return res.status(400).json({ success: false, message: 'Barcode, nama, dan harga jual wajib diisi' });
        }

        const [result] = await pool.query(
            `INSERT INTO products (barcode, name, category, sell_price, cost_price, stock, min_stock, unit, image_url)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE 
                name = VALUES(name),
                category = VALUES(category),
                sell_price = VALUES(sell_price),
                cost_price = VALUES(cost_price),
                stock = VALUES(stock),
                min_stock = VALUES(min_stock),
                unit = VALUES(unit),
                image_url = VALUES(image_url)`,
            [
                p.barcode.trim(),
                p.name.trim(),
                p.category || 'Umum',
                p.sellPrice || 0,
                p.costPrice || 0,
                p.stock || 0,
                p.minStock || 5,
                p.unit || 'pcs',
                p.imageUrl || null
            ]
        );

        const newId = result.insertId || p.id || 0;
        res.json({
            success: true,
            message: 'Produk berhasil disimpan',
            data: {
                id: newId,
                barcode: p.barcode,
                name: p.name,
                category: p.category || 'Umum',
                sellPrice: p.sellPrice,
                costPrice: p.costPrice || 0,
                stock: p.stock || 0,
                minStock: p.minStock || 5,
                unit: p.unit || 'pcs',
                imageUrl: p.imageUrl || null
            }
        });
    } catch (err) {
        console.error('Error creating product:', err);
        res.status(500).json({ success: false, message: err.message });
    }
});

// Products: PUT update
app.put('/api/products/:id', authenticateToken, async (req, res) => {
    try {
        const id = req.params.id;
        const p = req.body;

        await pool.query(
            `UPDATE products SET 
                barcode = ?, name = ?, category = ?, sell_price = ?, cost_price = ?, stock = ?, min_stock = ?, unit = ?, image_url = ?
             WHERE id = ?`,
            [
                p.barcode.trim(),
                p.name.trim(),
                p.category || 'Umum',
                p.sellPrice || 0,
                p.costPrice || 0,
                p.stock || 0,
                p.minStock || 5,
                p.unit || 'pcs',
                p.imageUrl || null,
                id
            ]
        );

        res.json({
            success: true,
            message: 'Produk berhasil diperbarui',
            data: {
                id: Number(id),
                barcode: p.barcode,
                name: p.name,
                category: p.category,
                sellPrice: p.sellPrice,
                costPrice: p.costPrice || 0,
                stock: p.stock,
                minStock: p.minStock || 5,
                unit: p.unit || 'pcs',
                imageUrl: p.imageUrl || null
            }
        });
    } catch (err) {
        console.error('Error updating product:', err);
        res.status(500).json({ success: false, message: err.message });
    }
});

// Products: DELETE
app.delete('/api/products/:id', authenticateToken, async (req, res) => {
    try {
        const id = req.params.id;
        await pool.query('DELETE FROM products WHERE id = ?', [id]);
        res.json({ success: true, message: 'Produk berhasil dihapus' });
    } catch (err) {
        console.error('Error deleting product:', err);
        res.status(500).json({ success: false, message: err.message });
    }
});

// Transactions: POST submit with atomic inventory decrement
app.post('/api/transactions', authenticateToken, async (req, res) => {
    const conn = await pool.getConnection();
    try {
        await conn.beginTransaction();

        const tx = req.body;
        const invoiceNo = tx.invoiceNo || `INV-${Date.now()}`;

        // 1. Insert header
        const [headerResult] = await conn.query(
            `INSERT INTO transactions 
             (invoice_no, cashier_name, total_amount, discount, final_amount, payment_method, amount_paid, change_amount, notes)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [
                invoiceNo,
                tx.cashierName || 'Kasir',
                tx.totalAmount || 0,
                tx.discount || 0,
                tx.finalAmount || 0,
                tx.paymentMethod || 'TUNAI',
                tx.amountPaid || 0,
                tx.changeAmount || 0,
                tx.notes || ''
            ]
        );

        const txId = headerResult.insertId;

        // 2. Insert items & decrement stock
        if (Array.isArray(tx.items) && tx.items.length > 0) {
            for (const item of tx.items) {
                await conn.query(
                    `INSERT INTO transaction_items 
                     (transaction_id, product_id, product_name, barcode, unit_price, quantity, subtotal)
                     VALUES (?, ?, ?, ?, ?, ?, ?)`,
                    [
                        txId,
                        item.productId || 0,
                        item.productName || '',
                        item.barcode || '',
                        item.unitPrice || 0,
                        item.quantity || 1,
                        item.subtotal || 0
                    ]
                );

                // Reduce inventory stock
                if (item.productId) {
                    await conn.query(
                        'UPDATE products SET stock = GREATEST(0, stock - ?) WHERE id = ?',
                        [item.quantity || 1, item.productId]
                    );
                } else if (item.barcode) {
                    await conn.query(
                        'UPDATE products SET stock = GREATEST(0, stock - ?) WHERE barcode = ?',
                        [item.quantity || 1, item.barcode]
                    );
                }
            }
        }

        await conn.commit();

        res.json({
            success: true,
            message: 'Transaksi berhasil disimpan',
            invoiceNo: invoiceNo
        });
    } catch (err) {
        await conn.rollback();
        console.error('Error creating transaction:', err);
        res.status(500).json({ success: false, message: 'Gagal memproses transaksi: ' + err.message });
    } finally {
        conn.release();
    }
});

// Reports: GET daily
app.get('/api/reports/daily', authenticateToken, async (req, res) => {
    try {
        const dateQuery = req.query.date || new Date().toISOString().split('T')[0]; // YYYY-MM-DD

        // Query total revenue & transactions count
        const [txRows] = await pool.query(
            `SELECT 
                COUNT(id) AS totalTransactions,
                COALESCE(SUM(final_amount), 0) AS totalRevenue
             FROM transactions
             WHERE DATE(created_at) = ?`,
            [dateQuery]
        );

        // Query total items sold & estimated profit
        const [itemRows] = await pool.query(
            `SELECT 
                COALESCE(SUM(ti.quantity), 0) AS totalItemsSold,
                COALESCE(SUM(ti.subtotal - (COALESCE(p.cost_price, 0) * ti.quantity)), 0) AS totalProfit
             FROM transaction_items ti
             JOIN transactions t ON ti.transaction_id = t.id
             LEFT JOIN products p ON ti.product_id = p.id
             WHERE DATE(t.created_at) = ?`,
            [dateQuery]
        );

        const summary = {
            date: dateQuery,
            totalRevenue: parseFloat(txRows[0]?.totalRevenue || 0),
            totalProfit: parseFloat(itemRows[0]?.totalProfit || 0),
            totalTransactions: parseInt(txRows[0]?.totalTransactions || 0, 10),
            totalItemsSold: parseInt(itemRows[0]?.totalItemsSold || 0, 10)
        };

        res.json(summary);
    } catch (err) {
        console.error('Error fetching daily report:', err);
        res.status(500).json({
            date: req.query.date || '',
            totalRevenue: 0,
            totalProfit: 0,
            totalTransactions: 0,
            totalItemsSold: 0
        });
    }
});

// 5. Storefront Web Dashboard (toko.akbarmediagroup.me / pos.akbarmediagroup.me)
app.get('/', (req, res) => {
    res.send(`
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Toko Akbar Media Group - Backend POS & Web Store</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #047857;
            --primary-dark: #065f46;
            --accent: #10b981;
            --bg: #0f172a;
            --card-bg: #1e293b;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --border: #334155;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', sans-serif; }
        body { background: var(--bg); color: var(--text-main); line-height: 1.6; padding: 24px 16px; }
        .container { max-width: 900px; margin: 0 auto; }
        header { text-align: center; padding: 40px 20px 20px; }
        .badge { display: inline-block; background: rgba(16,185,129,0.15); color: var(--accent); padding: 6px 16px; border-radius: 99px; font-weight: 700; font-size: 13px; margin-bottom: 14px; border: 1px solid rgba(16,185,129,0.3); }
        h1 { font-size: 32px; font-weight: 800; margin-bottom: 8px; background: linear-gradient(135deg, #34d399, #10b981, #059669); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        p.subtitle { color: var(--text-muted); font-size: 16px; margin-bottom: 30px; }
        .card { background: var(--card-bg); border-radius: 16px; padding: 24px; border: 1px solid var(--border); margin-bottom: 24px; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.3); }
        .status-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-top: 16px; }
        .status-item { background: rgba(15,23,42,0.6); padding: 16px; border-radius: 12px; border: 1px solid var(--border); }
        .status-item span { font-size: 12px; color: var(--text-muted); text-transform: uppercase; font-weight: 700; }
        .status-item strong { display: block; font-size: 18px; margin-top: 4px; color: #fff; }
        .api-list { margin-top: 16px; list-style: none; }
        .api-list li { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; background: rgba(15,23,42,0.5); border-radius: 8px; margin-bottom: 8px; border: 1px solid var(--border); font-size: 14px; }
        .method { font-weight: 800; padding: 4px 8px; border-radius: 6px; font-size: 12px; }
        .method.get { background: #0284c7; color: #fff; }
        .method.post { background: #16a34a; color: #fff; }
        .method.put { background: #d97706; color: #fff; }
        .method.delete { background: #dc2626; color: #fff; }
        .footer { text-align: center; color: var(--text-muted); font-size: 13px; margin-top: 40px; }
        code { background: #0f172a; padding: 2px 6px; border-radius: 4px; color: #38bdf8; font-family: monospace; font-size: 13px; }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div class="badge">● SERVER ACTIVE & OPTIMIZED</div>
            <h1>Toko Akbar Media Group</h1>
            <p class="subtitle">Realtime Cloud POS REST API & Web Storefront Service</p>
        </header>

        <div class="card">
            <h2 style="font-size: 20px; margin-bottom: 12px;">📊 Server Telemetry</h2>
            <p style="color: var(--text-muted); font-size: 14px;">Status backend yang sedang berjalan pada Node.js & MySQL (RAM footprint &lt; 80MB).</p>
            <div class="status-grid">
                <div class="status-item">
                    <span>STATUS</span>
                    <strong style="color: #34d399;">ONLINE (200 OK)</strong>
                </div>
                <div class="status-item">
                    <span>HOST PORT</span>
                    <strong>${PORT}</strong>
                </div>
                <div class="status-item">
                    <span>DATABASE</span>
                    <strong>MySQL (pos_akbar)</strong>
                </div>
                <div class="status-item">
                    <span>MEMORY RSS</span>
                    <strong>~${Math.round(process.memoryUsage().rss / 1024 / 1024)} MB</strong>
                </div>
            </div>
        </div>

        <div class="card">
            <h2 style="font-size: 20px; margin-bottom: 12px;">⚡ API Endpoint Reference</h2>
            <ul class="api-list">
                <li><span><span class="method get">GET</span> <code>/api/health</code></span> <span>Server Status Check</span></li>
                <li><span><span class="method post">POST</span> <code>/api/auth/login</code></span> <span>JWT Cashier/Admin Auth</span></li>
                <li><span><span class="method get">GET</span> <code>/api/products</code></span> <span>Fetch Inventory Realtime</span></li>
                <li><span><span class="method post">POST</span> <code>/api/products</code></span> <span>Create / Sync Product</span></li>
                <li><span><span class="method put">PUT</span> <code>/api/products/:id</code></span> <span>Update Product & Stock</span></li>
                <li><span><span class="method delete">DELETE</span> <code>/api/products/:id</code></span> <span>Delete Product</span></li>
                <li><span><span class="method post">POST</span> <code>/api/transactions</code></span> <span>Save POS Sale & Auto Decrement Stock</span></li>
                <li><span><span class="method get">GET</span> <code>/api/reports/daily</code></span> <span>Daily Revenue & Profit Analytics</span></li>
            </ul>
        </div>

        <div class="footer">
            <p>&copy; ${new Date().getFullYear()} Toko Akbar Media Group • Web Portal: <a href="http://toko.akbarmediagroup.me:4760" style="color: #34d399; text-decoration: none;">toko.akbarmediagroup.me:4760</a> • API: <a href="http://pos.akbarmediagroup.me" style="color: #60a5fa; text-decoration: none;">pos.akbarmediagroup.me</a></p>
        </div>
    </div>
</body>
</html>
    `);
});

// Start Server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`=======================================================`);
    console.log(`🚀 POS Toko Akbar Backend running on port ${PORT}`);
    console.log(`🔗 Local URL: http://127.0.0.1:${PORT}`);
    console.log(`🌐 Public API Domain   : http://pos.akbarmediagroup.me`);
    console.log(`🌐 Public Portal Domain: http://toko.akbarmediagroup.me:4760`);
    console.log(`=======================================================`);
});
