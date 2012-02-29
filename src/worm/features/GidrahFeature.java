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

import java.util.List;
import java.util.StringTokenizer;

import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Point;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Element;

import worm.Res;
import worm.Statistics;
import worm.Worm;
import worm.brains.BrainFeature;
import worm.brains.TacticalBrainFeature;
import worm.entities.Gidrah;
import worm.entities.Movement;
import worm.weapons.WeaponFeature;

import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.Range;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.util.Util;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * $Id: GidrahFeature.java,v 1.47 2010/10/14 22:53:20 foo Exp $
 * Describes a gidrah
 * @author $Author: foo $
 * @version $Revision: 1.47 $
 */
public class GidrahFeature extends Feature implements Statistics {

	private static final long serialversionUID = 1L;

	public static final int SPAWN_TYPE_NONE = 0;
	public static final int SPAWN_TYPE_CONSTANT = 1;
	public static final int SPAWN_TYPE_EXPLODE = 2;

	/*
	 * Feature data
	 */

	/** Latin name */
	@Data
	private String latinName;

	/** Appearance */
	private LayersFeature appearance;

	/** Attack appearance */
	private LayersFeature attackAppearance, attackAppearanceLeft, attackAppearanceRight;

	/** Frozen appearance */
	private ResourceArray frozenAppearance;

	/** Frozen appearance */
	private LayersFeature idleAppearance;

	/** Death appearance */
	private LayersFeature deathAppearance, deathAppearanceLeft, deathAppearanceRight;

	/** Stunned appearance */
	private LayersFeature stunAppearance;

	/** Death sound */
	private String death;

	/** Hit points */
	private int hitPoints;

	private int hitPointsX;
	private int hitPointsY;

	/** Armour */
	private int armour;

	/** Points value */
	private int points;

	/** Movement speed (pixels/sec) */
	private float speed;

	/** Pause duration */
	private int pause;

	/** Squares to move */
	private int moves;

	/** Diagonal movement preference */
	private boolean diagonal;

	/** Size, if rectangular */
	private Rectangle bounds;

	/** Offset to sprites */
	private Point offset;

	/** Offset for laser beams */
	private Point beamOffset;

	/** Weapon, if any */
	private String weapon;

	/** Brain */
	private String brain;

	/** Angry? Angry gidrahs can attack multiple times before dying */
	private boolean angry;

	/** Wraith? Wraiths can only be harmed by capacitors, and pass through barriers and over mines */
	private boolean wraith;

	/** Is this a boss gidrah? */
	private boolean boss;

	/** Is this a flying gidrah? Flying gidrahs fly directly over any buildings and tiles. They must be armed with guns to be effective. */
	private boolean flying;

	/** If we're a spawning gidrah, what gidrah do we spawn? */
	private String spawn;

	/** If we're a spawning gidrah, what sort of spawning do we do? 1 = constant, 2 = explode on death */
	private int spawnType;

	/** Spawn rate (constant) or number-to-spawn (explode on death) */
	private Range spawnRate;

	/** Spawn distance */
	private Range spawnDistance;

	/** Are we a gidlet? Gidlets run through barriers and mines and aren't targeted by turrets */
	private boolean gidlet;

	/** Are we a shielded gidrah? Shielded gidrahs must have their shields disabled by explosives */
	private boolean shielded;

	/** If we're a shielded gidrah, what does our shield look like? */
	private String shieldAnimation;

	/** Deflect emitter */
	private String deflectEmitter;
	private int deflectXOffset;
	private int deflectYOffset;

	/** Roar */
	private String roar;

	/** Ambient roar */
	private String amb;

	/** Explody? */
	private boolean exploding;

	/** Explosion radius */
	private float explosionRadius;

	/** Strength */
	private int strength;

	/** Bomb */
	private String bomb;

	/** Flying height */
	private float height;

	/** Drop attack - flying gidrah drops on its target to attack it */
	private boolean dropAttack;

