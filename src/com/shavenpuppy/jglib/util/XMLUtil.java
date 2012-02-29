/*
 * Copyright (c) 2003-onwards Shaven Puppy Ltd
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'Shaven Puppy' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.shavenpuppy.jglib.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.java.dev.eval.Expression;

import org.lwjgl.util.Color;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.Rectangle;
import org.lwjgl.util.vector.Vector3f;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.shavenpuppy.jglib.IResource;
import com.shavenpuppy.jglib.Point2f;
import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorParser;
import com.shavenpuppy.jglib.resources.DimensionParser;
import com.shavenpuppy.jglib.resources.Point2fParser;
import com.shavenpuppy.jglib.resources.PointParser;
import com.shavenpuppy.jglib.resources.RectangleParser;
import com.shavenpuppy.jglib.resources.TextWrapper;
import com.shavenpuppy.jglib.resources.Vector3fParser;

/**
 * Some simple XML utilities
 * @author cas
 */
public final class XMLUtil {

	/** A stash of variables which we automatically decode */
	private static final Map<String, String> vars = new HashMap<String, String>();

	/**
	 * No constructor; this is a static class
	 */
	private XMLUtil() {}

	/**
	 * Add a variable
	 * @param key The key
	 * @param value the value
	 */
	public static void putVar(String key, String value) {
		vars.put(key, value);
	}

	/**
	 * Decode a variable. Variables are in the form $varname. If the incoming
	 * expression is not a variable or is not recognised it is simply returned
	 * verbatim.
	 * @param in The incoming attribute
	 * @return String
	 * @throws Exception
	 */
	private static String decode(String in) throws Exception {
		if (in != null && in.length() > 1 && in.charAt(0) == '$') {
			String key = in.substring(1);
			if (key.charAt(0) == '#') {
				// It's a class name and reflection job in the form $$full.class.name.member
				int lastIdx = key.lastIndexOf('.');
				String member = key.substring(lastIdx + 1);
				String className = key.substring(1, lastIdx);
				Class<?> clazz = Class.forName(className);
				Field field = clazz.getDeclaredField(member);
				field.setAccessible(true);
				return String.valueOf(field.get(null));
			} else {
				String val = vars.get(key);
				if (val == null) {
					throw new Exception("Unknown variable "+in);
				} else {
					return val;
				}
			}
		} else {
			return in;
		}
	}

	/**
	 * Parse an expression made up of variables and simple operators. Incoming strings beginning with the =
	 * character are parsed as simple expressions from left to right.
	 * The expressions understood are +, -, *, / for numeric values and &amp; for string values (concatenator)
	 * @param in The incoming expression
	 * @return the final resolved value
	 * @throws Exception
	 */
	public static String parse(String in) throws Exception {
		if (in.length() < 2 || in.charAt(0) != '=') {
			return in;
		}

		String original = in;
		// Replace all variables recursively
		int dollarPosition;
		int minPos = 0;
		while ((dollarPosition = in.indexOf('$', minPos)) != -1) {
			if (dollarPosition > 0 && in.charAt(dollarPosition - 1) == '\\') {
				// It was an escaped dollar
				break;
			}
			// Scan to next operator
			int endPosition = in.length();
			boolean escaped = false;
			StringBuilder tokenBuilder = new StringBuilder(in.length());
			tokenBuilder.append("$");
			for (int i = dollarPosition + 1; i < in.length(); i ++) {
				char c = in.charAt(i);
				if (c == '\\') {
					if (escaped) {
						tokenBuilder.append(c);
						continue;
					} else {
						escaped = true;
					}
				}
				if (escaped) {
					continue;
				} else if (c == '+' || c == '-' || c == '/' || c == '*' || c == ',' || c == '$' || c == '(' || c == ')' || Character.isWhitespace(c)) {
					endPosition = i;
					break;
				} else {
					tokenBuilder.append(c);
				}
			}
			String token = tokenBuilder.toString();//in.substring(dollarPosition, endPosition);
			try {
				String decoded = decode(token);
				if (!decoded.equals(token)) {
					in = in.substring(0, dollarPosition) + decoded + in.substring(endPosition);
				} else {
					minPos = dollarPosition + 1;
				}
			} catch (Exception e) {
				System.err.println("Failed to parse "+original);
				throw e;
			}
		}


		StringBuilder ret = new StringBuilder(in.length() * 2);
		// First split the incoming string up into comma separated chunks, if any
		StringTokenizer st = new StringTokenizer(in.substring(1), ",");
		while (st.hasMoreTokens()) {
			String subElement = st.nextToken().trim();
			if (subElement.charAt(0) == '=') {
				subElement = subElement.substring(1);
			}
			// Replace all escaped chars with themselves by simply removing the \
			subElement = subElement.replace("\\", "");
			try {
				// If it's a string, simply use verbatim
				if (Character.isJavaIdentifierStart(subElement.charAt(0))) {
					ret.append(subElement);
				} else {
					ret.append(String.valueOf(Expression.eval(subElement)));
				}
			} catch (Exception e) {
				// It wasn't an expression after all. Or maybe there was an error
				ret.append(subElement);
				System.err.println("Failed to parse "+original+" (sent "+subElement+" to parser)");
//				throw e;
			}
			if (st.hasMoreTokens()) {
				ret.append(',');
			}
		}
		//System.out.println(in+" resolved to: "+ret.toString());
		return ret.toString();
	}

