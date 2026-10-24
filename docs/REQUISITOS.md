# Análisis de panaderia.docx

Se revisaron el texto y las siete imágenes del documento. Los dibujos representan el acceso, una referencia general de interfaz, la creación de pedidos, la caja, el historial de pagos y las pantallas de usuarios y productos.

| Requisito del documento | Implementación |
|---|---|
| Acceso simple mediante usuario y contraseña | Ventana de ingreso, sin registro público ni selector de rol |
| Redirigir según el rol | Paneles exclusivos de empleado, cajero o administrador |
| Los clientes no interactúan con el sistema | No existe un rol cliente |
| Cuenta personal por empleado | Gestión de usuarios con nombres únicos y roles |
| Buscar productos y elegir cantidad | Catálogo filtrable, cantidad y carrito de pedido |
| Quitar productos del pedido | Botón Quitar producto antes de enviar |
| Asignar una cuenta de 1 a 5 | Selector de cuentas disponibles, sin duplicados activos |
| Enviar pedido al cajero e informar total | Registro persistente, mensaje con cuenta y total; aparece en Caja |
| Confirmar o rechazar pedido | Acciones con validación de estado y motivo de rechazo |
| Generar boleta | Boleta numerada, guardado TXT e impresión/PDF |
| Marcar el pedido como pagado | Registro del importe recibido, cálculo de vuelto y fecha de pago |
| Sección de pedidos pagados | Historial con detalle, fecha, hora y total |
| Registrar, modificar y eliminar usuarios | Panel Usuarios del administrador |
| Agregar, modificar y eliminar productos | Panel Productos del administrador |
| Ver ganancias diarias, semanales, mensuales y anuales | Indicadores y tabla de ventas cobradas agrupadas por período |
| Filtrar desde/hasta y generar total | Filtros inclusivos por fecha de pago y suma de resultados |

## Criterios funcionales

- Se utiliza Java Swing por su ejecución directa en IntelliJ y por no requerir dependencias gráficas adicionales.
- La interfaz sigue la organización de los bocetos, con formularios y tablas adaptados al uso de escritorio.
- El alcance de esta entrega es local, en un equipo. Cambiar de usuario permite recorrer empleado → cajero → administrador sobre los mismos datos.
- Los importes se calculan con `BigDecimal`, en soles y con dos decimales.
- Las cuentas se reutilizan; los números de pedido y boleta no se reutilizan.
- Las ventas pagadas conservan su información histórica al modificar usuarios o productos.
- Los rechazos quedan fuera de los activos y conservan su motivo para consulta.
- Los reportes suman ventas cobradas, porque no se proporcionan costos para calcular utilidad neta.
- La boleta interna no realiza trámites ni envíos a servicios fiscales.

## Comprobación manual sugerida

1. Administrador: crear un producto y un empleado personal.
2. Empleado: añadir dos productos, quitar uno, asignar cuenta y enviar a caja.
3. Cajero: revisar el total, confirmar y generar una boleta pendiente de pago.
4. Cajero: pagar con un importe superior, comprobar el vuelto y el historial.
5. Empleado: verificar que la cuenta volvió a quedar disponible.
6. Administrador: consultar el reporte del día, cambiar agrupación y exportar CSV.
7. Cerrar y abrir la aplicación: comprobar que permanecen los datos.

Las pruebas automatizadas de negocio y el recorrido gráfico se encuentran en `src/test/java`.
