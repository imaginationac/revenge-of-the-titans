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

import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;

/**
 * Describes game configuration parameters
 */
public class GameConfiguration extends Feature {

	private static final long serialVersionUID = 1L;

	/** Singleton */
	private static GameConfiguration instance;

	/** Title */
	@Data
	private String title;

	/** Initial interval at which a saucer spawns, ticks */
	private int saucerInterval; // Saucer every 20 seconds

	/** Saucer interval adjust */
	private int nextSaucerInterval; // .. +1 second each time

	/** Delay factor before aliens appear */
	private float initialLevelDelayFactor; // Multiplied by map size to get delay in ticks.

	/** Basic delay before aliens appear, in ticks */
	private int longDelay;

	/** Staggers spawnpoint starting times */
	private int spawnpointDelayAdjust;

	/** Delay between individual gids spawning at a spawnpoint */
	private int spawnDelay;

	/** Ignore buildings in difficulty calculation until at least this much value has been spent */
	private int builtBuildingsValueThreshold;

	/** Factor by which to multiply cash-in-the-bank vs. cash-in-buildings for calculating difficulty */
	private float bankFactor;

	/** Base difficulty factor in $ */
	private float difficultyFactor;

	/** Crystal agitation factor, for Survival, Sandbox, and Endless modes */
	private float crystalAgitationFactor;

	/** Adds to the base difficulty factor each level */
	private float difficultyFactorPerLevel;

	/** Amount by which Endless mode difficulty increases past 50 levels */
	private float endlessDifficultyCreep;

	/** Every time user makes an easier level, difficulty is adjusted by this amount */
	private float difficultyAdjustmentFactor;

	/** Adjust difficulty by this much when base is in the centre of the map, times the level number */
	private float centralDifficultyAdjustPerLevel;

	/** New game initial money */
	private int normalInitialMoney;

	/** Crystal scavenging rates */
	private float[] scavengeRate;

	/** Survival mode difficulty factors (1 for each world) */
	private float[] survivalModeDifficultyFactors;

	/** Repair cost % */
	private float repairCost;

	private int survivalWaveLengthTimeAdjust; // Every minute, survival wave lengths increase
	private int survivalWaveLengthTimeOffset; // .. only after 3 minutes have elapsed though
	private int survivalWaveLength; // The basic size of a survival gidrah spawn wave
	private int survivalSpawnpointSpawnInterval; // Every 90 seconds, spawn a new Survival spawnpoint...
	private int survivalSpawnpointSpawnIntervalAdjust; // ...then lengthen time till next interval by 90 seconds
	private int baseMaxSurvivalSpawnpoints; // No more than this many spawnpoints please
	private int survivalSpawnpointsPerMapSize; // Divide map size by this value and add to BASE_MAX_SURVIVAL_SPAWNPOINTS
	private int[] survivalGidrahUnlockInterval; // Every n kills, unlock a new gidrah in Survival
	private int[] survivalGidrahUnlockIntervalAdjust; // ...then lengthen time till next interval by this many kills
	private int survivalCrystalInterval; // Every 60 seconds, spawn a crystal
	private int survivalCrystalIntervalAdjust; // ...plus 2 seconds each time
	private int survivalBossInterval; // Boss every 5 minutes
	private int survivalBossIntervalAdjust; // ...plus 3 minutes each time
	private float survivalDifficultyAdjuster; // adds to difficulty factor every time we can't unlock a gidrah
	private float[] difficultyAttempts; // Reduce difficulty after repeated attempts
	private int[] survivalInitialMoney;
	private int xmasCrystalInterval; // Every 60 seconds, spawn a crystal
	private int xmasCrystalIntervalAdjust; // ...plus 2 seconds each time
	private int xmasInitialMoney;
	private float xmasDifficultyFactor;

