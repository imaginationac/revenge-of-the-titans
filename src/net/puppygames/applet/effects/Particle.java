/*
 * Copyright (c) 2003 Shaven Puppy Ltd
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
package net.puppygames.applet.effects;


import java.util.ArrayList;

import net.puppygames.applet.Res;
import net.puppygames.applet.Screen;
import net.puppygames.applet.Tickable;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;

import com.shavenpuppy.jglib.interpolators.ColorInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;

/**
 * $Id: Particle.java,v 1.11 2010/08/03 23:43:39 foo Exp $
 *
 * @author $Author: foo $
 * @version $Revision: 1.11 $
 */
public class Particle implements Tickable {

	private final Color color = new Color();

//	private static final Map countMap = new HashMap();
//	private static class Int {
//		int value;
//	}

	static class ParticlePool {
		private final ArrayList<Particle> pool = new ArrayList<Particle>(1024);

		Particle obtain(Emitter parent, float x, float y, float yOffset, float angle, float velocity, float acceleration, int duration,
				int fadeDuration, ReadableColor startColor, ReadableColor endColor) {

			Particle ret;

			if (pool.size() == 0) {
				ret = new Particle();
				ret.fromPool = true;
			} else {
				ret = pool.remove(pool.size() - 1);
				assert ret.fromPool : "Node not from pool found in pool!";
				assert ret.pooled : "Node wasn't pooled but we found it in the pool!";
				ret.pooled = false;
			}

			ret.init(parent, x, y, yOffset, angle, velocity, acceleration, duration,
					fadeDuration, startColor, endColor);

			return ret;
		}

		private void release(Particle particle) {
			assert particle.fromPool : "Node not from pool attempt to place in pool!";
			assert !particle.pooled : "Node should already be in the pool!";
			particle.pooled = true;
			pool.add(particle);
		}

	}

	public static final ParticlePool POOL = new ParticlePool();

	/**
	 * Particle cap. When we reach half this value, only every other particle
	 * is spawned. When we reach 3/4 this value, only every fourth particle is
	 * spawned.
	 */
	private static int maxParticles = 1024;

	/**
	 * Pixel cap, as per particle cap; -1 means no pixel cap
	 */
	private static int maxPixels = -1;

	/** Total pixels */
	private static int totalPixels;

	/** Number of particles */
	private static int numParticles;

	/** Particle falloff count */
	private static int falloffCount;

	/** Parent emitter */
	private Emitter parent;

	private String tag;

	boolean fromPool, pooled;
	float x, y, angle, velocity, acceleration;
	float ax, ay, vx, vy;
	int tick, duration, fadeDuration;
	ReadableColor startColor, endColor;
	boolean fading, finished;
	float scale = 1.0f, endScale = 1.0f, startScale = 1.0f;
	Sprite sprite;
	Appearance appearance;
	Emitter emitter;
	boolean floorSet, ceilingSet, leftWallSet, rightWallSet;
	float rotation;
	boolean relativeRotation;
	float floor, ceiling, leftWall, rightWall;
	int layer = 1, subLayer = 0;
	int size = 0;
	float yOffset;
	boolean doYOffset;
	boolean forced;

	private Particle() {
	}

	private void init(Emitter parent, float x, float y, float yOffset, float angle, float velocity, float acceleration, int duration,
			int fadeDuration, ReadableColor startColor, ReadableColor endColor) {
		this.parent = parent;
		this.tag = parent.getTag();
		this.x = x;
		this.y = y;
		this.yOffset = yOffset;
		this.angle = angle;
		this.velocity = velocity;
		this.acceleration = acceleration;
		this.duration = duration;
		this.fadeDuration = fadeDuration;
		this.startColor = startColor;
		this.endColor = endColor;

		ax = 0.0f;
		ay = 0.0f;
		vx = 0.0f;
		vy = 0.0f;
		tick = 0;
		fading = false;
		finished = false;
		scale = endScale = startScale = 1.0f;
		sprite = null;
		appearance = null;
		emitter = null;
		floorSet = false;
		ceilingSet = false;
		leftWallSet = false;
		rightWallSet = false;
		floor = 0.0f;
		ceiling = 0.0f;
		leftWall = 0.0f;
		rightWall = 0.0f;
		layer = 1;
		subLayer = 0;
		rotation = 0.0f;
		relativeRotation = false;
		size = 0;
		doYOffset = false;
		forced = false;
	}

	public void setForced(boolean forced) {
		this.forced = forced;
	}

	/**
	 * @param relativeRotation the relativeRotation to set
	 */
	public void setRelativeRotation(boolean relativeRotation) {
		this.relativeRotation = relativeRotation;
	}

