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

import java.util.NoSuchElementException;

import worm.util.IntIterator;


/**
 *  This class represents iterators over lists of int values.
 *
 *  @see        java.util.ListIterator
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.0     2002/29/12
 *  @since      1.0
 */
public interface IntListIterator extends IntIterator {

    /**
     *  Adds a specified element to the list at this iterator's
     *  current position.
     *
     *  @param      v
     *              the element to add.
     *
     *  @throws     UnsupportedOperationException
     *              if addition is not supported by this
     *              iterator.
     */
    void add(int v);

    /**
     *  Indicates whether more int values can be returned by this
     *  iterator by calling <tt>previous()</tt>.
     *
     *  @return     <tt>true</tt> if more int values can be returned
     *              by this iterator in backwards direction; returns
     *              <tt>false</tt> otherwise.
     *
     *  @see        #previous()
     */
    boolean hasPrevious();

    /**
     *  Returns the index of the element that would be returned by
     *  a call to <tt>next()</tt>.
     *
     *  @return     the index of the element that would be returned by
     *              a call to <tt>next()</tt>.
     *
     *  @see        #next()
     */
    int nextIndex();

    /**
     *  Returns the previous int value of this iterator.
     *
     *  @return     the previous int value of this iterator.
     *
     *  @throws     NoSuchElementException
     *              if no more elements are available from this
     *              iterator in backwards direction.
     *
     *  @see        #hasPrevious()
     */
    int previous();

    /**
     *  Returns the index of the element that would be returned by
     *  a call to <tt>previous()</tt>.
     *
     *  @return     the index of the element that would be returned by
     *              a call to <tt>previous()</tt>; if no more elements
     *              are available in backwards direction, <tt>-1</tt>
     *              is returned.
     *
     *  @see        #previous()
     */
    int previousIndex();

    /**
     *  Sets the last element returned to a specified value.
     *
     *  @param      v
     *              the new value of the element.
     *
     *  @throws     UnsupportedOperationException
     *              if replacement is not supported by this iterator.
     *
     *  @throws     IllegalStateException
     *              if no element has been returned by this iterator
     *              yet.
     */
    void set(int v);

}