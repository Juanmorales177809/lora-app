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

    class SecureStorageException(
        message: String,
        cause: Throwable? = null
    ) : IllegalStateException(message, cause)

    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        prefs?.let { return it }

        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { prefs = it }
        } catch (e: Exception) {
            Log.e("SecurityManager", "Unable to initialize encrypted token storage", e)
            throw SecureStorageException("Secure token storage is unavailable", e)
        }
    }

    fun saveToken(context: Context, token: String) {
        val saved = getPrefs(context).edit()
            .putString(KEY_TOKEN, token)
            .commit()

        if (!saved) {
            throw SecureStorageException("Unable to write secure token storage")
        }
    }

    fun getToken(context: Context): String? {
        return try {
            getPrefs(context).getString(KEY_TOKEN, null)
        } catch (e: SecureStorageException) {
            Log.e("SecurityManager", "Unable to read secure token storage", e)
            null
        } catch (e: Exception) {
            Log.e("SecurityManager", "Error getting token", e)
            null
        }
    }

    fun saveUserId(context: Context, userId: Int) {
        val saved = getPrefs(context).edit()
            .putInt(KEY_USER_ID, userId)
            .commit()

        if (!saved) {
            throw SecureStorageException("Unable to write secure user storage")
        }
    }

    fun saveSession(context: Context, token: String, userId: Int) {
        val saved = getPrefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .commit()

        if (!saved) {
            clear(context)
            throw SecureStorageException("Unable to write secure session storage")
        }
    }

    fun getUserId(context: Context): Int? {
        return try {
            getPrefs(context).getInt(KEY_USER_ID, -1).takeIf { it != -1 }
        } catch (e: SecureStorageException) {
            Log.e("SecurityManager", "Unable to read secure user storage", e)
            null
        } catch (e: Exception) {
            Log.e("SecurityManager", "Error getting user id", e)
            null
        }
    }

    fun clear(context: Context): Boolean {
        val appContext = context.applicationContext
        var cleared = false

        try {
            cleared = prefs?.edit()?.clear()?.commit()
                ?: getPrefs(appContext).edit().clear().commit()
        } catch (e: Exception) {
            Log.e("SecurityManager", "Unable to clear encrypted token storage", e)
        } finally {
            prefs = null
        }

        return cleared || appContext.deleteSharedPreferences(PREFS_NAME)
    }
}
