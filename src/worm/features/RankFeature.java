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
package worm.features;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import net.puppygames.applet.Game;

import com.shavenpuppy.jglib.resources.Feature;

/**
 * Ranks
 */
public class RankFeature extends Feature {

	private static final long serialVersionUID = 1L;

	private static final SortedSet<RankFeature> RANKS = new TreeSet<RankFeature>(new Comparator<RankFeature>() {
		@Override
		public int compare(RankFeature r0, RankFeature r1) {
			if (r0.getPoints() < r1.getPoints()) {
				return -1;
			} else if (r0.getPoints() > r1.getPoints()) {
				return 1;
			} else {
				return 0;
			}
		}
	});

	private String title;
	private int points;
	private LayersFeature appearance;

	private transient HintFeature hint;

	/**
	 * C'tor
	 */
	public RankFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the points
	 */
	public int getPoints() {
		return points;
	}

	@Override
	protected void doRegister() {
		RANKS.add(this);
	}

	@Override
	protected void doDeregister() {
		RANKS.remove(this);
	}

	@Override
	protected void doCreate() {
		super.doCreate();

		hint = new HintFeature();
		hint.setDetails(appearance, "{color:gui-mid font:tinyfont.glfont}" + Game.getMessage("ultraworm.intermission.new_rank") + " BONUS:$"+points/10+"\n{color:gui-bright font:smallfont.glfont}"+title);
		hint.create();
	}

	public HintFeature getHint() {
		return hint;
	}

	public LayersFeature getAppearance() {
		return appearance;
	}

	public static RankFeature getRank(int points) {
		RankFeature ret = null;
		for (Iterator<RankFeature> i = RANKS.iterator(); i.hasNext(); ) {
			RankFeature next = i.next();
			if (ret == null) {
				ret = next;
			}
			if (next.getPoints() > points) {
				return ret;
			}
			ret = next;
		}
		return ret;
	}
}
