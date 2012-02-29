/*
 * Copyright (c) 2003-onwards Shaven Puppy Ltd
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'Shaven Puppy' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.shavenpuppy.jglib;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import com.shavenpuppy.jglib.resources.ImageWrapper;
/**
 * An Image.
 */
public class Image implements Serializable, ImageWrapper {

	private static final long serialVersionUID = 8L;
	private static final int MAGIC = 0x1234;

	/** JPEG Compression interface */
	public interface JPEGCompressor {
		/**
		 * Compress the specified RGB data and return a ByteBuffer with the compressed image in it.
		 * @param width
		 * @param height
		 * @param src
		 * @return dest
		 * @throws Exception
		 */
		ByteBuffer compress(int width, int height, ByteBuffer src) throws Exception;
	}

	/** JPEG Decompression interface */
	public interface JPEGDecompressor {
		/**
		 * Decompress an incoming RGB JPEG image into the specified ByteBuffer.
		 * @param src
		 * @param dest
		 * @throws Exception
		 */
		void decompress(ByteBuffer src, ByteBuffer dest) throws Exception;
	}


	/** JPEG compressor */
	private static JPEGCompressor compressor;

	/** JPEG decompressor */
	private static JPEGDecompressor decompressor;

	/*
	 * Supported image types.
	 *
	 * All RGB(A) types use 8-bits per color, packed.
	 * Paletted uses an 8-bit index palette.
	 * Luminance is 8-bit greyscale
	 * Luminance is 8-bit greyscale + 8 bit alpha
	 */

	public static final int RGB = 0;
	public static final int RGBA = 1;
	public static final int LUMINANCE = 2;
	public static final int LUMINANCE_ALPHA = 3;
	public static final int ARGB = 4;
	public static final int ABGR = 5;
	public static final int BGR = 6;
	public static final int BGRA = 7;

	/*
	 * Maps types to pixel sizes in bytes
	 */
	private static final int[] TYPE_TO_SIZE = new int[] {3,4,1,2,4,4,3,4};

	/** The image data */
	private transient WrappedBuffer wrappedData;
	private transient ByteBuffer data;

	/** The image dimensions */
	private transient int width, height;

	/** Image type */
	private transient int type;

	/** Use JPEG compressor on serialize/deserialize */
	private transient boolean useJPEG;

	/**
	 * Constructor for SpriteImage, used by serialization.
	 */
	public Image() {
		super();
	}

	/**
	 * Dispose of the data buffer
	 */
	public void dispose() {
		data = null;
		wrappedData.dispose();
	}

	/**
	 * C'tor
	 * @param width
	 * @param height
	 * @param type
	 */
	public Image(int width, int height, int type) {
		this.width = width;
		this.height = height;
		this.type = type;

		wrappedData = DirectBufferAllocator.allocate(width * height * TYPE_TO_SIZE[type]);
		data = wrappedData.getBuffer();
	}

	/**
	 * C'tor
	 * @param width
	 * @param height
	 * @param type
	 * @param img
	 */
	public Image(int width, int height, int type, byte[] img) {
		this.width = width;
		this.height = height;
		this.type = type;

		assert width * height * TYPE_TO_SIZE[type] == img.length : "Image is incorrect size.";
		wrappedData = DirectBufferAllocator.allocate(img.length);
		data = wrappedData.getBuffer();
		data.put(img);
		data.flip();
	}

	/**
	 * Constructor for SpriteImage. The incoming ByteBuffer is sliced() to get the image
	 * data (no reference to the incoming buffer is retained).
	 * @param width The image width
	 * @param height The image height
	 * @param typ The image type
	 * @param imageData A direct bytebuffer whose position() and limit() mark the image data
	 */
	public Image(int width, int height, int type, ByteBuffer imageData) {
		this.width = width;
		this.height = height;
		this.type = type;

		assert width * height * TYPE_TO_SIZE[type] == imageData.remaining() : "Image is incorrect size.";
		assert imageData.isDirect() : "Image must be stored in a direct byte buffer.";

		data = imageData.slice();
	}



	/**
	 * SpriteImage loader
	 */
	public static Image read(InputStream is) throws Exception {
		Image ret = new Image();
		ret.readExternal(new DataInputStream(is));
		return ret;
	}

	/**
	 * SpriteImage writer
	 */
	public static void write(Image image, OutputStream os) throws Exception {
		image.writeExternal(os);
	}

	public void writeExternal(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		doWrite(dos);
		dos.flush();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		DataOutputStream dos = new DataOutputStream(stream);
		doWrite(dos);
		dos.flush();
	}

