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

import java.io.Serializable;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
/**
 * A customer's registration details
 */
public final class RegistrationDetails implements Serializable {

	/** Encoding for prefs */
	private static final String ENCODING = "UTF-8";
	private static final String OLD_ENCODING = "ISO-8859-1";

	public static final long serialVersionUID = 1L;

	/** The public key */
	public static final String PUBLIC_KEY =
		"308201b83082012c06072a8648ce3804013082011f02818100fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c70215009760508f15230bccb292b982a2eb840bf0581cf502818100f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a0381850002818100cfff377f44e641902f83c8bd33af2f02efddbb53466d3b0a889f4f936fc88e1f8f39427944d35c04f6c7d6afa38a642d54faa18117a9aeac6f1f5e98b2d8dbdff60406aa11fd43962d052319d883cf361ed65b0b35453fcb37496be9e69448ce4445626629ada640b2f16d56c4c062252bca564087d920ec7805ac2520dd4620";

	/** The key and value for date checking */
	private static final String ENCRYPT_KEY = "name";
	private static final int EXPIRY_DAYS = 7;
	private static final String PASSWORD ="xyzzy";

	/** THe name to use when the registration is unregistered */
	public static final String UNREGISTERED_NAME = "Unregistered";

	/** The algorithm used */
	public static final String ALGORITHM = "DSA";

	/** The algorithm used to do signatures */
	private static final String SIGNATURE_ALGORITHM = "SHA1withDSA";

	/** Game title */
	private final String game;

	/** Customer's name */
	private String name;

	/** Customer's address */
	private String address;

	/** Customer's email address */
	private String email;

	/** Customer's registration code */
	private byte[] regCode;

	/** Whether the key can be exported */
	private boolean exportable;

	/**
	 * Database interface
	 */
	public interface RegistrationDatabase {
		/**
		 * Create a new registration record. The registration record will be in unregistered
		 * status awaiting the user to activate their copy.
		 * <p>
		 * This method is called by the private ecommerce server when a successful payment
		 * has been processed.
		 *
		 * @param regCode The HexEncoded registration key
		 * @param name The registered username
		 * @param address The registered address
		 * @param email The registered email address
		 * @param authCode The authorisation code (last 14 chars of the regcode)
		 * @param purchaseDate The purchase date
		 * @param game The game title
		 * @param orderNumber The order number from the payment processor
		 * @param configuration A serializable configuration object that can be encoded in toString()
		 * @throws SQLException
		 */
		public void insertRegistration(
			String regcode,
			String name,
			String address,
			String email,
			String authcode,
			java.sql.Date purchasedate,
			String game,
			String orderNumber,
			ConfigurationDetails configuration
		) throws SQLException;
	}

	/**
	 * C'tor; loads the registratoin details from a result set.
	 * @param rs Result set
	 */
	public RegistrationDetails(ResultSet rs) throws SQLException {
		game = rs.getString("game");
		regCode = HexDecoder.decode(rs.getString("regcode"));
		name = rs.getString("name");
		email = rs.getString("email");
		address = rs.getString("address");
	}

	/**
	 * C'tor; loads the registration details from local preferences
	 * @param game The game title
	 */
	public RegistrationDetails(String game) throws Exception {
		this.game = game;

		Preferences prefs = Preferences.userNodeForPackage(RegistrationDetails.class).node(game);
		name = prefs.get("name", UNREGISTERED_NAME);
		address = prefs.get("address", "");
		email = prefs.get("email", "");
		String code = prefs.get("key", "unregistered");
		if (code.equals("unregistered")) {
			regCode = null;
		} else {
			regCode = HexDecoder.decode(code);
		}
	}

	/**
	 * Constructor
	 */
	public RegistrationDetails(
		String game,
		String name,
		String address,
		String email
	) {
		this(game, name, address, email, null);
	}

	/**
	 * Constructor
	 */
	public RegistrationDetails(
		String game,
		String name,
		String address,
		String email,
		String key
	) {
		this.game = game;
		this.name = name;
		this.address = address;
		this.email = email;
		this.regCode = HexDecoder.decode(key);
	}

	public void createNewRegistration(RegistrationDatabase db, String orderNo, String game, String authCode, java.sql.Date purchaseDate, ConfigurationDetails configuration) throws SQLException {
		db.insertRegistration(HexEncoder.encode(regCode), name, address, email, authCode, purchaseDate, game, orderNo, configuration);
	}

	public void createNewRegistration(RegistrationDatabase db, String orderNo, String game, java.sql.Date purchaseDate, ConfigurationDetails configuration) throws SQLException {
		db.insertRegistration(HexEncoder.encode(regCode), name, address, email, getAuthCode(), purchaseDate, game, orderNo, configuration);
	}

