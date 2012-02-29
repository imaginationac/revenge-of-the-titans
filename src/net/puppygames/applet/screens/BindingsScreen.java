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
package net.puppygames.applet.screens;

import java.util.LinkedList;
import java.util.List;

import net.puppygames.applet.*;
import net.puppygames.applet.effects.SFX;

import org.lwjgl.input.*;
import org.lwjgl.util.Point;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.sprites.ImageBank;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * $Id: BindingsScreen.java,v 1.2 2010/05/20 21:17:33 foo Exp $
 * Redefine keys
 * @author $Author: foo $
 * @version $Revision: 1.2 $
 */
public class BindingsScreen extends Screen {

	private static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	/** Singleton */
	private static BindingsScreen instance;

	/** Phases */
	private static final int PHASE_NORMAL = 0;
	private static final int PHASE_WAIT = 1;

	/*
	 * Areas
	 */
	private static final String OK = "ok";
	private static final String CANCEL = "cancel";
	private static final String INSTRUCTIONS = "instructions";
	private static final String KEY_PREFIX = "bindingarea.";
	private static final String BINDINGS_GROUP = "bindingarea";
	private static final String RESET = "reset";

	/*
	 * Resource data
	 */

	/** An imagebank of keys */
	private String keys;

	/** An imagebank of mousebuttons */
	private String mouse;

	/** An imagebank of controller buttons */
	private String controller;

	/** An imagebank of dpad buttons */
	private String dpad;

	/** Character offset */
	private Point offset = new Point(0, 0);

	/** Key areas */
	private List<KeyArea> keyAreas;

	/** Allow mouse bindings */
	private boolean allowMouse = true;

	/** Allow keyboard bindings */
	private boolean allowKeyboard = true;

	/** Allow controller bindings */
	private boolean allowController = true;

	/**
	 * Keys
	 */
	private static class KeyArea extends Feature {

		private static final long serialVersionUID = 1L;

		/** Binding we're linked to */
		private String binding;

		/** Location */
		private Point location;

		/** Text location */
		private Point textLocation;

		/** Glyph sprite */
		private transient Sprite glyphSprite;

		/** Screen */
		private transient BindingsScreen screen;

		/**
		 * C'tor
		 */
		public KeyArea() {
			setAutoCreated();
			setSubResource(true);
		}

		void init(BindingsScreen screen) {
			this.screen = screen;
			glyphSprite = screen.allocateSprite(this);
			glyphSprite.setLocation(location.getX(), location.getY());
			glyphSprite.setOffset(screen.offset.getX(), screen.offset.getY());
			glyphSprite.setLayer(1);
			update();
		}

		void cleanup() {
			if (glyphSprite != null) {
				glyphSprite.deallocate();
				glyphSprite = null;
			}
		}

