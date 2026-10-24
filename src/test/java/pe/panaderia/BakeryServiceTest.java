package pe.panaderia;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import static pe.panaderia.Domain.*;

/** Pruebas de integración sin bibliotecas externas. Ejecutar compilar.ps1 -Probar. */
public final class BakeryServiceTest {
    private static int assertions;
    @FunctionalInterface interface Checked { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("panaderia-test-");
        MutableClock clock = new MutableClock(Instant.parse("2026-12-31T23:59:00Z"));
        long paidId;
        try {
            try (Store store = new Store(directory)) {
                BakeryService service = new BakeryService(store, clock);
                fails(service::products, "No hay acceso sin sesión");
                fails(() -> service.login("admin", "incorrecta".toCharArray()), "Se rechaza contraseña incorrecta");
                service.login("ADMIN", "Admin123".toCharArray());
                check(service.users().size() == 3, "Se crean los tres roles");
                check(!new String(Files.readAllBytes(directory.resolve("panaderia.dat")), java.nio.charset.StandardCharsets.ISO_8859_1)
                        .contains("Admin123"), "El archivo no contiene la contraseña en texto plano");
                fails(() -> service.deleteUser(1), "No se puede eliminar el administrador conectado");
                fails(() -> service.saveUser(1L, "admin", "Admin", Role.EMPLEADO, new char[0]), "No se puede quitar su propio rol");
                fails(() -> service.saveUser(null, "ADMIN", "Otro", Role.CAJERO, "Clave123".toCharArray()), "Usuarios únicos");
                service.saveUser(null, "maria", "María", Role.EMPLEADO, "Clave123".toCharArray());
                long maria = service.users().stream().filter(u -> u.username().equals("maria")).findFirst().orElseThrow().id();
                service.saveUser(maria, "maria", "María López", Role.EMPLEADO, new char[0]);
                service.saveProduct(null, "Pan de prueba", new BigDecimal("0.10"));
                long bread = service.products().stream().filter(p -> p.name().equals("Pan de prueba")).findFirst().orElseThrow().id();
                fails(() -> service.saveProduct(null, "Pan de prueba", BigDecimal.ONE), "Productos únicos");
                fails(() -> service.saveProduct(null, "Inválido", new BigDecimal("0.001")), "Máximo dos decimales");
                fails(() -> service.saveProduct(null, "Inválido", BigDecimal.ZERO), "Precio positivo");
                fails(() -> service.createOrder(1, Map.of(bread, 1)), "Administrador no crea pedidos de empleado");

                service.login("maria", "Clave123".toCharArray());
                check(service.currentUser().name().equals("María López"), "Editar conserva contraseña y tildes");
                fails(service::users, "Empleado no administra usuarios");
                fails(() -> service.saveProduct(null, "Ajeno", BigDecimal.ONE), "Empleado no modifica catálogo");
                fails(() -> service.createOrder(0, Map.of(bread, 1)), "Cuenta válida");
                fails(() -> service.createOrder(1, Map.of()), "Pedido no vacío");
                fails(() -> service.createOrder(1, Map.of(bread, -1)), "Cantidad positiva");
                fails(() -> service.createOrder(1, Map.of(bread, 10000)), "Cantidad máxima");
                Order first = service.createOrder(1, Map.of(bread, 3, 2L, 2));
                paidId = first.id();
                check(first.total().compareTo(new BigDecimal("8.30")) == 0, "Cálculo decimal exacto");
                check(!service.availableAccounts().contains(1), "Cuenta ocupada");
                fails(() -> service.createOrder(1, Map.of(bread, 1)), "No se duplica una cuenta activa");
                fails(() -> service.confirm(first.id()), "Empleado no confirma pedidos");
                service.login("empleado", "Empleado123".toCharArray());
                check(service.orders().isEmpty(), "Cada empleado ve sus propios pedidos");
                Order rejected = service.createOrder(2, Map.of(2L, 1));
                service.createOrder(3, Map.of(2L, 1));
                service.createOrder(4, Map.of(2L, 1));
                service.createOrder(5, Map.of(2L, 1));
                check(service.availableAccounts().isEmpty(), "Las cinco cuentas se pueden ocupar");

                service.login("cajero", "Cajero123".toCharArray());
                check(service.orders().size() == 5, "Cajero ve pedidos de todos los empleados");
                fails(service::users, "Cajero no administra usuarios");
                fails(() -> service.pay(first.id(), new BigDecimal("10.00")), "No se paga sin confirmar");
                fails(() -> service.receipt(first.id()), "No se genera boleta pendiente de confirmación");
                service.confirm(first.id());
                check(service.receipt(first.id()).contains("PENDIENTE DE PAGO"), "Boleta confirmada indica pago pendiente");
                fails(() -> service.confirm(first.id()), "No se confirma dos veces");
                fails(() -> service.pay(first.id(), new BigDecimal("8.29")), "No se admite pago insuficiente");
                clock.instant = Instant.parse("2027-01-01T00:01:00Z");
                Order paid = service.pay(first.id(), new BigDecimal("10.00"));
                check(paid.change().compareTo(new BigDecimal("1.70")) == 0, "Vuelto correcto");
                check(service.receipt(first.id()).contains("B001-00000001"), "Boleta numerada");
                fails(() -> service.pay(first.id(), new BigDecimal("10.00")), "No se cobra dos veces");
                fails(() -> service.reject(first.id(), "Error"), "No se rechaza un pedido pagado");
                fails(() -> service.reject(rejected.id(), "  "), "Rechazo con motivo");
                service.reject(rejected.id(), "Cliente canceló");
                check(service.paidBetween(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31)).isEmpty(), "Reporte usa fecha de pago, no creación");
                check(service.paidBetween(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 1)).size() == 1, "Fechas inclusivas y solo pagados");
                fails(() -> service.paidBetween(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 1, 1)), "Rango invertido rechazado");
                check(Documents.csv(List.of(paid)).contains("8.30"), "Exportación incluye importe cobrado");

