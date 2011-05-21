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
import java.io.*;
import java.nio.ByteBuffer;

import com.shavenpuppy.jglib.resources.ImageWrapper;
/**
 * An Image.
 */
public class Image implements Serializable, ImageWrapper {

	static final long serialVersionUID = 7L;

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
		public ByteBuffer compress(int width, int height, ByteBuffer src) throws Exception;
	}

	/** JPEG Decompression interface */
	public interface JPEGDecompressor {
		/**
		 * Decompress an incoming RGB JPEG image into the specified ByteBuffer.
		 * @param src
		 * @param dest
		 * @throws Exception
		 */
		public void decompress(ByteBuffer src, ByteBuffer dest) throws Exception;
	}


	/** JPEG compressor */
	private static JPEGCompressor compressor;

	/** JPEG decompressor */
	private static JPEGDecompressor decompressor;

	/** The image data */
	private transient WrappedBuffer wrappedData;
	private transient ByteBuffer data;

	/** The image dimensions */
	private int width, height;

	/** Image type */
	private int type;

	/** Use JPEG compressor on serialize/deserialize */
	private boolean useJPEG;

	/** Use delta-planar compression */
	private boolean deltaPlanar;

	/** Palette, if any */
	private Palette palette;

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
	public static final int PALETTED = 8;

	/*
	 * Maps types to pixel sizes in bytes
	 */
	private static final int[] typeToSize = new int[] {3,4,1,2,4,4,3,4,1};

//	/*
//	 * Magic number
//	 */
//	private static final int MAGIC = 0xF00B;
//	private static final int VERSION = 3;
//
//	private transient int numDisposed, numRead;

	/**
	 * Constructor for SpriteImage, used by serialization.
	 */
	public Image() {
		super();
	}

	/**
	 * Dispose of the data buffer and palette.
	 */
	public void dispose() {
//		System.out.println("Image "+this+" disposed "+(++numDisposed)+" times, read "+numRead+" times");
		data = null;
		palette = null;
		wrappedData.dispose();
	}

	/**
	 * Constructor for SpriteImage.
	 */
	public Image(int width, int height, int type) {
		this.width = width;
		this.height = height;
		this.type = type;

		wrappedData = DirectBufferAllocator.allocate(width * height * typeToSize[type]);
		data = wrappedData.getBuffer();
		//data = ByteBuffer.allocateDirect(width * height * typeToSize[type]).order(ByteOrder.nativeOrder());
		if (type == PALETTED) {
			palette = new Palette(Palette.RGBA, 256);
		}
	}

	/**
	 * Constructor for SpriteImage.
	 */
	public Image(int width, int height, int type, byte[] img) {
		this.width = width;
		this.height = height;
		this.type = type;

		assert width * height * typeToSize[type] == img.length : "Image is incorrect size.";
		//data = ByteBuffer.allocateDirect(img.length).order(ByteOrder.nativeOrder());
		wrappedData = DirectBufferAllocator.allocate(img.length);
		data = wrappedData.getBuffer();
		data.put(img);
		data.flip();
		if (type == PALETTED) {
			palette = new Palette(Palette.RGBA, 256);
		}
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

		assert width * height * typeToSize[type] == imageData.remaining() : "Image is incorrect size.";
		assert imageData.isDirect() : "Image must be stored in a direct byte buffer.";

		data = imageData.slice();
		if (type == PALETTED) {
			palette = new Palette(Palette.RGBA, 256);
		}
	}



	/**
	 * SpriteImage loader
	 */
	public static Image read(InputStream is) throws Exception {
		return (Image) (new ObjectInputStream(is)).readObject();
//		Image ret = new Image();
//		ret.readExternal(new ObjectInputStream(is));
//		return ret;
	}

	/**
	 * SpriteImage writer
	 */
	public static void write(Image image, OutputStream os) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(image);
		oos.flush();
		oos.reset();

