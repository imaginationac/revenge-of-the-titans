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

import java.io.*;
import java.nio.ByteBuffer;

import net.puppygames.applet.Game;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.Effect;

import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;
import org.lwjgl.util.ReadableColor;

import worm.Layers;
import worm.Worm;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.opengl.GLRenderable;

/**
 * Fog effect
 */
public class FogEffect extends Effect {

	private static final int PERM[] = {151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69,
			142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
			57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146,
			158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161,
			1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173,
			186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58,
			17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129,
			22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210,
			144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204,
			176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215,
			61, 156, 180};

	/*
	 * These are Ken Perlin's proposed gradients for 3D noise. I kept them for better consistency with the reference implementation,
	 * but there is really no need to pad this to 16 gradients for this particular implementation. If only the "proper" first 12
	 * gradients are used, they can be extracted from the grad4[][] array: grad3[i][j] == grad4[i*2][j], 0<=i<=11, j=0,1,2
	 */
	private static final int[] GRAD3 = {0, 1, 1, 0, 1, -1, 0, -1, 1, 0, -1, -1, 1, 0, 1, 1, 0, -1, -1, 0, 1, -1, 0, -1, 1, 1, 0, 1,
			-1, 0, -1, 1, 0, -1, -1, 0, 1, 0, -1, -1, 0, -1, 0, -1, 1, 0, 1, 1};

	/*
	 * These are my own proposed gradients for 4D noise. They are the coordinates of the midpoints of each of the 32 edges of a
	 * tesseract, just like the 3D noise gradients are the midpoints of the 12 edges of a cube.
	 */
	private static final int[] GRAD4 = {0, 1, 1, 1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, -1, -1, 0, -1, 1, 1, 0, -1, 1, -1, 0, -1, -1,
			1, 0, -1, -1, -1, 1, 0, 1, 1, 1, 0, 1, -1, 1, 0, -1, 1, 1, 0, -1, -1, -1, 0, 1, 1, -1, 0, 1, -1, -1, 0, -1, 1, -1, 0,
			-1, -1, 1, 1, 0, 1, 1, 1, 0, -1, 1, -1, 0, 1, 1, -1, 0, -1, -1, 1, 0, 1, -1, 1, 0, -1, -1, -1, 0, 1, -1, -1, 0, -1, 1,
			1, 1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, -1, -1, 0, -1, 1, 1, 0, -1, 1, -1, 0, -1, -1, 1, 0, -1, -1, -1, 0};

	/*
	 * This is a look-up table to speed up the decision on which simplex we are in inside a cube or hypercube "cell" for 3D and 4D
	 * simplex noise. It is used to avoid complicated nested conditionals in the GLSL code. The table is indexed in GLSL with the
	 * results of six pair-wise comparisons beween the components of the P=(x,y,z,w) coordinates within a hypercube cell. c1 = x>=y
	 * ? 32 : 0; c2 = x>=z ? 16 : 0; c3 = y>=z ? 8 : 0; c4 = x>=w ? 4 : 0; c5 = y>=w ? 2 : 0; c6 = z>=w ? 1 : 0; offsets =
	 * simplex[c1+c2+c3+c4+c5+c6]; o1 = step(160,offsets); o2 = step(96,offsets); o3 = step(32,offsets); (For the 3D case, c4, c5,
	 * c6 and o3 are not needed.)
	 */
	private static final byte[] SIMPLEX4 = {0, 64, (byte) 128, (byte) 192, 0, 64, (byte) 192, (byte) 128, 0, 0, 0, 0, 0,
			(byte) 128, (byte) 192, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 64, (byte) 128, (byte) 192, 0, 0, (byte) 128, 64,
			(byte) 192, 0, 0, 0, 0, 0, (byte) 192, 64, (byte) 128, 0, (byte) 192, (byte) 128, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 64, (byte) 192, (byte) 128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 64, (byte) 128, 0, (byte) 192, 0, 0, 0, 0, 64, (byte) 192, 0, (byte) 128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, (byte) 128, (byte) 192, 0, 64, (byte) 128, (byte) 192, 64, 0, 64, 0, (byte) 128, (byte) 192, 64, 0, (byte) 192,
			(byte) 128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 128, 0, (byte) 192, 64, 0, 0, 0, 0, (byte) 128, 64, (byte) 192,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 128, 0, 64,
			(byte) 192, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 192, 0, 64, (byte) 128, (byte) 192, 0, (byte) 128, 64, 0, 0, 0,
			0, (byte) 192, 64, (byte) 128, 0, (byte) 128, 64, 0, (byte) 192, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 192, 64, 0,
			(byte) 128, 0, 0, 0, 0, (byte) 192, (byte) 128, 0, 64, (byte) 192, (byte) 128, 64, 0};

