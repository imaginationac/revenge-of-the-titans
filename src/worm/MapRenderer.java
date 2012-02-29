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
package worm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Color;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;

import worm.animation.SimpleThingWithLayers;
import worm.features.DecalFeature;
import worm.features.LayersFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.ColorInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLTexture;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;

import static org.lwjgl.opengl.GL11.*;


/**
 * A renderable chunk of map.
 * @author Cas
 */
public class MapRenderer implements MapListener {

	/** The size of a tile */
	public static final int TILE_SIZE = 16;

	public static final int OPAQUE_SIZE = 6;
	public static final int FADE_SIZE = 12;
	private static final short[] FADE_INDICES = {0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15};
	private static final short[] OPAQUE_INDICES = {16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23, 24, 25, 26, 24, 26, 27, 28, 29, 30, 28, 30, 31};

	/** Layers */
	private static final int FLOOR_LAYER = 0;

	private static final int FLOOR_FADE = 4;

	/** Temporary color */
	private static final Color TEMP = new Color();

	/** Dimensions of the renderable screen, in tiles */
	private int tileWidth, tileHeight;

	/** Origin on screen */
	private int originX, originY;

	/** A bunch of TileInfos */
	private TileInfo[] tileInfo;

	/** Fog o' war */
	private TickableObject fogOfWar;

	/** Map we're rendering */
	private GameMap map;

	/** Screen */
	private final Screen screen;

	/** A Map of visible tiles. This maps map coordinates to RenderedTiles. */
	private final Map<Point, RenderedTile> visibleMap = new HashMap<Point, RenderedTile>();

	/** Visibility */
	private boolean visible = true;

	/** Current location, in pixels */
	private int x, y;

	/** Debug */
	private boolean debug;

	/** A RenderedTile contains all the information needed to render a tile. */
	private class RenderedTile implements TileInfo.TileDisplay {
		private Sprite[] tileSprite;
		private Sprite[][] decalSprite;
		private SimpleThingWithLayers[] layersSprite;
		private Emitter[][] layersEmitter;
		private ReadablePoint[][] decalOffset;
		private Emitter emitter;
		private int emitterX, emitterY;
		private Tile[] displayed;
		private final int mapx, mapy;
		private int screenx, screeny;
		private final float ratio00, ratio10, ratio11, ratio01;

		private boolean updated;

		/**
		 * C'tor
		 * @param src
		 */
		RenderedTile(int mapx, int mapy) {
			this.mapx = mapx;
			this.mapy = mapy;
			ratio00 = ColorAttenuationConstants.dist(mapx, mapy, map.getWidth(), map.getHeight()) / ColorAttenuationConstants.getMaxDist();
			ratio10 = ColorAttenuationConstants.dist(mapx + 1, mapy, map.getWidth(), map.getHeight()) / ColorAttenuationConstants.getMaxDist();
			ratio11 = ColorAttenuationConstants.dist(mapx + 1, mapy + 1, map.getWidth(), map.getHeight()) / ColorAttenuationConstants.getMaxDist();
			ratio01 = ColorAttenuationConstants.dist(mapx, mapy + 1, map.getWidth(), map.getHeight()) / ColorAttenuationConstants.getMaxDist();
		}

		void setUpdated() {
			updated = true;
		}

		boolean isUpdated() {
			return updated;
		}

