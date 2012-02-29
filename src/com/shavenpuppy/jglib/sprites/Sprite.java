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
package com.shavenpuppy.jglib.sprites;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Stack;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;

import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.Util;

/**
 * A Sprite has associated with it an Animation and frame counter OR a single SpriteImage, and has a position
 * and offset.
 */
public class Sprite implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Appearance to use when sprite is accidentally imageless */
	private static SpriteImage missingImage;

	/** Index of sprite into the sprite engine pool */
	int index;

	/** The sprite engine */
	private final SpriteEngine engine;

	/** The sprite allocator (used for serialization purposes - make sure this is SMALL! */
	private SpriteAllocator allocator;

	/** The sprite's current owner - used for debugging more than anything else */
	private Serializable owner;

	/** Are we allocated? */
	private boolean allocated;

	/** The Animation if any */
	private Animation animation;

	/** Framelist, if any */
	private ResourceArray frameList;

	/** Index into framelist */
	private int frame;

	/** Loop counter */
	private int loop;

	/** Current frame tick */
	private int tick;

	/** Animation sequence */
	private int sequence;

	/** The current "event" state */
	private int event;

	/** Pause animation */
	private boolean paused;

	/** Current child x and y offsets */
	private float childXOffset, childYOffset;

	/** The current image */
	private SpriteImage image;

	/** Current style */
	private Style style;

	/** Layer */
	private int layer;

	/** Sublayer */
	private int subLayer;

	/** World coordinates */
	private float x, y;

	/** Offset coordinates */
	private float ox, oy;

	/** Y Sort offset */
	private float ySortOffset;

	/** Flash mode: renders an additive mode version on top */
	private boolean flash;

	/** Colours of the four corners (bottom left, bottom right, top right, top left) */
	private final ReadableColor[] color = new ReadableColor[]
	    {
			Color.WHITE,
			Color.WHITE,
			Color.WHITE,
			Color.WHITE
		};

	/** Handy constant */
	private static final int NO_SCALE = FPMath.fpValue(1);

	/** Sprite scale */
	private int xscale = NO_SCALE, yscale = NO_SCALE;

	/** Sprite alpha 0..255 */
	private int alpha = 255;

	/** Rotation angle */
	private int angle = 0;

	/** Sprite visibility */
	private boolean visible = true;

	/** Sprite active flag */
	private boolean active = true;

	/** Mirrored */
	private boolean mirrored;

	/** Flipped */
	private boolean flipped;

	/** Offset by childXOffset/childYOffset*/
	private boolean doChildOffset;

	/** Stack entries */
	static class StackEntry implements Serializable {
		private static final long serialVersionUID = 1L;

		final Animation animation;
		final int sequence;

		StackEntry(Animation animation, int sequence) {
			this.animation = animation;
			this.sequence = sequence;
		}
	}

	/** Stack */
	private Stack<StackEntry> stack;

	/**
	 * Constructor for Sprite.
	 */
	Sprite(SpriteEngine engine) {
		super();

		this.engine = engine;
	}

	/**
	 * @return the Sprite Engine
	 */
	public SpriteEngine getEngine() {
		return engine;
	}

	public void setAllocator(SpriteAllocator allocator) {
	    this.allocator = allocator;
    }

	public SpriteAllocator getAllocator() {
	    return allocator;
    }

	/**
	 * Copy a source sprite. Only information required for rendering is copied.
	 * @param src
	 */
	void copy(Sprite src) {
		image = src.image;
		style = src.style;
		layer = src.layer;
		subLayer = src.subLayer;
		x = src.x;
		y = src.y;
		ox = src.ox;
		oy = src.oy;
		ySortOffset = src.ySortOffset;
		flash = src.flash;
		for (int i = 0; i < 4; i ++) {
			color[i] = src.color[i];
		}
		xscale = src.xscale;
		yscale = src.yscale;
		alpha = src.alpha;
		angle = src.angle;
		visible = src.visible;
		active = src.active;
		mirrored = src.mirrored;
		flipped = src.flipped;
		doChildOffset = src.doChildOffset;
	}

	/**
	 * Init this sprite back to construction values
	 * @param newOwner The sprite's new owner
	 */
	public void init(Serializable newOwner) {
		if (allocated) {
			throw new IllegalStateException(this + "Already allocated: "+newOwner+" can't have it!");
		}
		allocated = true;
		reset();
		owner = newOwner;
		active = true;
		visible = true;
		image = null;
		flash = false;
		ox = 0;
		oy = 0;
		x = 0;
		y = 0;
		color[0] = ReadableColor.WHITE;
		color[1] = ReadableColor.WHITE;
		color[2] = ReadableColor.WHITE;
		color[3] = ReadableColor.WHITE;
		layer = 0;
		subLayer = 0;
		xscale = NO_SCALE;
		yscale = NO_SCALE;
		alpha = 255;
		angle = 0;
		mirrored = false;
		flipped = false;
		doChildOffset = false;
		style = null;
	}

	/**
	 * @return the owner
	 */
	public Serializable getOwner() {
		return owner;
	}

	/**
	 * @return the x coordinate
	 */

	public float getX() {
		return x;
	}

	/**
	 * Sets the x coordinate.
	 * @param x The x to set
	 */

	public void setX(float x) {
		this.x = x;
	}

	/**
	 * Gets the y coordinate.
	 * @return the y coordinate
	 */

	public float getY() {
		return y;
	}

	/**
	 * Sets the y.
	 * @param y The y to set
	 */

	public void setY(float y) {
		this.y = y;
	}

	/**
	 * Convenience accessor
	 */
	public Vector2f getLocation(Vector2f ret) {
		if (ret == null) {
			ret = new Vector2f(x, y);
		} else {
			ret.set(x, y);
		}
		return ret;
	}

	/**
	 * Convenience accessor
	 */

	public void setLocation(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void setLocation(ReadableVector2f newLocation) {
		this.x = newLocation.getX();
		this.y = newLocation.getY();
	}

	/**
	 * Convenience accessor
	 */

	public Vector2f getOffset(Vector2f ret) {
		if (ret == null) {
			ret = new Vector2f(ox, oy);
		} else {
			ret.set(ox, oy);
		}
		return ret;
	}

	/**
	 * Convenience accessor
	 */

	public void setOffset(float ox, float oy) {
		this.ox = ox;
		this.oy = oy;
	}

	/**
	 * Convenience accessor
	 */

	public void setOffset(ReadableVector2f location) {
		this.ox = location.getX();
		this.oy = location.getY();
	}

	/**
	 * Frame ticker. Call this every frame.
	 */

	public void tick() {
		// If the sprite is not active, don't do anything.
		// If there's no animation, we don't need to do anything.
		// If we're paused we don't need to do anything.
		if (!active || animation == null || isPaused()) {
			return;
		}

		animation.animate(this);
	}

	/**
	 * Set flash mode on or off. This renders the sprite more brightly.
	 * @param flash
	 */

	public void setFlash(boolean flash) {
		this.flash = flash;
	}

	/**
	 * Is the sprite visible
	 * @return true if the sprite is visible
	 */

	public boolean isVisible() {
		return visible;
	}

	/**
	 * Sets whether the sprite is visible or not
	 * @param visible Sprite visibility
	 */

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * Gets the alpha transparency of the whole sprite.
	 * @return int between 0 and 255
	 */

	public int getAlpha() {
		return alpha;
	}

	/**
	 * Sets the alpha transparency of the whole sprite.
	 * @param alpha The alpha to set, which should be between 0 and 255
	 */

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	/**
	 * Gets the scale. Scale is 16:16 fixed point.
	 * @return Returns a float
	 */

	public int getXScale() {
		return xscale;
	}

	/**
	 * Gets the scale. Scale is 16:16 fixed point.
	 * @return Returns a float
	 */

	public int getYScale() {
		return yscale;
	}

	/**
	 * Sets the scale. Scale is specified in 8-bit fixed point.
	 * @param scale The scale to set
	 */
	public void setScale(int scale) {
		this.xscale = scale;
		this.yscale = scale;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#setScale(int, int)
	 */

	public void setScale(int xscale, int yscale) {
		this.xscale = xscale;
		this.yscale = yscale;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Scaled#adjustScale(int, int)
	 */

	public void adjustScale(int xscale, int yscale) {
		this.xscale += xscale;
		this.yscale += yscale;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Transparent#adjustAlpha(int)
	 */

	public void adjustAlpha(int delta) {
		alpha = Math.max(0, Math.min(255, alpha + delta));
	}

	/**
	 * Determines whether the sprite is active. If it isn't active is is simply
	 * not processed at all by the sprite renderer.
	 * @return true if the sprite is active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Sets whether the sprite is active.
	 * @param active The active to set
	 * @see isActive()
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animation.Animated#hide()
	 */
	public void hide() {
		setVisible(false);
	}

	/**
	 * Sets the value of one of the sprite's corner colors, used to
	 * attenuate it when it is drawn.
	 * @param index The color index (0..3: BL, BR, TR, TL)
	 * @param r The new red value
	 * @param g The new green value
	 * @param b The new blue value
	 * @param a The new alpha value
	 */

	public void setColor(int index, ReadableColor color) {
		this.color[index] = color;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#setColors(org.lwjgl.util.ReadableColor)
	 */

	public void setColors(ReadableColor src) {
		color[0] = src;
		color[1] = src;
		color[2] = src;
		color[3] = src;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#setAngle(int)
	 */

	public void setAngle(int angle) {
		this.angle = angle % 0xFFFF;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#getAngle()
	 */

	public int getAngle() {
		return angle;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#adjustAngle(int)
	 */

	public void adjustAngle(int delta) {
		angle = (angle + delta) % 0xFFFF;
	}

	/**
	 * Returns the color of one of the sprite's corners.
	 * @param index The color index (0..3: BL, BR, TR, TL)
	 * @param color The color to store the values in, or null to create a new one
	 * @return a color
	 */

	public ReadableColor getColor(int index) {
		return color[index];
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#setCurrentImage(com.shavenpuppy.jglib.sprites.SpriteImage)
	 */

	public void setImage(SpriteImage image) {
		this.image = image;
		if (image == null) {
			return;
		}
		assert image.isCreated() : "Image "+image+" not created!";
		assert image.getStyle() != null : "Image "+image+" has no style!";
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#deactivate()
	 */

	public void deactivate() {
		setActive(false);
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#getCurrentImage()
	 */

	public SpriteImage getImage() {
		return image;//image == null ? missingImage : image;
	}

	/**
	 * Set the override style
	 * @param style
	 */
	public void setStyle(Style style) {
		this.style = style;
	}

	/**
	 * @return the override style
	 */
	public Style getStyle() {
		return style != null ? style : getImage() != null ? getImage().getStyle() : null;
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#moveLocation(int, int)
	 */
	public void moveLocation(int dx, int dy) {
		x += dx;
		y += dy;
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#moveOffset(int, int)
	 */
	public void moveOffset(int dx, int dy) {
		ox += dx;
		oy += dy;
	}

	/**
	 * Returns the layer.
	 * @return int
	 */

	public int getLayer() {
		return layer;
	}

	/**
	 * Sets the layer.
	 * @param layer The layer to set
	 */

	public void setLayer(int layer) {
		this.layer = layer;
	}

	/**
	 * Deallocate this sprite and return it to the sprite engine
	 */

	public void deallocate() {
		if (!allocated) {
			assert false;
			return;
		}
		setStyle(null);
		allocated = false;
		engine.deallocate(this);
		owner = null;
	}

	/** This is intended only for use by the containing SpriteEngine as a way to do fast .clear operations. */
	void forceDeallocate()
	{
		assert allocated;

		allocated = false;
		visible = false;
	}


	@Override
    public String toString() {
		return "Sprite[idx="+index+", owner="+owner+", image="+image+", active="+active+", visible="+visible+", position="+x+","+y+", "+getAnimation()+", "+getStyle()+"]";
	}

	/**
	 * @return true if the sprite is "flashing"
	 */

	public boolean isFlashing() {
		return flash;
	}

	/**
	 * Sets the sprite's appearance.
	 * @param appearance
	 */
	public void setAppearance(Appearance appearance) {
		if (appearance == null) {
			image = null;
			setAnimation(null);
			rewind();
		} else{
			appearance.toSprite(this);
		}
	}

	public boolean isAllocated() {
		return allocated;
	}

	/**
	 * @return Returns the flipped.
	 */

	public boolean isFlipped() {
		return flipped;
	}

	/**
	 * @param flipped The flipped to set.
	 */

	public void setFlipped(boolean flipped) {
		this.flipped = flipped;
	}

	/**
	 * @return Returns the mirrored.
	 */

	public boolean isMirrored() {
		return mirrored;
	}

	/**
	 * @param mirrored The mirrored to set.
	 */

	public void setMirrored(boolean mirrored) {
		this.mirrored = mirrored;
	}

	/**
	 * @return Returns the doChildOffset.
	 */
	public boolean isDoChildOffset() {
		return doChildOffset;
	}

	/**
	 * @param doChildOffset The doChildOffset to set.
	 */
	public void setDoChildOffset(boolean doChildOffset) {
		this.doChildOffset = doChildOffset;
	}

	/**
	 * Rotates a sprite towards a particular direction (specified in relative
	 * coordinates)
	 * @param dirX
	 * @param dirY
	 */
	public void rotateToTarget(float dirX, float dirY) {
		setAngle(FPMath.fpYaklyDegrees( Util.angleFromDirection(dirX, dirY)));
	}

	/**
	 * Serialization support. We completely replace the serialized sprite with an
	 * instance of SerializedSprite instead.
	 */
    private Object writeReplace() throws ObjectStreamException {
		SerializedSprite ss = new SerializedSprite();
		ss.fromSprite(this);
		return ss;
    }

    /**
	 * @param missingImage The missingImage to set.
	 */
	public static void setMissingImage(SpriteImage missingImage) {
		Sprite.missingImage = missingImage;
	}

	/**
	 * @return Returns the missingImage.
	 */
	public static SpriteImage getMissingImage() {
		return missingImage;
	}

	float getOffsetY() {
		return oy;
	}

	/**
	 * @param sortOffset the ySortOffset to set
	 */
	public void setYSortOffset(float sortOffset) {
		ySortOffset = sortOffset;
	}

	/**
	 * @return the ySortOffset
	 */
	public float getYSortOffset() {
		return ySortOffset;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Layered#getSubLayer()
	 */

	public int getSubLayer() {
		return subLayer;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Layered#setSubLayer(int)
	 */

	public void setSubLayer(int newSubLayer) {
		this.subLayer = newSubLayer;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#pushSequence()
	 */

	public void pushSequence() {
		if (stack == null) {
			stack = new Stack<StackEntry>();
		}
		if (stack.size() > 10) {
			System.err.println("Stack overflow "+this+"/"+getAnimation());
			return;
		}
//		System.out.println(this+" pushed "+getAnimation()+" / "+(getSequence()+1));
		stack.push(new StackEntry(getAnimation(), getSequence() + 1));
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#popSequence()
	 */

	public void popSequence() {
		if (stack == null) {
			return;
		}
		if (stack.size() == 0) {
			System.err.println("Stack underflow "+this+"/"+getAnimation());
			return;
		}
		StackEntry se = stack.pop();
		setAnimationNoRewind(se.animation);
		setSequence(se.sequence);
	}

	/**
	 * Accessor for {@link SerializedSprite}
	 * @return the stack
	 */
	Stack<StackEntry> getStack() {
		return stack;
	}

	/**
	 * Accessor for {@link SerializedSprite}
	 * @param stack
	 */
	void setStack(Stack<StackEntry> stack) {
		this.stack = stack;
	}


	public void reset() {
		animation = null;
		frameList = null;
		sequence = 0;
		frame = 0;
		tick = 0;
		event = 0;
		paused = false;
		childXOffset = 0;
		childYOffset = 0;
	}

	/**
	 * Gets the animation.
	 * @return Returns a Animation
	 */
	public Animation getAnimation() {
		return animation;
	}

	/**
	 * Sets the animation.
	 * @param animation The animation to set
	 */
	public void setAnimation(Animation animation) {
		if (animation != null) {
			assert animation.isCreated();
		}
		this.animation = animation;
		rewind();
	}

	/**
	 * Sets the animation, without rewinding
	 * @param animation
	 */
	void setAnimationNoRewind(Animation animation) {
		this.animation = animation;
		tick = 0;
	}

	/**
	 * Rewind the animation, if any. The first sequence of the animation, if any, is executed immediately.
	 */
	public void rewind() {
		sequence = 0;
		tick = 0;
		tick();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animation.Animated#getCurrentSequence()
	 */
	public int getSequence() {
		return sequence;
	}
	/**
	 * @see com.shavenpuppy.jglib.sprites.Animation.Animated#getCurrentTick()
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animation.Animated#eventReceived(int)
	 */

	public void eventReceived(int event) {
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#setCurrentSequence(int)
	 */

	public void setSequence(int newSeq) {
		sequence = newSeq;
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#setCurrentTick(int)
	 */

	public void setTick(int newTick) {
		tick = newTick;
	}

	/**
	 * @return int
	 */

	public int getEvent() {
		return event;
	}

	/**
	 * Sets the event.
	 * @param event The event to set
	 */

	public void setEvent(int event) {
		this.event = event;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#isPaused()
	 */

	public final boolean isPaused() {
		return paused;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#setPaused(boolean)
	 */

	public final void setPaused(boolean paused) {
		this.paused = paused;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#addLoop(int)
	 */

	public final void addLoop(int d) {
		loop += d;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#getLoop()
	 */

	public final int getLoop() {
		return loop;
	}


	public final void setLoop(int i) {
		loop = i;
	}


	public float getChildXOffset(){
		return childXOffset;
	}


	public void setChildXOffset(float childXOffset) {
		this.childXOffset = childXOffset;
	}


	public float getChildYOffset(){
		return childYOffset;
	}


	public void setChildYOffset(float childYOffset) {
		this.childYOffset = childYOffset;
	}


	public void setFrameList(ResourceArray frameList) {
		this.frameList = frameList;
		if (frameList != null) {
			updateFrame();
		}
	}


	public ResourceArray getFrameList() {
		return frameList;
	}

	/**
	 * @return the frame
	 */

	public int getFrame() {
		return frame;
	}

	/**
	 * @param frame the frame to set
	 */

	public boolean setFrame(int frame) {
		this.frame = frame;
		if (frameList != null) {
			return updateFrame();
		} else {
			return false;
		}
	}

	private boolean updateFrame() {
		if (frame < 0 || frame >= frameList.getNumResources()) {
			return false;
		}
		Appearance newAppearance = (Appearance) frameList.getResource(frame);
		if ((animation != null && newAppearance != animation) || animation == null) {
			return newAppearance.toSprite(this);
		} else {
			return false;
		}

	}
}
