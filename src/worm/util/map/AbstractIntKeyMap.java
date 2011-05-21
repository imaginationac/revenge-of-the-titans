/*
 *  Primitive Collections for Java.
 *  Copyright (C) 2003  Søren Bak
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

import worm.util.hash.DefaultIntHashFunction;

/**
 *  This class represents an abstract base for implementing
 *  maps from int values to objects. All operations that can be implemented
 *  using iterators
 *  are implemented as such. In most cases, this is
 *  hardly an efficient solution, and at least some of those
 *  methods should be overridden by sub-classes.
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.0     2003/10/1
 *  @since      1.0
 */
public abstract class AbstractIntKeyMap<V> implements IntKeyMap<V> {

    /** Default constructor to be invoked by sub-classes. */
    protected AbstractIntKeyMap() { }

    @Override
	public void clear() {
        IntKeyMapIterator<V> i = entries();
        while (i.hasNext()) {
            i.next();
            i.remove();
        }
    }

    @Override
	public V remove(int key) {
        IntKeyMapIterator<V> i = entries();
        while (i.hasNext()) {
            i.next();
            if (i.getKey() == key) {
                V value = i.getValue();
                i.remove();
                return value;
            }
        }
        return null;
    }

    @Override
	public void putAll(IntKeyMap<V> map) {
        IntKeyMapIterator<V> i = map.entries();
        while (i.hasNext()) {
            i.next();
            put(i.getKey(), i.getValue());
        }
    }

    @Override
	public boolean containsKey(int key) {
        IntKeyMapIterator<V> i = entries();
        while (i.hasNext()) {
            i.next();
            if (i.getKey() == key) {
				return true;
			}
        }
        return false;
    }

    @Override
	public V get(int key) {
        IntKeyMapIterator<V> i = entries();
        while (i.hasNext()) {
            i.next();
            if (i.getKey() == key) {
				return i.getValue();
			}
        }
        return null;
    }

    @Override
	public boolean containsValue(Object value) {
        IntKeyMapIterator<V> i = entries();
        if (value == null) {
            while (i.hasNext()) {
                i.next();
                if (value == null) {
					return true;
				}
            }
        } else {
            while (i.hasNext()) {
                i.next();
                if (value.equals(i.getValue())) {
					return true;
				}
            }
        }
        return false;
    }

    @Override
	public boolean equals(Object obj) {
        if (!(obj instanceof IntKeyMap)) {
			return false;
		}
        IntKeyMap<?> map = (IntKeyMap<?>) obj;
        if (size() != map.size()) {
			return false;
		}
        IntKeyMapIterator<V> i = entries();
        while (i.hasNext()) {
            i.next();
            int k = i.getKey();
            Object v = i.getValue();
            if (v == null) {
                if (map.get(k) != null) {
					return false;
				}
                if (!map.containsKey(k)) {
					return false;
				}
            } else {
                if (!v.equals(map.get(k))) {
					return false;
				}
            }
        }
        return true;
    }

    @Override
	public int hashCode() {
        int h = 0;
        IntKeyMapIterator<V> i = entries();
        while (i.hasNext()) {
            i.next();
            h += DefaultIntHashFunction.INSTANCE.hash(i.getKey()) ^ i.getValue().hashCode();
        }
        return h;
    }

    @Override
	public boolean isEmpty()
    { return size() == 0; }

    @Override
	public int size() {
        int size = 0;
        IntKeyMapIterator<V> i = entries();
        while (i.hasNext()) {
            i.next();
            size++;
        }
        return size;
    }

    /**
     *  Returns a string representation of this map.
     *
     *  @return     a string representation of this map.
     */
    @Override
	public String toString() {
        StringBuilder s = new StringBuilder();
        s.append('[');
        IntKeyMapIterator<V> i = entries();
        while (i.hasNext()) {
            if (s.length() > 1) {
				s.append(',');
			}
            i.next();
            s.append(String.valueOf(i.getKey()));
            s.append("->");
            s.append(String.valueOf(i.getValue()));
        }
        s.append(']');
        return s.toString();
    }

    /**
     *  Does nothing. Sub-classes may provide an implementation to
     *  minimize memory usage, but this is not required since many
     *  implementations will always have minimal memory usage.
     */
    public void trimToSize()
    { }

}