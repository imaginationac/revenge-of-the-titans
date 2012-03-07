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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.puppygames.applet.AppletMessageCheckerRemote;
import net.puppygames.applet.Configuration;
import net.puppygames.applet.Game;
import net.puppygames.applet.MessageReturn;
import net.puppygames.applet.News;
import net.puppygames.applet.PlayerSlot;
import net.puppygames.applet.Res;
import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.FadeEffect;
import net.puppygames.applet.effects.SFX;
import net.puppygames.gamecommerce.shared.RegistrationDetails;

import org.lwjgl.Sys;

import com.shavenpuppy.jglib.util.CheckOnline;

import static org.lwjgl.opengl.GL11.*;


/**
 * $Id: TitleScreen.java,v 1.9 2010/07/09 23:35:54 foo Exp $
 * @author $Author: foo $
 * @version $Revision: 1.9 $
 */
public class TitleScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static final boolean TEST_NEW_VERSION_MESSAGE = true;
	private static final boolean TEST_MESSAGE_LONG = true;
	private static final boolean TEST_REGISTRATION_DETAILS = true;

	private static final int FADE_DELAY = 300;
	private static final int FADE_DURATION = 120;

	protected static final String ID_SLOT = "slot";
	protected static final String ID_VERSION = "version";
	protected static final String ID_REGISTRATION = "registration";

	/** Title screen instance */
	private static TitleScreen instance;

	/*
	 * Resource data
	 */

	/*
	 * Transient data
	 */

	/** Did we check for messages already? */
	private transient boolean messagesChecked;

	/** Did we check for news already? */
	private transient boolean newsChecked;

	/** News */
	private transient List<News> news;

	/** Current news */
	private transient News currentNews;

	/** Pending message */
	private transient MessageReturn pendingMessage;

	/** Did we check for deregistration? */
	private transient boolean deregistrationChecked;

	/** Fade out reg details */
	private transient int tick;

	/** Fading state */
	private transient int fade;
	private static final int FADE_NORMAL = 0;
	private static final int FADE_FADING = 1;
	private static final int FADE_FADED = 2;

	private transient FadeEffect fadeEffect;

	/**
	 * @param name
	 */
	public TitleScreen(String name) {
		super(name);
	}

	@Override
	protected void doRegister() {
		instance = this;
	}

	@Override
	protected void doDeregister() {
		instance = null;
	}

	public static void instantiate() {
		if (!instance.isCreated()) {
			try {
				instance.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * @return Returns the instance.
	 */
	public static TitleScreen getInstance() {
		return instance;
	}

	/**
	 * Show the title screen
	 */
	public static void show() {
		instantiate();
		instance.open();
	}

	@Override
	protected void doTick() {
		if (pendingMessage != null) {
			// Check to see if there's an actual message
			String msg = pendingMessage.getMessage();
			if (msg != null && pendingMessage.getTitle() != null) {
				msg = msg.replaceAll("\\$website", Game.getWebsite());
				msg = msg.replaceAll("\\$download", Game.getDownload());
				msg = msg.replaceAll("\\$contact", Game.getContactURL());
				Res.getInfoDialog().doModal(pendingMessage.getTitle(), msg, new Runnable() {
					@Override
					public void run() {
					}
				});
			}
			// Check to see if the game is being reconfigured
			if (pendingMessage.getReconfigure() != null) {
				try {
					Game.setConfiguration((Configuration) pendingMessage.getReconfigure());
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
			pendingMessage = null;
		}

		if (news != null && news.size() > 0 && currentNews == null) {
			// Check to see if there's an actual message
			currentNews = news.remove(0);
			DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
			Res.getInfoDialog().doModal(df.format(currentNews.getDate().getTime()) + " NEWS", currentNews.getMessage(), new Runnable() {
				@Override
				public void run() {
					if (currentNews.getURL() != null) {
						Sys.openURL(currentNews.getURL());
					}
					currentNews = null;
				}
			});
		}

		switch (fade) {
			case FADE_NORMAL:
				tick++;
				if (tick > FADE_DELAY) {
					tick = 0;
					fade = FADE_FADING;
				}
				break;
			case FADE_FADING:
				tick++;
				if (tick > FADE_DURATION) {
					tick = 0;
					fade = FADE_FADED;
				}
				break;
			default:
				break;
		}

	}

	@Override
	protected void preRender() {
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	}

	@Override
	protected void onClicked(String id) {
		GenericButtonHandler.onClicked(id);
		if (ID_SLOT.equals(id)) {
			SlotScreen.show();
		}
	}

	@Override
	protected void onOpen() {
		GenericButtonHandler.onOpen(this);

		try {
			SFX.createSFX();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		// Show the Buy button if necessary
		doUpdateRegistrationDetails();

		// If we're slot managed but don't yet have a name, prompt for a player name
		if (Game.isSlotManaged() && Game.getPlayerSlot() == null) {
			EnterNameDialog.show(false, new Runnable() {
				@Override
				public void run() {

				}
			});
		}

		// Possibly check for a message
		new Thread() {
            @Override
			public void run() {
				if (Game.getDontCheckMessages() || !Game.isRemoteCallAllowed() || !CheckOnline.isOnline()) {
					// Won't be allowed to connect
					return;
				}
				if (messagesChecked && newsChecked && (deregistrationChecked || Game.isRegistered())) {
					// Already checked everything
					return;
				}
				try {
					if (Game.DEBUG && TEST_NEW_VERSION_MESSAGE) {
						messagesChecked = true;
						if (TEST_MESSAGE_LONG) {
	                        pendingMessage = new MessageReturn("TEST LONG MESSAGE", "Unfortunately, newsletter signup failed for the following reason:\n\n There has been a problem connecting to the Puppygames newsletter server. Please ensure you are online and try again, or alternatively contact us on contact@humblebundle.com for assistance.", null);
                        } else {
	                        pendingMessage = new MessageReturn("TEST MESSAGE", "This is an example message returned from the message service\nThis is a test of Q, ß and ç æ ø and ö\nTest of ö and ÑÀÁÂÃÄÅ working with leading.", null);
                        }
					}
					AppletMessageCheckerRemote amcr = (AppletMessageCheckerRemote) Naming.lookup("//"+AppletMessageCheckerRemote.REMOTE_HOST+"/"+AppletMessageCheckerRemote.REMOTE_NAME);
					if (!messagesChecked && pendingMessage == null) {
						messagesChecked = true;
						pendingMessage = amcr.checkForMessages(Game.getTitle(), Game.getVersion(), Game.getGame().getMessageSequence(), Game.getConfiguration());
					}
					try {
						if (!newsChecked && news == null) {
							newsChecked = true;
							DateFormat df = new SimpleDateFormat("yyyyMMMdd");
							String lastCheckedString = Game.getGlobalPreferences().get("lastnewscheck", "");
							Date lastCheckedDate = null;
							try {
								lastCheckedDate = df.parse(lastCheckedString);
							} catch (Exception e) {
								lastCheckedDate = new Date();
							}
							Calendar dayToCheckFrom = Calendar.getInstance();
							dayToCheckFrom.setTime(lastCheckedDate);
							news = amcr.getNews(dayToCheckFrom);
							dayToCheckFrom.setLenient(true);
							dayToCheckFrom.add(Calendar.DAY_OF_YEAR, 1);
							Game.getGlobalPreferences().put("lastnewscheck", df.format(dayToCheckFrom.getTime()));
						}
					} catch (Throwable e) {
						e.printStackTrace(System.err);
					}
					try {
						if (!deregistrationChecked && Game.isRegistered() && Game.getRegistrationDetails() != null) {
							deregistrationChecked = true;
							boolean stillRegistered = amcr.checkRegistrationValid(Game.getRegistrationDetails());

							if (!stillRegistered) {
								System.out.println("Registration is invalidated.");
								Game.setRegistrationDetails(null);
								updateRegistrationDetails();
							}
						}
					} catch (Throwable e) {
						e.printStackTrace(System.err);
					}
				} catch (Throwable e) {
					e.printStackTrace(System.err);
				}
			}
		}.start();
	}

	/**
	 * Update registration details
	 */
	public static void updateRegistrationDetails() {
		instantiate();
		instance.doUpdateRegistrationDetails();
	}

	/**
	 * Update slot details
	 */
	public static void updateSlotDetails() {
		instantiate();
		instance.doUpdateSlotDetails();
	}

	private void doUpdateSlotDetails() {
		if (!Game.isSlotManaged()) {
			return;
		}

		PlayerSlot slot = Game.getPlayerSlot();
		if (slot == null) {
			getArea(getSlotID()).setText(Game.getMessage("lwjglapplets.titlescreen.entername"));
		} else {
			String msg = Game.getMessage("lwjglapplets.titlescreen.welcome");
			msg = msg.replace("[name]", slot.getName().toUpperCase());
			getArea(getSlotID()).setText(msg);
		}
	}

	@SuppressWarnings("unused")
    private void doUpdateRegistrationDetails() {
		tick = 0;
		fade = FADE_NORMAL;

		if (fadeEffect != null) {
			fadeEffect.remove();
			fadeEffect = null;
		}
		fadeEffect = new FadeEffect(FADE_DELAY, FADE_DURATION) {
			@Override
            protected void onTicked() {
				getArea(getRegistrationID()).setTextAlpha(getAlpha());
			}
		};
		fadeEffect.spawn(this);

		if (Game.DEBUG && TEST_REGISTRATION_DETAILS) {
			getArea(getRegistrationID()).setText("Registered to Wing Commander Testy 'McTesticles' McTestington (someone.with.a.long.email.address@puppygames.net)");
		} else {
			RegistrationDetails regDetails = Game.getRegistrationDetails();
			if (regDetails == null) {
				getArea(getRegistrationID()).setText("");
			} else {
				boolean nameOnly = Game.getLocalPreferences().getBoolean("nameOnly", false);
				getArea(getRegistrationID()).setText(regDetails.toTitleScreen(nameOnly).toUpperCase());
				Game.getLocalPreferences().putBoolean("nameOnly", true);
			}
		}

		if (Game.isRegistered()) {
			getArea(getVersionID()).setText("v" + Game.getVersion());
		} else {
			getArea(getVersionID()).setText("v" + Game.getVersion() + " DEMO");
		}

		updateControls();
	}

	/**
	 * Updates the visibility of the BUY and MOREGAMES buttons depending on whether the game is registered or not
	 */
	protected void updateControls() {
		if (getArea(GenericButtons.BUY) != null) {
			getArea(GenericButtons.BUY).setEnabled(!Game.isRegistered());
		}
		//if (getArea(GenericButtons.MOREGAMES) != null) {
		//	getArea(GenericButtons.MOREGAMES).setVisible(!Game.isRegistered());
		//}
	}

	/**
	 * @return the ID of the current slot area
	 */
	public String getSlotID() {
		return ID_SLOT;
	}

	/**
	 * @return the ID of the version area
	 */
	public String getVersionID() {
		return ID_VERSION;
	}

	/**
	 * @return the ID of the registration area
	 */
	public String getRegistrationID() {
		return ID_REGISTRATION;
	}
}
