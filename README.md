# ds-practico-spring-bdd

Trabajo práctico — Diseño de Software.
Conexión de una aplicación Spring Boot (Kotlin) a un motor de bases de
datos MariaDB provisto por el laboratorio.

## Contexto

En este práctico vas a conectar la aplicación a un motor MariaDB que el
laboratorio pone a disposición en un servidor. La dirección y el puerto
del servidor, así como las credenciales y el nombre de la base, te los
entrega el docente al comenzar. La base de datos **no tiene tablas del
usuario todavía** — crear las tablas también es parte de tu trabajo y se
aborda en pasos posteriores a este laboratorio.

### Datos de conexión provistos por el laboratorio

Estos valores los completa el docente en clase. Dejá espacios en blanco
en tu `application.properties` hasta tenerlos confirmados:

| Parámetro              | Valor provisto por el docente |
|------------------------|-------------------------------|
| Host / IP del servidor |                               |
| Puerto                 |                               |
| Nombre de la base      |                               |
| Usuario                |                               |
| Contraseña             |                               |

> Si al intentar conectarte no podés o el motor no responde, avisale al
> docente antes de seguir. No adivines direcciones ni cambies credenciales
> por tu cuenta.

## Requisitos previos

- JDK 21 (`java -version` debe reportar 21.x).
- Maven wrapper del proyecto (`./mvnw`).
- Acceso de red al servidor del laboratorio (directo o vía SSH, según la
  topología que el docente indique).

## Paso 1 — Instalación de dependencias (`pom.xml`)

Vas a agregar bloques al `pom.xml`. Te explico qué hace cada uno y dónde
ubicarlo, sin cambiar nada de lo que ya está.

### 1.1 Dependencias para JPA y driver de MariaDB

Añadí el siguiente bloque dentro de `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <scope>runtime</scope>
</dependency>
```

- `spring-boot-starter-data-jpa` — trae Hibernate y Spring Data, los
  componentes que traducen clases Kotlin a tablas y operaciones.
- `org.mariadb.jdbc:mariadb-java-client` — driver JDBC que entiende el
  protocolo de MariaDB. Va en alcance `runtime` porque tu código de
  aplicación no lo usa directamente, sólo Hibernate.

### 1.2 Plugin Kotlin para entidades JPA

JPA exige un constructor sin argumentos en las entidades y Kotlin no lo
genera por defecto en clases comunes. Sin este plugin la aplicación falla
en runtime con un mensaje confuso al instanciar la entidad. Por eso
agregás dos cambios pequeños al `kotlin-maven-plugin` que ya tenés en
`pom.xml`:

- Sumá una nueva dependencia dentro del bloque `<dependencies>` del
  plugin (el actual sólo tiene `kotlin-maven-allopen`):

```xml
<dependency>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-noarg</artifactId>
    <version>${kotlin.version}</version>
</dependency>
```

- Sumá `<plugin>jpa</plugin>` al lado de `<plugin>spring</plugin>` dentro
  de `<compilerPlugins>`.

Con eso, las clases anotadas con `@Entity` reciben el constructor sin
argumentos que Hibernate necesita sin que tengas que escribirlo a mano.

## Paso 2 — Configuración del proyecto (`application.properties`)

Vas a reemplazar/ampliar `src/main/resources/application.properties`
pegando estos bloques y sustituyendo los placeholders `<...>` por los
valores que te entregó el docente (los mismos de la tabla del inicio del
README).

```
# Conexión al motor
spring.datasource.url=jdbc:mariadb://<HOST>:<PUERTO>/<NOMBRE_BD>
spring.datasource.username=<USUARIO>
spring.datasource.password=<CONTRASEÑA>
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# Capa JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
```

Por qué cada decisión:

- `jdbc:mariadb://…` (no `jdbc:mysql://…`) — el driver distingue el
  protocolo aunque MariaDB sea compatible con MySQL.
- `ddl-auto=validate` — el esquema es responsabilidad del equipo de diseño
  de datos en pasos posteriores; Hibernate debe verificar que las clases
  Kotlin que vas a mapear luego coincidan con las tablas que vos mismo
  crees, **nunca** debe generar ni modificar schema. Es la separación de
  responsabilidades que este práctico busca enseñar.
- `show-sql=true` para ver las sentencias mientras aprendés.
- `MariaDBDialect` fijo, sin dejar que Spring lo adivine.

> Si los valores del docente tienen caracteres especiales en la URL (por
> ejemplo `&` o espacios), codificalos siguiendo el estándar URL.
> Si la conexión pasa por un túnel SSH, en `spring.datasource.url`
> apuntás a `127.0.0.1:3306` (ver Anexo).

