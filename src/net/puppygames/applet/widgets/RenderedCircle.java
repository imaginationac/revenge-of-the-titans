package net.puppygames.applet.widgets;

import java.util.HashMap;
import java.util.Map;

import net.puppygames.applet.Factory;
import net.puppygames.applet.Pool;
import net.puppygames.applet.SimplePool;

import com.shavenpuppy.jglib.sprites.SimpleRenderable;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders a circle using a triangle strip.
 */
public class RenderedCircle implements SimpleRenderable {

	/** Number of segments per radius */
	private static final float DEFAULT_SEGMENTS_PER_RADIUS = 0.25f;

	/** Minimum number of segments */
	private static final int MIN_SEGMENTS = 8;

	/** Maximum number of segments */
	private static final int MAX_SEGMENTS = 256;

	/** Pools of indicies. Note: could be optimised to use sparse int mapping. */
	private static Map<Integer, Pool<short[]>> INDEX_POOL = new HashMap<Integer, Pool<short[]>>();

	/** Segments per radius */
	private float segmentsPerRadius = DEFAULT_SEGMENTS_PER_RADIUS;

	/** Location */
	private float x, y;

	/** Radius */
	private float radius;

	/** Thickness */
	private float thickness = 1.0f;

	/**
	 * C'tor
	 */
	public RenderedCircle() {
	}

	/**
	 * C'tor
     * @param x
     * @param y
     * @param radius
     * @param thickness
     */
    public RenderedCircle(float x, float y, float radius, float thickness) {
	    this.x = x;
	    this.y = y;
	    this.radius = radius;
	    this.thickness = thickness;
    }

    public void setSegmentsPerRadius(float segmentsPerRadius) {
	    this.segmentsPerRadius = segmentsPerRadius;
    }

    public void setRadius(float radius) {
	    this.radius = radius;
    }

    public void setThickness(float thickness) {
	    this.thickness = thickness;
    }

    public void setLocation(float x, float y) {
	    this.x = x;
	    this.y = y;
    }

	@Override
	public void render(SimpleRenderer renderer) {
		if (radius <= 0.0f || thickness <= 0.0f) {
			return;
		}

		int segments = (int) Math.max(MIN_SEGMENTS, Math.max(MAX_SEGMENTS, radius * segmentsPerRadius));

		// Find indices in the pool
		short[] indices = obtainIndices(segments * 2 + 2);
		short offset = renderer.getVertexOffset();
		int index = 0;
		for (int i = 0; i <= segments; i ++) {
			double angle = i * Math.PI * 2.0 / segments;
			renderer.glTexCoord2f(0.0f, 0.0f);
			renderer.glVertex2f(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
			renderer.glTexCoord2f(1.0f, 0.0f);
			renderer.glVertex2f(x + (float) Math.cos(angle) * (radius - thickness), y + (float) Math.sin(angle) * (radius - thickness));
			indices[index ++] = (short) (offset + index);
		}
		indices[index ++] = offset;
		indices[index ++] = (short) (offset + 1);
		renderer.glRender(GL_TRIANGLE_STRIP, indices);

		// Release indices back to the pool
		releaseIndices(indices);
	}

	private static short[] obtainIndices(final int size) {
		Integer s = new Integer(size);
		Pool<short[]> pooled = INDEX_POOL.get(s);
		if (pooled == null) {
			pooled = new SimplePool<short[]>(new Factory<short[]>() {
				@Override
				public short[] createNew() {
				    return new short[size];
				}
			}, 1);
			INDEX_POOL.put(s, pooled);
		}
		return pooled.obtain();
	}

	private static void releaseIndices(short[] indices) {
		Integer s = new Integer(indices.length);
		Pool<short[]> pooled = INDEX_POOL.get(s);
		pooled.release(indices);
	}

}
