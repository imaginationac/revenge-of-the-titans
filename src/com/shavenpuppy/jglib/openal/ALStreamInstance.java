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
package com.shavenpuppy.jglib.openal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.WaveWrapper;

import static org.lwjgl.openal.AL10.*;

/**
 * $Id: ALStreamInstance.java,v 1.29 2011/10/03 15:08:19 cix_foo Exp $
 *
 * @author $Author: cix_foo $
 * @version $Revision: 1.29 $
 */
public class ALStreamInstance extends Resource {

	private static final long serialVersionUID = 1L;
	private static final int BUFSIZE = 65536;
	private static final int BUFFERS = 6;

	private static int num;

	/*
	 * Transient data
	 */

	private transient ALStream sourceStream;

	/** The url */
	private transient String url;

	/** The buffers */
	private transient ALBuffer buffers[];
	private transient int writeBuf;

	/** Input stream */
	private transient InputStream inputStream;

	/** Buffer */
	private transient byte[] buf;

	/** Upload buffer */
	private transient ByteBuffer byteBuf;

	/** Current buffer read pos */
	private transient int pos;

	private transient int type;
	private transient int frequency;
	private transient int format;

	private transient int numQueued;
	private transient boolean waitForQueue, playing, startPlaying;
	private transient boolean looped;

	/** Samples played (roughly) */
	private transient int samplesPlayed;

	/** Size of a sample (1,2,4,8) */
	private transient int sampleSize;

	private ALSource owner;

	/**
	 * C'tor
	 */
	ALStreamInstance(ALStream sourceStream) {
		super("StreamInstance "+(num++));
		init(sourceStream);
	}

	void init(ALStream sourceStream) {
		this.sourceStream = sourceStream;
		this.url = sourceStream.getURL();
		this.inputStream = sourceStream.getInputStream();
		this.format = sourceStream.getFormat();
        // We'll either have a URL or an InputStream
		assert url == null && inputStream != null || url != null && inputStream == null;
		this.looped = sourceStream.isLooped();
		this.samplesPlayed = 0;
	}

	/**
	 * @return Returns the sourceStream.
	 */
	public ALStream getSourceStream() {
		return sourceStream;
	}

	synchronized void setOwner(ALSource newOwner) {
		if (owner != newOwner) {
			setPlaying(false);
		}
		owner = newOwner;
	}

	/**
	 * @return Returns the owner.
	 */
	public ALSource getOwner() {
		return owner;
	}

	/**
	 * @return Returns the type.
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return the format
	 */
	public int getFormat() {
		return format;
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {

		if (!org.lwjgl.openal.AL.isCreated()) {
			return;
		}

		try {
			// 1. Generate buffers
			buffers = new ALBuffer[BUFFERS];
			for (int i = 0; i < buffers.length; i ++) {
				buffers[i] = new ALBuffer("StreamBuffer "+i+"["+this+"]");
				buffers[i].create();
			}

			buf = new byte[BUFSIZE];
			byteBuf = BufferUtils.createByteBuffer(BUFSIZE);

			initStream();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void initStream() throws Exception {

		// First get the wave if necessary
		if (url != null) {
			if (url.startsWith("resource:")) {
				// Load directly from Resources
				WaveWrapper waveResource = (WaveWrapper) Resources.get(url.substring(9));
				if (waveResource == null) {
					throw new RuntimeException("Resource "+url+" not found");
				}
				inputStream = null;
				inputStream = waveResource.getStream();
				type = waveResource.getType();
				frequency = waveResource.getFrequency();
			}
		}

		if (inputStream == null) {
			throw new Exception("No input stream specified for "+this);
		}

		format = AL.translateFormat(type);
		switch (format) {
            case AL_FORMAT_MONO8:
                sampleSize = 1;
                break;
            case AL_FORMAT_STEREO8:
            case AL_FORMAT_MONO16:
                sampleSize = 2;
                break;
            case AL_FORMAT_STEREO16:
                sampleSize = 4;
                break;
            default:
                assert false;
		}
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		if (buffers != null) {
			for (int i = 0; i < buffers.length; i ++) {
				buffers[i].destroy();
			}
			buffers = null;
		}
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
			inputStream = null;
		}
		buf = null;
		byteBuf = null;
	}

	/**
	 * @return Returns the playing.
	 */
	public synchronized boolean isPlaying() {
		return playing;
	}

	/**
	 * @param playing The playing to set.
	 */
	public synchronized void setPlaying(boolean playing) {
		if (this.playing == playing) {
			return;
		}
		this.playing = playing;
		if (!playing) {
			numQueued = 0;
			waitForQueue = false;
			startPlaying = false;
			writeBuf = 0;
			owner.stop();
			// Dequeue everything
			int queued;
			do {
				queued = alGetSourcei(owner.getSourceID(), AL_BUFFERS_QUEUED);
				if (queued > 0) {
					owner.dequeue(queued);
				}
			} while (queued > 0);
			int processed;
			do {
				processed = alGetSourcei(owner.getSourceID(), AL_BUFFERS_PROCESSED);
				if (processed > 0) {
					AL.scratch.ints.clear();
					AL.scratch.ints.limit(processed);
					alSourceUnqueueBuffers(owner.getSourceID(), AL.scratch.ints);
				}
			} while (processed > 0);
		} else {
			startPlaying = true;
		}
		notifyAll();
	}

	/**
	 * @return the number of samples played (roughly).
	 */
	public int getSamplesPlayed() {
	    return samplesPlayed;
	}

	void reset() throws Exception {
	    if (url != null && inputStream != null) {
	        try {
	            inputStream.close();
	        } catch (IOException e) {
	        }
	        inputStream = null;
	    }
		initStream();
	}

	/**
	 * Tick. This keeps our buffers full. This is run in a separate thread.
	 */
	public synchronized void tick() throws Exception {
		if (!buffers[writeBuf].isCreated()) {
			// Wait for resource to be created...
			return;
		}

		if (!isPlaying()) {
			return;
		}
		if (waitForQueue) {
			int processed = alGetSourcei(owner.getSourceID(), AL_BUFFERS_PROCESSED);
			if (processed <= 0) {
				// Have to wait..
				return;
			}

			owner.dequeue(processed);
			numQueued -= processed;
			waitForQueue = false;
		}

		// We are now on the second buffer; this means we can unqueue the first, and
		// queue the buffer that's been filled; and start decoding the next buffer.
		int read = inputStream.read(buf, pos, buf.length - pos);
		if (read == -1) {
			if (looped) {
				initStream();
				tick();
			} else {
				if (numQueued == 0) {
					setPlaying(false);
				} else {
					waitForQueue = true;
				}
			}
		} else {
			pos += read;
			if (pos == buf.length) {
				pos = 0;
				// We're full. Upload to AL. We'll count the samples as being played.
				samplesPlayed += buf.length / sampleSize;
				byteBuf.put(buf);
				byteBuf.flip();
				final ALBuffer bufToQueue = buffers[writeBuf ++];
				if (writeBuf == buffers.length) {
					writeBuf = 0;
				}
				numQueued ++;
				if (numQueued == buffers.length) {
					waitForQueue = true;
				}
				if (startPlaying) {
					owner.unattach(); // Ensure source has no buffers in it of the wrong format
					startPlaying = false;
				}
				// Ensure that the buffer is not somehow inexplicably already queued
				alBufferData(bufToQueue.getBufferID(), format, byteBuf, frequency);
				owner.queue(bufToQueue);
				if (owner.getInt(AL_SOURCE_STATE) != AL_PLAYING) {
					owner.play();
				}
				byteBuf.clear();
			}
		}

	}


}
