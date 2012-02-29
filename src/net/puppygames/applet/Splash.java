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
package net.puppygames.applet;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.Color;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.IResource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.opengl.GLTexture;
import com.shavenpuppy.jglib.resources.Feature;

import static org.lwjgl.opengl.GL11.*;

import static org.lwjgl.util.glu.GLU.*;

/**
 * Splash screen
 */
public class Splash extends Feature implements Resources.CreatingCallback {

	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG = false;

	/*
	 * Feature data
	 */
	private String splashScreenImage;

	private Rectangle loaderBounds;

	private int count;

	private Color clearColor, barColor, loadingColor;

	private boolean solid;

	private float scale = 1.0f;

	/*
	 * Transient data
	 */

	private transient GLTexture splashScreenImageTexture;

	private transient int lastCount, oldBounds;

	/**
	 * C'tor
	 */
	public Splash(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * @see genesis.Feature#doCreate()
	 */
	@Override
	protected void doCreate() {
		super.doCreate();

		glMatrixMode(GL_PROJECTION);
		glPushMatrix();
		glLoadIdentity();
		gluOrtho2D(0.0f, Game.getWidth(), 0.0f, Game.getHeight());
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

	}

	/*
	 * (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@SuppressWarnings("unused")
	@Override
	protected void doDestroy() {
		if (Game.DEBUG && DEBUG) {
			System.out.println("Total resources created during splash screen: " + Resources.getNumCreated());
		}
		super.doDestroy();
	}

	/**
	 * Render
	 */
	public void render() {
		int created = Math.min(count, Resources.getNumCreated());
		if (created <= lastCount) {
			return;
		}
		lastCount = created;
		int newBounds = loaderBounds.getX() + (created * loaderBounds.getWidth()) / count;
		if (newBounds == oldBounds) {
			return;
		}
		oldBounds = newBounds;
		if (DEBUG) {
			System.out.println("Create total of " + lastCount);
		}
		glClearColor(clearColor.getRed() / 255.0f, clearColor.getGreen() / 255.0f, clearColor.getBlue() / 255.0f,
		        clearColor.getAlpha() / 255.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		glPushMatrix();
		glTranslatef((Game.getWidth() - (splashScreenImageTexture.getWidth() * scale)) / 2.0f,
		        (Game.getHeight() - (splashScreenImageTexture.getHeight() * scale)) / 2.0f, 0);
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
		splashScreenImageTexture.render();
		glBegin(GL_QUADS);
		{
			glTexCoord2f(0.0f, 1.0f);
			glVertex2f(0.0f, 0.0f);
			glTexCoord2f(1.0f, 1.0f);
			glVertex2f(splashScreenImageTexture.getWidth() * scale, 0.0f);
			glTexCoord2f(1.0f, 0.0f);
			glVertex2f(splashScreenImageTexture.getWidth() * scale, splashScreenImageTexture.getHeight() * scale);
			glTexCoord2f(0.0f, 0.0f);
			glVertex2f(0.0f, splashScreenImageTexture.getHeight() * scale);
		}
		glEnd();
		glPopMatrix();

		glDisable(GL_TEXTURE_2D);

		if (barColor != null) {
			glColor4f(barColor.getRed() / 255.0f, barColor.getGreen() / 255.0f, barColor.getBlue() / 255.0f,
			        barColor.getAlpha() / 255.0f);
		} else {
			glColor3f(1.0f, 1.0f, 1.0f);
		}

		if (solid) {
			glPolygonMode(GL_FRONT, GL_FILL);
		} else {
			glPolygonMode(GL_FRONT, GL_LINE);
		}

		int ox = (Game.getWidth() - Game.getScale()) / 2;
		int oy = (Game.getHeight() - Game.getScale()) / 2;
		glBegin(GL_QUADS);
		{
			glVertex2i(ox + loaderBounds.getX(), oy + loaderBounds.getY());
			glVertex2i(ox + loaderBounds.getX() + loaderBounds.getWidth(), oy + loaderBounds.getY());
			glVertex2i(ox + loaderBounds.getX() + loaderBounds.getWidth(), oy + loaderBounds.getY() + loaderBounds.getHeight());
			glVertex2i(ox + loaderBounds.getX(), oy + loaderBounds.getY() + loaderBounds.getHeight());
		}
		glEnd();

		glPolygonMode(GL_FRONT, GL_FILL);
		if (loadingColor != null) {
			glColor4f(loadingColor.getRed() / 255.0f, loadingColor.getGreen() / 255.0f, loadingColor.getBlue() / 255.0f,
			        loadingColor.getAlpha() / 1024.0f);
		} else {
			glColor3f(1.0f, 1.0f, 0.25f);
		}

		glRecti(ox + loaderBounds.getX() - 1, oy + loaderBounds.getY() - 1, ox + newBounds + 1, oy + loaderBounds.getY()
		        + loaderBounds.getHeight() + 1);
		if (loadingColor != null) {
			glColor4f(loadingColor.getRed() / 255.0f, loadingColor.getGreen() / 255.0f, loadingColor.getBlue() / 255.0f,
			        loadingColor.getAlpha() / 255.0f);
		} else {
			glColor3f(1.0f, 1.0f, 1.0f);
		}

		glRecti(ox + loaderBounds.getX(), oy + loaderBounds.getY(), ox + newBounds,
		        oy + loaderBounds.getY() + loaderBounds.getHeight());

		Display.update();
	}

	/*
	 * (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resources.CreatingCallback#onCreating(com.shavenpuppy.jglib.Resource)
	 */
	@Override
	public void onCreating(IResource resource) {
		if (DEBUG) {
			System.out.println("Creating " + resource);
		}
		if (!Display.isCreated() || Display.isCloseRequested()) {
			Game.exit();
		} else {
			render();
		}
	}

}
