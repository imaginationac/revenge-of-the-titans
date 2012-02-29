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

import java.util.ArrayList;

import net.puppygames.applet.effects.EmitterFeature;
import net.puppygames.applet.screens.DialogScreen;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

import worm.features.GidrahFeature;
import worm.features.LayersFeature;
import worm.features.TileSetFeature;
import worm.generator.MapTemplate;
import worm.tiles.FloorTile;
import worm.tiles.SpawnPoint;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.openal.ALStream;
import com.shavenpuppy.jglib.opengl.GLBaseTexture;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.sprites.Animation;
import com.shavenpuppy.jglib.sprites.SpriteImage;


/**
 * $Id: Res.java,v 1.66 2010/10/16 02:17:16 foo Exp $
 *
 * @author $Author: foo $
 * @version $Revision: 1.66 $
 */
public class Res extends net.puppygames.applet.Res {

	private static final long serialVersionUID = 1L;

	private static Res instance;

	/*
	 * Resource data
	 */

	public static final ReadableColor GREEN = new Color(125, 255, 0);

	private String
		repairEmitter="repair.emitter",
		repairAllEmitter="repairall.emitter",
		buildingDamageEmitter = "buildingdamage.emitter",
		iceShardsSmallEmitter = "iceshards.small.emitter",
		iceShardsAngryEmitter = "iceshards.angry.emitter",
		iceShardsBossEmitter = "iceshards.boss.emitter",
		gidrahPain = "gidrahpain.emitter",
		deflectEmitter = "deflect.emitter",
		buildingSmokeEmitter = "fire.smoke.emitter",
		buildingFlamesEmitter = "ruins.medium.fire.emitter"
		;

	private String
		range = "range.texture",
		solid = "solid.texture",
		dash = "dash.texture",
		beam = "beam.texture",
		laserTexture = "laser.texture"
		;

	private String
		reload = "reload.array",
		ammo = "ammo.array",
		reloadLarge = "reload.large.array",
		ammoLarge = "ammo.large.array",
		energyAmmo = "energy.array",
		money = "money.array",
		bossHitPoints = "bossHitPoints.array"
		;

	private String
		reloadIndicator = "reload.indicator.animation",
		reloadLargeIndicator = "reload.large.indicator.animation",
		iceAnimation = "ice.small.animation",
		mousePointer = "mousepointer.default.layers",
		mousePointerOutOfRange = "mousepointer.outofrange.layers",
		mousePointerCantBuild = "mousepointer.cantbuild.layers",
		mousePointerOnTarget = "mousepointer.ontarget.layers",
		mousePointerOffTarget = "mousepointer.offtarget.layers",
		mousePointerPickup = "mousepointer.pickup.layers",
		mousePointerReload = "mousepointer.reload.layers",
		mousePointerBezerkOnTarget = "mousepointer.bezerk.ontarget.layers",
		mousePointerBezerkOffTarget = "mousepointer.bezerk.offtarget.layers",
		mousePointerBezerkOutOfRange = "mousepointer.bezerk.outofrange.layers",
		mousePointerSmartbomb = "mousepointer.smartbomb.layers",
		mousePointerBlastmine = "mousepointer.blastmine.layers",
		mousePointerSellOff = "mousepointer.sell.off.layers",
		mousePointerSellOn = "mousepointer.sell.on.layers",
		mousePointerGrabOn = "mousepointer.grab.on.layers",
		buildIndicator = "spriteimage.buildindicator",
		beamStart = "spriteimage.proximity.start.01",
		beamEnd = "spriteimage.proximity.end.01"
		;

	private String
		saucerSound = "saucer.buffer",
		redAlertSound = "redAlert.buffer",
		endLevelBonus = "endLevelBonus.buffer",
		capacitor = "capacitor.buffer",
		capacitorStart = "capacitorStart.buffer",
		freezeSound = "freeze.buffer",
		shieldSound = "shields.buffer",
		bezerkSound = "bezerk.buffer",
		repairZapSound = "repairZap.buffer"
		;

