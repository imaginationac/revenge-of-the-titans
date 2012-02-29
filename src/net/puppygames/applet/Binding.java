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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.shavenpuppy.jglib.resources.Feature;

/**
 * $Id: Binding.java,v 1.2 2010/10/14 11:59:25 foo Exp $
 * Describes a binding of a key to a game command.
 * @author $Author: foo $
 * @version $Revision: 1.2 $
 */
public class Binding extends Feature {

	private static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	/**
	 * Indicates a mouse button binding
	 */
	public static final String MOUSE = "mouse";

	/**
	 * Indicates a keyboard binding
	 */
	public static final String KEYBOARD = "keyboard";

	/**
	 * Indicates a controller button binding
	 */
	public static final String CONTROLLER = "controller";

	/**
	 * Indicates a controller D-Pad binding
	 */
	public static final String DPAD = "dpad";

	public static final String DPAD_RIGHT = "D-Pad Right";
	public static final String DPAD_UP = "D-Pad Up";
	public static final String DPAD_LEFT= "D-Pad Left";
	public static final String DPAD_DOWN = "D-Pad Down";
	public static final int DPAD_RIGHT_INDEX = 0;
	public static final int DPAD_UP_INDEX = 1;
	public static final int DPAD_LEFT_INDEX = 2;
	public static final int DPAD_DOWN_INDEX = 3;

	/*
	 * Standard bindings
	 */
	public static final String FOCUS_UP = "focus.up.binding";
	public static final String FOCUS_DOWN = "focus.down.binding";
	public static final String FOCUS_LEFT = "focus.left.binding";
	public static final String FOCUS_RIGHT = "focus.right.binding";
	public static final String SELECT = "select.binding";
	public static final String YES = "yes.binding";
	public static final String NO = "no.binding";
	public static final String CANCEL = "cancel.binding";
	public static final String FOCUS_UP_ALT = "focus.up.binding.alt";
	public static final String FOCUS_DOWN_ALT = "focus.down.binding.alt";
	public static final String FOCUS_LEFT_ALT = "focus.left.binding.alt";
	public static final String FOCUS_RIGHT_ALT = "focus.right.binding.alt";
	public static final String SELECT_ALT = "select.binding.alt";
	public static final String YES_ALT = "yes.binding.alt";
	public static final String NO_ALT = "no.binding.alt";
	public static final String CANCEL_ALT = "cancel.binding.alt";



	/** All bindings: a map of binding names to bindings */
	private static final Map<String, Binding> BINDINGS = new HashMap<String, Binding>();

	/** All bindings in order */
	private static final List<Binding> ALL = new LinkedList<Binding>();

	/** Whether bindings are enabled */
	private static boolean enabled = true; // Enabled by default

	private static class SavedBinding implements Serializable {
		private static final long serialVersionUID = -389520805784486704L;

		String name;
		String type;
		int index;

		SavedBinding(Binding b) {
			name = b.getName();
			type = b.type;
			index = b.index;
		}
	}

	/*
	 * Resource data
	 */

	/** Binding type (keyboard, mouse, controller) */
	private String type;

	/** Binding value */
	private String value;

	/** Hold "down" */
	private boolean hold;

	/** Description */
	private String description;

	/** Default binding */
	private String defaultType;
	private int defaultIndex;

	/*
	 * Transient data
	 */

	/** Button index in the controller */
	private transient int index;

	/** Is the binding down? */
	private transient boolean down;

	/** Was the binding down last time? */
	private transient boolean wasDown;

