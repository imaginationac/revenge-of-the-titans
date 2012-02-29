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

import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.Range;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sprites.Appearance;

/**
 * $Id: EmitterFeature.java,v 1.11 2010/08/03 20:44:07 foo Exp $
 * The Emitter feature describes a particle effect that is either instantaneous
 * or lasts an amount of time.
 * @author $Author: foo $
 * @version $Revision: 1.11 $
 */
public class EmitterFeature extends Feature {

	private static final long serialVersionUID = 1L;

	private static final int MARGIN = 48;

	/*
	 * Resource data
	 */

	/** Debug tag */
	@Data
	private String tag;

	/** The duration of the emitter. If null then the emitter is removed after 1 emission */
	private Range duration;

	/** Infinite: continue emitting until removed */
	private boolean infinite;

	/** The number of particles to emit per tick */
	private Range particlesPerTick;

	/** Maximum particles to emit before puttering out. */
	private int maxParticles;

	/** Appearance */
	private String appearance;

	/** Particle layer */
	private int layer, subLayer;

	/** Chained emitter */
	private String next;

	/** Velocity */
	private Range velocity;

	/** Acceleration */
	private Range acceleration;

	/** Spawning radius */
	private Range radius;

	/** Initial scale */
	private Range startScale;

	/** Scale at end of particle duration */
	private Range scale;

	/** Final scale at end of fade */
	private Range endScale;

	/** Gravity */
	private Range gravityX, gravityY;

	/** Duration */
	private Range particleDuration;

	/** Fade */
	private Range fadeDuration;

	/** Start hue range */
	private Range startHue;

	/** Saturation */
	private Range startSaturation;

	/** Brightness */
	private Range startBrightness;

	/** End hue range */
	private Range endHue;

	/** Saturation */
	private Range endSaturation;

	/** Brightness */
	private Range endBrightness;

	/** Angle range */
	private Range angle;

	/** Sound effect */
	private String sound;

	/** Start / end pitch */
	private float startPitch, endPitch;

	/** Start / end volume */
	private float startVolume, endVolume;

	/** Embedded next */
	private EmitterFeature chain;

	/** Slave emitter attached to particles */
	private EmitterFeature slave;

	/** Optional coordinates to spawn at */
	private float x, y;

	/** Y offset */
	private float yOffset;

	/** Delay before we start emitting */
	private Range delay;

	/** Delay after we finish emitting, in a loop */
	private Range delayAfter;

	/** Whether particles should be rotated according to their emitted angle */
	private boolean rotate;

	/** Whether particles should rotate according to their own angle as well */
	private boolean relativeRotate;

	/** Interpolation: number of pixels */
	private int interpolation;

	/** Scale all values */
	private Range emitterScale;

	/** Don't attenuate sounds */
	private boolean dontAttenuate;

	/** Don't move Y coordinate; just use sprite offset instead */
	private boolean doYOffset;

	/** Force emission even if offscreen */
	private boolean forceEmit;

	/*
	 * Transient data
	 */

	private transient EmitterFeature nextFeature;
	private transient Appearance appearanceResource;
	private transient ALBuffer soundResource;

	/**
	 * An Emitter effect
	 */
	public class EmitterInstance extends Emitter {

		private boolean delayed;
		private int tick;
		private int actualDuration;
		private int actualDelay;
		private float ix, iy; // initial location
		private float iyo; // Initial Y offset
		private final Screen screen;
		private final EmitterInstance parent;
		private EmitterInstance chainInstance;
		private boolean finished, done;
		private SoundEffect soundEffect;
		private boolean soundWasLooped;
		private float oldx, oldy, oldYoffset;
		private boolean doInterpolate;
		private boolean delayingAfter;
		private float instanceScale;
		private int numParticles;

		private EmitterInstance(EmitterInstance parent, Screen screen) {
			this.screen = screen;
			this.parent = parent;

			if (parent != null) {
				setLocation(parent.ix + EmitterFeature.this.x, parent.iy + EmitterFeature.this.y);
				setYOffset(parent.iyo + EmitterFeature.this.yOffset);
			} else {
				setLocation(EmitterFeature.this.x, EmitterFeature.this.y);
				setYOffset(EmitterFeature.this.yOffset);
			}


		}

		@Override
		protected void doSpawnEffect() {
			initEmitter();
		}

