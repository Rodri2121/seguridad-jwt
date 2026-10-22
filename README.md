# NicaMarket API — Seguridad con Spring Security y JWT

API REST de **productos** y **categorías** de Comercial NicaMarket, S. A., protegida con
Spring Security y JSON Web Tokens (Práctica guiada n.º 9, Servicios Web, UAM).

- Java 21 · Spring Boot 4.1.1 (Spring Security 7) · Maven
- Paquete base: `com.example.factura`
- Rama de entrega: `seguridad-jwt`
- Datos en memoria (3 categorías y 4 productos iniciales; se reinician al detener la app).

## 1. Configurar el secreto JWT

Los tokens se firman con HS256 y una clave de 32 bytes que **no** está en el repositorio.

1. Genere una clave Base64 (cualquiera de las dos opciones):

   ```powershell
   $b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
   ```

   o ejecute la clase temporal `GenerarClave` de la guía (`SecureRandom` + `Base64`).

2. En IntelliJ: **Run → Edit Configurations → FacturaApplication → Environment variables** y agregue
   `JWT_SECRET_BASE64=<clave generada>`.
3. Reinicie la aplicación cada vez que cambie la variable.

Si la variable falta o la clave tiene menos de 32 bytes, la aplicación no inicia y muestra el motivo.

## 2. Ejecutar

Abra el proyecto en IntelliJ IDEA y ejecute `FacturaApplication`, o desde la terminal
(con la variable definida):

```bash
mvn spring-boot:run
```

Servidor en `http://localhost:8080`.

## 3. Usuarios de laboratorio

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `Admin123!` | ADMIN |
| `operador` | `Operador123!` | USER |

Credenciales solo para desarrollo; no deben usarse en producción.

## 4. Reglas de acceso

| Método | Ruta | Invitado | USER | ADMIN |
|--------|------|:-------:|:----:|:-----:|
| GET | `/api/publico/saludo` | ✅ | ✅ | ✅ |
| POST | `/api/auth/login` | ✅ | ✅ | ✅ |
| GET | `/api/auth/perfil` | 401 | ✅ | ✅ |
| GET | `/api/productos`, `/api/productos/{id}` | 401 | ✅ | ✅ |
| POST / PUT / DELETE | `/api/productos/**` | 401 | 403 | ✅ |
| GET | `/api/categorias`, `/api/categorias/{id}` | 401 | ✅ | ✅ |
| POST / PUT / DELETE | `/api/categorias/**` | 401 | 403 | ✅ |

Flujo: `POST /api/auth/login` → la respuesta trae `token` → enviarlo en cada solicitud con el
encabezado `Authorization: Bearer <token>`. El token vence en 30 minutos.

`GET /api/auth/perfil` devuelve el usuario y los roles leídos del JWT:

```json
{ "usuario": "operador", "roles": ["USER"] }
```

## 5. Pruebas con Postman

Importe `postman/NicaMarket-JWT.postman_collection.json`. La variable `baseUrl` ya apunta a
`http://localhost:8080`. Ejecute primero la carpeta **0. Autenticación** (guarda `tokenAdmin` y
`tokenUser` automáticamente) y después el resto, o toda la colección con el *Collection Runner*.
Cada solicitud incluye una prueba del código esperado y una descripción de por qué se obtiene.

| # | Solicitud | Token | Esperado | Motivo |
|---|-----------|-------|----------|--------|
| 1 | GET `/api/publico/saludo` | — | 200 | Ruta pública (`permitAll`) |
| 2 | POST `/api/auth/login` | — | 200 + JWT | Credenciales válidas |
| 3 | POST `/api/auth/login` (clave mala) | — | 401 | Falla la autenticación |
| 4 | GET `/api/productos` | — | 401 | Recurso protegido sin identidad |
| 5 | GET `/api/productos` | USER | 200 | USER puede consultar |
| 6 | POST `/api/productos` | USER | 403 | Autenticado pero sin rol ADMIN |
| 7 | GET `/api/productos` | modificado | 401 | La firma no coincide |
| 8 | POST `/api/productos` | ADMIN | 201 | ADMIN autorizado y datos válidos |
| C1 | GET `/api/categorias` | — | 401 | Recurso protegido sin token |
| C2–C3 | GET `/api/categorias[/1]` | USER | 200 | USER puede consultar |
| C4–C6 | POST / PUT / DELETE categorías | USER | 403 | Falta el rol ADMIN |
| C7–C9 | POST / PUT / DELETE categorías | ADMIN | 201 / 200 / 204 | ADMIN autorizado |
| C10 | POST `/api/categorias` | inválido | 401 | Token no válido |
| P1–P3 | GET `/api/auth/perfil` | —, USER, ADMIN | 401 / 200 / 200 | Requiere autenticación |

**401 vs 403:** 401 significa que la API no sabe quién eres (sin token, token alterado o vencido);
403 significa que sabe quién eres, pero tu rol no tiene permiso para esa operación.

## 6. Estructura

```
com/example/factura/
├── FacturaApplication.java
├── config/        SecurityConfig (usuarios, JWT, reglas por rol)
├── controllers/   AuthController, PublicoController, ProductoController, CategoriaController
├── dto/           LoginRequest, LoginResponse, PerfilResponse
├── services/      TokenService, ProductoService, CategoriaService
├── model/         Producto, Categoria
└── exception/     RecursoNoEncontradoException, ApiExceptionHandler
```

## 7. Notas de seguridad

- El secreto JWT y los tokens no se publican en el repositorio ni en capturas.
- `JwtDecoder` valida firma, vencimiento y emisor (`nicamarket-laboratorio`): leer el contenido de
  un JWT no basta para confiar en él, porque cualquiera puede decodificarlo y modificarlo; solo la
  firma demuestra que lo emitió esta API.
- CSRF está deshabilitado porque la API no usa sesiones ni cookies (`STATELESS` + Bearer Token).
- En producción conviene separar el emisor de tokens, usar un gestor de secretos, HTTPS y un
  proveedor de identidad especializado.
