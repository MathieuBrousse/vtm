/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.layers.tile.vector.labeling;

import org.oscim.backend.canvas.Color;
import org.oscim.core.MapPosition;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.TextItem;
import org.oscim.theme.styles.Line;

class Debug {

	private final static float[] mDebugPoints = new float[8];
	// TODO Auto-generated method stub
	static ElementLayers dbg;

	static void addDebugBox(Label l, TextItem ti, int overlaps, boolean prev,
	        float scale) {

		LineLayer ll;
		if (prev) {
			if (overlaps == 1)
				ll = dbg.getLineLayer(4);
			else
				ll = dbg.getLineLayer(5);

		} else {
			if (ti.width > ti.length * scale) {
				ll = dbg.getLineLayer(1);
				overlaps = 3;
			}
			else if (overlaps == 1)
				ll = dbg.getLineLayer(0);
			else if (overlaps == 2)
				ll = dbg.getLineLayer(3);
			else
				ll = dbg.getLineLayer(2);
		}
		float[] points = mDebugPoints;
		float width = (ti.x2 - ti.x1) / 2f;
		float height = (ti.y2 - ti.y1) / 2f;
		points[0] = (l.x - width * scale);
		points[1] = (l.y - height * scale);
		points[2] = (l.x + width * scale);
		points[3] = (l.y + height * scale);
		ll.addLine(points, 4, false);

		System.arraycopy(l.bbox.vec, 2, points, 0, 8);
		if (l.bbox != null && overlaps != 3) {
			ll.addLine(points, 8, true);
		}
	}

	static void addDebugLayers(ElementLayers dbg) {
		int alpha = 0xaaffffff;

		dbg.clear();
		dbg.addLineLayer(0, new Line((Color.BLUE & alpha), 2));
		dbg.addLineLayer(1, new Line((Color.RED & alpha), 2));
		dbg.addLineLayer(3, new Line((Color.YELLOW & alpha), 2));
		dbg.addLineLayer(2, new Line((Color.GREEN & alpha), 2));
		dbg.addLineLayer(4, new Line((Color.CYAN & alpha), 2));
		dbg.addLineLayer(5, new Line((Color.MAGENTA & alpha), 2));
	}

	public static void draw(MapPosition pos, GLViewport m, ElementLayers layers) {
		//		if (layers.baseLayers != null) {
		//			//setMatrix(pos, m, true);
		//
		//			for (RenderElement l = layers.baseLayers; l != null;) {
		//				if (l.type == RenderElement.POLYGON) {
		//					l = PolygonLayer.Renderer.draw(pos, l, m, true, 1, false);
		//				} else {
		//					//float div = (float) (mMapPosition.scale / (1 << pos.zoomLevel));
		//					l = LineLayer.Renderer.draw(layers, l, pos, m, div);
		//				}
		//			}
		//		}
	}

}
