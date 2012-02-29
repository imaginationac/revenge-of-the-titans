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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.sprites.SimpleRenderable;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;
import com.shavenpuppy.jglib.util.Decodeable;

import static org.lwjgl.opengl.GL11.*;
/**
 * Displays styled text in a box, automatically wrapping words.
 */
public class GLStyledText implements SimpleRenderable {

	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG = false;

	/** The text to display, with formatting commands in curly brackets eg. {font:big.glfont}HELLO */
	private String text;

	/** The strings of text: a List of StyledText */
	private final List<StyledText> strings = new ArrayList<StyledText>();

	/** The lines of text: a List of StyledLines */
	private final List<StyledLine> lines = new ArrayList<StyledLine>();

	/** The size and location of the text area */
	private final Rectangle bounds = new Rectangle();

	/** The calculated text height - may be larger than that given by bounds */
	private int textHeight;

	/** The number of actual glyphs */
	private int numGlyphs;

	/** The text alignment */
	private HorizontalAlignment horizontalAlignment = LEFT;

	/** Vertical alignment */
	private VerticalAlignment verticalAlignment = TOP;

	/** Justified text */
	private boolean justified;

	/** Changed flag: if the text changes etc. this is tripped */
	private boolean changed = true;

	/** Blend colour */
	private ReadableColor color = Color.WHITE;

	/** Alpha */
	private int alpha = 255;

	/** Leading */
	private int leading;

	/** String factory: takes the text, parses it, and creates runs of StyledText */
	private StyledTextFactory factory;

	private int penY;
	private StyledWord currentWord = null;
	private final ArrayList<StyledWord> currentWords = new ArrayList<StyledWord>();
	private StyledLine currentLine = null;
	private StyledText currentStyle = null;


	/**
	 * StyledText represents text in a particular colour and font.
	 */
	public interface StyledText extends SimpleRenderable {

		/**
		 * Get the top color of the text
		 * @return a ReadableColor; never null
		 */
		ReadableColor getTopColor();

		/**
		 * Get teh bottom color of the text
		 * @return a ReadableColor; never null
		 */
		ReadableColor getBottomColor();

		/**
		 * Get the font for the text
		 * @return a GLFont; never null
		 */
		GLFont getFont();

		/**
		 * Get the text
		 * @return a String; never null, but may be empty
		 */
		String getText();

		/**
		 * Add a thing to render
		 * @param renderable
		 */
		void add(SimpleRenderable renderable);

	}

	public interface StyledTextFactory {

		/**
		 * Parse the incoming text, and generate a List of StyledText.
		 * @param text The raw text; may not be null
		 * @param dest Destination list; may not be null; will be cleared first
		 */
		void parse(String text, List<StyledText> dest);

	}

	/**
	 * The default styled text factory
	 */
	public static class DefaultStyledTextFactory implements StyledTextFactory {

		private ReadableColor topColor, bottomColor;
		private GLFont font;

		public DefaultStyledTextFactory(ReadableColor topColor, ReadableColor bottomColor, GLFont font) {
			this.topColor = topColor;
			this.bottomColor = bottomColor;
			this.font = font;
		}

		@Override
		public void parse(String text, List<StyledText> dest) {
			dest.clear();

			// start parsing. Better get a { first!
			StringBuilder sb = new StringBuilder(text.length());
			int n = text.length();
			for (int i = 0; i < n; ) {
				char c = text.charAt(i);
				if (c == '{') {
					if (sb.length() > 0) {
						DefaultStyledText dst = new DefaultStyledText(sb.toString(), font, topColor, bottomColor);
						dest.add(dst);
						sb = new StringBuilder(text.length() - i);
					}
					i += parse(text, i + 1);
				} else {
					sb.append(c);
					i ++;
				}
			}

			if (sb.length() > 0) {
				DefaultStyledText dst = new DefaultStyledText(sb.toString(), font, topColor, bottomColor);
				dest.add(dst);
			}
		}

