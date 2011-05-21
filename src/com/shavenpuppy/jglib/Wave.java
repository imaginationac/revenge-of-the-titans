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
import java.nio.ByteOrder;

import com.shavenpuppy.jglib.resources.WaveWrapper;
/**
 * A sound wave. These are stored on disk in our own special format, compressed.
 * The image header is as follows:
 *
 * MAGIC - int
 * version - int
 * samples - int
 * type - int
 * loopstart - int
 * loopend - int
 * loopcount - int
 * freq - int
 * length of data
 * data
 */
public final class Wave implements Serializable, WaveWrapper {

	static final long serialVersionUID = 4L;

	/** Wrapped data */
	private transient WrappedBuffer wrappedData;

	/** The wave data */
	private transient ByteBuffer data;

	/** Number of samples */
	private transient int samples;

	/** Wave type */
	private transient int type;

	/** Base wave frequency */
	private transient int freq;

	/*
	 * Supported wave types.
	 */

	public static final int MONO_8BIT = 0;
	public static final int MONO_16BIT = 1;
	public static final int STEREO_8BIT = 2;
	public static final int STEREO_16BIT = 3;

	/*
	 * Maps types to sample sizes in bytes
	 */
	private static final int[] typeToSize = new int[] {1,2,2,4};

	/*
	 * Magic number
	 */
	private static final int MAGIC = 0xB00F;
	private static final int VERSION = 1;


	private transient int numRead, numDisposed;

	/**
	 * Simple c'tor
	 */
	public Wave() {
		super();
	}

	/**
	 * Dispose of the data buffer.
	 */
	public void dispose() {
//		System.out.println("Wave "+this+" disposed "+(++numDisposed)+" times, read "+numRead+" times");
		data = null;
		if (wrappedData != null) {
			wrappedData.dispose();
		}
//		System.gc();
	}

	/**
	 * Constructor for SpriteImage.
	 */
	public Wave(int samples, int type, int freq) {
		this.samples = samples;
		this.freq = freq;
		this.type = type;

		data = ByteBuffer.allocateDirect(samples * typeToSize[type]).order(ByteOrder.nativeOrder());
	}

	/**
	 * Constructor for SpriteImage.
	 */
	public Wave(int samples, int type, int freq, byte[] wave) {
		this.samples = samples;
		this.freq = freq;
		this.type = type;

		assert samples * typeToSize[type] == wave.length : "Wave is incorrect size.";
		data = ByteBuffer.allocateDirect(wave.length).order(ByteOrder.nativeOrder());
		data.put(wave);
	}

	/**
	 * Constructor for SpriteImage.
	 */
	public Wave(int samples, int type, int freq, WrappedBuffer waveData) {
		this.samples = samples;
		this.freq = freq;
		this.type = type;

		//assert samples * typeToSize[type] <= waveData.remaining() : "Wave is incorrect size: "+waveData.capacity()+" vs. "+samples * typeToSize[type];
		wrappedData = waveData;
		data = waveData.getBuffer();
	}

	/**
	 * Constructor for SpriteImage.
	 */
	public Wave(int samples, int type, int freq, ByteBuffer waveData) {
		this.samples = samples;
		this.freq = freq;
		this.type = type;

		//assert samples * typeToSize[type] <= waveData.remaining() : "Wave is incorrect size: "+waveData.capacity()+" vs. "+samples * typeToSize[type];
		data = waveData;
	}



	/**
	 * Wave loader
	 */
	public static Wave read(InputStream is) throws Exception {
		Wave ret = new Wave();
		ret = (Wave) new ObjectInputStream(is).readObject();
		return ret;
	}

	/**
	 * Wave writer
	 */
	public static void write(Wave wave, OutputStream os) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(wave);
		oos.flush();
		oos.reset();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		writeHeader(stream);
		byte[] buf = new byte[data.limit()];
		data.rewind();
		data.get(buf);
		data.rewind();
		stream.writeInt(buf.length);
		stream.write(buf);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		readHeader(stream);
		int length = stream.readInt();
		byte[] buf = new byte[length];
		stream.readFully(buf);
		data = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
		data.put(buf);
		data.rewind();
	}

	public void readHeader(DataInput is) throws IOException {
		samples = is.readInt();
		type = is.readInt();
		freq = is.readInt();
	}

	public void writeHeader(DataOutput os) throws IOException {
		os.writeInt(samples);
		os.writeInt(type);
		os.writeInt(freq);
	}

	/**
	 * Gets the data.
	 * @return Returns an MemoryBuffer
	 */
	public ByteBuffer getData() {
//		System.out.println("Wave "+this+" read "+(++numRead)+" times, disposed "+numDisposed+" times");
		return data;
	}


	/**
	 * Gets the number of samples
	 * @return Returns an int
	 */
	public int getSamples() {
		return samples;
	}


	/**
	 * Gets the type.
	 * @return Returns a int
	 */
	@Override
	public int getType() {
		return type;
	}

	/**
	 * Get bytes per sample for a given type
	 * @param type One of the Wave types
	 * @return bytes per sample
	 */
	public static int getBytesPerSample(int type) {
		return typeToSize[type];
	}

	/**
	 * Gets the frequency.
	 * @return Returns the frequency in Hz
	 */
	@Override
	public int getFrequency() {
		return freq;
	}

	/*
	 * Debug output
	 */
	@Override
	public String toString() {
		return "Wave["+samples+" samples, type="+type+"]";
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getWave()
	 */
	@Override
	public Wave getWave() {
		return this;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getStream()
	 */
	@Override
	public InputStream getStream() throws Exception {
		return null; // This aint a stream
	}

	/**
	 * Set data directly
	 */
	public void setData(byte[] newData) {
		data.put(newData);
		data.rewind();
	}

}
