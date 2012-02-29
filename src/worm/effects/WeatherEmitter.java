package worm.effects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.puppygames.applet.Factory;
import net.puppygames.applet.Game;
import net.puppygames.applet.Pool;
import net.puppygames.applet.Screen;
import net.puppygames.applet.SimplePool;
import net.puppygames.applet.Tickable;

import org.lwjgl.util.Color;
import org.lwjgl.util.Point;

import worm.GameMap;
import worm.MapRenderer;
import worm.Tile;
import worm.Worm;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.Range;
import com.shavenpuppy.jglib.sprites.Animation;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.Util;

/**
 * Handles rain and snow weather effects.
 */
public class WeatherEmitter extends Feature implements Tickable {

	private static final long serialVersionUID = 1L;

	/** Layer */
	private int layer, subLayer;

	/** Initial particle height */
	private float height;

	/** Particle angle */
	private Range angle;

	/** Animation for the particle */
	private Animation animation;

	/** Minimum gravity */
	private float minGravity;

	/** Scale */
	private Range startScale, endScale;

	/** Falling velocity */
	private Range gravity;

	/** Acceleration */
	private Range acceleration;

	/** Modulate X position by cos * time * this value */
	private Range xmod;

	/** HSV */
	private Range startHue, startBrightness, startSaturation;

	/** HSV */
	private Range endHue, endBrightness, endSaturation;

	/** Modulation rate */
	private float modRate;

	/** Wind - x velocity range */
	private Range wind;

	/** Ground animation, when the particle hits the ground */
	private Animation groundAnimation;

	/** Max particles : screen size ratio. Screen logical size area * density = max particles. */
	private float density;

	/** Screen border size (logical units): spawn particles this far outside the visible screen area */
	private int border;

	/*
	 * Transient
	 */

	private transient boolean active;

	private transient Screen screen;

	private transient Pool<WeatherParticle> pool;
	private transient List<WeatherParticle> particles;

	/**
	 * A single weather particle
	 */
	private class WeatherParticle implements Serializable {

		private static final long serialVersionUID = 1L;

		private boolean finished;
		private Sprite sprite;
		private float x, y, z;
		private int tick;
		private float vx, vz;
		private float ox;
		private float accel;
		private float startH, startS, startB;
		private float initialScale, finalScale;
		private float endH, endS, endB;
		private final Color color = new Color();

		/**
		 * C'tor
		 */
		WeatherParticle() {
		}

		void init() {
			finished = false;
		}

		void spawn() {
			finished = false;
			Point spriteOffset = GameScreen.getSpriteOffset();
			x = Util.random(-border, Game.getWidth() + border) - spriteOffset.getX();
			y = Util.random(-border, Game.getHeight() + border) - spriteOffset.getY();
			GameMap map = Worm.getGameState().getMap();
			for (int z = 0; z < GameMap.LAYERS; z ++) {
				Tile t = map.getTile((int) (x / MapRenderer.TILE_SIZE), (int) (y / MapRenderer.TILE_SIZE), z);
				if (t != null && (t.isSolid() || t.isImpassable())) {
					// Don't spawn at all
					return;
				}
			}
			z = height;

			sprite = screen.allocateSprite(screen);
			sprite.setLayer(layer);
			sprite.setSubLayer(subLayer);
			sprite.setAnimation(animation);

			initialScale = startScale.getValue();
			finalScale = endScale.getValue();
			sprite.setAngle(FPMath.fpYaklyDegrees(Math.toRadians(angle.getValue())));

			vx = wind.getValue();
			vz = gravity.getValue();
			ox = xmod.getValue();
			accel = acceleration.getValue();

			startH = startHue.getValue();
			startS = startSaturation.getValue();
			startB = startBrightness.getValue();
			endH = endHue.getValue();
			endS = endSaturation.getValue();
			endB = endBrightness.getValue();

			particles.add(this);
		}

		boolean isActive() {
			return !finished;
		}

		void remove() {
			if (finished) {
				return;
			}
			finished = true;
			if (sprite != null) {
				sprite.deallocate();
				sprite = null;
			}
        }

		void tick() {
			if (sprite.getEvent() == 1) {
				remove();
			} else {
				Point spriteOffset = GameScreen.getSpriteOffset();
				int xoffset = spriteOffset.getX();
				int yoffset = spriteOffset.getY();
				if (x + xoffset < -border * 2 || y + yoffset < -border * 2 || x + xoffset >= border * 2 + Game.getWidth() || y + yoffset >= border * 2 + Game.getHeight()) {
					remove();
				} else if (z != 0.0f) {
					vz += accel;
					if (vz < minGravity) {
						vz = minGravity;
					}
					x += vx;
					z -= vz;
					tick ++;
					if (z <= 0.0f) {
						z = 0.0f;
						if (groundAnimation == null) {
							remove();
						} else {
							sprite.setAnimation(groundAnimation);
						}
					}
					if (sprite != null) {
						float ratio = z / height;
						color.fromHSB
							(
								LinearInterpolator.instance.interpolate(endH, startH, ratio),
								LinearInterpolator.instance.interpolate(endS, startS, ratio),
								LinearInterpolator.instance.interpolate(endB, startB, ratio)
							);
						sprite.setColors(color);
						float currentScale = LinearInterpolator.instance.interpolate(finalScale, initialScale, ratio);
						sprite.setScale(FPMath.fpValue(currentScale));
					}
				}
			}
        }

		void update() {
			Point spriteOffset = GameScreen.getSpriteOffset();
			sprite.setLocation(x + spriteOffset.getX(), y + spriteOffset.getY());
			float xpos = (float) Math.cos(Math.toRadians(tick * modRate)) * ox * vz;
			sprite.setOffset(xpos, z);
        }

	}

	/**
	 * C'tor
	 * @param name
	 */
	public WeatherEmitter(String name) {
		super(name);
	}

	@Override
	public void spawn(Screen screen) {
		this.screen = screen;
		active = true;
		particles = new ArrayList<WeatherParticle>();
		pool = new SimplePool<WeatherParticle>(new Factory<WeatherParticle>() {
			@Override
			public WeatherParticle createNew() {
			    return new WeatherParticle();
			}
		}, 0);
		screen.addTickable(this);
	}

	/**
	 * @return the maximum number of particles we should have onscreen
	 */
	private int getMaxParticles() {
		int fps = Game.getFPS();
		if (fps >= Game.getFrameRate() - 2) {
			return (int) (density * Game.getWidth() * Game.getHeight());
		} else {
			// Cut down snow to 1/10th if framerate falls
			return (int) (0.1f * density * Game.getWidth() * Game.getHeight());
		}
	}

	@Override
	public void tick() {
		// Spawn more weather particles
		int numToSpawn = Math.min(200, getMaxParticles() - particles.size());
		for (int i = 0; i < numToSpawn; i ++) {
			pool.obtain().spawn();
		}

		int n = particles.size();
		for (int i = 0; i < n; ) {
			WeatherParticle p = particles.get(i);
			p.tick();
			if (!p.isActive()) {
				particles.remove(i);
				pool.release(p);
				n --;
			} else {
				i ++;
			}
		}
	}

	@Override
	public void update() {
		int n = particles.size();
		for (int i = 0; i < n; i ++) {
			particles.get(i).update();
		}
	}

	@Override
	public boolean isActive() {
	    return active;
	}

	@Override
	public void remove() {
		if (!active) {
			return;
		}
		active = false;
		for (WeatherParticle p : particles) {
			p.remove();
		}
		particles = null;
		pool = null;
	}

}