		private int parse(String text, int pos) {
			// Find index of }
			int idx = text.indexOf('}', pos);
			// Get substring
			String format = text.substring(pos, idx);
			// Split into tokens separated by spaces
			StringTokenizer st = new StringTokenizer(format, " ");
			// Parse each token, a key:value pair
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				int colon = token.indexOf(':');
				if (colon == -1) {
					if (DEBUG) {
						System.out.println("Bad token : "+token);
					}
				} else {
					String key = token.substring(0, colon).toLowerCase();
					String value = token.substring(colon + 1);
					if (key.equals("top")) {
						topColor = new MappedColor(value);
					} else if (key.equals("bottom")) {
						bottomColor = new MappedColor(value);
					} else if (key.equals("color")) {
						topColor = new MappedColor(value);
						bottomColor = topColor;
					} else if (key.equals("font")) {
						font = (GLFont) Resources.get(value);
					} else {
						if (DEBUG) {
							System.out.println("Bad key : "+key);
						}
					}
				}
			}
			return idx - pos + 2;
		}
	}

	/**
	 * The simple styled text factory, which doesn't do any styling except apply a font and uniform color to all the text
	 */
	public static class SimpleStyledTextFactory implements StyledTextFactory {

		private final ReadableColor color;
		private final GLFont font;

		public SimpleStyledTextFactory(ReadableColor color, GLFont font) {
			this.color = color;
			this.font = font;
		}

		@Override
		public void parse(String text, List<StyledText> dest) {
			dest.clear();
			DefaultStyledText dst = new DefaultStyledText(text, font, color, color);
			dest.add(dst);
		}
	}

	/**
	 * Default styled text implementation
	 */
	public static class DefaultStyledText implements StyledText {

		private final ReadableColor bottomColor, topColor;
		private final GLFont font;
		private final String text;
		private final ArrayList<SimpleRenderable> renderables = new ArrayList<SimpleRenderable>();

		public DefaultStyledText(String text, GLFont font, ReadableColor topColor, ReadableColor bottomColor) {
			this.text = text;
			this.font = font;
			this.topColor = topColor;
			this.bottomColor = bottomColor;
		}

		@Override
		public ReadableColor getBottomColor() {
			return bottomColor;
		}

		@Override
		public GLFont getFont() {
			return font;
		}

		@Override
		public String getText() {
			return text;
		}

		@Override
		public ReadableColor getTopColor() {
			return topColor;
		}

		@Override
		public void add(SimpleRenderable renderable) {
			renderables.add(renderable);
		}

		@Override
		public void render(SimpleRenderer renderer) {
			int n = renderables.size();
			if (n == 0) {
				return;
			}
			renderer.glRender(new GLRenderable() {
				@Override
				public void render() {
					font.render();
				}
			});
			for (int i = 0; i < n; i ++) {
				SimpleRenderable renderable = renderables.get(i);
				renderable.render(renderer);
			}
		}
	}

	/**
	 * A StyledWord is a sequence of GLGlyphs associated with a StyledText. It optionally
	 * has a wordBreak at the end of it, allowing spaces to be expanded. Initially the origin for a StyledWorld is 0,0
	 */
	private class StyledWord implements SimpleRenderable {
		private final ArrayList<GLGlyph> glyphs = new ArrayList<GLGlyph>();
		private final StyledText style;

		/** Scaled metrics */
		private int ascent, descent, width;

		/** Gap at the end of the word, if we're not the last word on the line */
		private int gap;

		private GLGlyph lastGlyph;

		private final Color topColor = new Color();
		private final Color bottomColor = new Color();

		StyledWord(StyledText style) {
			this.style = style;

			ascent = style.getFont().getAscent();
			descent = style.getFont().getDescent();
		}

		@Override
		public void render(SimpleRenderer renderer) {
			int n = glyphs.size();
			if (n == 0) {
				return;
			}

			if (style.getTopColor() != null) {
				ColorUtil.blendColor(style.getTopColor(), color, topColor);
				ColorUtil.blendColor(style.getBottomColor(), color, bottomColor);
			}

			for (int i = 0; i < n; i ++) {
				glyphs.get(i).render(topColor, bottomColor, alpha, renderer);
			}
		}

		void addGlyph(GLGlyph glyph) {
			glyphs.add(glyph);
			glyph.setLocation(width + glyph.getBearingX(), glyph.getBearingY());
			width += glyph.getAdvance();

			// Maybe kern with last glyph
			if (lastGlyph != null) {
				width += glyph.getKerningAfter(lastGlyph);
			}

			lastGlyph = glyph;
		}

		void layout(int x, int y) {
			int n = glyphs.size();
			for (int i = 0; i < n; i ++) {
				GLGlyph glyph = glyphs.get(i);
				glyph.setLocation(glyph.getXpos() + x, glyph.getYpos() + y);
			}
		}

		int calcGap() {
			return style.getFont().map(' ').getAdvance();
		}

		void addGap() {
			gap = calcGap();
		}

		@Override
		public String toString() {
			return "StyledWord["+style+", "+glyphs+"]";
		}
	}

	/**
	 * A StyledLine is a List of StyledWords. Initially all at y coordinate 0 until laid out.
	 */
	private class StyledLine {

		private final ArrayList<StyledWord> words = new ArrayList<StyledWord>();

		/** The ascent and descent of the line (no leading) */
		private int ascent, descent;

		/** Total width */
		private int width;

		/** Current font */
		GLFont currentFont;

		private boolean paragraphBreak;

		private StyledWord lastWord;

		StyledLine(GLFont currentFont) {
			this.currentFont = currentFont;
		}

		/**
		 * Check if this list of unbroken words fits
		 * @param newWords
		 * @return true if they all fit
		 */
		boolean fits(ArrayList<StyledWord> newWords) {
			if (lastWord == null) {
				// No words on this line yet - so always allow a fit
				return true;
			}
			int testWidth = width;
			int n = newWords.size();
			for (int i = 0; i < n; i ++) {
				testWidth += (newWords.get(i)).width;
			}
			if (lastWord != null) {
				testWidth += lastWord.calcGap();
			}
			return testWidth <= bounds.getWidth();
		}

		@Override
		public String toString() {
			return "StyledLine["+width+", "+ascent+", "+descent+", "+paragraphBreak+", words:" + words+"]";
		}

		void addWords(List<StyledWord> newWords) {
			words.addAll(newWords);
			int n = newWords.size();
			for (int i = 0; i < n; i ++) {
				StyledWord word = newWords.get(i);
				ascent = Math.max(ascent, word.ascent);
				descent = Math.max(descent, word.descent);
				width += word.width;
			}
			if (lastWord != null) {
				// Add a gap from the last word
				lastWord.addGap();
				width += lastWord.gap;
			}

			lastWord = newWords.get(n - 1);
		}

		void layout(int y) {
			int n = words.size();
			if (n == 0) {
				return;
			}

			int x = bounds.getX();

			if (justified && !paragraphBreak && n != 1) {
				// Spread the available whitespace out amongst the words.
				int availableWidth = bounds.getWidth() - width;
				int spread = availableWidth / (words.size() - 1);
				int remainder = availableWidth % (words.size() - 1);
				for (int i = 0; i < n; i ++) {
					StyledWord word = words.get(i);
					word.gap += spread;
					if (i < remainder) {
						word.gap ++;
					}
					word.layout(x, y);
					x += word.width + word.gap;
				}
				return;
			}

			if (horizontalAlignment == RIGHT) {
				// Move to right
				x += bounds.getWidth() - width;
			} else if (horizontalAlignment == CENTERED) {
				// Move to middle
				x += bounds.getWidth() - width >> 1;
			}

			for (int i = 0; i < n; i ++) {
				StyledWord word = words.get(i);
				word.layout(x, y);
				x += word.width + word.gap;
			}

		}
	}


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

	/** Vertical Alignments */
	public abstract static class VerticalAlignment implements Serializable, Decodeable {
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
			} else if (in.equalsIgnoreCase(VCENTERED.display)) {
				return VCENTERED;
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
	public static final VerticalAlignment VCENTERED = new VerticalAlignment("Centered") {
		private static final long serialVersionUID = 1L;
		private Object readResolve() throws ObjectStreamException {
			return VCENTERED;
		}
	};

	/**
	 * GLTextArea constructor comment.
	 */
	public GLStyledText() {
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

	private void endWord(boolean nextLine, boolean wordBreak) {
		if (currentStyle == null) {
			return;
		}
		if (currentWord != null) {
			currentStyle.add(currentWord);
		}
		if (currentLine == null) {
			currentLine = new StyledLine(currentStyle.getFont());
		}
		if (!currentLine.fits(currentWords)) {
			// Start a new line.
			lines.add(currentLine);
			currentLine = new StyledLine(currentStyle.getFont());
		}

		if (wordBreak || nextLine) {
			// Add current words to the line
			if (currentWords.size() == 0) {

			} else {
				currentLine.addWords(currentWords);
				currentWords.clear();
			}
		}

		currentWord = null;
		if (nextLine) {
			lines.add(currentLine);
			currentLine = new StyledLine(currentStyle.getFont());
		}
	}

	/**
	 * Performs a layout of the text. This should be performed if the text or font is changed or the size of
	 * the text area is adjusted.
	 */
	private void layout() {
		if (!changed) {
			return;
		}

		// Reset everything
		changed = false;
		textHeight = 0;
		numGlyphs = 0;
		strings.clear();
		lines.clear();

		// Parse the text into StyledTexts
		if (factory == null) {
			// There's no factory
			if (DEBUG) {
				System.out.println("GLStyledText has no factory! "+text);
			}
			return;
		}

		if (text == null) {
			return;
		}

		// Parse the text into a bunch of StyledText
		factory.parse(text, strings);

		// Parse the StyledText into StyledWords and StyledLines
		int numStrings = strings.size();
		penY = 0;
		for (int i = 0; i < numStrings; i ++) {
			currentStyle = strings.get(i);
			int length = currentStyle.getText().length();
			for (int j = 0; j < length; j ++) {
				char c = currentStyle.getText().charAt(j);
				if (c == '\n') {
					// End current word and go to the next line.
					endWord(true, true);
				} else if (c == ' ') {
					// End current word.
					endWord(false, true);
				} else {
					// Append glyph to current word.
					if (currentWord == null) {
						currentWord = new StyledWord(currentStyle);
						currentWords.add(currentWord);
					}
					currentWord.addGlyph(currentStyle.getFont().map(c));
				}
			}
			endWord(false, false);
		}
		endWord(true, true);
		currentWord = null;
		currentLine = null;
		currentStyle = null;
		currentWords.clear();

		// Calculate height of the text now.
		int numLines = lines.size();
		if (numLines == 0) {
			return;
		}
		for (int i = 0; i < numLines; i ++) {
			StyledLine line = lines.get(i);
			if (line.lastWord == null) {
				line.ascent = line.currentFont.getAscent();
				line.descent = line.currentFont.getDescent();
			}
			textHeight += line.ascent + line.descent;
			if (i < numLines - 1) {
				textHeight += leading;
			}
		}
		int firstLineAscent = (lines.get(0)).ascent;

		// Now align to given box. Currently the glyphs baseline is at 0,0 and they stretch downwards into negative
		// coordinates. If TOP aligned then need shifting up by -penY. If BOTTOM aligned then they need shifting up
		// by the specified height minus penY.

		final int ty;

		if (verticalAlignment == TOP) {
			// Translate all glyphs up
			ty = bounds.getHeight() - firstLineAscent;

		} else if (verticalAlignment == VCENTERED) {
			// Translate all glyphs up
			ty = textHeight + (bounds.getHeight() - textHeight) / 2 - firstLineAscent;

		} else {
			// Translate all glyphs up
			ty = textHeight - firstLineAscent;

		}

		penY = 0;
		for (int i = 0; i < numLines; i ++) {
			StyledLine line = lines.get(i);
			line.layout(bounds.getY() + ty - penY);
			penY += line.descent + leading;
			if (i < numLines - 1) {
				StyledLine nextLine = lines.get(i + 1);
				penY += nextLine.ascent;
			}
		}

	}

	@Override
	public void render(SimpleRenderer renderer) {
		layout();

		if (strings.size() == 0) {
			return;
		}

		// Setup state
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glEnable(GL_TEXTURE_2D);
				glEnable(GL_BLEND);
				glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
			}
		});

		// Render each renderable in turn
		int n = strings.size();
		for (int i = 0; i < n; i ++) {
			SimpleRenderable renderable = strings.get(i);
			renderable.render(renderer);
		}
	}

	/**
	 * @return the numGlyphs
	 */
	public int getNumGlyphs() {
		return numGlyphs;
	}

	/**
	 * Set the horizontal alignment
	 */
	public void setHorizontalAlignment(HorizontalAlignment alignment) {
		if (this.horizontalAlignment == alignment) {
			return;
		}
		this.horizontalAlignment = alignment;
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
	 * @return
	 */
	public int getX() {
		return bounds.getX();
	}

	/**
	 * @return
	 */
	public int getY() {
		return bounds.getY();
	}

	/**
	 * @param x
	 * @param y
	 */
	public void setLocation(int x, int y) {
		if (bounds.getX() == x && bounds.getY() == y) {
			return;
		}
		bounds.setLocation(x, y);
		changed = true;
	}
	/**
	 * @param p
	 */
	public void setLocation(ReadablePoint p) {
		setLocation(p.getX(), p.getY());
	}

	/**
	 * @param x
	 */
	public void setX(int x) {
		if (bounds.getX() == x) {
			return;
		}
		bounds.setX(x);
		changed = true;
	}

	/**
	 * @param y
	 */
	public void setY(int y) {
		if (bounds.getY() == y) {
			return;
		}
		bounds.setY(y);
		changed = true;
	}

	/**
	 * @return the height of the text bounds
	 */
	public int getHeight() {
		return bounds.getHeight();
	}

	/**
	 * @return the width of the text bounds
	 */
	public int getWidth() {
		return bounds.getWidth();
	}

	public void setHeight(int height) {
		if (bounds.getHeight() == height) {
			return;
		}
		changed = true;
		bounds.setHeight(height);
	}

	public void setWidth(int width) {
		if (bounds.getWidth() == width) {
			return;
		}
		changed = true;
		bounds.setWidth(width);
	}

	public void setBounds(int x, int y, int w, int h) {
		setLocation(x, y);
		setSize(w, h);
	}

	public void setBounds(ReadableRectangle r) {
		setLocation(r);
		setSize(r);
	}

	public void setSize(int w, int h) {
		if (bounds.getWidth() == w && bounds.getHeight() == h) {
			return;
		}
		changed = true;
		bounds.setSize(w, h);
	}

	public void setSize(ReadableDimension d) {
		setSize(d.getWidth(), d.getHeight());
	}

	public VerticalAlignment getVerticalAlignment() {
		return verticalAlignment;
	}

	public HorizontalAlignment getHorizontalAlignment() {
		return horizontalAlignment;
	}

	/**
	 * Set the text
	 * @param newText, may be null
	 */
	public void setText(String newText) {
		if (newText != text) {
			text = newText;
			changed = true;
		}
	}

	/**
	 * @return the RAW text (with all its formatting); may be null
	 */
	public String getText() {
		return text;
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	public void setFactory(StyledTextFactory factory) {
		if (this.factory != factory) {
			this.factory = factory;
			changed = true;
		}
	}

	public StyledTextFactory getFactory() {
		return factory;
	}

	public void setJustified(boolean justified) {
		if (this.justified != justified) {
			this.justified = justified;
			changed = true;
		}
	}

	public boolean isJustified() {
		return justified;
	}

	/**
	 * Force layout or not layout
	 * @param changed
	 */
	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	// chaz hack!

	public int getNumLines() {
		layout();
		return lines.size();
	}

	public void setColor(ReadableColor color) {
		this.color = color;
	}

	public ReadableColor getColor() {
		return color;
	}
}
