/*
 * Copyright (c) 2004 Covalent Software Ltd
 * All rights reserved.
 */
package net.puppygames.applet.effects;

import net.puppygames.applet.Screen;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

import com.shavenpuppy.jglib.interpolators.ColorInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.resources.ColorSequenceWrapper;

import static org.lwjgl.opengl.GL11.*;


/**
 * $Id: StrobeEffect.java,v 1.3 2010/03/29 22:39:43 foo Exp $
 * @version $Revision: 1.3 $
 * @author $Author: foo $
 */
public class StrobeEffect extends Effect {

	/** Single instance */
	private static StrobeEffect instance;

	private static final short[] INDICES = {0, 1, 2, 3};

	/** Cached color */
	private final Color cachedColor = new Color();

	/** Color */
	private ReadableColor color, finalColor;

	/** .. or color sequence */
	private ColorSequenceWrapper sequence;

	/** Duration */
	private int duration;

	/** Tick */
	private int tick;

	/**
	 * Private c'tor
	 */
	private StrobeEffect() {
		if (instance != null) {
			instance.remove();
			instance = this;
		}
	}

	/**
	 * C'tor
	 */
	public StrobeEffect(ReadableColor color, int duration) {
		this();
		this.color = color;
		this.finalColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
		this.duration = duration;
	}

	/**
	 * C'tor
	 * @param sequence
	 */
	public StrobeEffect(ColorSequenceWrapper sequence) {
		this();
		this.sequence = sequence;
	}

	@Override
    public boolean isEffectActive() {
		if (sequence != null) {
			return !sequence.isFinished(tick);
		} else {
			return tick < duration;
		}
	}

	@Override
	protected void doTick() {
		tick ++;
		if (sequence == null) {
			ColorInterpolator.interpolate(color, finalColor, (float) tick / (float) duration, LinearInterpolator.instance, cachedColor);
		} else {
			sequence.getColor(tick, cachedColor);
		}
	}

	@Override
	protected void render() {
		if (!isStarted()) {
			return;
		}
		if ((tick & 4) == 0) {
			glRender(new GLRenderable() {
				@Override
				public void render() {
					glEnable(GL_BLEND);
					glDisable(GL_TEXTURE_2D);
					glBlendFunc(GL_ONE, GL_ONE);
				}
			});
			Screen s = getScreen();
			float preMultAlpha = cachedColor.getAlpha() / 255.0f;
			glColor4ub((byte) (cachedColor.getRed() * preMultAlpha), (byte) (cachedColor.getGreenByte() * preMultAlpha), (byte) (cachedColor.getBlueByte() * preMultAlpha), cachedColor.getAlphaByte());
			glVertex2f(0, 0);
			glVertex2f(s.getWidth(), 0);
			glVertex2f(s.getWidth(), s.getHeight());
			glVertex2f(0, s.getHeight());
			glRender(GL_TRIANGLE_FAN, INDICES);
		}
	}

	@Override
	public int getDefaultLayer() {
	    return Integer.MAX_VALUE; // On top of EVERYTHING!
	}

	@Override
	protected void doRemove() {
		instance = null;
	}

	@Override
	public boolean isBackgroundEffect() {
		return true;
	}
}
