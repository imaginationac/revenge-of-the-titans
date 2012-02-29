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
package worm.features;

import worm.Tile;
import worm.generator.MapTemplate;
import worm.tiles.BasePoint;
import worm.tiles.FloorTile;
import worm.tiles.SpawnPoint;

import com.shavenpuppy.jglib.resources.Feature;

/**
 * Template for map generation
 */
abstract class BaseTemplateFeature extends Feature implements MapTemplate {

	private static final long serialVersionUID = 1L;

	private String base;
	private String eastSpawn;
	private String westSpawn;
	private String northSpawn;
	private String southSpawn;
	private String midSpawn;
	private String floor0;
	private String floor1;
	private String floor2;
	private String floor3;
	private String floorTransitions;
	private String wall, internalWall;

	private boolean bigInternalWalls;

	private transient BasePoint baseResource;
	private transient FloorTile floor0Resource;
	private transient FloorTile floor1Resource;
	private transient FloorTile floor2Resource;
	private transient FloorTile floor3Resource;
	private transient SpawnPoint eastSpawnResource;
	private transient SpawnPoint westSpawnResource;
	private transient SpawnPoint northSpawnResource;
	private transient SpawnPoint southSpawnResource;
	private transient SpawnPoint midSpawnResource;
	private transient TileSetFeature wallResource;
	private transient TileSetFeature internalWallResource;
	private transient TileSetFeature floorTransitionsResource;

	/**
	 * C'tor
	 */
	public BaseTemplateFeature() {
		setAutoCreated();
	}

	/**
	 * C'tor
	 * @param name
	 */
	public BaseTemplateFeature(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	public BasePoint getBase() {
		return baseResource;
	}

	public SpawnPoint getEastSpawn() {
		return eastSpawnResource;
	}

	@Override
	public short getFill() {
		return 0;
	}

	@Override
	public FloorTile getFloor0() {
		return floor0Resource;
	}

	@Override
	public FloorTile getFloor1() {
		return floor1Resource;
	}

	@Override
	public FloorTile getFloor2() {
		return floor2Resource;
	}

	@Override
	public FloorTile getFloor3() {
		return floor3Resource;
	}

	@Override
	public SpawnPoint getMidSpawn() {
		return midSpawnResource;
	}

	public SpawnPoint getNorthSpawn() {
		return northSpawnResource;
	}

	public SpawnPoint getSouthSpawn() {
		return southSpawnResource;
	}

	public SpawnPoint getWestSpawn() {
		return westSpawnResource;
	}

	@Override
	public Tile getWall(int neighbours) {
		return wallResource.getTiles().get(neighbours);
	}

	@Override
	public Tile getFloorTransitions(int neighbours) {
		return floorTransitionsResource.getTiles().get(neighbours);
	}

	@Override
	public Tile getInternalWall(int neighbours) {
		return internalWallResource.getTiles().get(neighbours);
	}

	@Override
	public boolean getBigInternalWalls() {
		return bigInternalWalls;
	}


}
