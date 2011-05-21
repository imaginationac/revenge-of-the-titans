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
 *
 * The original code to this class I think came from pjt33 on java-gaming.org - many thanks!
 */
package worm.path;

import java.io.Serializable;

import worm.util.map.IntKeyOpenHashMap;


class BinaryHeap implements Serializable {

	private static final long serialVersionUID = 1L;

//	private final String nodePoolName;
//
//	transient NodePool nodePool;

	private Node[] heap = new Node[128];
	private int size = 0;
	private IntKeyOpenHashMap<Node> userstateToNodeMap;

	BinaryHeap() {
//		this.nodePoolName = nodePoolName;
//		this.nodePool = AStar.getNodePool(nodePoolName);
		this.userstateToNodeMap = new IntKeyOpenHashMap<Node>(4096);
	}

	void clear() {
		for (int i = 0; i < size; i ++) {
			heap[i].heapIdx = -1;
//			nodePool.release(heap[i]);
			heap[i] = null;
		}
		size = 0;
		userstateToNodeMap.clear();
	}

//	/**
//	 * Deserialization support
//	 */
//	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
//		stream.defaultReadObject();
//		nodePool = AStar.getNodePool(nodePoolName);
//	}

	void insert(Node node) {
		// Expand if necessary.
		if (size == heap.length) {
			Node[] newHeap = new Node[size << 1];
			System.arraycopy(heap, 0, newHeap, 0, size);
			heap = newHeap;
		}

		// Map userstate
		if (userstateToNodeMap.put(node.userState, node) != null) {
			throw new IllegalStateException("Binary heap already contains "+node);
		}

		// Insert at end and bubble up.
		heap[size] = node;
		node.heapIdx = size;
		upHeap(size++);
	}

	Node peek() {
		if (size == 0) {
			throw new IllegalStateException("Can't peek - heap is empty");
		}
		return heap[0];
	}

	Node pop() {
		if (size == 0) {
			throw new IllegalStateException("Can't pop - heap is empty");
		}
		Node popped = heap[0];
		heap[0] = heap[--size];
		heap[size] = null;
		if (size > 0) {
			downHeap(0);
		}
		// Unmap userstate
		popped.heapIdx = -1;
		if (userstateToNodeMap.remove(popped.userState) == null) {
			throw new IllegalStateException("Popped but wasn't in the heap");
		}
		return popped;
	}

	void remove(Node node) {
		if (size == 0) {
			throw new IllegalStateException("Binary heap is empty: attempting to remove node "+node);
		}
		if (node.heapIdx == -1) {
			throw new IllegalStateException("Node "+node+" is not in the binary heap");
		}
		// This is what node.heapIdx is for.
		heap[node.heapIdx] = heap[--size];
		heap[size] = null;
		if (size > node.heapIdx) {
			if (heap[node.heapIdx].f < node.f) {
				upHeap(node.heapIdx);
			} else {
				downHeap(node.heapIdx);
			}
		}
		// Just as a precaution: should make stuff blow up if the node is
		// abused.
		node.heapIdx = -1;
		//nodePool.release(node);
		// Unmap userstate
		userstateToNodeMap.remove(node.userState);
	}

	Node getNode(int userstate) {
		if (size == 0) {
			return null;
		}
		return userstateToNodeMap.get(userstate);
	}

	void changeCost(Node node, int newCost) {
		float oldCost = node.f;
		node.f = newCost;
		if (newCost < oldCost) {
			upHeap(node.heapIdx);
		} else {
			downHeap(node.heapIdx);
		}
	}

	int size() {
		return size;
	}

	boolean isEmpty() {
		return size == 0;
	}

	private void upHeap(int idx) {
		Node node = heap[idx];
		int cost = node.f;
		while (idx > 0) {
			int parentIdx = idx - 1 >> 1;
			Node parent = heap[parentIdx];
			if (cost < parent.f) {
				heap[idx] = parent;
				parent.heapIdx = idx;
				idx = parentIdx;
			} else {
				break;
			}
		}
		heap[idx] = node;
		node.heapIdx = idx;
	}

	private void downHeap(int idx) {
		Node node = heap[idx];
		int cost = node.f;

		while (true) {
			int leftIdx = 1 + (idx << 1);
			int rightIdx = leftIdx + 1;

			if (leftIdx >= size) {
				break;
			}

			// We definitely have a left child.
			Node leftNode = heap[leftIdx];
			int leftCost = leftNode.f;
			// We may have a right child.
			Node rightNode;
			int rightCost;

			if (rightIdx >= size) {
				// Only need to compare with left.
				rightNode = null;
				rightCost = Integer.MAX_VALUE;
			} else {
				rightNode = heap[rightIdx];
				rightCost = rightNode.f;
			}

			// Find the smallest of the three costs: the corresponding node
			// should be the parent.
			if (leftCost < rightCost) {
				if (leftCost < cost) {
					heap[idx] = leftNode;
					leftNode.heapIdx = idx;
					idx = leftIdx;
				} else {
					break;
				}
			} else {
				if (rightCost < cost) {
					heap[idx] = rightNode;
					rightNode.heapIdx = idx;
					idx = rightIdx;
				} else {
					break;
				}
			}
		}

		heap[idx] = node;
		node.heapIdx = idx;
	}
}
