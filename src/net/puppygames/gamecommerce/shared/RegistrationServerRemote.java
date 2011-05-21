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
package net.puppygames.gamecommerce.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Client interface to the public registration server.
 */
public interface RegistrationServerRemote extends Remote {

	/** Remote name of the server */
	public static final String REMOTE_NAME = "registration";

	/** The URL of the registration server */
	public static final String RMI_URL = "//puppygames.net/" + REMOTE_NAME;

	/**
	 * Perform affiliate registration
	 *
	 * @param email The customer's email address
	 * @param authCode The customer's authorisation code
	 * @param game The game name
	 * @param version The game version
	 * @param affiliate The affiliate code
	 * @param configuration The game configuration details
	 * @return registration details
	 * @throws RemoteException
	 * @throws Exception
	 */
	public RegistrationDetails register(String email, String authCode, String game, String version, String affiliate, ConfigurationDetails configuration)
			throws RemoteException, Exception;

	/**
	 * Perform affiliate registration
	 * @param email
	 * @param game
	 * @param version
	 * @param affiliate
	 * @param installation
	 * @param os_name
	 * @param configuration
	 * @return
	 * @throws RemoteException
	 * @throws Exception
	 */
	public RegistrationDetails register(String email, String game, String version, String affiliate, long installation, String os_name,
			ConfigurationDetails configuration) throws RemoteException, RegisterException, Exception;

	/**
	 * Perform registration
	 * @param email
	 * @param game
	 * @param version
	 * @param installation
	 * @param os_name
	 * @param configuration
	 * @return
	 * @throws RemoteException
	 * @throws RegisterException
	 * @throws Exception
	 */
	public RegistrationDetails register(String email, String game, String version, long installation, String os_name, ConfigurationDetails configuration)
			throws RemoteException, RegisterException, Exception;

}
