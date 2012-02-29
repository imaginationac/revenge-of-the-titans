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
package com.shavenpuppy.jglib.sprites;

import org.lwjgl.opengl.OpenGLException;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.opengl.GLBaseTexture;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * An SpriteImage is just a quad inside a texture.
 * Because a SpriteImage no public constructor taking a name as an argument it cannot
 * be mapped and created directly in the resources XML file.
 */
public class SpriteImage extends Resource implements Appearance {

	private static final long serialVersionUID = 1L;

	/** The imagebank, if any */
	private transient ImageBank imageBank;

	/** The image bank name */
	private String imageBankName;

	/** The texture */
	private transient GLBaseTexture texture;

	/** The rendering style */
	private transient Style style;

	/** The name of the rendering style */
	private String styleName;

	/** The pixel coordinates of the image and its hotspot */
	private int x, y, w, h, hotspotx, hotspoty;

	/** Offset */
	private boolean offset = true;

	/** The quad's texture coordinates */
	private float tx0, tx1, ty0, ty1;

	/**
	 * C'tor
	 */
	protected SpriteImage() {
	}

	/**
	 * Constructor for SpriteImage.
	 */
	SpriteImage(String name, String imageBankName, String styleName) {
		this(name);
		this.imageBankName = imageBankName;
		this.styleName = styleName;
	}

	/**
	 * Constructor with known quad dimensions in pixels
	 */
	SpriteImage(String name, String imageBankName, String styleName, int x, int y, int w, int h, int hotspotx, int hotspoty, boolean offset) {
		this(name, imageBankName, styleName);

		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.hotspotx = hotspotx;
		this.hotspoty = hotspoty;
		this.offset = offset;

	}

	/**
	 * Constructor with known quad dimensions in pixels and existing GL texture and styles
	 */
	public SpriteImage(GLBaseTexture texture, Style style, int x, int y, int w, int h, int hotspotx, int hotspoty, boolean offset) {
		this.texture = texture;
		this.style = style;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.hotspotx = hotspotx;
		this.hotspoty = hotspoty;
		this.offset = offset;
	}


	/**
	 * Public constructor for simply making the image from a whole texture
	 */
	public SpriteImage(String texture, String styleName, int hotspotx, int hotspoty) {
		this(texture + "-image");
		this.texture = Resources.get(texture);
		this.style = (Style) Resources.get(styleName);
		this.hotspotx = hotspotx;
		this.hotspoty = hotspoty;
		this.w = this.texture.getWidth();
		this.h = this.texture.getHeight();
		tx1 = 1.0f;
		ty1 = 1.0f;
	}

	/**
	 * Public constructor for simply making a spriteimage without a texture
	 */
	public SpriteImage(String styleName, int width, int height, int hotspotx, int hotspoty) {
		this(styleName+"-"+width+"x"+height);
		this.styleName = styleName;
		this.hotspotx = hotspotx;
		this.hotspoty = hotspoty;
		this.w = width;
		this.h = height;
	}

	/**
	 * Ensure registration of the image with our little map, and get us an index number too
	 */
	private SpriteImage(String name) {
		super(name);
	}

	/**
	 * Load from an XML element
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {

		x = Integer.parseInt(element.getAttribute("x"));
		y = Integer.parseInt(element.getAttribute("y"));
		w = Integer.parseInt(element.getAttribute("w"));
		h = Integer.parseInt(element.getAttribute("h"));
		hotspotx = Integer.parseInt(element.getAttribute("hx"));
		hotspoty = Integer.parseInt(element.getAttribute("hy"));
		styleName = element.getAttribute("style");
		offset = XMLUtil.getBoolean(element, "offset", true);

	}

	/**
	 * Calculate the texture coordinates of the quad
	 */
	private void calculateTextureCoordinates() {
		double tw = texture.getWidth();
		double th = texture.getHeight();

		// Offset: the default for linear styled textures
		if (offset) {
			tx0 = (float) ((x + 0.5f) / tw);
			tx1 = (float) ((x + w - 0.5f) / tw);
			ty0 = (float) ((y + 0.5f) / th);
			ty1 = (float) ((y + h - 0.5f) / th);
		} else {
			// Not offset: for linear styled textures
			tx0 = (float) (x / tw);
			tx1 = (float) ((x + w) / tw);
			ty0 = (float) (y / th);
			ty1 = (float) ((y + h) / th);
		}

	}

	/* (non-Javadoc)
	 * @see ALResource#doCreate()
	 */
	@Override
	protected void doCreate() {
		if (texture == null) {
			if (imageBankName != null) {
				imageBank = (ImageBank) Resources.get(imageBankName);
				if (imageBank == null) {
					throw new OpenGLException("Imagebank " + imageBankName + " does not exist");
				}
				texture = imageBank.texture;
				if (texture == null) {
					throw new OpenGLException("Imagebank " + imageBankName +" has no texture");
				}
			}
		}
		if (style == null) {
			if (styleName == null || "".equals(styleName)) {
				styleName = imageBank.getDefaultStyleName();
			}
			style = (Style) Resources.get(styleName);
		}

		if (texture != null) {
			calculateTextureCoordinates();
		}
	}

	@Override
	public void archive() {
		imageBankName = null;
		styleName = null;
	}

	/* (non-Javadoc)
	 * @see ALResource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		imageBank = null;
		texture = null;
	}

	/**
	 * Accessor
	 */
	public final int getWidth() {
		return w;
	}

	/**
	 * Accessor
	 */
	public final int getHeight() {
		return h;
	}

	/**
	 * Gets the tx0.
	 * @return Returns a float
	 */
	public final float getTx0() {
		return tx0;
	}

	/**
	 * Gets the tx1.
	 * @return Returns a float
	 */
	public final float getTx1() {
		return tx1;
	}

	/**
	 * Gets the ty0.
	 * @return Returns a float
	 */
	public final float getTy0() {
		return ty0;
	}

	/**
	 * Gets the ty1.
	 * @return Returns a float
	 */
	public final float getTy1() {
		return ty1;
	}

	/**
	 * Gets the hotspotx.
	 * @return Returns a int
	 */
	public final int getHotspotX() {
		return hotspotx;
	}

	/**
	 * Gets the hotspoty.
	 * @return Returns a int
	 */
	public final int getHotspotY() {
		return hotspoty;
	}

	/**
	 * Gets the texture.
	 * @return Returns a GLTexture
	 */
	public final GLBaseTexture getTexture() {
		return texture;
	}

	/**
	 * Returns the style.
	 * @return Style
	 */
	public Style getStyle() {
		return style;
	}

	@Override
	public boolean toSprite(Sprite target) {
		target.setImage(this);
		return false;
	}

	/**
	 * @return the x
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return y;
	}
}
