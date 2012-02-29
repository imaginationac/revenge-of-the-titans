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

import java.util.List;

import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Color;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadablePoint;
import org.w3c.dom.Element;

import worm.AttenuatedColor;
import worm.ColorAttenuationConstants;
import worm.MapRenderer;
import worm.Worm;
import worm.animation.ThingWithLayers;

import com.shavenpuppy.jglib.Point2f;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.sprites.Animation;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;
import com.shavenpuppy.jglib.sprites.SpriteImage;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Describes layers of animation
 */
public class LayersFeature extends Feature {

	/**
	 * The name of shadows colours
	 */
	public static final String SHADOW_COLOR_NAME = "shadow".intern();

	private static final long serialVersionUID = 1L;

	private static final Color TEMP = new Color();

	/*
	 * Resource data
	 */

	/** The various layers */
	private Layer[] sprite;
	private EmitterLayer[] emitter;
	private float scale = 1.0f;
	private Point offset;
	private float ySortOffset;

	/*
	 * Transient data
	 */

	/** Whether we have any "colored" sprites */
	private transient boolean hasColored;

	/** Whether any of the colored sprites are attenuated */
	private transient boolean hasAttenuated;

	/** Layers */
	private static class Layer extends Feature {

		private String id;
		private int layer;
		private int subLayer;
		private Point2f offset;
		private float ySortOffset;
		private String animation;
		private String image;
		private boolean doChildOffset;

		// Either we use colored, to color the whole sprite; or we use topColored and bottomColored, to color the top and bottom of the sprite
		private MappedColor colored;
		private MappedColor topColored, bottomColored;

		/** If true, then we'll attenuate the colors with the level colors attenuation colour according to approx. distance from map centre */
		private boolean attenuated;

		private transient Animation animationResource;
		private transient SpriteImage imageResource;

		public Layer() {
			setAutoCreated();
			setSubResource(true);
		}

	}

	/** Emitters */
	private static class EmitterLayer extends Feature {
		private String emitter;
		private Point2f offset;
		private float ySortOffset;
		private boolean doChildOffset;

		private transient EmitterFeature emitterFeature;

		public EmitterLayer() {
			setAutoCreated();
			setSubResource(true);
		}
	}

	/**
	 * C'tor
	 */
	public LayersFeature() {
		setAutoCreated();
	}

	/**
	 * C'tor
	 * @param name
	 */
	public LayersFeature(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> layerElements = XMLUtil.getChildren(element, "sprite");
		if (layerElements.size() > 0) {
			sprite = new Layer[layerElements.size()];
			int count = 0;
			for (Element child : layerElements) {
				sprite[count] = new Layer();
				sprite[count].load(child, loader);
				count ++;
			}
		}

		List<Element> emitterLayerElements = XMLUtil.getChildren(element, "emitter");
		if (emitterLayerElements.size() > 0) {
			emitter = new EmitterLayer[emitterLayerElements.size()];
			int emitterCount = 0;
			for (Element child : emitterLayerElements) {
				emitter[emitterCount] = new EmitterLayer();
				emitter[emitterCount].load(child, loader);
				emitterCount ++;
			}
		}

	}

	@Override
	protected void doCreate() {
		super.doCreate();

		if (sprite != null) {
			for (Layer element : sprite) {
				element.create();
				if (element.colored != null || element.topColored != null && element.bottomColored != null) {
					hasColored = true;
					if (element.attenuated) {
						hasAttenuated = true;
					}
				}
			}
		}
		if (emitter != null) {
			for (EmitterLayer element : emitter) {
				element.create();
			}
		}
	}

	@Override
	protected void doDestroy() {
		super.doDestroy();

		if (sprite != null) {
			for (Layer element : sprite) {
				element.destroy();
			}
		}
		if (emitter != null) {
			for (EmitterLayer element : emitter) {
				element.destroy();
			}
		}
	}

