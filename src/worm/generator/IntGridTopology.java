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
package worm.generator;

import worm.IntGrid;
import worm.path.Topology;

import com.shavenpuppy.jglib.util.IntList;

/**
 * Describes the topology of the simple int-grid used in the random map generator
 */
class IntGridTopology implements Topology, SimpleTiles {

	private final IntGrid map;

	private SolidCheck check;

	/**
	 * C'tor
	 */
	IntGridTopology(IntGrid map) {
		this.map = map;
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getWidth()
	 */
	@Override
	public int getWidth() {
		return map.getWidth();
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getHeight()
	 */
	@Override
	public int getHeight() {
		return map.getHeight();
	}

	/**
	 * @param check the check to set
	 */
	void setCheck(SolidCheck check) {
		this.check = check;
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getCost(int, int)
	 */
	@Override
	public int getCost(int from, int to) {
		if (from == to) {
			return 0;
		}

//		int fromX = from & 0xFFFF;
//		int fromY = from >> 16;
//		int toX = to & 0xFFFF;
//		int toY = to >> 16;

		// Always the same cost
		return 1;
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getDistance(int, int)
	 */
	@Override
	public int getDistance(int from, int to) {
		int fromX = getX(from);
		int fromY = getY(from);
		int toX = getX(to);
		int toY = getY(to);

		return Math.abs(toX - fromX) + Math.abs(toY - fromY);
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getNeighbours(int, int, worm.path.IntList)
	 */
	@Override
	public void getNeighbours(int node, int parent, IntList dest) {
		dest.clear();

		int x = getX(node);
		int y = getY(node);

		int n = pack(x, y + 1);
		int v = map.getValue(x, y + 1);
		if (n != parent && !check.isSolid(x, y + 1, v)) {
			dest.add(n);
		}
		n = pack(x, y - 1);
		v = map.getValue(x, y - 1);
		if (n != parent && !check.isSolid(x, y - 1, v)) {
			dest.add(n);
		}
		n = pack(x + 1, y);
		v = map.getValue(x + 1, y);
		if (n != parent && !check.isSolid(x + 1, y, v)) {
			dest.add(n);
		}
		n = pack(x - 1, y);
		v = map.getValue(x - 1, y);
		if (n != parent && !check.isSolid(x - 1, y, v)) {
			dest.add(n);
		}

	}

	static int pack(int x, int y) {
		return x & 0xFFFF | y << 16;
	}

	static int getX(int state) {
		if ((state & 0xFFFF) <= 0x7FFF) {
			return state & 0x7FFF;
		} else {
			return state & 0xFFFF | 0xFFFF0000;
		}
	}

	static int getY(int state) {
		return state >> 16;
	}

}
