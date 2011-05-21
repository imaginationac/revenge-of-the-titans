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

import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.IntList;

/**
 * To make the PathFinder search arbitrary topologies, you should implement
 * this interface. Each node in the topology must have a completely unique
 * index. For grid topologies, use x + y * width as an index.
 */
public interface Topology {

	static final float SPEED_SCALE = 8.0f;
	static final int ROAD_COST = FPMath.fpValue(SPEED_SCALE);
	static final int NORMAL_COST = ROAD_COST * 2;
	static final int BOG_COST = ROAD_COST * 3;
	static final float SPEED_SCALE_FACTOR = 1.0f / SPEED_SCALE;

	/**
	 * Calculates the cost of moving from one node to another. The nodes are
	 * guaranteed adjacent, as returned by getNeighbours(). If the destination
	 * node is impassable then the cost should return -1.
	 *
	 * @param from The node you're moving from
	 * @param to The node you're moving to
	 * @return the cost of traversing a node from another node, or -1 if
	 * the move is blocked.
	 */
	int getCost(int from, int to);

	/**
	 * Get the states adjacent to the specified state.
	 *
	 * @param node The state which we want adjacent nodes for
	 * @param parent The parent state of node, or -1 if there is no parent
	 * @param dest An intlist to place the nodes in
	 */
	void getNeighbours(int node, int parent, IntList dest);

	/**
	 * Get the estimated cost to the goal from a particular state
	 * @param from The state to guess from
	 * @param to The state to guess to
	 * @return the estimated cost to get to the goal
	 */
	int getDistance(int from, int to);

	/**
	 * Get the width of the map
	 * @return int
	 */
	int getWidth();

	/**
	 * Get the height of the map
	 * @return int
	 */
	int getHeight();
}