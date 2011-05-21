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

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Image;
import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.interpolators.Interpolator;
import com.shavenpuppy.jglib.interpolators.InterpolatorBase;
import com.shavenpuppy.jglib.resources.ImageWrapper;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * An image generated using an interpolator
 */
public class InterpolatorImageResource extends Resource implements ImageWrapper {


	public static final long serialVersionUID = 1L;

	/*
	 * Resource data
	 */

	/** Image type */
	private int type;

	/** Size */
	private int size;

	/** Interpolator */
	private String interpolator;

	/*
	 * Transient data
	 */

	/** The resulting image */
	private transient Image image;

	/** The interpolator instance */
	private transient Interpolator interpolatorInstance;

	/**
	 * @param name
	 */
	public InterpolatorImageResource(String name) {
		super(name);
	}

	/**
	 * Constructor
	 * @param name
	 * @param size
	 * @param type
	 * @param interpolator
	 */
	public InterpolatorImageResource(String name, int size, int type, Interpolator interpolatorInstance) {
		super(name);

		this.size = size;
		this.type = type;
		this.interpolatorInstance = interpolatorInstance;
	}

	@Override
    protected void doCreate(){
		image = new Image(size, size, type);
		interpolatorInstance = InterpolatorBase.decode(interpolator);

		final float sizeBy2 = (size - 1) * 0.5f;
		final int radius = size >> 1;
		for (int y = -radius; y < radius; y ++) {
			double yd = y * y;
			for (int x = -radius; x < radius; x ++) {
				float dist = (float)Math.sqrt(x * x + yd) / sizeBy2;
				int c = (int) (interpolatorInstance.interpolate(0.0f, 255.0f, dist));
				if (type == Image.LUMINANCE_ALPHA) {
					image.getData().put((byte) 255);
					image.getData().put(((c <= 255) ? (byte)c : (byte)0xff));
				} else {
					image.getData().put(((c <= 255) ? (byte)c : (byte)0xff));
				}
			}
		}

		// Prepare for reading
		image.getData().flip();
	}

	@Override
    public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		size = XMLUtil.getInt(element, "size");
		interpolator = XMLUtil.getString(element, "interpolator");
		String stype = element.getAttribute("type");
		if (stype.equalsIgnoreCase("luminance")) {
			type = Image.LUMINANCE;
		} else if (stype.equalsIgnoreCase("alpha")) {
			type = Image.LUMINANCE;
		} else if (stype.equalsIgnoreCase("luminance_alpha")) {
			type = Image.LUMINANCE_ALPHA;
		} else {
			throw new Exception("Unknown type "+stype);
		}
	}

	/**
	 * @return the image
	 */
	@Override
    public final Image getImage() {
		assert isCreated();
		return image;
	}

}
