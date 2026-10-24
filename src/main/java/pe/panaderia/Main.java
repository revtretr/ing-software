package pe.panaderia;

import pe.panaderia.ui.BakeryFrame;
import pe.panaderia.ui.Ui;

import javax.swing.*;
import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.installTheme();
            Store store = null;
            try {
                String defaultDirectory = Path.of(System.getProperty("user.home"), ".panaderia-app").toString();
                store = new Store(Path.of(System.getProperty("panaderia.data", defaultDirectory)));
                BakeryService service = new BakeryService(store);
                Store openStore = store;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try { openStore.close(); } catch (Exception ignored) { /* El proceso está terminando. */ }
                }, "cerrar-datos"));
                new BakeryFrame(service, store.directory()).setVisible(true);
            } catch (Exception e) {
                if (store != null) {
                    try { store.close(); } catch (Exception ignored) { /* Mostrar el error de inicio. */ }
                }
                JOptionPane.showMessageDialog(null, "No se pudo iniciar Panadería.\n" + e.getMessage(),
                        "Panadería", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
