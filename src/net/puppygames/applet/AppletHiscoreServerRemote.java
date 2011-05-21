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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * $Id: AppletHiscoreServerRemote.java,v 1.5 2008/03/21 14:42:43 foo Exp $
 *
 * Remote interface to hiscore server
 *
 * @author $Author: foo $
 * @version $Revision: 1.5 $
 */
public interface AppletHiscoreServerRemote extends Remote {

	public static final String REMOTE_NAME = "applet_hiscore_server";
	public static final String REMOTE_HOST = "puppygames.net";
	public static final int MAX_SCORES = 100;

	/**
	 * Submit a Score to the server.
	 * @param score A hiscore
	 * @return a HiscoresReturn
	 * @throws RemoteException
	 * @throws Exception
	 */
	public HiscoresReturn submit2(Score score) throws Exception, RemoteException;

	/**
	 * Get the hiscores for a particular game
	 * @param game
	 * @return A List of Scores
	 */
	public List<Score> getHiscores(String game) throws Exception, RemoteException;

	/**
	 * Submit a Score to the server.
	 * @param score A hiscore
	 * @return a List of Scores
	 * @throws RemoteException
	 * @throws Exception
	 */
	public List<Score> submit(Score score) throws Exception, RemoteException;

}
