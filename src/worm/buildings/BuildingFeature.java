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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import net.puppygames.applet.Game;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Point;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.Rectangle;

import worm.ShopItem;
import worm.Statistics;
import worm.Worm;
import worm.entities.Building;
import worm.features.LayersFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.resources.TextResource;
import com.shavenpuppy.jglib.sprites.Appearance;

/**
 * Describes a building
 * @author Cas
 */
public abstract class BuildingFeature extends Feature implements ShopItem, Statistics {

	private static final long serialVersionUID = 1L;

	private static final HashMap<String, BuildingFeature> BUILDINGS = new HashMap<String, BuildingFeature>();
	private static final HashMap<String, BuildingFeature> BUILDINGS_BY_RESEARCH_ID = new HashMap<String, BuildingFeature>();

	public static final int HITPOINTS_DIVISOR = 4;

	/*
	 * Resource data
	 */

	/** Building's research id */
	private String research;

	/** HUD area on game screen */
	private String hud;

	/** Number of hit points the building has */
	private int hitPoints;

	/** Cost in $ to buy the building */
	private int cost;

	/** icon to use */
	private String shopIcon;

	/** Title */
	private String title;

	/** Title */
	private String shortTitle;

	/** Short bonus description - one liner eg. +1 RELOAD RATE */
	private String bonusDescription;

	/** Full description */
	private String description;

	/** Bounding box relative to map location */
	private Rectangle bounds;

	/** Appearance */
	private LayersFeature appearance;

	/** Shielded appearance */
	private LayersFeature shieldedAppearance;

	/** Forcefield appearance */
	private LayersFeature forcefieldAppearance;

	/** Hovered appearance */
	private LayersFeature hoveredAppearance;

	/** Death appearance */
	private LayersFeature deathAppearance;

	/** Shop / info appearance */
	private LayersFeature shopAppearance;

	/** Repair emitter */
	private String repairEmitter;

	/** Build emitter */
	private String buildEmitter;

	/** Is this building "paintable"? */
	private boolean paintable;

	/** Hitpoints graphics */
	private String hitPointsGraphics;

	/** Emitter floor */
	private float floor;

	/** Offset to sprites */
	private Point offset;

	/** Points to the mouseoff tooltip key graphic */
	private String tooltipGraphic;

	/** Number of available units */
	private int numAvailable = 0;

	/** Max available units */
	private int maxAvailable = 0;

	/** Agitation factor */
	private float agitation;

	/** Flames offset */
	private Point flamesOffset;

	/** Flames emitter */
	private String flamesEmitter;

	/*
	 * Transient data
	 */

	private transient ResourceArray hitPointsGraphicsArray;

	private transient EmitterFeature repairEmitterResource;
	private transient EmitterFeature buildEmitterResource;
	private transient EmitterFeature flamesEmitterResource;

	private transient Appearance tooltipGraphicResource;
	private transient TextResource descriptionResource;


