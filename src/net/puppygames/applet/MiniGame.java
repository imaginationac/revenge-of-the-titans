/**
 *
 */
package net.puppygames.applet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.prefs.BackingStoreException;

import net.puppygames.applet.effects.EffectFeature;
import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.screens.CreditsScreen;
import net.puppygames.applet.screens.DialogScreen;
import net.puppygames.applet.screens.HiscoresScreen;
import net.puppygames.applet.screens.InstructionsScreen;
import net.puppygames.applet.screens.NagScreen;
import net.puppygames.applet.screens.RegisterScreen;
import net.puppygames.applet.screens.SignUpScreen;
import net.puppygames.applet.screens.TitleScreen;
import net.puppygames.applet.screens.UnlockBonusScreen;
import net.puppygames.gamecommerce.shared.NewsletterIncentive;

import org.lwjgl.Sys;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.TextResource;

/**
 * Puppygames MiniGames, which has basic state management for our simple arcade games.
 */
public abstract class MiniGame extends Game {

	private static final boolean TESTSIGNUP = true;
	private static final boolean IGNORESIGNUP = false;

	private static final String RESTORE_GAME_DIALOG_FEATURE = "restore_game.dialog";
	private static final String SAVE_GAME_EFFECT_FEATURE = "save_game.effect";

	/** Games this session */
	private static int playedThisSession;

	/** Instructions shown? */
	private static boolean shownInstructions;

	/** Ticks played so far */
	private static int playedTicks;

	/** Prevent the Buy page appearing */
	private static boolean preventBuy;

	/** Do the Buy page */
	private static boolean doBuy;

	/** Game state */
	private static GameState gameState;

	/** Allow saving the game */
	private static boolean allowSave = true;

	/** Restore game dialog */
	private static DialogScreen restoreGameDialog;

	/** Top screen */
	private static Screen topScreen;

	/** Score group */
	private static String scoreGroup;

	/** Prize to be redeemed on starting a new game */
	private static PrizeFeature prize;

	/*
	 * Resource data
	 */

	/** Don't allow remote hiscores */
	private boolean dontUseRemoteHiscores;


	/*
	 * Transient data
	 */

	/** Whether to submit hiscores */
	private transient boolean submitRemoteHiscores;

	/**
     * Begin a new game
     */
    public static void beginNewGame() {
    	System.out.println("Begin new game");
    	// If demo version, count down the number of plays
    	if (!Game.isRegistered()) {
    		// If not played before and not registered, open help first
    		if (!shownInstructions && MiniGame.maybeShowHelp()) {
    			return;
    		}

    		int played = getLocalPreferences().getInt("played" + Game.getVersion(), 0);
    		played ^= 0xAF6AD755;
    		played = played >> 16 & 0xFFFF | played << 16;
    		played ^= 0xCCCCCABE;

    		if (played == 0x1B9965D4) {
    			played = 0;
    		}
    		int tix = getLocalPreferences().getInt("tix", 0);
    		System.out.println("You have played " + Game.getTitle() + " " + played + " times for " + tix / 60 + " minutes");
    		System.out.println("Max games " + Game.getConfiguration().getMaxGames() + " / max time " + Game.getConfiguration().getMaxTime() / 60 + " / max level " + Game.getConfiguration().getMaxLevel());
    		if (MiniGame.isDemoExpired() && (Game.getConfiguration().isCrippled() || playedThisSession > 0)) {
    			// Nag and quit on second game this session
    			NagScreen.show("Your demo has expired!", true);
    			return;
    		} else {
    			played++;
    			played ^= 0xCCCCCABE;
    			played = played >> 16 & 0xFFFF | played << 16;
    			played ^= 0xAF6AD755;
    			getLocalPreferences().putInt("played" + Game.getVersion(), played);
    			flushPrefs();
    			Game.getGameInfo().onNewGame();
    			playedThisSession++;
    		}
    	}
    	SFX.newGame();

    	getGame().onBeginNewGame();
    }

