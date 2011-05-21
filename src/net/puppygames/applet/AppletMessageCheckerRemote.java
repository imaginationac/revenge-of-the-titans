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
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import net.puppygames.gamecommerce.shared.ConfigurationDetails;
import net.puppygames.gamecommerce.shared.RegistrationDetails;

/**
 * $Id: AppletMessageCheckerRemote.java,v 1.4 2006/04/09 22:28:27 foo Exp $
 *
 * @author $Author: foo $
 * @version $Revision: 1.4 $
 */
public interface AppletMessageCheckerRemote extends Remote {

	public static final String REMOTE_NAME = "message_checker";
	public static final String REMOTE_HOST = "puppygames.net";

	/**
	 * Checks for server messages
	 * @param game
	 * @param sequenceNumber
	 * @return null, if no messages are available; or a MessageReturn, if there are.
	 * @throws RemoteException
	 * @deprecated 1.1
	 */
	@Deprecated
	public MessageReturn checkForMessages(String game, int sequenceNumber) throws Exception, SQLException, RemoteException;

	/**
	 * Checks for server messages and optionally reconfigures the game
	 * @param game
	 * @param version
	 * @param sequenceNumber
	 * @param currentConfig The game's current configuration
	 * @return null, if no messages are available; or a MessageReturn, if there are.
	 * @throws RemoteException
	 * @since 1.2
	 */
	public MessageReturn checkForMessages(String game, String version, int sequenceNumber, ConfigurationDetails currentConfig) throws Exception, SQLException, RemoteException;

	/**
	 * Checks for registration revocation.
	 * @param registration
	 * @return true if registration is valid; false if it should be disabled
	 * @throws RemoteException
	 * @since 1.3
	 */
	public boolean checkRegistrationValid(RegistrationDetails registration) throws RemoteException;

	/**
	 * Checks for news
	 * @param lastCheck The last date the news was checked
	 * @return a List of News
	 * @throws RemoteException
	 * @since 1.4
	 */
	public List<News> getNews(Calendar lastCheck) throws RemoteException;

}
