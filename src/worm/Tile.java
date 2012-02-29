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
package worm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Point;
import org.w3c.dom.Element;

import worm.features.DecalFeature;
import worm.features.LayersFeature;
import worm.features.TileSetFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.sprites.Animation;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.sprites.SpriteImage;
import com.shavenpuppy.jglib.util.Util;
import com.shavenpuppy.jglib.util.XMLUtil;


/**
 * Describes an object that will be rendered on the map
 * @author Cas
 */
public abstract class Tile extends Feature {

	private static final long serialVersionUID = 1L;

	private static final int MAX_TILES = Short.MAX_VALUE;
	private static final String DEFAULT_FLOOR_COLOR = "floor";
	private static final String DEFAULT_ITEM_COLOR = "item";

	/** All tiles */
	private static final Tile[] TILES = new Tile[MAX_TILES];

	/** Maximum tile index */
	private static int maxTileIndex = -1;

	private static final Tile[] TEMP = new Tile[GameMap.LAYERS];

	/*
	 * Feature data
	 */

	/** Layer */
	private int layer;

	/** Sprite layer, if different */
	private int spriteLayer;

	/** Index */
	private short i;

	/** Tile group name */
	private String group;

	/** An emitter */
	private String emitter;

	/** Emitter position */
	private Point emitterPos;

	/** Image */
	private String image;

	/** The tile's appearance, if animated */
	private Animation animation;

	/** Randomizer: automatically replace this tile with one from the following list when drawn */
	private ResourceArray random;

	/** Tile set */
	private String set;

	/** Next tile in sequence */
	private String nextTile;

	/** Tooltip */
	private String tooltip;

	/** @deprecated Decals */
	@Deprecated
	private List<DecalFeature> decals;

	/** Layers - use instead of decals */
	private String layers;

	/** Colorize ? */
	private MappedColor colored;

	/** Spec: whether to register this tile or not. If spec==true, then this is a "specification" tile, just used as a template for other tiles */
	private boolean spec;

	/** Whether to apply attenuation */
	private boolean attenuated;

	/** Cost to traverse */
	private float cost;

	/*
	 * Transient data
	 */
	private transient SpriteImage imageResource;
	private transient Tile nextTileResource;
	private transient EmitterFeature emitterFeature;
	private transient TileSetFeature tileSetFeature;
	private transient LayersFeature layersFeature;

	/**
	 * Get the tile of the specified index. Out-of-bounds tiles always return the EmptyTile
	 * @param idx The tile index
	 * @return a Tile, or the EmptyTile
	 */
	public static Tile getTile(int idx) {
		if (idx < 0 || idx >= TILES.length) {
			return Tile.getEmptyTile();
		} else {
			Tile ret = TILES[idx];
			if (ret == null) {
				return Tile.getEmptyTile();
			} else {
				return ret;
			}
		}
	}

	/**
	 * Get the largest tile index
	 * @return
	 */
	public static int getMaxTileIndex() {
		return maxTileIndex;
	}

	/**
	 * @return Returns the tile array
	 */
	public static Tile[] getAllTiles() {
		return TILES;
	}

	/**
	 * C'tor
	 */
	public Tile() {
		setAutoCreated();
	}

