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
package com.shavenpuppy.jglib.sprites;

import java.io.IOException;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.XMLResourceWriter;

/**
 * An Animation command which displays an image for a number of frames.
 * An Animation will typically consist of a number of FrameCommands,
 * followed by a GotoCommand to set the sequence back to the beginning.
 */
public class FrameCommand extends Command {

	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG = false;

	/** The duration of the frame */
	private int duration;

	/** offset for positioning additional sprites or emitters - eg for making eyes stick to a gidrah during walk cycle */
	private float childXOffset, childYOffset;

	/** The name of the image to display... */
	private String appearanceName;

	/** ... or a framelist index */
	private int idx;

	/** The new appearance to display */
	private transient Appearance spriteAppearance;

	/**
	 * Constructor for FrameCommand.
	 */
	public FrameCommand() {
		super();
	}

	@Override
	public void archive() {
		appearanceName = null;
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated)
	 */
	@Override
	public boolean execute(Sprite target) {
		int currentSequence = target.getSequence();
		int currentTick = target.getTick() + 1;

		if (currentTick == 1) {
			boolean twiddle;
			target.setChildXOffset(childXOffset);
			target.setChildYOffset(childYOffset);
			if (appearanceName == null) {
				// Using frameindex
				twiddle = target.setFrame(idx);
			} else if (spriteAppearance == null) {
				if (DEBUG) {
					System.err.println("frame "+appearanceName+" not found");
				}
				return false;
			} else {
				twiddle = spriteAppearance.toSprite(target);
			}
			if (twiddle) {
				return false; // Don't execute the next command - already been done when we set an animation
			}
		}

		if (currentTick > duration) {
			target.setSequence(currentSequence + 1);
			target.setTick(0);
			return true; // Next command
		} else {
			target.setTick(currentTick);
			return false; // Don't execute the next command
		}
//
//
//		if (currentTick > duration) {
//			if (duration == 0) {
//				if (appearanceName == null) {
//					// Using frameindex
//					target.setFrame(idx);
//				} else if (spriteAppearance == null) {
//					if (DEBUG) {
//						System.err.println("frame "+appearanceName+" not found");
//					}
//				} else if (spriteAppearance.toSprite(target)) {
//					// This is a new animation; we should return right away instead of advancing the sequence number, and not
//					// execute the next instruction as this will already have occurred
//					return false;
//				}
//			}
//			target.setSequence(++currentSequence);
//			target.setTick(0);
//			target.setChildXOffset(childXOffset);
//			target.setChildYOffset(childYOffset);
//			return true; // Execute the next command
//		} else {
//			target.setTick(currentTick);
//			boolean twiddle;
//			if (appearanceName == null) {
//				// Using frameindex
//				twiddle = target.setFrame(idx);
//			} else if (spriteAppearance == null) {
//				if (DEBUG) {
//					System.err.println("frame "+appearanceName+" not found");
//				}
//				return false;
//			} else {
//				twiddle = spriteAppearance.toSprite(target);
//			}
//			if (twiddle) {
//				if (duration == 0 && target.getTick() == 0) {
//					return true;
//				}
//				return false;
//			} else {
//				if (duration == 0 && target.getTick() == 0) {
//					return true;
//				}
//				target.setChildXOffset(childXOffset);
//				target.setChildYOffset(childYOffset);
//				return false; // Don't execute the next command
//			}
//		}

	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, Loader)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		if (element.hasAttribute("idx")) {
			idx = Integer.parseInt(element.getAttribute("idx"));
		} else {
			appearanceName = element.getAttribute("i");
		}
		duration = Integer.parseInt(element.getAttribute("d"));
		if (element.hasAttribute("childXOffset")) {
			childXOffset = Float.parseFloat(element.getAttribute("childXOffset"));
		} else {
			childXOffset = 0;
		}
		if (element.hasAttribute("childYOffset")) {
			childYOffset = Float.parseFloat(element.getAttribute("childYOffset"));
		} else {
			childYOffset = 0;
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (appearanceName != null) {
			writer.writeAttribute("i", appearanceName, true);
		} else {
			writer.writeAttribute("idx", idx, true);
		}
		writer.writeAttribute("d", duration, true);
		writer.writeAttribute("childXOffset", childXOffset, false);
		writer.writeAttribute("childYOffset", childYOffset, false);
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {
		// Load the image
		if (appearanceName != null) {
			spriteAppearance = (Appearance) Resources.get(appearanceName);
		}
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		spriteAppearance = null;
	}

//	private void updateSpriteAppearanceUsingIndex(Animated target) {
//		ResourceArray frameList = target.getFrameList();
//		if (frameList == null) {
//			// Not set.
//			if (DEBUG) {
//				System.err.println("no framelist set on "+this);
//			}
//		} else {
//			if (idx < 0 || idx >= frameList.getNumResources()) {
//				// No-op
//				if (DEBUG) {
//					System.err.println("Tried to set frame index "+idx+" but frame list "+frameList+" only has "+frameList.getNumResources());
//				}
//			} else {
//				spriteAppearance = (Appearance) frameList.getResource(idx);
//			}
//		}
//	}
}
