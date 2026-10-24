# Panadería — aplicación Java para IntelliJ IDEA

Aplicación de escritorio basada en `panaderia.docx`, escrita en **Java 17 y Swing**, con interfaz en español. Incluye código fuente, JAR ejecutable, configuración Maven y scripts para Windows. No requiere bibliotecas externas para compilar con el JDK.

## 1. Abrir y ejecutar en IntelliJ IDEA

1. Extrae **todo** el archivo `Panaderia-IntelliJ.zip`.
2. En IntelliJ IDEA, elige **File → Open** y selecciona `Panaderia/pom.xml`. Ábrelo como proyecto Maven.
3. En **File → Project Structure → Project**, selecciona **JDK 17 o superior**. Si no tienes uno, usa **Download JDK**, versión 17, proveedor Eclipse Temurin.
4. Espera a que IntelliJ termine de importar el proyecto.
5. Abre `src/main/java/pe/panaderia/Main.java` y pulsa el triángulo verde junto a `main`, opción **Run 'Main.main()'**.

También se incluye la configuración de ejecución **Panaderia**. Si IntelliJ solicita un módulo, selecciona el módulo importado `panaderia`.

## 2. Usuarios iniciales

Estas cuentas se crean automáticamente al iniciar con una carpeta de datos nueva.

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | `Admin123` |
| Empleado | `empleado` | `Empleado123` |
| Cajero | `cajero` | `Cajero123` |

Las contraseñas distinguen mayúsculas y minúsculas. El administrador puede crear cuentas personales, editar roles y cambiar contraseñas desde **Usuarios**. Al modificar un usuario, dejar la contraseña vacía conserva la anterior.

## 3. Ejecutar el JAR en Windows

Con Java 17 o superior instalado, haz doble clic en **`ejecutar.bat`**. El script busca Java en `JAVA_HOME`, en `PATH` y en las carpetas habituales de instalación, incluida `.jdks` de IntelliJ.

También puedes abrir una terminal en la carpeta `Panaderia` y ejecutar:

```powershell
java -jar target/panaderia.jar
```

El JAR ya está compilado. Para recompilar tras modificar el código:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\compilar.ps1
```

Con Maven instalado, también se admite `mvn clean package`. La primera importación Maven puede necesitar internet para descargar sus plugins; `compilar.ps1` solo necesita el JDK.

### Versión Windows con Java incluido

La entrega adicional `Panaderia-Windows.zip` contiene una aplicación portable para Windows de 64 bits. Extrae el ZIP completo y abre **`Panaderia/Panaderia.exe`**. Conserva las carpetas `app` y `runtime` junto al ejecutable. Esta versión incluye su propio Java y comparte los datos locales con el JAR.

Para regenerarla con un JDK completo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\empacar-portable.ps1 -Destino .\mi-entrega
```

## 4. Recorrido de uso

### Administrador

- **Usuarios:** registrar, modificar y eliminar empleados, cajeros y administradores. No permite eliminarse ni quitarse el rol de administrador desde la sesión propia.
- **Productos:** agregar, buscar, modificar y eliminar productos con precio unitario en soles.
- **Resumen y reportes:** ver ventas de hoy, semana, mes y año; filtrar el historial por rango de fechas; agrupar totales por día, semana, mes o año; consultar boletas y exportar CSV.

### Empleado

1. Busca y selecciona un producto.
2. Indica la cantidad y pulsa **Agregar al pedido**. Añadir el mismo producto acumula la cantidad; **Quitar producto** elimina su línea.
3. Elige una de las **cuentas 1 a 5** disponibles.
4. Pulsa **Enviar a caja** e indica al cliente el número de cuenta y el total.
5. Consulta **Mis pedidos** para ver el estado, detalle y eventual motivo de rechazo.

### Cajero

1. En **Caja · Pedidos**, selecciona una cuenta y revisa el detalle.
2. Pulsa **Confirmar**, o **Rechazar** e indica el motivo.
3. Con el pedido confirmado, puedes generar la boleta con estado **Pendiente de pago**.
4. Pulsa **Registrar pago**, escribe el importe recibido y revisa el vuelto.
5. El pedido pasa a **Pedidos pagados**, queda guardado y su cuenta vuelve a estar disponible.
6. Guarda la boleta en TXT o utiliza **Imprimir / PDF**. Para PDF en Windows, selecciona la impresora **Microsoft Print to PDF**.