	/**
	 * @return an authorisation code
	 */
	public String getAuthCode() {
		String packedRegCode = HexEncoder.encode(regCode);
		return packedRegCode.substring(packedRegCode.length() - 14, packedRegCode.length());
	}

	/**
	 * Stash this in some preferences
	 */
	public void toPreferences() {
		if (regCode == null) {
			return;
		}
		Preferences prefs = Preferences.userNodeForPackage(RegistrationDetails.class).node(game);
		prefs.put("name", name);
		prefs.put("address", address);
		prefs.put("email", email);
		prefs.put("key", HexEncoder.encode(regCode));
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Clear away registration details for a game
	 * @param game
	 */
	public static void clearRegistration(String game) {
		Preferences prefs = Preferences.userNodeForPackage(RegistrationDetails.class).node(game);
		try {
			prefs.removeNode();
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Check to ensure that this is a valid registration.
	 * @param a public encryption key to use as validation
	 * @return true if the registration is valid, false otherwise
	 * @throws Exception if validation could not be performed for some
	 * reason (in which case the code isn't valid anyway)
	 */
	public boolean validate(PublicKey publicKey) throws Exception {
		if (regCode == null) {
			return false;
		}

		// Try three different encodings
		try {
			if (validate(publicKey, ENCODING)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		try {
			if (validate(publicKey, OLD_ENCODING)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		return validate(publicKey, null);
	}
	private boolean validate(PublicKey publicKey, String encoding) throws Exception {
		Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
		dsa.initVerify(publicKey);
		if (encoding != null) {
			dsa.update(name.getBytes(encoding));
			dsa.update(address.getBytes(encoding));
			dsa.update(email.getBytes(encoding));
		} else {
			dsa.update(name.getBytes());
			dsa.update(address.getBytes());
			dsa.update(email.getBytes());
		}
		return dsa.verify(regCode);
	}

	/**
	 * Create the regcode for these user details using a private key.
	 * @param privateKey The private key to use
	 * @throws Exception if something goes wrong
	 */
	public void register(PrivateKey privateKey) throws Exception {
		Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
		dsa.initSign(privateKey);
		dsa.update(name.getBytes(ENCODING));
		dsa.update(address.getBytes(ENCODING));
		dsa.update(email.getBytes(ENCODING));
		regCode = dsa.sign();
	}

	/**
	 * Deregister the game! This happens when a game finds itself using the
	 * hiscore server and the hiscore server reports them as banned.
	 * Nothing happens immediately but the regcode will be invalid in a few days
	 * days...
	 */
	public void deregister() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(RegistrationDetails.class).node(game);
			if (prefs.get(ENCRYPT_KEY, "").equals(name)) {
				// First export registration over the top of the current one, so the one
				// that's stored has a 21-day expiry...
				toPreferences();
				// Then clear the little flag that tells us to check the date by sneakily
				// putting a space on the end
				prefs.put(ENCRYPT_KEY, name+" ");
			}
		} catch (Exception e) {
		}

	}

	/**
	 * @return the game
	 */
	public String getGame() {
		return game;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

 	/**
 	 * @return the regcode
 	 */
 	public byte[] getRegCode() {
 		return regCode;
 	}

 	/**
 	 * @param nameOnly TODO
 	 * @return a String to display on the title screen
 	 */
 	public String toTitleScreen(boolean nameOnly) {
 		if (name.equals(UNREGISTERED_NAME)) {
 			return UNREGISTERED_NAME;
 		} else if (nameOnly) {
 			return "Registered to "+name;
 		} else {
 			return "Registered to "+name+" ("+email+") "+address;
 		}
 	}

 	/* (non-Javadoc)
 	 * @see java.lang.Object#toString()
 	 */
 	@Override
	public String toString() {
 		if (name.equals(UNREGISTERED_NAME)) {
 			return "RegistrationDetails[UNREGISTERED]";
 		} else {
 			return "RegistrationDetails["+name+"/"+email+"/"+address+"]";
 		}
 	}

 	/**
 	 * Check registration details. If we reckon the user is unregistered we'll throw
 	 * an exception of some sort. Otherwise we'll return the registration details.
 	 * @param gameTitle The game's title
 	 * @return RegistrationDetails
 	 * @throws Exception
 	 */
 	public static RegistrationDetails checkRegistration(String gameTitle) throws Exception {
 		RegistrationDetails testRegistrationDetails = new RegistrationDetails(gameTitle);
		KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(HexDecoder.decode(PUBLIC_KEY));
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		if (testRegistrationDetails.validate(publicKey)) {
			return testRegistrationDetails;
		} else {
			return null;
		}
 	}
}
