package pe.panaderia;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;

/** Un archivo local con escritura atómica y bloqueo para evitar dos escritores. */
public final class Store implements AutoCloseable {
    private final Path directory;
    private final Path file;
    private final FileChannel channel;
    private final FileLock lock;

    public Store(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath();
        Files.createDirectories(this.directory);
        file = this.directory.resolve("panaderia.dat");
        channel = FileChannel.open(this.directory.resolve("panaderia.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = channel.tryLock();
        } catch (OverlappingFileLockException e) {
            channel.close();
            throw new IOException("La aplicación ya está abierta con esta carpeta de datos.", e);
        } catch (IOException e) {
            channel.close();
            throw e;
        }
        if (acquired == null) {
            channel.close();
            throw new IOException("La aplicación ya está abierta. Utiliza la ventana existente.");
        }
        lock = acquired;
    }

    public Path directory() { return directory; }

    public Domain.State load() throws IOException {
        if (!Files.exists(file)) return null;
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            input.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "maxdepth=40;maxrefs=2000000;maxbytes=100000000;pe.panaderia.Domain$*;java.base/*;!*"));
            Object data = input.readObject();
            if (!(data instanceof Domain.State state) || state.version() != 1) {
                throw new IOException("Formato de datos no compatible.");
            }
            return state;
        } catch (ClassNotFoundException | RuntimeException e) {
            throw new IOException("No se pueden leer los datos. El archivo original se ha conservado.", e);
        }
    }

    public void save(Domain.State state) throws IOException {
        Path temporary = Files.createTempFile(directory, "panaderia-", ".tmp");
        try {
            try (FileOutputStream stream = new FileOutputStream(temporary.toFile());
                 ObjectOutputStream output = new ObjectOutputStream(stream)) {
                output.writeObject(state);
                output.flush();
                stream.getFD().sync();
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override public void close() throws IOException {
        try { if (lock.isValid()) lock.release(); } finally { channel.close(); }
    }
}
