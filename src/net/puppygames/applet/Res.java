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

import net.puppygames.applet.screens.DialogScreen;

import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.sprites.SpriteImage;

/**
 * $Id: Res.java,v 1.5 2010/09/02 16:34:54 chazeem Exp $
 * Miscellaneous resources.
 * <p>
 * @author $Author: chazeem $
 * @version $Revision: 1.5 $
 */
public class Res extends Feature {

	private static final long serialVersionUID = 1L;

	/*
	 * Singleton
	 */

	private static Res instance;

	/*
	 * Resource data
	 */

	private String
		tinyFont = "tinyfont.glfont",
		smallFont = "smallfont.glfont",
		bigFont = "bigfont.glfont",
		tinyFontFullscreen = "tinyfont.fullscreen.glfont",
		smallFontFullscreen = "smallfont.fullscreen.glfont",
		bigFontFullscreen = "bigfont.fullscreen.glfont",
		particleImage = "spriteimage.particle",
		messageTitleBackground = "message_title_background",
		messageBodyBackground="message_body_background",
		progressBackground = "progress_background",
		yesNoCancelDialog = "yesnocancel.dialog",
		yesCancelDialog = "yescancel.dialog",
		deleteYesCancelDialog = "deleteyescancel.dialog",
		errorDialog = "error.dialog",
		infoDialog = "info.dialog",
		cancelDialog = "cancel.dialog"
		;

	/*
	 * Transient data
	 */

	private transient GLFont tinyFontResource, smallFontResource, bigFontResource;
	private transient SpriteImage particleImageResource;
	private transient Background messageTitleBackgroundResource, messageBodyBackgroundResource, progressBackgroundResource;
	private transient DialogScreen yesNoCancelDialogResource, yesCancelDialogResource, deleteYesCancelDialogResource,errorDialogResource, infoDialogResource, cancelDialogResource;

	/**
	 * C'tor
	 */
	public Res() {
		super("res");
	}

	@Override
	protected void doRegister() {
		assert instance == null;
		instance = this;
	}

	@Override
	protected void doDeregister() {
		instance = null;
	}

	/**
	 * Create the resources
	 */
	public static void createResources() throws Exception {
		instance.create();
	}

	/**
	 * @return Returns the bigFontResource.
	 */
	public static GLFont getBigFont() {
		return instance.bigFontResource;
	}

	/**
	 * @return Returns the smallFontResource.
	 */
	public static GLFont getSmallFont() {
		return instance.smallFontResource;
	}

	/**
	 * @return Returns the tinyFontResource.
	 */
	public static GLFont getTinyFont() {
		return instance.tinyFontResource;
	}

	/**
	 * @return Returns the particleImageResource.
	 */
	public static SpriteImage getParticleImage() {
		return instance.particleImageResource;
	}

	/**
	 * @return Returns the messageBodyBackgroundResource.
	 */
	public static Background getMessageBodyBackground() {
		return instance.messageBodyBackgroundResource;
	}

	/**
	 * @return Returns the messageTitleBackgroundResource.
	 */
	public static Background getMessageTitleBackground() {
		return instance.messageTitleBackgroundResource;
	}

	/**
	 * @return
	 */
	public static Background getProgressBackground() {
		return instance.progressBackgroundResource;
	}

	/**
	 * @return the yesNoCancelDialogResource
	 */
	public static DialogScreen getYesNoCancelDialog() {
		return instance.yesNoCancelDialogResource;
	}

	/**
	 * @return the yesNoCancelDialogResource
	 */
	public static DialogScreen getYesCancelDialog() {
		return instance.yesCancelDialogResource;
	}

	/**
	 * @return the yesNoCancelDialogResource
	 */
	public static DialogScreen getDeleteYesCancelDialog() {
		return instance.deleteYesCancelDialogResource;
	}

	/**
	 * @return the errorDialogResource
	 */
	public static DialogScreen getErrorDialog() {
		return instance.errorDialogResource;
	}

	public static DialogScreen getInfoDialog() {
		return instance.infoDialogResource;
	}

	public static DialogScreen getCancelDialog() {
		return instance.cancelDialogResource;
	}
}
