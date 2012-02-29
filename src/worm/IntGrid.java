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
import java.util.Arrays;

/**
 * A simple integer grid
 */
public class IntGrid implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int width, height, fill;
	private final int[] value;

	/**
	 * C'tor
	 * @param width
	 * @param height
	 * @parma fill The default value
	 */
	public IntGrid(int width, int height, int fill) {
		this.width = width;
		this.height = height;
		this.fill = fill;
		value = new int[width * height];
		Arrays.fill(value, fill);
	}

	public void clear() {
		Arrays.fill(value, fill);
	}

	private int getIndex(int x, int y) {
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return -1;
		}
		return x + y * width;
	}

	public int getValue(int x, int y) {
		if (x < 0) {
			x = 0;
		} else if (x >= width) {
			x = width - 1;
		}
		if (y < 0) {
			y = 0;
		} else if (y >= height) {
			y = height - 1;
		}
		int idx = getIndex(x, y);
		if (idx == -1) {
			return fill;
		}
		return value[idx];
	}

	public void setValue(int x, int y, int newValue) {
		int idx = getIndex(x, y);
		if (idx == -1) {
			return;
		}
		value[idx] = newValue;
	}

	/**
	 * @return the width
	 */
	public final int getWidth() {
		return width;
	}

	/**
	 * @return the height
	 */
	public final int getHeight() {
		return height;
	}

}
