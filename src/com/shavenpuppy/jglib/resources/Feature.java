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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.IResource;
import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * $Id: Feature.java,v 1.24 2011/10/14 15:16:41 cix_foo Exp $
 * Base class for game resources
 * @author $Author: cix_foo $
 * @version $Revision: 1.24 $
 */
public abstract class Feature extends Resource {

	private static final long serialVersionUID = 1L;

	/** All game features */
	private static final List<Feature> FEATURES = new ArrayList<Feature>();

	/*
	 * Resource data
	 */

	/** Autocreate this feature at application init */
	private boolean autoCreated;

	/** Inherit data from another feature */
	private String inherit;

	/*
	 * Transient data
	 */

	private transient Feature inheritFeature;

	/**
	 * C'tor
	 */
	public Feature() {
		super();
	}

	/**
	 * C'tor
	 * @param name
	 */
	public Feature(String name) {
		super(name);
	}

	/**
	 * Autocreate all auto-created features
	 */
	public static void autoCreate() throws Exception {
		for (Feature feature : FEATURES) {
			if (feature.autoCreated) {
				feature.create();
			}
		}

		for (IResource resource : Resources.list()) {
			if (resource.isCreated()) {
				resource.archive();
			}
		}

		System.gc();
	}

	@Override
	public void archive() {
		clearStrings();
	}

	protected final void setAutoCreated() {
		autoCreated = true;
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		autoCreated = XMLUtil.getBoolean(element, "autoCreated", autoCreated);
		// Default processing
		XMLUtil.grabXMLAttributes(loader, this, Feature.class, element);
	}

