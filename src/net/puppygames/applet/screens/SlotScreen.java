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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.PlayerSlot;
import net.puppygames.applet.Res;

/**
 * A dialog screen that allows users to select, create, or delete player slots. Slots
 * are shown using SlotEffects.
 */
public class SlotScreen extends DialogScreen implements SlotEffectListener {

	private static SlotScreen instance;

	/** max slots */
	private static final int maxSlots = 6;

	private static final String ID_NEW = "new";
	private static final String ID_DELETE = "delete";
	private static final String ID_RENAME = "rename";
	private static final String ID_SLOT_ORIGIN = "slot_origin";

	/** Slot class */
	private String slotClass;

	/** Gap between slots, vertically */
	private int gap;

	/** The slot effects */
	private transient List<SlotEffect> slots;

	/** Slot class */
	private transient Class<? extends PlayerSlot> slotClazz;

	/** Currently selected slot index */
	private transient int selectedIndex;

	private transient Area slotOriginArea;

	/**
	 * C'tor
	 * @param name
	 */
	public SlotScreen(String name) {
		super(name);
	}

	@Override
	protected void doRegister() {
		assert instance == null;
		instance = this;
	}

	@Override
	protected void doDeregister() {
		assert instance == this;
		instance = null;
	}

	/**
	 * Show the slot management screen
	 */
	public static void show() {
		if (!instance.isCreated()) {
			instance.create();
		}
		instance.open();
	}

	@Override
	protected void doCreateScreen() {
		try {
			slotClazz = (Class<? extends PlayerSlot>) Class.forName(slotClass);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		slotOriginArea = getArea(ID_SLOT_ORIGIN);
	}

	@Override
	protected void doOnOpen() {
		rebuild();
	}

	private void rebuild() {
		if (slots != null) {
			for (Iterator<SlotEffect> i = slots.iterator(); i.hasNext(); ) {
				SlotEffect se = i.next();
				se.remove();
			}
			slots = null;
		}
		List<PlayerSlot> playerSlots = PlayerSlot.getSlots();

		slots = new ArrayList<SlotEffect>(playerSlots.size());
		int y = slotOriginArea.getBounds().getY();

		for (PlayerSlot ps : playerSlots) {
			try {
				SlotEffect se = (SlotEffect) slotClazz.newInstance();
				se.setSlot(ps);
				se.setLocation(slotOriginArea.getBounds().getX(), y);
				se.spawn(this);
				se.setSlotEffectListener(this);
				if (ps.equals(Game.getPlayerSlot())) {
					se.setSelected(true);
				}
				slots.add(se);
				y -= gap;
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}

		selectedIndex = -2; // Force change
		// Find the current slot
		int idx = -1;
		for (Iterator<SlotEffect> i = slots.iterator(); i.hasNext(); ) {
			SlotEffect se = i.next();
			idx ++;
			if (se.isSelected()) {
				break;
			}
		}
		setSelectedIndex(idx);
	}

	@Override
	protected void onClicked(String id) {
		if (ID_NEW.equals(id)) {
			EnterNameDialog.show(true, new Runnable() {
				@Override
				public void run() {
					if (EnterNameDialog.isOKClicked()) {
						close();
					}
				}
			});
		} else if (ID_DELETE.equals(id)) {
			deleteSelected();
		} else if (ID_RENAME.equals(id)) {
			renameSelected();
		} else if (CANCEL.equals(id)) {
			if (selectedIndex != -1) {
				SlotEffect se = slots.get(selectedIndex);
				Game.setPlayerSlot(se.getSlot());
				close();
			}
		}
	}

	/**
	 * Delete the currently selected slot, and select the one that comes up next.
	 */
	protected void deleteSelected() {
		if (selectedIndex == -1) {
			return;
		}
		final SlotEffect se = slots.get(selectedIndex);
		String msg = Game.getMessage("lwjglapplets.slotscreen.delete_message");
		msg = msg.replace("[slot]", se.getSlot().getName().toUpperCase());
		Res.getDeleteYesCancelDialog().doModal(Game.getMessage("lwjglapplets.slotscreen.delete_title"), msg, new Runnable() {
			@Override
			public void run() {
				int option = Res.getDeleteYesCancelDialog().getOption();
				if (option == DialogScreen.OK_OPTION || option == DialogScreen.YES_OPTION) {
					se.getSlot().delete();
					if (selectedIndex == slots.size() - 1) {
						selectedIndex--;
					}
					if (selectedIndex == -1) {
						Game.setPlayerSlot(null);
						assert false; // We shouldn't really ever get here as delete is supposed to have been enabled...
					} else {
						PlayerSlot slot = slots.get(selectedIndex).getSlot();
						System.out.println("Setting player slot to " + slot.getName());
						Game.setPlayerSlot(slot);
					}
					rebuild();
				}
			}
		});
	}

	/**
	 * Rename the currently selected slot. Not yet implemented
	 */
	protected void renameSelected() {
	}

	@Override
	protected void doCleanup() {
		slots = null;
	}

	@Override
	public void onSlotEffectChanged(SlotEffect effect) {
		if (effect.isSelected()) {
			setSelectedIndex(slots.indexOf(effect));
			// Deselect all the others
			for (SlotEffect se : slots) {
				if (se != effect) {
					se.setSelected(false);
				}
			}
		}
	}

	@Override
	public void onSlotEffectEdited(SlotEffect effect) {
		// TODO Not yet implemented
	}

	/**
	 * Sets the currently selected slot
	 * @param newIndex
	 */
	public void setSelectedIndex(int newIndex) {
		if (selectedIndex != newIndex) {
			selectedIndex = newIndex;
			enableControls();
		}
	}

	protected void enableControls() {
		setEnabled(ID_NEW, slots.size() < maxSlots);
		setEnabled(ID_DELETE, selectedIndex != -1 && slots.size() > 1);
		setEnabled(ID_RENAME, selectedIndex != -1);
		setEnabled(CANCEL, selectedIndex != -1 || (selectedIndex == -1 && Game.getPlayerSlot() != null && Game.getPlayerSlot().exists()));
	}

}