	/**
	 * @return true if the specified attribute is present and not empty or null in the element
	 */
	public static boolean hasAttribute(Element element, String attribute) {
		String s = element.getAttribute(attribute);
		if (s == null || "".equals(s)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Determine whether a single child is available of a particular type.
	 * @return true if the specified child element is present
	 * @throws Exception if the child is present multiple times.
	 */
	public static boolean hasChild(Element element, String child) throws Exception {
		NodeList nodes = element.getChildNodes();
		Element ret = null;
		for (int i = 0; i < nodes.getLength(); i ++) {
			Node childNode = nodes.item(i);
			if (childNode.getNodeName().equals(child) && childNode.getNodeType() == Node.ELEMENT_NODE) {
				if (ret != null) {
					throw new Exception("Child element '"+child+"' present multiple times");
				} else {
					ret = (Element) childNode;
				}
			}
		}
		return ret != null;
	}

	/**
	 * @param child
	 * @return the single child element or null
	 * @throws Exception if the child is present multiple times
	 */
	public static Element getChild(Element element, String child) throws Exception {
		NodeList nodes = element.getChildNodes();
		Element ret = null;
		for (int i = 0; i < nodes.getLength(); i ++) {
			Node childNode = nodes.item(i);
			if (childNode.getNodeName().equalsIgnoreCase(child) && childNode.getNodeType() == Node.ELEMENT_NODE) {
				if (ret != null) {
					throw new Exception("Child element '"+child+"' present multiple times");
				} else {
					ret = (Element) childNode;
				}
			}
		}
		if (ret == null) {
			return null;
		} else {
			return ret;
		}
	}

	/**
	 * @param child
	 * @return the single child element or null
	 * @throws Exception if the child is present multiple times
	 */
	public static Element getFirstChild(Element element, String child) throws Exception {
		NodeList nodes = element.getChildNodes();
		Element ret = null;
		for (int i = 0; i < nodes.getLength(); i ++) {
			Node childNode = nodes.item(i);
			if (childNode.getNodeName().equalsIgnoreCase(child) && childNode.getNodeType() == Node.ELEMENT_NODE) {
				ret = (Element) childNode;
				return ret;
			}
		}
		return null;
	}

	/**
	 * @param name The name of the child elements you want
	 * @return a List of child Elements
	 */
	public static List<Element> getChildren(Element element, String name) throws Exception {
		NodeList nodes = element.getChildNodes();
		ArrayList<Element> ret = new ArrayList<Element>(nodes.getLength());
		for (int i = 0; i < nodes.getLength(); i ++) {
			Node childNode = nodes.item(i);
			if (childNode.getNodeName().equals(name) && childNode.getNodeType() == Node.ELEMENT_NODE) {
				ret.add((Element) childNode);
			}
		}
		return ret;
	}

	/**
	 * @return a List of child Elements
	 */
	public static List<Element> getChildren(Element element) throws Exception {
		NodeList nodes = element.getChildNodes();
		ArrayList<Element> ret = new ArrayList<Element>(nodes.getLength());
		for (int i = 0; i < nodes.getLength(); i ++) {
			Node childNode = nodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				ret.add((Element) childNode);
			}
		}
		return ret;
	}

	/**
	 * A convenience method for getting boolean values out of XML elements.
	 * Booleans are true if the string value is "true", ignoring case.
	 * @param attribute The name of the attribute
	 * @throws NumberFormatException If the supplied attribute is not a number
	 * @throws Exception if the value is missing
	 * @return the parsed integer value
	 */
	public static boolean getBoolean(Element element, String attribute) throws Exception {
		String s = parse(element.getAttribute(attribute));
		if (s == null || "".equals(s)) {
			throw new Exception("Attribute '"+attribute+"' has not been specified for "+element.getNodeName());
		} else {
			return Boolean.valueOf(s).booleanValue();
		}
	}

	public static boolean getOptionalBoolean(Element element, String attribute, boolean defaultValue)
	{
		String s = element.getAttribute(attribute);
		if (s == null || "".equals(s)) {
			return defaultValue;
		} else {
			return Boolean.valueOf(s).booleanValue();
		}
	}

	/**
	 * A convenience method for getting boolean values out of XML elements.
	 * Booleans are true if the string value is "true", ignoring case.
	 * @param attribute The name of the attribute
	 * @param defaultValue The default value to return if no default is specified
	 * @throws NumberFormatException If the supplied attribute is not a number
	 * @return the parsed integer value
	 */
	public static boolean getBoolean(Element element, String attribute, boolean defaultValue) throws Exception {
		String s = parse(element.getAttribute(attribute));
		if (s == null || "".equals(s)) {
			return defaultValue;
		} else {
			return Boolean.valueOf(s).booleanValue();
		}
	}


	/**
	 * A convenience method for getting float values out of XML elements
	 * @param attribute The name of the attribute
	 * @throws NumberFormatException If the supplied attribute is not a number
	 * @throws Exception if the value is missing
	 * @return the parsed float value
	 */
	public static float getFloat(Element element, String attribute) throws Exception {
		String s = parse(element.getAttribute(attribute));
		if (s == null || "".equals(s)) {
			throw new Exception("Attribute '"+attribute+"' has not been specified for "+element.getNodeName());
		} else {
			return Float.parseFloat(s);
		}
	}


	/**
	 * A convenience method for getting float values out of XML elements
	 * @param attribute The name of the attribute
	 * @param defaultValue The default value to return if no default is specified
	 * @throws NumberFormatException If the supplied attribute is not a number
	 * @return the parsed float value
	 */
	public static float getFloat(Element element, String attribute, float defaultValue) throws Exception {
		String s = parse(element.getAttribute(attribute));
		if (s == null || "".equals(s)) {
			return defaultValue;
		} else {
			return Float.parseFloat(s);
		}
	}


	/**
	 * A convenience method for getting integer values out of XML elements
	 * @param attribute The name of the attribute
	 * @throws NumberFormatException If the supplied attribute is not a number
	 * @throws Exception if the value is missing
	 * @return the parsed integer value
	 */
	public static int getInt(Element element, String attribute) throws Exception {
		String s = parse(element.getAttribute(attribute));
		if (s == null || "".equals(s)) {
			throw new Exception("Attribute '"+attribute+"' has not been specified for "+element.getNodeName());
		} else
		{
			// Small hack to allow strings like "+1" to be recognised
			if (s.startsWith("+")) {
				s = s.substring(1);
			}
			return (int) (Float.parseFloat(s));
		}
	}

	public static int getOptionalInt(Element element, String attribute, int defaultValue)
	{
		if (element.hasAttribute(attribute))
		{
			String s = element.getAttribute(attribute);

			if (s.startsWith("+")) {
				s = s.substring(1);
			}

			return Integer.parseInt(s);
		}
		return defaultValue;
	}


	/**
	 * A convenience method for getting integer values out of XML elements
	 * @param attribute The name of the attribute
	 * @param defaultValue The default value to return if no default is specified
	 * @throws NumberFormatException If the supplied attribute is not a number
	 * @return the parsed integer value
	 */
	public static int getInt(Element element, String attribute, int defaultValue) throws Exception {
		String s = parse(element.getAttribute(attribute));
		if (s == null || "".equals(s)) {
			return defaultValue;
		} else {
			return (int) (Float.parseFloat(s));
		}
	}


	/**
	 * A convenience method for getting string values out of XML elements
	 * @param attribute The name of the attribute
	 * @return the string value, which will not be null
	 * @throws Exception the value is not specified
	 */
	public static String getString(Element element, String attribute) throws Exception {
		String s;
		try {
			s = parse(element.getAttribute(attribute));
		} catch (Exception e) {
			System.err.println("Failed to parse attribute '"+attribute+"' in element "+element.getNodeName());
			throw e;
		}
		if (s == null || "".equals(s)) {
			throw new Exception("Attribute '"+attribute+"' has not been specified for "+element.getNodeName());
		} else {
			return s;
		}
	}


	/**
	 * A convenience method for getting string values out of XML elements
	 * @param attribute The name of the attribute
	 * @param defaultValue The default value to return if no default is specified
	 * @return the string value, which will not be null
	 */
	public static String getString(Element element, String attribute, String defaultValue) throws Exception {
		String s = parse(element.getAttribute(attribute));
		if (s == null || "".equals(s)) {
			return defaultValue;
		} else {
			return s;
		}
	}


	/**
	 * Grab the text data inside a node. You can reference TextResources already loaded by using =<resource>
	 * @param defaultText The default text to use if none is present
	 * @return String
	 */
	public static String getText(Element element, String defaultText) throws Exception {
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i ++) {
			Node child = children.item(i);
			if (child instanceof Text) {
				Text text = (Text) child;
				String ret = text.getData().trim();
				if (ret.equals("")) {
					continue;
				}
				if (ret.startsWith("=")) {
					// It's a reference to a TextWrapper
					TextWrapper wrapper = (TextWrapper) Resources.get(ret.substring(1));
					if (wrapper == null) {
						throw new NullPointerException("Failed to find "+ret.substring(1)+" text resource.");
					}
					ret = wrapper.getText();
				} else {
					// Replace escape characters
					ret = ret.replace("\\n", "\n");
					ret = ret.replace("\\t", "\t");
////					ret = ret.replaceAll("\\\\n", "\n");
////					ret = ret.replaceAll("\\\\t", "\t");
				}
				return ret;
			}
		}
		return defaultText;
	}

