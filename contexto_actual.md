# Matchpoint — Contexto técnico del proyecto

*Última actualización: 9 de septiembre de 2026*

## 1. Propósito

Proyecto de portfolio para postular a una vacante de **backend developer jr**. Objetivo: demostrar conocimiento práctico de un stack amplio (Spring Boot, Django, microservicios, Kafka, Docker, Kubernetes, Postgres, MongoDB, Grafana, Prometheus, Swagger, testing con Mockito, SonarQube, CI/CD, Git/GitHub, React, Nginx, logging estructurado) mediante una app funcional, no ejercicios sueltos.

## 2. Dominio de la app

**Matchpoint**: conecta jugadores de fútbol con grupos que necesitan sumar gente, filtrando por ubicación, nivel y posición. Arrancamos acotado a **fútbol** (categorías F5/F7/F8/F11), con la idea de eventualmente extender a otros deportes.

Decisión de diseño clave: la **posición es informativa/sugerida, no un hard constraint**. Un grupo con cupo libre acepta jugadores aunque no cubran exactamente la posición que falta (evita que el matching sea demasiado difícil). El cupo real que bloquea es la capacidad total del grupo (`maxPlayers` / `currentPlayers`).

## 3. Infraestructura

- **VPS**: Oracle Cloud Always Free, shape `VM.Standard.A1.Flex` (ARM Ampere), redimensionado a **2 OCPU / 12 GB RAM** (era 1 OCPU/6GB). Boot volume 50GB. Se agregó swap de 2GB como colchón.
  - Importante: Oracle bajó el límite Always Free de 4 OCPU/24GB a 2 OCPU/12GB a mediados de 2026. El resize se hizo in-place (sin recrear la instancia), vía consola OCI, editando el shape.
- **SO**: Ubuntu (22.04/24.04).
- **Desarrollo**: 100% local en Windows (PC), con Docker Desktop. Las imágenes se buildean y suben al VPS recién cuando un servicio está funcionalmente terminado — el VPS no se usa como entorno de desarrollo activo.
- **Arquitectura de red**: en OCI hay que abrir puertos en dos capas — Security List/NSG (consola web) **y** iptables interno de la instancia. Ambas se configuraron para 22/80/443/6443.

## 4. Repos GitHub (multi-repo)

- `groups-service` — Spring Boot + Postgres (**en desarrollo activo**)
- `players-service` — Django DRF + MongoDB (pendiente, no iniciado)
- `notifications-service` — Spring Boot liviano + Kafka consumer (pendiente, no iniciado)
- `frontend` — React (pendiente)
- `infra` — manifests de k8s, docker-compose de infraestructura compartida (Kafka, Prometheus, Grafana, Loki, Nginx) (pendiente)

**Flujo de trabajo Git**: GitHub Flow (no Gitflow completo — se consideró sobre-ingeniería para un proyecto individual). Rama por feature (`feature/...`) → PR contra `main` → merge. Branch protection en `main` pendiente de activar (`Require pull request before merging`; se sumará `Require status checks` cuando haya CI). Commits siguiendo convención **Conventional Commits** (`feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`).

### Convención de flujo de trabajo por paso de implementación (aplica a todos los repos)
Antes de escribir código de cualquier paso nuevo (DTO, service, controller, test, etc.):
1. Crear rama: `feature/{descripcion-corta}` desde `main` actualizado.
2. Implementar y verificar el paso.
3. Commit con Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `chore:`, `docs:`).
4. Push + PR contra `main` (branch protection pendiente de activar).

Esto se hace **automáticamente al arrancar cada paso nuevo**, sin que haga falta pedirlo explícitamente.

## 5. Estado actual de `groups-service`

### Setup técnico
- Spring Boot 4.1.1 (Initializr), Maven, **Java 21** (nota: la máquina tiene Java 22 instalado también, se usa 21 puntualmente para este proyecto por ser LTS).
- Dependencias: Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Lombok, DevTools, **springdoc-openapi-starter-webmvc-ui** (Swagger, ya integrado).
- Postgres 16 corriendo local vía `docker-compose.yml` (no confundir con el futuro `infra` repo). Timezone forzado a UTC tanto en el contenedor (`TZ=UTC`, `PGTZ=UTC`) como en la JVM.
- `spring.jpa.hibernate.ddl-auto=update` — **temporal**, pendiente migrar a Flyway/Liquibase para versionado real de esquema.
- **JaCoCo 0.8.12** integrado en el `pom.xml` para reporte de coverage (`target/site/jacoco/index.html` tras `mvn clean test`), con un piso mínimo de **40% line coverage** — se va a ir subiendo a medida que crezca la cobertura real.

