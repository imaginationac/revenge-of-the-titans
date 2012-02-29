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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.StringTokenizer;

import org.lwjgl.util.Point;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.util.Decodeable;

/**
 * This lays out text in a string within a specific width. The text baseline is at (0,0)
 * and then the y coordinate becomes negative as the text goes down each line.
 */
public class TextLayout {

	/** The font, from which we can get the ascent, descent, and leading */
	private final Font font;

	/** Scaling */
	private final float scale;

	/** The glyphs: not all used */
	private final Glyph[] glyph;

	/** Number of glyphs used */
	private int numGlyphs;

	/** The format we're going to use */
	private final Format format;

	/** Horizontal alignments */
	public abstract static class HorizontalAlignment implements Serializable, Decodeable {
		private static final long serialVersionUID = 1L;
		private final String display;
		private HorizontalAlignment(String display) {
			this.display = display;
		}
		@Override
		public String toString() {
			return display;
		}

		/**
		 * Decode method, for Decodeable marker
		 * @param in
		 * @return
		 * @throws Exception
		 */
		public static Object decode(String in) throws Exception {
			if (in.equalsIgnoreCase(LEFT.display)) {
				return LEFT;
			} else if (in.equalsIgnoreCase(RIGHT.display)) {
				return RIGHT;
			} else if (in.equalsIgnoreCase(CENTERED.display)) {
				return CENTERED;
			} else {
				throw new Exception("Unknown horizontal alignment '"+in+"'");
			}
		}
	}

	/** The alignment */
	private HorizontalAlignment alignment;
	public static final HorizontalAlignment LEFT = new HorizontalAlignment("Left") {
		private static final long serialVersionUID = 1L;
		private Object readResolve() throws ObjectStreamException {
			return LEFT;
		}
	};
	public static final HorizontalAlignment RIGHT = new HorizontalAlignment("Right") {
		private static final long serialVersionUID = 1L;
		private Object readResolve() throws ObjectStreamException {
			return RIGHT;
		}
	};
	public static final HorizontalAlignment CENTERED = new HorizontalAlignment("Centered") {
		private static final long serialVersionUID = 1L;
		private Object readResolve() throws ObjectStreamException {
			return CENTERED;
		}
	};

	/** The glyph positions */
	private final int[] x, y;

	/** Leading */
	private int leading;

	/** The layout's width - specified during construction */
	private int width;

	/** The layout's height - available after construction */
	private int height;

	/** Keeps track of penX and penY */
	private int penX, penY;

	/** Handy geometry */
	private static final Rectangle tempBounds = new Rectangle();
	private static final Point tempPoint = new Point();

	/**
	 * The text layout target. Once you've laid out some text int the text layout you need to
	 * apply it to some target.
	 */
	public interface Target {
		/**
		 * Tells the target how many glyphs will be set. Called first.
		 */
		public void setNumGlyphs(int n);

		/**
		 * Each glyph is set via this method
		 */
		public void setGlyph(int index, Glyph glyph, int x, int y);
	}

	/**
	 * Formatting class. Formats a paragraph.
	 */
	public static abstract class Format implements Serializable, Decodeable {
		private static final long serialVersionUID = 1L;

		private final String display;

		private Format(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}

		/**
		 * Format the incoming paragraph starting at the current position penX, penY.
		 * Should leave penX, penY in the location that the next paragraph should start.
		 */
		abstract void format(TextLayout layout, String paragraph, boolean lastParagraph);

		/**
		 * Decode method, for Decodeable marker
		 * @param in
		 * @return
		 * @throws Exception
		 */
		public static Object decode(String in) throws Exception {
			if (in.equalsIgnoreCase(NO_WRAP.display)) {
				return NO_WRAP;
			} else if (in.equalsIgnoreCase(WRAPPED.display)) {
				return WRAPPED;
			} else if (in.equalsIgnoreCase(JUSTIFIED.display)) {
				return JUSTIFIED;
			} else if (in.equalsIgnoreCase(WORD_WRAP.display)) {
				return WORD_WRAP;
			} else {
				throw new Exception("Unknown text format '"+in+"'");
			}
		}

	}

