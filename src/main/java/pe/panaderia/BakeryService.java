package pe.panaderia;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

import static pe.panaderia.Domain.*;

/** Reglas de negocio y permisos, independientes de la interfaz gráfica. */
public final class BakeryService {
    private final Store store;
    private final Clock clock;
    private State state;
    private Long sessionUser;

    public BakeryService(Store store) throws IOException { this(store, Clock.systemDefaultZone()); }

    BakeryService(Store store, Clock clock) throws IOException {
        this.store = store;
        this.clock = clock;
        state = store.load();
        if (state == null) {
            state = new State(1, 4, 7, 1, List.of(
                    new User(1, "admin", "Administrador", Role.ADMINISTRADOR, Passwords.hash("Admin123".toCharArray())),
                    new User(2, "empleado", "Empleado", Role.EMPLEADO, Passwords.hash("Empleado123".toCharArray())),
                    new User(3, "cajero", "Cajero", Role.CAJERO, Passwords.hash("Cajero123".toCharArray()))),
                    List.of(new Product(1, "Pan francés", new BigDecimal("0.30")),
                            new Product(2, "Empanada", new BigDecimal("4.00")),
                            new Product(3, "Pan de molde", new BigDecimal("8.00")),
                            new Product(4, "Budín", new BigDecimal("3.50")),
                            new Product(5, "Croissant", new BigDecimal("2.50")),
                            new Product(6, "Alfajor", new BigDecimal("2.00"))), List.of());
            store.save(state);
        }
    }

    public User login(String username, char[] password) {
        sessionUser = null;
        User user = state.users().stream().filter(u -> u.username().equalsIgnoreCase(username.trim()))
                .findFirst().orElse(null);
        if (user == null || !Passwords.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        }
        sessionUser = user.id();
        return user;
    }

    public void logout() { sessionUser = null; }