	private void doWrite(DataOutputStream stream) throws IOException {
		stream.writeInt(MAGIC);
		stream.writeInt(width);
		stream.writeInt(height);
		stream.writeInt(type);
		stream.writeInt(useJPEG ? 1 : 0);

		data.rewind();

		if (useJPEG && hasAlpha() && TYPE_TO_SIZE[type] == 4) {
			// Use special JPEG compression. First extract a 3byte BGR image from our source
			ByteBuffer bgr = extractBGR();
			// Compress it
			ByteBuffer compressed;
			try {
				compressed = compressor.compress(width, height, bgr);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw new IOException("Failed to compress: "+e.getMessage());
			}
			stream.writeInt(compressed.capacity());
			System.out.println("Compressed image to "+compressed.capacity()+" bytes, down from "+(width * height * 4));
			stream.write(compressed.array());
			// Extract the alpha
			compressed = createAlphaDelta();
			stream.write(compressed.array());
		} else if (useJPEG && !hasAlpha() && TYPE_TO_SIZE[type] == 3) {
			// Use normal JPEG compression.
			ByteBuffer compressed;
			try {
				compressed = compressor.compress(width, height, data);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw new IOException("Failed to compress: "+e.getMessage());
			}
			stream.writeInt(compressed.capacity());
			System.out.println("Compressed image to "+compressed.capacity()+" bytes, down from "+(width * height * 3));
			stream.write(compressed.array());
		} else {
			byte[] buf = new byte[data.limit() - data.position()];
			data.get(buf);
			data.flip();
			stream.writeInt(buf.length);
			stream.write(buf);
		}

	}

