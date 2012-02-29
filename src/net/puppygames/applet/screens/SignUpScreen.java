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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.NagState;
import net.puppygames.applet.PrizeFeature;
import net.puppygames.applet.Res;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.widgets.TextField;
import net.puppygames.gamecommerce.shared.GenericServerRemote;
import net.puppygames.gamecommerce.shared.NewsletterIncentive;
import net.puppygames.gamecommerce.shared.ValidateUtil;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.util.CheckOnline;
import com.shavenpuppy.jglib.util.HexDecoder;

import static org.lwjgl.opengl.GL11.*;

/**
 * Special screen for non-Puppygames customers that allows them to join our newsletter for a little prize.
 */
public class SignUpScreen extends Screen {

	private static final long serialVersionUID = 1L;

	/** Singleton */
	private static SignUpScreen instance;

	private static final Object cancelLock = new Object();

	/*
	 * Areas
	 */
	private static final String EMAIL = "email";
	private static final String SIGNUP = "signup";
	private static final String LATER = "later";
	private static final String MESSAGE = "signup-nag";
	private static final String DONTSHOWAGAIN = "dontShowAgain";
	private static final String DONTSHOWAGAIN_ON = "dontShowAgain_on";
	private static final String DONTSHOWAGAIN_OFF = "dontShowAgain_off";

	/*
	 * Layout
	 */
	private MappedColor topColor, bottomColor, color;
	private Rectangle emailInsets;
	private String font;

	private transient TextField emailField;
	private transient boolean valid;
	private transient GLFont fontResource;
	private transient DialogScreen waitDialog;
	private transient Thread signUpThread;
	private transient boolean cancelSignUp;
	private transient Runnable thingToDo;
	private transient boolean waitForMouse;
	private transient TickableObject emailObject;
	private transient PrizeFeature prize;

