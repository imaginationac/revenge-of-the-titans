/*
 *  Primitive Collections for Java.
 *  Copyright (C) 2002, 2003  Søren Bak
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package worm.util.list;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import worm.util.IntCollection;
import worm.util.hash.DefaultIntHashFunction;
import worm.util.util.Exceptions;


/**
 *  This class represents an array implemenation of lists of
 *  int values.
 *
 *  @see        java.util.ArrayList
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.3     21-08-2003 19:27
 *  @since      1.0
 */
public class IntArrayList extends AbstractIntList implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	/** Constant indicating relative growth policy. */
    private static final int    GROWTH_POLICY_RELATIVE      = 0;

    /** Constant indicating absolute growth policy. */
    private static final int    GROWTH_POLICY_ABSOLUTE      = 1;

    /**
     *  The default growth policy of this list.
     *  @see    #GROWTH_POLICY_RELATIVE
     *  @see    #GROWTH_POLICY_ABSOLUTE
     */
    private static final int    DEFAULT_GROWTH_POLICY       = GROWTH_POLICY_RELATIVE;

    /** The default factor with which to increase the capacity of this list. */
    public static final double DEFAULT_GROWTH_FACTOR        = 1.0;

    /** The default chunk size with which to increase the capacity of this list. */
    public static final int    DEFAULT_GROWTH_CHUNK         = 10;

    /** The default capacity of this list. */
    public static final int    DEFAULT_CAPACITY             = 10;

    /** The elements of this list (indices <tt>0</tt> to <tt>size-1</tt>). */
    private transient int[] data;

    /**
     *  The current size of this list.
     *  @serial
     */
    private int size;

    /**
     *  The growth policy of this list (0 is relative growth, 1 is absolute growth).
     *  @serial
     */
    private int growthPolicy;

    /**
     *  The growth factor of this list, if the growth policy is
     *  relative.
     *  @serial
     */
    private double growthFactor;

    /**
     *  The growth chunk size of this list, if the growth policy is
     *  absolute.
     *  @serial
     */
    private int growthChunk;

    private IntArrayList(int capacity, int growthPolicy, double growthFactor, int growthChunk) {
        if (capacity < 0) {
			Exceptions.negativeArgument("capacity", String.valueOf(capacity));
		}
        if (growthFactor < 0.0) {
			Exceptions.negativeArgument("growthFactor", String.valueOf(growthFactor));
		}
        if (growthChunk < 0) {
			Exceptions.negativeArgument("growthChunk", String.valueOf(growthChunk));
		}
        data = new int[capacity];
        size = 0;
        this.growthPolicy = growthPolicy;
        this.growthFactor = growthFactor;
        this.growthChunk = growthChunk;
    }

    /**
     *  Creates a new array list with capacity 10 and a relative
     *  growth factor of 1.0.
     *
     *  @see        #IntArrayList(int,double)
     */
    public IntArrayList() {
        this(DEFAULT_CAPACITY);
    }

    /**
     *  Creates a new array list with the same elements as a
     *  specified collection. The elements of the specified collection
     *  are added to the end of the list in the collection's iteration
     *  order.
     *
     *  @param      c
     *              the collection whose elements to add to the new
     *              list.
     *
     *  @throws     NullPointerException
     *              if <tt>c</tt> is <tt>null</tt>.
     */
    public IntArrayList(IntCollection c) {
        this(c.size());
        addAll(c);
    }

    /**
     *  Creates a new array list with the same elements as a
     *  specified array. The elements of the specified array
     *  are added to the end of the list in order of the array.
     *
     *  @param      a
     *              the array whose elements to add to the new
     *              list.
     *
     *  @throws     NullPointerException
     *              if <tt>a</tt> is <tt>null</tt>.
     *
     *  @since      1.1
     */
    public IntArrayList(int[] a) {
        this(a.length);
        System.arraycopy(a, 0, data, 0, a.length);
        size = a.length;
    }

    /**
     *  Creates a new array list with a specified capacity and a
     *  relative growth factor of 1.0.
     *
     *  @param      capacity
     *              the initial capacity of the list.
     *
     *  @see        #IntArrayList(int,double)
     *
     *  @throws     IllegalArgumentException
     *              if <tt>capacity</tt> is negative.
     */
    public IntArrayList(int capacity) {
        this(capacity, DEFAULT_GROWTH_FACTOR);
    }

    /**
     *  Creates a new array list with a specified capacity and
     *  relative growth factor.
     *
     *  <p>The array capacity increases to <tt>capacity()*(1+growthFactor)</tt>.
     *  This strategy is good for avoiding many capacity increases, but
     *  the amount of wasted memory is approximately the size of the list.
     *
     *  @param      capacity
     *              the initial capacity of the list.
     *
     *  @param      growthFactor
     *              the relative amount with which to increase the
     *              the capacity when a capacity increase is needed.
     *
     *  @throws     IllegalArgumentException
     *              if <tt>capacity</tt> is negative;
     *              if <tt>growthFactor</tt> is negative.
     */
    public IntArrayList(int capacity, double growthFactor) {
        this(capacity, GROWTH_POLICY_RELATIVE, growthFactor, DEFAULT_GROWTH_CHUNK);
    }

    /**
     *  Creates a new array list with a specified capacity and
     *  absolute growth factor.
     *
     *  <p>The array capacity increases to <tt>capacity()+growthChunk</tt>.
     *  This strategy is good for avoiding wasting memory. However, an
     *  overhead is potentially introduced by frequent capacity increases.
     *
     *  @param      capacity
     *              the initial capacity of the list.
     *
     *  @param      growthChunk
     *              the absolute amount with which to increase the
     *              the capacity when a capacity increase is needed.
     *
     *  @throws     IllegalArgumentException
     *              if <tt>capacity</tt> is negative;
     *              if <tt>growthChunk</tt> is negative.
     */
    public IntArrayList(int capacity, int growthChunk) {
        this(capacity, GROWTH_POLICY_ABSOLUTE, DEFAULT_GROWTH_FACTOR, growthChunk);
    }

    // ---------------------------------------------------------------
    //      Array management
    // ---------------------------------------------------------------

    /**
     *  Computes the new capacity of the list based on a needed
     *  capacity and the growth policy.
     *
     *  @param      capacity
     *              the needed capacity of the list.
     *
     *  @return     the new capacity of the list.
     */
    private int computeCapacity(int capacity) {
        int newcapacity;
        if (growthPolicy == GROWTH_POLICY_RELATIVE) {
			newcapacity = (int)(data.length * (1.0 + growthFactor));
		} else {
			newcapacity = data.length + growthChunk;
		}
        if (newcapacity < capacity) {
			newcapacity = capacity;
		}
        return newcapacity;
    }

    /**
     *  Ensures that this list has at least a specified capacity.
     *  The actual capacity is calculated from the growth factor
     *  or growth chunk specified to the constructor.
     *
     *  @param      capacity
     *              the minimum capacity of this list.
     *
     *  @return     the new capacity of this list.
     *
     *  @see        #capacity()
     */
    public int ensureCapacity(int capacity) {
        if (capacity > data.length) {
            int[] newdata = new int[capacity = computeCapacity(capacity)];
            System.arraycopy(data, 0, newdata, 0, size);
            data = newdata;
        }
        return capacity;
    }

    /**
     *  Returns the current capacity of this list. The capacity is the
     *  number of elements that the list can contain without having to
     *  increase the amount of memory used.
     *
     *  @return     the current capacity of this list.
     *
     *  @see        #ensureCapacity(int)
     */
    public int capacity()
    { return data.length; }

    // ---------------------------------------------------------------
    //      Operations not supported by abstract implementation
    // ---------------------------------------------------------------

    @Override
	public void add(int index, int v) {
        if (index < 0 || index > size) {
			Exceptions.indexOutOfBounds(index, 0, size);
		}
        ensureCapacity(size+1);
        //  Move data
        int block = size-index;
        if (block > 0) {
			System.arraycopy(data, index, data, index+1, block);
		}
        data[index] = v;
        size++;
    }

    @Override
	public int get(int index) {
        if (index < 0 || index >= size) {
			Exceptions.indexOutOfBounds(index, 0, size-1);
		}
        return data[index];
    }

    @Override
	public int set(int index, int v) {
        if (index < 0 || index >= size) {
			Exceptions.indexOutOfBounds(index, 0, size-1);
		}
        int result = data[index];
        data[index] = v;
        return result;
    }

    @Override
	public int removeElementAt(int index) {
        if (index < 0 || index >= size) {
			Exceptions.indexOutOfBounds(index, 0, size-1);
		}
        int result = data[index];
        //  Move data
	    int block = size-(index+1);
        if (block > 0) {
			System.arraycopy(data, index+1, data, index, block);
		}
        size--;
        return result;
    }

    /**
     *  Minimizes the memory used by this array list. The underlying
     *  array is replaced by an array whose size is exactly the number
     *  of elements in this array list. The method can be used to
     *  free up memory after many removals.
     */
    @Override
	public void trimToSize() {
        if (data.length > size) {
            int[] newdata = new int[size];
            System.arraycopy(data, 0, newdata, 0, size);
            data = newdata;
        }
    }

    /**
     *  Returns a clone of this array list.
     *
     *  @return     a clone of this array list.
     *
     *  @since      1.1
     */
    @Override
	public Object clone() {
        try {
            IntArrayList c = (IntArrayList)super.clone();
            c.data = new int[data.length];
            System.arraycopy(data, 0, c.data, 0, size);
            return c;
        } catch (CloneNotSupportedException e) {
            Exceptions.cloning(); return null;
        }
    }

    // ---------------------------------------------------------------
    //      Operations overwritten for efficiency
    // ---------------------------------------------------------------

    @Override
	public int size()
    { return size; }

    @Override
	public boolean isEmpty()
    { return size == 0; }

    @Override
	public void clear()
    { size = 0; }

    @Override
	public boolean contains(int v) {
        for (int i = 0; i < size; i++) {
			if (data[i] == v) {
				return true;
			}
		}
        return false;
    }

    @Override
	public int indexOf(int c) {
        for (int i = 0; i < size; i++) {
			if (data[i] == c) {
				return i;
			}
		}
        return -1;
    }

    /**
     *  @since      1.2
     */
    @Override
	public int indexOf(int index, int c) {
        if (index < 0 || index > size) {
			Exceptions.indexOutOfBounds(index, 0, size);
		}
        for (int i = index; i < size; i++) {
			if (data[i] == c) {
				return i;
			}
		}
        return -1;
    }


    @Override
	public int lastIndexOf(int c) {
        for (int i = size-1; i >= 0; i--) {
			if (data[i] == c) {
				return i;
			}
		}
        return -1;
    }

    @Override
	public boolean remove(int v) {
        int index = indexOf(v);
        if (index != -1) {
            removeElementAt(index);
            return true;
        }
        return false;
    }

    @Override
	public int[] toArray() {
        int[] a = new int[size];
        System.arraycopy(data, 0, a, 0, size);
        return a;
    }

    @Override
	public int[] toArray(int[] a) {
        if (a == null || a.length < size) {
			a = new int[size];
		}
        System.arraycopy(data, 0, a, 0, size);
        return a;
    }

    @Override
	public boolean equals(Object obj) {
        if (this == obj) {
			return true;
		}
        if (!(obj instanceof IntList)) {
			return false;
		}
        int i1 = 0;
        IntListIterator i2 = ((IntList)obj).listIterator();
        while(i1 < size && i2.hasNext()) {
			if (data[i1++] != i2.next()) {
				return false;
			}
		}
        return !(i1 < size || i2.hasNext());
    }

    @Override
	public int hashCode() {
        int h = 1;
        for (int i = 0; i < size; i++) {
			h = 31*h + DefaultIntHashFunction.INSTANCE.hash(data[i]);
		}
        return h;
    }

    // ---------------------------------------------------------------
    //      Serialization
    // ---------------------------------------------------------------

    /**
     *  @serialData     Default fields; the capacity of the
     *                  list (<tt>int</tt>); the list's elements
     *                  (<tt>int</tt>).
     *
     *  @since          1.1
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(data.length);
        for (int i = 0; i < size; i++) {
			s.writeInt(data[i]);
		}
    }

    /**
     *  @since          1.1
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        data = new int[s.readInt()];
        for (int i = 0; i < size; i++) {
			data[i] = s.readInt();
		}
    }

}