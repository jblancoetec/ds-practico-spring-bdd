# ds-practico-spring-bdd

Trabajo práctico — Diseño de Software.
Conexión de una aplicación Spring Boot (Kotlin) a un motor de bases de
datos MariaDB provisto por el laboratorio, y mapeo mínimo de la entidad
`Producto` (GET listar paginado y POST crear). El CRUD completo queda
para prácticos subsiguientes.

> El nombre `bdd` del proyecto es la abreviatura de **Base de Datos**
> en español, no de Behavior-Driven Development. Cualquier confusión
> con Cucumber/JBehave es, justamente, de eso.

## Contexto

En este práctico vas a conectar la aplicación a un motor MariaDB que el
laboratorio pone a disposición en un servidor. La dirección y el puerto
del servidor, así como las credenciales y el nombre de la base, te los
entrega el docente al comenzar. La base de datos **llega con la tabla
vacía**: al arrancar la app por primera vez, **Flyway** ejecuta la
migración `V1__productos.sql` (incluida en este repo) y crea la tabla
`productos`. A partir de ahí, el CRUD que armes opera contra esa tabla.

### Datos de conexión provistos por el laboratorio

Estos valores los completa el docente en clase. Dejá los espacios en
blanco hasta tenerlos confirmados; los valores por defecto del archivo
`application-lab.properties` apuntan a `localhost:3306` para que puedas
hacer una primera prueba local antes de apuntar al servidor del aula.

| Parámetro              | Valor provisto por el docente |
|------------------------|-------------------------------|
| Host / IP del servidor |                               |
| Puerto                 |                               |
| Nombre de la base      |                               |
| Usuario                |                               |
| Contraseña             |                               |

> Si al intentar conectarte no podés o el motor no responde, avisale al
> docente antes de seguir. No adivines direcciones ni cambies
> credenciales por tu cuenta.

## Requisitos previos

- JDK 21 (`java -version` debe reportar 21.x).
- Maven wrapper del proyecto (`./mvnw`).
- Acceso de red al servidor del laboratorio (directo o vía SSH).

## Paso 1 — Instalación de dependencias (`pom.xml`)

Vas a agregar bloques al `pom.xml`. Antes de pegarlos, mirá esta tabla:
te dice **qué hace cada cosa**, no sólo qué sintaxis XML usar.

### ¿Qué hace cada dependencia?

| Dependencia | Para qué sirve |
|-------------|----------------|
| `spring-boot-starter-data-jpa` | Trae **Spring Data** (que te ahorra escribir un repositorio: una interfaz `JpaRepository<Entidad, ID>` ya tiene `findAll`, `save`, `deleteById`, etc.) y **Hibernate** (que traduce las clases Kotlin a filas en el motor). Activa auto-configuración: si ve las deps + un datasource, monta el `EntityManagerFactory` solo. |
| `org.mariadb.jdbc:mariadb-java-client` | **Driver JDBC** que habla con MariaDB por TCP. Va en alcance `runtime` porque tu código no la usa directamente; Hibernate la consume por debajo. Sin este jar, el handshake JDBC falla con `No suitable driver`. |
| `spring-boot-starter-validation` | Habilita `@field:NotBlank`, `@field:Size`, `@field:DecimalMin`, `@field:Min`, etc. sobre los DTOs. Sin este starter, las anotaciones son sólo anotaciones: no se ejecutan, no hay 400 automáticos, tenés que validar a mano en el servicio. |
| `org.flywaydb:flyway-core` + `org.flywaydb:flyway-mysql` | **Versionado de esquema**. Cada cambio de estructura pasa a un archivo `src/main/resources/db/migration/V<n>__nombre.sql`. Spring Boot los aplica al arrancar, en orden. Tu esquema deja de vivir "en la cabeza del docente" y queda commiteado en el repo del proyecto. |

### Bloques a pegar en `<dependencies>`