		private EmitterFeature getFeature() {
			return EmitterFeature.this;
		}

		@Override
		public void setYOffset(float yOffset) {
			this.iyo = yOffset;
			if (chainInstance != null) {
				chainInstance.setYOffset(yOffset + chainInstance.getFeature().yOffset);
			}
		}

		@Override
		protected void playSound(ALBuffer sound) {
			// Override to stop sounds playing here.
		}

		@Override
		public String getTag() {
			return tag;
		}

		private void initEmitter() {
			if (delay == null) {
				actualDelay = 0;
				if (soundResource != null) {
					if (soundEffect == null || !soundWasLooped) {
						if (getAttenuator() != null && !dontAttenuate) {
							soundEffect = Game.allocateSound(soundResource, getAttenuator().getVolume(ix, iy), 1.0f, this);
						} else {
							soundEffect = Game.allocateSound(soundResource, 1.0f, 1.0f, this);
						}
						soundWasLooped = soundResource.isLooped();
					}
				}
			} else {
				actualDelay = (int) delay.getValue();
				delayed = actualDelay > 0;
			}

			if (duration == null) {
				actualDuration = 0;
			} else {
				actualDuration = (int) duration.getValue();
			}

			// Maybe spawn some chained emitters?
			if (nextFeature != null) {
				if (chainInstance != null) {
					chainInstance.remove();
					chainInstance = null;
				}
				chainInstance = (EmitterInstance) nextFeature.spawn(this, screen);
			}
			if (chain != null) {
				if (chainInstance != null) {
					chainInstance.remove();
					chainInstance = null;
				}
				chainInstance = (EmitterInstance) chain.spawn(this, screen);
			}

			// Choose scale at this point
			instanceScale = emitterScale == null ? 1.0f : emitterScale.getValue();

			doInterpolate = false;
			numParticles = 0;
		}

		@Override
		public void setLocation(float x, float y) {
			this.ix = x;
			this.iy = y;
			if (chainInstance != null) {
				chainInstance.setLocation(x + chainInstance.getFeature().x, y + chainInstance.getFeature().y);
			}
		}

		@Override
		public void setOffset(ReadablePoint offset) {
			super.setOffset(offset);

			// Also set offset of chained emitter instances if any
			if (chainInstance != null) {
				chainInstance.setOffset(offset);
			}
		}

		@Override
		public void setGain(float gain) {
			this.gain = gain;
			if (soundEffect != null) {
				if (getAttenuator() != null && !dontAttenuate) {
					soundEffect.setGain(soundResource.getGain() * gain * Game.getSFXVolume() * getAttenuator().getVolume(ix, iy + iyo), this);
				} else {
					soundEffect.setGain(soundResource.getGain() * gain * Game.getSFXVolume(), this);
				}
			}
		}

		@Override
		protected void doUpdate() {
			// Update sound gain
			if (soundEffect == null || soundResource == null) {
				return;
			}
			if (getAttenuator() != null && !dontAttenuate) {
				soundEffect.setGain(soundResource.getGain() * gain * Game.getSFXVolume() * getAttenuator().getVolume(ix, iy + iyo), this);
			}
		}

		@Override
		protected void doTick() {
			if (finished) {
				return;
			}

			tick ++;
			float xx = ix, yy = iy, oyy = iyo;

			if (delayed) {
				if (tick <= actualDelay) {
					return;
				}
				delayed = false;
				tick = 0;
				if (delayingAfter) {
					delayingAfter = false;
					initEmitter();
				} else if (soundResource != null) {
					if (soundEffect != null && soundWasLooped) {
						soundEffect.setLooped(false, this);
						soundEffect = null;
					}
					if (getAttenuator() != null && !dontAttenuate) {
						soundEffect = Game.allocateSound(soundResource, getAttenuator().getVolume(xx, yy + oyy), 1.0f, this);
					} else {
						soundEffect = Game.allocateSound(soundResource, 1.0f, 1.0f, this);
					}
					soundWasLooped = soundResource.isLooped();
				}
			}

			if (getInterpolation() == 0 || !doInterpolate) {
				// Just emit at the new location
				emit(xx, yy, oyy);
				doInterpolate = true;
			} else {
				// Interpolate
				double dx = xx - oldx;
				double dy = yy - oldy;
				double doy = oyy - oldYoffset;
				double dist = Math.sqrt(dx * dx + dy * dy + doy * doy);
				int steps = (int) (dist / getInterpolation() + 0.5);
				for (int i = 1; i <= steps && (maxParticles == 0 || numParticles < maxParticles); i++) {
					float ratio = (float) i / (float) steps;
					float xxx = LinearInterpolator.instance.interpolate(oldx, xx, ratio);
					float yyy = LinearInterpolator.instance.interpolate(oldy, yy, ratio);
					float oyyy = LinearInterpolator.instance.interpolate(oldYoffset, oyy, ratio);
					emit(xxx, yyy, oyyy);
				}
			}

			// Remember last position
			oldx = xx;
			oldy = yy;
			oldYoffset = iyo;
		}

