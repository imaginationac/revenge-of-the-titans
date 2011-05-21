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
package com.shavenpuppy.jglib.resources;

import com.shavenpuppy.jglib.opengl.GLUtil;
import com.shavenpuppy.jglib.util.Parseable;

/**
 * $Id: GLEnum.java,v 1.5 2011/04/18 23:28:06 cix_foo Exp $
 * For decoding GL enums automatically
 * @author $Author: cix_foo $
 * @version $Revision: 1.5 $
 */
public class GLEnum implements Parseable {

	/** The actual GL value */
	private int value;

	/** "Null" value */
	private boolean isNull;

	/**
	 * C'tor
	 */
	public GLEnum() {
	}

	/**
	 * C'tor
	 */
	public GLEnum(int value) {
		this.value = value;
		isNull = false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (isNull) {
			return "(Null)";
		}
		return GLUtil.recode(value);
	}

	/**
	 * @return Returns the value (on;y valid when !isNull())
	 */
	public int getValue() {
		return value;
	}

	/**
	 * @return Returns the isNull.
	 */
	public boolean isNull() {
		return isNull;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.util.Parseable#fromString(java.lang.String)
	 */
	@Override
	public void fromString(String src) throws Exception {
		isNull = true;
		if (src == null || "".equals(src) || "(Null)".equals(src)) {
			value = 0;
			return;
		} else {
			value = GLUtil.decode(src);
			isNull = false;
		}
	}

}
