package com.example.reservas.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecurityManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"

    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                prefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context.applicationContext, // Use application context for singleton
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.e("SecurityManager", "Error creating EncryptedSharedPreferences, falling back to clear text", e)
                // Fallback to normal prefs if encryption fails (better than crashing)
                prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
        return prefs!!
    }

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return try {
            getPrefs(context).getString(KEY_TOKEN, null)
        } catch (e: Exception) {
            Log.e("SecurityManager", "Error getting token", e)
            null
        }
    }

    fun saveUserId(context: Context, userId: Int) {
        getPrefs(context).edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): Int {
        return getPrefs(context).getInt(KEY_USER_ID, -1)
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
