# Changelog
Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y [versionado semántico](https://semver.org/lang/es/).

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
