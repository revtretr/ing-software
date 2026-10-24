package pe.panaderia.ui;

import pe.panaderia.BakeryService;
import pe.panaderia.Documents;
import pe.panaderia.Domain.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static pe.panaderia.ui.Ui.*;

final class OrdersPanel extends JPanel {
    private final BakeryService service;
    private final DefaultTableModel model = model("Pedido", "Cuenta", "Empleado", "Estado", "Total");
    private final JTable table = table(model);
    private final JTextArea details = textArea();
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"Activos", "Todos", "Pendiente", "Confirmado", "Pagado", "Rechazado"});
    private final JTextField search = new JTextField(14);
    private final JButton confirmButton;
    private final JButton rejectButton;
    private final JButton payButton;
    private final JButton receiptButton;
    private List<Order> shown = List.of();

    OrdersPanel(BakeryService service, boolean cashier) {
        super(new BorderLayout());
        this.service = service;
        JPanel page = page(cashier ? "Pedidos en caja" : "Mis pedidos",
                cashier ? "Confirma el pedido, registra el pago y entrega la boleta." : "Consulta el estado de los pedidos que enviaste a caja.");
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(row(new JLabel("Estado"), filter, new JLabel("Pedido / cuenta"), search,
                button("Actualizar", MUTED, this::refresh)), BorderLayout.NORTH);
        content.add(split(new JScrollPane(table), new JScrollPane(details), 0.62), BorderLayout.CENTER);
        confirmButton = button("Confirmar", GREEN, () -> { service.confirm(selectedOrder().id()); refresh(); });
        rejectButton = button("Rechazar", RED, () -> {
            Order order = selectedOrder();
            String reason = JOptionPane.showInputDialog(this, "Motivo del rechazo:", "Rechazar pedido #" + order.id(), JOptionPane.QUESTION_MESSAGE);
            if (reason != null) { service.reject(order.id(), reason); refresh(); }
        });
        payButton = button("Registrar pago", this::pay);
        receiptButton = button("Generar boleta", MUTED, this::receipt);
        if (cashier) content.add(row(confirmButton, rejectButton, payButton, receiptButton), BorderLayout.SOUTH);
        page.add(content, BorderLayout.CENTER);
        add(page);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateDetails(); });
        filter.addActionListener(e -> refresh());
        onChange(search, this::refresh);
        refresh();
    }

    void refresh() {
        Long selectedId = table.getSelectedRow() < 0 ? null : shown.get(table.convertRowIndexToModel(table.getSelectedRow())).id();
        String state = (String) filter.getSelectedItem();
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        List<Order> next = service.orders().stream()
                .filter(o -> "Todos".equals(state) || ("Activos".equals(state) ? BakeryService.isOpen(o) : o.status().toString().equals(state)))
                .filter(o -> query.isEmpty() || String.valueOf(o.id()).contains(query) || String.valueOf(o.account()).contains(query))
                .toList();
        table.clearSelection();
        model.setRowCount(0);
        shown = next;
        for (Order order : shown) model.addRow(new Object[]{order.id(), order.account(), order.employeeName(), order.status(), Documents.money(order.total())});
        int selection = 0;
        for (int i = 0; i < shown.size(); i++) if (Long.valueOf(shown.get(i).id()).equals(selectedId)) selection = i;
        if (!shown.isEmpty()) table.setRowSelectionInterval(selection, selection);
        updateDetails();
    }

    private Order selectedOrder() { return shown.get(selected(table)); }

    private void updateDetails() {
        Order order = table.getSelectedRow() < 0 ? null : selectedOrder();
        details.setText(order == null ? "Selecciona un pedido para ver su detalle." : Documents.detail(order));
        details.setCaretPosition(0);
        confirmButton.setEnabled(order != null && order.status() == Status.PENDIENTE);
        rejectButton.setEnabled(order != null && BakeryService.isOpen(order));
        payButton.setEnabled(order != null && order.status() == Status.CONFIRMADO);
        receiptButton.setEnabled(order != null && (order.status() == Status.CONFIRMADO || order.status() == Status.PAGADO));
    }

    private void pay() throws Exception {
        Order order = selectedOrder();
        String value = JOptionPane.showInputDialog(this, "Total: " + Documents.money(order.total()) + "\nImporte recibido (S/):", order.total().toPlainString());
        if (value == null) return;
        BigDecimal received = decimal(value);
        Order paid = service.pay(order.id(), received);
        refresh();
        info(this, "Pago registrado.\nVuelto: " + Documents.money(paid.change()) + "\nLa cuenta " + paid.account() + " está disponible.");
        showReceipt(this, paid.receiptNumber(), service.receipt(paid.id()));
    }

    private void receipt() {
        Order order = selectedOrder();
        showReceipt(this, order.receiptNumber(), service.receipt(order.id()));
    }
}
