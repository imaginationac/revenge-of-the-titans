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

import worm.generator.MapGenerator;
import worm.generator.MapGeneratorParams;
import worm.generator.PerlinMapGenerator;

/**
 * Template for map generation
 */
public class PerlinTemplateFeature extends BaseTemplateFeature {

	private static final long serialVersionUID = 1L;

	private int scale, octaves;
	private float persistence, threshold;

	private float minMainTunnelWidth;
	private float maxMainTunnelWidth;
	private float minSpawnTunnelWidth;
	private float maxSpawnTunnelWidth;

	/**
	 * C'tor
	 */
	public PerlinTemplateFeature() {
		setAutoCreated();
	}

	public int getScale() {
	    return scale;
    }

	public int getOctaves() {
	    return octaves;
    }

	public float getPersistence() {
	    return persistence;
    }

	public float getThreshold() {
	    return threshold;
    }

	/**
	 * C'tor
	 * @param name
	 */
	public PerlinTemplateFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return the minMainTunnelWidth
	 */
	public float getMinMainTunnelWidth() {
		return minMainTunnelWidth;
	}

	/**
	 * @return the maxMainTunnelWidth
	 */
	public float getMaxMainTunnelWidth() {
		return maxMainTunnelWidth;
	}

	/**
	 * @return the minSpawnTunnelWidth
	 */
	public float getMinSpawnTunnelWidth() {
		return minSpawnTunnelWidth;
	}

	/**
	 * @return the maxSpawnTunnelWidth
	 */
	public float getMaxSpawnTunnelWidth() {
		return maxSpawnTunnelWidth;
	}

	/* (non-Javadoc)
	 * @see worm.generator.MapTemplate#createGenerator(int)
	 */
	@Override
	public MapGenerator createGenerator(MapGeneratorParams mapGeneratorParams) {
		return new PerlinMapGenerator(this, mapGeneratorParams);
	}

}
