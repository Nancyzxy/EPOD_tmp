/*
*      _______                       _        ____ _     _
*     |__   __|                     | |     / ____| |   | |
*        | | __ _ _ __ ___  ___  ___| |    | (___ | |___| |
*        | |/ _` | '__/ __|/ _ \/ __| |     \___ \|  ___  |
*        | | (_| | |  \__ \ (_) \__ \ |____ ____) | |   | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|   |_|
*                                                         
* -----------------------------------------------------------
*
*  TarsosLSH is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info    : http://tarsos.0110.be/tag/TarsosLSH
*  Github  : https://github.com/JorenSix/TarsosLSH
*  Releases: http://tarsos.0110.be/releases/TarsosLSH/
* 
*/

package be.tarsos.lsh.families;

import DataStructure.Vector;

/**
 * <p>
 * Calculates the intersection of one vector with an other vector. Intersection
 * is normally between 0 and 1. 1 meaning 100% overlap. To make it a distance
 * measure, 1 - intersection is done, so 1 becomes no intersection, 0 total
 * overlap.
 * </p>
 * <p>
 * This distance measure has no related hash family.
 * </p>
 * @author Joren Six
 */
public class IntersectionDistance implements DistanceMeasure {

	@Override
	public double distance(Vector one, Vector other) {
		double intersectionSum = 0.0;
		double thisArea = 0.0;
		double otherArea = 0.0;
		for(int d=0; d < one.getDimensions(); d++) {
			intersectionSum += Math.min(one.get(d),other.get(d));
			thisArea+=one.get(d);
			otherArea+=other.get(d);
		}
		double area = Math.max(thisArea, otherArea);
		//Prevent division by zero
		return area == 0 ? 1 : 1 - intersectionSum/area;
	}

}