	/** No wrap - the maximum width is ignored */
	private static final class NoWrap extends Format {
		private static final long serialVersionUID = 1L;
		NoWrap() {
			super("None");
		}
		@Override
		void format(TextLayout layout, String paragraph, boolean lastParagraph) {
			final int n = paragraph.length();
			Glyph last = null;
			layout.penY -= layout.font.getAscent() * layout.scale;
			for (int i = 0; i < n; i++) {
				Glyph next = layout.font.map(paragraph.charAt(i));
				next.getBounds(tempBounds);
				next.getBearing(tempPoint);
				// Scale bounds and bearing and kerning
				tempBounds.setBounds
					(
						(int) (tempBounds.getX() * layout.scale),
						(int) (tempBounds.getY() * layout.scale),
						(int) (tempBounds.getWidth() * layout.scale),
						(int) (tempBounds.getHeight() * layout.scale)
					);
				tempPoint.setLocation
					(
						(int) (tempPoint.getX() * layout.scale),
						(int) (tempPoint.getY() * layout.scale)
					);
				int kerning = (int) (next.getKerningAfter(last) * layout.scale);
				layout.x[layout.numGlyphs] = tempPoint.getX() + layout.penX - kerning;
				layout.y[layout.numGlyphs] = layout.penY + tempPoint.getY();
				layout.glyph[layout.numGlyphs++] = next;
				layout.penX += (int) (next.getAdvance() * layout.scale) + kerning;
			}

			if (lastParagraph) {
				layout.align(0, layout.penX);
				layout.nextLine(true);
			}
		}
		private Object readResolve() throws ObjectStreamException {
			return NO_WRAP;
		}

	}

	/** Wrapped - the characters are wrapped at the specified width */
	private static final class Wrapped extends Format {
		private static final long serialVersionUID = 1L;
		Wrapped() {
			super("Character Wrap");
		}
		@Override
		void format(TextLayout layout, String paragraph, boolean lastParagraph) {
			final int n = paragraph.length();
			Glyph last = null;

			int lastWidth = 0, lastAdvance = 0, startGlyph = layout.numGlyphs;

			layout.penY -= layout.font.getAscent() * layout.scale;

			for (int i = 0; i < n; i++) {
				Glyph next = layout.font.map(paragraph.charAt(i));
				next.getBounds(tempBounds);
				next.getBearing(tempPoint);
				// Scale bounds and bearing and kerning
				tempBounds.setBounds
					(
						(int) (tempBounds.getX() * layout.scale),
						(int) (tempBounds.getY() * layout.scale),
						(int) (tempBounds.getWidth() * layout.scale),
						(int) (tempBounds.getHeight() * layout.scale)
					);
				tempPoint.setLocation
					(
						(int) (tempPoint.getX() * layout.scale),
						(int) (tempPoint.getY() * layout.scale)
					);
				int kerning = (int) (next.getKerningAfter(last) * layout.scale);

				// Does the character fit on the line? If not, we must start a new line.
				if (layout.penX + tempPoint.getX() + tempBounds.getWidth() - kerning > layout.width) {
					layout.align(startGlyph, layout.penX + lastWidth - lastAdvance);
					startGlyph = layout.numGlyphs;
					layout.nextLine(false);
					layout.penY -= ((layout.font.getAscent() + layout.font.getDescent()) * layout.scale + layout.leading);
					last = null;
				} else {
					last = next;
				}

				layout.x[layout.numGlyphs] = tempPoint.getX() + layout.penX - kerning;
				layout.y[layout.numGlyphs] = layout.penY + tempPoint.getY();
				layout.glyph[layout.numGlyphs] = next;
				lastAdvance = (int) (next.getAdvance() * layout.scale) + kerning;
				layout.penX += lastAdvance;
				lastWidth = layout.x[layout.numGlyphs] - tempPoint.getX();

				layout.numGlyphs++;
			}

			layout.align(startGlyph, layout.penX + lastWidth - lastAdvance);
			layout.nextLine(lastParagraph);

		}
		private Object readResolve() throws ObjectStreamException {
			return WRAPPED;
		}

	}

