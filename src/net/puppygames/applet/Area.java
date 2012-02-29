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

package net.puppygames.applet;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;
import net.puppygames.applet.effects.SFX;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.Color;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLStyledText;
import com.shavenpuppy.jglib.opengl.GLStyledText.HorizontalAlignment;
import com.shavenpuppy.jglib.opengl.GLStyledText.VerticalAlignment;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.resources.RectangleParser;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.sprites.SpriteImage;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.XMLUtil;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: Area.java,v 1.49 2010/11/03 16:21:37 foo Exp $
 * An Area in a Screen.
 * @author $Author: foo $
 * @version $Revision: 1.49 $
 */
public class Area extends Feature implements Tickable, Bounded {

	public static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	private static final Color TEMPCOLOR = new Color();

	/** Id */
	@Data
	private String id;

	/** Group or groups (separated by commas) */
	@Data
	private String group;

	/** Sprite image when mouse is not over the area */
	@Data
	private String mouseOff;

	/** Sprite image when mouse is over the area */
	@Data
	private String mouseOn;

	/** Sprite image when disabled */
	@Data
	private String disabled;

	/** Hitboxes */
	private boolean useBounds;
	private List<ReadableRectangle> hitBoxes;

	/** Position */
	private Point position;

	/** Optional size, if no images specified */
	private Dimension size;

	/** Optional sprite offset */
	private Point offset;

	/** Whether to click */
	private boolean noClick;

	/** Debug */
	private boolean debug;

	/** Layer */
	private int layer;

	/** Visibility */
	private boolean visible = true;

	/** Font */
	private String font;

	/** Text */
	@Data
	private String text;

	/** Text alignement */
	@Data
	private String halign, valign;

	/** Colour */
	private MappedColor color, disabledColor, mouseOnColor;

	/** Top colour */
	private MappedColor topColor, disabledTopColor, mouseOnTopColor;

	/** Bottom colour */
	private MappedColor bottomColor, disabledBottomColor, mouseOnBottomColor;

	/** Left focus */
	@Data
	private String leftFocus;

	/** Right focus */
	@Data
	private String rightFocus;

	/** Up focus */
	@Data
	private String upFocus;

	/** Down focus */
	@Data
	private String downFocus;

	/** Next focus */
	@Data
	private String nextFocus;

	/** Previous focus */
	@Data
	private String prevFocus;

	/** Default focus */
	private boolean defaultFocus;

	/** Mirrored */
	private boolean mirrored;

	/** Flipped */
	private boolean flipped;

	/** Background */
	@Data
	private String background, disabledBackground, mouseOnBackground;

	/** Background layer */
	private int bglayer;

	/** Text layer */
	private int textLayer;

	/** Leading, for text */
	private int leading;

	/** Flags for monkeying */
	private boolean hasSize, hasPosition;

	/** Whether to write text as sub-tag in XML output */
	private boolean textAsSubTag;

	/** Text offset */
	private Point textOffset;

	/** Emitter */
	@Data
	private String emitter;

	/** Emitter offset */
	private Point emitterOffset;

	/** Linked area */
	@Data
	private String link;

	/** Scale of Sprite image */
	private float scale = 0.0f;

	/** Text shadow colour */
	private MappedColor textShadowColor;

	/** Text shadow offset */
	private Point textShadowOffset;

	/** Text bounds */
	private Rectangle textBounds;

	/** Initial alpha */
	private int alpha, textAlpha;

	/** Master */
	@Data
	private String master;

	/** Anchors */
	private ArrayList<Anchor> anchors;

	/** Hold click down */
	private boolean holdClick;

	/** All caps */
	private boolean allCaps;

	/** Grab on click */
	private boolean grab;

	/*
	 * Transient data
	 */

	private transient Appearance mouseOnResource, mouseOffResource, disabledResource;
	private transient int state;
	private static final int STATE_MOUSEOFF = 0;
	private static final int STATE_MOUSEON = 1;
	private static final int STATE_DISABLED = 2;
	private transient Sprite sprite;
	private transient Rectangle bounds;
	private transient Screen screen;
	private transient boolean ignoreMouse;
	private transient boolean enabled;
	private transient GLStyledText textArea;
	private transient String[] groups;
	private transient Background backgroundResource, disabledBackgroundResource, mouseOnBackgroundResource;
	private transient TickableObject backgroundObject, textObject;
	private transient GLFont fontResource;
	private transient boolean selectDown, armed, initing, waitForMouse;
	private transient EmitterFeature emitterFeature;
	private transient Appearance currentAppearance;
	private transient Background.Instance backgroundInstance;
	private transient Emitter emitInstance;
	private transient List<Area> slaves;
	private transient Area masterArea;

