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

import java.rmi.Naming;

import net.puppygames.applet.*;
import net.puppygames.applet.Game;
import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.widgets.TextField;
import net.puppygames.gamecommerce.shared.*;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.util.CheckOnline;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: RegisterScreen.java,v 1.5 2010/09/27 00:05:50 foo Exp $
 * <p>
 * Shown before the title screen to allow the player to register the game.
 *
 * @author $Author: foo $
 * @version $Revision: 1.5 $
 */
public class RegisterScreen extends Screen {

	public static final long serialVersionUID = 1L;

	/** End screen instance */
	private static RegisterScreen instance;

	private static final Object cancelLock = new Object();

	/*
	 * Button IDs
	 */
	private static final String EMAIL = "email";
	private static final String REGISTER = "register";
	private static final String LATER = "later";
	private static final String ORDER = "order";

	/*
	 * Layout
	 */
	private MappedColor topColor, bottomColor, color;
	private Rectangle emailInsets;
	private String font;

	private transient TextField emailField;
	private transient boolean valid;
	private transient RegistrationDetails regDetails;
	private transient GLFont fontResource;
	private transient DialogScreen waitDialog;
	private transient Thread registerThread;
	private transient boolean cancelRegistration;
	private transient Runnable thingToDo;
	private transient boolean waitForMouse;
	private transient TickableObject emailObject;

	/**
	 * C'tor
	 */
	public RegisterScreen(String name) {
		super(name);
	}

	@Override
	protected void doCreateScreen() {
		emailField = new TextField(64, getArea(EMAIL).getBounds().getWidth()) {
			@Override
			public boolean acceptChar(char c) {
				return Character.isLetterOrDigit(c) || c >= ' ' && c <= 127;
			}
			@Override
			protected void onChangeFocus() {
				SFX.textEntered();
				requestFocus(getArea(REGISTER));
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
	}

	@Override
	protected void onResized() {
		Area emailArea = getArea(EMAIL);
		emailField.setLocation(emailArea.getBounds().getX() + emailInsets.getX(), emailArea.getBounds().getY() + emailInsets.getY());
		emailField.setWidth(emailArea.getBounds().getWidth() - emailInsets.getWidth());
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
	 * Show the registration screen
	 */
	public static void show() {
		if (!instance.isCreated()) {
			try {
				instance.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		instance.open();
	}

	@Override
	protected void onOpen() {
		Game.setPauseEnabled(false);
		Game.buy(false);
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
	}

	@Override
	protected void onClose() {
		Game.setPauseEnabled(true);
		if (emailObject != null) {
			emailObject.remove();
			emailObject = null;
		}

	}


	@Override
	protected void onClicked(String id) {
		if (waitForMouse) {
			return;
		}
		if (id.equals(EMAIL)) {
			emailField.setEditing(true);
		} else if (id.equals(REGISTER)) {
			registration();
		} else if (id.equals(LATER)) {
			NagScreen.show("You know you want to!", false);
		} else if (id.equals(ORDER)) {
			Game.buy(true);
		}
	}

	private void registration() {
		if (!checkValid()) {
			return;
		}

		if (!CheckOnline.isOnline()) {
			registrationFailed("It appears that you are not connected to the internet! Please exit "+Game.getTitle()+" and connect to the internet, and try again.", true);
		} else {
			waitDialog = Res.getCancelDialog();
			waitDialog.doModal("REGISTERING", "Please wait whilst "+Game.getTitle()+" contacts the Puppy Games registration server to unlock your game.",
					new Runnable() {
				@Override
				public void run() {
					int option = waitDialog.getOption();
					// Cancel registration
					if (option != DialogScreen.NONE) {
						waitForMouse = true;
						synchronized (cancelLock) {
							cancelRegistration = true;
							if (registerThread != null) {
								registerThread.interrupt();
							}
						}
					}
					waitDialog = null;
				}
			});

			registerThread = new Thread() {
				@Override
				public void run() {
					try {
						RegistrationServerRemote server;
						try {
							server = (RegistrationServerRemote) Naming.lookup(RegistrationServerRemote.RMI_URL);
						} catch (Exception e) {
							e.printStackTrace(System.err);
							synchronized (cancelLock) {
								if (!cancelRegistration) {
									Game.onRegistrationDisaster();
									registrationFailed("There has been a problem connecting to the Puppy Games registration server. Please ensure you are online and try again, or alternatively contact us on "+
											Game.getSupportEmail()+" for assistance.", false);
								}
								cancelRegistration = false;
							}
							return;
						}
						RegistrationDetails regDetails;
						try {
							regDetails = server.register(emailField.getText(), Game.getTitle(), Game.getVersion(), Game.getInstallation(), System
									.getProperty("os.name"), Game.getConfiguration());
							Game.onRegistrationRecovery();
							registrationSuccess(regDetails);
						} catch (RegisterException e) {
							Game.onRegistrationRecovery();
							registrationFailed(e.getMessage(), true);
							return;
						} catch (Throwable e) {
							e.printStackTrace(System.err);
							synchronized (cancelLock) {
								if (!cancelRegistration) {
									Game.onRegistrationDisaster();
									registrationFailed("There has been a problem registering your game with Puppy Games registration server. Please contact us on "+
											Game.getSupportEmail()+" for assistance.", true);
									cancelRegistration = false;
								}
							}
							return;
						}
					} finally {
						synchronized (cancelLock) {
							registerThread = null;
						}
					}
				}
			};

			registerThread.start();
		}
	}

	/**
	 * Called when registration fails
	 * @param message The failure message
	 * @param nextPhase The phase to go to next
	 */
	private void registrationFailed(final String message, final boolean fatal) {
		thingToDo = new Runnable() {
			@Override
			public void run() {
				if (waitDialog != null && waitDialog.isOpen()) {
					waitDialog.close();
					waitDialog = null;
				}
				Res.getInfoDialog().doModal("FAILED", message, new Runnable() {
					@Override
					public void run() {
						Res.getInfoDialog().getOption();
						if (fatal) {
							TitleScreen.show();
						}
						waitForMouse = true;
					}
				});
				Game.buy(false);
			}
		};
	}

	/**
	 * Called when registration succeeds
	 * @param regDetails The registration details
	 */
	private void registrationSuccess(final RegistrationDetails regDetails) {
		thingToDo = new Runnable() {
			@Override
			public void run() {
				Game.clearBuy();
				Game.onRemoteCallSuccess();
				TitleScreen.instantiate();
				Game.setRegistrationDetails(regDetails);
				if (waitDialog != null && waitDialog.isOpen()) {
					waitDialog.close();
					waitDialog = null;
				}

				Res.getInfoDialog().doModal("SUCCESS", "Congratulations! Your copy of "+Game.getTitle()+" has been unlocked and registered to you.", new Runnable() {
					@Override
					public void run() {
						Res.getInfoDialog().getOption();
						TitleScreen.show();
					}
				});
				RegisterScreen.this.regDetails = regDetails;
			}
		};
	}

	@Override
	protected void doTick() {
		if (!isBlocked()) {
			if (!Mouse.isButtonDown(0)) {
				waitForMouse = false;
			}
			emailField.tick();
			boolean wasEnabled = getArea(REGISTER).isEnabled();
			if (wasEnabled != valid) {
				setEnabled(REGISTER, valid);
			}
		}
		if (thingToDo != null) {
			thingToDo.run();
			thingToDo = null;
		}
	}

	/**
	 * Check validity and enable/disable the Register button
	 */
	private boolean checkValid() {
		valid = ValidateUtil.isEmail(emailField.getText());
		return valid;
	}

	@Override
	protected void preRender() {
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	}
}