	/** Weapon range */
	private float minWeaponRange, maxWeaponRange;

	/** Can gid be stunned when attacking? */
	private boolean noStunOnAttack;

	/** Medal to award when killing boss */
	private String medal;

	/** Minimum level (endless) */
	private int minLevel;

	/** Required research (endless) */
	private String[] research;

	/** Disruptor proofing */
	private boolean disruptorProof;

	/*
	 * Transient data
	 */


	private transient EmitterFeature deflectEmitterResource;
	private transient ALBuffer deathBuffer;
	private transient WeaponFeature weaponFeature;
	private transient BrainFeature brainFeature;
	private transient GidrahFeature spawnFeature;
	private transient Appearance shieldAnimationResource;
	private transient ALBuffer roarBuffer;
	private transient ALBuffer ambBuffer;
	private transient BombFeature bombFeature;

	/**
	 * @param name
	 */
	public GidrahFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return the noStunOnAttack
	 */
	public boolean getNoStunOnAttack() {
		return noStunOnAttack;
	}

	/**
	 * @return the deathBuffer
	 */
	public ALBuffer getDeathBuffer() {
		return deathBuffer;
	}

	/**
	 * @return the minWeaponRange
	 */
	public float getMinWeaponRange() {
		return minWeaponRange;
	}

	/**
	 * @return the maxWeaponRange
	 */
	public float getMaxWeaponRange() {
		return maxWeaponRange;
	}

	/**
	 * @return the dropAttack
	 */
	public boolean isDropAttack() {
		return dropAttack;
	}

	/**
	 * @return the bombFeature
	 */
	public BombFeature getBomb() {
		return bombFeature;
	}

	/**
	 * @return the height
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * @return the strength
	 */
	public int getStrength() {
		return strength;
	}

	/**
	 * @return the explosionRadius
	 */
	public float getExplosionRadius() {
		return explosionRadius;
	}

	/**
	 * @return the exploding
	 */
	public boolean isExploding() {
		return exploding;
	}

	/**
	 * @return the roarBuffer
	 */
	public ALBuffer getRoar() {
		return roarBuffer;
	}

	/**
	 * @return the ambBuffer
	 */
	public ALBuffer getAmbientRoar() {
		return ambBuffer;
	}

	/**
	 * @return Returns the appearance.
	 */
	public LayersFeature getAppearance() {
		return appearance;
	}

	public LayersFeature getIdleAppearance() {
		return idleAppearance;
	}

	/**
	 * @return the deathAppearance
	 */
	public LayersFeature getDeathAppearance() {
		return deathAppearance;
	}

	public LayersFeature getDeathAppearanceLeft() {
		return deathAppearanceLeft;
	}

	public LayersFeature getDeathAppearanceRight() {
		return deathAppearanceRight;
	}

	/**
	 * @return the attackAppearance
	 */
	public LayersFeature getAttackAppearance() {
		return attackAppearance;
	}

	/**
	 * @return the attackAppearanceLeft
	 */
	public LayersFeature getAttackAppearanceLeft() {
		return attackAppearanceLeft;
	}

	/**
	 * @return the attackAppearanceRight
	 */
	public LayersFeature getAttackAppearanceRight() {
		return attackAppearanceRight;
	}

	/**
	 * @return the frozenAppearance
	 */
	public LayersFeature getFrozenAppearance() {
		return (LayersFeature) frozenAppearance.getResource(Util.random(0, frozenAppearance.getNumResources() - 1));
	}

	/**
	 * @return the stunAppearance
	 */
	public LayersFeature getStunAppearance() {
		if (stunAppearance!=null) {
			return stunAppearance;
		} else {
			return idleAppearance;
		}
	}