	/** Word wrapped - words are wrapped at the specified width */
	private static final class WordWrapped extends Format {
		private static final long serialVersionUID = 1L;
		WordWrapped() {
			super("Word Wrap");
		}
		@Override
		void format(TextLayout layout, String paragraph, boolean lastParagraph) {

			StringTokenizer st = new StringTokenizer(paragraph, " ", true);

			int lastGlyphWidth = 0, lastAdvanceWidth = 0, startGlyph = layout.numGlyphs;
			boolean firstWordDone = false;

			layout.penY -= layout.font.getAscent() * layout.scale;

			while (st.hasMoreTokens()) {
				String word = st.nextToken();

				// Work out the width of the word...
				Glyph last = null;
				int w = 0, lastWidthOffset = 0, lastAdvance = 0;
				final int n = word.length();
				for (int i = 0; i < n; i++) {
					Glyph next = layout.font.map(word.charAt(i));
					next.getBounds(tempBounds);
					next.getBearing(tempPoint);
					// Scale bounds and bearing and kerning
					tempBounds.setBounds
						(
							(int) (tempBounds.getX() * layout.scale),
							(int) (tempBounds.getY() * layout.scale),
							(int) (tempBounds.getWidth() * layout.scale),
							(int) (tempBounds.getHeight() * layout.scale)
						);
					tempPoint.setLocation
						(
							(int) (tempPoint.getX() * layout.scale),
							(int) (tempPoint.getY() * layout.scale)
						);
					int kerning = (int) (next.getKerningAfter(last) * layout.scale);
					lastAdvance = (int) (next.getAdvance() * layout.scale) + kerning;
					lastWidthOffset = tempPoint.getX() + tempBounds.getWidth() - kerning;
					w += lastAdvance;
					last = next;
				}
				w = (w - lastAdvance) + lastWidthOffset;

				// Does the word fit on the line?
				if (firstWordDone && layout.penX + w > layout.width) {
					// No, so start a new one. First justify the current line
					layout.align(startGlyph, layout.penX + lastGlyphWidth - lastAdvanceWidth);
					startGlyph = layout.numGlyphs;
					layout.nextLine(false);
					layout.penY -= layout.font.getAscent() * layout.scale;
					firstWordDone = false;
				} else {
					// Yes, so append a space to the previous word on the line, if there was one
					// Advance by the width of a single space glyph
					if (firstWordDone) {
//						Glyph space = layout.font.map(' ');
//						layout.penX += space.getAdvance();
					}
				}

				// 'Draw' the word
				last = null;
				for (int i = 0; i < n; i++) {
					Glyph next = layout.font.map(word.charAt(i));
					next.getBounds(tempBounds);
					next.getBearing(tempPoint);
					// Scale bounds and bearing and kerning
					tempBounds.setBounds
						(
							(int) (tempBounds.getX() * layout.scale),
							(int) (tempBounds.getY() * layout.scale),
							(int) (tempBounds.getWidth() * layout.scale),
							(int) (tempBounds.getHeight() * layout.scale)
						);
					tempPoint.setLocation
						(
							(int) (tempPoint.getX() * layout.scale),
							(int) (tempPoint.getY() * layout.scale)
						);
					int kerning = (int) (next.getKerningAfter(last) * layout.scale);

					layout.x[layout.numGlyphs] = tempPoint.getX() + layout.penX - kerning;
					layout.y[layout.numGlyphs] = layout.penY + tempPoint.getY();
					layout.glyph[layout.numGlyphs] = next;
					lastAdvance = (int) (next.getAdvance() * layout.scale) + kerning;
					layout.penX += lastAdvance;

					layout.numGlyphs++;
					last = next;
				}

				firstWordDone = true;
			}

			// Align the last line
			layout.align(startGlyph, layout.penX + lastGlyphWidth - lastAdvanceWidth);
			layout.nextLine(lastParagraph);

		}
		private Object readResolve() throws ObjectStreamException {
			return WORD_WRAP;
		}

	}

	/** Justified - words are wrapped at the specified width and then whitespace is expanded to fill the line */
	private static final class Justified extends Format {
		private static final long serialVersionUID = 1L;
		Justified() {
			super("Justified");
		}
		@Override
		void format(TextLayout layout, String paragraph, boolean lastParagraph) {
			layout.penY -= layout.font.getDescent() * layout.scale;
			if (!lastParagraph) {
				layout.penY -= layout.leading;
			}
			layout.penX = 0;
		}
		private Object readResolve() throws ObjectStreamException {
			return JUSTIFIED;
		}
	}

