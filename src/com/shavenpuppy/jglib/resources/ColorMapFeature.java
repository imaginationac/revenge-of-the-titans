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
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Describes a map of names to colors
 */
public class ColorMapFeature extends Feature implements ReadableColorMap {

	private static final long serialVersionUID = -5807833249115496127L;

	private static ColorMapFeature defaultColorMap;

	/*
	 * Resource data
	 */

	public static class ColorName implements Serializable, Comparable<ColorName> {

		private static final long serialVersionUID = 1L;

		public final String name;
		public final int order;

		public ColorName(String name, int order) {
			this.name = name.toLowerCase();
			this.order = order;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public int compareTo(ColorName c) {
			if (order < c.order) {
				return -1;
			} else if (order > c.order) {
				return 1;
			} else {
				return 0;
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ColorName other = (ColorName) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}


	}

	/** Map of ColorNames to Colors */
	private SortedMap<ColorName, Color> colorNameMap;

	/** Simple map of names to Colors */
	private Map<String, Color> colorMap;

	/** Whether to use HSV colors */
	private boolean hsb = true;

	/** Whether this is the "default" color map */
	private boolean default_;

	/**
	 * C'tor
	 */
	public ColorMapFeature() {
		super();
	}

	/**
	 * C'tor
	 */
	public ColorMapFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return the defaultColorMap, specified by being the last color map loaded with default="true"
	 */
	public static ColorMapFeature getDefaultColorMap() {
		return defaultColorMap;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> colors = XMLUtil.getChildren(element, "color");
		colorNameMap = new TreeMap<ColorName, Color>();
		colorMap = new HashMap<String, Color>();
		int count = 0;
		for (Element color : colors) {
			String id = XMLUtil.getString(color, "id");
			int order = XMLUtil.getInt(color, "order", count ++);
			Color c = ColorParser.parse(XMLUtil.getString(color, "c"));
			colorNameMap.put(new ColorName(id, order), c);
			colorMap.put(id, c);
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#shouldWriteAttribute(java.lang.String)
	 */
	@Override
	protected boolean shouldWriteAttribute(String attribute) {
		if (attribute.equals("autoCreated") || attribute.equals("colorMap") || attribute.equals("colorNameMap")) {
			return false;
		} else {
			return super.shouldWriteAttribute(attribute);
		}
	}

	@Override
	protected void doWriteChildren(XMLResourceWriter writer) throws IOException {
		boolean isCompact = writer.isCompact();
		writer.setCompact(true);
		for (Map.Entry<ColorName, Color> e : colorNameMap.entrySet()) {
			ColorName cn = e.getKey();
			Color value = e.getValue();
			writer.writeTag("color");
			writer.writeAttribute("id", cn.name, true);
			writer.writeAttribute("order", cn.order);
			if (hsb) {
				float[] colorHSB = value.toHSB(null);
				writer.writeAttribute("c", "!"+(int)(255.0f * colorHSB[0])+","+(int)(255.0f * colorHSB[1])+","+(int)(255.0f * colorHSB[2])+","+value.getAlpha());
			} else {
				writer.writeAttribute("c", value.getRed()+","+value.getGreen()+","+value.getBlue()+","+value.getAlpha());
			}
			writer.closeTag();
		}
		writer.setCompact(isCompact);
	}

	/**
	 * Get a color by its name
	 * @param name
	 * @return a ReadableColor
	 */
	@Override
	public ReadableColor getColor(String name) {
		return colorMap.get(name);
	}

	/**
	 * Set a color. The color is <em>copied</em> from newColor, which means that references to the color obtained with {@link #getColor(String)}
	 * will automatically be looking at the new color. A color that is not present yet will be added to the map, and the color name
	 * map.
	 * @param name
	 * @param newColor
	 */
	public void setColor(String name, ReadableColor newColor) {
		name = name.toLowerCase();
		Color oldColor = colorMap.get(name);
		if (oldColor == null) {
			oldColor = new Color();
			colorMap.put(name, oldColor);
			colorNameMap.put(new ColorName(name, colorNameMap.size()), oldColor);
		}
		oldColor.setColor(newColor);
	}

	/**
	 * @return the map of ColorNames to Colors (used by the diddler)
	 */
	public SortedMap<ColorName, Color> getColorNameMap() {
		return colorNameMap;
	}

	/**
	 * Copy the colors out of another {@link ReadableColorMap}
	 * @param source
	 */
	public void copy(ReadableColorMap source) {
		for (String name : source.getNames()) {
			setColor(name, source.getColor(name));
		}
	}

	@Override
	public Set<String> getNames() {
		return colorMap.keySet();
	}

	@Override
	protected void doRegister() {
		super.doRegister();

		if (default_) {
			ColorMapFeature.defaultColorMap = this;
		}
	}

	@Override
	protected void doDeregister() {
		super.doDeregister();

		if (default_) {
			ColorMapFeature.defaultColorMap = null;
		}
	}
}
