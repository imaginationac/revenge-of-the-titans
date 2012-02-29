/*
 * Copyright (c) 2003 Shaven Puppy Ltd
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
 *
 * Code in here adapted from JOrbis example code. Thanks guys.
 */
package com.shavenpuppy.jglib.sound;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.w3c.dom.Element;

import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;
import com.shavenpuppy.jglib.*;
import com.shavenpuppy.jglib.resources.WaveWrapper;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * An Ogg-Vorbis loader. Loads a .ogg file and creates a Wave instance out of
 * it.
 *
 * @author foo
 */
public class OggVorbis extends Resource implements WaveWrapper {

	private static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	/** Conversion buffer size */
	private static int convsize = 4096 * 2;

	/** Conversion buffer */
	private static byte[] convbuffer = new byte[convsize];

	/** Decoded cache */
	private static String cacheDirectory;

	/*
	 * Resource data
	 */

	/** The source .OGG URL. Can begin with classpath: */
	private String url;

	/** Streamed? */
	private boolean streamed;

	/*
	 * Transient data
	 */

	/** The decompressed Wave for non-streamed Oggs */
	private transient Wave wave;

	/** Wave frequency */
	private transient int frequency = 0;

	/** Wave type */
	private transient int type = 0;

	/**
	 * Constructor
	 */
	public OggVorbis() {
		super();
	}

	/**
	 * Resource constructor
	 *
	 * @param name
	 */
	public OggVorbis(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element,
	 *      com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		url = XMLUtil.getString(element, "url");
		streamed = XMLUtil.getBoolean(element, "streamed", false);
	}

	/**
	 * @return true if this is a streamed Ogg
	 */
	public boolean isStreamed() {
		return streamed;
	}

