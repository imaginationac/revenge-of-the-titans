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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.MapRenderer;
import worm.Worm;
import worm.WormGameState;
import worm.features.GidrahFeature;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.interpolators.OpenLinearInterpolator;

/**
 * For moving flying gidrahs
 */
class FlyingMovement implements Movement {

	private static final long serialVersionUID = 1L;

	private static final Rectangle TEMP = new Rectangle();
	private static final ArrayList<Entity> COLLISIONS = new ArrayList<Entity>();
	private static final int MAX_RAMP_UP_DURATION = 7200;
	private static final int RAMP_UP_DURATION = 3600;
	private static final int RAMP_UP_PER_LEVEL = 60;
	private static final float MARGIN = 64.0f;
	private static final int BOMB_CHECK = 16;
	private static final int DROP_TIME = 16;
	private static final float BOSS_DROP_ATTACK_RANGE = 4.0f;

	final Set<Building> bombed = new HashSet<Building>();

	final Gidrah gidrah;
	final float ox, oy;

	int rampTick;
	Building target;
	Building bombTarget;
	double tx, ty, angle;
	int bombTick, dropTick;
	boolean dropping;

	/**
	 * C'tor
	 * @param gidrah
	 */
	FlyingMovement(Gidrah gidrah) {
		this.gidrah = gidrah;
		this.ox = gidrah.getMapX();
		this.oy = gidrah.getMapY();
	}

	@Override
	public void remove() {
	}

	@Override
	public void reset() {
	}

	private void drop() {
		dropping = true;
		gidrah.initAttack(bombTarget);
	}

	/**
	 * We're dropping onto our target to squish it
	 */
	private void doDrop() {
		dropTick ++;

	}

	@Override
	public void tick() {
		if (target == null) {
			gidrah.findTarget();
			target = (Building) gidrah.getTarget();
			if (target == null) {
				return;
			}
			tx = target.getMapX() + target.getCollisionX();
			ty = target.getMapY() + target.getCollisionY();
			double gx = gidrah.getMapX() + MapRenderer.TILE_SIZE * 0.5;
			double gy = gidrah.getMapY() + MapRenderer.TILE_SIZE * 0.5;
			double dx = tx - gx;
			double dy = ty - gy;
			angle = Math.atan2(dy, dx);
		}

		if (!Worm.getGameState().isLevelActive()) {
			rampTick ++;
		}
		double mx = Math.cos(angle) * getSpeed() * 0.016666666666666666666666666666667;
		double my = Math.sin(angle) * getSpeed() * 0.016666666666666666666666666666667;
		gidrah.setMirrored(mx < 0.0);

		float newX = (float) (gidrah.getMapX() + mx);
		float newY = (float) (gidrah.getMapY() + my);
		if (dropping) {
			doDrop();
		} else if 	(
				newX > Worm.getGameState().getMap().getWidth() * MapRenderer.TILE_SIZE + MARGIN
			||	newY > Worm.getGameState().getMap().getHeight() * MapRenderer.TILE_SIZE + MARGIN
			||	newX < -MARGIN
			||	newY < -MARGIN
			)
		{
			// Go back to the start and retarget
			newX = ox;
			newY = oy;
			bombed.clear();
			gidrah.spawnBomb();
			bombTarget = null;
			target = null;
		} else {
			if (bombTarget != null) {
				bombTick ++;
				if (bombTick >= DROP_TIME) {
					bombTick = 0;
					// Drop a bomb! Or jump onto target.
					if (gidrah.getFeature().isDropAttack()) {
						drop();
					} else {
						gidrah.dropBomb((float) mx, (float) my);
						bombTarget = null;
					}
				}
			} else if (gidrah.getFeature().isDropAttack() && gidrah.getFeature().isBoss()) {
				// Drop attack boss only drops onto its one target - the base
				if (target.getDistanceTo(gidrah.getMapX() + MapRenderer.TILE_SIZE / 2, gidrah.getMapY() + MapRenderer.TILE_SIZE / 2) < BOSS_DROP_ATTACK_RANGE) {
					bombTarget = target;
					bombTick = 0;
				}
			} else {
				// Are we over a building?
				bombTick ++;
				TEMP.setBounds((int) newX + MapRenderer.TILE_SIZE / 2, (int) newY + MapRenderer.TILE_SIZE / 2, MapRenderer.TILE_SIZE / 2, MapRenderer.TILE_SIZE / 2);
				Entity.getCollisions(TEMP, COLLISIONS);
				if (bombed.size() > 0) {
					for (Iterator<Building> i = bombed.iterator(); i.hasNext(); ) {
						Building b = i.next();
						if (!b.isActive()) {
							i.remove();
						}
					}
					if (bombed.size() > 0 && COLLISIONS.size() > 0) {
						COLLISIONS.removeAll(bombed);
					}
				}
				int n = COLLISIONS.size();
				for (int i = 0; i < n; i ++) {
					Entity entity = COLLISIONS.get(i);
					if (entity == gidrah || !entity.isActive() || !entity.canCollide()) {
						return;
					}
					if (entity instanceof Building) {
						Building building = (Building) entity;
						if (building.isWorthAttacking() && !building.isBarricade() && !building.isMineField()) {
							bombTarget = building;
							bombTick = 0;
							bombed.add(building);
						}
					}
				}
			}
		}
		gidrah.setLocation(newX, newY);

	}

	float getSpeed() {
		WormGameState gameState = Worm.getGameState();
		GidrahFeature feature = gidrah.getFeature();
		float minSpeed = feature.getSpeed();
		float maxSpeed = feature.getSpeed() * MAX_SPEED_MULTIPLIER;
		float ret;
		ret = OpenLinearInterpolator.instance.interpolate(minSpeed, maxSpeed, gameState.getDifficulty());
		if (!gameState.isLevelActive()) {
			ret = LinearInterpolator.instance.interpolate
				(
					ret,
					Math.max(120.0f, ret * LinearInterpolator.instance.interpolate(1.0f, 1.5f, gameState.getGidrahDeathRatio())),
					(float) rampTick / (float) Math.min(MAX_RAMP_UP_DURATION, RAMP_UP_DURATION + gameState.getLevel() * RAMP_UP_PER_LEVEL) // ramp up duration takes longer and longer
				);
		}
		if (dropping) {
			// Tend to zero as we drop
			ret = LinearInterpolator.instance.interpolate(ret, 0.0f, dropTick / gidrah.getFeature().getHeight());
		}
		return ret;
	}

	@Override
	public void maybeRethink(Rectangle bounds) {
	}

	@Override
	public void adjust(float newX, float newY) {
	}

	@Override
	public boolean isMoving() {
		return true;
	}

	@Override
	public void attack() {
		// No need to do anything
	}

	@Override
	public void dontAttack() {
		// Drop the bomb!
		double mx = Math.cos(angle) * getSpeed() * 0.016666666666666666666666666666667;
		double my = Math.sin(angle) * getSpeed() * 0.016666666666666666666666666666667;
		gidrah.dropBomb((float) mx, (float) my);
	}
}
