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

import net.puppygames.applet.PlayerSlot;
import net.puppygames.applet.effects.Effect;

/**
 * Abstract base class for {@link SlotEffect}s
 */
public abstract class AbstractSlotEffect extends Effect implements SlotEffect {

	/** Whether this effect is currently being hovered over by the mouse */
	private boolean hovered;

	/** Whether this effect is currently the "selected" one */
	private boolean selected;

	/** The slot */
	private PlayerSlot slot;

	/** Location */
	private int x, y;

	/** Listener */
	private SlotEffectListener listener;

	/** Enabled */
	private boolean enabled = true;

	/**
	 * C'tor
	 */
	public AbstractSlotEffect() {
	}

	/**
	 * Enable or disable the effect
	 * @param enabled the enabled to set
	 */
	@Override
	public final void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			onSetEnabled();
		}
	}
	protected void onSetEnabled() {}

	/**
	 * @return true if we're enabled, that is, responding to mouse & keyboard
	 */
	public final boolean isEnabled() {
		return enabled;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.SlotEffect#getSlot()
	 */
	@Override
	public final PlayerSlot getSlot() {
		return slot;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.SlotEffect#isHovered()
	 */
	@Override
	public final boolean isHovered() {
		return hovered;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.SlotEffect#isSelected()
	 */
	@Override
	public final boolean isSelected() {
		return selected;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.SlotEffect#setLocation(int, int)
	 */
	@Override
	public final void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
		onSetLocation();
	}
	protected void onSetLocation() {}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.SlotEffect#setSlot(net.puppygames.applet.PlayerSlot)
	 */
	@Override
	public final void setSlot(PlayerSlot slot) {
		if (this.slot != slot) {
			this.slot = slot;
			onSetSlot();
			fireChanged();
		}
	}
	protected void onSetSlot() {}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.SlotEffect#setSlotEffectListener(net.puppygames.applet.screens.SlotEffectListener)
	 */
	@Override
	public final void setSlotEffectListener(SlotEffectListener listener) {
		this.listener = listener;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.SlotEffect#setSelected(boolean)
	 */
	@Override
	public final void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.selected = selected;
			onSetSelected();
			fireChanged();
		}
	}
	protected void onSetSelected() {}

	/**
	 * Sets the hovered state
	 * @param hovered
	 */
	protected final void setHovered(boolean hovered) {
		if (this.hovered != hovered) {
			this.hovered = hovered;
			onSetHovered();
			fireChanged();
		}
	}
	protected void onSetHovered() {}

	/**
	 * Inform the listener there has been a change
	 */
	protected final void fireChanged() {
		if (listener != null) {
			listener.onSlotEffectChanged(this);
		}
	}

	/**
	 * @return the x
	 */
	public final int getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public final int getY() {
		return y;
	}

}
