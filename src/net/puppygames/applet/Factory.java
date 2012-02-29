/**
 *
 */
package net.puppygames.applet;

/**
 * Pooled object factory for use by a {@link Pool}
 */
public interface Factory<T> {

	T createNew();

}
