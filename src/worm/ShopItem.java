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

import org.lwjgl.util.ReadablePoint;

import worm.features.LayersFeature;

import com.shavenpuppy.jglib.sprites.Appearance;

/**
 * Things that can appear in the shop
 */
public interface ShopItem {

	/**
	 * What does it look like?
	 * @return a LayersFeature to show this thing in the shop
	 */
	LayersFeature getShopAppearance();

	/**
	 * Offset to draw the thing at in the shop to compensate for its size
	 * @return a ReadablePoint (may not be null)
	 */
	ReadablePoint getShopOffset();

	/**
	 * How much does it cost / how many do we have?
	 * @return int
	 */
	int getShopValue();

	/**
	 * What is this things initial value?
	 * @return int
	 */
	int getInitialValue();

	/**
	 * Is this thing available?
	 * @return true if we can click on this thing in the shop
	 */
	boolean isAvailable();

	/**
	 * Get the title of this thing for the shop
	 * @return String
	 */
	String getTitle();

	/**
	 * Get the shorter title of this thing for the shop, or return title
	 * @return String
	 */
	String getShortTitle();

	/**
	 * Get the description of this thing for the shop
	 * @return String
	 */
	String getDescription();

	/**
	 * Get the (optional) "bonus text" for this thing
	 * @return String
	 */
	String getBonusDescription();

	/**
	 * Icon name
	 * @return int
	 */
	String getShopIcon();

	/**
	 * When clicked in the shop, do this
	 */
	void onClickedInShop();

	/**
	 * Should this option be enabled in the shop?
	 * @return boolean
	 */
	boolean isEnabledInShop();

	/**
	 * Get the number of this item available
	 * @return integer
	 */
	int getInventory();

	/**
	 * @return the number of this item automatically manufactured per level
	 */
	int getNumAvailable();

	/**
	 * @return the graphic to use for the toolip shortcut key
	 */
	Appearance getTooltipGraphic();
}
