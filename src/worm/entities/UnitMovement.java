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
package worm.entities;

import java.util.ArrayList;

import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.MapRenderer;
import worm.WormGameState;
import worm.path.AStar;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.util.IntList;
import com.shavenpuppy.jglib.util.Util;

/**
 * Handles movement for a unit
 */
class UnitMovement implements Movement {

	private static final long serialVersionUID = 1L;

	private static final ArrayList<Entity> COLLISIONS = new ArrayList<Entity>();
	private static final Rectangle BOUNDS = new Rectangle();

	private static final int MAX_TOTAL_THINK_TIME = 128;
	private static final int MAX_THINK_TIME = 32;

	private static int totalThinkTime = 0;

	/** The unit */
	final Unit unit;

	/** Pathfinding */
	final AStar astar;

	/** Topology */
	final UnitGameMapTopology topology;

	/** Path */
	final IntList path = new IntList(true, WormGameState.ABS_MAX_SIZE);

	/** Movement start and end */
	float sourceMapX, sourceMapY, targetMapX, targetMapY;

	/** Movement paused */
	boolean paused;

	/** Thinking */
	boolean thinking;

	/** Movement tick */
	int tick, currentSpeed;

	/** Occupied position */
	int occupiedX, occupiedY;

	/**
	 * C'tor
	 */
	UnitMovement(Unit unit) {
		this.unit = unit;
		topology = new UnitGameMapTopology(this);
		astar = new AStar(topology);
	}

	/**
	 * @return true if the move succeeded
	 */
	boolean updateLocation() {
		float ratio = (float) tick / currentSpeed;
		float oldX = unit.getMapX();
		float oldY = unit.getMapY();
		float newX;
		unit.setLocation
			(
				newX = LinearInterpolator.instance.interpolate(targetMapX, sourceMapX, ratio),
				LinearInterpolator.instance.interpolate(targetMapY, sourceMapY, ratio)
			);

		// If we bump into another unit, rollback
		Entity.getCollisions(unit.getBounds(BOUNDS), COLLISIONS);
		for (int i = COLLISIONS.size(); -- i >= 0; ) {
			Entity e = COLLISIONS.get(i);
			if (e == unit) {
				continue;
			}
			if (e instanceof Unit && System.identityHashCode(e) > System.identityHashCode(unit)) {
				// Rollback
				unit.setLocation(oldX, oldY);
				return false;
			}

		}

		if (oldX < newX) {
			unit.setMirrored(false);
		} else if (oldX > newX) {
			unit.setMirrored(true);
		}

		return true;
	}

	@Override
	public void adjust(float newX, float newY) {
	}

	@Override
	public void remove() {
		astar.cancel();
	}

	@Override
	public boolean isMoving() {
		return !paused && !thinking;
	}


	@Override
	public void tick() {
		if (paused) {
			tick --;
			if (tick <= 0) {
				paused = false;
				// Choose next square to move to and start moving
				chooseDestination();
			}
		} else {
			if (thinking) {
				think();
				if (thinking) {
					// Still thinking
					return;
				}
			}
			if (tick > 0) {
				tick --;
				if (!updateLocation()) {
					tick ++;
				}
			}

			if (tick == 0) {
				chooseDestination();
			}
		}
	}

	/**
	 * @return the unit
	 */
	public Unit getUnit() {
		return unit;
	}

	@Override
	public void reset() {
		path.clear();
	}

	/**
	 * Choose the next square to move to
	 */
	void chooseDestination() {
		sourceMapX = unit.getMapX();
		sourceMapY = unit.getMapY();

		// Now, where are we going?

		// Firstly if we've got a path that leads to the destination, let's check to see if the next step in that path is clear. If it is clear,
		// let's carry on using it instead of invoking A*.
		if (next()) {
			return;
		}

		path.clear();
		astar.findPath(UnitGameMapTopology.pack(unit.getTileX(), unit.getTileY()), UnitGameMapTopology.pack(unit.getTarget().getTileX(), unit.getTarget().getTileY()), path);
		thinking = true;
		think();
	}

	/**
	 * Take the next step along our chosen path.
	 * @return false if we need to calculate a new path
	 */
	boolean next() {
		if (path.size() == 0) {
			return false;
		}

		int nextTarget = path.remove(0);
		int targetTileX = GidrahGameMapTopology.getX(nextTarget);
		int targetTileY = GidrahGameMapTopology.getY(nextTarget);
		if (topology.canMove(unit.getTileX(), unit.getTileY(), targetTileX, targetTileY)) {
			// If this is a diagonal move, we take a bit longer over it, and ensure at least 1 route
			if (Math.abs(unit.getTileX() - targetTileX) + Math.abs(unit.getTileY() - targetTileY) > 1) {
				tick = (int) (unit.getFeature().getSpeed() * 1.42f);
			} else {
				tick = unit.getFeature().getSpeed();
			}
			currentSpeed = tick;
			targetMapX = targetTileX * MapRenderer.TILE_SIZE + Util.random(0, MapRenderer.TILE_SIZE - 1);
			targetMapY = targetTileY * MapRenderer.TILE_SIZE + Util.random(0, MapRenderer.TILE_SIZE - 1);
			thinking = false;
			paused = false;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Search A* pathfinder for a few steps. If we find the goal, start moving. If we fail, choose destination again. If
	 * we still haven't found anything, just return.
	 */
	void think() {
		for (int i = 0; i < MAX_THINK_TIME && totalThinkTime ++ < MAX_TOTAL_THINK_TIME; i ++) {
			switch (astar.nextStep()) {
				case AStar.SEARCH_STATE_SUCCEEDED:
					// Found the goal! Move one step closer.
					next();
					return;
				case AStar.SEARCH_STATE_FAILED:
					// Total failure. Wait a bit then think again.
					thinking = false;
					paused = true;
					tick = 8;
					path.clear();
					return;
				case AStar.SEARCH_STATE_SEARCHING:
					// Carry on searching;
					break;
				default:
					assert false;
					break;
			}
		}
		// Just return
		return;
	}

	@Override
	public void maybeRethink(Rectangle bounds) {
		// Ignore
	}

	@Override
	public void attack() {
		// Ignore
	}

	@Override
	public void dontAttack() {
		// Ignore
	}

	public static void resetTotalThinkTime() {
		totalThinkTime = 0;
	}

}
