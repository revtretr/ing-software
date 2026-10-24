package pe.panaderia.ui;

import pe.panaderia.BakeryService;
import pe.panaderia.Documents;
import pe.panaderia.Domain.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static pe.panaderia.ui.Ui.*;

final class ProductsPanel extends JPanel {
    private final BakeryService service;
    private final DefaultTableModel model = model("ID", "Producto", "Precio unitario");
    private final JTable table = table(model);
    private final JTextField search = new JTextField(25);
    private List<Product> shown = List.of();

    ProductsPanel(BakeryService service) {
        super(new BorderLayout());
        this.service = service;
        JPanel page = page("Catálogo de productos", "Los empleados podrán añadir estos productos a sus pedidos. Precios en soles (PEN).");
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(row(new JLabel("Buscar"), search, button("Agregar producto", () -> edit(null))), BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(row(button("Modificar producto", () -> edit(shown.get(selected(table)))),
                button("Eliminar producto", RED, () -> {
                    Product product = shown.get(selected(table));
                    if (confirm(this, "¿Eliminar «" + product.name() + "» del catálogo?\nLos pedidos registrados conservarán su detalle.")) {
                        service.deleteProduct(product.id());
                        refresh();
                    }
                })), BorderLayout.SOUTH);
        page.add(content, BorderLayout.CENTER);
        add(page);
        onChange(search, this::refresh);
        refresh();
    }

    private void refresh() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        shown = service.products().stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Product::name)).toList();
        model.setRowCount(0);
        shown.forEach(p -> model.addRow(new Object[]{p.id(), p.name(), Documents.money(p.price())}));
    }

    private void edit(Product product) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), product == null ? "Agregar producto" : "Modificar producto",
                Dialog.ModalityType.APPLICATION_MODAL);
        JTextField name = new JTextField(product == null ? "" : product.name(), 25);
        JTextField price = new JTextField(product == null ? "" : product.price().toPlainString(), 12);
        dialog.add(form(new String[]{"Producto", "Precio unitario (S/)"}, name, price), BorderLayout.CENTER);
        JButton save = button("Guardar producto", () -> {
            service.saveProduct(product == null ? null : product.id(), name.getText(), decimal(price.getText()));
            refresh();
            dialog.dispose();
        });
        dialog.add(row(save, button("Cancelar", MUTED, dialog::dispose)), BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(save);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
