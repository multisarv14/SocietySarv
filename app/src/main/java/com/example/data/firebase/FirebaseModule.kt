package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Single entry point to the Cloud Firestore backend used by SocietySarv.
 *
 * Project  : multisarv-mobileapps
 * Database : societysarv-app-db  (a named database, NOT "(default)" -- the
 *            default database belongs to another app in the same project)
 */
object FirebaseModule {

    const val DATABASE_ID = "societysarv-app-db"
    private const val TAG = "SocietySarvFirebase"

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    fun firestore(context: Context): FirebaseFirestore {
        return firestoreInstance ?: synchronized(this) {
            firestoreInstance ?: create(context).also { firestoreInstance = it }
        }
    }

    private fun create(context: Context): FirebaseFirestore {
        val app = FirebaseApp.getInstance()
            ?: FirebaseApp.initializeApp(context.applicationContext)
            ?: error("Firebase could not be initialised - is google-services.json present?")

        return FirebaseFirestore.getInstance(app, DATABASE_ID).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                // Offline persistence keeps the society data readable without a
                // network connection and re-syncs writes when it comes back.
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
        }
    }

    /**
     * Signs the device in anonymously so that Firestore rules can require an
     * authenticated identity. The app's own login stays exactly as it is - this
     * only gives the SDK a token to talk to the backend with, and is a no-op if
     * the Anonymous provider is not enabled yet.
     */
    fun ensureSignedIn(onDone: (Boolean) -> Unit = {}) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onDone(true)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.i(TAG, "Anonymous sign-in ok: ${it.user?.uid}")
                onDone(true)
            }
            .addOnFailureListener { e ->
                // ADMIN_ONLY_OPERATION => the Anonymous provider is still off in
                // the console. Firestore rules currently allow unauthenticated
                // access, so the app keeps working either way.
                Log.w(TAG, "Anonymous sign-in unavailable: ${e.message}")
                onDone(false)
            }
    }
}

/** Firestore collection names - these mirror the old Room table names 1:1. */
object Collections {
    const val SOCIETY_INFO = "society_info"
    const val RESIDENTS = "residents"
    const val MAINTENANCE_BILLS = "maintenance_bills"
    const val EXPENSE_RECORDS = "expense_records"
    const val NON_FEE_REVENUES = "non_fee_revenues"
    const val VISITOR_LOGS = "visitor_logs"
    const val NOTICES = "notices"
    const val COMPLAINTS = "complaints"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val COUNTERS = "counters"

    /** The society master configuration lives in a single well-known document. */
    const val SOCIETY_DOC = "main"
}