	private static boolean inited;
	private static int vertexShader, fragmentShader, programObj;
	private static String fragmentProgram, vertexProgram;
	private static int location_permTexture, location_simplexTexture, location_gradTexture, location_time;
	private static int perm_texture, grad_texture, simplex_texture;

	private TickableObject tickable;
	private boolean finished;

	/**
	 * C'tor
	 */
	public FogEffect() {
	}

	private static void dispose() {
		if (!inited) {
			return;
		}

		inited = false;
		ARBShaderObjects.glDeleteObjectARB(vertexShader);
		ARBShaderObjects.glDeleteObjectARB(fragmentShader);
		ARBShaderObjects.glDeleteObjectARB(programObj);
		GL11.glDeleteTextures(perm_texture);
		GL11.glDeleteTextures(grad_texture);
		GL11.glDeleteTextures(simplex_texture);
	}
	private static void initialise() {
		if (inited) {
			return;
		}

		// Create the vertex shader.
		vertexShader = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);

		// Load the shader source
		{
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("GLSLnoisetest4.vert");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuilder sb = new StringBuilder(65536);
			try {
				while ((line = br.readLine()) != null) {
					sb.append(line);
					sb.append("\n");
				}
			} catch (IOException e) {
				e.printStackTrace(System.err);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			vertexProgram = sb.toString();
		}
		ARBShaderObjects.glShaderSourceARB(vertexShader, vertexProgram);
		ARBShaderObjects.glCompileShaderARB(vertexShader);
		if (ARBShaderObjects.glGetObjectParameteriARB(vertexShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
			throw new RuntimeException(ARBShaderObjects.glGetInfoLogARB(vertexShader, 4096));
		}

		// Create the fragment shader.
		fragmentShader = ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
		{
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("GLSLnoisetest4.frag");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuilder sb = new StringBuilder(65536);
			try {
				while ((line = br.readLine()) != null) {
					sb.append(line);
					sb.append("\n");
				}
			} catch (IOException e) {
				e.printStackTrace(System.err);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			fragmentProgram = sb.toString();
		}
		ARBShaderObjects.glShaderSourceARB(fragmentShader, fragmentProgram);
		ARBShaderObjects.glCompileShaderARB(fragmentShader);

		if (ARBShaderObjects.glGetObjectParameteriARB(fragmentShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
			throw new RuntimeException(ARBShaderObjects.glGetInfoLogARB(fragmentShader, 4096));

		}

		// Create a program object and attach the two compiled shaders.
		programObj = ARBShaderObjects.glCreateProgramObjectARB();
		ARBShaderObjects.glAttachObjectARB(programObj, vertexShader);
		ARBShaderObjects.glAttachObjectARB(programObj, fragmentShader);

		// Link the program object and print out the info log.
		ARBShaderObjects.glLinkProgramARB(programObj);
		if (ARBShaderObjects.glGetObjectParameteriARB(programObj, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
			throw new RuntimeException(ARBShaderObjects.glGetInfoLogARB(programObj, 4096));
		}
		// Locate the uniform shader variables so we can set them later:

		// a texture ID ("permTexture") and a float ("time").
		location_permTexture = ARBShaderObjects.glGetUniformLocationARB(programObj, "permTexture");
		if (location_permTexture == -1) {
			System.out.println("Failed to locate uniform variable 'permTexture'.");
		}
		location_simplexTexture = ARBShaderObjects.glGetUniformLocationARB(programObj, "simplexTexture");
		if (location_simplexTexture == -1) {
			System.out.println("Failed to locate uniform variable 'simplexTexture'.");
		}
		location_gradTexture = ARBShaderObjects.glGetUniformLocationARB(programObj, "gradTexture");
		if (location_gradTexture == -1) {
			System.out.println("Failed to locate uniform variable 'gradTexture'.");
		}
		location_time = ARBShaderObjects.glGetUniformLocationARB(programObj, "time");
		if (location_time == -1) {
			System.out.println("Failed to locate uniform variable 'time'.");
		}

		initPermTexture();
		initGradTexture();
		initSimplexTexture();

		inited = true;
	}

	/*
	 * initPermTexture(GLuint *texID) - create and load a 2D texture for a combined index permutation and gradient lookup table.
	 * This texture is used for 2D and 3D noise, both classic and simplex.
	 */
	private static void initPermTexture() {
		ByteBuffer pixels = BufferUtils.createByteBuffer(256 * 256 * 4);

		perm_texture = GL11.glGenTextures(); // Generate a unique texture ID
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, perm_texture); // Bind the texture to texture unit 0

		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 256; j++) {
				int value = PERM[j + PERM[i] & 0xFF];
				pixels.put((byte) (GRAD3[(value & 0x0F) * 3] * 64 + 64)); // Gradient x
				pixels.put((byte) (GRAD3[(value & 0x0F) * 3 + 1] * 64 + 64)); // Gradient y
				pixels.put((byte) (GRAD3[(value & 0x0F) * 3 + 2] * 64 + 64));
				pixels.put((byte) value); // Permuted index
			}
		}
		pixels.flip();
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 256, 256, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
	}

	/*
	 * initSimplexTexture(GLuint *texID) - create and load a 1D texture for a simplex traversal order lookup table. This is used for
	 * simplex noise only, and only for 3D and 4D noise where there are more than 2 simplices. (3D simplex noise has 6 cases to sort
	 * out, 4D simplex noise has 24 cases.)
	 */
	private static void initSimplexTexture() {
		ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE1_ARB); // Activate a different texture unit (unit 1)

		simplex_texture = GL11.glGenTextures(); // Generate a unique texture ID
		GL11.glBindTexture(GL11.GL_TEXTURE_1D, simplex_texture); // Bind the texture to texture unit 1
		ByteBuffer simplex4 = BufferUtils.createByteBuffer(SIMPLEX4.length);
		simplex4.put(SIMPLEX4);
		simplex4.flip();
		GL11.glTexImage1D(GL11.GL_TEXTURE_1D, 0, GL11.GL_RGBA, 64, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, simplex4);
		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB); // Switch active texture unit back to 0 again
	}

	/*
	 * initGradTexture(GLuint *texID) - create and load a 2D texture for a 4D gradient lookup table. This is used for 4D noise only.
	 */
	private static void initGradTexture() {
		ByteBuffer pixels = BufferUtils.createByteBuffer(256 * 256 * 4);

		ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE2_ARB); // Activate a different texture unit (unit 2)

		grad_texture = GL11.glGenTextures(); // Generate a unique texture ID
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, grad_texture); // Bind the texture to texture unit 2

		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 256; j++) {
				int value = PERM[j + PERM[i] & 0xFF];
				pixels.put((byte) (GRAD4[(value & 0x1F) * 3] * 64 + 64)); // Gradient x
				pixels.put((byte) (GRAD4[(value & 0x1F) * 3 + 1] * 64 + 64)); // Gradient y
				pixels.put((byte) (GRAD4[(value & 0x1F) * 3 + 2] * 64 + 64)); // Gradient z
				pixels.put((byte) (GRAD4[(value & 0x1F) * 3 + 3] * 64 + 64)); // Gradient z
			}
		}
		pixels.flip();
		// GLFW texture loading functions won't work here - we need GL_NEAREST lookup.
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 256, 256, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB); // Switch active texture unit back to 0 again
	}

	@Override
	protected void doSpawn() {
		initialise();

		tickable = new TickableObject() {
			@Override
			protected void render() {
				if (Game.wasKeyPressed(Keyboard.KEY_BACK)) {
					dispose();
					initialise();
				}
				glRender(new GLRenderable() {
					@Override
					public void render() {
						GL11.glEnable(GL11.GL_TEXTURE_1D);
						GL11.glEnable(GL11.GL_TEXTURE_2D);
						GL11.glEnable(GL11.GL_BLEND);
						GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
						ARBShaderObjects.glUseProgramObjectARB(programObj);
						if (perm_texture != 0) {
							GL11.glBindTexture(GL11.GL_TEXTURE_2D, perm_texture); // Bind the texture to texture unit 1
						}
						if (simplex_texture != 0) {
							ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE1_ARB); // Activate a different texture unit (unit 1)
							GL11.glBindTexture(GL11.GL_TEXTURE_1D, simplex_texture); // Bind the texture to texture unit 1
							ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB); // Activate a different texture unit (unit 1)
						}
						if (grad_texture != 0) {
							ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE2_ARB); // Activate a different texture unit (unit 2)
							GL11.glBindTexture(GL11.GL_TEXTURE_2D, grad_texture); // Bind the texture to texture unit 2
							ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB); // Activate a different texture unit (unit 2)
						}
						if (location_permTexture != -1) {
							ARBShaderObjects.glUniform1iARB(location_permTexture, 0); // Texture unit 0
						}
						if (location_simplexTexture != -1) {
							ARBShaderObjects.glUniform1iARB(location_simplexTexture, 1); // Texture unit 1
						}
						if (location_gradTexture != -1) {
							ARBShaderObjects.glUniform1iARB(location_gradTexture, 2); // Texture unit 2
						}
						if (location_time != -1) {
							float time = Sys.getTime() / 12000.0f;
							ARBShaderObjects.glUniform1fARB(location_time, time);
						}
					}
				});
				ReadableColor c = Worm.getGameState().getLevelFeature().getColors().getColor("floor-fog");
				glColor4ub(c.getRedByte(), c.getGreenByte(), c.getBlueByte(), (byte) 200);
				float z = 0.0f;
				glBegin(GL11.GL_QUADS);
				float ox = -GameScreen.getSpriteOffset().getX() / (Game.getScale() * 2.0f);
				float oy = -GameScreen.getSpriteOffset().getY() / (Game.getScale() * 2.0f);
				float w = Game.getWidth() / 1024.f;
				float h = Game.getHeight() / 1024.f;
				glTexCoord2f(ox, oy);
				glVertex3f(0.0f, 0.0f, z);
				glTexCoord2f(ox + w, oy);
				glVertex3f(Game.getWidth(), 0.0f, z);
				glTexCoord2f(ox + w, oy + h);
				glVertex3f(Game.getWidth(), Game.getHeight(), z);
				glTexCoord2f(ox, oy + h);
				glVertex3f(0.0f, Game.getHeight(), z);
				glEnd();
				glRender(new GLRenderable() {
					@Override
					public void render() {
						ARBShaderObjects.glUseProgramObjectARB(0);
						GL11.glDisable(GL11.GL_TEXTURE_1D);
					}
				});
			}
		};
		tickable.setLayer(Layers.FOG);
		tickable.spawn(getScreen());
	}

	@Override
	protected void doRemove() {
		finished = true;
	}

	@Override
	protected void doUpdate() {
	}

	@Override
	public boolean isActive() {
		return !finished;
	}

	@Override
	protected void doTick() {
	}

	@Override
	protected void doRender() {
	}

}