	/**
	 * Spawn a new Gidrah at the specified location. No other gidrah may be present at the specified location.
	 * @param screen
	 * @return the new Gidrah
	 */
	public final Gidrah spawn(Screen screen, int tileX, int tileY, int type) {
		Gidrah ret = new Gidrah(this, tileX, tileY, type);
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
	 * @return the hitPoints
	 */
	public final int getHitPointsX() {
		return hitPointsX;
	}

	/**
	 * @return the hitPoints
	 */
	public final int getHitPointsY() {
		return hitPointsY;
	}

	/**
	 * @return armour points
	 */
	public int getArmour() {
		return armour;
	}

	/**
	 * @return the points
	 */
	public final int getPoints() {
		return points;
	}

	/**
	 * @return true if this is a wraith
	 */
	public boolean isWraith() {
		return wraith;
	}

	/**
	 * @return true if this is a boss gidrah
	 */
	public boolean isBoss() {
		return boss;
	}

	/**
	 * @return the bounds
	 */
	public Rectangle getBounds() {
		return bounds;
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
	 * @return the minSpeed
	 */
	public float getSpeed() {
		return speed;
	}

	/**
	 * @return the minPause
	 */
	public int getPause() {
		return pause;
	}

	/**
	 * @return the angry
	 */
	public boolean isAngry() {
		return angry;
	}

	/**
	 * @return the gidlet
	 */
	public boolean isGidlet() {
		return gidlet;
	}

	/**
	 * @return the flying
	 */
	public boolean isFlying() {
		return flying;
	}

	/**
	 * @return the shielded
	 */
	public boolean isShielded() {
		return shielded;
	}

	/**
	 * @return the spawnFeature
	 */
	public GidrahFeature getSpawn() {
		return spawnFeature;
	}

	/**
	 * @return the spawnType
	 */
	public int getSpawnType() {
		return spawnType;
	}

	/**
	 * @return the spawnRate
	 */
	public Range getSpawnRate() {
		return spawnRate;
	}

	public Point getOffset() {
		return offset;
	}

	public Point getBeamOffset() {
		return beamOffset;
	}

	/**
	 * @return the deflectEmitterResource
	 */
	public EmitterFeature getDeflectEmitter() {
		if (deflectEmitterResource!=null) {
			return deflectEmitterResource;
		} else {
			return Res.getDeflectEmitter();
		}
	}

	public int getDeflectXOffset() {
		return deflectXOffset;
	}

	public int getDeflectYOffset() {
		return deflectYOffset;
	}

	/**
	 * @return the diagonal
	 */
	public boolean getDiagonal() {
		return diagonal;
	}

	/**
	 * @return the relative value of the gidrah
	 */
	public int getValue() {
		int ret = hitPoints;

		ret += armour * 10;
		ret += strength * 5;

		if (angry) {
			ret *= 2;
		}
		if (flying) {
			ret *= 2;
		}
		if (weapon != null) {
			ret *= 2;
		}
		if (wraith) {
			ret *= 2;
		}
		if (brainFeature instanceof TacticalBrainFeature) {
			ret *= 2;
		}
		if (spawn != null) {
			ret *= 2;
		}

		return ret;
	}

	/**
	 * @return the medal
	 */
	public String getMedal() {
		return medal;
	}

	/**
	 * @return the minMove
	 */
	public int getMoves() {
		return moves;
	}

	/**
	 * Is this gidrah unlocked in Endless mode?
	 * @return boolean
	 */
	public boolean isUnlocked() {
//		System.out.println("Testing "+this);
		if (Worm.getGameState().getLevel() >= minLevel) {
			// Automatically unlock after a certain level
//			System.out.println("--minlevel satisfied:"+minLevel);
			return true;
		}

		// All research must be present
		outer: for (int i = 0; i < research.length; i ++) {
			StringTokenizer st = new StringTokenizer(research[i], " |");
//			System.out.println(" research: "+research[i]);
			while (st.hasMoreTokens()) {
				String r = st.nextToken();
				if (Worm.getGameState().isResearched(r)) {
//					System.out.println("--"+r+" is researched");
					continue outer;
				}
			}
//			System.out.println("  !!!Not available");
			return false;
		}

		return true;
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> children = XMLUtil.getChildren(element, "research");
		if (children.size() > 0) {
			research = new String[children.size()];
			int count = 0;
			for (Element child : children) {
				research[count ++] = XMLUtil.getText(child, "").trim();
			}
		}
	}

	public boolean isDisruptorProof() {
	    return disruptorProof;
    }

	public Range getSpawnDistance() {
		return spawnDistance;
	}

	@Override
	public String toString() {
		return "GidrahFeature["+getName()+"]";
	}

		// titan:\n{font:smallfont.glfont}GIDRUS GIDRUS{font:tinyfont.glfont}\n\n hitpoints: 2-6\n armour: NONE\n strength: 3\n speed: 20-40 MPH\nweight: 30 T

	@Override
	public void appendFullStats(StringBuilder dest) {
		appendTitle(dest);
		dest.append("\n\n");
		appendBasicStats(dest);
	}

	@Override
	public void appendTitle(StringBuilder dest) {
		dest.append("titan:\n{font:smallfont.glfont}");
		dest.append(latinName);
		dest.append("{font:tinyfont.glfont}");
	}

	@Override
	public void appendBasicStats(StringBuilder dest) {
		dest.append(Game.getMessage("ultraworm.gidrahstats.hitpoints")+": ");
		if (wraith) {
			dest.append(Game.getMessage("ultraworm.gidrahstats.unknown"));
		} else {
			dest.append(hitPoints);
			dest.append("+");
		}

		dest.append("\n"+Game.getMessage("ultraworm.gidrahstats.armour")+": ");
		if (wraith) {
			dest.append(Game.getMessage("ultraworm.gidrahstats.unknown"));
		} else if (armour > 0) {
			dest.append(armour);
		} else {
			dest.append(Game.getMessage("ultraworm.gidrahstats.none"));
		}

		dest.append("\n"+Game.getMessage("ultraworm.gidrahstats.strength")+": ");
		if (wraith) {
			dest.append(Game.getMessage("ultraworm.gidrahstats.unknown"));
		} else {
			dest.append(strength);
		}

		dest.append("\n"+Game.getMessage("ultraworm.gidrahstats.speed")+": ");
		if (wraith) {
			dest.append(Game.getMessage("ultraworm.gidrahstats.unknown"));
		} else {
			int minSpeedMph, maxSpeedMph;
			minSpeedMph = (int) (speed * 2f);
			maxSpeedMph = (int) (speed * 2f * Movement.MAX_SPEED_MULTIPLIER);
			if (minSpeedMph > 50) {
				minSpeedMph -= minSpeedMph % 10;
			} else if (minSpeedMph > 10) {
				minSpeedMph -= minSpeedMph % 5;
			}
			if (maxSpeedMph > 50) {
				maxSpeedMph -= maxSpeedMph % 10;
			} else if (maxSpeedMph > 10) {
				maxSpeedMph -= maxSpeedMph % 5;
			}
			dest.append(minSpeedMph);
			dest.append("-");
			dest.append(maxSpeedMph);
			if (flying) {
				dest.append(" KN");
			} else {
				dest.append(" MPH");
			}
		}

		if (!wraith) {
			dest.append("\n"+Game.getMessage("ultraworm.gidrahstats.weight")+": ");
			int weight;
			if (gidlet) {
				weight = 1;
			} else {
				weight = hitPoints * 5 + 20;
				weight -= weight % 10;
				if (angry) {
					weight *= 3;
					weight += armour * 20;
				} else {
					weight += armour * 5;
				}
				if (flying) {
					weight /= 10;
					if (weight > 5) {
						weight -= weight % 5;
					}
				}
			}
			dest.append(weight);
			dest.append(" T");
		}

	}

}