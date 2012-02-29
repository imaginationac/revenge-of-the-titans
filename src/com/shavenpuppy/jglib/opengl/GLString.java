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

import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.sprites.SimpleRenderable;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * A GLString is a renderable string of characters
 *
 * @author: cas
 */
public class GLString implements SimpleRenderable {

	private GLFont font;

	private char[] text;
	private int[] x, ox;
	private int[] y, oy;
	private GLGlyphBuffer buffer;

	private int xpos, ypos;
	private int length;

	private boolean changed;

	// Used by getSize() and getGlyphIndex():
	private static final Rectangle temp = new Rectangle();

	// The entire size of the string, after a layout
	private final Rectangle size = new Rectangle();
	private boolean resized = false;

	private boolean coloured;
	private ReadableColor topColour, bottomColour;
	private int alpha = 255;

	/**
	 * GLString constructor comment.
	 */
	public GLString(int length) {
		super();

		text = new char[length];
		x = new int[length];
		y = new int[length];
		ox = new int[length];
		oy = new int[length];
		buffer = new GLGlyphBuffer(length);
		this.length = 0;
	}

	/**
	 * @param coloured the coloured to set
	 */
	public void setColoured(boolean coloured) {
		this.coloured = coloured;
	}

	/**
	 * @return the coloured
	 */
	public boolean isColoured() {
		return coloured;
	}

	public GLString(String text, GLFont font) {
		this(text == null ? 0 : text.length());
		setFont(font);
		setText(text);
	}

	/**
	 * Returns the capacity
	 */
	public int capacity() {
		return text.length;
	}
	/**
	 * Returns the index of the glyph at x (relative to origin 0, 0 of this glstring).
	 * Don't forget this isn't necesarily the character position in the string! Some characters
	 * get turned into more than one glyph which screws everything up. Nor will it work if you
	 * manually start offsetting or positioning glyphs.
	 * Returns -1 if there's no hit.
	 */
	public int getCharIndexAt(float xx, float yy) {
		// Layout and resize if necessary
		layout();

		// Recalc size
		getBounds(size);

		for (int i = 0; i < buffer.length; i++) {
			if (buffer.glyph[i] != null) {
				temp.setBounds((x[i] + xpos), size.getY(), (buffer.glyph[i].getWidth()), size.getHeight());
				if (temp.contains((int)xx, (int)yy)) {
					return i;
				}
			}
		}
		// Look to see if we're off the end
		if (yy >= size.getY() && yy <= size.getY() + size.getHeight() && xx >= size.getX() + size.getWidth()) {
			return buffer.length;
		} else if (yy >= size.getY() && yy <= size.getY() + size.getHeight() && xx < size.getX()) {
			return 0;
		} else {
			return -1;
		}
	}
	/**
	 * Returns the glyph at the specified location in the string
	 */
	public GLGlyph getGlyph(int index) {
		return buffer.glyph[index];
	}
	/**
	 * Returns the index of the glyph at x,y (relative to origin 0, 0 of this glstring).
	 * Don't forget this isn't necessarily the character position in the string! Some characters
	 * get turned into more than one glyph which screws everything up.
	 * Returns -1 if there's no hit.
	 */
	public int getGlyphIndexAt(float xx, float yy) {
		// Layout and resize if necessary
		layout();

		for (int i = 0; i < buffer.glyph.length; i++) {
			if (buffer.glyph[i] != null) {
				temp.setBounds(
					(x[i] + ox[i] + xpos),
					(y[i] + oy[i] + ypos),
					(x[i] + ox[i] + xpos + buffer.glyph[i].getWidth()),
					(y[i] + oy[i] + ypos + buffer.glyph[i].getHeight()));
				if (temp.contains((int)xx, (int)yy)) {
					return i;
				}
			}
		}
		return -1;
	}
	/**
	 * Returns the size of this String and stashes it in the incoming WritableRectangle.
	 * @param ret The destination rectangle
	 */
	public Rectangle getBounds(Rectangle ret) {
		// Layout if necessary
		layout();

		if (ret == null) {
			ret = new Rectangle();
		}

		// If no change in size since last time then just return the previously calculated size
		if (!resized) {
			ret.setBounds(size);
			return ret;
		}

		boolean doneOne = false;
		for (int i = 0; i < length; i++) {
			GLGlyph g = buffer.glyph[i];
			if (g != null) {
				if (!doneOne) {
					ret.setBounds
						(
							(x[i] + ox[i] + xpos),
							(y[i] + oy[i] + ypos),
							buffer.glyph[i].getWidth(),
							buffer.glyph[i].getHeight()
						);
					doneOne = true;
				} else {
					temp.setBounds
						(
							(x[i] + ox[i] + xpos),
							(y[i] + oy[i] + ypos),
							buffer.glyph[i].getWidth(),
							buffer.glyph[i].getHeight()
						);
					ret.union(temp, ret);
				}
			}
		}

		// Remember the size
		size.setBounds(ret);

		// No longer need to recalculate size
		resized = false;

		return ret;
	}

	/**
	 * Returns the contents as a string.
	 */
	public String getText() {
		return new String(text, 0, length);
	}

