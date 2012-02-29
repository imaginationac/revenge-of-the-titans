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
package worm.powerups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.util.Color;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadablePoint;

import worm.GameStateInterface;
import worm.ShopItem;
import worm.Worm;
import worm.features.LayersFeature;

import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.sprites.Appearance;

/**
 * $Id: PowerupFeature.java,v 1.25 2010/10/16 02:17:16 foo Exp $
 *
 * @author $Author: foo $
 * @version $Revision: 1.25 $
 */
public abstract class PowerupFeature extends Feature implements ShopItem {

	private static final long serialVersionUID = 3805954676462402870L;

	/** All powerups */
	private static final ArrayList<PowerupFeature> POWERUPS = new ArrayList<PowerupFeature>();

	/*
	 * Resource data
	 */

	private String title, shortTitle;

	private String collect;
	private LayersFeature appearance, shopAppearance, collectAppearance, vanishAppearance;
	private String description, tooltipGraphic;

	private Color labelColorStart, labelColorEnd;

	/** where the building goes in shop */
	private int shopIndex;

	/** icon to use */
	private String shopIcon;

	/** whether we've got a shortcut */
	private boolean shortcut;

	/** hint */
	private String hint;

	/** Difficulty contribution */
	private float difficulty;

	private transient ALBuffer collectResource;
	private transient Appearance tooltipGraphicResource;


	/**
	 * @param name
	 */
	public PowerupFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return the hint
	 */
	public String getHint() {
		return hint;
	}

	@Override
	protected void doRegister() {
		POWERUPS.add(this);
	}

	@Override
	protected void doDeregister() {
		POWERUPS.remove(this);
	}

	public float getDifficulty() {
	    return difficulty;
    }

	/**
	 * Perform actions necessary when powerup is used.
	 * @param gsi Game state
	 */
	public void activate(GameStateInterface gsi) {
	}

	/**
	 * If we're collectable (like most powerups), return true here. Otherwise activate is called immediately on collection
	 * and displayes the {@link #getDescription()} text in a label.
	 */
	public boolean isCollectable() {
		return true;
	}

	/**
	 * @return the label
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
	 * @return Returns the shop Index
	 */
	public int getShopIndex() {
		return shopIndex;
	}

	/**
	 * @return Returns the shop icon
	 */
	@Override
	public String getShopIcon() {
		return shopIcon;
	}

	/* (non-Javadoc)
	 * @see worm.ShopItem#getDescription()
	 */
	@Override
	public final String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see worm.ShopItem#getBonusDescription()
	 */
	@Override
	public String getBonusDescription() {
		return null;
	}

	/* (non-Javadoc)
	 * @see worm.ShopItem#getShopAppearance()
	 */
	@Override
	public final LayersFeature getShopAppearance() {
		return shopAppearance;
	}

	/* (non-Javadoc)
	 * @see worm.ShopItem#onClickedInShop()
	 */
	@Override
	public void onClickedInShop() {
		Worm.getGameState().usePowerup(this);
	}

	/**
	 * @return the collect
	 */
	public ALBuffer getCollectSound() {
		return collectResource;
	}

	/* (non-Javadoc)
	 * @see worm.ShopItem#isEnabledInShop()
	 */
	@Override
	public boolean isEnabledInShop() {
		return getShopValue() > 0;
	}

	@Override
	public int getInventory() {
		return Worm.getGameState().getNumPowerups(this);
	}

	@Override
	public int getShopValue() {
		return Worm.getGameState().getNumPowerups(this);
	}

	/**
	 * @return the appearance
	 */
	public LayersFeature getAppearance() {
		return appearance;
	}

	/**
	 * @return the collectAppearance
	 */
	public LayersFeature getCollectAppearance() {
		return collectAppearance;
	}

	/**
	 * @return the vanishAppearance
	 */
	public LayersFeature getVanishAppearance() {
		return vanishAppearance;
	}

	/**
	 * @return the labelColorStart
	 */
	public Color getLabelColorStart() {
		return labelColorStart;
	}

	/**
	 * @return the labelColorStart
	 */
	public Color getLabelColorEnd() {
		return labelColorEnd;
	}



	@Override
	public boolean isAvailable() {
		return true;
	}

	/**
	 * Gets all the powerups in the game, listed in the order they appear in the XML config
	 * @return an unmodifiable List of powerups
	 */
	public static List<PowerupFeature> getPowerups() {
		return Collections.unmodifiableList(POWERUPS);
	}

	@Override
	public ReadablePoint getShopOffset() {
		return new Point(0, 0);
	}

	public boolean hasShortCut() {
		return shortcut;
	}

	@Override
	public Appearance getTooltipGraphic() {
		return tooltipGraphicResource;
	}

	@Override
	public int getNumAvailable() {
	    return 0;
	}

	@Override
	public int getInitialValue() {
	    return 0;
	}
}
