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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.rmi.Naming;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.screens.BindingsScreen;
import net.puppygames.applet.screens.TitleScreen;
import net.puppygames.gamecommerce.shared.GameInfo;
import net.puppygames.gamecommerce.shared.GameInfoServerRemote;
import net.puppygames.gamecommerce.shared.RegistrationDetails;
import net.puppygames.steam.Steam;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.Rectangle;
import org.lwjgl.util.Timer;

import com.shavenpuppy.jglib.IResource;
import com.shavenpuppy.jglib.Image;
import com.shavenpuppy.jglib.Image.JPEGDecompressor;
import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.Wave;
import com.shavenpuppy.jglib.jpeg.JPEGDecoder;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.openal.ALStream;
import com.shavenpuppy.jglib.resources.ClassLoaderResource;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.DynamicResource;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.ResourceConverter;
import com.shavenpuppy.jglib.resources.ResourceLoadedListener;
import com.shavenpuppy.jglib.resources.StringArray;
import com.shavenpuppy.jglib.resources.TextResource;
import com.shavenpuppy.jglib.resources.TextWrapper;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sound.SoundPlayer;
import com.shavenpuppy.jglib.sprites.SoundCommand;
import com.shavenpuppy.jglib.util.CheckOnline;
import com.shavenpuppy.jglib.util.Util;

import static org.lwjgl.opengl.GL11.*;

import static org.lwjgl.openal.AL10.*;

/**
 * The main Game class
 */
public abstract class Game extends Feature {

	/*
	 * Static game data
	 */

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_GAME_RESOURCE_NAME = "game.puppygames";
	private static final String DEFAULT_ROAMING_PREFS_FILENAME = "prefs.xml";

	public static final boolean DEBUG = false;
	private static final boolean REGISTERED = false;
	private static final boolean FORCEUSELOG = false;
	private static final boolean TESTREGISTER = false;

	/** Keydown state tracking */
	private static final boolean[] KEYDOWN = new boolean[Keyboard.KEYBOARD_SIZE];
	private static final boolean[] KEYWASDOWN = new boolean[Keyboard.KEYBOARD_SIZE];

	/** Mouse events */
	private static final List<MouseEvent> MOUSEEVENTS = new ArrayList<MouseEvent>();

	/** Restore filename */
	protected static final String RESTORE_FILE = "restore.dat";

	/** Force run even when not focused */
	private static boolean alwaysRun;

	/** Pause enabled */
	private static boolean pauseEnabled = true;

	/** Sound effects enabled */
	private static boolean sfxEnabled = true;

	/** Music enabled */
	private static boolean musicEnabled = true;

	/** Paused mode */
	private static boolean paused;

	/** Finished flag */
	private static boolean finished;

	/** Initialised flag */
	private static boolean initialised;

	/** Arguments passed in main() */
	private static Properties properties;

	/** Registration details */
	private static RegistrationDetails registrationDetails;

	/** Registered flag */
	private static boolean registered;

	/** Unique installation number */
	protected static long installation;

	/** Global Puppy Games preferences */
	private static Preferences GLOBALPREFS;

	/** Local Puppy Games preferences */
	private static Preferences LOCALPREFS;

	/** Roaming game preferences */
	private static Preferences ROAMINGPREFS;

	/** Configuration */
	protected static Configuration configuration;

	/** Singleton */
	private static Game game;

	/** Game info, for logging */
	private static GameInfo gameInfo;

	/** Sound player */
	private static SoundPlayer soundPlayer;

	/** All sound players */
	private static List<SoundPlayer> soundPlayers;

	/** Music */
	private static SoundEffect music;

	/** Roaming files directory prefix */
	private static String roamingDirPrefix;

	/** Local files directory prefix */
	private static String localDirPrefix;

	/** Stash the local log here */
	private static String GAMEINFO_FILE;

	/** Music volume 0..100 */
	private static int musicVolume;

	/** Effects volume 0..100 */
	private static int sfxVolume;

	/** Display bounds */
	private static Rectangle viewPort;

	/** Game title */
	private static String title;

	/** Version */
	private static String version;

	/** Internal version, used for local settings */
	private static String internalVersion;

	/** Steam App ID */
	private static int appID;

	/** Current player slot */
	private static PlayerSlot playerSlot;

	/** Viewport offset */
	private static int viewportXoffset, viewportYoffset, viewportWidth, viewportHeight;

	/** Force sleep instead of yield */
	private static boolean forceSleep;

	/** Modded? */
	private static boolean modded;

	/** Mod name */
	private static String modName;

	/** Frame mouse visibility */
	private static boolean mouseVisible = true;

	/** Current FPS */
	private static final int[] FPS = new int[60];
	private static int fps = 0, currentFPS = 60;

	static {
		Image.setDecompressor(new JPEGDecompressor() {
			@Override
			public void decompress(ByteBuffer src, ByteBuffer dest) throws Exception {
				byte[] data = new byte[src.capacity()];
				src.get(data);
				ByteArrayInputStream bais = new ByteArrayInputStream(data);
				Image img = JPEGDecoder.loadFromByteStream(bais);
				dest.put(img.getData());
				img.dispose();
				dest.flip();
			}
		});
	}

	/*
	 * Feature data
	 */

	/** Display title */
	private String displayTitle;

	/** Game dimensions */
	private int width, height;

	/** Framerate */
	private int frameRate;

	/** Message sequence number */
	private int messageSequence;

	/** Support email */
	@Data
	private String supportEmail = "support@puppygames.net";

	/** Support url */
	@Data
	private String supportURL = "www.puppygames.net/support/support.php";

	/** Contact url */
	@Data
	private String contactURL = "www.puppygames.net/contact.php";

	/** Website */
	@Data
	private String website = "www.puppygames.net";

	/** Download URL */
	@Data
	private String download = "www.puppygames.net";

	/** Buy url */
	@Data
	private String buyURL = "";

	/** More Games url */
	@Data
	private String moreGamesURL = "www.puppygames.net";

	/** Splash screen name (NOT mirrored by a transient Splash) */
	@Data
	private String splash = "splash.screen";

	/** Window Icon */
	@Data
	private String icon;

	/** Default to fullscreen */
	private int defaultFullscreen = 0;

	/** Don't check for messages? */
	private boolean dontCheckMessages;

	/** Slot management */
	private boolean useSlotManagement;

	/** Sound voices */
	private int soundVoices = 32;

	/** GUI scale in pixels */
	private int scale;

	/** Don't scale the game */
	private boolean dontScale;

	/** Preregistered? */
	private boolean preregistered;

	/** Preregistered to (optional) */
	@Data
	private String preregisteredTo;

	/** Preregistered until this date, then it turns back into a demo (optional). Format is yyyy-mm-dd */
	@Data
	private String preregisteredUntil;

	/** Locale locked: game only preregistered in this locale (optional) */
	@Data
	private String preregisteredLocale;

	/** Keyboard locked: game only preregistered in this locale (optional) */
	@Data
	private String preregisteredLanguage;

	/** Language 2-char code (en, de, etc) */
	@Data
	private String language = "en"; // Defaults to English

	/** Default input mode */
	private String defaultInputMode = InputDeviceType.DESKTOP.name();

	/*
	 * Transient data
	 */

	private transient int panic;

	private transient boolean wasGrabbed;

	private transient int logicalWidth, logicalHeight;

	private transient boolean catchUp;

	private transient float masterGain, targetMasterGain;

	/** Current input mode */
	private transient InputDeviceType inputMode;

	/** Prefs saver */
	private static PrefsSaverThread prefsSaver;

	public static class PrefsSaverThread extends Thread {

		boolean threadFinished;
		boolean triggered;

		/**
		 * C'tor
		 */
		public PrefsSaverThread() {
			super("Prefs Saver Thread");
			setPriority(NORM_PRIORITY - 1);
		}

		public synchronized void save() {
			triggered = true;
			notifyAll();
		}

		synchronized void finish() {
			threadFinished = true;
			save();
			try {
				join();
			} catch (InterruptedException e) {
				e.printStackTrace(System.err);
			}
		}