	/**
	 * Returns the contents as a string.
	 */
	public String getText(int start, int count) {
		return new String(text, start, count);
	}
	/**
	 * Does this string need laying out?
	 */
	protected boolean isChanged() {
		return changed;
	}
	/**
	 * Layout the text. Also computes the bounds of the string.
	 */
	public void layout() {
		if (font == null || !isChanged()) {
			return;
		}

		buffer = font.getGlyphBuffer(text, 0, length, buffer);
		for (int i = 0; i < buffer.length; i++) {
			if (buffer.glyph[i] != null) {
				x[i] = buffer.glyph[i].getXpos();
				y[i] = buffer.glyph[i].getYpos();
			}
		}

		// No longer need to layout
		setChanged(false);

		// Recalculate the size if asked later
		resized = true;

	}
	/**
	 * Returns the current length of the string
	 */
	public int length() {
		return length;
	}

	@Override
	public void render(SimpleRenderer renderer) {
		if (font == null) {
			throw new OpenGLException("Null font in "+this);
		}

		layout();

		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				// Bind the font's texture first
				font.getTexture().render();
				glEnable(GL_TEXTURE_2D);
				glEnable(GL_BLEND);
				glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
				glEnableClientState(GL_COLOR_ARRAY);
				glEnableClientState(GL_TEXTURE_COORD_ARRAY);
			}
		});

		// Render all the glyphs
		for (int i = 0; i < buffer.length; i++) {
			GLGlyph g = buffer.glyph[i];
			if (g != null) {
				g.setLocation(x[i] + ox[i] + xpos, y[i] + oy[i] + ypos);
				if (coloured) {
					g.render(topColour, bottomColour, alpha, renderer);
				} else {
					g.render(renderer);
				}
			}
		}
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	/**
	 * Mark this string as needing layout or not.
	 */
	protected void setChanged(boolean changed) {
		this.changed = changed;
	}
	/**
	 * Replaces the font
	 */
	public void setFont(GLFont font) {
		if (this.font == font) {
			return;
		}
		if (font == null) {
			throw new NullPointerException("Can't set font to null");
		}
		setChanged(true);
		this.font = font;
		resized = true;
	}

	/**
	 * @return Returns the font.
	 */
	public GLFont getFont() {
		return font;
	}

	/**
	 * @return Returns the final coordinates of the specified glyph
	 */
	public Rectangle getGlyphBounds(int index, Rectangle dest) {
		if (isChanged()) {
			layout();
		}
		if (dest == null) {
			dest = new Rectangle();
		}
		if (index < 0) {
			dest.setBounds(xpos, ypos, 0, font.getAscent() + font.getDescent());
		} else {
			GLGlyph g = buffer.glyph[index];
			if (g != null) {
				dest.setBounds(x[index] + ox[index] + xpos, y[index] + oy[index] + ypos, g.getAdvance(), font.getAscent() + font.getDescent());
			}
		}
		return dest;
	}

	/**
	 * Sets a glyph's location
	 */
	public void setGlyphLocation(int index, int xx, int yy) {
		x[index] = xx;
		y[index] = yy;
		resized = true;
	}
	/**
	 * Sets a glyph's location relative to its original location at 0,0
	 */
	public void setGlyphOffset(int index, int xx, int yy) {
		ox[index] = xx;
		oy[index] = yy;
		resized = true;
	}
	/**
	 * Sets the length of the string; must be less than the capacity or it gets capped.
	 */
	public void setLength(int length) {
		this.length = Math.min(capacity(), length);
		setChanged(true);
		resized = true;
	}
	/**
	 * Sets the location of the whole string.
	 */
	public void setLocation(int xp, int yp) {
		if (this.xpos != xp || this.ypos != yp) {
			this.xpos = xp;
			this.ypos = yp;
			resized = true;
		}
	}

	public int getX() {
		return xpos;
	}

	public int getY() {
		return ypos;
	}

	/**
	 * @return the location of the string
	 */
	public Point getLocation(Point ret) {
		if (ret == null) {
			ret = new Point();
		}
		ret.setLocation(xpos, ypos);
		return ret;
	}

	/**
	 * Set the text.
	 */
	public void setText(String s) {
		if (s == null) {
			s = "";
		}
		length = Math.min(text.length, s.length());
		s.getChars(0, length, text, 0);
		setChanged(true);
		resized = true;
	}
	/**
	 * Set a portion of the text.
	 */
	public void setText(String s, int start, int end, int destpos) {
		if (s == null) {
			s = "";
		}
		length = Math.max(destpos + (end - start), length);
		s.getChars(start, end, text, destpos);
		setChanged(true);
		resized = true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GLString["+(new String(text))+","+x+", "+y+"]";
	}

	public void setColour(ReadableColor c) {
		setColoured(true);
		bottomColour = c;
		topColour = c;
	}

	public void setBottomColour(ReadableColor c) {
		setColoured(true);
		bottomColour = c;
	}

	public void setTopColour(ReadableColor c) {
		setColoured(true);
		topColour = c;
	}

	public ReadableColor getTopColour() {
		return topColour;
	}

	public ReadableColor getBottomColour() {
		return bottomColour;
	}

	public int getAlpha() {
		return alpha;
	}

}
