package pe.panaderia.ui;

import pe.panaderia.BakeryService;
import pe.panaderia.Documents;
import pe.panaderia.Domain.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import static pe.panaderia.ui.Ui.*;

final class EmployeePanel extends JPanel {
    private final BakeryService service;
    private final JTextField search = new JTextField(20);
    private final DefaultTableModel catalogModel = model("Producto", "Precio unitario");
    private final DefaultTableModel cartModel = model("Producto", "Cantidad", "Precio", "Subtotal");
    private final JTable catalog = table(catalogModel);
    private final JTable cart = table(cartModel);
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JComboBox<Integer> accounts = new JComboBox<>();
    private final JLabel total = label("TOTAL  S/ 0.00", 24, true);
    private final Map<Long, Integer> draft = new LinkedHashMap<>();
    private List<Product> shown = List.of();
    private List<Long> cartIds = List.of();

    EmployeePanel(BakeryService service) {
        super(new BorderLayout());
        this.service = service;
        JPanel page = page("Un nuevo pedido", "Selecciona productos, asigna una cuenta y envía el pedido a caja.");
        JPanel left = new JPanel(new BorderLayout(8, 12));
        left.add(row(new JLabel("Buscar"), search), BorderLayout.NORTH);
        left.add(new JScrollPane(catalog), BorderLayout.CENTER);
        quantity.setPreferredSize(new Dimension(85, 33));
        left.add(row(new JLabel("Cantidad"), quantity, button("Agregar al pedido", GREEN, this::addItem)), BorderLayout.SOUTH);
        JPanel right = new JPanel(new BorderLayout(8, 12));
        right.add(row(label("Tu pedido", 18, true), button("Quitar producto", RED, () -> {
            draft.remove(cartIds.get(selected(cart)));
            refreshCart();
        })), BorderLayout.NORTH);
        right.add(new JScrollPane(cart), BorderLayout.CENTER);
        JPanel checkout = new JPanel(new GridLayout(0, 1, 0, 8));
        renderCombo(accounts, value -> "Cuenta " + value);
        checkout.add(row(new JLabel("Cuenta disponible"), accounts, button("Actualizar", MUTED, this::refreshCatalog)));
        checkout.add(row(total));
        checkout.add(row(button("Enviar a caja", this::send), button("Vaciar", MUTED, () -> {
            if (!draft.isEmpty() && confirm(this, "¿Vaciar el pedido actual?")) { draft.clear(); refreshCart(); }
        })));
        right.add(checkout, BorderLayout.SOUTH);
        page.add(split(left, right, 0.45), BorderLayout.CENTER);
        add(page);
        onChange(search, this::refreshProducts);
        refreshCatalog();
    }

    boolean hasDraft() { return !draft.isEmpty(); }

    void refreshCatalog() {
        refreshProducts();
        Integer previous = (Integer) accounts.getSelectedItem();
        accounts.removeAllItems();
        service.availableAccounts().forEach(accounts::addItem);
        if (previous != null && service.availableAccounts().contains(previous)) accounts.setSelectedItem(previous);
        refreshCart();
    }

    private void refreshProducts() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        shown = service.products().stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Product::name)).toList();
        catalogModel.setRowCount(0);
        shown.forEach(p -> catalogModel.addRow(new Object[]{p.name(), Documents.money(p.price())}));
    }

    private void addItem() throws Exception {
        Product product = shown.get(selected(catalog));
        quantity.commitEdit();
        int count = ((Number) quantity.getValue()).intValue() + draft.getOrDefault(product.id(), 0);
        if (count > 9999) throw new IllegalArgumentException("El máximo por producto es 9999 unidades.");
        draft.put(product.id(), count);
        refreshCart();
    }

    private void refreshCart() {
        Map<Long, Product> products = new HashMap<>();
        service.products().forEach(p -> products.put(p.id(), p));
        draft.keySet().removeIf(id -> !products.containsKey(id));
        cartIds = new ArrayList<>(draft.keySet());
        cartModel.setRowCount(0);
        BigDecimal sum = new BigDecimal("0.00");
        for (long id : cartIds) {
            Product product = products.get(id);
            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(draft.get(id)));
            sum = sum.add(subtotal);
            cartModel.addRow(new Object[]{product.name(), draft.get(id), Documents.money(product.price()), Documents.money(subtotal)});
        }
        total.setText("TOTAL  " + Documents.money(sum));
    }

    private void send() throws Exception {
        Integer account = (Integer) accounts.getSelectedItem();
        if (account == null) throw new IllegalArgumentException("No hay cuentas libres. Caja debe cobrar o rechazar un pedido.");
        Order order = service.createOrder(account, draft);
        draft.clear();
        refreshCatalog();
        info(this, "Pedido #" + order.id() + " enviado a caja.\n\nIndica al cliente:\nCuenta " + order.account()
                + "\nTotal a pagar: " + Documents.money(order.total()));
    }
}
