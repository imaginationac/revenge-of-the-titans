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
package com.shavenpuppy.jglib.sprites;

import org.lwjgl.opengl.GLContext;

import static org.lwjgl.opengl.EXTSecondaryColor.*;
import static org.lwjgl.opengl.GL11.*;


/**
 * Renders sprites as a white flash. Used when the sprite has setFlash(true) applied
 * to it.
 */
final class FlashStyle extends AbstractStyle {

	private static final long serialVersionUID = 1L;

	/** A handy singleton instance */
	static FlashStyle instance;

	public FlashStyle(String name) {
		super(name);
	}

	@Override
	public void setupState() {
		if (GLContext.getCapabilities().GL_EXT_secondary_color) {
			glEnable(GL_COLOR_SUM_EXT);
			glSecondaryColor3ubEXT((byte)255, (byte)255, (byte)255);
		}
		glBlendFunc(GL_SRC_ALPHA, GL_ONE);
		glEnableClientState(GL_VERTEX_ARRAY);
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		glEnableClientState(GL_COLOR_ARRAY);
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
	}

	@Override
	public void resetState() {
		if (GLContext.getCapabilities().GL_EXT_secondary_color) {
			glDisable(GL_COLOR_SUM_EXT);
			glSecondaryColor3ubEXT((byte)0, (byte)0, (byte)0);
		}
		glDisableClientState(GL_COLOR_ARRAY);
		glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
		glDisable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
		glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
	}

	@Override
	public int getStyleID() {
		return 4;
	}

	@Override
	public AlphaOp getAlphaOp() {
	    return AlphaOp.PREMULTIPLY;
	}

	@Override
	public boolean getRenderSprite() {
		return true;
	}
}
