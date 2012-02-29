/*
 * Copyright (c) 2004 Covalent Software Ltd
 * All rights reserved.
 */
package net.puppygames.applet.effects;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.WeakHashMap;

import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;

import org.lwjgl.util.ReadablePoint;

import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Attenuator;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;

/**
 * Special effects. Although special effects are {@link TickableObject TickableObjects}, and therefore derived from
 * {@link Serializable}, they're not meant to be serialized (legacy restriction).
 */
public abstract class Effect extends TickableObject {

	/** Default offsets on various screens */
	private static final WeakHashMap<SpriteAllocator, ReadablePoint> DEFAULT_OFFSETS = new WeakHashMap<SpriteAllocator, ReadablePoint>();

	/** Default attenuators on various screens */
	private static final WeakHashMap<SpriteAllocator, Attenuator> DEFAULT_ATTENUATORS = new WeakHashMap<SpriteAllocator, Attenuator>();

	/** Default layer for effects */
	private static final int DEFAULT_LAYER = 100;

	/** Delay before effect is ticked plus 1 tick */
	private int delay = 1;

	/** Paused */
	private boolean paused;

	/** Sound effect to play when the effect starts */
	private ALBuffer sound;

	/** Offset */
	private ReadablePoint offset;

	/** Sound attenuator */
	private Attenuator attenuator;

	/** Sound effect */
	private transient SoundEffect soundEffect;

	/**
	 * Sets the default offset to be used for foreground effects on a particular screen. This is effectively like calling {@link #setOffset(ReadablePoint)}
	 * on all your effects spawned on that screen.
	 * @param screen The screen to define; may not be null
	 * @param defaultOffset The default offset; may be null
	 */
	public static void setDefaultOffset(SpriteAllocator screen, ReadablePoint defaultOffset) {
		DEFAULT_OFFSETS.put(screen, defaultOffset);
	}

	/**
	 * Sets the default attenuator to be used for foreground effects on a particular screen. This is effectively like calling {@link #setAttenuator(Attenuator)}
	 * on all your effects spawned on that screen.
	 * @param screen The screen to define; may not be null
	 * @param defaultAttenuator the default attenuator to set; may be null
	 */
	public static void setDefaultAttenuator(SpriteAllocator screen, Attenuator defaultAttenuator) {
		DEFAULT_ATTENUATORS.put(screen, defaultAttenuator);
	}

	/**
	 * C'tor
	 */
	public Effect() {
		setLayer(getDefaultLayer());
	}

	/**
	 * Sets the offset for this effect. You don't need to do this if you've called {@link #setDefaultOffset(Screen, ReadablePoint)};
	 * or alternatively, you may want to override the default offset, and pass in some other offset (such as null)
	 * @param offset the offset to set, may be null
	 */
	public void setOffset(ReadablePoint offset) {
		this.offset = offset;
	}

	/**
	 * @return the offset
	 */
	public ReadablePoint getOffset() {
		return offset;
	}

	/**
	 * Sets the sound attenuator for this effect. You don't need to do this if you've called {@link #setDefaultAttenuator(Screen, Attenuator)};
	 * or alternatively, you may want to override the default attenuator, and pass in some other attenuator (such as null)
	 * @param attenuator the attenuator to set for sound volume; may be null
	 */
	public void setAttenuator(Attenuator attenuator) {
		this.attenuator = attenuator;
	}

	/**
	 * @return the attenuator
	 */
	public Attenuator getAttenuator() {
		return attenuator;
	}

	/**
	 * @return true if the effect has started
	 */
	public boolean isStarted() {
		return delay == 0;
	}

	/**
	 * Pause or unpause ticking.
	 * @param paused
	 */
	public final void setPaused(boolean paused) {
		if (this.paused != paused) {
			this.paused = paused;
			onPausedChanged();
		}
	}
	protected void onPausedChanged() {}

	/**
	 * @return true if we're paused
	 */
	public final boolean isPaused() {
		return paused;
	}

	/**
	 * Start the effect
	 */
	public void start() {
		delay = 0;
		if (sound != null) {
			playSound(sound);
		}
		init();
	}

	/**
	 * Play a sound for this effect when the effect starts. Override to provide attenuated sound or prevent sounds.
	 * @param sound The sound; may not be null
	 */
	protected void playSound(ALBuffer sound) {
		soundEffect = Game.allocateSound(sound, 1.0f, 1.0f, this);
	}

	@Override
    protected final void doSpawn() {
		if (!isBackgroundEffect()) {
			if (offset == null) {
				setOffset(DEFAULT_OFFSETS.get(getScreen()));
			}
			if (attenuator == null) {
				setAttenuator(DEFAULT_ATTENUATORS.get(getScreen()));
			}
		}
		doSpawnEffect();
	}

	protected void doSpawnEffect() {
	}

	/**
	 * Set the delay
	 * @param delay
	 */
	public final void setDelay(int delay) {
		this.delay = Math.max(1, delay + 1);
	}

	/**
	 * Tick.
	 */
	@Override
	public final void tick() {
		if (paused) {
			return;
		}
		if (delay > 0) {
			delay --;
			if (delay == 0) {
				start();
			}
		}
		if (delay == 0) {
			doTick();
		}
		if (!isEffectActive()) {
			remove();
		}
	}

	@Override
	public void update() {
		if (delay == 0) {
			doUpdate();
			if (soundEffect != null && attenuator != null) {
				updateSound(sound.getGain(), soundEffect);
			}
		}
	}

	/**
	 * Update the sound effect. Called if there is a sound effect and an attenuator, from {@link #update()}.
	 * @param gain The master gain for the sound
	 * @param effect The sound effect to update
	 */
	protected void updateSound(float gain, SoundEffect effect) {
	}

	/**
	 * Updates effect graphics
	 */
	protected void doUpdate() {
	}

	/**
	 * @param sound The sound to set.
	 */
	public final void setSound(ALBuffer sound) {
		this.sound = sound;
	}

	/**
	 * @return the sound
	 */
	public final ALBuffer getSound() {
		return sound;
	}

	/**
	 * Init. Called for delayed effects the moment they are ready.
	 */
	protected void init() {
	}

	/**
	 * Tick the effect
	 */
	protected abstract void doTick();

	/**
	 * "Finish" the effect. This lets the effect decide when to remove itself.
	 * The default is to remove the effect immediately.
	 */
	public void finish() {
		remove();
	}

	/**
	 * @return true if the effect is finished
	 */
	public boolean isFinished() {
		return !isActive();
	}

	/**
	 * @return true if the effect is active
	 */
	public boolean isEffectActive() {
		return isActive();
	}

	/**
	 * @return true if this is a background effect
	 */
	public boolean isBackgroundEffect() {
		return false;
	}

	/**
	 * Gets the default layer for effects of this type. Normally effects are drawn right on top of everything so we use
	 * {@value #DEFAULT_LAYER}. Override this for more reasonable layer values.
	 * <em>Note</em> this method is called from the constructor so it can't rely on any post-construction stuff
	 * @return integer
	 */
	public int getDefaultLayer() {
		return DEFAULT_LAYER;
	}

	/**
	 * Prevent serialization
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		throw new NotSerializableException(this+" is not serializable");
	}

	/**
	 * Prevent deserialization
	 */
	private void readObject(ObjectInputStream in) throws IOException {
		throw new NotSerializableException(this+" is not serializable");
	}
}