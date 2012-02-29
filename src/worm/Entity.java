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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.Tickable;

import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.lwjgl.util.vector.Vector2f;

import worm.animation.ThingWithLayers;
import worm.entities.Bomb;
import worm.entities.Building;
import worm.entities.Bullet;
import worm.entities.Gidrah;
import worm.entities.Saucer;
import worm.entities.Smartbomb;
import worm.entities.Unit;
import worm.features.LayersFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.algorithms.Bresenham;
import com.shavenpuppy.jglib.sprites.Animation;
import com.shavenpuppy.jglib.sprites.ReadablePosition;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.Util;

/**
 * $Id: Entity.java,v 1.90 2010/10/31 12:38:03 foo Exp $
 * @version $Revision: 1.90 $
 * @author $Author: foo $
 * <p>
 */
public abstract class Entity implements Tickable, Serializable, ReadablePosition, ThingWithLayers {

	private static final long serialVersionUID = 1L;

	private static final Rectangle BOUNDS = new Rectangle();
	private static final Rectangle TEMP = new Rectangle();
	private static final ArrayList<Entity> ENTITYCACHE = new ArrayList<Entity>();
	private static final Bresenham BRESENHAM = new Bresenham();

	/** Collision quadtree */
	private static final CollisionManager COLLISIONMANAGER = new GridCollisionManager(MapRenderer.TILE_SIZE);// QuadTree(WormGameState.MAP_WIDTH, WormGameState.MAP_HEIGHT, 6);

	/** Node of the quadtree we're in */
	private transient CollisionManager node;

	/** Location */
	private float mapX, mapY, oldX, oldY, oldR;

	/** Tile X & Y */
	private int tileX, tileY;

	/** Screen location */
	private float screenX, screenY;

	/** Last known appearance */
	private LayersFeature oldAppearance;

	/** Active? */
	private boolean active;

	/** Flash */
	private boolean flash;

	/** Visible? */
	private boolean visible = true;

	/** Sprites */
	private Sprite[] sprite;


	private int hackyTick = 4;

	/** offset totals */
	private float yOffsetTotal = 0;
	private float xOffsetTotal = 0;

	/**
	 * C'tor
	 */
	public Entity() {
	}

	/**
	 * Returns the offset for any lasers this entity might be using (ok, the Saturn boss)
	 * @return
	 */
	public float getBeamXOffset() {
		return getOffsetX();
	}

	/**
	 * Returns the offset for any lasers this entity might be using (ok, the Saturn boss)
	 * @return
	 */
	public float getBeamYOffset() {
		return getOffsetY();
	}

	/**
	 * @param visible the visible to set
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
		if (sprite != null) {
			for (Sprite element : sprite) {
				if (element != null) {
					element.setVisible(visible);
				}
			}
		}
	}

	/**
	 * @return the visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Drop a building on top of this entity. Damages the building by the specified number of hitpoints returned.
	 * @return int
	 */
	public int crush() {
		return 0;
	}

	/**
	 * Get the distance to a coordinate
	 * @param xx
	 * @param yy
	 * @return distance, in pixels
	 */
	public float getDistanceTo(float xx, float yy) {
		return Vector2f.sub(new Vector2f(xx, yy), new Vector2f(getX(), getY()), null).length();
	}

	/**
	 * Get the distance to another entity
	 * @param xx
	 * @param yy
	 * @return distance, in pixels
	 */
	public float getDistanceTo(Entity e) {
		return Vector2f.sub(new Vector2f(e.getX(), e.getY()), new Vector2f(getX(), getY()), null).length();
	}

