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

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.resources.ResourceLoadedListener;

/**
 * An abstract resource, owned by some operating system thing.
 */
public abstract class Resource implements IResource, Cloneable {

	private static final long serialVersionUID = 1L;

	/*
	 * Resource data
	 */

	/** The unique name of the resource */
	protected String name;

	/** Locked: this resource cannot be reloaded */
	private boolean locked;

	/** Subresource: lifecycle & storage managed by owner resource */
	private boolean subResource;

	/** Creation serialization support */
	private final CreationDeserialization creationDeserialization = new CreationDeserialization(this);

	/*
	 * Transient data
	 */

	// Has the resource been created;
	transient boolean created, creating, destroying;

	/** Special twiddler that causes resources to create() when they are deserialized if they were created at serialization time */
	private static class CreationDeserialization implements Serializable {
		private static final long serialVersionUID = 1L;

		private final Resource res;

		private boolean created;

		CreationDeserialization(Resource res) {
			this.res = res;
		}

    	private Object readResolve() throws ObjectStreamException {
    		try {
    			if (created) {
    				Resources.queue(res);
    			}
    			return this;
    		} catch (Exception e) {
    			e.printStackTrace(System.err);
    			throw new InvalidObjectException("Failed to deserialize resource "+res+" due to "+e);
    		}
    	}

    	private void writeObject(ObjectOutputStream stream) throws IOException {
    		created = res.isCreated();
    		stream.defaultWriteObject();
    	}
	}

	/**
	 * Special interface for development time.
	 */
	public interface Loader {

		/**
		 * Get a resource from an Element.
		 * @param element The element to get the resource from
		 * @return a Resource
		 * @throws Exception if there's anything wrong
		 */
		IResource load(Element element) throws Exception;

		/**
		 * Include further resources from the specified input stream
		 * @param is An XML input stream
		 */
		void include(InputStream is) throws Exception;

		/**
		 * Temorarily add some mappings to the loader.
		 * @param map A Map of Strings (XML tag names) to Classes derived from Resource
		 */
		void pushMap(Map<String, Class<? extends IResource>> map);

		/**
		 * Remove the last set of temporary mappings from the loader.
		 * If no temporary mappings have been added (with pushMap()),
		 * then nothing happens.
		 * @return the removed mapping, or null if there wasn't one
		 */
		Map<String, Class<? extends IResource>> popMap();

		/**
		 * @param loadedListener
		 */
		void setLoadedListener(ResourceLoadedListener loadedListener);

		ResourceLoadedListener getLoadedListener();

		/**
		 * Sets overwrite mode. Existing resources are overwritten by newer ones without
		 * throwing an exception.
		 */
		void setOverwrite(boolean overwrite);

		/**
		 * @return
		 */
		boolean isOverwrite();
	}

	/**
	 * A default public constructor, for serialization purposes.
	 */
	public Resource() {
	}

	/**
	 * Construct a resource with a name.
	 */
	public Resource(String name) {
		this.name = name;
	}

	/**
	 * Create this resource. This method blocks until this resource has been created.
	 */
	@Override
    public final void create() {
		if (!created && !creating) {
			creating = true;
			if (Resources.getCreatingCallback() != null) {
				Resources.getCreatingCallback().onCreating(this);
			}
			doCreate();
			creating = false;
			created = true;
		}
	}

	/**
	 * Destroys the resource. This method blocks until this resource has been destroyed.
	 */
	@Override
    public final void destroy() {
		if (created && !destroying) {
			destroying = true;
			doDestroy();
			destroying = false;
			created = false;
		}
	}

	/**
	 * Subclasses must implement this method to do the creation of external resources.
	 */
	protected void doCreate() {}

	/**
	 * Subclasses must implement this method to do the destruction of external resources.
	 */
	protected void doDestroy() {}

	/**
	 * Has this object been created by the allocator?
	 */
	@Override
    public synchronized final boolean isCreated() {
		return created;
	}

	/**
	 * Returns the resource's name
	 */
	@Override
	public String toString() {
		if (name == null) {
			return "a "+getClass().getName();
		} else {
			return name;
		}
	}