		void updateColors() {
			for (int i = 0; i < displayed.length; i++) {
				// Fade layer 0 tiles
				if (displayed[i] != null) {
					MappedColor color = getColor(displayed[i]);
					Sprite ts = tileSprite[i];
					if (i == 0 || displayed[i] != null && displayed[i].isAttenuated()) {
						if (color.getColorName() != null && color.getColorName().intern() == LayersFeature.SHADOW_COLOR_NAME) {
							ts.setColor(0, new AttenuatedColor(color, map.getFFade(mapx, mapy), FLOOR_FADE, ratio00, 0, true));
							ts.setColor(1, new AttenuatedColor(color, map.getFFade(mapx + 1, mapy), FLOOR_FADE, ratio10, 0, true));
							ts.setColor(2, new AttenuatedColor(color, map.getFFade(mapx + 1, mapy + 1), FLOOR_FADE, ratio11, 0, true));
							ts.setColor(3, new AttenuatedColor(color, map.getFFade(mapx, mapy + 1), FLOOR_FADE, ratio01, 0, true));
						} else {
							ts.setColor(0, new AttenuatedColor(color, map.getFFade(mapx, mapy), FLOOR_FADE, ratio00, 0, false));
							ts.setColor(1, new AttenuatedColor(color, map.getFFade(mapx + 1, mapy), FLOOR_FADE, ratio10, 0, false));
							ts.setColor(2, new AttenuatedColor(color, map.getFFade(mapx + 1, mapy + 1), FLOOR_FADE, ratio11, 0, false));
							ts.setColor(3, new AttenuatedColor(color, map.getFFade(mapx, mapy + 1), FLOOR_FADE, ratio01, 0, false));
						}
					} else {
						ts.setColors(color);
					}
					List<DecalFeature> decals = displayed[i].getDecals();
					if (decals != null) {
						for (int j = 0; j < decalSprite[i].length; j ++) {
							DecalFeature df = decals.get(j);
							if (df != null) {
								MappedColor decalColor = getColor(df);
								Sprite ds = decalSprite[i][j];
								if (df.isAttenuated()) {
									if (decalColor.getColorName() != null && decalColor.getColorName() == LayersFeature.SHADOW_COLOR_NAME) {
										ds.setColor(0, new AttenuatedColor(decalColor, map.getFFade(mapx, mapy), FLOOR_FADE, ratio00, 0, true));
										ds.setColor(1, new AttenuatedColor(decalColor, map.getFFade(mapx + 1, mapy), FLOOR_FADE, ratio10, 0, true));
										ds.setColor(2, new AttenuatedColor(decalColor, map.getFFade(mapx + 1, mapy + 1), FLOOR_FADE, ratio11, 0, true));
										ds.setColor(3, new AttenuatedColor(decalColor, map.getFFade(mapx, mapy + 1), FLOOR_FADE, ratio01, 0, true));
									} else {
										ds.setColor(0, new AttenuatedColor(decalColor, map.getFFade(mapx, mapy), FLOOR_FADE, ratio00, 0, false));
										ds.setColor(1, new AttenuatedColor(decalColor, map.getFFade(mapx + 1, mapy), FLOOR_FADE, ratio10, 0, false));
										ds.setColor(2, new AttenuatedColor(decalColor, map.getFFade(mapx + 1, mapy + 1), FLOOR_FADE, ratio11, 0, false));
										ds.setColor(3, new AttenuatedColor(decalColor, map.getFFade(mapx, mapy + 1), FLOOR_FADE, ratio01, 0, false));
									}
								} else {
									ds.setColors(decalColor);
								}
							}
						}
					}

					LayersFeature layers = displayed[i].getLayers();
					if (layers != null) {
						layers.updateColors(layersSprite[i].getSprites(), ratio00, ratio10, ratio11, ratio01, map.getFFade(mapx, mapy), map.getFFade(mapx, mapy), map.getTFade(mapx, mapy), map.getTFade(mapx, mapy));
					}
				}
			}
		}

