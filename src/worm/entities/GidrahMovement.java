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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.Point;
import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.GameMap;
import worm.MapRenderer;
import worm.Worm;
import worm.WormGameState;
import worm.features.GidrahFeature;
import worm.path.AStar;

import com.shavenpuppy.jglib.interpolators.CosineInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.interpolators.OpenLinearInterpolator;
import com.shavenpuppy.jglib.util.IntList;
import com.shavenpuppy.jglib.util.Util;

/**
 * Handles movement for a gidrah
 */
class GidrahMovement implements Movement {

	private static final long serialVersionUID = 1L;

	/** A queue of gidrahs who should rethink their routes */
	private static final List<GidrahMovement> QUEUE = new ArrayList<GidrahMovement>(WormGameState.MAX_GIDRAHS);

	/** A Set which shadows QUEUE */
	private static final Set<GidrahMovement> QUEUESET = new HashSet<GidrahMovement>();

	private static final ArrayList<Entity> COLLISIONS = new ArrayList<Entity>();
	private static final Rectangle BOUNDS = new Rectangle();

	private static final float GIDRAH_MAX_SPEED = 4.0f; // in ticks per 16 pixels
	private static final int MAX_TOTAL_THINK_TIME = 256;
	private static final int MAX_THINK_TIME = 256;
	private static final int MAX_RAMP_UP_DURATION = 4800;
	private static final int RAMP_UP_DURATION = 2400;
	private static final int RAMP_UP_PER_LEVEL = 60;
	private static final float SUICIDE_DISTANCE = 256.0f;
	private static final float BASE_DISTANCE = 512.0f;
	private static final float DANGER_SPEEDUP_FACTOR = 0.5f;
	private static final int RETHINK_MAX = 300;
	private static final int MAX_FAILS = 100;

	public static int totalThinkTime;

	private static class PointPair implements Serializable {
		private static final long serialVersionUID = 1L;

		final Point a = new Point(), b = new Point();

		@Override
		public int hashCode() {
			return a.hashCode() ^ b.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			PointPair ep = (PointPair) obj;
			return ep.a.equals(a) && ep.b.equals(b) || ep.a.equals(b) && ep.b.equals(a);
		}
		@Override
		public String toString() {
			return "PointPair["+a+","+b+"]";
		}
		public Rectangle getBounds() {
			Rectangle ret = new Rectangle();
			ret.add(a.getX() + 1, a.getY() + 1);
			ret.add(b.getX() + 1, b.getY() + 1);
			return ret;
		}
	}
	private static final PointPair SCRATCH_POINT_PAIR = new PointPair();

	/** The gidrah */
	private final Gidrah gidrah;

	/** Pathfinding */
	private final AStar astar;

	/** Topology */
	private final GidrahGameMapTopology topology;

	/** Path */
	private final IntList path = new IntList(true, WormGameState.ABS_MAX_SIZE);

	/** gameState */
	private final WormGameState gameState;

	/** Game map */
	private final GameMap map;

	/** Gidrah feature */
	private final GidrahFeature feature;

	/** Movement start and end */
	private float sourceMapX, sourceMapY, targetMapX, targetMapY;

	/** Movement paused */
	private boolean paused;

	/** Thinking */
	private boolean thinking;

	/** Knocked back */
	private boolean knockedBack;

	/** Movement tick */
	private int tick, currentSpeed, rampTick, rethinkTick;

	/** Number of squares to move */
	private int moves;

	/** Occupied position */
	private int occupiedX, occupiedY;

	/** Current path start and end */
	private PointPair startAndEnd;

	/** Diagonal preference */
	private boolean diagonal;

	/** Fail count */
	private int failCount;

	/**
	 * C'tor
	 */
	GidrahMovement(Gidrah gidrah) {
		this.gidrah = gidrah;
		this.feature = gidrah.getFeature();
		this.gameState = Worm.getGameState();
		this.map = gameState.getMap();

		diagonal = feature.getDiagonal();
		topology = new GidrahGameMapTopology(this);
		astar = new AStar(topology);
		if (!feature.isGidlet()) {
			map.setOccupied(occupiedX = gidrah.getTileX(), occupiedY = gidrah.getTileY());
		}

	}

