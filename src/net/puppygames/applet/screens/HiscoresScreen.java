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
package net.puppygames.applet.screens;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Naming;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import net.puppygames.applet.AppletHiscoreServerRemote;
import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.GameInputStream;
import net.puppygames.applet.GameOutputStream;
import net.puppygames.applet.HiscoresReturn;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.Res;
import net.puppygames.applet.RoamingFile;
import net.puppygames.applet.Score;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.LabelEffect;
import net.puppygames.applet.effects.ProgressEffect;
import net.puppygames.applet.effects.SFX;
import net.puppygames.applet.widgets.TextField;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableRectangle;

import com.shavenpuppy.jglib.TextLayout;
import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLTextArea;
import com.shavenpuppy.jglib.resources.ColorSequenceResource;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;
import com.shavenpuppy.jglib.sprites.Sprite;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: HiscoresScreen.java,v 1.6 2010/08/03 23:43:39 foo Exp $
 *
 * @author $Author: foo $
 * @version $Revision: 1.6 $
 */
public class HiscoresScreen extends Screen {

	private static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	private static final String REMOTE_ON = "remote_on";
	private static final String REMOTE_OFF = "remote_off";

 	private static final String REGISTERED_COORDS = "registered_coords";
 	private static final String RANK_COORDS = "rank_coords";
 	private static final String NAME_COORDS = "name_coords";
 	private static final String SCORE_COORDS = "score_coords";

 	/** Singleton */
	private static HiscoresScreen instance;

	/** 100 Rows */
	private static final ArrayList<Row> rows = new ArrayList<Row>(100);
	private static List<Score> allScores;

	/*
	 * Resource data
	 */

	/** Layout */
	private int yPos;
	private int yGap;
	private int scoresPerPage = 10;
	private MappedColor progressBackgroundColor = new MappedColor(Color.BLUE);
	private MappedColor progressBarColor = new MappedColor(Color.WHITE);
	private String rankFont, nameFont, scoreFont;
	private MappedColor rankColor, nameColor, scoreColor = new MappedColor(Color.WHITE);

	/*
	 * Transient data
	 */
	private transient Appearance registeredAppearanceResource;
	private transient GLFont rankFontResource, nameFontResource, scoreFontResource;
	private transient boolean remoteHiscores;
	private transient TickableObject hiscoresObject;
	private transient ReadableRectangle registeredCoords, rankCoords, nameCoords, scoreCoords;

	/** Score wrapper */
	class Row {

		int pos;
		String rank, points;
		GLTextArea rankLabel, pointsLabel;
		Score score;
		TextField field;
		final Color color = new Color(Color.WHITE);
		Sprite registeredSprite;

		Row(int pos, Score score) {
			this.score = score;
			this.pos = pos;
			rank = String.valueOf(score.getRank() + 1);
			points = String.valueOf(score.getPoints());
			if (registeredAppearanceResource != null) {
				registeredSprite = allocateSprite(HiscoresScreen.this);
				if (registeredSprite != null) {
					registeredSprite.setVisible(score.isRegistered());
					registeredSprite.setAppearance(registeredAppearanceResource);
					registeredSprite.setLayer(4);
				}
			}
			rankLabel = new GLTextArea();
			rankLabel.setHorizontalAlignment(TextLayout.RIGHT);
			rankLabel.setVerticalAlignment(GLTextArea.TOP);
			rankLabel.setFont(rankFontResource);
			rankLabel.setText(rank);
			rankLabel.setColour(rankColor);
			pointsLabel = new GLTextArea();
			pointsLabel.setHorizontalAlignment(TextLayout.RIGHT);
			pointsLabel.setVerticalAlignment(GLTextArea.TOP);
			pointsLabel.setFont(scoreFontResource);
			pointsLabel.setText(points);
			pointsLabel.setColour(scoreColor);

			field = new TextField(24, nameCoords.getWidth()) {
				@Override
				protected void onEdited() {
					SFX.keyTyped();
				}
				@Override
				protected void onChangeFocus() {
					SFX.textEntered();
					setKeyboardNavigationEnabled(true);
					Row.this.score.setName(field.getText().trim());
					submitLocalScore(Row.this.score);
					if (MiniGame.getSubmitRemoteHiscores()) {
						submitRemoteScore(Row.this.score);
					}
				}
			};
			field.setAllCaps(true);
			field.setText(score.getName().toUpperCase());
			field.setFont(nameFontResource);
			field.setColour(nameColor);
			onResized();
		}