	/**
	 * Provides a hashcode
	 */
	@Override
	public int hashCode() {
		if (name != null) {
			return name.hashCode();
		} else {
			return super.hashCode();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Resource)) {
			return false;
		}
		Resource r = (Resource) obj;
		if (obj == this) {
			return true;
		}
		if (r.name == null || name == null) {
			// Unnamed resources aren't equal, generally
			return false;
		}
		return r.name.equals(name);
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
    public void load(Element element, Loader loader) throws Exception {
	}

	/**
	 * Optional operation: save the resource to an XML element.
	 * @param parent The parent resource, if any
	 * @return a new Element, or null, if the resource can't be saved to XML
	 */
	public Element save(Resource parent) {
		return null;
	}

	@Override
    public void register() {
	}

	@Override
    public void deregister() {
	}

	@Override
    public final boolean isLocked() {
		return locked;
	}

	/**
	 * Lock or unlock the resource.
	 * @param locked
	 * @see #isLocked()
	 * @see #load(Element, Loader)
	 */
	public final void setLocked(boolean locked) {
		this.locked = locked;
	}

	@Override
    public final boolean isSubResource() {
		return subResource;
	}

	/**
	 * Marks this resource as being a sub-resource. Sub-resources are loaded
	 * directly by their parent resource calling load() on them.
	 * @param subResource
	 */
	public final void setSubResource(boolean subResource) {
		this.subResource = subResource;
	}

	/**
	 * Serialization support. We completely replace the serialized resource with an
	 * instance of SerializedResource instead.
	 */
    public final Object writeReplace() throws ObjectStreamException {
    	if (Resources.isRunMode()) {
    		// We're in "Run Mode"
    		if (name == null) {
    			// No name, so have to serialize directly.
    			return this;
    		} else {
    			// Got a name, so send a SerializedResource that just references this
    			return new SerializedResource(this);
    		}
    	} else {
    		// This is "compile time" so we serialize directly
    		return this;
    	}
    }


    private static final class SerializedResource implements Serializable {

    	private static final long serialVersionUID = 1L;

    	private String resourceName;
    	private transient Resource resource;

    	public SerializedResource(Resource resourceToSerialize) {
    		resourceName = resourceToSerialize.getName();
    	}

    	/**
    	 * Provides a hashcode
    	 */
    	@Override
		public int hashCode() {
   			return resourceName.hashCode();
    	}

    	@Override
		public boolean equals(Object obj) {
    		if (obj == null || !(obj instanceof SerializedResource)) {
    			return false;
    		}
    		SerializedResource r = (SerializedResource) obj;
    		if (obj == this) {
    			return true;
    		}
    		if (r.resource != null && resource != null) {
    			return r.resource.equals(resource);
    		} else if (r.resourceName != null && resourceName != null) {
    			return r.resourceName.equals(resourceName);
    		} else {
    			return false;
    		}
    	}

    	/**
    	 * @returns a Resource
    	 */
    	private Object readResolve() throws ObjectStreamException {
    		try {
    			return Resources.get(resourceName);
    		} catch (Exception e) {
    			throw new InvalidObjectException("Failed to deserialize resource "+resourceName+" due to "+e);
    		}
    	}
    }

    /**
     * Sets the name of this resource.
     * @param name
     */
    @Override
    public final void setName(String name) {
    	IResource oldResource = Resources.forget(this);
		this.name = name;
		if (oldResource != null) {
			Resources.put(this);
		}
	}

	/**
	 * Write out XML for this Resource
	 * @param writer The XML resource writer
	 * @throws IOException
	 */
	@Override
    public final void toXML(XMLResourceWriter writer) throws IOException {
		// See if there's a registered tag for this
		String tag = Resources.getTag(getClass());
		if (tag == null) {
			// No, so write an "instance" tag
			writer.writeTag("instance");
			writer.writeAttribute("class", getClass().getName());
		} else {
			// Yes
			writer.writeTag(tag);
		}

		if (name != null) {
			writer.writeAttribute("name", name);
		}

		// Write fields out to XML now
		doToXML(writer);

		// Close the tag.
		writer.closeTag();
	}

	/**
	 * Implement this method to write out the attributes and sub-resources of your
	 * Resource. name is already written out for you.
	 * @param writer
	 * @throws IOException
	 */
	protected void doToXML(XMLResourceWriter writer) throws IOException {
	}

	/**
	 * "Archive" the Resource; its definition can be erased, leaving only the "created" part
	 */
	@Override
    public void archive() {
	}
}
