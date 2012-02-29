package net.puppygames.applet;

import java.util.ArrayList;

/**
 * A pool of objects for reuse
 * @param <T>
 */
public class SimplePool<T> implements Pool<T> {

	private final Factory<T> factory;
	private final int maxSize;

	private final ArrayList<T> pool = new ArrayList<T>(4);

	/**
	 * C'tor
	 * @param factory Constructs new instances when necessary
	 * @param maxSize Maximum size this pool should grow to, or 0, for unlimited
	 */
	public SimplePool(Factory<T> factory, int maxSize) {
		this.factory = factory;
		this.maxSize = maxSize;
	}

	@Override
    public T obtain() {
		if (pool.size() == 0) {
			return factory.createNew();
		} else {
			return pool.remove(pool.size() - 1);
		}
	}

	@Override
    public void release(T obj) {
		if (Game.DEBUG) {
			if (obj == null) {
				throw new IllegalArgumentException("Can't release null to "+this);
			}
			if (pool.contains(obj)) {
				throw new IllegalStateException(obj+" already present in pool "+this);
			}
		}
		if (maxSize == 0 || pool.size() < maxSize) {
			pool.add(obj);
		}
	}
}
