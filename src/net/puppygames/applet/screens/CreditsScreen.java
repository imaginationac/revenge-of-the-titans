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
 * $Id: CreditsScreen.java,v 1.1 2009/07/01 15:38:53 foo Exp $
 * Displays game instructions
 * @author $Author: foo $
 * @version $Revision: 1.1 $
 */
public class CreditsScreen extends Screen implements GenericButtons {

	private static final long serialVersionUID = 1L;

	/** End screen instance */
	private static CreditsScreen instance;


	/**
	 * @param name
	 */
	public CreditsScreen(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see genesis.Feature#doRegister()
	 */
	@Override
	protected void doRegister() {
		instance = this;
	}

	/**
	 * Show the credits screen
	 */
	public static void show() {
		if (instance == null) {
			return;
		}
		if (!instance.isCreated()) {
			try {
				instance.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		instance.open();
	}

	/* (non-Javadoc)
	 * @see genesis.Screen#onClicked(java.lang.String)
	 */
	@Override
	protected void onClicked(String id) {
		GenericButtonHandler.onClicked(id);
	}

	/* (non-Javadoc)
	 * @see genesis.Screen#onOpen()
	 */
	@Override
	protected void onOpen() {
		GenericButtonHandler.onOpen(this);
	}

}
