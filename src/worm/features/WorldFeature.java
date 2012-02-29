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

import org.w3c.dom.Element;

import worm.buildings.BaseBuildingFeature;
import worm.generator.MapTemplate;

import com.shavenpuppy.jglib.openal.ALStream;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.resources.TextResource;
import com.shavenpuppy.jglib.util.Util;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Describes a whole world
 */
public class WorldFeature extends Feature {

	private static final long serialVersionUID = 1L;

	private static final ArrayList<WorldFeature> WORLDS = new ArrayList<WorldFeature>(5);

	/** Title */
	@Data
	private String title;

	/** Untranslated title */
	@Data
	private String untranslated;

	/** Index */
	private int index;

	/** The gidrahs in the world */
	private ResourceArray gidrahs;

	/** The angry gidrahs in the world */
	private ResourceArray angryGidrahs;

	/** The base building */
	private String base;

	/** Buildings */
	private ResourceArray buildings;

	/** Welcome text */
	private TextResource welcomeText;

	/** Map generation template */
	private String template;

	/** Stream */
	private String stream;

	/** Don't register - used for the spurious Xmas world */
	private boolean xmas;


	// chaz hack for world intro

	/** Setting */
	private String setting;

	/** The story */
	private StoryFeature storyFeature;

	private String weaponSetting;
	private String buildingSetting;
	private String techSetting;

	private int survivalMaxType0;
	private int survivalMaxType1;
	private int survivalMaxType2;
	private int survivalMaxType3;
	private int survivalMaxBoss;

	/*
	 * Transient data
	 */

	private transient BaseBuildingFeature baseFeature;
	private transient BaseTemplateFeature templateFeature;
	private transient ALStream streamFeature;
	private transient SettingFeature settingFeature, weaponSettingFeature, buildingSettingFeature, techSettingFeature;

	/**
	 * C'tor
	 * @param name
	 */
	public WorldFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	@Override
	protected void doRegister() {
		if (!xmas) {
			index = WORLDS.size();
			WORLDS.add(this);
		}
	}

	@Override
	protected void doDeregister() {
		if (!xmas) {
			WORLDS.remove(this);
		}
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		if (XMLUtil.hasChild(element, "story")) {
			storyFeature = new StoryFeature();
			storyFeature.load(XMLUtil.getChild(element, "story"), loader);
		}
	}

	/**
	 * @return
	 */
	public BaseBuildingFeature getBase() {
		return baseFeature;
	}

	/**
	 * @return
	 */
	public ResourceArray getBuildings() {
		return buildings;
	}

	/**
	 * @param type A digit between 0 and 3 inclusive or -1 to return a random gidrah
	 * @return a Gidrah
	 */
	public GidrahFeature getGidrah(int type) {
		if (type == -1) {
			type = Util.random(0, 3);
		}
		return (GidrahFeature) gidrahs.getResource(type);
	}

	/**
	 * @param type A digit between 0 and 3 inclusive or -1 to return a random gidrah
	 * @return an ANGRY Gidrah
	 */
	public GidrahFeature getAngryGidrah(int type) {
		if (type == -1) {
			type = Util.random(0, 3);
		}
		return (GidrahFeature) angryGidrahs.getResource(type);
	}

	/**
	 * @return the welcome text
	 */
	public String getWelcomeText() {
		return welcomeText.getText();
	}

	/**
	 * @return the template to use for world generation
	 */
	public MapTemplate getTemplate() {
		return templateFeature;
	}

	public String getTitle() {
		return title;
	}

	public String getUntranslated() {
	    return untranslated;
    }

	/**
	 * @return the number of worlds
	 */
	public static int getNumWorlds() {
		return WORLDS.size();
	}

	/**
	 * @return the streamFeature
	 */
	public ALStream getStream() {
		return streamFeature;
	}

	/**
	 * Gets a world by its index
	 * @param idx
	 * @return
	 */
	public static WorldFeature getWorld(int idx) {
		return WORLDS.get(idx);
	}

	/**
	 * @return the story
	 */
	public StoryFeature getStory() {
		return storyFeature;
	}

	/**
	 * @return the setting in which we will read the story
	 */
	public SettingFeature getSettingFeature() {
		return settingFeature;
	}

	public SettingFeature getSetting(String type) {
		if (type.equals("weapon")) {
			return weaponSettingFeature;
		} else if (type.equals("tech")) {
			return techSettingFeature;
		} else {
			return buildingSettingFeature;
		}
	}

	/**
	 * @return the survivalMaxBoss
	 */
	public int getSurvivalMaxBoss() {
		return survivalMaxBoss;
	}

	/**
	 * @return the survivalMaxType0
	 */
	public int getSurvivalMaxType(int index) {
		switch (index) {
			case 0:
				return survivalMaxType0;
			case 1:
				return survivalMaxType1;
			case 2:
				return survivalMaxType2;
			case 3:
				return survivalMaxType3;
			default:
				assert false : index;
				return -1;
		}
	}

}
