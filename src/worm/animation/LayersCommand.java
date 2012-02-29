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

import org.w3c.dom.Element;

import worm.features.LayersFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.sprites.Command;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Sets a layered thing with a new appearance
 */
public class LayersCommand extends Command {

	private String layers;

	/**
	 *
	 */
	public LayersCommand() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated, int)
	 */
	@Override
	public boolean execute(Sprite target) {
		LayersFeature layersFeature = (LayersFeature) Resources.get(layers);
		if (layersFeature != null) {
			((ThingWithLayers) ((Sprite) target).getOwner()).requestSetAppearance(layersFeature);
			return false;
		} else {
			System.out.println("Failed to find layers "+layers);
			return true;
		}
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		layers = XMLUtil.getString(element, "layers");
	}

}
