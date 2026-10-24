package pe.panaderia.ui;

import pe.panaderia.BakeryService;
import pe.panaderia.Domain.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static pe.panaderia.ui.Ui.*;

final class UsersPanel extends JPanel {
    private final BakeryService service;
    private final DefaultTableModel model = model("ID", "Usuario", "Nombre", "Rol");
    private final JTable table = table(model);
    private final JTextField search = new JTextField(25);
    private List<User> shown = List.of();

    UsersPanel(BakeryService service) {
        super(new BorderLayout());
        this.service = service;
        JPanel page = page("Usuarios del sistema", "Registra empleados, cajeros y administradores. Cada persona utiliza su propia cuenta.");
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(row(new JLabel("Buscar"), search, button("Registrar usuario", () -> edit(null))), BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(row(button("Modificar usuario", () -> edit(shown.get(selected(table)))),
                button("Eliminar usuario", RED, () -> {
                    User user = shown.get(selected(table));
                    if (confirm(this, "¿Eliminar el usuario @" + user.username() + "?")) {
                        service.deleteUser(user.id());
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
        shown = service.users().stream().filter(u -> (u.username() + " " + u.name() + " " + u.role())
                .toLowerCase(Locale.ROOT).contains(query)).toList();
        model.setRowCount(0);
        shown.forEach(u -> model.addRow(new Object[]{u.id(), u.username(), u.name(), u.role()}));
    }

    private void edit(User user) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), user == null ? "Registrar usuario" : "Modificar usuario",
                Dialog.ModalityType.APPLICATION_MODAL);
        JTextField username = new JTextField(user == null ? "" : user.username(), 22);
        JTextField name = new JTextField(user == null ? "" : user.name(), 22);
        JComboBox<Role> role = new JComboBox<>(Role.values());
        if (user != null) role.setSelectedItem(user.role());
        JPasswordField password = new JPasswordField(22);
        JPanel fields = form(new String[]{"Usuario", "Nombre", "Rol", "Contraseña"}, username, name, role, password);
        JPanel body = new JPanel(new BorderLayout());
        body.add(fields, BorderLayout.CENTER);
        body.add(row(label(user == null ? "Contraseña: de 6 a 128 caracteres." : "Contraseña vacía: conservar la actual.", 12, false)), BorderLayout.SOUTH);
        dialog.add(body, BorderLayout.CENTER);
        JButton save = button("Guardar usuario", () -> {
            char[] secret = password.getPassword();
            try {
                service.saveUser(user == null ? null : user.id(), username.getText(), name.getText(), (Role) role.getSelectedItem(), secret);
                refresh();
                dialog.dispose();
            } finally { Arrays.fill(secret, '\0'); }
        });
        dialog.add(row(save, button("Cancelar", MUTED, dialog::dispose)), BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(save);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
