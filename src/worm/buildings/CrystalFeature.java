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
package worm.buildings;

import net.puppygames.applet.effects.LabelEffect;

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import worm.CrystalResource;
import worm.Entity;
import worm.EntitySpawningFeature;
import worm.GameConfiguration;
import worm.Layers;
import worm.MapRenderer;
import worm.Res;
import worm.Worm;
import worm.WormGameState;
import worm.effects.CrystalSpawnEffect;
import worm.entities.Building;
import worm.entities.Gidrah;
import worm.features.LayersFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;

/**
 * $Id: CrystalFeature.java,v 1.18 2010/10/16 02:17:16 foo Exp $
 * An obstacle.
 * @author $Author: foo $
 * @version $Revision: 1.18 $
 */
public class CrystalFeature extends BuildingFeature implements EntitySpawningFeature {

	private static final long serialVersionUID = 1L;

	private static final int GROWTH_TIME_PER_SIZE = 120;

	/** Value of the crystal in $ */
	private int value;

	/** Size (1 = small, 2 = medium, 3 = large) */
	private int size;

	/** Appearances for different number of hitpoints */
	private LayersFeature usedAppearance;

	private static final int PHASE_NORMAL = 0;
	private static final int PHASE_GROWING = 1;
	private static final int PHASE_USED_UP = 2;

	/**
	 * Crystal instances
	 */
	private class CrystalInstance extends Building implements CrystalResource {

		private static final long serialVersionUID = 1L;

		private int growTick;
		private int consumed;
		private int phase = PHASE_NORMAL;
		private int beams;

		private transient LabelEffect valueEffect;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected CrystalInstance(boolean ghost) {
			super(CrystalFeature.this, ghost);
		}

		@Override
		public boolean isLaserOver() {
			return true;
		}

		@Override
		public int getBeams() {
			return beams;
		}

		@Override
		public void addBeams(int n) {
			beams += n;
		}

		@Override
		public int getSize() {
			return size;
		}

		@Override
		public boolean isReady() {
			return phase == PHASE_NORMAL;
		}

		@Override
		protected void doBuildingSpawn() {
			WormGameState gameState = Worm.getGameState();
			gameState.addCrystals(1);
			gameState.addInitialCrystals(1);
			gameState.addUnminedCrystal(this);
			// Only really need to rethink routes in survival mode because crystals only spawn dynamically in survival:
			int gameMode = gameState.getGameMode();
			if (gameMode == WormGameState.GAME_MODE_SURVIVAL || gameMode == WormGameState.GAME_MODE_XMAS) {
				int tx = (int) (getMapX() / MapRenderer.TILE_SIZE);
				int ty = (int) (getMapY() / MapRenderer.TILE_SIZE);
				Gidrah.rethinkRoutes(new Rectangle(tx, ty, size == 1 ? 1 : 2, 1));
				phase = PHASE_GROWING;
				CrystalSpawnEffect fx = new CrystalSpawnEffect(this);
				fx.spawn(GameScreen.getInstance());
			}
		}

		@Override
		protected void doBuildingTick() {
			if (phase != PHASE_GROWING) {
				return;
			}

			growTick ++;
			if (growTick > GROWTH_TIME_PER_SIZE * size) {
				// Fully grown
				phase = PHASE_NORMAL;
			} else {
				// Still growing
				for (int i = 0; i < getNumSprites(); i ++) {
					float ratio = (float) growTick / (float) (size * GROWTH_TIME_PER_SIZE);
					Sprite sprite = getSprite(i);
					float xScale = LinearInterpolator.instance.interpolate(0.0f, 0.5f, ratio);
					float yScale = LinearInterpolator.instance.interpolate(0.0f, 0.5f, ratio);
					sprite.setScale(FPMath.fpValue(xScale),FPMath.fpValue(yScale));
					int alpha = (int) LinearInterpolator.instance.interpolate(0, 255, ratio);
					sprite.setAlpha(alpha);
				}
			}
		}

		@Override
		public void consume(int amount) {
			switch (phase) {
				case PHASE_USED_UP:
				case PHASE_GROWING:
					return;
				case PHASE_NORMAL:
					consumed += amount;
					if (!hasRemaining()) {
						// we're all used up
						phase = PHASE_USED_UP;
						updateAppearance();
						for (int i = 0; i < getNumSprites(); i ++) {
							if (getSprite(i).getLayer()==3) {
								float xScale = 0.4f;
								float yScale = 0.3f;
								int alpha = 96;
								this.getSprite(i).setScale(FPMath.fpValue(xScale),FPMath.fpValue(yScale));
								this.getSprite(i).setAlpha(alpha);
							} else {
								float xScale = 0.3f;
								float yScale = 0.2f;
								this.getSprite(i).setScale(FPMath.fpValue(xScale),FPMath.fpValue(yScale));
							}
						}

						// Clear obstacle on map
						WormGameState gameState = Worm.getGameState();
						clearMap();
						gameState.addCrystals(-1);
						gameState.removeUnminedCrystal(this);

						if (valueEffect != null) {
							valueEffect.finish();
							valueEffect = null;
						}
					} else {
						for (int i = 0; i < getNumSprites(); i ++) {
							float ratio = consumed / (float) (value);

							// make shadows not scale so much
							Sprite sprite = getSprite(i);
							if (sprite.getLayer() == Layers.CRYSTAL_SHADOW) {
								float xScale = LinearInterpolator.instance.interpolate(0.5f, 0.4f, ratio);
								float yScale = LinearInterpolator.instance.interpolate(0.5f, 0.3f, ratio);
								int alpha = (int) LinearInterpolator.instance.interpolate(255, 96, ratio);
								sprite.setScale(FPMath.fpValue(xScale),FPMath.fpValue(yScale));
								sprite.setAlpha(alpha);
							} else {
								float xScale = LinearInterpolator.instance.interpolate(0.5f, 0.3f, ratio);
								float yScale = LinearInterpolator.instance.interpolate(0.5f, 0.2f, ratio);
								sprite.setScale(FPMath.fpValue(xScale),FPMath.fpValue(yScale));

							}
						}

						if (valueEffect != null) {
							valueEffect.setText("$"+getRemaining());
						}
					}
					break;
				default:
					assert false : "Unknown phase "+phase;
			}
		}