	/**
	 * Use reflection to read XML attributes into simple instance variables of the same name.
	 * @param destination The object to read into
	 * @param root The root class that we should use
	 * @param element
	 * @throws Exception
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
    public static void grabXMLAttributes(Resource.Loader loader, Object destination, Class<?> root, Element element) throws Exception {
		Class<?> clazz = destination.getClass();
		while (root.isAssignableFrom(clazz)) {
			Field[] fields = clazz.getDeclaredFields();
			ArrayList<Field> sortedStringFields = new ArrayList<Field>(fields.length / 2 + 1);
			// Sort in descending order of field name length
			class LengthSorter implements Comparator<Field> {
				@Override
				public int compare(Field fa, Field fb) {
					int fal = fa.getName().length();
					int fbl = fb.getName().length();
					if (fal > fbl) {
						return -1;
					} else if (fal < fbl) {
						return 1;
					} else {
						return 0;
					}
				}
			}
			LengthSorter sorter = new LengthSorter();
			for (int i = 0; i < fields.length; i ++) {
				Field f = fields[i];
				// Set all fields accessible
				f.setAccessible(true);
				// Ignore a few well-known fields
				String fieldName = f.getName().toLowerCase();
				if (fieldName.equals("name") || fieldName.equals("class") || fieldName.equals("autoCreated") || fieldName.equals("inherit")) {
					continue;
				}

				// ordinary string fields:
				// If the field is final or static or transient then ignore it
				int modifiers = f.getModifiers();
				// Ignore final and static fields
				if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
					continue;
				}

				if (!Modifier.isTransient(modifiers) && f.getType() == String.class) {
					sortedStringFields.add(f);
				}
			}
			Collections.sort(sortedStringFields, sorter);


			for (int i = 0; i < fields.length; i ++) {
				// If the field is final then ignore it
				Field currentField = fields[i];
				if (Modifier.isFinal(currentField.getModifiers()) || Modifier.isStatic(currentField.getModifiers())) {
					continue;
				}

				if (Modifier.isTransient(currentField.getModifiers())) {
					// The special case of the transient field. There might be an embedded object in there, so instead of having to use a name
					// string and a named object elsewhere, we'll manufacture a name and do it all behind the scenes.

					// First: is it a resource?
					if (!IResource.class.isAssignableFrom(currentField.getType())) {
						continue;
					}

					// Second: is there a corresponding non-final non-transient non-static String field thats name is a prefix of this
					// field that's null?
					Field prefix = null;
					for (Iterator<Field> ssi = sortedStringFields.iterator(); ssi.hasNext(); ) {
						prefix = ssi.next();
						if (currentField.getName().toLowerCase().startsWith(prefix.getName().toLowerCase()) && prefix.get(destination) == null) {
							// Yay!
							break;
						} else {
							prefix = null;
						}
					}
					if (prefix == null) {
						// No good, can't find one.
						continue;
					}


					// Third: find a child element with the same name as the field. If there's one and only one, and it contains a matching
					// Resource, we'll do our magic.
					Element child = XMLUtil.getChild(element, prefix.getName().toLowerCase());
					if (child != null) {
						// Got a child. So now we'll load the child of the child
						List<Element> childrenOfChild = XMLUtil.getChildren(child);
						if (childrenOfChild.size() == 1) {
							// If there's an old resource, destroy it
							IResource oldResource = (IResource) currentField.get(destination);
							if (oldResource != null) {
								if (oldResource.isCreated()) {
									oldResource.destroy();
								}
								currentField.set(destination, null);
							}
							IResource resource = loader.load(childrenOfChild.get(0));
							if (currentField.getType().isAssignableFrom(resource.getClass())) {
								// OK. We've got a brand new Resource. Unfortunately it's transient so it won't be persisted here. What we'll do then is
								// stuff its manufactured name in the String field instead.
								prefix.set(destination, resource.getName());
								continue;
							} else {
								throw new Exception("Child resource "+resource+" not compatible with field "+currentField.getName()+" of "+destination);
							}
						}
					}


				}
				String attribute = XMLUtil.getString(element, currentField.getName(), null);

				// If not found, try to find an all lower case match
				if (attribute == null) {
					attribute = XMLUtil.getString(element, currentField.getName().toLowerCase(), null);
				}

				if (attribute == null) {
					// Is it a Resource?
					if (!IResource.class.isAssignableFrom(currentField.getType())) {
						continue;
					}
					/*
					 * It's not an attribute.. perhaps it's a child resource feature. These are
					 * present in the following format:
					 * <fieldname><childtag .... ></fieldname>
					 */
					Element child = XMLUtil.getChild(element, currentField.getName().toLowerCase());
					if (child != null) {
						// Got a child. So now we'll load the child of the child
						List<Element> childrenOfChild = XMLUtil.getChildren(child);
						if (childrenOfChild.size() == 0) {
							// No child present, so assume null
						} else if (childrenOfChild.size() == 1) {
							// If there's an old resource, destroy it
							IResource oldResource = (IResource) currentField.get(destination);
							if (oldResource != null) {
								if (oldResource.isCreated()) {
									oldResource.destroy();
								}
								currentField.set(destination, null);
							}
							IResource resource = loader.load(childrenOfChild.get(0));
							if (currentField.getType().isAssignableFrom(resource.getClass())) {
								currentField.set(destination, resource);
							} else {
								throw new Exception("Child resource "+resource+" not compatible with field "+currentField.getName()+" of "+destination);
							}
						} else {
							// Multiple children present! Best to ignore it.
							continue;
						}
						continue;
					}
					continue;
				}
				if (currentField.getType().equals(int.class)) {
					if (attribute.endsWith("fp")) {
						currentField.setInt(destination, FPMath.parse(attribute));
					} else {
						currentField.setInt(destination, (int) Float.parseFloat(attribute));
					}
				} else if (currentField.getType().equals(int[].class)) {
					currentField.set(destination, ArrayParser.parseInts(attribute));
				} else if (currentField.getType().equals(short.class)) {
					currentField.setShort(destination, (short) Float.parseFloat(attribute));
				} else if (currentField.getType().equals(byte.class)) {
					currentField.setByte(destination, (byte) Float.parseFloat(attribute));
				} else if (currentField.getType().equals(String.class)) {
					// Replace \n, \t etc
					if (attribute != null && attribute.indexOf("\\") != -1) {
						attribute = attribute.replace("\\n", "\n");
					}
					currentField.set(destination, attribute);
				} else if (currentField.getType().equals(float.class)) {
					currentField.setFloat(destination, Float.parseFloat(attribute));
				} else if (currentField.getType().equals(float[].class)) {
					currentField.set(destination, ArrayParser.parseFloats(attribute));
				} else if (currentField.getType().equals(double.class)) {
					currentField.setDouble(destination, Double.parseDouble(attribute));
				} else if (currentField.getType().equals(double[].class)) {
					currentField.set(destination, ArrayParser.parseDoubles(attribute));
				} else if (currentField.getType().equals(char.class)) {
					currentField.setChar(destination, attribute.charAt(0));
				} else if (currentField.getType().equals(boolean.class)) {
					currentField.setBoolean(destination, Boolean.valueOf(attribute).booleanValue());
				} else if (currentField.getType() == Rectangle.class) {
					currentField.set(destination, RectangleParser.parse(attribute));
				} else if (currentField.getType() == Point2f.class) {
					currentField.set(destination, Point2fParser.parse(attribute));
				} else if (currentField.getType() == Point.class) {
					currentField.set(destination, PointParser.parse(attribute));
				} else if (currentField.getType() == Vector3f.class) {
					currentField.set(destination, Vector3fParser.parse(attribute));
				} else if (currentField.getType() == Dimension.class) {
					currentField.set(destination, DimensionParser.parse(attribute));
				} else if (currentField.getType() == Color.class) {
					currentField.set(destination, ColorParser.parse(attribute));
				} else if (Enum.class.isAssignableFrom(currentField.getType())) {
					currentField.set(destination, Enum.valueOf((Class) currentField.getType(), attribute));
				} else if (Decodeable.class.isAssignableFrom(currentField.getType())) {
					Method decodeMethod = currentField.getType().getDeclaredMethod("decode", new Class[] {String.class});
					currentField.set(destination, decodeMethod.invoke(currentField.getType(), new Object[] {attribute}));
				} else if (Parseable.class.isAssignableFrom(currentField.getType())) {
					// It's a parseable object, so create a new instance of it
					Parseable parseable = (Parseable) currentField.getType().newInstance();
					// and decode it
					parseable.fromString(attribute);
					currentField.set(destination, parseable);
				} else {
					// Ignore this field - it's an Object type so we should handle it manually
				}
			}

			// Get next class up...
			clazz = clazz.getSuperclass();
		}
	}

	/**
	 * Scans through the list of child elements
	 * @param resource
	 * @param resourceElement
	 * @throws Exception
	 */
	public static void loadChildResources(IResource resource, Element resourceElement, Resource.Loader loader) throws Exception {
		// Get a list of the non-transient non-final non-static null Resource fields in the incoming Resource
		// and load elements that correspond to them.
		Class<?> clazz = resource.getClass();
		while (IResource.class.isAssignableFrom(clazz)) {
			Field[] fields = clazz.getDeclaredFields();
			for (int i = 0; i < fields.length; i ++) {
				Field f = fields[i];
				f.setAccessible(true);
				// If the field is final etc. then ignore it
				if (Modifier.isFinal(f.getModifiers()) || Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
					continue;
				}
				// If it's already set, ignore it
				Object value = f.get(resource);
				if (value != null) {
					continue;
				}
				// If it's not a Resource, ignore it
				if (!IResource.class.isAssignableFrom(f.getType())) {
					continue;
				}
				// Look for a child element with the same name
				Element childElement = getFirstChild(resourceElement, f.getName());
				if (childElement != null) {
					Resource newResource = (Resource) f.getType().newInstance();
					newResource.load(childElement, loader);
					f.set(resource, newResource);
				}

			}

			// Get next class up...
			clazz = clazz.getSuperclass();
		}
		getChildren(resourceElement);
	}

	public static float getNormalisedFloat(Element element, String string, float defaultValue) throws Exception
	{
		assert (defaultValue >= 0f && defaultValue <= 1f);

		float val = getFloat(element, string, defaultValue);

		val = val < 0f ? 0f : val;
		val = val > 1f ? 1f : val;
		return val;
	}
}
