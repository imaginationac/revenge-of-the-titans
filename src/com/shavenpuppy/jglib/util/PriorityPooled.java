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
 * A PriorityPooled object has a priority and is either "active" or "inactive", and
 * has an age, and may be owned by someone while it is active. Every frame it is ticked
 * and asked whether it is still active.
 */
public interface PriorityPooled {

	/**
	 * @return true if this is active
	 */
	public boolean isActive();

	/**
	 * Deactivate this object. This allows an object to clean up when it is
	 * forcibly deactivated by a higher priority request.
	 */
	public void deactivate();

	/**
	 * Allocate this object to a new owner. Before calling other methods on it
	 * you should check to see whether you're still the owner.
	 * @param newOwner the new owner
	 */
	public void allocate(Object newOwner);


	/**
	 * @return the current owner of this object
	 */
	public Object getOwner();

	/**
	 * Tick this object, which allows it to do frame-by-frame processing to
	 * determine whether it is active or not.
	 */
	public void tick();

	/**
	 * Lock this object so that it can't be allocated to a new owner
	 */
	public void lock();

	/**
	 * Unlock this object
	 */
	public void unlock();

	/**
	 * Is this object locked?
	 * @return boolean
	 */
	public boolean isLocked();

	/**
	 * Set the priority of this thing
	 * @param newPriority
	 */
	public void setPriority(int newPriority);

	/**
	 * Get the priority of this thing
	 * @return int
	 */
	public int getPriority();

}
