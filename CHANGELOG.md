# Changelog
Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y [versionado semántico](https://semver.org/lang/es/).

## [Sin publicar]
### Añadido
- Los clientes hacen pedidos desde el menú (carrito y selección de mesa) y siguen su estado en "Mis pedidos".
- Fotos de los platos: el administrador las sube (JPG, PNG o WEBP, máx. 5 MB) y el menú las muestra.
- Migración V6: `pedido.cliente_id`; `mesero_id` se asigna cuando el personal atiende el pedido.
- Pipeline CI/CD en GitHub Actions (EC-06): build, pruebas con cobertura JaCoCo y SonarCloud opcional, Gitleaks, Trivy, imágenes en GHCR y despliegue blue-green con reversión automática por entorno (desarrollo, staging con k6 y ZAP, producción con aprobación manual y etiqueta de versión). Dependabot para Maven, npm y Actions.
### Cambiado
- Spring Boot 3.3.4 → 3.5.16, Tomcat 10.1.59, Jackson 2.21.8 y PostgreSQL JDBC 42.7.14 (CVE críticos y altos detectados por Trivy).
- Imagen del frontend sobre `nginx:1.30.7-alpine`; nginx resuelve el backend dinámicamente para permitir el cambio blue-green sin reiniciar.
### Corregido
- Reservas: el personal ve las próximas reservas, no solo las del día actual.
- Reservas: el paso de confirmar la reserva ahora es visible después de consultar disponibilidad.

## [1.0.0] - 2026-11-06 (Línea base LB-01)
### Añadido
- Autenticación con JWT y roles cliente, mesero y administrador.
- CRUD de platos y categorías del menú digital.
- Registro de pedidos con estados y actualización en tiempo real.
- Reservas por fecha y hora con validación de disponibilidad.
- Apertura y cierre de caja y reporte de ventas diario en PDF.
- Pruebas unitarias y de integración de los cinco módulos.
### Corregido
- Solapamiento de reservas en la misma mesa y franja horaria.
