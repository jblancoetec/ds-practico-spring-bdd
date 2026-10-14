package etec.bdd.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import tools.jackson.core.JacksonException
import java.time.Instant

/**
 * `@RestControllerAdvice` con cuerpo de error uniforme. Se aplica a
 * todos los `@RestController` del proyecto.
 *
 * Capa lo que captura:
 *   - `ProductoNoEncontradoException`  → 404 (id inexistente)
 *   - `MethodArgumentNotValidException` → 400 (validación declarativa fallida)
 *   - `JacksonException` (Jackson 3)    → 400 (JSON malformado)
 *
 * En Spring Boot 4 / Jackson 3 la excepción unificada de Jackson
 * es `tools.jackson.core.JacksonException`. En Jackson 2 era
 * `com.fasterxml.jackson.core.JsonProcessingException`; ya no existe
 * con ese nombre en Spring Boot 4.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    data class ApiError(
        val timestamp: Instant,
        val error: String,
        val message: String,
    )

    @ExceptionHandler(ProductoNoEncontradoException::class)
    fun handleNotFound(ex: ProductoNoEncontradoException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(
                timestamp = Instant.now(),
                error = "Producto no encontrado",
                message = ex.message.orEmpty(),
            )
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val detalles = ex.bindingResult.allErrors.joinToString("; ") {
            it.defaultMessage.orEmpty()
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                timestamp = Instant.now(),
                error = "Datos inválidos",
                message = detalles,
            )
        )
    }

    @ExceptionHandler(JacksonException::class)
    fun handleJackson(ex: JacksonException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                timestamp = Instant.now(),
                error = "JSON inválido",
                message = ex.message.orEmpty(),
            )
        )
}
