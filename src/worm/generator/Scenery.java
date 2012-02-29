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

import worm.tiles.Crystal;
import worm.tiles.Detail;
import worm.tiles.Obstacle;
import worm.tiles.Ruin;

/**
 * Describes scenery
 */
public interface Scenery {

	/**
	 * @return the absolute maximum number of ruins to create
	 */
	public int getAbsMaxRuins();

	/**
	 * @return the maximum number of obstacles to create
	 */
	public int getMaxObstacles();

	/**
	 * @return the maximum number of ruin clusters
	 */
	public int getMaxRuinClusters();

	/**
	 * @return the maximum size of a ruin cluster
	 */
	public float getMaxRuinClusterSize();

	/**
	 * @return the maximum number of ruins to make in a ruin cluster
	 */
	public int getMaxRuins();

	/**
	 * @return the minimum number of obstacles to create
	 */
	public int getMinObstacles();

	/**
	 * @return the minimum size of a ruin cluster
	 */
	public float getMinRuinClusterSize();

	/**
	 * Gets an obstacle tile, which cannot be placed on top of road
	 * @return an obstacle, which must be a destructable obstacle
	 */
	public Obstacle getObstacle();

	/**
	 * Get a road tile that will link up the specified junction
	 * @param n
	 * @param e
	 * @param s
	 * @param w
	 * @return
	 */
	public Detail getRoad(boolean n, boolean e, boolean s, boolean w);

	/**
	 * Gets an obstacle tile which can be placed on a road
	 * @return an obstacle, which must be a destructable obstacle
	 */
	public Obstacle getRoadObstacle();

	/**
	 * Gets a ruin tile of the specified size.
	 * @param width 1 &le; width &le; 3
	 * @param height 1 &le; height &le; 3
	 * @return an obstacle, which must be an indestructable solid ruin; or null, if no such sized ruin is available
	 */
	public Ruin getRuin(int size, int height);

	/**
	 * Gets a crystal tile of the specified size.
	 * @param size 1 &le; width &le; 3
	 * @return a crystal, or null, if no such sized crystal is available
	 */
	public Crystal getCrystal(int size);

	public double getRandomRockyThreshold();

}
