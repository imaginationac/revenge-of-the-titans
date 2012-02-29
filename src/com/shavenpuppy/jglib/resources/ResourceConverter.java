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
package com.shavenpuppy.jglib.resources;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.shavenpuppy.jglib.IResource;
import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Resource converter. Reads in resources XML file and writes out a serialized
 * version. Usage: ResourceConverter <input file> <output file>
 *
 * @author foo
 */
public class ResourceConverter implements Resource.Loader {

	private static final boolean DEBUG = false;

	/** For artificially generating names */
	private static int uniqueNameCounter;

	static final Class<?>[] namedSig = new Class[] {String.class};
	static final Class<?>[] unnamedSig = new Class[] {};

	private Map<String, Class<? extends IResource>> typeMap = new HashMap<String, Class<? extends IResource>>();

	// A stack of temporary mappings
	Stack<Map<String, Class<? extends IResource>>> stack = new Stack<Map<String, Class<? extends IResource>>>();

	/** Overwrite mode */
	private boolean overwrite;

	/** Track included files */
	private final Set<String> included = new HashSet<String>();

	private ResourceLoadedListener loadedListener;

	/** Manufactured resource names */
	private final Stack<String> resourcePath = new Stack<String>();

	/** Classloader */
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	/**
	 * Constructor
	 */
	public ResourceConverter() {
	}

	public ResourceConverter(ResourceLoadedListener loadedListener, ClassLoader classLoader) {
		this.loadedListener = loadedListener;
		this.classLoader = classLoader;
	}

	/**
	 * Constructor for ResourceConverter.
	 */
	public ResourceConverter(String resourceName) throws Exception {
		include(resourceName);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		try {
			if (args.length != 2) {
				System.err.println("Usage: ResourceConverter <classpath-xml-resource> <destpath>");
				System.exit(-1);
			}
			new ResourceConverter(args[0]); // warning suppressed
			Resources.save(new FileOutputStream(args[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reset the included files, so you can load them in again.
	 */
	public void resetIncludes() {
		included.clear();
	}

	private void log(String msg) {
		if (DEBUG) {
			System.err.println(msg);
		}
	}

	/**
	 * Include a resources file. If the file has already been included it is not
	 * included again.
	 *
	 * @param resourceName the resource name which is the XML file
	 */
	public void include(String resourceName) throws Exception {
		log("Including resources from:" + resourceName);

		// Path starts here
		try {
			resourcePath.push(resourceName);

			// First attempt: search classpath
			InputStream is;
			is = classLoader.getResourceAsStream(resourceName);
			if (is == null) {
				ClassLoader l = Thread.currentThread().getContextClassLoader();
				is = l.getResourceAsStream(resourceName);
			}

			if (is == null) {
				// Second attempt: search working directory
				is = new BufferedInputStream(new FileInputStream(resourceName));
			}

			try {
				include(is);
			} catch (Exception e) {
				throw new Exception("Failed to include '"+resourceName+"'", e);
			}
		} finally {
			// Pop path
			resourcePath.pop();
		}
	}

	/**
	 * Include resources directly from a snippet
	 */
	public void load(String input) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new ByteArrayInputStream(input.getBytes()));

		Element root = document.getDocumentElement();

		load(root);
	}

	/**
	 * Include resources from the specified input stream
	 *
	 * @param is An XML input stream
	 */
	@Override
	public void include(InputStream is) throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(is);

		Element root = document.getDocumentElement();

		String name = XMLUtil.getString(root, "name", null);
		if (name != null) {
			if (included.contains(name)) {
				return;
			}
			included.add(name);
			resourcePath.push(name);
		}

		// Having determined the mappings from this file we can then continue
		// by loading all the tags we find.
		NodeList childNodeList = root.getChildNodes();
		for (int i = 0; i < childNodeList.getLength(); i++) {
			Node node = childNodeList.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element featureElement = (Element) node;
			load(featureElement);
		}

		if (name != null) {
			resourcePath.pop();
		}

	}

	/**
	 * Conditional loading.
	 */
	private void ifDef(Element element, boolean match) throws Exception {
		String key = element.getAttribute("key");
		String value = element.getAttribute("value");

		log("Checking condition: " + key + "=" + value + "...");
		if (System.getProperty(key, "<undefined>").equals(value) == match) {
			log("true");
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child instanceof Element) {
					load((Element) child);
				}
			}
		} else {
			log("false");
		}
	}

