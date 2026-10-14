package etec.bdd.service

import etec.bdd.dto.ProductoRequest
import etec.bdd.entity.Producto
import etec.bdd.exception.ProductoNoEncontradoException
import etec.bdd.repository.ProductoRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * Servicio de negocio para `Producto`. La validación de DTO
 * ya la hace `@Valid` en el controller; acá queda la lógica de
 * negocio (resolver entidades, decidir si lanzar
 * `ProductoNoEncontradoException`, persistir cambios).
 *
 * Para este laboratorio el alcance es GET (paginado), GET por id
 * y POST. PUT y DELETE se dejan para prácticos subsiguientes.
 */
@Service
class ProductoService(
    private val productoRepository: ProductoRepository,
) {
    fun listar(pageable: Pageable): Page<Producto> =
        productoRepository.findAll(pageable)

    fun obtener(id: Long): Producto =
        productoRepository.findById(id).orElseThrow {
            ProductoNoEncontradoException(id)
        }

    fun crear(req: ProductoRequest): Producto {
        val entity = Producto(
            nombre = req.nombre,
            precio = req.precio,
            stock = req.stock,
        )
        return productoRepository.save(entity)
    }
}