	/**
	 * C'tor
	 */
	public Area() {
		super();
		setAutoCreated();
	}

	/**
	 * C'tor
	 * @param name
	 */
	public Area(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return the bounds (never null)
	 */
	@Override
	public ReadableRectangle getBounds() {
		if (bounds == null) {
			if (sprite != null) {
				SpriteImage image = sprite.getImage();
				if (image != null) {
					bounds = new Rectangle((int) sprite.getX(), (int) sprite.getY(), image.getWidth(), image.getHeight());
				}
			}
			if (bounds == null) {
				bounds = new Rectangle(0, 0, 0, 0);
			}
		}
		return bounds;
	}

	/**
	 * @return the layer
	 */
	public int getLayer() {
		return layer;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		if (size != null) {
			hasSize = true;
		}
		if (position != null) {
			hasPosition = true;
		}
		if (text == null && XMLUtil.hasChild(element, "text")) {
			text = XMLUtil.getText(XMLUtil.getChild(element, "text"), "<missing text>");
			textAsSubTag = true;
		}
		if (!XMLUtil.hasAttribute(element, "alpha")) {
			alpha = 255;
		}
		if (!XMLUtil.hasAttribute(element, "textAlpha")) {
			textAlpha = 255;
		}
		Element hitboxesElement = XMLUtil.getChild(element, "hitboxes");
		if (hitboxesElement != null) {
			useBounds = XMLUtil.getBoolean(hitboxesElement, "useBounds", false);
			List<Element> hitBoxesChildren = XMLUtil.getChildren(hitboxesElement, "hitbox");
			hitBoxes = new ArrayList<ReadableRectangle>(hitBoxesChildren.size() + 1);
			for (Element child : hitBoxesChildren) {
				Rectangle r = RectangleParser.parse(XMLUtil.getText(child, "0,0,0,0"));
				hitBoxes.add(r);
			}
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

		if (allCaps && text != null) {
			text = text.toUpperCase();
		}
	}

	/**
	 * Called immediately when screen is opened
	 */
	public void init() {

		onResized();

		if (sprite != null && mouseOffResource != null) {
			sprite.setAppearance(mouseOffResource);
			sprite.rewind();
			if (offset != null) {
				sprite.setOffset(offset.getX(), offset.getY());
			}
			updateScale();
		}

		maybeCreateTextObject();

		if (emitterFeature != null) {
			initEmitter();
		}

		doSetVisible(visible);
		initing = true;
		setAppearanceForState();
		syncSlaves();
		waitForMouse = true;
		initing = false;
	}

	public void waitForMouse() {
		waitForMouse = true;
	}

	public void onResized() {
		// Apply anchors unless screen is "centred"
		if (anchors != null) {
			for (Anchor anchor : anchors) {
				anchor.apply(this);
			}
		} else if (position != null) {
			boolean centreX = screen.isCentred() || screen.isCentredX();
			boolean centreY = screen.isCentred() || screen.isCentredY();
			if (centreX || centreY) {
				ReadableRectangle bounds = getBounds();
				int newX = centreX ? (Game.getWidth() - Game.getScale()) / 2 + position.getX() : position.getX();
				int newY = centreY ? (Game.getHeight() - Game.getScale()) / 2 + position.getY() : position.getY();
				boolean hasPositionOld = hasPosition; // Hack to workaround position being changed in setBounds
				hasPosition = false;
				setBounds(newX, newY, bounds.getWidth(), bounds.getHeight());
				hasPosition = hasPositionOld;
			}
		}

		updateTextColors();
	}

	/**
	 * Maybe create text object?
	 */
	private void maybeCreateTextObject() {
		if (textArea != null && getScreen() != null) {
			if (textObject != null) {
				textObject.remove();
				textObject = null;
			}
			textObject = new TickableObject((text.length() + 32) * 4) {
				@Override
				protected void render() {
					if (textShadowColor != null && textShadowOffset != null) {
						glRender(new GLRenderable() {
							@Override
							public void render() {
								glPushMatrix();
								glTranslatef(textShadowOffset.getX(), textShadowOffset.getY(), 0.0f);
							}
						});
						ColorUtil.setAlpha(textShadowColor, alpha, TEMPCOLOR);
						textArea.setColor(TEMPCOLOR);
						try {
							textArea.render(this);
						} catch (Exception e) {
							System.out.println("Failed to render "+Area.this+" due to "+e);
							e.printStackTrace();
						}
						updateTextColors();
						glRender(new GLRenderable() {
							@Override
							public void render() {
								glPopMatrix();
							}
						});
						textArea.setColor(ReadableColor.WHITE);
					}
					textArea.render(this);
				}
			};
			textObject.setLayer(textLayer);
			textObject.setVisible(visible);
			textObject.spawn(screen);
		}
	}

	/**
	 * Create an emitter
	 */
	private void initEmitter() {
		emitInstance = emitterFeature.spawn(getScreen());
		int ox, oy;
		if (emitterOffset != null) {
			ox = emitterOffset.getX();
			oy = emitterOffset.getY();
		} else {
			ox = 0;
			oy = 0;
		}
		emitInstance.setLocation(position.getX() + ox, position.getY() + oy);
	}

	@Override
	protected void doCreate() {
		super.doCreate();

		if (group != null) {
			StringTokenizer st = new StringTokenizer(group, ",", false);
			groups = new String[st.countTokens()];

			for (int count = 0 ; st.hasMoreTokens(); count ++) {
				groups[count] = st.nextToken().trim();
			}

		}

		if (mouseOffResource != null) {
			SpriteImage img;
			if (mouseOffResource instanceof SpriteImage) {
				img = (SpriteImage) mouseOffResource;
				if (position != null && img != null) {
					bounds = new Rectangle(position.getX() - img.getHotspotX(), position.getY() - img.getHotspotY(),
							img.getWidth(), img.getHeight());
					if (!hasSize) {
						size = new Dimension(img.getWidth(), img.getHeight());
					}
				}
			}
		}
		if (size == null && mouseOff != null) {
			//throw new Exception("Can't create area "+this+": "+mouseOff+" not found.");
		}

		// If both size and position are explicit, override any bounds from the mouseOff image
		if (size != null && position != null) {
			bounds = new Rectangle(position, size);
		} else if (position != null) {
			bounds = new Rectangle(position, new Dimension(0, 0));
		} else if (size != null) {
			bounds = new Rectangle(new Point(0, 0), size);
		} else {
			bounds = new Rectangle();
		}

		if (text != null) {
			createTextArea();
		}

		enabled = true;

	}

	@Override
	protected void doDestroy() {
		super.doDestroy();

		cleanup();
	}

	private void maybeCreateSprite() {
		if (mouseOffResource != null) {
			if (sprite != null) {
				sprite.setVisible(visible);
				sprite.setAlpha(alpha);
				return;
			}
			sprite = screen.allocateSprite(this);
			sprite.setAlpha(alpha);
			sprite.setLayer(layer);
			sprite.setLocation(bounds.getX(), bounds.getY());
			sprite.setMirrored(mirrored);
			sprite.setFlipped(flipped);
			sprite.setVisible(visible);
			if (getOffset() != null) {
				sprite.setOffset(getOffset().getX(), getOffset().getY());
			}
			updateScale();
		} else {
			if (sprite != null) {
				if (sprite.isAllocated()) {
					sprite.deallocate();
				}
				sprite = null;
			}
		}
	}

	private void updateScale() {
		if (scale != 1 && scale != 0 && sprite != null) {
			sprite.setScale(FPMath.fpValue(scale));
		}
	}

	/**
	 * Init
	 * @param newScreen
	 */
	@Override
	public void spawn(Screen newScreen) {
		this.screen = newScreen;

		newScreen.addTickable(this);

		maybeCreateSprite();
		setBackground(backgroundResource);

		if (master != null) {
			masterArea = newScreen.getArea(master);
			if (masterArea == null) {
				System.out.println("Warning: Area "+id+" references master "+master+", which does not exist on screen "+newScreen);
			} else {
				masterArea.addSlave(this);
			}
		}

		syncSlaves();
	}

	private void addSlave(Area slave) {
		if (slaves == null) {
			slaves = new ArrayList<Area>();
		}
		if (!slaves.contains(slave)) {
			slaves.add(slave);
		}
		syncSlaves();
	}

	/**
	 * Synchronizes mouse over, visible, and enabled state down to each of the slaves
	 */
	private void syncSlaves() {
		syncVisibility();
		syncEnabled();
		syncStates();
	}

	private GLFont selectFont() {
		GLFont ret = fontResource;
		if (ret == null) {
			ret = Res.getTinyFont();
		}

		return ret;
	}

	/**
	 * Cleanup
	 */
	public void cleanup() {
		if (sprite != null) {
			if (sprite.isAllocated()) {
				sprite.deallocate();
			}
			sprite = null;
		}
		if (backgroundObject != null) {
			backgroundObject.remove();
			backgroundObject = null;
		}
		if (textObject != null) {
			textObject.remove();
			textObject = null;
		}

		if (emitInstance != null) {
			emitInstance.remove();
			emitInstance = null;
		}

		currentAppearance = null;
		slaves = null;
	}

	@Override
	public void update() {
		// No need to do anything, it's all handled in tick()
	}

	private boolean isInHitZone(int x, int y) {
		// Check slaves
		if (slaves != null) {
			for (int i = 0; i < slaves.size(); i ++) {
				Area slave = slaves.get(i);
				if (slave.isInHitZone(x, y)) {
					return true;
				}
			}
		}
		if (hitBoxes != null) {
			for (int i = 0; i < hitBoxes.size(); i ++) {
				Rectangle rect = (Rectangle) hitBoxes.get(i);
				if (rect.contains(x, y)) {
					return true;
				}
			}
		} else {
			if (getBounds() != null) {
				return bounds.contains(x, y);
			}
		}
		return false;
	}

	@Override
	public void tick() {
		List<MouseEvent> mouseEvents = Game.getMouseEvents();
		int n = mouseEvents.size();
		if (n == 0) {
			doTick(null);
		} else {
			for (int i = 0; i < n; i ++) {
				MouseEvent event = mouseEvents.get(i);
				doTick(event);
			}
		}
	}

	private void doTick(MouseEvent event) {
		if (!screen.isOpen()) {
			return;
		}

		if (screen.getGrabbed() != null && screen.getGrabbed() != this) {
			return;
		}

		if (masterArea != null) {
			return;
		}

		boolean mouseDown = Mouse.isButtonDown(0);
		if (waitForMouse) {
			if (mouseDown) {
				if (!enabled) {
					setState(STATE_DISABLED);
				} else {
					setState(STATE_MOUSEOFF);
				}
				return;
			} else {
				waitForMouse = false;
			}
		}

		armed = false;
		if (isVisible()) {
			if (event != null && event.getButton() == 0) {
				processMouseChange(event.isButtonDown());
			} else {
				processMouseChange(selectDown);
			}
		}
	}

	/**
	 * Helper function to process mouse events.
	 * @param mouseDown
	 * @see #tick()
	 */
	private void processMouseChange(boolean mouseDown) {
		boolean clicked = false;

		armed = isInHitZone(screen.getMouseX(), screen.getMouseY());
		selectDown = mouseDown && armed;

		if (selectDown && armed) {
			clicked = true;
			if (grab) {
				screen.setGrabbed(this);
			}
		} else if (grab && !selectDown) {
			screen.setGrabbed(null);
		}

		if (!enabled) {
			setState(STATE_DISABLED);
		} else if (screen.isEnabled()) {
			if (armed) {
				setState(STATE_MOUSEON);
			} else {
				setState(STATE_MOUSEOFF);
			}
		} else {
			setState(STATE_MOUSEOFF);
		}

		if (selectDown && visible && armed && !noClick && !ignoreMouse && enabled && screen.isEnabled() && clicked) {
			if (!holdClick) {
				SFX.buttonClick();
				ignoreMouse = true;
			}
			doClick();
		} else if (!selectDown) {
			ignoreMouse = false;
		}
	}

	public void doClick() {
		if (master != null) {
			masterArea.doClick();
		} else {
			screen.onClicked(id);
		}
	}

	private void setBackground(Background newBG) {
		if (newBG != null) {
			backgroundInstance = newBG.spawn();
			backgroundInstance.setBounds(bounds);
			backgroundInstance.setAlpha(alpha);
			createBackgroundObject();
		} else {
			if (backgroundObject != null) {
				backgroundObject.remove();
				backgroundObject = null;
			}
			backgroundInstance = null;
		}
	}

	private void createBackgroundObject() {
		if (backgroundObject != null) {
			backgroundObject.remove();
			backgroundObject = null;
		}

		backgroundObject = new TickableObject() {
			@Override
			protected void render() {
				backgroundInstance.render(this);
			}
		};
		backgroundObject.spawn(screen);
		backgroundObject.setLayer(bglayer);
		backgroundObject.setVisible(visible);
	}

	public void setMouseOnAppearance(Appearance newMouseOnResource) {
		initing = true;
		mouseOnResource = newMouseOnResource;
		currentAppearance = null;
		setAppearanceForState();
		initing = false;
	}

	public void setMouseOffAppearance(Appearance newMouseOffResource) {
		initing = true;
		mouseOffResource = newMouseOffResource;
		currentAppearance = null;
		maybeCreateSprite();
		setAppearanceForState();
		initing = false;
	}

	public Appearance getMouseOffAppearance() {
		return mouseOffResource;
	}

	/**
	 * @return the mouseOnResource
	 */
	public Appearance getMouseOnAppearance() {
		return mouseOnResource;
	}

	/**
	 * @return the disabledResource
	 */
	public Appearance getDisabledAppearance() {
		return disabledResource;
	}

	public void setDisabledAppearance(Appearance newDisabledResource) {
		initing = true;
		disabledResource = newDisabledResource;
		currentAppearance = null;
		setAppearanceForState();
		initing = false;
	}

	private void setAppearanceForState() {
		switch (state) {
			case STATE_MOUSEOFF:
				if (mouseOffResource != null) {
					setAppearance(mouseOffResource);
				}
				setBackground(backgroundResource);
				if (visible && !noClick && !initing) {
					screen.onHover(id, false);
				}
				break;
			case STATE_MOUSEON:
				if (mouseOnResource != null) {
					setAppearance(mouseOnResource);
				}
				if (mouseOnBackgroundResource != null) {
					setBackground(mouseOnBackgroundResource);
				} else {
					setBackground(backgroundResource);
				}
				if (visible && !noClick && !initing) {
					SFX.buttonHover();
					screen.onHover(id, true);
				}
				break;
			case STATE_DISABLED:
				if (disabledResource != null) {
					setAppearance(disabledResource);
				}
				if (disabledBackgroundResource != null) {
					setBackground(disabledBackgroundResource);
				} else {
					setBackground(backgroundResource);
				}
				break;
			default:
				assert false;
		}
		updateTextColors();
	}

	private void setState(int newState) {
		if (state == newState) {
			return;
		}
		state = newState;
		setAppearanceForState();
		syncStates();
	}

	/**
	 * Synchronizes appearance, armed, and selectDown down to slaves
	 */
	private void syncStates() {
		if (slaves == null) {
			return;
		}
		for (int i = 0; i < slaves.size(); i ++) {
			Area slave = slaves.get(i);
			slave.armed = armed;
			slave.selectDown = selectDown;
			slave.setState(state);
		}
	}

	private void setAppearance(Appearance appearance) {
		if (sprite == null || currentAppearance == appearance) {
			return;
		}
		sprite.setAnimation(null);
		sprite.setAppearance(appearance);
		currentAppearance = appearance;
	}

//	/**
//	 * Maybe render something
//	 */
//	public void render() {
//		if (position != null && size != null && debug) {
//			glDisable(GL_TEXTURE_2D);
//			glEnable(GL_BLEND);
//			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
//			glColor4f(1.0f, 1.0f, 1.0f, 0.2f);
//			glBegin(GL_QUADS);
//			glVertex2i(position.getX(), position.getY());
//			glVertex2i(position.getX() + size.getWidth(), position.getY());
//			glVertex2i(position.getX() + size.getWidth(), position.getY() + size.getHeight());
//			glVertex2i(position.getX(), position.getY() + size.getHeight());
//			glEnd();
//		}
//	}

	/**
	 * Get the area's ID
	 * @return String
	 */
	public String getID() {
		return id;
	}

	@Override
	public String toString() {
		return "Area["+id+"]";
	}

	/**
	 * Get the area's sprite
	 * @return sprite
	 */
	public Sprite getSprite() {
		return sprite;
	}

	/**
	 * @return the Screen the Area is on
	 */
	public Screen getScreen() {
		return screen;
	}

	/**
	 * Set visible
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		if (this.visible == visible) {
			// Do linked area anyway
			if (link != null) {
				Area linked = screen.getArea(link);
				if (linked != null) {
					linked.setVisible(visible);
				} else {
					System.out.println("Linked area '"+link+"' does not exist");
				}
			}

			return;
		}
		doSetVisible(visible);
	}

	protected void doSetVisible(boolean visible) {
		this.visible = visible;
		if (visible) {
			// Ignore the mouse until it is released
			ignoreMouse = true;
		}
		if (sprite != null) {
			sprite.setVisible(visible);
			// Restart any animation
			if (visible) {
				if (isEnabled() || disabledResource == null) {
					mouseOffResource.toSprite(sprite);
					currentAppearance = mouseOffResource;
				} else {
					disabledResource.toSprite(sprite);
					currentAppearance = disabledResource;
				}
			} else {
				currentAppearance = null;
				sprite.rewind();
			}
		}
		if (backgroundObject != null) {
			backgroundObject.setVisible(visible);
			if (sprite != null) {
				sprite.setActive(visible);
			}
		}
		if (textObject != null) {
			textObject.setVisible(visible);
		}
		if (emitInstance != null) {
			if (!visible) {
				emitInstance.remove();
				emitInstance = null;
			}
		} else if (emitterFeature != null) {
			initEmitter();
		}

		// Do link, if any
		if (link != null) {
			Area linked = screen.getArea(link);
			if (linked != null) {
				linked.doSetVisible(visible);
			} else {
				System.out.println("Linked area '"+link+"' does not exist");
			}
		}

		syncVisibility();

		if (visible && isFocusable()) {
			requestFocus();
		}
	}

	/**
	 * Synchronizes visibility down to slaves
	 */
	private void syncVisibility() {
		if (slaves == null) {
			return;
		}
		for (int i = 0; i < slaves.size(); i ++) {
			Area slave = slaves.get(i);
			slave.setVisible(visible);
		}
	}



	/**
	 * Set alpha
	 * @param alpha 0..255
	 */
	public void setAlpha(int alpha) {
		this.alpha = alpha;
		if (sprite != null) {
			sprite.setAlpha(alpha);
		}
		if (backgroundInstance != null) {
			backgroundInstance.setAlpha(alpha);
		}

		updateTextColors();

	}

	/**
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;

		setState(enabled ? STATE_MOUSEOFF : STATE_DISABLED);
		setAppearanceForState();
//		if (sprite != null) {
//			if (enabled) {
//				setAppearance(mouseOffResource);
//			} else if (disabledResource != null) {
//				setAppearance(disabledResource);
//			}
//		}
		if (link != null) {
			Area linked = screen.getArea(link);
			if (linked != null) {
				linked.setEnabled(enabled);
			} else {
				System.err.println("Linked area '"+link+"' does not exist");
			}
		}

		syncEnabled();
	}

	/**
	 * Synchronizes visibility down to slaves
	 */
	private void syncEnabled() {
		if (slaves == null) {
			return;
		}
		for (int i = 0; i < slaves.size(); i ++) {
			Area slave = slaves.get(i);
			slave.setEnabled(enabled);
		}
	}


	/**
	 * Is the area enabled?
	 * @return boolean
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Is the area visible?
	 * @return boolean
	 */
	public boolean isVisible() {
		return visible;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Tickable#isActive()
	 */
	@Override
	public boolean isActive() {
		return isCreated();
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Tickable#remove()
	 */
	@Override
	public void remove() {
		// Ignore
	}

	/**
	 * Gets the name of the Area that should receive focus next
	 * @return String or null
	 */
	public String getNextFocus() {
		return nextFocus;
	}

	/**
	 * Gets the name of the Area that should receive focus before
	 * @return String or null
	 */
	public String getPrevFocus() {
		return prevFocus;
	}

	public String getLeftFocus() {
		return leftFocus;
	}

	public String getRightFocus() {
		return rightFocus;
	}

	public String getUpFocus() {
		return upFocus;
	}

	public String getDownFocus() {
		return downFocus;
	}

	/**
	 * Are we focused?
	 * @return boolean
	 */
	public boolean isFocused() {
		return screen.getFocus() == this && screen.isKeyboardNavigationEnabled() && visible && enabled && !noClick;
	}

	/**
	 * @return
	 */
	public boolean isDefaultFocus() {
		return defaultFocus;
	}

	/**
	 * Are we focusable?
	 * @return boolean
	 */
	public boolean isFocusable() {
		return visible && enabled && !noClick && masterArea == null;
	}

	/**
	 * Is this area in this group?
	 * @param groupToCheck
	 */
	public boolean isInGroup(String groupToCheck) {
		if (groupToCheck == null || groups == null) {
			return false;
		}
		for (int i = 0; i < groups.length; i ++) {
			if (groupToCheck.equals(groups[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Request focus on this Area
	 */
	private void requestFocus() {
		screen.requestFocus(this);
	}

	/**
	 * @return the background instance, if any
	 */
	public Background.Instance getBackground() {
		return backgroundInstance;
	}

	/**
	 * Get the current image displayed, if any
	 * @return a SpriteImage, or null
	 */
	public SpriteImage getCurrentImage() {
		return sprite != null ? sprite.getImage() : null;
	}

	/**
	 * @return the position
	 */
	public ReadablePoint getPosition() {
		return position;
	}

	/**
	 * Set the bounds
	 */
	@Override
	public void setBounds(int x, int y, int w, int h) {
		bounds.setBounds(x, y, w, h);
		if (sprite != null) {
			sprite.setLocation(x, y);
		}
		if (hasSize) {
			size.setSize(w, h);
		}
		if (hasPosition) {
			position.setLocation(x, y);
		}
		if (textArea != null) {
			updateTextBounds();
		}
		if (backgroundInstance != null) {
			backgroundInstance.setBounds(bounds);
		}
		if (emitInstance != null) {
			int ox, oy;
			if (emitterOffset != null) {
				ox = emitterOffset.getX();
				oy = emitterOffset.getY();
			} else {
				ox = 0;
				oy = 0;
			}
			emitInstance.setLocation(x + ox, y + oy);
		}
	}

	public void setTextOffset(int x, int y) {
		if (x == 0 && y == 0) {
			textOffset = null;
		} else {
			textOffset = new Point(x, y);
		}
		updateTextBounds();
	}

	public GLFont getFont() {
		return fontResource;
	}

	public String getText() {
		return text;
	}

	/**
	 * @return the textOffset
	 */
	public ReadablePoint getTextOffset() {
		if (textOffset == null) {
			return new Point();
		} else {
			return textOffset;
		}
	}

	private void updateTextBounds() {
		if (textArea == null) {
			return;
		}
		Rectangle b = null;
		if (bounds != null) {
			b = bounds;
		}
		if (textBounds != null) {
			b = textBounds;
			if (bounds != null) {
				b.translate(bounds);
			}
		}
		if (b == null) {
			return;
		}
		int x = b.getX();
		int y = b.getY();
		int w = b.getWidth();
		int h = b.getHeight();
		if (textOffset != null) {
			x += textOffset.getX();
			y += textOffset.getY();
		}
		textArea.setBounds(x, y, w, h);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#shouldWriteAttribute(java.lang.String)
	 */
	@Override
	protected boolean shouldWriteAttribute(String attribute) {
		if 	(
				"text".equals(attribute)
			|| 	"textAsSubTag".equals(attribute)
			|| 	"hasSize".equals(attribute)
			|| 	"hasPosition".equals(attribute)
			||	"useBounds".equals(attribute)
			|| 	(
					"size".equals(attribute)
				&&	!hasSize
				)
			|| 	(
					"position".equals(attribute)
				&&	!hasPosition
				)
			||	(
					"alpha".equals(attribute)
				&&	alpha == 255
				)
			)
		{
			return false;
		} else {
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doWriteAttributes(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doWriteAttributes(XMLResourceWriter writer) throws IOException {
		if (!textAsSubTag) {
			writer.writeAttribute("text", text);
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doWriteChildren(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doWriteChildren(XMLResourceWriter writer) throws IOException {
		if (textAsSubTag) {
			writer.writeTag("text");
			writer.writeText(text);
			writer.closeTag();
		}
		if (hitBoxes != null) {
			writer.writeTag("hitboxes");
			if (useBounds) {
				writer.writeAttribute("useBounds", true);
			}
			for (ReadableRectangle r : hitBoxes) {
				writer.writeTag("hitbox");
				writer.writeText(String.valueOf(r.getX()));
				writer.writeText(", ");
				writer.writeText(String.valueOf(r.getY()));
				writer.writeText(", ");
				writer.writeText(String.valueOf(r.getWidth()));
				writer.writeText(", ");
				writer.writeText(String.valueOf(r.getHeight()));
				writer.closeTag();
			}
			writer.closeTag();
		}
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = allCaps ? text.toUpperCase() : text;

		if (textArea == null && text != null) {
			// Create text area
			createTextArea();
		} else if (textArea != null && text == null) {
			// Remove text area
			textArea = null;
			if (textObject != null) {
				textObject.remove();
				textObject = null;
			}
		} else if (textArea != null && text != null) {
			// Update text area
		//	textArea.setText(getFormatString() + text);

			textArea.setText(text);
		}
	}

	private String getFormatString() {
		StringBuilder sb = new StringBuilder(32);

		sb.append("{font:");
		sb.append(selectFont().getName());
		if (color != null) {
			sb.append(" color:");
			sb.append(color);
		} else {
			if (topColor != null) {
				sb.append(" top:");
				sb.append(topColor);
			}
			if (bottomColor != null) {
				sb.append(" bottom:");
				sb.append(bottomColor);
			}
		}
		sb.append("}");

		return sb.toString();
	}

	private void createTextArea() {
		textArea = new GLStyledText();
		textArea.setText(text);
		textArea.setLeading(leading);
		textArea.setAlpha(textAlpha);
		try {
			textArea.setVerticalAlignment(valign == null ? GLStyledText.TOP : (VerticalAlignment) VerticalAlignment.decode(valign));
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		try {
			textArea.setHorizontalAlignment(halign == null ? GLStyledText.CENTERED : (HorizontalAlignment) HorizontalAlignment.decode(halign));
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		updateTextColors();
		updateTextBounds();
		maybeCreateTextObject();
	}

	private void updateTextColors() {
		if (textArea == null) {
			return;
		}

		ReadableColor currentColor, currentTopColor, currentBottomColor;

		switch (state) {
			case STATE_DISABLED:
				currentColor = disabledColor != null ? disabledColor : color;
				currentTopColor = disabledTopColor != null ? disabledTopColor : topColor;
				currentBottomColor = disabledBottomColor != null ? disabledBottomColor : bottomColor;
				break;
			case STATE_MOUSEOFF:
				currentColor = color;
				currentTopColor = topColor;
				currentBottomColor = bottomColor;
				break;
			case STATE_MOUSEON:
				currentColor = mouseOnColor != null ? mouseOnColor : color;
				currentTopColor = mouseOnTopColor != null ? mouseOnTopColor : topColor;
				currentBottomColor = mouseOnBottomColor != null ? mouseOnBottomColor : bottomColor;
				break;
			default:
				assert false;
				return;
		}

		if (currentTopColor != null & currentBottomColor != null) {
			textArea.setFactory(new GLStyledText.DefaultStyledTextFactory(currentTopColor, currentBottomColor, selectFont()));
		} else if (currentColor != null) {
			textArea.setFactory(new GLStyledText.DefaultStyledTextFactory(currentColor, currentColor, selectFont()));
		} else {
			textArea.setFactory(new GLStyledText.DefaultStyledTextFactory(ReadableColor.WHITE, ReadableColor.WHITE, selectFont()));
		}
		textArea.setAlpha((alpha * textAlpha) / 255);
	}


	// chaz hack!

//	private void updateBackgroundColors() {
//		if (backgroundInstance == null) {
//			return;
//		}
//
//		ReadableColor currentColor;
//
//
//		switch (state) {
//			case STATE_DISABLED:
//				currentColor = bgDisabledColor != null ? bgDisabledColor : bgColor;
//				break;
//			case STATE_MOUSEOFF:
//				currentColor = bgColor;
//				break;
//			case STATE_MOUSEON:
//				currentColor = bgMouseOnColor != null ? bgMouseOnColor : bgColor;
//				break;
//			default:
//				assert false;
//				return;
//		}
//
//		if (currentColor != null) {
//			backgroundInstance.setColor(currentColor);
//			backgroundInstance.setAlpha(alpha);
//		}
//	}





	/**
	 * @return the selectDown
	 */
	public boolean isSelectDown() {
		return selectDown;
	}

	/**
	 * @return the armed
	 */
	public boolean isArmed() {
		return armed;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(int x, int y) {
		if (x == 0 && y == 0) {
			this.offset = null;
		} else {
			this.offset = new Point(x, y);
		}
		if (sprite != null) {
			sprite.setOffset(x, y);
		}
	}

	/**
	 * @return the offset; may be null
	 */
	public ReadablePoint getOffset() {
		return offset;
	}

	/**
	 * @return the textBounds
	 */
	public Rectangle getTextBounds() {
		return textBounds;
	}

	public void setTextColors(ReadableColor top, ReadableColor bottom) {
		if (topColor == null) {
			topColor = new MappedColor();
		}
		if (bottomColor == null) {
			bottomColor = new MappedColor();
		}
		topColor.setColor(top);
		bottomColor.setColor(bottom);
		updateTextColors();
	}

	public void setDisabledTextColors(ReadableColor top, ReadableColor bottom) {
		if (disabledTopColor == null) {
			disabledTopColor = new MappedColor();
		}
		if (disabledBottomColor == null) {
			disabledBottomColor = new MappedColor();
		}
		disabledTopColor.setColor(top);
		disabledBottomColor.setColor(bottom);
		updateTextColors();
	}

	public void setMouseOnTextColors(ReadableColor top, ReadableColor bottom) {
		if (mouseOnTopColor == null) {
			mouseOnTopColor = new MappedColor();
		}
		if (mouseOnBottomColor == null) {
			mouseOnBottomColor = new MappedColor();
		}
		mouseOnTopColor.setColor(top);
		mouseOnBottomColor.setColor(bottom);
		updateTextColors();
	}

	public void setTextAlpha(int textAlpha) {
		this.textAlpha = textAlpha;
		if (textArea != null) {
			textArea.setAlpha((alpha * textAlpha) / 255);
		}
	}

	public int getTextHeight() {
		return textArea != null ? textArea.getHeight() : 0;
	}

	public boolean isAllCaps() {
	    return allCaps;
    }

	public void setAllCaps(boolean allCaps) {
	    this.allCaps = allCaps;
    }
}
