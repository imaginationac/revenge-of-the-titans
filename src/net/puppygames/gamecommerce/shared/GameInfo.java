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
package net.puppygames.gamecommerce.shared;

import java.io.*;
import java.sql.Date;
import java.sql.SQLException;

/**
 * A simple class storing game runtime information in it for collection play statistic from players.
 */
public final class GameInfo implements Serializable {

	public static final long serialVersionUID = 1L;

	/** The game name */
	private final String game;

	/** The game version */
	private final String version;

	/** Installation ID */
	private final long installation;

	/** The reg code */
	private String regCode;

	/** Adapter & version */
	private final String display_adapter;
	private final String adapter_version;

	/** The GL_RENDERER and GL_EXTENSIONS strings */
	private String glvendor;
	private String glversion;
	private String glrenderer;
	private String gldriver;

	/** The crash recovery flag: the last game crashed */
	private final boolean crashRecovery;

	/** The length of time the last run was in seconds */
	private int runtime;

	/** The number of time the game was played */
	private int played;

	/* Useful properties */
	private String os_arch, os_version, os_name, java_version, user_country;
	private boolean webstarted;

	/** Arbitrary string configuration */
	private String config;

	/** Exception: added in new version */
	private Throwable exception;

	/**
	 * Database interface
	 */
	public interface Database {
		public void insertGameInfo(
			String host,
			String game,
			String version,
			long installation,
			String regCode,
			String display_adapter,
			String adapter_version,
			String glvendor,
			String glrenderer,
			String glversion,
			String gldriver,
			boolean crashRecovery,
			int runtime,
			int played,
			String os_name,
			String os_version,
			String os_arch,
			String java_version,
			String user_country,
			boolean webstarted,
			String config,
			Date date,
			String exception
		) throws SQLException;
	}

	/**
	 * Create a new GameInfo.
	 */
	public GameInfo(String game, String version, long installation, boolean crashRecovery, String display_adapter, String adapter_version, String config)
	{
		this.game = game;
		this.version = version;
		this.installation = installation;
		this.crashRecovery = crashRecovery;
		this.display_adapter = display_adapter;
		this.adapter_version = adapter_version;
		this.os_name = System.getProperty("os.name");
		this.os_version = (System.getProperty("os.version")+" "+System.getProperty("sun.os.patch.level", "")).trim();
		this.os_arch = (System.getProperty("os.arch")+" "+System.getProperty("sun.cpu.isalist", "")).trim();
		this.java_version = System.getProperty("java.vm.version");
		this.user_country = System.getProperty("user.country");
		this.webstarted = !System.getProperty("jnlpx.home", "!").equals("!");
		this.config = config;
	}

	/**
	 * Sets the exception that occurred
	 * @param exception
	 */
	public void setException(Throwable exception) {
		this.exception = exception;
	}

	/**
	 * Update with further information. Because a myriad of things can go wrong at different bits of game
	 * initialization, we supply this extra information a bit later than the initial constructor.
	 * @param glvendor The GL vendor
	 * @param glrenderer The GL renderer
	 * @param glversion The GL version
	 * @param gldriver The GL driver
	 * @param registration The registration (or null if unregistered)
	 */
	public void update(String glvendor, String glrenderer, String glversion, String gldriver, RegistrationDetails registration) {
		this.glvendor = glvendor;
		this.glrenderer = glrenderer;
		this.glversion = glversion;
		this.gldriver = gldriver;
		this.regCode = registration != null ? registration.getAuthCode() : null;
	}

	/**
	 * Stash in a database
	 * @param db The database
	 * @throws SQLException
	 */
	public void insertInto(Database db, String host) throws SQLException {
		String exceptionMessage = "OK";
		if (exception != null) {
			StringWriter sw = new StringWriter(512);
			PrintWriter pw = new PrintWriter(sw);
			if (exception.getMessage() != null) {
				pw.write(exception.getClass().getName()+" thrown with message ");
				pw.write(exception.getMessage());
			} else {
				pw.write(exception.getClass().getName()+" thrown (no message)");
			}
			pw.write('\n');
			exception.printStackTrace(pw);
			pw.flush();
			exceptionMessage = sw.getBuffer().toString();
		}

		db.insertGameInfo(
			host,
			game,
			version,
			installation,
			regCode,
			display_adapter,
			adapter_version,
			glvendor,
			glrenderer,
			glversion,
			gldriver,
			crashRecovery,
			runtime,
			played,
			os_name,
			os_version,
			os_arch,
			java_version,
			user_country,
			webstarted,
			config,
			new java.sql.Date(new java.util.Date().getTime()),
			exceptionMessage
		);
	}

	public void addTime(int timeInSeconds) {
		runtime += timeInSeconds;
	}

	/**
	 * Called whenever a new game is started
	 */
	public void onNewGame() {
		played ++;
	}

	/**
	 * @return the game title
	 */
	public String getGameTitle() {
		return game;
	}

	/**
	 * @return the version
	 */
	public String getGameVersion() {
		return version;
	}

	/**
	 * @return the installation id
	 */
	public long getInstallation() {
		return installation;
	}

	/**
	 * @return
	 */
	public boolean isCrashRecovery() {
		return crashRecovery;
	}

	/**
	 * @return
	 */
	public Throwable getException() {
		return exception;
	}
}
