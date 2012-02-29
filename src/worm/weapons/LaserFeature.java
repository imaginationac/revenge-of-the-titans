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
package worm.weapons;

import java.util.ArrayList;

import net.puppygames.applet.effects.Effect;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;
import net.puppygames.applet.widgets.Beam;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.GameMap;
import worm.MapRenderer;
import worm.Res;
import worm.Tile;
import worm.Worm;
import worm.WormGameState;
import worm.entities.Gidrah;
import worm.entities.PlayerWeaponInstallation;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.resources.Range;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.sprites.SpriteImage;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.ShortList;
import com.shavenpuppy.jglib.util.Util;

import static org.lwjgl.opengl.GL11.*;



/**
 * The laser weapon
 * @author Cas
 */
public class LaserFeature extends WeaponFeature {

	private static final long serialVersionUID = 1L;

	private static final Rectangle TEMP = new Rectangle();

	private static final GLRenderable SETUP = new GLRenderable() {
		@Override
		public void render() {
			glEnable(GL_TEXTURE_2D);
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		}
	};


	private static final ArrayList<Entity> ENTITIES = new ArrayList<Entity>();

	/** Damage */
	private Range damage;

	/** Beam duration */
	private int duration;

	/** Fade duration */
	private int fadeDuration;

	/** Beam width */
	private float width, innerWidth;

	/** Layer */
	private int layer;

	/** Beam length */
	private float length;

	/** Sweep angle, in degrees */
	private int sweep;

	/** Target only: beam doesn't strike intermediate targets or walls */
	private boolean targetOnly;

	/** Reflection emitter */
	private EmitterFeature reflectionEmitter;

	/** Beam emitter */
	private EmitterFeature beamEmitter;

	/** Color */
	private Color color, innerColor;

	/** Beam emitter sprite overlay */
	private String beamEmitterOverlay;

	private transient SpriteImage beamEmitterOverlayResource;

	/**
	 * Instance of the laser
	 */
	private class LaserInstance extends WeaponInstance {

		private static final long serialVersionUID = 1L;

		private transient LaserBeam beam;
		private double angle;
		private int tick;
		private boolean reverse;

		/**
		 * C'tor
		 * @param entity
		 */
		public LaserInstance(Entity entity) {
			super(entity);
		}

		@Override
		protected void doFire(float targetX, float targetY) {
			// Spawn a beam
			if (beam != null) {
				return;
			}
			reverse = !reverse;
			beam = new LaserBeam(this);
			beam.sx = entity.getMapX() + entity.getOffsetX();
			beam.sy = entity.getMapY() + entity.getOffsetY();
			beam.tx = targetX;
			beam.ty = targetY;
			beam.setOffset(GameScreen.getSpriteOffset());
			beam.spawn(GameScreen.getInstance());

			double dx = beam.tx - beam.sx;
			double dy = beam.ty - beam.sy;
			angle = Math.atan2(dy, dx);
			tick = 0;
		}

		@Override
		public void tick() {
			super.tick();

			if (beam != null) {
				beam.sx = entity.getMapX() + entity.getOffsetX() + entity.getBeamXOffset();
				beam.sy = entity.getMapY() + entity.getOffsetY() + entity.getBeamYOffset();
				if (getEntity() == null || targetOnly) {
					beam.tx = getTargetX();
					beam.ty = getTargetY();
				} else {
					// Sweep angle
					if (tick < duration) {
						tick ++;
						double radians = Math.toRadians(sweep);
						float ratio = (float) tick / (float) duration;
						double newAngle =
							reverse ?
									LinearInterpolator.instance.interpolate((float) (angle + radians), (float) (angle - radians), ratio)
								:
									LinearInterpolator.instance.interpolate((float) (angle - radians), (float) (angle + radians), ratio);
						beam.tx = beam.sx + Math.cos(newAngle) * 100.0;
						beam.ty = beam.sy + Math.sin(newAngle) * 100.0;
					}
				}
				if (!beam.isActive()) {
					beam = null;
				}
			}
		}

		@Override
		public boolean isReady() {
			return super.isReady() && beam == null; // Prevent cooling towers causing beams to overlap
		}

