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

import net.puppygames.applet.PrizeFeature;
import worm.Worm;
import worm.WormGameState;
import worm.powerups.PowerupFeature;

import com.shavenpuppy.jglib.Resources;

/**
 * Give the player some powerups
 */
public class MultiPrizeFeature extends PrizeFeature {

	private static final long serialVersionUID = 1L;

	/**
	 * C'tor
	 * @param name
	 */
	public MultiPrizeFeature(String name) {
		super(name);
	}

	@Override
	public boolean isValid() {
		int maxLevel = Worm.getMaxLevel(WormGameState.GAME_MODE_CAMPAIGN);
		return maxLevel >= 5 && maxLevel < WormGameState.MAX_LEVELS;
	}

	@Override
	public void redeem() {
		int maxLevel = Worm.getMaxLevel(WormGameState.GAME_MODE_CAMPAIGN);
		if (maxLevel < WormGameState.MAX_LEVELS) {
			WormGameState.MetaState ms;
			try {
				ms = WormGameState.MetaState.load(maxLevel, WormGameState.GAME_MODE_CAMPAIGN);
				ms.bonusPowerup((PowerupFeature) Resources.get("bezerk.powerup"), 1);
				ms.bonusPowerup((PowerupFeature) Resources.get("shield.powerup"), 1);
				ms.bonusPowerup((PowerupFeature) Resources.get("smartbomb.powerup"), 1);
				ms.bonusPowerup((PowerupFeature) Resources.get("freeze.powerup"), 1);
				ms.bonusPowerup((PowerupFeature) Resources.get("repair.powerup"), 1);
				ms.save();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

}