		@Override
		public void setTiles(Tile[] tile) {
			updated = false;
			if (tile != null) {
				if (tileSprite == null) {
					tileSprite = new Sprite[tile.length];
					for (int i = 0; i < tileSprite.length; i ++) {
						tileSprite[i] = screen.allocateSprite(screen);
						tileSprite[i].setLayer(FLOOR_LAYER + i);
					}
				}
				if (decalSprite == null) {
					decalSprite = new Sprite[tile.length][];
					decalOffset = new ReadablePoint[tile.length][];
				}
				if (layersSprite == null) {
					layersSprite = new SimpleThingWithLayers[tile.length];
					layersEmitter = new Emitter[tile.length][];
				}

				for (int i = 0; i < tileSprite.length; i ++) {
					if (tile[i] == null) {
						tileSprite[i].setVisible(false);
					} else {
						tile[i].toSprite(tileSprite[i]);

						if (!visible) {
							tileSprite[i].setVisible(false);
						}
					}
				}

				// Maybe there's an emitter?
				EmitterFeature ef = null;
				int emitterLayer = 0;
				for (int i = tile.length; --i >= 0; ) {
					if (tile[i] != null) {
						ef = tile[i].getEmitter();
						emitterLayer = i;
						if (ef != null) {
							break;
						}
					}
				}
				if (emitter == null && ef != null) {
					emitter = ef.spawn(screen);
					Point p = tile[emitterLayer].getEmitterPos();
					if (p != null) {
						emitterX = p.getX();
						emitterY = p.getY();
					}
					emitter.setLocation(mapx * TILE_SIZE + emitterX, mapy * TILE_SIZE + emitterY);
				} else if (emitter != null && ef == null) {
					emitter.remove();
					emitter = null;
				}

				if (emitter != null) {
					emitter.setVisible(visible);
				}

				if (displayed == null) {
					// All the tiles are new
					displayed = new Tile[tile.length];
					for (int i = 0; i < displayed.length; i ++) {
						displayed[i] = tile[i];
						if (displayed[i] != null) {
							// Maybe create decals
							List<DecalFeature> decals = tile[i].getDecals();
							if (decals != null) {
								decalSprite[i] = new Sprite[decals.size()];
								decalOffset[i] = new ReadablePoint[decals.size()];
								for (int j = 0; j < decalSprite[i].length; j ++) {
									DecalFeature df = decals.get(j);
									if (df != null) {
										decalSprite[i][j] = screen.allocateSprite(screen);

										decalSprite[i][j].setScale(FPMath.fpValue(df.getScale()));
										decalSprite[i][j].setLayer(df.getLayer());
										decalSprite[i][j].setSubLayer(df.getSubLayer());
										decalSprite[i][j].setYSortOffset(df.getYSortOffset());
										decalOffset[i][j] = df.getOffset();
										df.getAppearance().toSprite(decalSprite[i][j]);
									}
								}
							}
							if (tile[i].getLayers() != null) {
								layersSprite[i] = new SimpleThingWithLayers(GameScreen.getInstance());
								tile[i].getLayers().createSprites(GameScreen.getInstance(), layersSprite[i]);
								Point p = tile[i].getEmitterPos();
								if (p != null) {
									emitterX = p.getX();
									emitterY = p.getY();
								}
								layersEmitter[i] = tile[i].getLayers().createEmitters(GameScreen.getInstance(), mapx * TILE_SIZE + emitterX, mapy * TILE_SIZE + emitterY);
							}
						}
					}
					updateColors();
				} else {
					// Some tiles are new
					boolean doColorsUpdate = GameScreen.isDiddlerOpen();
					for (int i = 0; i < displayed.length; i++) {
						if (displayed[i] != tile[i]) {
							doColorsUpdate = true;
							// Maybe remove decals
							if (decalSprite[i] != null) {
								for (int j = 0; j < decalSprite[i].length; j ++) {
									if (decalSprite[i][j] != null) {
										decalSprite[i][j].deallocate();
									}
								}
								decalSprite[i] = null;
								decalOffset[i] = null;
							}
							// Maybe remove layers
							if (layersSprite[i] != null) {
								layersSprite[i].remove();
								layersSprite[i] = null;
							}
							// Maybe emitters too
							if (layersEmitter[i] != null) {
								for (int j = 0; j < layersEmitter[i].length; j ++) {
									if (layersEmitter[i][j] != null) {
										layersEmitter[i][j].remove();
									}
								}
								layersEmitter[i] = null;
							}
							// Maybe create decals
							List<DecalFeature> decals = tile[i].getDecals();
							if (decals != null) {
								decalSprite[i] = new Sprite[decals.size()];
								decalOffset[i] = new ReadablePoint[decals.size()];
								for (int j = 0; j < decalSprite[i].length; j ++) {
									DecalFeature df = decals.get(j);
									if (df != null) {
										decalSprite[i][j] = screen.allocateSprite(screen);
										decalSprite[i][j].setLayer(df.getLayer());
										decalSprite[i][j].setSubLayer(df.getSubLayer());
										decalSprite[i][j].setYSortOffset(df.getYSortOffset());
										decalSprite[i][j].setScale(FPMath.fpValue(df.getScale()));
										decalOffset[i][j] = df.getOffset();
										df.getAppearance().toSprite(decalSprite[i][j]);
									}
								}
							}
							// Maybe create layers
							if (tile[i].getLayers() != null) {
								layersSprite[i] = new SimpleThingWithLayers(GameScreen.getInstance());
								tile[i].getLayers().createSprites(GameScreen.getInstance(), layersSprite[i]);
								Point p = tile[i].getEmitterPos();
								if (p != null) {
									emitterX = p.getX();
									emitterY = p.getY();
								}
								layersEmitter[i] = tile[i].getLayers().createEmitters(GameScreen.getInstance(), mapx * TILE_SIZE + emitterX, mapy * TILE_SIZE + emitterY);
							}
							displayed[i] = tile[i];
						}
					}
					if (doColorsUpdate) {
						updateColors();
					}
				}
			} else {
				remove();
			}
		}

