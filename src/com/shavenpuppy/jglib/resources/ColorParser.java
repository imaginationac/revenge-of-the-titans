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

import java.text.ParseException;
import java.util.StringTokenizer;

import org.lwjgl.util.Color;

/**
 * Parses a String representation of a Color
 *
 * @author foo
 */
public abstract class ColorParser {

	/**
	 * No constructor
	 */
	private ColorParser() {
		super();
	}

	/**
	 * Parse the incoming String, which will be of the form r,g,b,a, where
	 * r, b, g, and a are integers ranging from 0..255; or if the string begins
	 * ! then instead it will be H,S,V,A
	 * @param in The String to parse
	 * @return the parsed Color
	 * @throws ParseException if the Color cannot be parsed
	 */
	public static Color parse(String in) throws ParseException {
		Color ret = new Color();

		try {
			StringTokenizer st;
			if (in.startsWith("!")) {
				st = new StringTokenizer(in.substring(1), ", \t", false);
				int h, s, b;
				if (st.hasMoreTokens()) {
					h = Integer.parseInt(st.nextToken());
				} else {
					throw new ParseException("Missing hue value.", 0);
				}
				if (st.hasMoreTokens()) {
					s = Integer.parseInt(st.nextToken());
				} else {
					throw new ParseException("Missing saturation value.", 0);
				}
				if (st.hasMoreTokens()) {
					b = Integer.parseInt(st.nextToken());
				} else {
					throw new ParseException("Missing brightness value.", 0);
				}
				ret.fromHSB(h / 255.0f, s / 255.0f, b / 255.0f);
			} else {
				st = new StringTokenizer(in, ", \t", false);
				if (st.hasMoreTokens()) {
					ret.setRed(Integer.parseInt(st.nextToken()));
				} else {
					throw new ParseException("Missing red value.", 0);
				}
				if (st.hasMoreTokens()) {
					ret.setGreen(Integer.parseInt(st.nextToken()));
				} else {
					throw new ParseException("Missing green value.", 0);
				}
				if (st.hasMoreTokens()) {
					ret.setBlue(Integer.parseInt(st.nextToken()));
				} else {
					throw new ParseException("Missing blue value.", 0);
				}
			}
			if (st.hasMoreTokens()) {
				ret.setAlpha(Integer.parseInt(st.nextToken()));
			} else {
				ret.setAlpha(255);
			}

			return ret;
		} catch (NumberFormatException e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}

}
