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
package com.shavenpuppy.jglib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Allocates direct bytebuffers of a particular size.
 * @author foo
 */
public final class DirectBufferAllocator {

	/** Map of sizes to Lists of WrappedBuffers */
	private static final Map<Integer, WrappedBuffer> memMap = new TreeMap<Integer, WrappedBuffer>();

	/**
	 * No c'tor
	 */
	private DirectBufferAllocator() {
	}

	public static WrappedBuffer allocate(int size) {

		// Find the list for that size, if any
		Integer iSize = new Integer(size);

		for (Iterator<Map.Entry<Integer, WrappedBuffer>> i = memMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<Integer, WrappedBuffer> entry = i.next();
			Integer entrySize = entry.getKey();
			WrappedBuffer wrapped = entry.getValue();
			if (wrapped.isDisposed()) {
				if (entrySize.intValue() >= size) {
					wrapped.allocate();
					//System.out.println("Reused a buffer of size "+entrySize+" for data of size "+size);
					return wrapped;
				} else {
					i.remove();
					wrapped.clear();
					//System.out.println("Collected a buffer of size "+entrySize);
				}
			}
		}

		ByteBuffer data = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		WrappedBuffer wrapped = new WrappedBuffer(data);
		memMap.put(iSize, wrapped);
		//System.out.println("Couldn't find a buffer of size "+size);
		return wrapped;

	}
}