	// Smartbomb effect
	private String
		explosionTexture = "explosion.texture",
		smartBombTexture = "smartbomb.texture"
		;

	/** Spawnpoints */
	private String
		northSpawnPoint = "north.spawnpoints",
		eastSpawnPoint = "east.spawnpoints",
		southSpawnPoint = "south.spawnpoints",
		westSpawnPoint = "west.spawnpoints"
		;

	private String
		slotBackground = "slot.background",
		slotHoveredBackground = "slot.hovered.background",
		slotSelectedBackground = "slot.selected.background",
		beamBackground = "proximity.background"
		;


	private String
		floorEdgeTransition = "transition.tileset"
		;

	private String
		arrowNorth = "arrow.north.layers",
		arrowSouth = "arrow.south.layers",
		arrowEast = "arrow.east.layers",
		arrowWest = "arrow.west.layers",
		arrowNorthWest = "arrow.northwest.layers",
		arrowNorthEast = "arrow.northeast.layers",
		arrowSouthWest = "arrow.southwest.layers",
		arrowSouthEast = "arrow.southeast.layers",
		arrowMidSpawner = "arrow.midspawner.layers",
		survivalMapTemplate = "survival.templates",
		survivalBosses = "survival.bosses.array",
		xmasBosses = "xmas.bosses.array",
		xmasGidrahs = "xmas.gidrahs.array",
		xmasAngryGidrahs = "xmas.angrygidrahs.array"
		;

	private String
		researchNagDialog = "research-nag.dialog",
		modeLockedDialog = "mode-locked.dialog",
		ingameInfoDialog = "ingame.info.dialog";
	
	private String factoryMining = "factoryMining.buffer";

	/*
	 * Transient data
	 */

	private transient GLBaseTexture
		smartBombTextureResource,
		explosionTextureResource,
		solidTextureResource,
		dashTextureResource,
		rangeTextureResource,
		beamTextureResource,
		laserTextureResource
		;
	private transient ALBuffer
		saucerSoundBuffer,
		redAlertSoundBuffer,
		endLevelBonusBuffer,
		factoryMiningBuffer,
		capacitorBuffer,
		capacitorStartBuffer,
		freezeSoundBuffer,
		bezerkSoundBuffer,
		shieldSoundBuffer,
		repairZapSoundBuffer
		;
	private transient EmitterFeature
		buildingDamageEmitterFeature,
		gidrahPainEmitterFeature,
		iceShardsSmallEmitterFeature,
		iceShardsAngryEmitterFeature,
		iceShardsBossEmitterFeature,
		deflectEmitterFeature,
		buildingSmokeEmitterFeature,
		buildingFlamesEmitterFeature
		;
	private transient Animation
		iceAnimationResource,
		reloadIndicatorResource,
		reloadLargeIndicatorResource
		;

	private transient SpriteImage
		beamStartResource,
		beamEndResource
		;

	private transient LayersFeature
		mousePointerResource,
		mousePointerCantBuildResource,
		mousePointerOffTargetResource,
		mousePointerOnTargetResource,
		mousePointerReloadResource,
		mousePointerPickupResource,
		mousePointerBezerkOffTargetResource,
		mousePointerBezerkOnTargetResource,
		mousePointerBezerkOutOfRangeResource,
		mousePointerOutOfRangeResource,
		mousePointerSmartbombResource,
		mousePointerBlastmineResource,
		mousePointerSellOnResource,
		mousePointerSellOffResource,
		mousePointerGrabOnResource
		;

	private transient LayersFeature
		arrowNorthLayersFeature,
		arrowSouthLayersFeature,
		arrowEastLayersFeature,
		arrowWestLayersFeature,
		arrowNorthEastLayersFeature,
		arrowNorthWestLayersFeature,
		arrowSouthWestLayersFeature,
		arrowSouthEastLayersFeature,
		arrowMidSpawnerLayersFeature
		;


	private transient SpriteImage
		buildIndicatorResource
		;

	private transient EmitterFeature
		repairEmitterFeature,
		repairAllEmitterFeature
		;

