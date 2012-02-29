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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.puppygames.applet.Anchor;
import net.puppygames.applet.Bounded;
import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.Effect;
import net.puppygames.applet.effects.Emitter;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Element;

import worm.Statistics;
import worm.Worm;
import worm.animation.SimpleThingWithLayers;
import worm.features.StoryFeature.ParagraphFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * A story setting. This has a background consisting of a LayersFeature, and then a number of references to
 * CharacterFeatures with coordinates and bounding boxes for speech bubbles. Not all characters will necessarily
 * be used.
 */
public class SettingFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/** Rectangular background */
	@Data
	private String bg;

	/** Bg layer */
	private int bgLayer;

	/** Background */
	@Data
	private String background;

	/** Background position */
	private Point position;

	/** Background size */
	private Dimension size;

	/** Actors: map of character ids to ActorFeatures */
	private Map<String, ActorFeature> actors;

	// chaz hack! use world story characters?
	private boolean worldIntro;

	/** Centre x / y/ both */
	@Data
	private String centre = "both";

	/** Anchors for setting */
	private ArrayList<Anchor> anchors;

	/** Anchors for bg */
	private ArrayList<Anchor> bgAnchors;

	/*
	 * Transient
	 */

	private transient LayersFeature backgroundFeature;
	private transient Background bgFeature;

	/**
	 * Instances
	 */
	private class SettingInstance extends Effect implements Setting {

		private final StoryFeature story;

		private SimpleThingWithLayers backgroundLayers;
		private Emitter[] backgroundEmitter;
		private TickableObject bgObject;

		private class BGInstance implements Bounded {
			Background.Instance bgInstance;
			Rectangle bounds = new Rectangle(position, size);

			BGInstance(Background.Instance bgInstance) {
				this.bgInstance = bgInstance;
				bgInstance.setBounds(bounds);
			}

			@Override
			public ReadableRectangle getBounds() {
				return bounds;
			}
			@Override
			public void setBounds(int x, int y, int w, int h) {
				bounds.setBounds(x, y, w, h);
			}

			void render(SimpleRenderer renderer) {
				bgInstance.render(renderer);
			}
		}

		private BGInstance bgInstance;

		private Set<CharacterFeature> actorsInUse;
		private List<Actor> actorInstances;
		private Actor currentActor;

		private boolean waitForMouse;
		private int currentIdx;
		private boolean done;

		private Rectangle bounds = new Rectangle(position == null ? new Point(0, 0) : position,
				size == null ? new Dimension(Game.getScale(), Game.getScale()) : size);

		/**
		 * C'tor
		 * @param story The story to read out in this setting
		 */
		public SettingInstance(StoryFeature story) {
			this.story = story;
		}

		@Override
		public boolean isCentredX() {
			return "x".equals(centre);
		}
		@Override
		public boolean isCentredY() {
			return "y".equals(centre);
		}
		@Override
		public boolean isCentred() {
			return "both".equals(centre);
		}

		@Override
		public void onResized() {
			// Apply anchors unless screen is "centred"
			if (anchors != null) {
				for (Iterator<Anchor> i = anchors.iterator(); i.hasNext(); ) {
					Anchor anchor = i.next();
					anchor.apply(this);
				}
			} else if (position != null) {
				boolean centreX = isCentred() || isCentredX();
				boolean centreY = isCentred() || isCentredY();
				if (centreX || centreY) {
					ReadableRectangle currentBounds = getBounds();
					int newX = centreX ? (Game.getWidth() - Game.getScale()) / 2 + position.getX() : position.getX();
					int newY = centreY ? (Game.getHeight() - Game.getScale()) / 2 + position.getY() : position.getY();
					setBounds(newX, newY, currentBounds.getWidth(), currentBounds.getHeight());
				}
			}

			// Resize bg
			if (bgAnchors != null) {
				for (Iterator<Anchor> i = bgAnchors.iterator(); i.hasNext(); ) {
					Anchor anchor = i.next();
					anchor.apply(bgInstance);
				}
			}


			// Resize actors
			for (Iterator<Actor> i = actorInstances.iterator(); i.hasNext(); ) {
				Actor actor = i.next();
				actor.onResized();
			}
		}

		@Override
		public ReadableRectangle getBounds() {
			return bounds;
		}

		@Override
		public void setBounds(int x, int y, int w, int h) {
			bounds.setBounds(x, y, w, h);
			if (backgroundLayers != null && backgroundLayers.getSprites() != null) {
				for (int i = 0; i < backgroundLayers.getSprites().length; i ++) {
					backgroundLayers.getSprite(i).setLocation(x, y);
				}
			}
		}
		@Override
		protected void doSpawnEffect() {
			// Create area bg
			if (bgFeature != null) {
				bgInstance = new BGInstance(bgFeature.spawn());
				bgObject = new TickableObject() {
					@Override
					protected void render() {
						bgInstance.render(this);
					}
				};
				bgObject.spawn(getScreen());
				bgObject.setLayer(bgLayer);
			}

			// Create the background
			backgroundLayers = new SimpleThingWithLayers(getScreen());
			if (backgroundFeature != null) {
				backgroundFeature.createSprites(getScreen(), backgroundLayers);
				backgroundEmitter = backgroundFeature.createEmitters(getScreen(), 0.0f, 0.0f);
			}

			// Create the actors
			actorInstances = new ArrayList<Actor>();

			List<ParagraphFeature> characters = story.getChars();

			actorsInUse = new HashSet<CharacterFeature>();
			for (Iterator<ParagraphFeature> i = characters.iterator(); i.hasNext(); ) {
				ParagraphFeature pf = i.next();
				CharacterFeature cf = pf.getCharacter();

				// Find the corresponding ActorFeature
				if (cf != null) {
					ActorFeature af = actors.get(cf.getName());
					if (af != null) {
						Actor actor = null;
						if (!actorsInUse.contains(cf)) {
							actorsInUse.add(cf);
							actor = af.spawn(getScreen(), this);
						} else {
							// Find actor already spawned
							for (Iterator<Actor> j = actorInstances.iterator(); j.hasNext(); ) {
								Actor a = j.next();
								if (a.getCharacter() == cf) {
									actor = a;
									break;
								}
							}
						}
						assert actor != null;
						String text = pf.getText();

						// Parse special values
						if (text.indexOf("[stats]") != -1) {
							text = text.replaceAll("\\[stats\\]", Worm.getGameState().getStatsText());
						}

						while (text.indexOf("[") != -1) {
							int idx1 = text.indexOf("[");
							int idx3 = text.indexOf(":", idx1);
							String type = text.substring(idx1 + 1, idx3);
							int idx2 = text.indexOf("]", idx1);
							String statsName = text.substring(idx1 + 7, idx2);
							Statistics stats = (Statistics) Resources.peek(statsName);
							StringBuilder sb = new StringBuilder(256);
							if (type.equals("stats")) {
								stats.appendFullStats(sb);
							} else if (type.equals("basic")) {
								stats.appendBasicStats(sb);
							} else if (type.equals("title")) {
								stats.appendTitle(sb);
							}
							text = text.substring(0, idx1) + sb + text.substring(idx2 + 1);
						}

						actor.setDelay(pf.getDelay());
						actor.setFadeAfter(pf.getFadeAfter());
						actor.addText(text);
						actorInstances.add(actor);
						if (currentActor == null) {
							currentActor = actor;
						}
					}
				}
			}

			// Start the first one off
			if (currentActor != null) {
				currentActor.begin();
			}
			currentIdx = 0;
			waitForMouse = true;
		}

		@Override
		protected void doRemove() {
			if (currentActor != null) {
				currentActor.remove();
				currentActor = null;
			}
			if (bgObject != null) {
				bgObject.remove();
				bgObject = null;
			}

			// Remove the background
			if (backgroundLayers != null) {
				backgroundLayers.remove();
				backgroundLayers = null;
			}
			if (backgroundEmitter != null) {
				for (Emitter element : backgroundEmitter) {
					if (element != null) {
						element.remove();
					}
				}
				backgroundEmitter = null;
			}
		}

		@Override
		protected void doTick() {
			if (Mouse.isButtonDown(0)) {
				if (!waitForMouse) {
					waitForMouse = true;
					if (currentActor != null) {
						if (!currentActor.advance()) {
							next();
						}
					}
				}
			} else {
				waitForMouse = false;
			}

			if (currentActor != null && currentActor.isFinished()) {
				next();
			}
		}

		private void next() {
			currentIdx ++;
			if (currentIdx < actorInstances.size()) {
				currentActor = actorInstances.get(currentIdx);
				currentActor.begin();
			} else {
				currentActor = null;
			}
		}

		@Override
		public boolean isEffectActive() {
			return !done;
		}

		@Override
		protected void render() {
			// Nothing to render
		}

		@Override
		public void finish() {
			done = true;
		}

	}


	/**
	 * C'tor
	 */
	public SettingFeature() {
		setAutoCreated();
	}

	/**
	 * C'tor
	 * @param name
	 */
	public SettingFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * Spawn an instance of this Setting, and read out the given story in it
	 * @param screen
	 * @param story
	 * @return
	 */
	public Setting spawn(Screen screen, StoryFeature story) {
		SettingInstance ret = new SettingInstance(story);
		ret.spawn(screen);
		return ret;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> children = XMLUtil.getChildren(element, "actor");
		actors = new HashMap<String, ActorFeature>();
		for (Element child : children) {
			ActorFeature af = new ActorFeature();
			af.load(child, loader);
			actors.put(af.getCharacter(), af);
		}
		// Anchors
		List<Element> anchorElements = XMLUtil.getChildren(element, "anchor");
		if (anchorElements.size() > 0) {
			anchors = new ArrayList<Anchor>(anchorElements.size());
			for (Element anchorChild : anchorElements) {
				Anchor anchor = (Anchor) loader.load(anchorChild);
				anchors.add(anchor);
			}
		}

		// bg Anchors
		if (XMLUtil.hasChild(element, "bganchors")) {
			Element bgAnchorsElement = XMLUtil.getChild(element, "bganchors");
			List<Element> bganchorElements = XMLUtil.getChildren(bgAnchorsElement, "anchor");
			if (bganchorElements.size() > 0) {
				bgAnchors = new ArrayList<Anchor>(bganchorElements.size());
				for (Element bganchorChild : bganchorElements) {
					Anchor bganchor = (Anchor) loader.load(bganchorChild);
					bgAnchors.add(bganchor);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doCreate()
	 */
	@Override
	protected void doCreate() {
		super.doCreate();

		for (Iterator<ActorFeature> i = actors.values().iterator(); i.hasNext(); ) {
			ActorFeature af = i.next();
			af.create();
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		super.doDestroy();

		for (Iterator<ActorFeature> i = actors.values().iterator(); i.hasNext(); ) {
			ActorFeature af = i.next();
			af.destroy();
		}
	}

}
