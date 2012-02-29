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
package worm.features;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import worm.MapRenderer;

import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.resources.ColorMapFeature;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Describes a level's colors
 * @author Cas
 */
public class LevelColorsFeature extends ColorMapFeature {

	private static final long serialVersionUID = 1L;

	/** All the different colour sets */
	private static final ArrayList<LevelColorsFeature> COLORS = new ArrayList<LevelColorsFeature>();

	/*
	 * Resource data
	 */

	/** Map of names to Syncs */
	private Map<String, Sync> syncMap;

	private static class Sync implements Serializable {
		private static final long serialVersionUID = 1L;
		int duration;
		int current;

		void tick() {
			current ++;
			if (current >= duration) {
				current = 0;
			}
		}
	}

	/**
	 * C'tor
	 */
	public LevelColorsFeature() {
		super();
	}

	/**
	 * C'tor
	 */
	public LevelColorsFeature(String name) {
		super(name);
		setAutoCreated();
	}

	public void init(MapRenderer renderer) {
		ColorMapFeature.getDefaultColorMap().copy(this);

		// Reset all syncs
		for (Iterator<Sync> i = syncMap.values().iterator(); i.hasNext(); ) {
			Sync s = i.next();
			s.current = 0;
		}
	}

	public void tick() {
		for (Iterator<Sync> i = syncMap.values().iterator(); i.hasNext(); ) {
			Sync s = i.next();
			s.tick();
		}
	}

	public int getValue(String id) {
		Sync s = syncMap.get(id);
		if (s == null) {
			return -1;
		} else {
			return s.current;
		}
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> syncs = XMLUtil.getChildren(element, "sync");
		syncMap = new HashMap<String, Sync>();
		for (Element sync : syncs) {
			String id = XMLUtil.getString(sync, "id");
			Sync s = new Sync();
			s.duration = XMLUtil.getInt(sync, "d");
			syncMap.put(id, s);
		}
	}

	@Override
	protected boolean shouldWriteAttribute(String attribute) {
		if (attribute.equals("syncMap")) {
			return false;
		} else {
			return super.shouldWriteAttribute(attribute);
		}
	}

	@Override
	protected void doRegister() {
		super.doRegister();
		COLORS.add(this);
	}

	@Override
	protected void doDeregister() {
		super.doDeregister();
		COLORS.remove(this);
	}

	@Override
	protected void doWriteChildren(XMLResourceWriter writer) throws IOException {
		super.doWriteChildren(writer);
		boolean isCompact = writer.isCompact();
		writer.setCompact(true);
		for (Entry<String, Sync> e : syncMap.entrySet()) {
			String key = e.getKey();
			Sync value = e.getValue();
			writer.writeTag("sync");
			writer.writeAttribute("id", key, true);
			writer.writeAttribute("d", value.duration);
			writer.closeTag();
		}
		writer.setCompact(isCompact);
	}

	public static LevelColorsFeature getColors(int idx) {
		return COLORS.get(idx % COLORS.size());
	}

}