		void onResized() {
			int rowYPos = rankCoords.getY() + yPos - pos * yGap;
			if (registeredSprite != null && registeredCoords != null) {
				registeredSprite.setLocation(registeredCoords.getX(), registeredCoords.getY() + rowYPos);
			}
			rankLabel.setBounds(rankCoords.getX(), rowYPos - Res.getSmallFont().getDescent(), rankCoords.getWidth(), Res.getSmallFont().getHeight());
			pointsLabel.setBounds(scoreCoords.getX(), rowYPos - Res.getSmallFont().getDescent(), scoreCoords.getWidth(), Res.getSmallFont().getHeight());
			field.setLocation(nameCoords.getX(), nameCoords.getY() + yPos - pos * yGap);
			field.setWidth(nameCoords.getWidth());
		}

		void render(SimpleRenderer renderer) {
			renderer.glRender(new GLRenderable() {
				@Override
				public void render() {
					glPushMatrix();
					glTranslatef(1.0f, -1.0f, 0.0f);
				}
			});
			renderer.glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
			rankLabel.render(renderer);
			pointsLabel.render(renderer);
			field.render(renderer);
			renderer.glRender(new GLRenderable() {
				@Override
				public void render() {
					glPopMatrix();
				}
			});
			ColorUtil.setGLColorPre(color, renderer);
		}

		void tick() {
			field.tick();
		}

		void remove() {
			if (registeredSprite != null) {
				registeredSprite.deallocate();
				registeredSprite = null;
			}
		}
	}

	/** Score to submit */
	private transient Score scoreToSubmit;

	/** Whether to submit */
	private transient boolean doSubmit;

	/** Current page */
	private transient int page;

	/*
	 * Phases
	 */
	private static final int PHASE_NORMAL = 0;
	private static final int PHASE_EDIT = 1;
	private static final int PHASE_SUBMIT = 2;
	private static final int PHASE_DOWNLOAD = 3;

	/** Buttons */
	private static final String NEXT = "next";
	private static final String PREV = "prev";

	/** Current phase */
	private int phase;

	/** Message */
	private ProgressEffect progress;

	/** Color sequence tick */
	private int colorTick;

	/** Delay */
	private int tick;

	private static final int DELAY = 60;

	private static final ColorSequenceResource COLORSEQUENCE = new ColorSequenceResource(
			new ColorSequenceResource.SequenceEntry[] {
					new ColorSequenceResource.SequenceEntry(Color.RED, 4, 4),
					new ColorSequenceResource.SequenceEntry(Color.ORANGE, 4, 4),
					new ColorSequenceResource.SequenceEntry(Color.YELLOW, 4, 4),
					new ColorSequenceResource.SequenceEntry(Color.GREEN, 4, 4),
					new ColorSequenceResource.SequenceEntry(Color.CYAN, 4, 4),
					new ColorSequenceResource.SequenceEntry(Color.BLUE, 4, 4),
					new ColorSequenceResource.SequenceEntry(Color.PURPLE, 4, 4)
			},
			ColorSequenceResource.REPEAT);
	private Row editingRow;

	/**
	 * C'tor
	 */
	public HiscoresScreen(String name) {
		super(name);
	}

	@Override
	protected void doRegister() {
		super.doRegister();
		instance = this;
	}

	@Override
	protected void doDeregister() {
		super.doDeregister();
		instance = null;
	}

