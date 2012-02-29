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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.puppygames.applet.Game;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.Screen;
import net.puppygames.applet.screens.TitleScreen;
import net.puppygames.gamecommerce.shared.GenericServerRemote;
import worm.Res;
import worm.SurvivalParams;
import worm.TimeUtil;
import worm.Worm;
import worm.features.WorldFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorMapFeature;

/**
 * Select which level you want to play in Survival mode
 */
public class SelectSurvivalLevelScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static SelectSurvivalLevelScreen instance;
	private static final Object LOCK = new Object();

	private static final String ID_BACK = "back";
	private static final String ID_PLAY = "play";
	private static final String GROUP_ID_WORLD = "select.world.";
	private static final String GROUP_ID_WORLD_SELECTED = "selected.world.";
	private static final String ID_WORLD = "select.world.";

	private static final String GROUP_ID_TERRAIN = "select.terrain.";
	private static final String GROUP_ID_TERRAIN_SELECTED = "selected.terrain.";
	private static final String ID_TERRAIN = "select.terrain.";

	private static final String GROUP_ID_SIZE = "select.size.";
	private static final String GROUP_ID_SIZE_SELECTED = "selected.size.";
	private static final String ID_SIZE = "select.size.";

	private static final String ID_BEST_TIME = "best_time";
	private static final String ID_ONLINE_BEST_TIME = "online_best_time";
	private static final String ID_ONLINE_BEST_TIME_LABEL = "online_best_time_label";

	private static final float[] DIFFICULTY = {-0.25f, -0.125f, 0.0f, 0.125f, 0.25f};
	private static final int[] SIZE = {52, 74, 96}; // Ensure no bigger than WormGameState.ABS_MAX_SIZE

	private transient int world = 0;
	private transient int template = 0;
	private transient int mapsize = 0;

	private transient List<Runnable> ops;
	private transient ScoreGetter current, pending;

	private static class Params {
		int world, template, mapsize;

		public Params(int world, int template, int mapsize) {
	        this.world = world;
	        this.template = template;
	        this.mapsize = mapsize;
        }

		@Override
        public String toString() {
	        StringBuilder builder = new StringBuilder();
	        builder.append("Params [world=");
	        builder.append(world);
	        builder.append(", template=");
	        builder.append(template);
	        builder.append(", mapsize=");
	        builder.append(mapsize);
	        builder.append("]");
	        return builder.toString();
        }

		@Override
        public int hashCode() {
	        final int prime = 31;
	        int result = 1;
	        result = prime * result + mapsize;
	        result = prime * result + template;
	        result = prime * result + world;
	        return result;
        }

		@Override
        public boolean equals(Object obj) {
	        if (this == obj) {
		        return true;
	        }
	        if (obj == null) {
		        return false;
	        }
	        if (getClass() != obj.getClass()) {
		        return false;
	        }
	        Params other = (Params) obj;
	        if (mapsize != other.mapsize) {
		        return false;
	        }
	        if (template != other.template) {
		        return false;
	        }
	        if (world != other.world) {
		        return false;
	        }
	        return true;
        }



	}

	private static class Hiscore {
		String name;
		int score;
	}

	private transient Map<Params, Hiscore> cache;

	private class ScoreGetter extends Thread {
		final Params params;

		public ScoreGetter(Params params) {
			super("Remove hiscore getter");
			setDaemon(true);
	        this.params = params;
        }

		private Hiscore getHiscore() throws Exception {
			GenericServerRemote server = (GenericServerRemote) Naming.lookup(GenericServerRemote.RMI_URL);
			String ret;
			StringBuilder command = new StringBuilder(256);

			addParam("cmd", "getrevengehiscore", command);
			addParam("version", Game.getVersion() + Game.getModName(), command);
			addParam("world", String.valueOf(params.world), command);
			addParam("terrain", String.valueOf(params.template), command);
			addParam("size", String.valueOf(params.mapsize), command);

			System.out.println("Executing remote command "+command);

			ret = server.doCommand(command.toString());
			System.out.println("Remote command result "+ret);
			String result = getParam(ret, "result", "FAILED");
			if ("SUCCESS".equals(result)) {
				Hiscore hs = new Hiscore();
				hs.name = getParam(ret, "name", "");
				hs.score = Integer.parseInt(getParam(ret, "score", "0"));
				return hs;
			} else if ("FAILED".equals(result)) {
				throw new Exception("Server result: "+getParam(result, "reason", "unknown"));
			} else {
				throw new Exception("Don't understand server response "+ret);
			}
        }

		@Override
        public void run() {

			try {

	            Hiscore cached = cache.get(params);
	            if (cached == null) {
		            ops.add(new Runnable() {
		            	@Override
		            	public void run() {
		            		if (params.world == world && params.template == template && params.mapsize == SIZE[mapsize]) {
				    			setVisible(ID_ONLINE_BEST_TIME, true);
				    			setVisible(ID_ONLINE_BEST_TIME_LABEL, true);
				    			getArea(ID_ONLINE_BEST_TIME).setText("");
				    			getArea(ID_ONLINE_BEST_TIME_LABEL).setText(Game.getMessage("ultraworm.selectsurvival.fetching_online_hiscore")+"...");
		            		}
		            	}
		            });
	            	cached = getHiscore();
	            	cache.put(params, cached);
	            } else {
	            }

	            final Hiscore hiscore = cached;
	            ops.add(new Runnable() {
	            	@Override
	            	public void run() {
	            		if (params.world == world && params.template == template && params.mapsize == SIZE[mapsize]) {
			    			setVisible(ID_ONLINE_BEST_TIME, true);
			    			setVisible(ID_ONLINE_BEST_TIME_LABEL, true);
		            		getArea(ID_ONLINE_BEST_TIME_LABEL).setText(Game.getMessage("ultraworm.selectsurvival.online_hiscore_record")+":");
		            		doUpdateBestTime(ID_ONLINE_BEST_TIME, params.world, params.template, params.mapsize, hiscore.name, hiscore.score);
	            		}
	            	}
	            });
            } catch (InterruptedException e) {
	            // Ignore
            } catch (Exception e) {
            	e.printStackTrace(System.err);
	            ops.add(new Runnable() {
	            	@Override
	            	public void run() {
	            		if (params.world == world && params.template == template && params.mapsize == SIZE[mapsize]) {
	                		getArea(ID_ONLINE_BEST_TIME_LABEL).setText(Game.getMessage("ultraworm.selectsurvival.failed_to_retrieve"));
	            		}
	            	}
	            });
            } finally {
            	synchronized (LOCK) {
            		current = null;
            	}
            }
		}
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

	/**
	 * C'tor
	 */
	public SelectSurvivalLevelScreen(String name) {
		super(name);
		setAutoCreated();
	}

	public static void show() {
		instance.open();
	}

	@Override
	protected void doRegister() {
		instance = this;
	}

	@Override
	protected void doCreateScreen() {
		ops = Collections.synchronizedList(new ArrayList<Runnable>());
		cache = Collections.synchronizedMap(new HashMap<Params, Hiscore>());
	}

	@Override
	protected void doTick() {
		synchronized (LOCK) {
			if (current == null && pending != null) {
				current = pending;
				pending = null;
				current.start();
			}
		}
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
	protected void onOpen() {
		cache.clear();

		// Always use earth color map
		ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get("earth.colormap"));

		// Max world available:
		int maxWorld = Worm.getMaxWorld();

		for (int i = 0; i < 5; i ++) {

			if (i == world) {
				// Remember last selected world
				setGroupVisible(GROUP_ID_WORLD+i, false);
				setGroupVisible(GROUP_ID_WORLD_SELECTED+i, true);
			} else {
				setGroupVisible(GROUP_ID_WORLD_SELECTED+i, false);
			}

			setGroupEnabled(GROUP_ID_WORLD+i, i < maxWorld);

		}

		for (int i = 0; i < 4; i ++) {
			if (i == template) {
				// Remember last selected template
				setGroupVisible(GROUP_ID_TERRAIN+i, false);
				setGroupVisible(GROUP_ID_TERRAIN_SELECTED+i, true);
			} else {
				setGroupVisible(GROUP_ID_TERRAIN_SELECTED+i, false);
			}
		}

		for (int i = 0; i < 3; i ++) {
			if (i == mapsize) {
				// Remember last selected size
				setGroupVisible(GROUP_ID_SIZE+i, false);
				setGroupVisible(GROUP_ID_SIZE_SELECTED+i, true);
			} else {
				setGroupVisible(GROUP_ID_SIZE_SELECTED+i, false);
			}
		}

		updateBestTime();

	}

	@Override
	protected void onClose() {
		synchronized (LOCK) {
			if (current != null) {
				current.interrupt();
				current = null;
			}
			pending = null;
		}
		ops.clear();
	}

	@Override
	protected void onClicked(String id) {
		if (ID_BACK.equals(id)) {
			TitleScreen.show();
		} else if (ID_PLAY.equals(id)) {
			// Construct a completely new gamestate at this point
			Worm.resetGameState();
			Worm.getGameState().doInit(new SurvivalParams(template, WorldFeature.getWorld(world), Res.getSurvivalMapTemplate(world, template), DIFFICULTY[world], SIZE[mapsize], true));

		} else if (id.startsWith(ID_WORLD)) {

			int newWorld = Character.getNumericValue(id.charAt(ID_WORLD.length()));

			if (newWorld!=world){
				setGroupVisible(GROUP_ID_WORLD+world, true);
				setGroupVisible(GROUP_ID_WORLD_SELECTED+world, false);
				world = newWorld;
				setGroupVisible(GROUP_ID_WORLD+world, false);
				setGroupVisible(GROUP_ID_WORLD_SELECTED+world, true);
				updateBestTime();
			}
		} else if (id.startsWith(ID_TERRAIN)) {

			int newTemplate = Character.getNumericValue(id.charAt(ID_TERRAIN.length()));

			if (newTemplate!=template){
				setGroupVisible(GROUP_ID_TERRAIN+template, true);
				setGroupVisible(GROUP_ID_TERRAIN_SELECTED+template, false);
				template = newTemplate;
				setGroupVisible(GROUP_ID_TERRAIN+template, false);
				setGroupVisible(GROUP_ID_TERRAIN_SELECTED+template, true);
				updateBestTime();
			}
		} else if (id.startsWith(ID_SIZE)) {

			int newMapsize = Character.getNumericValue(id.charAt(ID_SIZE.length()));

			if (newMapsize!=mapsize){
				setGroupVisible(GROUP_ID_SIZE+mapsize, true);
				setGroupVisible(GROUP_ID_SIZE_SELECTED+mapsize, false);
				mapsize = newMapsize;
				setGroupVisible(GROUP_ID_SIZE+mapsize, false);
				setGroupVisible(GROUP_ID_SIZE_SELECTED+mapsize, true);
				updateBestTime();
			}
		}

	}

	private void doUpdateBestTime(String area, int w, int t, int ms, String name, int bestTime) {
		String msg = "{font:bigfont.glfont}";
		if (bestTime == 0) {
			msg += Game.getMessage("ultraworm.selectsurvival.not_yet_played");
		} else {
			msg += TimeUtil.format(bestTime);
		}
		if (bestTime == 0 || name.equals("")) {
			getArea(area).setText(msg);
		} else {
			getArea(area).setText(msg+"{font:tinyfont.glfont}\n("+name+")");
		}
	}

	private void updateBestTime() {
		int bestTime = Game.getRoamingPreferences().getInt("survival.hiscore."+WorldFeature.getWorld(world).getUntranslated()+"."+Res.getSurvivalMapTemplate(world, template).getClass().getName()+"."+SIZE[mapsize]+".time", 0);
		String name = Game.getRoamingPreferences().get("survival.hiscore."+WorldFeature.getWorld(world).getUntranslated()+"."+Res.getSurvivalMapTemplate(world, template).getClass().getName()+"."+SIZE[mapsize]+".name", "");
		doUpdateBestTime(ID_BEST_TIME, world, template, mapsize, name, bestTime);
		if (MiniGame.getSubmitRemoteHiscores()) {
			synchronized (LOCK) {
				if (current != null) {
					current.interrupt();
				}
				pending = new ScoreGetter(new Params(world, template, SIZE[mapsize]));
			}
		} else {
			setVisible(ID_ONLINE_BEST_TIME, false);
			setVisible(ID_ONLINE_BEST_TIME_LABEL, false);
		}
	}
}