	private transient ResourceArray
		reloadArray,
		ammoArray,
		reloadLargeArray,
		ammoLargeArray,
		energyAmmoArray,
		moneyArray,
		bossHitPointsArray,
		northSpawnPointResource,
		eastSpawnPointResource,
		southSpawnPointResource,
		westSpawnPointResource,
		survivalMapTemplateResource,
		survivalBossesResource,
		xmasBossesResource,
		xmasGidrahsResource,
		xmasAngryGidrahsResource
		;

	private transient ResourceArray[]
	    endlessGidrahsResource,
	    endlessAngryGidrahsResource,
	    survivalGidrahsResource,
	    survivalAngryGidrahsResource
        ;

	private transient Background
		slotBackgroundResource,
		slotHoveredBackgroundResource,
		slotSelectedBackgroundResource,
		beamBackgroundResource
		;


	private transient ALStream[] ambientResource;

	private transient TileSetFeature floorEdgeTransitionResource;

	private transient DialogScreen researchNagDialogResource, modeLockedDialogResource, ingameInfoDialogResource;

	private transient Animation[] quicklaunchCountOff, quicklaunchCountOn, quicklaunchCount10Off, quicklaunchCount10On;

	/**
	 * C'tor
	 */
	public Res() {
	}

	@Override
	protected void doRegister() {
		super.doRegister();
		instance = this;
	}

	@Override
	protected void doDeregister() {
		instance = null;
		super.doDeregister();
	}

	@Override
	protected void doCreate() {
		super.doCreate();

		ambientResource = new ALStream[50];
		for (int i = 0; i < 50; i ++) {
			ambientResource[i] = Resources.get("level"+fmt(i)+".stream");
		}

		endlessGidrahsResource = new ResourceArray[4];
		endlessAngryGidrahsResource = new ResourceArray[4];
		for (int i = 0; i < 4; i ++) {
			endlessGidrahsResource[i] = Resources.get("endless.gidrahs."+i);
			endlessAngryGidrahsResource[i] = Resources.get("endless.angrygidrahs."+i);
		}

		survivalGidrahsResource = new ResourceArray[4];
		survivalAngryGidrahsResource = new ResourceArray[4];
		for (int i = 0; i < 4; i ++) {
			survivalGidrahsResource[i] = Resources.get("survival.gidrahs."+i);
			survivalAngryGidrahsResource[i] = Resources.get("survival.angrygidrahs."+i);
		}

		quicklaunchCountOn = new Animation[10];
		quicklaunchCountOff = new Animation[10];
		quicklaunchCount10On = new Animation[10];
		quicklaunchCount10Off = new Animation[10];
		for (int i = 0; i < 10; i++) {
			quicklaunchCountOff[i] = Resources.get("quicklaunch.counter."+i+".off.animation");
			quicklaunchCountOn[i] = Resources.get("quicklaunch.counter."+i+".on.animation");
			quicklaunchCount10Off[i] = Resources.get("quicklaunch.counter.10."+i+".off.animation");
			quicklaunchCount10On[i] = Resources.get("quicklaunch.counter.10."+i+".on.animation");
		}
	}

	public static Animation getQuicklaunchCountOff(int i) {
		return instance.quicklaunchCountOff[i];
	}

	public static Animation getQuicklaunchCountOn(int i) {
		return instance.quicklaunchCountOn[i];
	}

	public static Animation getQuicklaunchCount10Off(int i) {
		return instance.quicklaunchCount10Off[i];
	}

	public static Animation getQuicklaunchCount10On(int i) {
		return instance.quicklaunchCount10On[i];
	}

	public static GLBaseTexture getSmartBombTexture() {
		return instance.smartBombTextureResource;
	}

	public static ALBuffer getSaucerSound() {
		return instance.saucerSoundBuffer;
	}

