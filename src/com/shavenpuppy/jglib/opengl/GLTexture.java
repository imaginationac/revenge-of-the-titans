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
package com.shavenpuppy.jglib.opengl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.util.glu.GLU;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Image;
import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ImageWrapper;
import com.shavenpuppy.jglib.util.XMLUtil;

import static org.lwjgl.opengl.ARBTextureCompression.*;
import static org.lwjgl.opengl.EXTAbgr.*;
import static org.lwjgl.opengl.EXTBgra.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

import static org.lwjgl.util.glu.GLU.*;
/**
 * A standard 1D or 2D GLTexture loaded from an ImageResource
 */
public class GLTexture extends GLBaseTexture {

	private static final long serialVersionUID = 1L;

	/*
	 * Resource data
	 */

	/**
	 * The source url - this should be an URL. To load a file from the classpath,
	 * the URL should begin with classpath:/ To load from a Resource, use resource:
	 */
	protected String url;

	/** True if the Image should be discarded after use */
	protected boolean discardImage;

	/** True if the ImageResource should be discarded after use */
	protected boolean discardImageResource;

	/** The destination format */
	protected int dstFormat;

	/** The source format */
	protected int srcFormat = -1; // Determine from the image

	/*
	 * Transient data
	 */

	/** A source image resource */
	protected transient ImageWrapper imageResource;

	/** A source image */
	protected transient Image image;

	/** The width and height of the texture. 1D textures have no height */
	protected transient int width, height;

	/**
	 * C'tor
	 */
	public GLTexture() {
	}

	/**
	 * Normal constructor
	 */
	public GLTexture(String name) {
		super(name);
	}

	/**
	 * GLTexture constructor comment.
	 */
	public GLTexture(
		String name,
		String url,
		int target,
		int dstFormat,
		int minMode,
		int magMode,
		boolean wrap)
	{
		super(name, target, minMode, magMode, wrap);

		this.url = url;
		this.dstFormat = dstFormat;

		discardImage = true;
		discardImageResource = true;
	}

	/**
	 * GLTexture constructor comment.
	 */
	public GLTexture(
		String name,
		ImageWrapper imageResource,
		int target,
		int dstFormat,
		int minMode,
		int magMode,
		boolean wrap)
	{
		super(name, target, minMode, magMode, wrap);

		this.imageResource = imageResource;
		this.dstFormat = dstFormat;

		discardImage = true;
	}

	/**
	 * GLTexture constructor comment.
	 */
	public GLTexture(
		String name,
		Image image,
		int target,
		int dstFormat,
		int minMode,
		int magMode,
		boolean wrap)
	{
		super(name, target, minMode, magMode, wrap);

		this.image = image;
		this.dstFormat = dstFormat;

		discardImageResource = true;
	}

