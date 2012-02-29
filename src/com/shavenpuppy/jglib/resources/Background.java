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

import java.io.Serializable;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.lwjgl.util.WritableDimension;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLBaseTexture;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.sprites.AlphaOp;
import com.shavenpuppy.jglib.sprites.SimpleRenderable;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;
import com.shavenpuppy.jglib.sprites.SpriteImage;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: Background.java,v 1.32 2011/08/02 16:10:36 cix_foo Exp $
 * <p>
 * A Background is a texture split up into 9 areas. The corner areas
 * are absolute in size; the edges and middle are tiled to fit the background.
 * @author $Author: cix_foo $
 * @version $Revision: 1.32 $
 */
public class Background extends Feature {

	private static final long serialVersionUID = 1L;

	private static final GLRenderable SETUP_BLEND = new GLRenderable() {
		@Override
		public void render() {
			glEnable(GL_TEXTURE_2D);
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		}
	};

	private static final GLRenderable SETUP_OPAQUE = new GLRenderable() {
		@Override
		public void render() {
			glEnable(GL_TEXTURE_2D);
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ZERO);
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		}
	};

	public static enum Blend {
		TRANSPARENT(SETUP_BLEND, AlphaOp.PREMULTIPLY),
		GLOWING(SETUP_BLEND, AlphaOp.ZERO),
		OPAQUE(SETUP_OPAQUE, AlphaOp.KEEP);

		final GLRenderable op;
		final AlphaOp alphaOp;

		Blend(GLRenderable op, AlphaOp alphaOp) {
			this.op = op;
			this.alphaOp = alphaOp;
		}
	}

	private static final Color TEMP = new Color();

	/*
	 * Resource data
	 */

	/** Corners */
	private String top_left, top_right, bottom_left, bottom_right;

	/** Edges */
	private String top, left, bottom, right;

	/** Middle */
	private String middle;

	/** Color */
	private MappedColor color, topColor, bottomColor, leftColor, rightColor, topLeftColor, bottomLeftColor, topRightColor, bottomRightColor;

	/** Insets */
	private Rectangle insets;

	/** Default blending */
	private Blend blend = Blend.TRANSPARENT;

	/*
	 * Transient data
	 */

	private transient SpriteImage top_left_image, top_right_image, bottom_left_image,
		bottom_right_image, top_image, left_image, bottom_image, right_image, middle_image;

	/**
	 * Instances of the Background
	 */
	public class Instance implements SimpleRenderable, Serializable {

		private static final long serialVersionUID = 1L;

		/** Temp stuff */
		private transient GLBaseTexture current;

		private ReadableRectangle bounds;
		private ReadableColor instanceColor = Color.WHITE;
		private final Color blendColor = new Color();
		private SimpleRenderer renderer;
		private int alpha = 255;
		private Blend blend = Background.this.blend;

		private Instance() {
		}

		/**
		 * Sets the blend mode. Defaults to TRANSPARENT.
		 * @param blend
		 */
		public void setBlend(Blend blend) {
	        this.blend = blend;
        }

		@Override
		public void render(SimpleRenderer renderer) {
			renderer.glRender(blend.op);
			renderBackground(renderer);
		}

		public void renderBackground(SimpleRenderer renderer) {
			this.renderer = renderer;
			current = null;
			drawBottomLeft();
			drawBottom();
			drawLeft();
			drawTopLeft();
			drawBottomRight();
			drawMiddle();
			drawTop();
			drawRight();
			drawTopRight();
			current = null;
			renderer = null;
		}

		private int getW() {
			return bounds.getWidth() + insets.getWidth() + insets.getX();
		}
		private int getH() {
			return bounds.getHeight() + insets.getHeight() + insets.getY();
		}
		private void drawBottomLeft() {
			int w = Math.min(getW(), bottom_left_image.getWidth());
			int h = Math.min(getH(), bottom_left_image.getHeight());
			drawImage(bottom_left_image, 0, 0, w, h);
		}
		private void drawBottom() {
			int w = getW() - (bottom_left_image.getWidth() + bottom_right_image.getWidth());
			int h = Math.min(bottom_image.getHeight(), getH());
			drawImage(bottom_image, bottom_left_image.getWidth(), 0, w, h);
		}
		private void drawLeft() {
			int w = Math.min(left_image.getWidth(), getW());
			int h = getH() - (bottom_left_image.getHeight() + top_left_image.getHeight());
			drawImage(left_image, 0, left_image.getY(), w, h);
		}
		private void drawTopLeft() {
			int y = Math.max(bottom_left_image.getHeight(), getH() - top_left_image.getHeight());
			int w = Math.min(getW(), top_left_image.getWidth());
			int h = Math.min(getH() - y, top_left_image.getHeight());
			drawImage(top_left_image, 0, y, w, h);
		}
		private void drawBottomRight() {
			int x = Math.max(bottom_left_image.getWidth(), getW() - bottom_right_image.getWidth());
			int w = Math.min(getW() - x, bottom_right_image.getWidth());
			int h = Math.min(getH(), bottom_right_image.getHeight());
			drawImage(bottom_right_image, x, 0, w, h);
		}
		private void drawMiddle() {
			int w = getW() - (left_image.getWidth() + right_image.getWidth());
			int h = getH() - (bottom_image.getHeight() + top_image.getHeight());
			drawImage(middle_image, middle_image.getX(), middle_image.getY(), w, h);
		}
		private void drawTop() {
			int y = Math.max(bottom_image.getHeight(), getH() - top_image.getHeight());
			int w = getW() - (top_left_image.getWidth() + top_right_image.getWidth());
			int h = Math.min(top_image.getHeight(), getH());
			drawImage(top_image, top_left_image.getWidth(), y, w, h);
		}
		private void drawRight() {
			int x = Math.max(left_image.getWidth(), getW() - right_image.getWidth());
			int w = Math.min(right_image.getWidth(), getW());
			int h = getH() - (bottom_right_image.getHeight() + top_right_image.getHeight());
			drawImage(right_image, x, bottom_right_image.getHeight(), w, h);
		}
		private void drawTopRight() {
			int x = Math.min(getW() - top_right_image.getWidth(), getW());
			int y = Math.min(getH() - top_right_image.getHeight(), getH());
			int w = getW() - x;
			int h = getH() - y;
			drawImage(top_right_image, x, y, w, h);
		}
		private void drawImage(SpriteImage image, int x, int y, int w, int h) {
			if (w < 1 || h < 1) {
				return;
			}
			blit(image, x, y, w, h);
		}
		private void calcColorAtPoint(int x, int y) {
			float wid = getWidth();
			float hgt = getHeight();
			float fx = Math.min(wid, Math.max(0.0f, x - getRightInset()));
			float fy = Math.min(hgt, Math.max(0.0f, y - getTopInset()));

			double bottomLeftArea = fx * fy;
			double bottomRightArea = (wid - fx) * fy;
			double topLeftArea = fx * (hgt - fy);
			double topRightArea = (wid - fx) * (hgt - fy);
			double totalArea = wid * hgt;

			double bottomLeftRatio = bottomLeftArea / totalArea;
			double topLeftRatio = topLeftArea / totalArea;
			double bottomRightRatio = bottomRightArea / totalArea;
			double topRightRatio = topRightArea / totalArea;

			blendColor.set
				(
					(int) (bottomLeftColor.getRed() * topRightRatio + topLeftColor.getRed() * bottomRightRatio + bottomRightColor.getRed() * topLeftRatio + topRightColor.getRed() * bottomLeftRatio),
					(int) (bottomLeftColor.getGreen() * topRightRatio + topLeftColor.getGreen() * bottomRightRatio + bottomRightColor.getGreen() * topLeftRatio + topRightColor.getGreen() * bottomLeftRatio),
					(int) (bottomLeftColor.getBlue() * topRightRatio + topLeftColor.getBlue() * bottomRightRatio + bottomRightColor.getBlue() * topLeftRatio + topRightColor.getBlue() * bottomLeftRatio),
					(int) (bottomLeftColor.getAlpha() * topRightRatio + topLeftColor.getAlpha() * bottomRightRatio + bottomRightColor.getAlpha() * topLeftRatio + topRightColor.getAlpha() * bottomLeftRatio)
				);

			ColorUtil.blendColor(blendColor, instanceColor, TEMP);
			blend.alphaOp.op(TEMP, alpha, renderer);

//			float alpha00 = (blendColor.getAlpha() * instanceColor.getAlpha() * alpha) / 65025;
//			float preMultAlpha = preMult ? alpha00 / 255.0f : 1.0f;
//			float actualAlpha = preMult ? 0.0f : alpha00;
//			renderer.glColor4ub
//				(
//					(byte) (preMultAlpha * blendColor.getRed() * instanceColor.getRed() / 255),
//					(byte) (preMultAlpha * blendColor.getGreen() * instanceColor.getGreen() / 255),
//					(byte) (preMultAlpha * blendColor.getBlue() * instanceColor.getBlue() / 255),
//					(byte) (actualAlpha)
//				);

		}
		private void blit(final SpriteImage image, int x, int y, int w, int h) {
			if (current != image.getTexture()) {
				current = image.getTexture();
				renderer.glRender(current);
			}

			calcColorAtPoint(x, y);
			renderer.glTexCoord2f(image.getTx0(), image.getTy0());
			short idx = renderer.glVertex2f(x - insets.getX() + bounds.getX(), y - insets.getY() + bounds.getY());

			float tx1 = image.getTx1();
			calcColorAtPoint(x + w, y);
			renderer.glTexCoord2f(tx1, image.getTy0());
			renderer.glVertex2f(x + w - insets.getX() + bounds.getX(), y - insets.getY() + bounds.getY());

			float ty1 = image.getTy1();
			calcColorAtPoint(x + w, y + h);
			renderer.glTexCoord2f(tx1, ty1);
			renderer.glVertex2f(x + w - insets.getX() + bounds.getX(), y + h - insets.getY() + bounds.getY());

			calcColorAtPoint(x, y + h);
			renderer.glTexCoord2f(image.getTx0(), ty1);
			renderer.glVertex2f(x - insets.getX() + bounds.getX(), y + h - insets.getY() + bounds.getY());

			renderer.glRender(GL_TRIANGLE_FAN, new short[] {idx, (short) (idx + 1), (short) (idx + 2), (short) (idx + 3)});

		}

		public int getHeight() {
			return bounds.getHeight() + insets.getHeight() + insets.getY();
		}

		public void getSize(WritableDimension dest) {
			dest.setSize(bounds.getWidth() - insets.getWidth() - insets.getX(), bounds.getHeight() - insets.getHeight() - insets.getY());
		}

		public int getWidth() {
			return bounds.getWidth() - insets.getWidth() - insets.getX();
		}

		public void setBounds(ReadableRectangle bounds) {
			this.bounds = bounds;
		}

		public ReadableRectangle getBounds() {
			return bounds;
		}

		public void setColor(ReadableColor src) {
			instanceColor = src;
		}

		public void setAlpha(int alpha) {
			this.alpha = alpha;
		}

		public ReadableColor getColor() {
			return instanceColor;
		}

		public int getXInset() {
			return insets.getX();
		}
		public int getYInset() {
			return insets.getY();
		}
		public int getRightInset() {
			return insets.getWidth();
		}
		public int getTopInset() {
			return insets.getHeight();
		}
	}

	/**
	 * C'tor
	 */
	public Background() {
		super();
		setAutoCreated();
	}

	/**
	 * @param name
	 */
	public Background(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * Create an instance of the background
	 * @return a new Instance
	 */
	public Instance spawn() {
		return new Instance();
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		if (topColor == null) {
			topColor = color;
		}
		if (bottomColor == null) {
			bottomColor = color;
		}
		if (leftColor == null) {
			leftColor = color;
		}
		if (rightColor == null) {
			rightColor = color;
		}
		if (topLeftColor == null) {
			topLeftColor = topColor;
		}
		if (topLeftColor == null) {
			topLeftColor = leftColor;
		}
		if (topRightColor == null) {
			topRightColor = topColor;
		}
		if (topRightColor == null) {
			topRightColor = rightColor;
		}
		if (bottomLeftColor == null) {
			bottomLeftColor = bottomColor;
		}
		if (bottomLeftColor == null) {
			bottomLeftColor = leftColor;
		}
		if (bottomRightColor == null) {
			bottomRightColor = bottomColor;
		}
		if (bottomRightColor == null) {
			bottomRightColor = rightColor;
		}
		if (topLeftColor == null) {
			topLeftColor = new MappedColor(ReadableColor.WHITE);
		}
		if (bottomLeftColor == null) {
			bottomLeftColor = new MappedColor(ReadableColor.WHITE);
		}
		if (topRightColor == null) {
			topRightColor = new MappedColor(ReadableColor.WHITE);
		}
		if (bottomRightColor == null) {
			bottomRightColor = new MappedColor(ReadableColor.WHITE);
		}

		if (insets == null) {
			insets = new Rectangle();
		}
	}


}