	public static EmitterFeature getBuildingDamageEmitter() {
		return instance.buildingDamageEmitterFeature;
	}
	public static LayersFeature getMousePointer() {
		return instance.mousePointerResource;
	}
	public static LayersFeature getMousePointerOnTarget() {
		return instance.mousePointerOnTargetResource;
	}
	public static LayersFeature getMousePointerOffTarget() {
		return instance.mousePointerOffTargetResource;
	}
	public static LayersFeature getMousePointerBezerkOnTarget() {
		return instance.mousePointerBezerkOnTargetResource;
	}
	public static LayersFeature getMousePointerBezerkOffTarget() {
		return instance.mousePointerBezerkOffTargetResource;
	}
	public static LayersFeature getMousePointerCantBuild() {
		return instance.mousePointerCantBuildResource;
	}
	public static LayersFeature getMousePointerSmartbomb() {
		return instance.mousePointerSmartbombResource;
	}
	public static LayersFeature getMousePointerBlastmine() {
		return instance.mousePointerBlastmineResource;
	}
	public static LayersFeature getMousePointerPickup() {
		return instance.mousePointerPickupResource;
	}
	public static LayersFeature getMousePointerReload() {
		return instance.mousePointerReloadResource;
	}
	public static LayersFeature getMousePointerSellOff() {
		return instance.mousePointerSellOffResource;
	}
	public static LayersFeature getMousePointerSellOn() {
		return instance.mousePointerSellOnResource;
	}
	public static GLBaseTexture getExplosionTexture() {
		return instance.explosionTextureResource;
	}
	public static EmitterFeature getGidrahPainEmitter() {
		return instance.gidrahPainEmitterFeature;
	}
	public static Animation getReloadIndicator() {
		return instance.reloadIndicatorResource;
	}
	public static Animation getReloadLargeIndicator() {
		return instance.reloadLargeIndicatorResource;
	}
	public static EmitterFeature getBuildingFlamesEmitter() {
		return instance.buildingFlamesEmitterFeature;
	}
	public static EmitterFeature getBuildingSmokeEmitter() {
		return instance.buildingSmokeEmitterFeature;
	}
	public static LayersFeature getMousePointerBezerkOutOfRange() {
		return instance.mousePointerBezerkOutOfRangeResource;
	}
	public static LayersFeature getMousePointerOutOfRange() {
		return instance.mousePointerOutOfRangeResource;
	}

	public static Animation getIceAnimation() {
		return instance.iceAnimationResource;
	}

	public static EmitterFeature getIceShardsSmallEmitter() {
		return instance.iceShardsSmallEmitterFeature;
	}

	public static EmitterFeature getIceShardsAngryEmitter() {
		return instance.iceShardsAngryEmitterFeature;
	}

	public static EmitterFeature getIceShardsBossEmitter() {
		return instance.iceShardsBossEmitterFeature;
	}


	public static EmitterFeature getRepairEmitter() {
		return instance.repairEmitterFeature;
	}
	public static EmitterFeature getRepairAllEmitter() {
		return instance.repairAllEmitterFeature;
	}


	public static SpriteImage getBuildIndicator() {
		return instance.buildIndicatorResource;
	}

	public static EmitterFeature getDeflectEmitter() {
		return instance.deflectEmitterFeature;
	}

	public static SpriteImage getReload(float ratio) {
		return (SpriteImage) instance.reloadArray.getResource((int) LinearInterpolator.instance.interpolate(instance.reloadArray.getNumResources() - 1, 0, ratio));
	}

	public static SpriteImage getAmmo(int ammo, int maxAmmo) {
		return (SpriteImage) instance.ammoArray.getResource((int) LinearInterpolator.instance.interpolate(0, instance.ammoArray.getNumResources() - 1, (float) ammo / (float) maxAmmo));
	}

	public static SpriteImage getReloadLarge(float ratio) {
		return (SpriteImage) instance.reloadLargeArray.getResource((int) LinearInterpolator.instance.interpolate(instance.reloadLargeArray.getNumResources() - 1, 0, ratio));
	}

	public static SpriteImage getAmmoLarge(int ammo, int maxAmmo) {
		return (SpriteImage) instance.ammoLargeArray.getResource((int) LinearInterpolator.instance.interpolate(0, instance.ammoLargeArray.getNumResources() - 1, (float) ammo / (float) maxAmmo));
	}

