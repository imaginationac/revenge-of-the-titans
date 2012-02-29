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
package worm.effects;

import net.puppygames.applet.Game;
import net.puppygames.applet.effects.Effect;

import org.lwjgl.util.ReadablePoint;

import worm.MapRenderer;
import worm.Res;
import worm.Worm;
import worm.animation.SimpleThingWithLayers;
import worm.features.LayersFeature;
import worm.screens.GameScreen;

/**
 * The arrows at the edge effect
 */
public class ArrowEffect extends Effect {

	private static final int HALF_TILE = MapRenderer.TILE_SIZE / 2;

	private boolean done;
	private int mapx, mapy, tilex, tiley;
	private int targetMapX, targetMapY;
	private LayersFeature current;
	private SimpleThingWithLayers layers;

	public ArrowEffect(int tilex, int tiley) {
		setSpawnLocation(tilex, tiley);
		mapx = targetMapX;
		mapy = targetMapY;
	}

	public void setSpawnLocation(int tilex, int tiley)  {
		this.tilex = tilex;
		this.tiley = tiley;
		targetMapX = tilex * MapRenderer.TILE_SIZE + HALF_TILE;
		targetMapY = tiley * MapRenderer.TILE_SIZE + HALF_TILE;
	}

	@Override
	public void render() {
	}

	@Override
	protected void doTick() {
		if (mapx != targetMapX) {
			mapx += targetMapX > mapx ? 1 : -1;
		}
		if (mapy != targetMapY) {
			mapy += targetMapY > mapy ? 1 : -1;
		}
	}

	private void setCurrent(LayersFeature newLayers) {
		if (newLayers == current) {
			return;
		}
		removeSprites();
		current = newLayers;
		if (current != null) {
			layers = new SimpleThingWithLayers(getScreen());
			current.createSprites(getScreen(), layers);
		}
	}

	@Override
	protected void doRemove() {
		done = true;
		removeSprites();
	}

	private void removeSprites() {
		if (layers != null) {
			layers.remove();
			layers = null;
		}
	}

	@Override
	protected void doUpdate() {
		ReadablePoint offset = GameScreen.getSpriteOffset();
		int screenX = mapx + offset.getX();
		int screenY = mapy + offset.getY();

		if (screenX <= HALF_TILE) {
			if (screenY < HALF_TILE) {
				setCurrent(Res.getSouthWestArrow());
			} else if (screenY > Game.getHeight() - HALF_TILE) {
				setCurrent(Res.getNorthWestArrow());
			} else {
				setCurrent(Res.getWestArrow());
			}
		} else if (screenX >= Game.getWidth() - HALF_TILE) {
			if (screenY < HALF_TILE) {
				setCurrent(Res.getSouthEastArrow());
			} else if (screenY > Game.getHeight() - HALF_TILE) {
				setCurrent(Res.getNorthEastArrow());
			} else {
				setCurrent(Res.getEastArrow());
			}
		} else if (screenY < HALF_TILE) {
			setCurrent(Res.getSouthArrow());
		} else if (screenY > Game.getHeight() - HALF_TILE) {
			setCurrent(Res.getNorthArrow());
		} else {
			if (tilex == -1) {
				setCurrent(Res.getWestArrow());
			} else if (tiley == -1) {
				setCurrent(Res.getSouthArrow());
			} else if (tilex == Worm.getGameState().getMap().getWidth()) {
				setCurrent(Res.getEastArrow());
			} else if (tiley == Worm.getGameState().getMap().getHeight()) {
				setCurrent(Res.getNorthArrow());
			} else {
				// It's a mid spawner, and it's fully visible
				setCurrent(Res.getMidSpawnerArrow());
			}
		}

		if (layers == null) {
			return;
		}

		if (screenX < HALF_TILE) {
			screenX = HALF_TILE;
		} else if (screenX >= Game.getWidth() - HALF_TILE) {
			screenX = Game.getWidth() - 1 - HALF_TILE;
		}

		if (screenY < HALF_TILE) {
			screenY = HALF_TILE;
		} else if (screenY >= Game.getHeight() - HALF_TILE) {
			screenY = Game.getHeight() - 1 - HALF_TILE;
		}

		for (int i = 0; i < layers.getSprites().length; i ++) {
			layers.getSprite(i).setLocation(screenX, screenY);
		}
	}

	@Override
	public boolean isEffectActive() {
		return !done;
	}

}
