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

import java.io.Serializable;

import worm.IntGrid;

import com.shavenpuppy.jglib.util.IntList;

/**
 * Implements the basic heuristic A* route finding algorithm, using integers
 * as an optimised format for node states. If you can pack all your state information
 * for a search node into an int, use this version of A* instead. For example, a grid
 * search can easily be packed into an int by packing the coordinates of the grid in
 * bytes.
 */
public class AStar implements PathFinder, Serializable {

	private static final long serialVersionUID = 1L;

	private static class IntVal implements Serializable {
		private static final long serialVersionUID = 1L;

		private int val;

		IntVal(int v) {
			val = v;
		}

		@Override
		public boolean equals(Object o) {
			return ((Node) o).userState == val;
		}
		@Override
		public int hashCode() {
			return val;
		}

		IntVal set(int v) {
			val = v;
			return this;
		}
	}

	private final IntVal tempInt = new IntVal(0);

	/** The current list of open nodes */
	private final BinaryHeap openList;

	/** The current list of closed nodes */
	private final IntGrid closedList;
	private int closedCount;

	/** The map */
	private final Topology map;

	/** A current list of neighbours. Optimized for 2D grids to begin with but it will adjust itself */
	private IntList neighbours = new IntList(true, 2048);

//	/** Map of node pool names to NodePools */
//	private static final Map NODEPOOLS = new HashMap();

//	/** Node pool name */
//	private final String nodePoolName;

//	/** Node pool we're using */
//	private transient NodePool nodePool;

	/** Maximum brain size */
	private int maxSize;

	/** The goal node */
	private Node end;

	/** The initial node */
	private Node start;

	/** The number of steps */
	private int steps;

	/** The current path */
	private IntList path;

	/** Search states */
	public static final int SEARCH_STATE_SUCCEEDED = 1;
	public static final int SEARCH_STATE_FAILED = 2;
	public static final int SEARCH_STATE_NOT_INITIALIZED = 3;
	public static final int SEARCH_STATE_INVALID = 4;
	public static final int SEARCH_STATE_CANCELLED = 5;
	public static final int SEARCH_STATE_SEARCHING = 6;

	/** The current search state */
	private int state = SEARCH_STATE_NOT_INITIALIZED;

	/**
	 * Constructor for AStar.
	 * @param map The map to traverse
	 */
	public AStar(Topology map) {
		this.map = map;
//		this.nodePoolName = nodePoolName;
//		this.nodePool = getNodePool(nodePoolName);
		openList = new BinaryHeap();
		closedList = new IntGrid(map.getWidth() / 32 + (map.getWidth() % 32 != 0 ? 1 : 0), map.getHeight(), 0);
		maxSize = map.getWidth() * map.getHeight();
	}

//	/**
//	 * Deserialization support
//	 */
//	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
//		stream.defaultReadObject();
//		nodePool = getNodePool(nodePoolName);
//	}

//	static NodePool getNodePool(String nodePoolName) {
//		synchronized (NODEPOOLS) {
//			NodePool np = (NodePool) NODEPOOLS.get(nodePoolName);
//			if (np == null) {
//				np = new NodePool();
//				NODEPOOLS.put(nodePoolName, np);
//			}
//			return np;
//		}
//	}

	/**
	 * @see com.shavenpuppy.jglib.algorithms.PathFinder#findPath(int, int, IntList)
	 */
	@Override
	public void findPath(int startState, int endState, IntList path) {
		cleanup();
		steps = 0;
		this.path = path;
		state = SEARCH_STATE_SEARCHING;
		start = new Node(startState, null);//nodePool.obtain(startState, null);
		openList.insert(start);
		start.g = 0;
		start.h = map.getDistance(startState, endState);
		start.f = start.h;
		end = new Node(endState, null);//nodePool.obtain(endState, null);
	//	System.out.println("Starting with "+start+" and heading for "+end);
	}

	/**
	 * Cancel the current search
	 */
	@Override
	public void cancel() {
		state = SEARCH_STATE_CANCELLED;
		cleanup();
	}

	/**
	 * Clean up when a search is finished
	 */
	private void cleanup() {
		openList.clear();
		closedList.clear();
		closedCount = 0;
		neighbours.clear();
	}

