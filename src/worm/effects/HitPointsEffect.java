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

import net.puppygames.applet.effects.Effect;
import worm.animation.SimpleThingWithLayers;
import worm.buildings.BuildingFeature;
import worm.entities.Building;
import worm.features.LayersFeature;

/**
 * For showing the hitpoints or forcefield points of buildings
 */
public class HitPointsEffect extends Effect {

	private static final int ALPHA_SHOW_DELTA = 16;
	private static final int ALPHA_HIDE_DELTA = 8;

	private final Building building;
	private int alpha = 255;
	private int hitPoints;

	private boolean show = true;
	private boolean finished;
	private boolean done;

	private SimpleThingWithLayers layers;

	/**
	 * C'tor
	 */
	public HitPointsEffect(Building building) {
		this.building = building;
	}

	/**
	 * @param the hitpoints to show
	 */
	public void setHitPoints(int newHitPoints) {
		if (newHitPoints != hitPoints) {
			hitPoints = newHitPoints;
			updateImage();
		}
	}

	@Override
	protected void doSpawnEffect() {
		hitPoints = building.getHitPoints();
		updateImage();
	}

	private void updateImage() {
		// Clear old sprites away
		removeSprites();

		// chaz hack! don't do this for mines!...
		if (building.isMineField()) {
			return;
		}

		// NOTE: divide by 4 because I quadrupled hitpoints
		LayersFeature hitPointsGraphics = building.getFeature().getHitPointsGraphics(hitPoints / BuildingFeature.HITPOINTS_DIVISOR);
		if (hitPointsGraphics != null) {
			layers = new SimpleThingWithLayers(getScreen());
			hitPointsGraphics.createSprites(getScreen(), layers);
		}
	}

	@Override
	protected void doUpdate() {
		if (layers != null) {
			for (int i = 0; i < layers.getSprites().length; i ++) {
				layers.getSprite(i).setLocation(building.getScreenX(), building.getScreenY());
			}
		}
	}

	/**
	 * Show or hide the effect.
	 * @param show the show to set
	 */
	public void setShow(boolean show) {
		this.show = show;
	}

	/**
	 * Reset to totally visible
	 */
	public void reset() {
		show = true;
		alpha = 255;
	}

	@Override
	public void finish() {
		finished = true;
		setShow(false);
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	protected void doTick() {
		if (!show) {
			alpha = Math.max(0, alpha - ALPHA_HIDE_DELTA);
			if (finished && alpha == 0) {
				remove();
				return;
			}
		} else {
			alpha = Math.min(255, alpha + ALPHA_SHOW_DELTA);
		}

		if (layers != null) {
			for (int i = 0; i < layers.getSprites().length; i ++) {
				layers.getSprite(i).setAlpha(alpha);
			}
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
	public boolean isEffectActive() {
		return !done;
	}

	@Override
	protected void render() {
		// Nothing to actually render
	}
}