	/**
	 * @param rotation the rotation to set
	 */
	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public void setAppearance(Appearance appearance) {
		this.appearance = appearance;
	}

	public void setLayer(int layer) {
		this.layer = layer;
	}

	public void setAx(float ax) {
		this.ax = ax;
	}

	public void setAy(float ay) {
		this.ay = ay;
	}

	public void setEmitter(Emitter emitter) {
		this.emitter = emitter;
		emitter.setLocation(x, y);
		emitter.setYOffset(yOffset);
		emitter.setOffset(parent.getOffset());
	}

	@Override
	public void spawn(Screen screen) {
		if (emitter != null) {
			emitter.setLocation(x, y);
			emitter.setYOffset(yOffset);
		}
		if (!forced) {
			if (numParticles > maxParticles) {
				return;
			} else if (numParticles > maxParticles * 0.75f) {
				falloffCount ++;
				if (falloffCount < 3) {
					return;
				} else {
					falloffCount = 0;
				}
			} else if (numParticles > maxParticles * 0.66f) {
				falloffCount ++;
				if (falloffCount < 2) {
					return;
				} else {
					falloffCount = 0;
				}
			}
			// Now check fill rate
			if (maxPixels != -1) {
				if (totalPixels > maxPixels) {
					return;
				} else if (totalPixels > maxPixels * 0.75f) {
					falloffCount ++;
					if (falloffCount < 3) {
						return;
					} else {
						falloffCount = 0;
					}
				} else if (totalPixels > maxPixels * 0.66f) {
					falloffCount ++;
					if (falloffCount < 2) {
						return;
					} else {
						falloffCount = 0;
					}
				}
			}
		}

		sprite = screen.allocateSprite(screen);
		if (sprite != null) {
			if (appearance != null) {
				sprite.setAppearance(appearance);
			} else {
				sprite.setImage(Res.getParticleImage());
			}
			sprite.setVisible(parent.isVisible());
			screen.addTickable(this);
			sprite.setLayer(layer);
			sprite.setSubLayer(subLayer);
			sprite.setLocation(x, y);
			sprite.setOffset(0.0f, yOffset);
			numParticles ++;
//			if (parent.getTag() != null) {
//				Int i = (Int) countMap.get(parent.getTag());
//				if (i == null) {
//					i = new Int();
//					countMap.put(parent.getTag(), i);
//				}
//				i.value ++;
//			}

		}
	}

	@Override
	public void remove() {
		if (sprite != null) {
			sprite.deallocate();
			sprite = null;
			numParticles --;
//			totalPixels -= size;
//			if (parent.getTag() != null) {
//				Int i = (Int) countMap.get(parent.getTag());
//				i.value --;
//			}
		}
		if (emitter != null) {
			emitter.remove();
			emitter = null;
		}
		POOL.release(this);
	}

	/**
	 * @return the number of particles currently in use
	 */
	public static int getNumParticles() {
		return numParticles;
	}

	/**
	 * @return the totalPixels
	 */
	public static int getTotalPixels() {
		return totalPixels;
	}

//	/**
//	 * @return the number of particles currently in use with a particular tag
//	 */
//	public static int getNumParticles(String tag) {
//		Int ret = (Int) countMap.get(tag);
//		if (ret == null) {
//			return 0;
//		} else {
//			return ret.value;
//		}
//	}

