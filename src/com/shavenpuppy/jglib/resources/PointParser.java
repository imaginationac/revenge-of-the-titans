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

import java.util.StringTokenizer;

import org.lwjgl.util.Point;

import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Parses a String representation of a Point
 *
 * @author foo
 */
public final class PointParser {

	/**
	 * No constructor
	 */
	private PointParser() {
		super();
	}

	/**
	 * Parse the incoming String, which will be of the form x,y
	 * @param in The String to parse
	 * @return the parsed Point
	 * @throws Exception if the Point cannot be parsed
	 */
	public static Point parse(String in) throws Exception {
		Point ret = new Point();

		StringTokenizer st = new StringTokenizer(in, ", \t", false);
		if (st.hasMoreTokens()) {
			ret.setX((int) Float.parseFloat(XMLUtil.parse(st.nextToken())));
		} else {
			throw new Exception("Missing x value.");
		}
		if (st.hasMoreTokens()) {
			ret.setY((int) Float.parseFloat(XMLUtil.parse(st.nextToken())));
		} else {
			throw new Exception("Missing y value.");
		}

		return ret;
	}

}
