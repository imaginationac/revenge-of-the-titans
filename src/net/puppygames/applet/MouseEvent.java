package net.puppygames.applet;

import org.lwjgl.input.Mouse;

/**
 * Mouse events
 */
public class MouseEvent {

	private int x, y, dwheel, dx, dy, button;
	private boolean buttonDown;
	private long time;

	public MouseEvent() {
	}

	public void fromMouse() {
		dx = Mouse.getEventDX();
		dy = Mouse.getEventDY();
		dwheel = Mouse.getEventDWheel();
		x = Mouse.getEventX();
		y = Mouse.getEventY();
		button = Mouse.getEventButton();
		buttonDown = Mouse.getEventButtonState();
		time = Mouse.getEventNanoseconds();
	}

	@Override
    public String toString() {
	    StringBuilder builder = new StringBuilder();
	    builder.append("MouseEvent [time=");
	    builder.append(time);
	    builder.append(", x=");
	    builder.append(x);
	    builder.append(", y=");
	    builder.append(y);
	    builder.append(", dx=");
	    builder.append(dx);
	    builder.append(", dy=");
	    builder.append(dy);
	    builder.append(", dwheel=");
	    builder.append(dwheel);
	    builder.append(", button=");
	    builder.append(button);
	    builder.append(", buttonDown=");
	    builder.append(buttonDown);
	    builder.append("]");
	    return builder.toString();
    }

	@Override
    public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + button;
	    result = prime * result + (buttonDown ? 1231 : 1237);
	    result = prime * result + dwheel;
	    result = prime * result + dx;
	    result = prime * result + dy;
	    result = prime * result + (int) (time ^ (time >>> 32));
	    result = prime * result + x;
	    result = prime * result + y;
	    return result;
    }

	@Override
    public boolean equals(Object obj) {
	    if (this == obj) {
		    return true;
	    }
	    if (obj == null) {
		    return false;
	    }
	    if (getClass() != obj.getClass()) {
		    return false;
	    }
	    MouseEvent other = (MouseEvent) obj;
	    if (button != other.button) {
		    return false;
	    }
	    if (buttonDown != other.buttonDown) {
		    return false;
	    }
	    if (dwheel != other.dwheel) {
		    return false;
	    }
	    if (dx != other.dx) {
		    return false;
	    }
	    if (dy != other.dy) {
		    return false;
	    }
	    if (time != other.time) {
		    return false;
	    }
	    if (x != other.x) {
		    return false;
	    }
	    if (y != other.y) {
		    return false;
	    }
	    return true;
    }

	public int getX() {
    	return x;
    }

	public int getY() {
    	return y;
    }

	public int getDWheel() {
    	return dwheel;
    }

	public int getDX() {
    	return dx;
    }

	public int getDY() {
    	return dy;
    }

	public int getButton() {
    	return button;
    }

	public boolean isButtonDown() {
    	return buttonDown;
    }

	public long getTime() {
	    return time;
    }
}
