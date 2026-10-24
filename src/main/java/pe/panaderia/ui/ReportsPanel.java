package pe.panaderia.ui;

import pe.panaderia.BakeryService;
import pe.panaderia.Documents;
import pe.panaderia.Domain.Order;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.List;

import static pe.panaderia.ui.Ui.*;

final class ReportsPanel extends JPanel {
    private final BakeryService service;
    private final boolean administrator;
    private final JTextField from = new JTextField(10);
    private final JTextField to = new JTextField(10);
    private final JComboBox<String> period = new JComboBox<>(new String[]{"Hoy", "Esta semana", "Este mes", "Este año", "Todo", "Personalizado"});
    private final JComboBox<String> grouping = new JComboBox<>(new String[]{"Diaria", "Semanal", "Mensual", "Anual"});
    private final DefaultTableModel paidModel = model("Boleta", "Cuenta", "Fecha de pago", "Empleado", "Total");
    private final DefaultTableModel summaryModel = model("Período", "Pedidos pagados", "Ventas cobradas");
    private final JTable paidTable = table(paidModel);
    private final JTextArea details = textArea();
    private final JLabel total = label("", 21, true);
    private final JLabel appliedRange = label("", 12, false);
    private final JLabel[] metrics = new JLabel[4];
    private List<Order> shown = List.of();

