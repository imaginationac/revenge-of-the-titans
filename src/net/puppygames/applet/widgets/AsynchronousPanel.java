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

import net.puppygames.applet.Game;
import net.puppygames.applet.effects.Effect;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.ReadableDimension;

/**
 * Displays aysnchronous operations progress and error messages
 * @author Cas
 */
public class AsynchronousPanel extends Effect {

	/** The operation we're performing */
	private final AsynchronousOperation op;

	/** Thread performing the work */
	private Thread worker;

	/** Exception message box */
	private MessageBox errorMessageBox;

	/** Normal message box */
	private MessageBox messageBox;

	/** Are we still active */
	private boolean finished;

	/** Bounds on screen */
	private ReadableDimension size;

	/** Modal operation */
	private boolean modal;

	/** Whether screen was enabled or not before we went modal */
	private boolean wasEnabled;

	/**
	 * C'tor
	 */
	public AsynchronousPanel(ReadableDimension size, boolean modal, final AsynchronousOperation op) {
		this.size = size;
		this.op = op;

		worker = new Thread("Aynchronous Operation "+op) {
			{
				setPriority(NORM_PRIORITY - 1);
			}
			@Override
			public void run() {
				try {
					op.run();
				} catch (Exception e) {
					onException(e);
				} finally {
					finished = true;
				}
			}
		};

		messageBox = new MessageBox();
		messageBox.setSize(size.getWidth(), size.getHeight());
		messageBox.setMessage(op.getMessage());
		messageBox.setTitle(op.getTitle());

		this.modal = modal;
	}

	@Override
	protected void doSpawnEffect() {
		if (modal) {
			wasEnabled = getScreen().isEnabled();
			getScreen().setEnabled(false);
		}
		worker.start();
	}

	@Override
	protected void render() {
		messageBox.render(this);
	}

	@Override
	protected void doTick() {
		if (getScreen().isClosed() || getScreen().isClosing()) {
			// Attempt to cancel the async op if it's cancellable
			op.cancel();
			remove();
		}
	}

	@Override
	public synchronized boolean isEffectActive() {
		return !finished;
	}

	@Override
	protected synchronized void doRemove() {
		finished = true;

		if (errorMessageBox != null) {
			new Effect() {

				boolean done;

				@Override
				protected void render() {
					errorMessageBox.render(this);
				}

				@Override
				protected void doTick() {
					if (Mouse.isCreated() && Mouse.isButtonDown(0)) {
						remove();
					}
					if (Keyboard.isCreated() && Keyboard.getNumKeyboardEvents() > 0) {
						remove();
					}
				}

				@Override
				protected void doRemove() {
					done = true;
					getScreen().setEnabled(wasEnabled);
				}

				@Override
				public boolean isEffectActive() {
					return !done;
				}

			}.spawn(getScreen());
		} else {
			getScreen().setEnabled(wasEnabled);
		}

		op.onCompleted();


	}

	/**
	 * Called if an exception occurs. This puts up an exception message box
	 * and waits for a click or keyboard press.
	 */
	private synchronized void onException(Exception e) {
		errorMessageBox = new MessageBox();
		errorMessageBox.setTitle(Game.getMessage("lwjglapplets.async.title"));
		errorMessageBox.setMessage(op.getErrorMessage());
		errorMessageBox.setSize(size.getWidth(), size.getHeight());
		e.printStackTrace(System.err);
	}

}