```xml
<!-- Laboratorio: persistencia con Spring Data JPA, MariaDB y migraciones Flyway -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

Las versiones se resuelven desde la BOM de `spring-boot-starter-parent`
(4.1.1); no las pongas a mano.

### El plugin Kotlin `kotlin-maven-noarg` + compiler plugin `jpa`

JPA exige que las entidades tengan un constructor sin argumentos. Kotlin
no lo genera en clases comunes (sí en `data class`, pero las `data class`
tienen problemas de identidad con entidades). Por eso agregás:

- Una nueva dependencia dentro del bloque `<dependencies>` del
  `kotlin-maven-plugin` (el actual sólo tiene `kotlin-maven-allopen`):
  ```xml
  <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-maven-noarg</artifactId>
      <version>${kotlin.version}</version>
  </dependency>
  ```
- Un nuevo elemento `<plugin>jpa</plugin>` dentro de `<compilerPlugins>`,
  al lado del `spring` que ya está.

Combinados, le dicen al compilador que añada un constructor sin
argumentos a cualquier clase anotada con `@Entity`. Sin esto, el primer
`findById` o `save` falla con un mensaje confuso sobre instanciación.

### El flag `-Xjsr305=strict`

Quedó configurado en el `pom.xml` desde `start.spring.io`. Sirve para
que los tipos `nullability` que vienen desde Java (driver JDBC,
Hibernate) se traten como estrictos en Kotlin. Eso evita que clases
Java que el driver expone te lleguen con nullables flexibles donde
esperás valores. **Lo conservamos.**

(El Kotlin 2.4 ya marca el flag como default; por eso removimos del
pom el otro flag relacionado, `-Xannotation-default-target=param-property`,
que se volvió redundante y dispara una WARNING de compilación. Si lo
ves listado en algún snippet viejo, no lo agregues.)

### Jackson 3 en Spring Boot 4

Spring Boot 4 migró de Jackson 2 (`com.fasterxml.jackson.*`) a Jackson 3
(`tools.jackson.*`). Si vas a usar `ObjectMapper`, `JsonSerialize`,
`@JsonSerialize` o cualquier tipo de Jackson en tu código, importá
siempre desde `tools.jackson.*`, **no** desde `com.fasterxml.jackson.*`.
Por ejemplo:

| Jackson 2 (lo de antes)                | Jackson 3 + Spring Boot 4 (lo de ahora)       |
|----------------------------------------|----------------------------------------------|
| `com.fasterxml.jackson.core.JsonProcessingException` | `tools.jackson.core.JacksonException` |
| `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper`         |
| `com.fasterxml.jackson.databind.annotation.JsonSerialize` | `tools.jackson.databind.annotation.JsonSerialize` |
| `com.fasterxml.jackson.databind.ser.std.ToStringSerializer` | `tools.jackson.databind.ser.std.ToStringSerializer` |

Las excepciones de Jackson 3 viven todas bajo `JacksonException`. Si
copiás código viejo, lo más probable es que falle el import.

## Paso 2 — Configuración del proyecto (`application.properties`)

`src/main/resources/application.properties` queda con la configuración
técnica (JPA, Flyway, logging) y referencia al perfil `lab` para los
datos de conexión. Los valores del docente van en
`application-lab.properties`.

```
spring.application.name=bdd
spring.profiles.active=${SPRING_PROFILES_ACTIVE:lab}

# Capa JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.open-in-view=false

