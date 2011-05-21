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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.*;
import java.util.HashMap;

import org.lwjgl.opengl.*;

import com.shavenpuppy.jglib.MultiBuffer;
/**
 *
 * @author foo
 */
public class GLUtil {

	/** A map of constant names to values */
	private static final HashMap<String, Integer> glConstantsMap = new HashMap<String, Integer>(513, 1.0f);

	static {
		loadGLConstants();
	}

	/** A handy bit of scratch memory */
	public final static MultiBuffer scratch = new MultiBuffer(64);

	/**
	 * Decode a gl string constant
	 */
	public static int decode(String glstring) throws OpenGLException {
		Integer i = glConstantsMap.get(glstring.toUpperCase());
		if (i == null) {
			throw new OpenGLException(glstring+" is not a recognised GL constant");
		} else {
			return i.intValue();
		}
	}

	/**
	 * Recode a gl constant back into a string
	 */
	public static String recode(int code) {
		for (String s : glConstantsMap.keySet()) {
			Integer n = glConstantsMap.get(s);
			if (n.intValue() == code) {
				return s;
			}
		}
		throw new OpenGLException(code+" is not a known GL code");
	}

	/**
	 * Reads all the constant enumerations from this class and stores them
	 * so we can decode them from strings.
	 * @see #decode(String)
	 * @see #recode(int)
	 */
	private static void loadGLConstants() {
		Class<?>[] classes = new Class[] {
			GL11.class,
			GL12.class,
			GL13.class,
			GL14.class,
			GL15.class,
			ARBMultitexture.class,
			ARBTextureCubeMap.class,
			ARBDepthTexture.class,
			ARBFragmentProgram.class,
			ARBMatrixPalette.class,
			ARBMultisample.class,
			ARBPointParameters.class,
			ARBShadow.class,
			ARBShadowAmbient.class,
			ARBTextureBorderClamp.class,
			ARBTextureCompression.class,
			ARBTextureEnvCombine.class,
			ARBTextureEnvDot3.class,
			ARBTextureMirroredRepeat.class,
			ARBTransposeMatrix.class,
			ARBVertexBlend.class,
			ARBVertexBufferObject.class,
			ARBVertexProgram.class,
			ARBWindowPos.class,
			EXTDrawRangeElements.class,
			EXTAbgr.class,
			EXTBgra.class,
			EXTBlendFuncSeparate.class,
			EXTBlendSubtract.class,
			EXTCompiledVertexArray.class,
			EXTFogCoord.class,
			EXTMultiDrawArrays.class,
			EXTPackedPixels.class,
			EXTPointParameters.class,
			EXTRescaleNormal.class,
			EXTSecondaryColor.class,
			EXTSeparateSpecularColor.class,
			EXTSharedTexturePalette.class,
			EXTStencilTwoSide.class,
			EXTStencilWrap.class,
			EXTTextureCompressionS3TC.class,
			EXTTextureEnvCombine.class,
			EXTTextureEnvDot3.class,
			EXTTextureFilterAnisotropic.class,
			EXTTextureLODBias.class,
			EXTVertexShader.class,
			EXTVertexWeighting.class,
			ATIElementArray.class,
			ATIEnvmapBumpmap.class,
			ATIFragmentShader.class,
			ATIPnTriangles.class,
			ATISeparateStencil.class,
			ATITextureMirrorOnce.class,
			ATIVertexArrayObject.class,
			ATIVertexStreams.class,
			NVCopyDepthToColor.class,
			NVDepthClamp.class,
			NVEvaluators.class,
			NVFence.class,
			NVFogDistance.class,
			NVLightMaxExponent.class,
			NVOcclusionQuery.class,
			NVPackedDepthStencil.class,
			NVPointSprite.class,
			NVRegisterCombiners.class,
			NVRegisterCombiners2.class,
			NVTexgenReflection.class,
			NVTextureEnvCombine4.class,
			NVTextureRectangle.class,
			NVTextureShader.class,
			NVTextureShader2.class,
			NVTextureShader3.class,
			NVVertexArrayRange.class,
			NVVertexArrayRange2.class,
			NVVertexProgram.class
		};
		for (int i = 0; i < classes.length; i ++) {
			loadGLConstants(classes[i]);
		}
	}

