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

import worm.util.AbstractIntCollection;
import worm.util.IntCollection;
import worm.util.IntIterator;
import worm.util.hash.DefaultIntHashFunction;
import worm.util.util.Exceptions;

/**
 *  This class represents an abstract base for implementing
 *  lists of int values. All operations that can be implemented
 *  using iterators and the <tt>get()</tt> and <tt>set()</tt> methods
 *  are implemented as such. In most cases, this is
 *  hardly an efficient solution, and at least some of those
 *  methods should be overridden by sub-classes.
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.2     21-08-2003 19:14
 *  @since      1.0
 */
public abstract class AbstractIntList extends AbstractIntCollection implements IntList {

    /** Default constructor to be invoked by sub-classes. */
    protected AbstractIntList() { }

    @Override
	public boolean add(int v)
    { add(size(), v); return true; }

    /**
     *  Throws <tt>UnsupportedOperationException</tt>.
     *
     *  @throws     UnsupportedOperationException
     *              unconditionally.
     */
    @Override
	public void add(int index, int v)
    { Exceptions.unsupported("add"); }


    @Override
	public boolean addAll(int index, IntCollection c) {
        if (index < 0 || index > size()) {
			Exceptions.indexOutOfBounds(index, 0, size());
		}
        IntIterator i = c.iterator();
        boolean result = i.hasNext();
        while (i.hasNext()) {
            add(index, i.next());
            index++;
        }
        return result;
    }

    @Override
	public int indexOf(int c) {
        return indexOf(0, c);
    }

    /**
     *  @since      1.2
     */
    @Override
	public int indexOf(int index, int c) {
        IntListIterator i = listIterator(index);
        while (i.hasNext()) {
			if (i.next() == c) {
				return i.previousIndex();
			}
		}
        return -1;
    }

    @Override
	public IntIterator iterator()
    { return listIterator(); }

    @Override
	public int lastIndexOf(int c) {
        IntListIterator i = listIterator(size());
        while (i.hasPrevious()) {
			if (i.previous() == c) {
				return i.nextIndex();
			}
		}
        return -1;
    }

    @Override
	public int lastIndexOf(int index, int c) {
        IntListIterator i = listIterator(index);
        while (i.hasPrevious()) {
			if (i.previous() == c) {
				return i.nextIndex();
			}
		}
        return -1;
    }

    @Override
	public IntListIterator listIterator()
    { return listIterator(0); }

    @Override
	public IntListIterator listIterator(final int index) {
        if (index < 0 || index > size()) {
			Exceptions.indexOutOfBounds(index, 0, size());
		}

        return new IntListIterator() {
            private int ptr = index;
            private int lptr = -1;

            // -------------------------------------------------------
            //      Implementation of Iterator
            // -------------------------------------------------------

            @Override
			public boolean hasNext() {
                return ptr < size();
            }

            @Override
			public int next() {
                if (ptr == size()) {
					Exceptions.endOfIterator();
				}
                lptr = ptr++;
                return get(lptr);
            }

            @Override
			public void remove() {
                if (lptr == -1) {
					Exceptions.noElementToRemove();
				}
                AbstractIntList.this.removeElementAt(lptr);
                if (lptr < ptr) {
					ptr--;
				}
                lptr = -1;
            }

            // -------------------------------------------------------
            //      Implementation of ListIterator
            // -------------------------------------------------------

            @Override
			public void add(int v) {
                AbstractIntList.this.add(ptr++, v);
                lptr = -1;
            }

            @Override
			public boolean hasPrevious() {
                return ptr > 0;
            }

            @Override
			public int nextIndex()
            { return ptr; }

            @Override
			public int previous() {
                if (ptr == 0) {
					Exceptions.startOfIterator();
				}
                ptr--;
                lptr = ptr;
                return get(ptr);
            }

            @Override
			public int previousIndex()
            { return ptr-1; }

            @Override
			public void set(int v) {
                if (lptr == -1) {
					Exceptions.noElementToSet();
				}
                AbstractIntList.this.set(lptr, v);
            }

        };
    }

    /**
     *  Throws <tt>UnsupportedOperationException</tt>.
     *
     *  @throws     UnsupportedOperationException
     *              unconditionally.
     */
    @Override
	public int removeElementAt(int index)
    { Exceptions.unsupported("removeElementAt"); throw new RuntimeException(); }

    @Override
	public boolean equals(Object obj) {
        if (this == obj) {
			return true;
		}
        if (!(obj instanceof IntList)) {
			return false;
		}
        IntListIterator i1 = listIterator();
        IntListIterator i2 = ((IntList)obj).listIterator();
        while(i1.hasNext() && i2.hasNext()) {
			if (i1.next() != i2.next()) {
				return false;
			}
		}
        return !(i1.hasNext() || i2.hasNext());
    }

    @Override
	public int hashCode() {
        int h = 1;
        IntIterator i = iterator();
        while (i.hasNext()) {
			h = (31*h + DefaultIntHashFunction.INSTANCE.hash(i.next()));
		}
        return h;
    }

}