	/**
	 * C'tor
	 */
	public SignUpScreen(String name) {
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
				requestFocus(getArea(SIGNUP));
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
	public static void show(PrizeFeature prize) {
		if (!instance.isCreated()) {
			try {
				instance.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		instance.prize = prize;
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

		getArea(MESSAGE).setText(prize.getScreenMessage());

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
	}


	@Override
	protected void onClicked(String id) {
		if (waitForMouse) {
			return;
		}
		if (id.equals(EMAIL)) {
			emailField.setEditing(true);
		} else if (id.equals(SIGNUP)) {
			signUp();
		} else if (id.equals(LATER)) {
			close();
		} else if (id.equals(DONTSHOWAGAIN)) {
			NagState nagState = MiniGame.getNagState();
			switch (nagState) {
				case DONT_NAG:
					nagState = NagState.NOT_YET_SHOWN;
					break;
				case NOT_YET_SHOWN:
					nagState = NagState.DONT_NAG;
					break;
				default:
					assert false : "Shouldn't have nag state "+nagState;
					return;
			}
			MiniGame.setNagState(nagState);
			enableButtons();
		}
	}

	private void enableButtons() {
		boolean dontShowAgain = MiniGame.getNagState() == NagState.DONT_NAG;
		setVisible(DONTSHOWAGAIN_ON, 	 dontShowAgain);
		setVisible(DONTSHOWAGAIN_OFF, 	!dontShowAgain);

		getArea(DONTSHOWAGAIN_OFF).init();
		getArea(DONTSHOWAGAIN_ON).init();
	}

	private static void addParam(String param, String data, StringBuilder sb) throws UnsupportedEncodingException {
		if (sb.length() > 0) {
			sb.append('&');
		}
		sb.append(param);
		sb.append('=');
		sb.append(URLEncoder.encode(data, "utf8"));
	}

	private void signUp() {
		if (!checkValid()) {
			return;
		}

		if (!CheckOnline.isOnline()) {
			String msg = Game.getMessage("lwjglapplets.signupscreen.failed_message");
			msg = msg.replace("[title]", Game.getDisplayTitle());
			signUpFailed(msg, true);
		} else {
			waitDialog = net.puppygames.applet.Res.getCancelDialog();
			String msg = Game.getMessage("lwjglapplets.signupscreen.wait_message");
			msg = msg.replace("[title]", Game.getDisplayTitle());
			waitDialog.doModal(Game.getMessage("lwjglapplets.signupscreen.wait_title"), msg,
					new Runnable() {
				@Override
				public void run() {
					int option = waitDialog.getOption();
					// Cancel registration
					if (option != DialogScreen.NONE) {
						waitForMouse = true;
						synchronized (cancelLock) {
							cancelSignUp = true;
							if (signUpThread != null) {
								signUpThread.interrupt();
							}
						}
					}
					waitDialog = null;
				}
			});

			signUpThread = new Thread() {
				@Override
				public void run() {
					try {
						GenericServerRemote server;
						try {
							server = (GenericServerRemote) Naming.lookup(GenericServerRemote.RMI_URL);
						} catch (Exception e) {
							e.printStackTrace(System.err);
							synchronized (cancelLock) {
								if (!cancelSignUp) {
									String msg = Game.getMessage("lwjglapplets.signupscreen.connect_failed_message");
									msg = msg.replace("[email]", Game.getSupportEmail());
									signUpFailed(msg, false);
								}
								cancelSignUp = false;
							}
							return;
						}
						String ret;
						StringBuilder command = new StringBuilder(256);

						try {
							addParam("cmd", "signup", command);
							addParam("email", emailField.getText(), command);
							addParam("installation", String.valueOf(Game.getInstallation()), command);
							addParam("game", Game.getTitle(), command);
							addParam("version", Game.getVersion(), command);
							addParam("prize", prize.getName(), command);
							addParam("message", prize.getEmailMessage(), command);

							System.out.println("Executing remote command "+command);

							ret = server.doCommand(command.toString());
							System.out.println("Remote command result "+ret);
							String result = getParam(ret, "result", "FAILED");
							if ("SUCCESS".equals(result)) {
								String code = getParam(ret, "code", "");
								NewsletterIncentive ni = new NewsletterIncentive(emailField.getText(), Game.getTitle(), Game.getVersion(), Game.getInstallation(), prize.getName());
								ni.setCode(HexDecoder.decode(code));
								signUpSuccess(ni);
							} else if ("FAILED".equals(result)) {
								signUpFailed(getParam(ret, "reason", Game.getMessage("lwjglapplets.signupscreen.unknown_reasons")), true);
							} else {
								throw new Exception("Don't understand server response "+ret);
							}
						} catch (RemoteException e) {
							e.printStackTrace(System.err);
							signUpFailed(e.getMessage(), true);
							return;
						} catch (Throwable e) {
							e.printStackTrace(System.err);
							synchronized (cancelLock) {
								if (!cancelSignUp) {
									Game.onRegistrationDisaster();
									String msg = Game.getMessage("lwjglapplets.signupscreen.signup_failed_message");
									msg = msg.replace("[email]", Game.getSupportEmail());
									signUpFailed(msg, true);
									cancelSignUp = false;
								}
							}
							return;
						}
					} finally {
						synchronized (cancelLock) {
							signUpThread = null;
						}
					}
				}
			};

			signUpThread.start();
		}
	}

	/**
	 * Called when registration fails
	 * @param message The failure message
	 * @param nextPhase The phase to go to next
	 */
	private void signUpFailed(final String message, final boolean fatal) {
		thingToDo = new Runnable() {
			@Override
			public void run() {
				if (waitDialog != null && waitDialog.isOpen()) {
					waitDialog.close();
					waitDialog = null;
				}
				Res.getInfoDialog().doModal
					(
						Game.getMessage("lwjglapplets.signupscreen.failed"),
						Game.getMessage("lwjglapplets.signupscreen.general_failure_message")+"\n\n"+message,
						new Runnable() {
							@Override
							public void run() {
								net.puppygames.applet.Res.getInfoDialog().getOption();
								if (fatal) {
									MiniGame.setNagState(NagState.DONT_NAG);
									MiniGame.showTitleScreen();
								}
								waitForMouse = true;
							}
						}
					);
			}
		};
	}

	/**
	 * Called when signup call succeeds
	 */
	private void signUpSuccess(final NewsletterIncentive ni) {
		thingToDo = new Runnable() {
			@Override
			public void run() {
				Game.onRemoteCallSuccess();
				if (waitDialog != null && waitDialog.isOpen()) {
					waitDialog.close();
					waitDialog = null;
				}

				// Serialize the nag incentive
				FileOutputStream fos = null;
				BufferedOutputStream bos = null;
				ObjectOutputStream oos = null;
				try {
					fos = new FileOutputStream(MiniGame.getIncentiveFile());
					bos = new BufferedOutputStream(fos);
					oos = new ObjectOutputStream(bos);
					oos.writeObject(ni);
				} catch (IOException e) {
					e.printStackTrace(System.err);
				} finally {
					try {
						if (oos != null) {
							oos.flush();
							oos.close();
						}
					} catch (IOException e) {
					}
				}

				MiniGame.setNagState(NagState.PRIZE_AWAITS);
				UnlockBonusScreen.show(ni);
			}
		};
	}

	private static final String getParam(String data, String param, String _default) {
		StringTokenizer st = new StringTokenizer(data, "&", false);
		param += "=";
		while (st.hasMoreTokens()) {
			String t = st.nextToken();
			if (t.startsWith(param)) {
				try {
					return URLDecoder.decode(t.substring(param.length()), "utf8");
				} catch (UnsupportedEncodingException e) {
					System.err.println("Failed to decode "+data);
					e.printStackTrace(System.err);
				}
			}
		}
		return _default;
	}

	@Override
	protected void doTick() {
		if (!isBlocked()) {
			if (!Mouse.isButtonDown(0)) {
				waitForMouse = false;
			}
			emailField.tick();
			boolean wasEnabled = getArea(SIGNUP).isEnabled();
			if (wasEnabled != valid) {
				setEnabled(SIGNUP, valid);
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
