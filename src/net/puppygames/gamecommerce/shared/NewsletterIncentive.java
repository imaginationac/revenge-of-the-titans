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
import java.util.Arrays;
/**
 * Manages newsletter incentives
 */
public final class NewsletterIncentive implements Serializable {

	public static final long serialVersionUID = 1L;

	public static final int CODE_LENGTH = 8;

	/** Encoding for prefs */
	private static final String ENCODING = "UTF-8";

	/** The algorithm used */
	public static final String ALGORITHM = "DSA";

	/** The algorithm used to do signatures */
	private static final String SIGNATURE_ALGORITHM = "SHA1withDSA";

	/** The public key */
	public static final String PUBLIC_KEY =
		"308201b83082012c06072a8648ce3804013082011f02818100fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c70215009760508f15230bccb292b982a2eb840bf0581cf502818100f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a0381850002818100cfff377f44e641902f83c8bd33af2f02efddbb53466d3b0a889f4f936fc88e1f8f39427944d35c04f6c7d6afa38a642d54faa18117a9aeac6f1f5e98b2d8dbdff60406aa11fd43962d052319d883cf361ed65b0b35453fcb37496be9e69448ce4445626629ada640b2f16d56c4c062252bca564087d920ec7805ac2520dd4620";

	/** Email */
	private final String email;

	/** Game title */
	private final String game;

	/** Version */
	private final String version;

	/** Installation */
	private final long installation;

	/** Prize */
	private final String prize;

	/** The special encrypted code */
	private byte[] code;


	/**
	 * C'tor
	 * @param email TODO
	 * @param game
	 * @param version
	 * @param installation
	 * @param prize
	 */
	public NewsletterIncentive(String email, String game, String version, long installation, String prize) {
		this.email = email;
	    this.game = game;
	    this.version = version;
	    this.installation = installation;
	    this.prize = prize;
    }

	public String getPrize() {
	    return prize;
    }

	/**
	 * @return a short code, {@link #CODE_LENGTH} characters long
	 */
	public String getShortCode() {
		String packedRegCode = HexEncoder.encode(code);
		return packedRegCode.substring(packedRegCode.length() - CODE_LENGTH, packedRegCode.length());
	}

	/**
	 * Check to ensure that this is a valid newsletter incentive
	 * @return true if valid, false if otherwise
	 * @throws Exception if validation could not be performed for some
	 * reason (in which case the code isn't valid anyway)
	 */
	public boolean validate() throws Exception {
		KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(HexDecoder.decode(PUBLIC_KEY));
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
		dsa.initVerify(publicKey);
		dsa.update(email.getBytes(ENCODING));
		dsa.update(game.getBytes(ENCODING));
		dsa.update(version.getBytes(ENCODING));
		dsa.update(String.valueOf(installation).getBytes(ENCODING));
		dsa.update(prize.getBytes(ENCODING));
		return dsa.verify(code);
	}

	/**
	 * Create the regcode for these user details using a private key.
	 * @param privateKey The private key to use
	 * @throws Exception if something goes wrong
	 */
	public void register(PrivateKey privateKey) throws Exception {
		Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
		dsa.initSign(privateKey);
		dsa.update(email.getBytes(ENCODING));
		dsa.update(game.getBytes(ENCODING));
		dsa.update(version.getBytes(ENCODING));
		dsa.update(String.valueOf(installation).getBytes(ENCODING));
		dsa.update(prize.getBytes(ENCODING));
		code = dsa.sign();
	}

 	/**
 	 * @return the regcode
 	 */
 	public byte[] getCode() {
 		return code;
 	}

 	/**
 	 * @param code
 	 */
 	public void setCode(byte[] code) {
	    this.code = code;
    }

 	@Override
    public String toString() {
	    StringBuilder builder = new StringBuilder();
	    builder.append("NewsletterIncentive [email=");
	    builder.append(email);
	    builder.append(", game=");
	    builder.append(game);
	    builder.append(", version=");
	    builder.append(version);
	    builder.append(", installation=");
	    builder.append(installation);
	    builder.append(", prize=");
	    builder.append(prize);
	    builder.append(", code=");
	    builder.append(Arrays.toString(code));
	    builder.append("]");
	    return builder.toString();
    }
}
