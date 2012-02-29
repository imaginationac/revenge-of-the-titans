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
package com.shavenpuppy.jglib.opengl;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.lwjgl.util.WritableDimension;
import org.lwjgl.util.WritablePoint;

import com.shavenpuppy.jglib.Glyph;
import com.shavenpuppy.jglib.TextLayout;
import com.shavenpuppy.jglib.sprites.SimpleRenderable;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;
import com.shavenpuppy.jglib.util.Decodeable;

import static org.lwjgl.opengl.GL11.*;
/**
 * Displays text in a box, automatically wrapping words.
 * @author: cas
 */
public class GLTextArea implements SimpleRenderable, WritablePoint, WritableDimension {

	private static final long serialVersionUID = 1L;

	// The font to use
	protected GLFont font;

	// The text to display
	private final StringBuilder text = new StringBuilder();

	// The laid-out glyphs (not all elements will be used)
	private GLGlyph[] glyph;

	// The number of glyphs used in the glyph[] array
	private int numGlyphs;

	// The size of the text area
	private final Dimension size = new Dimension();

	// The calculated text height
	private int textHeight;

	// The location
	private final Point location = new Point();

	// The formatter
	private TextLayout.Format format = TextLayout.WORD_WRAP;

	// The text alignment, from TextLayout
	private TextLayout.HorizontalAlignment alignment = TextLayout.LEFT;

	// Vertical alignment
	private VerticalAlignment verticalAlignment = TOP;

	/** Changed flag */
	private boolean changed = true;

	/** Whether to colour text */
	private boolean coloured;
	private ReadableColor topColour, bottomColour;

	private int alpha = 255;

	/** Leading */
	private int leading;

	/** Vertical Alignments */
	public abstract static class VerticalAlignment implements Serializable, Decodeable{
		private static final long serialVersionUID = 1L;
		private final String display;
		private VerticalAlignment(String display) {
			this.display = display;
		}
		@Override
		public String toString() {
			return display;
		}

		public static Object decode(String in) throws Exception {
			if (in.equalsIgnoreCase(TOP.display)) {
				return TOP;
			} else if (in.equalsIgnoreCase(BOTTOM.display)) {
				return BOTTOM;
			} else if (in.equalsIgnoreCase(BASELINE.display)) {
				return BASELINE;
			} else if (in.equalsIgnoreCase(CENTERED.display)) {
				return CENTERED;
			} else {
				throw new Exception("Unknown vertical alignment '"+in+"'");
			}
		}

	}

	/**
	 * The text is drawn flush with the top of the text area.
	 */
	public static final VerticalAlignment TOP = new VerticalAlignment("Top") {
		private static final long serialVersionUID = 1L;
		private Object readResolve() throws ObjectStreamException {
			return TOP;
		}
	};
	/**
	 * The text is drawn flush with the bottom of the text area.
	 */
	public static final VerticalAlignment BOTTOM = new VerticalAlignment("Bottom") {
		private static final long serialVersionUID = 1L;
		private Object readResolve() throws ObjectStreamException {
			return BOTTOM;
		}
	};
	/**
	 * Vertically centres the text in the textarea's bounds
	 */
	public static final VerticalAlignment CENTERED = new VerticalAlignment("Centered") {
		private static final long serialVersionUID = 1L;
		private Object readResolve() throws ObjectStreamException {
			return CENTERED;
		}
	};
	/**
	 * Baseline alignment. The first line of glyphs is placed on <strong>top</strong> of the
	 * text area's bounding box; subsequent lines are underneath.
	 */
	public static final VerticalAlignment BASELINE = new VerticalAlignment("Baseline") {
		private static final long serialVersionUID = 1L;
		private Object readResolve() throws ObjectStreamException {
			return BASELINE;
		}
	};

	/*
	 * Editing stuff
	 */

	/** Old text */
	private String oldText;

	/** Are we editing? */
	private boolean editing;

	/** Is the cursor currently visible? */
	private boolean cursorVisible;

	/** Current cursor position in the string */
	private int cursorPos;

