package net.puppygames.applet.widgets;

import org.lwjgl.util.ReadableColor;

import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.sprites.SimpleRenderable;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders a ring shape
 */
public class Ring implements SimpleRenderable {

	private static final int DEFAULT_STEPS = 128;

	private ReadableColor color = ReadableColor.WHITE;
	private float thickness = 1.0f;
	private int steps;

	/** "Dash" length */
	private float dash = 1.0f;
	private float radius;
	private float x, y;
	private int alpha = 255;

	private short[] indices;

	/**
	 * C'tor
	 */
	public Ring() {
		setSteps(DEFAULT_STEPS);
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

	public float getRadius() {
	    return radius;
    }

	public void setAlpha(int alpha) {
	    this.alpha = alpha;
    }

	public int getAlpha() {
	    return alpha;
    }

	public void setLocation(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void setSteps(int steps) {
		if (this.steps != steps) {
			this.steps = steps;
			indices = null;
			indices = new short[steps * 2 + 2];
		}
    }

	public void setThickness(float thickness) {
	    this.thickness = thickness;
    }

	public void setColor(ReadableColor color) {
	    this.color = color;
    }

	public int getSteps() {
	    return steps;
    }

	public ReadableColor getColor() {
	    return color;
    }

	public float getThickness() {
	    return thickness;
    }

	public float getX() {
	    return x;
    }

	public float getY() {
	    return y;
    }

	public void setDash(float dash) {
	    this.dash = dash;
    }

	public float getDash() {
	    return dash;
    }

	@Override
	public void render(SimpleRenderer renderer) {
		if (radius <= 0.0f || alpha <= 0) {
			return;
		}

		short idx = renderer.getVertexOffset();
		ColorUtil.setGLColorPre(color, alpha, renderer);
		int count = 0;
		for (int i = 0; i <= steps; i ++) {
			double angle = i * Math.PI * 2.0 / steps;
			float tx = (float) ((i * Math.PI * 2.0 * radius) / (steps * dash));
			renderer.glTexCoord2f(tx, 0.0f);
			renderer.glVertex2f(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
			renderer.glTexCoord2f(tx, 1.0f);
			renderer.glVertex2f(x + (float) Math.cos(angle) * (radius - thickness), y + (float) Math.sin(angle) * (radius - thickness));

			indices[count] = (short) (idx + count);
			indices[count + 1] = (short) (idx + count + 1);
			count += 2;
		}
//		indices[count] = idx;
//		indices[count + 1] = (short) (idx + 1);
		renderer.glRender(GL_TRIANGLE_STRIP, indices);
	}

}