	/**
	 * Can we see this location?
	 * @param mapX
	 * @param mapY
	 * @param positive If non null, if this entity is detected, algorithm returns true
	 * @param targetFlying If true, and positive is a flying gidrah, we ignore walls and simply return true
	 * @return true if there is a LOS to the specified map location
	 */
	public boolean canSee(float mapX, float mapY, Entity positive, boolean targetFlying) {
		if (targetFlying && positive != null && positive.isFlying()) {
			return true;
		}
		WormGameState gameState = Worm.getGameState();
		GameMap map = gameState.getMap();
		ArrayList<Entity> entities = gameState.getEntities();
		int n = entities.size();
		// Create a list of solid entities we think are somewhere in the LOS
		ENTITYCACHE.clear();
		for (int i = 0; i < n; i ++) {
			Entity e = entities.get(i);
			if (e != this && e.isActive() && e.isSolid()) {
				double dist = Util.distanceFromLineToPoint(getX(), getY(), mapX, mapY, e.getX(), e.getY());
				if (dist >= 0.0 && dist <= e.getRadius()) {
					ENTITYCACHE.add(e);
				}
			}
		}
		int numCachedEntities = ENTITYCACHE.size();
		BRESENHAM.plot((int) getX(), (int) getY(), (int) mapX, (int) mapY);
		int oldMapX = -1, oldMapY = -1;
		while (BRESENHAM.next()) {
			int x = BRESENHAM.getX() / MapRenderer.TILE_SIZE;
			int y = BRESENHAM.getY() / MapRenderer.TILE_SIZE;
			if (x != oldMapX || y != oldMapY) {
				for (int z = 0; z < GameMap.LAYERS; z ++) {
					Tile tile = map.getTile(x, y, z);
					if (tile != null && !tile.isBulletThrough()) {
						// Tile blocks LOS
						return false;
					}
				}
				oldMapX = x;
				oldMapY = y;
			}

			// Check entity list
			if (positive != null) {
				for (int i = 0; i < numCachedEntities; i ++) {
					Entity e = ENTITYCACHE.get(i);
					if (e == positive && e.getDistanceTo(BRESENHAM.getX(), BRESENHAM.getY()) < e.getRadius()) {
						return true;
					}
				}
			}

			// Skip 4 pixels at a time
			if (!BRESENHAM.next()) {
				break;
			}
			if (!BRESENHAM.next()) {
				break;
			}
			if (!BRESENHAM.next()) {
				break;
			}
		}
		return true;
	}


	/**
	 * Respawn from saved state
	 * @param screen
	 */
	public final void respawn(SpriteAllocator screen) {
		doRespawn();
	}

	/**
	 * Called after deserializing a game.
	 */
	protected void doRespawn() {
	}

	@Override
	public final void spawn(Screen screen) {
		createSprites(screen);
		if (sprite == null) {
			remove();
			return;
		}
		active = true;
		Worm.getGameState().addEntity(this);
		doSpawn();
		update();
	}

	protected void createSprites(Screen screen) {
		setSprites(new Sprite[] { screen.allocateSprite(this) });
	}

	/**
	 * Called by the game state to add an entity to the appropriate list
	 * @param gsi
	 */
	public abstract void addToGameState(GameStateInterface gsi);

	/**
	 * Called by the game state to remove an entity from the appropriate list
	 * @param gsi
	 */
	public abstract void removeFromGameState(GameStateInterface gsi);

	/**
	 * Called by spawn() after the entity has been added to the screen
	 */
	protected void doSpawn() {
	}

	/**
	 * Can the entity collide?
	 * @return boolean
	 */
	public abstract boolean canCollide();

	/**
	 * Can the entity be crushed by a building?
	 * @return true if the entity can be crushed
	 */
	public boolean canBeCrushed() {
		return isActive() && isSolid() && canCollide();
	}

	/**
	 * Is the entity alive? If not it is removed
	 * @return boolean
	 */
	@Override
	public final boolean isActive() {
		return active;
	}

	/**
	 * Remove the entity
	 */
	@Override
	public final void remove() {
		removeSprites();
		if (!active) {
			// Already removed
			return;
		}
		active = false;
		Worm.getGameState().removeEntity(this);
		if (node != null) {
			node.remove(this);
			node = null;
		}
		doRemove();
	}

