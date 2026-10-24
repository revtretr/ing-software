package pe.panaderia.ui;

import pe.panaderia.BakeryService;
import pe.panaderia.Domain.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.Arrays;

import static pe.panaderia.ui.Ui.*;

public final class BakeryFrame extends JFrame {
    private final BakeryService service;
    private final Path directory;
    private EmployeePanel employeePanel;

    public BakeryFrame(BakeryService service, Path directory) {
        super("Panadería · Gestión de pedidos");
        this.service = service;
        this.directory = directory;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (canLeave()) System.exit(0);
            }
        });
        setMinimumSize(new Dimension(1000, 700));
        setSize(1240, 820);
        setLocationRelativeTo(null);
        showLogin();
    }

    private boolean canLeave() {
        return employeePanel == null || !employeePanel.hasDraft()
                || confirm(this, "Hay un pedido sin enviar. ¿Deseas descartarlo y salir?");
    }

    private void showLogin() {
        employeePanel = null;
        JPanel root = new JPanel(new GridBagLayout());
        JPanel card = new JPanel(new BorderLayout(0, 28));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(225, 215, 200)),
                new EmptyBorder(42, 46, 42, 46)));
        JPanel title = new JPanel(new GridLayout(0, 1, 0, 10));
        JLabel brand = label("PANADERÍA", 32, true);
        brand.setForeground(ACCENT);
        title.add(brand);
        title.add(label("Bienvenido a tu espacio de trabajo", 16, false));
        card.add(title, BorderLayout.NORTH);
        JTextField username = new JTextField(20);
        JPasswordField password = new JPasswordField(20);
        card.add(form(new String[]{"Usuario", "Contraseña"}, username, password), BorderLayout.CENTER);
        JButton login = button("Ingresar", () -> {
            char[] secret = password.getPassword();
            try {
                service.login(username.getText(), secret);
                showWorkspace();
            } finally {
                Arrays.fill(secret, '\0');
                password.setText("");
            }
        });
        card.add(login, BorderLayout.SOUTH);
        root.add(card);
        setContentPane(root);
        getRootPane().setDefaultButton(login);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(username::requestFocusInWindow);
    }

    private void showWorkspace() {
        User user = service.currentUser();
        JPanel root = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(15, 24, 15, 24));
        header.setBackground(INK);
        JLabel title = label("PANADERÍA  /  " + user.role(), 21, true);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        JLabel identity = label(user.name() + "  ·  @" + user.username(), 14, false);
        identity.setForeground(Color.WHITE);
        JPanel actions = row(identity, button("Cerrar sesión", ACCENT, () -> {
            if (canLeave()) { service.logout(); showLogin(); }
        }));
        actions.setOpaque(false);
        header.add(actions, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        switch (user.role()) {
            case EMPLEADO -> {
                employeePanel = new EmployeePanel(service);
                tabs.addTab("Nuevo pedido", employeePanel);
                OrdersPanel orders = new OrdersPanel(service, false);
                tabs.addTab("Mis pedidos", orders);
                tabs.addChangeListener(e -> {
                    if (tabs.getSelectedComponent() == orders) orders.refresh();
                    else employeePanel.refreshCatalog();
                });
            }
            case CAJERO -> {
                OrdersPanel orders = new OrdersPanel(service, true);
                ReportsPanel reports = new ReportsPanel(service, false);
                tabs.addTab("Caja · Pedidos", orders);
                tabs.addTab("Pedidos pagados", reports);
                tabs.addChangeListener(e -> {
                    if (tabs.getSelectedComponent() == orders) orders.refresh();
                    else run(this, reports::refresh);
                });
            }
            case ADMINISTRADOR -> {
                ReportsPanel reports = new ReportsPanel(service, true);
                tabs.addTab("Resumen y reportes", reports);
                tabs.addTab("Usuarios", new UsersPanel(service));
                tabs.addTab("Productos", new ProductsPanel(service));
                tabs.addChangeListener(e -> {
                    if (tabs.getSelectedComponent() == reports) run(this, reports::refresh);
                });
            }
        }
        root.add(tabs, BorderLayout.CENTER);
        JLabel footer = label("  Guardado automático  ·  " + directory, 11, false);
        footer.setBorder(new EmptyBorder(8, 18, 8, 18));
        footer.setForeground(MUTED);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(null);
        revalidate();
        repaint();
    }
}
