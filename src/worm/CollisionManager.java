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
package worm;

import java.util.List;

import org.lwjgl.util.ReadableRectangle;

interface CollisionManager {

	/**
	 * Clears the quadtree. Do this before adding all the entities.
	 */
	void clear();

	/**
	 * Store an entity in this node
	 * @param entity
	 */
	void store(Entity entity);

	/**
	 * Add an entity to the quadtree
	 * @param entity
	 * @return the node we end up adding the entity to, or null if we can't fit it in
	 */
	CollisionManager add(Entity entity);

	/**
	 * Find out where in this quadtree we'd place this rectangle.
	 * @param rect
	 * @return the node we would end up placing the rectangle in, or null if we can't fit it in
	 */
	CollisionManager submit(ReadableRectangle rect);

	/**
	 * Remove an entity from this node. Recursively traverses the nodes until it finds the entity.
	 * @param entity
	 * @return true if the entity was removed; false if it wasn't found
	 */
	boolean remove(Entity entity);

	/**
	 * Populate a list with all the entities colliding with the specified entity. Only entities in this node, its children, and all parent nodes
	 * are checked.
	 * @param entity The entity to check against
	 * @param dest The list to store collisions in, may be null
	 * @return dest, or a new List or dest is null
	 */
	List<Entity> checkCollisions(Entity entity, List<Entity> dest);

	/**
	 * Populate a list with all the entities colliding with the specified rectangle. Only entities in this node, its children, and all parent nodes
	 * are checked.
	 * @param rect The entity to check against
	 * @param dest The list to store collisions in, may be null
	 * @return dest, or a new List or dest is null
	 */
	List<Entity> checkCollisions(ReadableRectangle rect, List<Entity> dest);

	/**
	 * Check all the collisions in this node.
	 */
	void checkCollisions();

}