	private void clearStrings() {
		Class<?> clazz = this.getClass();
		while (Feature.class.isAssignableFrom(clazz)) {
			Field[] fields = clazz.getDeclaredFields();
			ArrayList<Field> sortedStringFields = new ArrayList<Field>(fields.length / 2 + 1);
			ArrayList<Field> sortedResourceFields = new ArrayList<Field>(fields.length / 2 + 1);
			for (int i = 0; i < fields.length; i ++) {
				Field f = fields[i];
				// Ignore a few well-known fields
				String fieldName = f.getName().toLowerCase();
				if (fieldName.equals("name") || fieldName.equals("class") || fieldName.equals("autoCreated") || fieldName.equals("inherit")) {
					continue;
				}
				f.setAccessible(true);

				// ordinary string fields:
				// If the field is final or static or transient then ignore it
				int modifiers = f.getModifiers();
				// Ignore final and static fields
				if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
					continue;
				}

				if (f.getAnnotation(Data.class) != null) {
					// It's "data" - don't touch
					continue;
				} else if (!Modifier.isTransient(modifiers) && f.getType() == String.class) {
					sortedStringFields.add(f);
				} else if (Modifier.isTransient(modifiers) && IResource.class.isAssignableFrom(f.getType())) {
					sortedResourceFields.add(f);
				}
			}

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
			Collections.sort(sortedStringFields, sorter);
			Collections.sort(sortedResourceFields, sorter);

			for (Field stringField : sortedStringFields) {
				// Search for a field beginning with this field's name
				for (Iterator<Field> j = sortedResourceFields.iterator(); j.hasNext(); ) {
					Field resourceField = j.next();
					if (resourceField.getName().startsWith(stringField.getName())) {
						resourceField.setAccessible(true);
						String resourceName;
						try {
							resourceName = (String) stringField.get(this);
							if (resourceName != null && Resources.exists(resourceName)) {
								IResource r = Resources.peek(resourceName);
								if (resourceField.getType().isAssignableFrom(r.getClass())) {
									stringField.set(this, null);
									//System.out.println("Zapped "+this.getName()+"."+stringField);
								}
							} else {
								// Don't attempt to get this field again
								j.remove();
							}
						} catch (Exception e) {
							assert false : "Should never happen: "+e;
						}
						break;
					}
				}
			}

			// Get next class up...
			clazz = clazz.getSuperclass();
		}
	}


	/**
	 * Do default creation. This scans through the class heirarchy, looking for non-transient non-final non-static String fields,
	 * and then trying to find a corresponding transient, non-final, non-static field that starts with the name of the first
	 * field. The original String field is meant to be a Resource which can be stashed in the transient partner field.
	 */
	private void defaultCreation() throws Exception {
		// First deal with inheritance, a special case
		if (inherit != null && inheritFeature == null) {
			inheritFeature = (Feature) Resources.get(inherit);
			if (inheritFeature == null) {
				throw new Exception(this+" (a "+getClass().getName()+") cannot inherit "+inherit+" as it doesn't exist");
			}
			if (!inheritFeature.getClass().isAssignableFrom(this.getClass())) {
				throw new Exception(this+" (a "+getClass().getName()+") cannot inherit "+inherit+" (a "+inheritFeature.getClass().getName()+")");
			}

			Class<?> clazz = this.getClass();
			while (Feature.class.isAssignableFrom(clazz)) {
				Field[] fields = clazz.getDeclaredFields();
				for (int i = 0; i < fields.length; i ++) {
					Field f = fields[i];
					// Ignore a few well-known fields
					String fieldName = f.getName().toLowerCase();
					if (fieldName.equals("name") || fieldName.equals("class") || fieldName.equals("autoCreated") || fieldName.equals("inherit")) {
						continue;
					}
					f.setAccessible(true);
					// If the field is final or static or transient then ignore it
					int modifiers = f.getModifiers();
					if (Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
						continue;
					}
					Object inheritedValue = f.get(inheritFeature);
					if (inheritedValue != null) {
						Object currentValue = f.get(this);
						if (currentValue != null) {
							// Might be a primitive type
							if (f.getType().isPrimitive()) {
								// Yes it is. In which case ignore "default" values like false and 0.
								if (currentValue.equals(Boolean.FALSE) || (currentValue instanceof Number && ((Number) currentValue).doubleValue() == 0.0)) {
									f.set(this, inheritedValue);
								}
							}
						} else {
							f.set(this, inheritedValue);
						}
					}
				}
				// Get next class up...
				clazz = clazz.getSuperclass();
			}
		}

		Class<?> clazz = this.getClass();
		while (Feature.class.isAssignableFrom(clazz)) {
			Field[] fields = clazz.getDeclaredFields();
			ArrayList<Field> sortedStringFields = new ArrayList<Field>(fields.length / 2 + 1);
			ArrayList<Field> sortedResourceFields = new ArrayList<Field>(fields.length / 2 + 1);
			for (int i = 0; i < fields.length; i ++) {
				Field f = fields[i];
				// Ignore a few well-known fields
				String fieldName = f.getName().toLowerCase();
				if (fieldName.equals("name") || fieldName.equals("class") || fieldName.equals("autoCreated") || fieldName.equals("inherit")) {
					continue;
				}
				f.setAccessible(true);

				// ordinary string fields:
				// If the field is final or static or transient then ignore it
				int modifiers = f.getModifiers();
				// Ignore final and static fields
				if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
					continue;
				}

				if (!Modifier.isTransient(modifiers) && f.getType() == String.class) {
					sortedStringFields.add(f);
				} else if (Modifier.isTransient(modifiers) && IResource.class.isAssignableFrom(f.getType())) {
					sortedResourceFields.add(f);
				}
			}

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
			Collections.sort(sortedStringFields, sorter);
			Collections.sort(sortedResourceFields, sorter);

			for (Field stringField : sortedStringFields) {
				// Search for a field beginning with this field's name
				for (Iterator<Field> j = sortedResourceFields.iterator(); j.hasNext(); ) {
					Field resourceField = j.next();
					if (resourceField.getName().startsWith(stringField.getName())) {
						if (resourceField.get(this) != null) {
							// It's already assigned something
							continue;
						}
						resourceField.setAccessible(true);
						String resourceName = (String) stringField.get(this);
						if (resourceName != null && Resources.exists(resourceName)) {
							IResource r = Resources.get(resourceName);
							if (resourceField.getType().isAssignableFrom(r.getClass())) {
								resourceField.set(this, r);
							} else {
								throw new Exception(resourceField.getName()+" cannot be assigned a "+r.getClass().getName());
							}
						} else {
							// Don't attempt to get this field again
							j.remove();
						}
						break;
					}
				}
			}

			// Get next class up...
			clazz = clazz.getSuperclass();
		}

		// Now create all non-transient Resource fields
		clazz = this.getClass();
		while (Feature.class.isAssignableFrom(clazz)) {
			Field[] fields = clazz.getDeclaredFields();
			for (int i = 0; i < fields.length; i ++) {
				Field f = fields[i];
				int rfmodifiers = f.getModifiers();
				if (Modifier.isTransient(rfmodifiers) || Modifier.isStatic(rfmodifiers)) {
					continue;
				}

				f.setAccessible(true);

				Object resource = f.get(this);
				if (resource != null && IResource.class.isAssignableFrom(resource.getClass())) {
					((IResource)resource).create();
				}
			}

			// Get next class up...
			clazz = clazz.getSuperclass();
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {
		try {
			defaultCreation();
		} catch (Exception t) {
			throw new RuntimeException("Failed to create "+this+" due to "+t, t);
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		defaultDestroy();
	}

	/**
	 * Default destruction: null out any transient Resource fields.
	 */
	private void defaultDestroy() {
		Class<?> clazz = this.getClass();
		while (Feature.class.isAssignableFrom(clazz)) {
			Field[] fields = clazz.getDeclaredFields();
			for (int i = 0; i < fields.length; i ++) {
				Field f = fields[i];
				// Ignore a few well-known fields
				if (f.getName().equals("name") || f.getName().equals("class") || f.getName().equals("autoCreated")) {
					continue;
				}
				f.setAccessible(true);
				// If the field is final or static or not transient then ignore it
				int modifiers = f.getModifiers();
				if (!Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
					continue;
				}
				if (IResource.class.isAssignableFrom(f.getType())) {
					try {
						IResource r = (IResource) f.get(this);
						// If the resource is locked, don't null it either
						if (r != null && !r.isLocked()) {
							f.set(this, null);
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace(System.err);
					}
				}
			}

			// Get next class up...
			clazz = clazz.getSuperclass();
		}

		// Now zap any non-transient resource fields
		clazz = this.getClass();
		while (Feature.class.isAssignableFrom(clazz)) {
			Field[] fields = clazz.getDeclaredFields();
			for (int i = 0; i < fields.length; i ++) {
				Field f = fields[i];
				int rfmodifiers = f.getModifiers();
				if (Modifier.isTransient(rfmodifiers) || Modifier.isStatic(rfmodifiers)) {
					continue;
				}
				if (!IResource.class.isAssignableFrom(f.getType())) {
					continue;
				}
				f.setAccessible(true);
				try {
					if (f.get(this) == null) {
						continue;
					}
					IResource resource = (IResource) f.get(this);
					if (resource != null) {
						resource.destroy();
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace(System.err);
				}
			}

			// Get next class up...
			clazz = clazz.getSuperclass();
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {

		if (autoCreated && shouldWriteAttribute("autoCreated")) {
			writer.writeAttribute("autoCreated", autoCreated);
		}

		Class<?> clazz = getClass();

		List<Field> normalFields = new LinkedList<Field>();
		List<Field> objectFields = new LinkedList<Field>();

		while (Feature.class.isAssignableFrom(clazz)) {
			Field[] fields = clazz.getDeclaredFields();
			for (int i = 0; i < fields.length; i ++) {
				Field f = fields[i];
				// Ignore a few well-known fields
				if (f.getName().equals("name") || f.getName().equals("class") || f.getName().equals("autoCreated")) {
					continue;
				}
				f.setAccessible(true);
				// If the field is final or static or transient then ignore it
				int modifiers = f.getModifiers();
				if (Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
					continue;
				}

				if (Resource.class.isAssignableFrom(f.getType())) {
					objectFields.add(f);
				} else {
					normalFields.add(f);
				}
			}


			// Get next class up...
			clazz = clazz.getSuperclass();
		}

		// Now write it all out. But first let's construct a vanilla Feature
		// and look at the default values.
		Feature tester;
		try {
			tester = getClass().newInstance();
		} catch (InstantiationException e1) {
			throw new IOException(e1.getMessage());
		} catch (IllegalAccessException e1) {
			throw new IOException(e1.getMessage());
		}
		for (Field field : normalFields) {
			String name = field.getName();
			if (shouldWriteAttribute(name)) {
				try {
					Object value = field.get(this);
					Object defaultValue = field.get(tester);

					if (value != null && !value.equals(defaultValue)) {
						writer.writeAttribute(name, value);
					}
				} catch (IllegalArgumentException e) {
					throw new IOException(e.getMessage());
				} catch (IllegalAccessException e) {
					throw new IOException(e.getMessage());
				}
			}
		}
		// Let user write some attributes now
		doWriteAttributes(writer);

		for (Field field : objectFields) {
			String name = field.getName();
			if (shouldWriteChild(name)) {
				try {
					Resource value = (Resource) field.get(this);
					if (value != null) {
						writer.writeTag(name);
						value.toXML(writer);
						writer.closeTag();
					}
				} catch (IllegalArgumentException e) {
					throw new IOException(e.getMessage());
				} catch (IllegalAccessException e) {
					throw new IOException(e.getMessage());
				}
			}
		}

		// Let user write some objects
		doWriteChildren(writer);
	}

	/**
	 * Write any attributes out here
	 * @param writer
	 * @throws IOException
	 */
	protected void doWriteAttributes(XMLResourceWriter writer) throws IOException {
	}

	/**
	 * Write any child tags or text etc out here
	 * @param writer
	 * @throws IOException
	 */
	protected void doWriteChildren(XMLResourceWriter writer) throws IOException {
	}

	/**
	 * Override to selectively write attributes out automatically
	 * @param attribute
	 * @return true if this attribute should be automatically written (default true)
	 */
	protected boolean shouldWriteAttribute(String attribute) {
		return true;
	}

	/**
	 * Override to selectively write children out automatically
	 * @param child
	 * @return true if this child should be automatically written (default true)
	 */
	protected boolean shouldWriteChild(String child) {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#deregister()
	 */
	@Override
	public final void deregister() {
		if (FEATURES.remove(this)) {
			doDeregister();
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#register()
	 */
	@Override
	public final void register() {
		if (FEATURES.contains(this)) {
			return;
		}
		FEATURES.add(this);
		doRegister();
	}

	/**
	 * Override for extra registration
	 */
	protected void doRegister() {
	}

	/**
	 * Override for extra deregistration
	 */
	protected void doDeregister() {
	}

}
