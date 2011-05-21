/*
 * Copyright (c) 2003 Shaven Puppy Ltd All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  * Neither the name of 'Shaven Puppy' nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.puppygames.applet.effects;
import net.puppygames.applet.Game;

import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Feature;
/**
 * $Id: SFX.java,v 1.1 2009/07/01 15:38:53 foo Exp $
 * Standard sound effects used by the screens.
 * @author $Author: foo $
 * @version $Revision: 1.1 $
 */
public class SFX extends Feature {

	private static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */
	
	/** SFX instance */
	private static SFX instance;
	
	/*
	 * Resource data
	 */
	private String
			buttonHover = "buttonhover.buffer",
			buttonClick = "buttonclick.buffer",
			keyTyped = "keytyped.buffer",
			textEntered = "textentered.buffer",
			newGame = "newGame.buffer",
			gameOver = "gameOver.buffer";
	/*
	 * Transient data
	 */
	private transient ALBuffer
			buttonHoverBuffer,
			buttonClickBuffer,
			newGameBuffer,
			keyTypedBuffer,
			textEnteredBuffer,
			gameOverBuffer;
	
	/**
	 * C'tor
	 */
	public SFX() {
		super("sfx");
		setAutoCreated();
	}
	
	/**
	 * @return Returns the textEnteredBuffer.
	 */
	public static ALBuffer getTextEnteredBuffer() {
		return instance.textEnteredBuffer;
	}
	
	/**
	 * @return Returns the gameOverBuffer.
	 */
	public static ALBuffer getGameOverBuffer() {
		return instance.gameOverBuffer;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see genesis.Feature#doRegister()
	 */
	@Override
	protected void doRegister() {
		instance = this;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see genesis.Feature#doDeregister()
	 */
	@Override
	protected void doDeregister() {
		instance = null;
	}
	
	public static void buttonHover() {
		Game.allocateSound(instance.buttonHoverBuffer);
	}
	public static void buttonClick() {
		Game.allocateSound(instance.buttonClickBuffer);
	}
	public static void newGame() {
		Game.allocateSound(instance.newGameBuffer);
	}
	public static void gameOver() {
		Game.allocateSound(instance.gameOverBuffer);
	}
	public static void keyTyped() {
		Game.allocateSound(instance.keyTypedBuffer);
	}
	public static void textEntered() {
		Game.allocateSound(instance.textEnteredBuffer);
	}
	/**
	 * Create the sound effects
	 */
	public static void createSFX() throws Exception {
		instance.create();
	}

}