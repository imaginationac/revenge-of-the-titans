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
package com.shavenpuppy.jglib.algorithms;

/**
 * Radix Sort algorithm.
 *
 * Creation date: (12/21/00 4:14:46 PM)
 * @author: cas
 */
public final class RadixSort {
	/**
	 * The RadixSort can sort things which are Sortable.
	 * They simply have to return an int which is their sort order.
	 */
	public interface Sortable {
		public int sortOrder();
	}

	private final int[] mHistogram = new int[1024]; // Counters for each byte
	private final int[] mOffset = new int[256]; // Offsets (nearly a cumulative distribution function)
	private int mCurrentSize = -1; // Current size of the indices list
	private int[] mIndices; // Two lists, swapped each pass
	private int[] mIndices2;
	private int[] sortOrder; // For sorting Sortables
	private int[] p;
	/**
	 * RadixSort constructor comment.
	 */
	public RadixSort()
	{
		super();
	}

	public RadixSort(int initialLength)
	{
		resizeTo(initialLength);
	}

	private void resizeTo(int length)
	{
		if (length > mCurrentSize)
		{
			mIndices = new int[length];
			mIndices2 = new int[length];
			sortOrder = new int[length];

			p = new int[length << 2];
			mCurrentSize = length;

			// Initialize indices so that the input buffer is read in sequential order
			resetIndices();
		}
	}