### Estructura de paquetes
```
com.matchpoint.groups_service
├── domain
│   ├── Category, Group, PositionSlot, PositionTemplate, JoinRequest
│   └── enums/ (Position, GroupStatus, JoinRequestStatus)
├── repository (Spring Data JPA)
├── service (CategoryService, GroupService, JoinRequestService — todos implementados)
├── controller (CategoryController, GroupController implementados; JoinRequestController pendiente)
├── dto
│   ├── request/ (CategoryRequest, GroupRequest, JoinRequestRequest)
│   └── response/ (CategoryResponse, GroupResponse, JoinRequestResponse)
└── exception
    ├── ResourceNotFoundException  (usada en GroupService y JoinRequestService)
    ├── BusinessRuleException      (usada en JoinRequestService: grupo lleno, JoinRequest ya procesada)
    └── GlobalExceptionHandler     (@RestControllerAdvice, respuestas con ProblemDetail / RFC 7807)
```

### Modelo de dominio (Postgres, tablas confirmadas: `categories`, `category_surfaces`, `groups`, `position_templates`, `position_slots`, `join_requests`)

- **Category**: nombre (F5/F7/F8/F11), `playersPerTeam`, `allowedSurfaces` (`@ElementCollection`).
- **PositionTemplate**: plantilla por categoría (posición + cantidad esperada). Se usa para auto-generar `PositionSlot` al crear un `Group` — **ya implementado** en `GroupService.createGroup`.
- **Group**: incluye `maxPlayers`/`currentPlayers` (capacidad real que bloquea el ingreso) y `minLevel`/`maxLevel` (opcionales). Métodos de dominio: `hasAvailableSpot()`, `isFull()`. Al aceptar una `JoinRequest` que llena el cupo, el `Group` pasa automáticamente a `GroupStatus.FULL` (decisión tomada en la implementación de `JoinRequestService.acceptJoinRequest`).
- **PositionSlot**: informativo (muestra qué posiciones faltan), **no bloquea** el ingreso de un jugador. Método `hasAvailableSlot()`.
- **JoinRequest**: `playerId` es un `Long` plano, **no** una relación JPA (referencia cruzada a `players-service`). `requestedPosition` es opcional — si viene null, la solicitud entra como "comodín".
- Enums con `@Enumerated(EnumType.STRING)` en todos los casos.

### Lógica de negocio de `JoinRequest` — **implementada** (`JoinRequestService`)
Flujo: toda `JoinRequest` se crea en estado `PENDING` (requiere autorización del dueño del grupo para pasar a `ACCEPTED`/`REJECTED` vía un endpoint separado — no hay auto-aceptación).

1. Si `group.isFull()` → `acceptJoinRequest` lanza `BusinessRuleException` (409), sin importar posición.
2. Si hay lugar → incrementa `currentPlayers`; si con eso se llega a `maxPlayers`, el `Group` pasa a `FULL`.
3. Si `requestedPosition` no es null y ese `PositionSlot.hasAvailableSlot()` → incrementa también `filledSlots` de ese slot.
4. Si `requestedPosition` es null o esa posición ya está cubierta → se acepta igual como comodín, sin tocar `PositionSlot`.
5. Si la `JoinRequest` ya fue procesada antes (no está `PENDING`) → `BusinessRuleException` (evita doble accept/reject).
6. `rejectJoinRequest` es simple: marca `REJECTED`, no toca `Group` ni `PositionSlot`.

**⚠️ Pendiente crítico anotado**: no existe todavía capa de autenticación/autorización en el proyecto (no hay JWT/sesiones, `players-service` ni siquiera arrancó). Por lo tanto, `acceptJoinRequest`/`rejectJoinRequest` **no verifican que quien llama sea el dueño del grupo** — la regla de negocio está definida y documentada, pero la validación de identidad queda pendiente para cuando exista un mecanismo de auth real (probablemente JWT emitido por `players-service` o un API Gateway).

