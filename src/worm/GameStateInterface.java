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

import worm.buildings.BuildingFeature;
import worm.entities.Building;
import worm.entities.Gidrah;
import worm.entities.Saucer;
import worm.entities.Unit;

/**
 * Interface to game state.
 * @author Cas
 */
public interface GameStateInterface extends Serializable {

	int getAvailableStock(BuildingFeature bf);
	void addAvailableStock(BuildingFeature bf, int n);

	void addToGidrahs(Gidrah gidrah);
	void addToUnits(Unit unit);
	void addToBuildings(Building building);
	void addToSaucers(Saucer saucer);

	void removeFromGidrahs(Gidrah gidrah);
	void removeFromUnits(Unit unit);
	void removeFromBuildings(Building building);
	void removeFromSaucers(Saucer saucer);

	/**
	 * Buff shield generator ability by 1 for the duration of the level
	 */
	void buffShieldGenerators();

	/**
	 * Buff scanner ability by 1 for the duration of the level
	 */
	void buffScanners();

	/**
	 * Buff reactor ability by 1 for the duration of the level
	 */
	void buffReactors();

	/**
	 * Buff capacitor ability by 1 for the duration of the level
	 */
	void buffCapacitors();

	/**
	 * Buff battery ability by 1 for the duration of the level
	 */
	void buffBatteries();

	/**
	 * Buff cooling tower ability by 1 for the duration of the level
	 */
	void buffCoolingTowers();

	/**
	 * Sets smart bomb mode. The next click fires a smartbomb from that location.
	 */
	void setSmartbombMode();

	/**
	 * Repair all buildings fully.
	 */
	void repairFully();

	/**
	 * Freeze the gidrahs for some time
	 * @param duration
	 */
	void freeze(int duration);

	/**
	 * Activate bezerk mode
	 * @param duration
	 */
	void bezerk(int duration);

	/**
	 * Activate shields
	 * @param duration
	 */
	void invulnerable(int duration);

	/**
	 * Tweak money
	 * @param delta
	 */
	void addMoney(int delta);

}
