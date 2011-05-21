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
package worm.util.util;

import java.util.NoSuchElementException;

import worm.util.map.NoSuchMappingException;


/**
 *  This class provides static methods for throwing exceptions.
 *  It is only provided as a utility class for the collection
 *  implementations and is not a part of the API.
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.0     21-08-2003 18:44
 */
public class Exceptions {

    public static void indexOutOfBounds(int index, int low, int high) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException("Index out of bounds: " + index + ", valid range is " + low + " to " + high);
    }

    public static void nullArgument(String name) throws NullPointerException {
        throw new NullPointerException("The specified " + name + " is null");
    }

    public static void negativeArgument(String name, Object value) throws IllegalArgumentException {
        throw new IllegalArgumentException(name + " cannot be negative: " + String.valueOf(value));
    }

    public static void negativeOrZeroArgument(String name, Object value) throws IllegalArgumentException {
        throw new IllegalArgumentException(name + " must be a positive value: " + String.valueOf(value));
    }

    // ---------------------------------------------------------------
    //      Iterator errors
    // ---------------------------------------------------------------

    public static void endOfIterator() throws NoSuchElementException {
        throw new NoSuchElementException("Attempt to iterate past iterator's last element.");
    }

    public static void startOfIterator() throws NoSuchElementException {
        throw new NoSuchElementException("Attempt to iterate past iterator's first element.");
    }

    public static void noElementToRemove() throws IllegalStateException {
        throw new IllegalStateException("Attempt to remove element from iterator that has no current element.");
    }

    public static void noElementToGet() throws IllegalStateException {
        throw new IllegalStateException("Attempt to get element from iterator that has no current element. Call next() first.");
    }

    public static void noElementToSet() throws IllegalStateException {
        throw new IllegalStateException("Attempt to set element in iterator that has no current element.");
    }

    // ---------------------------------------------------------------
    //      Map errors
    // ---------------------------------------------------------------

    public static void noLastElement() throws IllegalStateException {
        throw new IllegalStateException("No value to return. Call containsKey() first.");
    }

    public static void noSuchMapping(Object key) throws NoSuchMappingException {
        throw new NoSuchMappingException("No such key in map: " + String.valueOf(key));
    }

    // ---------------------------------------------------------------
    //      Deque errors
    // ---------------------------------------------------------------

    public static void dequeNoFirst() throws IndexOutOfBoundsException {
            throw new IndexOutOfBoundsException("Attempt to get first element of empty deque");
    }

    public static void dequeNoLast() throws IndexOutOfBoundsException {
            throw new IndexOutOfBoundsException("Attempt to get last element of empty deque");
    }

    public static void dequeNoFirstToRemove() throws IndexOutOfBoundsException {
            throw new IndexOutOfBoundsException("Attempt to remove last element of empty deque");
    }

    public static void dequeNoLastToRemove() throws IndexOutOfBoundsException {
            throw new IndexOutOfBoundsException("Attempt to remove last element of empty deque");
    }

    // ---------------------------------------------------------------
    //      Adapter value errors
    // ---------------------------------------------------------------

    public static void nullElementNotAllowed() throws IllegalArgumentException {
        throw new IllegalArgumentException("Attempt to add a null value to an adapted primitive set.");
    }

    public static void cannotAdapt(String name) throws IllegalStateException {
        throw new IllegalStateException("The " + name + " contains values preventing it from being adapted to a primitive " + name);
    }

    // ---------------------------------------------------------------
    //
    // ---------------------------------------------------------------

    public static void unsupported(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Attempt to invoke unsupported operation: " + name);
    }

    public static void unmodifiable(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Attempt to modify unmodifiable " + name);
    }

    public static void cloning() throws RuntimeException {
        throw new RuntimeException("Clone is not supported");
    }

    public static void invalidRangeBounds(Object first, Object last) throws IllegalArgumentException {
        throw new IllegalArgumentException("First ("+first+") cannot be greater than last ("+last+")");
    }

    public static void cannotMergeRanges(Object r1, Object r2) throws IllegalArgumentException {
        throw new IllegalArgumentException("Ranges cannot be merged: " + r1.toString() + " and " + r2.toString());
    }

    // ---------------------------------------------------------------
    //      Sorted set errors
    // ---------------------------------------------------------------

    public static void setNoFirst() throws NoSuchElementException {
        throw new NoSuchElementException("Attempt to get first element of empty set");
    }

    public static void setNoLast() throws NoSuchElementException {
        throw new NoSuchElementException("Attempt to get last element of empty set");
    }

    public static void invalidSetBounds(Object low, Object high) throws IllegalArgumentException {
        throw new IllegalArgumentException("Lower bound ("+low+") cannot be greater than upper bound ("+high+")");
    }

    public static void valueNotInSubRange(Object value) throws IllegalArgumentException {
        throw new IllegalArgumentException("Attempt to add a value outside valid range: " + value);
    }

    public static void invalidUpperBound(Object value) throws IllegalArgumentException {
        throw new IllegalArgumentException("Upper bound is not in valid sub-range: " + value);
    }

    public static void invalidLowerBound(Object value) throws IllegalArgumentException {
        throw new IllegalArgumentException("Lower bound is not in valid sub-range: " + value);
    }



}