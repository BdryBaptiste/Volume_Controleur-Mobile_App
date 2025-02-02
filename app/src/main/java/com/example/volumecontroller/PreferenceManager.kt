package com.example.volumecontroller

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SERVER_ADDRESS = "server_address"

    fun getServerAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_ADDRESS, "http://192.168.1.1:5000/") ?: "http://192.168.1.1:5000/"
    }

    fun setServerAddress(context: Context, address: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_ADDRESS, address).apply()
    }
}
