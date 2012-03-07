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



/**
 * Callback interface for the purchase and register windows.
 */
public interface Game {

	/**
	 * @return the game title, eg. "Alien Flux"
	 */
	public String getTitle();

	/**
	 * @return the game version, eg. "1.5d"
	 */
	public String getVersion();

	/**
	 * @return the affililate code
	 */
	public String getAffiliate();

	/**
	 * @return the installation number
	 */
	public long getInstallation();

	/**
	 * @return the game configuration
	 */
	public ConfigurationDetails getConfiguration();

	/**
	 * @return your website (minus the protocol), eg. "www.oddlabs.com"
	 */
	public String getWebsite();

	/**
	 * @return a String describing the normal price in dollars, eg. "$19.95"
	 */
	public String getPriceUSD();

	/**
	 * @return a String describing the normal price in sterling, eg. "£19.95"
	 */
	public String getPriceGBP();

	/**
	 * @return a String describing the normal price in euros, eg. "E19.95"
	 */
	public String getPriceEUR();

	/**
	 * Sets the registration details after a successful registration of a purchased
	 * game. You may want to update your title screen at this point to show the
	 * new reigstration details and unlock any locked features.
	 * @param newDetails The new registration details
	 */
	public void setRegistrationDetails(RegistrationDetails newDetails);

}