//		image.writeExternal(oos);
//		oos.flush();
//		oos.reset();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();

		data.rewind();

		if (useJPEG && hasAlpha() && typeToSize[type] == 4) {
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
		} else if (useJPEG && !hasAlpha() && typeToSize[type] == 3) {
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
		} else if (deltaPlanar) {
			// Use delta planar compression.
			ByteBuffer split = splitIntoPlanes();
			split.rewind();
			stream.write(split.array());
		} else {
			byte[] buf = new byte[data.limit() - data.position()];
			data.get(buf);
			data.flip();
			stream.write(buf);
		}

	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();

		int length = width * height * typeToSize[type];
		wrappedData = DirectBufferAllocator.allocate(length);
		data = wrappedData.getBuffer();
		//data = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());

		if (useJPEG && hasAlpha() && typeToSize[type] == 4) {
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
		} else if (useJPEG && !hasAlpha() && typeToSize[type] == 3) {
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
		} else if (deltaPlanar) {
			// Use normal delta-planar decompression
			ByteBuffer deltaPlanes = ByteBuffer.allocateDirect(length);
			byte[] buf = new byte[length];
			stream.readFully(buf);
			deltaPlanes.put(buf);
			mergePlanes(deltaPlanes);
		} else {
			// Fast image read
			byte[] buf = new byte[length];
			stream.readFully(buf);
			data.put(buf);
			data.flip();
		}
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
	 * Splits the data up into separate planes
	 * @returns a new ByteBuffer with the data in it
	 */
	private ByteBuffer splitIntoPlanes() {

		ByteBuffer buf = ByteBuffer.allocate(data.capacity());

		switch (type) {
			case LUMINANCE:
			case PALETTED:
				buf.put(data);
				return buf;

			case LUMINANCE_ALPHA:
				buf.position(buf.capacity() / 2);
				ByteBuffer alpha = buf.slice();
				buf.position(0);
				for (int y = 0; y < height; y ++) {
					int ol = 0, oa = 0, nl = 0, na = 0;
					for (int x = 0; x < width; x ++) {
						nl = data.get();
						na = data.get();
						buf.put((byte) (nl - ol));
						alpha.put((byte) (na - oa));
						ol = nl;
						oa = na;
					}
				}
				break;
			case RGB:
			case BGR:
			{
				buf.position(buf.capacity() / 3);
				ByteBuffer buf1 = buf.slice();
				buf.position(2 * buf.capacity() / 3);
				ByteBuffer buf2 = buf.slice();
				buf.position(0);
				for (int y = 0; y < height; y ++) {
					int o0 = 0, o1 = 0, o2 = 0, n0 = 0, n1 = 0, n2 = 0;
					for (int x = 0; x < width; x ++) {
						n0 = data.get();
						n1 = data.get();
						n2 = data.get();
						buf.put((byte) (n0 - o0));
						buf1.put((byte) (n1 - o1));
						buf2.put((byte) (n2 - o2));
						o0 = n0;
						o1 = n1;
						o2 = n2;
					}
				}
			}
			break;
			case RGBA:
			case ABGR:
			case ARGB:
			case BGRA:
			{
				buf.position(buf.capacity() / 4);
				ByteBuffer buf1 = buf.slice();
				buf.position(2 * buf.capacity() / 4);
				ByteBuffer buf2 = buf.slice();
				buf.position(3 * buf.capacity() / 4);
				ByteBuffer buf3 = buf.slice();
				buf.position(0);
				for (int y = 0; y < height; y ++) {
					int o0 = 0, o1 = 0, o2 = 0, o3 = 0, n0 = 0, n1 = 0, n2 = 0, n3 = 0;
					for (int x = 0; x < width; x ++) {
						n0 = data.get();
						n1 = data.get();
						n2 = data.get();
						n3 = data.get();
						buf.put((byte) (n0 - o0));
						buf1.put((byte) (n1 - o1));
						buf2.put((byte) (n2 - o2));
						buf3.put((byte) (n3 - o3));
						o0 = n0;
						o1 = n1;
						o2 = n2;
						o3 = n3;
					}
				}
			}
			break;
		}

		data.rewind();

		return buf;
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
	 * Merges the data from separate planes
	 * @param buf The source data
	 */
	private void mergePlanes(ByteBuffer buf) {

		buf.flip();

		switch (type) {
			case LUMINANCE:
			case PALETTED:
				data.put(buf);
				return;

			case LUMINANCE_ALPHA:
				buf.position(buf.capacity() / 2);
				ByteBuffer alpha = buf.slice();
				buf.position(0);
				for (int y = 0; y < height; y ++) {
					int ol = 0, oa = 0;
					for (int x = 0; x < width; x ++) {
						ol += buf.get();
						oa += alpha.get();
						data.put((byte) ol);
						data.put((byte) oa);
					}
				}
				break;
			case RGB:
			case BGR:
			{
				buf.position(buf.capacity() / 3);
				ByteBuffer buf0 = buf.slice();
				buf.position(2 * buf.capacity() / 3);
				ByteBuffer buf1 = buf.slice();
				buf.position(0);
				for (int y = 0; y < height; y ++) {
					int o0 = 0, o1 = 0, o2 = 0;
					for (int x = 0; x < width; x ++) {
						o0 += buf.get();
						o1 += buf0.get();
						o2 += buf1.get();
						data.put((byte) o0);
						data.put((byte) o1);
						data.put((byte) o2);
					}
				}
			}
				break;
			case RGBA:
			case ABGR:
			case ARGB:
			case BGRA:
			{
				buf.position(buf.capacity() / 4);
				ByteBuffer buf0 = buf.slice();
				buf.position(2 * buf.capacity() / 4);
				ByteBuffer buf1 = buf.slice();
				buf.position(3 * buf.capacity() / 4);
				ByteBuffer buf2 = buf.slice();
				buf.position(0);
				for (int y = 0; y < height; y ++) {
					int o0 = 0, o1 = 0, o2 = 0, o3 = 0;
					for (int x = 0; x < width; x ++) {
						o0 += buf.get();
						o1 += buf0.get();
						o2 += buf1.get();
						o3 += buf2.get();
						data.put((byte) o0);
						data.put((byte) o1);
						data.put((byte) o2);
						data.put((byte) o3);
					}
				}
			}
				break;
		}

		data.flip();
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
		buffer.append(", deltaPlanar=");
		buffer.append(deltaPlanar);
		buffer.append(", ");
		if (palette != null) {
			buffer.append("palette=");
			buffer.append(palette);
		}
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * Returns the palette.
	 * @return Palette
	 */
	public Palette getPalette() {
		return palette;
	}

	/**
	 * Sets the palette.
	 * @param palette The palette to set
	 */
	public void setPalette(Palette palette) {
		assert type == PALETTED : "Not a paletted image.";
		assert palette != null : "Cannot set a null palette.";
		this.palette = palette;
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
	 * Sets lossless compression method.
	 * @param deltaPlanar true to use delta-planar compression
	 */
	public void setDeltaPlanar(boolean deltaPlanar) {
		this.deltaPlanar = deltaPlanar;
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

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.ImageWrapper#getImage()
	 */
	@Override
	public Image getImage() {
		return this;
	}
}