### API
- Versionado en URL: `/api/v1/...`.
- `POST /api/v1/categories` — implementado y probado manualmente.
- `POST /api/v1/groups` — implementado, devuelve `201` + header `Location: /api/v1/groups/{id}` (vía `ServletUriComponentsBuilder`), probado manualmente con curl (`-v` confirma el header).
- **Pendiente**: `JoinRequestController`, con la siguiente forma acordada:
  - `POST /api/v1/groups/{groupId}/join-requests` → crea la solicitud (201 + `Location`). Anidado bajo `Group` porque una `JoinRequest` no existe sin su grupo padre.
  - `PATCH /api/v1/join-requests/{id}/accept` → acepta. Plano (no necesita `groupId` en la URL).
  - `PATCH /api/v1/join-requests/{id}/reject` → rechaza.
  - Se usa `PATCH` (no `PUT`/`POST`) por ser una transición de estado parcial, no reemplazo del recurso ni creación.
- Swagger UI disponible en `http://localhost:8080/swagger-ui.html`.
  - Limitación conocida: no genera ejemplos válidos para `Map<Enum, ...>` (placeholders `additionalProp1`) — hay que editar las keys a mano (afecta `CategoryRequest.positionCounts`).
- Logging estructurado con SLF4J/Logback en todos los services (`info` para eventos de negocio, `debug` para detalle técnico).

### Manejo de errores
- Package `exception` con `ResourceNotFoundException`, `BusinessRuleException` y `GlobalExceptionHandler` (`@RestControllerAdvice`).
- Respuestas de error siguen **RFC 7807 (`ProblemDetail`)**.
- Mapeo de excepciones a status HTTP:
  - `ResourceNotFoundException` → 404 (en uso: `GroupService`, `JoinRequestService`)
  - `BusinessRuleException` → 409 (en uso: `JoinRequestService` — grupo lleno, solicitud ya procesada)
  - `MethodArgumentNotValidException` (falla de `@Valid`) → 400
  - `HttpMessageNotReadableException` (JSON malformado / enum inválido en Map) → 400
  - Catch-all `Exception` → 500, logueado con `log.error(...)`
- **Pendiente**: sumar test unitario del `GlobalExceptionHandler` en sí — hoy tiene 0% de coverage en JaCoCo porque ningún test lo ejercita directamente (aunque ya se ejercitan las excepciones que maneja, vía los service tests).

### Decisiones de arquitectura clave (para justificar en entrevista)
- **DTOs separados de entidades JPA**: Controller habla en DTOs, Service traduce entre DTOs y entidades.
- **Rich domain model**: lógica como `hasAvailableSlot()`, `hasAvailableSpot()`, `isFull()` vive dentro de las entidades.
- **Constructor injection** (no `@Autowired` en campos).
- **`@Transactional`** en operaciones multi-entidad (`createCategory`, `createGroup`, `acceptJoinRequest`).
- **Manejo centralizado de errores con `ProblemDetail` (RFC 7807)**.
- **Referencias por id, no por objeto anidado, en requests con relación FK**: `GroupRequest.categoryId` (Long) en vez de un `CategoryRequest` embebido — el service resuelve la entidad real vía repository y lanza `ResourceNotFoundException` si no existe. Mismo criterio aplicado consistentemente.
- **`Location` header en creación de recursos**: `GroupController` ya lo implementa (`ResponseEntity.created(location)`); pendiente aplicar el mismo patrón en `CategoryController` (anotado como refactor futuro).

### Testing y coverage
- Mockito + JUnit 5 (`@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`), assertions con **AssertJ**.
- **18 tests pasando en total**:
  - `CategoryServiceTest` — 3 tests (happy path, `ArgumentCaptor` de campos, edge case sin posiciones).
  - `GroupServiceTest` — 4 tests (happy path, `ArgumentCaptor` de `PositionSlot`, categoría sin templates, categoría inexistente → `ResourceNotFoundException`).
  - `JoinRequestServiceTest` — 11 tests (create happy path + grupo inexistente; accept: comodín, slot disponible, slot ya cubierto, llega a `maxPlayers`→`FULL`, grupo lleno→excepción, ya procesada→excepción, id inexistente→excepción; reject happy path).
