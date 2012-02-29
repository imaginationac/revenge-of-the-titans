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
package worm.screens;

import net.puppygames.applet.Area;

import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.sprites.SpriteImage;

/**
 * @author Cas
 */
public class InstructionsScreen extends net.puppygames.applet.screens.InstructionsScreen {

	private static final long serialVersionUID = 1L;

	private static final String ID_NEXT = "next";
	private static final String ID_PREV = "prev";
	private static final String ID_HINTS = "hints";
	private static final String ID_PAGE_COUNT = "page_count";

	/** Hints */
	private ResourceArray hints;

	/*
	 * Transient data
	 */

	private transient int currentHint;
	private transient Area hintsArea;
	private transient Area pageCountArea;

	/**
	 * C'tor
	 * @param name
	 */
	public InstructionsScreen(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#doCreateScreen()
	 */
	@Override
	protected void doCreateScreen() {
		hintsArea = getArea(ID_HINTS);
		pageCountArea = getArea(ID_PAGE_COUNT);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.InstructionsScreen#onOpen()
	 */
	@Override
	protected void onOpen() {
		super.onOpen();

		setCurrentHint(0);
		setEnabled("exit", false);
	}

	private void setCurrentHint(int newCurrentHint) {
		currentHint = newCurrentHint;
		int numHints = hints.getNumResources();
		hintsArea.getSprite().setImage((SpriteImage) hints.getResource(newCurrentHint));
		pageCountArea.setText(newCurrentHint+1+" / "+numHints);
		setEnabled(ID_PREV,(newCurrentHint==0 ? false : true));
		setEnabled(ID_NEXT,(newCurrentHint==numHints-1 ? false : true));
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.screens.InstructionsScreen#onClicked(java.lang.String)
	 */
	@Override
	protected void onClicked(String id) {
		super.onClicked(id);

		if (id.equals(ID_PREV)) {
			int newCurrentHint = currentHint - 1;
			if (newCurrentHint < 0) {
				newCurrentHint = hints.getNumResources() - 1;
			}
			setCurrentHint(newCurrentHint);
		}
		if (id.equals(ID_NEXT)) {
			int newCurrentHint = currentHint + 1;
			if (newCurrentHint >= hints.getNumResources()) {
				newCurrentHint = 0;
			}
			setCurrentHint(newCurrentHint);
		}
	}
}
