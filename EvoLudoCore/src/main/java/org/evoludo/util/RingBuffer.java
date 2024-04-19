//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For publications in any form, you are kindly requested to attribute the
// author and project as follows:
//
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Non-blocking ring buffer. Entries can be added continuously but only the most
 * recent entries are retained up to the buffers capacity. This is useful to
 * retain, for example, the most recent entries of a time series.
 * <p>
 * <strong>Note:</strong> arrays, primitives and java generics don't play well
 * together, which necessitates specialized instances of RingBuffer. In
 * particular, subclasses don't work either, which results in code redundancy in
 * the different implementations.
 * 
 * @author Christoph Hauert
 *
 * @param <T> data type of buffer
 */
public class RingBuffer<T> implements Iterable<T> {

	/**
	 * Array to hold buffer of <code>T</code> objects.
	 */
	ArrayList<T> buffer;

	/**
	 * Index of most recently added element in buffer.
	 */
	int bufferPtr;

	/**
	 * Buffer capacity, i.e. the maximum number of elements the buffer can contain.
	 */
	int bufferCapacity;

	/**
	 * The depth of a buffer containing arrays.
	 * <ul>
	 * <li>{@code &gt;0}: length of array entries
	 * <li>{@code 0}: entries are objects (not arrays)
	 * <li>{@code -1}: entries are arrays of different lengths
	 * <li>{@code -2}: buffer is mixture of objects and arrays
	 * </ul>
	 */
	int bufferDepth = 0;

	/**
	 * Create new ring buffer for storing up to <code>capacity</code> entries.
	 * 
	 * @param capacity maximum number of entries
	 * @throws IllegalArgumentException if <code>capacity&le;0</code>
	 */
	public RingBuffer(int capacity) throws IllegalArgumentException {
		setCapacity(capacity);
	}

	/**
	 * Set maximum number of entries in ring buffer to <code>capacity</code>. If the
	 * current buffer <code>size</code> exceeds the new <code>capacity</code> then
	 * entries with indices <code>capacity</code> through <code>size-1</code> are
	 * discarded. After setting the capacity the size of the buffer is at most
	 * <code>capacity</code>.
	 * 
	 * @param capacity maximum number of entries
	 * @throws IllegalArgumentException if <code>capacity&le;0</code>
	 */
	public void setCapacity(int capacity) throws IllegalArgumentException {
		if (capacity < 0)
			throw new IllegalArgumentException("RingBuffer capacity must be >0!");
		if (buffer == null) {
			buffer = new ArrayList<T>(capacity);
			bufferPtr = -1;
			bufferCapacity = capacity;
			return;
		}
		if (bufferCapacity > capacity) {
			if (0.8 * buffer.size() > capacity) {
				if (bufferPtr + capacity < bufferCapacity) {
					for (int n = 0; n < bufferPtr; n++)
						buffer.remove(0);
					if (capacity < buffer.size()) {
						int count = buffer.size() - capacity;
						for (int n = 0; n < count; n++)
							buffer.remove(capacity);
					}
					bufferPtr = -1;
				} else {
					int idx = (bufferPtr + capacity) % bufferCapacity;
					int count = bufferPtr - idx;
					for (int n = 0; n < count; n++)
						buffer.remove(idx);
					bufferPtr = idx;
				}
				buffer.trimToSize();
				bufferCapacity = capacity;
			}
		} else if (bufferCapacity < capacity) {
			// grow buffer - rotate elements such that most recent is in position 0
			for (int n = 0; n < bufferPtr; n++)
				buffer.add(buffer.remove(0));
			bufferPtr = (isEmpty() ? -1 : 0);
			bufferCapacity = capacity;
		}
	}

	/**
	 * @return capacity of ring buffer
	 */
	public int capacity() {
		return bufferCapacity;
	}

	/**
	 * Get the depth of array entries.
	 * 
	 * @return
	 *         <ul>
	 *         <li>{@code &gt;0}: length of array entries
	 *         <li>{@code 0}: entries are objects (not arrays)
	 *         <li>{@code -1}: entries are arrays of different lengths
	 *         <li>{@code -2}: buffer is mixture of objects and arrays
	 *         </ul>
	 */
	public int depth() {
		return bufferDepth;
	}

	/**
	 * @return number of entries in ring buffer
	 */
	public int size() {
		return buffer.size();
	}

	/**
	 * @return <code>true</code> if ring buffer is empty
	 */
	public boolean isEmpty() {
		return (buffer.size() == 0);
	}

