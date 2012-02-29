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
package net.puppygames.applet.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.puppygames.applet.Anchor;
import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;

import org.lwjgl.util.Point;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * $Id: PowerDisplayFeature.java,v 1.5 2010/08/03 23:43:39 foo Exp $
 * Displays coloured bars indicating the level of a powerup.
 * @author $Author: foo $
 * @version $Revision: 1.5 $
 */
public class PowerDisplayFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/*
	 * Static stuff
	 */

	/** Bounds (used for determining mouse clicks) */
	private Rectangle bounds = new Rectangle(0, 0, 0, 0);

	/** Default gap between bars where not specified */
	private int gap;

	/** Location */
	private Point location = new Point(0, 0);

	/** Layer */
	private int layer;

	/** Bars */
	private List<BarFeature> bars;

	/** Which direction the bars go */
	private int dx, dy;

	/** Scale of bar Sprite image */
	private float scale = 1.0f;

	/** Anchors */
	private ArrayList<Anchor> anchors;

	/**
	 * Bars
	 */
	private static class BarFeature extends Feature {

		private static final long serialVersionUID = 1L;

		private String on, off;

		private transient Appearance onResource, offResource;

		private Screen screen;

		private class BarInstance {
			Sprite sprite;
			PowerDisplay display;

			void init(PowerDisplay display, SpriteAllocator screen) {
				this.display = display;
				sprite = screen.allocateSprite(BarFeature.this);
				if (sprite == null) {
					throw new RuntimeException("Not enough sprites to create power display");
				}
				sprite.setLayer(display.getFeature().layer);
				if (offResource == null) {
					sprite.setAppearance(onResource);
					sprite.setVisible(false);
				}
				if (display.getFeature().scale != 1.0f) {
					sprite.setScale(FPMath.fpValue(display.getFeature().scale));
				}
			}

			void cleanup() {
				if (sprite != null) {
					sprite.deallocate();
					sprite = null;
				}
			}

			void setUsed(boolean used) {
				if (used) {
					sprite.setAppearance(onResource);
					sprite.setVisible(display.isVisible());
				} else if (offResource == null) {
					sprite.setVisible(false);
				} else {
					sprite.setAppearance(offResource);
					sprite.setVisible(display.isVisible());
				}
			}

			void setLocation(float x, float y) {
				sprite.setLocation(x, y);
			}
		}

		/**
		 * C'tor
		 */
		private BarFeature() {
			setAutoCreated();
			setSubResource(true);
		}

		/**
		 * @param screen
		 * @return
		 */
		BarInstance init(PowerDisplay display, SpriteAllocator screen) {
			BarInstance ret = new BarInstance();
			ret.init(display, screen);
			return ret;
		}
	}

	/**
	 * C'tor
	 */
	public PowerDisplayFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * C'tor
	 */
	public PowerDisplayFeature() {
		setAutoCreated();
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> barElements = XMLUtil.getChildren(element, "bar");
		bars = new ArrayList<BarFeature>(barElements.size());
		for (Element barElement : barElements) {
			BarFeature bf = new BarFeature();
			bf.load(barElement, loader);
			bars.add(bf);
		}

		// Anchors
		List<Element> anchorElements = XMLUtil.getChildren(element, "anchor");
		if (anchorElements.size() > 0) {
			anchors = new ArrayList<Anchor>(anchorElements.size());
			for (Element anchorChild : anchorElements) {
				Anchor anchor = (Anchor) loader.load(anchorChild);
				anchors.add(anchor);
			}
		}

	}

	@Override
	protected void doCreate() {
		super.doCreate();
		for (Iterator<BarFeature> i = bars.iterator(); i.hasNext(); ) {
			BarFeature bar = i.next();
			bar.create();
		}
	}

	@Override
	protected void doDestroy() {
		super.doDestroy();
		for (Iterator<BarFeature> i = bars.iterator(); i.hasNext(); ) {
			BarFeature bar = i.next();
			bar.destroy();
		}
	}

	/**
	 * Create a power display instance
	 * @param screen
	 */
	public PowerDisplay spawn(final Screen screen) {
		PowerDisplay ret = new PowerDisplay() {

			/** My bounds */
			Rectangle instance_bounds = new Rectangle(PowerDisplayFeature.this.bounds);

			/** The bars */
			BarFeature.BarInstance[] bar;

			/** Location */
			float x, y;

			/** Used bars */
			int used = -1;

			/** Visibility */
			boolean visible = true;

			@Override
			public PowerDisplayFeature getFeature() {
				return PowerDisplayFeature.this;
			}

			@Override
			public boolean isVisible() {
				return visible;
			}

			@Override
			public void setVisible(boolean visible) {
				if (this.visible == visible) {
					return;
				}
				this.visible = visible;
				doSetUsed();
			}

			@Override
			public void setUsed(int used) {
				if (this.used == used) {
					return;
				}
				this.used = used;
				doSetUsed();
			}
			private void doSetUsed() {
				for (int i = 0; i < Math.min(bar.length, used); i ++) {
					bar[i].setUsed(true);
				}
				for (int i = Math.max(0, used); i < bar.length; i ++) {
					bar[i].setUsed(false);
				}
			}

			@Override
			public ReadableRectangle getBounds() {
				return new Rectangle((int) x, (int) y, 0, 0);
			}

			@Override
			public void setBounds(int x, int y, int w, int h) {
				setLocation(x, y);
			}

			@Override
			public void setLocation(float x, float y) {
				this.x = x;
				this.y = y;
				instance_bounds.setLocation((int) x, (int) y);
				for (int i = 0; i < bar.length; i ++) {
					bar[i].setLocation(x, y);
					if (gap == 0) {
						x += dx;
						y += dy;
					} else {
						x += gap;
					}
				}
			}

			@Override
			public ReadablePoint getBarLocation(int idx) {
				if (idx >= bar.length || idx < 1) {
					return null;
				}
				return new Point((int) bar[idx - 1].sprite.getX(), (int) bar[idx - 1].sprite.getY());
			}

			@Override
			public void setAlpha(int alpha) {
				for (int i = 0; i < bar.length; i ++) {
					bar[i].sprite.setAlpha(alpha);
				}
			}

			@Override
			public Sprite getSprite(int idx) {
				return bar[idx].sprite;
			}

			@Override
			public int getBarAt(int xx, int yy) {
				if (!instance_bounds.contains(xx, yy)) {
					return -1;
				}
				return (getMax() * (xx - instance_bounds.getX())) / (instance_bounds.getWidth());
			}

			@Override
			public void init(SpriteAllocator screen) {
				bar = new BarFeature.BarInstance[bars.size()];
				for (int i = 0; i < bar.length; i ++) {
					bar[i] = bars.get(i).init(this, screen);
				}
			}

			@Override
			public void cleanup() {
				if (bar == null) {
					return;
				}
				for (int i = 0; i < bar.length; i ++) {
					bar[i].cleanup();
				}
				bar = null;
			}

			@Override
			public int getUsed() {
				return used;
			}

			@Override
			public int getMax() {
				return bars.size();
			}

			@Override
			public void onResized() {
				// Apply anchors unless screen is "centred"
				if (anchors != null) {
					for (Iterator<Anchor> i = anchors.iterator(); i.hasNext(); ) {
						Anchor anchor = i.next();
						anchor.apply(this);
					}
				} else if (location != null) {
					boolean centreX = screen.isCentred() || screen.isCentredX();
					boolean centreY = screen.isCentred() || screen.isCentredY();
					if (centreX || centreY) {
						int newX = (centreX ? (Game.getWidth() - Game.getScale()) / 2 + location.getX() : location.getX());
						int newY = (centreY ? (Game.getHeight() - Game.getScale()) / 2 + location.getY() : location.getY());
						setLocation(newX, newY);
					}
				}
			}

		};
		ret.init(screen);
		ret.setLocation(location.getX(), location.getY());
		ret.setUsed(0);
		return ret;
	}

	/**
	 * Get the total bars
	 * @return int
	 */
	public int getTotalBars() {
		return bars.size();
	}



}
