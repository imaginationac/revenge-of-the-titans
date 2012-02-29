/**
 *
 */
package net.puppygames.applet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import net.puppygames.steam.Steam;
import net.puppygames.steam.SteamException;

import org.lwjgl.BufferUtils;

/**
 * The GameOutputStream is a replacement for the use of {@link FileOutputStream}. It will use the Steam cloud to or the local
 * filesystem dependent on whether Steam is present, etc.
 */
public class GameOutputStream extends OutputStream {

	/** Buffer size for Steam writes */
	private static final int BUFFER_SIZE = 256 * 1024;

	/** The file path, relative to the game's storage dir */
	private final String file;

	/** Append or overwrite */
	private final boolean append;

	/** Using Steam? */
	private final boolean usingSteam;

	/** Buffer for Steam... */
	private final ByteArrayOutputStream baos;

	/** ...or FileOutputStream for local */
	private final OutputStream fos;

	/** Shadows either output stream */
	private final OutputStream os;

	/** Have we been flushed? */
	private boolean flushed;

	/** Have we been closed? */
	private boolean closed;

	/**
	 * C'tor
	 * @param file
	 */
	public GameOutputStream(String file) throws IOException {
		this(file, false);
	}

	/**
	 * C'tor
	 * @param file
	 * @param append
	 */
	public GameOutputStream(String file, boolean append) throws IOException {
		this.file = file;
		this.append = append;

		usingSteam = Game.isUsingSteamCloud();

		if (usingSteam) {
			baos = new ByteArrayOutputStream(BUFFER_SIZE);
			fos = null;
			os = baos;

			// If appending, read the original file in
			if (append) {
				readExisting();
			}
		} else {
			baos = null;
			fos = new BufferedOutputStream(new FileOutputStream(file));
			os = fos;
		}
	}

	private void readExisting() throws IOException {
		if (!Steam.getRemoteStorage().fileExists(file)) {
			if (Game.DEBUG) {
				System.out.println("Steam says "+file+" does not exist");
			}
			return;
		}
		GameInputStream gis = new GameInputStream(file);
		byte[] buf = new byte[BUFFER_SIZE];
		int ret;
		do {
			ret = gis.read(buf, 0, BUFFER_SIZE);
			if (ret != -1) {
				baos.write(buf, 0, ret);
			}
		} while (ret != -1);
		gis.close();
		if (Game.DEBUG) {
			System.out.println("Successfully read existing file "+file+" from cloud");
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
	    if (usingSteam) {
	    	// Ignore
	    } else {
		    os.flush();
	    }
	    flushed = true;
    }

	private void doSteamFlush() throws IOException {
    	ByteBuffer byteBuffer = BufferUtils.createByteBuffer(baos.size());
    	byteBuffer.put(baos.toByteArray());
    	byteBuffer.flip();
    	try {
    		Steam.getRemoteStorage().fileWrite(file, byteBuffer);
    		System.out.println("Wrote "+file+" to Steam cloud");
    	} catch (SteamException e) {
    		throw new IOException(e);
    	}
    	baos.reset();
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
			if (usingSteam) {
				doSteamFlush();
			}
		} finally {
			os.close();
		}
    }


}