	/**
	 * Find a layer by name
	 * @param id Name; cannot be null
	 * @return an index, or -1
	 */
	public int getLayer(String id) {
		if (sprite == null) {
			return -1;
		}
		for (int i = 0; i < sprite.length; i ++) {
			if (id.equals(sprite[i].id)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Create the sprites for an entity
	 * @param allocator The screen to create the sprites on
	 * @param owner Owner of the sprites
	 */
	public void createSprites(SpriteAllocator allocator, ThingWithLayers owner) {
		createSprites(allocator, 0.0f, 0.0f, owner);
	}

	/**
	 * Create the sprites for an entity
	 * @param allocator The screen to create the sprites on
	 * @param mapX location of the entity's centre
	 * @param mapY location of the entity's centre
	 * @param owner Owner of the sprites
	 */
	public void createSprites(SpriteAllocator allocator, float mapX, float mapY, ThingWithLayers owner) {
		if (sprite == null) {
			owner.setSprites(new Sprite[0]);
			return;
		}

		Sprite[] s = new Sprite[sprite.length];
		float ox, oy;
		if (offset != null) {
			ox = offset.getX();
			oy = offset.getY();
		} else {
			ox = 0.0f;
			oy = 0.0f;
		}
		for (int i = 0; i < s.length; i ++) {
			s[i] = allocator.allocateSprite(owner);
		}
		owner.setSprites(s);
		for (int i = 0; i < s.length; i ++) {
			if (scale != 1.0f) {
				s[i].setScale(FPMath.fpValue(scale));
			}
			s[i].setLayer(sprite[i].layer);
			s[i].setSubLayer(sprite[i].subLayer);
			if (sprite[i].offset != null) {
				s[i].setOffset((sprite[i].offset.getX() + ox) * scale, (sprite[i].offset.getY() + oy) * scale);
			} else {
				s[i].setOffset(ox * scale, oy * scale);
			}
			s[i].setYSortOffset((ySortOffset + sprite[i].ySortOffset) * scale);
			if (sprite[i].doChildOffset) {
				s[i].setDoChildOffset(true);
			}
			if (sprite[i].animationResource != null) {
				s[i].setAnimation(sprite[i].animationResource);
			} else {
				s[i].setImage(sprite[i].imageResource);
			}
		}
		createColors(s);
		updateColors(s, mapX, mapY);
	}

	public void updateLocation(Sprite[] existingSprite, float x, float y) {
		for (Sprite element : existingSprite) {
			element.setLocation(x, y);
		}
	}

	/**
	 * Create the emitters at a specific location
	 * @param screen TODO
	 * @param x
	 * @param y
	 * @return an array of emitters (may be empty but won't be null)
	 */
	public Emitter[] createEmitters(Screen screen, float x, float y) {
		if (emitter == null) {
			return new Emitter[0];
		}
		Emitter[] e = new Emitter[emitter.length];
		for (int i = 0; i < e.length; i ++) {
			if (emitter[i].emitterFeature != null) {
				e[i] = emitter[i].emitterFeature.spawn(screen);
				float ox, oy;
				if (emitter[i].offset != null) {
					ox = emitter[i].offset.getX() * scale;
					oy = emitter[i].offset.getY() * scale;
				} else {
					ox = 0;
					oy = 0;
				}
				e[i].setLocation(x + ox, y + oy);
				e[i].setYOffset(emitter[i].ySortOffset * scale);
			}
		}
		return e;
	}

	/**
	 * Update an existing array of emitters
	 * @param existingEmitters
	 * @param x TODO
	 * @param y TODO
	 */
	public void updateEmitters(Emitter[] existingEmitters, float x, float y) {
		for (int i = 0; i < existingEmitters.length; i ++) {
			if (existingEmitters[i] != null) {
				float ox, oy;
				if (emitter[i].offset != null) {
					ox = emitter[i].offset.getX() * scale;
					oy = emitter[i].offset.getY() * scale;
				} else {
					ox = 0;
					oy = 0;
				}
				existingEmitters[i].setLocation(x + ox, y + oy);
			}
		}
	}

	public void updateScale(Sprite[] existingSprites, float newScale) {
		for (int i = 0; i < existingSprites.length; i ++) {
			existingSprites[i].setScale(FPMath.fpValue(newScale * scale));
			if (sprite[i].offset != null) {
				existingSprites[i].setOffset(sprite[i].offset.getX() * newScale * scale, sprite[i].offset.getY() * newScale * scale);
			}
			existingSprites[i].setYSortOffset(sprite[i].ySortOffset * newScale * scale);
		}
	}

	public void updateColors(Sprite[] existingSprites, float mapX, float mapY) {
		if (!hasColored) {
			return;
		}
		float mapWidth, mapHeight, ratio = 0.0f;
		if (hasAttenuated) {
			mapWidth = Worm.getGameState().getMap().getWidth();
			mapHeight = Worm.getGameState().getMap().getHeight();
			ratio = ColorAttenuationConstants.dist(mapX / MapRenderer.TILE_SIZE, mapY / MapRenderer.TILE_SIZE, mapWidth, mapHeight) / ColorAttenuationConstants.getMaxDist();
		}
		updateColors(existingSprites, ratio);
	}

	public void updateColors(Sprite[] existingSprites, float ratio) {
		if (!hasColored) {
			return;
		}
		updateColors(existingSprites, ratio, ratio, ratio, ratio, 0, 0, 0, 0);
	}

	public void updateColors(Sprite[] existingSprites, float ratio00, float ratio10, float ratio11, float ratio01, int fade00, int fade10, int fade11, int fade01) {
		if (!hasColored) {
			return;
		}
		for (int i = 0; i < existingSprites.length; i ++) {
			if ((sprite[i].colored != null || sprite[i].topColored != null && sprite[i].bottomColored != null) && sprite[i].attenuated) {
				if (existingSprites[i].getColor(0) instanceof AttenuatedColor) { // animcolor command puts ordinary Colors back into sprites. Bah
					((AttenuatedColor) existingSprites[i].getColor(0)).setRatio(ratio00);
					((AttenuatedColor) existingSprites[i].getColor(1)).setRatio(ratio10);
					((AttenuatedColor) existingSprites[i].getColor(2)).setRatio(ratio11);
					((AttenuatedColor) existingSprites[i].getColor(3)).setRatio(ratio01);
					((AttenuatedColor) existingSprites[i].getColor(0)).setFade(fade00);
					((AttenuatedColor) existingSprites[i].getColor(1)).setFade(fade10);
					((AttenuatedColor) existingSprites[i].getColor(2)).setFade(fade11);
					((AttenuatedColor) existingSprites[i].getColor(3)).setFade(fade01);
				}
			}
		}
	}

	private void createColors(Sprite[] existingSprites) {
		if (!hasColored) {
			return;
		}
		for (int i = 0; i < existingSprites.length; i ++) {
			if (sprite[i].colored != null) {
				MappedColor c = sprite[i].colored;
				if (c != null) {
					if (sprite[i].attenuated) {
						if (c.getColorName() != null && c.getColorName().intern() == SHADOW_COLOR_NAME) {
							existingSprites[i].setColor(0, new AttenuatedColor(c, true));
							existingSprites[i].setColor(1, new AttenuatedColor(c, true));
							existingSprites[i].setColor(2, new AttenuatedColor(c, true));
							existingSprites[i].setColor(3, new AttenuatedColor(c, true));
						} else {
							existingSprites[i].setColor(0, new AttenuatedColor(c));
							existingSprites[i].setColor(1, new AttenuatedColor(c));
							existingSprites[i].setColor(2, new AttenuatedColor(c));
							existingSprites[i].setColor(3, new AttenuatedColor(c));
						}
					} else {
						existingSprites[i].setColors(c);
					}
				}
			} else if (sprite[i].topColored != null && sprite[i].bottomColored != null) {
				MappedColor topColor = sprite[i].topColored;
				MappedColor bottomColor = sprite[i].bottomColored;
				if (topColor != null && bottomColor != null) {
					if (sprite[i].attenuated) {
						existingSprites[i].setColor(0, new AttenuatedColor(bottomColor));
						existingSprites[i].setColor(1, new AttenuatedColor(bottomColor));
						existingSprites[i].setColor(2, new AttenuatedColor(topColor));
						existingSprites[i].setColor(3, new AttenuatedColor(topColor));
					} else {
						existingSprites[i].setColor(0, bottomColor);
						existingSprites[i].setColor(1, bottomColor);
						existingSprites[i].setColor(2, topColor);
						existingSprites[i].setColor(3, topColor);
					}
				}
			}
		}
	}

	/**
	 * @return the scale
	 */
	public float getScale() {
		return scale;
	}

	/**
	 * @return the offset
	 */
	public ReadablePoint getOffset() {
		return offset;
	}
}
