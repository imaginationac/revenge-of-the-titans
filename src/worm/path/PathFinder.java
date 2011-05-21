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
package worm.path;

import com.shavenpuppy.jglib.util.IntList;

/**
 * A PathFinder can find a path between two indexed nodes in a Map.
 */
public interface PathFinder {

	/**
	 * Find the best path through the Map from a start node to an end node.
	 * After calling findPath() you need to call nextStep() repeatedly until
	 * nextStep() returns SEARCH_STATE_SUCCEEDED or SEARCH_STATE_FAILED.
	 *
	 * The path found will be returned in path.
	 *
	 * @param start The start node state
	 * @param end The end node state
	 * @param path Contains the path through the nodes
	 */
	void findPath(int start, int end, IntList path);

	/**
	 * Perform the next step in the search.
	 * @return SEARCH_STATE_SUCCEEDED if the search has found a path
	 * SEARCH_STATE_FAILED if no path could be found
	 * SEARCH_STATE_INVALID if the search is not complete yet
	 * SEARCH_STATE_NOT_INITIALIZED if you forgot to call findPath()
	 */
	int nextStep();


	/**
	 * Cancel the current search.
	 */
	void cancel();


	// Search states
	static final int SEARCH_STATE_SUCCEEDED = 1;
	static final int SEARCH_STATE_FAILED = 2;
	static final int SEARCH_STATE_NOT_INITIALIZED = 3;
	static final int SEARCH_STATE_INVALID = 4;
	static final int SEARCH_STATE_CANCELLED = 5;
	static final int SEARCH_STATE_SEARCHING = 6;

}