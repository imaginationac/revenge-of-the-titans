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
package com.shavenpuppy.jglib.openal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.OpenALException;

import com.shavenpuppy.jglib.MultiBuffer;
import com.shavenpuppy.jglib.Wave;
/**
 * OpenAL, with handy supporting code.
 *
 * @author foo
 */
public class AL {

	/** A map of constant names to values */
	private static final Map<String, Integer> AL_CONSTANTS_MAP = new HashMap<String, Integer>(513, 1.0f);
	private static boolean constantsLoaded;
	static {
		loadALConstants();
	}

	/** A handy bit of scratch memory */
	public static final MultiBuffer scratch = new MultiBuffer(1024);

	/**
	 * Decode an al string constant
	 */
	public static int decode(String alstring) {
		Integer i = AL_CONSTANTS_MAP.get(alstring.toUpperCase());
		if (i == null) {
			throw new OpenALException(alstring + " is not a recognised AL constant");
		} else {
			return i.intValue();
		}
	}

	/**
	 * Recode an al constant back into a string
	 */
	public static String recode(int code) {
		for (String s : AL_CONSTANTS_MAP.keySet()) {
			Integer n = AL_CONSTANTS_MAP.get(s);
			if (n.intValue() == code) {
				return s;
			}
		}
		throw new OpenALException(code + " is not a known AL code");
	}

	/**
	 * Reads all the constant enumerations from this class and stores them
	 * so we can decode them from strings.
	 * @see #decode()
	 * @see #recode()
	 */
	private static void loadALConstants() {
		if (constantsLoaded) {
			return;
		}

		Class<AL10> intf = AL10.class;
		Field[] field = intf.getFields();
		for (int i = 0; i < field.length; i++) {
			try {
				if (Modifier.isStatic(field[i].getModifiers())
					&& Modifier.isPublic(field[i].getModifiers())
					&& Modifier.isFinal(field[i].getModifiers())
					&& field[i].getType().equals(int.class)) {
					AL_CONSTANTS_MAP.put(field[i].getName(), new Integer(field[i].getInt(null)));
				}
			} catch (Exception e) {
			}
		}

		constantsLoaded = true;
	}

	public static int translateFormat(int waveType) {
		int format;
		switch (waveType) {
			case Wave.MONO_8BIT:
				format = AL10.AL_FORMAT_MONO8;
				break;
			case Wave.MONO_16BIT:
				format = AL10.AL_FORMAT_MONO16;
				break;
			case Wave.STEREO_8BIT:
				format = AL10.AL_FORMAT_STEREO8;
				break;
			case Wave.STEREO_16BIT:
				format = AL10.AL_FORMAT_STEREO16;
				break;
			default:
				throw new OpenALException("Unknown wave format.");
		}
		return format;
	}
}