	/**
	 * Enable buttons
	 */
	protected void enableButtons() {
		if (page == 0) {
			setEnabled(PREV, false);
		} else {
			setEnabled(PREV, phase == PHASE_NORMAL);
		}
		if (page < getNumPages() - 1) {
			setEnabled(NEXT, phase == PHASE_NORMAL);
		} else {
			setEnabled(NEXT, false);
		}

		setEnabled(GenericButtons.BUY, phase != PHASE_EDIT);
		setEnabled(GenericButtons.CREDITS, phase != PHASE_EDIT);
		setEnabled(GenericButtons.EXIT, phase != PHASE_EDIT);
		setEnabled(GenericButtons.HELP, phase != PHASE_EDIT);
		setEnabled(GenericButtons.HISCORES, phase != PHASE_EDIT);
		setEnabled(GenericButtons.MOREGAMES, phase != PHASE_EDIT);
		setEnabled(GenericButtons.OPTIONS, phase != PHASE_EDIT);
		setEnabled(GenericButtons.PLAY, phase != PHASE_EDIT);
		setEnabled(GenericButtons.CLOSE, true);

		setEnabled(REMOTE_ON, phase != PHASE_EDIT);
		setEnabled(REMOTE_OFF, phase != PHASE_EDIT);

		setVisible(REMOTE_ON, MiniGame.getSubmitRemoteHiscores() && remoteHiscores);
		setVisible(REMOTE_OFF, MiniGame.getSubmitRemoteHiscores() && !remoteHiscores);

	}

	@Override
	protected void onClicked(String id) {
		GenericButtonHandler.onClicked(id);

		if (PREV.equals(id)) {
			if (phase == PHASE_EDIT) {
				return;
			}
			GenericButtonHandler.onClicked(id);
			setPage(Math.max(0, page - 1));
		} else if (NEXT.equals(id)) {
			if (phase == PHASE_EDIT) {
				return;
			}
			GenericButtonHandler.onClicked(id);
			setPage(Math.min(getNumPages() - 1, page + 1));
		} else if (REMOTE_ON.equals(id)) {
			if (phase == PHASE_EDIT) {
				return;
			}
			GenericButtonHandler.onClicked(id);
			setShowRemote(false);
		} else if (REMOTE_OFF.equals(id)) {
			if (phase == PHASE_EDIT) {
				return;
			}
			GenericButtonHandler.onClicked(id);
			setShowRemote(true);
		}
	}

	/**
	 * Sets whether to show remote hiscores or not
	 * @param flag
	 */
	private void setShowRemote(boolean flag) {
		if (remoteHiscores != flag) {
			remoteHiscores = flag;
			loadHiscores();
		}

		enableButtons();
	}

	private void loadHiscores() {
		if (remoteHiscores) {
			loadRemoteScores();
		} else {
			loadLocalScores();
		}
	}

	private void loadRemoteScores() {
		// Download more scores
		phase = PHASE_DOWNLOAD;
		progress = new ProgressEffect(Game.getMessage("lwjglapplets.hiscoresscreen.downloadprogress"), progressBackgroundColor, progressBarColor);
		progress.spawn(this);
		new Thread() {
			@Override
			public void run() {
				try {
					AppletHiscoreServerRemote server = (AppletHiscoreServerRemote) Naming.lookup("//"+AppletHiscoreServerRemote.REMOTE_HOST+"/"+AppletHiscoreServerRemote.REMOTE_NAME);
					List<Score> scores = server.getHiscores(Game.getTitle());
					Game.onRemoteCallSuccess();
					setScoreList(scores);
				} catch (Exception e) {
					Res.getErrorDialog().doModal(Game.getMessage("lwjglapplets.hiscoresscreen.problems"), Game.getMessage("lwjglapplets.hiscoresscreen.unavailable")+" ("+e+")");
				} finally {
					phase = PHASE_NORMAL;
					synchronized (HiscoresScreen.this) {
						tick = 0;
						if (progress != null) {
							progress.setFinished(true);
							progress = null;
						}
					}
					enableButtons();
				}
			}
		}.start();
	}