	/**
	 * Remove the sprite(s)
	 */
	protected final void removeSprites() {
		if (sprite != null) {
			for (Sprite element : sprite) {
				if (element != null) {
					element.deallocate();
				}
			}
			sprite = null;
		}
	}

	/**
	 * Sets a new bunch of sprites to use
	 * @param newSprite
	 */
	@Override
	public final void setSprites(Sprite[] newSprite) {
		removeSprites();
		sprite = newSprite;
		update();
	}

	@Override
	public LayersFeature getAppearance() {
		return null;
	}

	@Override
	public void requestSetAppearance(LayersFeature newAppearance) {
	}

	protected void doRemove() {
	}

	/**
	 * Collision with another entity
	 * @param inCollisionWith The entity with which we have collided
	 */
	public abstract void onCollision(Entity entity);

	public void onCollisionWithSmartbomb(Smartbomb smartbomb) {
	}

	public void onCollisionWithGidrah(Gidrah gidrah) {
	}

	public void onCollisionWithUnit(Unit unit) {
	}

	public void onCollisionWithSaucer(Saucer saucer) {
	}

	public void onCollisionWithBuilding(Building building) {
	}

	public void onCollisionWithBullet(Bullet bullet) {
	}

	public void onCollisionWithBomb(Bomb bomb) {
	}

	/**
	 * Get radius. Only applicable if isRound() returns true.
	 * @return radius
	 */
	public abstract float getRadius();

	/**
	 * Get bounds. Only applicable if isRound() returns false.
	 * @param bounds Destination rectangle to stash bounds in, or null, to create a new Rectangle
	 * @returns bounds, or a new Rectangle, if bounds was null.
	 */
	public abstract Rectangle getBounds(Rectangle bounds);

	/**
	 * @return true if this entity is round, or false if rectangular
	 */
	public abstract boolean isRound();

	/**
	 * Get all the entities who are touching this entity
	 * @param dest A list to store the entities in, or null, to construct a new one
	 * @return dest, or a new List if dest was null
	 */
	public final List<Entity> checkCollisions(List<Entity> dest) {
		if (node != null) {
			return node.checkCollisions(this, dest);
		} else {
			// Haven't got a node in the quadtree. Use the root
			assert false : this + " is not in the quadtree!";
			return COLLISIONMANAGER.checkCollisions(this, dest);
		}
	}

	/**
	 * Find out which entities are touching the specified rectangle
	 * @param rect
	 * @param dest
	 * @return
	 */
	public static List<Entity> getCollisions(ReadableRectangle rect, List<Entity> dest) {
		return COLLISIONMANAGER.checkCollisions(rect, dest);
	}

	/**
	 * Are we touching a specific point?
	 * @param x
	 * @param y
	 * @return boolean
	 */
	public final boolean isTouching(float x, float y) {
		if (isRound() && getRadius() == 0) {
			return false;
		}

		if (!isRound() && getBounds(BOUNDS).isEmpty()) {
			return false;
		}

		if (isRound()) {
			return getDistanceTo(x, y) < getRadius();
		} else {
			return BOUNDS.contains((int) x, (int) y);
		}
	}

	/**
	 * Are we touching a circle?
	 * @param x
	 * @param y
	 * @return boolean
	 */
	public final boolean isTouching(float x, float y, float radius) {
		if (isRound() && getRadius() == 0) {
			return false;
		}

		if (!isRound() && getBounds(BOUNDS).isEmpty()) {
			return false;
		}

		if (isRound()) {
			return getDistanceTo(x, y) < getRadius() + radius;
		} else {
			return rectRoundCollisionCheck(x, y, radius, BOUNDS);
		}
	}

	public final boolean isTouching(ReadableRectangle rect) {
		if (isRound() && getRadius() == 0) {
			return false;
		}

		if (!isRound() && getBounds(BOUNDS).isEmpty()) {
			return false;
		}

		if (isRound()) {
			return rectRoundCollisionCheck(mapX, mapY, getRadius(), rect);
		} else {
			return BOUNDS.intersects(rect);
		}
	}