    ReportsPanel(BakeryService service, boolean administrator) {
        super(new BorderLayout());
        this.service = service;
        this.administrator = administrator;
        JPanel page = page(administrator ? "Resumen de ventas" : "Pedidos pagados",
                administrator ? "Ingresos de pedidos cobrados. Filtra por fecha de pago y consulta cada boleta." : "Historial de cobros con fecha, hora y detalle del pedido.");
        JPanel content = new JPanel(new BorderLayout(12, 12));
        JPanel upper = new JPanel(new BorderLayout(8, 12));
        if (administrator) {
            JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
            String[] titles = {"HOY", "ESTA SEMANA", "ESTE MES", "ESTE AÑO"};
            for (int i = 0; i < titles.length; i++) {
                JPanel card = new JPanel(new GridLayout(0, 1, 0, 10));
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(230, 220, 206)),
                        BorderFactory.createEmptyBorder(15, 18, 15, 18)));
                card.add(label(titles[i], 12, true));
                metrics[i] = label("S/ 0.00", 24, true);
                metrics[i].setForeground(ACCENT);
                card.add(metrics[i]);
                cards.add(card);
            }
            upper.add(cards, BorderLayout.NORTH);
        }
        JPanel filters = new JPanel(new GridLayout(0, 1));
        filters.add(row(period, new JLabel("Desde"), from, new JLabel("Hasta"), to,
                button("Aplicar", () -> { period.setSelectedItem("Personalizado"); refresh(); })));
        filters.add(row(label("Fechas: AAAA-MM-DD · Se incluyen ambos días.", 12, false), appliedRange));
        upper.add(filters, BorderLayout.SOUTH);
        content.add(upper, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Detalle de cobros", split(new JScrollPane(paidTable), new JScrollPane(details), 0.65));
        if (administrator) {
            JPanel summary = new JPanel(new BorderLayout(8, 8));
            summary.add(row(new JLabel("Agrupar ventas"), grouping), BorderLayout.NORTH);
            summary.add(new JScrollPane(table(summaryModel)), BorderLayout.CENTER);
            tabs.addTab("Totales por período", summary);
        }
        content.add(tabs, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 1));
        bottom.add(row(total));
        bottom.add(row(button("Ver boleta", () -> {
            Order order = shown.get(selected(paidTable));
            showReceipt(this, order.receiptNumber(), service.receipt(order.id()));
        }), button("Exportar CSV", GREEN, () -> saveText(this, "ventas-panaderia.csv", Documents.csv(shown))),
                button("Actualizar", MUTED, this::refresh)));
        content.add(bottom, BorderLayout.SOUTH);
        page.add(content, BorderLayout.CENTER);
        add(page);
        period.addActionListener(e -> {
            if (!"Personalizado".equals(period.getSelectedItem())) { setDates(); run(this, this::refresh); }
        });
        grouping.addActionListener(e -> updateSummary());
        paidTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = paidTable.getSelectedRow();
            details.setText(row < 0 ? "Selecciona una venta para ver su detalle." : Documents.detail(shown.get(paidTable.convertRowIndexToModel(row))));
            details.setCaretPosition(0);
        });
        setDates();
        refresh();
    }

    private void setDates() {
        LocalDate today = LocalDate.now();
        LocalDate start = today;
        LocalDate end = today;
        switch ((String) Objects.requireNonNull(period.getSelectedItem())) {
            case "Esta semana" -> { start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); end = start.plusDays(6); }
            case "Este mes" -> { start = today.withDayOfMonth(1); end = today.with(TemporalAdjusters.lastDayOfMonth()); }
            case "Este año" -> { start = today.withDayOfYear(1); end = LocalDate.of(today.getYear(), 12, 31); }
            case "Todo" -> {
                List<Order> paid = service.paidBetween(LocalDate.MIN, LocalDate.MAX);
                start = paid.stream().map(o -> o.paidAt().toLocalDate()).min(LocalDate::compareTo).orElse(today);
                end = paid.stream().map(o -> o.paidAt().toLocalDate()).max(LocalDate::compareTo).orElse(today);
            }
            default -> { }
        }
        from.setText(start.toString());
        to.setText(end.toString());
    }

    void refresh() {
        LocalDate start;
        LocalDate end;
        try { start = LocalDate.parse(from.getText().strip()); end = LocalDate.parse(to.getText().strip()); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Usa fechas válidas con formato AAAA-MM-DD, por ejemplo 2026-10-22."); }
        List<Order> next = service.paidBetween(start, end);
        paidTable.clearSelection();
        paidModel.setRowCount(0);
        shown = next;
        for (Order order : shown) paidModel.addRow(new Object[]{order.receiptNumber(), order.account(),
                Documents.DATE_TIME.format(order.paidAt()), order.employeeName(), Documents.money(order.total())});
        total.setText(shown.size() + " pedidos pagados  ·  Total: " + Documents.money(sum(shown)));
        appliedRange.setText("Mostrando: " + start + " → " + end);
        details.setText("Selecciona una venta para ver su detalle.");
        if (!shown.isEmpty()) paidTable.setRowSelectionInterval(0, 0);
        updateSummary();
        if (administrator) {
            LocalDate today = LocalDate.now();
            LocalDate[] starts = {today, today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today.withDayOfMonth(1), today.withDayOfYear(1)};
            LocalDate[] ends = {today, starts[1].plusDays(6), today.with(TemporalAdjusters.lastDayOfMonth()),
                    LocalDate.of(today.getYear(), 12, 31)};
            for (int i = 0; i < metrics.length; i++) metrics[i].setText(Documents.money(sum(service.paidBetween(starts[i], ends[i]))));
        }
    }

    private static BigDecimal sum(List<Order> orders) {
        return orders.stream().map(Order::total).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    private void updateSummary() {
        Map<String, List<Order>> groups = new TreeMap<>(Comparator.reverseOrder());
        for (Order order : shown) {
            LocalDate date = order.paidAt().toLocalDate();
            String key = switch (grouping.getSelectedIndex()) {
                case 1 -> String.format("%04d · Semana %02d", date.get(WeekFields.ISO.weekBasedYear()), date.get(WeekFields.ISO.weekOfWeekBasedYear()));
                case 2 -> YearMonth.from(date).toString();
                case 3 -> String.valueOf(date.getYear());
                default -> date.toString();
            };
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(order);
        }
        summaryModel.setRowCount(0);
        groups.forEach((key, orders) -> summaryModel.addRow(new Object[]{key, orders.size(), Documents.money(sum(orders))}));
    }
}