		private void emit(float xx, float yy, float oyy) {
			if (isVisible()) {
				int n = particlesPerTick == null ? 0 : (int) (particlesPerTick.getValue() + 0.5f);

				// If emitter is way outside screen bounds, emit nothing
				ReadablePoint offset = getOffset();
				float offsetXpos, offsetYpos;
				if (offset != null) {
					offsetXpos = offset.getX() + xx;
					offsetYpos = offset.getY() + yy + oyy;
				} else {
					offsetXpos = xx;
					offsetYpos = yy + oyy;
				}
				if (!forceEmit && (offsetXpos < -MARGIN || offsetYpos < -MARGIN || offsetXpos > screen.getWidth() + MARGIN || offsetYpos > screen.getHeight() + MARGIN)) {
					numParticles += n;
				} else {
					for (int i = 0; i < n && (maxParticles == 0 || numParticles < maxParticles); i ++) {
						numParticles ++;
						Color startColor;
						if (startHue == null) {
							startColor = new Color(ReadableColor.WHITE);
						} else {
							startColor = new Color();
							startColor.fromHSB(
									startHue.getValue(),
									startSaturation == null ? 1.0f : startSaturation.getValue(),
									startBrightness == null ? 1.0f : startBrightness.getValue());
						}
						Color endColor;
						if (endHue == null) {
							endColor = startColor;
						} else {
							endColor = new Color();
							endColor.fromHSB(
									endHue.getValue(),
									endSaturation == null ? 1.0f : endSaturation.getValue(),
									endBrightness == null ? 1.0f : endBrightness.getValue());
						}
						float xxx = xx, yyy = yy, oyyy = oyy;
						if (radius != null) {
							float r = radius.getValue() * instanceScale;
							double randomRadiusAngle = Math.random() * 2.0 * Math.PI;
							xxx += r * Math.cos(randomRadiusAngle);
							if (doYOffset) {
								yyy = yy;
								oyyy += r * Math.sin(randomRadiusAngle);
							} else {
								yyy += r * Math.sin(randomRadiusAngle);
							}
						}
						Particle p = Particle.POOL.obtain
							(
								this,
								xxx, yyy, oyyy,
								EmitterFeature.this.angle == null ? (float) Math.random() * 360.0f : EmitterFeature.this.angle.getValue() + angle,
								velocity == null ? 0.0f : velocity.getValue() * instanceScale,
								acceleration == null ? 0.0f : acceleration.getValue() * instanceScale,
								(int) particleDuration.getValue(),
								(int) fadeDuration.getValue(),
								startColor,
								endColor
							);
						p.setAx(gravityX == null ? 0.0f : gravityX.getValue() * instanceScale);
						p.setAy(gravityY == null ? 0.0f : gravityY.getValue() * instanceScale);
						p.setAppearance(appearanceResource);
						p.setLayer(layer);
						p.setSubLayer(subLayer);
						p.setDoYOffset(doYOffset);
						p.setStartScale(startScale == null ? instanceScale : startScale.getValue() * instanceScale);
						p.setEndScale(endScale == null ? instanceScale : endScale.getValue() * instanceScale);
						if (EmitterFeature.this.scale != null) {
							p.setScale(EmitterFeature.this.scale.getValue() * instanceScale);
						} else {
							p.setScale(instanceScale);
						}
						if (ceilingSet) {
							p.setCeiling(ceiling);
						}
						if (floorSet) {
							p.setFloor(floor);
						}
						if (leftWallSet) {
							p.setLeftWall(leftWall);
						}
						if (rightWallSet) {
							p.setRightWall(rightWall);
						}
						if (rotate) {
							p.setRotation(p.getAngle());
						}
						p.setRelativeRotation(relativeRotate);
						if (slave != null) {
							p.setForced(true);
						}
						p.spawn(screen);
						if (slave != null && isVisible()) {
							Emitter slaveEmitter = slave.spawn(screen);
							slaveEmitter.setOffset(getOffset());
							p.setEmitter(slaveEmitter);
							slaveEmitter.update();
						}
					}
				}
			}
			if (infinite) {
				if (actualDuration > 0 && tick >= actualDuration) {
					tick = 0;
					if (delayAfter != null) {
						actualDelay = (int) delayAfter.getValue();
						delayed = actualDelay > 0;
						delayingAfter = delayed;
					} else {
						// Back to the beginning
						initEmitter();
					}
				}
			} else {
				if (tick >= actualDuration) {
					finish();
				} else if (soundEffect != null) {
					float ratio = (float) tick / (float) (actualDuration + 1);
					if (startVolume != endVolume) {
						soundEffect.setGain(LinearInterpolator.instance.interpolate(startVolume, endVolume, ratio), Game.class);
					}
					if (startPitch != endPitch) {
						soundEffect.setPitch(LinearInterpolator.instance.interpolate(startPitch, endPitch, ratio), Game.class);
					}
				}
				if (maxParticles > 0 && numParticles >= maxParticles) {
					finish();
				}
			}

			oldx = xx;
			oldy = yy;
			oldYoffset = iyo;



		}