## Verificación al terminar los pasos 1 y 2

Esperá que estos comandos terminen sin errores:

- `./mvnw -q -DskipTests package` — confirma dependencias y compilación.
- `./mvnw -q test` — corre el `contextLoads()`. Spring Boot intentará
  armar el contexto de aplicación, lo que implica abrir una conexión
  JDBC contra la URL configurada. **Una prueba exitosa indica que la
  capa de infraestructura quedó correctamente enchufada**; no valida
  todavía el esquema porque todavía no hay entidades que comparar.
- (Recomendado manualmente) `./mvnw spring-boot:run` y revisar en logs:
  debe aparecer `HikariPool-... - Start completed.` y la conexión JDBC al
  host/puerto declarado. Cualquier error de `Access denied` o
  `Connection refused` indica credenciales equivocadas, IP/puerto mal
  transcrito o ausencia del túnel SSH. Volvé sobre `application.properties`
  antes de seguir.

## Próximos pasos (no incluidos en este README)

El modelado de `productos` (diseño de datos, decisión de columnas y
restricciones), la creación de la tabla vía el motor, el mapeo en Kotlin,
el repositorio, el servicio y el controlador se cubren en prácticos
subsiguientes. Este laboratorio cierra la conexión del proyecto con el
motor y nada más.

---

## Anexo (recomendado) — Hablarle al motor desde la terminal

Cuando algo no anda, es más rápido abrir una terminal, conectarse al
mismo motor que va a usar la app y ver qué responde ahí. La app va a
hacer exactamente lo mismo (un handshake JDBC), así que probando acá
descartás la mayor parte de las dudas de configuración.

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

Windows: instalador "MariaDB Server" o "MariaDB Client" desde mariadb.org
(agregá `C:\Program Files\MariaDB …\bin` al PATH).

Conexión directa (TCP permitido hasta el servidor):

```
mariadb -h <HOST> -P <PUERTO> -u <USUARIO> -p
# te pide la contraseña interactivamente
```

Sin túnel:

```
mariadb -h <HOST> -P <PUERTO> -u <USUARIO> -p<CONTRASEÑA> <NOMBRE_BD>
```

(Notá: `-p<CONTRASEÑA>` sin espacio es cómodo pero deja la contraseña en
el historial de la shell. Para logs o capturas, preferí `-p` y tipearla
interactiva.)

### Si sólo podés llegar al servidor por SSH

El puerto 3306 puede estar bloqueado en la red del laboratorio. Lo
normal es hacer un túnel local:

```
# túnel "puro": tu localhost:3306 -> puerto 3306 del servidor
ssh -L 3306:127.0.0.1:3306 usuario@<HOST>
```

Después, mientras el túnel esté abierto:

```
mariadb -h 127.0.0.1 -P 3306 -u <USUARIO> -p
```

Y en `spring.datasource.url` ponés:

```
jdbc:mariadb://127.0.0.1:3306/<NOMBRE_BD>
```

Variante "bastión": si el servidor de la BD es interno y sólo llegás vía
un servidor puente:

```
ssh -L 3306:<IP_INTERNA_BD>:3306 usuario@<IP_BASTION>
```

### Comandos útiles una vez adentro

```
SHOW DATABASES;           # listar bases que tu usuario puede ver
USE <NOMBRE_BD>;          # entrar
SHOW TABLES;              # debería estar VACÍO en este punto
SELECT VERSION();         # confirmar dialecto y versión
\q                        # salir
```

Si `SHOW TABLES` ya muestra tablas que vos no creaste, conviene avisarle
al docente antes de seguir: o el entorno no estaba limpio o estás
apuntando a una base compartida.

### Errores típicos desde la terminal

- `ERROR 1045 (28000): Access denied for user …` → usuario o contraseña
  mal transcritos.
- `ERROR 2003 (HY000): Can't connect to MySQL server on '<HOST>'` → IP o
  puerto mal, o tráfico bloqueado (probar el túnel SSH).
- `ERROR 1130 (HY000): Host '<HOST>' is not allowed to connect` → el
  motor tiene `bind-address` o reglas de `user@host` que limitan desde
  qué IP se acepta la conexión; pedirle al docente que revise.

### Una advertencia operativa

Pegar contraseñas reales en terminales compartidas, capturas de pantalla
o apuntes queda en evidencia. Si vas a versionar algo, usá variables de
entorno (`SPRING_DATASOURCE_PASSWORD`) o perfiles separados; eso sí lo
veremos en otro práctico.