	/**
	 * Returns the sorted indexes.
	 * Creation date: (22/12/2000 01:05:01)
	 */
	public int[] getIndices() {
		return mIndices;
	}
	/**
	 * Resets the indices. Returns a self-reference.
	 * Creation date: (12/21/00 4:26:11 PM)
	 */
	public RadixSort resetIndices() {
		for (int i = 0; i < mCurrentSize; i++) {
	        mIndices[i] = i;
        }

		return this;
	}
	/**
	 * Main sort routine
	 * Input	:	input			a list of integer values to sort
	 * Output	:	mIndices,		a list of indices in sorted order, i.e. in the order you may process your data
	 * Return	:	Self-Reference
	 * Exception:	-
	 * Remark	:	this one is for integer values
	 * Creation date: (12/21/00 4:29:01 PM)
	 */
	public RadixSort sort(int[] input) {
		return sort(input, input.length);
	}
	/**
	 * Main sort routine
	 * Input	:	input			a list of integer values to sort
	 * Output	:	mIndices,		a list of indices in sorted order, i.e. in the order you may process your data
	 * Return	:	Self-Reference
	 * Exception:	-
	 * Remark	:	this one is for integer values
	 * Creation date: (12/21/00 4:29:01 PM)
	 */
	public RadixSort sort(int[] input, int length) {
		if (input == null) {
	        throw new IllegalArgumentException("Null array input to radix sort");
        }

		if (length == 0) {
	        return this;
        }

		// Resize lists if needed
		resizeTo(length);

		// Clear counters
		java.util.Arrays.fill(mHistogram, 0);

		// Create histograms (counters). Counters for all passes are created in one run.
		// Pros:	read input buffer once instead of four times
		// Cons:	mHistogram is 4Kb instead of 1Kb
		// We must take care of signed/unsigned values for temporal coherence.... I just
		// have 2 code paths even if just a single opcode changes. Self-modifying code, someone?

		// Temporal coherence
		boolean alreadySorted = true; // Optimism...
		int iC = 0;
		// Prepare to count.

		// We must get the incoming integer array as a sequence of bytes
		int pC = 0;
		for (int i = 0; i < length; i++) {
			p[pC++] = input[i] & 0xFF;
			p[pC++] = (input[i] >> 8) & 0xFF;
			p[pC++] = (input[i] >> 16) & 0xFF;
			p[pC++] = (input[i] >> 24) & 0xFF;
		}
		int pLen = pC;
		pC = 0;

		int h0 = 0; // Histogram for first pass (LSB)
		int h1 = 256; // Histogram for second pass
		int h2 = 512; // Histogram for third pass
		int h3 = 768; // Histogram for last pass (MSB)

		// Temporal coherence
		int PrevVal = input[mIndices[0]];

		while (pC != pLen) {
			// Temporal coherence
			int Val = input[mIndices[iC++]]; // Read input buffer in previous sorted order
			if (Val < PrevVal) {
				alreadySorted = false;
				break;
			} // Check whether already sorted or not
			PrevVal = Val; // Update for next iteration

			// Create histograms
			mHistogram[h0 + p[pC++]]++;
			mHistogram[h1 + p[pC++]]++;
			mHistogram[h2 + p[pC++]]++;
			mHistogram[h3 + p[pC++]]++;
		}

		// If all input values are already sorted, we just have to return and leave the previous list unchanged.
		// That way the routine may take advantage of temporal coherence, for example when used to sort transparent faces.
		if (alreadySorted) {
	        return this;
        }

		// otherwise continue building histograms
		while (pC != pLen) {
			// Create histograms
			mHistogram[h0 + p[pC++]]++;
			mHistogram[h1 + p[pC++]]++;
			mHistogram[h2 + p[pC++]]++;
			mHistogram[h3 + p[pC++]]++;
		}

		// Compute #negative values involved if needed
		int NbNegativeValues = 0;
		// An efficient way to compute the number of negatives values we'll have to deal with is simply to sum the 128
		// last values of the last histogram. Last histogram because that's the one for the Most Significant Byte,
		// responsible for the sign. 128 last values because the 128 first ones are related to positive numbers.
		for (int i = 128; i < 256; i++)
		 {
	        NbNegativeValues += mHistogram[768 + i]; // 768 for last histogram, 128 for negative part
        }

		// Radix sort, j is the pass number (0=LSB, 3=MSB)
		for (int j = 0; j < 4; j++) {
			// Reset flag. The sorting pass is supposed to be performed. (default)
			boolean PerformPass = true;

			// Check pass validity [some cycles are lost there in the generic case, but that's ok, just a little loop]
			for (int i = 0; i < 256; i++) {
				// If all values have the same byte, sorting is useless. It may happen when sorting bytes or words instead of dwords.
				// This routine actually sorts words faster than dwords, and bytes faster than words. Standard running time (O(4*n))is
				// reduced to O(2*n) for words and O(n) for bytes. Running time for floats depends on actual values...
				int CurCount = mHistogram[(j << 8) + i];
				if (CurCount == length) {
					PerformPass = false;
					break;
				}
				// If at least one count is not null, we suppose the pass must be done. Hence, this test takes very few CPU time in the generic case.
				if (CurCount != 0) {
	                break;
                }
			}

			// Sometimes the fourth (negative) pass is skipped because all numbers are negative and the MSB is 0xFF (for example). This is
			// not a problem, numbers are correctly sorted anyway.
			if (PerformPass) {
				if (j != 3) {
					// Here we deal with positive values only

					// Create offsets
					mOffset[0] = 0;
					for (int i = 1; i < 256; i++) {
	                    mOffset[i] = mOffset[i - 1] + mHistogram[(j << 8) + i - 1];
                    }
				} else {
					// This is a special case to correctly handle negative integers. They're sorted in the right order but at the wrong place.

					// Create biased offsets, in order for negative numbers to be sorted as well
					mOffset[0] = NbNegativeValues; // First positive number takes place after the negative ones
					for (int i = 1; i < 128; i++)
					 {
	                    mOffset[i] = mOffset[i - 1] + mHistogram[(j << 8) + i - 1]; // 1 to 128 for positive numbers
                    }

					// Fixing the wrong place for negative values
					mOffset[128] = 0;
					for (int i = 129; i < 256; i++) {
	                    mOffset[i] = mOffset[i - 1] + mHistogram[(j << 8) + i - 1];
                    }
				}

				// Perform Radix Sort
				pC = j;
				for (int i = 0; i < length; i++) {
					int id = mIndices[i];
					int pindex = (id << 2) + pC;
					int mOffsetindex = p[pindex];
					int mIndices2index = mOffset[mOffsetindex]++;
					mIndices2[mIndices2index] = id;
					//				mIndices2[mOffset[p[(id<<2) + pC]]++] = id;
				}

				// Swap pointers for next pass
				int[] Tmp = mIndices;
				mIndices = mIndices2;
				mIndices2 = Tmp;
			}
		}
		return this;
	}
	/**
	 * Main sort routine
	 * Input	:	input			a list of integer values to sort
	 * Output	:	mIndices,		a list of indices in sorted order, i.e. in the order you may process your data
	 * Return	:	Self-Reference
	 * Exception:	-
	 * Remark	:	this one is for integer values
	 * Creation date: (12/21/00 4:29:01 PM)
	 */
	public RadixSort sort(Sortable[] input, int length) {
		if (input == null) {
	        throw new IllegalArgumentException("Null array input to radix sort");
        }

		if (length == 0) {
	        return this;
        }

		// Resize lists if needed
		resizeTo(length);

		// Clear counters
		java.util.Arrays.fill(mHistogram, 0);

		// Create histograms (counters). Counters for all passes are created in one run.
		// Pros:	read input buffer once instead of four times
		// Cons:	mHistogram is 4Kb instead of 1Kb
		// We must take care of signed/unsigned values for temporal coherence.... I just
		// have 2 code paths even if just a single opcode changes. Self-modifying code, someone?

		// Temporal coherence
		boolean AlreadySorted = true; // Optimism...
		int iC = 0;
		// Prepare to count.

		// We must get the incoming integer array as a sequence of bytes
		int pC = 0;
		for (int i = 0; i < length; i++) {
			int so = input[i].sortOrder();
			sortOrder[i] = so;
			p[pC++] = so & 0xFF;
			p[pC++] = (so >> 8) & 0xFF;
			p[pC++] = (so >> 16) & 0xFF;
			p[pC++] = (so >> 24) & 0xFF;
		}
		int pLen = pC;
		pC = 0;

		int h0 = 0; // Histogram for first pass (LSB)
		int h1 = 256; // Histogram for second pass
		int h2 = 512; // Histogram for third pass
		int h3 = 768; // Histogram for last pass (MSB)

		// Temporal coherence
		int PrevVal = sortOrder[mIndices[0]];

		while (pC != pLen) {
			// Temporal coherence
			int Val = sortOrder[mIndices[iC++]]; // Read input buffer in previous sorted order
			if (Val < PrevVal) {
				AlreadySorted = false;
				break;
			} // Check whether already sorted or not
			PrevVal = Val; // Update for next iteration

			// Create histograms
			mHistogram[h0 + p[pC++]]++;
			mHistogram[h1 + p[pC++]]++;
			mHistogram[h2 + p[pC++]]++;
			mHistogram[h3 + p[pC++]]++;
		}

		// If all input values are already sorted, we just have to return and leave the previous list unchanged.
		// That way the routine may take advantage of temporal coherence, for example when used to sort transparent faces.
		if (AlreadySorted) {
	        return this;
        }

		// otherwise continue building histograms
		while (pC != pLen) {
			// Create histograms
			mHistogram[h0 + p[pC++]]++;
			mHistogram[h1 + p[pC++]]++;
			mHistogram[h2 + p[pC++]]++;
			mHistogram[h3 + p[pC++]]++;
		}

		// Compute #negative values involved if needed
		int NbNegativeValues = 0;
		// An efficient way to compute the number of negatives values we'll have to deal with is simply to sum the 128
		// last values of the last histogram. Last histogram because that's the one for the Most Significant Byte,
		// responsible for the sign. 128 last values because the 128 first ones are related to positive numbers.
		for (int i = 128; i < 256; i++)
		 {
	        NbNegativeValues += mHistogram[768 + i]; // 768 for last histogram, 128 for negative part
        }

		// Radix sort, j is the pass number (0=LSB, 3=MSB)
		for (int j = 0; j < 4; j++) {
			// Reset flag. The sorting pass is supposed to be performed. (default)
			boolean PerformPass = true;

			// Check pass validity [some cycles are lost there in the generic case, but that's ok, just a little loop]
			for (int i = 0; i < 256; i++) {
				// If all values have the same byte, sorting is useless. It may happen when sorting bytes or words instead of dwords.
				// This routine actually sorts words faster than dwords, and bytes faster than words. Standard running time (O(4*n))is
				// reduced to O(2*n) for words and O(n) for bytes. Running time for floats depends on actual values...
				int CurCount = mHistogram[(j << 8) + i];
				if (CurCount == length) {
					PerformPass = false;
					break;
				}
				// If at least one count is not null, we suppose the pass must be done. Hence, this test takes very few CPU time in the generic case.
				if (CurCount != 0) {
	                break;
                }
			}

			// Sometimes the fourth (negative) pass is skipped because all numbers are negative and the MSB is 0xFF (for example). This is
			// not a problem, numbers are correctly sorted anyway.
			if (PerformPass) {
				if (j != 3) {
					// Here we deal with positive values only

					// Create offsets
					mOffset[0] = 0;
					for (int i = 1; i < 256; i++) {
	                    mOffset[i] = mOffset[i - 1] + mHistogram[(j << 8) + i - 1];
                    }
				} else {
					// This is a special case to correctly handle negative integers. They're sorted in the right order but at the wrong place.

					// Create biased offsets, in order for negative numbers to be sorted as well
					mOffset[0] = NbNegativeValues; // First positive number takes place after the negative ones
					for (int i = 1; i < 128; i++)
					 {
	                    mOffset[i] = mOffset[i - 1] + mHistogram[(j << 8) + i - 1]; // 1 to 128 for positive numbers
                    }

					// Fixing the wrong place for negative values
					mOffset[128] = 0;
					for (int i = 129; i < 256; i++) {
	                    mOffset[i] = mOffset[i - 1] + mHistogram[(j << 8) + i - 1];
                    }
				}

				// Perform Radix Sort
				pC = j;
				for (int i = 0; i < length; i++) {
					int id = mIndices[i];
					int pindex = (id << 2) + pC;
					int mOffsetindex = p[pindex];
					int mIndices2index = mOffset[mOffsetindex]++;
					mIndices2[mIndices2index] = id;
					//				mIndices2[mOffset[p[(id<<2) + pC]]++] = id;
				}

				// Swap pointers for next pass
				int[] Tmp = mIndices;
				mIndices = mIndices2;
				mIndices2 = Tmp;
			}
		}
		return this;
	}
}