	@Override
	public void tick() {
		tick ++;
		float dx = velocity * (float) Math.cos(Math.toRadians(angle));
		float dy = velocity * (float) Math.sin(Math.toRadians(angle));
		velocity += acceleration;
		if (velocity <= 0.0f) {
			velocity = 0.0f;
			acceleration = 0.0f;
		}
		vx += ax;
		vy += ay;
		x += dx + vx;
		if (doYOffset) {
			yOffset += dy + vy;
		} else {
			y += dy + vy;
		}
		if (leftWallSet && x < leftWall) {
			vx = -vx;
			dx = -dx;
			angle = (float) Math.atan2(dy + vy, dx + vx);
			x = leftWall + leftWall - x;
		} else if (rightWallSet && x > rightWall) {
			vx = -vx;
			dx = -dx;
			angle = (float) Math.atan2(dy + vy, dx + vx);
			x = rightWall - (x - rightWall);
		}
		if (doYOffset) {
			if (floorSet && yOffset + y < floor) {
				vy = -vy;
				dy = -dy;
				angle = (float) Math.atan2(dy + vy, dx + vx);
				yOffset = floor + floor - yOffset; // FIXME
			} else if (ceilingSet && yOffset + y > ceiling) {
				vy = -vy;
				dy = -dy;
				angle = (float) Math.atan2(dy + vy, dx + vx);
				yOffset = ceiling - (yOffset - ceiling);
			}
		} else {
			if (floorSet && y < floor) {
				vy = -vy;
				dy = -dy;
				angle = (float) Math.atan2(dy + vy, dx + vx);
				y = floor + floor - y;
			} else if (ceilingSet && y > ceiling) {
				vy = -vy;
				dy = -dy;
				angle = (float) Math.atan2(dy + vy, dx + vx);
				y = ceiling - (y - ceiling);
			}
		}
		if (emitter != null) {
			emitter.setLocation(x, y);
			emitter.setYOffset(yOffset);
		}
		if (fading) {
			float ratio = (float) tick / (float) fadeDuration;
			color.setColor(endColor);
			color.setAlpha(255 - (int)(endColor.getAlpha() * ratio));
			sprite.setScale(FPMath.fpValue(LinearInterpolator.instance.interpolate(scale, endScale, ratio)));
			if (tick == fadeDuration) {
				finished = true;
				tick = 0;
			}
		} else {
			float ratio = (float) tick / (float) duration;
			ColorInterpolator.interpolate(
					startColor,
					endColor,
					ratio,
					LinearInterpolator.instance,
					color
					);
			sprite.setScale(FPMath.fpValue(LinearInterpolator.instance.interpolate(startScale, scale, ratio)));
			if (tick >= duration) {
				fading = true;
				tick = 0;
			}
		}
		sprite.setColors(color);
		sprite.setAngle(FPMath.fpYaklyDegrees(Math.toRadians(relativeRotation ? angle + rotation : rotation)));

//		// Calculate number of pixels
//		totalPixels -= size;
//		SpriteImage image = sprite.getImage();
//		if (image != null) {
//			int imageWidth = (int) (FPMath.floatValue(scale) * image.getWidth());
//			int imageHeight = (int) (FPMath.floatValue(scale) * image.getHeight());
//			size = imageWidth * imageHeight;
//		} else {
//			size = 0;
//		}
//		totalPixels += size;

	}

	@Override
	public void update() {
		ReadablePoint offset = parent.getOffset();
		int xx = (int) x;
		int yy = (int) y;
		if (offset != null) {
			xx += offset.getX();
			yy += offset.getY();
		}
		if (doYOffset) {
			sprite.setLocation(xx, yy);
			sprite.setOffset(0.0f, yOffset);
		} else {
			sprite.setLocation(xx, yy);
		}
		sprite.setVisible(parent.isVisible());
	}

	/**
	 * @param doYOffset the doYOffset to set
	 */
	public void setDoYOffset(boolean doYOffset) {
		this.doYOffset = doYOffset;
	}

	@Override
	public boolean isActive() {
		return sprite != null && !finished;
	}

	public void setCeiling(float ceiling) {
		this.ceiling = ceiling;
		ceilingSet = true;
		if (emitter != null) {
			emitter.setCeiling(ceiling);
		}
	}

	public void setFloor(float floor) {
		this.floor = floor;
		floorSet = true;
		if (emitter != null) {
			emitter.setFloor(floor);
		}
	}

	public void setLeftWall(float leftWall) {
		this.leftWall = leftWall;
		leftWallSet = true;
		if (emitter != null) {
			emitter.setLeftWall(leftWall);
		}
	}

	public void setRightWall(float rightWall) {
		this.rightWall = rightWall;
		rightWallSet = true;
		if (emitter != null) {
			emitter.setRightWall(rightWall);
		}
	}

	/**
	 * Sets the particle size, in 16:16FP
	 * @param scale
	 */
	public void setScale(float scale) {
		this.scale = scale;
	}

	public void setEndScale(float endScale) {
		this.endScale = endScale;
	}

	public void setStartScale(float startScale) {
		this.startScale = startScale;
	}

	/**
	 * Sets the maximum number of particles allowed
	 * @param maxParticles
	 */
	public static void setMaxParticles(int maxParticles) {
		Particle.maxParticles = maxParticles;
	}

	/**
	 * @param maxPixels the maxPixels to set (-1 for unlimited, the default)
	 */
	public static void setMaxPixels(int maxPixels) {
		Particle.maxPixels = maxPixels;
	}

	/**
	 * @return the maxParticles
	 */
	public static int getMaxParticles() {
		return maxParticles;
	}

	/**
	 * @return the maxPixels
	 */
	public static int getMaxPixels() {
		return maxPixels;
	}

	/**
	 * @return the angle that the particle is moving at
	 */
	public float getAngle() {
		return angle;
	}

	/**
	 * @param subLayer the subLayer to set
	 */
	public void setSubLayer(int subLayer) {
		this.subLayer = subLayer;
	}
}