	/**
	 * C'tor
	 */
	public Tile(String name) {
		super(name);
		setAutoCreated();
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		// Load decals
		List<Element> decalChildren = XMLUtil.getChildren(element, "decal");
		if (decalChildren.size() > 0) {
			decals = new ArrayList<DecalFeature>(decalChildren.size());
			for (Element decalChild : decalChildren) {
				DecalFeature decal = (DecalFeature) loader.load(decalChild);
				decals.add(decal);
			}
		}

		// Default value for "colored" depends on layer
		if (colored == null) {
			if (layer == 0) {
				colored = new MappedColor();
				colored.fromString(DEFAULT_FLOOR_COLOR);
			} else if (layer == 1) {
				colored = new MappedColor();
				colored.fromString(DEFAULT_ITEM_COLOR);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doCreate()
	 */
	@Override
	protected void doCreate() {
		super.doCreate();

		// Create decals
		if (decals != null) {
			for (Iterator<DecalFeature> it = decals.iterator(); it.hasNext(); ) {
				DecalFeature decal = it.next();
				decal.create();
			}
		}
	}

	/**
	 * @return true if the item is solid and can't be moved through or shot through
	 */
	public boolean isSolid() {
		return false;
	}

	/**
	 * @return true if the item is impassable and can't be moved through but CAN be shot through
	 */
	public boolean isImpassable() {
		return isSolid();
	}

	/**
	 * @return true if a bullet can pass through this tile
	 */
	public boolean isBulletThrough() {
		return !isSolid();
	}

	/**
	 * @return Returns the animation for this tile, if it has no image
	 */
	public final Animation getAnimation() {
		assert isCreated() : this+" is not created";
		return animation;
	}

	/**
	 * @return Returns the image to use for this item, if it's not animated.
	 */
	public final SpriteImage getImage() {
		assert isCreated() : this+" is not created";
		return imageResource;
	}

	/**
	 * Stash in a sprite
	 * @param sprite
	 * @param toggled
	 */
	public void toSprite(Sprite sprite) {
		assert isCreated() : this+" is not created";
		if (sprite == null) {
			return;
		}
		if (imageResource != null) {
			sprite.setAnimation(null);
			sprite.setImage(imageResource);
			sprite.setVisible(true);
		} else if (animation != null) {
			if (sprite.getAnimation() != animation) {
				sprite.setAnimation(animation);
			}
			sprite.setVisible(true);
		} else {
			sprite.setVisible(false);
		}

		if (spriteLayer != 0) {
			sprite.setLayer(spriteLayer);
		}

//		// Ensure tiles on same layer as entities get drawn underneath when they're exactly aligned
//		sprite.setYSortOffset(1);
	}

	/**
	 * @return Returns the index.
	 */
	public final short getIndex() {
		return i;
	}

	/**
	 * @return Returns the layer.
	 */
	public final int getLayer() {
		return layer;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doRegister()
	 */
	@Override
	protected void doRegister() {
		if (spec) {
			return;
		}
		if (TILES[i] != null) {
			throw new RuntimeException("Tile "+i+" is already defined as "+TILES[i]);
		}
		TILES[i] = this;
		maxTileIndex = Math.max(i, maxTileIndex);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doDeregister()
	 */
	@Override
	protected void doDeregister() {
		if (spec) {
			return;
		}
		TILES[i] = null;
	}

	/**
	 * Called when the tile is drawn on the map
	 * @param map
	 * @param x
	 * @param y
	 */
	public void onDrawn(GameMap map, int x, int y) {
	}

	/**
	 * Called when the tile is removed from the map
	 * @param map
	 * @param x
	 * @param y
	 */
	public void onCleared(GameMap map, int x, int y) {
	}

	/**
	 * Draw to a map
	 * @param map
	 * @param x
	 * @param y
	 * @return the tile just drawn if using a sequence
	 */
	public Tile toMap(GameMap map, int x, int y, boolean useRandom) {
		assert isCreated() : this+" is not created";
//		// Maybe clear item. Do this first so undo works
//		if (getLayer() == 0 && isSolid()) {
//			map.clearItem(x, y);
//		}

		// If we've got a random set, pick one
		Tile next = getNextTile();
		if (random != null && useRandom) {
			Tile t = (Tile) random.getResource(Util.random(0, random.getNumResources() - 1));
			map.setTile(x, y, t.getLayer(), t);
		} else if (next != null && useRandom) {
			// Check existing tile. If it's the same as this tile, use the next tile.
			Tile old = map.getTile(x, y, getLayer());
			if (old == this) {
				next.toMap(map, x, y, false);
				return next;
			} else {
				map.setTile(x, y, getLayer(), this);
			}
		} else {
			// Otherwise, it's just a straightforward draw...
			map.setTile(x, y, getLayer(), this);
		}

		return null;
	}

	/**
	 * @return Returns the emitter, if any
	 */
	public final EmitterFeature getEmitter() {
		return emitterFeature;
	}

	/**
	 * @return Returns the emitter position; may be null (only valid if there's an emitter feature)
	 */
	public final Point getEmitterPos() {
		return emitterPos;
	}

	/**
	 * @return Returns the group.
	 */
	public final String getGroup() {
		return group;
	}

	/**
	 * @return the Empty Tile which is always tile 0
	 */
	public static Tile getEmptyTile() {
		return getTile(0);
	}

	/**
	 * @return Returns the nextTileResource.
	 */
	public Tile getNextTile() {
		if (nextTile != null && nextTileResource == null) {
			// Parse nextTile as a number and use that index instead
			return getTile(Integer.parseInt(nextTile));
		}
		return nextTileResource;
	}

	/**
	 * @return Returns the tooltip.
	 */
	public String getTooltip() {
		if (tooltip == null) {
			return Resources.getTag(getClass());
		} else {
			return tooltip;
		}
	}

	/**
	 * @param tileSet
	 */
	public void setTileSet(TileSetFeature tileSet) {
		this.tileSetFeature = tileSet;
	}

	/**
	 * @return Returns the tileSetFeature.
	 */
	public TileSetFeature getTileSet() {
		return tileSetFeature;
	}

	/**
	 * @return Returns the spriteLayer.
	 */
	public int getSpriteLayer() {
		return spriteLayer;
	}

	/**
	 * @return a List of DecalFeatures, or null, if there are no decals
	 */
	public List<DecalFeature> getDecals() {
		return decals;
	}

	/**
	 * @return Returns the colored.
	 */
	public MappedColor getColor() {
		return colored;
	}

	/**
	 * @return layers
	 */
	public LayersFeature getLayers() {
		return layersFeature;
	}

	/**
	 * Processes this tile with the incoming {@link MapProcessor}, when the map is loaded
	 * @param processor
	 * @param x
	 * @param y
	 */
	public void process(MapProcessor processor, int x, int y) {
	}

	/**
	 * @return true if we're to attenuate this tile's main sprite image
	 */
	public boolean isAttenuated() {
		return attenuated;
	}

	/**
	 * @return the cost to traverse the tile
	 */
	public float getCost() {
		return cost;
	}
}
