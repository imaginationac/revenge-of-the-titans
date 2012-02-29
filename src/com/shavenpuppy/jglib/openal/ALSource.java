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

import java.util.ArrayList;

import org.lwjgl.openal.OpenALException;
import org.lwjgl.util.vector.Vector3f;

import com.shavenpuppy.jglib.Resource;

import static org.lwjgl.openal.AL10.*;


/**
 * A sound source
 */
public class ALSource extends Resource {

	private static final long serialVersionUID = 1L;

	/** All the known created sources */
	private static ArrayList<ALSource> createdSources = new ArrayList<ALSource>(8);

	/** Source id */
	private transient int source;

	/** The currently attached buffer to this source */
	private transient ALBufferID attachedBuffer;

	/**
	 * Constructor for ALSource.
	 */
	public ALSource() {
		super();
	}

	/**
	 * Unattach a buffer from all known sources
	 * @param buffer The buffer to remove from any sources
	 */
	static synchronized void unattach(ALBuffer buffer) {
		for (int i = 0; i < createdSources.size(); i ++) {
			ALSource s = createdSources.get(i);
			if (s.attachedBuffer == buffer) {
				s.stop();
				s.unattach();
			}
		}
	}

	/**
	 * @see com.shavenpuppy.jglib.openAL_ALPlayable#play()
	 */
	public void play() {
		assert isCreated() : this + " is not created yet";
		alSourcePlay(source);
	}

	/*
	 * Other source commands...
	 */

	/**
	 * Pause the source
	 */
	public void pause() {
		assert isCreated() : this + " is not created yet";
		alSourcePause(source);
	}

	/**
	 * Stop the source
	 */
	public void stop() {
		assert isCreated() : this + " is not created yet";
		alSourceStop(source);
	}

	/**
	 * Rewind the source
	 */
	public void rewind() {
		assert isCreated() : this + " is not created yet";
		alSourceRewind(source);
	}

	/**
	 * Set a source property.
	 * @param property One of
	 * <ul>
	 * <li>AL_PITCH</li>
	 * <li>AL_GAIN</li>
	 * <li>AL_MAX_DISTANCE</li>
	 * <li>AL_ROLLOFF_FACTOR</li>
	 * <li>AL_REFERENCE_DISTANCE</li>
	 * <li>AL_MIN_GAIN</li>
	 * <li>AL_MAX_GAIN</li>
	 * <li>AL_CONE_OUTER_GAIN</li>
	 * </ul>
	 * @param value The value to set the property to
	 */
	public void set(int property, float value) {
		assert isCreated() : this + " is not created yet";
		alSourcef(source, property, value);
	}

	/**
	 * Set a source property
	 * @param property One of
	 * <ul>
	 * <li>AL_POSITION</li>
	 * <li>AL_VELOCITY</li>
	 * <li>AL_DIRECTION</li>
	 * </ul>
	 * @param x
	 * @param y
	 * @param z
	 */
	public void set(int property, float x, float y, float z) {
		assert isCreated() : this + " is not created yet";
		alSource3f(source, property, x, y, z);
	}

	/**
	 * Set a source property
	 * @param property One of
	 * <ul>
	 * <li>
	 * <ul>AL_SOURCE_RELATIVE</ul>
	 * <ul>AL_CONE_INNER_ANGLE</ul>
	 * <ul>AL_CONE_OUTER_ANGLE</ul>
	 * <ul>AL_LOOPING</ul>
	 * <ul>AL_BUFFER</ul>
	 * <ul>AL_SOURCE_STATE</ul>
	 * </ul>
	 */
	public void set(int property, int value) {
		assert isCreated() : this + " is not created yet";
		alSourcei(source, property, value);
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {

		if (!org.lwjgl.openal.AL.isCreated()) {
			return;
		}

		// Generate a buffer
		source = alGenSources();
		createdSources.add(this);
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		if (source != 0) {
			unattach();
			alDeleteSources(source);
			source = 0;
			createdSources.remove(this);
		}
	}

	/**
	 * @return the source ID
	 */
	public int getSourceID() {
		assert isCreated() : this + " is not created yet";
		return source;
	}

	/**
	 * Attach a buffer to this source.
	 * @param buffer The buffer to attach
	 */
	public void attach(ALBufferID buffer) {
		assert isCreated() : this + " is not created yet";
		try {
			unattach();
			set(AL_BUFFER, buffer.getBufferID());
			attachedBuffer = buffer;
		} catch (OpenALException e) {
			System.err.println("Failed: "+buffer.getBufferID()+" source: "+source);
			throw e;
		}

		if (buffer.isLooped()) {
			set(AL_LOOPING, AL_TRUE);
		} else {
			set(AL_LOOPING, AL_FALSE);
		}

	}

	/**
	 * Queue a buffer on this source. Use this to stream buffers to a source.
	 * @param buffer The buffer to queue
	 */
	public void queue(ALBufferID buffer) {
		assert isCreated() : this + " is not created yet";
		if (attachedBuffer != null) {
			attachedBuffer = null;
			stop();
		}
		try {
			alSourceQueueBuffers(source, buffer.getBufferID());
		} catch (OpenALException e) {
			System.out.println("Failed to queue "+buffer+" on "+this);
			throw e;
		}
		set(AL_LOOPING, AL_FALSE);
	}

	/**
	 * Deueue a buffer on this source. Use this to stream buffers to a source.
	 */
	public void dequeue(int num) {
		assert isCreated() : this + " is not created yet";
		if (num <= 0 || num > AL.scratch.ints.capacity()) {
			throw new OpenALException("Can't dequeue "+num+" buffers");
		}
		AL.scratch.ints.clear();
		AL.scratch.ints.limit(num);
		alSourceUnqueueBuffers(source, AL.scratch.ints);
	}

	/**
	 * Unattach any attached buffer(s)
	 */
	public void unattach() {
		stop();
		if (attachedBuffer != null) {
			set(AL_BUFFER, 0);
			attachedBuffer = null;
		}
	}

	/**
	 * Make this sound source looped or not
	 * @param looped Whether to loop or not
	 */
	public void setLooped(boolean looped) {
		set(AL_LOOPING, looped ? AL_TRUE : AL_FALSE);
	}

	@Override
	public String toString() {
		return "ALSource["+source+"]";
	}

	/**
	 * Get a source integer parameter (from alGetSourcei)
	 * @param param The parameter
	 * @return the source integer parameter
	 */
	public int getInt(int param) {
		assert isCreated() : this + " is not created yet";
		return alGetSourcei(source, param);
	}

	/**
	 * Get a source float parameter (from alGetSourcef)
	 * @param param The parameter
	 * @return the source integer parameter
	 */
	public float getFloat(int param) {
		assert isCreated() : this + " is not created yet";
		return alGetSourcef(source, param);
	}

	/**
	 * Get a source vector parameter (from alGetSourcefv)
	 * @param param The parameter
	 * @param vec The destination vector, or null if you want a new one
	 * @return vec, or a new vector3f.
	 */
	public Vector3f getFloats(int param, Vector3f ret) {
		assert isCreated() : this + " is not created yet";
		AL.scratch.floats.clear();
		alGetSource(source, param, AL.scratch.floats);
		if (ret == null) {
			ret = new Vector3f();
		}
		ret.x = AL.scratch.floats.get(0);
		ret.y = AL.scratch.floats.get(1);
		ret.z = AL.scratch.floats.get(2);
		return ret;
	}

}
