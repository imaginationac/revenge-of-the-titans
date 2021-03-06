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
package worm.tiles;


import worm.EntitySpawningFeature;
import worm.MapProcessor;
import worm.Tile;
import worm.buildings.CrystalFeature;

/**
 * A tile that spawns a crystal building and then erases itself.
 * @author Cas
 */
public class Crystal extends Tile {

	/*
	 * Feature data
	 */

	/** Crystal feature to spawn */
	private String crystal;

	/** Size (1, 2, 3) */
	private int size;

	/** Width / height in tiles */
	private int width, height;

	/*
	 * Transient data
	 */

	private transient CrystalFeature crystalFeature;

	/**
	 * C'tor
	 */
	public Crystal() {
	}

	/**
	 * C'tor
	 * @param name
	 */
	public Crystal(String name) {
		super(name);
	}

	@Override
	public void process(MapProcessor processor, int x, int y) {
		processor.spawnEntity(crystalFeature, x, y);
	}

	/**
	 * @return Returns the gidrahFeature.
	 */
	public EntitySpawningFeature getCrystalFeature() {
		return crystalFeature;
	}

	@Override
	public boolean isSolid() {
		return true;
	}

	public int getSize() {
		return size;
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}
}
