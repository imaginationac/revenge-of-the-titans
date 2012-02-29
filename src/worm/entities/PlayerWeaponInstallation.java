package worm.entities;

public interface PlayerWeaponInstallation {

	/**
	 * @return true if this installation is currently aiming at aerial targets (only really applies to the Laser Turret and
	 * buffed tanks)
	 */
	boolean isFiringAtAerialTargets();

}
