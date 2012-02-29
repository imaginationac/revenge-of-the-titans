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
import java.util.Random;

import org.lwjgl.util.Point;

import worm.features.XmasTemplateFeature;

import com.shavenpuppy.jglib.interpolators.CosineInterpolator;
import com.shavenpuppy.jglib.util.Util;

/**
 * Xmas map generator using perlin noise and arbitrary "water" threshold. Has walls to east, west and south.
 */
public class XmasMapGenerator extends BaseMapGenerator {

	private final XmasTemplateFeature xmasTemplate;

	/**
	 * C'tor
	 * @param template
	 * @param level
	 * @param levelFeature TODO
	 */
	public XmasMapGenerator(XmasTemplateFeature template, MapGeneratorParams mapGeneratorParams) {
		super(template, mapGeneratorParams);

		xmasTemplate = template;
	}

	@Override
	protected float getMaxSpawnTunnelSize() {
		return xmasTemplate.getMaxSpawnTunnelWidth();
	}

	@Override
	protected float getMinSpawnTunnelSize() {
		return xmasTemplate.getMinSpawnTunnelWidth();
	}

	@Override
	protected void generateAreas() {
		// Fill with Perlin noise
		PerlinNoise noise = new PerlinNoise(new Random().nextInt(), xmasTemplate.getScale(), xmasTemplate.getOctaves(), xmasTemplate.getPersistence());
		for (int y = 0; y < getHeight(); y ++) {
			for (int x = 0; x < getWidth(); x ++) {
				if (getValue(x, y) != BASE) {
					if (noise.perlinNoise(x, y) > xmasTemplate.getThreshold()) {
						setValue(x, y, WALL);
					} else {
						setValue(x, y, FLOOR);
					}
				}
			}
		}

		// Fill east, west and south
		for (int x = 0; x < getWidth(); x ++) {
			drawArea(x, 0, CosineInterpolator.instance.interpolate(2.0f, 6.0f, (float) Math.random()), WALL);
		}
		for (int y = 0; y < getHeight(); y ++) {
			drawArea(0, y, CosineInterpolator.instance.interpolate(2.0f, 6.0f, (float) Math.random()), WALL);
			drawArea(getWidth() - 1, y, CosineInterpolator.instance.interpolate(2.0f, 6.0f, (float) Math.random()), WALL);
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
		drawArea(s.getX(), s.getY(), (xmasTemplate.getMinMainTunnelWidth() + xmasTemplate.getMaxMainTunnelWidth()) / 2.0f, FLOOR);
		for (int i = 0; i < basesCopy.size() - 1; i ++) {
			s = basesCopy.get(i);
			t = basesCopy.get(i + 1);
			drawLine(s.getX(), s.getY(), t.getX(), t.getY(), xmasTemplate.getMinMainTunnelWidth(), xmasTemplate.getMaxMainTunnelWidth(), FLOOR);
		}

	}

//	public static void main(String[] args) {
//		PerlinNoise noise = new PerlinNoise((int) (Math.random() * Integer.MAX_VALUE), 5, 5, 0.5f);
//		for (int y = 0; y < 64; y ++) {
//			for (int x = 0; x < 64; x ++) {
//				if (noise.perlinNoise(x, y) > 0.20f) {
//					System.out.print('#');
//				} else {
//					System.out.print('.');
//				}
//			}
//			System.out.println();
//		}
//
//	}


}
