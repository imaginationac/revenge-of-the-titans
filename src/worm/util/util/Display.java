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
package worm.util.util;

/**
 *  This class provides static methods for display of collection
 *  elements. It is only provided as a utility class for the collection
 *  implementations and is not a part of the API.
 *
 *  @author     S&oslash;ren Bak
 *  @version    1.2     21-08-2003 20:25
 */
public class Display {

    public static String display(int v) {
        return String.valueOf(v);
    }

    private static final String displayChars =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#¤%&/()=?\'@£${[]}+|^~*-_.:,;<>\\";

    static String hexChar(char v) {
        String s = Integer.toHexString(v);
        switch (s.length()) {
        case 1: return "\\u000"+s;
        case 2: return "\\u00"+s;
        case 3: return "\\u0"+s;
        case 4: return "\\u"+s;
        default:
            throw new RuntimeException("Internal error");
        }
    }

}