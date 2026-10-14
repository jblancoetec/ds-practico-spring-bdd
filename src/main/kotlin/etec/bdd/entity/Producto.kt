package etec.bdd.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Entidad que mapea la tabla `productos`. El esquema es versionado por
 * Flyway (ver `src/main/resources/db/migration/V1__productos.sql`).
 *
 * Cada `@Column` está dimensionado a medida del esquema:
 *   - `nullable = false`         calza con `NOT NULL`
 *   - `length = 120`             calza con `VARCHAR(120)`
 *   - `precision = 10, scale = 2` calza con `DECIMAL(10,2)`
 *
 * Si tocás cualquiera de estos valores, `spring.jpa.hibernate.ddl-auto=validate`
 * rompe al arrancar la app y te dice qué desajuste hay. Eso es bueno:
 * es la red de seguridad.
 *
 * Sobre `id: Long? = null`: Hibernate asigna el id en el INSERT (la columna
 * es `BIGINT AUTO_INCREMENT`). Antes del primer insert el id no existe en
 * la entidad, así que dejamos el tipo como nullable. El plugin
 * `kotlin-maven-noarg` + plugin de compilación `jpa` genera el
 * constructor sin argumentos que Hibernate exige; no es necesario
 * escribirlo a mano.
 */
@Entity
@Table(name = "productos")
class Producto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 120)
    var nombre: String = "",

    @Column(nullable = false, precision = 10, scale = 2)
    var precio: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var stock: Int = 0,
)
