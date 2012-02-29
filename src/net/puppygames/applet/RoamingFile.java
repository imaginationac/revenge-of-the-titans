/**
 *
 */
package net.puppygames.applet;

import java.io.File;

import net.puppygames.steam.Steam;

/**
 * {@link RoamingFile} is a cover for Files that might be either implemented using the Steam cloud, or in the local filesystem.
 */
public class RoamingFile {

	private final String path;

	public RoamingFile(String path) {
		this.path = path;
	}

	public boolean exists() {
		if (Game.isUsingSteamCloud()) {
			return Steam.getRemoteStorage().fileExists(path);
		} else {
			return new File(path).exists();
		}
	}

	public boolean delete() {
		if (Game.isUsingSteamCloud()) {
			if (Game.DEBUG) {
				System.out.println("Deleting steam file "+path);
			}
			return Steam.getRemoteStorage().fileDelete(path);
		} else {
			return new File(path).delete();
		}
	}

	/**
	 * @return the relative path prefixed with the directory prefix
	 */
	public String getPath() {
		return path;
	}

	@Override
    public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((path == null) ? 0 : path.hashCode());
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
	    RoamingFile other = (RoamingFile) obj;
	    if (path == null) {
		    if (other.path != null) {
			    return false;
		    }
	    } else if (!path.equals(other.path)) {
		    return false;
	    }
	    return true;
    }

	@Override
    public String toString() {
	    StringBuilder builder = new StringBuilder();
	    builder.append("RoamingFile [");
	    if (path != null) {
		    builder.append("path=");
		    builder.append(path);
	    }
	    builder.append("]");
	    return builder.toString();
    }

}
