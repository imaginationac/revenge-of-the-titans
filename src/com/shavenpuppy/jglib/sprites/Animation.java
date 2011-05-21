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

import java.io.IOException;
import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.XMLResourceWriter;

/**
 * An Animation specifies a sequence of Commands which control the image displayed
 * by an Animated thing.
 */
public class Animation extends AnimatedAppearanceResource {

	public static final long serialVersionUID = 1L;


	/** Known commands */
	private static final Map<String, Class<? extends Resource>> commandTagMap = new HashMap<String, Class<? extends Resource>>();
	static {
		commandTagMap.put("angle", AngleCommand.class);
		commandTagMap.put("offset", OffsetCommand.class);
		commandTagMap.put("move", MoveCommand.class);
		commandTagMap.put("scale", ScaleCommand.class);
		commandTagMap.put("frame", FrameCommand.class);
		commandTagMap.put("flag", FlagCommand.class);
		commandTagMap.put("alpha", AlphaCommand.class);
		commandTagMap.put("color", ColorCommand.class);
		commandTagMap.put("animcolor", AnimColorCommand.class);
		commandTagMap.put("goto", GotoCommand.class);
		commandTagMap.put("next", NextCommand.class);
		commandTagMap.put("delay", RandomDelayCommand.class);
		commandTagMap.put("random", RandomGotoCommand.class);
		commandTagMap.put("event", EventCommand.class);
		commandTagMap.put("loop", LoopCommand.class);
		commandTagMap.put("repeat", RepeatCommand.class);
		commandTagMap.put("sound", SoundCommand.class);
		commandTagMap.put("label", LabelCommand.class);
		commandTagMap.put("sub", SubCommand.class);
		commandTagMap.put("return", ReturnCommand.class);
		commandTagMap.put("frameset", FrameListCommand.class);
	}

	/** Commands */
	private Command[] command;

	/** Labels */
	private Map<String, Integer> labels;

	/**
	 * Constructor for Animation.
	 */
	public Animation() {
		super();
	}

	/**
	 * Constructor for Animation.
	 */
	public Animation(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see GLXMLResource#load(Element)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {

		labels = null; // for reload...
		try {
			// Add the known commands - you have to specify other commands yourself
			// in the XML
			loader.pushMap(commandTagMap);

			// The child tags of the element should all be descended from Commands.
			NodeList childTagList = element.getChildNodes();

			ArrayList<Resource> commandList = new ArrayList<Resource>(childTagList.getLength());
			for (int i = 0; i < childTagList.getLength(); i ++) {
				if (childTagList.item(i) instanceof Element) {
					Element childElement = (Element) childTagList.item(i);
					Resource childResource = loader.load(childElement);
					if (!(childResource instanceof Command)) {
						throw new Exception("Only Command resources are allowed inside an Animation; got a "+childResource);
					}
					if (childResource instanceof LabelCommand) {
						String id = ((LabelCommand) childResource).getID();
						if (labels == null) {
							labels = new HashMap<String, Integer>();
						} else if (labels.containsKey(id)) {
							throw new Exception("Animation "+this+" already contains label "+id);
						}
						labels.put(id, new Integer(commandList.size()));
					} else {
						commandList.add(childResource);
					}
				}
			}

			command = new Command[commandList.size()];
			commandList.toArray(command);
		} finally {
			// Make sure we remove those commandTags
			loader.popMap();
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		boolean wasCompact = writer.isCompact();
		writer.setCompact(true);
		for (int i = 0; i < command.length; i ++) {
			command[i].toXML(writer);
		}
		writer.setCompact(wasCompact);
	}

	/* (non-Javadoc)
	 * @see ALResource#doCreate()
	 */
	@Override
	protected void doCreate() {
		// Create all the commands
		for (int i = 0; i < command.length; i ++) {
			command[i].create();
		}

	}

	/* (non-Javadoc)
	 * @see ALResource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		// Destroy all the commands
		for (int i = 0; i < command.length; i ++) {
			command[i].destroy();
		}
	}

	/**
	 * Animate an animated thing. The animated thing should pass in its current
	 * sequence and current tick.
	 *
	 * The tick is increased by one, and if this is greater than or equal to
	 * the duration specified for the current sequence, the sequence number is
	 * increased by one and the new sequence's command is executed. If the
	 * duration of the command is 0 then the next command is executed, and so on,
	 * until a duration &gt; 0 is encountered.
	 *
	 * If there are no more sequences then nothing happens.
	 *
	 * When a command is executed, it is applied to the Animated thing.
	 *
	 * @param animated The thing that wants to be animated
	 * @param tickRate The number of ticks that pass this time
	 */
	public void animate(Animated animated, int tickRate) {

		int currentSequence;

		do {
			currentSequence = animated.getSequence();

			// Do nothing if the current sequence is not valid
			if (currentSequence < 0 || currentSequence >= command.length) {
				return;
			}

		} while (command[currentSequence].execute(animated, tickRate));

	}

	/**
	 * Get the sequence number of the label.
	 * @param id
	 * @return -1 if the label isn't found
	 */
	public int getLabel(String id) {
		if (labels == null) {
			return -1;
		}
		Integer ret = labels.get(id);
		if (ret == null) {
			return -1;
		} else {
			return ret.intValue();
		}
	}

	@Override
	public boolean toAnimated(Animated target) {
		target.setAnimation(this);
		return true;
	}

}