	/** Formatting constants */
	public static final Format NO_WRAP = new NoWrap();
	public static final Format WRAPPED = new Wrapped();
	public static final Format WORD_WRAP = new WordWrapped();
	public static final Format JUSTIFIED = new Justified();

	/** Keep whitespace without removing it - useful for editing text areas */
	private boolean keepWhiteSpace;

	/**
	 * Constructor for TextLayout.
	 */
	public TextLayout(Font font, float scale, int leading, String text, int width, Format format, HorizontalAlignment alignment) {
		this.font = font;
		this.scale = scale;
		this.leading = leading;
		this.width = width;
		this.format = format;
		this.alignment = alignment;
		glyph = new Glyph[text.length()];
		x = new int[glyph.length];
		y = new int[glyph.length];
		doLayout(text);
	}

	/**
	 * @param keepWhiteSpace the keepWhiteSpace to set
	 */
	public void setKeepWhiteSpace(boolean keepWhiteSpace) {
		this.keepWhiteSpace = keepWhiteSpace;
	}

	/**
	 * @return the keepWhiteSpace
	 */
	public boolean getKeepWhiteSpace() {
		return keepWhiteSpace;
	}

	/**
	 * Does the layout
	 */
	private void doLayout(String text) {

		// Remove superfluous carriage returns at the end
		if (!keepWhiteSpace) {
			while (text.endsWith("\n")) {
				text = text.substring(0, text.length() - 1);
			}
		}

		StringTokenizer st = new StringTokenizer(text, "\n", true);

		boolean lastTokenWasParagraph = true;

		// Break the text up into paragraphs separated by newlines

		while (st.hasMoreTokens()) {
			String paragraph = st.nextToken();
			if ("\n".equals(paragraph)) {
				if (lastTokenWasParagraph) {
					penY -= (int) ((font.getAscent() + font.getDescent()) * scale) + leading;
				}
				lastTokenWasParagraph = true;
				continue;
			} else {
				lastTokenWasParagraph = false;
			}

			format.format(this, paragraph, !st.hasMoreTokens());
		}

		// There we go, and here's the height (making the bounding box (0, -height) - (width, 0))
		height = -penY;
	}

	/**
	 * Returns the glyphs and their offsets. Make sure the incoming arrays are big enough
	 * to accommodate the results (ie. at least as big as numGlyphs)
	 */
	public void getGlyphs(Glyph[] glyph, int[] x, int[] y) {
		System.arraycopy(this.glyph, 0, glyph, 0, numGlyphs);
		System.arraycopy(this.x, 0, x, 0, numGlyphs);
		System.arraycopy(this.y, 0, y, 0, numGlyphs);
	}

	/**
	 * Gets the font.
	 * @return Returns a Font
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * Gets the height.
	 * @return Returns a int
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Gets the width.
	 * @return Returns a int
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Gets the numGlyphs.
	 * @return Returns a int
	 */
	public int getNumGlyphs() {
		return numGlyphs;
	}

	/**
	 * Convenience function to go to the next line
	 */
	private void nextLine(boolean isLastParagraph) {
		penY -= font.getDescent() * scale;
		if (!isLastParagraph) {
			penY -= leading;
		}
		penX = 0;
	}
	/**
	 * Gets the alignment.
	 * @return alignment
	 */
	public HorizontalAlignment getAlignment() {
		return alignment;
	}

	/**
	 * Align glyphs
	 */
	private void align(int startGlyph, int advance) {
		int dx;

		if (alignment == RIGHT) {
			// Move to right
			dx = width - advance;
		} else if (alignment == CENTERED) {
			// Move to middle
			dx = (width - advance) >> 1;
		} else {
			// Do nothing
			dx = 0;
		}

		for (int i = startGlyph; i < numGlyphs; i++) {
			x[i] += dx;
		}
	}

	/**
	 * Apply the text layout to a target.
	 */
	public void apply(Target target) {

		target.setNumGlyphs(numGlyphs);
		for (int i = 0; i < numGlyphs; i++) {
			target.setGlyph(i, glyph[i], x[i], y[i]);
		}

	}
}