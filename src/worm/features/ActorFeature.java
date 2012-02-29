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
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.puppygames.applet.Anchor;
import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.Effect;
import net.puppygames.applet.effects.FadeEffect;

import org.lwjgl.util.Color;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Element;

import worm.animation.SimpleThingWithLayers;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.opengl.GLStyledText;
import com.shavenpuppy.jglib.opengl.GLStyledText.DefaultStyledText;
import com.shavenpuppy.jglib.opengl.GLStyledText.StyledText;
import com.shavenpuppy.jglib.opengl.GLStyledText.StyledTextFactory;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * An Actor in a Setting. Uses a CharacterFeature and has coordinates for itself and its speechbubble.
 */
public class ActorFeature extends Feature {

	private static final long serialVersionUID = 1L;

	private static final Color TEMP = new Color();
	private static final Color INVISIBLE = new Color(0,0,0,0);

	private static final int LEADING = -1;

	/** Where to place the character RELATIVE TO BOUNDS!! */
	private Point position;

	/** The character */
	private String character;

	/** Speech bubble bounds */
	private Rectangle bounds;

	/** Whether to mirror the character */
	private boolean mirrored;

	/** Whether to fade in the character */
	private boolean fade;

	/** How to expand */
	@Data
	private String expand = "up";

	@Data
	private String leading;
	private int textLeading;

	/** Anchors */
	private ArrayList<Anchor> anchors;

	/*
	 * Transient
	 */

	private transient CharacterFeature characterFeature;

	/**
	 * Instances
	 */
	private class ActorInstance extends Effect implements Actor {

		private static final int TEXT_DELAY = 30;
		private static final int FADE_IN_DURATION = 16;

		private final ArrayList<String> text = new ArrayList<String>(1);
		private int paragraph;
		private int tick, pos, nextCharDelay, voiceTick;
		private Background.Instance bg;
		private GLStyledText textArea;
		private boolean done;
		private SimpleThingWithLayers layersSprite;
		private Sprite foreground;
		private AnimatedTextFactory factory;
		private final Setting setting;
		private Rectangle instanceBounds;
		private int fadeAfter;

		private int delayTick;
		private int fadeInTick;
		private int nextParagraphDelayTick;
		private char last = 0;
		private SoundEffect currentSound;

		private class AnimatedTextFactory implements StyledTextFactory {

			int numVisibleGlyphs;
			private ReadableColor topColor, bottomColor;
			private GLFont font;
			private StringBuilder parsed;


			public AnimatedTextFactory() {
				topColor = characterFeature.getColor();
				bottomColor = topColor;
				font = net.puppygames.applet.Res.getTinyFont();
			}

			public void setNumVisibleGlyphs(int numVisibleGlyphs) {
				if (this.numVisibleGlyphs != numVisibleGlyphs) {
					this.numVisibleGlyphs = numVisibleGlyphs;
					textArea.setChanged(true);
				}
			}

			int length() {
				return parsed.length();
			}

			char charAt(int n) {
				return parsed.charAt(n);
			}

			@Override
			public void parse(String text, List<StyledText> dest) {
				dest.clear();

				// start parsing. Better get a { first!
				StringBuilder sb = new StringBuilder(text.length());
				parsed = new StringBuilder(text.length());
				int n = text.length();
				int style = -1;
				for (int i = 0; i < n; ) {
					char c = text.charAt(i);
					if (c == '{') {
						if (sb.length() > 0) {
							DefaultStyledText dst = new DefaultStyledText(sb.toString(), font, topColor, bottomColor);
							dest.add(dst);
							sb = new StringBuilder(text.length() - i);
						}
						i += parse(text, i + 1);
						if (style == 2) {
							topColor = bottomColor = INVISIBLE;
						}
					} else {
						if (parsed.length() >= numVisibleGlyphs) {
							if (style != 2) {
								if (sb.length() > 0) {
									DefaultStyledText dst = new DefaultStyledText(sb.toString(), font, topColor, bottomColor);
									dest.add(dst);
									sb = new StringBuilder(text.length() - i);
								}
								style = 2;
								// Make these glyphs invisible
								topColor = bottomColor = INVISIBLE;
							}
						} else {
							if ((Character.isUpperCase(c) || Character.isDigit(c) || c == '+' || c == '$') && style != 0) {
								if (sb.length() > 0) {
									DefaultStyledText dst = new DefaultStyledText(sb.toString(), font, topColor, bottomColor);
									dest.add(dst);
									sb = new StringBuilder(text.length() - i);
								}
								style = 0;
								topColor = characterFeature.getBoldColor();
								bottomColor = topColor;
							} else if (Character.isLowerCase(c) && style != 1) {
								if (sb.length() > 0) {
									DefaultStyledText dst = new DefaultStyledText(sb.toString(), font, topColor, bottomColor);
									dest.add(dst);
									sb = new StringBuilder(text.length() - i);
								}
								style = 1;
								topColor = characterFeature.getColor();
								bottomColor = topColor;
							}
						}
						sb.append(Character.toUpperCase(c));
						if (!Character.isWhitespace(c)) {
							parsed.append(c);
						}
						i ++;
					}
				}

				if (sb.length() > 0) {
					DefaultStyledText dst = new DefaultStyledText(sb.toString(), font, topColor, bottomColor);
					dest.add(dst);
				}
			}

