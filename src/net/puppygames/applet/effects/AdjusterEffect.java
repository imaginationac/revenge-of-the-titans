/**
 * 
 */
package net.puppygames.applet.effects;



/**
 * Adjust to a new value
 */
public abstract class AdjusterEffect extends Effect {

	private final int rate;
	
	private int current;
	private int target;
	private boolean finished;
	
	public AdjusterEffect(int rate, int initial, int target) {
		this.rate = rate;
		this.current = initial;
		this.target = target;
	}
	
	public void setTarget(int target) {
		this.target = target;
	}
	
	public int getTarget() {
		return target;
	}
	
	public void setCurrent(int current) {
		this.current = current;
	}
	
	public int getCurrent() {
		return current;
	}

	@Override
	protected void doTick() {
		if (current < target) {
			current = Math.min(current + rate, target);
		} else if (current > target) {
			current = Math.max(current - rate, target);
		}
	}
	
	@Override
	public void finish() {
		current = target;
		remove();
	}
	
	@Override
	protected void doRemove() {
		finished = true;
	}

	@Override
	public boolean isActive() {
		return !finished;
	}

}
