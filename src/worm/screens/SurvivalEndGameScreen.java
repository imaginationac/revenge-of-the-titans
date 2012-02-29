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
package worm.screens;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import net.puppygames.applet.Game;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.Screen;
import net.puppygames.gamecommerce.shared.GenericServerRemote;
import worm.Res;
import worm.TimeUtil;
import worm.Worm;
import worm.WormGameState;

/**
 * The End Game Screen ends the game when the player is killed
 */
public class SurvivalEndGameScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static SurvivalEndGameScreen instance;

	private static final String ID_QUIT = "gameover_quit";
	private static final String ID_RESTART = "gameover_restart";
	private static final String ID_NEWMAP = "gameover_newmap";
	private static final String ID_ABANDON = "gameover_abandon";
	private static final String ID_MEDALS = "gameover_medals";

	private static final String ID_BUTTON = "gameover_";
	private static final String ID_TITLE = "_title";
	private static final String ID_DESC = "_desc";
	private static final String GROUP_LABELS = "labels";
	private static final String ID_DEFAULT_MSG = "default_msg";
	private static final String ID_TIME_MESSAGE = "time_message";
	private static final String ID_SUBMIT_MESSAGE = "submit_message";

	private transient String hoveredID;

	private transient List<Runnable> ops;

	/**
	 * C'tor
	 */
	public SurvivalEndGameScreen(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	protected void doRegister() {
		instance = this;
	}

	@Override
	protected void doDeregister() {
		instance = null;
	}

	@Override
	protected void doCreateScreen() {
	    ops = Collections.synchronizedList(new ArrayList<Runnable>());
	}

	@Override
	protected void onOpen() {

		ops.clear();

		setGroupVisible(GROUP_LABELS, false);
		Worm.setMouseAppearance(Res.getMousePointer());

		String msg;
		WormGameState gameState = Worm.getGameState();
		int currTime = gameState.getLevelTick();
		String key = "survival.hiscore."+gameState.getWorld().getUntranslated()+"."+gameState.getSurvivalParams().getTemplate().getClass().getName()+"."+gameState.getSurvivalParams().getSize();
		Preferences preferences = Game.getRoamingPreferences();
		int bestTime = preferences.getInt(key+".time", 0);
		String name = preferences.get(key+".name", "");
		if (bestTime == 0) {
			msg = "{font:tinyfont.glfont color:titles.colormap:button-red}"+Game.getMessage("ultraworm.survivalendgame.time_survived")+": "+TimeUtil.format(currTime);
			maybeSubmitHiscore(key, Game.getPlayerSlot().getName(), currTime);
		} else if (bestTime < currTime) {
			msg = "{font:tinyfont.glfont color:titles.colormap:button-red-text}"+Game.getMessage("ultraworm.survivalendgame.new_best_time")+" "+TimeUtil.format(currTime)+"\n{color:titles.colormap:button-red}"+Game.getMessage("ultraworm.survivalendgame.previous_record")+": "+TimeUtil.format(bestTime)+" ("+name+")";
			maybeSubmitHiscore(key, Game.getPlayerSlot().getName(), currTime);
		} else {
			msg = "{font:tinyfont.glfont color:titles.colormap:button-red}"+Game.getMessage("ultraworm.survivalendgame.time_survived")+": "+TimeUtil.format(currTime)+"\n"+Game.getMessage("ultraworm.survivalendgame.didnt_beat")+" "+TimeUtil.format(bestTime)+" ("+name+")";
			getArea(ID_SUBMIT_MESSAGE).setVisible(false);
		}

		getArea(ID_TIME_MESSAGE).setText(msg);

	}

	@Override
	protected void onClose() {
		ops.clear();
	}

	private static void addParam(String param, String data, StringBuilder sb) throws UnsupportedEncodingException {
		if (sb.length() > 0) {
			sb.append('&');
		}
		sb.append(param);
		sb.append('=');
		sb.append(URLEncoder.encode(data, "utf8"));
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

	private void maybeSubmitHiscore(String key, final String name, final int time) {
		Game.getRoamingPreferences().putInt(key+".time", time);
		Game.getRoamingPreferences().put(key+".name", name);
		Game.flushPrefs();
		if (MiniGame.getSubmitRemoteHiscores()) {
			// Submit a remote hiscore!
			getArea(ID_SUBMIT_MESSAGE).setVisible(true);
			new Thread() {

				private void setMessage(final String message) {
					ops.add(new Runnable() {
						@Override
						public void run() {
							getArea(ID_SUBMIT_MESSAGE).setText(message);
						}
					});
				}

				@Override
				public void run() {
					GenericServerRemote server;
					try {
						setMessage("{font:tinyfont.glfont color:titles.colormap:button-red}"+Game.getMessage("ultraworm.survivalendgame.submitting")+"...");
						server = (GenericServerRemote) Naming.lookup(GenericServerRemote.RMI_URL);
					} catch (Exception e) {
						e.printStackTrace(System.err);
						setMessage("{font:tinyfont.glfont color:titles.colormap:button-red}"+Game.getMessage("ultraworm.survivalendgame.failed"));
						return;
					}
					String ret;
					StringBuilder command = new StringBuilder(256);

					try {
						addParam("cmd", "revengehiscore", command);
						addParam("installation", String.valueOf(Game.getInstallation()), command);
						addParam("version", Game.getVersion() + Game.getModName(), command);
						addParam("world", String.valueOf(Worm.getGameState().getSurvivalParams().getWorld().getIndex()), command);
						addParam("template", String.valueOf(Worm.getGameState().getSurvivalParams().getTemplateIndex()), command);
						addParam("size", String.valueOf(Worm.getGameState().getSurvivalParams().getSize()), command);
						addParam("name", name, command);
						addParam("score", String.valueOf(time), command);

						System.out.println("Executing remote command "+command);

						ret = server.doCommand(command.toString());
						System.out.println("Remote command result "+ret);
						String result = getParam(ret, "result", "FAILED");
						if ("SUCCESS".equals(result)) {
							// Hurrah!
							if ("TRUE".equals(getParam(ret, "beat", "FALSE"))) {
								setMessage(Game.getMessage("ultraworm.survivalendgame.you_are_winner"));
							} else {
								setMessage(Game.getMessage("ultraworm.survivalendgame.you_are_loser")+" "+TimeUtil.format(Integer.parseInt(getParam(ret, "score", "0")))+" ("+getParam(ret, "name", "[unknown]")+")");
							}
						} else if ("FAILED".equals(result)) {
							throw new Exception("Server result: "+getParam(result, "reason", "unknown"));
						} else {
							throw new Exception("Don't understand server response "+ret);
						}
					} catch (Exception e) {
						e.printStackTrace(System.err);
						setMessage("{font:tinyfont.glfont color:titles.colormap:button-red}"+Game.getMessage("ultraworm.survivalendgame.failed"));
						return;
					}
				}
			}.start();
		} else {
			getArea(ID_SUBMIT_MESSAGE).setVisible(false);
		}
	}

	/**
	 * Shows the Research Screen with research information about the specified world.
	 * @param world
	 */
	public static void show() {
		instance.open();
	}

	@Override
	protected void doTick() {
		// Do stuff in the "event thread"
		synchronized (ops) {
			if (ops.size() > 0) {
				for (Runnable r : ops) {
					r.run();
				}
				ops.clear();
			}
		}
	}

	@Override
	protected void onClicked(String id) {
		if (ID_RESTART.equals(id)) {
			Worm.getGameState().restartSurvival(false);
		} else if (ID_NEWMAP.equals(id)) {
			Worm.getGameState().restartSurvival(true);
		} else if (ID_QUIT.equals(id)) {
			Worm.getGameState().quit();
		} else if (ID_ABANDON.equals(id)) {
			Worm.getGameState().showLevelSelectScreen();
		} else if (ID_MEDALS.equals(id)) {
			//close();
			MedalsScreen.show();
		}
	}

	@Override
	protected void onHover(String id, boolean on) {
		if (on) {
			if (id.startsWith(ID_BUTTON)) {
				if (!id.endsWith(ID_TITLE) & !id.endsWith(ID_DESC)) {
					setVisible(id + ID_TITLE, true);
					setVisible(id + ID_DESC, true);

					setVisible(ID_DEFAULT_MSG, false);
					hoveredID = id;
				}
			}
		} else {
			if (id.startsWith(ID_BUTTON)) {
				setVisible(id + ID_TITLE, false);
				setVisible(id + ID_DESC, false);

				if (hoveredID == id) {
					hoveredID = null;
					setVisible(ID_DEFAULT_MSG, true);
				}
			}
		}
	}

}
