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

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.OpenALException;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.openal.ALSource;
import com.shavenpuppy.jglib.openal.ALStream;
import com.shavenpuppy.jglib.openal.ALStreamInstance;
import com.shavenpuppy.jglib.util.PriorityPooled;

import static org.lwjgl.openal.AL10.*;

/**
 * A sound effect
 */
public final class SoundEffect implements PriorityPooled {

	final ALSource source = new ALSource();
	private int priority;
	private Object owner;
	private SoundEffect link;
	private boolean locked;

	private boolean updatePosition;
	private boolean updateVelocity;
	private boolean updateGain;
	private boolean updatePitch;
	private boolean updateState;
	private boolean updateBuffer;
	private boolean updateLooping;
	private boolean updateAttenuated;

	private float x, y, z, dx, dy, dz, gain, pitch;
	boolean looped;
	boolean attenuated;
	int fadeTick;
	int fadeDuration;
	float initialGain, finalGain;
	ALBuffer buffer;
	ALStreamInstance stream;
	int fadeType;
	private static final int FADE_NONE = 0;
	private static final int FADE_IN = 1;
	private static final int FADE_OUT = 2;

	private int state = -1;
	private static final int DO_PLAY = 0;
	private static final int DO_PAUSE = 1;
	private static final int DO_REWIND = 2;
	private static final int DO_STOP = 3;

	private final SoundPlayer player;

	// Constructor
	SoundEffect(SoundPlayer player) {
		this.player = player;
	}

	private void doInit() {
		x = 0;
		y = 0;
		z = 0;
		dx = 0;
		dy = 0;
		dz = 0;
		state = DO_PLAY;
		fadeType = FADE_NONE;
		fadeTick = 0;
		attenuated = true;
		updatePosition = true;
		updateVelocity = true;
		updateGain = true;
		updatePitch = true;
		updateState = true;
		updateLooping = true;
		updateAttenuated = true;
		source.set(AL_REFERENCE_DISTANCE, 1024.0f);
		source.set(AL_ROLLOFF_FACTOR, 1.0f);
		source.set(AL_MIN_GAIN, 0.0f);
		source.set(AL_MAX_GAIN, 1.0f);
		if (link != null) {
			link.deactivate();
			link = null;
		}
	}

	/**
	 * Initialise the sound effect with a stream
	 *
	 * @param stream
	 *            The sound stream
	 */
	void init(ALStream stream) throws Exception {
		this.buffer = null;
		this.stream = stream.getInstance(source);
		pitch = stream.getPitch();
		gain = stream.getGain();
		looped = stream.isLooped();
		doInit();
	}

	/**
	 * Initialise this sound effect
	 *
	 * @param buf
	 *            The sound buffer
	 */
	void init(ALBuffer buf) {
		this.buffer = buf;
		this.stream = null;
		updateBuffer = true;
		pitch = buffer.getPitch();
		gain = buffer.getGain();
		looped = buffer.isLooped();
		doInit();
	}

	/**
	 * Update is called every frame by Player.play(), and takes note of any
	 * changes the user has specified.
	 */
	void update() {
		try {
			// If this is a stream and we're no longer the owner, quietly
			// relinquish the
			// stream
			if (stream != null && stream.getOwner() != source) {
				System.out.println(this + " lost ownership of stream " + stream
						+ " to " + stream.getOwner());
				stream = null;
				deactivate();
				return;
			}

			// Do fades
			switch (fadeType) {
			case FADE_IN:
				gain = LinearInterpolator.instance.interpolate(initialGain,
						finalGain, (float) fadeTick / (float) fadeDuration);
				fadeTick++;
				updateGain = true;
				if (fadeTick >= fadeDuration) {
					fadeType = FADE_NONE;
				}
				break;
			case FADE_OUT:
				gain = LinearInterpolator.instance.interpolate(initialGain,
						finalGain, (float) fadeTick / (float) fadeDuration);
				fadeTick++;
				updateGain = true;
				if (fadeTick >= fadeDuration) {
					fadeType = FADE_NONE;
					state = DO_STOP;
					updateState = true;
				}
				break;
			case FADE_NONE:
				break;
			default:
				assert false;
			}

			if (updatePosition) {
				source.set(AL_POSITION, x, y, z);
				updatePosition = false;
			}

			if (updateVelocity) {
				source.set(AL_VELOCITY, dx, dy, dz);
				updateVelocity = false;
			}

			if (updatePitch) {
				source.set(AL_PITCH, pitch);
				updatePitch = false;
			}

			if (updateLooping) {
				source.setLooped(looped);
				updateLooping = false;
			}

			if (updateAttenuated) {
				source.set(AL_ROLLOFF_FACTOR, attenuated ? 1.0f : 0.0f);
				updateAttenuated = false;
			}

			if (updateGain) {
				source.set(AL10.AL_GAIN, gain);
				updateGain = false;
			}

			if (updateBuffer) {
				if (buffer != null) {
					source.attach(buffer);
				}
				updateBuffer = false;
			}

			if (updateState) {
				switch (state) {
				case SoundEffect.DO_PLAY:
					if (stream != null) {
						stream.setPlaying(true);
						player.registerStream(stream);
					} else {
						source.play();
					}
					break;
				case SoundEffect.DO_PAUSE:
					source.pause();
					break;
				case SoundEffect.DO_REWIND:
					source.rewind();
					break;
				case SoundEffect.DO_STOP:
					if (stream != null) {
						player.deregisterStream(stream);
						stream.setPlaying(false);
						deactivate();
						return;
					} else {
						source.stop();
					}
					break;
				default:
					assert false;
				}
			}

			// Query the source to see if it has finished playing
			if (!updateState) {
				if (buffer != null) {
					int sourceState = source.getInt(AL_SOURCE_STATE);
					if (sourceState == AL_STOPPED || sourceState == AL_INITIAL) {
						// System.out.println("Deactivating "+buffer+" state is "+AL.recode(sourceState));
						deactivate();
					}
				} else if (stream != null && !stream.isPlaying()) {
					deactivate();
				}
			}
		} catch (OpenALException e) {
			deactivate();
		}

		updateState = false;
	}

