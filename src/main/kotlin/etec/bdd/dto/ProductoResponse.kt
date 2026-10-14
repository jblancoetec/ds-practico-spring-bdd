package etec.bdd.dto

import etec.bdd.entity.Producto
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import java.math.BigDecimal

/**
 * DTO de salida. Separado de la entidad para que la capa de transporte
 * no exponga anotaciones JPA al cliente ni le entregue un objeto con
 * relaciones lazy sin querer.
 *
 * Sobre `@JsonSerialize(using = ToStringSerializer::class)` en `precio`:
 * el motor guarda `950.00`. Si lo serializás como número, Jackson 3 lo
 * emite como `950.0` y el cliente no distingue entre `950.00`, `950` y
 * `9.5` — peligroso para redondeos monetarios. La anotación fuerza
 * `"precio":"950.00"` y preserva los ceros significativos.
 *
 * Importante: en Spring Boot 4 / Jackson 3 las clases de Jackson
 * viven en `tools.jackson.*`, no en `com.fasterxml.jackson.*`.
 */
data class ProductoResponse(
    val id: Long,
    val nombre: String,

    @JsonSerialize(using = ToStringSerializer::class)
    val precio: BigDecimal,

    val stock: Int,
) {
    companion object {
        fun fromEntity(p: Producto): ProductoResponse =
            ProductoResponse(
                id = p.id ?: 0L,
                nombre = p.nombre,
                precio = p.precio,
                stock = p.stock,
            )
    }
}
