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

package net.puppygames.applet;

import net.puppygames.gamecommerce.shared.ConfigurationDetails;

import com.shavenpuppy.jglib.util.Util;

/**
 * $Id: Configuration.java,v 1.1 2009/07/01 15:38:53 foo Exp $
 *
 * Configures the game on first install to provide us with a random distribution
 * of nag tactics.
 *
 * @author $Author: foo $
 * @version $Revision: 1.1 $
 */
public class Configuration implements ConfigurationDetails {

	private static final long serialVersionUID = 2L;

	/*
	 * Choices
	 */
	private static final int[] MAX_GAMES = {0, 10, 15, 20, 25};
	private static final int[] MAX_TIME = {0, 800, 1600, 2400, 3000};
	private static final int[] MAX_LEVEL = {0, 10, 15, 20, 25};

	/** Max games that can be played (0=unlimited) */
	private int maxGames = -1;

	/** Max time that the demo can be played for, in seconds (0=unlimited)*/
	private int maxTime = -1;

	/** Max level */
	private int maxLevel = -1;

	/** Crippled demo? */
	private int crippled = -1;

	/** Start fullscreen? */
	private int fullscreen = -1;

	/**
	 * C'tor
	 */
	public Configuration() {
	}

	/**
	 * Encodes the configuration into a hex string
	 * @return String, which will be an 8-digit hex string
	 * @throws IllegalStateException if any value is -1 still
	 */
	@Override
	public String encode() {
		StringBuilder sb = new StringBuilder(5);
		if (maxGames == -1) {
			throw new IllegalStateException("maxGames not set");
		}
		if (maxTime == -1) {
			throw new IllegalStateException("maxTime not set");
		}
		if (maxGames < 16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(maxGames));

		if (maxTime < 1600) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(maxTime / 100));
		sb.append(Integer.toHexString(crippled));
		if (maxLevel < 16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(maxLevel));
		sb.append(Integer.toHexString(fullscreen));
		return sb.toString();
	}

	/**
	 * Decodes the configuration from a hex string
	 * @param encoded
	 * @throws Exception
	 */
	@Override
	public ConfigurationDetails decode(String encoded) throws Exception {
		maxGames = Integer.parseInt(encoded.substring(0, 2), 16);
		maxTime = Integer.parseInt(encoded.substring(2, 4), 16) * 100;
		if (encoded.length() > 4) {
			crippled = Integer.parseInt(encoded.substring(4, 5), 16);
			if (encoded.length() > 5) {
				maxLevel = Integer.parseInt(encoded.substring(5, 7), 16);
				if (encoded.length() > 7) {
					fullscreen = Integer.parseInt(encoded.substring(7, 8), 16);
				} else {
					fullscreen = Util.random(0, 1);
				}
			} else {
				maxLevel = MAX_LEVEL[1];
			}
		} else {
			crippled = 1;
		}
		if (maxGames == 0 && maxTime == 0) {
			if (Util.random(0, 1) == 0) {
				maxGames = MAX_GAMES[1];
			} else {
				maxTime = MAX_TIME[1];
			}
		}
		return this;
	}

	/**
	 * Init the configuration. Any value which is -1 is randomized.
	 */
	public void init() {
		Util.setSeed(System.currentTimeMillis());

		if (maxGames == -1) {
			maxGames = MAX_GAMES[Util.random(0, MAX_GAMES.length - 1)];
		}
		if (maxTime == -1) {
			maxTime = MAX_TIME[Util.random(0, MAX_TIME.length - 1)];
		}
		if (maxGames == 0 && maxTime == 0) {
			if (Util.random(0, 1) == 0) {
				maxGames = MAX_GAMES[1];
			} else {
				maxTime = MAX_TIME[1];
			}
		}
		if (crippled == -1) {
			crippled = Util.random(0, 1);
		}
		if (fullscreen == -1) {
			fullscreen = Util.random(0, 1);
		}
		if (maxLevel == -1) {
			maxLevel = MAX_LEVEL[Util.random(0, MAX_LEVEL.length - 1)];
		}
	}

	/**
	 * @return Returns the maxGames.
	 */
	public int getMaxGames() {
		return maxGames;
	}
	/**
	 * @return Returns the maxTime.
	 */
	public int getMaxTime() {
		return maxTime;
	}
	/**
	 * Is the game cripped?
	 * @return boolean
	 */
	public boolean isCrippled() {
		return crippled == 1;
	}
	/**
	 * Get the max. level of the game. 0 = no max
	 * @return int
	 */
	public int getMaxLevel() {
		return maxLevel;
	}
	/**
	 * @return
	 */
	public boolean isFullscreen() {
		return fullscreen == 1;
	}
}
