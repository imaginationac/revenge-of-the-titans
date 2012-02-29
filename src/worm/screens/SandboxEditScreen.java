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

import java.util.List;

import net.puppygames.applet.Area;
import net.puppygames.applet.Screen;
import worm.GameMap;
import worm.MapRenderer;
import worm.Res;
import worm.SandboxParams;
import worm.Worm;
import worm.WormGameState;
import worm.features.LevelFeature;
import worm.features.LevelFeature.SandboxLevelFeature;
import worm.features.WorldFeature;
import worm.generator.EmptyMapGenerator;
import worm.generator.MapGeneratorParams;
import worm.generator.MapTemplate;

/**
 * Select editor screen
 */
public class SandboxEditScreen extends Screen {
	
	private static final String ID_MAP = "map";

	private static SandboxEditScreen instance;
	private static final Object LOCK = new Object();

	private transient MapRenderer renderer;
	private transient Area mapArea;

	private transient int world = 0;
	private transient int template = 0;
	private transient int level = -1;
	private transient int levelInWorld = -1;
	private transient int mapsize = 74;
	private transient float mapX, mapY;
	private transient MapTemplate mapTemplate;
	private transient SandboxParams levelParams;
	private transient SandboxLevelFeature levelFeature;
	private transient GameMap map;

	private transient List<Runnable> ops;



	/**
	 * C'tor
	 */
	public SandboxEditScreen(String name) {
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
		mapArea = getArea(ID_MAP);
	}

	@Override
	protected void doTick() {
		renderer.render((int) mapX + renderer.getOriginX(), (int) mapY + renderer.getOriginY());
	}

	@Override
	protected void onResized() {
		if (renderer == null) {
			renderer = new MapRenderer(this);
			renderer.setOrigin(-MapRenderer.TILE_SIZE * 4, -MapRenderer.TILE_SIZE * 4);
			renderer.setMap(map);
		}
		renderer.onResized();
		renderer.render((int) mapX + renderer.getOriginX(), (int) mapY + renderer.getOriginY());
		//setHover(null);
	}

	@Override
	protected void onOpen() {
		mapTemplate = Res.getSandboxMapTemplate(world, template);
		levelParams = new SandboxParams(WorldFeature.getWorld(world), mapTemplate, 72 ); // WorldFeature, Template, Size
		levelFeature = (SandboxLevelFeature)LevelFeature.generateSandbox( levelParams );
		MapGeneratorParams mapGeneratorParams = new MapGeneratorParams( 0.5f, WormGameState.GAME_MODE_SANDBOX, level, levelFeature, levelInWorld, 0, 0, levelFeature.getWorld() );
		EmptyMapGenerator bmg = new EmptyMapGenerator( mapTemplate, mapGeneratorParams);
		this.map = bmg.generate();
		
		onResized();

		Worm.setMouseAppearance(Res.getMousePointer());
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
	}
}