	public ClassLoader getClassLoader() {
	    return classLoader;
    }

	/**
	 * @see com.shavenpuppy.jglib.Resource.Loader#load(org.w3c.dom.Element)
	 */
	@Override
	public IResource load(Element featureElement) throws Exception {
		if (featureElement.getNodeName().equals("include")) {
			String name = featureElement.getAttribute("resource");
			if (name == null) {
				throw new Exception("Missing 'resource' attribute in include tag");
			}
			log("Including " + name);
			include(name);
			return null;
		} else if (featureElement.getNodeName().equals("map")) {
			String tag = featureElement.getAttribute("tag");
			if (tag == null || "".equals(tag)) {
				throw new Exception("Missing 'tag' attribute");
			} else if (tag.equals("map")) {
				throw new Exception("Cannot reassign 'map' tag");
			}

			String className = featureElement.getAttribute("class");
			if (className == null || "".equals(className)) {
				throw new Exception("Missing 'class' attribute");
			}

			Class<? extends Resource> clazz = (Class<? extends Resource>) Class.forName(className, true, getClassLoader());
			if (!Resource.class.isAssignableFrom(clazz)) {
				throw new Exception(clazz + " is not a Resource.");
			}
			Class<? extends IResource> oldClazz = typeMap.get(tag);
			typeMap.put(tag, clazz);
			Resources.registerTag(clazz, tag); // useful
			if (oldClazz == null) {
				log("Mapping <" + tag + "> to " + className);
			} else if (oldClazz != clazz) {
				log("Re-mapping <" + tag + "> to " + className+" : old mapping was "+oldClazz.getName());
			}

			return null;
		} else if (featureElement.getNodeName().equals("instance")) {
			String className = featureElement.getAttribute("class");
			if (className == null || "".equals(className)) {
				throw new Exception("Missing 'class' attribute");
			}

			Class<? extends Resource> clazz = (Class<? extends Resource>) Class.forName(className, true, getClassLoader());
			if (!Resource.class.isAssignableFrom(clazz)) {
				throw new Exception(clazz + " is not a Resource.");
			}

			return loadInstance(true, featureElement, clazz);
		} else if (featureElement.getNodeName().equals("ifdef")) {
			ifDef(featureElement, true);
			return null;
		} else if (featureElement.getNodeName().equals("ifndef")) {
			ifDef(featureElement, false);
			return null;
		} else if (featureElement.getNodeName().equals("property")) {
			XMLUtil.putVar(XMLUtil.getString(featureElement, "key"), XMLUtil.getString(featureElement, "value"));
			return null;
		}

		Class<? extends IResource> featureClass = typeMap.get(featureElement.getTagName());
		if (featureClass == null) {
			featureClass = Resources.getMapping(featureElement.getTagName());
			if (featureClass == null) {
				throw new Exception("No mapping specified for " + featureElement.getTagName());
			}
		}

		return loadInstance(false, featureElement, featureClass);
	}