# Flyway — versionado de esquema (las migraciones viven en el proyecto)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# Logging estructurado simple (thread, logger corto, mensaje)
logging.pattern.console=%5p | %t | %c{1.} | %m%n
```

`src/main/resources/application-lab.properties` recibe los valores
literales. Editá los placeholders antes de correr la aplicación:

```
# Perfil "lab" — valores provistos por el docente en clase.
spring.datasource.url=jdbc:mariadb://localhost:3306/practico_bdd
spring.datasource.username=estudiante
spring.datasource.password=estudiante_practico
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
```

Por qué cada decisión:

- `ddl-auto=validate` — el esquema lo define Flyway. Hibernate debe
  verificar que las clases Kotlin que vas a mapear coincidan con las
  tablas que la migración creó, **nunca** debe generar ni modificar
  schema. Esto detecta al instante cualquier desajuste con la tabla
  (cambiar `length=120` por algo mayor, agregar `nullable=true` a una
  columna `NOT NULL`, etc.) — la app no arranca hasta que coincida.
- `show-sql=true` para ver las sentencias mientras aprendés.
- `open-in-view=false` — Hibernate 7 lo activa por defecto, pero la
  convención moderna es cerrarlo: el `EntityManager` se cierra al
  terminar la transacción, en lugar de quedar abierto durante el
  render de la respuesta HTTP. Eso evita queries "dormidas" en
  lugares inesperados.
- **`spring.jpa.properties.hibernate.dialect`** ya **no** se setea.
  Hibernate 7 autodetecta el dialecto a partir del driver JDBC;
  setearlo manualmente produce un aviso de deprecación
  `HHH90000025` y no agrega valor.

### Camino alternativo: variables de entorno

Si querés evitar valores literales en `application-lab.properties`
(recomendable fuera del aula), usá variables de entorno. Copiá
`.env.example` a `.env`, completá los valores y arrancá la app con
`./mvnw spring-boot:run` desde la misma shell. Spring Boot las
resuelve a través de "relaxed binding" contra
`spring.datasource.*`. Las credenciales quedan fuera del repo.

```
SPRING_PROFILES_ACTIVE=lab
SPRING_DATASOURCE_URL=jdbc:mariadb://HOST:PUERTO/NOMBRE_BD
SPRING_DATASOURCE_USERNAME=USUARIO
SPRING_DATASOURCE_PASSWORD=CONTRASEÑA
```

`.env` queda ignorado por `.gitignore` automáticamente; `.env.example`
sí se versiona como contrato del formato.

## Paso 3 — Mapeo mínimo (objetivo del laboratorio)

Vas a construir el camino mínimo desde la entidad hasta el endpoint
REST. El alcance es: **dos endpoints** (`GET` listar paginado y `POST`
crear) más un `GET` por id para verificar el manejo de 404. El CRUD
completo (PUT, DELETE, paginación con más controles, validaciones de
negocio adicionales) se cubre en prácticos subsiguientes.

### 3.1 Entidad — `src/main/kotlin/etec/bdd/entity/Producto.kt`

```kotlin
package etec.bdd.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

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
```

Por qué cada `@Column` está dimensionado así:

- `nullable = false` — coincide con `NOT NULL`. Si no coincide,
  `ddl-auto=validate` rompe al arrancar.
- `length = 120` — coincide con `VARCHAR(120)`.
- `precision = 10, scale = 2` — coincide con `DECIMAL(10,2)`.

Sobre `id: Long? = null`: Hibernate asigna el id en el INSERT (la
columna es `BIGINT AUTO_INCREMENT`). Aceptar `null` refleja que
antes del primer `INSERT` el id no existe. El plugin
`kotlin-maven-noarg` + plugin `jpa` genera el constructor sin
argumentos que Hibernate exige; no es necesario escribirlo a mano.

### 3.2 Repositorio — `src/main/kotlin/etec/bdd/repository/ProductoRepository.kt`

```kotlin
package etec.bdd.repository

import etec.bdd.entity.Producto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductoRepository : JpaRepository<Producto, Long>
```

Heredás `findAll(Pageable)`, `findById(Long)`, `save(Producto)`,
`deleteById(Long)`, `existsById(Long)`, etc. No hace falta declarar
nada para este laboratorio.

### 3.3 DTOs

Pedido y respuesta, ambos separados de la entidad para que el cliente
no vea anotaciones JPA.

**`src/main/kotlin/etec/bdd/dto/ProductoRequest.kt`** — entrada con
validación declarativa:

```kotlin
package etec.bdd.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

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
```

Las anotaciones `@field:*` se procesan gracias a
`spring-boot-starter-validation` y al `@Valid` del controller. No hace
falta validar a mano dentro del servicio.

**`src/main/kotlin/etec/bdd/dto/ProductoResponse.kt`** — salida con
`BigDecimal` formateado como string:

```kotlin
package etec.bdd.dto

