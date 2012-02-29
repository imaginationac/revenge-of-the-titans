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
package com.shavenpuppy.jglib.sound;

import java.util.*;

import org.lwjgl.openal.OpenALException;

import com.shavenpuppy.jglib.openal.*;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.util.PriorityPool;

import static org.lwjgl.openal.AL10.*;

/**
 * The sound player. This queues up commands to play one-shot sound effects using
 * a limited pool of sources.
 *
 * Each sound effect is given a priority. When a sound of higher priority is requested
 * and there are no spare sources, the oldest, lowest priority sound effect is stopped
 * and replaced with the new one.
 *
 * Every video frame you should call play() which will play your requested sounds.
 * This syncs it up nicely with the video.
 */
public class SoundPlayer extends Feature {

	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG = false;

	/** The number of sound sources */
	private int sources;

	/** A Priority Pool which takes care of allocation */
	private transient PriorityPool priorityPool;

	/** The sound effects */
	private transient SoundEffect[] fx;

	/** Streams */
	private transient List<ALStreamInstance> streams;

	/**
	 * Thread for playing streams
	 */
	private class StreamThread extends Thread {

		private boolean finished;

		StreamThread() {
			super("Stream Thread");
			// Execute at higher priority than the game
			setPriority(NORM_PRIORITY + 3);
			setDaemon(true);
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			while (!finished && streams != null) {
				synchronized (streams) {
					for (Iterator<ALStreamInstance> i = streams.iterator(); i.hasNext(); ) {
						ALStreamInstance stream = i.next();
						try {
							if (stream.isCreated()) {
								stream.tick();
							}
						} catch (Exception e) {
							i.remove();
						}
					}
				}
				try {
					Thread.sleep(1); // Roughly 1000fps update
				} catch (InterruptedException e) {
				} catch (ThreadDeath e) {
					finished = true;
				}
			}
		}

		void finish() {
			finished = true;
		}
	}

	private transient StreamThread streamThread;

	/**
	 * Construct a sound player that has the specified number of sources.
	 * @param sources The number of sources
	 */
	public SoundPlayer(int sources) {
		this.sources = sources;
	}

	/**
	 * @param name
	 */
	public SoundPlayer(String name) {
		super(name);
	}

	/**
	 * @see com.shavenpuppy.jglib.openal.ALPlayable#play()
	 */
	public void play() {
		assert isCreated();
		priorityPool.tick();
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {

		ArrayList<SoundEffect> tfx = new ArrayList<SoundEffect>(sources);
		try {
			for (int i = 0; i < sources; i ++) {
				SoundEffect se =  new SoundEffect(this);
				se.source.create();
				tfx.add(se);
			}
		} catch (OpenALException e) {
			// That's as many as we can have
			if (tfx.size() < sources) {
				System.out.println(this+" only managed to create "+tfx.size()+" sources but requested "+sources);
			}
		}
		fx = new SoundEffect[tfx.size()];
		tfx.toArray(fx);
		streams = new LinkedList<ALStreamInstance>();
		priorityPool = new PriorityPool(fx);
		streamThread = new StreamThread();
		streamThread.start();
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		priorityPool.reset();
		priorityPool = null;
		for (int i = 0; i < fx.length; i ++) {
			fx[i].source.destroy();
			fx[i] = null;
		}

		synchronized (streams) {
			streams = null;
		}
		streamThread.finish();
		streamThread = null;
	}

	/**
	 * Allocate a sound effect. The sound effect is plucked from the pool and owned by the
	 * specified owner and returned. If no suitable slot is found then null is returned.
	 * The owner may then subsequently issue commands to the SoundEffect although these
	 * will be ignored if it is no longer the owner of the SoundEffect.
	 *
	 * Don't forget to play() the sound!
	 *
	 * @param buf The sound buffer to play
	 * @param owner The owner of the sound effect
	 * @return a SoundEffect, or null
	 */
	public SoundEffect allocate(ALBuffer buf, Object owner) {
		return allocate(buf, buf.getPriority(), owner);
	}
	public SoundEffect allocate(ALBuffer buf, int priority, Object owner) {
		assert isCreated();


		SoundEffect ret = (SoundEffect) priorityPool.allocate(priority, owner);
		if (ret != null) {
//			System.out.println(this+"("+hashCode()+") allocated sound "+buf);
			// We got a sound effect, so initialize it:
			ret.init(buf);

			// If there's 2 channels to it, allocate another:
			if (buf.getFormat() == AL_FORMAT_STEREO16 || buf.getFormat() == AL_FORMAT_STEREO8) {
				ret.lock();
				SoundEffect link = (SoundEffect) priorityPool.allocate(priority, owner);
				if (link == null || link == ret) {
					if (DEBUG) {
						System.out.println("Couldn't get second channel for "+buf);
					}
					ret.deactivate();
					ret = null;
				} else {
					ret.unlock();
					ret.setLink(link);
				}
			}

		}
		return ret;
	}

	/**
	 * Allocate a streamed sound effect. The sound effect is plucked from the pool and owned by the
	 * specified owner and returned. If no suitable slot is found then null is returned.
	 * The owner may then subsequently issue commands to the SoundEffect although these
	 * will be ignored if it is no longer the owner of the SoundEffect.
	 *
	 * Don't forget to play() the sound!
	 *
	 * @param buf The stream to play
	 * @param owner The owner of the sound effect
	 * @return a SoundEffect, or null
	 */
	public SoundEffect allocate(ALStream buf, Object owner) {
		return allocate(buf, buf.getPriority(), owner);
	}
	public SoundEffect allocate(ALStream buf, int priority, Object owner) {
		assert isCreated();

		SoundEffect ret = (SoundEffect) priorityPool.allocate(priority, owner);
		if (ret != null) {
			// We got a sound effect, so initialize it:
			try {
				ret.init(buf);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				ret.deactivate();
				ret = null;
			}

			// If it's a stereo stream we need to attempt to allocate another source and fail
			// if we can't get one:
			if (ret != null && (buf.getFormat() == AL_FORMAT_STEREO16 || buf.getFormat() == AL_FORMAT_STEREO8)) {
				ret.lock();
				SoundEffect link = (SoundEffect) priorityPool.allocate(priority, owner);
				if (link == null || link == ret) {
					ret.deactivate();
					ret = null;
				} else {
					ret.unlock();
					ret.setLink(link);
				}
			}
		}

		return ret;
	}

	/**
	 * Register a stream so that it will be decoded in the stream thread
	 */
	void registerStream(ALStreamInstance stream) {
		synchronized (streams) {
			if (!streams.contains(stream)) {
				streams.add(stream);
			}
		}
	}

	/**
	 * Deregister a stream
	 */
	void deregisterStream(ALStreamInstance stream) {
		synchronized (streams) {
			streams.remove(stream);
		}
	}

}
