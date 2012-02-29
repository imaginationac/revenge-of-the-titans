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

import java.util.ArrayList;

import org.lwjgl.util.Point;

import worm.features.PlainsTemplateFeature;

import com.shavenpuppy.jglib.util.Util;

/**
 * Map generator using perlin noise and arbitrary "water" threshold
 */
public class PlainsMapGenerator extends BaseMapGenerator {

	private final PlainsTemplateFeature plainsTemplate;

	/**
	 * C'tor
	 * @param template
	 * @param level
	 * @param levelFeature TODO
	 */
	public PlainsMapGenerator(PlainsTemplateFeature template, MapGeneratorParams mapGeneratorParams) {
		super(template, mapGeneratorParams);

		plainsTemplate = template;
	}

	/* (non-Javadoc)
	 * @see worm.generator.BaseMapGenerator#getMaxSpawnTunnelSize()
	 */
	@Override
	protected float getMaxSpawnTunnelSize() {
		return plainsTemplate.getMaxSpawnTunnelWidth() * getAreaScale();
	}

	/* (non-Javadoc)
	 * @see worm.generator.BaseMapGenerator#getMinSpawnTunnelSize()
	 */
	@Override
	protected float getMinSpawnTunnelSize() {
		return plainsTemplate.getMinSpawnTunnelWidth();
	}

	/* (non-Javadoc)
	 * @see worm.generator.AbstractMapGenerator#generateAreas()
	 */
	@Override
	protected void generateAreas() {
		// Fill with floor
		for (int y = 0; y < getHeight(); y ++) {
			for (int x = 0; x < getWidth(); x ++) {
				if (getValue(x, y) != BASE) {
					setValue(x, y, FLOOR);
				}
			}
		}

		int n = (int) (Util.random(plainsTemplate.getMinBlotches(), plainsTemplate.getMaxBlotches()) * getAreaScale());
		for (int i = 0; i < n; i ++) {
			int x = Util.random(0, getWidth() - 1);
			int y = Util.random(0, getHeight() - 1);
			float r = Util.random() * (plainsTemplate.getMaxBlotchSize() - plainsTemplate.getMinBlotchSize()) + plainsTemplate.getMinBlotchSize();
			int x2 = Util.random(x - 5, x + 5);
			int y2 = Util.random(y - 5, y + 5);
			if (x == x2) {
				x2 += Util.random(0, 1) * 2 - 1;
			}
			if (y == y2) {
				y2 += Util.random(0, 1) * 2 - 1;
			}
			float r2 = Util.random() * plainsTemplate.getMaxBlotchSize() *  - plainsTemplate.getMinBlotchSize() + plainsTemplate.getMinBlotchSize();
			drawLine(x, y, x2, y2, r, r2, WALL);
		}

		// Draw random blotch lines between each base first
		ArrayList<Point> basesCopy = new ArrayList<Point>(getBases());
		for (int i = 0; i < basesCopy.size(); i ++) {
			Point p1 = basesCopy.get(i);
			int j = Util.random(0, basesCopy.size() - 1);
			Point p2 = basesCopy.get(j);
			basesCopy.set(i, p2);
			basesCopy.set(j, p1);
		}

		Point s = basesCopy.get(0), t;
		drawArea(s.getX(), s.getY(), (plainsTemplate.getMinMainTunnelWidth() + plainsTemplate.getMaxMainTunnelWidth()) / 2.0f, FLOOR);
		for (int i = 0; i < basesCopy.size() - 1; i ++) {
			s = basesCopy.get(i);
			t = basesCopy.get(i + 1);
			drawLine(s.getX(), s.getY(), t.getX(), t.getY(), plainsTemplate.getMinMainTunnelWidth(), plainsTemplate.getMaxMainTunnelWidth(), FLOOR);
		}

	}

//	public static void main(String[] args) {
//		PlainsTemplateFeature ptf = new PlainsTemplateFeature() {
//			public float getMaxMainTunnelWidth() {
//				return 8.0f;
//			}
//			public float getMinMainTunnelWidth() {
//				return 1.5f;
//			}
//			public float getMaxSpawnTunnelWidth() {
//				return 8.0f;
//			}
//			public float getMinSpawnTunnelWidth() {
//				return 1.5f;
//			}
//			public float getMinBlotchSize() {
//				return 0.5f;
//			}
//			public float getMaxBlotchSize() {
//				return 3.0f;
//			}
//			public int getMinBlotches() {
//				return 3;
//			}
//			public int getMaxBlotches() {
//				return 30;
//			}
//			public int getAbsMaxRuins() {
//				return 25;
//			}
//			public int getMaxRuinClusters() {
//				return 5;
//			}
//			public float getMinRuinClusterSize() {
//				return 6.0f;
//			}
//			public float getMaxRuinClusterSize() {
//				return 10.0f;
//			}
//			public int getMaxRuins() {
//				return 10;
//			}
//			public Obstacle getObstacle() {
//				return new Obstacle();
//			}
//			public Ruin getRuin(int width, int height) {
//				return new Ruin();
//			}
//
//		};
//		for (int i = 0; i < 50; i ++) {
//			PlainsMapGenerator g = new PlainsMapGenerator(ptf, i, i % WormGameState.LEVELS_IN_WORLD);
//			System.out.println("Generating Level "+i+"------------------------------------------------------------");
//			g.setTest(true);
//			g.generate();
//			//g.dump();
//		}
//	}



}
