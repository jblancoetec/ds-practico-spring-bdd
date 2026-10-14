package etec.bdd.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * DTO de entrada para crear o actualizar un producto. Las anotaciones
 * `@field:*` se procesan gracias a `spring-boot-starter-validation`
 * y al `@Valid` que se coloca en los argumentos del controller.
 * No hace falta validar a mano dentro del servicio.
 */
data class ProductoRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 120)
    val nombre: String,

    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val precio: BigDecimal,

    @field:NotNull
    @field:Min(0)
    val stock: Int,
)
