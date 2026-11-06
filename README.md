# SIGR – Sistema Integral de Gestión de Restaurante

Aplicación web para gestionar pedidos, reservas, menú, caja y reportes.

| Módulo | Elemento de configuración | Funcionalidad |
|---|---|---|
| Autenticación | EC-01 | Login con JWT; roles cliente, mesero y administrador |
| Menú digital | EC-02 | CRUD de platos y categorías; menú público |
| Pedidos | EC-03 | Registro por mesa y seguimiento en tiempo real (WebSocket) |
| Reservas | EC-04 | Reservas por fecha y hora con validación de disponibilidad |
| Caja y reportes | EC-05 | Apertura, cobro, cierre de caja y reporte diario en PDF |

**Stack:** React 18 + Vite + Material UI · Java 17 + Spring Boot 3 · PostgreSQL 15 + Flyway · Docker Compose.

## Requisitos
- Git, Docker 24+ y Docker Compose
- (Desarrollo local) Java 17, Maven 3.9, Node.js 20

## Ejecución rápida
```bash
git clone https://github.com/equipoX/sigr.git
cd sigr
cp .env.example .env        # edite JWT_SECRET y las contraseñas
docker compose up -d --build
```

- Frontend: http://localhost:5173
- API: http://localhost:8080/api
- Salud: http://localhost:8080/api/actuator/health

## Usuario inicial
`admin@sigr.local` (la contraseña se define en `.env` con `SIGR_ADMIN_PASSWORD`).
Desde **Usuarios** el administrador crea los meseros; los clientes se registran solos.

## Desarrollo local
```bash
docker compose up -d db                       # solo la base de datos
cd backend && mvn spring-boot:run             # API en :8080
cd frontend && npm install && npm run dev     # UI en :5173 (proxy /api → :8080)
```

## Pruebas
```bash
cd backend && mvn test       # unitarias + integración (H2 con migraciones Flyway)
cd frontend && npm test      # Vitest + Testing Library
cd frontend && npm run lint  # ESLint
```

## Endpoints principales
| Método | Ruta | Rol |
|---|---|---|
| POST | `/api/auth/login`, `/api/auth/registro` | público |
| GET | `/api/menu/categorias`, `/api/menu/platos` | público |
| POST/PUT/DELETE | `/api/menu/categorias/**`, `/api/menu/platos/**` | ADMIN |
| GET/POST | `/api/pedidos` · PATCH `/api/pedidos/{id}/estado` | MESERO, ADMIN |
| GET | `/api/reservas/disponibilidad` · POST `/api/reservas` · GET `/api/reservas/mias` | autenticado |
| GET | `/api/reservas?fecha=` | MESERO, ADMIN |
| GET `/api/caja/actual` · POST `/api/caja/abrir`, `/pagos`, `/cerrar` · GET `/api/caja/reporte` | | ADMIN |
| GET/POST | `/api/usuarios` | ADMIN |

Tablero en tiempo real: STOMP sobre `ws://<host>/api/ws`, tópico `/topic/pedidos`.

## Convenciones
- Ramas: `main` (estable, protegida), `develop`, `feature/<id>-<descripcion>`, `hotfix/<id>`.
- Commits: Conventional Commits (`feat(pedidos): ...`, `fix(reservas): ...`).
- Versiones: SemVer. Línea base inicial **LB-01 = v1.0.0**.

Documentación adicional en [`docs/`](docs/).
