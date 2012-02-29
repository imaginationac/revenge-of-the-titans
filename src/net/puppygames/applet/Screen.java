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
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.puppygames.applet.effects.SFX;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.openal.ALStream;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLString;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;
import com.shavenpuppy.jglib.sprites.SpriteEngine;
import com.shavenpuppy.jglib.sprites.SpriteImage;
import com.shavenpuppy.jglib.sprites.StaticSpriteEngine;
import com.shavenpuppy.jglib.util.XMLUtil;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: Screen.java,v 1.32 2010/09/02 23:42:44 foo Exp $
 * A Screen. This consists of a number of named Areas.
 * @author $Author: foo $
 * @version $Revision: 1.32 $
 */
public abstract class Screen extends Feature implements SpriteAllocator {

	public static final long serialVersionUID = 1L;

	private static final int MINIMUM_MUSIC_FADE_DURATION = 30;

	/*
	 * Static data
	 */

	/** All the open screens, in the order they should be rendered */
	private static final List<Screen> SCREENS = new ArrayList<Screen>(4);

	/** Extra ticking to do as a result of trying to tick within tick */
	private static final List<Screen> EXTRA_TICKING = new ArrayList<Screen>(1);

	/** Monkeying: slow tick speed */
	private static final int SLOW_TICK_SPEED = 4;

	/** Lazily created empty cursor */
	private static Cursor emptyCursor = null;
	private static boolean emptyCursorCreated;

	/*
	 * Resource data
	 */

	/** The areas */
	private List<Area> areas;

	/** Is the mouse visible? */
	private boolean mouseVisible;

	/** Enable keyboard navigation (space, return, cursor keys & tab/shift tab) */
	private boolean keyboardNavigation;

	/** Background music */
	private String music;

	/** Alteratively, use a stream */
	private String stream;

	/** Hotkeys */
	private List<HotKey> hotkeys;

	/** Dialog */
	private boolean dialog;

	/** Offset location on screen - handy for transparent dialogs */
	private int offsetX, offsetY;

	/** Whether we're using unique sprites */
	private boolean uniqueSprites;

	/** Transition */
	private String transition;

	/** Centre the screen according to game scale */
	private String centre;

	/** Layer above which not to Y-sort */
	private int sortLayer;

	/** Allcaps areas */
	private boolean allCaps;

	/*
	 * Transient data
	 */

	/** Sprite engine */
	private transient StaticSpriteEngine spriteEngine;

	/** A timer */
	private transient int timer;

	/** Already ticking */
	private transient boolean alreadyTicking;

	/** Phase */
	private transient int phase;

	/** Alpha */
	private transient float alpha;

	/** Mouse coords */
	private transient int mouseX, mouseY, oldMouseX, oldMouseY;

	/** A map of area names to areas */
	private transient Map<String, Area> nameToArea;

	/** Enabled */
	private transient boolean enabled;

	/** Tickables */
	private transient List<Tickable> tickables;

	/** Current focused area */
	private transient Area focus;

	/** Grabbed area */
	private transient Area grabbed;

	/** Are we inited? */
	private transient boolean inited;

	/** Background music */
	private transient ALBuffer musicResource;

	/** Streamed music */
	private transient ALStream streamResource;

	/** Pause */
	private transient boolean paused;

	/** Monkeying mode */
	private transient boolean monkeying;

	/** Monkey mode: dragging flag */
	private transient Area dragging;

	/** Monkey mode: mouse was down flag */
	private transient boolean mouseWasDown;

	/** Monkey mode: was mouse grabbed */
	private transient boolean wasMouseGrabbed;

	/** Monkey mode: selection tick */
	private transient int selectTick;

	/** Monkey mode: drag handle */
	private transient int dragX, dragY;

	/** Monkey mode: slow mode ticker */
	private transient int slowTick;

	/** Override keyboard navigation */
	private transient boolean disableKeyboardNavigation;

	/** Transition handling */
	private transient Transition transitionFeature;

//	/** Constrain mouse */
//	private transient ReadableRectangle constrainMouse;

	/** Monkeying */
	private transient MonkeyRenderable monkeyRenderable;
	private class MonkeyRenderable extends TickableObject {
		@Override
		protected void render() {
			// Draw all area borders
			glRender(new GLRenderable() {
				@Override
				public void render() {
					glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
					glDisable(GL_TEXTURE_2D);
					glEnable(GL_BLEND);
				}
			});

			int n = areas.size();
			boolean foundHover = false;
			for (int i = n; -- i >= 0; ) {
				Area area = areas.get(i);
				boolean hover = false;
				Rectangle r = (Rectangle) area.getBounds();
				if (r == null) {
					ReadablePoint p = area.getPosition();
					SpriteImage si = area.getCurrentImage();
					if (si != null && p != null) {
						r = new Rectangle(p.getX(), p.getY(), si.getWidth(), si.getHeight());
					}
				}
				if (!foundHover && r != null) {
					if (r.contains(mouseX, mouseY)) {
						foundHover = true;
						hover = true;
					}
				}
				if (r != null) {
					renderMonkeyedArea(area.getID(), r, hover);
				}
			}
		}

