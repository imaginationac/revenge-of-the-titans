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
package com.shavenpuppy.jglib;

import java.io.*;
import java.util.*;

/**
 * Holds Resources. Essentially, a Resource is a handle to a native peer
 * represented by a ResourceAllocator which is some native operating system
 * entity, like a graphics card or something. This Resources class statically
 * keeps hold of all the resources we know that the O/S has so that they won't
 * get garbage collected by Java and left in the O/S, or removed from the O/S
 * and left in Java. Know This about resources: 1) They must have unique names
 * 2) They must all be created and destroyed in the same thread
 *
 * @author foo
 */
public final class Resources {

	public static final boolean DEBUG = false;

	static {
		if (System.getProperty("java.protocol.handler.pkgs") == null) {
			System.getProperties().put("java.protocol.handler.pkgs", "com.shavenpuppy.jglib.resources.protocol");
		}
	}

	/** Serialization mode: set to true to serialize as SerializedResources */
	private static boolean runMode;

	/** A map of resource names to resources */
	private static final HashMap<String, Resource> resourceMap = new HashMap<String, Resource>(256, 0.25f);

	/** A map of resource classes to xml tags */
	private static final HashMap<Class<? extends Resource>, String> classToTagMap = new HashMap<Class<? extends Resource>, String>();

	/** A map of xml tags to resource classes */
	private static final HashMap<String, Class<? extends Resource>> tagToClassMap = new HashMap<String, Class<? extends Resource>>();

	/** Resources in the order that they are loaded */
	private static final List<Resource> all = new LinkedList<Resource>();

	/** A set of unnamed resources */
	private static final Set<Resource> unnamedSet = new HashSet<Resource>();

	/** A queue of resources that need to be created */
	private static final List<Resource> queue = new LinkedList<Resource>();

	/** Number of resources created */
	private static int numCreated;

	/** Resource creation callback */
	public interface CreatingCallback {

		/**
		 * Fired when a resource is about to be created
		 * @param resource
		 */
		void onCreating(Resource resource);

	}

	/** Callback listener */
	private static CreatingCallback creatingCallback;

	/**
	 * NO constructor for Resources.
	 */
	private Resources() {
	}

	/**
	 * Gets a resource by its string description (as returned by its getName()
	 * method). The resource is not created.
	 *
	 * @param name The name of the resource
	 * @return the Resource or null if the resource doesn't exist
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Resource> T peek(String name) {
		return (T) resourceMap.get(name.toLowerCase());
	}

	/**
	 * Gets a resource by its string description (as returned by its getName()
	 * method). If the resource has not yet been created then it is created and
	 * this method will block until it has been created.
	 *
	 * @param name The name of the resource
	 * @return the created resource, ready to use, or null if it can't be returned
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Resource> T get(String name) {
		if (DEBUG) {
			System.err.println("Get resource " + name);
		}

		Resource ret = resourceMap.get(name.toLowerCase());

		if (ret == null) {
			System.err.println("WARNING: Resource '" + name + "' not found");
			return null;
		}

		if (!ret.isCreated()) {
			if (creatingCallback != null) {
				creatingCallback.onCreating(ret);
			}
			ret.create();
			numCreated++;
		}

		return (T) ret; // Warning suppressed
	}

	/**
	 * @return true if the specified resource exists in the Resources
	 */
	public static boolean exists(String name) {
		return resourceMap.containsKey(name.toLowerCase());
	}

	/**
	 * Puts a resource in the resource map, overwriting any existing resource
	 * which has the same name. This method will block if necessary.
	 *
	 * @param resource The resource to store
	 */
	public static void put(Resource resource) {
		if (resource.getName() == null) {
			throw new RuntimeException("Unnamed resource " + resource + " cannot be put in the named set");
		}
		Resource old = resourceMap.put(resource.getName().toLowerCase(), resource);
		if (old != null) {
			old.deregister();
			// Note that we don't destroy it - it has to be garbage
			// collected...
		}
		if (DEBUG) {
			System.err.println("Put " + resource.getName());
		}
		resource.register();
		all.add(resource);
	}

	/**
	 * Add an unnamed resource to the set of unnamed resources. It will be saved
	 * with all the other resources, and loaded back again when loaded - and
	 * registered. Named resources cannot be added to the unnamed set.
	 *
	 * @param res The resource to add
	 */
	public static void add(Resource resource) {
		if (resource.getName() != null) {
			throw new RuntimeException("Named resource " + resource + " cannot be added to the unnamed set.");
		}
		if (DEBUG) {
			System.err.println("Put unnnamed: " + resource);
		}
		unnamedSet.add(resource);
		resource.register();
		all.add(resource);
	}

	/**
	 * Remove an unnamed resource from the set of unnamed resources.
	 *
	 * @param res The resource to remove
	 */
	public static void remove(Resource resource) {
		if (unnamedSet.remove(resource)) {
			resource.deregister();
		}
		all.remove(resource);
	}

	/**
	 * Removes a resource by its name. If the resource has been created, it will
	 * be destroyed and this method will block until it has been destroyed.
	 *
	 * @param name The name of the resource
	 * @return the resource that was removed, if any
	 */
	public static Resource remove(String name) {
		Resource ret;

		ret = resourceMap.remove(name.toLowerCase());
		all.remove(ret);

		if (ret != null) {
			ret.destroy();
			ret.deregister();
		}

		return ret;
	}

