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

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.rmi.Naming;
import java.text.*;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.puppygames.applet.effects.EffectFeature;
import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.screens.*;
import net.puppygames.gamecommerce.shared.*;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.*;
import org.lwjgl.openal.AL10;
import org.lwjgl.opengl.*;
import org.lwjgl.util.Rectangle;
import org.lwjgl.util.Timer;

import com.shavenpuppy.jglib.*;
import com.shavenpuppy.jglib.Image.JPEGDecompressor;
import com.shavenpuppy.jglib.jpeg.JPEGDecoder;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.openal.ALStream;
import com.shavenpuppy.jglib.resources.*;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sound.SoundPlayer;
import com.shavenpuppy.jglib.sprites.SoundCommand;
import com.shavenpuppy.jglib.util.CheckOnline;
import com.shavenpuppy.jglib.util.Util;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: Game.java,v 1.56 2010/10/17 21:04:01 foo Exp $ Abstract base class for a "game"
 *
 * @author $Author: foo $
 * @version $Revision: 1.56 $
 */
public abstract class Game extends Feature {

	/*
	 * Static game data
	 */

	private static final long serialVersionUID = 1L;

	private static final String RESTORE_GAME_DIALOG_FEATURE = "restore_game.dialog";
	private static final String SAVE_GAME_EFFECT_FEATURE = "save_game.effect";
	private static final String DEFAULT_GAME_RESOURCE_NAME = "game.puppygames";
	private static final String DEFAULT_USER_PREFS_FILENAME = "prefs.xml";

	public static final boolean DEBUG = false;
	private static final boolean REGISTERED = false;
	private static final boolean FORCEUSELOG = false;
	private static final boolean TESTREGISTER = false;

	/** Keydown state tracking */
	private static final boolean[] KEYDOWN = new boolean[Keyboard.KEYBOARD_SIZE];
	private static final boolean[] KEYWASDOWN = new boolean[Keyboard.KEYBOARD_SIZE];

	/** Restore filename */
	protected static final String RESTORE_FILE = "restore.dat";

	/** Force run even when not focused */
	private static boolean alwaysRun;

	/** Do the Buy page */
	private static boolean doBuy;

	/** Prevent the Buy page appearing */
	private static boolean preventBuy;

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
	private static long installation;

	/** Score group */
	private static String scoreGroup;

	/** Game preferences */
	private static Preferences PREFS, GLOBALPREFS;

	/** Configuration */
	private static Configuration configuration;

	/** Games this session */
	private static int playedThisSession;

	/** Instructions shown? */
	private static boolean shownInstructions;

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

	/** Local files directory prefix */
	private static String dirPrefix;

	/** Stash the local log here */
	private static String GAMEINFO_FILE;

	/** Ticks played so far */
	private static int playedTicks;

	/** Music volume 0..100 */
	private static int musicVolume;

	/** Effects volume 0..100 */
	private static int sfxVolume;

	/** Initial display mode */
	private static DisplayMode initialMode;

	/** Display bounds */
	private static Rectangle viewPort;

	/** Game state */
	private static GameState gameState;

	/** Allow saving the game */
	private static boolean allowSave = true;

	/** Top screen */
	public static Screen topScreen;

	/** Game title */
	private static String title;

	/** Internal title, used for local prefs */
	private static String internalTitle;

	/** Version */
	private static String version;

	/** Internal version, used for local settings */
	private static String internalVersion;

	/** Restore game dialog */
	private static DialogScreen restoreGameDialog;

	/** Current player slot */
	private static PlayerSlot playerSlot;

	/** Custom display mode */
	private static boolean customDisplayMode;

	/** Viewport offset */
	private static int viewportXoffset, viewportYoffset, viewportWidth, viewportHeight;

	/** Force sleep instead of yield */
	private static boolean forceSleep;

	/** Modded? */
	private static boolean modded;

	/** Mod name */
	private static String modName;

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

	/** Default to fullscreen */
	private int defaultFullscreen = 0;

	/** Don't allow remote hiscores */
	private boolean dontUseRemoteHiscores;

	/** Don't check for messages? */
	private boolean dontCheckMessages;

	/** Slot management */
	private boolean useSlotManagement;

	/** Sound voices */
	private int soundVoices = 64;

	/** Use variable window sizing */
	private boolean useWindowSizing;

	/** GUI scale in pixels */
	private int scale;

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

	/*
	 * Transient data
	 */

	/** Window size */
	private transient float windowSize;

	private transient int panic;

	private transient boolean wasGrabbed;

	private transient boolean submitRemoteHiscores;

	private transient int logicalWidth;

	private transient int logicalHeight;

	private transient boolean catchUp;

	private transient float masterGain, targetMasterGain;

