package pe.panaderia.ui;

import pe.panaderia.BakeryService;
import pe.panaderia.Domain.Status;
import pe.panaderia.Store;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Recorrido de la interfaz real; requiere un entorno con escritorio gráfico. */
public final class UiSmokeTest {
    private static BakeryFrame frame;
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path data = Files.createTempDirectory("panaderia-ui-");
        Path pictures = args.length == 0 ? data.resolve("capturas") : Path.of(args[0]);
        Files.createDirectories(pictures);
        try (Store store = new Store(data)) {
            BakeryService service = new BakeryService(store);
            AtomicReference<Throwable> error = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                try {
                    Ui.installTheme();
                    frame = new BakeryFrame(service, data);
                    frame.setVisible(true);
                    capture(pictures, "01-ingreso.png");
                    login("empleado", "Empleado123");
                    EmployeePanel employee = find(frame, EmployeePanel.class).get(0);
                    List<JTable> tables = find(employee, JTable.class);
                    check(tables.get(0).getRowCount() == 6, "Catálogo visible");
                    tables.get(0).setRowSelectionInterval(0, 0);
                    click(employee, "Agregar al pedido");
                    check(tables.get(1).getRowCount() == 1, "Producto agregado desde interfaz");
                    capture(pictures, "02-empleado.png");
                    withDialogs(() -> click(employee, "Enviar a caja"));
                    check(service.orders().size() == 1, "Pedido creado desde interfaz");
                    click(frame, "Cerrar sesión");
                    login("cajero", "Cajero123");
                    OrdersPanel orders = find(frame, OrdersPanel.class).get(0);
                    check(find(orders, JTable.class).get(0).getRowCount() == 1, "Pedido recibido en caja");
                    click(orders, "Confirmar");
                    check(service.orders().get(0).status() == Status.CONFIRMADO, "Confirmación desde interfaz");
                    capture(pictures, "03-cajero.png");
                    withDialogs(() -> click(orders, "Registrar pago"));
                    check(service.orders().get(0).status() == Status.PAGADO, "Pago desde interfaz");
                    JTabbedPane tabs = find(frame, JTabbedPane.class).get(0);
                    tabs.setSelectedIndex(1);
                    ReportsPanel paid = find(frame, ReportsPanel.class).get(0);
                    check(find(paid, JTable.class).get(0).getRowCount() == 1, "Historial actualizado");
                    capture(pictures, "04-pagados.png");
                    click(frame, "Cerrar sesión");
                    login("admin", "Admin123");
                    ReportsPanel reports = find(frame, ReportsPanel.class).get(0);
                    check(find(reports, JTable.class).get(0).getRowCount() == 1, "Reporte de administrador");
                    capture(pictures, "05-administrador.png");
                    tabs = find(frame, JTabbedPane.class).get(0);
                    tabs.setSelectedIndex(1);
                    check(find(find(frame, UsersPanel.class).get(0), JTable.class).get(0).getRowCount() == 3, "Usuarios visibles");
                    capture(pictures, "06-usuarios.png");
                    tabs.setSelectedIndex(2);
                    check(find(find(frame, ProductsPanel.class).get(0), JTable.class).get(0).getRowCount() == 6, "Productos visibles");
                    capture(pictures, "07-productos.png");
                } catch (Throwable failure) { error.set(failure); }
                finally {
                    for (Window window : Window.getWindows()) window.dispose();
                }
            });
            if (error.get() != null) throw new AssertionError("Falló el recorrido gráfico", error.get());
            System.out.println("OK: " + checks + " comprobaciones gráficas. Capturas: " + pictures.toAbsolutePath());
        } finally {
            try (var paths = Files.walk(data)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void login(String username, String password) {
        find(frame, JTextField.class).stream().filter(field -> !(field instanceof JPasswordField)).findFirst().orElseThrow().setText(username);
        find(frame, JPasswordField.class).get(0).setText(password);
        click(frame, "Ingresar");
    }

    private static void click(Container root, String text) {
        JButton button = find(root, JButton.class).stream().filter(b -> b.getText().equals(text)).findFirst().orElseThrow();
        check(button.isEnabled(), "Botón habilitado: " + text);
        button.doClick();
    }

    private static void withDialogs(Runnable action) {
        AtomicReference<String> error = new AtomicReference<>();
        Timer timer = new Timer(100, event -> {
            for (Window window : Window.getWindows()) {
                if (!(window instanceof JDialog dialog) || !dialog.isShowing()) continue;
                List<JOptionPane> panes = find(dialog, JOptionPane.class);
                if (!panes.isEmpty()) {
                    JOptionPane pane = panes.get(0);
                    if (pane.getMessageType() == JOptionPane.ERROR_MESSAGE) error.set(String.valueOf(pane.getMessage()));
                    if (pane.getWantsInput()) pane.setInputValue("10.00");
                    pane.setValue(JOptionPane.OK_OPTION);
                } else {
                    dialog.dispose();
                }
            }
        });
        timer.start();
        try { action.run(); }
        finally { timer.stop(); }
        if (error.get() != null) throw new AssertionError(error.get());
    }

    private static <T> List<T> find(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        if (type.isInstance(root)) found.add(type.cast(root));
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) found.addAll(find(container, type));
            else if (type.isInstance(child)) found.add(type.cast(child));
        }
        return found;
    }

    private static void capture(Path directory, String name) throws Exception {
        frame.validate();
        layoutTree(frame);
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { frame.paint(graphics); } finally { graphics.dispose(); }
        ImageIO.write(image, "png", directory.resolve(name).toFile());
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layoutTree(nested);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
