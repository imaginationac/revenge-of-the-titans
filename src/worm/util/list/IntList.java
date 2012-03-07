/*
 *  Primitive Collections for Java.
 *  Copyright (C) 2002  Søren Bak
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

import worm.util.IntCollection;

/**
 *  This interface represents lists of int values.
 *
 *  @see        java.util.List
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.2     24-08-2003 20:34
 *  @since      1.0
 */
public interface IntList extends IntCollection {

    /**
     *  Adds an element to this list at a specified index. All
     *  elements from the specified index and forward are pushed
     *  to their successor's indices.
     *
     *  @param      index
     *              the index at which to add the element. If
     *              <tt>index == size()</tt> the element is appended
     *              to this list.
     *
     *  @param      v
     *              the int value to add to this list.
     *
     *  @throws     UnsupportedOperationException
     *              if the operation is not supported by this
     *              list.
     *
     *  @throws     IndexOutOfBoundsException
     *              if <tt>index</tt> does not denote a valid insertion
     *              position (valid: <tt>0 - size()</tt>).
     *
     *  @see        #add(int)
     *  @see        #addAll(IntCollection)
     *  @see        #addAll(int,IntCollection)
     */
    void add(int index, int v);

    /**
     *  Adds all the elements of a specified collection to
     *  this list starting at a specified index. The elements are
     *  inserted in the specified collection's iteration order.
     *  All elements from the specified index and forward are pushed
     *  to their successors' indices (<tt>c.size()</tt> indices).
     *
     *  @param      index
     *              the index at which to insert the elements of
     *              the specified collection. If
     *              <tt>index == size()</tt> the elements are appended
     *              to this list.
     *
     *  @param      c
     *              the collection whose elements to add to this
     *              list.
     *
     *  @return     <tt>true</tt> if this list was modified
     *              as a result of adding the elements of <tt>c</tt>;
     *              returns <tt>false</tt> otherwise.
     *
     *  @throws     UnsupportedOperationException
     *              if the operation is not supported by this
     *              list.
     *
     *  @throws     NullPointerException
     *              if <tt>c</tt> is <tt>null</tt>.
     *
     *  @throws     IndexOutOfBoundsException
     *              if <tt>index</tt> does not denote a valid insertion
     *              position (valid: <tt>0 - size()</tt>).
     *
     *  @see        #add(int)
     *  @see        #add(int, int)
     *  @see        #addAll(IntCollection)
     */
    boolean addAll(int index, IntCollection c);

    /**
     *  Returns the element at a specified position in this list.
     *
     *  @param      index
     *              the position of the element to return.
     *
     *  @return     the element at the specified position.
     *
     *  @throws     IndexOutOfBoundsException
     *              if <tt>index</tt> does not denote a valid index
     *              in this list.
     */
    int get(int index);

    /**
     *  Returns the index of the first occurance of a specified
     *  element in this list.
     *
     *  @param      c
     *              the element to find.
     *
     *  @return     the index of the first occurance of the specified
     *              element in this list; returns <tt>-1</tt>, if the
     *              element is not contained in this list.
     */
    int indexOf(int c);

    /**
     *  Returns the index of the first occurance of a specified
     *  element in this list after or at a specified index.
     *
     *  @param      c
     *              the element to find.
     *
     *  @param      index
     *              the index at which to start the search.
     *
     *  @return     the index of the first occurance of the specified
     *              element in this list; returns <tt>-1</tt>, if the
     *              element is not contained in this list.
     *
     *  @throws     IndexOutOfBoundsException
     *              if <tt>index</tt> does not denote a valid
     *              iteration position (valid: <tt>0 - size()</tt>).
     *
     *  @since      1.2
     */
    int indexOf(int index, int c);

    /**
     *  Returns the index of the last occurance of a specified
     *  element in this list.
     *
     *  @param      c
     *              the element to find.
     *
     *  @return     the index of the last occurance of the specified
     *              element in this list; returns <tt>-1</tt>, if the
     *              element is not contained in this list.
     */
    int lastIndexOf(int c);

    /**
     *  Returns the index of the last occurance of a specified
     *  element in this list before a specified index.
     *
     *  @param      c
     *              the element to find.
     *
     *  @param      index
     *              the index at which to start the search. Note that
     *              the element at <code>index</code> is not included
     *              in the search.
     *
     *  @return     the index of the last occurance of the specified
     *              element in this list; returns <tt>-1</tt>, if the
     *              element is not contained in this list.
     *
     *  @throws     IndexOutOfBoundsException
     *              if <tt>index</tt> does not denote a valid
     *              iteration position (valid: <tt>0 - size()</tt>).
     *
     *  @since      1.2
     */
    int lastIndexOf(int index, int c);

    /**
     *  Returns a list iterator over this list.
     *
     *  @return     a list iterator over this list.
     */
    IntListIterator listIterator();

    /**
     *  Returns a list iterator over this list, starting from a
     *  specified index.
     *
     *  @param      index
     *              the index at which to begin the iteration.
     *
     *  @return     a list iterator over this list.
     *
     *  @throws     IndexOutOfBoundsException
     *              if <tt>index</tt> does not denote a valid
     *              iteration position (valid: <tt>0 - size()</tt>).
     */
    IntListIterator listIterator(int index);

    /**
     *  Removes the element at a specified index in this list. All
     *  elements following the removed element are pushed to their
     *  predecessor's indices.
     *
     *  @param      index
     *              the index of the element to remove.
     *
     *  @return     the value of the element removed.
     *
     *  @throws     UnsupportedOperationException
     *              if the operation is not supported by this
     *              list.
     *
     *  @throws     IndexOutOfBoundsException
     *              if <tt>index</tt> does not denote a valid
     *              element position (valid: <tt>0 - size()-1</tt>).
     */
    int removeElementAt(int index);

    /**
     *  Sets a specified element to a new value.
     *
     *  @param      index
     *              the index of the element whose value to set.
     *
     *  @param      v
     *              the new value of the specified element.
     *
     *  @return     the previous value of the element.
     *
     *  @throws     UnsupportedOperationException
     *              if the operation is not supported by this
     *              list.
     *
     *  @throws     IndexOutOfBoundsException
     *              if <tt>index</tt> does not denote a valid
     *              element position (valid: <tt>0 - size()-1</tt>).
     */
    int set(int index, int v);

}