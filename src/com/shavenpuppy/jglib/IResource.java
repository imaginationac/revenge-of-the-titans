/**
 *
 */
package com.shavenpuppy.jglib;

import java.io.IOException;
import java.io.Serializable;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource.Loader;

/**
 * Interface for the basic requirements of a Resource that can be created or destroyed
 * @see Resource
 */
public interface IResource extends Serializable {

	/**
	 * Create the resource. If the resource is already created, do nothing.
	 */
	void create();

	/**
	 * Destroy the resource. If the resource is not yet created, do nothing.
	 */
	void destroy();

	/**
	 * @return true if the resource has been "created" successfully
	 */
	boolean isCreated();

	/**
	 * @return true if the resource should not be destroyed
	 */
	boolean isLocked();

	/**
	 * @return the name of this resource; may be null after construction
	 */
	String getName();

	/**
	 * Sets the name of this resource
	 * @param name Cannot be null
	 */
	void setName(String name);

	/**
	 * Register this resource
	 */
	void register();

	/**
	 * Deregister this resource
	 */
	void deregister();

	/**
	 * Archive this resource. Once archived it cannot be re-created.
	 */
	void archive();

	/**
	 * Load the resource from an XML element. The element may have attributes which
	 * are useful to specify things about the resource.
	 * @param element The element to load the resource from.
	 * @param loader The loader which is taking care of the loading.
	 */
	void load(Element element, Loader loader) throws Exception;

	/**
	 * Write out XML for this Resource
	 * @param writer The XML resource writer
	 * @throws IOException
	 */
	void toXML(XMLResourceWriter writer) throws IOException;

	/**
	 * @return true if this a sub-resource
	 */
	boolean isSubResource();

}
