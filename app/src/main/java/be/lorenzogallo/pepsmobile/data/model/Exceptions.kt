package be.lorenzogallo.pepsmobile.data.model

sealed class PepsException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AuthException(message: String) : PepsException(message)
class SessionExpiredException : PepsException("Session expirée")
class NetworkException(message: String, cause: Throwable? = null) : PepsException(message, cause)
class ParseException(message: String, cause: Throwable? = null) : PepsException(message, cause)

fun Throwable.toUserMessage(): String = when (this) {
    is AuthException -> message ?: "Authentification refusée"
    is SessionExpiredException -> "Session expirée, reconnexion nécessaire"
    is NetworkException -> message ?: "Connexion impossible"
    is ParseException -> "Impossible de lire la réponse de PepSchool"
    else -> "Erreur inattendue"
}
