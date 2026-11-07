# Manual de despliegue – SIGR

## 1. Obtener la versión de la línea base
```bash
git clone https://github.com/Jhonii10/sigr.git
cd sigr
git checkout v1.0.0
```

## 2. Configurar variables de entorno
```bash
cp .env.example .env
```
Defina como mínimo `DB_PASSWORD`, `JWT_SECRET` (32+ caracteres) y `SIGR_ADMIN_PASSWORD`.

## 3. Construir y levantar los servicios
```bash
docker compose up -d --build
docker compose ps
```
Servicios: `db` (PostgreSQL 15, puerto 5432), `backend` (Spring Boot, 8080) y `frontend` (Nginx, 5173).
Flyway aplica automáticamente las migraciones V1 a V5 al iniciar el backend.

## 4. Verificar
```bash
curl http://localhost:8080/api/actuator/health   # {"status":"UP"}
```
Ingrese a http://localhost:5173 con el administrador configurado.

## 5. Operación
| Acción | Comando |
|---|---|
| Ver logs | `docker compose logs -f backend` |
| Detener | `docker compose down` |
| Respaldar BD | `docker compose exec db pg_dump -U sigr sigr > respaldo.sql` |
| Restaurar BD | `cat respaldo.sql \| docker compose exec -T db psql -U sigr sigr` |
| Actualizar versión | `git fetch --tags && git checkout vX.Y.Z && docker compose up -d --build` |

## 6. Solución de problemas
- **El backend no inicia:** revise que `JWT_SECRET` tenga al menos 32 caracteres.
- **Error de Flyway "checksum mismatch":** se modificó una migración ya aplicada; restáurela y cree una nueva.
- **El tablero no se actualiza en vivo:** verifique que el proxy permita WebSocket en `/api/ws`.