	private void loadLocalScores() {
		GameInputStream gis = null;
		ObjectInputStream ois = null;
		try {
			RoamingFile hiscoresFile = new RoamingFile(getHiscoreFileName());
			if (hiscoresFile.exists()) {
				gis = new GameInputStream(getHiscoreFileName());
				ois = new ObjectInputStream(gis);
				setScoreList((ArrayList<Score>) ois.readObject()); // warning suppressed
			} else {
				setScoreList(new ArrayList<Score>(0));
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			setScoreList(new ArrayList<Score>(0));
		} finally {
			if (gis != null) { try { gis.close(); } catch (Exception e) {} }
		}
	}

	/**
	 * @return the filename to use for local hiscores
	 */
	private String getHiscoreFileName() {
		return Game.getRoamingDirectoryPrefix()+"hiscores.dat";
	}

	/**
	 * Set the currently displayed page
	 * @param newPage
	 */
	public void setPage(int newPage) {
		this.page = newPage;
		enableButtons();
	}

	/**
	 * @return the number of hiscore pages (minimum 1)
	 */
	protected final int getNumPages() {
		return (rows.size() - 1) / scoresPerPage + 1;
	}

	@Override
	protected void doTick() {
		if (editingRow != null) {
			COLORSEQUENCE.getColor(colorTick ++, editingRow.color);
		}
		for (int i = 0; i < rows.size(); i ++) {
			Row row = rows.get(i);
			row.tick();
		}
	}

	@Override
	protected void doCleanup() {
		if (hiscoresObject != null) {
			hiscoresObject.remove();
			hiscoresObject = null;
		}

		for (Iterator<Row> i = rows.iterator(); i.hasNext(); ) {
			Row row = i.next();
			row.remove();
		}
		rows.clear();
	}

	/**
	 * Show the hiscore screen.
	 * @param score The player's score, if a new hiscore is being entered
	 */
	public static void show(Score score) {
		if (score != null && score.getPoints() > 0) {
			instance.doSubmit = true;
		} else {
			instance.doSubmit = false;
		}
		instance.scoreToSubmit = score;
		instance.open();
	}

	@Override
	protected void onClose() {
		Game.setPauseEnabled(true);
	}



	@Override
	protected synchronized void onOpen() {
		// Always put hiscores in local hiscore table
		remoteHiscores = false;

		enableButtons();

		GenericButtonHandler.onOpen(this);
		tick = 0;
		colorTick = 0;
		Game.setPauseEnabled(false);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		loadHiscores();

		// Rendering is now done with a TickableObject
		hiscoresObject = new TickableObject() {
			@Override
			public void tick() {
				for (int i = 0; i < rows.size(); i ++) {
					Row row = rows.get(i);
					if (i >= page * scoresPerPage && i < page * scoresPerPage + scoresPerPage) {
						if (row.registeredSprite != null) {
							row.registeredSprite.setVisible(row.score.isRegistered());
						}
					} else {
						if (row.registeredSprite != null) {
							row.registeredSprite.setVisible(false);
						}
					}
				}
			}
			@Override
			protected void render() {
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glEnable(GL_TEXTURE_2D);
						glEnable(GL_BLEND);
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
						glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
					}
				});
				for (int i = 0; i < rows.size(); i ++) {
					Row row = rows.get(i);
					if (i >= page * scoresPerPage && i < page * scoresPerPage + scoresPerPage) {
						row.render(this);
					}
				}
			}
		};
		hiscoresObject.setLayer(100);
		hiscoresObject.spawn(this);

	}

	private synchronized void setScoreList(List<Score> scores) {
		allScores = scores;
		for (Iterator<Row> i = rows.iterator(); i.hasNext(); ) {
			Row row = i.next();
			row.remove();
		}
		rows.clear();

		// Create pages of Rows. If we've got a score to submit, we will insert
		// an editable row somewhere in there as well, which means we'll have to
		// adjust all the ranks of subsequent scores
		int rank = 0;
		int page = 0, editingPage = 0;
		int pos = 0;
		boolean done = false;
		phase = PHASE_NORMAL;
		for (Iterator<Score> i = allScores.iterator(); i.hasNext(); ) {
			Score s = i.next();
			if (doSubmit && scoreToSubmit != null && scoreToSubmit.compareTo(s) == -1 && !done) {
				// We've got a hiscore to submit, and it's beaten this score! So this row
				// is the editing row.
				scoreToSubmit.setRank(rank ++);
				Row row = new Row(pos, scoreToSubmit);
				row.field.setEditing(true);
				setKeyboardNavigationEnabled(false);
				rows.add(row);
				editingPage = page;
				done = true;
				pos ++;
				if (pos == scoresPerPage) {
					pos = 0;
					page ++;
				}
				phase = PHASE_EDIT;
			}

			s.setRank(rank ++);
			Row row = new Row(pos, s);
			rows.add(row);

			pos ++;
			if (pos == scoresPerPage) {
				pos = 0;
				page ++;
			}
		}

		// So we've got to the end.. perhaps we've not managed to find a slot?
		if (doSubmit && scoreToSubmit != null && !done && rank < AppletHiscoreServerRemote.MAX_SCORES) {
			scoreToSubmit.setRank(rank ++);
			Row row = new Row(pos, scoreToSubmit);
			row.field.setEditing(true);
			setKeyboardNavigationEnabled(false);
			rows.add(row);
			editingPage = page;
			phase = PHASE_EDIT;
		}

		setPage(editingPage);
	}

	private void submitRemoteScore(final Score score) {
		phase = PHASE_SUBMIT;
		progress = new ProgressEffect(Game.getMessage("lwjglapplets.hiscoresscreen.submitting"), progressBackgroundColor, progressBarColor);
		progress.spawn(instance);
		enableButtons();

		new Thread() {
			@Override
			public void run() {
				try {
					AppletHiscoreServerRemote server = (AppletHiscoreServerRemote) Naming.lookup("//"+AppletHiscoreServerRemote.REMOTE_HOST+"/"+AppletHiscoreServerRemote.REMOTE_NAME);
					doSubmit = false;
					HiscoresReturn ret = server.submit2(score);
					if (remoteHiscores) {
						setScoreList(ret.getScores());
					}
					if (ret.getMessage() != null) {
						StringTokenizer st = new StringTokenizer(ret.getMessage(), "\n", false);
						List<String> tokens = new LinkedList<String>();
						while (st.hasMoreTokens()) {
							tokens.add(st.nextToken());
						}
						int h = tokens.size() * Res.getBigFont().getHeight();
						int y = (Game.getHeight() - h) / 2 + (tokens.size() - 1) * Res.getBigFont().getHeight();
						int delay = 0;
						for (Iterator<String> i = tokens.iterator(); i.hasNext(); ) {
							LabelEffect le = new LabelEffect(Res.getBigFont(), i.next(), ReadableColor.WHITE, ReadableColor.CYAN, 240, 120);
							le.setLocation(Game.getWidth() / 2, y);
							y -= Res.getBigFont().getHeight();
							le.setDelay(delay);
							delay += 30;
							le.setSound(SFX.getTextEnteredBuffer());
							le.spawn(instance);
						}
					}
					Game.onRemoteCallSuccess();
				} catch (SQLException e) {
					e.printStackTrace(System.err);
					Game.onRemoteCallSuccess(); // The actual call succeeded
					Res.getErrorDialog().doModal(Game.getMessage("lwjglapplets.hiscoresscreen.problems"), e.getMessage());
				} catch (Exception e) {
					e.printStackTrace(System.err);
					Res.getErrorDialog().doModal("PROBLEMS", e.getMessage());
				} finally {
					phase = PHASE_NORMAL;
					progress.setFinished(true);
					progress = null;
					tick = 0;
					enableButtons();
				}
			}
		}.start();
	}

	private void submitLocalScore(Score score) {
		doSubmit = false;
		allScores.add(score);
		Collections.sort(allScores);
		GameOutputStream gos = null;
		ObjectOutputStream oos = null;
		try {
			gos = new GameOutputStream(getHiscoreFileName());
			oos = new ObjectOutputStream(gos);
			oos.writeObject(allScores);
			oos.flush();
			gos.flush();
			phase = PHASE_NORMAL;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			Res.getErrorDialog().doModal(Game.getMessage("lwjglapplets.hiscoresscreen.problems"), Game.getMessage("lwjglapplets.hiscoresscreen.problemsaving")+" ("+e+")");
		} finally {
			if (oos != null) { try { oos.close(); } catch (Exception e) {} }
			if (gos != null) { try { gos.close(); } catch (Exception e) {} }
			enableButtons();
		}
	}

	@Override
	protected void onResized() {
		Area registeredArea = getArea(REGISTERED_COORDS);
		if (registeredArea != null) {
			registeredCoords = registeredArea.getBounds();
		}
		rankCoords = getArea(RANK_COORDS).getBounds();
		nameCoords = getArea(NAME_COORDS).getBounds();
		scoreCoords = getArea(SCORE_COORDS).getBounds();

		for (Row row : rows) {
			row.onResized();
		}
	}
}
