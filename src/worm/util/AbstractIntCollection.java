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
package worm.util;

import worm.util.util.Exceptions;

/**
 *  This class represents an abstract base for implementing
 *  collections of int values. All operations that can be implemented
 *  using iterators are implemented as such. In most cases, this is
 *  hardly an efficient solution, and at least some of those
 *  methods should be overridden by sub-classes.
 *
 *  <p>In this implementation, <tt>size()</tt> is calculated by
 *  iterating over the collection. Make sure that <tt>size()</tt>
 *  is overwritten or that iterators do not depend on the
 *  <tt>size()</tt> method.
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.3     21-08-2003 20:16
 *  @since      1.0
 */
public abstract class AbstractIntCollection implements IntCollection {

    /** Default constructor to be invoked by sub-classes. */
    protected AbstractIntCollection() { }

    /**
     *  Throws <tt>UnsupportedOperationException</tt>.
     *
     *  @throws     UnsupportedOperationException
     *              unconditionally.
     */
    @Override
	public boolean add(int v)
    { Exceptions.unsupported("add"); return false; }

    @Override
	public boolean addAll(IntCollection c) {
        IntIterator i = c.iterator();  //  Throws NullPointerException
        boolean result = false;
        while (i.hasNext()) {
			result = result | add(i.next());
		}
        return result;
    }

    @Override
	public void clear() {
        IntIterator i = iterator();
        while (i.hasNext()) {
            i.next();
            i.remove();
        }
    }

    @Override
	public boolean contains(int v) {
        IntIterator i = iterator();
        while (i.hasNext()) {
			if (i.next() == v) {
				return true;
			}
		}
        return false;
    }

    @Override
	public boolean containsAll(IntCollection c) {
        IntIterator i = c.iterator();  //  Throws NullPointerException
        while (i.hasNext()) {
			if (!contains(i.next())) {
				return false;
			}
		}
        return true;
    }

    @Override
	public boolean isEmpty()
    { return size() == 0; }

    @Override
	public boolean remove(int v) {
        IntIterator i = iterator();
        boolean result = false;
        while (i.hasNext()) {
            if (i.next() == v) {
                i.remove();
                result = true;
                break;
            }
        }
        return result;
    }

    @Override
	public boolean removeAll(IntCollection c) {
        if (c == null) {
			Exceptions.nullArgument("collection");
		}
        IntIterator i = iterator();
        boolean result = false;
        while (i.hasNext()) {
            if (c.contains(i.next())) {
                i.remove();
                result = true;
            }
        }
        return result;
    }

    @Override
	public boolean retainAll(IntCollection c) {
        if (c == null) {
			Exceptions.nullArgument("collection");
		}
        IntIterator i = iterator();
        boolean result = false;
        while (i.hasNext()) {
            if (!c.contains(i.next())) {
                i.remove();
                result = true;
            }
        }
        return result;
    }

    @Override
	public int size() {
        IntIterator i = iterator();
        int size = 0;
        while (i.hasNext()) {
            i.next();
            size++;
        }
        return size;
    }

    @Override
	public int[] toArray() {
        return toArray(null);
    }

    @Override
	public int[] toArray(int[] a) {
        int size = size();
        if (a == null || a.length < size) {
			a = new int[size];
		}
        IntIterator i = iterator();
        int index = 0;
        while (i.hasNext()) {
            a[index] = i.next();
            index++;
        }
        return a;
    }

    /**
     *  Does nothing. Sub-classes may provide an implementation to
     *  minimize memory usage, but this is not required since many
     *  implementations will always have minimal memory usage.
     */
    @Override
	public void trimToSize()
    { }

    /**
     *  Returns a string representation of this collection.
     *
     *  @return     a string representation of this collection.
     */
    @Override
	public String toString() {
        StringBuilder s = new StringBuilder();
        s.append('[');
        IntIterator i = iterator();
        while (i.hasNext()) {
            if (s.length() > 1) {
				s.append(',');
			}
            s.append(worm.util.util.Display.display(i.next()));
        }
        s.append(']');
        return s.toString();
    }

}