/**
 *
 */
package com.shavenpuppy.jglib.sprites;

import com.shavenpuppy.jglib.util.FloatList;
import com.shavenpuppy.jglib.util.ShortList;

/**
 * Really basic geometry data which is used by {@link Style}
 */
public class GeometryData {

	private final FloatList vertexData;
	private final ShortList indexData;

	/**
	 * C'tor
     * @param vertexData
     * @param indexData
     */
    public GeometryData(FloatList vertexData, ShortList indexData) {
	    this.vertexData = vertexData;
	    this.indexData = indexData;
    }

    public void clear() {
    	vertexData.clear();
    	indexData.clear();
    }

	public FloatList getVertexData() {
	    return vertexData;
    }

	public ShortList getIndexData() {
	    return indexData;
    }

}
