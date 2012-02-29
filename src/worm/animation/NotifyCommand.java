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

import java.io.IOException;

import org.w3c.dom.Element;

import worm.features.LayersFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Command;
import com.shavenpuppy.jglib.sprites.Sprite;

/**
 * Notifies the sprite on a named layer in a {@link ThingWithLayers} to switch to a new animation
 */
public class NotifyCommand extends Command {

	/** Name of the layer */
	private String layer;

	/** New animation to do (optional) */
	private String i;

	/** Frame index if not using i */
	private int idx;

	/** Sequence label to go to (optional) */
	private String id;

	private transient Appearance appearance;

	/**
	 * C'tor
	 */
	public NotifyCommand() {
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		layer = element.getAttribute("layer");
		idx = -1;
		if (element.hasAttribute("i")) {
			i = element.getAttribute("i");
		} else if (element.hasAttribute("idx")) {
			idx = Integer.parseInt(element.getAttribute("idx"));
		}
		if (element.hasAttribute("id")) {
			id = element.getAttribute("id");
		}
	}

	@Override
	public boolean execute(Sprite target) {
		int currentSequence = target.getSequence();
		target.setSequence(currentSequence + 1);
		ThingWithLayers thingWithLayers = (ThingWithLayers) ((Sprite) target).getOwner();
		LayersFeature layers = thingWithLayers.getAppearance();
		if (layers == null) {
			// No layers - ignore
			return true;
		}
		int layerIndex = layers.getLayer(layer);
		if (layerIndex != -1) {
			Sprite sprite = thingWithLayers.getSprite(layerIndex);
			if (sprite == target || sprite == null) {
				// Avoid recursion!
				return true;
			}
			if (appearance != null && sprite.getAnimation() != appearance) {
				appearance.toSprite(sprite);
			} else if (idx != -1) {
				ResourceArray frameList = sprite.getFrameList();
				if (frameList != null) {
					if (idx >= frameList.getNumResources() || idx < 0) {
						System.err.println("Warning: index "+idx+" not present in "+frameList+" referenced by "+target+" on "+sprite);
					} else {
						Appearance newAppearance = (Appearance) frameList.getResource(idx);
						if (newAppearance != null) {
							newAppearance.toSprite(sprite);
						}
					}
				}
			}

			if (id != null && sprite.getAnimation() != null) {
				int seq = sprite.getAnimation().getLabel(id);
				if (seq == -1) {
					// Skip and ignore
					System.err.println("Warning: "+sprite.getAnimation()+" missing label "+id);
				} else {
					sprite.setSequence(seq);
				}
			}
		}
		return true;
	}

	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		writer.writeAttribute("layer", layer, true);
		writer.writeAttribute("i", i);
		writer.writeAttribute("idx", idx);
		writer.writeAttribute("id", id);
	}

	@Override
	protected void doCreate() {
		if (i != null) {
			appearance = (Appearance) Resources.get(i);
		}
	}

	@Override
	public void archive() {
		i = null;
	}

	@Override
	protected void doDestroy() {
		appearance = null;
	}

}