		@Override
		public void remove() {
			if (beam != null) {
				beam.finish();
				beam = null;
			}
		}
	}

	private class LaserBeam extends Effect {

		final ShortList indices = new ShortList();

		final LaserInstance weapon;
		final int mapWidth;
		final int mapHeight;

		Sprite beamEmitterOverlaySprite;
		boolean fading;
		int tick;
		double sx, sy, tx, ty;
		boolean enemyFire;
		boolean aerialTargets;
		boolean harmless;

		transient Beam innerBeam, outerBeam;
		transient Emitter beamStartEmitter;

		class Segment {
			double x0, y0, x1, y1;
			float startRatio, endRatio;
		}

		ArrayList<Segment> segments = new ArrayList<Segment>();

		private LaserBeam(LaserInstance weapon) {
			this.weapon = weapon;
			enemyFire = weapon.entity instanceof Gidrah;
			if (!enemyFire) {
				aerialTargets = ((PlayerWeaponInstallation) weapon.entity).isFiringAtAerialTargets();
			}
			beamStartEmitter = beamEmitter.spawn(GameScreen.getInstance());
			beamStartEmitter.setOffset(GameScreen.getSpriteOffset());
			mapWidth = Worm.getGameState().getMap().getWidth();
			mapHeight = Worm.getGameState().getMap().getHeight();
		}

		@Override
		protected void render() {
			ReadablePoint offset = getOffset();
			int ox, oy;
			if (offset == null) {
				ox = 0;
				oy = 0;
			} else {
				ox = offset.getX();
				oy = offset.getY();
			}

			float alpha;
			if (fading) {
				alpha = LinearInterpolator.instance.interpolate(1.0f, 0.0f, (float) tick / (float) fadeDuration);
			} else {
				alpha = 1.0f;
			}

			float innerAlpha = Math.max(0.0f, alpha - (Util.random() * 0.25f));
			float outerAlpha = Math.max(0.0f, alpha - (Util.random() * 0.25f));
			glRender(SETUP);
			glRender(Res.getLaserTexture());
			for (int i = 0; i < segments.size(); i ++) {
				Segment segment = segments.get(i);
				outerBeam.setLocation((float) segment.x0 + ox, (float) segment.y0 + oy, (float) segment.x1 + ox, (float) segment.y1 + oy);
				innerBeam.setLocation((float) segment.x0 + ox, (float) segment.y0 + oy, (float) segment.x1 + ox, (float) segment.y1 + oy);
				innerBeam.setStartColor(ColorUtil.setAlpha(ColorUtil.premultiply(innerColor, (int) (innerAlpha * segment.startRatio), null), 0, null));
				innerBeam.setEndColor(ColorUtil.setAlpha(ColorUtil.premultiply(innerColor, (int) (innerAlpha * segment.endRatio), null), 0, null));
				outerBeam.setStartColor(ColorUtil.setAlpha(ColorUtil.premultiply(color, (int) (outerAlpha * segment.startRatio), null), 0, null));
				outerBeam.setEndColor(ColorUtil.setAlpha(ColorUtil.premultiply(color, (int) (outerAlpha * segment.endRatio), null), 0, null));
				outerBeam.render(this);
				innerBeam.render(this);
			}
		}

		@Override
		protected void doSpawnEffect() {
			outerBeam = new Beam();
			outerBeam.setWidth(width);
			innerBeam = new Beam();
			innerBeam.setWidth(innerWidth);
		}

		@Override
		public int getDefaultLayer() {
		    return layer;
		}


