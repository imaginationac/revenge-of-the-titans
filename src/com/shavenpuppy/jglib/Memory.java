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
import java.util.HashMap;
import java.util.Map;

/**
 * Memory is a crude memory allocator that attempts to manage memory allocation.
 * Somewhat crudely memory is allocated in chunks to the nearest power of two;
 * the smallest allocation is 256 bytes.
 */
public class Memory extends Resource {

	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG = false;

	/**
	 * A class to keep track of allocations in an allocator
	 */
	private static class AllocationHandler {

		/** The source buffer */
		final ByteBuffer allocator;

		/** Entries */
		final Entry entry;

		/**
		 * Entries in the lists
		 */
		class Entry {
			int position;
			int length;
			boolean allocated;
			Entry next, prev;
			/**
			 * Merge this free entry with the next entry. All contiguous entries
			 * are merged.
			 */
			void merge() {
				assert allocated;
				allocated = false;
				// Now merge all contiguous blocks together:
				// 1. Scan back to first allocated block
				Entry current = this;
				while (current.prev != null && !current.prev.allocated) {
					current = current.prev;
				}
				// 2. Now scan forward to first allocated block
				while (current.next != null && !current.next.allocated) {
					current.length += current.next.length;
					current.next = current.next.next;
				}
				if (current.next != null) {
					current.next.prev = current;
				}
			}
			/**
			 * Split this entry into an allocated part and unallocated part.
			 */
			void split(int size) {
				assert !allocated;
				assert size <= length;

				allocated = true;

				if (size == length) {
					// Allocated the whole thing
					return;
				} else {
					Entry newEntry = new Entry();
					newEntry.prev = this;
					newEntry.next = next;
					newEntry.position = position + size;
					newEntry.length = length - size;
					length = size;
					next = newEntry;
				}
			}

			/**
			 * Create us a buffer out of this tree branch
			 */
			ByteBuffer createBuffer() {
				ByteBuffer dup = allocator.duplicate();
				dup.clear().limit(position + length);
				dup.position(position);
				return dup.slice().order(ByteOrder.nativeOrder()); // For fuck's sake, why doesn't it take the byteorder from the original buffer???
			}

		}

		/**
		 * Constructor
		 */
		AllocationHandler(ByteBuffer allocator) {
			this.allocator = allocator;

			// Create a single freelist entry
			entry = new Entry();
			entry.length = allocator.capacity();
		}

		/**
		 * Allocate some memory
		 */
		Entry allocate(int size) {
			// Simple and inefficient algorithm: just scan through the linked list until we find an Entry
			// big enough
			Entry e = entry;
			while (e != null) {
				if (e.allocated || e.length < size) {
					e = e.next;
				} else {
					e.split(size);
					return e;
				}
			}
			return null;
		}

	}

	/** A map of names to AllocationHandlers */
	private static final Map<String, AllocationHandler> allocatorMap = new HashMap<String, AllocationHandler>();

	/** The source buffer used */
	private String allocatorName;

	/** The length of the memory we want to allocate */
	private int length;

	/** The branch of the allocation tree in which this memory is allocated */
	private AllocationHandler.Entry entry;

	/** And a nice safe buffer we can use to access the memory */
	private ByteBuffer buffer;

	/**
	 * Constructor for Memory.
	 *
	 * @param allocator The name of the ByteBuffer in which you wish to allocate memory
	 * @param length The number of bytes you wish to allocate
	 */
	public Memory(String allocatorName, int length) {
		super("Memory allocation ("+length+" bytes)");
		this.allocatorName = allocatorName;
		this.length = length;
	}

	/**
	 * Initialize with an allocator
	 * @param name The name of the allocator
	 * @param buf The memory itself
	 */
	public static synchronized void init(String name, ByteBuffer allocator) {
		AllocationHandler ah = new AllocationHandler(allocator);
		allocatorMap.put(name, ah);
	}

	/* (non-Javadoc)
	 * @see com.powersolve.opengl.ALResource#doCreate()
	 */
	@Override
	protected final synchronized void doCreate() {

		// Find a tree for the allocator; if none exists then create one.
		AllocationHandler handler = allocatorMap.get(allocatorName);

		// Minimum length is 32 bytes
		length = Math.max(32, length);

		entry = handler.allocate(length);
		if (entry == null) {
			throw new RuntimeException("Couldn't allocate "+length+" bytes from "+allocatorName);
		}
		buffer = entry.createBuffer();

		//System.out.println("Allocated memory "+Sys.getDirectBufferAddress(buffer)+" Pos:"+entry.position+"->"+(entry.position+entry.length)+" to "+Sys.getDirectBufferAddress(allocator)+"/"+allocator.hashCode());
	//	Thread.dumpStack();
	}

	/* (non-Javadoc)
	 * @see com.powersolve.opengl.ALResource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
	//	System.out.println("Deallocated mem/ory @ "+entry.position+"->"+(entry.position+entry.length)+" to "+allocator.hashCode());
		entry.merge();
		entry = null;
		buffer = null;
	}

	/**
	 * Returns the buffer
	 */
	public final ByteBuffer getBuffer() {
		assert isCreated();
		return buffer;
	}


	/**
	 * Cleanup. All memory from a particular allocator is removed.
	 */
	public static synchronized void cleanup(String name) {
		allocatorMap.remove(name);
	}

	/**
	 * Slice and dice memory.
	 * @param src The source ByteBuffer
	 * @param byteOffset The offset, in bytes, from which to take a sub-buffer
	 * @param length The length of the sub-buffer to chop out
	 * @return the new ByteBuffer
	 */
	public static ByteBuffer chop(ByteBuffer src, int byteOffset, int length) {
		ByteBuffer ret = src.duplicate();

		ret.clear().position(byteOffset).limit(byteOffset + length);
		ret = ret.slice();
		ret.order(ByteOrder.nativeOrder());

		return ret;
	}

	/**
	 * Slice and dice memory.
	 * @param src The source ByteBuffer
	 * @param byteOffset The offset, in bytes, from which to take a sub-buffer
	 * @return the new ByteBuffer
	 */
	public static ByteBuffer chop(ByteBuffer src, int byteOffset) {
		ByteBuffer ret = src.duplicate();

		ret.clear().position(byteOffset);
		ret = ret.slice();
		ret.order(ByteOrder.nativeOrder());

		return ret;
	}

}