	boolean isDiagonal() {
		return diagonal;
	}

	@Override
	public void attack() {
		map.setAttacking(occupiedX, occupiedY);
	}

	@Override
	public void dontAttack() {
		map.clearAttacking(occupiedX, occupiedY);
	}

	@Override
	public void adjust(float newX, float newY) {
		sourceMapX = newX;
		sourceMapY = newY;
		targetMapX = occupiedX * MapRenderer.TILE_SIZE;
		targetMapY = occupiedY * MapRenderer.TILE_SIZE;
		thinking = false;
		paused = false;

		float dx = targetMapX - sourceMapX;
		float dy = targetMapY - sourceMapY;
		if (dx == 0.0f && dy == 0.0f) {
			return;
		}
		// getspeed is the time the gidrah takes to move 16 pixels.
		// therefore getspeed/16 is teh time the gidrah takes to move 1 pixel
		// therefore the movement should be distance * getspeed / 16
		tick = currentSpeed = (int) Math.max(1, (Math.sqrt(dx * dx + dy * dy) * getSpeed() / 16.0f));
		knockedBack = true;
	}

	/**
	 * @return true if the move succeeded
	 */
	boolean updateLocation() {
		float ratio = (float) tick / currentSpeed;
		float oldX = gidrah.getMapX();
		float oldY = gidrah.getMapY();
		float newX;
		gidrah.setLocation
			(
				newX = LinearInterpolator.instance.interpolate(targetMapX, sourceMapX, ratio),
				LinearInterpolator.instance.interpolate(targetMapY, sourceMapY, ratio)
			);

		// Gidlets aren't allowed to bump into other gidrahs
		if (feature.isGidlet()) {
			// If we bump into another gidrah, rollback
			Entity.getCollisions(gidrah.getBounds(BOUNDS), COLLISIONS);
			for (int i = 0; i < COLLISIONS.size(); i ++) {
				Entity e = COLLISIONS.get(i);
				if (e == gidrah) {
					continue;
				}
				if (e instanceof Gidrah && System.identityHashCode(e) > System.identityHashCode(gidrah)) {
					// Rollback
					gidrah.setLocation(oldX, oldY);
					return false;
				}

			}
		}

		if (oldX < newX) {
			gidrah.setMirrored(false);
		} else if (oldX > newX) {
			gidrah.setMirrored(true);
		}

		return true;
	}

