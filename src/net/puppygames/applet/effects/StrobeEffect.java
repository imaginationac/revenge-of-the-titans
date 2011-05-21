/*
 * Copyright (c) 2004 Covalent Software Ltd
 * All rights reserved.
 */
package net.puppygames.applet.effects;

import net.puppygames.applet.Game;
import net.puppygames.applet.TickableObject;

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

	/** Tickable object */
	private TickableObject tickableObject;


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
	public boolean isActive() {
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
	protected void doRender() {
	}

	@Override
	protected void doSpawn() {
		tickableObject = new TickableObject() {
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
							glBlendFunc(GL_SRC_ALPHA, GL_ONE);
						}
					});
					glColor4ub(cachedColor.getRedByte(), cachedColor.getGreenByte(), cachedColor.getBlueByte(), cachedColor.getAlphaByte());
					glBegin(GL_QUADS);
					glVertex2f(0, 0);
					glVertex2f(Game.getWidth(), 0);
					glVertex2f(Game.getWidth(), Game.getHeight());
					glVertex2f(0, Game.getHeight());
					glEnd();
				}
			}
		};
		tickableObject.spawn(getScreen());
		tickableObject.setLayer(Integer.MAX_VALUE);
	}

	@Override
	protected void doRemove() {
		if (tickableObject != null) {
			tickableObject.remove();
			tickableObject = null;
		}
		instance = null;
	}

	@Override
	public boolean isBackgroundEffect() {
		return false;
	}
}
