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

import net.puppygames.applet.Screen;

/**
 * A dialog screen with yes, no, ok, cancel buttons, available in
 * various configurations
 * @author Cas
 */
public class DialogScreen extends Screen {

	/*
	 * Button IDs
	 */

	protected static final String OK = "ok";
	protected static final String CANCEL = "cancel";
	protected static final String YES = "yes";
	protected static final String NO = "no";
	protected static final String TITLE = "title";
	protected static final String MESSAGE = "message";

	/*
	 * Option IDs
	 */

	public static final int NONE = -1;
	public static final int OK_OPTION = 0;
	public static final int CANCEL_OPTION = 1;
	public static final int YES_OPTION = 2;
	public static final int NO_OPTION = 3;

	/*
	 * Transient data
	 */

	private transient int option;

	/** The callback to execute when the dialog is closed (optional) */
	private transient Runnable callback;

	/**
	 * C'tor
	 * @param name
	 */
	public DialogScreen(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#onOpen()
	 */
	@Override
	protected final void onOpen() {
		option = NONE;
		doOnOpen();
	}
	protected void doOnOpen() {}

	/**
	 * Sets the title and message and opens the dialog, and sets the callback to
	 * run after this dialog is closed. This callback is executed no matter
	 * which option was chosen. Find out which option was chosen with
	 * getOption().
	 * @param callback the callback to set
	 */
	public void doModal(String title, String message, Runnable callback) {
		if (getArea(TITLE) != null && title != null) {
			getArea(TITLE).setText(title);
		}
		if (getArea(MESSAGE) != null && message != null) {
			getArea(MESSAGE).setText(message);
		}
		this.callback = callback;
		open();
	}

	/**
	 * Sets the title and message and opens the dialog, and sets the callback to
	 * run after this dialog is closed. This callback is executed no matter
	 * which option was chosen. Find out which option was chosen with
	 * @param callback the callback to set
	 */
	public void doModal(String title, String message) {
		doModal(title, message, null);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#onClose()
	 */
	@Override
	protected final void onClose() {
		if (callback != null) {
			callback.run();
			callback = null;
		}
		doOnClose();
	}
	protected void doOnClose() {}

	/**
	 * @return the chosen option
	 */
	public int getOption() {
		return option;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#onClicked(java.lang.String)
	 */
	@Override
	protected void onClicked(String id) {
		if (OK.equals(id)) {
			option = OK_OPTION;
			close();
		}
		if (CANCEL.equals(id)) {
			option = CANCEL_OPTION;
			close();
		}
		if (YES.equals(id)) {
			option = YES_OPTION;
			close();
		}
		if (NO.equals(id)) {
			option = NO_OPTION;
			close();
		}
	}
}