	@Override
	public void remove() {
		astar.cancel();

		if (!feature.isGidlet()) {
			map.clearOccupied(occupiedX, occupiedY);
		}

		// Remove from the queue if we're in there
		if (QUEUESET.contains(this)) {
			QUEUESET.remove(this);
			QUEUE.remove(this);
		}
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
				} else {
					if (!gameState.isLevelActive()) {
						rampTick ++;
					}
				}
			}

			if (tick <= 0) {
				// Stop moving now, maybe
				moves --;
				if (moves <= 0) {
					if (knockedBack) {
						knockedBack = false;
					} else {
						tick = getPause();
					}
					if (tick > 0) {
						paused = true;
					} else {
						chooseDestination();
					}
				} else {
					chooseDestination();
				}
			}
		}
	}

	int getSpeed() {
		float minSpeed = feature.getSpeed(); // in pixels/sec
		float maxSpeed = feature.getSpeed() * MAX_SPEED_MULTIPLIER; // in pixels/sec
		float ret;
		float difficulty = gameState.getDifficulty();
		if (feature.isExploding()) {
			// Speed up as we get in range of the target
			Entity target = gidrah.getTarget();
			if (target != null && target.isActive()) {
				float distance = gidrah.getDistanceTo(target);
				if (distance > 0.0f) {
					float ratio = distance / SUICIDE_DISTANCE;
					minSpeed = CosineInterpolator.instance.interpolate(minSpeed * 4.0f, minSpeed, ratio);
					maxSpeed = CosineInterpolator.instance.interpolate(maxSpeed * 4.0f, maxSpeed, ratio);
				}
			}
		} else {
			// Speed up as we get in range of the base
			Building base = gameState.getBase();
			if (base != null && base.isActive()) {
				float distance = gidrah.getDistanceTo(base);
				if (distance > 0.0f && distance <= BASE_DISTANCE) {
					float ratio = distance / BASE_DISTANCE;
					maxSpeed = LinearInterpolator.instance.interpolate(maxSpeed * (1.0f + difficulty), maxSpeed, ratio);
				}
			}
		}
		ret = OpenLinearInterpolator.instance.interpolate(minSpeed, maxSpeed, difficulty);

		if (gameState.isRushActive()) {
			ret = (int) LinearInterpolator.instance.interpolate
				(
					ret,
					ret * LinearInterpolator.instance.interpolate(1.0f, 3.0f, gameState.getGidrahDeathRatio()),
					getSpeedupRamp()
				);
		}

		if (gidrah.isTangled()) {
			ret = Math.min(maxSpeed, ret * 0.25f);
		} else if (!gidrah.getFeature().isBoss()) {
			// Increase gidrah speed with danger, unless we're a boss
			ret += Math.max(0, map.getDanger(gidrah.getTileX(), gidrah.getTileY()) - gidrah.getFeature().getArmour()) * DANGER_SPEEDUP_FACTOR * difficulty;
		}

		// ret now contains the speed of the gidrah in pixels / 60ticks. We'll convert this into the number of ticks it takes to move
		// 16 pixels, which is what getSpeed() needs to return
		return (int) Math.max(GIDRAH_MAX_SPEED, 960.0f / ret);
	}

	float getSpeedupRamp() {
		if (gameState.isRushActive()) {
			return Math.min(1.0f, (float) rampTick / (float) Math.min(MAX_RAMP_UP_DURATION, RAMP_UP_DURATION + gameState.getLevel() * RAMP_UP_PER_LEVEL)); // ramp up duration takes longer and longer
		} else {
			return 0.0f;
		}
	}

	int getPause() {
		return (int) LinearInterpolator.instance.interpolate(feature.getPause(), feature.getPause() / Movement.MAX_SPEED_MULTIPLIER, gameState.getDifficulty());
	}

	/**
	 * @return the gidrah
	 */
	public Gidrah getGidrah() {
		return gidrah;
	}

	@Override
	public void reset() {
		path.clear();
	}

	/**
	 * Choose the next square to move to
	 */
	void chooseDestination() {
		if (moves <= 0) {
			moves = Util.random(feature.getMoves(), (int) (feature.getMoves() * Movement.MAX_SPEED_MULTIPLIER));
		}

		sourceMapX = gidrah.getMapX();
		sourceMapY = gidrah.getMapY();

		// Now, where are we going?

		// Firstly if we've got a path that leads to the destination, let's check to see if the next step in that path is clear. If it is clear,
		// let's carry on using it instead of invoking A*.
		if (next()) {
			return;
		}

		path.clear();

		startAndEnd = new PointPair();
		startAndEnd.a.setLocation(gidrah.getTileX(), gidrah.getTileY());
		Entity target = gidrah.getTarget();
		startAndEnd.b.setLocation(target.getTileX(), target.getTileY());

		if (startAndEnd.a.equals(startAndEnd.b)) {
			// Already at target!
			if (feature.isGidlet()) {
				targetMapX = target.getTileX() * MapRenderer.TILE_SIZE + Util.random(0, MapRenderer.TILE_SIZE - 1);
				targetMapY = target.getTileY() * MapRenderer.TILE_SIZE + Util.random(0, MapRenderer.TILE_SIZE - 1);
				thinking = false;
				paused = false;
				tick = getSpeed();
				currentSpeed = tick;
			} else {
				// Rethink target. Kill if necessary.
				rethinkTick ++;
				if (rethinkTick > RETHINK_MAX) {
					gidrah.remove();
				} else {
					gidrah.findTarget();
				}
			}
			return;
		}

		astar.findPath(GidrahGameMapTopology.pack(gidrah.getTileX(), gidrah.getTileY()), GidrahGameMapTopology.pack(target.getTileX(), target.getTileY()), path);
		thinking = true;
		//think();
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
		//assert targetTileX != 0 || targetTileY != 0;

		if (topology.canMove(gidrah.getTileX(), gidrah.getTileY(), targetTileX, targetTileY)) {
			// If this is a diagonal move, we take a bit longer over it, and ensure at least 1 route using HV movement only
			if (Math.abs(gidrah.getTileX() - targetTileX) + Math.abs(gidrah.getTileY() - targetTileY) > 1) {
				tick = (int) (getSpeed() * 1.42f);
			} else {
				tick = getSpeed();
			}
			tick *= topology.getSpeed(targetTileX, targetTileY);
			currentSpeed = tick;
			targetMapX = targetTileX * MapRenderer.TILE_SIZE + (feature.isGidlet() ? Util.random(0, MapRenderer.TILE_SIZE - 1) : 0);
			targetMapY = targetTileY * MapRenderer.TILE_SIZE + (feature.isGidlet() ? Util.random(0, MapRenderer.TILE_SIZE - 1) : 0);
			thinking = false;
			paused = false;

			if (!feature.isGidlet()) {
				// Erase occupation of current square
				map.clearOccupied(occupiedX, occupiedY);
				// and occupy the destination square
				map.setOccupied(occupiedX = targetTileX, occupiedY = targetTileY);
			}

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
//		long timeThen = Sys.getTime();
		for (int i = 0; i < MAX_THINK_TIME && ++totalThinkTime < MAX_TOTAL_THINK_TIME; i ++) {
//			System.out.println("Step "+i);
			switch (astar.nextStep()) {
				case AStar.SEARCH_STATE_SUCCEEDED:
					failCount = 0;
//					long timeNow = Sys.getTime();
					//System.out.println("Route found: "+((double)(timeNow - timeThen)) / Sys.getTimerResolution()+"s, path "+path.size()+", steps "+astar.getNumSteps());
					// Found the goal! Move one step closer.
//					System.out.println("Gidrah "+gidrah+" found goal in "+astar.getNumSteps()+" steps");
					startAndEnd = new PointPair();
					startAndEnd.a.setLocation(gidrah.getTileX(), gidrah.getTileY());
					startAndEnd.b.setLocation(gidrah.getTarget().getTileX(), gidrah.getTarget().getTileY());

					if (!next()) {
						chooseDestination();
					}
					// Remove from the queue if we're in there
					if (QUEUESET.contains(this)) {
						QUEUESET.remove(this);
						QUEUE.remove(this);
					}
					return;
				case AStar.SEARCH_STATE_FAILED:
					// Total failure. Wait a bit then think again.
//					System.out.println("Gidrah "+gidrah+" totally failed to find goal after "+astar.getNumSteps()+" steps");
					thinking = false;
					paused = true;
					tick = Util.random(10, 30);
					path.clear();
					// Remove from the queue if we're in there
					if (QUEUESET.contains(this)) {
						QUEUESET.remove(this);
						QUEUE.remove(this);
					}
					failCount ++;
					if (failCount > MAX_FAILS) {
						gidrah.onMovementFail();
					}
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
		// Does our path intersect the bounds?
		if (startAndEnd != null && startAndEnd.getBounds().intersects(bounds)) {
			int n = path.size();
			for (int i = 0; i < n; i ++) {
				int coord = path.get(i);
				int x = GidrahGameMapTopology.getX(coord);
				int y = GidrahGameMapTopology.getY(coord);
				if (bounds.contains(x, y)) {
					// Queue for a rethink
					queue(this);
					return;
				}
			}
		}
	}

	private static void queue(GidrahMovement gm) {
		if (QUEUESET.contains(gm)) {
			// Already queued
			return;
		}
		QUEUE.add(gm);
		QUEUESET.add(gm);
	}

	private static void processQueue() {
		if (QUEUE.size() == 0) {
			return;
		}

		// If the gidrah at the head of the queue is not thinking, start it thinking. When it finds a route
		// it'll remove itself from the queue.
		GidrahMovement gm = QUEUE.get(0);
		if (!gm.thinking) {
			gm.reset();
		}
	}


	public static void resetTotalThinkTime() {
		totalThinkTime = 0;
		// Also process queue
		processQueue();
	}

	public static void init() {
		QUEUE.clear();
		QUEUESET.clear();
	}
}