	/**
	 * Load an instance of the class specified
	 */
	private IResource loadInstance(boolean isInstance, Element featureElement, Class<? extends IResource> featureClass) throws Exception {
		// Check that we're creating something appropriate...
		assert Resource.class.isAssignableFrom(featureClass) : featureClass.getName() + " is not a Resource";
		Constructor<? extends IResource> namedCtor = null, unnamedCtor = null;
		boolean canBeUnNamed = false, canBeNamed = false, named = false;
		try {
			namedCtor = featureClass.getConstructor(namedSig);
			canBeNamed = true;
		} catch (NoSuchMethodException e) {
		}
		try {
			unnamedCtor = featureClass.getConstructor(unnamedSig);
			canBeUnNamed = true;
		} catch (NoSuchMethodException e) {
		}

		if (!canBeNamed && !canBeUnNamed) {
			throw new Exception("No suitable constructor found for " + featureElement.getTagName());
		}

		// Construct a new instance
		try {
			IResource newFeature = null;
			String name = null;
			name = featureElement.getAttribute("name");
			named = !"".equals(name);
			boolean exists = false, wasCreated = false;
			// If in overwrite mode, check to see if a named resource already
			// exists
			if (overwrite && named) {
				exists = Resources.exists(name);
				if (exists) {
					// It already exists, so we'll re-use it. If it is already
					// created it
					// must first be destroyed:
					newFeature = Resources.peek(name);
					// Check the resource is not locked
					if (newFeature.isLocked()) {
						// It's locked so don't load
						return newFeature;
					} else {
						log("Reloading " + newFeature);
					}
					wasCreated = newFeature.isCreated();
					if (wasCreated) {
						newFeature.destroy();
					}
					newFeature.deregister();
				}
			}

			if (newFeature == null) {
				if (canBeNamed && named && namedCtor != null) {
					newFeature = namedCtor.newInstance(new Object[] {name});
				} else if (canBeUnNamed && !named && unnamedCtor != null) {
					newFeature = unnamedCtor.newInstance(new Object[] {});
				} else if (canBeNamed && !named) {
					throw new Exception("Resource has no 'name' attribute (a " + featureElement.getTagName() + "/"
							+ featureClass.getName() + ")");
				} else if (canBeUnNamed && named) {
					throw new Exception("Resource cannot be named (a " + featureClass.getName() + ")");
				} else {
					throw new Exception("Something strange happened.");
				}
			}

			if (!named) {
				// Construct a name if we ain't got one
				StringBuilder sb = new StringBuilder(64);
//				for (String s : resourcePath) {
//					sb.append(s);
//					sb.append('.');
//				}
				sb.append(newFeature.getClass().getName());
				sb.append(uniqueNameCounter++);
				newFeature.setName(sb.toString());
			}

			// Load it from the element node
			try {
				newFeature.load(featureElement, this);
			} catch (Exception e) {
				System.err.println("Failed to load " + newFeature + " due to " + e);
				throw e;
			}

			if (!newFeature.isSubResource()) {
				if (exists) {
					newFeature.register();
					if (wasCreated) {
						newFeature.create();
					}
					if (loadedListener != null) {
						loadedListener.resourceLoaded(newFeature); // TODO:
						// Should be
						// reloaded()?
					}
				} else {
					// And stash it in the Resources
					if (!overwrite && Resources.exists(name)) {
						throw new Exception("Resource " + newFeature + " has already been defined.");
					}
					Resources.put(newFeature);
					if (loadedListener != null) {
						loadedListener.resourceLoaded(newFeature);
					}
					log("Resource " + newFeature + ": loaded");
				}
			}
			return newFeature;
		} catch (InstantiationException e) {
			e.printStackTrace(System.err);
			throw new Exception("Resource " + featureElement.getAttribute("name") + " [" + featureElement.getAttribute("class")
					+ "] failed to instantiate.", e.getCause());
		} catch (Exception e) {
			log("Exception loading resource from tag " + featureElement.getNodeName());
			NamedNodeMap atts = featureElement.getAttributes();
			for (int i = 0; i < atts.getLength(); i++) {
				log("\tAttribute " + atts.item(i).getNodeName() + "=" + featureElement.getAttribute(atts.item(i).getNodeName()));
			}
			throw e;
		} catch (AssertionError e) {
			log("Exception loading resource from tag " + featureElement.getNodeName());
			NamedNodeMap atts = featureElement.getAttributes();
			for (int i = 0; i < atts.getLength(); i++) {
				log("\tAttribute " + atts.item(i).getNodeName() + "=" + featureElement.getAttribute(atts.item(i).getNodeName()));
			}
			throw e;
		}
	}

	@Override
	public void setLoadedListener(ResourceLoadedListener loadedListener) {
		this.loadedListener = loadedListener;
	}

	@Override
	public ResourceLoadedListener getLoadedListener() {
		return loadedListener;
	}

	/**
	 * Sets overwrite mode. Existing resources are overwritten by newer ones
	 * without throwing an exception.
	 */
	@Override
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	@Override
	public boolean isOverwrite() {
		return overwrite;
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource.Loader#pushMap(Map)
	 */
	@Override
	public void pushMap(Map<String, Class<? extends IResource>> map) {
		stack.push(typeMap);
		typeMap = new HashMap<String, Class<? extends IResource>>(typeMap);
		typeMap.putAll(map);

		// Permanently remember tags
		for (Map.Entry<String, Class<? extends IResource>> entry : typeMap.entrySet()) {
			String tag = entry.getKey();
			Class<? extends IResource> clazz = entry.getValue();
			Resources.registerTag(clazz, tag);
		}
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource.Loader#popMap()
	 */
	@Override
	public Map<String, Class<? extends IResource>> popMap() {
		if (stack.size() == 0) {
			return null;
		}
		Map<String, Class<? extends IResource>> existingMap = typeMap;
		typeMap = stack.pop();
		return existingMap;
	}

}