		/**
		 * Removes all sprites and emitter associated with this tile
		 */
		void remove() {
			if (tileSprite != null) {
				for (Sprite element : tileSprite) {
					if (element != null) {
						element.deallocate();
					}
				}
				tileSprite = null;
			}
			if (decalSprite != null) {
				for (Sprite[] element : decalSprite) {
					if (element != null) {
						for (int j = 0; j < element.length; j ++) {
							element[j].deallocate();
						}
					}
				}
				decalSprite = null;
				decalOffset = null;
			}
			if (emitter != null) {
				emitter.remove();
				emitter = null;
			}
			for (int i = 0; i < layersSprite.length; i ++) {
				if (layersSprite[i] != null) {
					layersSprite[i].remove();
					layersSprite[i] = null;
				}
			}
			for (int i = 0; i < layersEmitter.length; i ++) {
				if (layersEmitter[i] != null) {
					for (int j = 0; j < layersEmitter[i].length; j ++) {
						if (layersEmitter[i][j] != null) {
							layersEmitter[i][j].remove();
						}
					}
					layersEmitter[i] = null;
				}
			}
			layersSprite = null;
			layersEmitter = null;
			displayed = null;
		}

		/**
		 * Sets the location onscreen
		 * @param sx
		 * @param sy
		 */
		void setLocation(int sx, int sy) {
			screenx = sx;
			screeny = sy;

			if (tileSprite != null) {
				for (Sprite element : tileSprite) {
					if (element != null) {
						element.setLocation(sx, sy);
					}
				}
			}
			if (decalSprite != null) {
				for (int i = 0; i < decalSprite.length; i ++) {
					if (decalSprite[i] != null) {
						for (int j = 0; j < decalSprite[i].length; j ++) {
							if (decalSprite[i][j] != null) {
								decalSprite[i][j].setLocation(sx + decalOffset[i][j].getX(), sy + decalOffset[i][j].getY());
							}
						}
					}
				}
			}
			if (layersSprite != null) {
				for (int i = 0; i < layersSprite.length; i ++) {
					if (layersSprite[i] != null) {
						displayed[i].getLayers().updateLocation(layersSprite[i].getSprites(), sx, sy);
					}
				}
			}
		}

