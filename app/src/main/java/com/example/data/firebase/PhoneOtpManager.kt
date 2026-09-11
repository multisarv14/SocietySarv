package com.example.data.firebase

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/**
 * Firebase phone-number OTP, used by the "Forgot PIN" flow on the login screen.
 *
 * The member proves they still hold the mobile number registered against their
 * flat, and only then are they allowed to set a new login PIN. The Firebase
 * sign-in that the credential produces is incidental - all this class reports
 * back is whether the code was genuine.
 *
 * Note for whoever runs the Firebase console: the Phone provider has to be
 * enabled under Authentication -> Sign-in method, and the app's SHA-1/SHA-256
 * fingerprints registered, or [sendOtp] comes back with a configuration error.
 */
object PhoneOtpManager {

    private const val TAG = "SocietySarvOtp"
    private const val TIMEOUT_SECONDS = 60L

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    /** True once a code has been sent and is waiting to be entered. */
    val isAwaitingCode: Boolean get() = verificationId != null

    /**
     * Normalises a 10-digit Indian mobile number into the E.164 form Firebase
     * expects. A number the member already stored with a country code is left
     * alone.
     */
    fun toE164(rawNumber: String, defaultCountryCode: String = "+91"): String {
        val digitsOnly = rawNumber.filter { it.isDigit() || it == '+' }
        return when {
            digitsOnly.startsWith("+") -> digitsOnly
            digitsOnly.length > 10 -> "+$digitsOnly"
            else -> "$defaultCountryCode$digitsOnly"
        }
    }

    fun reset() {
        verificationId = null
        resendToken = null
    }

    /**
     * Sends an OTP to [phoneNumber].
     *
     * [onAutoVerified] fires when Android reads the SMS itself and no code ever
     * has to be typed; [onCodeSent] fires when the member does have to type it.
     */
    fun sendOtp(
        activity: Activity,
        phoneNumber: String,
        resend: Boolean = false,
        onCodeSent: () -> Unit,
        onAutoVerified: () -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Instant / auto-retrieval: the device recognised the SMS itself.
                Log.i(TAG, "OTP auto-verified")
                onAutoVerified()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "OTP send failed", e)
                onError(describe(e))
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                resendToken = token
                onCodeSent()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(toE164(phoneNumber))
            .setTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .apply { if (resend) resendToken?.let { setForceResendingToken(it) } }
            .build()

        runCatching { PhoneAuthProvider.verifyPhoneNumber(options) }
            .onFailure { e -> onError(describe(e)) }
    }

    /** Checks the 6-digit code the member typed against the code that was sent. */
    fun verifyCode(code: String, onResult: (Boolean, String?) -> Unit) {
        val id = verificationId
        if (id == null) {
            onResult(false, "Request an OTP first.")
            return
        }
        val credential = runCatching { PhoneAuthProvider.getCredential(id, code.trim()) }
            .getOrElse {
                onResult(false, "That OTP is not valid.")
                return
            }
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener {
                reset()
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "OTP verify failed", e)
                onResult(false, describe(e))
            }
    }

    private fun describe(e: Throwable): String = when {
        e is FirebaseAuthInvalidCredentialsException ->
            "That OTP is incorrect or has expired. Please try again."
        e is FirebaseTooManyRequestsException ->
            "Too many OTP requests from this device. Please try again later."
        e.message?.contains("BILLING_NOT_ENABLED", ignoreCase = true) == true ->
            "Phone sign-in is not enabled for this Firebase project yet."
        e.message?.contains("not authorized", ignoreCase = true) == true ->
            "This app is not authorised for phone sign-in. Enable the Phone provider in Firebase."
        else -> e.message ?: "Could not send the OTP. Check your network and try again."
    }
}
