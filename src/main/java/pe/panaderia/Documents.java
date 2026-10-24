package pe.panaderia;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static pe.panaderia.Domain.*;

public final class Documents {
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Documents() {}

    public static String money(BigDecimal value) { return "S/ " + value.setScale(2).toPlainString(); }

    public static String detail(Order order) {
        StringBuilder text = new StringBuilder();
        text.append("PEDIDO #").append(order.id()).append("  ·  CUENTA ").append(order.account()).append('\n')
                .append("Estado: ").append(order.status()).append('\n')
                .append("Fecha: ").append(DATE_TIME.format(order.createdAt())).append('\n')
                .append("Empleado: ").append(order.employeeName()).append("\n\n");
        for (Line line : order.lines()) {
            text.append(line.name()).append('\n').append("  ").append(line.quantity()).append(" × ")
                    .append(money(line.price())).append(" = ").append(money(line.subtotal())).append('\n');
        }
        text.append("\nTOTAL: ").append(money(order.total())).append('\n');
        if (order.cashierName() != null) text.append("Cajero: ").append(order.cashierName()).append('\n');
        if (order.paidAt() != null) {
            text.append("Pagado: ").append(DATE_TIME.format(order.paidAt())).append('\n')
                    .append("Recibido: ").append(money(order.received())).append('\n')
                    .append("Vuelto: ").append(money(order.change())).append('\n');
        }
        if (order.rejectionReason() != null) text.append("Motivo: ").append(order.rejectionReason()).append('\n');
        return text.toString();
    }

    public static String receipt(Order order) {
        return "                 PANADERÍA\n"
                + "           BOLETA " + order.receiptNumber() + "\n"
                + "================================================\n"
                + (order.status() == Status.PAGADO ? "                 PAGADO\n" : "              PENDIENTE DE PAGO\n")
                + "================================================\n"
                + detail(order)
                + "================================================\n"
                + "             ¡Gracias por su compra!\n\n"
                + "Comprobante interno de venta.\n"
                + "No es un comprobante electrónico de SUNAT.\n";
    }

    public static String csv(List<Order> orders) {
        StringBuilder result = new StringBuilder("\uFEFFBoleta;Pedido;Cuenta;Fecha de pago;Empleado;Cajero;Total (PEN)\r\n");
        for (Order order : orders) {
            result.append(cell(order.receiptNumber())).append(';').append(order.id()).append(';')
                    .append(order.account()).append(';').append(cell(DATE_TIME.format(order.paidAt()))).append(';')
                    .append(cell(order.employeeName())).append(';').append(cell(order.cashierName())).append(';')
                    .append(order.total().toPlainString()).append("\r\n");
        }
        return result.toString();
    }

    private static String cell(String value) {
        if (value == null) value = "";
        if (value.matches("^[=+@-].*")) value = "'" + value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
