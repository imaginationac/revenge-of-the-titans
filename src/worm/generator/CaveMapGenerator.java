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

import worm.features.CaveTemplateFeature;

import com.shavenpuppy.jglib.algorithms.Bresenham;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.util.Util;

/**
 * Standard map generation algorithm; uses particular rules to place a number of bases and spawnpoints down.
 */
public class CaveMapGenerator extends BaseMapGenerator {

	private final CaveTemplateFeature caveTemplate;

	/**
	 * C'tor
	 * @param template
	 * @param level
	 * @param levelFeature TODO
	 */
	public CaveMapGenerator(CaveTemplateFeature template, MapGeneratorParams mapGeneratorParams) {
		super(template, mapGeneratorParams);

		caveTemplate = template;
	}

	/* (non-Javadoc)
	 * @see worm.generator.BaseMapGenerator#getMaxSpawnTunnelSize()
	 */
	@Override
	protected float getMaxSpawnTunnelSize() {
		return caveTemplate.getMaxSpawnTunnelWidth();
	}

	/* (non-Javadoc)
	 * @see worm.generator.BaseMapGenerator#getMinSpawnTunnelSize()
	 */
	@Override
	protected float getMinSpawnTunnelSize() {
		return caveTemplate.getMinSpawnTunnelWidth();
	}

	/* (non-Javadoc)
	 * @see worm.generator.AbstractMapGenerator#generateAreas()
	 */
	@Override
	protected void generateAreas() {
		// Fill with wall
		for (int y = 0; y < getHeight(); y ++) {
			for (int x = 0; x < getWidth(); x ++) {
				if (getValue(x, y) != BASE) {
					setValue(x, y, WALL);
				}
			}
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
		drawArea(s.getX(), s.getY(), (caveTemplate.getMinMainTunnelWidth() + caveTemplate.getMaxMainTunnelWidth()) / 2.0f, FLOOR);
		for (int i = 0; i < basesCopy.size() - 1; i ++) {
			s = basesCopy.get(i);
			t = basesCopy.get(i + 1);
			drawLine(s.getX(), s.getY(), t.getX(), t.getY(), caveTemplate.getMinMainTunnelWidth(), caveTemplate.getMaxMainTunnelWidth(), FLOOR);
		}

		// Carve out blotches
		int numBlotches = Util.random(3, 5);
		float size = (float) (Util.random() * 8.0) + 1.5f;
		int sx = s.getX();
		int sy = s.getY();
		drawArea(sx, sy, size, FLOOR);
		Bresenham bh = new Bresenham();
		for (int i = 0; i < numBlotches; i ++) {
			// Pick a new random size and location
			float endSize = Util.random() * (caveTemplate.getMaxMainTunnelWidth() - caveTemplate.getMinMainTunnelWidth()) + caveTemplate.getMinMainTunnelWidth();
			int tx = Util.random(OBSTACLE_MARGIN + (int) Math.max(size, endSize), getWidth() - OBSTACLE_MARGIN - (int) Math.max(size, endSize) - 1);
			int ty = Util.random(OBSTACLE_MARGIN + (int) Math.max(size, endSize), getHeight() - OBSTACLE_MARGIN - (int) Math.max(size, endSize) - 1);
			float steps = bh.plot(sx, sy, tx, ty);
			for (int step = 0; bh.next(); step ++) {
				int x = bh.getX();
				int y = bh.getY();
				drawArea(x, y, LinearInterpolator.instance.interpolate(size, endSize, step / steps), FLOOR);
			}
			size = endSize;
			sx = tx;
			sy = ty;
		}

	}

}
