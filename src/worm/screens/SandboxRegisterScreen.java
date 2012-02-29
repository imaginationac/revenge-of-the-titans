package worm.screens;

import static org.lwjgl.opengl.GL11.glClearColor;

import java.rmi.Naming;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.Res;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.screens.DialogScreen;
import net.puppygames.applet.screens.RegisterScreen;
import net.puppygames.applet.screens.TitleScreen;
import net.puppygames.applet.widgets.TextField;
import net.puppygames.gamecommerce.shared.RegisterException;
import net.puppygames.gamecommerce.shared.RegistrationDetails;
import net.puppygames.gamecommerce.shared.RegistrationServerRemote;
import net.puppygames.gamecommerce.shared.ValidateUtil;

import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Rectangle;

import worm.SFX;

import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.util.CheckOnline;

public class SandboxRegisterScreen extends RegisterScreen {

	private static final long serialVersionUID = 1L;

	/** Default instance */
	private static RegisterScreen instance;

	private static final Object CANCEL_LOCK = new Object();

	/*
	 * Button IDs
	 */
	protected static final String EMAIL = "email";
	protected static final String REGISTER = "register";
	protected static final String LATER = "later";
	protected static final String ORDER = "order";

	/*
	 * Layout
	 */
	protected MappedColor topColor;

	protected MappedColor bottomColor;

	protected MappedColor color;
	private Rectangle emailInsets;
	private String font;

	/** Internal name of product we're registering. Defaults to "Sandbox Mode" */
	private String product;

	/** Display name of product we're registering. Defaults to "Sandbox Mode" */
	private String displayProduct;

	private transient TextField emailField;
	private transient boolean valid;
	protected transient GLFont fontResource;
	private transient DialogScreen waitDialog;
	private transient Thread registerThread;
	private transient boolean cancelRegistration;
	private transient Runnable thingToDo;
	private transient boolean waitForMouse;
	private transient TickableObject emailObject;

	/**
	 * C'tor
	 */
	public SandboxRegisterScreen(String name) {
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
		emailField.setEditing(true);
		waitForMouse = true;
	
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
			product = "Sandbox Mode";
		}

		if (displayProduct == null) {
			displayProduct = "Sandbox Mode";
		}

		checkValid();
		setEnabled(REGISTER, valid);
	
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
			close();
			SandboxNagScreen.show();
		} else if (id.equals(ORDER)) {
			SandboxRegisterScreen.buySandboxMode();
		}
	}

	protected void registration() {
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
				SandboxRegisterScreen.buySandboxMode();
			}
		};
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

	/**
     * Buy the game
     */
    public static void buySandboxMode() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					String page = "https://secure.bmtmicro.com/servlets/Orders.ShoppingCart?CID=1899&PRODUCTID=18990030";
					if (!Sys.openURL(page)) {
						throw new Exception("Failed to open URL "+page);
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
					Game.alert("Please open your web browser on the page http://" + Game.getWebsite());
				}
			}
		});
		Game.exit();
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
	protected boolean checkValid() {
		valid = ValidateUtil.isEmail(emailField.getText());
		return valid;
	}

	@Override
	protected void preRender() {
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	}
}