			private int parse(String text, int pos) {
				// Find index of }
				int idx = text.indexOf('}', pos);
				// Get substring
				String format = text.substring(pos, idx);
				// Split into tokens separated by spaces
				StringTokenizer st = new StringTokenizer(format, " ");
				// Parse each token, a key:value pair
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					int colon = token.indexOf(':');
					if (colon == -1) {
						if (Game.DEBUG) {
							System.out.println("Bad token : "+token);
						}
					} else {
						String key = token.substring(0, colon).toLowerCase();
						String value = token.substring(colon + 1);
						if (key.equals("top")) {
							topColor = new MappedColor(value);
						} else if (key.equals("bottom")) {
							bottomColor = new MappedColor(value);
						} else if (key.equals("color")) {
							topColor = new MappedColor(value);
							bottomColor = topColor;
						} else if (key.equals("font")) {
							font = (GLFont) Resources.get(value);
						} else {
							if (Game.DEBUG) {
								System.out.println("Bad key : "+key);
							}
						}
					}
				}
				return idx - pos + 2;
			}
		}

		/**
		 * C'tor
		 * @param text The text that this actor will read out
		 */
		public ActorInstance(Setting setting) {
			this.setting = setting;
			instanceBounds = new Rectangle(bounds);
			if (leading == null) {
				textLeading = LEADING;
			} else {
				textLeading = Integer.parseInt(leading);
			}
		}

		@Override
		public void setFadeAfter(int time) {
			this.fadeAfter = time;
		}

		@Override
		public void addText(String text) {
			// chaz hack! just to make things a bit more readable in levels-.xml...   '\n' already works btw
			this.text.add(replace(text, "\n ", "\n"));
			if (this.text.size() == 1) {
				initTextAndBubble();
			}
		}

		private String replace(String src, String a, String b) {
	        return Pattern.compile(a, Pattern.LITERAL).matcher(src).replaceAll(Matcher.quoteReplacement(b));
		}

		@Override
		public CharacterFeature getCharacter() {
			return characterFeature;
		}

		@Override
		public void onResized() {
			// Apply anchors unless screen is "centred"
			if (anchors != null) {
				for (Anchor anchor : anchors) {
					anchor.apply(this);
				}
			} else if (position != null) {
				boolean centreX = setting.isCentred() || setting.isCentredX();
				boolean centreY = setting.isCentred() || setting.isCentredY();
				if (centreX || centreY) {
					int newX = centreX ? (Game.getWidth() - Game.getScale()) / 2 + bounds.getX() : bounds.getX();
					int newY = centreY ? (Game.getHeight() - Game.getScale()) / 2 + bounds.getY() : bounds.getY();
					setBounds(newX, newY, bounds.getWidth(), bounds.getHeight());
				}
			}

			if (layersSprite != null) {
				for (int i = 0; i < layersSprite.getSprites().length; i ++) {
					layersSprite.getSprite(i).setLocation(instanceBounds.getX() - bounds.getX() + position.getX(), instanceBounds.getY() - bounds.getY() + position.getY());
				}
			}
			if (foreground != null) {
				foreground.setLocation(instanceBounds.getX() - bounds.getX() + position.getX(), instanceBounds.getY() - bounds.getY() + position.getY());
			}

			int textHeight = textArea.getTextHeight();
			if ("down".equals(expand)) {
				textArea.setVerticalAlignment(GLStyledText.TOP);
				textArea.setLocation(instanceBounds.getX(), (instanceBounds.getY() + instanceBounds.getHeight() - Math.max(instanceBounds.getHeight(), textHeight)));
				textArea.setHeight(Math.max(instanceBounds.getHeight(), textHeight));

				if (bg!=null) {
					bg.setBounds(new Rectangle(instanceBounds.getX(), instanceBounds.getY() + instanceBounds.getHeight() - textArea.getHeight(), instanceBounds.getWidth(), textArea.getHeight()));
				}

			} else {
				textArea.setLocation(instanceBounds.getX(),instanceBounds.getY());
				textArea.setVerticalAlignment(GLStyledText.BOTTOM);
				textArea.setHeight(Math.max(instanceBounds.getHeight(), textHeight));
				if (bg!=null) {
					bg.setBounds(new Rectangle(instanceBounds.getX(), instanceBounds.getY(), instanceBounds.getWidth(), textArea.getHeight()));
				}
			}

		}

