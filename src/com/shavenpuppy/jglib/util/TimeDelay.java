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
 * @author John Campbell
 */
public class TimeDelay
{
	/** Number of ticks that have elapsed */
	private int ticks;

	/** Maximum amount of ticks that can occur until the timer has finished */
	private int maxTicks;

	/** Create a time delay with a specified maximum tick amount */
	public TimeDelay(int maxTicks)
	{
		reset(maxTicks);
	}

	public TimeDelay(TimeDelay copy)
	{
		this.ticks = copy.ticks;
		this.maxTicks = copy.maxTicks;
	}

	public TimeDelay(int maxTicks, boolean startExpired)
	{
		this.maxTicks = maxTicks;
		this.ticks = maxTicks;
	}

	/** Reset the timer (using the already specified maximum tick amount) */
	public void reset()
	{
		ticks = 0;
	}

	/** Set the maximum amount of ticks and reset the timer */
	public void reset(int newMaxTicks)
	{
		ticks = 0;
		this.maxTicks = newMaxTicks;
	}


	public void tick()
	{
		if (!hasExpired()) {
	        ticks++;
        }
	}

	public boolean hasExpired()
	{
		return (ticks >= maxTicks);
	}
}
