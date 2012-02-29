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
import java.util.List;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.IResource;
import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * An array of resources
 * @author foo
 */
public class ResourceArray extends Resource {

	private static final long serialVersionUID = 1L;

	/** The resources, by name */
	private String[] resource;

	/** Embedded resources */
	private IResource[] embedded;

	/** The resources, in an array */
	private transient IResource[] resourceInstance;

	/**
	 * C'tor
	 */
	public ResourceArray() {
		super();
	}

	/**
	 * @param name
	 */
	public ResourceArray(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		List<Element> children = XMLUtil.getChildren(element, "item");
		resource = new String[children.size()];
		embedded = new IResource[children.size()];
		for (int i = 0; i < children.size(); i ++) {
			Element child = children.get(i);
			// Look for a child element
			List<Element> subChildren = XMLUtil.getChildren(child);
			if (subChildren.size() == 1) {
				embedded[i] = loader.load(subChildren.get(0));
			} else if (subChildren.size() > 1) {
				throw new Exception("<item> tag cannot contain multiple sub-items");
			} else {
				resource[i] = child.getTextContent().trim();
			}
		}
	}

	@Override
	protected void doCreate() {
		resourceInstance = new Resource[resource.length];
		for (int i = 0; i < resource.length; i ++) {
			if (embedded[i] != null) {
				resourceInstance[i] = embedded[i];
				embedded[i].create();
			} else {
				resourceInstance[i] = Resources.get(resource[i]);
			}
		}

	}

	@Override
	protected void doDestroy() {
		for (int i = 0; i < resource.length; i ++) {
			if (embedded[i] != null) {
				embedded[i].destroy();
			}
		}
		resourceInstance = null;
	}

	/**
	 * Get the resource at the specified index
	 * @param idx
	 * @return
	 */
	public IResource getResource(int idx) {
		return resourceInstance[idx];
	}

	/**
	 * @return the number of resources
	 */
	public int getNumResources() {
		return resourceInstance.length;
	}

	@Override
	public void archive() {
		resource = null;
		embedded = null;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		for (int i = 0; i < resource.length; i ++) {
			writer.writeTag("item");
			if (resource[i] != null) {
				writer.writeText(resource[i]);
			} else if (embedded[i] != null) {
				((Resource) embedded[i]).toXML(writer);
			}
			writer.closeTag();
		}
	}
}