		@Override
		protected void doRemove() {
			done = true;
			if (soundEffect != null) {
				soundEffect.setLooped(false, this);
			}
			soundEffect = null;

			// Remove chained emitters
			if (chainInstance != null) {
				chainInstance.finish();
				chainInstance = null;
			}
		}

		@Override
		public void finish() {
			// Stop emission and wait for sound effect to finish. If there's no sound effect, we're done
			if (finished) {
				return;
			}
			finished = true;
			if (soundEffect == null) {
				remove();
			} else {
				soundEffect.setLooped(false, this);
			}

		}

		@Override
		public boolean isEffectActive() {
			return !done;
		}

	}

	/**
	 * C'tor
	 */
	public EmitterFeature() {
	}

	/**
	 * @param name
	 */
	public EmitterFeature(String name) {
		super(name);
	}

	/**
	 * Spawn an Emitter effect.
	 * @param screen
	 * @return an Emitter
	 */
	public Emitter spawn(Screen screen) {
		Emitter ret = new EmitterInstance(null, screen);
		ret.spawn(screen);
		return ret;
	}

	/**
	 * Spawn a chained Emitter effect.
	 * @param screen
	 * @return an Emitter
	 */
	private Emitter spawn(EmitterInstance parent, Screen screen) {
		Emitter ret = new EmitterInstance(parent, screen);
		ret.spawn(screen);
		return ret;
	}

	/**
	 * @return Returns the acceleration.
	 */
	public Range getAcceleration() {
		return acceleration;
	}

	/**
	 * @param acceleration The acceleration to set.
	 */
	public void setAcceleration(Range acceleration) {
		this.acceleration = acceleration;
	}

	/**
	 * @return Returns the angle.
	 */
	public Range getAngle() {
		return angle;
	}

	/**
	 * @param angle The angle to set.
	 */
	public void setAngle(Range angle) {
		this.angle = angle;
	}

	/**
	 * @return Returns the appearance.
	 */
	public String getAppearance() {
		return appearance;
	}

	/**
	 * @param appearance The appearance to set.
	 */
	public void setAppearance(String appearance) {
		this.appearance = appearance;
	}

	/**
	 * @return Returns the chain.
	 */
	public EmitterFeature getChain() {
		return chain;
	}

	/**
	 * @param chain The chain to set.
	 */
	public void setChain(EmitterFeature chain) {
		this.chain = chain;
	}

	/**
	 * @return Returns the duration.
	 */
	public Range getDuration() {
		return duration;
	}

	/**
	 * @param duration The duration to set.
	 */
	public void setDuration(Range duration) {
		this.duration = duration;
	}

	/**
	 * @return Returns the endBrightness.
	 */
	public Range getEndBrightness() {
		return endBrightness;
	}

	/**
	 * @param endBrightness The endBrightness to set.
	 */
	public void setEndBrightness(Range endBrightness) {
		this.endBrightness = endBrightness;
	}

