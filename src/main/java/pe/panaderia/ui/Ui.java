package pe.panaderia.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Ui {
    public static final Color INK = new Color(51, 41, 33);
    public static final Color MUTED = new Color(113, 103, 92);
    public static final Color BACKGROUND = new Color(247, 245, 240);
    public static final Color ACCENT = new Color(173, 78, 22);
    public static final Color GREEN = new Color(39, 114, 80);
    public static final Color RED = new Color(171, 49, 49);

    private Ui() {}
    @FunctionalInterface public interface Action { void run() throws Exception; }

    public static void installTheme() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { /* Usar el tema predeterminado. */ }
        Font font = new Font("SansSerif", Font.PLAIN, 14);
        for (Object key : UIManager.getDefaults().keySet().toArray()) {
            if (key.toString().endsWith(".font")) UIManager.put(key, font);
        }
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Table.selectionBackground", new Color(247, 224, 195));
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TextField.caretForeground", INK);
        UIManager.put("OptionPane.messageFont", font);
    }

    public static JPanel page(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout(16, 16));
        page.setBorder(new EmptyBorder(20, 24, 20, 24));
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 6));
        heading.add(label(title, 25, true));
        JLabel description = label(subtitle, 13, false);
        description.setForeground(MUTED);
        heading.add(description);
        page.add(heading, BorderLayout.NORTH);
        return page;
    }

    public static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        return label;
    }

    public static JPanel row(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        for (Component component : components) panel.add(component);
        return panel;
    }

    public static JButton button(String text, Color color, Action action) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color),
                new EmptyBorder(9, 13, 9, 13)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> run(button, action));
        return button;
    }

    public static JButton button(String text, Action action) { return button(text, ACCENT, action); }

    public static void run(Component parent, Action action) {
        try { action.run(); }
        catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (e instanceof IOException) message = "No se pudo leer o guardar el archivo.\n" + message;
            JOptionPane.showMessageDialog(parent, message, "Revisa la operación", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirmar", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Panadería", JOptionPane.INFORMATION_MESSAGE);
    }

    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(233, 229, 221));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    public static int selected(JTable table) {
        if (table.getSelectedRow() < 0) throw new IllegalArgumentException("Selecciona una fila primero.");
        return table.convertRowIndexToModel(table.getSelectedRow());
    }

    public static JTextArea textArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setMargin(new Insets(16, 16, 16, 16));
        return area;
    }

    public static JSplitPane split(Component left, Component right, double weight) {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(weight);
        split.setDividerLocation(weight);
        split.setBorder(null);
        left.setMinimumSize(new Dimension(260, 100));
        right.setMinimumSize(new Dimension(260, 100));
        return split;
    }

    public static void onChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    public static BigDecimal decimal(String value) {
        try { return new BigDecimal(value.strip().replace(',', '.')); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Ingresa un importe numérico, por ejemplo 4.50."); }
    }

    public static JPanel form(String[] labels, JComponent... inputs) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 5, 8, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            constraints.gridy = i;
            constraints.gridx = 0;
            constraints.weightx = 0;
            panel.add(new JLabel(labels[i]), constraints);
            constraints.gridx = 1;
            constraints.weightx = 1;
            panel.add(inputs[i], constraints);
        }
        return panel;
    }

    public static void saveText(Component parent, String filename, String content) throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar " + filename);
        chooser.setSelectedFile(new java.io.File(filename));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        if (Files.exists(path) && !confirm(parent, "El archivo ya existe. ¿Deseas reemplazarlo?")) return;
        Files.writeString(path, content, StandardCharsets.UTF_8);
        info(parent, "Archivo guardado en:\n" + path.toAbsolutePath());
    }

    public static void showReceipt(Component parent, String number, String content) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Boleta " + number,
                Dialog.ModalityType.APPLICATION_MODAL);
        JTextArea area = textArea();
        area.setText(content);
        area.setCaretPosition(0);
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(row(button("Guardar TXT", () -> saveText(dialog, number + ".txt", content)),
                button("Imprimir / PDF", GREEN, () -> area.print()),
                button("Cerrar", MUTED, dialog::dispose)), BorderLayout.SOUTH);
        dialog.setSize(630, 670);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public static <T> void renderCombo(JComboBox<T> combo, java.util.function.Function<T, String> label) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @SuppressWarnings("unchecked")
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                     boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == null ? "" : label.apply((T) value),
                        index, selected, focus);
            }
        });
    }
}