	public void readExternal(InputStream is) throws IOException {
		DataInputStream stream = new DataInputStream(is);
		int magic = stream.readInt();
		if (magic != MAGIC) {
			throw new IOException("Stream corrupt - expected magic number "+MAGIC+" but got "+magic);
		}
		width = stream.readInt();
		if (width <= 0) {
			throw new IOException("Illegal width: got "+width);
		}
		height = stream.readInt();
		if (height <= 0) {
			throw new IOException("Illegal height: got "+height);
		}
		type = stream.readInt();
		if (type < 0 || type > 7) {
			throw new IOException("Illegal type: got "+type);
		}
		useJPEG = stream.readInt() == 1;

		int length = width * height * TYPE_TO_SIZE[type];
		wrappedData = DirectBufferAllocator.allocate(length);
		data = wrappedData.getBuffer();

		if (useJPEG && hasAlpha() && TYPE_TO_SIZE[type] == 4) {
			// Use special JPEG decompression
			int compressedSize = stream.readInt();
			ByteBuffer compressed = ByteBuffer.allocate(compressedSize);
			byte[] buf = new byte[compressedSize];
			stream.readFully(buf);
			compressed.put(buf);
			compressed.flip();
			ByteBuffer uncompressed = ByteBuffer.allocateDirect(width * height * 3);
			try {
				decompressor.decompress(compressed, uncompressed);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw new IOException("Failed to decompress: "+e.getMessage(), e);
			}
			insertBGR(uncompressed);
			ByteBuffer alphaDelta = ByteBuffer.allocate(width * height);
			buf = null;
			buf = new byte[width * height];
			stream.readFully(buf);
			alphaDelta.put(buf).flip();
			mergeAlphaDelta(alphaDelta);
		} else if (useJPEG && !hasAlpha() && TYPE_TO_SIZE[type] == 3) {
			// Use normal JPEG decompression
			int compressedSize = stream.readInt();
			ByteBuffer compressed = ByteBuffer.allocate(compressedSize);
			byte[] buf = new byte[compressedSize];
			stream.readFully(buf);
			compressed.put(buf);
			compressed.flip();
			data = ByteBuffer.allocateDirect(width * height * 3);
			try {
				decompressor.decompress(compressed, data);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw new IOException("Failed to decompress: "+e.getMessage(), e);
			}
			data.rewind();
		} else {
			// Fast image read
			int actualLength = stream.readInt();
			if (actualLength != length) {
				throw new IOException("Corrupt: expected length "+length+", but read "+actualLength+" from stream");
			}
			byte[] buf = new byte[length];
			stream.readFully(buf);
			data.put(buf);
			data.flip();
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();

		DataInputStream dis = new DataInputStream(stream);
		readExternal(dis);
	}


//	/**
//	 * Is this number a power of 2?
//	 */
//	private static boolean isPowerOf2(int n) {
//		// Scan for first set bit...
//		int i = 0;
//		int p = 1;
//		for (; i < 32; i ++) {
//			if ( (n & p) == p)
//				break;
//			else
//				p <<= 1;
//		}
//		// Now make sure no other bits are set
//		for (; i < 31; i ++) {
//			p <<= 1;
//			if ( (n & p) == p)
//				return false;
//		}
//		return true;
//	}

	/**
	 * Extract a BGR image from a 4-byte image with alpha
	 * @return ByteBuffer
	 */
	private ByteBuffer extractBGR() {
		ByteBuffer ret = ByteBuffer.allocate(width * height * 3);
		int n = width * height * 4;
		switch (type) {
			case RGBA:
				for (int i = 0; i < n; i += 4) {
					ret.put(data.get(i + 2));
					ret.put(data.get(i + 1));
					ret.put(data.get(i + 0));
				}
				break;
			case BGRA:
				for (int i = 0; i < n; i += 4) {
					ret.put(data.get(i + 0));
					ret.put(data.get(i + 1));
					ret.put(data.get(i + 2));
				}
				break;
			case ARGB:
				for (int i = 0; i < n; i += 4) {
					ret.put(data.get(i + 3));
					ret.put(data.get(i + 2));
					ret.put(data.get(i + 1));
				}
				break;
			case ABGR:
				for (int i = 0; i < n; i += 4) {
					ret.put(data.get(i + 1));
					ret.put(data.get(i + 2));
					ret.put(data.get(i + 3));
				}
				break;
			default:
				assert false;
		}
		return ret;
	}

	/**
	 * Insert a BGR image into a 4-byte image with alpha
	 * @param buf The BGR image source
	 */
	private void insertBGR(ByteBuffer buf) {
		int n = width * height * 4;
		switch (type) {
			case RGBA:
				for (int i = 0; i < n; i += 4) {
					data.put(i + 2, buf.get());
					data.put(i + 1, buf.get());
					data.put(i + 0, buf.get());
				}
				break;
			case BGRA:
				for (int i = 0; i < n; i += 4) {
					data.put(i + 0, buf.get());
					data.put(i + 1, buf.get());
					data.put(i + 2, buf.get());
				}
				break;
			case ARGB:
				for (int i = 0; i < n; i += 4) {
					data.put(i + 3, buf.get());
					data.put(i + 2, buf.get());
					data.put(i + 1, buf.get());
				}
				break;
			case ABGR:
				for (int i = 0; i < n; i += 4) {
					data.put(i + 1, buf.get());
					data.put(i + 2, buf.get());
					data.put(i + 3, buf.get());
				}
				break;
			default:
				assert false;
		}
	}

	/**
	 * Return a delta-compressed alpha channel
	 * @return alpha
	 */
	private ByteBuffer createAlphaDelta() {
		ByteBuffer buf = ByteBuffer.allocate(width * height);
		int pos;
		if (type == RGBA  || type == BGRA) {
			pos = 3;
		} else {
			pos = 0;
		}
		for (int y = 0; y < height; y ++) {
			int o0 = 0, n0 = 0;
			for (int x = 0; x < width; x ++) {
				n0 = data.get(pos);
				buf.put((byte) (n0 - o0));
				o0 = n0;
				pos += 4;
			}
		}
		return buf;
	}

	/**
	 * Merge an incoming alpha-delta channel with the existing color data
	 * @param buf
	 */
	private void mergeAlphaDelta(ByteBuffer buf) {
		int pos;
		if (type == RGBA  || type == BGRA) {
			pos = 3;
		} else {
			pos = 0;
		}
		for (int y = 0; y < height; y ++) {
			int o0 = 0;
			for (int x = 0; x < width; x ++) {
				o0 += buf.get();
				data.put(pos, (byte) o0);
				pos += 4;
			}
		}
	}

	/**
	 * Gets the data.
	 * @return Returns an ByteBuffer
	 */
	public ByteBuffer getData() {
//		System.out.println("Image "+this+" read "+(++numRead)+" times, disposed "+numDisposed+" times");
		return data;
	}


	/**
	 * Gets the height.
	 * @return Returns a int
	 */
	public int getHeight() {
		return height;
	}


	/**
	 * Gets the type.
	 * @return Returns a int
	 */
	public int getType() {
		return type;
	}


	/**
	 * Gets the width.
	 * @return Returns a int
	 */
	public int getWidth() {
		return width;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Image [width=");
		buffer.append(width);
		buffer.append(", height=");
		buffer.append(height);
		buffer.append(", type=");
		buffer.append(type);
		buffer.append(", useJPEG=");
		buffer.append(useJPEG);
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * Sets the compression method. This is only used when serializing the image
	 * to an ObjectOutputStream; hence there is no method to query the compression
	 * method. Note that only RGBA/ARGB/BGRA/ABGR formats support JPEG compression.
	 * @param useJPEG true to use JPEG image compression
	 */
	public void setUseJPEG(boolean useJPEG) {
		this.useJPEG = useJPEG;
	}

	/**
	 * Sets the compressor that shall be used to compress image data when it is serialized
	 * and it is specified as COMPRESSION_JPEG.
	 * @param compressor The compressor to set.
	 */
	public static void setCompressor(JPEGCompressor compressor) {
		Image.compressor = compressor;
	}

	/**
	 * Sets the decompressor that shall be used to decompress JPEG-compressed image data.
	 * @param decompressor The decompressor to set.
	 */
	public static void setDecompressor(JPEGDecompressor decompressor) {
		Image.decompressor = decompressor;
	}

	/**
	 * Has this image got an alpha channel?
	 * @return boolean
	 */
	public boolean hasAlpha() {
		return type == LUMINANCE_ALPHA || type == RGBA || type == ARGB || type == ABGR || type == BGRA;
	}

	@Override
	public Image getImage() {
		return this;
	}
}
