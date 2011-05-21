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
package com.shavenpuppy.jglib.util;

import java.util.Comparator;

import org.lwjgl.opengl.DisplayMode;

/**
 * Comparator that orders LWJGL display modes.
 *
 * Highest frequency modes are given higher priority, if frequency is identical
 * then we prefer modes with the higher amount of colour precision.
 *
 * Width and height of display modes are ignored (the idea being that you've
 * already chosen and filtered on a resolution and now want the best mode for
 * that size).
 *
 * @author John Campbell
 */
public class SimpleDisplayModeComparator implements Comparator<DisplayMode> {

	public static final SimpleDisplayModeComparator instance = new SimpleDisplayModeComparator();

	private SimpleDisplayModeComparator() {
	}

	@Override
	public int compare(DisplayMode dm1, DisplayMode dm2) {
		if (dm1.getFrequency() == dm2.getFrequency()) {
			// Sort by bpp if freq identical
			if (dm1.getBitsPerPixel() == dm2.getBitsPerPixel()) {
				return 0;
			} else if (dm1.getBitsPerPixel() > dm2.getBitsPerPixel()) {
				return -1;
			} else {
				return 1;
			}

		} else if (dm1.getFrequency() > dm2.getFrequency()) {
			return -1;
		} else {
			return 1;
		}
	}
}
