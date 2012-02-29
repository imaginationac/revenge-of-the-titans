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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.shavenpuppy.jglib.Resources;


/**
 * $Id: DynamicResource.java,v 1.7 2011/06/22 12:02:45 cix_foo Exp $
 * This is a resource that dynamically loads further resources in XML for you.
 * @author $Author: cix_foo $
 * @version $Revision: 1.7 $
 */
public class DynamicResource extends Feature {

	private static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	/** A Map of all named DynamicResources */
	private static final List<DynamicResource> DYNAMIC_RESOURCES = new ArrayList<DynamicResource>(1);

	/*
	 * Resource data
	 */

	/** Classloader */
	private String classLoader;

	/** URL of the XML stream */
	private String url;

	/*
	 * Transient data
	 */

	/** Listen for resource loads */
	private transient ResourceLoadedListener listener;
	private transient ClassLoaderResource classLoaderResource;

	/**
	 * C'tor
	 */
	public DynamicResource() {
	}

	/**
	 * C'tor
	 * @param name
	 */
	public DynamicResource(String name) {
		super(name);
	}

	/**
	 * C'tor
	 * @param name
	 * @param url
	 */
	public DynamicResource(String name, String url) {
		super(name);
		this.url = url;
	}

	@Override
	protected void doRegister() {
		if (Resources.DEBUG) {
			System.out.println("Registering dynamic XML "+this);
		}
		DYNAMIC_RESOURCES.remove(this);
		DYNAMIC_RESOURCES.add(this);
	}

	@Override
	protected void doDeregister() {
		DYNAMIC_RESOURCES.remove(this);
	}

	private ClassLoader getClassLoader() {
		if (classLoaderResource == null) {
			return getClass().getClassLoader();
		} else {
			return classLoaderResource.getClassLoader();
		}
	}

	@Override
	protected void doCreate() {
		super.doCreate();

		try {
			BufferedInputStream bis;
			if (url.startsWith("classpath:")) {
				bis = new BufferedInputStream(getClassLoader().getResourceAsStream(url.substring(10)));
			} else {
				bis = new BufferedInputStream(new URL(url).openStream());
			}

			ResourceConverter loader = new ResourceConverter(listener, getClassLoader());
			loader.setOverwrite(true);
			if (Resources.DEBUG) {
				System.out.println("Including dynamic resource from "+url);
			}
			loader.include(bis);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Destroy and recreate all dynamic resources.
	 */
	public static void createAll() {
		for (DynamicResource dr : DYNAMIC_RESOURCES) {
			if (dr.isCreated()) {
				dr.destroy();
			}
			dr.create();
		}
	}

	public void setListener(ResourceLoadedListener listener) {
		this.listener = listener;
	}

	public ResourceLoadedListener getListener() {
		return listener;
	}


}
