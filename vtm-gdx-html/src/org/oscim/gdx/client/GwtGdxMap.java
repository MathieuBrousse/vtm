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
package org.oscim.gdx.client;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.gdx.GdxMap;
import org.oscim.layers.tile.BitmapTileLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.source.TileSource;
import org.oscim.tiling.source.bitmap.DefaultSources.NaturalEarth;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

class GwtGdxMap extends GdxMap {
	static final Logger log = LoggerFactory.getLogger(GwtGdxMap.class);

	SearchBox mSearchBox;

	@Override
	public void create() {
		MapConfig c = MapConfig.get();

		// stroke text takes about 70% cpu time in firefox:
		// https://bug568526.bugzilla.mozilla.org/attachment.cgi?id=447932
		// <- circle/stroke test 800ms firefox, 80ms chromium..
		// TODO use texture atlas to avoid drawing text-textures
		if (GwtApplication.agentInfo().isLinux() && GwtApplication.agentInfo().isFirefox())
			GwtCanvasAdapter.NO_STROKE_TEXT = true;

		CanvasAdapter.g = GwtCanvasAdapter.INSTANCE;
		CanvasAdapter.textScale = 0.7f;
		GLAdapter.g = (GL20) Gdx.graphics.getGL20();
		GLAdapter.GDX_WEBGL_QUIRKS = true;
		MapRenderer.setBackgroundColor(0xffffff);
		//Gdx.app.setLogLevel(Application.LOG_DEBUG);

		super.create();

		double lat = c.getLatitude();
		double lon = c.getLongitude();
		int zoom = c.getZoom();

		float tilt = 0;
		float rotation = 0;

		if (Window.Location.getHash() != null) {
			String hash = Window.Location.getHash();

			hash = hash.substring(1);
			String[] pairs = hash.split(",");

			for (String p : pairs) {
				try {
					if (p.startsWith("lat="))
						lat = Double.parseDouble(p.substring(4));
					else if (p.startsWith("lon="))
						lon = Double.parseDouble(p.substring(4));
					else if (p.startsWith("scale="))
						zoom = Integer.parseInt(p.substring(6));
					else if (p.startsWith("rot="))
						rotation = Float.parseFloat(p.substring(4));
					else if (p.startsWith("tilt="))
						tilt = Float.parseFloat(p.substring(5));
				} catch (NumberFormatException e) {

				}
			}
		}
		MapPosition p = new MapPosition();
		p.setZoomLevel(zoom);
		p.setPosition(lat, lon);
		log.debug("map position: " + p.x + "/" + p.y + " " + lat + "/" + lon);

		p.bearing = rotation;
		p.tilt = tilt;
		mMap.setMapPosition(p);

		//mMap.getViewport().setTilt(tilt);
		//mMap.getViewport().setRotation(rotation);

		String url = c.getTileUrl();
		String sourceName = c.getTileSource();

		TileSource tileSource;
		tileSource = new OSciMap4TileSource(url);

		initDefaultLayers(tileSource, false, true, true);

		if ("naturalearth".equals(c.getBackgroundLayer()))
			mMap.setBackgroundMap(new BitmapTileLayer(mMap, new NaturalEarth()));

		mSearchBox = new SearchBox(mMap);

		// update URL hash to current position, every 5 seconds
		Timer timer = new Timer() {
			private int curLon, curLat, curZoom, curTilt, curRot;
			private MapPosition pos = new MapPosition();

			public void run() {
				mMap.viewport().getMapPosition(pos);
				int lat = (int) (MercatorProjection.toLatitude(pos.y) * 1000);
				int lon = (int) (MercatorProjection.toLongitude(pos.x) * 1000);
				int rot = (int) (pos.bearing);
				rot = (int) (pos.bearing) % 360;
				//rot = rot < 0 ? -rot : rot;

				if (curZoom != pos.zoomLevel || curLat != lat || curLon != lon
				        || curTilt != rot || curRot != (int) (pos.bearing)) {

					curLat = lat;
					curLon = lon;
					curZoom = pos.zoomLevel;
					curTilt = (int) pos.tilt;
					curRot = rot;

					String newURL = Window.Location
					    .createUrlBuilder()
					    .setHash("scale=" + pos.zoomLevel + ",rot=" + curRot
					            + ",tilt=" + curTilt + ",lat=" + (curLat / 1000f)
					            + ",lon=" + (curLon / 1000f))
					    .buildString();
					Window.Location.replace(newURL);
				}
			}
		};
		timer.scheduleRepeating(5000);
	}

	@Override
	protected void createLayers() {
	}
}