- JaCoCo genera el reporte de coverage tras `mvn clean test`.

### Convención de idioma (aplica a todo el código)
- **Todo el código en inglés**: clases, métodos, variables, comentarios, logs, `@DisplayName`.
- **Español reservado exclusivamente a valores de negocio/dominio**: nombres de categorías (`F5`, `F7`), superficies (`cesped_sintetico`), etc.

## 6. Problemas conocidos y notas técnicas importantes

### Timezone (UTC) — recurrente, ojo con esto en cada entorno nuevo
El alias `America/Buenos_Aires` no es reconocido como timezone válido por el driver JDBC de Postgres. Hay que aplicar el fix por separado en **cada JVM que toque la base**:

1. **IntelliJ**: `-Duser.timezone=UTC` como JVM arg en la run configuration (no se propaga a Maven).
2. **`mvn test` desde terminal**: `maven-surefire-plugin` con `<argLine>@{argLine} -Duser.timezone=UTC</argLine>` en el `pom.xml` (`@{argLine}` con evaluación lazy, necesario para no pisar el `-javaagent` de JaCoCo).
3. **Docker Compose**: ya forzado con `TZ=UTC` y `PGTZ=UTC`.

**Riesgo pendiente a futuro (anotado, no resuelto):** el mismo patrón "cada JVM nueva necesita el flag" probablemente reaparezca en:
- El runner de **GitHub Actions** (CI).
- El **contenedor Docker** del propio `groups-service` cuando se dockerice.
- El **VPS de Oracle Cloud** si se corre algo fuera de Docker.
- Cualquier **servicio nuevo** (`players-service`, `notifications-service`).

Conclusión práctica: **cada vez que se agregue un entorno de ejecución nuevo, verificar explícitamente el timezone en vez de asumir que el fix de un entorno se propaga a los demás.**

## 7. Pendientes inmediatos (próxima sesión)

1. **`JoinRequestController`** — implementar con la forma de API ya acordada (ver sección 5, bloque "API"):
   - `POST /api/v1/groups/{groupId}/join-requests`
   - `PATCH /api/v1/join-requests/{id}/accept`
   - `PATCH /api/v1/join-requests/{id}/reject`
2. Test unitario de `GlobalExceptionHandler` (hoy sin cobertura directa).
3. Migrar `ddl-auto=update` a Flyway.
4. Activar branch protection en `main` en GitHub.
5. (Refactor menor, no urgente) Aplicar el patrón de `Location` header también en `CategoryController`, igual que ya está en `GroupController`.

## 8. Pendientes de mediano plazo (no iniciados)

- `players-service` (Django DRF + MongoDB): perfiles de jugador, reviews. **Nota**: este es también el candidato natural para resolver el pendiente crítico de autorización (dueño del grupo) mencionado en la sección 5, probablemente vía JWT.
- `notifications-service` (Spring Boot + Kafka consumer).
- Integración Kafka entre servicios (eventos: `GroupJoinRequested`, `PlayerAcceptedIntoGroup`, `GroupCompleted`).
- Frontend React.
- Dockerizar cada servicio (VPS es ARM/arm64, PC de desarrollo es x86_64 — build con `docker buildx --platform linux/arm64`; recordar fijar `TZ=UTC` en cada Dockerfile).
- Repo `infra`: docker-compose completo, luego migración a k3s.
- CI en GitHub Actions (build, test, SonarQube, build/push de imágenes) — recordar fijar timezone UTC en el runner/step.
- CD hacia el VPS (k3s).
- Observabilidad: Prometheus + Grafana + Loki.
- Documentación final (README por repo, diagramas de arquitectura, ADRs).

## 9. Notas de entorno local (Windows)

- Java 22 (global) + JDK 21 (para este proyecto, LTS).
- Python 3.14 (global) + Python 3.12 pendiente instalar para Django.
- Node 24 LTS, Docker 27.0.3, Git 2.43 — sin problemas de compatibilidad.
- PowerShell es la terminal usada; ojo con `curl` (alias de `Invoke-WebRequest` — usar `curl.exe` o `Invoke-RestMethod`).