		private void renderMonkeyedArea(String id, Rectangle r, boolean hover) {
			if (hover) {
				glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			} else {
				glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
			}
			boolean stippled = dragging != null && dragging.getID() != null && dragging.getID().equals(id);
			if (stippled) {
				selectTick = (selectTick + 1) % 64;
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glLineStipple(1, (short)(0xF0F0F >> (selectTick >> 3)));
						glEnable(GL_LINE_STIPPLE);
					}
				});
			}

			short idx = glVertex2f(r.getX(), r.getY());
			glVertex2f(r.getX() + r.getWidth(), r.getY());
			glVertex2f(r.getX() + r.getWidth(), r.getY() + r.getHeight());
			glVertex2f(r.getX(), r.getY() + r.getHeight());
			glRender(GL_LINE_LOOP, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 3)});

			if (stippled) {
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glEnable(GL_TEXTURE_2D);
						glEnable(GL_BLEND);
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
						glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
					}
				});
				GLString s = new GLString(id, Res.getTinyFont());
				glColor3f(0, 0, 0);
				s.setLocation(r.getX() + 1, r.getY() + r.getHeight() - 1);
				s.render(this);
				glColor3f(1, 1, 1);
				s.setLocation(r.getX(), r.getY() + r.getHeight());
				s.render(this);

				GLString s2 = new GLString(r.getX()+", "+r.getY(), Res.getTinyFont());
				glColor3f(0, 0, 0);
				s2.setLocation(r.getX() + 1, r.getY() - 1);
				s2.render(this);
				glColor3f(1, 1, 1);
				s2.setLocation(r.getX(), r.getY());
				s2.render(this);

				GLString s3 = new GLString(r.getWidth()+"x"+r.getHeight(), Res.getTinyFont());
				glColor3f(0, 0, 0);
				s3.setLocation(r.getX() + r.getWidth() - s3.getBounds(null).getWidth() + 1, r.getY() + r.getHeight() - s3.getBounds(null).getHeight() - 1);
				s3.render(this);
				glColor3f(1, 1, 1);
				s3.setLocation(r.getX() + r.getWidth() - s3.getBounds(null).getWidth(), r.getY() + r.getHeight() - s3.getBounds(null).getHeight());
				s3.render(this);
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glDisable(GL_TEXTURE_2D);
					}
				});
			}
			if (!stippled) {
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glLineStipple(1, (short) 0xFFFF);
						glDisable(GL_LINE_STIPPLE);
					}
				});
			}
		}
	}

	/*
	 * Phases
	 */
	private static final int CLOSED = 0;
	private static final int OPENING = 1;
	private static final int OPEN = 2;
	private static final int CLOSING = 3;
	private static final int BLOCKED = 4;

	/**
	 * Describes a Ctrl-key that will fire an onClicked to a screen.
	 */
	private class HotKey extends Feature {

		/** Key modifier */
		private String modifier;

		/** The key */
		private String key;

		/** The area ID */
		private String area;

		/** Command ID */
		private String command;

		/** Hold down */
		private boolean hold;

		private transient boolean wasDown;
		private transient boolean waitForRelease;

		/**
		 * C'tor
		 */
		public HotKey() {
			setAutoCreated();
			setSubResource(true);
		}

		@Override
		public void archive() {
			// Don't archive
		}

		void init() {
			int k = Keyboard.getKeyIndex(key);
			if (k != Keyboard.KEY_NONE) {
				if (Keyboard.isKeyDown(k)) {
					int k2 = Keyboard.getKeyIndex(modifier);
					if (k2 == Keyboard.KEY_NONE || Keyboard.isKeyDown(k2)) {
						// The key was down when the window opened; let's wait until it's up
						waitForRelease = true;
					}
				}
			}
		}

		void check() {
			int k = Keyboard.getKeyIndex(key);
			if (k != Keyboard.KEY_NONE) {
				if (Keyboard.isKeyDown(k)) {
					int k2 = Keyboard.getKeyIndex(modifier);
					if (k2 != Keyboard.KEY_NONE && !Keyboard.isKeyDown(k2)) {
						wasDown = false;
						waitForRelease = false;
						return;
					}
					if (waitForRelease) {
						return;
					}
					if (command != null) {
						if (wasDown) {
							return;
						} else {
							wasDown = !hold;
							onClicked(command);
						}
					} else {
						Area a = getArea(area);
						if (a != null && a.isEnabled()) {
							if (wasDown) {
								return;
							} else {
								wasDown = !hold;
								onClicked(area);
							}
						}
					}
				} else {
					wasDown = false;
					waitForRelease = false;
				}
			}
		}

	}


	/**
	 * C'tor
	 */
	public Screen(String name) {
		super(name);
	}

	/**
	 * @param transition the transition to set
	 */
	public void setTransition(Transition transition) {
		if (transition == null) {
			this.transitionFeature = new InstantTransition();
		} else {
			this.transitionFeature = transition;
		}
	}

	/**
	 * @return the transitionFeature
	 */
	public Transition getTransition() {
		return transitionFeature;
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> children = XMLUtil.getChildren(element, "area");
		areas = new ArrayList<Area>(children.size());
		for (Element child : children) {
			Area area = (Area) loader.load(child);// new Area();
			if (allCaps) {
				area.setAllCaps(true);
			}
			areas.add(area);
		}

		List<Element> hotkeyElements = XMLUtil.getChildren(element, "hotkey");
		hotkeys = new ArrayList<HotKey>(hotkeyElements.size());
		for (Element hotkeyElement : hotkeyElements) {
			HotKey hk = new HotKey();
			hk.load(hotkeyElement, loader);
			hotkeys.add(hk);
		}


	}

	@Override
	protected final void doCreate() {
		super.doCreate();

		// Create somewhere to stash all our tickables in
		if (!inited) {
			tickables = new ArrayList<Tickable>(256);
			// The first tickable is a sprite engine
			spriteEngine = new StaticSpriteEngine(true, sortLayer, uniqueSprites, 1);
			spriteEngine.setName("SpriteEngine["+getName()+"]");
			spriteEngine.setLocked(true); // Prevent Feature.defaultDestroy from killing us
			spriteEngine.create();

			if (transitionFeature == null) {
				transitionFeature = new ZoomTransition();
			}
			inited = true;
		}
		nameToArea = new HashMap<String, Area>();
		for (Area area : areas) {
			area.create();
			area.spawn(this);
			if (area.getID() != null) {
				nameToArea.put(area.getID().toLowerCase(), area);
			}
		}
		for (HotKey hk : hotkeys) {
			hk.create();
		}

		doCreateScreen();
	}

	@Override
	protected final void doDestroy() {
		super.doDestroy();

		// Remove the areas from the tickables list as well
		for (Area area : areas) {
			area.destroy();
		}
		for (HotKey hk : hotkeys) {
			hk.destroy();
		}
		nameToArea = null;

		doDestroyScreen();
	}

	protected void doCreateScreen() {
	}

	protected void doDestroyScreen() {
	}

	/**
	 * An Area is requesting focus
	 */
	public void requestFocus(Area area) {
		if (area == null) {
			setFocus(null);
		} else if (area.isFocusable()) {
			setFocus(area.getID());
		}
	}

	/**
	 * Check keyboard navigation keys
	 */
	private void checkKeyboardNavigation() {
		String changeFocus = null;
		int count = 0;
		while (count < areas.size()) {
			if (Binding.isBindingDown(Binding.FOCUS_LEFT) || Binding.isBindingDown(Binding.FOCUS_LEFT_ALT)) {
				if (focus == null) {
					findFirstFocus();
				} else {
					changeFocus = focus.getLeftFocus();
				}
			} else if (Binding.isBindingDown(Binding.FOCUS_RIGHT) || Binding.isBindingDown(Binding.FOCUS_RIGHT_ALT)) {
				if (focus == null) {
					findFirstFocus();
				} else {
					changeFocus = focus.getRightFocus();
				}
			} else if (Binding.isBindingDown(Binding.FOCUS_UP) || Binding.isBindingDown(Binding.FOCUS_UP_ALT)) {
				if (focus == null) {
					findFirstFocus();
				} else {
					changeFocus = focus.getUpFocus();
				}
			} else if (Binding.isBindingDown(Binding.FOCUS_DOWN) || Binding.isBindingDown(Binding.FOCUS_DOWN_ALT)) {
				if (focus == null) {
					findFirstFocus();
				} else {
					changeFocus = focus.getDownFocus();
				}
			} else if (Game.wasKeyPressed(Keyboard.KEY_TAB)) {
				if (focus == null) {
					findFirstFocus();
				} else {
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
						changeFocus = focus.getPrevFocus();
					} else {
						changeFocus = focus.getNextFocus();
					}
				}
			} else {
				return;
			}
			if (changeFocus == null) {
				return;
			}
			setFocus(changeFocus);
			if (focus != null && focus.isFocused()) {
				return;
			}
		}
	}

	/**
	 * Find first focusable Area
	 */
	private void findFirstFocus() {
		// Search for default focus first
		for (Area area : areas) {
			if (area.isFocusable() && area.isDefaultFocus()) {
				focus = area;
				return;
			}
		}
		// Then just pick the first one
		for (Area area : areas) {
			if (area.isFocusable()) {
				focus = area;
				return;
			}
		}
		focus = null;
	}

	/**
	 * Set an area to be the focused area
	 * @param areaName
	 */
	private void setFocus(String areaName) {
		if (areaName == null) {
			focus = null;
		} else {
			focus = getArea(areaName);
		}
	}

	/**
	 * Tick the screen.
	 */
	@SuppressWarnings("unused")
    public final void tick() {
		if (!isCreated()) {
			throw new RuntimeException("Screen "+this+"["+System.identityHashCode(this)+"]"+" is not created but is being ticked!");
		}

		if (alreadyTicking) {
			if (!EXTRA_TICKING.contains(this)) {
				EXTRA_TICKING.add(this);
			}
			return;
		}
		alreadyTicking = true;

		oldMouseX = mouseX;
		oldMouseY = mouseY;
		mouseX = (int) Game.physicalXtoLogicalX(Game.getMouseX());
		mouseY = (int) Game.physicalYtoLogicalY(Game.getMouseY());
//		if (!isMouseVisible()) {
//			boolean setMousePosition = false;
//			int minX = constrainMouse == null ? 0 : constrainMouse.getX();
//			int minY = constrainMouse == null ? 0 : constrainMouse.getY();
//			int maxW = constrainMouse == null ? getWidth() : constrainMouse.getWidth();
//			int maxH = constrainMouse == null ? getHeight() : constrainMouse.getHeight();
//			if (mouseX < minX) {
//				mouseX = minX;
//				setMousePosition = true;
//			} else if (mouseX >= maxW) {
//				mouseX = maxW - 1;
//				setMousePosition = true;
//			}
//			if (mouseY < minY) {
//				mouseY = minY;
//				setMousePosition = true;
//			} else if (mouseY >= maxH) {
//				mouseY = maxH - 1;
//				setMousePosition = true;
//			}
//			if (setMousePosition) {
//				Mouse.setCursorPosition((int) Game.logicalXtoPhysicalX(mouseX), (int) Game.logicalYtoPhysicalY(mouseY));
//			}
//		}
		switch (phase) {
			case OPENING:
				if (++timer >= transitionFeature.getOpeningDuration()) {
					setPhase(OPEN);
					timer = 0;
				} else {
					doTick();
					tickEverything();
					//updateEverything();
					postTick();
					break;
				}
			case OPEN:
				if (!paused) {
					if (enabled && !monkeying) {
						if (keyboardNavigation) {
							checkKeyboardNavigation();
						}
						for (int i = 0; i < areas.size(); i ++) {
							Area area = areas.get(i);
							area.tick();
						}
						// Check hotkeys
						if (hotkeys != null) {
							int numKeys = hotkeys.size();
							for (int i = 0; i < numKeys; i ++) {
								HotKey hotKey = hotkeys.get(i);
								hotKey.check();
							}
						}
					}
					// Check for monkeying mode
					if (Game.DEBUG && Game.wasKeyPressed(Keyboard.KEY_F11)) { // warning suppressed
						if (monkeying) {
							monkeying = false;
							System.out.println("No longer Monkeying");
							if (monkeyRenderable != null) {
								monkeyRenderable.remove();
								monkeyRenderable = null;
							}
							Mouse.setGrabbed(wasMouseGrabbed);
							SFX.gameOver();
						} else {
							monkeying = true;
							System.out.println("Monkeying");
							monkeyRenderable = new MonkeyRenderable();
							monkeyRenderable.setLayer(100);
							monkeyRenderable.spawn(this);
							wasMouseGrabbed = Mouse.isGrabbed();
							Mouse.setGrabbed(false);
							SFX.textEntered();
						}
					}

					if (monkeying) {
						monkey();
					} else {
						// Custom ticking code now
						doTick();
						tickEverything();
						//updateEverything();
						postTick();
					}

//					// Clear unused keyboard events
//					while (Keyboard.next()) {
//						// Do nothing
//					}
//					// Clear unused mouse events
//					while (Mouse.next()) {
//						// Do nothing
//					}
//					// Clear unused controller events
//					while (Controllers.next()) {
//						// Do nothing
//					}
				}
				break;
			case CLOSING:
				if (++timer >= transitionFeature.getClosingDuration()) {
					setPhase(CLOSED);
					removeAllTickables();
					doCleanup();
				} else {
					doTick();
					tickEverything();
					postTick();
				}
				break;
			case CLOSED:
				break;
			case BLOCKED:
				// Screen is blocked by a dialog. Still allow it to tick, unless it's paused
				if (!paused) {
					doTick();
					tickEverything();
					postTick();
				}
				break;
			default:
				assert false;
		}

		alreadyTicking = false;
	}

	public final void update() {
		updateEverything();
		doUpdate();
	}

	protected void doUpdate() {
	}

	/**
	 * Tick all the screen's tickables
	 */
	private void tickEverything() {
		for (int i = 0; i < tickables.size(); ) {
			Tickable tickable = tickables.get(i);
			if (tickable.isActive()) {
				tickable.tick();
				i ++;
			} else {
				tickables.remove(i);
				tickable.remove();
			}
		}

		// Then finally the sprite engine
		spriteEngine.tick();
	}

	/**
	 * Update everything
	 */
	protected final void updateEverything() {
		for (int i = 0; i < tickables.size(); ) {
			Tickable tickable = tickables.get(i);
			if (tickable.isActive()) {
				tickable.update();
				i ++;
			} else {
				tickables.remove(i);
				tickable.remove();
			}
		}
	}

	/**
	 * Has the mouse moved this tick?
	 * @return boolean
	 */
	public boolean mouseMoved() {
		return mouseX != oldMouseX || mouseY != oldMouseY;
	}

	/**
	 * Do ticking
	 */
	protected void doTick() {
	}

	/**
	 * Post-tick, called after everything has been ticked
	 */
	protected void postTick() {
	}

	/**
	 * Add a tickable
	 */
	public final void addTickable(Tickable tickable) {
		assert !tickables.contains(tickable) : "Tickable "+tickable+" already added to "+this;
		tickables.add(tickable);
	}

	/**
	 * Detach a specific tickable. It is removed from the tickable list
	 * but not "removed" as such
	 * @param tickable
	 */
	public final void detachTickable(Tickable tickable) {
		tickables.remove(tickable);
	}

	/**
	 * Clear all tickables
	 */
	public final void removeAllTickables() {
		// Properly destroy everything
		int n = tickables.size();
		for (int i = 0; i < n; ) {
			Tickable tickable = tickables.get(i);
			tickable.remove();
			if (tickable.isActive()) {
				i ++;
			} else {
				n --;
				tickables.set(i, tickables.get(n));
				tickables.remove(n);
			}
		}
	}

	/**
	 * Remove all sprites
	 */
	protected void removeAllSprites() {
		spriteEngine.clear();
	}

	/**
	 * Called when an area is clicked on
	 * @param id The id of the area clicked on
	 */
	protected void onClicked(String id) {
	}

	/**
	 * Called when an area is hovered on or off
	 * @param id The id of the area
	 * @param on Whether the area is hovered over or not
	 */
	protected void onHover(String id, boolean on) {
	}

	/**
	 * Is the screen active?
	 * @return boolean
	 */
	private boolean isActive() {
		return phase != CLOSED;
	}

	private void setPhase(int newPhase) {
		if (phase == newPhase) {
			return;
		}
		int oldPhase = phase;
		phase = newPhase;
		if (newPhase != BLOCKED && oldPhase == BLOCKED) {
			clearKeyboardEvents();
			initHotkeys();
			for (Area area : areas) {
				area.waitForMouse();
			}
			onUnblocked();
		} else if (newPhase == BLOCKED && oldPhase != BLOCKED) {
			onBlocked();
		} else if (newPhase == OPENING) {
			clearKeyboardEvents();
		}
	}
	protected void onUnblocked() {}
	protected void onBlocked() {}

	private static void clearKeyboardEvents() {
		while (Keyboard.next()) {
			// Do nothing
		}
	}

	/**
	 * Force this screen into "open" state instantly.
	 */
	public void forceOpen() {
		phase = OPEN;
	}

	/**
	 * Remove the screen
	 * @param instantly Whether to close the screen instantly
	 */
	public final void close(boolean instantly) {
		if (isClosing() || isClosed()) {
			return;
		}
		setPhase(CLOSING);
		timer = 0;
		focus = null;
		onClose();

		if (instantly) {
			removeAllTickables();
			doCleanup();
			setPhase(CLOSED);
		} else {
			setPhase(CLOSING);
			tick();
		}
	}

	/**
	 * Close the screen, with an animation effect
	 */
	public final void close() {
		close(false);
	}


	/**
	 * Do things that need doing to clean up the screen
	 */
	protected void onClose() {
		// By default do nothing
	}

	/**
	 * Open the screen. This initializes the screen and adds it to the top of the screen stack.
	 * The current top of the screen stack is told to close, unless this is a dialog screen, in
	 * which case it is BLOCKED instead.
	 */
	public final void open() {
		if (!isCreated()) {
			throw new RuntimeException("Screen "+this+" is not created but is being opened!");
		}
		if (isOpen() || isOpening()) {
			return;
		}
		for (int i = 0; i < SCREENS.size(); i ++) {
			Screen screen = SCREENS.get(i);
			if (screen == this) {
				screen.removeAllTickables();
				screen.doCleanup();
			} else if (dialog && screen.phase != CLOSED && screen.phase != CLOSING) {
				screen.setPhase(BLOCKED);
			} else {
				screen.close();
			}
		}
		if (SCREENS.contains(this)) {
			// Bring screen to the top
			SCREENS.remove(this);
		}
		SCREENS.add(this);
		setPhase(OPENING);
		enabled = true;
		timer = 0;
		focus = null;
		if (musicResource != null) {
			Game.playMusic(musicResource, Math.max(MINIMUM_MUSIC_FADE_DURATION, transitionFeature.getOpeningDuration()));
		}
		if (streamResource != null) {
			Game.playMusic(streamResource, Math.max(MINIMUM_MUSIC_FADE_DURATION, transitionFeature.getOpeningDuration()));
		}
		for (Area area : areas) {
			area.init();
		}
		initHotkeys();
		resized();
		onOpen();
		tick();
	}

	private void initHotkeys() {
		for (HotKey hk : hotkeys) {
			hk.init();
		}
	}

	private void resized() {
		for (Area area : areas) {
			area.onResized();
		}
		onResized();
	}
	protected void onResized() {}

	/**
	 * Initialize the screen
	 */
	protected void onOpen() {
		// Do nothing by default
	}

	/**
	 * Called when a screen becomes the topmost screen again after a blocking dialog
	 * is removed from on top of it
	 */
	protected void onReopen() {
	}

	/**
	 * Cleanup the screen when it's no longer visible
	 */
	protected void doCleanup() {
	}

	/**
	 * @param alpha the alpha to set
	 */
	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	/**
	 * Render the screen
	 */
	public final void render() {

		glPushMatrix();
		glTranslatef(offsetX, offsetY, 0.0f);

		int startPhase = phase;
		if (startPhase == OPENING) {
			transitionFeature.preRenderOpening(this, timer);
		} else if (startPhase == CLOSING) {
			transitionFeature.preRenderClosing(this, timer);
		} else {
			setAlpha(1.0f);
		}

		// Do stuff before any rendering occurs
		preRender();

		// Reset the colour
		glColor4f(1.0f, 1.0f, 1.0f, alpha);

		// Render background (for example, clear the screen)
		renderBackground();

		// Render sprites
		renderSprites(spriteEngine, alpha);

//		// Render areas
//		for (Area area : areas) {
//			area.render();
//		}

		// Render foreground, eg. text etc.
		renderForeground();

		// Do any post processing
		postRender();

		if (startPhase == OPENING) {
			transitionFeature.postRenderOpening(this, timer);
		} else if (startPhase == CLOSING) {
			transitionFeature.postRenderClosing(this, timer);
		}

		glPopMatrix();
	}

	/**
	 * Render sprites
	 * @param engine The sprite engine to render
	 * @param alpha Alpha value, 0.0f ... 1.0f
	 */
	protected void renderSprites(SpriteEngine engine, float alpha) {
		spriteEngine.setAlpha(alpha);
		spriteEngine.render();
	}

	/**
	 * Render stuff before the sprites
	 */
	protected void renderBackground() {
	}

	/**
	 * Render stuff after the sprites
	 */
	protected void renderForeground() {
	}

	/**
	 * Pre-render. Called before any rendering at all.
	 */
	protected void preRender() {
	}

	/**
	 * Post-render. Called after everything else is rendered.
	 */
	protected void postRender() {
	}

	@Override
	public Sprite allocateSprite(Serializable owner) {
		Sprite ret = spriteEngine.allocateSprite(owner);
		ret.setAllocator(this);
		return ret;
	}

	private static boolean mouseCurrentlyVisible = true;

	/**
	 * Tick all screens
	 */
	public static void tickAllScreens() {
		// Constrain mouse to window
		if (!mouseCurrentlyVisible) {
			int x = Mouse.getX();
			int y = Mouse.getY();
			boolean set = false;
			if (x < 0) {
				x = 0;
				set = true;
			}
			if (y < 0) {
				y = 0;
				set = true;
			}
			if (set) {
				Mouse.setCursorPosition(x, y);
			}
		}

		// Unblock the topmost screen
		Screen top = getTopScreen();
		if (top != null && top.isBlocked()) {
			top.setPhase(OPEN);
			top.onReopen();
		}
		for (int i = 0; i < SCREENS.size(); ) {
			Screen screen = SCREENS.get(i);
			screen.tick();
			if (screen.isActive() && screen.isCreated()) {
				i ++;
			} else {
				SCREENS.remove(i);
			}
		}
		for (int i = 0; i < EXTRA_TICKING.size(); i ++) {
			EXTRA_TICKING.get(i).tick();
		}
		EXTRA_TICKING.clear();

		try {
			if (!isMouseVisible() && mouseCurrentlyVisible) {
				mouseCurrentlyVisible = false;
				if (!emptyCursorCreated) {
					emptyCursorCreated = true;
					try {
				        emptyCursor = new Cursor(1, 1, 0, 0, 1, BufferUtils.createIntBuffer(1), null);
			        } catch (LWJGLException e) {
				        e.printStackTrace(System.err);
			        }
				}
				Mouse.setNativeCursor(emptyCursor);
				Mouse.setGrabbed(true);
			} else if (isMouseVisible() && !mouseCurrentlyVisible) {
				mouseCurrentlyVisible = true;
				Mouse.setNativeCursor(null);
				Mouse.setGrabbed(false);
			}
		} catch (LWJGLException e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Update all screens
	 */
	public static void updateAllScreens() {
		for (int i = 0; i < SCREENS.size(); i ++) {
			Screen screen = SCREENS.get(i);
			screen.update();
		}
	}

	/**
	 * Render all screens
	 */
	public static void renderAllScreens() {
		// Render all the screens from bottom to top
		for (int i = 0; i < SCREENS.size(); i ++) {
			Screen screen = SCREENS.get(i);
			screen.render();
		}
	}

	/**
	 * Is the mouse visible?
	 * @return boolean
	 */
	public static final boolean isMouseVisible() {
		if (Game.isPaused()) {
			return true;
		}
		if (SCREENS.size() > 0) {
			Screen topmost = SCREENS.get(SCREENS.size() - 1);
			return topmost.mouseVisible;
		}
		// Show the mouse if there's no screens
		return true;
	}

	/**
	 * @return Returns the alpha.
	 */
	protected final float getAlpha() {
		return alpha;
	}

	/**
	 * @return Returns the mouseX.
	 */
	public int getMouseX() {
		return mouseX;
	}

	/**
	 * @return Returns the mouseY.
	 */
	public int getMouseY() {
		return mouseY;
	}

	/**
	 * Get a named area
	 * @param name
	 * @return an Area, or null, if the area doesn't exist
	 */
	public Area getArea(String name) {
		return nameToArea.get(name.toLowerCase());
	}

	/**
	 * @return true if the window is opening
	 */
	public final boolean isOpening() {
		return phase == OPENING;
	}

	/**
	 * @return true if the window is closing
	 */
	public final boolean isClosing() {
		return phase == CLOSING;
	}

	/**
	 * @return true if the window is closed
	 */
	public final boolean isClosed() {
		return phase == CLOSED;
	}

	/**
	 * @return true if the window is open
	 */
	public final boolean isOpen() {
		return phase == OPEN;
	}

	/**
	 * @return true if the screen is blocked by a dialog
	 */
	public final boolean isBlocked() {
		return phase == BLOCKED;
	}

	/**
	 * Disable all the areas
	 */
	public final void setEnabled(boolean enabled) {
		this.enabled = enabled;
		for (Area area : areas) {
			area.setEnabled(enabled);
		}
	}

	/**
	 * Enable/disable keyboard navigation
	 * @param keyboardNavigation
	 */
	public final void setKeyboardNavigationEnabled(boolean keyboardNavigation) {
		this.disableKeyboardNavigation = !keyboardNavigation;
	}

	/**
	 * Is keyboard navigation enabled?
	 * @return boolean
	 */
	public final boolean isKeyboardNavigationEnabled() {
		return keyboardNavigation && !disableKeyboardNavigation;
	}

	/**
	 * Focus next control
	 */
	public final void nextFocus() {
	}

	/**
	 * Focus previous control
	 */
	public final void prevFocus() {
	}

	/**
	 * Get the currently focused area
	 * @return Area, or null
	 */
	public Area getFocus() {
		return focus;
	}

	/**
	 * Enable / disable a button
	 * @param enabled
	 */
	public void setEnabled(String area, boolean enabled) {
		Area a = getArea(area);
		if (a == null) {
			return;
		}
		a.setEnabled(enabled);
	}

	/**
	 * Show / hide a button
	 * @param visible
	 */
	public void setVisible(String area, boolean visible) {
		Area a = getArea(area);
		if (a == null) {
			return;
		}
		a.setVisible(visible);
	}

	/**
	 * Enable / disable a group of buttons
	 * @param group
	 * @param enabled
	 */
	public void setGroupEnabled(String group, boolean enabled) {
		int n = areas.size();
		for (int i = 0; i < n; i ++) {
			Area area = areas.get(i);
			if (area.isInGroup(group)) {
				area.setEnabled(enabled);
			}
		}
	}

	/**
	 * Show / hide a group of buttons
	 * @param group
	 * @param visible
	 */
	public void setGroupVisible(String group, boolean visible) {
		int n = areas.size();
		for (int i = 0; i < n; i ++) {
			Area area = areas.get(i);
			if (area.isInGroup(group)) {
				area.setVisible(visible);
			}
		}
	}

	/**
	 * Set alpha for a group of buttons
	 * @param group
	 * @param visible
	 */
	public void setGroupAlpha(String group, int alpha) {
		int n = areas.size();
		for (int i = 0; i < n; i ++) {
			Area area = areas.get(i);
			if (area.isInGroup(group)) {
				area.setAlpha(alpha);
			}
		}
	}

	/**
	 * Get the topmost screen
	 * @return a Screen or null if no screens are open
	 */
	public static Screen getTopScreen() {
		if (SCREENS.size() == 0) {
			return null;
		}
		return SCREENS.get(SCREENS.size() - 1);
	}

	public static void onGameResized() {
		for (int i = 0; i < SCREENS.size(); i ++) {
			Screen s = SCREENS.get(i);
			s.resized();
		}
	}

	/**
	 * Get the visible Areas under a coordinate
	 * @param x
	 * @param y
	 * @returns a List of Areas under x, y
	 */
	public List<Area> getAreasUnder(int x, int y) {
		List<Area> ret = new LinkedList<Area>();
		int n = areas.size();
		for (int i = 0; i < n; i ++) {
			Area area = areas.get(i);
			if (area.isVisible() && ((Rectangle) area.getBounds()).contains(x, y)) {
				ret.add(area);
			}
		}
		return ret;
	}

	/**
	 * Get all areas
	 * @return an unmodifiable List of Areas in the screen
	 */
	public List<Area> getAreas() {
		return Collections.unmodifiableList(areas);
	}

	/**
	 * Get all areas in a group
	 * @param group The group name
	 * @return an unmodifiable List of Areas
	 */
	public List<Area> getAreas(String group) {
		List<Area> ret = new ArrayList<Area>(areas.size());
		for (Area area : areas) {
			if (area.isInGroup(group)) {
				ret.add(area);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	/**
	 * Sets (or clears) the grabbed area. When an Area is grabbed,
	 * no other areas are processed by mouse or keyboard.
	 * @param grabbed the grabbed to set
	 */
	public void setGrabbed(Area grabbed) {
		this.grabbed = grabbed;
	}

	/**
	 * @return the grabbed area
	 */
	public Area getGrabbed() {
		return grabbed;
	}

	/**
	 * Sets the translation offset for the screen's rendering. Use this to
	 * make transparent dialogs appear in different locations.
	 * @param offsetX
	 * @param offsetY
	 */
	public void setOffset(int offsetX, int offsetY) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	/**
	 * Allow developer to drag Areas around
	 */
	private void monkey() {
		if (Mouse.isButtonDown(0)) {
			if (mouseWasDown && dragging != null) {
				// Drag
				ReadableRectangle r = dragging.getBounds();
				dragging.setBounds(mouseX - dragX, mouseY - dragY, r.getWidth(), r.getHeight());
			} else {
				// Find out what to drag
				int n = areas.size();
				dragging = null;
				for (int i = n; -- i >= 0; ) {
					Area area = areas.get(i);
					Rectangle r = (Rectangle) area.getBounds();
					if (r.contains(mouseX, mouseY)) {
						dragging = area;
						dragX = mouseX - r.getX();
						dragY = mouseY - r.getY();
						break;
					}
				}
			}

			mouseWasDown = true;
		} else {
			mouseWasDown = false;
		}

		if (dragging != null) {
			if (slowTick > 0) {
				slowTick --;
			} else {
				Rectangle r = (Rectangle) dragging.getBounds();
				ReadablePoint of = dragging.getOffset();
				SpriteImage si = dragging.getCurrentImage();
				ReadablePoint to = dragging.getTextOffset();
				int tx = to.getX();
				int ty = to.getY();
				if (Keyboard.isKeyDown(Keyboard.KEY_R) && si != null) {
					// Size to sprite
					r.setSize(si.getWidth(), si.getHeight());
				}
				boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {
					int ox, oy;
					if (of == null) {
						ox = 0;
						oy = 0;
					} else {
						ox = of.getX();
						oy = of.getY();
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
						oy ++;
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
						oy --;
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
						ox --;
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
						ox ++;
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					dragging.setOffset(ox, oy);
				} else if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
					if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
						r.setSize(r.getWidth(), r.getHeight() + 1);
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
						r.setSize(r.getWidth(), r.getHeight() - 1);
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
						r.setSize(r.getWidth() - 1, r.getHeight());
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
						r.setSize(r.getWidth() + 1, r.getHeight());
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
				} else if (Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {
					if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
						ty ++;
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
						ty --;
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
						tx --;
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
						tx ++;
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
				} else {
					if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
						r.setLocation(r.getX(), r.getY() + 1);
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
						r.setLocation(r.getX(), r.getY() - 1);
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
						r.setLocation(r.getX() - 1, r.getY());
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
					if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
						r.setLocation(r.getX() + 1, r.getY());
						slowTick = ctrlDown ? SLOW_TICK_SPEED : 0;
					}
				}
				dragging.setBounds(r.getX(), r.getY(), r.getWidth(), r.getHeight());
				dragging.setTextOffset(tx, ty);
			}
		}

		if (Game.wasKeyPressed(Keyboard.KEY_X)) {
			// Write out area XML
			try {
				PrintWriter pw = new PrintWriter(System.out);
				XMLResourceWriter writer = new XMLResourceWriter(pw);
				for (Area area : areas) {
					area.toXML(writer);
				}
				pw.flush();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * @return true if the screen is enabled
	 */
	public final boolean isEnabled() {
		return enabled;
	}

	/**
	 * Pause or unpause the screen. This causes tickables to stop ticking.
	 * Everything is still rendered though.
	 * @param paused
	 */
	public void setPaused(boolean paused) {
		this.paused = paused;
		onSetPaused();
	}
	protected void onSetPaused() {}

	/**
	 * @return true if the screen is paused
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * @return the offsetX
	 */
	public int getOffsetX() {
		return offsetX;
	}

	/**
	 * @return the offsetY
	 */
	public int getOffsetY() {
		return offsetY;
	}

	/**
	 * @return the timer used for screen transitions
	 */
	public int getTransitionTick() {
		return timer;
	}

	public boolean isCentredX() {
		return "x".equals(centre);
	}
	public boolean isCentredY() {
		return "y".equals(centre);
	}
	public boolean isCentred() {
		return "both".equals(centre);
	}

	public int getWidth() {
		return Game.getWidth();
	}

	public int getHeight() {
		return Game.getHeight();
	}

//	/**
//	 * Constrain the mouse
//	 * @param constrainMouse Mouse constraints (logical coordinates); or null to clear
//	 */
//	public void setConstrainMouse(ReadableRectangle constrainMouse) {
//	    this.constrainMouse = constrainMouse;
//    }
}
