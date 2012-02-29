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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.TextResource;

/**
 * Research items
 */
public class ResearchFeature extends Feature {

	private static final long serialVersionUID = 1L;

	public static final String BIOLOGY = "biology"; // +1 Damage
	public static final String ANATOMY = "anatomy"; // +1 AP
	public static final String OPTICS = "optics"; // +0.5 scanner range
	public static final String EXTRACTION = "extraction"; // +1 rate for refineries
	public static final String FACTORY = "factory";
	public static final String REACTOR = "reactor";
	public static final String SCANNER = "scanner";
	public static final String COOLINGTOWER = "coolingtower";
	public static final String BATTERY = "battery";
	public static final String CAPACITOR = "capacitor";
	public static final String SHIELDGENERATOR = "shieldgenerator";
	public static final String MINES = "mines";
	public static final String CLUSTERMINES = "clustermines";
	public static final String BLASTMINES = "blastmines";
	public static final String CONCRETE = "concrete";
	public static final String STEEL = "steel";
	public static final String TITANIUM = "titanium";
	public static final String NANOMESH = "nanomesh";
	public static final String AUTOLOADER = "autoloader";

	public static final String TANGLEWEB = "tangleweb";
	public static final String TANKFACTORY = "tankfactory";
	public static final String REPAIRDRONES = "repairdrones";
	public static final String SCARECROW = "scarecrow";
	public static final String CLOAKINGDEVICE = "cloakingdevicer";

	public static final String IONISATION = "ionisation"; // +1 capacitor range
	public static final String LITHIUM = "lithium"; // +1 ammo
	public static final String SODIUM = "sodium"; // +1 cooling
	public static final String PRECISION = "precision"; // +1 reload
	public static final String ADVANCEDEXPLOSIVES = "advancedexplosives"; // +33% blast damage
	public static final String PLASTIC = "plastic"; // +50% blast radius
	public static final String NANOHARDENING = "nanohardening"; // +1 armour
	public static final String DROIDBUFF = "droidbuff"; // +1 droid speed, 25% of droids have heavy blasters
	public static final String FINETUNING = "finetuning"; // +1 silos
	public static final String SHIELDING = "shielding"; // Disruptor shielding
	public static final String EXTRABARRICADES = "extrabarricades"; // +50% barricades
	public static final String EXTRAMINES = "extramines"; // +50% mines
	public static final String XRAYS = "xrays"; // -1 scanner

	private static final Map<String, ResearchFeature> RESEARCH = new HashMap<String, ResearchFeature>();

	private static final Map<String, List<String>> MEDAL_GROUPS = new HashMap<String, List<String>>();

	@Data
	private String id;
	@Data
	private String depends;
	private boolean available;

	@Data
	private String title;
	private TextResource description;
	private LayersFeature appearance;

	/** Available in registered only */
	private boolean registeredOnly;

	/** Setting to use for the story: "weapon" or "building" or "tech"*/
	@Data
	private String setting;

	/** The research story */
	private StoryFeature story;

	/** Medal groups (comma separated list) */
	@Data
	private String medals;

	/**
	 * C'tor
	 */
	public ResearchFeature() {
		setAutoCreated();
	}

	public LayersFeature getAppearance() {
		return appearance;
	}

	public String getID() {
		return id;
	}

	public String getDepends() {
		return depends;
	}

	public final boolean isDefaultAvailable() {
		return available;
	}

	public boolean isRegisteredOnly() {
		return registeredOnly;
	}

	public StoryFeature getStory() {
		return story;
	}

	public String getSetting() {
		return setting;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description.getText();
	}

	public String getMedals() {
		return medals;
	}

	@Override
	protected void doRegister() {
		RESEARCH.put(id, this);

		if (medals != null) {
			StringTokenizer st = new StringTokenizer(medals, ",", false);
			while (st.hasMoreTokens()) {
				String medal = st.nextToken();
				List<String> medalGroup = MEDAL_GROUPS.get(medal);
				if (medalGroup == null) {
					medalGroup = new LinkedList<String>();
					MEDAL_GROUPS.put(medal, medalGroup);
				}
				medalGroup.add(id);
			}
		}
	}

	public static Map<String, ResearchFeature> getResearch() {
		return Collections.unmodifiableMap(RESEARCH);
	}

	public static Map<String, List<String>> getMedalGroups() {
		return Collections.unmodifiableMap(MEDAL_GROUPS);
	}

	/**
	 * Called when something is researched
	 */
	public void onResearched() {
	}

	/**
	 * Called when something is unresearched
	 */
	public void onUnresearched() {
	}
}
