package net.puppygames.applet;

/**
 * Object pooling. Yuk! But sometimes a necessity.
 * @param <T>
 */
public interface Pool<T> {

	T obtain();

	void release(T obj);

}