    public User currentUser() {
        return state.users().stream().filter(u -> Objects.equals(u.id(), sessionUser)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Inicia sesión para continuar."));
    }

    private User require(Role... roles) {
        User user = currentUser();
        if (!Arrays.asList(roles).contains(user.role())) {
            throw new IllegalStateException("Tu rol no tiene permiso para realizar esta acción.");
        }
        return user;
    }

    public List<User> users() { require(Role.ADMINISTRADOR); return state.users(); }
    public List<Product> products() { currentUser(); return state.products(); }

    public List<Order> orders() {
        User user = currentUser();
        return state.orders().stream()
                .filter(o -> user.role() != Role.EMPLEADO || o.employeeId() == user.id())
                .sorted(Comparator.comparingLong(Order::id).reversed()).toList();
    }

    public List<Integer> availableAccounts() {
        require(Role.EMPLEADO);
        Set<Integer> busy = new HashSet<>();
        state.orders().stream().filter(BakeryService::isOpen).forEach(o -> busy.add(o.account()));
        return java.util.stream.IntStream.rangeClosed(1, 5).filter(i -> !busy.contains(i)).boxed().toList();
    }

    public static boolean isOpen(Order order) {
        return order.status() == Status.PENDIENTE || order.status() == Status.CONFIRMADO;
    }

    public void saveUser(Long id, String username, String name, Role role, char[] password) throws IOException {
        User actor = require(Role.ADMINISTRADOR);
        username = text(username, "Usuario", 30).toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z0-9._-]{3,30}")) {
            throw new IllegalArgumentException("Usuario: usa de 3 a 30 letras sin tildes, números, punto, guion o guion bajo.");
        }
        name = text(name, "Nombre", 80);
        Objects.requireNonNull(role, "Selecciona un rol.");
        User existing = id == null ? null : findUser(id);
        if (existing != null && actor.id() == id && role != Role.ADMINISTRADOR) {
            throw new IllegalArgumentException("No puedes quitarte el rol de administrador en tu sesión.");
        }
        for (User user : state.users()) {
            if (user.username().equalsIgnoreCase(username) && !Objects.equals(user.id(), id)) {
                throw new IllegalArgumentException("El nombre de usuario ya está registrado.");
            }
        }
        String hash = existing != null && password.length == 0 ? existing.passwordHash() : Passwords.hash(password);
        List<User> users = new ArrayList<>(state.users());
        if (id != null) users.removeIf(u -> u.id() == id);
        users.add(new User(id == null ? state.nextUser() : id, username, name, role, hash));
        commit(new State(1, state.nextUser() + (id == null ? 1 : 0), state.nextProduct(), state.nextOrder(),
                users, state.products(), state.orders()));
    }

    public void deleteUser(long id) throws IOException {
        User actor = require(Role.ADMINISTRADOR);
        findUser(id);
        if (actor.id() == id) throw new IllegalArgumentException("No puedes eliminar tu propia cuenta.");
        List<User> users = state.users().stream().filter(u -> u.id() != id).toList();
        commit(new State(1, state.nextUser(), state.nextProduct(), state.nextOrder(), users, state.products(), state.orders()));
    }

    private User findUser(long id) {
        return state.users().stream().filter(u -> u.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El usuario ya no existe."));
    }

    public void saveProduct(Long id, String name, BigDecimal price) throws IOException {
        require(Role.ADMINISTRADOR);
        name = text(name, "Producto", 80);
        price = amount(price);
        if (price.signum() <= 0 || price.compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero y no superar S/ 1 000 000.");
        }
        if (id != null) findProduct(id);
        for (Product product : state.products()) {
            if (product.name().equalsIgnoreCase(name) && !Objects.equals(product.id(), id)) {
                throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
            }
        }
        List<Product> products = new ArrayList<>(state.products());
        if (id != null) products.removeIf(p -> p.id() == id);
        products.add(new Product(id == null ? state.nextProduct() : id, name, price));
        commit(new State(1, state.nextUser(), state.nextProduct() + (id == null ? 1 : 0), state.nextOrder(),
                state.users(), products, state.orders()));
    }

    public void deleteProduct(long id) throws IOException {
        require(Role.ADMINISTRADOR);
        findProduct(id);
        commit(new State(1, state.nextUser(), state.nextProduct(), state.nextOrder(), state.users(),
                state.products().stream().filter(p -> p.id() != id).toList(), state.orders()));
    }

    private Product findProduct(long id) {
        return state.products().stream().filter(p -> p.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El producto ya no está disponible."));
    }

    public Order createOrder(int account, Map<Long, Integer> quantities) throws IOException {
        User user = require(Role.EMPLEADO);
        if (!availableAccounts().contains(account)) throw new IllegalArgumentException("La cuenta está ocupada o no es válida.");
        if (quantities.isEmpty()) throw new IllegalArgumentException("Agrega al menos un producto al pedido.");
        List<Line> lines = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = findProduct(entry.getKey());
            if (entry.getValue() == null || entry.getValue() < 1 || entry.getValue() > 9999) {
                throw new IllegalArgumentException("Cada cantidad debe estar entre 1 y 9999.");
            }
            lines.add(new Line(product.id(), product.name(), product.price(), entry.getValue()));
        }
        Order order = new Order(state.nextOrder(), account, user.id(), user.name(), LocalDateTime.now(clock),
                lines, Status.PENDIENTE, null, null, null, null);
        List<Order> orders = new ArrayList<>(state.orders());
        orders.add(order);
        commit(new State(1, state.nextUser(), state.nextProduct(), state.nextOrder() + 1,
                state.users(), state.products(), orders));
        return order;
    }

    public Order confirm(long id) throws IOException {
        User actor = require(Role.CAJERO);
        Order order = findOrder(id);
        if (order.status() != Status.PENDIENTE) throw new IllegalArgumentException("Solo se pueden confirmar pedidos pendientes.");
        return replace(order, Status.CONFIRMADO, actor.name(), null, null, null);
    }

    public Order reject(long id, String reason) throws IOException {
        User actor = require(Role.CAJERO);
        Order order = findOrder(id);
        if (!isOpen(order)) throw new IllegalArgumentException("Este pedido ya fue pagado o rechazado.");
        return replace(order, Status.RECHAZADO, actor.name(), null, text(reason, "Motivo", 200), null);
    }

    public Order pay(long id, BigDecimal received) throws IOException {
        User actor = require(Role.CAJERO);
        Order order = findOrder(id);
        if (order.status() != Status.CONFIRMADO) throw new IllegalArgumentException("Confirma el pedido antes de registrar el pago.");
        received = amount(received);
        if (received.compareTo(order.total()) < 0) throw new IllegalArgumentException("El importe recibido es menor que el total.");
        return replace(order, Status.PAGADO, actor.name(), LocalDateTime.now(clock), null, received);
    }

    private Order findOrder(long id) {
        return state.orders().stream().filter(o -> o.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
    }

    private Order replace(Order old, Status status, String cashier, LocalDateTime paidAt,
                          String reason, BigDecimal received) throws IOException {
        Order updated = new Order(old.id(), old.account(), old.employeeId(), old.employeeName(), old.createdAt(),
                old.lines(), status, cashier, paidAt, reason, received);
        List<Order> orders = state.orders().stream().map(o -> o.id() == old.id() ? updated : o).toList();
        commit(new State(1, state.nextUser(), state.nextProduct(), state.nextOrder(), state.users(), state.products(), orders));
        return updated;
    }

    public List<Order> paidBetween(LocalDate from, LocalDate to) {
        require(Role.CAJERO, Role.ADMINISTRADOR);
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("El rango de fechas no es válido.");
        return orders().stream().filter(o -> o.status() == Status.PAGADO)
                .filter(o -> !o.paidAt().toLocalDate().isBefore(from) && !o.paidAt().toLocalDate().isAfter(to)).toList();
    }

    public String receipt(long id) {
        require(Role.CAJERO, Role.ADMINISTRADOR);
        Order order = findOrder(id);
        if (order.status() != Status.CONFIRMADO && order.status() != Status.PAGADO) {
            throw new IllegalArgumentException("Confirma el pedido para generar su boleta.");
        }
        return Documents.receipt(order);
    }

    private void commit(State updated) throws IOException {
        store.save(updated);
        state = updated;
    }

    static BigDecimal amount(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Ingresa un importe válido.");
        try { return value.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Usa como máximo dos decimales."); }
    }

    private static String text(String value, String label, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + ": ingresa entre 1 y " + max + " caracteres, sin saltos de línea.");
        }
        return value.strip();
    }
}
