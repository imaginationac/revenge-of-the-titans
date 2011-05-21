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
package com.shavenpuppy.jglib;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

import org.lwjgl.util.*;
import org.lwjgl.util.vector.Vector3f;

/**
 * Writes out resources as XML
 * @author Cas
 */
public class XMLResourceWriter {

	private final Writer writer;

	/** Current indent */
	private int indent;

	/** Stack of tags */
	private final Stack<String> tags = new Stack<String>();

	/** Writing attributes? */
	private boolean writingAttributes;

	/** First attribute flag */
	private boolean firstAttribute;

	/** Compact mode */
	private boolean compact;

	/**
	 * C'tor
	 */
	public XMLResourceWriter(Writer writer) {
		this.writer = writer;
	}

	/**
	 * @param compact the compact to set
	 */
	public void setCompact(boolean compact) {
		this.compact = compact;
	}

	/**
	 * @return the compact mode flag
	 */
	public boolean isCompact() {
		return compact;
	}

	/**
	 * @return the writer
	 */
	public Writer getWriter() {
		return writer;
	}

	/**
	 * Write out a tag
	 * @param tag The tag name you want to use
	 * @param indent The indent level
	 * @return a String full of lovely XML
	 * @throws IOException
	 */
	public void writeTag(String tag) throws IOException {
		if (writingAttributes) {
			indent --;
			if (!firstAttribute) {
				writeIndent();
			}
			writer.write(">\n");
			indent ++;
		}
		writeIndent();
		writer.write("<");
		writer.write(tag);
		indent ++;
		tags.push(tag);
		writingAttributes = true;
		firstAttribute = true;
	}

	public void closeTag() throws IOException {
		String tag = tags.pop();
		indent --;
		if (writingAttributes) {
			if (!firstAttribute && !compact) {
				writeIndent();
			}
			writer.write("/>\n");
		} else {
			writeIndent();
			writer.write("</");
			writer.write(tag);
			writer.write(">\n");
		}
		writingAttributes = false;
	}

	private void writeIndent() throws IOException {
		for (int i = 0; i < indent; i ++) {
			writer.write('\t');
		}
	}

	public void writeAttribute(String name, Object data) throws IOException {
		writeAttribute(name, data, false);
	}

	public void writeAttribute(String name, Object data, boolean alwaysWrite) throws IOException {
		if (!writingAttributes) {
			throw new IOException("Can't write attributes here");
		}
		if (data == null) {
			return;
		}
		// Don't write default values
		if (!alwaysWrite) {
			if (data instanceof String) {
				if (((String) data).equals("")) {
					return;
				}
			} else if (data instanceof Number) {
				if (((Number) data).doubleValue() == 0.0) {
					return;
				}
			} else if (data instanceof Boolean) {
				if (!((Boolean) data).booleanValue()) {
					return;
				}
			}
		}

		if (firstAttribute) {
			if (!compact) {
				writer.write('\n');
			} else {
				writer.write(' ');
			}
			firstAttribute = false;
		} else {
			if (compact) {
				writer.write(' ');
			}
		}
		if (!compact) {
			writeIndent();
		}
		writer.write(name);
		writer.write("=\"");

		// Special cases
		if (data instanceof Rectangle) {
			Rectangle r = (Rectangle) data;
			writer.write(String.valueOf(r.getX()));
			writer.write(", ");
			writer.write(String.valueOf(r.getY()));
			writer.write(", ");
			writer.write(String.valueOf(r.getWidth()));
			writer.write(", ");
			writer.write(String.valueOf(r.getHeight()));
		} else if (data instanceof Point) {
			Point p = (Point) data;
			writer.write(String.valueOf(p.getX()));
			writer.write(", ");
			writer.write(String.valueOf(p.getY()));
		} else if (data instanceof Vector3f) {
			Vector3f v = (Vector3f) data;
			writer.write(String.valueOf(v.getX()));
			writer.write(", ");
			writer.write(String.valueOf(v.getY()));
			writer.write(", ");
			writer.write(String.valueOf(v.getZ()));
		} else if (data instanceof Dimension) {
			Dimension d = (Dimension) data;
			writer.write(String.valueOf(d.getWidth()));
			writer.write(", ");
			writer.write(String.valueOf(d.getHeight()));
		} else if (data instanceof Color) {
			Color c = (Color) data;
			writer.write(String.valueOf(c.getRed()));
			writer.write(", ");
			writer.write(String.valueOf(c.getGreen()));
			writer.write(", ");
			writer.write(String.valueOf(c.getBlue()));
			writer.write(", ");
			writer.write(String.valueOf(c.getAlpha()));
		} else {
			writer.write(data.toString());
		}
		if (compact) {
			writer.write('"');
		} else {
			writer.write("\"\n");
		}
	}

	public void writeText(String text) throws IOException {
		if (writingAttributes) {
			writingAttributes = false;
			writer.write(">");
		}
		writer.write(text);
	}

 	public void writeAttribute(String name, boolean value) throws IOException {
 		writeAttribute(name, value, false);
	}

 	public void writeAttribute(String name, float value) throws IOException {
 		writeAttribute(name, value, false);
	}

 	public void writeAttribute(String name, double value) throws IOException {
 		writeAttribute(name, value, false);
	}

 	public void writeAttribute(String name, int value) throws IOException {
 		writeAttribute(name, value, false);
	}

 	public void writeAttribute(String name, boolean value, boolean alwaysWrite) throws IOException {
 		writeAttribute(name, new Boolean(value), alwaysWrite);
	}

 	public void writeAttribute(String name, float value, boolean alwaysWrite) throws IOException {
 		writeAttribute(name, new Float(value), alwaysWrite);
	}

 	public void writeAttribute(String name, double value, boolean alwaysWrite) throws IOException {
 		writeAttribute(name, new Double(value), alwaysWrite);
	}

 	public void writeAttribute(String name, int value, boolean alwaysWrite) throws IOException {
 		writeAttribute(name, new Integer(value), alwaysWrite);
	}

}