	/**
	 * C'tor
	 * @param name
	 */
	public GameConfiguration(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	protected void doRegister() {
		instance = this;

		System.out.println("Game configuration now set to "+title);
	}

	@Override
	protected void doDeregister() {
		instance = null;
	}

	/**
	 * Gets the game configuration
	 * @return a GameConfiguration (should not be null)
	 */
	public static GameConfiguration getInstance() {
		return instance;
	}

	public int getSaucerInterval() {
    	return saucerInterval;
    }

	public int getNextSaucerInterval() {
    	return nextSaucerInterval;
    }

	public float getInitialLevelDelayFactor() {
    	return initialLevelDelayFactor;
    }

	public int getLongDelay() {
    	return longDelay;
    }

	public int getSpawnpointDelayAdjust() {
    	return spawnpointDelayAdjust;
    }

	public int getSpawnDelay() {
    	return spawnDelay;
    }

	public int getBuiltBuildingsValueThreshold() {
    	return builtBuildingsValueThreshold;
    }

	public float getBankFactor() {
    	return bankFactor;
    }

	public float getDifficultyFactor() {
    	return difficultyFactor;
    }

	public float getDifficultyFactorPerLevel() {
    	return difficultyFactorPerLevel;
    }

	public float getEndlessDifficultyCreep() {
    	return endlessDifficultyCreep;
    }

	public float getDifficultyAdjustmentFactor() {
    	return difficultyAdjustmentFactor;
    }

	public int getNormalInitialMoney() {
    	return normalInitialMoney;
    }

	public float[] getSurvivalModeDifficultyFactors() {
    	return survivalModeDifficultyFactors;
    }

	public int getSurvivalWaveLengthTimeAdjust() {
    	return survivalWaveLengthTimeAdjust;
    }

	public int getSurvivalWaveLengthTimeOffset() {
    	return survivalWaveLengthTimeOffset;
    }

	public int getSurvivalWaveLength() {
    	return survivalWaveLength;
    }

	public int getSurvivalSpawnpointSpawnInterval() {
    	return survivalSpawnpointSpawnInterval;
    }

	public int getSurvivalSpawnpointSpawnIntervalAdjust() {
    	return survivalSpawnpointSpawnIntervalAdjust;
    }

	public int getBaseMaxSurvivalSpawnpoints() {
    	return baseMaxSurvivalSpawnpoints;
    }

	public int getSurvivalSpawnpointsPerMapSize() {
    	return survivalSpawnpointsPerMapSize;
    }

	public int[] getSurvivalGidrahUnlockInterval() {
    	return survivalGidrahUnlockInterval;
    }

	public int[] getSurvivalGidrahUnlockIntervalAdjust() {
    	return survivalGidrahUnlockIntervalAdjust;
    }

	public int getSurvivalCrystalInterval() {
    	return survivalCrystalInterval;
    }

	public int getSurvivalCrystalIntervalAdjust() {
    	return survivalCrystalIntervalAdjust;
    }

	public int getXmasCrystalInterval() {
    	return xmasCrystalInterval;
    }

	public int getXmasCrystalIntervalAdjust() {
    	return xmasCrystalIntervalAdjust;
    }

	public int getSurvivalBossInterval() {
    	return survivalBossInterval;
    }

	public int getSurvivalBossIntervalAdjust() {
    	return survivalBossIntervalAdjust;
    }

	public float getSurvivalDifficultyAdjuster() {
    	return survivalDifficultyAdjuster;
    }

	public float[] getDifficultyAttempts() {
    	return difficultyAttempts;
    }

	public int[] getSurvivalInitialMoney() {
    	return survivalInitialMoney;
    }

	public int getXmasInitialMoney() {
    	return xmasInitialMoney;
    }

	public float getXmasDifficultyFactor() {
    	return xmasDifficultyFactor;
    }

	public float getCentralDifficultyAdjustPerLevel() {
	    return centralDifficultyAdjustPerLevel;
    }

	public float[] getScavengeRate() {
	    return scavengeRate;
    }

	public float getRepairCost() {
	    return repairCost;
    }

	public float getCrystalAgitationFactor() {
	    return crystalAgitationFactor;
    }
}
