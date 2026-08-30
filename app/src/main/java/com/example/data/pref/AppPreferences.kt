package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pos_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_STORE_NAME = "store_name"
        private const val KEY_STORE_ADDRESS = "store_address"
        private const val KEY_STORE_PHONE = "store_phone"
        private const val KEY_RECEIPT_FOOTER = "receipt_footer"
        private const val KEY_PAPER_SIZE = "paper_size" // "58mm" or "80mm"
        private const val KEY_AUTO_PRINT = "auto_print"
        private const val KEY_BT_PRINTER_MAC = "bt_printer_mac"
        private const val KEY_BT_PRINTER_NAME = "bt_printer_name"
    }

    var jwtToken: String?
        get() = prefs.getString(KEY_JWT_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_JWT_TOKEN, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "akbar") ?: "akbar"
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Akbar Maulana (Owner)") ?: "Akbar Maulana (Owner)"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "superadmin") ?: "superadmin"
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    val isSuperAdmin: Boolean
        get() = userRole == "superadmin" || userRole == "owner"

    val isAdmin: Boolean
        get() = isSuperAdmin || userRole == "admin"

    val isKasir: Boolean
        get() = userRole == "kasir"

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "http://pos.akbarmediagroup.me") ?: "http://pos.akbarmediagroup.me"
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var storeName: String
        get() = prefs.getString(KEY_STORE_NAME, "TOKO AKBAR MEDIA GROUP") ?: "TOKO AKBAR MEDIA GROUP"
        set(value) = prefs.edit().putString(KEY_STORE_NAME, value).apply()

    var storeAddress: String
        get() = prefs.getString(KEY_STORE_ADDRESS, "Jl. Akbar Media No. 8, Indonesia") ?: "Jl. Akbar Media No. 8, Indonesia"
        set(value) = prefs.edit().putString(KEY_STORE_ADDRESS, value).apply()

    var storePhone: String
        get() = prefs.getString(KEY_STORE_PHONE, "0812-3456-7890") ?: "0812-3456-7890"
        set(value) = prefs.edit().putString(KEY_STORE_PHONE, value).apply()

    var receiptFooter: String
        get() = prefs.getString(KEY_RECEIPT_FOOTER, "Terima kasih atas kunjungan Anda!\nBarang yang dibeli tidak dapat ditukar.") ?: "Terima kasih atas kunjungan Anda!"
        set(value) = prefs.edit().putString(KEY_RECEIPT_FOOTER, value).apply()

    var paperSize: String
        get() = prefs.getString(KEY_PAPER_SIZE, "58mm") ?: "58mm"
        set(value) = prefs.edit().putString(KEY_PAPER_SIZE, value).apply()

    var autoPrint: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PRINT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PRINT, value).apply()

    var bluetoothPrinterMac: String?
        get() = prefs.getString(KEY_BT_PRINTER_MAC, null)
        set(value) = prefs.edit().putString(KEY_BT_PRINTER_MAC, value).apply()

    var bluetoothPrinterName: String?
        get() = prefs.getString(KEY_BT_PRINTER_NAME, null)
        set(value) = prefs.edit().putString(KEY_BT_PRINTER_NAME, value).apply()

    fun logout() {
        prefs.edit()
            .remove(KEY_JWT_TOKEN)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}
