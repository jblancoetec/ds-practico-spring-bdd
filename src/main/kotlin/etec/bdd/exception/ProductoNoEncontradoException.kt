package etec.bdd.exception

/**
 * Excepción de dominio para el caso "el id que pediste no existe".
 * El `GlobalExceptionHandler` la mapea a 404 con cuerpo JSON uniforme.
 */
class ProductoNoEncontradoException(id: Long) :
    RuntimeException("Producto con id=$id no encontrado")
