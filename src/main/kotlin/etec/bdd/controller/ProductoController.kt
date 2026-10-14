package etec.bdd.controller

import etec.bdd.dto.ProductoRequest
import etec.bdd.dto.ProductoResponse
import etec.bdd.service.ProductoService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints REST para `Producto`. Alcance de este laboratorio:
 *   - `GET /api/productos`            listar (paginado, query `?page=…&size=…&sort=…`)
 *   - `GET /api/productos/{id}`       obtener uno (404 si no existe)
 *   - `POST /api/productos`           crear (201 Created; 400 por validación)
 *
 * `@Valid` antes del `@RequestBody` activa las anotaciones
 * `@field:NotBlank`, `@field:Size`, `@field:DecimalMin`, `@field:Min`
 * del DTO; si alguna falla, `MethodArgumentNotValidException` cae en
 * `GlobalExceptionHandler` con 400 y cuerpo uniforme.
 */
@RestController
@RequestMapping("/api/productos")
class ProductoController(
    private val service: ProductoService,
) {
    @GetMapping
    fun listar(pageable: Pageable): Page<ProductoResponse> =
        service.listar(pageable).map { ProductoResponse.fromEntity(it) }

    @PostMapping
    fun crear(@Valid @RequestBody req: ProductoRequest): ResponseEntity<ProductoResponse> {
        val creado = service.crear(req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProductoResponse.fromEntity(creado))
    }

    @GetMapping("/{id}")
    fun obtener(@PathVariable id: Long): ProductoResponse =
        ProductoResponse.fromEntity(service.obtener(id))
}
