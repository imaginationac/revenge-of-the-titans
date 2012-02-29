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
package net.puppygames.applet.widgets;

import net.puppygames.applet.Game;
import net.puppygames.applet.Res;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.TextLayout;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLTextArea;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: MessageBox.java,v 1.3 2010/02/25 22:45:03 foo Exp $
 * <p>Displays a messagebox with some text in it centred on the screen
 *
 * @author $Author: foo $
 * @version $Revision: 1.3 $
 */
public class MessageBox {

	/** Bounds */
	private final Rectangle bounds = new Rectangle();

	/** Title */
	private final GLTextArea title = new GLTextArea();

	/** Message */
	private final GLTextArea message = new GLTextArea();

	/** Title background */
	private final Background.Instance titleBackground;

	/** Background */
	private final Background.Instance background;

	/** Margin */
	private static final int MARGIN = 8;

	/**
	 * C'tor
	 */
	public MessageBox() {
		title.setFont(Res.getBigFont());
		title.setHorizontalAlignment(TextLayout.CENTERED);
		title.setVerticalAlignment(GLTextArea.TOP);
		titleBackground = Res.getMessageTitleBackground().spawn();
		message.setFont(Res.getSmallFont());
		message.setVerticalAlignment(GLTextArea.TOP);
		background = Res.getMessageBodyBackground().spawn();
	}

	/**
	 * Set the size of the messagebox. It is automatically centred on the screen.
	 * @param width
	 * @param height
	 */
	public void setSize(int width, int height) {
		bounds.setSize(width, height);
		bounds.setLocation((Game.getWidth() - width) / 2, (Game.getHeight() - height) / 2);

		int titleHeight = title.getFont().getHeight() + MARGIN * 2;
		title.setBounds(MARGIN, height - MARGIN * 2 - titleHeight, width - MARGIN * 2, titleHeight);
		message.setBounds(MARGIN, title.getY() - MARGIN - titleHeight, width - MARGIN * 2, height - title.getY() - MARGIN * 2);
		titleBackground.setBounds(new Rectangle(0, 0, title.getWidth(), title.getHeight()));
		background.setBounds(new Rectangle(0, 0, width, height));
	}

	/**
	 * Set the title.
	 * @param titleText
	 */
	public void setTitle(String titleText) {
		title.setText(titleText);
	}

	/**
	 * Set the message.
	 * @param messageText
	 */
	public void setMessage(String messageText) {
		message.setText(messageText);
	}

	/**
	 * Render
	 */
	public void render(SimpleRenderer renderer) {
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glPushMatrix();
				glTranslatef(bounds.getX(), bounds.getY(), 0.0f);

				glEnable(GL_TEXTURE_2D);
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
			}
		});
		background.render(renderer);
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glPushMatrix();
				glTranslatef(title.getX(), title.getY() + title.getFont().getDescent(), 0.0f);
			}
		});
		titleBackground.render(renderer);
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glPopMatrix();
				glPushMatrix();
				glTranslatef(2, -2, 0.0f);
			}
		});
		title.setColour(new Color(0, 0, 0, 128));
		title.render(renderer);
		message.setColour(new Color(0, 0, 0, 128));
		message.render(renderer);
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glPopMatrix();
				glColor3f(1.0f, 1.0f, 1.0f);
			}
		});
		title.setColour(ReadableColor.WHITE);
		message.setColour(ReadableColor.WHITE);
		title.render(renderer);
		message.render(renderer);
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glPopMatrix();
			}
		});
	}
}