	private static void loadGLConstants(Class<?> intf) {
		Field[] field = intf.getFields();
		for (int i = 0; i < field.length; i ++) {
			try {
				if (Modifier.isStatic(field[i].getModifiers()) && Modifier.isPublic(field[i].getModifiers()) && Modifier.isFinal(field[i].getModifiers()) && field[i].getType().equals(int.class)) {
					glConstantsMap.put(field[i].getName(), new Integer(field[i].getInt(null)));
				}
			} catch (Exception e) {
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.lwjgl.opengl.CoreGL#clientActiveTexture(int)
	 */
	public static void glClientActiveTexture(int texture) {
		ContextCapabilities capabilities = GLContext.getCapabilities();
		if (capabilities.OpenGL13) {
			org.lwjgl.opengl.GL13.glClientActiveTexture(texture);
		} else if (capabilities.GL_ARB_multitexture) {
			ARBMultitexture.glClientActiveTextureARB(texture);
		} else {
			throw new OpenGLException("ARB_multitexture not supported.");
		}
	}

	/**
	 * @return true if multitexturing is supported either by OpenGL13 or by ARB_multitexture
	 */
	public static boolean isMultitextureSupported() {
		ContextCapabilities capabilities = GLContext.getCapabilities();
		return capabilities.OpenGL13 || capabilities.GL_ARB_multitexture;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.opengl.CoreGL#activeTexture(int)
	 */
	public static void glActiveTexture(int texture) {
		ContextCapabilities capabilities = GLContext.getCapabilities();
		if (capabilities.OpenGL13) {
			org.lwjgl.opengl.GL13.glActiveTexture(texture);
		} else if (capabilities.GL_ARB_multitexture) {
			ARBMultitexture.glActiveTextureARB(texture);
		} else {
			throw new OpenGLException("ARB_multitexture not supported.");
		}
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.opengl.CoreGL#drawRangeElements(int, int, int, int, int, int)
	 */
	public static void glDrawRangeElements(int mode, int start, int end, ByteBuffer indices) {
		ContextCapabilities capabilities = GLContext.getCapabilities();
		if (capabilities.OpenGL12) {
			org.lwjgl.opengl.GL12.glDrawRangeElements(mode, start, end, indices);
		} else if (capabilities.GL_EXT_draw_range_elements) {
			EXTDrawRangeElements.glDrawRangeElementsEXT(mode, start, end, indices);
		} else {
			throw new OpenGLException("EXT_draw_range_elements not supported.");
		}
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.opengl.CoreGL#drawRangeElements(int, int, int, int, int, int)
	 */
	public static void glDrawRangeElements(int mode, int start, int end, ShortBuffer indices) {
		if (GLContext.getCapabilities().OpenGL12) {
			org.lwjgl.opengl.GL12.glDrawRangeElements(mode, start, end, indices);
		} else if (GLContext.getCapabilities().GL_EXT_draw_range_elements) {
			EXTDrawRangeElements.glDrawRangeElementsEXT(mode, start, end, indices);
		} else {
			throw new OpenGLException("EXT_draw_range_elements not supported.");
		}
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.opengl.CoreGL#drawRangeElements(int, int, int, int, int, int)
	 */
	public static void glDrawRangeElements(int mode, int start, int end, IntBuffer indices) {
		ContextCapabilities capabilities = GLContext.getCapabilities();
		if (capabilities.OpenGL12) {
			org.lwjgl.opengl.GL12.glDrawRangeElements(mode, start, end, indices);
		} else if (capabilities.GL_EXT_draw_range_elements) {
			EXTDrawRangeElements.glDrawRangeElementsEXT(mode, start, end, indices);
		} else {
			throw new OpenGLException("EXT_draw_range_elements not supported.");
		}
	}


}
