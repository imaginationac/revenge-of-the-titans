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
package worm.animation;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector3f;
import org.w3c.dom.Element;

import worm.Worm;
import worm.WormGameState;
import worm.entities.Gidrah;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.sprites.Command;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.XMLUtil;
import com.shavenpuppy.jglib.vector.Vector3i;

/**
 * Adjust the rotation angle of a sprite to point at the player
 */
public class LightAngleCommand extends Command {

	public static final long serialVersionUID = 1L;

	private static final Vector3f scratch = new Vector3f();
	private static Vector3i location;

	private int rate;

	/**
	 * Constructor for FrameCommand.
	 */
	public LightAngleCommand() {
		super();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated)
	 */
	@Override
	public boolean execute(Sprite target) {
		int currentSequence = target.getSequence();
		float x = target.getX()+target.getOffset(null).getX();
		float y = target.getY()+target.getOffset(null).getY();
		WormGameState gameState = Worm.getGameState();
		if (gameState == null) {
			target.setSequence(currentSequence + 1);
			return true;
		}

		// pick a random gidrah for now

		Gidrah gidrahTarget = null;
		ArrayList<Gidrah> gidrahs = Worm.getGameState().getGidrahs();
		int n = gidrahs.size();
		if (n == 0) {
			// No gidrahs left!
			target.setVisible(false);
			target.setSequence(currentSequence + 1);
			return true;
		}
		float maxDist = 125.0f;
		float bestDist = maxDist;
		float dist;
		// Find the closest gidrah
		for (int i = 0; i < n; i ++) {
			Gidrah g = gidrahs.get(i);
			if (g.isActive()){

				dist = g.getDistanceTo(x, y);

				if (dist < bestDist ) {
					bestDist = dist;
					gidrahTarget = g;
				}
			}
		}
		if (gidrahTarget == null) {
			target.setVisible(false);
			target.setSequence(currentSequence + 1);
			return true;
		}

		float px = gidrahTarget.getMapX();
		float py = gidrahTarget.getMapY();

		int targetAngle = FPMath.fpYaklyDegrees(Math.atan2(py - y, px - x));
		int currentAngle = target.getAngle();

		int newAngle;
		int diff = Math.abs(targetAngle - currentAngle);
		if (diff < rate) {
			newAngle = targetAngle;
		} else if (diff > 32768) {
			// Go the other way
			if (targetAngle < currentAngle) {
				newAngle = currentAngle + rate;
			} else {
				newAngle = currentAngle - rate;
			}
		} else {
			if (targetAngle < currentAngle) {
				newAngle = currentAngle - rate;
			} else {
				newAngle = currentAngle + rate;
			}
		}
		while (newAngle < 0) {
			newAngle += 65536;
		}

		target.setVisible(true);
		target.setAngle(newAngle);
		int xScale = FPMath.fpValue(bestDist/maxDist*2.5);
		int yScale = FPMath.fpValue((1.0f-bestDist/maxDist)*0.5+0.25);
		target.setScale(xScale,yScale);
		target.setSequence(currentSequence + 1);
		return true; // Execute the next command
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, Loader)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		rate = XMLUtil.getInt(element, "rate", 65536);
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
	}

}
