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

import org.lwjgl.Sys;




/**
 * A PriorityPool takes care of allocating objects by priority from a finite pool.
 */
public class PriorityPool {

	/** Pool wrapper */
	private final class PoolWrapper {

		/** The index into the pool, when active */
		private int index;

		/** The Pooled thing's owner */
		private Object owner;

		/** The age of this Pooled thing */
		private long age;

		/** The actual pooled object */
		private final PriorityPooled pooled;

		/**
		 * Constructor
		 */
		PoolWrapper(PriorityPooled pooled) {
			this.pooled = pooled;
		}

		int getPriority() {
			return pooled.getPriority();
		}

		boolean isLocked() {
			return pooled.isLocked();
		}

		/**
		 * @return true if this is active
		 */
		boolean isActive() {
			return owner != null;
		}

		/**
		 * Deactivate this object. The owner is cleared.
		 */
		void deactivate() {
			if (isActive()) {
				pooled.deactivate();
				owner = null;
				returnToPool(this);
			}
		}

		/**
		 * Allocate this object. The object's priority is set and it becomes active.
		 * @param priority The object's new priority
		 * @param owner The object's new owner
		 */
		void allocate(int priority, Object owner) {
			this.owner = owner;
			age = Sys.getTime();
			pooled.allocate(owner);
			pooled.setPriority(priority);
		}

		/**
		 * Perform ticking. If this object is active then we check to see whether it
		 * should be deactivated.
		 */
		void tick() {
			if (isActive()) {
				pooled.tick();
				if (!pooled.isActive()) {
					deactivate();
				}
			}
		}

		/**
		 * Reset to initial 'unused' state
		 */
		void reset() {
			deactivate();
		}
	}

	/** The pool */
	private final PoolWrapper[] pool;

	/** Copy of the pool */
	private final PoolWrapper[] poolCopy;

	/** The number of active entries */
	private int inUse;

	/**
	 * Constructor for PriorityPool.
	 * @param pool[] the pooled objects, which should be all the same class, and unique, and not null
	 */
	public PriorityPool(PriorityPooled[] pooled) {
		pool = new PoolWrapper[pooled.length];
		poolCopy = new PoolWrapper[pooled.length];
		for (int i = 0; i < pooled.length; i ++) {
			pool[i] = new PoolWrapper(pooled[i]);
			pool[i].index = i;
		}
	}

	/**
	 * Allocate an object from the pool. The rule is as follows:
	 * <ol>
	 * <li>If the pool contains an inactive entry, it is activated and returned.</li>
	 * <li>Otherwise if no entries in the pool are at a lower priority, null is returned.</li>
	 * <li>Otherwise the oldest entry of the same priority is deactivated and returned.</li>
	 * </ol>
	 * @param priority The priority of this allocation
	 * @param owner The new owner of the object
	 * @return an object from the pool, or null if no object can be returned.
	 */
	public PriorityPooled allocate(int priority, Object owner) {
		PriorityPooled ret = null;

		// If we haven't used all the slots, just return the next unused slot
		// right away:
		if (inUse < pool.length) {
			pool[inUse].allocate(priority, owner);
			return pool[inUse ++].pooled;
		}

		// Search through the list of unlocked things finding the oldest lowest priority one:
		int lowestPriority = 999999;
		long oldestAge = Long.MAX_VALUE;//0x7FFFFFFFFFFFFFFFL;
		int oldestIndex = -1;

		for (int i = 0; i < inUse ; i ++) {
			if (pool[i].isLocked()) {
				continue;
			}
			if (pool[i].getPriority() == lowestPriority) {
				if (pool[i].age < oldestAge) {
					oldestAge = pool[i].age;
					oldestIndex = i;
				}
			} else if (pool[i].getPriority() < lowestPriority) {
				lowestPriority = pool[i].getPriority();
				oldestAge = pool[i].age;
				oldestIndex = i;
			}
		}

		if (oldestIndex >= 0) {
			// Get the actual object, because when it's deactivated it'll get moved
			PoolWrapper pw = pool[oldestIndex];
			if (pw.getPriority() <= priority) {
				// Deactivate someone else's and snatch it
				pw.deactivate();
				inUse ++;
				pw.allocate(priority, owner);
				ret = pw.pooled;
			}
		}

		return ret;

	}

	/**
	 * Return a pooled item back to the pool of inactive things.
	 * @param item The thing to return
	 */
	private void returnToPool(PoolWrapper item) {
		pool[item.index] = pool[inUse - 1];
		pool[item.index].index = item.index;
		pool[inUse - 1] = item;
		item.index = -- inUse;
	}

	/**
	 * Tick. Call this every frame. This will check each active item the pool to see
	 * if it has deactivated.
	 */
	public void tick() {
		final int n = inUse;
		System.arraycopy(pool, 0, poolCopy, 0, inUse);
		for (int i = 0; i < n; i ++) {
			poolCopy[i].tick();
		}
	}

	/**
	 * Reset the pool. All pooled objects are deactivated.
	 */
	public void reset() {
		for (int i = inUse; --i >= 0; ) {
			pool[i].reset();
		}
	}
}
