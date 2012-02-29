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

import java.util.*;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Special bonus prizes for non-Puppygames customers as an incentive to sign up people for the newsletter
 */
public abstract class PrizeFeature extends Feature {

	private static final List<PrizeFeature> PRIZES = new ArrayList<PrizeFeature>();

	/** Message to display on screen describing the prize */
	private String screenMessage;

	/** Message to display in email */
	private String emailMessage;

	/** Message to display when redeemed */
	private String successMessage;

	/**
	 * C'tor
	 * @param name
	 */
	public PrizeFeature(String name) {
		super(name);
		setAutoCreated();
	}

	public String getScreenMessage() {
	    return screenMessage;
    }

	public String getEmailMessage() {
	    return emailMessage;
    }

	public String getSuccessMessage() {
	    return successMessage;
    }

	@Override
	protected void doRegister() {
		PRIZES.add(this);
	}

	@Override
	protected void doDeregister() {
		PRIZES.remove(this);
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		screenMessage = XMLUtil.getText(XMLUtil.getChild(element, "screenMessage"), "");
		emailMessage = XMLUtil.getText(XMLUtil.getChild(element, "emailMessage"), "");
		successMessage = XMLUtil.getText(XMLUtil.getChild(element, "successMessage"), "");
	}

	/**
	 * @return true if this prize should be offered
	 */
	public abstract boolean isValid();

	/**
	 * Redeem this prize
	 */
	public abstract void redeem();

	/**
	 * @return an unmodifiable List of all the PrizeFeatures
	 */
	public static List<PrizeFeature> getPrizes() {
		return Collections.unmodifiableList(PRIZES);
	}
}