	/**
	 * @see com.shavenpuppy.jglib.algorithms.PathFinder#nextStep()
	 */
	@Override
	public int nextStep() {

		if (state != SEARCH_STATE_SEARCHING) {
			return state;
		}

		// Failure is defined as emptying the open list as there is nothing left to
		// search...
		if (openList.isEmpty()) {
			state = SEARCH_STATE_FAILED;
			cleanup();
			return state;
		}

		// Incremement step count
		steps ++;

		// Pop the best node (the one with the lowest f) : this is at the head of the list
		Node n = openList.pop(); // get pointer to the node

		// Check for the goal, once we pop that we're done
		if (n.equals(end)) {
			goalFound(n);
		} else {
			goalNotFound(n);
		}

		return state;
	}

	/**
	 * @return the steps used so far
	 */
	public int getNumSteps() {
		return steps;
	}

	/**
	 * The goal has been found.
	 * @param n The current node
	 */
	private void goalFound(Node n) {
		end.parent = n.parent;

		// A special case is that the goal was passed in as the start state
		// so handle that here
		path.clear();
		if (n != start) {
			// set the child pointers in each node (except Goal which has no child)
			Node nodeChild = n;
			Node nodeParent = n.parent;

			do {
				nodeParent.child = nodeChild;
				nodeChild = nodeParent;
				nodeParent = nodeParent.parent;

			} while (!nodeChild.equals(start)); // Start is always the first node by definition

			// Fill in the path
			n = start;
			do {
				n = n.child;
				if (n != null) {
					path.add(n.userState);
				}
			} while (n != null && !n.equals(end));
		}

		state = SEARCH_STATE_SUCCEEDED;

		cleanup();
	}

	private static int pack(int x, int y) {
		return x & 0xFFFF | y << 16;
	}

	private static int getX(int state) {
		if ((state & 0xFFFF) <= 0x7FFF) {
			return state & 0x7FFF;
		} else {
			return state & 0xFFFF | 0xFFFF0000;
		}
	}

	private static int getY(int state) {
		return state >> 16;
	}

	/**
	 * The goal has not been found.
	 * @param n The current node
	 */
	private void goalNotFound(Node n) {

		// We now need to generate the neighbours of this node. We ask the map for the neighbours.
		// By passing in the parent of the current node we help the map to avoid returning a neighbour
		// that simply backtracks.

		//System.out.println("Get neighbours for "+n.userState+"/"+n.parent);
		map.getNeighbours(n.userState, n.parent != null ? n.parent.userState : n.userState, neighbours);

		int numNeighbours = neighbours.size();
		for (int i = 0; i < numNeighbours; i ++) {
			int newState = neighbours.get(i);
			assert newState != -1 : "-1 newstate, neighbours="+neighbours;
			int newg = n.g + map.getCost(n.userState, newState);

			// Now find the node on the open or closed lists. If it is on a list
			// already but the node that is already there has a better (lower) g
			// score then forget about this neighbour.

			// Check closed list
			int x = getX(newState);
			assert x >= 0;
			int xx = x / 32;
			int xxx = 1 << (x & 31);
			int v = closedList.getValue(xx, getY(newState));
			if ((v & xxx) != 0) {
				continue;
			}

			// Then the open list:
			Node foundOnOpenList = openList.getNode(newState);
			if (foundOnOpenList != null) {
				if (foundOnOpenList.g <= newg) {
					// Already got a node that's cheaper on the open list, or it's closed
					continue;
				}
				openList.remove(foundOnOpenList);
			}

			// Now add a new node
			Node newNode = new Node(newState, n);//nodePool.obtain(newState, n);
			newNode.g = newg;
			newNode.h = map.getDistance(newState, end.userState);
			newNode.f = newNode.g + newNode.h;
			if (openList.size() == maxSize) {
				state = SEARCH_STATE_FAILED;
				return;
			}
			openList.insert(newNode);
		}

		if (closedCount == maxSize) {
			state = SEARCH_STATE_FAILED;
			return;
		}
		closedCount ++;

		int x = getX(n.userState);
		if (x >= 0) {
			int xx = x / 32;
			int xxx = 1 << (x & 31);
			int v = closedList.getValue(xx, getY(n.userState));
			closedList.setValue(xx, getY(n.userState), v | xxx);
		}

	}

}