	/**
	 * @return Returns the endHue.
	 */
	public Range getEndHue() {
		return endHue;
	}

	/**
	 * @param endHue The endHue to set.
	 */
	public void setEndHue(Range endHue) {
		this.endHue = endHue;
	}

	/**
	 * @return Returns the endPitch.
	 */
	public float getEndPitch() {
		return endPitch;
	}

	/**
	 * @param endPitch The endPitch to set.
	 */
	public void setEndPitch(float endPitch) {
		this.endPitch = endPitch;
	}

	/**
	 * @return Returns the endSaturation.
	 */
	public Range getEndSaturation() {
		return endSaturation;
	}

	/**
	 * @param endSaturation The endSaturation to set.
	 */
	public void setEndSaturation(Range endSaturation) {
		this.endSaturation = endSaturation;
	}

	/**
	 * @return Returns the endScale.
	 */
	public Range getEndScale() {
		return endScale;
	}

	/**
	 * @param endScale The endScale to set.
	 */
	public void setEndScale(Range endScale) {
		this.endScale = endScale;
	}

	/**
	 * @return Returns the endVolume.
	 */
	public float getEndVolume() {
		return endVolume;
	}

	/**
	 * @param endVolume The endVolume to set.
	 */
	public void setEndVolume(float endVolume) {
		this.endVolume = endVolume;
	}

	/**
	 * @return Returns the fadeDuration.
	 */
	public Range getFadeDuration() {
		return fadeDuration;
	}

	/**
	 * @param fadeDuration The fadeDuration to set.
	 */
	public void setFadeDuration(Range fadeDuration) {
		this.fadeDuration = fadeDuration;
	}

	/**
	 * @return Returns the gravityX.
	 */
	public Range getGravityX() {
		return gravityX;
	}

	/**
	 * @param gravityX The gravityX to set.
	 */
	public void setGravityX(Range gravityX) {
		this.gravityX = gravityX;
	}

	/**
	 * @return Returns the gravityY.
	 */
	public Range getGravityY() {
		return gravityY;
	}

	/**
	 * @param gravityY The gravityY to set.
	 */
	public void setGravityY(Range gravityY) {
		this.gravityY = gravityY;
	}

	/**
	 * @return Returns the infinite.
	 */
	public boolean isInfinite() {
		return infinite;
	}

	/**
	 * @param infinite The infinite to set.
	 */
	public void setInfinite(boolean infinite) {
		this.infinite = infinite;
	}

	/**
	 * @return Returns the particleDuration.
	 */
	public Range getParticleDuration() {
		return particleDuration;
	}

	/**
	 * @param particleDuration The particleDuration to set.
	 */
	public void setParticleDuration(Range particleDuration) {
		this.particleDuration = particleDuration;
	}

	/**
	 * @return Returns the particlesPerTick.
	 */
	public Range getParticlesPerTick() {
		return particlesPerTick;
	}

	/**
	 * @param particlesPerTick The particlesPerTick to set.
	 */
	public void setParticlesPerTick(Range particlesPerTick) {
		this.particlesPerTick = particlesPerTick;
	}

	/**
	 * @return Returns the radius.
	 */
	public Range getRadius() {
		return radius;
	}

	/**
	 * @param radius The radius to set.
	 */
	public void setRadius(Range radius) {
		this.radius = radius;
	}

	/**
	 * @return Returns the scale.
	 */
	public Range getScale() {
		return scale;
	}

	/**
	 * @param scale The scale to set.
	 */
	public void setScale(Range scale) {
		this.scale = scale;
	}

	/**
	 * @param startScale the startScale to set
	 */
	public void setStartScale(Range startScale) {
		this.startScale = startScale;
	}

	/**
	 * @return the startScale
	 */
	public Range getStartScale() {
		return startScale;
	}

	/**
	 * @return Returns the slave.
	 */
	public EmitterFeature getSlave() {
		return slave;
	}

	/**
	 * @param slave The slave to set.
	 */
	public void setSlave(EmitterFeature slave) {
		this.slave = slave;
	}

	/**
	 * @return Returns the sound.
	 */
	public String getSound() {
		return sound;
	}

	/**
	 * @param sound The sound to set.
	 */
	public void setSound(String sound) {
		this.sound = sound;
	}

	/**
	 * @return Returns the startBrightness.
	 */
	public Range getStartBrightness() {
		return startBrightness;
	}

