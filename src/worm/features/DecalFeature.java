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
package worm.features;

import org.lwjgl.util.Point;
import org.lwjgl.util.ReadablePoint;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.sprites.Animation;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.SpriteImage;

/**
 * Decals are drawn on top of tiles.
 * @author Cas
 */
public class DecalFeature extends Feature {

	private static final long serialVersionUID = 1L;

	private int layer, subLayer;
	private Point offset;
	private int ySortOffset;

	private float scale = 1.0f;
	private String animation;
	private String image;
	private MappedColor colored;
	private boolean relativeRotation;
	private boolean attenuated = true;

	private transient Animation animationResource;
	private transient SpriteImage imageResource;

	/**
	 * C'tor
	 */
	public DecalFeature() {
		setAutoCreated();
	}

	/**
	 * C'tor
	 * @param name
	 */
	public DecalFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return Returns the offset.
	 */
	public ReadablePoint getOffset() {
		return offset;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doCreate()
	 */
	@Override
	protected void doCreate() {
		super.doCreate();

		assert animationResource != null || imageResource != null : "Decal has no image or animation: "+animation+"/"+image;
	}

	/**
	 * @return Returns the animationResource.
	 */
	public Appearance getAppearance() {
		return animationResource == null ? (Appearance) imageResource : animationResource;
	}

	/**
	 * @return Returns the layer.
	 */
	public int getLayer() {
		return layer;
	}

	/**
	 * @return the sublayer
	 */
	public int getSubLayer() {
		return subLayer;
	}

	/**
	 * @return the ySortOffset
	 */
	public int getYSortOffset() {
		return ySortOffset;
	}

	/**
	 * @return Returns the colored.
	 */
	public MappedColor getColor() {
		return colored;
	}

	public boolean isRelativeRotation() {
		return relativeRotation;
	}

	/**
	 * @return the attenuated
	 */
	public boolean isAttenuated() {
		return attenuated;
	}

	/**
	 * @return the scale
	 */
	public float getScale() {
		return scale;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);
		if (offset == null) {
			offset = new Point();
		}
	}
}
