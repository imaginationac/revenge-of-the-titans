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
package worm;

import java.io.Serializable;

import worm.features.WorldFeature;
import worm.generator.MapTemplate;

/**
 * Transient class for initialising Sandbox games
 */
public class SandboxParams implements Serializable {

	private static final long serialVersionUID = 1L;

	private int templateIndex;

	private WorldFeature world;
	private MapTemplate template;
	private float difficulty;
	private int size;
	private boolean generateNew;

	/**
	 * C'tor
	 * @param world
	 * @param template
	 * @param size
	 */
	public SandboxParams(WorldFeature world, MapTemplate template, int size) {
		this.world = world;
		this.template = template;
		this.size = size;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("SandboxParams [world=");
		buffer.append(world);
		buffer.append(", template=");
		buffer.append(template);
		buffer.append(", size=");
		buffer.append(size);
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * @return the world
	 */
	public WorldFeature getWorld() {
		return world;
	}

	/**
	 * @return the template
	 */
	public MapTemplate getTemplate() {
		return template;
	}

	/**
	 * @return the map size
	 */
	public int getSize() {
		return size;
	}
}
