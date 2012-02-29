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
package com.shavenpuppy.jglib.resources;

import java.io.Serializable;

import com.shavenpuppy.jglib.util.Parseable;
import com.shavenpuppy.jglib.util.Util;

/**
 * $Id: DieRoll.java,v 1.4 2011/10/01 00:33:45 cix_foo Exp $
 * Describes a random range of values in Dungeons and Dragons format (ed 3d6, 1d10+4)
 * @author $Author: cix_foo $
 * @version $Revision: 1.4 $
 */
public class DieRoll implements Serializable, Parseable {

	private static final long serialVersionUID = 1L;

	/** Number of dice to roll */
	private int num;

	/** Dice size */
	private int dice;

	/** Modifier */
	private int modifier;

	/**
	 * C'tor
	 */
	public DieRoll() {
	}

	/**
	 * C'tor
	 */
	public DieRoll(int num, int dice) {
		this(num, dice, 0);
	}

	/**
	 * C'tor
	 */
	public DieRoll(int num, int dice, int modifier) {
		this.num = num;
		this.dice = dice;
		this.modifier = modifier;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.util.Parseable#fromString(java.lang.String)
	 */
	@Override
	public void fromString(String src) throws Exception {
		int idx = src.indexOf('d');
		if (idx == -1) {
			num = 0;
			modifier = Integer.parseInt(src);
			return;
		}
		num = Integer.parseInt(src.substring(0, idx));
		int idx2 = src.indexOf('+');
		if (idx2 == -1) {
			idx2 = src.indexOf('-');
			if (idx2 == -1) {
				dice = Integer.parseInt(src.substring(idx + 1, src.length()));
				modifier = 0;
			} else {
				dice = Integer.parseInt(src.substring(idx + 1, idx2));
				modifier = - Integer.parseInt(src.substring(idx2 + 1, src.length()));
			}
		} else {
			dice = Integer.parseInt(src.substring(idx + 1, idx2));
			modifier = Integer.parseInt(src.substring(idx2 + 1, src.length()));
		}
	}

	/**
	 * Get a random value from this range
	 * @return int
	 */
	public int getValue() {
		int total = modifier;
		for (int i = 0; i < num; i ++) {
			total += Util.random(1, dice);
		}
		return total;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (num == 0) {
			return String.valueOf(modifier);
		} if (modifier == 0) {
			return num+"d"+dice;
		} else {
			return num+"d"+dice+""+modifier;
		}
	}
}
