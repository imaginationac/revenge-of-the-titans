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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.Dimension;

import worm.generator.Scenery;
import worm.tiles.Crystal;
import worm.tiles.Detail;
import worm.tiles.Obstacle;
import worm.tiles.Ruin;

import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.util.Util;

/**
 * Template for map generation
 */
public class SceneryFeature extends Feature implements Scenery {

	private static final long serialVersionUID = 1L;

	private ResourceArray obstacles;
	private ResourceArray roadObstacles;
	private ResourceArray ruins;
	private ResourceArray crystals;

	private int minObstacles;
	private int maxObstacles;

	private int absMaxRuins;
	private int maxRuins;
	private int maxRuinClusters;
	private float minRuinClusterSize;
	private float maxRuinClusterSize;
	private double randomRockyThreshold;
	private String roads;
	private transient ResourceArray roadsArray;

	private transient Map<Dimension, ArrayList<Ruin>> ruinsMap;
	private transient Map<Integer, ArrayList<Crystal>> crystalsMap;

	/**
	 * C'tor
	 */
	public SceneryFeature() {
		setAutoCreated();
	}

	/**
	 * C'tor
	 * @param name
	 */
	public SceneryFeature(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	public double getRandomRockyThreshold() {
		return randomRockyThreshold;
	}

	/**
	 * @return the minObstacles
	 */
	@Override
	public int getMinObstacles() {
		return minObstacles;
	}

	/**
	 * @return the maxObstacles
	 */
	@Override
	public int getMaxObstacles() {
		return maxObstacles;
	}

	@Override
	protected void doCreate() {
		super.doCreate();

		// Populate the ruins map if we've got a ruins tag
		if (ruins != null) {
			ruinsMap = new HashMap<Dimension, ArrayList<Ruin>>();
			for (int i = 0; i < ruins.getNumResources(); i ++) {
				Ruin ruin = (Ruin) ruins.getResource(i);
				if (ruin != null) {
					Dimension key = new Dimension(ruin.getWidth(), ruin.getHeight());
					ArrayList<Ruin> ruinList = ruinsMap.get(key);
					if (ruinList == null) {
						ruinList = new ArrayList<Ruin>(4);
						ruinsMap.put(key, ruinList);
					}
					ruinList.add(ruin);
					//System.out.println("RUINS?:"+ruinList);
				}
			}
		}
		if (crystals != null) {
			crystalsMap = new HashMap<Integer, ArrayList<Crystal>>();
			for (int i = 0; i < crystals.getNumResources(); i ++) {
				Crystal crystal = (Crystal) crystals.getResource(i);
				if (crystal != null) {
					Integer key = new Integer(crystal.getSize());
					ArrayList<Crystal> crystalList = crystalsMap.get(key);
					if (crystalList == null) {
						crystalList = new ArrayList<Crystal>(4);
						crystalsMap.put(key, crystalList);
					}
					crystalList.add(crystal);
					//System.out.println("RUINS?:"+ruinList);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		super.doDestroy();

		ruinsMap = null;
	}

	@Override
	public Ruin getRuin(int width, int height) {
		ArrayList<Ruin> ruinsList = ruinsMap.get(new Dimension(width, height));
		if (ruinsList == null) {
			return null;
		}
		return ruinsList.get(Util.random(0, ruinsList.size() - 1));
	}

	@Override
	public Crystal getCrystal(int size) {
		ArrayList<Crystal> crystalList = crystalsMap.get(new Integer(size));
		if (crystalList == null) {
			assert false : "No crystal of size "+size;
			return null;
		}
		return crystalList.get(Util.random(0, crystalList.size() - 1));
	}

	@Override
	public Obstacle getObstacle() {
		return (Obstacle) obstacles.getResource(Util.random(0, obstacles.getNumResources() - 1));
	}

	@Override
	public Obstacle getRoadObstacle() {
		return (Obstacle) roadObstacles.getResource(Util.random(0, roadObstacles.getNumResources() - 1));
	}

	@Override
	public int getAbsMaxRuins() {
		return absMaxRuins;
	}

	@Override
	public int getMaxRuinClusters() {
		return maxRuinClusters;
	}

	@Override
	public float getMaxRuinClusterSize() {
		return maxRuinClusterSize;
	}

	@Override
	public float getMinRuinClusterSize() {
		return minRuinClusterSize;
	}

	@Override
	public int getMaxRuins() {
		return maxRuins;
	}

	@Override
	public Detail getRoad(boolean n, boolean e, boolean s, boolean w) {
		return (Detail) roadsArray.getResource((n ? 1 : 0) | (e ? 2 : 0) | (s ? 4 : 0) | (w ? 8 : 0));
	}


}
