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
package net.puppygames.applet;

import java.io.Serializable;

/**
 * $Id: Score.java,v 1.10 2008/03/22 13:09:41 foo Exp $
 *
 * A Score
 *
 * @author $Author: foo $
 * @version $Revision: 1.10 $
 */
public class Score implements Serializable, Comparable<Score> {

	private static final long serialVersionUID = 3L;

	/** Max name length */
	public static final int MAX_LENGTH = 24;

	/** Rank */
	private int rank;

	/** Points */
	private final int points;

	/** Name */
	private String name;

	/** Group */
	private String group;

	/** Installation ID */
	private final long installation;

	/** Medals */
	private String medals;

	/** Game */
	private String game;

	/** Registered */
	private boolean registered;

	/** Version */
	private String version;

	/**
	 * C'tor
	 */
	public Score(String game, String version, String group, String name, long installation, int points, String medals, boolean registered) {
		this.game = game;
		this.version = version;
		this.group = "".equals(group) ? null : group;
		this.installation = installation;
		this.points = points;
		if (name != null) {
			setName(name);
		}
		if (medals == null) {
			medals = "";
		}
		this.medals = medals;
		this.registered = registered;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name.length() > MAX_LENGTH ? name.substring(0, MAX_LENGTH) : name;
	}

	/**
	 * @return Returns the player's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the points
	 */
	public int getPoints() {
		return points;
	}

	/**
	 * Comparable interface. The quickest 100% complete time is at the top.
	 * @param obj
	 * @return int
	 */
	@Override
	public int compareTo(Score obj) {
		if (obj == null) {
			return -1;
		}
		Score score = obj;
		if (score.points < this.points) {
			return -1;
		} else if (score.points == this.points) {
			return 0;
		} else {
			return 1;
		}
	}

	/**
	 * Is this score 'like' the incoming score?
	 * @param s The score to test
	 * @return boolean
	 */
	public boolean like(Score s) {
		return
			s.game.equals(game)
		&&	s.installation == installation
		&&	s.name.equals(name)
		&&
			(
				(
					s.group == null
				&&	group == null
				)
			||
				(
					s.group != null
				&&	s.group.equals(group)
				)
			);
	}

	@Override
    public String toString() {
	    StringBuilder builder = new StringBuilder();
	    builder.append("Score [");
	    if (name != null) {
		    builder.append("name=");
		    builder.append(name);
		    builder.append(", ");
	    }
	    builder.append("points=");
	    builder.append(points);
	    builder.append(", installation=");
	    builder.append(installation);
	    builder.append(", ");
	    if (game != null) {
		    builder.append("game=");
		    builder.append(game);
		    builder.append(", ");
	    }
	    if (version != null) {
		    builder.append("version=");
		    builder.append(version);
		    builder.append(", ");
	    }
	    builder.append("rank=");
	    builder.append(rank);
	    builder.append(", ");
	    if (group != null) {
		    builder.append("group=");
		    builder.append(group);
		    builder.append(", ");
	    }
	    if (medals != null) {
		    builder.append("medals=");
		    builder.append(medals);
		    builder.append(", ");
	    }
	    builder.append("registered=");
	    builder.append(registered);
	    builder.append("]");
	    return builder.toString();
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Score)) {
			return false;
		}
		Score s = (Score) obj;
		return
				s.game.equals(game)
			&&	s.installation == installation
			&&	s.medals.equals(medals)
			&&	s.name.equals(name)
			&&
				(
					(
						s.group == null
					&&	group == null
					)
				||
					(
						s.group != null
					&&	s.group.equals(group)
					)
				)
			&&	s.points == points;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return game.hashCode() ^ version.hashCode() ^ points ^ (int)installation ^ name.hashCode() ^ medals.hashCode() ^ (group == null ? 0 : group.hashCode());
	}

	/**
	 * @return Returns the installation.
	 */
	public long getInstallation() {
		return installation;
	}

	/**
	 * @return Returns the medals.
	 */
	public String getMedals() {
		return medals;
	}

	/**
	 * @return Returns the game.
	 */
	public String getGame() {
		return game;
	}

	/**
	 * @return Returns the rank.
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * @param rank The rank to set.
	 */
	public void setRank(int rank) {
		this.rank = rank;
	}

	public void setVersion(String version) {
	    this.version = version;
    }

	/**
	 * @return Returns the group.
	 */
	public String getGroup() {
		return group;
	}
	/**
	 * @param group The group to set.
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * @return Returns the registered.
	 */
	public boolean isRegistered() {
		return registered;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version == null ? "None" : version;
	}
}
