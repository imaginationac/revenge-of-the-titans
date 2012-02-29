/*
 * Copyright (c) 2004 Covalent Software Ltd
 * All rights reserved.
 */
package net.puppygames.applet.effects;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

import com.shavenpuppy.jglib.interpolators.ColorInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;

/**
 * $Id: SimpleBaseEffect.java,v 1.5 2010/03/24 23:18:25 foo Exp $
 * @version $Revision: 1.5 $
 * @author $Author: foo $
 * <p>
 */
public abstract class SimpleBaseEffect extends Effect {

	/** Colour cache */
	private final Color cachedColor = new Color();

	/** Duration */
	private int duration, fadeDuration;

	/** Tick */
	private int tick;

	/** Fade tick */
	private int fadeTick;

	/** Hue */
	private float hue;

	/** Whether to wibble the colours */
	private boolean coloured;

	/** Location */
	private float x, y;

	/** Size */
	private int width, height;

	/** Velocity */
	private float vx, vy;

	/** Acceleration */
	private float ax, ay;

	/** Finished? */
	private boolean finished;

	/** Start & end color */
	private ReadableColor startColor, endColor;

	/** Alpha */
	private int alpha = 255;

	/**
	 * C'tor
	 */
	public SimpleBaseEffect(ReadableColor startColor, ReadableColor endColor, int duration, int fadeDuration) {
		this.startColor = startColor;
		this.endColor = endColor;
		this.duration = duration;
		this.fadeDuration = fadeDuration;
	}

	/**
	 * @param alpha the alpha to set
	 */
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	/**
	 * @return the alpha
	 */
	public int getAlpha() {
		return alpha;
	}

	/**
	 * @return Returns the cachedColor.
	 */
	public Color getCachedColor() {
		return cachedColor;
	}

	/**
	 * @return Returns the x.
	 */
	public final float getX() {
		return x;
	}

	/**
	 * @return Returns the y.
	 */
	public final float getY() {
		return y;
	}

	/**
	 * @return Returns the width.
	 */
	public final int getWidth() {
		return width;
	}

	/**
	 * @return Returns the height.
	 */
	public final int getHeight() {
		return height;
	}

	/**
	 * Sets the size of the effect (for the purposes of centering etc)
	 * @param width
	 * @param height
	 */
	protected final void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
    public final boolean isEffectActive() {
		return !isStarted() || !finished || fadeTick <= fadeDuration;
	}

	/**
	 * Special wibbly colour
	 * @param coloured
	 */
	public final void setColoured(boolean coloured) {
		this.coloured = coloured;
	}

	/**
	 * @return
	 */
	public final boolean isColoured() {
		return coloured;
	}

	@Override
	protected final void doTick() {
		setLocation(x + vx, y + vy);
		vx += ax;
		vy += ay;

		doSimpleTick();

		if (isFinished()) {
			fadeTick ++;
			float ratio = (float) fadeTick / (float) fadeDuration;
			cachedColor.set(
					endColor.getRed(),
					endColor.getGreen(),
					endColor.getBlue(),
					(int) (endColor.getAlpha() * (1.0f - ratio))
					);
		} else {
			tick ++;
			if (tick >= duration && duration != 0) {// || (x < (-width / 2) || y < (-height / 2) || x > Game.getWidth() + (width / 2) || y > Game.getHeight() + (height / 2))) {
				finished = true;
			} else if (coloured) {
				hue += 0.01f;
			}
			if (duration != 0) {
				float ratio = (float) tick / (float) duration;
				ColorInterpolator.interpolate(startColor, endColor, ratio, LinearInterpolator.instance, cachedColor);
			} else {
				cachedColor.setColor(startColor);
			}
		}

		cachedColor.setAlpha((cachedColor.getAlpha() * alpha) / 255);
	}
	protected void doSimpleTick() {}

	/**
	 * Finish the effect prematurely
	 */
	@Override
	public final void finish() {
		finished = true;
	}

	/**
	 * @return true if the effect is finished OR inactive
	 */
	@Override
	public boolean isFinished() {
		return finished || !isActive();
	}

	/**
	 * Set the location of the text
	 * @param x, y
	 */
	public void setLocation(float x, float y) {
		this.x = x;
		this.y = y;
		doSetLocation();
	}
	protected void doSetLocation() {}

	/**
	 * @param vx
	 * @param vy
	 */
	public void setVelocity(float vx, float vy) {
		this.vx = vx;
		this.vy = vy;
	}

	/**
	 * @param ax
	 * @param ay
	 */
	public void setAcceleration(float ax, float ay) {
		this.ax = ax;
		this.ay = ay;
	}

	protected int getFadeTick() {
		return fadeTick;
	}

	protected int getFadeDuration() {
		return fadeDuration;
	}
}