Para probar el recorrido completo en este equipo: entra como empleado, crea un pedido, cierra sesión, ingresa como cajero y cóbralo; luego entra como administrador para ver el reporte.

## 5. Decisiones tomadas a partir del documento

- Los clientes no tienen cuentas de acceso. Son atendidos por empleados.
- Estados: `Pendiente → Confirmado → Pagado`. Un pedido pendiente o confirmado puede pasar a `Rechazado`. Los pagados no pueden cobrarse de nuevo ni rechazarse.
- Una cuenta queda ocupada hasta que se paga o rechaza el pedido. El número permanente de pedido y de boleta es independiente de la cuenta reutilizable.
- Los pedidos rechazados se retiran de la lista de activos y permanecen consultables con su motivo.
- Los pedidos conservan el nombre del empleado, los productos y sus precios originales, aunque después se modifique el catálogo o se elimine el usuario.
- La “ganancia” solicitada se interpreta como **ventas cobradas**, porque el documento no define costos ni gastos para calcular utilidad neta. No se agregan impuestos no especificados.
- Los reportes usan la **fecha de pago** y ambos extremos del rango son inclusivos. Las semanas van de lunes a domingo, con numeración ISO.
- Las boletas son comprobantes internos imprimibles; no incluyen emisión electrónica SUNAT, que no forma parte de los requisitos del documento.

## 6. Guardado de datos

Los cambios se guardan automáticamente en:

```text
<carpeta del usuario>/.panaderia-app/panaderia.dat
```

En Windows, normalmente: `C:\Users\TU_USUARIO\.panaderia-app\panaderia.dat`. La ruta exacta aparece al pie de la aplicación. Cerrar sesión o reiniciar conserva usuarios, productos y pedidos. Las contraseñas se almacenan derivadas con PBKDF2 y una sal aleatoria.

Esta entrega funciona **localmente en un equipo**, alternando las sesiones de los tres roles. El archivo se bloquea para evitar dos instancias simultáneas sobre los mismos datos. No hay servidor ni sincronización entre varias computadoras.

Para respaldar: cierra la aplicación y copia `panaderia.dat` a otra carpeta. Para restaurar: con la aplicación cerrada, reemplaza el archivo por tu respaldo.

Para usar datos separados durante una demostración:

```powershell
java "-Dpanaderia.data=./datos-demo" -jar target/panaderia.jar
```

En IntelliJ puedes colocar `-Dpanaderia.data=./datos-demo` en **Run → Edit Configurations → VM options**.

## 7. Pruebas

La suite ejecuta comprobaciones reales de permisos, cálculo monetario, transiciones de pedidos, límites de cuentas, reportes por fecha de pago, persistencia, bloqueo y fallos de escritura:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\compilar.ps1 -Probar
```

También puedes ejecutar el `main` de `src/test/java/pe/panaderia/BakeryServiceTest.java` desde IntelliJ. Son pruebas autónomas, sin JUnit; `mvn test` no las ejecuta automáticamente. Usan una carpeta temporal propia.

El `main` de `src/test/java/pe/panaderia/ui/UiSmokeTest.java` realiza un recorrido gráfico automático: ingreso, creación del pedido, confirmación, pago y consulta del reporte. Las capturas del recorrido verificado se incluyen en `docs/capturas`.

## 8. Estructura

```text
Panaderia/
├── pom.xml                     Proyecto Maven para IntelliJ
├── ejecutar.bat                Iniciar el JAR
├── compilar.bat / compilar.ps1  Compilar y verificar
├── target/panaderia.jar         Aplicación compilada
├── src/main/java/pe/panaderia/
│   ├── Main.java               Punto de entrada
│   ├── Domain.java             Usuarios, productos y pedidos
│   ├── BakeryService.java      Permisos y reglas de negocio
│   ├── Store.java              Persistencia local
│   ├── Passwords.java          Contraseñas
│   ├── Documents.java          Boletas y CSV
│   └── ui/                     Ventanas y paneles Swing
└── src/test/java/              Pruebas ejecutables
```