	public void setPosition(float x, float y, float z, Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		this.x = x;
		this.y = y;
		this.z = z;
		updatePosition = true;
	}

	public void setVelocity(float dx, float dy, float dz, Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		updateVelocity = true;
	}

	public void setGain(float gain, Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		this.gain = gain;
		updateGain = true;
	}

	public void setPitch(float pitch, Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		this.pitch = pitch;
		updatePitch = true;
	}

	public void play(Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		state = SoundEffect.DO_PLAY;
		updateState = true;
	}

	public void pause(Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		state = SoundEffect.DO_PAUSE;
		updateState = true;
	}

	public void stop(Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		state = SoundEffect.DO_STOP;
		updateState = true;
	}

	public void rewind(Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		state = SoundEffect.DO_REWIND;
		updateState = true;
	}

	public void setLooped(boolean looped, Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		this.looped = looped;
		updateLooping = true;
	}

	public void setAttenuated(boolean attenuated, Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		this.attenuated = attenuated;
		updateAttenuated = true;
	}

	public void setFade(int duration, float finalGain, boolean stopAtEnd,
			Object owner) {
		if (!isOwnedBy(owner)) {
			return;
		}
		this.fadeDuration = duration;
		this.initialGain = gain;
		this.finalGain = finalGain;
		fadeTick = 0;
		if (stopAtEnd) {
			fadeType = FADE_OUT;
		} else {
			fadeType = FADE_IN;
		}

	}

	public boolean isOwnedBy(Object owner) {
		return this.owner == owner;
	}

	@Override
	public String toString() {
		if (buffer != null) {
			return "SoundEffect[source=" + source + " buffer=" + buffer
					+ " owner=" + owner + "]";
		} else if (stream != null) {
			return "SoundEffect[source=" + source + " stream=" + stream
					+ " owner=" + owner + "]";
		} else {
			return "SoundEffect[source=" + source + " owner=" + owner + "]";
		}
	}

	/**
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#isActive()
	 */
	@Override
	public boolean isActive() {
		return locked || (buffer != null || stream != null) && owner != null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#lock()
	 */
	@Override
	public void lock() {
		locked = true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#isLocked()
	 */
	@Override
	public boolean isLocked() {
		return locked;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#unlock()
	 */
	@Override
	public void unlock() {
		locked = false;
	}

	/**
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#allocate(java.lang.Object)
	 */
	@Override
	public void allocate(Object owner) {
		this.owner = owner;
	}

	/**
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#getOwner()
	 */
	@Override
	public Object getOwner() {
		return owner;
	}

	/**
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#deactivate()
	 */
	@Override
	public void deactivate() {
		try {
			buffer = null;
			if (stream != null) {
				player.deregisterStream(stream);
				stream.setPlaying(false);
				stream = null;
			}
			owner = null;
			source.stop();
		} catch (OpenALException e) {
			// Silently ignore
		}

		unlock();

		if (link != null) {
			link.deactivate();
			link = null;
		}
	}

	/**
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#tick()
	 */
	@Override
	public void tick() {
		update();
	}

	/**
	 * Get the buffer
	 *
	 * @return the buffer
	 */
	public ALBuffer getBuffer() {
		return buffer;
	}

	/**
	 * Get the stream
	 *
	 * @return the stream
	 */
	public synchronized ALStreamInstance getStream() {
		return stream;
	}

	/**
	 * Get the source
	 *
	 * @return the source
	 */
	ALSource getSource() {
		return source;
	}

	/**
	 * @param link
	 *            the linked to set
	 */
	void setLink(SoundEffect link) {
		this.link = link;
		if (link != null) {
			link.lock();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#getPriority()
	 */
	@Override
	public int getPriority() {
		return priority;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.util.PriorityPooled#setPriority(int)
	 */
	@Override
	public void setPriority(int newPriority) {
		this.priority = newPriority;
	}
}