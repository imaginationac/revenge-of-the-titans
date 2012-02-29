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
package worm.features;

import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Point;
import org.lwjgl.util.Rectangle;

import worm.Barracks;
import worm.brains.BrainFeature;
import worm.entities.Unit;
import worm.weapons.WeaponFeature;

import com.shavenpuppy.jglib.resources.Feature;

/**
 * $Id: UnitFeature.java,v 1.3 2010/05/15 18:58:07 chazeem Exp $
 * Describes a player unit
 * @author $Author: chazeem $
 * @version $Revision: 1.3 $
 */
public class UnitFeature extends Feature {

	private static final long serialversionUID = 1L;

	/*
	 * Feature data
	 */

	/** Appearance */
	private LayersFeature appearance;

	/** Death appearance */
	private LayersFeature deathAppearance;

	/** Hit points */
	private int hitPoints;

	/** Movement duration (lower = faster) */
	private int speed;

	/** Size */
	private Rectangle bounds;

	/** Offset to sprites */
	private Point offset;

	/** Weapon */
	private String weapon;

	/** Brain */
	private String brain;

	/** Range to shoot */
	private float range;

	/** Aerial targets */
	private boolean aerialTargets;

	/** Repair drone flag */
	private boolean repair;

	/** Repair beam emitter start */
	private EmitterFeature beamStartEmitter;

	/** Repair beam emitter start */
	private EmitterFeature beamEndEmitter;

	/** Repair interval */
	private int repairInterval;

	/** Buffed repair interval */
	private int buffedRepairInterval;

	/*
	 * Transient data
	 */

	private transient WeaponFeature weaponFeature;
	private transient BrainFeature brainFeature;


	/**
	 * @param name
	 */
	public UnitFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return Returns the appearance.
	 */
	public LayersFeature getAppearance() {
		return appearance;
	}

	/**
	 * @return the deathAppearance
	 */
	public LayersFeature getDeathAppearance() {
		return deathAppearance;
	}

	/**
	 * Spawn a new unit at the specified location. No other unit may be present at the specified location.
	 * @param screen
	 * @return the new Unit
	 */
	public final Unit spawn(Barracks barracks, Screen screen, float mapX, float mapY) {
		Unit ret = new Unit(barracks, this, mapX, mapY);
		ret.spawn(screen);
		return ret;
	}

	/**
	 * @return the hitPoints
	 */
	public final int getHitPoints() {
		return hitPoints;
	}

	/**
	 * @return the bounds
	 */
	public Rectangle getBounds() {
		return bounds;
	}
	/**
	 * @return the sprite offset
	 */
	public Point getOffset() {
		return offset;
	}

	/**
	 * @return
	 */
	public WeaponFeature getWeapon() {
		return weaponFeature;
	}

	/**
	 * @return
	 */
	public BrainFeature getBrain() {
		return brainFeature;
	}

	/**
	 * @return the speed
	 */
	public int getSpeed() {
		return speed;
	}

	/**
	 * @return the range
	 */
	public float getRange() {
		return range;
	}

	public boolean isAerialTargets() {
	    return aerialTargets;
    }

	public boolean isRepairDrone() {
	    return repair;
    }

	public EmitterFeature getBeamEndEmitter() {
	    return beamEndEmitter;
    }

	public EmitterFeature getBeamStartEmitter() {
	    return beamStartEmitter;
    }

	public int getRepairInterval() {
	    return repairInterval;
    }

	public int getBuffedRepairInterval() {
	    return buffedRepairInterval;
    }

}