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

import java.io.*;


/**
 * Describes a credit card type
 *
 * @author cas
 */
public final class CreditCard implements Serializable {

	public static final long serialVersionUID = 1L;

	/** Name */
	private final String name;

	/*
	 * Non-serializable fields
	 */

	/** Expected lengths of the number */
	private transient final int[][] length;

	/** Allowed Prefixes */
	private transient final String[] prefix;

	/** Whether this is a debit card */
	private transient final boolean debit;

	/*
	 * Credit card types
	 */
	public static final CreditCard MASTERCARD = new CreditCard("Mastercard", new String[] {"51", "52", "53", "54", "55"}, new int[][] {{16}, {16}, {16}, {16}, {16}}, false);
	public static final CreditCard VISA = new CreditCard("Visa", new String[] {"4"}, new int[][] {{13, 16}}, false);
	public static final CreditCard AMEX = new CreditCard("AMEX", new String[] {"34", "37"}, new int[][] {{15}, {15}}, false);
	public static final CreditCard DINERS = new CreditCard("Diners Club", new String[] {"300", "301", "302", "303", "304", "305",
			"36", "38"}, new int[][] {{14}, {14}, {14}, {14}, {14}, {14}, {14}, {14}}, false);
	public static final CreditCard JCB = new CreditCard("JCB", new String[] {"3", "2131", "1800"}, new int[][] {{16}, {15}, {15}}, false);
	public static final CreditCard CREDIT = new CreditCard("Credit card", new String[] {""}, new int[][] {{13, 14, 15, 16,17,18,18,20}}, false);

	/*
	 * Debit card types
	 */
	public static final CreditCard DEBIT = new CreditCard("Debit card", new String[] {""}, new int[][] {{16,17,18,18,20}}, true);

	/** All supported cards */
	public static final CreditCard[] CARDS = new CreditCard[] {CREDIT, DEBIT};

	/**
	 * Private constructor. Only the static instances have derived versions of this class.
	 */
	private CreditCard(String name, String[] prefix, int[][] length, boolean debit) {
		this.name = name;
		this.prefix = prefix;
		this.length = length;
		this.debit = debit;
	}

	private String getType() {
		return debit ? "debit" : "credit";
	}

	/**
	 * Validate a credit card number. If the validation is successful, the function returns
	 * null. Otherwise it returns an error message.
	 * @param number The CC number
	 * @return null, for success; or an error message string
	 */
	public final String validate(String number) {

		// Validate that all characters are digits
		for (int i = 0; i < number.length(); i ++) {
			if (!Character.isDigit(number.charAt(i))) {
				return "The "+getType()+" card number should consist only of digits, with no spaces.";
			}
		}

		// Validate prefix for the card
		boolean prefixOK = false;
		for (int i = 0; i < prefix.length; i ++) {
			if (number.startsWith(prefix[i])) {
				// Validate length
				boolean lengthOK = false;
				for (int j = 0; j < length[i].length; j ++) {
					if (number.length() == length[i][j]) {
						lengthOK = true;
					}
				}
				if (!lengthOK) {
					return "The "+getType()+" number is invalid. Please check the number carefully for mistakes.";
				}
				prefixOK = true;
				break;
			}
		}
		if (!prefixOK) {
			return "The "+getType()+" number does not appear to be a "+name+" number.";
		}

		// Use Luhn validation to check the number is valid
		if (!Luhn.check(number)) {
			return "The "+getType()+" number is invalid. Please check the number carefully for mistakes.";
		}

		return null;
	}

	/**
	 * Serialization support
	 * @return a value guaranteed to be in the CARDS array
	 * @throws ObjectStreamException
	 */
	private Object readResolve() throws ObjectStreamException {
		for (int i = 0; i < CARDS.length; i ++) {
			if (CARDS[i].name.equals(name)) {
				return CARDS[i];
			}
		}
		throw new InvalidObjectException("Unknown credit card type "+name);
	}

	/**
	 * @return true if this is a debit card
	 */
	public boolean isDebitCard() {
		return debit;
	}

	/**
	 * @return the name of the card
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[Card:"+getName()+"]";
	}

}
