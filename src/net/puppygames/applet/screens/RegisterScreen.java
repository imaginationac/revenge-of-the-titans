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

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.Res;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.widgets.TextField;
import net.puppygames.gamecommerce.shared.RegisterException;
import net.puppygames.gamecommerce.shared.RegistrationDetails;
import net.puppygames.gamecommerce.shared.RegistrationServerRemote;
import net.puppygames.gamecommerce.shared.ValidateUtil;

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

	private static final long serialVersionUID = 1L;

	/** Default instance */
	private static RegisterScreen instance;

	private static final Object CANCEL_LOCK = new Object();

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

	/** Internal name of product we're registering. Defaults to {@link Game#getTitle()} */
	private String product;

	/** Display name of product we're registering. Defaults to {@link Game#getDisplayTitle()} */
	private String displayProduct;

	private transient TextField emailField;
	private transient boolean valid;
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

	/**
	 * Sets the "wait for mouse" flag, which causes the UI to wait until the mouse is released before responding
	 * @param waitForMouse
	 */
	public void setWaitForMouse(boolean waitForMouse) {
	    this.waitForMouse = waitForMouse;
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
		if (product == null) {
			assert instance == null;
			instance = this;
		}
	}

	@Override
	protected void doDeregister() {
		if (product == null) {
			assert instance != null;
			instance = null;
		}
	}

	/**
	 * Show the default registration screen
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
		MiniGame.buy(false);
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

		if (Game.isRegistered()) {
			emailField.setText(Game.getRegistrationDetails().getEmail());
		}

		if (product == null) {
			product = Game.getTitle();
		}

		if (displayProduct == null) {
			displayProduct = Game.getDisplayTitle();
		}

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
			MiniGame.buy(true);
		}
	}

	private void registration() {
		if (!checkValid()) {
			return;
		}

		if (!CheckOnline.isOnline()) {
			String failure = Game.getMessage("lwjglapplets.registerscreen.notconnected");
			failure = failure.replace("[title]", displayProduct);
			setThingToDo(registrationFailed(failure, true));
		} else {
			waitDialog = Res.getCancelDialog();
			String msg = Game.getMessage("lwjglapplets.registerscreen.registering_message");
			msg = msg.replace("[title]", product);
			waitDialog.doModal(Game.getMessage("lwjglapplets.registerscreen.registering"), msg,
					new Runnable() {
				@Override
				public void run() {
					int option = waitDialog.getOption();
					// Cancel registration
					if (option != DialogScreen.NONE) {
						waitForMouse = true;
						synchronized (CANCEL_LOCK) {
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
							synchronized (CANCEL_LOCK) {
								if (!cancelRegistration) {
									Game.onRegistrationDisaster();
									String msg = Game.getMessage("lwjglapplets.registerscreen.problem_connecting");
									msg = msg.replace("[email]", Game.getSupportEmail());
									setThingToDo(registrationFailed(msg, false));
								}
								cancelRegistration = false;
							}
							return;
						}
						RegistrationDetails regDetails;
						try {
							regDetails = server.register(emailField.getText(), product, Game.getVersion(), Game.getInstallation(), System
									.getProperty("os.name"), Game.getConfiguration());
							Game.onRegistrationRecovery();
							setThingToDo(registrationSuccess(regDetails));
						} catch (RegisterException e) {
							Game.onRegistrationRecovery();
							setThingToDo(registrationFailed(e.getMessage(), true));
							return;
						} catch (Throwable e) {
							e.printStackTrace(System.err);
							synchronized (CANCEL_LOCK) {
								if (!cancelRegistration) {
									Game.onRegistrationDisaster();
									String msg = Game.getMessage("lwjglapplets.registerscreen.problem_registering");
									msg = msg.replace("[email]", Game.getSupportEmail());
									setThingToDo(registrationFailed(msg, true));
									cancelRegistration = false;
								}
							}
							return;
						}
					} finally {
						synchronized (CANCEL_LOCK) {
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
	 * @param fatal Whether the error was fatal, which should dump us back to the title screen
	 */
	protected Runnable registrationFailed(final String message, final boolean fatal) {
		return new Runnable() {
			@Override
			public void run() {
				if (waitDialog != null && waitDialog.isOpen()) {
					waitDialog.close();
					waitDialog = null;
				}
				Res.getInfoDialog().doModal(Game.getMessage("lwjglapplets.registerscreen.failed"), message, new Runnable() {
					@Override
					public void run() {
						Res.getInfoDialog().getOption();
						if (fatal) {
							TitleScreen.show();
						}
						waitForMouse = true;
					}
				});
				MiniGame.buy(false);
			}
		};
	}

	/**
	 * Clears away the "wait..." dialog, if it's open
	 */
	protected final void maybeClearWaitDialog() {
		if (waitDialog != null && waitDialog.isOpen()) {
			waitDialog.close();
			waitDialog = null;
		}
	}

	/**
	 * Called when registration succeeds
	 * @param regDetails The registration details
	 * @return a Runnable which will be executed in the main thread immediately
	 */
	protected Runnable registrationSuccess(final RegistrationDetails regDetails) {
		return new Runnable() {
			@Override
			public void run() {
				MiniGame.clearBuy();
				Game.onRemoteCallSuccess();
				TitleScreen.instantiate();
				Game.setRegistrationDetails(regDetails);
				maybeClearWaitDialog();

				String msg = Game.getMessage("lwjglapplets.registerscreen.success_message");
				msg = msg.replace("[title]", displayProduct);
				Res.getInfoDialog().doModal(Game.getMessage("lwjglapplets.registerscreen.success"), msg, new Runnable() {
					@Override
					public void run() {
						Res.getInfoDialog().getOption();
						TitleScreen.show();
					}
				});
			}
		};
	}

	private synchronized void setThingToDo(Runnable thingToDo) {
	    this.thingToDo = thingToDo;
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

		synchronized (this) {
			if (thingToDo != null) {
				thingToDo.run();
				thingToDo = null;
			}
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