	/**
	 * C'tor
	 * @param name
	 */
	public BuildingFeature(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	protected void doRegister() {
		BUILDINGS.put(getName(), this);
		if (research != null) {
			BUILDINGS_BY_RESEARCH_ID.put(research, this);
		}
	}

	@Override
	protected void doDeregister() {
		BUILDINGS.remove(getName());
		if (research != null) {
			BUILDINGS_BY_RESEARCH_ID.remove(research);
		}
	}

	/**
	 * @return an unmodifiable Collection of all the Buildings
	 */
	public static Collection<BuildingFeature> getBuildings() {
		return Collections.unmodifiableCollection(BUILDINGS.values());
	}

	public static BuildingFeature getBuildingByResearchName(String researchName) {
		return BUILDINGS_BY_RESEARCH_ID.get(researchName);
	}

	/**
	 * @return Returns the hitPoints.
	 */
	public int getHitPoints() {
		return hitPoints;
	}

	/**
	 * @return Returns the bounds - may be null if this building uses radius
	 */
	public Rectangle getBounds() {
		return bounds;
	}

	/**
	 * @return Returns the bonusDescription.
	 */
	@Override
	public String getBonusDescription() {
		return bonusDescription;
	}

	/**
	 * @return Returns the shop Icon
	 */
	@Override
	public String getShopIcon() {
		return shopIcon;
	}

	/**
	 * @return Returns the description.
	 */
	@Override
	public String getDescription() {
		return descriptionResource.getText();
	}

	/**
	 * Spawn a building and add it to the game state.
	 * @return a new Building
	 */
	public Building build(float x, float y) {
		Building ret = doSpawn(false);

		if (!(this instanceof ObstacleFeature)) {
			EmitterFeature ef = getBuildEmitter();
			if (ef != null) {
				Emitter e = ef.spawn(GameScreen.getInstance());
				e.setLocation(x+ret.getBounds(null).getWidth()/2, y+(int)(ret.getBounds(null).getWidth()*0.4));
				e.setOffset(GameScreen.getSpriteOffset());
			}
		}

		ret.spawn(GameScreen.getInstance());
		ret.setLocation(x, y);
		ret.onBuild();

		return ret;
	}

	/**
	 * Spawn a building ghost
	 * @return a new ghost Building
	 */
	public Building ghost(float x, float y) {
		Building ret = doSpawn(true);

		ret.spawn(GameScreen.getInstance());
		ret.setLocation(x, y);

		return ret;
	}

	public abstract Building doSpawn(boolean ghost);

	/**
	 * @return Returns the cost.
	 */
	@Override
	public int getShopValue() {
		return cost;
	}

	@Override
    public int getInitialValue() {
		return cost;
	}

	/**
	 * @return Returns the title.
	 */
	@Override
	public String getTitle() {
		return title;
	}

	/**
	 * @return Returns the shorter title or title.
	 */
	@Override
	public String getShortTitle() {
		if (shortTitle!=null) {
			return shortTitle;
		} else {
			return title;
		}
	}

	/**
	 * @return the appearance of a building
	 */
	public LayersFeature getAppearance(Building building) {
		return appearance;
	}

	/**
	 * @return true if the building can be built
	 */
	@Override
	public boolean isAvailable() {
		return Worm.getGameState().isResearched(research);
	}

	/**
	 * @return true if we can "paint" with this building
	 */
	public boolean isPaintable() {
		return paintable;
	}

	/**
	 * Does this sort of building have an effect on us?
	 * @param feature
	 * @return
	 */
	public boolean isAffectedBy(BuildingFeature feature) {
		return false;
	}

	/* (non-Javadoc)
	 * @see worm.ShopItem#getShopAppearance()
	 */
	@Override
	public final LayersFeature getShopAppearance() {
		if (shopAppearance!=null) {
			return shopAppearance;
		} else {
			return appearance;
		}
	}

	/* (non-Javadoc)
	 * @see worm.ShopItem#onClickedInShop()
	 */
	@Override
	public final void onClickedInShop() {
		Worm.getGameState().setBuilding(this);
	}

	@Override
	public boolean isEnabledInShop() {
		return isAvailable() && (getInventory() != 0 || getNumAvailable() == 0);
	}

	/**
	 * @param target
	 * @return
	 */
	public boolean canBuildOnTopOf(BuildingFeature target) {
		return target instanceof BarricadeFeature || target instanceof MinefieldFeature || target instanceof ObstacleFeature;
	}

	/**
	 * @return the hitPointsGraphicsArray
	 */
	public LayersFeature getHitPointsGraphics(int hitPoints) {

		if (hitPoints <= 0 || hitPoints > hitPointsGraphicsArray.getNumResources()) {
			return (LayersFeature) hitPointsGraphicsArray.getResource(0);
		} else {
			return (LayersFeature) hitPointsGraphicsArray.getResource(hitPoints - 1);
		}
	}

	/**
	 * @return the shieldedAppearance
	 */
	public LayersFeature getShieldedAppearance() {
		return shieldedAppearance;
	}

	public LayersFeature getForcefieldAppearance() {
		return forcefieldAppearance;
	}

	/**
	 * @return the hoveredAppearance
	 */
	public LayersFeature getHoveredAppearance() {
		return hoveredAppearance;
	}

	/**
	 * @return the ricochetEmitterResource
	 */
	public EmitterFeature getRepairEmitter() {
		return repairEmitterResource;
	}

	/**
	 * @return the buildEmitterResource
	 */
	public EmitterFeature getBuildEmitter() {
		return buildEmitterResource;
	}

	/**
	 * @return the deathAppearance
	 */
	public LayersFeature getDeathAppearance() {
		return deathAppearance;
	}


	/**
	 * @return the emitter floor height
	 */
	public float getFloor() {
		return floor;
	}

	public Point getOffset() {
		return offset;
	}

	public boolean isFactory() {
		return false;
	}

	@Override
	public ReadablePoint getShopOffset() {
		if (shopAppearance==null) {
			return new Point(-(bounds.getX() + bounds.getWidth()) / 2, -(bounds.getY() + bounds.getHeight() / 2));
		} else {
			return null;
		}
	}

	/**
	 * @return the number of this kind of building available to build (default -1, infinite)
	 */
	@Override
    public int getNumAvailable() {
		return numAvailable;
	}

	/**
	 * @return the maximum number of this kind of building we're allowed to have
	 */
	public int getMaxAvailable() {
	    return maxAvailable;
    }

	@Override
	public final int getInventory() {
		return Worm.getGameState().getAvailableStock(this);
	}

	public String getHUD() {
		return hud;
	}

	@Override
	public Appearance getTooltipGraphic() {
		return tooltipGraphicResource;
	}

	public String getResearchName() {
		return research;
	}

	/**
	 * Gets the text for the research screen stats, placing it in the two incoming StringBuilders, which are cleared.
	 * @param stats_1_text
	 * @param stats_2_text
	 */
	public void getResearchStats(StringBuilder stats_1_text, StringBuilder stats_2_text) {
		stats_1_text.append("{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.cost")+": {font:tinyfont.glfont color:text-bold}$" + getInitialValue());

		if (getHitPoints() > 0) {
			stats_1_text.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.shields")+": {font:tinyfont.glfont color:text-bold}" + getHitPoints() / HITPOINTS_DIVISOR);
		}
		if (getBonusDescription() != null) {
			stats_1_text.append("\n");
			stats_1_text.append(getBonusDescription());
		}
	}

