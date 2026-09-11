package com.example.data.repository

import android.content.Context

/**
 * Remembers who was signed in so the app does not ask for a society code, unit
 * number and PIN every single time it is opened.
 *
 * Only the society code and the login identifier are kept - never the PIN. On
 * the next launch the member is looked up in Firestore again, so a member who
 * was removed, deactivated or had their PIN reset does not stay signed in on a
 * stale device, and a stolen device yields no credential to reuse elsewhere.
 */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** A remembered session, or null when nobody chose to stay signed in. */
    data class Saved(val societyCode: String, val identifier: String)

    fun save(societyCode: String, identifier: String) {
        prefs.edit()
            .putString(KEY_SOCIETY, societyCode.trim().uppercase())
            .putString(KEY_IDENTIFIER, identifier.trim())
            .apply()
    }

    fun read(): Saved? {
        val society = prefs.getString(KEY_SOCIETY, null)?.takeIf { it.isNotBlank() } ?: return null
        val identifier = prefs.getString(KEY_IDENTIFIER, null)?.takeIf { it.isNotBlank() } ?: return null
        return Saved(society, identifier)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE = "societysarv_session"
        const val KEY_SOCIETY = "society_code"
        const val KEY_IDENTIFIER = "login_identifier"
    }
}