		@Override
		public ReadableRectangle getBounds() {
			return instanceBounds;
		}

		@Override
		public void setBounds(int x, int y, int w, int h) {
			instanceBounds.setBounds(x, y, w, h);
		}

		@Override
		protected void render() {
			if (delayTick > 0 || isPaused()) {
				return;
			}
			int alpha = (int) LinearInterpolator.instance.interpolate(0.0f, 255.0f, (float) fadeInTick / FADE_IN_DURATION);
			if (bg != null) {
				bg.setAlpha(alpha);
				bg.render(this);
			}
			textArea.render(this);
		}

		@Override
		public int getDefaultLayer() {
			return characterFeature.getBubbleLayer();
		}

		@Override
		protected void doSpawnEffect() {
			last = 0;
			if (!fade) {
				initCharacter();
			}
		}

		private void initTextAndBubble() {
			if (characterFeature.getBubble() != null) {
				bg = characterFeature.getBubble().spawn();
			}

			textArea = new GLStyledText();
			factory = new AnimatedTextFactory();
			textArea.setFactory(factory);
			textArea.setLeading(textLeading);
			textArea.setText(text.get(paragraph));
			textArea.setWidth(instanceBounds.getWidth());

			onResized();

			factory.setNumVisibleGlyphs(0);
			delayTick = TEXT_DELAY;

		}

		private void setAppearance(LayersFeature layers) {
			// Remove existing sprites
			if (layersSprite != null) {
				layersSprite.remove();
				layersSprite = null;
			}
			layersSprite = new SimpleThingWithLayers(getScreen());
			layers.createSprites(getScreen(), layersSprite);

			if (layersSprite.getSprites() != null && mirrored) {
				for (int i = 0; i < layersSprite.getSprites().length; i ++) {
					layersSprite.getSprite(i).setMirrored(true);
				}
			}

			if (foreground != null && mirrored) {
				foreground.setMirrored(true);
			}
		}

		@Override
		public void begin() {
			if (isPaused()) {
				setPaused(false);
				//start();
				init();

				if (characterFeature.getTalkLayers() != null) {
					setAppearance(characterFeature.getTalkLayers());
				}
			} else {
				// Next paragraph
				paragraph ++;
				pos = 0;
				tick = 0;
				voiceTick = 0;
				// Delay
				nextParagraphDelayTick = 180;
			}
		}

		@Override
		public void end() {
			if (isPaused()) {
				begin();
			}
			if (paragraph == text.size() - 1) {
				finish();
			}

		}

		@Override
		public boolean advance() {
			// Bollocks to fixing this :)
			return true;
		}

		@Override
		public void finish() {
			if (!done) {
				tick = 0;
				delayTick = 0;
				fadeInTick = FADE_IN_DURATION;
				factory.setNumVisibleGlyphs(pos = factory.length());
				if (foreground != null && characterFeature.isAnimated()) {
					foreground.setAppearance(characterFeature.getDefaultAppearance());
				}
				if (characterFeature.getIdleLayers() != null) {
					setAppearance(characterFeature.getIdleLayers());
				}

				if (fadeAfter != 0) {
					new FadeEffect(fadeAfter, FADE_IN_DURATION) {
						@Override
                        protected void onTicked() {
							textArea.setAlpha(getAlpha());
						}
					}.spawn(getScreen());
				}
			}
		}

