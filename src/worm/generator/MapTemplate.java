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
package worm.generator;

import worm.Tile;
import worm.tiles.BasePoint;
import worm.tiles.FloorTile;
import worm.tiles.SpawnPoint;


/**
 * Describes bits and bobs available to a {@link MapGenerator}
 */
public interface MapTemplate {

	/**
	 * Construct and return a {@link MapGenerator} which uses this MapTemplate.
	 * @param level The level to generate a map for (0..49)
	 * @param levelInWorld The level in the world (0..9)
	 * @param levelFeature The levelfeature itself
	 * @return a {@link MapGenerator}
	 */
	public MapGenerator createGenerator(MapGeneratorParams mapGeneratorParams);

	/**
	 * @return the fill tile index; this will either be a wall or a floor
	 */
	public short getFill();

	/**
	 * @return a tile which will spawn gidrahs at any location other than the edge of the map
	 */
	public SpawnPoint getMidSpawn();

	/**
	 * @return floor type 0
	 */
	public FloorTile getFloor0();

	/**
	 * @return floor type 1
	 */
	public FloorTile getFloor1();

	/**
	 * @return floor type 2
	 */
	public FloorTile getFloor2();

	/**
	 * @return floor type 3
	 */
	public FloorTile getFloor3();

	/**
	 * @param neighbours
	 * @return floor transition
	 */
	public Tile getFloorTransitions(int neighboars);

	/**
	 * @return a tile that will
	 */
	public BasePoint getBase();

	/**
	 * @param neighbours
	 * @return the wall tile to use when there are a specified number of wall neighbours
	 */
	public Tile getWall(int neighbours);

	/**
	 * Get wall that is buried deep in layers of other wall
	 * @return a Tile
	 */
	public Tile getInternalWall(int neighbours);

	/**
	 * extra big internal wall tiles?
	 */
	public boolean getBigInternalWalls();

}
