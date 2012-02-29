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

import net.puppygames.applet.screens.AbstractSlotEffect;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.Rectangle;

import worm.Res;
import worm.Worm;
import worm.WormGameState;
import worm.features.LevelFeature;
import worm.features.WorldFeature;

import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLTextArea;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.resources.MappedColor;

import static org.lwjgl.opengl.GL11.*;

/**
 * Slot effect for the slot screen
 */
public class WormSlotEffect extends AbstractSlotEffect {

	private boolean done;

	private MappedColor unselectedSlotTextColor, hoveredSlotTextColor, selectedSlotTextColor;
	private GLTextArea name, info;
	private Background.Instance background, backgroundSelected, backgroundHovered;

	private static int SLOT_WIDTH = 218;
	private static int SLOT_HEIGHT = 33;

	/**
	 * C'tor
	 */
	public WormSlotEffect() {
	}


	@Override
	protected void doSpawnEffect() {
		unselectedSlotTextColor = new MappedColor("titles.colormap:button-text");
		hoveredSlotTextColor = new MappedColor("titles.colormap:button-text");
		selectedSlotTextColor = new MappedColor("titles.colormap:button-text-on");

		name = new GLTextArea();
		name.setFont(net.puppygames.applet.Res.getTinyFont());
		name.setBounds(8, -1, SLOT_WIDTH-16, SLOT_HEIGHT-4);
		name.setText(getSlot().getName().toUpperCase());

		//int maxLevel = Worm.getPlayerSlot().getPreferences().getInt("maxlevel", 0);
		int maxLevel = Math.min(WormGameState.MAX_LEVELS - 1, Worm.getMaxLevel(getSlot(), WormGameState.GAME_MODE_CAMPAIGN));
		String levelName = LevelFeature.getLevel(maxLevel).getTitle();
		int maxWorld = maxLevel / WormGameState.LEVELS_IN_WORLD;
		int maxMoney = maxLevel > 0 ? Worm.getExtraLevelData(getSlot(), maxLevel - 1, WormGameState.GAME_MODE_CAMPAIGN, "money", 0) : 0;
		String worldName = WorldFeature.getWorld(maxWorld).getTitle();

		info = new GLTextArea();
		info.setFont(net.puppygames.applet.Res.getTinyFont());
		info.setBounds(8, -3, SLOT_WIDTH-16, 20);
		info.setText(worldName+" : "+levelName + (maxMoney > 0 ? " : $"+maxMoney : ""));

		setBackground();
	}

	@Override
	protected void render() {
		glRender(new GLRenderable() {
			@Override
			public void render() {
				glPushMatrix();
				glTranslatef(getX(), getY(), 0.0f);
			}
		});
		background.render(this);
		glRender(new GLRenderable() {
			@Override
			public void render() {
				glEnable(GL_TEXTURE_2D);
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
			}
		});
		WormSlotEffect.this.name.render(this);
		info.render(this);
		glRender(new GLRenderable() {
			@Override
			public void render() {
				glPopMatrix();
			}
		});
	}

	@Override
	public int getDefaultLayer() {
	    return 5;
	}

	private void setBackground() {
		background = null;
		if (isSelected()) {
			background = Res.getSlotSelectedBackground().spawn();
			name.setColour(selectedSlotTextColor);
			info.setColour(selectedSlotTextColor);
			info.setAlpha((int)(selectedSlotTextColor.getAlpha()*0.4));
		} else if (isHovered()) {
			background = Res.getSlotHoveredBackground().spawn();
			name.setColour(hoveredSlotTextColor);
			info.setColour(hoveredSlotTextColor);
			info.setAlpha((int)(hoveredSlotTextColor.getAlpha()*0.4));
		} else {
			background = Res.getSlotBackground().spawn();
			name.setColour(unselectedSlotTextColor);
			info.setColour(unselectedSlotTextColor);
			info.setAlpha((int)(unselectedSlotTextColor.getAlpha()*0.4));
		}
		background.setBounds(new Rectangle(0, 0, SLOT_WIDTH, SLOT_HEIGHT));
	}


	@Override
	public void setEditing(boolean editing) {
		// Ignore for now
	}

	@Override
	protected void doTick() {
		if (getScreen().isBlocked()) {
			return;
		}
		int mx = getScreen().getMouseX();
		int my = getScreen().getMouseY();
		if (mx >= getX() && my >= getY() && mx < getX() + SLOT_WIDTH && my < getY() + SLOT_HEIGHT) {
			setHovered(true);
			if (Mouse.isButtonDown(0)) {
				setSelected(true);
			}
		} else {
			setHovered(false);
		}
	}

	@Override
	protected void onSetSelected() {
		setBackground();
	}

	@Override
	protected void onSetHovered() {
		setBackground();
	}

	@Override
	protected void doRemove() {
		done = true;
	}

	@Override
	public boolean isEffectActive() {
		return !done;
	}


}
