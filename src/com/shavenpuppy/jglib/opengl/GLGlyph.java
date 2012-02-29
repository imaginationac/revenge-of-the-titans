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
package com.shavenpuppy.jglib.opengl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.Glyph;
import com.shavenpuppy.jglib.sprites.SimpleRenderable;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * A GLGlyph is a GLSprite which can be used to draw character glyphs for a
 * GLFont. Creation date: (28/12/2001 18:10:49)
 *
 * @author: cas
 */
public class GLGlyph implements SimpleRenderable {

	/** Handle to the texture */
	protected transient GLBaseTexture texture;

	/** Handle to the glyph */
	protected transient Glyph glyph;

	/** Current position */
	protected transient int xpos, ypos;

	/** Scale */
	protected transient float scale;

	/** Handy rectangle */
	private static final Rectangle bounds = new Rectangle();

	public GLGlyph(GLBaseTexture texture, Glyph glyph, float scale) {
		this.texture = texture;
		this.glyph = glyph;
		this.scale = scale;
	}

	/**
	 * Copy constructor
	 */
	public GLGlyph(GLGlyph src) {
		this.texture = src.texture;
		this.glyph = src.glyph;
		this.scale = src.scale;
	}

	/**
	 * Copy from an existing glyph
	 */
	public void from(GLGlyph src) {
		init(src.texture, src.glyph);
	}

	public void init(GLBaseTexture texture, Glyph g) {
		this.texture = texture;
		glyph = g;
	}

	public java.lang.String getName() {
		return null;
	}

	public int getXpos() {
		return xpos;
	}

	public int getYpos() {
		return ypos;
	}

	@Override
	public void render(SimpleRenderer renderer) {
		render(null, null, 255, renderer);
	}

	public void render(ReadableColor topCol, ReadableColor bottomCol, int alpha, SimpleRenderer renderer) {

		glyph.getBounds(bounds);

		//		float x0 = (bounds.getX() + 0.5f) / texture.getWidth();
		//		float y1 = (bounds.getY() + 0.5f) / texture.getHeight();
		//		float x1 = (bounds.getX() - 0.5f + bounds.getWidth()) / texture.getWidth();
		//		float y0 = (bounds.getY() - 0.5f + bounds.getHeight()) / texture.getHeight();
		float x0, y0, x1, y1;
		int offset = texture.minMode == GL11.GL_NEAREST ? 0 : 1;
		if (offset == 0) {
			x0 = (bounds.getX()) / (float)texture.getWidth();
			y1 = (bounds.getY()) / (float)texture.getHeight();
			x1 = (bounds.getX() + bounds.getWidth()) / (float)texture.getWidth();
			y0 = (bounds.getY() + bounds.getHeight()) / (float)texture.getHeight();
		} else {
			x0 = (bounds.getX() + 0.5f) / texture.getWidth();
			y1 = (bounds.getY() + 0.5f) / texture.getHeight();
			x1 = (bounds.getX() + bounds.getWidth() + 0.5f) / texture.getWidth();
			y0 = (bounds.getY() + bounds.getHeight() + 0.5f) / texture.getHeight();
		}

		final boolean coloured = topCol != null && bottomCol != null;

		renderer.glTexCoord2f(x0, y0);
		if (coloured) {
			ColorUtil.setGLColorPre(bottomCol, alpha, renderer);
		}
		short idx = renderer.glVertex2f(xpos, ypos);

		renderer.glTexCoord2f(x1, y0);
		if (coloured) {
			ColorUtil.setGLColorPre(bottomCol, alpha, renderer);
		}
		renderer.glVertex2f((int)((bounds.getWidth() + offset) * scale) + xpos, ypos);

		renderer.glTexCoord2f(x1, y1);
		if (coloured) {
			ColorUtil.setGLColorPre(topCol, alpha, renderer);
		}
		renderer.glVertex2f((int)((bounds.getWidth() + offset) * scale) + xpos, (int)((bounds.getHeight() + offset) * scale) + ypos);

		renderer.glTexCoord2f(x0, y1);
		if (coloured) {
			ColorUtil.setGLColorPre(topCol, alpha, renderer);
		}
		renderer.glVertex2f(xpos, (int)((bounds.getHeight() + offset) * scale) + ypos);

		renderer.glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
	}

	public void setLocation(int xp, int yp) {
		this.xpos = xp;
		this.ypos = yp;
	}

	/**
	 * Convenience accessor
	 */
	public int getWidth() {
		return (int) (glyph.getWidth() * scale);
	}

	/**
	 * Convenience accessor
	 */
	public int getHeight() {
		return (int) (glyph.getHeight() * scale);
	}

	/**
	 * Convenience accessor
	 */
	public int getBearingX() {
		return (int) (glyph.getBearingX() * scale);
	}

	/**
	 * Convenience accessor
	 */
	public int getBearingY() {
		return (int) (glyph.getBearingY() * scale);
	}

	/**
	 * Convenience accessor
	 */
	public int getAdvance() {
		return (int) (glyph.getAdvance() * scale);
	}

	/**
	 * Delegate method. Determines kerning.
	 *
	 * @param g
	 * @return int
	 */
	public int getKerningAfter(GLGlyph g) {
		if (g == null) {
			return 0;
		}
		return (int) (glyph.getKerningAfter(g.glyph) * scale);
	}

	/**
	 * @param dest
	 */
	public void getBearing(Point dest) {
		glyph.getBearing(dest);
		dest.setLocation((int) (dest.getX() * scale), (int) (dest.getY() * scale));
	}

	/**
	 * @param dest
	 */
	public void getBounds(Rectangle dest) {
		glyph.getBounds(dest);
		dest.setBounds
		(
				(int) (dest.getX() * scale),
				(int) (dest.getY() * scale),
				(int) (dest.getWidth() * scale),
				(int) (dest.getHeight() * scale)
		);
	}

	@Override
	public String toString() {
		return "GLGlyph["+getXpos()+", "+getYpos()+": "+glyph.toString()+"]";
	}
}