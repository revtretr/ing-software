package pe.panaderia;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Modelos inmutables: los pedidos conservan nombres y precios históricos. */
public final class Domain {
    private Domain() {}

    public enum Role {
        EMPLEADO("Empleado"), CAJERO("Cajero"), ADMINISTRADOR("Administrador");
        private final String label;
        Role(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum Status {
        PENDIENTE("Pendiente"), CONFIRMADO("Confirmado"), PAGADO("Pagado"), RECHAZADO("Rechazado");
        private final String label;
        Status(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public record User(long id, String username, String name, Role role,
                       String passwordHash) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record Product(long id, String name, BigDecimal price) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record Line(long productId, String name, BigDecimal price, int quantity) implements Serializable {
        private static final long serialVersionUID = 1L;
        public BigDecimal subtotal() { return price.multiply(BigDecimal.valueOf(quantity)); }
    }

    public record Order(long id, int account, long employeeId, String employeeName,
                        LocalDateTime createdAt, List<Line> lines, Status status,
                        String cashierName, LocalDateTime paidAt, String rejectionReason,
                        BigDecimal received) implements Serializable {
        private static final long serialVersionUID = 1L;
        public Order { lines = List.copyOf(lines); }
        public BigDecimal total() {
            return lines.stream().map(Line::subtotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
        }
        public BigDecimal change() { return received == null ? BigDecimal.ZERO : received.subtract(total()); }
        public String receiptNumber() { return "B001-" + String.format("%08d", id); }
    }

    public record State(int version, long nextUser, long nextProduct, long nextOrder,
                        List<User> users, List<Product> products, List<Order> orders) implements Serializable {
        private static final long serialVersionUID = 1L;
        public State {
            users = List.copyOf(users);
            products = List.copyOf(products);
            orders = List.copyOf(orders);
        }
    }
}