	/**
	 * Decode the source format of an image
	 * @return the GL source format
	 */
	private int decodeSourceFormat(Image textureImage) {
		ContextCapabilities capabilities = GLContext.getCapabilities();
		switch (textureImage.getType()) {
			case Image.RGB:
				return GL_RGB;
			case Image.RGBA:
				return GL_RGBA;
			case Image.LUMINANCE:
				return GL_LUMINANCE;
			case Image.LUMINANCE_ALPHA:
				return GL_LUMINANCE_ALPHA;
			case Image.ARGB:
				throw new IllegalArgumentException("ARGB image format is not supported.");
			case Image.ABGR:
				if (capabilities.GL_EXT_abgr) {
					return GL_ABGR_EXT;
				} else {
					throw new IllegalArgumentException("ABGR image format is not supported.");
				}
			case Image.BGR:
				if (capabilities.GL_EXT_bgra) {
					return GL_BGR_EXT;
				} else {
					throw new IllegalArgumentException("BGR image format is not supported.");
				}
			case Image.BGRA:
				if (capabilities.GL_EXT_bgra) {
					return GL_BGRA_EXT;
				} else {
					throw new IllegalArgumentException("BGRA image format is not supported.");
				}
			default:
				throw new IllegalArgumentException("Unknown image format.");
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.opengl.GLBaseTexture#doCreateTexture()
	 */
	@Override
	protected void doCreateTexture() {

		try {
			// First get the image if necessary
			if (url != null) {
				if (url.startsWith("classpath:")) {
					// Load directly from a serialised Image in the classpath
					try {
						//System.out.println("Load "+url.substring(10));
						image = Image.read(new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(url.substring(10))));
					} catch (IOException e) {
						System.out.println("Failed to load system resource: "+url.substring(10));
						throw e;
					}
				} else if (url.startsWith("resource:")) {
					// Load directly from Resources
					imageResource = (ImageWrapper) Resources.get(url.substring(9));
				} else {
					// Load from a URL
					image = Image.read(new BufferedInputStream(new URL(url).openStream()));
				}
			}

		//	System.out.println("Creating texture "+imageResource);

			if (imageResource != null) {
				image = imageResource.getImage();
			}

			glBindTexture(target, texture);
			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
			ContextCapabilities capabilities = GLContext.getCapabilities();
			int wrapMode = wrap ? GL_REPEAT : (capabilities.OpenGL12 ? GL_CLAMP_TO_EDGE : GL_CLAMP);

			if (target == GL11.GL_TEXTURE_2D) {
				glTexParameteri(target, GL_TEXTURE_WRAP_T, wrapMode);
			}

			glTexParameteri(target, GL_TEXTURE_WRAP_S, wrapMode);
			glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magMode);
			glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minMode);

			// Perform pre-processing on the image.
			Image textureImage = preprocess();
			width = textureImage.getWidth();
			height = textureImage.getHeight();
			if (
						(com.shavenpuppy.jglib.util.Util.nextPowerOf2(width) != width
					|| 	com.shavenpuppy.jglib.util.Util.nextPowerOf2(height) != height)
				&& !capabilities.GL_ARB_texture_non_power_of_two
				)
			{
				throw new OpenGLException("Non-power-of-two texture not supported by card");
			}
			if (srcFormat == -1) {
				srcFormat = decodeSourceFormat(textureImage);
			}

			if (srcFormat == -1) {
				srcFormat = dstFormat;
			}

			final int elements;
			switch (srcFormat) {
				case GL_LUMINANCE :
				case GL_ALPHA :
				case GL_RED :
				case GL_GREEN :
				case GL_BLUE :
					elements = 1;
					break;
				case GL_LUMINANCE_ALPHA :
					elements = 2;
					break;
				case GL_BITMAP :
					elements = 1;
					break;
				case GL_RGB :
				case GL_BGR_EXT :
					elements = 3;
					break;
				case GL_RGBA :
				case GL_BGRA_EXT :
				case GL_ABGR_EXT :
					elements = 4;
					break;
				default :
					throw new OpenGLException("Unknown format "+srcFormat);

			}

			// Automatically disable compression if it's not available
			if (dstFormat == GL_COMPRESSED_RGB_ARB && !capabilities.GL_ARB_texture_compression) {
				dstFormat = GL_RGB;
			} else if (dstFormat == GL_COMPRESSED_RGBA_ARB && !capabilities.GL_ARB_texture_compression) {
				dstFormat = GL_RGBA;
			}

			if (target == GL_TEXTURE_1D) {
				glTexImage1D(
					target,
					0, // Top level
					dstFormat,
					width,
					0, // No border
					srcFormat,
					GL_UNSIGNED_BYTE,
					textureImage.getData()
					);
			} else if (target == GL_TEXTURE_2D) {
				glTexImage2D(
					target,
					0, // Top level
					dstFormat,
					width,
					height,
					0, // No border
					srcFormat,
					GL_UNSIGNED_BYTE,
					textureImage.getData()
					);
			}

			// Create mipmaps if necessary
			if (minMode == GL_LINEAR_MIPMAP_LINEAR || minMode == GL_LINEAR_MIPMAP_NEAREST || minMode == GL_NEAREST_MIPMAP_LINEAR || minMode == GL_NEAREST_MIPMAP_NEAREST) {
				int ret;
				if (target == GL_TEXTURE_2D) {
					if ((ret =
						gluBuild2DMipmaps(
							GL_TEXTURE_2D,
							elements,
							width,
							height,
							srcFormat,
							GL_UNSIGNED_BYTE,
							textureImage.getData()))
						!= 0) {
						throw new OpenGLException("Failure creating 2D mipmaps for "+this+": "+GLU.gluGetString(ret));
					}
				} else if (target == GL_TEXTURE_1D) {
					/*
					 * NOTE: not yet implemented in GLU
					if ((ret =
						GLU.gluBuild1DMipmaps(
							GL11.GL_TEXTURE_1D,
							elements,
							width,
							srcFormat,
							GL11.GL_UNSIGNED_BYTE,
							textureImage.getData()))
						!= 0)
						throw new OpenGLException(GLU.gluGetString(ret));
					*/
				}
			}

	/*
			// WARNING: some GL drivers might be retaining the reference to the original data!
			// Therefore we can no longer safely throw away our image data, and it has to remain
			// in the Java heap. But sod 'em.
	*/
	 		// Now we've finished with the image let's null it so it doesn't hang around in memory
			if (discardImage) {
				image = null;
			}

			if (discardImageResource) {
				imageResource = null;
			}

			textureImage.dispose();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * @return the texture's height (will be 1 for 1D textures)
	 */
	@Override
	public final int getHeight() {
		return height;
	}

	/**
	 * @return the texture's width
	 */
	@Override
	public final int getWidth() {
		return width;
	}

	/* (non-Javadoc)
	 * @see GLXMLResource#load(Element, Loader)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		super.load(element, loader);

		url = XMLUtil.getString(element, "url", null);
		target = GLUtil.decode(XMLUtil.getString(element, "target"));
		dstFormat = GLUtil.decode(XMLUtil.getString(element, "dst"));
		if (XMLUtil.getString(element, "src", null) != null) {
			srcFormat = GLUtil.decode(XMLUtil.getString(element, "src"));
		} else {
			srcFormat = -1; // Determine from the image
		}

		if (srcFormat == -1 && url == null) {
			// Must specify width and height
			width = XMLUtil.getInt(element, "width");
			height = XMLUtil.getInt(element, "height");
		}
		minMode = GLUtil.decode(XMLUtil.getString(element, "min"));
		magMode = GLUtil.decode(XMLUtil.getString(element, "mag"));
		wrap = GL_TRUE == GLUtil.decode(XMLUtil.getString(element, "wrap"));
	}

	/**
	 * Perform preprocessing on the image before it is turned into a texture.
	 * @return a new image, or the same image if no preprocessing is needed
	 */
	protected Image preprocess() {
		if (image == null) {
			// Create a blank image
			int type;
			switch (dstFormat) {
				case GL_RGB:
					type = Image.RGB;
					break;
				case GL_RGBA:
					type = Image.RGBA;
					break;
				case GL_LUMINANCE:
					type = Image.LUMINANCE;
					break;
				case GL_LUMINANCE_ALPHA:
					type = Image.LUMINANCE_ALPHA;
					break;
				default:
					type = Image.RGBA;
					break;
			}
			return new Image(width, height, type);
		} else {
			// By default we don't need to do anything
			return image;
		}
	}

	@Override
	public void archive() {
		url = null;
	}
}