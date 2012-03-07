/**
 *
 */
package net.puppygames.applet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import net.puppygames.steam.Steam;

import org.lwjgl.BufferUtils;

/**
 * An {@link InputStream} to replace {@link FileInputStream} that may use the Steam APIs to manipulate files. There's no need to
 * use a {@link BufferedInputStream} with {@link GameInputStream} as it does this itself where necessary.
 */
public class GameInputStream extends InputStream {

	/** File path relative to application's storage root */
	private final String file;

	/** Using Steam? */
	private final boolean usingSteam;

	/** File input stream, for when not using Steam... */
	private final InputStream fis;

	/** Byte array input stream, for when using Steam */
	private final ByteArrayInputStream bais;

	/** Shadow input stream */
	private final InputStream is;

	/**
	 * C'tor
	 * @param file
	 */
	public GameInputStream(String file) throws IOException {
		this.file = file;

		usingSteam = Game.isUsingSteamCloud();
		if (!usingSteam) {
			fis = new BufferedInputStream(new FileInputStream(file));
			bais = null;
			is = fis;
		} else {
			fis = null;
			bais = new ByteArrayInputStream(readSteamFile());
			is = bais;
		}
	}

	private byte[] readSteamFile() throws IOException {
		if (!Steam.getRemoteStorage().fileExists(file)) {
			throw new FileNotFoundException("Can't file Steam file "+file);
		}

		int size = Steam.getRemoteStorage().getFileSize(file);
		System.out.println("Steam says file "+file+" is "+size+" bytes");
		ByteBuffer byteBuffer = BufferUtils.createByteBuffer(size);
		Steam.getRemoteStorage().fileRead(file, byteBuffer);
		byte[] ret = new byte[byteBuffer.limit()];
		byteBuffer.get(ret);
		System.out.println("Successfully read Steam cloud file "+file);
		return ret;
	}

	@Override
    public int read() throws IOException {
	    return is.read();
    }

	@Override
    public int read(byte[] b) throws IOException {
	    return is.read(b);
    }

	@Override
    public int read(byte[] b, int off, int len) throws IOException {
	    return is.read(b, off, len);
    }

	@Override
    public long skip(long n) throws IOException {
	    return is.skip(n);
    }

	@Override
    public int available() throws IOException {
	    return is.available();
    }

	@Override
    public synchronized void mark(int readlimit) {
	    is.mark(readlimit);
    }

	@Override
    public synchronized void reset() throws IOException {
	    is.reset();
    }

	@Override
    public boolean markSupported() {
	    return is.markSupported();
    }

	@Override
	public void close() throws IOException {
	    is.close();
	}

}
