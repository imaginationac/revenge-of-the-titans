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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.opengl.GLBaseTexture;


/**
 * An ImageBank carves up a GLTexture into a number of SpriteImages.
 */
public class ImageBank extends Resource {

	private static final long serialVersionUID = 1L;

	/** The texture name */
	private String textureName;

	/** Default style */
	private String defaultStyleName;

	/** The texture */
	transient GLBaseTexture texture;

	private int tilesAcross;
	private int tilesDown;
	private int tilewidth;
	private int tileheight;
	private int hotspotx;
	private int hotspoty;
	private boolean grid;

	/** The images */
	private SpriteImage[] image;

	/**
	 * Constructor for ImageBank.
	 */
	public ImageBank(String name) {
		super(name);
	}



	/**
	 * @param texture
	 * @param tilesAcross
	 * @param tilesDown
	 * @param tilewidth
	 * @param tileheight
	 * @param hotspotx
	 * @param hotspoty
	 * @param grid
	 */
	public ImageBank(String name, GLBaseTexture texture, String styleName, int tilesAcross, int tilesDown, int tilewidth, int tileheight, int hotspotx, int hotspoty, boolean grid) {
		super(name);
		this.texture = texture;
		this.defaultStyleName = styleName;
		this.tilesAcross = tilesAcross;
		this.tilesDown = tilesDown;
		this.tilewidth = tilewidth;
		this.tileheight = tileheight;
		this.hotspotx = hotspotx;
		this.hotspoty = hotspoty;
		this.grid = grid;
		buildSpriteImages();
	}

	public int numImages() {
		return image.length;
	}
	public SpriteImage getImage(int index) {
		return image[index];
	}

	/* (non-Javadoc)
	 * @see GLXMLResource#load(Element, Loader)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {

		textureName = element.getAttribute("texture");
		defaultStyleName = element.getAttribute("defaultstyle");

		// Either the texture is carved up into a number of equally sized tiles, all with the same hotspot,
		// or each image is specified individually. The automatically generated sprite names either end with
		// a single .n index, or .x.y, depending on whether grid="true" is specified.

		try {
			tilesAcross = Integer.parseInt(element.getAttribute("tilesacross"));
			tilesDown = Integer.parseInt(element.getAttribute("tilesdown"));
			tilewidth = Integer.parseInt(element.getAttribute("tilewidth"));
			tileheight = Integer.parseInt(element.getAttribute("tileheight"));
			hotspotx = Integer.parseInt(element.getAttribute("hotspotx"));
			hotspoty = Integer.parseInt(element.getAttribute("hotspoty"));
			grid = Boolean.valueOf(element.getAttribute("grid")).booleanValue();

			buildSpriteImages();

		} catch (NumberFormatException e) {

			// Look for <spriteimage> tags instead
			NodeList imageList = element.getElementsByTagName("spriteimage");
			image = new SpriteImage[imageList.getLength()];
			for (int i = 0; i < image.length; i ++) {
		 		Element imageElement = (Element) imageList.item(i);
				image[i] = new SpriteImage(imageElement.getAttribute("name"), getName(), defaultStyleName);
				image[i].load(imageElement, loader);
				Resources.put(image[i]);
			}
		}

	}

	private void buildSpriteImages() {
		image = new SpriteImage[tilesAcross * tilesDown];
		int x = 0, y = 0;

		for (int i = 0; i < image.length; i++) {
			String spriteName;
			if (grid) {
				spriteName = getName()+"."+x+"."+y;
			} else {
				spriteName = getName()+"."+i;
			}
			image[i] = new SpriteImage(spriteName, getName(), defaultStyleName, tilewidth * x, tileheight * y, tilewidth, tileheight, hotspotx, hotspoty, true);
			Resources.put(image[i]);
			x ++;
			if (x >= tilesAcross) {
				x = 0;
				y ++;
			}
		}
	}

	/* (non-Javadoc)
	 * @see ALResource#doCreate()
	 */
	@Override
	protected void doCreate() {
		if (texture == null) {
			texture = (GLBaseTexture) Resources.get(textureName);
		}
		// Create all the images
		for (int i = 0; i < image.length; i ++) {
			image[i].create();
		}
	}

	@Override
	public void archive() {
		textureName = null;
		defaultStyleName = null;
	}

	/* (non-Javadoc)
	 * @see ALResource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		texture = null;
		// Destroy all the images
		for (int i = 0; i < image.length; i ++) {
			image[i].destroy();
		}
	}

	/**
	 * Returns the defaultStyleName.
	 * @return String
	 */
	public String getDefaultStyleName() {
		return defaultStyleName;
	}

}
