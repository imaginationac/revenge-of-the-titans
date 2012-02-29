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
 * Text validator utility
 */
public final class ValidateUtil {

	/**
	 * No constructor
	 */
	private ValidateUtil() {
	}

	/**
	 * Validate a name. A name is valid if it contains only the characters
	 * 'A-Z', space, hyphen and apostrophe. It must contain at least one space.
	 * @param name
	 * @return true if it's valid
	 */
	public static boolean isName(String name) {
		boolean hasSpace = false;
		for (int i = 0; i < name.length(); i ++) {
			char c = name.charAt(i);
			if (Character.isLetter(c)) {
				continue;
			}
			if (c == ' ') {
				hasSpace = true;
				continue;
			}
			if (c == '-' || c == '\'') {
				continue;
			}
			return false;
		}
		return hasSpace;
	}

	/**
	 * Validate a number. Only 0..9 are valid characters
	 * @param name
	 * @return true if it's a number
	 */
	public static boolean isNumber(String number) {
		if (number.length() == 0) {
			return false;
		}
		for (int i = 0; i < number.length(); i ++) {
			if (!Character.isDigit(number.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Validate a phone number. 0..9, ()+- and space are valid characters.
	 * The length must be >= 8 actual digits.
	 * @param phone
	 * @return true if it's valid
	 */
	public static boolean isPhone(String phone) {
		int len = 0;
		for (int i = 0; i < phone.length(); i ++) {
			char c = phone.charAt(i);
			if (Character.isDigit(c)) {
				len ++;
				continue;
			}
			if (c == ' ') {
				continue;
			}
			if (c == '#' || c == '+' || c == '-' || c == '(' || c == ')') {
				continue;
			}
			return false;
		}
		return len >= 8;
	}

	/**
	 * Validate an email address
	 * @param email
	 * @return true if it's valid
	 */
	public static boolean isEmail(String email) {
		// Check email address is valid
		int atPos = email.indexOf('@');
		int dotPos = email.indexOf('.');
		if (atPos < 1) {
			return false;
		} else if (dotPos < 1) {
			return false;
		} else if (!Character.isJavaIdentifierPart(email.charAt(0))) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Validate an address. An address can contain only alphanumeric information,
	 * apostrophes and spaces
	 * @param address
	 * @return true if it's valid
	 */
	public static boolean isAddress(String address) {
		for (int i = 0; i < address.length(); i ++) {
			char c = address.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				continue;
			}
			if (c == ' ') {
				continue;
			}
			if (c == '.' || c == '/' || c == '\'' || c == '\"' || c == ',' || c == '-' || c == '`') {
				continue;
			}
			return false;
		}
		return address.length() > 0;
	}




}
