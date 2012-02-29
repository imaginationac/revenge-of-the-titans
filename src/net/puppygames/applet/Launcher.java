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
package net.puppygames.applet;

import java.io.InputStream;
import java.util.Properties;

import com.shavenpuppy.jglib.util.ImprovedStringTokenizer;

/**
 * $Id: Launcher.java,v 1.2 2010/09/23 22:02:29 foo Exp $
 * Launcher applet.
 * <p>
 * @author $Author: foo $
 * @version $Revision: 1.2 $
 */
public class Launcher {

	private static final String DEFAULT_RESOURCES = "/resources.dat";

	/**
	 * C'tor
	 */
	public Launcher() {
	}

	/**
	 * Load and go!
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.setProperty("sun.java2d.opengl", "false");
			System.setProperty("sun.java2d.noddraw", "true");
			System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");

			String resources;
			try {
				resources = System.getProperty("net.puppygames.applet.Launcher.resources", DEFAULT_RESOURCES);
			} catch (SecurityException e) {
				resources = DEFAULT_RESOURCES;
			}
			InputStream is = Launcher.class.getResourceAsStream(resources);

			Properties properties = new Properties();
			for (int i = 0; i < args.length; i ++) {
				ImprovedStringTokenizer st = new ImprovedStringTokenizer(args[i]);
				while (st.hasMoreTokens()) {
					String arg = st.nextToken();
					int idx = arg.indexOf('=');
					if (idx != -1) {
						properties.put(arg.substring(0, idx), arg.substring(idx + 1));
					}
				}
			}

			Game.init(properties, is);
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			if (Game.getGameInfo() != null) {
				Game.getGameInfo().setException(e);
			}
			Game.exit();
		}
		System.exit(0);
	}

}