		/**
		 * Debugging
		 */
		void postRender() {
			if (debug) {
				if (map == null) {
					return;
				}
				Tile t0 = map.getTile(mapx, mapy, 0);
				Tile t1 = map.getTile(mapx, mapy, 1);
				Tile t2 = map.getTile(mapx, mapy, 2);
				if (t0 != null && t0.isImpassable() || t1 != null && t1.isImpassable() || t2 != null && t2.isImpassable()) {
					glDisable(GL_TEXTURE_2D);
					glDisable(GL_BLEND);
					glLineWidth(1.0f);
					// We need to work out the screen coordinates of the tile
					glColor3f(1,0,0);
					glBegin(GL_LINE_LOOP);
					glVertex2i(screenx, screeny);
					glVertex2i(screenx + TILE_SIZE, screeny);
					glVertex2i(screenx + TILE_SIZE, screeny + TILE_SIZE);
					glVertex2i(screenx, screeny + TILE_SIZE);
					glEnd();
				}
				if (t0 != null && !t0.isBulletThrough() || t1 != null && !t1.isBulletThrough() || t2 != null && !t2.isBulletThrough()) {
					glDisable(GL_TEXTURE_2D);
					glDisable(GL_BLEND);
					glLineWidth(1.0f);
					// We need to work out the screen coordinates of the tile
					glColor3f(1,0,0);
					glBegin(GL_LINES);
					glVertex2i(screenx, screeny);
					glVertex2i(screenx + TILE_SIZE, screeny + TILE_SIZE);
					glEnd();
				}

				if (map.isAttacking(mapx, mapy)) {
					glDisable(GL_TEXTURE_2D);
					glDisable(GL_BLEND);
					glLineWidth(1.0f);
					// We need to work out the screen coordinates of the tile
					glColor3f(0,1,0);
					glBegin(GL_LINE_LOOP);
					glVertex2i(screenx + 1, screeny + 1);
					glVertex2i(screenx - 1 + TILE_SIZE, screeny + 1);
					glVertex2i(screenx - 1+ TILE_SIZE, screeny + TILE_SIZE - 1);
					glVertex2i(screenx + 1, screeny + TILE_SIZE - 1);
					glEnd();
				}
				if (map.isOccupied(mapx, mapy)) {
					glDisable(GL_TEXTURE_2D);
					glDisable(GL_BLEND);
					glLineWidth(1.0f);
					// We need to work out the screen coordinates of the tile
					glColor3f(0,0,1);
					glBegin(GL_LINE_LOOP);
					glVertex2i(screenx + 2, screeny + 2);
					glVertex2i(screenx - 2 + TILE_SIZE, screeny + 2);
					glVertex2i(screenx - 2 + TILE_SIZE, screeny + TILE_SIZE - 2);
					glVertex2i(screenx + 2, screeny + TILE_SIZE - 2);
					glEnd();
				}

				int danger = map.getDanger(mapx, mapy);
				if (danger > 0) {
					glDisable(GL_TEXTURE_2D);
					glEnable(GL_BLEND);
					glBlendFunc(GL_SRC_ALPHA, GL_ONE);

					float alpha = 0.5f;
					float ratio = danger / 64.0f;
					Color c = ColorInterpolator.interpolate(ReadableColor.BLACK, ReadableColor.WHITE, ratio, LinearInterpolator.instance, new Color());
					glColor4ub(c.getRedByte(), c.getGreenByte(), c.getBlueByte(), (byte) (alpha * 255));
					//glColor4f(danger < 25 ? danger / 25.0f : danger == 0 ? 0.0f : 1.0f, danger >= 25 && danger < 50 ? (danger - 25.0f) / 25.0f : danger >= 50 ? 1.0f : 0.0f, danger >= 50 && danger < 100 ? (danger - 50.0f) / 50.0f : danger >= 100 ? 1.0f : 0.0f, alpha);
					glBegin(GL_QUADS);
					glVertex2i(screenx, screeny);
					glVertex2i(screenx + TILE_SIZE, screeny);
					glVertex2i(screenx + TILE_SIZE, screeny + TILE_SIZE);
					glVertex2i(screenx, screeny + TILE_SIZE);
					glEnd();

				}
			}
		}

	}

	/** Scratch Point used for visibleMap reading */
	private static final Point temp = new Point();

