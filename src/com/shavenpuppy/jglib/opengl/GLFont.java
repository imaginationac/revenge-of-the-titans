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

import java.io.BufferedInputStream;
import java.net.URL;

import org.lwjgl.util.Point;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Font;
import com.shavenpuppy.jglib.Glyph;
import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.FontResource;
import com.shavenpuppy.jglib.util.XMLUtil;

import static org.lwjgl.opengl.GL11.*;

/**
 * A font suitable for rendering with GL.
 */
public class GLFont extends Resource implements GLRenderable {

	private static final long serialVersionUID = 1L;

	/** Handy point */
	private static final Point tempPoint = new Point();

	/*
	 * Resource data
	 */

	/** The URL to load the font from */
	protected String url;

	/** Whether this is an ascii font or not */
	protected boolean ascii;

	/** OR... use minMode/magMode instead of linear/fullscreen */
	private int minMode, magMode;

	/** Font scale */
	private float scale = 1.0f;

	/*
	 * Transient data
	 */

	/** The font resource */
	protected transient FontResource fontResource;

	/** The font */
	protected transient Font font;

	/** The texture holding the font's image */
	protected transient GLTexture texture;

	/** Whether to discard the font image afterwards */
	protected transient boolean discardFontImage;

	/** Whether to discard the fontResource afterwards */
	protected transient boolean discardFontResource;

	/**
	 * Resource constructor
	 * @param name
	 */
	public GLFont(String name) {
		super(name);
	}

	/**
	 * GLFont constructor comment.
	 * @param name java.lang.String
	 */
	public GLFont(String name, String url, boolean ascii) {
		super(name);

		this.url = url;
		this.ascii = ascii;

		discardFontImage = true;
		discardFontResource = true;
	}

	/**
	 * GLFont constructor comment.
	 * @param name java.lang.String
	 */
	public GLFont(String name, Font font, boolean ascii)
	{
		super(name);

		if (font == null) {
			throw new NullPointerException("Null 'Font' parameter for GLFont "+name);
		}

		this.font = font;
		this.ascii = ascii;
		this.minMode = GL_LINEAR_MIPMAP_LINEAR;
		this.magMode = GL_LINEAR;

		discardFontResource = true;
	}

	/**
	 * GLFont constructor comment.
	 * @param name java.lang.String
	 */
	public GLFont(String name, FontResource fontResource, boolean ascii) {
		super(name);

		this.fontResource = fontResource;
		this.ascii = ascii;
	}

	@Override
	protected void doCreate() {

		try {
			// First get the image if necessary
			if (url != null) {
				if (url.startsWith("classpath:")) {
					// Load directly from a serialised Font in the classpath
					BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(url.substring(10)));
					font = new Font();
					font.readExternal(bis);
					bis.close();
				} else if (url.startsWith("resource:")) {
					// Load directly from Resources
					fontResource = (FontResource) Resources.get(url.substring(9));
				} else {
					// Load from a URL
					BufferedInputStream bis = new BufferedInputStream(new URL(url).openStream());
					font = new Font();
					font.readExternal(bis);
					bis.close();
				}
			}

			if (fontResource != null) {
				font = fontResource.getFont();
			}

			if (font == null) {
				throw new Exception("No font specified for GLFont "+this);
			}

			// Create a texture for the font
			texture = new GLTexture(getName(), font.getImage(), GL_TEXTURE_2D, GL_LUMINANCE_ALPHA, minMode, magMode, false);
			texture.create();

			if (discardFontImage) {
				font.getImage().dispose();
			}
			if (discardFontResource) {
				fontResource = null;
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to create font "+this+" font:"+font+" minMode:"+GLUtil.recode(minMode)+" magMode:"+GLUtil.recode(magMode));
		}

	}


	@Override
	protected void doDestroy() {
		texture.destroy();
		texture = null;
	}

	/**
	 * Convenience method
	 * @return the font's ascent
	 */
	public int getAscent() {
		return (int) (font.getAscent() * scale);
	}

	/**
	 * Convenience method
	 * @return the font's descent
	 */
	public int getDescent() {
		return (int) (font.getDescent() * scale);
	}

	/**
	 * Access to the underlying font
	 * @return the font used
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * @return the scale
	 */
	public float getScale() {
		return scale;
	}

	/**
	 * Convenience method
	 * @return the font's height (already scaled)
	 */
	public int getHeight() {
		return (int) ((font.getAscent() + font.getDescent()) * scale);
	}

	/**
	 * Convenience method
	 * @return the font's leading (already scaled)
	 */
	public int getLeading() {
		return (int) (font.getLeading() * scale);
	}

	/**
	 * @return the font's underlying texture
	 */
	public GLBaseTexture getTexture() {
		assert isCreated() : this + " is not created yet";
		return texture;
	}

	/**
	 * Binds the font's texture
	 */
	@Override
	public void render() {
		assert isCreated() : this + " is not created yet";
		texture.render();
	}

	/**
	 * Initialize a GLGlyphBuffer from an array of characters. The characters are laid
	 * out nicely in the correct positions.
	 * @param text The array of characters
	 * @param start The position in the text array at which to start
	 * @param end The position in the text array before which to finish
	 * @param buffer A pre-existing GLGlyphBuffer, or null, if a new buffer is to be created
	 * @return a GLGlyphBuffer with glyphs in for the specified text
	 */
	public GLGlyphBuffer getGlyphBuffer(char[] text, int start, int end, GLGlyphBuffer buffer) {


		// Create a glyph buffer big enough
		if (buffer == null || buffer.capacity() < end - start) {
			buffer = new GLGlyphBuffer(end - start);
		}

		int count = 0;
		Glyph last = null;
		int penX = 0;

		for (int i = start; i < end; i ++) {
			Glyph next = font.map(text[i]);

			if (buffer.glyph[count] == null) {
				buffer.glyph[count] = new GLGlyph(texture, next, scale);
			} else {
				buffer.glyph[count].init(texture, next);
			}

			next.getBearing(tempPoint);
			// Scale bearing
			tempPoint.setLocation((int)(tempPoint.getX() * scale), (int)(tempPoint.getY() * scale));

			// Scale kerning
			int kerning = (int) (next.getKerningAfter(last) * scale);
			buffer.glyph[count++].setLocation(tempPoint.getX() + penX - kerning, tempPoint.getY());
			penX += (int) (next.getAdvance() * scale) + kerning;
			last = next;
		}

		buffer.length = count;

		return buffer;
	}

	/**
	 * Map a single char to a new GLGlyph
	 * @param c The character
	 * @return a GLGlyph
	 */
	public GLGlyph map(char c) {
		return new GLGlyph(texture, font.map(c), scale);
	}

	@Override
	public void archive() {
		url = null;
	}

	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		super.load(element, loader);

		url = XMLUtil.getString(element, "url");
		ascii = XMLUtil.getBoolean(element, "ascii", true);
		scale = XMLUtil.getFloat(element, "scale", 1.0f);
		String minModeS = XMLUtil.getString(element, "minmode", null);
		String magModeS = XMLUtil.getString(element, "magmode", null);
		if (minModeS != null) {
			minMode = GLUtil.decode(minModeS);
		} else {
			minMode = GL_LINEAR_MIPMAP_LINEAR;
		}
		if (magModeS != null) {
			magMode = GLUtil.decode(magModeS);
		} else {
			magMode = GL_LINEAR;
		}
	}

}