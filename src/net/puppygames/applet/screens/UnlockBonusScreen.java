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

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.NagState;
import net.puppygames.applet.PrizeFeature;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.widgets.TextField;
import net.puppygames.gamecommerce.shared.NewsletterIncentive;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.resources.MappedColor;

import static org.lwjgl.opengl.GL11.*;

/**
 * Special screen for non-Puppygames customers that allows them to unlock a bonus from a code sent by email
 */
public class UnlockBonusScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static final boolean ALLOW_ANY_STRING = true;

	/** Singleton */
	private static UnlockBonusScreen instance;

	/*
	 * Button IDs
	 */
	private static final String CODE = "code";
	private static final String OK = "ok";
	private static final String CANCEL = "cancel";
	private static final String MESSAGE = "message";
	private static final String INSTRUCTION = "instruction";
	private static final String DONTSHOWAGAIN_OFF = "dontShowAgain_off";
	private static final String DONTSHOWAGAIN_ON = "dontShowAgain_on";
	private static final String DONTSHOWAGAIN = "dontShowAgain";
	private static final String CONTINUE = "continue";

	/*
	 * Layout
	 */
	private MappedColor topColor, bottomColor, color;
	private Rectangle codeInsets;
	private String font;

	private transient TextField emailField;
	private transient boolean valid;
	private transient GLFont fontResource;
	private transient boolean waitForMouse;
	private transient TickableObject emailObject;
	private transient NewsletterIncentive ni;
	private transient boolean done;


	/**
	 * C'tor
	 */
	public UnlockBonusScreen(String name) {
		super(name);
	}

	@Override
	protected void doCreateScreen() {
		emailField = new TextField(NewsletterIncentive.CODE_LENGTH, getArea(CODE).getBounds().getWidth()) {
			@Override
			public boolean acceptChar(char c) {
				return Character.isDigit(c) || Character.toUpperCase(c) >= 'A' && Character.toUpperCase(c) <= 'F';
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
		emailField.setFont(fontResource);
		if (color != null) {
			emailField.setColour(color);
		}
		if (topColor != null) {
			emailField.setTopColour(topColor);
		}
		if (bottomColor != null) {
			emailField.setBottomColour(bottomColor);
		}

		setVisible(CONTINUE, false);

	}

	@Override
	protected void onResized() {
		Area emailArea = getArea(CODE);
		emailField.setLocation(emailArea.getBounds().getX() + codeInsets.getX(), emailArea.getBounds().getY() + codeInsets.getY());
		emailField.setWidth(emailArea.getBounds().getWidth() - codeInsets.getWidth());
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
	 * Show the unlock bonus screen
	 */
	public static void show(NewsletterIncentive ni) {
		if (!instance.isCreated()) {
			try {
				instance.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		instance.ni = ni;
		instance.open();
	}

	@Override
	protected void onOpen() {
		Game.setPauseEnabled(false);
		emailField.setEditing(true);
		waitForMouse = true;
		checkValid();

		emailObject = new TickableObject() {
			@Override
			protected void render() {
				emailField.render(this);
			}
		};
		emailObject.setLayer(100);
		emailObject.spawn(this);
		enableButtons();
	}

	@Override
	protected void onClose() {
		Game.setPauseEnabled(true);
		if (emailObject != null) {
			emailObject.remove();
			emailObject = null;
		}
		MiniGame.showTitleScreen();

		NagState nagState = MiniGame.getNagState();
		if (nagState == NagState.REDEEMED || nagState == NagState.DONT_NAG) {
			MiniGame.getIncentiveFile().delete();
		}
	}


	@Override
	protected void onClicked(String id) {
		if (waitForMouse) {
			if (!Mouse.isButtonDown(0)) {
				waitForMouse = false;
			}
			return;
		}
		if (id.equals(CODE)) {
			emailField.setEditing(true);
		} else if (id.equals(OK) || id.equals(CONTINUE)) {
			if (done) {
				close();
			} else {
				unlock();
			}
		} else if (id.equals(CANCEL)) {
			close();
		} else if (id.equals(DONTSHOWAGAIN)) {
			NagState nagState = MiniGame.getNagState();
			switch (nagState) {
				case DONT_NAG:
					nagState = NagState.PRIZE_AWAITS;
					break;
				case PRIZE_AWAITS:
					nagState = NagState.DONT_NAG;
					break;
				case REDEEMED:
					return;
				default:
					assert false : "Shouldn't have nag state "+nagState;
					return;
			}
			MiniGame.setNagState(nagState);
			enableButtons();
		}
	}

	private void unlock() {
		if (!checkValid()) {
			return;
		}

		SFX.newGame();

		PrizeFeature prize = Resources.get(ni.getPrize());
		MiniGame.setPrize(prize);
		MiniGame.setNagState(NagState.REDEEMED);

		getArea(MESSAGE).setText(prize.getSuccessMessage());
		setVisible(CANCEL, false);
		setVisible(CODE, false);
		setVisible(INSTRUCTION, false);
		enableButtons();
		emailObject.remove();
		emailObject = null;

		//getArea(OK).setText("CONTINUE");
		setVisible(OK, false);
		setVisible(CONTINUE, true);

		done = true;

	}

	private void enableButtons() {
    	NagState nagState = MiniGame.getNagState();
    	setVisible(DONTSHOWAGAIN_ON, 	 nagState == NagState.DONT_NAG);
    	setVisible(DONTSHOWAGAIN_OFF, 	 nagState == NagState.PRIZE_AWAITS);
    	setVisible(DONTSHOWAGAIN, 		 nagState != NagState.REDEEMED);

    	getArea(DONTSHOWAGAIN_OFF).init();
    	getArea(DONTSHOWAGAIN_ON).init();
    	getArea(DONTSHOWAGAIN).init();
    }

	@Override
	protected void doTick() {
		if (!isBlocked()) {
			if (!Mouse.isButtonDown(0)) {
				waitForMouse = false;
			}
			if (!done) {
				emailField.tick();
				boolean wasEnabled = getArea(OK).isEnabled();
				if (wasEnabled != valid) {
					setEnabled(OK, valid);
				}
			}
		}
	}

	/**
	 * Check validity and enable/disable the Register button
	 */
	@SuppressWarnings("unused")
	private boolean checkValid() {
		return valid = (Game.DEBUG && ALLOW_ANY_STRING) || emailField.getText().equalsIgnoreCase(ni.getShortCode());
	}

	@Override
	protected void preRender() {
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	}
}
