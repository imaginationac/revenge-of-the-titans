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
package com.shavenpuppy.jglib.util;

import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/**
 * FPS Graph Logger
 * Logs frame times elapsed and draws them as a FPS graph (with markers for 30, 60 and 120 fps).
 *
 * Either call logFrameTime() every frame with your time delta (if you're doing your own time-based movement)
 * or just call tick() every frame which calculates the time elapsed and logs it for you.
 * If using tick() you should probably call reset() before you start your game loop to avoid a huge spike
 * due to the time elapsed between construction and the start of logging.
 *
 * Don't try and use logFrameTime() and tick() at the same time, that's just weird (and will likely produce
 * weird results too).
 *
 * Calling .render() draws the graph to the screen using basic GL11 commands. It expects the default OpenGL
 * state (ie. anything fancy like lighting or texturing disabled) and should leave the state untouched
 * (although your current colour might have changed).
 *
 * @author John Campbell
 */
public class FpsGraph
{
	private int[] samples;
	private long previousTime;

	public FpsGraph()
	{
		samples = new int[400];
		reset();
	}

	public FpsGraph(int numSamples)
	{
		samples = new int[numSamples];
		reset();
	}


	public void reset()
	{
		previousTime = Sys.getTime();
	}
	public void tick()
	{
		long currentTime = Sys.getTime();
		long diff = currentTime - previousTime;

		previousTime = currentTime;

		float timeDelta = (float)diff / Sys.getTimerResolution();

		if (timeDelta > 0.2f) {
	        timeDelta = 0.2f;
        }

		logFrameTime(timeDelta);
	}

	public void logFrameTime(float timeInSeconds)
	{
		// Old style scrolling behaviour
		{
			// Copy all back one
			for (int i=1; i<samples.length; i++)
			{
				samples[i-1] = samples[i];
			}

			samples[ samples.length-1 ] = (int)(1f/timeInSeconds);
		}
	}

	public void render()
	{
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();

		GL11.glLoadIdentity();
		GLU.gluOrtho2D(0, Display.getDisplayMode().getWidth(), 0, Display.getDisplayMode().getHeight());
		GL11.glDisable(GL11.GL_DEPTH_TEST);


		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		{
			GL11.glTranslatef(10, 10, 0);
			GL11.glScalef(0.5f, 0.5f, 0f);

			int width = samples.length;
			int height = 120;

			// 60Hz & 120Hz lines
			GL11.glBegin(GL11.GL_LINES);
			{
				GL11.glColor3f(0.8f, 0.8f, 0.8f);

				GL11.glVertex2f(0f, 30f);
				GL11.glVertex2f(width, 30f);

				GL11.glVertex2f(0f, 60f);
				GL11.glVertex2f(width, 60f);

				GL11.glVertex2f(0f, 100f);
				GL11.glVertex2f(width, 100f);
			}
			GL11.glEnd();

			// Trace line
			GL11.glBegin(GL11.GL_LINE_STRIP);
			{
				GL11.glVertex2f(0f, 0f);
				for (int i=0; i<samples.length; i++)
				{
					float GREEN_CUTOFF = 60f;
					float ORANGE_CUTOFF = 30f;

					if (samples[i] >= GREEN_CUTOFF) {
	                    GL11.glColor3f(0f, 1f, 0f);
                    } else if (samples[i] >= ORANGE_CUTOFF) {
	                    GL11.glColor3f(1f, 0.6f, 0.1f);
                    } else {
	                    GL11.glColor3f(1f, 0f, 0f);
                    }

					// Clamp to height
					float y = Math.min(samples[i], height);
					GL11.glVertex2f(i, y);
				}
			}
			GL11.glEnd();

			// Boundary lines
			GL11.glBegin(GL11.GL_LINE_LOOP);
			{
				GL11.glColor3f(0.4f, 0f, 1f);

				GL11.glVertex2f(0f, 0f);
				GL11.glVertex2f(width, 0f);
				GL11.glVertex2f(width, height);
				GL11.glVertex2f(0f, height);
			}
			GL11.glEnd();

			GL11.glColor4f(1f, 1f, 1f, 1f);

		}
		GL11.glPopMatrix();

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
	}
}