	/**
	 * @param startBrightness The startBrightness to set.
	 */
	public void setStartBrightness(Range startBrightness) {
		this.startBrightness = startBrightness;
	}

	/**
	 * @return Returns the startHue.
	 */
	public Range getStartHue() {
		return startHue;
	}

	/**
	 * @param startHue The startHue to set.
	 */
	public void setStartHue(Range startHue) {
		this.startHue = startHue;
	}

	/**
	 * @return Returns the startPitch.
	 */
	public float getStartPitch() {
		return startPitch;
	}

	/**
	 * @param startPitch The startPitch to set.
	 */
	public void setStartPitch(float startPitch) {
		this.startPitch = startPitch;
	}

	/**
	 * @return Returns the startSaturation.
	 */
	public Range getStartSaturation() {
		return startSaturation;
	}

	/**
	 * @param startSaturation The startSaturation to set.
	 */
	public void setStartSaturation(Range startSaturation) {
		this.startSaturation = startSaturation;
	}

	/**
	 * @return Returns the startVolume.
	 */
	public float getStartVolume() {
		return startVolume;
	}

	/**
	 * @param startVolume The startVolume to set.
	 */
	public void setStartVolume(float startVolume) {
		this.startVolume = startVolume;
	}

	/**
	 * @param layer
	 */
	public void setLayer(int layer) {
		this.layer = layer;
	}

	/**
	 * @return
	 */
	public int getLayer() {
		return layer;
	}

	/**
	 * @return Returns the velocity.
	 */
	public Range getVelocity() {
		return velocity;
	}

	/**
	 * @param velocity The velocity to set.
	 */
	public void setVelocity(Range velocity) {
		this.velocity = velocity;
	}

	/**
	 * @param delay the delay to set
	 */
	public void setDelay(Range delay) {
		this.delay = delay;
	}

	/**
	 * @return the delay
	 */
	public Range getDelay() {
		return delay;
	}

	/**
	 * @return the delayAfter
	 */
	public Range getDelayAfter() {
		return delayAfter;
	}

	/**
	 * @param delayAfter the delayAfter to set
	 */
	public void setDelayAfter(Range delayAfter) {
		this.delayAfter = delayAfter;
	}

	/**
	 * @return the relativeRotate flag
	 */
	public boolean isRelativeRotate() {
		return relativeRotate;
	}

	/**
	 * @return the rotate flag
	 */
	public boolean getRotate() {
		return rotate;
	}

	/**
	 * @param rotate the rotate to set
	 */
	public void setRotate(boolean rotate) {
		this.rotate = rotate;
	}

	/**
	 * @param relativeRotate the relativeRotate to set
	 */
	public void setRelativeRotate(boolean relativeRotate) {
		this.relativeRotate = relativeRotate;
	}

	/**
	 * @param interpolation the interpolation to set
	 */
	public void setInterpolation(int interpolation) {
		this.interpolation = interpolation;
	}

	/**
	 * @return the interpolation
	 */
	public int getInterpolation() {
		return interpolation;
	}

	/**
	 * @return the emitterScale
	 */
	public Range getEmitterScale() {
		return emitterScale;
	}

	/**
	 * @param emitterScale the emitterScale to set
	 */
	public void setEmitterScale(Range emitterScale) {
		this.emitterScale = emitterScale;
	}

	/**
	 * @param dontAttenuate the dontAttenuate to set
	 */
	public void setDontAttenuate(boolean dontAttenuate) {
		this.dontAttenuate = dontAttenuate;
	}

	/**
	 * @return the dontAttenuate
	 */
	public boolean isDontAttenuate() {
		return dontAttenuate;
	}

	/**
	 * @return the doYOffset
	 */
	public boolean isDoYOffset() {
		return doYOffset;
	}

	/**
	 * @param doYOffset the doYOffset to set
	 */
	public void setDoYOffset(boolean doYOffset) {
		this.doYOffset = doYOffset;
	}

	/**
	 * @return the subLayer
	 */
	public int getSubLayer() {
		return subLayer;
	}

	/**
	 * @param subLayer the subLayer to set
	 */
	public void setSubLayer(int subLayer) {
		this.subLayer = subLayer;
	}

	public int getMaxParticles() {
		return maxParticles;
	}

	public void setMaxParticles(int maxParticles) {
		this.maxParticles = maxParticles;
	}

}