		private void initCharacter() {
			if (characterFeature.getDefaultAppearance() != null) {
				foreground = getScreen().allocateSprite(ActorFeature.this);
				foreground.setLayer(characterFeature.getMouthLayer());
				foreground.setAppearance(characterFeature.getDefaultAppearance());
			}

			if (characterFeature.getIdleLayers() != null) {
				setAppearance(characterFeature.getIdleLayers());
			}
			if (layersSprite != null) {
				for (int i = 0; i < layersSprite.getSprites().length; i ++) {
					layersSprite.getSprite(i).setLocation(instanceBounds.getX() - bounds.getX() + position.getX(), instanceBounds.getY() - bounds.getY() + position.getY());
					if (fade) {
						layersSprite.getSprite(i).setAlpha(0);
					}
				}
			}
			if (foreground != null) {
				foreground.setLocation(instanceBounds.getX() - bounds.getX() + position.getX(), instanceBounds.getY() - bounds.getY() + position.getY());
				if (fade) {
					foreground.setAlpha(0);
				}
			}
		}

		@Override
		protected void doTick() {
			if (nextCharDelay == 0) {
				nextCharDelay = characterFeature.getTextSpeed();
			}

			if (nextParagraphDelayTick > 0) {
				nextParagraphDelayTick --;
				if (nextParagraphDelayTick == 0) {
					initTextAndBubble();
					if (characterFeature.getTalkLayers() != null) {
						setAppearance(characterFeature.getTalkLayers());
					}
				}
				return;
			}

			if (delayTick > 0) {
				delayTick --;
				if (delayTick == 0 && fade) {
					initCharacter();
				}
				return;
			}

			if (fadeInTick < FADE_IN_DURATION) {
				fadeInTick ++;
			}

			if (pos < factory.length()) {
				tick ++;
				if (tick > nextCharDelay) {
					tick = 0;
					char c = factory.charAt(pos ++);
					if (pos == factory.length()) {
						finish();
					}
					factory.setNumVisibleGlyphs(pos);
					c = Character.toLowerCase(c);
					if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y') {
						if (foreground != null && characterFeature.isAnimated()) {
							foreground.setAppearance(characterFeature.getVowelAppearance());
						}
						nextCharDelay = characterFeature.getTextSpeed();
					} else if (c == '.' || c == ',' || c == '!' || c == '?' || c == '&') {
						if (foreground != null && characterFeature.isAnimated()) {
							foreground.setAppearance(characterFeature.getDefaultAppearance());
						}
						nextCharDelay = characterFeature.getTextSpeed();
						if (characterFeature.isSpeech()) {
							nextCharDelay = characterFeature.getTextSpeed() * 3;
						}
					} else {
						if (foreground != null && characterFeature.isAnimated()) {
							foreground.setAppearance(characterFeature.getConsonantAppearance());
						}
						nextCharDelay = characterFeature.getTextSpeed();
					}
				}
				if (pos < factory.length()) {
					if (currentSound == null || !currentSound.isOwnedBy(this) || !currentSound.isActive() && currentSound.isOwnedBy(this)) {
						char c = factory.charAt(pos);
						voiceTick = characterFeature.getSpeechSpeed();
						ALBuffer buf = characterFeature.getSound(c);
						if (buf != null) {
							currentSound = Game.allocateSound(buf, 1.0f, 1.0f - (characterFeature.isSpeech() ? 0.05f * (float) Math.random() : 0.0f), this);
						}
					}
				}
			}
		}


		@Override
		public boolean isFinished() {
			return pos == factory.length();
		}

		@Override
		public boolean isEffectActive() {
			return !done;
		}

		@Override
		protected void doRemove() {
			done = true;
			if (layersSprite != null) {
				layersSprite.remove();
				layersSprite = null;
			}
			if (foreground != null) {
				foreground.deallocate();
				foreground = null;
			}
		}

		@Override
		protected void doUpdate() {

			int alpha = (int) LinearInterpolator.instance.interpolate(0.0f, 255.0f, (float) fadeInTick / FADE_IN_DURATION);
			if (layersSprite != null) {
				for (int i = 0; i < layersSprite.getSprites().length; i ++) {
					layersSprite.getSprite(i).setLocation(instanceBounds.getX() - bounds.getX() + position.getX(), instanceBounds.getY() - bounds.getY() + position.getY());
					if (fade) {
						layersSprite.getSprite(i).setAlpha(alpha);
					}
				}
			}
			if (foreground != null) {
				foreground.setLocation(instanceBounds.getX() - bounds.getX() + position.getX(), instanceBounds.getY() - bounds.getY() + position.getY());
				if (fade) {
					foreground.setAlpha(alpha);
				}
			}

			// chaz hack! - child offsets...

			if (layersSprite != null) {

				boolean searchForChildOffsets = false;

				for (int i = 0; i < layersSprite.getSprites().length; i ++) {
					// check anims for childOffset
					if (layersSprite.getSprite(i).isDoChildOffset()) {
						searchForChildOffsets=true;
					}
				}

				if (searchForChildOffsets) {

					float xOffset = 0;
					float yOffset = 0;
					float yOffsetTotal = 0;
					float xOffsetTotal = 0;

					for (int i = 0; i < layersSprite.getSprites().length; i ++) {

						boolean doOffset = false;

						// check for offset
						if (layersSprite.getSprite(i).getChildXOffset() != 0) {

							xOffset = layersSprite.getSprite(i).getChildXOffset();

							// offsets after first sprite in array arent scaled?
							if (i==0) {
								xOffset *= FPMath.floatValue(layersSprite.getSprite(0).getXScale());
							}

							xOffsetTotal += xOffset;
							doOffset = true;
						}
						if (layersSprite.getSprite(i).getChildYOffset() != 0) {

							yOffset = layersSprite.getSprite(i).getChildYOffset();

							// offsets after first sprite in array arent scaled?
							if (i==0) {
								yOffset *= FPMath.floatValue(layersSprite.getSprite(0).getYScale());
							}

							yOffsetTotal += yOffset;
							doOffset = true;
						}

						// if we've found an offset apply this to any sprites after where we found the offset

						if (doOffset) {

							if (mirrored) {
								xOffsetTotal = -xOffsetTotal;
							}

							for (int j = i+1; j < layersSprite.getSprites().length; j ++) {
								if (layersSprite.getSprite(j).isDoChildOffset()) {
									layersSprite.getSprite(j).setLocation(instanceBounds.getX() - bounds.getX() + position.getX() + xOffsetTotal, instanceBounds.getY() - bounds.getY() + position.getY() + yOffsetTotal);
								}
							}
						}
					}
				}
			}

			// chaz hack! - attach mouth to head - use first sprite in sprite

			if (foreground != null && layersSprite != null && !characterFeature.getSuppressChildOffsetMouth()) {

				boolean doOffset = false;
				float xOffset = 0, yOffset = 0;

				// check for offset
				if (layersSprite.getSprite(0).getChildXOffset() != 0) {
					xOffset = layersSprite.getSprite(0).getChildXOffset() * FPMath.floatValue(layersSprite.getSprite(0).getXScale());
					doOffset = true;
				}
				if (layersSprite.getSprite(0).getChildYOffset() != 0) {
					yOffset = layersSprite.getSprite(0).getChildYOffset() * FPMath.floatValue(layersSprite.getSprite(0).getYScale());
					doOffset = true;
				}

				if (doOffset) {
					if (mirrored) {
						xOffset = -xOffset;
					}
					foreground.setLocation(instanceBounds.getX() - bounds.getX() + position.getX() + xOffset, instanceBounds.getY() - bounds.getY() + position.getY() + yOffset);
				}
			}

		}
	}

	/**
	 * C'tor
	 */
	public ActorFeature() {
		setAutoCreated();
	}

	/**
	 * Spawn an instance of this actor, reading out the specified text.
	 * @param screen
	 * @param text
	 * @return the Actor
	 */
	public Actor spawn(Screen screen, Setting setting) {
		ActorInstance actor = new ActorInstance(setting);
		actor.spawn(screen);
		actor.setPaused(true);
		return actor;
	}

	/**
	 * @return the character name
	 */
	public String getCharacter() {
		return character;
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);
		// Anchors
		List<Element> anchorElements = XMLUtil.getChildren(element, "anchor");
		if (anchorElements.size() > 0) {
			anchors = new ArrayList<Anchor>(anchorElements.size());
			for (Element anchorChild : anchorElements) {
				Anchor anchor = (Anchor) loader.load(anchorChild);
				anchors.add(anchor);
			}
		}
	}
}
