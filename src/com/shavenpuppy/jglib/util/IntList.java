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

/**
 * Quickly hacked together expandable list of integers
 */
public class IntList implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	private int[] value;
	private int count = 0;
	private final boolean fastExpand;

	/**
	 * FloatList constructor comment.
	 */
	public IntList() {
		this(0);
	}

	/**
	 * FloatList constructor comment.
	 */
	public IntList(int size) {
		this(true, size);
	}

	/**
	 * FloatList constructor comment.
	 */
	public IntList(boolean fastExpand, int size) {
		super();
		this.fastExpand = fastExpand;
		value = new int[size];
	}

	/**
	 * add method comment.
	 */
	public int add(int f) {
		if (count == value.length) {
			int[] oldValue = value;
			if (fastExpand) {
				value = new int[(oldValue.length << 1) + 1];
			} else {
				value = new int[oldValue.length + 1];
			}
			System.arraycopy(oldValue, 0, value, 0, oldValue.length);
		}
		value[count] = f;
		return count++;
	}

	/**
	 * Remove an element and return it.
	 * @param idx The index of the element to remove
	 * @return the removed value
	 */
	public int remove(int idx) {
		if (idx >= count || idx < 0) {
			throw new IndexOutOfBoundsException("Referenced "+idx+", size="+count);
		}
		int ret = value[idx];
		if (idx < count - 1) {
			System.arraycopy(value, idx + 1, value, idx, count - idx - 1);
		}
		count --;
		return ret;
	}

	/**
	 * add method comment.
	 */
	public void addAll(int[] f) {
		ensureCapacity(count + f.length);
		System.arraycopy(f, 0, value, count, f.length);
		count += f.length;
	}

	/**
	 * add method comment.
	 */
	public void addAll(IntList f) {
		ensureCapacity(count + f.count);
		System.arraycopy(f.value, 0, value, count, f.count);
		count += f.count;
	}

	/**
	 * toArray method comment.
	 */
	public int[] array() {
		return value;
	}

	/**
	 * Insert the method's description here. Creation date: (11/03/2001
	 * 17:19:01)
	 */
	public int capacity() {
		return value.length;
	}

	/**
	 * clear method comment.
	 */
	public void clear() {
		count = 0;
	}

	/**
	 * Ensure the list is at least 'size' elements big.
	 */
	public void ensureCapacity(int size) {
		if (value.length >= size) {
			return;
		}
		int[] oldValue = value;
		value = new int[size];
		System.arraycopy(oldValue, 0, value, 0, oldValue.length);
	}

	/**
	 * get method comment.
	 */
	public int get(int index) {
		return value[index];
	}

	/**
	 * isEmpty method comment.
	 */
	public boolean isEmpty() {
		return count == 0;
	}

	/**
	 * size method comment.
	 */
	public int size() {
		return count;
	}

	/**
	 * Stash everything in an array.
	 */
	public int[] toArray(int[] dest) {
		if (dest == null) {
			dest = new int[count];
		}
		System.arraycopy(value, 0, dest, 0, count);
		return dest;
	}

	/**
	 * Pack list to its minimum size.
	 */
	public void trimToSize() {
		if (count == value.length) {
			return;
		}
		int[] oldValue = value;
		value = new int[count];
		System.arraycopy(oldValue, 0, value, 0, count);
	}
}