	public static SpriteImage getEnergyAmmo(int ammo, int maxAmmo) {
		return (SpriteImage) instance.energyAmmoArray.getResource((int) LinearInterpolator.instance.interpolate(0, instance.energyAmmoArray.getNumResources() - 1, (float) ammo / (float) maxAmmo));
	}

	public static SpriteImage getMoney(int tick, int productionRate) {
		if (tick == 0) {
			return (SpriteImage) instance.moneyArray.getResource(instance.moneyArray.getNumResources() - 1);
		} else {
			return (SpriteImage) instance.moneyArray.getResource((int) LinearInterpolator.instance.interpolate(instance.moneyArray.getNumResources(), 0, (float) tick / (float) productionRate));
		}
	}

	public static SpriteImage getBossHitPoints(float ratio) {
		return (SpriteImage) instance.bossHitPointsArray.getResource((int) LinearInterpolator.instance.interpolate(0, instance.bossHitPointsArray.getNumResources() - 1, ratio));
	}

	/**
	 * @return the slotBackgroundResource
	 */
	public static Background getSlotBackground() {
		return instance.slotBackgroundResource;
	}

	/**
	 * @return the slotHoveredBackgroundResource
	 */
	public static Background getSlotHoveredBackground() {
		return instance.slotHoveredBackgroundResource;
	}

	/**
	 * @return the slotSelectedBackgroundResource
	 */
	public static Background getSlotSelectedBackground() {
		return instance.slotSelectedBackgroundResource;
	}

	/**
	 * @return
	 */
	public static ALBuffer getRedAlertSound() {
		return instance.redAlertSoundBuffer;
	}

	public static Background getBeamBackground() {
		return instance.beamBackgroundResource;
	}

	public static SpriteImage getBeamStart() {
		return instance.beamStartResource;
	}

	public static SpriteImage getBeamEnd() {
		return instance.beamEndResource;
	}

	public static ALBuffer getEndLevelBonusSound() {
		return instance.endLevelBonusBuffer;
	}

	public static SpawnPoint getNorthSpawnPoint(int difficulty) {
		return (SpawnPoint) instance.northSpawnPointResource.getResource(difficulty);
	}

	public static SpawnPoint getEastSpawnPoint(int difficulty) {
		return (SpawnPoint) instance.eastSpawnPointResource.getResource(difficulty);
	}

	public static SpawnPoint getSouthSpawnPoint(int difficulty) {
		return (SpawnPoint) instance.southSpawnPointResource.getResource(difficulty);
	}

	public static SpawnPoint getWestSpawnPoint(int difficulty) {
		return (SpawnPoint) instance.westSpawnPointResource.getResource(difficulty);
	}

	public static GLBaseTexture getRangeTexture() {
		return instance.rangeTextureResource;
	}

	/**
	 * @param level
	 * @return
	 */
	public static ALStream getAmbient(int level) {
		return instance.ambientResource[level % instance.ambientResource.length];
	}

	private String fmt(int i) {
		if (i < 10) {
			return "0"+String.valueOf(i);
		} else {
			return String.valueOf(i);
		}
	}


	/**
	 * @return a tile with a gradient for them nice edges
	 */
	public static FloorTile getFloorEdgeTransition(boolean n, boolean ne, boolean e, boolean se, boolean s, boolean sw, boolean w, boolean nw) {
		ArrayList<Tile> a = instance.floorEdgeTransitionResource.getTiles();
		int idx = (n ? 1 : 0) + (ne ? 2 : 0) + (e ? 4 : 0) + (se  ? 8 : 0) + (s  ? 16 : 0) + (sw  ? 32 : 0) + (w  ? 64 : 0) + (nw  ? 128 : 0);
		return (FloorTile) a.get( idx-1 );
	}

