/**
 *
 */
package net.puppygames.applet;

import java.io.*;

/**
 * The GameOutputStream is a replacement for the use of {@link FileOutputStream}. It will use the Steam cloud to or the
 * local filesystem dependent on whether Steam is present, etc.
 */
public class GameOutputStream extends OutputStream {

    /**
     * Buffer size for Steam writes
     */
    private static final int BUFFER_SIZE = 256 * 1024;
    /**
     * The file path, relative to the game's storage dir
     */
    private final String file;
    /**
     * Append or overwrite
     */
    private final boolean append;
    /**
     * Buffer for Steam...
     */
    private final ByteArrayOutputStream baos;
    /**
     * ...or FileOutputStream for local
     */
    private final OutputStream fos;
    /**
     * Shadows either output stream
     */
    private final OutputStream os;
    /**
     * Have we been flushed?
     */
    private boolean flushed;
    /**
     * Have we been closed?
     */
    private boolean closed;

    /**
     * C'tor
     *
     * @param file
     */
    public GameOutputStream(String file) throws IOException {
        this(file, false);
    }

    /**
     * C'tor
     *
     * @param file
     * @param append
     */
    public GameOutputStream(String file, boolean append) throws IOException {
        this.file = file;
        this.append = append;

        baos = null;
        fos = new BufferedOutputStream(new FileOutputStream(file));
        os = fos;
    }

    private void readExisting() throws IOException {
        try (GameInputStream gis = new GameInputStream(file)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int ret;
            do {
                ret = gis.read(buf, 0, BUFFER_SIZE);
                if (ret != -1) {
                    baos.write(buf, 0, ret);
                }
            } while (ret != -1);
        }
        if (Game.DEBUG) {
            System.out.println("Successfully read existing file " + file + " from cloud");
        }
    }

    @Override
    public void write(int b) throws IOException {
        os.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        os.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        os.flush();
        flushed = true;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (!flushed) {
                flush();
            }
        } finally {
            os.close();
        }
    }
}
