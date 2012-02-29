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

import java.net.URLEncoder;

import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;

/**
 * Opens the browser on support pages
 *
 * @author foo
 */
public class Support {

	private static boolean supportQueued;

	/**
	 * Open the browser for a specified reason. The browser is opened when the application exits.
	 * @param reason The reason (opengl, crash)
	 */
	public static void doSupport(final String reason) {
		if (supportQueued || Game.DEBUG) {
			return;
		}
		supportQueued = true;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					String os_name = System.getProperty("os.name");
				 	String os_version = (System.getProperty("os.version")+" "+System.getProperty("sun.os.patch.level", "")).trim();
					String os_arch = (System.getProperty("os.arch")+" "+System.getProperty("sun.cpu.isalist", "")).trim();
					Sys.openURL("http://"+
							Game.getSupportURL()+
							"?type="+URLEncoder.encode(reason, "utf-8")+
							"&game="+URLEncoder.encode(Game.getTitle(), "utf-8")+
							"&version="+URLEncoder.encode(Game.getVersion(), "utf-8")+
							"&inst="+URLEncoder.encode(String.valueOf(Game.getInstallation()), "utf-8")+
							"&os_name="+URLEncoder.encode(os_name, "utf-8")+
							"&os_version="+URLEncoder.encode(os_version, "utf-8")+
							"&os_arch="+URLEncoder.encode(os_arch, "utf-8")+
							"&display_adapter="+URLEncoder.encode(Display.getAdapter(), "utf-8")+
							"&adapter_version="+URLEncoder.encode(Display.getVersion(), "utf-8")
						);
				} catch (Exception e) {
					// Fail silently
				}
			}
		});
	}

	public static boolean isSupportQueued() {
		return supportQueued;
	}
}
