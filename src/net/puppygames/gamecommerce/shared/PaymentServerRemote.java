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

import javax.crypto.SealedObject;



/**
 * The remote payment server
 */
public interface PaymentServerRemote extends Remote {

	/** Remote name of the server */
	public static final String REMOTE_NAME = "ingamepayment";

	/** The URL of the registration server */
	public static final String RMI_URL = "//puppygames.net/"+REMOTE_NAME;

	/** The encryption algorithm */
	public static final String ALGORITHM = "DES";

	/** The key encryption algorithm */
	public static final String KEY_ENCRYPTION_ALGORITHM = "RSA";

	/** The public RSA key */
	public static final String PUBLIC_KEY = "305c300d06092a864886f70d0101010500034b0030480241009cdc92607d2a70499c1a791b4a82d3e8c81b776077bbf382b60a3b15ee5ffec10a81e2d451be2fcbeead20163d13be676974c85d840559563e1b32dd1228cc090203010001";

	/**
	 * Perform online purchase & registration
	 * @param secretKey the key used to encrypt the payment details
	 * @param paymentDetails a SealedObject containing a PaymentDetails
	 * @return registration details
	 * @throws RemoteException
	 * @throws Exception
	 */
	public PaymentResult register(byte[] secretKey, SealedObject paymentDetails) throws RemoteException, Exception;



}