	/** Cursor flash tick */
	private int flashTick;

	/**
	 * GLTextArea constructor comment.
	 */
	public GLTextArea() {
	}

	/**
	 * Accessor
	 * @return com.powersolve.opengl.GLFont
	 */
	public GLFont getFont() {
		return font;
	}

	/**
	 * @return Returns the final coordinates of the specified glyph
	 */
	public Rectangle getGlyphBounds(int index, Rectangle dest) {
		layout();
		if (dest == null) {
			dest = new Rectangle();
		}
		if (index < 0) {
			dest.setBounds(location.getX(), location.getY(), 0, font.getAscent() + font.getDescent());
		} else {
			GLGlyph g = glyph[index];
			if (g != null) {
				dest.setBounds(g.xpos + location.getX(), g.ypos + location.getY(), g.getAdvance(), font.getAscent() + font.getDescent());
			}
		}
		return dest;
	}

	/**
	 * Returns the glyph at the specified location in the string
	 */
	public GLGlyph getGlyph(int index) {
		return glyph[index];
	}

	/**
	 * @param leading the leading to set
	 */
	public void setLeading(int leading) {
		this.leading = leading;
		changed = true;
	}

	/**
	 * @return the leading
	 */
	public int getLeading() {
		return leading;
	}

	/**
	 * Performs a layout of the text. This should be performed if the text or font is changed or the size of
	 * the text area is adjusted.
	 */
	private void layout() {
		if (!changed) {
			return;
		}

		changed = false;

		// Create initial layout
		assert text != null : this+" has no text";
		assert size != null : this+" has no size";
		assert font != null : this+" has no font";
		TextLayout textLayout = new TextLayout(font.getFont(), font.getScale(), leading, text.toString(), size.getWidth(), format, alignment);
		if (editing) {
			textLayout.setKeepWhiteSpace(true);
		}

		// Now we know how big the text was:
		textHeight = textLayout.getHeight();

		// Now align to given box. Currently the glyphs baseline is at 0,0 and they stretch downwards into negative
		// coordinates. If TOP aligned then need shifting up by -penY. If BOTTOM aligned then they need shifting up
		// by the specified height minus penY.

		final int ty;

		if (verticalAlignment == TOP) {
			// Translate all glyphs up
			ty = size.getHeight();

		} else if (verticalAlignment == CENTERED) {
			// Translate all glyphs up
			ty = textHeight + (size.getHeight() - textHeight) / 2;

		} else if (verticalAlignment == BASELINE) {
			// Move by first ascent
			ty = font.getAscent();

		} else {
			// Translate all glyphs up
			ty = textHeight;

		}

		// Get all the glyphs

		textLayout.apply(new TextLayout.Target() {
			/**
			 * Tells the target how many glyphs will be set. Called first.
			 */
			@Override
			public void setNumGlyphs(int n) {
				numGlyphs = n;
				if (glyph == null || glyph.length < numGlyphs || glyph.length > numGlyphs * 2) {
					glyph = null;
					glyph = new GLGlyph[numGlyphs];
				}
			}

			/**
			 * Each glyph is set via this method
			 */
			@Override
			public void setGlyph(int index, Glyph g, int x, int y) {
				if (glyph[index] == null) {
					glyph[index] = new GLGlyph(font.getTexture(), g, font.getScale());
				} else {
					glyph[index].init(font.getTexture(), g);
				}
				glyph[index].setLocation(x + location.getX(), y + ty + location.getY());
			}
		});

	}

