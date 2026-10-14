package etec.bdd.repository

import etec.bdd.entity.Producto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * `JpaRepository<Producto, Long>` ya hereda todos los métodos CRUD
 * y de paginación: `findAll(Pageable)`, `findById(Long)`,
 * `save(Producto)`, `deleteById(Long)`, `existsById(Long)`, etc.
 * No hace falta declarar nada a mano para este laboratorio.
 */
@Repository
interface ProductoRepository : JpaRepository<Producto, Long>