	private InputStream createStream() throws Exception {
		InputStream inStream;
		if (url.startsWith("classpath:")) {
			// Load directly from the classpath
			String resourcePath = url.substring("classpath:".length());

			inStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
			if (inStream == null) {
				throw new FileNotFoundException("Classpath resource not found:" + resourcePath);
			}

		} else {
			// Load from a URL
			inStream = new URL(url).openStream();
			if (inStream == null) {
				throw new FileNotFoundException("URL resource not found:" + url);
			}
		}
		if (inStream instanceof BufferedInputStream) {
			return inStream;
		} else {
			return new BufferedInputStream(inStream, 8192);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.resources.GenericResource#doCreate()
	 */
	@Override
	protected void doCreate() {

		// New feature: check the cache
		File cached = null;
		if (cacheDirectory != null && !streamed) {
			try {
				String cacheName;
				if (url.startsWith("classpath:")) {
					cacheName = url.substring(10) + ".wave." + ByteOrder.nativeOrder().toString();
				} else {
					cacheName = new URL(url).getFile() + ".wave." + ByteOrder.nativeOrder().toString();
				}
				cached = new File(cacheDirectory + File.separator + cacheName);
				if (cached.exists()) {
					wave = Wave.read(new BufferedInputStream(new FileInputStream(cached)));
					System.out.println("Loaded cached wave " + cacheName);
				}
			} catch (Exception e) {
				System.err.println("Failed to load " + url + " from cache.");
			}
			if (wave != null) {
				return;
			}
		}

		// Ok, failed to read from the cache. Try decoding the ogg.
		try {
			InputStream input = createStream();
	
			SyncState oy = new SyncState(); // sync and verify incoming physical
			// bitstream
			StreamState os = new StreamState(); // take physical pages, weld into a
			// logical stream of packets
			Page og = new Page(); // one Ogg bitstream page. Vorbis packets are
			// inside
			Packet op = new Packet(); // one raw packet of data for decode
	
			Info vi = new Info(); // struct that stores all the static vorbis
			// bitstream settings
			Comment vc = new Comment(); // struct that stores all the bitstream user
			// comments
			DspState vd = new DspState(); // central working state for the
			// packet->PCM decoder
			Block vb = new Block(vd); // local working space for packet->PCM
			// decode
	
			byte[] buffer;
			int bytes = 0;
			final ByteArrayOutputStream output = new ByteArrayOutputStream(1024 * 1024);
	
			// Decode setup
	
			oy.init(); // Now we can read pages
	
			// Whether we already have a frequency & type
			boolean inited = false;
	
			while (true) { // we repeat if the bitstream is chained
				int eos = 0;
	
				// grab some data at the head of the stream. We want the first page
				// (which is guaranteed to be small and only contain the Vorbis
				// stream initial header) We need the first page to get the stream
				// serialno.
	
				// submit a 4k block to libvorbis' Ogg layer
				int index = oy.buffer(4096);
				buffer = oy.data;
				bytes = input.read(buffer, index, 4096);
				oy.wrote(bytes);
	
				// Get the first page.
				if (oy.pageout(og) != 1) {
					// have we simply run out of data? If so, we're done.
					if (bytes < 4096) {
						break;
					}
	
					// error case. Must not be Vorbis data
					throw new Exception("Input does not appear to be an Ogg bitstream.");
				}
	
				// Get the serial number and set up the rest of decode.
				// serialno first; use it to set up a logical stream
				os.init(og.serialno());
	
				// extract the initial header from the first page and verify that
				// the
				// Ogg bitstream is in fact Vorbis data
	
				// I handle the initial header first instead of just having the code
				// read all three Vorbis headers at once because reading the initial
				// header is an easy way to identify a Vorbis bitstream and it's
				// useful to see that functionality seperated out.
	
				vi.init();
				vc.init();
				if (os.pagein(og) < 0) {
					// error; stream version mismatch perhaps
					throw new Exception("Error reading first page of Ogg bitstream data.");
				}
	
				if (os.packetout(op) != 1) {
					// no page? must not be vorbis
					throw new Exception("Error reading initial header packet.");
				}
	
				if (vi.synthesis_headerin(vc, op) < 0) {
					// error case; not a vorbis header
					throw new Exception("This Ogg bitstream does not contain Vorbis audio data.");
				}
	
				// At this point, we're sure we're Vorbis. We've set up the logical
				// (Ogg) bitstream decoder. Get the comment and codebook headers and
				// set up the Vorbis decoder
	
				// The next two packets in order are the comment and codebook
				// headers.
				// They're likely large and may span multiple pages. Thus we read
				// and submit data until we get our two packets, watching that no
				// pages are missing. If a page is missing, error out; losing a
				// header page is the only place where missing data is fatal. */
	
				int i = 0;
				while (i < 2) {
					while (i < 2) {
	
						int result = oy.pageout(og);
						if (result == 0) {
							break; // Need more data
							// Don't complain about missing or corrupt data yet.
							// We'll
							// catch it at the packet output phase
						}
	
						if (result == 1) {
							os.pagein(og); // we can ignore any errors here
							// as they'll also become apparent
							// at packetout
							while (i < 2) {
								result = os.packetout(op);
								if (result == 0) {
									break;
								}
								if (result == -1) {
									// Uh oh; data at some point was corrupted or
									// missing!
									// We can't tolerate that in a header. Die.
									throw new Exception("Corrupt secondary header. Exiting.");
								}
	
								vi.synthesis_headerin(vc, op);
								i++;
							}
						}
					}
	
					// no harm in not checking before adding more
					index = oy.buffer(4096);
					buffer = oy.data;
					bytes = input.read(buffer, index, 4096);
					if (bytes == 0 && i < 2) {
						throw new Exception("End of file before finding all Vorbis headers!");
					}
	
					oy.wrote(bytes);
				}
	
				// Determine Wave type. It's always 16 bit, we think.
				int newType, newFreq;
	
				switch (vi.channels) {
					case 1:
						newType = Wave.MONO_16BIT;
						break;
					case 2:
						newType = Wave.STEREO_16BIT;
						break;
					default:
						throw new Exception("Wave format only supports mono or stereo: this OGG has " + vi.channels + " channels.");
				}
				newFreq = vi.rate;
	
				if (inited) {
					if (newType != type || newFreq != frequency) {
						throw new Exception("Frequency and/or type has changed mid stream in chained OGG.");
					}
				} else {
					inited = true;
					type = newType;
					frequency = newFreq;
				}
	
				// At this point, break out if we're a stream
				if (streamed) {
					input.close();
					return;
				}
	
				convsize = 4096 / vi.channels;
	
				vd.synthesis_init(vi); // central decode state
				vb.init(vd); // local state for most of the decode
	
				float[][][] _pcm = new float[1][][];
				int[] _index = new int[vi.channels];
				// The rest is just a straight decode loop until end of stream
				while (eos == 0) {
					while (eos == 0) {
	
						int result = oy.pageout(og);
	
						if (result == 0) {
							break; // need more data
						}
	
						if (result == -1) { // missing or corrupt data at this page
							// position
							System.err.println("Corrupt or missing data in bitstream; continuing...");
						} else {
							os.pagein(og); // can safely ignore errors at
							// this point
							while (true) {
								result = os.packetout(op);
	
								if (result == 0) {
									break; // need more data
								}
								if (result == -1) { // missing or corrupt data at
									// this page position
									// no reason to complain; already complained
									// above
								} else {
									// we have a packet. Decode it
									int samples;
									if (vb.synthesis(op) == 0) { // test for
										// success!
										vd.synthesis_blockin(vb);
									}
	
									// **pcm is a multichannel float vector. In
									// stereo, for
									// example, pcm[0] is left, and pcm[1] is right.
									// samples is
									// the size of each channel. Convert the float
									// values
									// (-1.<=range<=1.) to whatever PCM format and
									// write it out
									final boolean bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
									while ((samples = vd.synthesis_pcmout(_pcm, _index)) > 0) {
										float[][] pcm = _pcm[0];
										int bout = (samples < convsize ? samples : convsize);
	
										// convert floats to 16 bit signed ints
										// (host order) and
										// interleave
	
										for (i = 0; i < vi.channels; i++) {
											int ptr = i << 1;
	
											int mono = _index[i];
	
											for (int j = 0; j < bout; j++) {
												int val = (int) (pcm[i][mono + j] * 32767.);
	
												// might as well guard against
												// clipping
												if (val > 32767) {
													val = 32767;
												} else if (val < -32768) {
													val = -32768;
												}
	
												if (val < 0) {
													val = val | 0x8000;
												}
												if (bigEndian) {
													convbuffer[ptr] = (byte) (val >>> 8);
													convbuffer[ptr + 1] = (byte) (val);
												} else {
													convbuffer[ptr] = (byte) (val);
													convbuffer[ptr + 1] = (byte) (val >>> 8);
												}
												ptr += (vi.channels) << 1;
											}
										}
	
										// Write converted samples to the conversion
										// buffer
										output.write(convbuffer, 0, 2 * vi.channels * bout);
										// Tell orbis how many samples were consumed
										vd.synthesis_read(bout);
									}
								}
							}
							if (og.eos() != 0) {
								eos = 1;
							}
						}
					}
					if (eos == 0) {
						index = oy.buffer(4096);
						buffer = oy.data;
						bytes = input.read(buffer, index, 4096);
						oy.wrote(bytes);
						if (bytes == 0) {
							eos = 1;
						}
					}
				}
	
				// clean up this logical bitstream; before exit we see if we're
				// followed by another [chained]
	
				os.clear();
	
				// ogg_page and ogg_packet structs always point to storage in
				// libvorbis. They're never freed or manipulated directly
	
				vb.clear();
				vd.clear();
				vi.clear(); // must be called last
			}
	
			// OK, clean up the framer
			oy.clear();
	
			ByteBuffer buf = ByteBuffer.allocateDirect(output.size()).order(ByteOrder.nativeOrder());
			buf.put(output.toByteArray());
			buf.flip();
	
			final int samples;
	
			if (!inited) {
				throw new Exception("Never found out the wave type!");
			}
	
			if (type == Wave.MONO_16BIT) {
				samples = output.size() / 2;
			} else {
				samples = output.size() / 4;
			}
			// wave = new Wave(samples, type, frequency, 0, 0, 0, buf);
			wave = new Wave(samples, type, frequency, new WrappedBuffer(buf));
	
			// Cache the wave
			if (cached != null) {
				try {
					FileOutputStream fos = new FileOutputStream(cached);
					Wave.write(wave, new BufferedOutputStream(fos));
					fos.flush();
					fos.close();
				} catch (Exception e) {
					System.err.println("Failed to cache " + url + " @ " + cached);
					e.printStackTrace(System.err);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
			
	}

	/**
	 * @return the Wave
	 * @throws Exception
	 */
	@Override
	public Wave getWave() throws Exception {
		if (streamed) {
			throw new Exception(this + " is a streamed ogg");
		}
		assert isCreated();
		if (wave.getData() == null) {
			// Reload disposed wave
			doCreate();
		}
		return wave;
	}

	/**
	 * Get the Ogg input stream
	 *
	 * @return OggInputStream
	 * @throws Exception if the resource has not yet been created
	 */
	@Override
	public InputStream getStream() throws Exception {
		if (!streamed) {
			throw new Exception(this + " is not a streamed ogg");
		}
		assert isCreated();
		return new OggInputStream(createStream());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.resources.GenericResource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		wave = null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getFrequency()
	 */
	@Override
	public int getFrequency() {
		return frequency;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getType()
	 */
	@Override
	public int getType() {
		return type;
	}

	/**
	 * Set the wave cache directory. When oggs are decoded we write the
	 * resulting wave files out to this cache directory to speed up startup next
	 * time round. If the wave cache directory is null then no cacheing is
	 * performed.
	 */
	public static void setCacheDirectory(String cache) {
		cacheDirectory = cache;
	}

}