	/** Prefs saver */
	public static PrefsSaverThread prefsSaver;

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
	 * @return Returns the game preferences.
	 */
	public static Preferences getPreferences() {
		return PREFS;
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
		File outFile = new File(dirPrefix + File.separator + fileName);
		if (outFile.exists()) {
			if (outFile.length() > 65535) {
				outFile.renameTo(new File(dirPrefix + File.separator + fileName + ".old"));
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

	/**
	 * Initialise the game. This must be called <strong>outside</strong> of the AWT thread!
	 * @param resourcesStream An InputStream for reading in a compiled resource data file, created by the JGLIB ResourceConverter
	 * tool.
	 * @throws Exception if the game fails to initialise correctly
	 */
	public static synchronized void init(Properties properties, InputStream resourcesStream) throws Exception {
		if (initialised) {
			return;
		}
		initialised = true;
		finished = false;

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

		System.out.println(new Date() + " Game: " + title + " " + version + " ["+internalVersion+"]");

		// Create preferences and determine / generate installation number
		GLOBALPREFS = Preferences.userNodeForPackage(Game.class);
		installation = GLOBALPREFS.getLong("installation", 0);
		if (installation == 0L) {
			installation = (long) (Math.random() * Long.MAX_VALUE);
			GLOBALPREFS.putLong("installation", installation);
		}
		System.out.println("Serial " + installation);


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

		// Load prefs from backup file
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		File prefsFile = new File(getUserPrefsFileName());
		boolean zapPrefs = false;
		if (prefsFile.exists()) {
			try {
				fis = new FileInputStream(prefsFile);
				bis = new BufferedInputStream(fis);
				Preferences.importPreferences(bis);
				System.out.println("Loaded preferences file "+prefsFile);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				zapPrefs = true;
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			System.out.println("Preferences file "+prefsFile+" does not exist.");
			zapPrefs = true;
		}
		PREFS = Preferences.userNodeForPackage(Game.class).node(title);
		if (zapPrefs) {
			PREFS.removeNode();
			PREFS.flush();
			PREFS = null;
			PREFS = Preferences.userNodeForPackage(Game.class).node(title);
			doFlushPrefs();
		}

		prefsSaver = new PrefsSaverThread();
		prefsSaver.start();


		// Load or create configuration
		byte[] cfg = PREFS.getByteArray("configuration", null);
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
		boolean wasBadExit = PREFS.getBoolean("badexit", false);

		// Initialise a gameinfo object so we can log things
		gameInfo = new GameInfo(getTitle(), getVersion(), getInstallation(), wasBadExit, org.lwjgl.opengl.Display.getAdapter(), org.lwjgl.opengl.Display.getVersion(), configuration.encode());

		System.out.println("Starting " + getTitle() + " " + getVersion());

		// If there was a bad exit, let's put up a message
		if (wasBadExit && !DEBUG) {
			Game.alert(getTitle() + " did not shut down correctly last time you tried to play.\n\nIf you are experiencing problems or bugs please " + ("".equals(getSupportEmail()) ? "visit " + getContactURL() : "contact " + getSupportEmail()) + "\nand tell us!");
			Support.doSupport("crash");
		}

		// Set the bad exit flag. The only way to clear it is to exit the game
		// cleanly via the exit() method.
		PREFS.putBoolean("badexit", true);
		flushPrefs();

		// Load mod if any is specified
		loadMods();

		DynamicResource.createAll();

		// Initialise sound
		initSound();

		// Gamepads... have to init first for LWJGL bug.
		try {
			Controllers.create();
		} catch (Exception e) {
			System.err.println("No gamepads or joysticks enabled due to " + e);
		}

		// Initialise display
		try {
			initDisplay();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			gameInfo.setException(e);
			Game.alert("You need to get new graphics card drivers in order to play " + getTitle() + ".\nPlease contact your system vendor for assistance.");
			Support.doSupport("opengl");
			exit();
		}

		// Show the splash screen
		Display.setVSyncEnabled(false);
		Splash splash = Splash.getInstance();
		if (splash != null) {
			try {
				splash.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				splash = null;
			}
		}

		// Autocreate features
		SFX.createSFX();
		Res.createResources();
		Feature.autoCreate();

		// We're in Run Mode now
		Resources.setRunMode(true);

		// Update gameinfo
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

		// Force sleep?
		forceSleep = properties.getProperty("sleep", Runtime.getRuntime().availableProcessors() > 1 ? "false" : "true").equalsIgnoreCase("true");

		// Finally, create the game
		game.create();

		// Load keyboard bindings
		loadBindings();

		// That's enough of the loading screen...
		if (splash != null) {
			splash.destroy();
			splash = null;
		}

		initVsync();

		// If slot managed, read current slot:
		if (isSlotManaged()) {
			String slotName = PREFS.get("slot_"+getInternalVersion(), null);
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

		// Show title screen or registration screen as appropriate
		if (!isRegistered() || TESTREGISTER) {
			showRegisterScreen();
		} else if (REGISTERED || game.preregistered) {
			preRegisteredStartup();
		} else {
			showTitleScreen();
		}

		// Remote hiscores?
		game.submitRemoteHiscores = PREFS.getBoolean("submitremotehiscores", !game.dontUseRemoteHiscores);

		// Now run!
		resourcesStream.close();
		resourcesStream = null;
		try {
			game.run();
		} catch (Throwable t) {
			System.err.println("Set exception to " + t + " : " + t.getMessage()+" Stack trace follows:");
			t.printStackTrace(System.err);
			gameInfo.setException(t);
		} finally {
			exit();
		}
	}

	/**
	 * Called instead of opening the title screen
	 */
	private static void preRegisteredStartup() {
		game.onPreRegisteredStartup();
	}

	protected void onPreRegisteredStartup() {
		// Default behaviour, just open the title screen
		showTitleScreen();
	}

	/**
	 * Get the local app settings dir
	 * @return String
	 */
	private static String getSettingsDir() {
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
	 * Get the directory prefix
	 * @return the directory prefix for "global" local settings, for all slots (includes trailing slash)
	 */
	public static String getDirectoryPrefix() {
		return dirPrefix;
	}

	/**
	 * Get the player directory prefix
	 * @return the directory prefix for the current player's "slot" local settings (includes trailing slash)
	 */
	public static String getPlayerDirectoryPrefix() {
		String ret = dirPrefix + "slots_" + getInternalVersion() + File.separator + playerSlot.getName();
		// Lazy creation
		File f = new File(ret);
		if (!f.exists()) {
			f.mkdirs();
		}
		return ret + File.separator;
	}

	/**
	 * Get the slot directory prefix
	 * @return the directory prefix for all the player slots (includes trailing slash)
	 */
	public static String getSlotDirectoryPrefix() {
		String ret = dirPrefix + "slots_" + getInternalVersion();
		// Lazy creation
		File f = new File(ret);
		if (!f.exists()) {
			f.mkdirs();
		}
		return ret + File.separator;
	}

	private static void initFiles() {
		boolean dirPrefixExists;
		String settingsDirName = getSettingsDir() + File.separator + "." + getTitle().replace(' ', '_').toLowerCase() + '_' + getInternalVersion();
		System.out.println("settingsDirName="+settingsDirName);
		File settingsDir = new File(settingsDirName);
		if (!settingsDir.exists()) {
			System.out.println("Creating settingsDir: "+settingsDirName);
			settingsDir.mkdirs();
			dirPrefixExists = settingsDir.exists();
		} else {
			dirPrefixExists = true;
		}
		dirPrefix = dirPrefixExists ? settingsDirName + File.separator : getSettingsDir() + File.separator;
		System.out.println("dirPrefix="+dirPrefix);
		GAMEINFO_FILE = dirPrefix + "log.dat";
	}

	/**
	 * Write tix out so we can count how long the player has played for
	 */
	private static void writeTix() {
		// Write tix
		int tix = PREFS.getInt("tix", 0);
		int newTix = playedTicks / getFrameRate();
		tix += newTix;
		playedTicks = 0;
		gameInfo.addTime(newTix);
		PREFS.putInt("tix", tix);
	}

	/**
	 * Call this every frame that the game is being played.
	 */
	public static void onTicked() {
		playedTicks++;
	}

	/**
	 * Write out the execution log. If we're online, then we'll read in the existing execution log (if any), append the current
	 * GameInfo, and send it to the server. If we're offline or unsuccessful we'll write it to disk for a rainy day. The log is
	 * deleted once it's sent successfully.
	 */
	@SuppressWarnings("unchecked")
	private static void writeLog() {
		if (gameInfo == null) {
			System.out.println("No game info log");
		}

		if (GAMEINFO_FILE == null) {
			System.out.println("Couldn't write log - no filename");
			return;
		}

		// Find and load any existing log
		File log = new File(getSettingsDir() + File.separator + GAMEINFO_FILE);
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
			writeTix();
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
	 * Clear the buy flag
	 */
	public static void clearBuy() {
		preventBuy = true;
	}

	/**
	 * Show the "More Games" URL
	 */
	public static void showMoreGames() {
		preventBuy = true;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					String page;
					PREFS.putBoolean("showregister", true);
					if (Resources.exists("moregames_url")) {
						TextResource tr = (TextResource) Resources.get("moregames_url");
						page = tr.getText();
					} else if (game.moreGamesURL != null && !"".equals(game.moreGamesURL)) {
						page = game.moreGamesURL;
					} else if (!System.getProperty("moregames_url", "!").equals("!")) {
						page = System.getProperty("moregames_url");
					} else {
						page = "http://" + getWebsite();
					}
					if (!Sys.openURL(page)) {
						throw new Exception("Failed to open URL "+page);
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
					Game.alert("Please open your web browser on the page http://" + getWebsite());
				}
			}
		});
		exit();
	}

	/**
	 * Buy the game
	 */
	public static void buy(boolean doExit) {
		if (!doBuy) {
			doBuy = true;
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					if (isRegistered() || preventBuy) {
						return;
					}
					PREFS.putBoolean("showregister", true);
					try {
						String page;
						if (Resources.exists("buy_url")) {
							TextResource tr = (TextResource) Resources.get("buy_url");
							page = tr.getText();
						} else if (game.buyURL != null && !"".equals(game.buyURL)) {
							page = game.buyURL;
						} else if (!System.getProperty("buy_url", "!").equals("!")) {
							page = System.getProperty("buy_url");
						} else {
							page = "http://" + getWebsite() + "/purchase/buy.php?game=" + URLEncoder.encode(getTitle(), "utf-8") + "&configuration=" + URLEncoder.encode(configuration.encode(), "utf-8") + "@installation@";
						}
						String replacement = "&installation=" + URLEncoder.encode(String.valueOf(installation), "utf-8");
						int idx = page.indexOf("@installation@");
						if (idx != -1) {
							StringBuilder sb = new StringBuilder(page);
							sb.replace(idx, idx + 14, replacement);
							page = sb.toString();
						}
						if (!Sys.openURL(page)) {
							throw new Exception("Failed to open URL "+page);
						}
					} catch (Exception e) {
						e.printStackTrace(System.err);
						Game.alert("Please open your web browser on the page http://" + getWebsite());
					}
				}
			});
		}
		if (doExit) {
			exit();
		}
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
	public static synchronized void exit() {
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
		if (PREFS != null) {
			PREFS.putBoolean("badexit", false);
		}
		if (prefsSaver != null) {
			prefsSaver.finish();
			prefsSaver = null;
		}

		// And nag :)
		if (game != null && !preventBuy) {
			buy(false);
		}

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
			if (music != null) {
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
			musicVolume = PREFS.getInt("musicvolume", 70);
			sfxVolume = PREFS.getInt("sfxvolume", 70);

			music = null;
		} catch (Exception e) {
			sfxEnabled = false;
			musicEnabled = false;
			e.printStackTrace();
			Game.alert("You need a sound card to to hear the sound effects and music in " + getTitle() + ".\n\nYou may have a suitable card but not have appropriate drivers.\n\nPlease contact " + getSupportEmail() + " for assistance or visit our website if you need help finding drivers for your sound card.");
			Support.doSupport("openal");
			if (org.lwjgl.openal.AL.isCreated()) {
				org.lwjgl.openal.AL.destroy();
			}
		}
	}

	/**
	 * Initialise the display
	 * @throws Exception if the display fails to initialise, probably because the graphics card has no OpenGL drivers
	 */
	private static void initDisplay() throws Exception {
		Display.setTitle(getTitle());
		if (Display.getParent() != null) {
			initialMode = new DisplayMode(Display.getParent().getWidth(), Display.getParent().getHeight());
			customDisplayMode = false;
		} else {
			if (properties.containsKey("width") && properties.containsKey("height")) {
				initialMode = new DisplayMode(Integer.parseInt(properties.getProperty("width", "800")), Integer.parseInt(properties.getProperty("height", "600")));
				customDisplayMode = true;
			} else {
				initialMode = Display.getDesktopDisplayMode();
				customDisplayMode = false;
			}
			if (properties.containsKey("vx")) {
				viewportXoffset = Integer.parseInt(properties.getProperty("vx", "0"));
			}
			if (properties.containsKey("vy")) {
				viewportYoffset = Integer.parseInt(properties.getProperty("vy", "0"));
			}
			if (properties.containsKey("vw")) {
				viewportWidth = Integer.parseInt(properties.getProperty("vw", "0"));
			}
			if (properties.containsKey("vh")) {
				viewportHeight = Integer.parseInt(properties.getProperty("vh", "0"));
			}
		}
		if ("!".equals(PREFS.get("fullscreen2", "!"))) {
			PREFS.putInt("fullscreen2", game.defaultFullscreen);
		}
		// Determine fullscreenness
		if (Boolean.getBoolean("net.puppygames.applet.Game.windowed")) {
			setFullscreen(false);
		} else {
			int fs = PREFS.getInt("fullscreen2", 0);
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
			if (fullscreen || customDisplayMode) {
				try {
					initFullscreen();
				} catch (LWJGLException e) {
					fullscreen = false;
					initWindow();
				}
			} else {
				initWindow();
			}

			PREFS.putInt("fullscreen2", fullscreen ? 2 : 1);
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

	private static int getRecommendedBPP() {
		return PREFS.getInt("recommendedbpp", initialMode.getBitsPerPixel());
	}

	/**
	 * Initialise fullscreen display (or a custom sized window)
	 */
	private static void initFullscreen() throws Exception {
		System.out.println("Initialising full screen display");

		// Use the desktop display mode
		if (customDisplayMode) {
			Display.setDisplayMode(initialMode);
		} else {
			Display.setDisplayModeAndFullscreen(Display.getDesktopDisplayMode());
		}
		if (Display.isCreated()) {
			Display.update();
		}
		System.out.println("Set fullscreen displaymode to " + Display.getDisplayMode());
		initVsync();
		if (!Display.isCreated()) {
			Display.create();
		}

		// Properly clear the entire display
		viewPort = new Rectangle(0, 0, Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight());
		resetViewport();
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		Display.update();

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		int displayWidth = viewportWidth == 0 ? Display.getDisplayMode().getWidth() : viewportWidth;
		int displayHeight = viewportHeight == 0 ? Display.getDisplayMode().getHeight() : viewportHeight;

		if (customDisplayMode || properties.containsKey("scale")) {
			int smallest = Math.min(displayWidth, displayHeight);
			int fits;
			if (game.useWindowSizing) {
				fits = smallest / game.scale;
			} else {
				fits = 1;
			}
			System.out.println("Scale "+properties.getProperty("scale", "!"));
			game.logicalWidth = (int) (displayWidth / Math.max(1.0, Float.parseFloat(properties.getProperty("scale", String.valueOf(fits)))));
			game.logicalHeight = (int) (displayHeight / Math.max(1.0, Float.parseFloat(properties.getProperty("scale", String.valueOf(fits)))));
			viewPort = new Rectangle(viewportXoffset, viewportYoffset, displayWidth, displayHeight);
		} else if (game.useWindowSizing) {
			// Now we want to scale the game up to be the biggest it can be in the display's smallest dimension, so that it is an
			// even multiple of the scale, and centre this viewport on the screen
			// Fit the scale into the smaller dimension, and scale the other dimension
			int ratio;
			if (displayWidth < displayHeight) {
				// Weirdy potrait orientation
				// Height is smallest.
				ratio = displayWidth / game.scale;
			} else {
				// Height is smallest.
				ratio = displayHeight / game.scale;
			}

			game.logicalWidth = displayWidth / ratio;
			game.logicalHeight = displayHeight / ratio;
			viewPort = new Rectangle(viewportXoffset, viewportYoffset, displayWidth, displayHeight);
		} else {
			// TODO: FIX ALL THIS SO IT USES MULTIPLES OF THE WIDTH OR HEIGHT
			game.logicalWidth = game.width;
			game.logicalHeight = game.height;
			int x, y, gw, gh;

			if ((float) displayWidth / (float) displayHeight > (float) getWidth() / (float) getHeight()) {
				// Fix height to the display, scale width accordingly
				gh = displayHeight;
				gw = (int) (game.width * (float) displayHeight / game.height);
			} else {
				// Fix width to the display, scale height accordingly
				gw = displayWidth;
				gh = (int) (game.height * (float) displayWidth / game.width);
			}
			System.out.println("Game scaled to " + gw + " x " + gh);
			x = (displayWidth - gw) / 2 + viewportXoffset;
			y = (displayHeight - gh) / 2 + viewportYoffset;
			viewPort = new Rectangle(x, y, gw, gh);
		}
		glFrustum(-game.logicalWidth / 64.0, game.logicalWidth / 64.0, -game.logicalHeight / 64.0, game.logicalHeight / 64.0, 8, 65536);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		resetViewport();
		Display.update();

		Screen.onGameResized();
	}

	/**
	 * Initialise windowed display
	 */
	private static void initWindow() throws Exception {
		System.out.println("Initialising window");
		Display.setFullscreen(false);
		Display.setVSyncEnabled(false);
		if (game.useWindowSizing) {
			// Initialise width if we've not yet got window size
			if (game.windowSize == 0.0f) {
				setWindowSize(2.0f);
				return;
			}
			Display.setDisplayMode(new DisplayMode(game.width, game.height));
		} else if (initialMode.getWidth() > getWidth() * 3.125 && initialMode.getHeight() > getHeight() * 3.125) {
			Display.setDisplayMode(new DisplayMode(game.width * 3, game.height * 3));
		} else if (initialMode.getWidth() > getWidth() * 2.25 && initialMode.getHeight() > getHeight() * 2.25) {
			Display.setDisplayMode(new DisplayMode(game.width * 2, game.height * 2));
		} else {
			Display.setDisplayMode(new DisplayMode(game.width, game.height));
		}

		if (!Display.isCreated()) {
			Display.create();
		}

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		if (game.useWindowSizing) {
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
		glFrustum(-game.logicalWidth / 64.0, game.logicalWidth / 64.0, -game.logicalHeight / 64.0, game.logicalHeight / 64.0, 8, 65536);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		viewPort = new Rectangle(0, 0, Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight());
		resetViewport();
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		Screen.onGameResized();
		System.out.println("Window sized to "+Display.getDisplayMode());
	}

	/**
	 * Reset the game viewport
	 */
	public static void resetViewport() {
		System.out.println("Set viewport to "+viewPort);
		GL11.glViewport(viewPort.getX(), viewPort.getY(), viewPort.getWidth(), viewPort.getHeight());
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
			if (PREFS.getLong("preregistered-installation", 0L) == generatePregregistrationKey()) {
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
							PREFS.putLong("preregistered-installation", generatePregregistrationKey());
							registered = true;
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
		PREFS.putByteArray("configuration", cfg);
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
		int ticksToDo = 1;
		long then = Sys.getTime() & 0x7FFFFFFFFFFFFFFFL;
		long framesTicked = 0;
		long timerResolution = Sys.getTimerResolution();
		while (!finished) {
			if (Display.isCloseRequested()) {
				// Check for O/S close requests
				exit();
			} else if (Display.isActive() || alwaysRun) {
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
					if (ticksToDo > 5) {
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
					framesTicked += ticksToDo;
					render();
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
				targetMasterGain = 1.0f;

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
				AL10.alListenerf(AL10.AL_GAIN, masterGain);
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
	 * @return true if the demo expired
	 */
	public static boolean isDemoExpired() {
		return game.doIsDemoExpired();
	}

	protected boolean doIsDemoExpired() {
		if (isRegistered()) {
			return false;
		}

		int played = PREFS.getInt("played" + getVersion(), 0);
		played ^= 0xAF6AD755;
		played = played >> 16 & 0xFFFF | played << 16;
		played ^= 0xCCCCCABE;

		if (played == 0x1B9965D4) {
			played = 0;
		}
		int tix = PREFS.getInt("tix", 0);
		return played < 0 || (played > configuration.getMaxGames() && configuration.getMaxGames() > 0 || configuration.getMaxGames() == 0) && (tix > configuration.getMaxTime() && configuration.getMaxTime() > 0 || configuration.getMaxTime() == 0);
	}

	public static boolean maybeShowHelp() {
		return game.doMaybeShowHelp();
	}

	/**
	 * @return true if help is shown; false if you just want to start the game
	 */
	protected boolean doMaybeShowHelp() {
		showHelp();
		return true;
	}

	/**
	 * Begin a new game
	 */
	public static void beginNewGame() {
		System.out.println("Begin new game");
		// If demo version, count down the number of plays
		if (!isRegistered()) {
			// If not played before and not registered, open help first
			if (!shownInstructions && maybeShowHelp()) {
				return;
			}

			int played = PREFS.getInt("played" + getVersion(), 0);
			played ^= 0xAF6AD755;
			played = played >> 16 & 0xFFFF | played << 16;
			played ^= 0xCCCCCABE;

			if (played == 0x1B9965D4) {
				played = 0;
			}
			int tix = PREFS.getInt("tix", 0);
			System.out.println("You have played " + getTitle() + " " + played + " times for " + tix / 60 + " minutes");
			System.out.println("Max games " + configuration.getMaxGames() + " / max time " + configuration.getMaxTime() / 60 + " / max level " + configuration.getMaxLevel());
			if (isDemoExpired() && (configuration.isCrippled() || playedThisSession > 0)) {
				// Nag and quit on second game this session
				NagScreen.show("Your demo has expired!", true);
				return;
			} else {
				played++;
				played ^= 0xCCCCCABE;
				played = played >> 16 & 0xFFFF | played << 16;
				played ^= 0xAF6AD755;
				PREFS.putInt("played" + getVersion(), played);
				gameInfo.onNewGame();
				playedThisSession++;
			}
		}
		SFX.newGame();

		game.onBeginNewGame();
	}

	/**
	 * Called to begin a new game.
	 */
	protected void onBeginNewGame() {
		if (isRestoreAvailable()) {
			// Ask to resume
			restoreGame();
		} else {
			cleanGame();
		}
	}

	/**
	 * Game over
	 */
	public static void gameOver() {
		SFX.gameOver();
		game.doGameOver();
	}

	/**
	 * Game over. This should put the game in a "dormant" state and display the "game over" message to the player. After a while
	 * someone will call endGame() and put us back on the title screen.
	 */
	protected abstract void doGameOver();

	/**
	 * End the game and return to the title screen (or the hiscore entry screen)
	 */
	public static void endGame() {
		writeTix();
		game.doEndGame();
	}

	/**
	 * End the game and return to the title screen (or the hiscore entry screen). The default is simply to return to the title
	 * screen.
	 */
	protected void doEndGame() {
		showTitleScreen();
	}

	/**
	 * Show the options screen (if any)
	 */
	public static void showOptions() {
		game.doShowOptions();
	}

	/**
	 * Show the credits screen (if any)
	 */
	public static void showCredits() {
		game.doShowCredits();
	}

	/**
	 * Show credits screen (if any).
	 */
	protected void doShowCredits() {
		CreditsScreen.show();
	}

	/**
	 * Show the help screen (if any)
	 */
	public static void showHelp() {
		shownInstructions = true;
		game.doShowHelp();
	}

	/**
	 * Show help screen (if any).
	 */
	protected void doShowHelp() {
		InstructionsScreen.show();
	}

	/**
	 * Show the title screen (if any)
	 */
	public static void showTitleScreen() {
		game.doShowTitleScreen();
	}

	/**
	 * Show the registration screen (if any)
	 */
	public static void showRegisterScreen() {
		game.doShowRegisterScreen();
	}

	/**
	 * Show the registration screen (if any)
	 */
	protected void doShowRegisterScreen() {
		RegisterScreen.show();
	}

	/**
	 * Show the title screen (if any)
	 */
	protected void doShowTitleScreen() {
		TitleScreen.show();
	}

	/**
	 * Show options screen (if any). Default is do nothing.
	 */
	protected void doShowOptions() {
	}

	/**
	 * Show the hiscores screen (if any)
	 */
	public static void showHiscores() {
		game.doShowHiscores();
	}

	/**
	 * Show hiscores screen (if any).
	 */
	protected void doShowHiscores() {
		HiscoresScreen.show(null);
	}

	/**
	 * Tick. Called every frame.
	 */
	private void tick() {
		// Process key & mouse bindings
		Binding.poll();

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
			// Tick screens
			Screen.tickAllScreens();
			// Custom ticking
			doTick();
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
				Mouse.setGrabbed(false);
				Display.setTitle(getTitle() + " [PAUSED]");
				game.onPaused();
			} else {
				Mouse.setGrabbed(game.wasGrabbed);
				Display.setTitle(getTitle());
				game.onResumed();
			}
		}
	}

	private final void doTick() {
		doGameTick();
	}

	protected abstract void doGameTick();

	protected void onPaused() {
	}

	protected void onResumed() {
	}

	/**
	 * Render. Called every frame.
	 */
	private void render() {
		Screen.updateAllScreens();

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
	 * @return Returns the scoreGroup.
	 */
	public static String getScoreGroup() {
		return scoreGroup;
	}

	/**
	 * @param scoreGroup The scoreGroup to set.
	 */
	public static void setScoreGroup(String scoreGroup) {
		Game.scoreGroup = scoreGroup;
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
		PREFS.putBoolean("online", true);
	}

	/**
	 * Are remote calls allowed?
	 * @return boolean
	 */
	public static boolean isRemoteCallAllowed() {
		return PREFS.getBoolean("online", false);
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
		PREFS.putInt("musicvolume", musicVolume);
	}

	/**
	 * Set sfx volume
	 * @param volume 0.0f...1.0f
	 */
	public static void setSFXVolume(float vol) {
		sfxVolume = (int) Math.max(0.0f, Math.min(100.0f, vol * 100.0f));
		PREFS.putInt("sfxvolume", sfxVolume);
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
	 * Load the keyboard and mouse bindings, if possible. Fails silently.
	 */
	public static void loadBindings() {
		try {
			byte[] b = PREFS.getByteArray("bindings", null);
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
			PREFS.putByteArray("bindings", baos.toByteArray());
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
	 * Save the game
	 */
	public static void saveGame() {
		game.doSaveGame();
	}

	protected String getSaveGameRegistryMagicLocation() {
		return "tox";
	}

	protected void doSaveGame() {
		if (!allowSave) {
			return;
		}

		File file = game.getRestoreFile();

		try {
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);

			// Set current magic number
			gameState.setMagic(new Random().nextLong());
			if (playerSlot != null) {
				playerSlot.getPreferences().putLong(getSaveGameRegistryMagicLocation(), gameState.getMagic());
			} else {
				PREFS.putLong(getSaveGameRegistryMagicLocation(), gameState.getMagic());
			}
			// This restore file is now only valid when tox in prefs is the
			// same as that in the file. As soon as we do a restore, we zap
			// the tox value :)
			oos.writeObject(gameState);
			oos.flush();
			fos.close();
			allowSave = false;

			onGameSaved();
			flushPrefs();

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		TitleScreen.show();
	}

	/**
	 * Called when the game has been saved. Use it to pop up some sort of confirmation on the title screen
	 */
	protected void onGameSaved() {
		((EffectFeature) Resources.get(SAVE_GAME_EFFECT_FEATURE)).spawn(TitleScreen.getInstance());
	}

	/**
	 * Is there a game to restore?
	 * @return boolean
	 */
	public static boolean isRestoreAvailable() {
		return game.getRestoreFile().exists();
	}

	/**
	 * Flushes all preferences (synchronously)
	 */
	private static void doFlushPrefs() {
		if (GLOBALPREFS != null) {
			synchronized (GLOBALPREFS) {
				try {
					GLOBALPREFS.sync();
					PREFS.sync();
					if (playerSlot != null) {
						playerSlot.getPreferences().sync();
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}

				// Now write backup file
				FileOutputStream fos = null;
				BufferedOutputStream bos = null;
				File prefsFile = new File(getUserPrefsFileName());
				try {
					fos = new FileOutputStream(prefsFile);
					bos = new BufferedOutputStream(fos);
					PREFS.exportSubtree(bos);
					bos.flush();
					System.out.println("Saved preferences file "+prefsFile);
				} catch (Exception e) {
					e.printStackTrace(System.err);
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace(System.err);
						}
					}
				}

			}
		}
	}

	public static String getUserPrefsFileName() {
		return game.doGetUserPrefsFileName();
	}

	protected String doGetUserPrefsFileName() {
		return getDirectoryPrefix() + DEFAULT_USER_PREFS_FILENAME;
	}

	/**
	 * Start a new game from scratch
	 */
	public static void cleanGame() {
		File file = game.getRestoreFile();
		if (file.exists() && !file.delete()) {
			System.err.println("Failed to delete save file "+file);
		}
		allowSave = true;
		game.onCleanGame();
	}

	/**
	 * Called to start a new game from scratch after deleting the restore file
	 */
	protected void onCleanGame() {
		setGameState(createGameState());
		gameState.init();
	}

	/**
	 * @return the File which we use to save games to
	 */
	protected File getRestoreFile() {
		if (playerSlot != null) {
			return new File(getPlayerDirectoryPrefix() + RESTORE_FILE);
		} else {
			return new File(getDirectoryPrefix() + RESTORE_FILE);
		}
	}

	/**
	 * Factory method to create a GameState
	 * @return a GameState
	 */
	protected abstract GameState createGameState();

	/**
	 * Restore the game
	 */
	public static void restoreGame() {
		game.onRestoreGame();
	}

	/**
	 * Asks the user if they want to restore their game, and then either restores it, starts a clean game, or does nothing.
	 */
	protected void onRestoreGame() {
		if (!isRestoreAvailable()) {
			return;
		}
		topScreen = Screen.getTopScreen();
		if (topScreen == null) {
			return;
		}
		try {
			restoreGameDialog = (DialogScreen) Resources.get(RESTORE_GAME_DIALOG_FEATURE);
			restoreGameDialog.doModal("RESTORE GAME", "WOULD YOU LIKE TO RESTORE YOUR SAVED GAME?", new Runnable() {
				@Override
				public void run() {
					topScreen.setEnabled(true);
					switch (restoreGameDialog.getOption()) {
						case DialogScreen.YES_OPTION:
							doRestoreGame();
							break;
						case DialogScreen.NO_OPTION:
							cleanGame();
							break;
						default:
							// Do nothing
							break;
					}
					restoreGameDialog = null;
				}
			});
			topScreen.setEnabled(false);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			game.onRestoreGameFailed(e);
		}
	}

	/**
	 * Called when a restore game failed
	 */
	protected void onRestoreGameFailed(Exception e) {
	}

	/**
	 * Restore the game
	 */
	protected void doRestoreGame() {
		allowSave = true;
		File file = getRestoreFile();
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		ObjectInputStream ois = null;
		boolean exceptionOccurred = false;
		try {
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis);
			ois = new ObjectInputStream(bis);
			setGameState((GameState) ois.readObject());
			// Check tox value in prefs...
			long tox;
			if (playerSlot != null) {
				tox = playerSlot.getPreferences().getLong(getSaveGameRegistryMagicLocation(), 0L);
			} else {
				tox = PREFS.getLong(getSaveGameRegistryMagicLocation(), 0L);
			}
			if (!DEBUG && tox != gameState.getMagic()) {
				throw new Exception("Invalid game state");
			}
			Resources.dequeue();
			gameState.reinit();
		} catch (Exception e) {
			exceptionOccurred = true;
			e.printStackTrace(System.err);
			onRestoreGameFailed(e);
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
			if (!Game.DEBUG) {
				// Vape tox value
				if (playerSlot != null) {
					playerSlot.getPreferences().putLong("tox", new Random().nextLong());
				} else {
					PREFS.putLong("tox", new Random().nextLong());
				}
				flushPrefs();

				// Now delete the file whatever happens. If an exception occurs, rename it instead.
				if (exceptionOccurred) {
					File newFile = new File(file.getPath()+".broken");
					if (!file.renameTo(newFile)) {
						System.err.println("Failed to rename "+file+" to "+newFile);
					}
				} else if (!file.delete()) {
					System.err.println("Failed to delete save file");
				}
			}
		}
	}

	/**
	 * Set game state
	 * @param newgameState
	 */
	protected void setGameState(GameState newGameState) {
		Game.gameState = newGameState;
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
		Sys.alert(getTitle(), message);
	}

	/**
	 * Forces game to run & render even when not focused
	 * @param alwaysRun
	 */
	public static void setAlwaysRun(boolean alwaysRun) {
		Game.alwaysRun = alwaysRun;
	}

	/**
	 * @return the dontUseRemoteHiscores
	 */
	public static boolean getDontUseRemoteHiscores() {
		return game.dontUseRemoteHiscores;
	}

	public static boolean getDontCheckMessages() {
		return game.dontCheckMessages;
	}

	public static boolean getSubmitRemoteHiscores() {
		return !game.dontUseRemoteHiscores && game.submitRemoteHiscores;
	}

	public static void setSubmitRemoteHiscores(boolean set) {
		game.submitRemoteHiscores = set;
		synchronized (PREFS) {
			PREFS.putBoolean("submitremotehiscores", !game.dontUseRemoteHiscores && game.submitRemoteHiscores);
		}
		new Thread() {
			@Override
			public void run() {
				try {
					synchronized (PREFS) {
						PREFS.flush();
					}
				} catch (BackingStoreException e) {
					e.printStackTrace(System.err);
				}
			}
		}.start();
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
		PREFS.put("slot_"+getInternalVersion(), newSlot.getName());
		try {
			PREFS.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace(System.err);
		}
		TitleScreen.updateSlotDetails();
		game.onSetPlayerSlot();
	}

	protected void onSetPlayerSlot() {}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doCreate()
	 */
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

	public static void setWindowSize(float windowSize) {
		float oldSize = game.windowSize;
		try {
			if (game.windowSize != windowSize || Display.isFullscreen() || customDisplayMode) {
				game.windowSize = windowSize;
				if (initialMode.getWidth() > initialMode.getHeight()) {
					// Normal landscape mode
					setSize((int) (initialMode.getWidth() * game.scale * windowSize / initialMode.getHeight()), (int) (game.scale * windowSize));
				} else {
					// Wierdy portrait mode
					setSize((int) (game.scale * windowSize), (int) (initialMode.getHeight() * game.scale * windowSize / initialMode.getWidth()));
				}
			}
		} catch (Exception e) {
			game.windowSize = oldSize;
		}
	}

	public static float getWindowSize() {
		return game.windowSize;
	}

	public static int getScale() {
		return game.scale;
	}

	public static boolean getUseWindowSizing() {
		return game.useWindowSizing;
	}

	/**
	 * Called by the registration screen when it completely fails to connect to Puppygames, or the Puppygames server throws an unexpected
	 * Throwable. The game becomes temporarily registered.
	 */
	public static void onRegistrationDisaster() {
		if (PREFS.getBoolean("puppygames_exists", false)) {
			return;
		}
		Game.registered = true;
	}

	public static void onRegistrationRecovery() {
		Game.registered = false;
		PREFS.putBoolean("puppygames_exists", true);
		try {
			PREFS.flush();
		} catch (BackingStoreException e) {
		}
	}

	/**
	 * @return true if we passed in width and height on commandline for a custom display resolution
	 */
	public static boolean isCustomDisplayMode() {
		return customDisplayMode;
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
			File file = new File(jarPath);
			if (!file.exists()) {
				System.err.println("Mod at "+jarPath+" does not exist.");
				continue;
			}

			try {
				System.out.println("Loading mod at "+jarPath);
				URLClassLoader cl = new URLClassLoader(new URL[] {file.toURI().toURL()});
				ClassLoaderResource clr = new ClassLoaderResource("mod.loader");
				clr.setClassLoader(cl);
				Resources.put(clr);
				if (cl.findResource("resources.dat") == null) {
					// Load from xml instead
					URL url = cl.getResource("resources.xml");
					ResourceConverter rc = new ResourceConverter(new ResourceLoadedListener() {
						@Override
						public void resourceLoaded(Resource loadedResource) {
							System.out.println("  Loaded "+loadedResource);
						}
					}, cl);
					rc.setOverwrite(true);
					rc.include(url.openStream());
				} else {
					URL url = cl.getResource("resources.dat");
					Resources.load(url.openStream());
				}
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
			} catch (Exception e) {
				System.err.println("Failed to load mod at "+jarPath);
				e.printStackTrace(System.err);
			}
		}
	}
}