	/**
	 * @return <code>true</code> if ring buffer is at capacity
	 */
	public boolean isFull() {
		return (buffer.size() == bufferCapacity);
	}

	/**
	 * Remove all entries from ring buffer. Reset buffer size to zero.
	 */
	public void clear() {
		buffer.clear();
		bufferPtr = -1;
		bufferDepth = 0;
	}

	/**
	 * Append new <code>entry</code> to ring buffer. If buffer is at capacity, the
	 * oldest entry is removed.
	 * 
	 * <h3>Important:</h3>
	 * Must create <em>copy</em> of data/array for adding to buffer. Otherwise,
	 * any subsequent changes to the data will also change the buffer. Copying
	 * of the entry cannot be reliably done in RingBuffer without reflection or
	 * other trickery.
	 * 
	 * @param entry to add to buffer
	 * 
	 * @see #replace(int, Object)
	 */
	public void append(T entry) {
		int size = buffer.size();
		int depth = arrayLength(entry);
		if (depth > 0) {
			if (size == 0)
				bufferDepth = depth;
			else if (bufferDepth != depth)
				bufferDepth = -1;
		} else {
			// mixing arrays an objects?
			if (bufferDepth != 0)
				bufferDepth = -2;
		}
		if (size < bufferCapacity) {
			buffer.add(++bufferPtr, entry);
		} else {
			bufferPtr = (bufferPtr + 1) % bufferCapacity;
			buffer.set(bufferPtr, entry);
		}
	}

	/**
	 * Helper method to deal with {@code entry} representing an array. Returns
	 * the length of the array or {@code -1} if it is not an array.
	 * 
	 * @param entry the buffer entry
	 * @return the length of the array or {@code -1} if {@code entry} is not an
	 *         array
	 * @throws IllegalArgumentException if unable to determine array type of
	 *                                  <code>entry</code>
	 */
	protected int arrayLength(T entry) {
		if (!entry.getClass().isArray())
			return -1;
		if (entry instanceof double[])
			return ((double[]) entry).length;
		if (entry instanceof Object[])
			return ((Object[]) entry).length;
		if (entry instanceof int[])
			return ((int[]) entry).length;
		if (entry instanceof float[])
			return ((float[]) entry).length;
		if (entry instanceof char[])
			return ((char[]) entry).length;
		if (entry instanceof long[])
			return ((long[]) entry).length;
		if (entry instanceof short[])
			return ((short[]) entry).length;
		if (entry instanceof byte[])
			return ((byte[]) entry).length;
		if (entry instanceof boolean[])
			return ((boolean[]) entry).length;
		throw new IllegalArgumentException("Unknown array type!");
	}

	/**
	 * Retrieve buffer entry at position <code>index</code> (without removing it).
	 * The most recently added entry has index 0, the one before index 1, etc. up
	 * the buffer size-1 (or its capacity-1, if the buffer is at capacity).
	 * 
	 * @param index of entry to retrieve
	 * @return buffer entry
	 * @throws IllegalArgumentException if <code>index&lt;0</code> or
	 *                                  <code>index&gt;size-1</code>.
	 */
	public T get(int index) throws IllegalArgumentException {
		if (index < 0 || index >= buffer.size())
			throw new IllegalArgumentException("Index (" + index + ") out of bounds [0, " + (buffer.size() - 1) + "]!");
		return buffer.get((bufferPtr - index + buffer.size()) % buffer.size());
	}

	/**
	 * Return first/oldest buffer entry.
	 * 
	 * @return first buffer entry or <code>null</code> if buffer is empty
	 */
	public T first() {
		if (isEmpty())
			return null;
		return get((bufferPtr - 1 + buffer.size()) % buffer.size());
	}

	/**
	 * Return last/most recent buffer entry.
	 * 
	 * @return first buffer entry or <code>null</code> if buffer is empty
	 */
	public T last() {
		if (isEmpty())
			return null;
		return get(0);
	}

