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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Describes a player slot in a slot-managed Game.
 */
public class PlayerSlot implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The player's name */
	private final String name;

	/** Lazily instantiated Preferences */
	private transient Preferences preferences;

	/**
	 * C'tor
	 * @param name May not be null
	 */
	public PlayerSlot(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof PlayerSlot && (((PlayerSlot) obj).name.equals(name));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Check if a slot already exists
	 * @return
	 */
	public boolean exists() {
		try {
			if (!Game.getRoamingPreferences().nodeExists("slots_"+Game.getInternalVersion()+"/"+name)) {
				return false;
			}
			return getPreferences().getBoolean("active", false);
		} catch (BackingStoreException e) {
			e.printStackTrace(System.err);
			return false;
		}
	}

	/**
	 * Deletes this slot. If the slot doesn't exist, this is a no-op
	 */
	public void delete() {
		if (exists()) {
			try {
				getPreferences().removeNode();
				getPreferences().flush();
				preferences = null;
			} catch (BackingStoreException e) {
				e.printStackTrace(System.err);
			}

			// Also delete directory
			String slotDir = Game.getSlotDirectoryPrefix() + name;
			File slotDirF = new File(slotDir);
			// Recursively delete contents
			delTree(slotDirF);
		}
	}

	private static void delTree(File dir) {
		if (!dir.exists()) {
			return;
		}
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i ++) {
			if (files[i].isDirectory()) {
				delTree(files[i]);
			} else {
				if (!files[i].delete()) {
					System.out.println("Failed to delete "+files[i]);
				} else {
					System.out.println("Deleted "+files[i]);
				}
			}
		}
		if (!dir.delete()) {
			System.out.println("Failed to delete "+dir);
		} else {
			System.out.println("Deleted "+dir);
		}
	}

	/**
	 * Returns the Preferences node where we can store slot data for this PlayerSlot.
	 * The node will be created if it doesn't exist already.
	 * @return a Preferences
	 */
	public synchronized Preferences getPreferences() {
		if (preferences == null) {
			preferences = Game.getRoamingPreferences().node("slots_"+Game.getInternalVersion()+"/"+name);
		}
		return preferences;
	}

	/**
	 * Creates this slot permanently. If the slot is already created this is a no-op.
	 * @throws BackingStoreException if the slot cannot be created
	 */
	public void create() throws BackingStoreException {
		if (!exists()) {
			getPreferences().putBoolean("active", true);
			getPreferences().flush();
		}
	}

	/**
	 * Gets all the player slots in a {@link List}
	 * @return a List of PlayerSlots.
	 */
	public static List<PlayerSlot> getSlots() {
		Preferences dir = Game.getRoamingPreferences().node("slots_"+Game.getInternalVersion());
		try {
			String[] name = dir.childrenNames();
			List<PlayerSlot> ret = new ArrayList<PlayerSlot>(name.length);
			for (int i = 0; i < name.length; i ++) {
				if (dir.node(name[i]).getBoolean("active", false)) {
					ret.add(new PlayerSlot(name[i]));
				}
			}
			return ret;
		} catch (BackingStoreException e) {
			return new ArrayList<PlayerSlot>(0);
		}
	}
}