	/**
	 * Collision detection.
	 * @param dest The entity to check for collision with.
	 * @return true if this entity is touching the destination entity; note that an entity can never be touching itself
	 */
	public final boolean isTouching(Entity dest) {
		if (dest == this) {
			return false;
		}

		if (isRound() && getRadius() == 0) {
			return false;
		}

		if (dest.isRound() && dest.getRadius() == 0) {
			return false;
		}

		if (!isRound() && getBounds(BOUNDS).isEmpty()) {
			return false;
		}

		if (!dest.isRound() && dest.getBounds(TEMP).isEmpty()) {
			return false;
		}

		// At this point, BOUNDS and TEMP hold bounding rectangles, which we might
		// use if it's a rect-rect collision

		if (isRound() && dest.isRound()) {
			// Round-Round collision check
			float dx = dest.mapX + dest.getCollisionX() - (this.mapX + this.getCollisionX());
			float dy = dest.mapY + dest.getCollisionY() - (this.mapY + this.getCollisionY());
			dx *= dx;
			dy *= dy;

			return Math.sqrt(dx + dy) < getRadius() + dest.getRadius();
		} else if (isRound() && !dest.isRound()) {
			// Round-Rect collison check
			return rectRoundCollisionCheck(mapX + getCollisionX(), mapY + getCollisionY(), getRadius(), TEMP);
		} else if (!isRound() && dest.isRound()) {
			// Rect-round collision check
			return rectRoundCollisionCheck(dest.mapX + dest.getCollisionX(), dest.mapY + dest.getCollisionY(), dest.getRadius(), BOUNDS);
		} else {
			// Rect-rect collision check
			return BOUNDS.intersects(TEMP);
		}
	}

