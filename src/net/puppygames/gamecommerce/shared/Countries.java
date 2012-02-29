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

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all the countries and the default currencies to use
 */
public final class Countries {

	/** All the countries supported and their currency */
	private static final String[] COUNTRY = {
		"United States", "USD",
		"United Kingdom", "GBP",
		"Canada", "USD",
		"Germany", "EUR",
		"France", "EUR",
		"Australia", "USD",
		"--------------------", null,
		"Argentina", "USD",
		"Australia", "USD",
		"Austria", "EUR",
		"Bahamas", "USD",
		"Belgium", "EUR",
		"Bermuda", "USD",
		"Brazil", "USD",
		"Bulgaria", "USD",
		"Canada", "USD",
		"Canary Is.", "USD",
		"Cayman Is.", "USD",
		"Cyprus", "EUR",
		"Czech Republic", "EUR",
		"Denmark", "EUR",
		"Falkland Is.", "GBP",
		"Faroe Is.", "GBP",
		"Finland", "EUR",
		"France", "EUR",
		"Germany", "EUR",
		"Gibraltar", "EUR",
		"Greece", "EUR",
		"Guernsey", "EUR",
		"Hong Kong", "USD",
		"Hungary", "EUR",
		"Iceland", "EUR",
		"Ireland", "EUR",
		"Israel", "EUR",
		"Italy", "EUR",
		"Jamaica", "USD",
		"Japan", "USD",
		"Jersey", "EUR",
		"Jordan", "USD",
		"Korea, D.P.R Of", "USD",
		"Korea, Rep. Of", "USD",
		"Kuwait", "USD",
		"Luxembourg", "EUR",
		"Mexico", "USD",
		"Netherlands", "EUR",
		"New Zealand", "USD",
		"Norway", "EUR",
		"Poland", "EUR",
		"Portugal", "EUR",
		"Saudi Arabia", "USD",
		"Singapore", "USD",
		"South Africa", "USD",
		"Spain", "EUR",
		"Sweden", "EUR",
		"Switzerland", "EUR",
		"Taiwan", "USD",
		"Tunisia", "USD",
		"Turkey", "EUR",
		"United Arab Emirates", "USD",
		"United Kingdom", "GBP",
		"United States", "USD",
		"Venezuela", "USD",
		"Virgin Is. British", "GBP",
		"Virgin Is. U.S.", "USD"
	};

	/**
	 * No constructor
	 */
	private Countries() {
	}

	/**
	 * @return a List of Countries
	 */
	public static List<String> getCountries() {
		List<String> ret = new ArrayList<String>(COUNTRY.length / 2);
		for (int i = 0; i < COUNTRY.length; i += 2) {
			ret.add(COUNTRY[i]);
		}
		return ret;
	}

	/**
	 * @param country The country
	 * @return the currency to use for a particular country, or null, for no valid currency
	 */
	public static String getCurrency(String country) {
		for (int i = 0; i < COUNTRY.length; i ++) {
			if (COUNTRY[i * 2].equals(country)) {
				return COUNTRY[i * 2 + 1];
			}
		}
		return null;
	}
}