	/**
	 * @param name
	 */
	public Binding(String name) {
		super(name);
		setAutoCreated();
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doRegister()
	 */
	@Override
	protected void doRegister() {
		BINDINGS.put(getName(), this);
		ALL.add(this);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doDeregister()
	 */
	@Override
	protected void doDeregister() {
		BINDINGS.remove(getName());
		ALL.remove(this);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doCreate()
	 */
	@Override
	protected void doCreate() {
		super.doCreate();

		if (KEYBOARD.equals(type)) {
			index = Keyboard.getKeyIndex(value);
		} else if (MOUSE.equals(type)) {
			index = Mouse.getButtonIndex(value);
		} else if (CONTROLLER.equals(type)) {
			index = -1;
			if (Controllers.getControllerCount() > 0) {
				Controller controller = Controllers.getController(0);
				for (int i = 0; i < controller.getButtonCount(); i ++) {
					if (controller.getButtonName(i).equals(value)) {
						index = i;
						break;
					}
				}
			}
		} else if (DPAD.equals(type)) {
			if (DPAD_LEFT.equals(value)) {
				index = DPAD_LEFT_INDEX;
			} else if (DPAD_RIGHT.equals(value)) {
				index = DPAD_RIGHT_INDEX;
			} else if (DPAD_UP.equals(value)) {
				index = DPAD_UP_INDEX;
			} else if (DPAD_DOWN.equals(value)) {
				index = DPAD_DOWN_INDEX;
			}
		}

		defaultType = type;
		defaultIndex = index;
	}

	/**
	 * Reset this binding to its default
	 */
	private void reset() {
		type = defaultType;
		index = defaultIndex;
	}

	/**
	 * Reset all bindings to defaults
	 */
	public static void resetToDefaults() {
		for (Binding b : BINDINGS.values()) {
			b.reset();
		}
		validate();
	}

	/**
	 * Load the bindings from an input stream. After loading they are validated.
	 * @param stream
	 * @throws Exception
	 */
	public static void load(InputStream stream) throws Exception {
		ObjectInputStream ois = new ObjectInputStream(stream);
        List<SavedBinding> newBindings = (List<SavedBinding>) ois.readObject();
		for (SavedBinding b : newBindings) {
			setBinding(b.name, b.type, b.index);
		}
		validate();
	}

	/**
	 * Save the bindings to an input stream.
	 * @param stream
	 * @throws Exception
	 */
	public static void save(OutputStream stream) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(stream);
		List<SavedBinding> output = new LinkedList<SavedBinding>();
		for (Binding b : BINDINGS.values()) {
			output.add(new SavedBinding(b));
		}
		oos.writeObject(output);
		oos.flush();
		oos.reset();
	}

	/**
	 * Validate all the bindings. Any bindings which are invalid are cleared,
	 * giving them a type of null.
	 */
	public static void validate() {
		for (Binding b : BINDINGS.values()) {
			if (KEYBOARD.equals(b.type) && !Keyboard.isCreated()) {
				b.type = null;
			} else if (MOUSE.equals(b.type) && !Mouse.isCreated()) {
				b.type = null;
			} else if (CONTROLLER.equals(b.type) && (!Controllers.isCreated() || Controllers.getControllerCount() == 0)) {
				//System.out.println("Zapped "+b);
				b.type = null;
			} else if (DPAD.equals(b.type) && (!Controllers.isCreated() || Controllers.getControllerCount() == 0)) {
				//System.out.println("Zapped "+b);
				b.type = null;
			}
		}
	}

	/**
	 * Get all the bindings.
	 * @return an unmodifiable List of binding names to Bindings
	 */
	public static List<Binding> getBindings() {
		return Collections.unmodifiableList(ALL);
	}

	/**
	 * Is this binding "down"? Call this method after Binding.poll()
	 * @return true if the binding is down
	 */
	public boolean isDown() {
		if (hold) {
			return down;
		} else if (wasDown) {
			return false;
		} else {
			return down;
		}
	}

	/**
	 * Poll all bindings
	 */
	public static void poll() {
		if (!enabled) {
			return;
		}
		for (Iterator<Binding> i = BINDINGS.values().iterator(); i.hasNext(); ) {
			Binding b = i.next();
			if (b.index == -1) {
				continue;
			}
			b.wasDown = b.down;
			if (KEYBOARD.equals(b.type)) {
				if (Keyboard.isKeyDown(b.index)) {
					b.down = true;
				} else {
					b.down = false;
				}
			} else if (MOUSE.equals(b.type)) {
				if (Mouse.getButtonCount() > b.index && Mouse.isButtonDown(b.index)) {
					b.down = true;
				} else {
					b.down = false;
				}
			} else if (CONTROLLER.equals(b.type)) {
				if (Controllers.getControllerCount() > 0 && Controllers.getController(0).getButtonCount() > b.index && Controllers.getController(0).isButtonPressed(b.index)) {
					b.down = true;
				} else {
					b.down = false;
				}
			} else if (DPAD.equals(b.type)) {
				if (Controllers.getControllerCount() == 0) {
					b.down = false;
				} else {
					float dx = Controllers.getController(0).getPovX();
					float dy = Controllers.getController(0).getPovY();
					switch (b.index) {
						case DPAD_LEFT_INDEX:
							b.down = dx < -0.5f;
							break;
						case DPAD_RIGHT_INDEX:
							b.down = dx > 0.5f;
							break;
						case DPAD_UP_INDEX:
							b.down = dy < -0.5f;
							break;
						case DPAD_DOWN_INDEX:
							b.down = dy > 0.5f;
							break;
						default:
							b.down = false;
					}
				}
			}
		}
	}

	/**
	 * Enable all bindings. When disabled, polling has no effect
	 * @param enable
	 */
	public static void setEnabled(boolean enabled) {
		Binding.enabled = enabled;
		for (Iterator<Binding> i = BINDINGS.values().iterator(); i.hasNext(); ) {
			Binding b = i.next();
			b.down = false;
			b.wasDown = false;
		}

	}

	/**
	 * Set a binding.
	 * @param name The name of the binding
	 * @param type The type ("keyboard" or "mouse")
	 * @param index The button index
	 */
	public static void setBinding(String name, String type, int index) {
		// Look for an existing binding of that name
		Binding b = BINDINGS.get(name);
		if (b == null) {
			// No such binding. So, er, bugger it, do nothing.
			assert false : "no such binding "+name;
			return;
		}
		b.type = type;
		b.index = index;
		System.out.println("Set binding "+name+" to "+type+":"+index);
//		// See if there is an existing binding already. If there is, then clear
//		// the old one.
//		if (type != null) {
//			for (Iterator i = BINDINGS.values().iterator(); i.hasNext(); ) {
//				Binding existing = (Binding) i.next();
//				if (!existing.name.equals(name)) {
//					if (b.type.equals(existing.type) && b.index == existing.index) {
//						// Clear it
//						existing.type = null;
//						existing.index = 0;
//					}
//				}
//			}
//		}
		validate(); // Just to be sure
	}

	/**
	 * Is a particular named binding down?
	 * @param name
	 * @return boolean
	 */
	public static boolean isBindingDown(String name) {
		Binding b = BINDINGS.get(name);
		if (b == null) {
			return false;
		} else {
			return b.isDown();
		}
	}

	/**
	 * @return Returns the index.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return Returns the type (null if undefined)
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}
}