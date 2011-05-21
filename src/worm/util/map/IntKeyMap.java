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
package worm.util.map;

import java.util.Collection;

import worm.util.set.IntSet;

/**
 *  This interface represents maps from int values to objects.
 *
 *  @see        java.util.Map
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.1     2003/6/1
 *  @since      1.0
 */
public interface IntKeyMap<V> {

    /**
     *  Clears this map.
     */
    void clear();

    /**
     *  Indicates whether this map contains a mapping from a specified
     *  key.
     *
     *  @param      key
     *              the key to test for.
     *
     *  @return     <tt>true</tt> if this map contains a mapping from
     *              the specified key; returns <tt>false</tt>
     *              otherwise.
     */
    boolean containsKey(int key);

    /**
     *  Indicates whether this map contains a mapping to a specified
     *  value.
     *
     *  @param      value
     *              the value to test for.
     *
     *  @return     <tt>true</tt> if this map contains at least one
     *              mapping to the specified value; returns
     *              <tt>false</tt> otherwise.
     */
    boolean containsValue(Object value);

    /**
     *  Returns an iterator over the entries of this map. It is
     *  possible to remove entries from this map using the iterator
     *  provided that the concrete map supports removal of
     *  entries.
     *
     *  @return     an iterator over the entries of this map.
     */
    IntKeyMapIterator<V> entries();

    /**
     *  Indicates whether this map is equal to some object.
     *
     *  @param      obj
     *              the object with which to compare this map.
     *
     *  @return     <tt>true</tt> if this map is equal to the
     *              specified object; returns <tt>false</tt>
     *              otherwise.
     */
    @Override
	boolean equals(Object obj);

    /**
     *  Maps a specified key to a value.
     *
     *  @param      key
     *              the key to map to a value.
     *
     *  @return     the value that the specified key maps to; returns
     *              <tt>null</tt>, if no mapping exists for the
     *              specified key.
     */
    V get(int key);

    /**
     *  Returns a hash code value for this map.
     *
     *  @return     a hash code value for this map.
     */
    @Override
	int hashCode();

    /**
     *  Indicates whether this map is empty.
     *
     *  @return     <tt>true</tt> if this map is empty; returns
     *              <tt>false</tt> otherwise.
     */
    public boolean isEmpty();

    /**
     *  Returns a set view of the keys of this map. The set is not
     *  directly modifiable, but changes to the map are reflected in
     *  the set.
     *
     *  @return     a set view of the keys of this map.
     */
    IntSet keySet();

    /**
     *  Adds a mapping from a specified key to a specified value to
     *  this map. If a mapping already exists for the specified key
     *  it is overwritten by the new mapping.
     *
     *  @param      key
     *              the key of the mapping to add to this map.
     *
     *  @param      value
     *              the value of the mapping to add to this map.
     *
     *  @return     the old value (which can be <tt>null</tt>) if a
     *              mapping from the specified key already existed
     *              in this map; returns <tt>null</tt> otherwise.
     *
     *  @throws     UnsupportedOperationException
     *              if the operation is not supported by this map.
     */
    V put(int key, V value);

    /**
     *  Adds all mappings from a specified map to this map. Any
     *  existing mappings whose keys collide with a new mapping is
     *  overwritten by the new mapping.
     *
     *  @param      map
     *              the map whose mappings to add to this map.
     *
     *  @throws     NullPointerException
     *              if <tt>map</tt> is <tt>null</tt>.
     *
     *  @throws     UnsupportedOperationException
     *              if the operation is not supported by this map.
     */
    void putAll(IntKeyMap<V> map);

    /**
     *  Removes the mapping from a specified key from this map.
     *
     *  @param      key
     *              the key whose mapping to remove from this map.
     *
     *  @return     the old value (which can be <tt>null</tt>) if a
     *              mapping from the specified key already existed
     *              in this map; returns <tt>null</tt> otherwise.
     *
     *  @throws     UnsupportedOperationException
     *              if the operation is not supported by this map.
     */
    V remove(int key);

    /**
     *  Returns the size of this map. The size is defined as the
     *  number of mappings from keys to values.
     *
     *  @return     the size of this map.
     */
    int size();

    /**
     *  Returns a collection view of the values in this map. The
     *  collection is not modifiable, but changes to the map are
     *  reflected in the collection.
     *
     *  @return     a collection view of the values in this map.
     */
    Collection<V> values();

}