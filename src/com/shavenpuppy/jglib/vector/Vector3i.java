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
package com.shavenpuppy.jglib.vector;

import java.nio.IntBuffer;

import com.shavenpuppy.jglib.util.FPMath;

/**
 * $Id: Vector3i.java,v 1.10 2011/04/18 23:28:06 cix_foo Exp $
 *
 * Holds a 3-tuple vector.
 *
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @version $Revision: 1.10 $
 */

public final class Vector3i extends Vector implements WritableVector3i {

	private static final long serialVersionUID = 1L;

	private int x, y, z;

	/**
	 * Constructor for Vector3f.
	 */
	public Vector3i() {
		super();
	}

	/**
	 * Constructor
	 */
	public Vector3i(ReadableVector3i src) {
		set(src);
	}

	/**
	 * Constructor
	 */
	public Vector3i(int x, int y, int z) {
		set(x, y, z);
	}

	/**
	 * Set values
	 */
	@Override
	public void set(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Load from another Vector3f
	 * @param src The source vector
	 */
	@Override
	public void set(ReadableVector3i src) {
		set(src.getX(), src.getY(), src.getZ());
	}

	/**
	 * @return the length squared of the vector
	 */
	@Override
	public long lengthSquared() {
		long lx = x;
		long ly = y;
		long lz = z;
		return (lx * lx + ly * ly + lz * lz) >> 16L;//FPMath.lmul(x, x) + FPMath.lmul(y, y) + FPMath.lmul(z, z);
	}

	/**
	 * Translate a vector
	 * @param x The translation in x
	 * @param y the translation in y
	 */
	@Override
	public void translate(int x, int y, int z) {
		this.x += x;
		this.y += y;
		this.z += z;
	}

    /**
     * Add a vector to another vector and place the result in a destination
     * vector.
     * @param left The LHS vector
     * @param right The RHS vector
     * @param dest The destination vector, or null if a new vector is to be created
     */
    public static void add(ReadableVector3i left, ReadableVector3i right, WritableVector3i dest) {
 		dest.set(left.getX() + right.getX(), left.getY() + right.getY(), left.getZ() + right.getZ());
    }

    /**
     * Subtract a vector from another vector and place the result in a destination
     * vector.
     * @param left The LHS vector
     * @param right The RHS vector
     * @param dest The destination vector, or null if a new vector is to be created
     */
    public static void sub(ReadableVector3i left, ReadableVector3i right, WritableVector3i dest) {
		dest.set(left.getX() - right.getX(), left.getY() - right.getY(), left.getZ() - right.getZ());
    }

	/**
	 * The cross product of two vectors.
	 *
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination result, or null if a new vector is to be created
	 */
	public static void cross(
		ReadableVector3i left,
		ReadableVector3i right,
		WritableVector3i dest)
	{

		dest.set(
			FPMath.mul(left.getY(), right.getZ()) - FPMath.mul(left.getZ(), right.getY()),
			FPMath.mul(right.getX(), left.getZ()) - FPMath.mul(right.getZ(), left.getX()),
			FPMath.mul(left.getX(), right.getY()) - FPMath.mul(left.getY(), right.getX())
			);
	}



	/**
	 * Negate a vector
	 */
	@Override
	public void negate() {
		x = -x;
		y = -y;
		z = -z;
	}

	/**
	 * Negate a vector and place the result in a destination vector.
	 * @param dest The destination vector or null if a new vector is to be created
	 */
	@Override
	public void negate(WritableVector3i dest) {
		dest.set(-x, -y, -z);
	}

	/**
	 * The dot product of two vectors is calculated as
	 * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
	public static int dot(ReadableVector3i left, ReadableVector3i right) {
		return (left.getX() * right.getX() + left.getY() * right.getY() + left.getZ() * right.getZ()) >> 16;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#load(FloatBuffer)
	 */
	@Override
	public void load(IntBuffer buf) {
    	x = buf.get();
    	y = buf.get();
    	z = buf.get();
 	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	@Override
	public void scale(int scale) {

		x = FPMath.mul(x, scale);
		y = FPMath.mul(y, scale);
		z = FPMath.mul(z, scale);

	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	@Override
	public void invscale(int scale) {

		x = FPMath.div(x, scale);
		y = FPMath.div(y, scale);
		z = FPMath.div(z, scale);


	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#store(FloatBuffer)
	 */
	@Override
	public void store(IntBuffer buf) {

		buf.put(x);
		buf.put(y);
		buf.put(z);

	}

	@Override
	public String toString() {
		return "["+FPMath.floatValue(x)+", "+FPMath.floatValue(y)+", "+FPMath.floatValue(z)+"]";
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.vector.ReadableVector3i#getX()
	 */
	@Override
	public int getX() {
		return x;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.vector.ReadableVector3i#getY()
	 */
	@Override
	public int getY() {
		return y;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.vector.ReadableVector3i#getZ()
	 */
	@Override
	public int getZ() {
		return z;
	}

	/**
	 * @param x The x to set.
	 */
	@Override
	public void setX(int x) {
		this.x = x;
	}
	/**
	 * @param y The y to set.
	 */
	@Override
	public void setY(int y) {
		this.y = y;
	}
	/**
	 * @param z The z to set.
	 */
	@Override
	public void setZ(int z) {
		this.z = z;
	}

}