import etec.bdd.entity.Producto
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import java.math.BigDecimal

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
```

Sobre `@JsonSerialize(using = ToStringSerializer::class)` en `precio`:
el motor guarda `950.00`. Si lo serializás como número, Jackson lo
emite como `950.0` y el cliente no puede distinguir entre `950.00`,
`950` y `9.5` — peligroso para redondeos monetarios. La anotación
fuerza `"precio":"950.00"` y preserva los ceros significativos. **Si tu
proyecto no lidia con montos, podés quitarla.**

Sobre los imports: en Spring Boot 4 / Jackson 3 las clases de Jackson
viven en `tools.jackson.*`. Si copiás tutoriales viejos te van a saltar
errores de import.

### 3.4 Servicio — `src/main/kotlin/etec/bdd/service/ProductoService.kt`

Lógica de negocio. Tres métodos para el alcance:

- `listar(pageable)` — `Page<Producto>` desde el repositorio.
- `obtener(id)` — `findById` o lanza `ProductoNoEncontradoException`
  (que el handler convierte a 404).
- `crear(req)` — instancia la entidad con los datos del request y
  guarda.

`actualizar(...)` y `eliminar(...)` quedan fuera de alcance (próximo
práctico).

### 3.5 Controlador — `src/main/kotlin/etec/bdd/controller/ProductoController.kt`

`@RestController` con `@RequestMapping("/api/productos")`. Tres
endpoints:

| Método | Path                      | Resultado esperado                              |
|--------|---------------------------|------------------------------------------------|
| GET    | `/api/productos`          | 200 + `Page<ProductoResponse>` (paginado)      |
| GET    | `/api/productos/{id}`     | 200 + body, **o** 404 si no existe             |
| POST   | `/api/productos`          | 201 + body, **o** 400 si la validación falla   |

El `@Valid` antes del `@RequestBody` activa las anotaciones del DTO;
si alguna falla, `MethodArgumentNotValidException` cae en el handler.

### 3.6 Manejo de errores — `exception/`

- `ProductoNoEncontradoException(id)` — excepción de dominio que el
  servicio lanza cuando un id no existe.
- `GlobalExceptionHandler` — `@RestControllerAdvice` que mapea:
  - `ProductoNoEncontradoException` → **404** con cuerpo uniforme.
  - `MethodArgumentNotValidException` → **400** con detalles de
    validación.
  - `JacksonException` (Jackson 3) → **400** por JSON malformado.

El cuerpo de error tiene siempre la misma forma:
`{"timestamp":..., "error":..., "message":...}`.

## Verificación

Después de los pasos 1, 2 y 3:

1. **`./mvnw -q compile`** — chequea dependencias y compilación. Si
   falla acá, revisá el orden de las dependencias o las anotaciones de
   la entidad.
2. **Levantar el motor** del aula (Docker en tu máquina, o el servidor
   que indique el docente).
3. **`./mvnw -q -DskipTests package`** — empaqueta el JAR.
4. **`./mvnw -q spring-boot:run`** — arrancá la app. Vas a ver en el
   log:
   - `Flyway Community Edition ... Starting...` y luego
     `Successfully applied 1 migration to schema ...` (la V1).
   - `HikariPool-1 - Start completed.`.
   - `Tomcat started on port 8080`.
5. **Probar los endpoints**:
   ```
   # Listar (paginado por defecto: 20 elementos, orden por id asc)
   curl -s "http://127.0.0.1:8080/api/productos?page=0&size=20" | jq
   # Crear
   curl -i -X POST http://127.0.0.1:8080/api/productos \
     -H 'Content-Type: application/json' \
     -d '{"nombre":"Resma A4","precio":8500.50,"stock":15}'
   # Obtener uno (404 si no existe)
   curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/productos/1
   ```

Errores comunes y qué revisar:

- `Access denied for user '…'` → credenciales mal transcritas en
  `application-lab.properties` o conflicto con `.env`.
- `Connection refused` o `UnknownHostException` → IP / puerto mal, o
  tráfico bloqueado (probar el túnel SSH del Anexo).
- `Schema-validation: missing table [productos]` → Flyway no aplicó la
  migración. Revisá los logs de Flyway; probablemente
  `spring.flyway.enabled` está en otra rama de configuración.
- `Schema-validation: column [...]` → discrepancia entre `@Column` y
  la tabla. Ajustá la entidad o ajustá la migración, **no** la app.

## Próximos pasos (no incluidos en este laboratorio)

Para los prácticos subsiguientes quedan, entre otros:

- `PUT /api/productos/{id}` y `DELETE /api/productos/{id}` para cerrar
  el CRUD.
- Manejo de errores más rico (404 con `Location`, validaciones de
  negocio adicionales).
- `application-{local,prod}.properties` con perfiles separados por
  entorno.
- Migración a **Testcontainers** para que los tests (`mvn test`) no
  pidan el motor del aula ni del servidor del docente — desacopla el
  CI y permite correr la suite offline.
- Validación con `@field:DecimalMin` y `@field:Min` extendida a más
  reglas, con `@ControllerAdvice` específico por tipo de error.
- Si el nombre del proyecto lo justifica a nivel institucional, un
  framework BDD (Cucumber-JVM, Kotest con spec) para escribir
  escenarios ejecutables. Recordá: en este repo `bdd` es la sigla de
  **Base de Datos**, no de Behavior-Driven Development.

---

## Anexo (recomendado) — Hablarle al motor desde la terminal

Cuando algo no anda, abrir una terminal y descartar problemas de
credenciales o puerto es la forma más rápida. La app va a hacer
exactamente lo mismo (un handshake JDBC), así que probar acá descarta
la mayor parte de las dudas de configuración.

### Cliente nativo

Linux:

```
sudo apt install mariadb-client         # Debian/Ubuntu
sudo dnf install mariadb                # Fedora/RHEL
```

macOS:

```
brew install mariadb-client
```

Windows: instalador "MariaDB Server" o "MariaDB Client" desde
mariadb.org (agregá `C:\Program Files\MariaDB …\bin` al PATH).

Conexión directa (TCP permitido hasta el servidor):

```
mariadb -h <HOST> -P <PUERTO> -u <USUARIO> -p
# te pide la contraseña interactivamente
```

Si los valores del laboratorio son los del perfil lab:

```
mariadb -h 127.0.0.1 -P 3306 -u estudiante -pestudiante_practico \
  practico_bdd \
  -e "SELECT id, nombre, precio, stock FROM productos ORDER BY id;"
