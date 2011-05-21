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
import net.puppygames.applet.Res;

import org.lwjgl.util.*;

import com.shavenpuppy.jglib.TextLayout;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLTextArea;
import com.shavenpuppy.jglib.resources.Background;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: ProgressEffect.java,v 1.2 2010/02/08 01:03:51 foo Exp $
 * Displays a message with a progress bar
 * <p>
 * @author $Author: foo $
 * @version $Revision: 1.2 $
 */
public class ProgressEffect extends Effect {

	/** Progress max */
	private static final int MAX = 30;

	/** Text area */
	private final GLTextArea textArea = new GLTextArea();

	/** Width & height */
	private int width, height;

	/** Progress */
	private int progress;

	/** Direction */
	private int direction;

	/** Background */
	private Background.Instance background;

	/** Border colour */
	private ReadableColor border;

	/** Bar colour */
	private ReadableColor bar;
	private Color fadeBar;

	/** Finished */
	private boolean finished;

	/**
	 * C'tor
	 */
	public ProgressEffect(String message, ReadableColor border, ReadableColor bar) {
		textArea.setFont(Res.getSmallFont());
		textArea.setVerticalAlignment(GLTextArea.BOTTOM);
		textArea.setHorizontalAlignment(TextLayout.CENTERED);
		textArea.setWidth((int)(Game.getWidth() * 0.66f));
		background = Res.getProgressBackground().spawn();
		setMessage(message);
		background.setColor(border);
		this.border = border;
		this.bar = bar;
		fadeBar = new Color(bar);
		fadeBar.setAlpha(0);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.Effect#doTick()
	 */
	@Override
	protected void doTick() {
		progress += direction;
		if (progress == 0) {
			direction = 1;
		} else if (progress == MAX - 1) {
			direction = -1;
		}
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.Effect#doRender(java.awt.Graphics2D)
	 */
	@Override
	protected void doRender() {

		glDisable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

		int x = textArea.getX();
		int y = textArea.getY();
		glEnable(GL_TEXTURE_2D);
		// Draw background
		glPushMatrix();
		glTranslatef(x, y - 12, 0);
		background.render();
		glPopMatrix();
		// Draw message
		ColorUtil.setGLColor(ReadableColor.WHITE);
		textArea.render();

		int xpos = (int) LinearInterpolator.instance.interpolate(x + 8, Game.getWidth() - x - 8, (float) progress / (float) MAX);
		int len = (Game.getWidth() - 2 * x) / MAX;

		glDisable(GL_TEXTURE_2D);
		glBegin(GL_QUADS);
		ColorUtil.setGLColor(direction == 1 ? fadeBar : bar);
		glVertex2i(xpos, y - 4);
		ColorUtil.setGLColor(direction == 1 ? bar : fadeBar);
		glVertex2i(xpos + len, y - 4);
		glVertex2i(xpos + len, y);
		ColorUtil.setGLColor(direction == 1 ? fadeBar : bar);
		glVertex2i(xpos, y);
		glEnd();

	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Thing#isAlive()
	 */
	@Override
	public boolean isActive() {
		return !finished;
	}

	/**
	 * @return Returns the finished.
	 */
	@Override
	public boolean isFinished() {
		return finished;
	}

	/**
	 * @param finished The finished to set.
	 */
	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	/**
	 * @param message The message to set.
	 */
	public void setMessage(String message) {
		textArea.setText(message);
		width = textArea.getWidth();
		height = textArea.getTextHeight() + 16;
		int x = (Game.getWidth() - width) / 2;
		int y = (Game.getHeight() - height) / 2;
		textArea.setLocation(x, y);
		background.setBounds(new Rectangle(0, 0, width, height));
	}

	/**
	 * @param border The border to set.
	 */
	public void setBorder(Color border) {
		this.border = border;
	}

}
