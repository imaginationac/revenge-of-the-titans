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
 *  This class implements methods for retrieving default values for
 *  each of the primitive types. The default values are returned by
 *  the maps' <tt>get()</tt>-methods when a specified key does not
 *  map to any value.
 *
 *  <p>Note: Later versions may provide the ability to configure
 *  the default values returned by maps.
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.0     2002/29/12
 *  @since      1.0
 */
public class MapDefaults {

    /**
     *  Returns a default int value (<tt>0</tt>).
     *
     *  @return     a default int value (<tt>0</tt>).
     */
    public static int defaultInt()
    { return 0; }

}