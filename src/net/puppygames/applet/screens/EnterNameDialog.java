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

import java.util.prefs.BackingStoreException;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.PlayerSlot;
import net.puppygames.applet.Res;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.widgets.TextField;

import com.shavenpuppy.jglib.resources.MappedColor;

/**
 * Enter name for player slot
 */
public class EnterNameDialog extends DialogScreen {

	private static final long serialVersionUID = 1L;

	/** End screen instance */
	private static EnterNameDialog instance;

	/*
	 * Button IDs
	 */
	private static final String NAME = "name";

	/*
	 * Layout
	 */

	/** Location of the name field */
	private int
		name_x,
		name_y;

	private MappedColor topColor, bottomColor, color;

	private boolean allCaps, allowUppercase;

	private transient TextField nameField;

	/** Are we allowed to click cancel? */
	private transient boolean allowCancel;

	/** Was OK clicked? */
	private transient boolean okClicked;

	private transient TickableObject nameObject;

	/**
	 * C'tor
	 */
	public EnterNameDialog(String name) {
		super(name);
	}

	@Override
	protected void doCreateScreen() {
		nameField = new TextField(14, getArea(NAME).getBounds().getWidth()) {
			@Override
			public boolean acceptChar(char c) {
				return Character.isLetterOrDigit(c) || c == ' ' || c == '_';
			}
			@Override
			protected void onChangeFocus() {
				SFX.textEntered();
				requestFocus(getArea(OK));
			}
			@Override
			protected void onEdited() {
				SFX.keyTyped();
				checkValid();
			}
		};
		if (color != null) {
			nameField.setColour(color);
		}
		if (topColor != null) {
			nameField.setTopColour(topColor);
		}
		if (bottomColor != null) {
			nameField.setBottomColour(bottomColor);
		}
		nameField.setAllCaps(allCaps);
		nameField.setAllowUppercase(allowUppercase);
		nameField.setFont(Res.getTinyFont());
		Area field = getArea(NAME);
		nameField.setLocation(name_x + field.getBounds().getX(), name_y + field.getBounds().getY());
	}

	@Override
	protected void onResized() {
		Area field = getArea(NAME);
		nameField.setLocation(name_x + field.getBounds().getX(), name_y + field.getBounds().getY());
	}

	@Override
	protected void doRegister() {
		instance = this;
	}

	@Override
	protected void doDeregister() {
		instance = null;
	}

	/**
	 * Show the enter name screen
	 * @param allowCancel Whether to show the cancel option
	 */
	public static void show(boolean allowCancel, Runnable callback) {
		if (!instance.isCreated()) {
			try {
				instance.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		instance.setAllowCancel(allowCancel);
		instance.doModal(Game.getMessage("lwjglapplets.enternamedialog.profile_name"), "", callback);
		instance.open();
	}

	@Override
	protected void doOnOpen() {
		Game.setPauseEnabled(false);
		nameField.setEditing(true);
		if (allowCancel) {
			setEnabled(CANCEL, true);
			setVisible(CANCEL, true);
		} else {
			setEnabled(CANCEL, false);
			setVisible(CANCEL, false);
		}
		checkValid();

		nameObject = new TickableObject() {
			@Override
			protected void render() {
				nameField.render(this);
			}
		};
		nameObject.setLayer(100);
		nameObject.spawn(this);
	}

	@Override
	protected void doOnClose() {
		Game.setPauseEnabled(true);
		if (nameObject != null) {
			nameObject.remove();
			nameObject = null;
		}
	}

	@Override
	protected void onClicked(String id) {
		if (id.equals(NAME)) {
			nameField.setEditing(true);
		} else if (id.equals(OK)) {
			PlayerSlot newSlot = new PlayerSlot(nameField.getText().trim().toLowerCase());
			if (newSlot.exists()) {
				Res.getErrorDialog().doModal(Game.getMessage("lwjglapplets.enternamedialog.error"), Game.getMessage("lwjglapplets.enternamedialog.exists"), null);
				return;
			}
			try {
				newSlot.create();
				Game.setPlayerSlot(newSlot);
				okClicked = true;
				close();
			} catch (BackingStoreException e) {
				e.printStackTrace(System.err);
				Res.getErrorDialog().doModal(Game.getMessage("lwjglapplets.enternamedialog.error"), Game.getMessage("lwjglapplets.enternamedialog.generalerror"), null);
				return;
			}
		} else if (id.equals(CANCEL) && allowCancel) {
			okClicked = false;
			close();
		}
	}

	@Override
	protected void doTick() {
		if (!isBlocked()) {
			if (!nameField.isEditing()) {
				nameField.setEditing(true);
			}
			nameField.tick();
		} else if (nameField.isEditing()) {
			nameField.setEditing(false);
		}
	}

	/**
	 * Sets whether we're allowed to cancel or not
	 * @param allowCancel the allowCancel to set
	 */
	private void setAllowCancel(boolean allowCancel) {
		this.allowCancel = allowCancel;
	}

	/**
	 * Check validity and enable/disable the ok button
	 */
	private boolean checkValid() {
		boolean ret = !nameField.getText().trim().equals("");
		setEnabled(OK, ret);
		return ret;
	}

	public static boolean isOKClicked() {
		return instance.okClicked;
	}

	public static String getSlotName() {
		return instance.nameField.getText().trim();
	}

	public static void setSlotName(String slotName) {
		instance.nameField.setText(slotName == null ? "" : slotName);
	}
}
