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
package worm.util;

import java.util.NoSuchElementException;

/**
 *  This class represents iterators over collections of int values.
 *
 *  @see        java.util.Iterator
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.0     2002/29/12
 *  @since      1.0
 */
public interface IntIterator {

    /**
     *  Indicates whether more int values can be returned by this
     *  iterator.
     *
     *  @return     <tt>true</tt> if more int values can be returned
     *              by this iterator; returns <tt>false</tt>
     *              otherwise.
     *
     *  @see        #next()
     */
    boolean hasNext();

    /**
     *  Returns the next int value of this iterator.
     *
     *  @return     the next int value of this iterator.
     *
     *  @throws     NoSuchElementException
     *              if no more elements are available from this
     *              iterator.
     *
     *  @see        #hasNext()
     */
    int next();

    /**
     *  Removes the last int value returned from the underlying
     *  collection.
     *
     *  @throws     UnsupportedOperationException
     *              if removal is not supported by this iterator.
     *
     *  @throws     IllegalStateException
     *              if no element has been returned by this iterator
     *              yet.
     */
    void remove();

}