	public static LayersFeature getNorthArrow() {
		return instance.arrowNorthLayersFeature;
	}
	public static LayersFeature getSouthArrow() {
		return instance.arrowSouthLayersFeature;
	}
	public static LayersFeature getEastArrow() {
		return instance.arrowEastLayersFeature;
	}
	public static LayersFeature getWestArrow() {
		return instance.arrowWestLayersFeature;
	}
	public static LayersFeature getNorthWestArrow() {
		return instance.arrowNorthWestLayersFeature;
	}
	public static LayersFeature getSouthEastArrow() {
		return instance.arrowSouthEastLayersFeature;
	}
	public static LayersFeature getNorthEastArrow() {
		return instance.arrowNorthEastLayersFeature;
	}
	public static LayersFeature getSouthWestArrow() {
		return instance.arrowSouthWestLayersFeature;
	}
	public static LayersFeature getMidSpawnerArrow() {
		return instance.arrowMidSpawnerLayersFeature;
	}
	public static DialogScreen getResearchNagDialog() {
		return instance.researchNagDialogResource;
	}
	public static DialogScreen getModeLockedDialog() {
		return instance.modeLockedDialogResource;
	}
	public static DialogScreen getIngameInfoDialog() {
		return instance.ingameInfoDialogResource;
	}
	public static ALBuffer getFactoryMiningBuffer() {
		return instance.factoryMiningBuffer;
	}
	public static ALBuffer getCapacitorBuffer() {
		return instance.capacitorBuffer;
	}
	public static ALBuffer getCapacitorStartBuffer() {
		return instance.capacitorStartBuffer;
	}
	public static ResourceArray getEndlessGidrahs(int type) {
		return instance.endlessGidrahsResource[type];
	}
	public static ResourceArray getEndlessAngryGidrahs(int type) {
		return instance.endlessAngryGidrahsResource[type];
	}
	public static ResourceArray getSurvivalGidrahs(int type) {
		return instance.survivalGidrahsResource[type];
	}
	public static ResourceArray getSurvivalAngryGidrahs(int type) {
		return instance.survivalAngryGidrahsResource[type];
	}
	public static ResourceArray getXmasGidrahs() {
		return instance.xmasGidrahsResource;
	}
	public static ResourceArray getXmasAngryGidrahs() {
		return instance.xmasAngryGidrahsResource;
	}
	public static MapTemplate getSurvivalMapTemplate(int worldIndex, int templateIndex) {
		ResourceArray templates = (ResourceArray) instance.survivalMapTemplateResource.getResource(worldIndex);
		return (MapTemplate) templates.getResource(templateIndex);
	}
	public static GidrahFeature getSurvivalBoss(int n) {
		return (GidrahFeature) instance.survivalBossesResource.getResource(n);
	}
	public static GidrahFeature getXmasBoss(int n) {
		return (GidrahFeature) instance.xmasBossesResource.getResource(n);
	}
	public static int getNumSurvivalBosses() {
		return instance.survivalBossesResource.getNumResources();
	}
	public static int getNumXmasBosses() {
		return instance.xmasBossesResource.getNumResources();
	}
	public static MapTemplate getSandboxMapTemplate(int worldIndex, int templateIndex) {
		ResourceArray templates = (ResourceArray) instance.survivalMapTemplateResource.getResource(worldIndex);
		return (MapTemplate) templates.getResource(0);
	}

	public static ALBuffer getFreezeSound() {
		return instance.freezeSoundBuffer;
	}

	public static ALBuffer getBezerkSound() {
		return instance.bezerkSoundBuffer;
	}

	public static ALBuffer getShieldSound() {
		return instance.shieldSoundBuffer;
	}
	public static ALBuffer getRepairZapSound() {
		return instance.repairZapSoundBuffer;
	}

	public static GLBaseTexture getLaserTexture() {
	    return instance.laserTextureResource;
    }

	public static LayersFeature getMousePointerGrab() {
	    return instance.mousePointerGrabOnResource;
    }

	public static GLBaseTexture getSolidTexture() {
	    return instance.solidTextureResource;
    }

	public static GLBaseTexture getDashTexture() {
	    return instance.dashTextureResource;
    }

	public static GLBaseTexture getBeamTexture() {
	    return instance.beamTextureResource;
    }
}