		@Override
		protected void doTick() {
			WormGameState gameState = Worm.getGameState();
			tick ++;
			double diffx = tx - sx;
			double diffy = ty - sy;
			double angle = Math.atan2(diffy, diffx);
			double dx = Math.cos(angle);
			double dy = Math.sin(angle);
			double x = sx, y = sy;
			segments.clear();
			Segment currentSegment = new Segment();
			currentSegment.x0 = sx;
			currentSegment.y0 = sy;
			currentSegment.startRatio = 255.0f;
			int totalLength = 0;
			Entity lastBounce = null;

			if (targetOnly) {
				// This is the Saturn boss laser
				currentSegment.x1 = tx;
				currentSegment.y1 = ty;
				currentSegment.endRatio = 255.0f;
				totalLength = 1;
				TEMP.setBounds((int) tx, (int) ty, 1, 1);
				ENTITIES.clear();
				Entity.getCollisions(TEMP, ENTITIES);
				int numEntities = ENTITIES.size();

				// Check collision with entities
				inner: for (int j = 0; j < numEntities; j ++) {
					Entity entity = ENTITIES.get(j);
					if (entity.isActive() && entity.isShootable() || !entity.isShootable() && enemyFire && entity.canCollide() && entity.isSolid()) {
						double ddx = entity.getMapX() + entity.getCollisionX() - tx;
						double ddy = entity.getMapY() + entity.getCollisionY() - ty;

						if (entity.isRound()) {
							double dist = Math.sqrt(ddx * ddx + ddy * ddy);
							if (dist > entity.getRadius()) {
								// Missed
								continue inner;
							}
						} else {
							if (!entity.getBounds(TEMP).contains((int) tx, (int) ty)) {
								// Missed
								continue inner;
							}
						}
						if (entity == weapon.getEntity()) {
							continue inner;
						}

						if (!harmless) {
							entity.laserDamage((int) damage.getValue());
							if (enemyFire) {
								harmless = true;
							}
						}
					}
				}

			} else {
				boolean dangerous = false;
				GameMap map = gameState.getMap();
				int tileX = (int) (x / MapRenderer.TILE_SIZE);
				int tileY = (int) (y / MapRenderer.TILE_SIZE);
				outer: for (int i = 0; i < length; i ++) {
					x += dx;
					y += dy;

					Segment lastSegment = currentSegment;

					// Check map collision unless we're firing at aerial targets
					if (!aerialTargets) {
						int newTileX = (int) (x / MapRenderer.TILE_SIZE);
						int newTileY = (int) (y / MapRenderer.TILE_SIZE);
						if (newTileX < 0 || newTileY < 0 || newTileX >= mapWidth || newTileY >= mapHeight) {
							break;
						}

						if (newTileX != tileX || newTileY != tileY) {
							tileX = newTileX;
							tileY = newTileY;

							for (int tileZ = 0; tileZ < GameMap.LAYERS; tileZ ++) {
								Tile t = map.getTile(tileX, tileY, tileZ);
								if (t != null && !t.isBulletThrough()) {
									// Hit a wall
									break outer;
								}
							}
						}
					}

					TEMP.setBounds((int) x, (int) y, 1, 1);
					ENTITIES.clear();
					Entity.getCollisions(TEMP, ENTITIES);
					int numEntities = ENTITIES.size();

					// Check collision with entities
					inner: for (int j = 0; j < numEntities; j ++) {
						Entity entity = ENTITIES.get(j);
						if (entity.isActive() && entity.isShootable() || !entity.isShootable() && enemyFire && entity.canCollide() && entity.isSolid()) {
							if (aerialTargets && (!entity.isFlying() || entity.isLaserOver())) {
								// Ignore ground targets if targeting aerial targets
								continue;
							}
							if (entity == lastBounce) {
								// Ignore last reflection
								continue;
							}
							double ddx = entity.getX() - x;
							double ddy = entity.getY() - y;

							if (entity.isRound()) {
								double dist = Math.sqrt(ddx * ddx + ddy * ddy);
								if (dist > entity.getRadius()) {
									// Missed
									continue inner;
								}
							} else {
								if (!entity.getBounds(TEMP).contains((int) x, (int) y)) {
									// Missed
									continue inner;
								}
							}
							if (!dangerous && entity == weapon.getEntity() || entity.isLaserThrough()) {
								sx = currentSegment.x0 = x;
								sy = currentSegment.y0 = y;
								continue inner;
							} else if (entity.isLaserProof()) {
								// Reflect!
								dangerous = true;
								lastBounce = entity;
								// First record current segment
								segments.add(currentSegment);
								currentSegment.endRatio = LinearInterpolator.instance.interpolate(255.0f, 0.0f, totalLength / length);
								currentSegment = new Segment();
								currentSegment.startRatio = LinearInterpolator.instance.interpolate(255.0f, 0.0f, totalLength / length);
								currentSegment.x0 = lastSegment.x1;
								currentSegment.y0 = lastSegment.y1;
								// Spawn sparks and ting
								Emitter e = reflectionEmitter.spawn(GameScreen.getInstance());
								e.setLocation((int) lastSegment.x1, (int) lastSegment.y1);
								e.setOffset(GameScreen.getSpriteOffset());


								// Reflect
								double angleOfCollision = Math.atan2(ddy, ddx);

								// Now reflect about this angle
								double diff = Math.atan2(dy, dx) - angleOfCollision;
								double reflection = angleOfCollision + Math.PI - diff;
								dx = Math.cos(reflection);
								dy = Math.sin(reflection);
								// Go back to where we were & move one more
								x = lastSegment.x1 + dx;
								y = lastSegment.y1 + dy;

								weapon.entity.onBulletDeflected(entity);
							} else {
								boolean hit = true;
								if (!harmless) {
									hit = entity.laserDamage((int) damage.getValue() + entity.getExtraDamage());
									if (enemyFire) {
										harmless = true;
									}
								}
								if (hit) {
									break outer;
								}
							}
						}

					}
					currentSegment.x1 = x;
					currentSegment.y1 = y;
					totalLength ++;
				}
			}
			if (totalLength > 0) {
				segments.add(currentSegment);
				currentSegment.endRatio = LinearInterpolator.instance.interpolate(255.0f, 0.0f, totalLength / length);

				if (tick > duration && !fading) {
					fading = true;
					tick = 0;
				}

				Emitter e = reflectionEmitter.spawn(GameScreen.getInstance());
				e.setLocation((int) currentSegment.x1, (int) currentSegment.y1);
				e.setOffset(GameScreen.getSpriteOffset());
			} else {
				remove();
			}
		}

