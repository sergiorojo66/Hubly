package com.example.rankup.data.network

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

object FcmSender {
    private const val BASE_URL = "https://fcm.googleapis.com/v1/projects/runk-up/messages:send"
    private val client = OkHttpClient()

    private fun getAccessToken(context: Context): String {
        val inputStream: InputStream = context.assets.open("runk-up-firebase-adminsdk-fbsvc-0a3f42db10.json")
        val credentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    suspend fun enviarNotificacionPush(
        context: Context,
        tokensParticipantes: List<String>,
        titulo: String,
        mensaje: String
    ) = withContext(Dispatchers.IO) {
        try {
            val tokenAuth = getAccessToken(context)

            for (tokenDevice in tokensParticipantes) {
                val jsonBody = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", tokenDevice)
                        put("notification", JSONObject().apply {
                            put("title", titulo)
                            put("body", mensaje)
                        })
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = jsonBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(BASE_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer $tokenAuth")
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("FcmSender", "Error enviando a token $tokenDevice: ${response.body?.string()}")
                    } else {
                        Log.d("FcmSender", "Notificación enviada con éxito")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FcmSender", "Error general en enviarNotificacionPush", e)
        }
    }
}