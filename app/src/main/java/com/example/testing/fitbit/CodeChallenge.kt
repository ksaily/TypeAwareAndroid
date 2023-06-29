package com.example.testing.fitbit

import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class CodeChallenge {
    companion object {
        const val CLIENT_ID: String = "2393N9"
        const val REDIRECT_URL: String = "https://type-aware-katri.com"
        val CODE_VERIFIER = getCodeVerifier()
        var authorizationCode : String? = null
        var uniqueState: String? = null

        /**
         * Create code verifier that will be sent to OAuth2 to request tokens
         */
        private fun getCodeVerifier(): String {
            val secureRandom = SecureRandom()
            val code = ByteArray(64)
            secureRandom.nextBytes(code)
            return Base64.encodeToString(
                code,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        }

        fun getCodeChallenge(verifier: String): String {
            val bytes = verifier.toByteArray()
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(bytes, 0, bytes.size)
            val digest = messageDigest.digest()
            return Base64.encodeToString(
                digest,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        }
    }
}