	@Override
	public void render(SimpleRenderer renderer) {
		layout();

		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				// Bind the font's texture first
				font.getTexture().render();
				glEnable(GL_TEXTURE_2D);
				glEnable(GL_BLEND);
				glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
			}
		});

		//renderer.glBegin(GL_QUADS);
		renderGlyphs(renderer);
		//renderer.glEnd();

		// Maybe draw cursor?
		if (cursorVisible && editing) {

			renderer.glRender(new GLRenderable() {
				@Override
				public void render() {
					glDisable(GL_TEXTURE_2D);
				}
			});

			int cursorX;
			int cursorY;
			if (cursorPos <= 0 || glyph.length == 0) {
				cursorPos = 0;
				cursorX = 0;
				cursorY = getHeight() - font.getAscent();
			} else {
				if (cursorPos > glyph.length) {
					cursorPos = glyph.length;
				}
				cursorX = glyph[cursorPos - 1].getXpos() + glyph[cursorPos - 1].getWidth();
				cursorY = glyph[cursorPos - 1].getYpos();
			}
			renderer.glColor(bottomColour);
			short idx = renderer.glVertex2f(cursorX, cursorY - font.getDescent());
			renderer.glVertex2f(cursorX + 4, cursorY - font.getDescent());
			renderer.glColor(topColour);
			renderer.glVertex2f(cursorX + 4, cursorY + font.getHeight() - font.getDescent());
			renderer.glVertex2f(cursorX, cursorY + font.getHeight() - font.getDescent());
			renderer.glRender(GL_TRIANGLE_FAN, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 3)});
		}
	}

	protected void renderGlyphs(SimpleRenderer renderer) {
		for (int i = 0; i < numGlyphs; i++) {
			if (glyph[i] != null) {
				if (coloured) {
					glyph[i].render(topColour, bottomColour, alpha, renderer);
				} else {
					glyph[i].render(renderer);
				}
			}
		}
	}

	/**
	 * @return the numGlyphs
	 */
	public int getNumGlyphs() {
		return numGlyphs;
	}

	/**
	 * Sets the font
	 */
	public void setFont(GLFont font) {
		if (this.font == font) {
			return;
		}
		this.font = font;
		setLeading(font.getLeading());
		changed = true;
	}

	/**
	 * Set the horizontal alignment
	 */
	public void setHorizontalAlignment(TextLayout.HorizontalAlignment alignment) {
		if (this.alignment == alignment) {
			return;
		}
		this.alignment = alignment;
		changed = true;
	}

	/**
	 * Set the vertical alignement
	 */
	public void setVerticalAlignment(VerticalAlignment valignment) {
		if (this.verticalAlignment == valignment) {
			return;
		}
		this.verticalAlignment = valignment;
		changed = true;
	}

	/**
	 * Returns the calculated text height. You can use this to resize the
	 * text area if you like.
	 */
	public int getTextHeight() {
		layout();
		return textHeight;
	}

	/**
	 * Gets the wrap.
	 * @return TextLayout.Format
	 */
	public TextLayout.Format getFormat() {
		return format;
	}

	/**
	 * Sets the text wrapping.
	 * @param wrap The wrap to set
	 */
	public void setFormat(TextLayout.Format format) {
		if (this.format == format) {
			return;
		}
		this.format = format;
		changed = true;
	}

	/*
	 * StringBuilder pseudo-delegate methods. Instead of returning the StringBuilder
	 * which would allow people to dick around with the text inappropriately, we
	 * return the GLTextArea instead.
	 */

	/**
	 * @param b
	 * @return
	 */
	public GLTextArea append(boolean b) {
		changed = true;
		text.append(b);
		return this;
	}
	/**
	 * @param c
	 * @return
	 */
	public GLTextArea append(char c) {
		changed = true;
		text.append(c);
		return this;
	}
	/**
	 * @param str
	 * @return
	 */
	public GLTextArea append(char[] str) {
		changed = true;
		text.append(str);
		return this;
	}
	/**
	 * @param str
	 * @param offset
	 * @param len
	 * @return
	 */
	public GLTextArea append(char[] str, int offset, int len) {
		changed = true;
		text.append(str, offset, len);
		return this;
	}
	/**
	 * @param d
	 * @return
	 */
	public GLTextArea append(double d) {
		changed = true;
		text.append(d);
		return this;
	}
	/**
	 * @param f
	 * @return
	 */
	public GLTextArea append(float f) {
		changed = true;
		text.append(f);
		return this;
	}
	/**
	 * @param i
	 * @return
	 */
	public GLTextArea append(int i) {
		changed = true;
		text.append(i);
		return this;
	}
	/**
	 * @param obj
	 * @return
	 */
	public GLTextArea append(Object obj) {
		changed = true;
		text.append(obj);
		return this;
	}
	/**
	 * @param str
	 * @return
	 */
	public GLTextArea append(String str) {
		changed = true;
		text.append(str);
		return this;
	}
	/**
	 * @param sb
	 * @return
	 */
	public GLTextArea append(StringBuilder sb) {
		changed = true;
		text.append(sb);
		return this;
	}
	/**
	 * @param l
	 * @return
	 */
	public GLTextArea append(long l) {
		changed = true;
		text.append(l);
		return this;
	}
	/**
	 * @return
	 */
	public int capacity() {
		return text.capacity();
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public GLTextArea delete(int start, int end) {
		changed = true;
		text.delete(start, end);
		return this;
	}
	/**
	 * @param index
	 * @return
	 */
	public GLTextArea deleteCharAt(int index) {
		changed = true;
		text.deleteCharAt(index);
		return this;
	}
	/**
	 * @param offset
	 * @param b
	 * @return
	 */
	public GLTextArea insert(int offset, boolean b) {
		changed = true;
		text.insert(offset, b);
		return this;
	}
	/**
	 * @param offset
	 * @param c
	 * @return
	 */
	public GLTextArea insert(int offset, char c) {
		changed = true;
		text.insert(offset, c);
		return this;
	}
	/**
	 * @param offset
	 * @param str
	 * @return
	 */
	public GLTextArea insert(int offset, char[] str) {
		changed = true;
		text.insert(offset, str);
		return this;
	}
	/**
	 * @param index
	 * @param str
	 * @param offset
	 * @param len
	 * @return
	 */
	public GLTextArea insert(int index, char[] str, int offset, int len) {
		changed = true;
		text.insert(index, str, offset, len);
		return this;
	}
	/**
	 * @param offset
	 * @param d
	 * @return
	 */
	public GLTextArea insert(int offset, double d) {
		changed = true;
		text.insert(offset, d);
		return this;
	}
	/**
	 * @param offset
	 * @param f
	 * @return
	 */
	public GLTextArea insert(int offset, float f) {
		changed = true;
		text.insert(offset, f);
		return this;
	}
	/**
	 * @param offset
	 * @param i
	 * @return
	 */
	public GLTextArea insert(int offset, int i) {
		changed = true;
		text.insert(offset, i);
		return this;
	}
	/**
	 * @param offset
	 * @param obj
	 * @return
	 */
	public GLTextArea insert(int offset, Object obj) {
		changed = true;
		text.insert(offset, obj);
		return this;
	}
	/**
	 * @param offset
	 * @param str
	 * @return
	 */
	public GLTextArea insert(int offset, String str) {
		changed = true;
		text.insert(offset, str);
		return this;
	}
	/**
	 * @param offset
	 * @param l
	 * @return
	 */
	public GLTextArea insert(int offset, long l) {
		changed = true;
		text.insert(offset, l);
		return this;
	}
	/**
	 * @return
	 */
	public int length() {
		return text.length();
	}
	/**
	 * @param start
	 * @param end
	 * @param str
	 * @return
	 */
	public GLTextArea replace(int start, int end, String str) {
		changed = true;
		text.replace(start, end, str);
		return this;
	}
	/**
	 * @param index
	 * @param ch
	 */
	public void setCharAt(int index, char ch) {
		if (text.charAt(index) == ch) {
			return;
		}
		changed = true;
		text.setCharAt(index, ch);
	}
	/**
	 * @param newLength
	 */
	public void setLength(int newLength) {
		if (newLength == text.length()) {
			return;
		}
		changed = true;
		text.setLength(newLength);
	}

	/*
	 * WritableRectangle delegate methods
	 */

	/**
	 * @param dest
	 */
	public void getLocation(WritablePoint dest) {
		location.getLocation(dest);
	}
	/**
	 * @return
	 */
	public int getX() {
		return location.getX();
	}
	/**
	 * @return
	 */
	public int getY() {
		return location.getY();
	}
	/**
	 * @param x
	 * @param y
	 */
	@Override
	public void setLocation(int x, int y) {
		if (location.getX() == x && location.getY() == y) {
			return;
		}
		changed = true;
		location.setLocation(x, y);
	}
	/**
	 * @param p
	 */
	@Override
	public void setLocation(ReadablePoint p) {
		if (location.getX() == p.getX() && location.getY() == p.getY()) {
			return;
		}
		changed = true;
		location.setLocation(p);
	}
	/**
	 * @param x
	 */
	@Override
	public void setX(int x) {
		if (location.getX() == x) {
			return;
		}
		changed = true;
		location.setX(x);
	}
	/**
	 * @param y
	 */
	@Override
	public void setY(int y) {
		if (location.getY() == y) {
			return;
		}
		changed = true;
		location.setY(y);
	}
	/**
	 * @param dx
	 * @param dy
	 */
	public void translate(int dx, int dy) {
		if (dx == 0 && dy == 0) {
			return;
		}
		changed = true;
		location.translate(dx, dy);
	}
	/**
	 * @param p
	 */
	public void translate(ReadablePoint p) {
		if (p.getX() == 0 && p.getY() == 0) {
			return;
		}
		changed = true;
		location.translate(p);
	}
	/**
	 * @param p
	 */
	public void untranslate(ReadablePoint p) {
		if (p.getX() == 0 && p.getY() == 0) {
			return;
		}
		changed = true;
		location.untranslate(p);
	}

	/**
	 * @return
	 */
	public int getHeight() {
		layout();
		return size.getHeight();
	}
	/**
	 * @return
	 */
	public int getWidth() {
		layout();
		return size.getWidth();
	}
	/**
	 * @param height
	 */
	@Override
	public void setHeight(int height) {
		if (size.getHeight() == height) {
			return;
		}
		changed = true;
		size.setHeight(height);
	}
	/**
	 * @param width
	 */
	@Override
	public void setWidth(int width) {
		if (size.getWidth() == width) {
			return;
		}
		changed = true;
		size.setWidth(width);
	}

	/**
	 * @param dest
	 */
	public void getSize(WritableDimension dest) {
		layout();
		size.getSize(dest);
	}

	public void setBounds(int x, int y, int w, int h) {
		setLocation(x, y);
		setSize(w, h);
	}

	public void setBounds(ReadablePoint p, ReadableDimension d) {
		setLocation(p);
		setSize(d);
	}

	public void setBounds(ReadableRectangle r) {
		setLocation(r);
		setSize(r);
	}

	/**
	 * @param w
	 * @param h
	 */
	@Override
	public void setSize(int w, int h) {
		if (size.getWidth() == w && size.getHeight() == h) {
			return;
		}
		changed = true;
		size.setSize(w, h);
	}
	/**
	 * @param d
	 */
	@Override
	public void setSize(ReadableDimension d) {
		if (size.equals(d)) {
			return;
		}
		changed = true;
		size.setSize(d);
	}
	/**
	 * @return verticalAlignment.
	 */
	public VerticalAlignment getVerticalAlignment() {
		return verticalAlignment;
	}
	/**
	 * @return alignment.
	 */
	public TextLayout.HorizontalAlignment getHorizontalAlignment() {
		return alignment;
	}

	/**
	 * Set the text
	 * @param newText, may not be null
	 */
	public void setText(String newText) {
		text.setLength(0);
		text.append(newText);
		changed = true;
		cursorPos = newText.length();
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text.toString();
	}

	/**
	 * @param coloured the coloured to set
	 */
	public void setColoured(boolean coloured) {
		this.coloured = coloured;
	}

	/**
	 * @return the coloured
	 */
	public boolean isColoured() {
		return coloured;
	}

	public void setColour(ReadableColor c) {
		setColoured(true);
		bottomColour = c;
		topColour = c;
	}

	public void setBottomColour(ReadableColor c) {
		setColoured(true);
		bottomColour = c;
	}

	public void setTopColour(ReadableColor c) {
		setColoured(true);
		topColour = c;
	}

	public void tick() {
		if (editing) {
			// We're editing
			flashCursor();
			processKeyboard();
		}
	}

	private void flashCursor() {
		flashTick ++;
		if (flashTick > 6) {
			flashTick = 0;
			cursorVisible = !cursorVisible;
		}
	}

	private void processKeyboard() {
		int oldCursorPos = cursorPos;

		while (Keyboard.next()) {
			if (!Keyboard.getEventKeyState()) {
				continue;
			}
			int key = Keyboard.getEventKey();

			switch (key) {
				case Keyboard.KEY_DOWN:
				case Keyboard.KEY_END:
					cursorPos = text.length();
					break;
				case Keyboard.KEY_UP:
				case Keyboard.KEY_HOME:
					cursorPos = 0;
					break;
				case Keyboard.KEY_LEFT:
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
						cursorPos = 0;
					} else if (cursorPos > 0) {
						cursorPos --;
					}
					break;
				case Keyboard.KEY_RIGHT:
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
						cursorPos = text.length();
					} else if (cursorPos < text.length()) {
						cursorPos ++;
					}
					break;
				case Keyboard.KEY_DELETE:
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
						text.setLength(0);
						cursorPos = 0;
						changed = true;
					} else if (cursorPos < text.length()) {
						text.deleteCharAt(cursorPos);
						changed = true;
					}
					onEdited();
					break;
				case Keyboard.KEY_BACK:
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
						text.setLength(0);
						cursorPos = 0;
						changed = true;
					} else {
						if (cursorPos > 0) {
							text.deleteCharAt(-- cursorPos);
							changed = true;
						}
					}
					onEdited();
					break;
				case Keyboard.KEY_TAB:
				case Keyboard.KEY_RETURN:
					// Change focus
					changeFocus();
					return;
				case Keyboard.KEY_ESCAPE:
					// Cancel edits
					cancel();
					return;
				default:
					// Type this character
					char c = Keyboard.getEventCharacter();
					if (c == 22 || key == Keyboard.KEY_INSERT && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
						String paste = Sys.getClipboard();
						if (paste == null) {
							break;
						}
						for (int i = 0; i < paste.length(); i ++) {
							c = Character.toLowerCase(paste.charAt(i));
							if (text.length() < text.capacity() && acceptChar(c)) {
								text.insert(cursorPos ++, c);
								changed = true;
								onEdited();
							} else {
								break;
							}
						}
					} else if (c == 26 && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
						undo();
					} else if (c >= 32 && c < 127) {
						if (acceptChar(c)) {
							text.insert(cursorPos ++, c);
							changed = true;
							onEdited();
						}
					}
					break;
			}
		}

		if (oldCursorPos != cursorPos) {
			cursorVisible = true;
			flashTick = 0;
		}
	}

	public boolean acceptChar(char c) {
		return true;
	}

	/**
	 * Change focus to something else. Called on TAB or RETURN
	 */
	public final void changeFocus() {
		editing = false;
		cursorVisible = false;
		flashTick = 0;
		onChangeFocus();
	}

	protected void onChangeFocus() {
	}

	protected void onEdited() {
	}

	public boolean isEditing() {
		return editing;
	}

	/**
	 * Sets this text field to "editing" mode or not
	 * @param editing
	 */
	public void setEditing(boolean editing) {
		this.editing = editing;

		if (editing) {
			oldText = text.toString();
			changed = true;
		}
	}

	/**
	 * Undo all edits. Does nothing if not currently editing.
	 */
	public void undo() {
		if (editing) {
			setText(oldText);
		}
	}

	/**
	 * Cancel editing programmatically. Also called when user taps ESC.
	 */
	public void cancel() {
		undo();
		setEditing(false);
		onCancelled();
	}

	/**
	 * Called when editing is cancelled
	 */
	protected void onCancelled() {
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

}
