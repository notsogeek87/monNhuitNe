package com.monnhuitne.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

data class N8nCredentials(val baseUrl: String, val apiKey: String)

/**
 * Contrairement à la PWA (navigateur sans coffre-fort matériel, d'où le
 * chiffrement PBKDF2+AES-GCM dérivé du PIN dans app/src/lib/storage), une app
 * Android a déjà un stockage chiffré adossé au Keystore matériel via
 * EncryptedSharedPreferences. Le PIN ici ne sert donc plus de clé de
 * chiffrement : c'est un simple verrou d'écran (hash comparé), plus simple et
 * tout aussi sûr sur cette plateforme.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "monnhuitne_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasCredentials(): Boolean = prefs.contains(KEY_BASE_URL) && prefs.contains(KEY_PIN_HASH)

    fun loadCredentials(): N8nCredentials? {
        val baseUrl = prefs.getString(KEY_BASE_URL, null) ?: return null
        val apiKey = prefs.getString(KEY_API_KEY, null) ?: return null
        return N8nCredentials(baseUrl, apiKey)
    }

    fun save(baseUrl: String, apiKey: String, pin: String) {
        prefs.edit()
            .putString(KEY_BASE_URL, baseUrl.trimEnd('/'))
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_PIN_HASH, hashPin(pin))
            .apply()
    }

    fun verifyPin(pin: String): Boolean = prefs.getString(KEY_PIN_HASH, null) == hashPin(pin)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("monnhuitne-pin:$pin".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val KEY_BASE_URL = "n8n_base_url"
        const val KEY_API_KEY = "n8n_api_key"
        const val KEY_PIN_HASH = "pin_hash"
    }
}