	/**
	 * C'tor
	 * @param screen The screen we're being drawn on
	 */
	public MapRenderer(Screen screen) {
		this.screen = screen;

		fogOfWar = new TickableObject() {

			final GLTexture fadeTexture = Resources.get("edgefade.texture");

			final GLRenderable init = new GLRenderable() {
				@Override
                public void render() {
					glEnable(GL_TEXTURE_2D);
					glEnable(GL_BLEND);
					glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
					glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_BLEND);
					fadeTexture.render();
				}
			};

			final GLRenderable init2 = new GLRenderable() {
				@Override
                public void render() {
					glDisable(GL_TEXTURE_2D);
					glDisable(GL_BLEND);
				}
			};

			@Override
			protected void render() {
				glRender(init);

				// Determine the offset in pixels
				int ox = x - originX;
				int oy = y - originY;

				// Bottom strip
				glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
				glTexCoord2f(0.0f, 0.5f);
				glVertex2f(0 - ox, 0 - oy);
				glTexCoord2f(0.0f, 0.5f);
				glVertex2f(map.getWidth() * TILE_SIZE - ox, 0 - oy);
				glTexCoord2f(0.5f, 0.5f);
				glVertex2f(map.getWidth() * TILE_SIZE - ox, TILE_SIZE * FADE_SIZE - oy);
				glTexCoord2f(0.5f, 0.5f);
				glVertex2f(0 - ox, TILE_SIZE * FADE_SIZE - oy);

				// Left strip
				glTexCoord2f(0.0f, 0.5f);
				glVertex2f(0 - ox, 0 - oy);
				glTexCoord2f(0.5f, 0.5f);
				glVertex2f(TILE_SIZE * FADE_SIZE - ox, 0 - oy);
				glTexCoord2f(0.5f, 0.5f);
				glVertex2f(TILE_SIZE * FADE_SIZE - ox, map.getHeight() * TILE_SIZE - oy);
				glTexCoord2f(0.0f, 0.5f);
				glVertex2f(0 - ox,  map.getHeight() * TILE_SIZE - oy);

				// Top strip
				glTexCoord2f(0.5f, 0.5f);
				glVertex2f(0 - ox, (map.getHeight() - FADE_SIZE - 1) * TILE_SIZE - oy);
				glTexCoord2f(0.5f, 0.5f);
				glVertex2f(map.getWidth() * TILE_SIZE - ox, (map.getHeight() - FADE_SIZE - 1) * TILE_SIZE - oy);
				glTexCoord2f(0.0f, 0.5f);
				glVertex2f(map.getWidth() * TILE_SIZE - ox, (map.getHeight()) * TILE_SIZE - oy);
				glTexCoord2f(0.0f, 0.5f);
				glVertex2f(0 - ox, (map.getHeight()) * TILE_SIZE - oy);

				// Right strip
				glTexCoord2f(0.5f, 0.5f);
				glVertex2f((map.getWidth() - FADE_SIZE - 1) * TILE_SIZE - ox, 0 - oy);
				glTexCoord2f(0.0f, 0.5f);
				glVertex2f((map.getWidth()) * TILE_SIZE - ox, 0 - oy);
				glTexCoord2f(0.0f, 0.5f);
				glVertex2f((map.getWidth()) * TILE_SIZE - ox, map.getHeight() * TILE_SIZE - oy);
				glTexCoord2f(0.5f, 0.5f);
				glVertex2f((map.getWidth() - FADE_SIZE - 1) * TILE_SIZE - ox,  map.getHeight() * TILE_SIZE - oy);

				glRender(GL_TRIANGLES, FADE_INDICES);

				glRender(init2);

				// Bottom opaque strip
				glVertex2f(0 - ox, 0 - oy - TILE_SIZE * OPAQUE_SIZE);
				glVertex2f((map.getWidth()) * TILE_SIZE - ox, 0 - oy - TILE_SIZE * OPAQUE_SIZE);
				glVertex2f((map.getWidth()) * TILE_SIZE - ox, 0 - oy);
				glVertex2f(0 - ox, 0 - oy);

				// Left opaque strip
				glVertex2f(0 - ox - TILE_SIZE * OPAQUE_SIZE, 0 - oy - TILE_SIZE * OPAQUE_SIZE);
				glVertex2f(0 - ox, 0 - oy - TILE_SIZE * OPAQUE_SIZE);
				glVertex2f(0 - ox, (map.getHeight() + OPAQUE_SIZE) * TILE_SIZE - oy);
				glVertex2f(0 - ox - TILE_SIZE * OPAQUE_SIZE,  (map.getHeight() + OPAQUE_SIZE) * TILE_SIZE - oy);

				// Top opaque strip
				glVertex2f(0 - ox, (map.getHeight()) * TILE_SIZE - oy);
				glVertex2f((map.getWidth()) * TILE_SIZE - ox, (map.getHeight()) * TILE_SIZE - oy);
				glVertex2f((map.getWidth()) * TILE_SIZE - ox, (map.getHeight() + OPAQUE_SIZE) * TILE_SIZE - oy);
				glVertex2f(0 - ox, (map.getHeight() + 1 + OPAQUE_SIZE) * TILE_SIZE - oy);

				// Right opaque strip
				glVertex2f((map.getWidth()) * TILE_SIZE - ox, 0 - oy - TILE_SIZE * OPAQUE_SIZE);
				glVertex2f((map.getWidth() + OPAQUE_SIZE) * TILE_SIZE - ox, 0 - oy - TILE_SIZE * OPAQUE_SIZE);
				glVertex2f((map.getWidth() + OPAQUE_SIZE) * TILE_SIZE - ox, (map.getHeight() + OPAQUE_SIZE) * TILE_SIZE - oy);
				glVertex2f((map.getWidth()) * TILE_SIZE - ox,  (map.getHeight() + OPAQUE_SIZE) * TILE_SIZE - oy);

				glRender(GL_TRIANGLES, OPAQUE_INDICES);

			}
		};
		fogOfWar.setLayer(Layers.FOG);
		fogOfWar.spawn(screen);
	}

	/**
	 * Called when the UI is resized
	 */
	public void onResized() {
		int adjustWidth = Game.getWidth() % TILE_SIZE > 0 ? 7 : 6;
		int adjustHeight = Game.getHeight() % TILE_SIZE > 0 ? 7 : 6;
		tileWidth = Game.getWidth() / TILE_SIZE + adjustWidth;
		tileHeight = Game.getHeight() / TILE_SIZE + adjustHeight;
		tileInfo = new TileInfo[tileWidth * tileHeight];
		for (int i = 0; i < tileInfo.length; i ++) {
			tileInfo[i] = new TileInfo();
		}

	}

	/**
	 * Sets the pixel origin of the map
	 */
	public void setOrigin(int originX, int originY) {
		this.originX = originX;
		this.originY = originY;
	}

	/**
	 * Cleanup
	 */
	public void cleanup() {
		setMap(null);
		if (fogOfWar != null) {
			fogOfWar.remove();
			fogOfWar = null;
		}
	}

	/**
	 * Sets the map that we are going to render
	 * @param map The map to set.
	 */
	public void setMap(GameMap map) {
		this.map = map;

		if (map != null) {

			float dx = map.getWidth() / 2.0f;
			float dy = map.getHeight() / 2.0f;
			ColorAttenuationConstants.setMaxDist((float) Math.sqrt(dx * dx + dy * dy));

			map.setListener(this);

		}

		// Reset the display
		reset();
	}

	/**
	 * Reset the display - clears away all the sprites etc.
	 */
	private void reset() {
		for (Entry<Point, RenderedTile> entry : visibleMap.entrySet()) {
			entry.getValue().remove();
		}
		visibleMap.clear();
	}

	/**
	 * @return Returns the map.
	 */
	public GameMap getMap() {
		return map;
	}

	/**
	 * Render the map at the specified pixel coordinates.
	 * @param x
	 * @param y
	 */
	public void render(int newx, int newy) {
		this.x = newx;
		this.y = newy;

		// No map? Draw nothing!
		if (map == null) {
			return;
		}

		// Determine the map coordinates
		int mx = x / MapRenderer.TILE_SIZE;
		int my = y / MapRenderer.TILE_SIZE;

		// Determine the offset in pixels
		int ox = x % MapRenderer.TILE_SIZE - originX;
		int oy = y % MapRenderer.TILE_SIZE - originY;

		// Read in the block of map tiles
		int count = 0;
		for (int yy = my; yy < my + tileHeight; yy ++) {
			for (int xx = mx; xx < mx + tileWidth; xx ++) {
				map.toTileInfo(xx, yy, tileInfo[count ++]);
			}
		}

		// Remove all those tiles that are no longer visible from the visible set,
		// which deallocates the sprites and removes any emitters.
		for (Iterator<Map.Entry<Point, RenderedTile>> i = visibleMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<Point, RenderedTile> entry = i.next();
			Point p = entry.getKey();
			if (p.getX() < mx || p.getX() >= mx + tileWidth || p.getY() < my || p.getY() >= my + tileHeight) {
				RenderedTile rt = entry.getValue();
				rt.remove();
				i.remove();
			}
		}

		// Now add the new tiles
		count = 0;
		for (int yy = my; yy < my + tileHeight; yy ++) {
			for (int xx = mx; xx < mx + tileWidth; xx ++) {
				// See if this tile is already visible.
				temp.setLocation(xx, yy);
				RenderedTile rt = visibleMap.get(temp);
				if (rt == null) {
					// It's not visible on the screen, so let's create one
					rt = new RenderedTile(xx, yy);

					// Plonk it in the visible map
					Point p = new Point(xx, yy);
					visibleMap.put(p, rt);

					// Update with current tileinfo
					tileInfo[count].toDisplay(rt);
				} else if (rt.isUpdated() || GameScreen.isDiddlerOpen()) {
					// Make sure colours update
					tileInfo[count].toDisplay(rt);
				}

				// Set the location on screen
				rt.setLocation((xx - mx) * TILE_SIZE - ox, (yy - my) * TILE_SIZE - oy);
				count ++;
			}
		}
	}

	/**
	 * Called after all sprites are rendered etc.
	 */
	public void postRender() {
		if (!visible) {
			return;
		}

		// Determine the map coordinates
		int mx = x / MapRenderer.TILE_SIZE;
		int my = y / MapRenderer.TILE_SIZE;

		for (int yy = my; yy < my + tileHeight; yy ++) {
			for (int xx = mx; xx < mx + tileWidth; xx ++) {
				// See if this tile is already visible.
				temp.setLocation(xx, yy);
				RenderedTile rt = visibleMap.get(temp);
				if (rt != null) {
					rt.postRender();
				}
			}
		}
	}


	/**
	 * Sets the visibility of the map
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
		if (!visible) {
			reset();
		}
	}

	/**
	 * @return return true if the renderer is visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * @return the width, in tiles, plus 1
	 */
	public int getWidth() {
		return tileWidth;
	}

	/**
	 * @return the height, in tiles, plus 1
	 */
	public int getHeight() {
		return tileHeight;
	}


	/**
	 * @return Returns the originX.
	 */
	public int getOriginX() {
		return originX;
	}

	/**
	 * @return Returns the originY.
	 */
	public int getOriginY() {
		return originY;
	}

	private MappedColor getColor(Tile t) {
		MappedColor ret;
		if (t == null) {
			return new MappedColor(ReadableColor.WHITE);
		} else {
			ret = t.getColor();
		}
		if (ret == null) {
			return new MappedColor(ReadableColor.WHITE);
		}
		return ret;
	}

	private MappedColor getColor(DecalFeature df) {
		MappedColor ret;
		if (df == null) {
			return new MappedColor(ReadableColor.WHITE);
		} else {
			ret = df.getColor();
		}
		if (ret == null) {
			return new MappedColor(ReadableColor.WHITE);
		}
		return ret;
	}

	/**
	 * @param debug the debug to set
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	@Override
	public void onChanged(int x, int y) {
		RenderedTile rt = visibleMap.get(temp);
		if (rt != null) {
			rt.setUpdated();
		}
	}
}
