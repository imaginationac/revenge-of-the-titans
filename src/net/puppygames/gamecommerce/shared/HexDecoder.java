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
package net.puppygames.gamecommerce.shared;

/**
 * A utility class to decode hexadecimal numbers from strings into byte arrays,
 * such as those produced by HexEncoder.
 */
public abstract class HexDecoder {

	/** No construction */
	private HexDecoder() {}

	/**
	 * Decode a string of hex into a byte array. The string must of course be
	 * even in length as each pair of digits makes one byte.
	 * @param in The input String
	 * @return byte[], or null, if in is null
	 * @throws IllegalArgumentException if the String is not even-length
	 * @throws NumberFormatException if the String does not contain only hex digits
	 */
	public static byte[] decode(String in) throws IllegalArgumentException, NumberFormatException {
		if (in == null) {
	        return null;
        }
		if ((in.length() & 1) == 1) {
	        throw new IllegalArgumentException("Input string must be an even length.");
        }
		byte[] ret = new byte[in.length() >> 1];
		for (int i = 0; i < ret.length; i ++) {
			ret[i] = (byte) Integer.parseInt(in.substring(i * 2, i * 2 + 2), 16);
		}
		return ret;
	}
}
