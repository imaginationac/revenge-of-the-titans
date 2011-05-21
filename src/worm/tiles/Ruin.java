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

import worm.Tile;


/**
 * A ruin. Might be shot over but not walked through.
 * @author Cas
 */
public class Ruin extends Tile {

	private boolean bulletThrough;

	private String bThruMap;

	/** Size in tiles */
	private int width, height;

	/** Whether roads should be drawn to this ruin */
	private boolean roads;

	/**
	 * C'tor
	 */
	public Ruin() {
	}

	/**
	 * C'tor
	 * @param name
	 */
	public Ruin(String name) {
		super(name);
	}



	//chaz hack! set bulletThrough to bThruMap(0) - this the right place to stick it, in doCreate? ... gosh it works :)

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doCreate()
	 */
	@Override
	protected void doCreate() {
		super.doCreate();

		if (bThruMap!=null){
			String v = String.valueOf(bThruMap.charAt(0));
			bulletThrough = v.equals("0");
		}
	}


	/**
	 * @return true if we should draw roads to this ruin
	 */
	public boolean getRoads() {
		return roads;
	}

	/* (non-Javadoc)
	 * @see tomb.Tile#isImpassable()
	 */
	@Override
	public boolean isImpassable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see worm.Tile#isSolid()
	 */
	@Override
	public boolean isSolid() {
		return true;
	}

	/* (non-Javadoc)
	 * @see worm.Tile#isBulletThrough()
	 */
	@Override
	public boolean isBulletThrough() {
		return bulletThrough;
	}

	public String getBThruMap() {
		return bThruMap;
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
