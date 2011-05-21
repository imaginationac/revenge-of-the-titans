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

import java.lang.reflect.Array;
import java.util.LinkedList;

public class ArrayParser {

	private static void validate(String in) throws Exception {
		if (in.charAt(0) != '{') {
			throw new Exception("Array must start with {");
		}
		if (in.charAt(in.length() - 1) != '}') {
			throw new Exception("Array must end with }");
		}
	}

	@SuppressWarnings("unchecked")
    public static <T> T[] parse(Parser<T> parser, Class<T> arrayType, String in) throws Exception {
		validate(in);
		LinkedList<T> values = new LinkedList<T>();
		MoreImprovedStringTokenizer st = new MoreImprovedStringTokenizer(in.substring(1, in.length() - 1), ",");
		while (st.hasMoreTokens()) {
			T t = parser.parse(st.nextToken());
			values.add(t);
		}

		Object ret = Array.newInstance(arrayType, values.size());
		return values.toArray((T[]) ret);
	}

    public static int[] parseInts(String in) throws Exception {
		validate(in);
		LinkedList<Integer> values = new LinkedList<Integer>();
		MoreImprovedStringTokenizer st = new MoreImprovedStringTokenizer(in.substring(1, in.length() - 1), ",");
		IntParser ip = new IntParser();
		while (st.hasMoreTokens()) {
			Integer t = ip.parse(st.nextToken().trim());
			values.add(t);
		}

		int[] ret = (int[]) Array.newInstance(int.class, values.size());
		for (int i = 0; i < ret.length; i ++) {
			ret[i] = values.get(i).intValue();
		}
		return ret;
	}

    public static float[] parseFloats(String in) throws Exception {
		validate(in);
		LinkedList<Float> values = new LinkedList<Float>();
		MoreImprovedStringTokenizer st = new MoreImprovedStringTokenizer(in.substring(1, in.length() - 1), ",");
		FloatParser ip = new FloatParser();
		while (st.hasMoreTokens()) {
			Float t = ip.parse(st.nextToken().trim());
			values.add(t);
		}

		float[] ret = (float[]) Array.newInstance(float.class, values.size());
		for (int i = 0; i < ret.length; i ++) {
			ret[i] = values.get(i).floatValue();
		}
		return ret;
	}

    public static double[] parseDoubles(String in) throws Exception {
		validate(in);
		LinkedList<Double> values = new LinkedList<Double>();
		MoreImprovedStringTokenizer st = new MoreImprovedStringTokenizer(in.substring(1, in.length() - 1), ",");
		DoubleParser ip = new DoubleParser();
		while (st.hasMoreTokens()) {
			Double t = ip.parse(st.nextToken().trim());
			values.add(t);
		}

		double[] ret = (double[]) Array.newInstance(double.class, values.size());
		for (int i = 0; i < ret.length; i ++) {
			ret[i] = values.get(i).doubleValue();
		}
		return ret;
	}

}