		void update() {
			Binding b;
			try {
				b = (Binding) Resources.get(binding);
				if (b.getType() == null) {
					System.out.println("No binding for "+binding);
					glyphSprite.setVisible(false);
				} else {
					glyphSprite.setVisible(true);
					if (b.getType().equals(Binding.KEYBOARD)) {
						glyphSprite.setAppearance(screen.keysImageBank.getImage(b.getIndex()));
					} else if (b.getType().equals(Binding.KEYBOARD)) {
						glyphSprite.setAppearance(screen.mouseImageBank.getImage(b.getIndex()));
					} else if (b.getType().equals(Binding.CONTROLLER)) {
						glyphSprite.setAppearance(screen.controllerImageBank.getImage(b.getIndex()));
					} else if (b.getType().equals(Binding.DPAD)) {
						glyphSprite.setAppearance(screen.dpadImageBank.getImage(b.getIndex()));
					}
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

	/*
	 * Transient data
	 */

	private transient ImageBank keysImageBank, mouseImageBank, controllerImageBank, dpadImageBank;

	/** Current phase */
	private transient int phase;

	/** Which binding we're setting */
	private transient String binding;

	/**
	 * @param name
	 */
	public BindingsScreen(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doRegister()
	 */
	@Override
	protected void doRegister() {
		instance = this;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doDeregister()
	 */
	@Override
	protected void doDeregister() {
		instance = null;
	}

	/**
	 * Show the redefine keys screnn if there is one
	 */
	public static void show() {
		if (instance != null) {
			if (!instance.isCreated()) {
				try {
					instance.create();
				} catch (Exception e) {
					e.printStackTrace(System.err);
					instance = null;
					return;
				}
			}
			instance.open();
		}
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#onClicked(java.lang.String)
	 */
	@Override
	protected void onClicked(String id) {
		GenericButtonHandler.onClicked(id);
		if (OK.equals(id)) {
			// Save the bindings
			Game.saveBindings();
			TitleScreen.show();
		} else if (CANCEL.equals(id)) {
			if (phase == PHASE_NORMAL) {
				Game.loadBindings();
				TitleScreen.show();
			} else {
				doneBinding();
			}
		} else if (RESET.equals(id)) {
			Binding.resetToDefaults();
			updateBindingsDisplay();
		} else if (id.startsWith(KEY_PREFIX)) {
			waitForInput(id.substring(KEY_PREFIX.length()));
		}
	}

	/**
	 * Wait for a keypress or mouse button to assign to a particular binding.
	 */
	public void waitForInput(String binding) {
		phase = PHASE_WAIT;
		setEnabled(OK, false);
		setEnabled(RESET, false);
		setVisible(INSTRUCTIONS, true);
		this.binding = binding;
		setKeyboardNavigationEnabled(false);
		setGroupEnabled(BINDINGS_GROUP, false);
		Game.setPauseEnabled(false);
		SFX.keyTyped();
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#doTick()
	 */
	@Override
	protected void doTick() {
		if (phase == PHASE_NORMAL) {
			return;
		}

		// Check for a keypress
		if (Game.wasKeyPressed(Keyboard.KEY_ESCAPE)) {
			doneBinding();
			return;
		}

		if (allowKeyboard) {
			for (int i = 0; i < Keyboard.KEYBOARD_SIZE; i ++) {
				if (Keyboard.isKeyDown(i) && i != Keyboard.KEY_ESCAPE && i != Keyboard.KEY_P) {
					Binding.setBinding(binding, Binding.KEYBOARD, i);
					doneBinding();
				}
			}
		}

		// Check for a mouse button
		if (allowMouse) {
			for (int i = 0; i < Mouse.getButtonCount(); i ++) {
				if (Mouse.isButtonDown(i)) {
					Binding.setBinding(binding, Binding.MOUSE, i);
					doneBinding();
				}
			}
		}

		// Check for controllers
		if (allowController) {
			if (Controllers.getControllerCount() > 0) {
				Controller c = Controllers.getController(0);
				for (int i = 0; i < c.getButtonCount(); i ++) {
					if (c.isButtonPressed(i)) {
						Binding.setBinding(binding, Binding.CONTROLLER, i);
						doneBinding();
					}
				}
				if (c.getPovX() < -0.5f) {
					Binding.setBinding(binding, Binding.DPAD, Binding.DPAD_LEFT_INDEX);
					doneBinding();
				}
				if (c.getPovX() > 0.5f) {
					Binding.setBinding(binding, Binding.DPAD, Binding.DPAD_RIGHT_INDEX);
					doneBinding();
				}
				if (c.getPovY() < -0.5f) {
					Binding.setBinding(binding, Binding.DPAD, Binding.DPAD_UP_INDEX);
					doneBinding();
				}
				if (c.getPovY() > 0.5f) {
					Binding.setBinding(binding, Binding.DPAD, Binding.DPAD_DOWN_INDEX);
					doneBinding();
				}

			}
		}
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#doCleanup()
	 */
	@Override
	protected void doCleanup() {
		// Cleanup the key areas
		for (KeyArea keyArea : keyAreas) {
			keyArea.cleanup();
		}
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#onOpen()
	 */
	@Override
	protected void onOpen() {
		GenericButtonHandler.onOpen(this);
		// Init the key areas
		for (KeyArea keyArea : keyAreas) {
			keyArea.init(this);
		}
	}

	/**
	 * Called when we're done with a new binding assignment
	 */
	public void doneBinding() {
		SFX.textEntered();
		phase = PHASE_NORMAL;
		setEnabled(OK, true);
		setEnabled(RESET, true);
		setVisible(INSTRUCTIONS, false);
		setKeyboardNavigationEnabled(true);
		setGroupEnabled(BINDINGS_GROUP, true);
		Game.setPauseEnabled(true);
		updateBindingsDisplay();
	}

	/**
	 * Update bindings display
	 */
	private void updateBindingsDisplay() {
		// Update the binding display
		for (KeyArea keyArea : keyAreas) {
			keyArea.update();
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> children = XMLUtil.getChildren(element, "key");
		keyAreas = new LinkedList<KeyArea>();
		for (Element child : children) {
			KeyArea section = new KeyArea();
			section.load(child, loader);
			keyAreas.add(section);
		}
	}
	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doCreate()
	 */
	@Override
	protected void doCreateScreen() {
		for (KeyArea section : keyAreas) {
			section.create();
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doDestroy()
	 */
	@Override
	protected void doDestroyScreen() {
		for (KeyArea section : keyAreas) {
			section.destroy();
		}
	}

}
