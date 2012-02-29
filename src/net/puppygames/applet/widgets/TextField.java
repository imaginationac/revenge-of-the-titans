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

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLString;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: TextField.java,v 1.5 2010/04/14 23:38:32 foo Exp $
 * <p>
 * A single line editing text field.
 *
 * @author $Author: foo $
 * @version $Revision: 1.5 $
 */
public class TextField {

	/** Are we editing? */
	private boolean editing;

	/** Old text value */
	private String oldText;

	/** Allow uppercase? */
	private boolean allowUppercase;

	/** All caps */
	private boolean allCaps;

	/** Cursor pos */
	private int cursorPos = 0;

	/** Flash ticker */
	private int flashTick = 0;

	/** Cursor visibility */
	private boolean cursorVisible;

	/** Edit display */
	private GLString display;

	/** Editing buffer */
	private final StringBuilder buffer;

	/** Width, in pixels */
	private int width;

	/**
	 * C'tor
	 */
	public TextField(int maxLength, int width) {
		display = new GLString(maxLength);
		buffer = new StringBuilder(maxLength);
		this.width = width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * Sets this text field to "editing" mode or not
	 * @param editing
	 */
	public void setEditing(boolean editing) {
		this.editing = editing;

		if (editing) {
			oldText = display.getText();
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
					cursorPos = display.length();
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
						cursorPos = display.length();
					} else if (cursorPos < display.length()) {
						cursorPos ++;
					}
					break;
				case Keyboard.KEY_DELETE:
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
						buffer.setLength(0);
						cursorPos = 0;
						display.setText("");
					} else if (cursorPos < display.length()) {
						buffer.deleteCharAt(cursorPos);
						display.setText(buffer.toString());
					}
					onEdited();
					break;
				case Keyboard.KEY_BACK:
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
						buffer.setLength(0);
						cursorPos = 0;
						display.setText("");
					} else {
						if (cursorPos > 0) {
							buffer.deleteCharAt(-- cursorPos);
							display.setText(buffer.toString());
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
							if (!addChar(c)) {
								break;
							}
						}
					} else if (c == 26 && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
						undo();
					} else if (c >= 32 && c < 127) {
						addChar(c);
					}
					break;
			}
		}

		if (oldCursorPos != cursorPos) {
			cursorVisible = true;
			flashTick = 0;
		}
	}

	private boolean addChar(char c) {
		if (!allowUppercase) {
			c = Character.toLowerCase(c);
		}
		if (allCaps) {
			c = Character.toUpperCase(c);
		}
		if (buffer.length() < buffer.capacity() && acceptChar(c)) {
			buffer.insert(cursorPos ++, c);
			display.setText(buffer.toString());
			onEdited();
			return true;
		} else {
			return false;
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

	public String getText() {
		return display.getText();
	}
	public int length() {
		return display.length();
	}
	public void setFont(GLFont font) {
		display.setFont(font);
	}
	public void setText(String s) {
		display.setText(s);
		buffer.delete(0, buffer.length());
		buffer.append(s);
		cursorPos = s.length();
	}
	public void setLocation(int xp, int yp) {
		display.setLocation(xp, yp);
	}

	/**
	 * @return the X position of the cursor
	 */
	private int getCursorXPos() {
		if (!editing) {
			return 0;
		} else {
			Rectangle tempBounds = display.getGlyphBounds(cursorPos - 1, null);
			return tempBounds.getX() + tempBounds.getWidth();
		}
	}

	public void render(final SimpleRenderer renderer) {
		final int cursorX = getCursorXPos();
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glEnable(GL_SCISSOR_TEST);
			}
		});
		if (cursorX - display.getX() > width && editing) {
			renderer.glRender(new GLRenderable() {
				@Override
				public void render() {
					glPushMatrix();
					glTranslatef(width - cursorX + display.getX(), 0.0f, 0.0f);
				}
			});
		}
		// Annoyingly have to scissor according to window coords...
		float wRatio = (float) Game.getViewPort().getWidth() / (float) Game.getWidth();
		float hRatio = (float) Game.getViewPort().getHeight() / (float) Game.getHeight();
		final int sx = (int)(display.getX() * wRatio)  + Game.getViewPort().getX();
		final int sw = (int)(width * wRatio);
		final int cy = display.getY() - display.getFont().getDescent();
		final int sy = (int)((cy) * hRatio) + Game.getViewPort().getY();
		final int sh = (int)(display.getFont().getHeight() * hRatio);
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glScissor(sx, sy, sw, sh);
			}
		});
		display.render(renderer);
		renderer.glRender(new GLRenderable() {
			@Override
			public void render() {
				glDisable(GL_SCISSOR_TEST);
			}
		});
		// Draw cursor
		if (cursorVisible && editing) {
			renderer.glRender(new GLRenderable() {
				@Override
				public void render() {
					glDisable(GL_TEXTURE_2D);
				}
			});
			if (display.isColoured()) {
				ColorUtil.setGLColorPre(display.getBottomColour(), display.getAlpha(), renderer);
			}
			short idx = renderer.glVertex2f(cursorX, cy);
			renderer.glVertex2f(cursorX + 4, cy);
			if (display.isColoured()) {
				ColorUtil.setGLColorPre(display.getTopColour(), display.getAlpha(), renderer);
			}
			renderer.glVertex2f(cursorX + 4, cy + display.getFont().getHeight());
			renderer.glVertex2f(cursorX, cy + display.getFont().getHeight());
			renderer.glRender(GL_TRIANGLE_FAN, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 3)});
			renderer.glRender(new GLRenderable() {
				@Override
				public void render() {
					glEnable(GL_TEXTURE_2D);
				}
			});
		}
		if (cursorX - display.getX() > width && editing) {
			renderer.glRender(new GLRenderable() {
				@Override
				public void render() {
					glPopMatrix();
				}
			});
		}
	}

	/**
	 * @param allowUppercase the allowUppercase to set
	 */
	public void setAllowUppercase(boolean allowUppercase) {
		this.allowUppercase = allowUppercase;
	}

	public void setAllCaps(boolean allCaps) {
		this.allCaps = allCaps;
	}

	public void setAlpha(int alpha) {
		display.setAlpha(alpha);
	}

	public void setBottomColour(ReadableColor c) {
		display.setBottomColour(c);
	}

	public void setColour(ReadableColor c) {
		display.setColour(c);
	}

	public void setTopColour(ReadableColor c) {
		display.setTopColour(c);
	}


}