		@Override
        public void clearMap() {
			int tx = (int) (getMapX() / MapRenderer.TILE_SIZE);
			int ty = (int) (getMapY() / MapRenderer.TILE_SIZE);
			WormGameState gameState = Worm.getGameState();
			gameState.getMap().clearItem(tx, ty);
			if (size > 1) {
				gameState.getMap().clearItem(tx + 1, ty);
			}
			// Update gidrah routes
			Gidrah.rethinkRoutes(new Rectangle(tx, ty, size == 1 ? 1 : 2, 1));
		}

		@Override
		public boolean canCollide() {
			return false;
		}

		@Override
		public boolean isLaserThrough() {
			return true;
		}

		@Override
		public int getSalePrice() {
			// Return the remaining value of the crystal
			int value = (int) (getRemaining() * Worm.getGameState().getScavengeRate());
			value /= 10;
			value *= 10;
			return value;
		}

		@Override
		public boolean hasRemaining() {
			return consumed < getAmount();
		}

		@Override
		public int getRemaining() {
			return getAmount() - consumed;
		}

		private int getAmount() {
			return value;
		}

		@Override
		protected boolean dontShowLabel() {
			return true;
		}

		@Override
		public boolean canSell() {
			return false;
		}

		@Override
		public void repair() {
			// Don't repair crystals
		}

		@Override
		public void repairFully() {
			// Don't repair crystals
		}

		@Override
		public boolean isBarricade() {
			return true;
		}

		@Override
		public boolean isCrystal() {
			return true;
		}

		@Override
		public boolean isWorthAttacking() {
			return false;
		}

		@Override
		public boolean shouldShowAttackWarning() {
			return false;
		}

		@Override
		public void onHovered(int mode) {
			// Show remaining crystal
			if (phase != PHASE_NORMAL) {
				return;
			}

			if (valueEffect == null) {
				valueEffect = new LabelEffect(net.puppygames.applet.Res.getTinyFont(), "$"+getRemaining(), ReadableColor.CYAN, ReadableColor.CYAN, 20, 10);
				valueEffect.setOffset(GameScreen.getSpriteOffset());
				valueEffect.setLocation(getX(), getY() + 16);
				valueEffect.spawn(GameScreen.getInstance());
				valueEffect.tick();
				valueEffect.setPaused(true);
			}
		}

		@Override
		public void onLeave(int mode) {
		    if (valueEffect != null) {
		    	valueEffect.setPaused(false);
		    	valueEffect = null;
		    }
		}


		@Override
		protected void doRemoveSpecialEffects() {
		    if (valueEffect != null) {
		    	valueEffect.remove();
		    	valueEffect = null;
		    }
		}



		@Override
		protected void doBuildingDestroy() {
			Worm.getGameState().addInitialCrystals(-1);
		}

		@Override
		protected void adjustProximity(Building target, int delta) {
			target.addCrystals(delta);
		}

		@Override
		protected boolean isAffectedBy(Building building) {
			return super.isAffectedBy(building) && phase != PHASE_USED_UP;
		}

		@Override
		public LayersFeature getMousePointer(boolean clicked) {
			if (Worm.getGameState().isSelling()) {
				return Res.getMousePointerSellOff();
			}
			return super.getMousePointer(clicked);
		}

		@Override
		public void addFactories(int n) {
			super.addFactories(n);

			if (getFactories() == 0) {
				Worm.getGameState().addUnminedCrystal(this);
			} else {
				Worm.getGameState().removeUnminedCrystal(this);
			}
		}

		@Override
		public float getAgitation() {
			int gameMode = Worm.getGameState().getGameMode();
			if (gameMode == WormGameState.GAME_MODE_CAMPAIGN || gameMode == WormGameState.GAME_MODE_ENDLESS) {
				return 0.0f;
			} else {
				return getRemaining() * GameConfiguration.getInstance().getCrystalAgitationFactor();
			}
		}
	}

	/**
	 * C'tor
	 * @param name
	 */
	public CrystalFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new CrystalInstance(ghost);
	}

	@Override
	public Entity spawn(int x, int y) {
		return build(x * MapRenderer.TILE_SIZE, y * MapRenderer.TILE_SIZE);
	}

	@Override
	public boolean isAffectedBy(BuildingFeature feature) {
		return feature instanceof FactoryBuildingFeature;
	}

	@Override
	public LayersFeature getAppearance(Building building) {
		if (building != null && ((CrystalInstance) building).hasRemaining()) {
			return super.getAppearance(building);
		} else {
			return super.getDeathAppearance();
		}
	}

	@Override
	public boolean removeAfterSpawn() {
		// Leave the exclude tiles in place so gidrahs avoid crystals
		return false;
	}
}
