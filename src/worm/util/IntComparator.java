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

/**
 *  This interface represents comparators of int values. The
 *  comparator interface is used for defining new orderings for
 *  int values.
 *
 *  @see        java.util.Comparator
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.0     2002/29/12
 *  @since      1.0
 */
public interface IntComparator {

    /**
     *  Compares two int values for order.
     *
     *  @param      v1
     *              the first int value in the comparison.
     *
     *  @param      v2
     *              the second int value in the comparison.
     *
     *  @return     a negative int value if <tt>v1 &lt; v2</tt>,
     *              <tt>0</tt> if <tt>v1</tt> is equal to <tt>v2</tt>,
     *              or a positive integer if <tt>v1 &gt; v2</tt>.
     */
    int compare(int v1, int v2);

}