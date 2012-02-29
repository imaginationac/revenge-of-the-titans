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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.screens.TitleScreen;
import worm.TimeUtil;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorMapFeature;

/**
 * Select which level you want to play in Sandbox mode
 */
public class SelectSandboxLevelScreen extends Screen {

	private static SelectSandboxLevelScreen instance;
	private static final Object LOCK = new Object();

	private static final String ID_CLOSE = "close";
	private static final String ID_PLAY = "play";
	private static final String ID_EDIT = "edit";
	private static final String ID_CREATE = "create";

	private transient int world = 0;
	private transient int template = 0;
	private transient int mapsize = 0;

	private transient List<Runnable> ops;

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

	/*
	 * Level map and metadata for exchange with server
	 */
	private class CloudMap {
		String name;
		String author;
		int hiscore;
		String hiscoreName;
		int mapWidth;
		int mapHeight;
		String mapData;
		
		CloudMap() {
			// DEVELOPMENT DEFAULTS
			name = "Foo";
			author = "Benny";
			hiscore = 1000;
			hiscoreName = "Rigby";
			mapWidth = 34;
			mapHeight = 22;
			mapData = "$$$$$#######$$$$$$$$#X............$$$$##XXXXX###$######X............$$$##XX.H.XXX###XXXXXX............$$$#XX......XXXXX.........%.......$$$#X.............................$$$#X............%.......%.......+$$$#X...%.............%...........$$$#X.....H^.....................+$$$#X.%..........................+$$$#X............%....%.%.........$$$#X......................%......$$$#X......@@@@.......%...........$$$#X......@@@@...................$$$#XX.....@@@@..............%....$$$##X...%.........^..............$$$$#X...........%.....%..........$$$$#XX........H..................$$$$##XXH$%................%......$$$$$##XX.........................$$$$$$##XXXXXXXXX.................$$$$$$$#########XXXX.............+$$$$$$$$$$$$$$$####X..............";
		}
	}
	private transient Map<Params,CloudMap> cache;

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
	public SelectSandboxLevelScreen(String name) {
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
		cache = Collections.synchronizedMap(new HashMap<Params,CloudMap>());
	}

	@Override
	protected void doTick() {
		synchronized (LOCK) {
			/*
			if (current == null && pending != null) {
				current = pending;
				pending = null;
				current.start();
			}
			*/
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
		//int maxWorld = Worm.getMaxWorld();
	}

	@Override
	protected void onClose() {
		synchronized (LOCK) {
			/*
			if (current != null) {
				current.interrupt();
				current = null;
			}
			pending = null;
			*/
		}
		ops.clear();
	}

	@Override
	protected void onClicked(String id) {
		if (ID_CLOSE.equals(id)) {
			TitleScreen.show();
		} else if (ID_EDIT.equals(id)) {
			//SandboxEditScreen ses  = Resources.get("edit.sandbox.screen");
			SandboxEditScreen.show();
		} else if (ID_PLAY.equals(id)) {
			// Construct a completely new gamestate at this point
			//Worm.resetGameState();
			//Worm.getGameState().doInit(new SandboxParams(WorldFeature.getWorld(world), Res.getSandboxMapTemplate(world, template), 0f, 74, true));
		}
	}

	private void doUpdateBestTime(String area, int w, int t, int ms, String name, int bestTime) {
		String msg = "{font:bigfont.glfont}";
		if (bestTime == 0) {
			msg += Game.getMessage("ultraworm.selectsandbox.not_yet_played");
		} else {
			msg += TimeUtil.format(bestTime);
		}
		if (bestTime == 0 || name.equals("")) {
			getArea(area).setText(msg);
		} else {
			getArea(area).setText(msg+"{font:tinyfont.glfont}\n("+name+")");
		}
	}

}
