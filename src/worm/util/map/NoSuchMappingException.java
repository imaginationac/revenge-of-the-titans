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
package worm.util.map;

/**
 *  Thrown to indicate that an attempt was made to retrieve a
 *  non-existing mapping in a map.
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.0     2002/30/12
 *  @since      1.0
 */
public class NoSuchMappingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
     *  Creates a new exception with a specified detail message.
     *  The message indicates the key of the mapping that was
     *  not available.
     *
     *  @param      s
     *              the detail message.
     *
     *  @throws     NullPointerException
     *              if <tt>s</tt> is <tt>null</tt>.
     */
    public NoSuchMappingException(String s) {
        super(s);
    }

}