    public static MiniGame getGame() {
    	return (MiniGame) Game.getGame();
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
    				if (Game.isRegistered() || preventBuy) {
    					return;
    				}
    				Game.getLocalPreferences().putBoolean("showregister", true);
    				try {
    					String page;
    					if (Resources.exists("buy_url")) {
    						TextResource tr = (TextResource) Resources.get("buy_url");
    						page = tr.getText();
    					} else if (Game.getBuyURL() != null && !"".equals(Game.getBuyURL())) {
    						page = Game.getBuyURL();
    					} else if (!System.getProperty("buy_url", "!").equals("!")) {
    						page = System.getProperty("buy_url");
    					} else {
    						page = "http://" + Game.getWebsite() + "/purchase/buy.php?game=" + URLEncoder.encode(Game.getTitle(), "utf-8") + "&configuration=" + URLEncoder.encode(Game.configuration.encode(), "utf-8") + "@installation@";
    					}
    					String replacement = "&installation=" + URLEncoder.encode(String.valueOf(Game.installation), "utf-8");
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
    					Game.alert("Please open your web browser on the page http://" + Game.getWebsite());
    				}
    			}
    		});
    	}
    	if (doExit) {
    		Game.exit();
    	}
    }

	/**
     * Clear the buy flag
     */
    public static void clearBuy() {
    	preventBuy = true;
    }

	/**
     * End the game and return to the title screen (or the hiscore entry screen)
     */
    public static void endGame() {
    	getGame().updateLog();
    	getGame().doEndGame();
    }

	/**
     * Game over
     */
    public static void gameOver() {
    	SFX.gameOver();
    	getGame().doGameOver();
    }

	/**
     * @return true if the demo expired
     */
    public static boolean isDemoExpired() {
    	return getGame().doIsDemoExpired();
    }

	/**
     * Is there a game to restore?
     * @return boolean
     */
    public static boolean isRestoreAvailable() {
    	return new RoamingFile(getGame().getRestoreFile()).exists();
    }

	public static boolean maybeShowHelp() {
    	return getGame().doMaybeShowHelp();
    }

	/**
     * Call this every frame that the game is being played.
     */
    public static void onTicked() {
    	playedTicks++;
    }

	/**
     * Restore the game
     */
    public static void restoreGame() {
    	getGame().onRestoreGame();
    }

	/**
     * Save the game
     */
    public static void saveGame() {
    	getGame().doSaveGame();
    }

	/**
     * Show the credits screen (if any)
     */
    public static void showCredits() {
    	getGame().doShowCredits();
    }

	/**
     * Show the help screen (if any)
     */
    public static void showHelp() {
    	shownInstructions = true;
    	getGame().doShowHelp();
    }

	/**
     * Show the hiscores screen (if any)
     */
    public static void showHiscores() {
    	getGame().doShowHiscores();
    }

	/**
     * Show the "More Games" URL
     */
    public static void showMoreGames() {
    	new Thread() {
    		@Override
    		public void run() {
    			try {
    				String page;
    				getLocalPreferences().putBoolean("showregister", true);
    				if (Resources.exists("moregames_url")) {
    					TextResource tr = (TextResource) Resources.get("moregames_url");
    					page = tr.getText();
    				} else if (getMoreGamesURL() != null && !"".equals(getMoreGamesURL())) {
    					page = getMoreGamesURL();
    				} else if (!System.getProperty("moregames_url", "!").equals("!")) {
    					page = System.getProperty("moregames_url");
    				} else {
    					page = "http://" + Game.getWebsite();
    				}
    				if (!Sys.openURL(page)) {
    					throw new Exception("Failed to open URL "+page);
    				}
    			} catch (Exception e) {
    				e.printStackTrace(System.err);
    				Game.alert("Please open your web browser on the page http://" + Game.getWebsite());
    			}
    		}
    	}.start();
    }

	/**
     * Show the options screen (if any)
     */
    public static void showOptions() {
    	getGame().doShowOptions();
    }

	/**
     * Show the registration screen (if any)
     */
    public static void showRegisterScreen() {
    	getGame().doShowRegisterScreen();
    }

	/**
     * Show the title screen (if any)
     */
    public static void showTitleScreen() {
    	getGame().doShowTitleScreen();
    }

	/**
     * Write tix out so we can count how long the player has played for
     */
    @Override
    protected void updateLog() {
    	// Write tix
    	int tix = getLocalPreferences().getInt("tix", 0);
    	int newTix = playedTicks / getFrameRate();
    	tix += newTix;
    	playedTicks = 0;
    	getGameInfo().addTime(newTix);
    	getLocalPreferences().putInt("tix", tix);
    	flushPrefs();
    }

	/**
	 * C'tor
	 * @param name
	 */
	public MiniGame(String name) {
		super(name);
	}

	/**
	 * End the game and return to the title screen (or the hiscore entry screen). The default is simply to return to the title
	 * screen.
	 */
	protected void doEndGame() {
		MiniGame.showTitleScreen();
	}

	/**
	 * Game over. This should put the game in a "dormant" state and display the "game over" message to the player. After a while
	 * someone will call endGame() and put us back on the title screen.
	 */
	protected abstract void doGameOver();

	protected boolean doIsDemoExpired() {
		if (isRegistered()) {
			return false;
		}

		int played = getLocalPreferences().getInt("played" + getVersion(), 0);
		played ^= 0xAF6AD755;
		played = played >> 16 & 0xFFFF | played << 16;
		played ^= 0xCCCCCABE;

		if (played == 0x1B9965D4) {
			played = 0;
		}
		int tix = getLocalPreferences().getInt("tix", 0);
		return played < 0 || (played > configuration.getMaxGames() && configuration.getMaxGames() > 0 || configuration.getMaxGames() == 0) && (tix > configuration.getMaxTime() && configuration.getMaxTime() > 0 || configuration.getMaxTime() == 0);
	}

	/**
	 * @return true if help is shown; false if you just want to start the game
	 */
	protected boolean doMaybeShowHelp() {
		MiniGame.showHelp();
		return true;
	}

	/**
	 * Show credits screen (if any).
	 */
	protected void doShowCredits() {
		CreditsScreen.show();
	}

	/**
	 * Show help screen (if any).
	 */
	protected void doShowHelp() {
		InstructionsScreen.show();
	}

	/**
	 * Show hiscores screen (if any).
	 */
	protected void doShowHiscores() {
		HiscoresScreen.show(null);
	}

	/**
	 * Show options screen (if any). Default is do nothing.
	 */
	protected void doShowOptions() {
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
	 * Called to begin a new game.
	 */
	protected void onBeginNewGame() {
		if (MiniGame.isRestoreAvailable()) {
			// Ask to resume
			MiniGame.restoreGame();
		} else {
			cleanGame();
		}
	}

	@Override
	protected void onExit() {
		// And nag :)
		if (getGame() != null && !preventBuy) {
			buy(false);
		}

	}

	/**
	 * Restore the game
	 */
	protected void doRestoreGame() {
		allowSave = true;
		String file = getRestoreFile();
		GameInputStream gis = null;
		ObjectInputStream ois = null;
		boolean exceptionOccurred = false;
		try {
			gis = new GameInputStream(file);
			ois = new ObjectInputStream(gis);
			setGameState((GameState) ois.readObject());
			// Check tox value in prefs...
			long tox;
			if (getPlayerSlot() != null) {
				tox = getPlayerSlot().getPreferences().getLong(getSaveGameRegistryMagicLocation(), 0L);
			} else {
				tox = getRoamingPreferences().getLong(getSaveGameRegistryMagicLocation(), 0L);
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
			if (gis != null) {
				try {
					gis.close();
				} catch (Exception e) {
				}
			}
			if (!Game.DEBUG) {
				// Vape tox value
				if (getPlayerSlot() != null) {
					getPlayerSlot().getPreferences().putLong("tox", new Random().nextLong());
				} else {
					getRoamingPreferences().putLong("tox", new Random().nextLong());
				}
				flushPrefs();

				// Now delete the file whatever happens. If an exception occurs, rename it instead, if we can, or delete it, if we
				// can't
				if (exceptionOccurred) {
					if (isUsingSteamCloud()) {
						// Nothing we can do with Steam.
						if (!new RoamingFile(file).delete()) {
							System.err.println("Failed to delete save file");
						}
					} else {
						File newFile = new File(file+".broken");
						if (!new File(file).renameTo(newFile)) {
							System.err.println("Failed to rename "+file+" to "+newFile);
							if (!new RoamingFile(file).delete()) {
								System.err.println("Failed to delete save file");
							}
						}
					}
				} else if (!new RoamingFile(file).delete()) {
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
		MiniGame.gameState = newGameState;
	}

	/**
	 * Factory method to create a GameState
	 * @return a GameState
	 */
	protected abstract GameState createGameState();

	/**
	 * Choose a random valid prize
	 * @return a {@link PrizeFeature}, or null
	 */
	private static PrizeFeature choosePrize() {
		List<PrizeFeature> prizes = new ArrayList<PrizeFeature>(PrizeFeature.getPrizes());
		Collections.shuffle(prizes);
		for (PrizeFeature pf : prizes) {
			if (pf.isValid()) {
				return pf;
			}
		}
		return null;
	}

	public static NagState getNagState() {
		return NagState.valueOf(getRoamingPreferences().get("nagstate", NagState.NOT_YET_SHOWN.name()));
	}

	public static void setNagState(NagState newNagState) {
		getRoamingPreferences().put("nagstate", newNagState.name());
		flushPrefs();
	}

	@SuppressWarnings("unused")
    @Override
	protected void onPreRegisteredStartup() {
		if (IGNORESIGNUP && DEBUG) {
			showTitleScreen();
			return;
		}
		NagState nagState = getNagState();
		if (TESTSIGNUP && DEBUG) {
			nagState = NagState.NOT_YET_SHOWN;
		}
		switch (nagState) {
			case NOT_YET_SHOWN:
				PrizeFeature prize = choosePrize();
				if (prize != null) {
					SignUpScreen.show(prize);
				} else {
					showTitleScreen();
				}
				break;
			case PRIZE_AWAITS:
				// Restore the incentive file
				FileInputStream fis = null;
				BufferedInputStream bis = null;
				ObjectInputStream ois = null;
				try {
					fis = new FileInputStream(getIncentiveFile());
					bis = new BufferedInputStream(fis);
					ois = new ObjectInputStream(bis);
					NewsletterIncentive ni = (NewsletterIncentive) ois.readObject();
					if (!ni.validate()) {
						throw new Exception("Existing incentive file is invalid.");
					}
					UnlockBonusScreen.show(ni);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					showTitleScreen();
				} finally {
					if (fis != null) {
						try {
							fis.close();
						} catch (IOException e) {
						}
					}
				}
				break;
			case DONT_NAG:
			case REDEEMED:
				showTitleScreen();
				break;
			default:
				assert false : "Unknown nag state "+nagState;
		}
	}

	public static File getIncentiveFile() {
		return new File(getRoamingDirectoryPrefix() + "incentive.dat");
	}

	public static void setPrize(PrizeFeature prize) {
	    MiniGame.prize = prize;
    }

	public static PrizeFeature getPrize() {
	    return prize;
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
			restoreGameDialog.doModal(getMessage("lwjglapplets.minigame.restoregame.title"), getMessage("lwjglapplets.minigame.restoregame.message"), new Runnable() {
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
			onRestoreGameFailed(e);
		}
	}

	/**
	 * Called when a restore game failed
	 */
	protected void onRestoreGameFailed(Exception e) {
	}

	protected String getSaveGameRegistryMagicLocation() {
		return "tox";
	}

	protected void doSaveGame() {
		if (!allowSave) {
			return;
		}

		String file = getRestoreFile();

		try {
			GameOutputStream gos = new GameOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(gos);

			// Set current magic number
			gameState.setMagic(new Random().nextLong());
			if (getPlayerSlot() != null) {
				getPlayerSlot().getPreferences().putLong(getSaveGameRegistryMagicLocation(), gameState.getMagic());
			} else {
				getRoamingPreferences().putLong(getSaveGameRegistryMagicLocation(), gameState.getMagic());
			}
			// This restore file is now only valid when tox in prefs is the
			// same as that in the file. As soon as we do a restore, we zap
			// the tox value :)
			oos.writeObject(gameState);
			oos.flush();
			gos.close();
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
	 * Start a new game from scratch
	 */
	public static void cleanGame() {
		RoamingFile file = new RoamingFile(getGame().getRestoreFile());
		if (file.exists() && !file.delete()) {
			System.err.println("Failed to delete save file "+file);
		}
		allowSave = true;
		getGame().onCleanGame();
	}

	/**
	 * Called to start a new game from scratch after deleting the restore file
	 */
	protected void onCleanGame() {
		setGameState(createGameState());
		gameState.init();
	}

	/**
	 * @return the path of the file which we use to save games to
	 */
	protected String getRestoreFile() {
		if (getPlayerSlot() != null) {
			return getPlayerDirectoryPrefix() + RESTORE_FILE;
		} else {
			return getRoamingDirectoryPrefix() + RESTORE_FILE;
		}
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
		MiniGame.scoreGroup = scoreGroup;
	}

	@Override
	protected void onInit() {
		// Remote hiscores?
		getGame().submitRemoteHiscores = getRoamingPreferences().getBoolean("submitremotehiscores", !getGame().dontUseRemoteHiscores);
	}

	@Override
	protected void onRegisteredStartup() {
		showTitleScreen();
	}

	@Override
	protected void onUnregisteredStartup() {
		showRegisterScreen();
	}

	public static boolean getDontUseRemoteHiscores() {
		return getGame().dontUseRemoteHiscores;
	}

	public static boolean getSubmitRemoteHiscores() {
		return !getGame().dontUseRemoteHiscores && getGame().submitRemoteHiscores;
	}

	public static void setSubmitRemoteHiscores(boolean set) {
		getGame().submitRemoteHiscores = set;
		synchronized (getRoamingPreferences()) {
			getRoamingPreferences().putBoolean("submitremotehiscores", !getGame().dontUseRemoteHiscores && getGame().submitRemoteHiscores);
		}
		new Thread() {
			@Override
			public void run() {
				try {
					synchronized (getRoamingPreferences()) {
						getRoamingPreferences().flush();
					}
				} catch (BackingStoreException e) {
					e.printStackTrace(System.err);
				}
			}
		}.start();
	}




}