	/**
	 * Forget a resource.
	 *
	 * @param resource
	 */
	static Resource forget(Resource resource) {
		all.remove(resource);
		if (resource.getName() != null) {
			return resourceMap.remove(resource.getName());
		} else {
			if (unnamedSet.contains(resource)) {
				unnamedSet.remove(resource);
				return resource;
			} else {
				return null;
			}
		}
	}

	/**
	 * Saves all serializable resources to a stream. The output stream is
	 * buffered here. Non-serializable resources are not saved.
	 *
	 * @param os The output stream to write to.
	 * @throws IOException if some kind of IO error occurs
	 */
	public static void save(OutputStream os) throws IOException {
		BufferedOutputStream bos = null;
		ObjectOutputStream oos = null;

		try {
			bos = new BufferedOutputStream(os);
			oos = new ObjectOutputStream(bos);

			oos.writeObject(all);
			oos.flush();
			bos.flush();

		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
			} catch (Exception e) {
			}
			try {
				if (bos != null) {
					bos.close();
				}
			} catch (Exception e) {
			}
		}

	}

	/**
	 * Load serialized resources from a stream. The stream is buffered here.
	 *
	 * @param is An input stream to load from.
	 * @throws IOException If some kind of IO error occurs
	 */
	public static void load(InputStream is) throws IOException, ClassNotFoundException {
		BufferedInputStream bis = new BufferedInputStream(is);
		ObjectInputStream ois = new ObjectInputStream(bis);

		@SuppressWarnings("unchecked")
		List<Resource> newAll = (List<Resource>) ois.readObject(); // Warning suppressed
		for (Resource res : newAll) {
			if (res.getName() != null) {
				put(res);
			} else {
				add(res);
			}
		}
	}

	/**
	 * Clear all the resources and de-register them.
	 */
	public static void clear() {
		if (DEBUG) {
			System.err.println("------RESOURCES CLEARING------");
		}
		for (Resource res : resourceMap.values()) {
			if (res != null) {
				if (res.isCreated()) {
					res.destroy();
				}
				res.deregister();
			}
		}
		resourceMap.clear();

		for (Resource res: unnamedSet) {
			res.destroy();
		}
		unnamedSet.clear();
		all.clear();
		System.gc();
		if (DEBUG) {
			System.err.println("------RESOURCES CLEARED------");
		}
	}

	/**
	 * Reset all the resources. Any created resources are destroyed.
	 */
	public static void reset() {
		if (DEBUG) {
			System.err.println("------RESOURCES RESETTING ------");
		}
		for (Resource res : resourceMap.values()) {
			if (res != null) {
				if (res.isCreated()) {
					res.destroy();
				}
			}
		}

		for (Resource res : unnamedSet) {
			res.destroy();
		}

		System.gc();

		if (DEBUG) {
			System.err.println("------RESOURCES RESET ------");
		}
	}

	/**
	 * Returns the total number of resources created.
	 *
	 * @return int
	 */
	public static int getNumCreated() {
		return numCreated;
	}

	/**
	 * @return Returns the creatingCallback.
	 */
	public static CreatingCallback getCreatingCallback() {
		return creatingCallback;
	}

	/**
	 * @param creatingCallback The creatingCallback to set.
	 */
	public static void setCreatingCallback(CreatingCallback creatingCallback) {
		Resources.creatingCallback = creatingCallback;
	}

	/**
	 * Get a directory of all named Resources of a particular type
	 *
	 * @param clazz The class of Resource
	 * @return an ArrayList of Resources
	 */
	public static <T extends Resource> ArrayList<T> list(Class<T> clazz) {
		LinkedList<T> ret = new LinkedList<T>();
		for (Map.Entry<String, Resource> entry : resourceMap.entrySet()) {
			if (!entry.getValue().named) {
				continue;
			}
			if (clazz.isAssignableFrom(entry.getValue().getClass())) {
				ret.add(clazz.cast(entry.getValue()));
			}
		}

		// Convert to a probably more efficient ArrayList...
		return new ArrayList<T>(ret);
	}

	/**
	 * @return an unmodifiable List of all the Resources
	 */
	public static List<Resource> list() {
		ArrayList<Resource> ret = new ArrayList<Resource>(resourceMap.values());
		ret.addAll(unnamedSet);
		return Collections.unmodifiableList(ret);
	}

	/**
	 * Controls how resource serialization works. When in development, building
	 * resource.dat files etc. using the ResourceConverter, leave this as false.
	 * When resources are serialized to disk they are serialized in their
	 * entirety.
	 * <p>
	 * When you are in a game, you don't actually want to reserialize resources,
	 * you only want to serialize references to them. Set this flag to true and
	 * this will be the case.
	 *
	 * @param runMode The runMode to set.
	 */
	public static void setRunMode(boolean runMode) {
		Resources.runMode = runMode;
	}

	/**
	 * @return Returns the runMode.
	 */
	public static boolean isRunMode() {
		return runMode;
	}

	public static void registerTag(Class<? extends Resource> clazz, String tag) {
		classToTagMap.put(clazz, tag);
		tagToClassMap.put(tag, clazz);
	}

	public static String getTag(Class<? extends Resource> clazz) {
		return classToTagMap.get(clazz);
	}

	public static Class<? extends Resource> getMapping(String tag) {
		return tagToClassMap.get(tag);
	}

	/**
	 * Create all uncreated resources
	 */
	public static void create() {
		for (Resource res : resourceMap.values()) {
			res.create();
		}
		for (Resource res : unnamedSet) {
			res.create();
		}
	}

	static void queue(Resource res) {
		queue.add(res);
	}

	public static void dequeue() {
		for (Resource r : queue) {
			r.create();
		}
		queue.clear();
	}
}