```

(Notá: `-p<CONTRASEÑA>` sin espacio es cómodo pero deja la contraseña
en el historial de la shell. Para logs o capturas, preferí `-p` y
tipearla interactiva.)

### Si sólo podés llegar al servidor por SSH

Túnel "puro" (tu `localhost:3306` → puerto `3306` del servidor):

```
ssh -L 3306:127.0.0.1:3306 usuario@<HOST>
```

Variante bastión (BD en host interno, llegás vía puente):

```
ssh -L 3306:<IP_INTERNA_BD>:3306 usuario@<IP_BASTION>
```

Mientras el túnel esté abierto:

```
mariadb -h 127.0.0.1 -P 3306 -u <USUARIO> -p
```

En `application-lab.properties` y/o `.env` apuntás la URL a
`jdbc:mariadb://127.0.0.1:3306/<NOMBRE_BD>` mientras el túnel esté
abierto.

### Comandos útiles una vez adentro

```
SHOW DATABASES;           # bases que tu usuario puede ver
USE <NOMBRE_BD>;          # entrar
SHOW TABLES;              # debería mostrar `productos` si Flyway corrió
SELECT VERSION();         # confirmar dialecto y versión
DESCRIBE productos;       # ver columnas y restricciones
\q                        # salir
```

Si `SHOW TABLES` no muestra `productos`, Flyway todavía no corrió
contra esta base. La app la crea al primer arranque; en línea de
comandos podés forzar revisando logs de Spring Boot olimpiando `target/`
y volviendo a levantar.

### Errores típicos

- `ERROR 1045 (28000): Access denied for user …` → usuario o
  contraseña mal transcritos.
- `ERROR 2003 (HY000): Can't connect to MySQL server on '<HOST>'` →
  IP o puerto mal, o tráfico bloqueado (probar el túnel SSH).
- `ERROR 1130 (HY000): Host '<HOST>' is not allowed to connect` → el
  motor tiene reglas de `user@host` que limitan desde qué IP se acepta
  la conexión; pedirle al docente que revise.

### Una advertencia operativa

Pegar contraseñas reales en terminales compartidas, capturas de
pantalla o apuntes queda en evidencia. Para algo más allá del aula
usá variables de entorno (`.env`, ignorado por git) o perfiles
separados.
