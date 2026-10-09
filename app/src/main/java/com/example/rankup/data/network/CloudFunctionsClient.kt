package com.example.rankup.data.network

import com.example.rankup.util.AppLogger
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

object CloudFunctionsClient {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun notifyEvent(eventId: String, title: String, body: String) {
        try {
            functions
                .getHttpsCallable("sendEventNotification")
                .call(mapOf("eventId" to eventId, "title" to title, "body" to body))
                .await()
        } catch (e: Exception) {
            AppLogger.e("CloudFunctionsClient", "Error al enviar notificación de evento", e)
        }
    }

    suspend fun joinPrivateEvent(eventId: String, password: String): Result<Unit> {
        return try {
            functions
                .getHttpsCallable("joinPrivateEvent")
                .call(mapOf("eventId" to eventId, "password" to password))
                .await()
            Result.success(Unit)
        } catch (e: FirebaseFunctionsException) {
            AppLogger.e("CloudFunctionsClient", "Error al unirse a evento privado (${e.code})", e)
            val message = when (e.code) {
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Contraseña incorrecta"
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> "El evento está completo"
                FirebaseFunctionsException.Code.NOT_FOUND -> "Evento no encontrado"
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Tu sesión ha caducado, vuelve a iniciar sesión"
                else -> "No se pudo unir al evento. Inténtalo de nuevo."
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            AppLogger.e("CloudFunctionsClient", "Error inesperado al unirse a evento privado", e)
            Result.failure(Exception("No se pudo unir al evento. Inténtalo de nuevo."))
        }
    }
}