		@Override
		public void run() {
			while (!threadFinished) {
				synchronized (this) {
					// Wait for first notification...
					try {
						wait();
					} catch (InterruptedException e) {
					}

					// Now wait for 200ms to elapse before finally flushing
					while (triggered) {
						triggered = false;
						try {
							wait(200L);
							if (!triggered) {
								doFlushPrefs();
							}
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}
	}

	/**
	 * C'tor
	 */
	public Game(String name) {
		super(name);
	}

	/**
	 * Enable / disable pausing
	 */
	public static void setPauseEnabled(boolean pauseEnabled) {
		Game.pauseEnabled = pauseEnabled;
		if (paused && !pauseEnabled) {
			paused = false;
		}
	}

	/**
	 * @return Returns the game.
	 */
    public static Game getGame() {
		return game;
	}

	/**
	 * @return Returns the game width (logical units).
	 */
	public static int getWidth() {
		return game.logicalWidth;
	}

	public static void setSize(int width, int height) {
		int oldWidth = game.width;
		int oldHeight = game.height;
		game.width = width;
		game.height = height;
		try {
			setFullscreen(false);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			game.width = oldWidth;
			game.height = oldHeight;
		}
	}

	/**
	 * @return Returns the game height (logical units).
	 */
	public static int getHeight() {
		return game.logicalHeight;
	}

	/**
	 * @return Returns the frameRate.
	 */
	public static int getFrameRate() {
		return game.frameRate;
	}

	/**
	 * @return Returns the title.
	 */
	public static String getTitle() {
		return title;
	}

	/**
	 * @return Returns the display title.
	 */
	public static String getDisplayTitle() {
		return game.displayTitle;
	}

	/**
	 * @return Returns the version.
	 */
	public static String getVersion() {
		return version;
	}

	/**
	 * @return the internal version number
	 */
	public static String getInternalVersion() {
		return internalVersion;
	}

	/**
	 * @return Returns the website URL, minus the schema (default www.puppygames.net)
	 */
	public static String getWebsite() {
		return game.website;
	}

	/**
	 * @return the URL for more games
	 */
	public static String getMoreGamesURL() {
	    return game.moreGamesURL;
    }

	/**
	 * @return the download URL
	 */
	public static String getDownload() {
		return game.download;
	}

	/**
	 * @return Returns the support website URL, minus the schema (default www.puppygames.net/support.php)
	 */
	public static String getSupportURL() {
		return game.supportURL;
	}

	/**
	 * Returns the support email address (default support@puppygames.net) Will return "", if the contact URL is to be used instead
	 * @return String
	 */
	public static String getSupportEmail() {
		return game.supportEmail;
	}

	/**
	 * @return Returns the support contact URL (default null)
	 */
	public static String getContactURL() {
		return game.contactURL;
	}

	/**
	 * @return Returns the URL from where you can buy this game
	 */
	public static String getBuyURL() {
	    return game.buyURL;
    }

	/**
	 * @return Returns the configuration.
	 */
	public static Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * @return Returns the gameInfo
	 */
	public static GameInfo getGameInfo() {
		return gameInfo;
	}

	/**
	 * @return Returns the installation.
	 */
	public static long getInstallation() {
		return installation;
	}

	/**
	 * @return Returns the global Puppygames preferences.
	 */
	public static Preferences getGlobalPreferences() {
		return GLOBALPREFS;
	}

	/**
	 * @return Returns the local Puppygames preferences.
	 */
	public static Preferences getLocalPreferences() {
		return LOCALPREFS;
	}

	/**
	 * @return Returns the roaming game preferences.
	 */
	public static Preferences getRoamingPreferences() {
		return ROAMINGPREFS;
	}

	/**
	 * @return Returns the registrationDetails.
	 */
	public static RegistrationDetails getRegistrationDetails() {
		return registrationDetails;
	}

	/**
	 * @return Returns the finished.
	 */
	public static boolean isFinished() {
		return finished;
	}

	/**
	 * @return Returns true if the game is paused.
	 */
	public static boolean isPaused() {
		return paused;
	}

	/**
	 * @return Returns true if the game is registered
	 */
	public static boolean isRegistered() {
		return REGISTERED || registered || game.preregistered;
	}

	/**
	 * Redirect a PrintStream to a file
	 */
	private static PrintStream redirectOutput(final PrintStream output, String fileName) throws FileNotFoundException {
		if (fileName == null || fileName.equals("")) {
			return output;
		}

		boolean append = true;
		File outFile = new File(localDirPrefix + File.separator + fileName);
		if (outFile.exists()) {
			if (outFile.length() > 65535) {
				outFile.renameTo(new File(localDirPrefix + File.separator + fileName + ".old"));
				append = false;
			}
		}
		final FileOutputStream fos = new FileOutputStream(outFile, append);
		OutputStream os = new OutputStream() {
			boolean wroteDate;

			@Override
			public void write(int b) throws IOException {
				if (!wroteDate) {
					wroteDate = true;
					write(new Date().toString());
					write("\t");
				}
				output.write(b);
				fos.write(b);
				if (b == '\n') {
					flush();
					output.flush();
					wroteDate = false;
				}
			}

			private void write(String s) throws IOException {
				int len = s.length();
				for (int i = 0; i < len; i ++) {
					write(s.charAt(i));
				}
			}
		};
		return new PrintStream(os);

	}

	private static void badDrivers() {
		String message = ((TextWrapper) Resources.get("lwjglapplets.game.baddrivers")).getText().replace("[title]", getDisplayTitle());
		Game.alert(message);
		Support.doSupport("opengl");
		exit();
	}

	/**
	 * Initialise the game. This must be called <strong>outside</strong> of the AWT thread!
	 * @param resourcesStream An InputStream for reading in a compiled resource data file, created by the JGLIB ResourceConverter
	 * tool.
	 * @throws Exception if the game fails to initialise correctly
	 */
	public static void init(Properties properties, InputStream resourcesStream) throws Exception {
		if (initialised) {
			return;
		}
		initialised = true;
		finished = false;

		// Timer hack
		@SuppressWarnings("unused")
		Thread timerHack = new Thread() {
			{
				this.setDaemon(true);
				this.start();
			}

			@Override
            public void run() {
				while (true) {
					try {
						Thread.sleep(Integer.MAX_VALUE);
					} catch (InterruptedException ex) {
					}
				}
			}
		};

		Game.properties = properties;

		// Load game resource metadata.
		Resources.load(resourcesStream);

		// Get the game's title & version directly from a text resource
		Game.title = ((TextResource) Resources.get("title")).getText().trim();
		Game.version = ((TextResource) Resources.get("version")).getText().trim();
		TextResource iv = (TextResource) Resources.peek("internalVersion");
		if (iv != null) {
			Game.internalVersion = iv.getText().trim();
		} else {
			Game.internalVersion = Game.version;
		}
		TextResource steamID = ((TextResource) Resources.peek("steam_app_id"));
		if (steamID != null) {
			Game.appID = Integer.parseInt(steamID.getText().trim());
		} else {
			Game.appID = 0;
		}

		// Find the game we want
		String gameResource;
		try {
			gameResource = System.getProperty("net.puppygames.applet.Game.gameResource", properties.getProperty("gameresource", DEFAULT_GAME_RESOURCE_NAME));
		} catch (SecurityException e) {
			e.printStackTrace(System.err);
			gameResource = DEFAULT_GAME_RESOURCE_NAME;
		}

		System.out.println("Game resource: "+gameResource);
		game = (Game) Resources.peek(gameResource);
		if (game.displayTitle == null) {
			game.displayTitle = title;
		}
		System.out.println(new Date() + " Game: " + title + " " + version + " ["+internalVersion+"]");

		// Maybe init Steam
		if (isUsingSteam()) {
			try {
				initSteam();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				String message = ((TextWrapper) Resources.get("lwjglapplets.game.steamerror")).getText().replace("[title]", getTitle()); // Can't use display title yet
				Game.alert(message);
				exit();
			}
		}

		// Initialise local filesystem
		initFiles();

		if (FORCEUSELOG || !DEBUG) {
			try {
				System.setOut(redirectOutput(System.out, properties.getProperty("out", "out.log")));
				System.setErr(redirectOutput(System.err, properties.getProperty("err", "err.log")));
			} catch (FileNotFoundException e) {
				// Ignore
				if (DEBUG) {
					e.printStackTrace(System.err);
				}
			}
		}

		// Create preferences and determine / generate installation number
		GLOBALPREFS = Preferences.userNodeForPackage(Game.class);
		installation = GLOBALPREFS.getLong("installation", 0);
		if (installation == 0L) {
			installation = (long) (Math.random() * Long.MAX_VALUE);
			GLOBALPREFS.putLong("installation", installation);
		}
		System.out.println("Serial " + installation);

		// Local prefs are just that: local
		LOCALPREFS = Preferences.userNodeForPackage(Game.class).node(title+" Local");


		// Load roaming prefs from backup file
		ROAMINGPREFS = Preferences.userNodeForPackage(Game.class).node(title);
		GameInputStream gis = null;
		RoamingFile prefsFile = new RoamingFile(getRoamingPrefsFileName());
		boolean zapPrefs = false, backupDone = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024);
		if (prefsFile.exists()) {
			// Back up the prefs before we attempt to import it
			writePrefs(baos);
			backupDone = true;
			try {
				gis = new GameInputStream(getRoamingPrefsFileName());
				Preferences.importPreferences(gis);
				if (DEBUG) {
					System.out.println("Loaded preferences file "+getRoamingPrefsFileName());
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
				zapPrefs = true;
			} finally {
				if (gis != null) {
					try {
						gis.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			System.out.println("Preferences file "+getRoamingPrefsFileName()+" does not exist.");
			zapPrefs = true;
		}
		if (zapPrefs) {
			try {
				ROAMINGPREFS.removeNode();
				ROAMINGPREFS.flush();
				ROAMINGPREFS = null;
				ROAMINGPREFS = Preferences.userNodeForPackage(Game.class).node(title);
				// Restore backup
				if (backupDone) {
					Preferences.importPreferences(new ByteArrayInputStream(baos.toByteArray()));
				}
				doFlushPrefs();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				// Carry on anyway
			}
		}

		prefsSaver = new PrefsSaverThread();
		prefsSaver.start();


		// Load or create configuration
		byte[] cfg = LOCALPREFS.getByteArray("configuration", null);
		if (cfg == null) {
			// No configuration present so create a new one
			createConfiguration();
		} else {
			ByteArrayInputStream bais = new ByteArrayInputStream(cfg);
			ObjectInputStream ois = new ObjectInputStream(bais);
			try {
				configuration = (Configuration) ois.readObject();
			} catch (Exception e) {
				// Corrupted, so create a new one
				createConfiguration();
			} finally {
				ois.close();
			}
		}

		if (DEBUG) {
			System.out.println("Proxy host: " + System.getProperty("http.proxyHost"));
		}

		// Check registration details
		checkRegistration();

		// Check for bad exit last time (only caused by VM crashes or machine crashes)
		boolean wasBadExit = LOCALPREFS.getBoolean("badexit", false);

		// Initialise a gameinfo object so we can log things
		gameInfo = new GameInfo(getTitle(), getVersion(), getInstallation(), wasBadExit, org.lwjgl.opengl.Display.getAdapter(), org.lwjgl.opengl.Display.getVersion(), configuration.encode());

		System.out.println("Starting " + getTitle() + " " + getVersion()+" (language: "+game.language+")");

		// If there was a bad exit, let's put up a message
		if (wasBadExit && !DEBUG) {
			String shutdownMessage;
			if ("".equals(getSupportEmail())) {
				shutdownMessage = ((TextWrapper) Resources.get("lwjglapplets.game.badexit.url.message")).getText().replace("[url]", getSupportURL());
			} else {
				shutdownMessage = ((TextWrapper) Resources.get("lwjglapplets.game.badexit.email.message")).getText().replace("[email]", getSupportEmail());
			}
			shutdownMessage = shutdownMessage.replace("[title]", getTitle());
			Game.alert(shutdownMessage);
			Support.doSupport("crash");
		}

		// Set the bad exit flag. The only way to clear it is to exit the game
		// cleanly via the exit() method.
		LOCALPREFS.putBoolean("badexit", true);
		flushPrefs();

		// Load DLC
		loadDLC();

		// Load mod if any is specified
		loadMods();

		DynamicResource.createAll();

		// Initialise sound
		initSound();

		// Gamepads... have to init first for LWJGL bug.
		try {
			Controllers.create();
		} catch (Exception e) {
			if (DEBUG) {
				System.err.println("No gamepads or joysticks enabled due to " + e);
			}
		}

		// Initialise display
		try {
			initDisplay();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			gameInfo.setException(e);
			badDrivers();
		}

		// Update gameinfo
		try {
			String glvendor = glGetString(GL_VENDOR);
			String glrenderer = glGetString(GL_RENDERER);
			String glversion = glGetString(GL_VERSION);
			String gldriver = null;
			int i = glversion.indexOf(' ');
			if (i != -1) {
				gldriver = glversion.substring(i + 1);
				glversion = glversion.substring(0, i);
			}
			gameInfo.update(glvendor, glrenderer, glversion, gldriver, registrationDetails);
			System.out.println("GL Vendor "+glvendor+", GL Renderer "+glrenderer+", GL Version "+glversion+", "+(gldriver != null ? "GL Driver "+gldriver : ""));
		} catch (Exception e) {
			e.printStackTrace(System.err);
			badDrivers();
			return;
		}

		// Show the splash screen
		//Display.setVSyncEnabled(false);
		Splash splashInstance = Resources.peek(game.splash);
		if (splashInstance != null) {
			try {
				splashInstance.create();
				Resources.setCreatingCallback(splashInstance);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				splashInstance = null;
				badDrivers();
				return;
			}
		}

		// Autocreate features
		SFX.createSFX();
		try {
			Res.createResources();
			Feature.autoCreate();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			gameInfo.setException(e);
			badDrivers();
			return;
		}

		// We're in Run Mode now
		Resources.setRunMode(true);

		// Force sleep?
		forceSleep = properties.getProperty("sleep", Runtime.getRuntime().availableProcessors() > 1 ? "false" : "true").equalsIgnoreCase("true");

		// Finally, create the game
		game.create();

		// Init input mode
		initInput();

		// Load keyboard bindings
		loadBindings();

		// That's enough of the loading screen...
		if (splashInstance != null) {
			Resources.setCreatingCallback(null);
			splashInstance.destroy();
			splashInstance = null;
		}

		initVsync();

		// If slot managed, read current slot:
		if (isSlotManaged()) {
			String slotName = ROAMINGPREFS.get("slot_"+getInternalVersion(), null);
			if (slotName != null) {
				PlayerSlot slot = new PlayerSlot(slotName);
				if (slot.exists()) {
					setPlayerSlot(slot);
				}
			}
		}

		// Now go into 3D...
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glFrustum(-Game.getWidth() / 64.0, Game.getWidth() / 64.0, -Game.getHeight() / 64.0, Game.getHeight() / 64.0, 8, 65536);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		game.onInit();

		// Show title screen or registration screen as appropriate
		if (!isRegistered() || TESTREGISTER) {
			unregisteredStartup();
		} else if (REGISTERED || game.preregistered) {
			preRegisteredStartup();
		} else {
			registeredStartup();
		}

		// Now run!
		resourcesStream.close();
		resourcesStream = null;
		try {
			game.run();
		} catch (Throwable t) {
			System.err.println("Set exception to " + t + " : " + t.getMessage()+" Stack trace follows:");
			t.printStackTrace(System.err);
			gameInfo.setException(t);
		}

		exit();
	}

	/**
	 * Called by {@link #init(Properties, InputStream)} just before we show any screens and start running the game
	 */
	protected void onInit() {
	}

	/**
	 * Called instead of opening the title screen
	 */
	private static void preRegisteredStartup() {
		game.onPreRegisteredStartup();
	}

	protected void onPreRegisteredStartup() {
	}

	private static void unregisteredStartup() {
		game.onUnregisteredStartup();
	}

	protected void onUnregisteredStartup() {
	}

	private static void registeredStartup() {
		game.onRegisteredStartup();
	}

	protected void onRegisteredStartup() {
	}

	/**
	 * Get the roaming app settings dir
	 * @return String
	 */
	private static String getRoamingSettingsDir() {
		if (isUsingSteamCloud()) {
			return "";
		} else {
			return getLocalSettingsDir();
		}
	}

	/**
	 * Get the local settings dir
	 * @return String
	 */
	private static String getLocalSettingsDir() {
		return
			properties.getProperty
				(
					"home",
					System.getProperty("os.name").startsWith("Mac OS")
						?
					System.getProperty("user.home", "") + "/Library/Application Support"
						:
					System.getProperty("user.home", "")
				);
	}

	/**
	 * Get the directory prefix for roaming files (includes trailing slash); these may be stored locally
	 * @return String
	 */
	public static String getRoamingDirectoryPrefix() {
		return roamingDirPrefix;
	}

	/**
	 * Get the directory prefix for local files (includes trailing slash)
	 * @return String
	 */
	public static String getLocalDirPrefix() {
	    return localDirPrefix;
    }

	/**
	 * Get the player directory prefix
	 * @return the directory prefix for the current player's "slot" local settings (includes trailing slash)
	 */
	public static String getPlayerDirectoryPrefix() {
		String ret = getRoamingDirectoryPrefix() + "slots_" + getInternalVersion() + File.separator + playerSlot.getName();
		// Lazy creation
		if (!isUsingSteamCloud()) {
			File f = new File(ret);
			if (!f.exists()) {
				f.mkdirs();
			}
		}
		return ret + File.separator;
	}

	/**
	 * Get the slot directory prefix
	 * @return the directory prefix for all the player slots (includes trailing slash)
	 */
	public static String getSlotDirectoryPrefix() {
		String ret = getRoamingDirectoryPrefix() + "slots_" + getInternalVersion();
		// Lazy creation
		File f = new File(ret);
		if (!f.exists()) {
			f.mkdirs();
		}
		return ret + File.separator;
	}

	/**
	 * Initialise all the file paths we need (local and roaming directories and prefixes)
	 */
	private static void initFiles() {
		boolean remoteDirPrefixExists;
		String remoteSettingsDirName = getRoamingSettingsDir() + File.separator + "." + getTitle().replace(' ', '_').toLowerCase() + '_' + getInternalVersion();
		System.out.println("Roaming settings dir name="+remoteSettingsDirName);
		if (!isUsingSteamCloud()) {
			File settingsDir = new File(remoteSettingsDirName);
			if (!settingsDir.exists()) {
				System.out.println("Creating roaming settings dir: "+remoteSettingsDirName);
				settingsDir.mkdirs();
				remoteDirPrefixExists = settingsDir.exists();
			} else {
				remoteDirPrefixExists = true;
			}
		} else {
			remoteDirPrefixExists = true;
		}

		boolean localDirPrefixExists;
		String localSettingsDirName = getLocalSettingsDir() + File.separator + "." + getTitle().replace(' ', '_').toLowerCase() + '_' + getInternalVersion();
		System.out.println("Local settings dir name="+localSettingsDirName);
		if (!isUsingSteamCloud()) {
			File localSettingsDir = new File(localSettingsDirName);
			if (!localSettingsDir.exists()) {
				System.out.println("Creating local settings dir: "+localSettingsDirName);
				localSettingsDir.mkdirs();
				localDirPrefixExists = localSettingsDir.exists();
			} else {
				localDirPrefixExists = true;
			}
		} else {
			localDirPrefixExists = true;
		}


		localDirPrefix = localDirPrefixExists ? localSettingsDirName + File.separator : getRoamingSettingsDir() + File.separator;
		roamingDirPrefix = remoteDirPrefixExists ? remoteSettingsDirName + File.separator : getRoamingSettingsDir() + File.separator;

		System.out.println("Local dir prefix="+localDirPrefix);
		System.out.println("Roaming dir prefix="+roamingDirPrefix);
		GAMEINFO_FILE = localDirPrefix + "log.dat";
	}

	/**
	 * Write out the execution log. If we're online, then we'll read in the existing execution log (if any), append the current
	 * GameInfo, and send it to the server. If we're offline or unsuccessful we'll write it to disk for a rainy day. The log is
	 * deleted once it's sent successfully.
	 */
	private static void writeLog() {
		if (gameInfo == null) {
			System.out.println("No game info log");
		}

		if (GAMEINFO_FILE == null) {
			System.out.println("Couldn't write log - no filename");
			return;
		}

		// Find and load any existing log
		File log = new File(getLocalSettingsDir() + File.separator + GAMEINFO_FILE);
		List<GameInfo> logList = null;
		if (log.exists()) {
			ObjectInputStream ois = null;
			BufferedInputStream bis = null;
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(log);
				bis = new BufferedInputStream(fis);
				ois = new ObjectInputStream(bis);
				logList = (List<GameInfo>) ois.readObject(); // Warning suppressed
			} catch (Exception e) {
				e.printStackTrace(System.err);
				logList = new ArrayList<GameInfo>(1);
			} finally {
				if (ois != null) {
					try {
						ois.close();
					} catch (Exception e) {
					}
				}
				if (bis != null) {
					try {
						bis.close();
					} catch (Exception e) {
					}
				}
				if (fis != null) {
					try {
						fis.close();
					} catch (Exception e) {
					}
				}
				ois = null;
				bis = null;
				fis = null;
			}
		} else {
			// Don't bother with logging if the game is registered - unless
			// we've got a support call pending
			if (isRegistered() && !Support.isSupportQueued()) {
				return;
			}
			logList = new ArrayList<GameInfo>(1);
		}
		if (gameInfo != null) {
			// Update gameInfo
			game.updateLog();
			logList.add(gameInfo);
		}

		// Attempt submission
		boolean isOnline = isRemoteCallAllowed();
		boolean submitted = false;
		if (CheckOnline.isOnline() && (isOnline || Support.isSupportQueued() || gameInfo.isCrashRecovery() || gameInfo.getException() != null)) {
			try {
				// System.out.print("Submitting log... ");
				GameInfoServerRemote server = (GameInfoServerRemote) Naming.lookup("//puppygames.net/" + GameInfoServerRemote.REMOTE_NAME);
				server.submit(logList);
				onRemoteCallSuccess();
				submitted = true;
				log.delete();
				// System.out.println(" Log submitted.");
			} catch (Throwable e) {
				System.err.println(" Failed to write log: "+e);
				e.printStackTrace(System.err);
			}
		}
		if (!submitted) {
			// Write out log for a rainy day
			FileOutputStream fos = null;
			BufferedOutputStream bos = null;
			ObjectOutputStream oos = null;
			try {
				fos = new FileOutputStream(log);
				bos = new BufferedOutputStream(fos);
				oos = new ObjectOutputStream(bos);
				oos.writeObject(logList);
				oos.flush();
				bos.flush();
				fos.flush();
			} catch (IOException e) {
				//e.printStackTrace(System.err);
			} finally {
				if (oos != null) {
					try {
						oos.close();
					} catch (Exception e) {
					}
				}
				if (bos != null) {
					try {
						bos.close();
					} catch (Exception e) {
					}
				}
				if (fos != null) {
					try {
						fos.close();
					} catch (Exception e) {
					}
				}
			}
		}
	}

	/**
	 * Update the log with any information before writing it
	 */
	protected void updateLog() {
	}

	public static void requestExit() {
		game.doRequestExit();
	}

	protected void doRequestExit() {
		// Don't ask just exit by default
		exit();
	}

	/**
	 * Exit the program
	 */
	public static void exit() {
		if (!initialised) {
			return;
		}
		if (game != null) {
			game.onExit();
		}
		initialised = false;
		finished = true;

		// Destroy the sound player
		if (soundPlayer != null && soundPlayer.isCreated()) {
			soundPlayer.destroy();
		}

		// Do any other cleanup
		if (game != null) {
			game.cleanup();
		}

		// Write log
		writeLog();

		// If we got here, it's because of a nice clean exit. We'll clear the
		// bad exit flag.
		if (LOCALPREFS != null) {
			LOCALPREFS.putBoolean("badexit", false);
		}
		if (prefsSaver != null) {
			prefsSaver.finish();
			prefsSaver = null;
		}


		Steam.destroy();
		AL.destroy();
		Display.destroy();
		System.exit(0);
	}

	/**
	 * Called on exit
	 */
	protected void onExit() {
	}

	/**
	 * Play a sound effect. The sound effect is always owned by the game, for simplicity
	 * @param buffer The sound to play
	 * @return a SoundEffect, or null, if a sound effect could not be created
	 */
	public static SoundEffect allocateSound(ALBuffer buffer) {
		return allocateSound(buffer, 1.0f, 1.0f);
	}

	/**
	 * Play a sound effect. The sound effect is always owned by the game, for simplicity
	 * @param buffer The sound to play
	 * @return a SoundEffect, or null, if a sound effect could not be created
	 */
	public static SoundEffect allocateSound(ALBuffer buffer, float gain, float pitch) {
		return allocateSound(buffer, gain, pitch, Game.class);
	}

	/**
	 * Play a sound effect. The sound effect is always owned by the game, for simplicity
	 * @param buffer The sound to play
	 * @return a SoundEffect, or null, if a sound effect could not be created
	 */
	public static SoundEffect allocateSound(ALBuffer buffer, float gain, float pitch, Object owner) {
		if (!org.lwjgl.openal.AL.isCreated() || !isSFXEnabled() || buffer == null || buffer.getWave() == null || soundPlayer == null) {
			return null;
		}
		SoundEffect effect = soundPlayer.allocate(buffer, (int) (buffer.getPriority() * gain), owner);
		if (effect != null && effect.getBuffer() != null) {
			effect.setAttenuated(false, owner);
			effect.setGain(effect.getBuffer().getGain() * sfxVolume * gain / 100.0f, owner);
			effect.setPitch(effect.getBuffer().getPitch() * pitch, owner);
			if (buffer.getWave().getType() == Wave.MONO_16BIT) {
				effect.setPosition(0.0f, 0.0f, 0.0f, owner);
			}
		}
		return effect;
	}

	/**
	 * Play a sound effect. The sound effect is always owned by the game, for simplicity
	 * @param buffer The sound to play
	 * @return a SoundEffect, or null, if a sound effect could not be created
	 */
	public static SoundEffect allocateSound(ALStream buffer) {
		if (!org.lwjgl.openal.AL.isCreated() || !isSFXEnabled() || buffer == null || soundPlayer == null) {
			return null;
		}
		SoundEffect effect = soundPlayer.allocate(buffer, Game.class);
		if (effect != null) {
			effect.setAttenuated(false, Game.class);
			effect.setGain(effect.getStream().getSourceStream().getGain() * sfxVolume / 100.0f, Game.class);
			if (effect.getStream().getType() == Wave.MONO_16BIT) {
				effect.setPosition(0.0f, 0.0f, 0.0f, Game.class);
			}
		}
		return effect;
	}

	/**
	 * Are sound effects enabled?
	 * @return boolean
	 */
	public static boolean isSFXEnabled() {
		return sfxEnabled;
	}

	/**
	 * Is music enabled?
	 * @return boolean
	 */
	public static boolean isMusicEnabled() {
		return musicEnabled;
	}

	/**
	 * Play some music. The existing music is nicely crossfaded.
	 * @param buf The buffer containing the music
	 * @param fade The fade length, in ticks
	 * @param gain Extra gain multiplier
	 */
	public static void playMusic(ALStream buf, int fade, float gain) {
		// Maybe don't do anything
		if (music == null && buf == null || music != null && (music.getStream() == null || buf == music.getStream().getSourceStream())) {
			if (music != null && music.getStream() != null) {
				music.setFade(fade + 1, gain * music.getStream().getSourceStream().getGain() * musicVolume / 100.0f, false, Game.class);
			}
			return;
		}

		// Fade out existing music
		boolean xfade = music != null;
		if (xfade) {
			music.setFade(fade + 1, 0.0f, true, Game.class);
		}

		if (buf == null || !isMusicEnabled()) {
			music = null;
		} else {
			music = allocateSound(buf);
		}
		if (music != null) {
			if (xfade) {
				music.setGain(0.0f, Game.class);
				music.setFade(fade + 1, buf == null ? 0.0f : gain * buf.getGain() * musicVolume / 100.0f, false, Game.class);
			} else {
				music.setGain(buf == null ? 0.0f : gain * buf.getGain() * musicVolume / 100.0f, Game.class);
			}
		}
	}

	/**
	 * Play some music. The existing music is nicely crossfaded.
	 * @param buf The buffer containing the music
	 * @param fade The fade length, in ticks
	 */
	public static void playMusic(ALStream buf, int fade) {
		playMusic(buf, fade, 1.0f);
	}

	/**
	 * Play some music. The existing music is nicely crossfaded.
	 * @param buf The buffer containing the music
	 * @param fade The fade length, in ticks
	 * @param gain Extra gain
	 */
	public static void playMusic(ALBuffer buf, int fade, float gain) {
		// Maybe don't do anything
		if (music == null && buf == null || music != null && buf == music.getBuffer()) {
			return;
		}

		// Fade out existing music
		boolean xfade = music != null;
		if (xfade) {
			music.setFade(fade, 0.0f, true, Game.class);
		}

		if (buf == null || !isMusicEnabled()) {
			music = null;
		} else {
			music = allocateSound(buf);
		}
		if (music != null) {
			if (xfade) {
				music.setGain(0.0f, Game.class);
				music.setFade(fade, buf == null ? 0.0f : gain * buf.getGain() * musicVolume / 100.0f, false, Game.class);
			} else {
				music.setGain(buf == null ? 0.0f : gain * buf.getGain() * musicVolume / 100.0f, Game.class);
			}
		}
	}

	/**
	 * Play some music. The existing music is nicely crossfaded.
	 * @param buf The buffer containing the music
	 * @param fade The fade length, in ticks
	 */
	public static void playMusic(ALBuffer buf, int fade) {
		playMusic(buf, fade, 1.0f);
	}

	/**
	 * Get the current music
	 * @return SoundEffect, or null
	 */
	public static SoundEffect getMusic() {
		return music;
	}

	/**
	 * Initialise the sound system
	 */
	private static void initSound() {
		// Create sound
		try {
			System.out.println("Initing sound");
			org.lwjgl.openal.AL.create();
			soundPlayer = new SoundPlayer(game.soundVoices);
			soundPlayer.create();
			SoundCommand.setDefaultSoundPlayer(soundPlayer);
			musicVolume = LOCALPREFS.getInt("musicvolume", 70);
			sfxVolume = LOCALPREFS.getInt("sfxvolume", 70);

			music = null;
		} catch (Exception e) {
			sfxEnabled = false;
			musicEnabled = false;
			e.printStackTrace();
			String message = ((TextWrapper) Resources.get("lwjglapplets.game.badsound")).getText().replace("[title]", getDisplayTitle());
			message = message.replace("[email]", getSupportEmail());
			Game.alert(message);
			Support.doSupport("openal");
			if (org.lwjgl.openal.AL.isCreated()) {
				org.lwjgl.openal.AL.destroy();
			}
		}
	}

	private static ByteBuffer imageToBuffer(BufferedImage src, int size) {
		ColorModel ccm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
		ByteBuffer ret = BufferUtils.createByteBuffer(size * size * 4);
		BufferedImage scaledImage = new BufferedImage(ccm, ccm.createCompatibleWritableRaster(size, size), false, null);
		Graphics2D g2d = (Graphics2D) scaledImage.getGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(src, 0, 0, size, size, null);
		g2d.dispose();
		byte[] data = (byte[]) scaledImage.getData().getDataElements(0, 0, size, size, new byte[size * size * 4]);
		ret.put(data);
		ret.rewind();
		return ret;
	}

	/**
	 * Creates the AWT display
	 * @throws LWJGLException
	 */
	private static void createDisplay() throws LWJGLException {
		// Load window size from prefs, or choose something sensible based on initial desktop display resolution
		setWindowSizeFromPreferences();

		Display.setResizable(true);
		try {
	        BufferedImage iconImage = ImageIO.read(Thread.currentThread().getContextClassLoader().getResource(game.icon));
			ByteBuffer[] imageBuffer = null;
			switch (LWJGLUtil.getPlatform()) {
				case LWJGLUtil.PLATFORM_WINDOWS:
					// Create 16x16 and 32x32 icons, as well as the original one
					imageBuffer = new ByteBuffer[3];
					imageBuffer[0] = imageToBuffer(iconImage, iconImage.getWidth());
					imageBuffer[1] = imageToBuffer(iconImage, 16);
					imageBuffer[2] = imageToBuffer(iconImage, 32);
					break;
				case LWJGLUtil.PLATFORM_MACOSX:
					// One 128x128 icon
					imageBuffer = new ByteBuffer[1];
					imageBuffer[0] = imageToBuffer(iconImage, 128);
					break;
				case LWJGLUtil.PLATFORM_LINUX:
					// One 32x32 icon
					imageBuffer = new ByteBuffer[1];
					imageBuffer[0] = imageToBuffer(iconImage, 32);
					break;
			}

			if (imageBuffer != null) {
				Display.setIcon(imageBuffer);
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		Display.setTitle(getDisplayTitle());
		Display.create();
	}

	private static void setWindowSizeFromPreferences() throws LWJGLException {
		int desktopWidth = Display.getDesktopDisplayMode().getWidth();
		int desktopHeight = Display.getDesktopDisplayMode().getHeight();
		int width = Math.max(game.scale, Math.min(desktopWidth, getLocalPreferences().getInt("window.width", (desktopWidth * 4) / 5)));
		int height = Math.max(game.scale, Math.min(desktopHeight, getLocalPreferences().getInt("window.height", (desktopHeight * 4) / 5)));
		game.width = width;
		game.height = height;
		Display.setDisplayMode(new DisplayMode(width, height));
	}

	/**
	 * Initialise the display
	 * @throws Exception if the display fails to initialise, probably because the graphics card has no OpenGL drivers
	 */
	private static void initDisplay() throws Exception {

		if (properties.containsKey("vx")) {
			viewportXoffset = Integer.parseInt(properties.getProperty("vx", "0"));
		} else {
			viewportXoffset = 0;
		}
		if (properties.containsKey("vy")) {
			viewportYoffset = Integer.parseInt(properties.getProperty("vy", "0"));
		} else {
			viewportYoffset = 0;
		}
		if (properties.containsKey("vw")) {
			viewportWidth = Integer.parseInt(properties.getProperty("vw", "0"));
		} else {
			viewportWidth = Display.getDesktopDisplayMode().getWidth();
		}
		if (properties.containsKey("vh")) {
			viewportHeight = Integer.parseInt(properties.getProperty("vh", "0"));
		} else {
			viewportHeight = Display.getDesktopDisplayMode().getHeight();
		}

		if ("!".equals(LOCALPREFS.get("fullscreen2", "!"))) {
			LOCALPREFS.putInt("fullscreen2", game.defaultFullscreen);
		}

		createDisplay();

		// Determine fullscreenness
		if (Boolean.getBoolean("net.puppygames.applet.Game.windowed")) {
			setFullscreen(false);
		} else {
			int fs = LOCALPREFS.getInt("fullscreen2", 0);
			switch (fs) {
				case 0:
					setFullscreen(configuration.isFullscreen());
					break;
				case 1:
					setFullscreen(false);
					break;
				case 2:
					setFullscreen(true);
					break;
			}
		}
	}

	/**
	 * Set/unset fullscreen mode
	 * @param fullscreen
	 */
	public static void setFullscreen(boolean fullscreen) throws Exception {
		try {

			try {
				if (Mouse.isCreated()) {
					Mouse.destroy();
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			try {
				if (Keyboard.isCreated()) {
					Keyboard.destroy();
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}

			// Use the desktop display mode
			if (fullscreen) {
				Display.setDisplayModeAndFullscreen(Display.getDesktopDisplayMode());
			} else {
				Display.setFullscreen(false);
				setWindowSizeFromPreferences();
			}
			game.width = Display.getWidth();
			game.height = Display.getHeight();
			if (Display.isCreated()) {
				Display.update();
			}
			initVsync();
			if (!Display.isCreated()) {
				Display.create();
			}
			try {
				Mouse.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			try {
				Keyboard.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}

			// Properly clear the entire display
			viewPort = new Rectangle(0, 0, Display.getWidth(), Display.getHeight());
			resetViewport();
			glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT);
			Display.update();
			Display.setResizable(false);
			if (!fullscreen) {
				Display.setResizable(true);
			}

			doResize();

			LOCALPREFS.putInt("fullscreen2", fullscreen ? 2 : 1);
			try {
				LOCALPREFS.sync();
			} catch (BackingStoreException e) {
			}
		} catch (Exception e) {
			System.err.println("Failed to set fullscreen=" + fullscreen + " due to " + e);
			throw e;
		}
	}

	/**
	 * Initialise vsync
	 */
	private static void initVsync() {
		int freq = Display.getDisplayMode().getFrequency();
		if (freq != getFrameRate()) {
			Display.setVSyncEnabled(false);
		} else {
			Display.setVSyncEnabled(true);
		}
	}

	private static void doResize() {
		if (!game.dontScale) {
			// Fit the scale into the smaller dimension, and scale the other dimension
			if (game.width < game.height) {
				// Weirdy potrait orientation
				double ratio = (double) game.width / (double) game.scale;
				game.logicalWidth = game.scale;
				game.logicalHeight = (int) (game.height / ratio);
			} else {
				// Normal landscape orientation
				double ratio = (double) game.height / (double) game.scale;
				game.logicalHeight = game.scale;
				game.logicalWidth = (int) (game.width / ratio);
			}
		} else {
			game.logicalWidth = game.width;
			game.logicalHeight = game.height;
		}
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glFrustum(-game.logicalWidth / 64.0, game.logicalWidth / 64.0, -game.logicalHeight / 64.0, game.logicalHeight / 64.0, 8, 65536);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		if (Display.isFullscreen()) {
			viewPort = new Rectangle(viewportXoffset, viewportYoffset, viewportWidth, viewportHeight);
		} else {
			viewPort = new Rectangle(0, 0, Display.getWidth(), Display.getHeight());
		}
		resetViewport();
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		Screen.onGameResized();
	}

	/**
	 * Reset the game viewport
	 */
	public static void resetViewport() {
//		System.out.println("Set viewport to "+viewPort);
		glViewport(viewPort.getX(), viewPort.getY(), viewPort.getWidth(), viewPort.getHeight());
	}

	/**
	 * Do custom cleanup
	 */
	protected void cleanup() {
	}

	private static long generatePregregistrationKey() {
		return getInstallation() ^ System.getProperty("user.country").hashCode() ^ (long) System.getProperty("user.language").hashCode() << 16L ^ (long)System.getProperty("user.home").hashCode() << 32L;
	}

	/**
	 * Validate the user's registration details. The outcome of this method is that the game will either be tagged as "registered"
	 * or not.
	 */
	private static void checkRegistration() {
		if (game.preregistered) {
			// This game is completely DRM free.
			return;
		}
		// Check for preregistration-until date
		if (game.preregisteredUntil != null) {
			// Is game already installed?
			if (LOCALPREFS.getLong("preregistered-installation", 0L) == generatePregregistrationKey()) {
				boolean ok = true;

				if (game.preregisteredLocale != null) {
					String country = Locale.getDefault().getCountry();
					System.out.println("Country check: "+country+" vs "+game.preregisteredLocale);
					if (!game.preregisteredLocale.equals(country)) {
						ok = false;
					}
				}

				if (game.preregisteredLanguage != null) {
					String language = Locale.getDefault().getLanguage();
					System.out.println("Language check: "+language+" vs "+game.preregisteredLanguage);
					if (!game.preregisteredLanguage.equals(language)) {
						ok = false;
					}
				}
				if (ok) {
					registered = true;
					return;
				}
			} else {
				// No. Are we after expiry date?
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				Date until;
				try {
					until = df.parse(game.preregisteredUntil);
					if (new Date().before(until)) {
						// No. So this game is preregistered, provided locale checks pass.
						boolean ok = true;

						if (game.preregisteredLocale != null) {
							String country = Locale.getDefault().getCountry();
							if (!game.preregisteredLocale.equals(country)) {
								ok = false;
							}
						}

						if (game.preregisteredLanguage != null) {
							String language = Locale.getDefault().getLanguage();
							if (!game.preregisteredLanguage.equals(language)) {
								ok = false;
							}
						}

						if (ok) {
							// Ok, we are now permanently registered
							LOCALPREFS.putLong("preregistered-installation", generatePregregistrationKey());
							registered = true;
							flushPrefs();
							return;
						}
					}
				} catch (ParseException e) {
					e.printStackTrace(System.err);
				}
			}
		}

		try {
			RegistrationDetails checkedDetails = RegistrationDetails.checkRegistration(getTitle());
			setRegistrationDetails(checkedDetails);
		} catch (Exception e) {
			setRegistrationDetails(null);
		}
	}

	/**
	 * Stash the customer's registration details.
	 * @param registrationDetails The new registration details
	 */
	public static void setRegistrationDetails(RegistrationDetails registrationDetails) {
		if (Game.registrationDetails == registrationDetails) {
			return;
		}
		Game.registrationDetails = registrationDetails;
		if (registrationDetails != null) {
			registered = true;
			System.out.println("Game is registered: " + registrationDetails);
			registrationDetails.toPreferences();
		} else {
			System.out.println("Game is unregistered.");
			registered = false;
			RegistrationDetails.clearRegistration(getTitle());
		}
	}

	private static void createConfiguration() throws Exception {
		configuration = new Configuration();
		// See if there's a configuration list
		if (Resources.exists("configurations")) {
			StringArray configurations = (StringArray) Resources.peek("configurations");
			String config = configurations.getString(Util.random(0, configurations.getNumStrings() - 1));
			configuration.decode(config);
		} else {
			configuration.init();
		}
		writeConfiguration();
	}

	private static void writeConfiguration() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(configuration);
		oos.flush();
		byte[] cfg = baos.toByteArray();
		LOCALPREFS.putByteArray("configuration", cfg);
		oos.close();
	}

	/**
	 * Set a new configuration
	 * @param newConfiguration
	 */
	public static void setConfiguration(Configuration newConfiguration) throws Exception {
		configuration = newConfiguration;
		writeConfiguration();
	}

	/**
	 * Run the game.
	 */
	@SuppressWarnings("unused")
    private void run() {
//    	// Linux hack
//		if (LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_LINUX && !Boolean.getBoolean("net.puppygames.applet.Game.dontAlwaysRun")) {
//			setAlwaysRun(true);
//		}

		int ticksToDo = 1;
		long then = Sys.getTime() & 0x7FFFFFFFFFFFFFFFL;
		long framesTicked = 0;
		long timerResolution = Sys.getTimerResolution();
		while (!finished) {
			if (!Display.isFullscreen() && (Display.getWidth() != game.width || Display.getHeight() != game.height)) {
				game.width = Display.getWidth();
				game.height = Display.getHeight();
				doResize();
				getLocalPreferences().putInt("window.width", game.width);
				getLocalPreferences().putInt("window.height", game.height);
				flushPrefs();
			}

			if (Display.isCloseRequested()) {
				finished = true;
				break;
			}

			if (Display.isDirty() || Display.isActive() || alwaysRun) {
				// The window is in the foreground, so we should play the game
				long now = Sys.getTime() & 0x7FFFFFFFFFFFFFFFL;
				long currentTimerResolution = Sys.getTimerResolution();
				if (currentTimerResolution != timerResolution) {
					// Timer resolution change -- all bets off
					if (DEBUG) {
						System.out.println("Timer resolution change from "+timerResolution+" to "+currentTimerResolution);
					}
					timerResolution = currentTimerResolution;
					then = now;
				}

				if (now > then) {
					long ticksElapsed = now - then;
					double shouldHaveTickedThisMany = (double) (getFrameRate() * ticksElapsed) / (double) timerResolution;
					ticksToDo = (int) Math.max(0.0, shouldHaveTickedThisMany - framesTicked);
					if (ticksToDo > 20) {
						// We're overrunning!
						if (DEBUG) {
							System.out.println("Frame overrun! "+ticksToDo);
						}
						ticksToDo = 1;
						then = now;
						framesTicked = 0;
					}
				} else if (now < then) {
					if (DEBUG) {
						System.out.println("Clock reset: "+Long.toHexString(now)+" vs "+Long.toHexString(then));
					}
					ticksToDo = 0;
					then = now;
					framesTicked = 0;
				} else {
					ticksToDo = 0;
				}

				if (ticksToDo > 0) {
					if (DEBUG && ticksToDo > 1) { // Warning suppressed
						System.out.println("Do "+ticksToDo+" ticks");
					}
					for (int i = 0; i < ticksToDo; i ++) {
						if (i > 0) {
							Display.processMessages();
							catchUp = true;
						} else {
							catchUp = false;
						}
						// If any tick takes longer than the allotted time, reset counters and stop ticking. We're basically running flat out.
						long tickThen = Sys.getTime() & 0x7FFFFFFFFFFFFFFFL;
						tick();
						long tickNow = Sys.getTime() & 0x7FFFFFFFFFFFFFFFL;
						long tickElapsed = tickNow - tickThen;
						if (tickElapsed < 0 || tickElapsed > (double) timerResolution / getFrameRate()) {
							ticksToDo = 0;
							then = tickThen;
							framesTicked = 1;
							catchUp = false;
							if (DEBUG) {
								System.out.println("Running flat out! "+tickElapsed+" vs "+(double) timerResolution / getFrameRate());
							}
						}
						Thread.yield();
					}
					if (ticksToDo > 0) {
						fps ++;
						if (fps == FPS.length) {
							fps = 0;
						}
						FPS[fps] = getFrameRate() / ticksToDo;
						int totalF = 0;
						for (int i = 0; i < FPS.length; i ++) {
							totalF += FPS[i];
						}
						currentFPS = totalF / FPS.length;
						framesTicked += ticksToDo;
					}
					render();
					// Steam support
					if (isUsingSteam()) {
						Steam.tick();
					}
					Display.update();
				}
				if (DEBUG || forceSleep) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
					}
				} else {
					Thread.yield();
				}

				// Ensure we have sound
				if (isPaused()) {
					targetMasterGain = 0.0f;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				} else {
					targetMasterGain = 1.0f;
				}

			} else {
				// The window is not in the foreground, so we can allow other stuff to run and
				// infrequently update

				// Silence the game
				targetMasterGain = 0.0f;

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				if (Display.isVisible() || Display.isDirty()) {
					// Only bother rendering if the window is visible or dirty
					render();
				}
				Display.update();
				if (Mouse.isGrabbed()) {
					Mouse.setGrabbed(false);
				}
			}

			// Master gain
			if (Math.abs(targetMasterGain - masterGain) < 0.1f) {
				masterGain = targetMasterGain;
			} else if (targetMasterGain > masterGain) {
				masterGain += 0.1f;
			} else {
				masterGain -= 0.1f;
			}
			if (isSFXEnabled()) {
				alListenerf(AL_GAIN, masterGain);
			}

		}
	}

	/**
	 * @return true if this frame is a catch-up frame
	 */
	public static boolean isCatchUp() {
		return game.catchUp;
	}

	/**
	 * @return mouse events since last update
	 */
	public static List<MouseEvent> getMouseEvents() {
		return MOUSEEVENTS;
	}

	/**
	 * Tick. Called every frame.
	 */
	private void tick() {
		// Process key & mouse bindings
		Binding.poll();

		// Process mouse events
		MOUSEEVENTS.clear();
		while (Mouse.next()) {
			MouseEvent event = new MouseEvent();
			event.fromMouse();
			MOUSEEVENTS.add(event);
		}

		// Process keydowns
		for (int i = 0; i < Keyboard.KEYBOARD_SIZE; i++) {
			if (Keyboard.isKeyDown(i)) {
				if (!KEYWASDOWN[i]) {
					KEYDOWN[i] = true;
				} else {
					KEYDOWN[i] = false;
				}
				KEYWASDOWN[i] = true;
			} else {
				KEYWASDOWN[i] = false;
				KEYDOWN[i] = false;
			}
		}

		// Check for escape
		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			panic++;
			if (panic == 60) {
				exit();
			}
		} else {
			panic = 0;
		}

		// Cheese for the camera
		if (wasKeyPressed(Keyboard.KEY_P) && pauseEnabled) {
			setPaused(!paused);
		}

		if (!paused) {
			Timer.tick();
			// Custom ticking
			doTick();
			// Tick screens
			Screen.tickAllScreens();
			// Now tick the sound engine
			if (org.lwjgl.openal.AL.isCreated()) {
				int n = soundPlayers.size();
				for (int i = 0; i < n; i ++) {
					SoundPlayer sp = soundPlayers.get(i);
					sp.play();
				}
			}
		}
	}

	public static void setPaused(boolean newPaused) {
		paused = newPaused;
		if (game != null) {
			if (paused) {
				game.wasGrabbed = Mouse.isGrabbed();
				//Mouse.setGrabbed(false);
				Display.setTitle(getDisplayTitle() + " [PAUSED]");
				game.onPaused();
			} else {
				//Mouse.setGrabbed(game.wasGrabbed);
				Display.setTitle(getDisplayTitle());
				game.onResumed();
			}
		}
	}

	protected abstract void doTick();

	protected void onPaused() {
	}

	protected void onResumed() {
	}

	/**
	 * Render. Called every frame.
	 */
	private void render() {
		Screen.updateAllScreens();
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		glPushMatrix();
		glTranslatef((float)(-logicalWidth / 2.0), (float)(-logicalHeight / 2.0), -256);
		preRender();
		Screen.renderAllScreens();
		postRender();
		glPopMatrix();
	}

	/**
	 * Called before any screens are rendered
	 */
	protected void preRender() {
	}

	/**
	 * Called after all screens are rendered
	 */
	protected void postRender() {
	}

	/**
	 * Check for keyboard presses. After checking, the key down state is cleared.
	 * @param key The keyboard key
	 * @return true if the key has just been pressed
	 */
	public static boolean wasKeyPressed(int key) {
		boolean ret = KEYDOWN[key];
		KEYDOWN[key] = false;
		return ret;
	}

	/**
	 * Called whenever a successful remote call is made, so we know that this app is allowed to contact teh intarweb.
	 */
	public static void onRemoteCallSuccess() {
		LOCALPREFS.putBoolean("online", true);
		try {
			LOCALPREFS.flush();
		} catch (BackingStoreException e) {
		}
	}

	/**
	 * Are remote calls allowed?
	 * @return boolean
	 */
	public static boolean isRemoteCallAllowed() {
		return LOCALPREFS.getBoolean("online", false);
	}

	/**
	 * Set music volume
	 * @param volume 0.0f...1.0f
	 */
	public static void setMusicVolume(float vol) {
		musicVolume = (int) Math.max(0.0f, Math.min(100.0f, vol * 100.0f));
		if (music != null) {
			if (music.getStream() != null) {
				if (music.getStream().getSourceStream() != null) {
					music.setGain(music.getStream().getSourceStream().getGain() * vol, Game.class);
				}
			} else if (music.getBuffer() != null) {
				music.setGain(music.getBuffer().getGain() * vol, Game.class);
			}
		}
		LOCALPREFS.putInt("musicvolume", musicVolume);
		try {
			LOCALPREFS.sync();
		} catch (BackingStoreException e) {
		}
	}

	/**
	 * Set sfx volume
	 * @param volume 0.0f...1.0f
	 */
	public static void setSFXVolume(float vol) {
		sfxVolume = (int) Math.max(0.0f, Math.min(100.0f, vol * 100.0f));
		LOCALPREFS.putInt("sfxvolume", sfxVolume);
		try {
			LOCALPREFS.sync();
		} catch (BackingStoreException e) {
		}
	}

	/**
	 * Get music volume
	 * @return volume 0.0f...1.0f
	 */
	public static float getMusicVolume() {
		return musicVolume / 100.0f;
	}

	/**
	 * Get sfx volume
	 * @return volume 0.0f...1.0f
	 */
	public static float getSFXVolume() {
		return sfxVolume / 100.0f;
	}

	/**
	 * Get the game viewport
	 * @return Rectangle
	 */
	public static Rectangle getViewPort() {
		return viewPort;
	}

	/**
	 * Initialise input
	 */
	public static void initInput() {
		try {
			game.inputMode = InputDeviceType.valueOf(LOCALPREFS.get("inputmode", InputDeviceType.DESKTOP.name()));
		} catch (Exception e) {
			game.inputMode = InputDeviceType.valueOf(game.defaultInputMode);
		}
	}

	/**
	 * Get the game's input mode
	 * @return the game's input mode
	 */
	public static InputDeviceType getInputMode() {
	    return game.inputMode;
    }

	/**
	 * Load the keyboard and mouse bindings, if possible. Fails silently.
	 */
	public static void loadBindings() {
		try {
			byte[] b = ROAMINGPREFS.getByteArray("bindings", null);
			if (b == null) {
				if (DEBUG) {
					System.out.println("No bindings found, so using defaults.");
				}
				Binding.resetToDefaults();
			} else {
				Binding.load(new ByteArrayInputStream(b));
			}
		} catch (Exception e) {
			if (DEBUG) {
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Save the keyboard and mouse bindings. Fails silently.
	 */
	public static void saveBindings() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
			Binding.save(baos);
			ROAMINGPREFS.putByteArray("bindings", baos.toByteArray());
			flushPrefs();
		} catch (Exception e) {
			if (DEBUG) {
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Open the redefine keys screen, if there is one
	 */
	public static void redefineKeys() {
		game.doRedefineKeys();
	}

	/**
	 * Flushes all preferences (synchronously)
	 */
	private static void doFlushPrefs() {
		GameOutputStream gos;
        try {
	        gos = new GameOutputStream(getRoamingPrefsFileName());
			writePrefs(gos);
			if (DEBUG) {
				System.out.println("Saved preferences file "+getRoamingPrefsFileName());
			}
        } catch (IOException e) {
	        e.printStackTrace(System.err);
        }
	}

	/**
	 * Synchronously write preferences to an output stream
	 * @param os
	 */
	private static void writePrefs(OutputStream os) {
		if (GLOBALPREFS != null) {
			synchronized (GLOBALPREFS) {
				try {
					GLOBALPREFS.sync();
					LOCALPREFS.sync();
					ROAMINGPREFS.sync();
					if (playerSlot != null) {
						playerSlot.getPreferences().sync();
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}

				try {
					ROAMINGPREFS.exportSubtree(os);
					os.flush();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				} finally {
					if (os != null) {
						try {
							os.close();
						} catch (IOException e) {
							e.printStackTrace(System.err);
						}
					}
				}

			}
		}
	}

	public static String getRoamingPrefsFileName() {
		return game.doGetRoamingPrefsFileName();
	}

	protected String doGetRoamingPrefsFileName() {
		return getRoamingDirectoryPrefix() + DEFAULT_ROAMING_PREFS_FILENAME;
	}

	/**
	 * Open the redefine keys screen, if there is one.
	 */
	protected void doRedefineKeys() {
		BindingsScreen.show();
	}

	/**
	 * @return Returns the messageSequence.
	 */
	public int getMessageSequence() {
		return messageSequence;
	}

	/**
	 * Wraps alert messages
	 */
	public static void alert(String message) {
		Sys.alert(getDisplayTitle(), message);
	}

	/**
	 * Forces game to run & render even when not focused
	 * @param alwaysRun
	 */
	public static void setAlwaysRun(boolean alwaysRun) {
		Game.alwaysRun = alwaysRun;
	}

	public static boolean getDontCheckMessages() {
		return game.dontCheckMessages;
	}

	/**
	 * Is this a slot-managed game?
	 * @return true if we're using player slots
	 */
	public static boolean isSlotManaged() {
		return game.useSlotManagement;
	}

	/**
	 * Get the current player slot. If we're not slot managed, we return null. Also, if we're a slot
	 * managed game but there is no currently selected slot, we return null (and should probably prompt
	 * for a slot name)
	 * @return a PlayerSlot, or null
	 */
	public static PlayerSlot getPlayerSlot() {
		return playerSlot;
	}

	/**
	 * Sets the current player slot that we're using
	 * @param newSlot; may not be null
	 */
	public static void setPlayerSlot(PlayerSlot newSlot) {
		if (newSlot == null) {
			throw new IllegalArgumentException("newSlot may not be null");
		}
		Game.playerSlot = newSlot;
		ROAMINGPREFS.put("slot_"+getInternalVersion(), newSlot.getName());
		try {
			ROAMINGPREFS.sync();
		} catch (BackingStoreException e) {
			e.printStackTrace(System.err);
		}
		TitleScreen.updateSlotDetails();
		game.onSetPlayerSlot();
	}

	protected void onSetPlayerSlot() {}

	@Override
	protected void doCreate() {
		super.doCreate();

		// Make a note of all the sound players
		soundPlayers = Resources.list(SoundPlayer.class);
		if (soundPlayer != null) {
			soundPlayers.add(soundPlayer);
		}
	}

	public static int getMouseX() {
		return game.doGetMouseX();
	}

	public static int getMouseY() {
		return game.doGetMouseY();
	}

	protected int doGetMouseX() {
		return Mouse.getX();
	}

	protected int doGetMouseY() {
		return Mouse.getY();
	}

	/**
	 * Convert a logical (game coordinate) into a physical screen coordinate
	 * @param logicalX
	 * @return
	 */
	public static float logicalXtoPhysicalX(float logicalX) {
		return logicalX * viewPort.getWidth() / getWidth() + viewPort.getX();
	}

	/**
	 * Convert a logical (game coordinate) into a physical screen coordinate
	 * @param logicalY
	 * @return
	 */
	public static float logicalYtoPhysicalY(float logicalY) {
		return logicalY * viewPort.getHeight() / getHeight() + viewPort.getY();
	}

	/**
	 * Convert a physical screen coordinate into a game coordinate
	 * @param physicalX
	 * @return
	 */
	public static float physicalXtoLogicalX(float physicalX) {
		return getWidth() * (physicalX - viewPort.getX()) / viewPort.getWidth();
	}

	/**
	 * Convert a physical screen coordinate into a game coordinate
	 * @param physicalY
	 * @return
	 */
	public static float physicalYtoLogicalY(float physicalY) {
		return getHeight() * (physicalY - viewPort.getY()) / viewPort.getHeight();
	}

	/**
	 * @return the game's default scale (logical pixels : real pixels)
	 */
	public static int getScale() {
		return game.scale;
	}

	/**
	 * Called by the registration screen when it completely fails to connect to Puppygames, or the Puppygames server throws an unexpected
	 * Throwable. The game becomes temporarily registered.
	 */
	public static void onRegistrationDisaster() {
		if (LOCALPREFS.getBoolean("puppygames_exists", false)) {
			return;
		}
		Game.registered = true;
	}

	public static void onRegistrationRecovery() {
		Game.registered = false;
		LOCALPREFS.putBoolean("puppygames_exists", true);
		try {
			LOCALPREFS.flush();
		} catch (BackingStoreException e) {
		}
	}

	/**
	 * Flushes preferences (asynchronously)
	 */
	public static void flushPrefs() {
		if (prefsSaver != null) {
			prefsSaver.save();
		} else {
			doFlushPrefs();
		}
	}

	public static boolean isModded() {
		return modded;
	}

	/**
	 * @return the current mod name; or "", if none.
	 */
	public static String getModName() {
		return modName;
	}

	/**
	 * Load mod specified on commandline
	 */
	private static final void loadMods() {
		modName = "";

		String modPath = properties.getProperty("mods", "");
		if (modPath.equals("")) {
			return;
		}

		// "mods" points to the full paths of jar files with mods in them, loaded in order.
		StringTokenizer st = new StringTokenizer(modPath, File.pathSeparator, false);
		while (st.hasMoreTokens()) {
			String jarPath = st.nextToken().trim();
			if (jarPath.equals("")) {
				continue;
			}
			loadMod(jarPath, false);
		}
	}

	/**
	 * Load DLC found under the /dlc directory
	 */
	private static final void loadDLC() {
		File dlcDir = new File("dlc");
		if (!dlcDir.exists()) {
			return;
		}
		File[] dlc = dlcDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		if (dlc == null) {
			return;
		}
		for (File file : dlc) {
			String jarPath;
            try {
	            jarPath = file.getCanonicalPath();
				loadMod(jarPath, true);
            } catch (IOException e) {
            	System.err.println("failed to load DLC from jar "+file.getPath());
	            e.printStackTrace(System.err);
            }
		}
	}

	/**
	 * Load a mod or DLC
	 * @param jarPath Full path to a .jar file
	 * @param dlc Whether this is offical DLC or not
	 */
	private static final void loadMod(String jarPath, boolean dlc) {
		File file = new File(jarPath);
		if (!file.exists()) {
			System.err.println((dlc ? "DLC" : "Mod")+" at "+jarPath+" does not exist.");
			return;
		}

		try {
			System.out.println("Loading "+(dlc ? "DLC" : "mod")+" at "+jarPath);
			URLClassLoader cl = new URLClassLoader(new URL[] {file.toURI().toURL()});
			ClassLoaderResource clr = new ClassLoaderResource("mod.loader");
			clr.setClassLoader(cl);
			Resources.put(clr);
			if (cl.findResource("resources.dat") == null) {
				// Load from xml instead
				URL url = cl.getResource("resources.xml");
				ResourceConverter rc = new ResourceConverter(new ResourceLoadedListener() {
					@Override
					public void resourceLoaded(IResource loadedResource) {
						System.out.println("  Loaded "+loadedResource);
					}
				}, cl);
				rc.setOverwrite(true);
				rc.include(url.openStream());
			} else {
				URL url = cl.getResource("resources.dat");
				Resources.load(url.openStream());
			}

			// DLC doesn't affect the modded status of the game
			if (!dlc) {
				modded = true;
				if (modName.length() > 0) {
					modName += ",";
				}
				String n;
				if (Resources.exists("modName")) {
					n = ((TextResource) Resources.get("modName")).getText().trim();
					if (n.equals("")) {
						n = "unknown_mod";
					}
				} else {
					n = "unknown_mod";
				}
				modName += n;
				System.out.println("Successfully loaded mod "+n+" from "+jarPath);
			} else {
				String n = ((TextResource) Resources.get("modName")).getText().trim();
				System.out.println("Successfully loaded DLC "+n+" from "+jarPath);
			}
		} catch (Exception e) {
			System.err.println("Failed to load "+(dlc ? "DLC" : "mod")+" at "+jarPath);
			e.printStackTrace(System.err);
		}
	}

	/**
	 * @return the game's 2 character language
	 */
	public static String getLanguage() {
		return game.language;
	}

	/**
	 * Shortcut to get a bit of wrapped text
	 * @param key Points to a {@link Resource} implementing {@link TextWrapper}
	 * @return a String
	 */
	public static String getMessage(String key) {
		return ((TextWrapper) Resources.get(key)).getText();
	}

	/**
	 * @return true if we're using Steam
	 */
	public static boolean isUsingSteam() {
		return Game.appID != 0;
	}

	/**
	 * @return true if we're able to use the Steam cloud
	 */
	public static boolean isUsingSteamCloud() {
		return isUsingSteam() && Steam.isCreated() && Steam.isSteamRunning() && Steam.getRemoteStorage().isCloudEnabledForAccount() && Steam.getRemoteStorage().isCloudEnabledForApp();
	}

	/**
	 * @return the Steam app ID, if we're using steam
	 */
	public static int getSteamAppID() {
		return Game.appID;
	}

	/**
	 * Initialise Steam.
	 */
	private static void initSteam() {
		Steam.init(Game.appID);
	}

	/**
	 * @return the on-the-fly FPS counter
	 */
	public static int getFPS() {
	    return currentFPS;
    }
}
