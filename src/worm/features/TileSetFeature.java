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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import worm.Tile;

import com.shavenpuppy.jglib.IResource;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Tile Sets.
 * @author Cas
 */
public class TileSetFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/** All the tilesets */
	private static final ArrayList<TileSetFeature> TILESETS = new ArrayList<TileSetFeature>();

	/** Tile set description */
	private String description;

	/** All the Tiles in this tileset */
	private final ArrayList<Tile> tiles = new ArrayList<Tile>(256);

	/** Default tile */
	private String default_;

	private transient Tile default_Resource;

	/**
	 * C'tor
	 * @param name
	 */
	public TileSetFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doRegister()
	 */
	@Override
	protected void doRegister() {
		TILESETS.add(this);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doDeregister()
	 */
	@Override
	protected void doDeregister() {
		TILESETS.remove(this);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		// Now load all the tiles contained within (and any other resources)
		List<Element> children = XMLUtil.getChildren(element);
		for (Element child : children) {
			IResource childResource = loader.load(child);
			if (childResource instanceof Tile) {
				Tile t = (Tile) childResource;
				t.setTileSet(this);
				tiles.add(t);
			}
		}
	}

	/**
	 * @return Returns the default tile for this tileset, which is what we fill layer 0 of the map with
	 */
	public Tile getDefault() {
		return default_Resource;
	}

	/**
	 * @return Returns the tiles.
	 */
	public ArrayList<Tile> getTiles() {
		return tiles;
	}

	/**
	 * @return Returns the tilesets.
	 */
	public static ArrayList<TileSetFeature> getTileSets() {
		return TILESETS;
	}
}