	private boolean rectRoundCollisionCheck(float x, float y, float radius, ReadableRectangle bounds) {
		return GeomUtil.circleRectCollision(x, y, radius, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
	}

	/**
	 * Tick. Called every frame if this entity is alive.
	 */
	@Override
	public final void tick() {
		try {
			doTick();
			float newR = getRadius();

			if (node != null) {
				if (canCollide() && isActive()) {
					// If we've changed radius, remove and re-add to the quadtree
					if (oldR != newR) {
						oldR = newR;
						addToCollisionManager();
					}
				} else {
					// Can't collide any more - remove from quadtree
					node.remove(this);
					node = null;
				}
			} else {
				if (canCollide() && isActive()) {
					// Add us in to the quadtree
					addToCollisionManager();
				}
			}
		} catch (Exception e) {
			System.err.println("Error ticking " + this);
			e.printStackTrace(System.err);
			remove();
		}
	}

	/**
	 * Do ticking
	 */
	protected void doTick() {
	}

	/**
	 * Recalculate screen positions relative to map scroll
	 */
	protected void calculateScreenPosition() {
		screenX = (int) mapX + GameScreen.getSpriteOffset().getX();
		screenY = (int) mapY + GameScreen.getSpriteOffset().getY();
	}

	protected final void setScreenX(float newScreenX) {
		screenX = newScreenX;
	}

	protected final void setScreenY(float newScreenY) {
		screenY = newScreenY;
	}

	/**
	 * @return screen coordinates
	 */
	public final float getScreenX() {
		return screenX;
	}

	/**
	 * @return screen coordinates
	 */
	public final float getScreenY() {
		return screenY;
	}

	/*
	 * (non-Javadoc)
	 * @see net.puppygames.applet.Tickable#update()
	 */
	@Override
	public final void update() {
		calculateScreenPosition();

		if (sprite != null) {

			// Firstly, if we're significantly offscreen, hide all the sprites.
			if (getScreenX() < -48.0f || getScreenX() >= Game.getWidth() + 48
					|| getScreenY() < -48.0f - getZ() || getScreenY() >= Game.getHeight() + 48 + getZ())
			{
				for (Sprite element : sprite) {
					element.setVisible(false);
				}
				doUpdate();
				return;
			} else {
				for (Sprite element : sprite) {
					element.setVisible(visible);
				}
			}

			boolean searchForChildOffsets = false;

			for (Sprite element : sprite) {

				element.setLocation(screenX, screenY);
				if (element.getLayer() > Layers.SHADOW) {
					element.setFlash(flash);
				}

				// shall we bother checking anims for childOffset stuff?
				if (element.isDoChildOffset()) {
					searchForChildOffsets=true;
				}
			}

			if (searchForChildOffsets) {

				float xOffset = 0;
				float yOffset = 0;
				yOffsetTotal = 0;
				xOffsetTotal = 0;

				for (int i = 0; i < sprite.length; i ++) {

					boolean doOffset = false;

					// check for offset
					if (sprite[i].getChildXOffset() != 0) {

						xOffset = sprite[i].getChildXOffset();
						xOffset *= FPMath.floatValue(sprite[0].getXScale());
						if (isMirrored() && xOffset!=0) {

							// chaz hack! - if we've got any <offset anim commands they'd need to be mirrored too
							// so for now will disable mirroring of childOffset if <offset x=""/> present
							if (sprite[i].getOffset(null).x==0) {
								xOffset = -xOffset;
							}

						}

						xOffsetTotal += xOffset;
						doOffset = true;
					}
					if (sprite[i].getChildYOffset() != 0) {

						yOffset = sprite[i].getChildYOffset();
						yOffset *= FPMath.floatValue(sprite[0].getYScale());
						yOffsetTotal += yOffset;
						doOffset = true;
					}

					// if we've found an offset apply this to any sprites after where we found the offset

					if (doOffset) {
						for (int j = i+1; j < sprite.length; j ++) {
							if (sprite[j].isDoChildOffset()) {
								sprite[j].setLocation(screenX + xOffsetTotal, screenY + yOffsetTotal);
								sprite[j].setYSortOffset(-yOffsetTotal-j); // the '-j' is chaz hack! budge layers YSortOffset a tad to force render ok
							}
						}
					}

				}
			}


			LayersFeature currentAppearance = getCurrentAppearance();
			float cx = getMapX() + getCollisionX();
			float cy = getMapY() + getCollisionY();
			if (currentAppearance != null && (GameScreen.isDiddlerOpen() || hackyTick > 0 || cx != oldX || cy != oldY || currentAppearance != oldAppearance)) {
				currentAppearance.updateColors(sprite, cx, cy);
				if (hackyTick > 0) {
					hackyTick --;
				}
			}
			oldX = cx;
			oldY = cy;
			oldAppearance = currentAppearance;
		}

		doUpdate();
	}

	protected void doUpdate() {
	}

	/**
	 * @param flash The flash to set.
	 */
	public final void setFlash(boolean flash) {
		this.flash = flash;
	}

	/**
	 * @return Returns the flash.
	 */
	public final boolean isFlashing() {
		return flash;
	}

	/**
	 * Set the location
	 * @param x
	 * @param y
	 */
	public final void setLocation(float x, float y) {
		float oldX = mapX;
		float oldY = mapY;
		this.mapX = x;
		this.mapY = y;

		// Update quadtree
		if (oldX != x || oldY != y) {
			tileX = fastFloor(getX() / MapRenderer.TILE_SIZE);
			tileY = fastFloor(getY() / MapRenderer.TILE_SIZE);
			if (canCollide() && isActive()) {
				addToCollisionManager();
			}
		}
		onSetLocation();
	}

	private static int fastFloor(float x) {
		int i = (int) x;
		return x >= 0.0f ? i : i == x ? i : i - 1;
	}

	protected void onSetLocation() {
	}

	/**
	 * Add this entity to the collision manager
	 */
	protected final void addToCollisionManager() {
		if (node != null) {
			if (!node.remove(this)) {
				assert false : "Entity "+this+" was not found!";
			}
		}
		node = COLLISIONMANAGER.add(this);
		if (node == null) {
			node = COLLISIONMANAGER;
			COLLISIONMANAGER.store(this);
		}
	}

	/**
	 * Add to collision manager if we're not already added
	 */
	protected final void maybeAddToCollisionManager() {
		if (node != null) {
			return;
		}
		addToCollisionManager();
	}

	/**
	 * @return the X collision offset
	 */
	public final float getCollisionX() {
		if (isRound()) {
			return getRoundCollisionX();
		} else {
			getBounds(TEMP);
			return TEMP.getX() - getMapX() + TEMP.getWidth() * 0.5f;
		}
	}
	protected float getRoundCollisionX() {
		return 0.0f;
	}

	/**
	 * @return the Y collision offset
	 */
	public final float getCollisionY() {
		if (isRound()) {
			return getRoundCollisionY();
		} else {
			getBounds(TEMP);
			return TEMP.getY() - getMapY() + TEMP.getHeight() * 0.5f;
		}
	}
	protected float getRoundCollisionY() {
		return 0.0f;
	}

	@Override
	public final float getX() {
		return getMapX() + getCollisionX();
	}

	@Override
	public final float getY() {
		return getMapY() + getCollisionY();
	}

	@Override
	public float getZ() {
		return 0.0f;
	}

	/**
	 * @return Returns the x.
	 */
	public final float getMapX() {
		return mapX;
	}

	/**
	 * @return Returns the y.
	 */
	public final float getMapY() {
		return mapY;
	}

	/**
	 * Returns the "shooting" position - the offset at which bullets etc. should spawn from
	 */
	public float getOffsetX() {
		return getCollisionX();
	}

	/**
	 * Returns the "shooting" position - the offset at which bullets etc. should spawn from
	 */
	public float getOffsetY() {
		return getCollisionY();
	}

	/**
	 * @return the current sprite "event", or 0 if there is no sprite
	 */
	public final int getEvent() {
		if (sprite == null || sprite.length == 0 || sprite[0] == null) {
			return 0;
		} else {
			return sprite[0].getEvent();
		}
	}

	/**
	 * Set the current sprite event
	 * @param event
	 */
	public final void setEvent(int event) {
		if (sprite != null && sprite[0] != null) {
			sprite[0].setEvent(event);
		}
	}

	/**
	 * Set the sprite's appearance
	 * @param appearance
	 */
	public final void setAnimation(int idx, Animation animation) {
		sprite[idx].setAnimation(animation);
	}

	/*
	 * (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.FlippedAndMirrored#isMirrored()
	 */
	public boolean isMirrored() {
		return sprite[0].isMirrored();
	}

	/**
	 * @param mirrored
	 */
	public void setMirrored(boolean mirrored) {
		if (sprite != null) {
			for (Sprite element : sprite) {
				element.setMirrored(mirrored);
			}
		}
	}

	/**
	 * @param alpha
	 */
	public void setAlpha(int alpha) {
		if (sprite != null) {
			for (Sprite element : sprite) {
				element.setAlpha(alpha);
			}
		}
	}

	/**
	 * @return true if the entity is shootable by the player's turrets
	 */
	public abstract boolean isShootable();

	/**
	 * @return true if the entity is a powerup
	 */
	public boolean isPowerup() {
		return false;
	}

	@Override
	public final Sprite getSprite(int n) {
		if (sprite == null) {
			// Not yet initialised
			return null;
		}
		return sprite[n];
	}

	protected final int getNumSprites() {
		if (sprite == null) {
			return 0;
		} else {
			return sprite.length;
		}
	}

	/**
	 * @return true if we should reflect laser beams
	 */
	public boolean isLaserProof() {
		return false;
	}

	/**
	 * @return true if the entity is disruptor proof
	 */
	public boolean isDisruptorProof() {
		return false;
	}

	/**
	 * Damage with a laser
	 * @param amount
	 * @return true if the beam is absorbed
	 */
	public boolean laserDamage(int amount) {
		return true;
	}

	/**
	 * @return true if lasers should pass through this entity
	 */
	public boolean isLaserThrough() {
		return !isSolid() && !isLaserProof();
	}

	/**
	 * @return true if the laser fires over this entity when firing at aerial targets
	 */
	public boolean isLaserOver() {
		return isLaserThrough();
	}

	public void capacitorDamage(int amount) {
	}

	public void stunDamage(int amount) {
	}

	public void disruptorDamage(int amount, boolean friendly) {
	}

	public void explosionDamage(int damageAmount, boolean friendly) {
	}

	/**
	 * Augment the power of any weapon being used
	 * @return
	 */
	public int getExtraDamage() {
		return 0;
	}

	/**
	 * No two solid objects can occupy the same space at the same time.
	 * @return true if this is a solid object
	 */
	public boolean isSolid() {
		return false;
	}

	/**
	 * Is this entity attackable by gidrahs?
	 * @return
	 */
	public boolean isAttackableByGidrahs() {
		return false;
	}

	/**
	 * Is this entity attackable by units?
	 * @return
	 */
	public boolean isAttackableByUnits() {
		return false;
	}

	/**
	 * Is this entity clickable by the player? (Powerups, saucers)
	 * @return
	 */
	public boolean isClickable() {
		return false;
	}

	/**
	 * Can we hover the mouse over this entity?
	 * @return
	 */
	public boolean isHoverable() {
		return true;
	}

	/**
	 * Gets the mouse pointer we should use when hovering over this entity
	 * @param clicked Whether the mouse is being clicked
	 * @return a Mouse pointer appearance
	 */
	public LayersFeature getMousePointer(boolean clicked) {
		if (Worm.getGameState().inRangeOfCapacitor()) {
			return Worm.getGameState().isBezerk() ? Res.getMousePointerBezerkOnTarget() : Res.getMousePointerOnTarget();
		} else {
			return Res.getMousePointer();
		}
	}

	/**
	 * Called when the player clicks on this entity
	 * @param mode The current mode
	 * @return a ClickAction constant
	 */
	public int onClicked(int mode) {
		return ClickAction.IGNORE;
	}

	/**
	 * Called when the player hovers the mouse over this entity
	 * @param mode The current mode
	 */
	public void onHovered(int mode) {
	}


	/**
	 * Called when the player stops hovering the mouse over this entity
	 * @param mode The current mode
	 */
	public void onLeave(int mode) {
	}

	/**
	 * @return the entity's X tile coordinate
	 */
	public final int getTileX() {
		return tileX;
	}

	/**
	 * @return the entity's Y tile coordinate
	 */
	public final int getTileY() {
		return tileY;
	}

	/**
	 * For those entities that have an appearance that uses a LayersFeature, override
	 * this method to return the current appearance.
	 * <p>The default is to return null.
	 * @return null, or a LayersFeature
	 */
	protected LayersFeature getCurrentAppearance() {
		return null;
	}

	public boolean usesAmmo() {
		return false;
	}

	public void onReloaded() {
	}

	/**
	 * Process collisions
	 */
	public static void checkCollisions() {
		COLLISIONMANAGER.checkCollisions();
	}

	public static void reset() {
		COLLISIONMANAGER.clear();
	}


	public float getFinalXOffset() {
		return this.xOffsetTotal;
	}
	public float getFinalYOffset() {
		return this.yOffsetTotal;
	}

	public boolean isFlying() {
		return false;
	}

	/**
	 * Called when the target has just completely deflected a bullet
	 * @param target
	 */
	public void onBulletDeflected(Entity target) {
	}

	/**
	 * @return a List of Entities this Entity should "ignore", or null
	 */
	public ArrayList<Entity> getIgnore() {
		return null;
	}
}
