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
package worm.util.hash;

/**
 *  This interface represents hash functions from int values
 *  to int values. The int value result is chosen to achieve
 *  consistence with the common
 *  {@link Object#hashCode() hashCode()}
 *  method. The interface is provided to alter the hash functions used
 *  by hashing data structures, like
 *  {@link bak.pcj.map.IntKeyIntChainedHashMap IntKeyIntChainedHashMap}
 *  or
 *  {@link bak.pcj.set.IntChainedHashSet IntChainedHashSet}.
 *
 *  @see        DefaultIntHashFunction
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.0     2002/29/12
 *  @since      1.0
 */
public interface IntHashFunction {

    /**
     *  Returns a hash code for a specified int value.
     *
     *  @param      v
     *              the value for which to return a hash code.
     *
     *  @return     a hash code for the specified value.
     */
    int hash(int v);

}