	protected String getBuildingType() {
		return Game.getMessage("ultraworm.researchstats.building_type_normal");
	}

	@Override
	public void appendFullStats(StringBuilder dest) {
		appendTitle(dest);
		dest.append("\n\n");
		appendBasicStats(dest);
	}

	@Override
	public void appendTitle(StringBuilder dest) {
		dest.append(Game.getMessage("ultraworm.researchstats.new")+" ");
		dest.append(getBuildingType());
		dest.append("\n{font:smallfont.glfont}");
		dest.append(shortTitle != null ? shortTitle : title);
		dest.append("{font:tinyfont.glfont}");
	}

	@Override
	public void appendBasicStats(StringBuilder dest) {
		dest.append(Game.getMessage("ultraworm.researchstats.cost_lowercase")+": $");
		dest.append(getInitialValue());
		dest.append("\n"+Game.getMessage("ultraworm.researchstats.hitpoints")+": ");
		dest.append(getHitPoints() / HITPOINTS_DIVISOR);
		if (getNumAvailable() > 0) {
			dest.append("\n"+Game.getMessage("ultraworm.researchstats.production_lowercase")+": ");
			dest.append(getNumAvailable());
		}
	}

	/**
	 * Some buildings make the game harder when put in play.
	 * @return the agitation factor
	 */
	public float getAgitation() {
	    return agitation;
    }

	public Point getFlamesOffset() {
	    return flamesOffset;
    }

	public EmitterFeature getFlamesEmitter() {
	    return flamesEmitterResource;
    }
}
