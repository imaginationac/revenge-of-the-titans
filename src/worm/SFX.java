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
package worm;

import net.puppygames.applet.Game;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.util.Util;

/**
 * $Id: SFX.java,v 1.15 2010/10/16 02:17:16 foo Exp $
 * Raider sound effects. Autocreated.
 * <p>
 * @author $Author: foo $
 * @version $Revision: 1.15 $
 */
public class SFX extends net.puppygames.applet.effects.SFX {

	private static final long serialVersionUID = 1L;

	private static final int NUM_BASHES = 5;
	private static final int NUM_BUILDING_DESTROYED = 6;
	private static final int NUM_RICOCHETS = 12;

	/** Sound effects instance */
	private static SFX instance;

	/*
	 * Sound effects names
	 */

	// Sound effects
	private String
		noAmmo = "noAmmo.buffer",
		build = "build.buffer",
		reload = "reload.buffer",
		reloaded = "reloaded.buffer",
		pickup = "pickup.buffer",
		cantBuild = "cantBuild.buffer",
		insufficientFunds = "insufficientFunds.buffer",
		factoryShutdown = "factoryShutdown.buffer",
		baseAttacked = "baseAttacked.buffer",
		baseDestructionImminent = "baseDestructionImminent.buffer",
		crystalSpawned = "crystalSpawned.buffer",
		sold = "sold.buffer",
		medalAwarded = "medalAwarded.buffer",
		achievementUnlocked = "achievementUnlocked.buffer",
		newRank = "newRank.buffer",
		blastMinePip = "blastMinePip.buffer",
		blastMineReady = "blastMineReady.buffer",
		smartbomb = "smartbomb.buffer",
		repair = "repair.buffer"
		;



	/*
	 * Created sound effects
	 */

	private transient ALBuffer
		noAmmoBuffer,
		buildBuffer,
		reloadBuffer,
		reloadedBuffer,
		pickupBuffer,
		cantBuildBuffer,
		insufficientFundsBuffer,
		factoryShutdownBuffer,
		baseAttackedBuffer,
		baseDestructionImminentBuffer,
		crystalSpawnedBuffer,
		soldBuffer,
		medalAwardedBuffer,
		achievementUnlockedBuffer,
		newRankBuffer,
		blastMinePipBuffer,
		blastMineReadyBuffer,
		smartbombBuffer,
		repairBuffer
		;

	private transient ALBuffer[]
	    bashBuffer,
	    bashDistantBuffer,
	    buildingDestroyedBuffer,
	    buildingDestroyedDistantBuffer,
	    ricochetBuffer
	    ;

	/**
	 * C'tor
	 */
	public SFX() {
	}

	@Override
	protected void doRegister() {
		super.doRegister();
		instance = this;
	}

	@Override
	protected void doDeregister() {
		super.doDeregister();
		instance = null;
	}

	public static void buildingDamaged(float x, float y, float gain) {
		int bash = Util.random(0, NUM_BASHES - 1);
		float pitch = (float) Math.random() / 20.0f;
		Game.allocateSound(instance.bashDistantBuffer[bash], Worm.calcGain(x, y) * gain, 1.0f - pitch, Game.class);
		Game.allocateSound(instance.bashBuffer[bash], Worm.calcGain(x, y) * gain, 1.0f - pitch, Game.class);
	}
	public static void buildingDestroyed(float x, float y, float gain) {
		int bash = Util.random(0, NUM_BUILDING_DESTROYED - 1);
		Game.allocateSound(instance.buildingDestroyedDistantBuffer[bash], Worm.calcGain(x, y) * gain, 1.0f, Game.class);
		Game.allocateSound(instance.buildingDestroyedBuffer[bash], Worm.calcGain(x, y) * gain, 1.0f, Game.class);
	}
	public static void ricochet(float x, float y, float gain) {
		int ric = Util.random(0, NUM_RICOCHETS - 1);
		Game.allocateSound(instance.ricochetBuffer[ric], Worm.calcGain(x, y) * gain, 1.0f, Game.class);
	}
	public static void blastMinePip(float x, float y) {
		Game.allocateSound(instance.blastMinePipBuffer, Worm.calcGain(x, y), 1.0f, Game.class);
	}
	public static void blastMineReady(float x, float y) {
		Game.allocateSound(instance.blastMineReadyBuffer, Worm.calcGain(x, y), 1.0f, Game.class);
	}
	public static void crystalSpawned(float x, float y) {
		Game.allocateSound(instance.crystalSpawnedBuffer, Worm.calcLoudGain(x, y), 1.0f, Game.class);
	}
	public static void reload() {
		Game.allocateSound(instance.reloadBuffer);
	}
	public static void reloaded() {
		Game.allocateSound(instance.reloadedBuffer);
	}
	public static void noAmmo() {
		Game.allocateSound(instance.noAmmoBuffer);
	}
	public static void sold() {
		Game.allocateSound(instance.soldBuffer);
	}
	public static void pickup() {
		Game.allocateSound(instance.pickupBuffer);
	}
	public static void repair(float x, float y) {
		Game.allocateSound(instance.repairBuffer, Worm.calcGain(x, y), 1.0f, Game.class);
	}
	public static void cantBuild() {
		Game.allocateSound(instance.cantBuildBuffer);
	}
	public static void insufficientFunds() {
		Game.allocateSound(instance.insufficientFundsBuffer);
	}
	public static void factoryShutdown() {
		Game.allocateSound(instance.factoryShutdownBuffer);
	}
	public static void baseAttacked() {
		Game.allocateSound(instance.baseAttackedBuffer);
	}
	public static SoundEffect baseDestructionImminent() {
		return Game.allocateSound(instance.baseDestructionImminentBuffer);
	}
	public static SoundEffect medalAwarded() {
		return Game.allocateSound(instance.medalAwardedBuffer);
	}
	public static SoundEffect achievementUnlocked() {
		return Game.allocateSound(instance.achievementUnlockedBuffer);
	}
	public static SoundEffect newRank() {
		return Game.allocateSound(instance.newRankBuffer);
	}
	public static SoundEffect smartbomb() {
		return Game.allocateSound(instance.smartbombBuffer);
	}


	@Override
	protected void doCreate() {
		super.doCreate();

		bashBuffer = new ALBuffer[NUM_BASHES];
		bashDistantBuffer = new ALBuffer[NUM_BASHES];
		for (int i = 0; i < NUM_BASHES; i ++) {
			bashBuffer[i] = (ALBuffer) Resources.get("bash"+(i+1)+".buffer");
			bashDistantBuffer[i] = (ALBuffer) Resources.get("bashDistant"+(i+1)+".buffer");
		}


		buildingDestroyedBuffer = new ALBuffer[NUM_BUILDING_DESTROYED];
		buildingDestroyedDistantBuffer = new ALBuffer[NUM_BUILDING_DESTROYED];
		for (int i = 0; i < NUM_BUILDING_DESTROYED; i ++) {
			buildingDestroyedBuffer[i] = (ALBuffer) Resources.get("crumble"+(i+1)+".buffer");
			buildingDestroyedDistantBuffer[i] = (ALBuffer) Resources.get("crumble"+(i+1)+"distant.buffer");
		}

		ricochetBuffer = new ALBuffer[NUM_RICOCHETS];
		for (int i = 0; i < NUM_RICOCHETS; i ++) {
			ricochetBuffer[i] = (ALBuffer) Resources.get("ric"+i+".buffer");
		}

	}

	@Override
	protected void doDestroy() {
		super.doDestroy();
	}

	public static void build() {
		Game.allocateSound(instance.buildBuffer);
	}
}