	/**
	 * Replace buffer entry at position <code>index</code> with <code>entry</code>.
	 * Return old entry at position <code>index</code>. The most recently added
	 * entry has index 0, the one before index 1, etc. up the buffer size-1 (or its
	 * capacity-1, if the buffer is at capacity).
	 * 
	 * <h3>Important:</h3>
	 * Must create <em>copy</em> of data/array for including in buffer. Otherwise,
	 * any subsequent changes to the data will also change the buffer. Copying
	 * of the entry cannot be reliably done in RingBuffer without reflection or
	 * other trickery.
	 * 
	 * @param index of entry to replace
	 * @param entry replacement
	 * @return previous buffer entry
	 * @throws IllegalArgumentException if <code>index&lt;0</code> or
	 *                                  <code>index&gt;size-1</code>.
	 * 
	 * @see #append(Object)
	 */
	public T replace(int index, T entry) throws IllegalArgumentException {
		int size = buffer.size();
		if (index < 0 || index >= size)
			throw new IllegalArgumentException("Index (" + index + ") out of bounds [0, " + (buffer.size() - 1) + "]!");
		index = (bufferPtr - index + size) % size;
		T old = buffer.remove(index);
		buffer.add(index, entry);
		int depth = arrayLength(entry);
		if (depth > 0) {
			if (size == 1)
				bufferDepth = depth;
			else if (bufferDepth != depth)
				bufferDepth = -1;
		} else {
			// mixing arrays an objects?
			if (bufferDepth != 0)
				bufferDepth = -2;
		}
		return old;
	}

	/**
	 * Replace most recent entry in ring buffer with <code>entry</code>.
	 * 
	 * <h3>Important:</h3>
	 * Must create <em>copy</em> of data/array for including in buffer. Otherwise,
	 * any subsequent changes to the data will also change the buffer. Copying
	 * of the entry cannot be reliably done in RingBuffer without reflection or
	 * other trickery.
	 * 
	 * @param entry replacement
	 * @return previous buffer entry
	 * 
	 * @see #replace(int, Object)
	 */
	public T replace(T entry) {
		return replace(0, entry);
	}

	/**
	 * Iterates backwards over all elements in this buffer starting with the most
	 * recent entry.
	 */
	private class BckItr implements Iterator<T> {

		/**
		 * Index of current element in Iterator.
		 */
		int cursor = 0;

		@Override
		public boolean hasNext() {
			return cursor < buffer.size();
		}

		@Override
		public T next() {
			int size = buffer.size();
			return buffer.get((bufferPtr - (cursor++) + size) % size);
		}
	}

	/**
	 * Iterates forward over all elements in this buffer starting with the oldest
	 * entry.
	 */
	private class FwdItr implements Iterator<T> {

		/**
		 * Index of current element in Iterator.
		 */
		int cursor = buffer.size();

		@Override
		public boolean hasNext() {
			return cursor > 0;
		}

		@Override
		public T next() {
			int size = buffer.size();
			return buffer.get((bufferPtr - (--cursor) + size) % size);
		}
	}

	/**
	 * Iterates forward over all elements in this buffer starting with the oldest
	 * entry.
	 */
	private class LstItr extends FwdItr implements ListIterator<T> {

		/**
		 * Creates a new {@code ListIterator} over all elements in this buffer starting
		 * with the oldest entry.
		 */
		public LstItr() {
			super();
		}

		/**
		 * Creates a new {@code ListIterator} over all elements in this buffer starting
		 * with the entry at {@code index}.
		 */
		public LstItr(int index) {
			super();
			cursor = index;
		}

		@Override
		public boolean hasPrevious() {
			return cursor > 0;
		}

		@Override
		public T previous() {
			return buffer.get((bufferPtr - (--cursor) + buffer.size()) % buffer.size());
		}

		@Override
		public int nextIndex() {
			return cursor;
		}

		@Override
		public int previousIndex() {
			return cursor - 1;
		}

		@Override
		public void add(T element) {
			throw new UnsupportedOperationException("Inserting elements in RingBuffer not supported!");
		}

		@Override
		public void set(T element) {
			replace(cursor, element);
		}

		@Override
		public void remove() {
			buffer.remove((bufferPtr + cursor) % buffer.size());
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new BckItr();
	}

	/**
	 * Returns an iterator over all elements in this buffer in chronological order.
	 * 
	 * @return the forward iterator
	 */
	public Iterator<T> ordered() {
		return new FwdItr();
	}

	/**
	 * Returns a list iterator over all elements in this buffer in chronological
	 * order.
	 * 
	 * @return the forward list iterator
	 */
	public ListIterator<T> listIterator() {
		return new LstItr();
	}

	/**
	 * Returns a list iterator over all elements in this buffer in chronological
	 * order starting with entry at {@code index}.
	 * 
	 * @param index the index of the first element to be returned
	 * @return the forward list iterator
	 */
	public ListIterator<T> listIterator(int index) {
		return new LstItr(index);
	}
}
