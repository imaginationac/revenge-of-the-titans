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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Even more improved string tokenizer, with delimiters
 */
public class MoreImprovedStringTokenizer implements Iterator<String> {
	private final String input;
	private final String delimiter;

	private int index;

	private final StringBuilder token;

	/**
	 * ImprovedStringTokenizer constructor comment.
	 */
	public MoreImprovedStringTokenizer(String input, String delimiter) {
		this.input = input;
		this.delimiter = delimiter;

		token = new StringBuilder(input.length());
	}

	/**
	 * Returns <tt>true</tt> if the iteration has more elements. (In other words, returns <tt>true</tt> if <tt>next</tt> would
	 * return an element rather than throwing an exception.)
	 *
	 * @return <tt>true</tt> if the iterator has more elements.
	 */
	public boolean hasMoreTokens() {
		return index < input.length();
	}

	/**
	 * Returns <tt>true</tt> if the iteration has more elements. (In other words, returns <tt>true</tt> if <tt>next</tt> would
	 * return an element rather than throwing an exception.)
	 *
	 * @return <tt>true</tt> if the iterator has more elements.
	 */
	@Override
	public boolean hasNext() {
		return hasMoreTokens();
	}

	/**
	 * Returns the next element in the interation.
	 *
	 * @returns the next element in the interation.
	 * @exception NoSuchElementException iteration has no more elements.
	 */
	@Override
	public String next() {
		return nextToken();
	}

	/**
	 * Returns the next token.
	 */
	public String nextToken() {
		if (!hasMoreTokens()) {
			throw new NoSuchElementException();
		}

		// Add characters until we come to more whitespace
		char c;
		boolean inquotes = false;
		token.setLength(0);

		while (index < input.length()) {
			c = input.charAt(index ++);
			if (inquotes) {
				if (c == '"') {
					inquotes = false;
				} else {
					token.append(c);
				}
			} else {
				if (c == '"') {
					inquotes = true;
				} else if (delimiter.indexOf(c) != -1) {
					// Hit a delimiter
					return token.toString();
				} else {
					token.append(c);
				}
			}
		}
		return token.toString();
	}

	/**
	 * Removes from the underlying collection the last element returned by the iterator (optional operation). This method can be
	 * called only once per call to <tt>next</tt>. The behavior of an iterator is unspecified if the underlying collection is
	 * modified while the iteration is in progress in any way other than by calling this method.
	 *
	 * @exception UnsupportedOperationException if the <tt>remove</tt> operation is not supported by this Iterator.
	 * @exception IllegalStateException if the <tt>next</tt> method has not yet been called, or the <tt>remove</tt> method has
	 *                already been called after the last call to the <tt>next</tt> method.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