		@Override
		protected void doUpdate() {
			if (beamStartEmitter == null) {
				return;
			}
			int ox = getOffset().getX();
			int oy = getOffset().getY();
			beamStartEmitter.setLocation((float) sx + ox, (float) sy + oy);
			if (beamEmitterOverlaySprite == null) {
				beamEmitterOverlaySprite = getScreen().allocateSprite(getScreen());
				beamEmitterOverlaySprite.setImage(beamEmitterOverlayResource);
				beamEmitterOverlaySprite.setLayer(layer + 1);
				beamEmitterOverlaySprite.setColors(color);
			}
			beamEmitterOverlaySprite.setLocation((int) sx + ox, (int) sy + oy);
			beamEmitterOverlaySprite.setScale(Util.random(FPMath.HALF + FPMath.EIGHTH, FPMath.HALF - FPMath.EIGHTH));
		}

		@Override
		public boolean isEffectActive() {
			return !fading || tick < fadeDuration;
		}

		@Override
		public void finish() {
			if (!fading) {
				fading = true;
				tick = 0;
				if (beamStartEmitter != null) {
					beamStartEmitter.remove();
					beamStartEmitter = null;
				}
			}
		}

		@Override
		protected void doRemove() {
			if (beamStartEmitter != null) {
				beamStartEmitter.remove();
				beamStartEmitter = null;
			}
			if (beamEmitterOverlaySprite != null) {
				beamEmitterOverlaySprite.deallocate();
				beamEmitterOverlaySprite = null;
			}
		}
	}

	/**
	 * C'tor
	 * @param name
	 */
	public LaserFeature(String name) {
		super(name);
	}

	@Override
	public WeaponInstance spawn(Entity entity) {
		return new LaserInstance(entity);
	}

	public int getDamage() {
		return (int) damage.getMax();
	}

	@Override
	public boolean isLaser() {
		return true;
	}

	@Override
	protected String getDamageStats() {
		StringBuilder sb = new StringBuilder();
		sb.append((int) damage.getMin() * duration);
		sb.append("-");
		sb.append((int) damage.getMax() * duration);
		return sb.toString();
	}
}