                service.login("admin", "Admin123".toCharArray());
                service.saveProduct(bread, "Pan nuevo", new BigDecimal("9.00"));
                service.deleteProduct(bread);
                service.deleteUser(maria);
                check(service.orders().stream().filter(o -> o.id() == first.id()).findFirst().orElseThrow().total().equals(first.total()), "Editar y eliminar conserva precios históricos");
                check(service.receipt(first.id()).contains("María López"), "Eliminar usuario conserva autor histórico");
                fails(() -> { try (Store ignored = new Store(directory)) { } }, "Bloqueo de segunda instancia");

                service.login("empleado", "Empleado123".toCharArray());
                check(service.availableAccounts().equals(List.of(1, 2)), "Pago y rechazo liberan cuentas");
                Order next = service.createOrder(1, Map.of(2L, 1));
                check(next.id() > rejected.id(), "Numeración permanente aunque se reutilice cuenta");
                service.logout();
                fails(service::orders, "Cerrar sesión revoca acceso");
            }
            try (Store reopened = new Store(directory)) {
                BakeryService restored = new BakeryService(reopened, clock);
                restored.login("admin", "Admin123".toCharArray());
                check(restored.orders().size() == 6, "Persistencia de todos los pedidos");
                check(restored.receipt(paidId).contains("Vuelto: S/ 1.70"), "Persistencia de pago y vuelto");
                fails(() -> restored.login("maria", "Clave123".toCharArray()), "Usuario eliminado no puede ingresar");
            }
            Path failureDir = directory.resolve("fallo-escritura");
            try (Store store = new Store(failureDir)) {
                BakeryService service = new BakeryService(store, clock);
                service.login("admin", "Admin123".toCharArray());
                int before = service.products().size();
                Files.delete(failureDir.resolve("panaderia.dat"));
                Files.createDirectory(failureDir.resolve("panaderia.dat"));
                Files.writeString(failureDir.resolve("panaderia.dat/obstaculo.txt"), "Prueba de error de escritura");
                fails(() -> service.saveProduct(null, "No guardado", BigDecimal.ONE), "Se informa fallo de guardado");
                check(service.products().size() == before, "Un fallo de disco no modifica el estado en memoria");
            }
            Path corrupt = directory.resolve("corrupto");
            Files.createDirectories(corrupt);
            Files.writeString(corrupt.resolve("panaderia.dat"), "contenido inválido");
            try (Store store = new Store(corrupt)) {
                fails(() -> new BakeryService(store, clock), "No se sobrescribe un archivo corrupto");
                check(Files.readString(corrupt.resolve("panaderia.dat")).equals("contenido inválido"), "Archivo original preservado");
            }
            System.out.println("OK: " + assertions + " comprobaciones de negocio y persistencia.");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }

    private static void fails(Checked action, String message) throws Exception {
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException | IOException expected) { assertions++; return; }
        throw new AssertionError(message);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
