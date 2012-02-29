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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import worm.Worm;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Describes a story for a level, which consists of a number of Char entries
 */
public class StoryFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/**
	 * A paragraph in the story
	 */
	public static class ParagraphFeature extends Feature {
		private static final long serialVersionUID = 1L;

		@Data
		private String id;
		@Data
		private String text;
		private boolean useWorld;
		private int delay;
		private int fadeAfter;

		ParagraphFeature() {
			setAutoCreated();
		}

		public int getDelay() {
			return delay;
		}

		public int getFadeAfter() {
			return fadeAfter;
		}

		@Override
		public void load(Element element, Loader loader) throws Exception {
			super.load(element, loader);

			//text = XMLUtil.getText(element, "MISSING TEXT");
			text = XMLUtil.getText(element, "");
		}

		public void dump() {
			System.out.println(id+": "+text);
		}

		/**
		 * @return the text
		 */
		public String getText() {
			return text;
		}

		/**
		 * @return the character
		 */
		public CharacterFeature getCharacter() {
			return (CharacterFeature) Resources.get(useWorld ? Worm.getGameState().getWorld().getUntranslated()+"."+id : id);
		}

	}

	/** List of CharFeatures */
	private List<ParagraphFeature> chars;

	/** Setting */
	private String setting;

	/** Qualifiers. This is a comma separated list of research ids. Prefix each with ! for "NOT" */
	@Data
	private String qualifier;

	private transient SettingFeature settingFeature;

	/**
	 * C'tor
	 */
	public StoryFeature() {
		setAutoCreated();
	}

	/**
	 * C'tor
	 */
	public StoryFeature(String name) {
		super(name);
		setAutoCreated();
	}

	public boolean qualifies() {
		if (qualifier == null || qualifier.equals("")) {
			return true;
		}

		StringTokenizer st = new StringTokenizer(qualifier, ",", false);
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.charAt(0) == '!') {
				token = token.substring(1);
				if (Worm.getGameState().isResearched(token)) {
					return false;
				}
			} else {
				if (!Worm.getGameState().isResearched(token)) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> children = XMLUtil.getChildren(element, "char");
		chars = new ArrayList<ParagraphFeature>(children.size());
		for (Element child : children) {
			ParagraphFeature _char = new ParagraphFeature();
			_char.load(child, loader);
			chars.add(_char);
		}
	}

	public void dump() {
		for (Iterator<ParagraphFeature> i = chars.iterator(); i.hasNext(); ) {
			ParagraphFeature c = i.next();
			c.dump();
		}
	}

	@Override
	protected void doCreate() {
		super.doCreate();
		for (Iterator<ParagraphFeature> i = chars.iterator(); i.hasNext(); ) {
			ParagraphFeature c = i.next();
			c.create();
		}
	}

	@Override
	protected void doDestroy() {
		super.doDestroy();
		for (Iterator<ParagraphFeature> i = chars.iterator(); i.hasNext(); ) {
			ParagraphFeature c = i.next();
			c.destroy();
		}
	}

	/**
	 * @return the chars
	 */
	public List<ParagraphFeature> getChars() {
		return chars;
	}

	/**
	 * @return the setting in which we will read the story
	 */
	public SettingFeature getSetting() {
		return settingFeature;
	}

	/**
	 * @param settingFeature the settingFeature to set
	 */
	public void setSetting(SettingFeature settingFeature) {
		this.settingFeature = settingFeature;
	}



}
