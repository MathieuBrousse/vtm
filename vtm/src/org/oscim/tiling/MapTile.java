/*
 * Copyright 2012, 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.tiling;

import org.oscim.core.Tile;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.TextItem;
import org.oscim.utils.pool.Inlist.List;
import org.oscim.utils.quadtree.Node;

/**
 * Extends Tile class to hold state and data for concurrent use in
 * TileManager (Main Thread),
 * TileLoader (Worker Thread) and
 * TileRenderer (GL Thread).
 */
public class MapTile extends Tile {

	public static class TileNode extends Node<TileNode, MapTile> {

	}

	/** To be removed: used by GWT backend */
	public TileLoader loader;

	public static final class State {
		public final static byte NONE = 0;

		/**
		 * STATE_LOADING means the tile is about to be loaded / loading.
		 * Tile belongs to TileLoader thread.
		 */
		public final static byte LOADING = 1 << 0;

		/**
		 * STATE_NEW_DATA: tile data is prepared for rendering.
		 * While 'locked' it belongs to GL Thread.
		 */
		public final static byte NEW_DATA = 1 << 1;

		/**
		 * STATE_READY: tile data is uploaded to GL.
		 * While 'locked' it belongs to GL Thread.
		 */
		public final static byte READY = 1 << 2;

		/**
		 * STATE_CANCEL: tile is removed from TileManager,
		 * but may still be processed by TileLoader.
		 */
		public final static byte CANCEL = 1 << 3;
	}

	public MapTile(TileNode node, int tileX, int tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);
		this.x = (double) tileX / (1 << zoomLevel);
		this.y = (double) tileY / (1 << zoomLevel);
		this.node = node;
	}

	byte state;

	public boolean state(int testState) {
		return (state & testState) != 0;
	}

	public byte getState() {
		return state;
	}

	/**
	 * absolute tile coordinates: tileX,Y / Math.pow(2, zoomLevel)
	 */
	public final double x;
	public final double y;

	/**
	 * current distance from map center
	 */
	public float distance;

	/**
	 * FIXME move to VectorMapTile
	 * Tile data set by TileLoader.
	 */

	public final List<SymbolItem> symbols = new List<SymbolItem>();
	public final List<TextItem> labels = new List<TextItem>();

	public ElementLayers layers;

	/**
	 * Tile is in view region. Set by TileRenderer.
	 */
	public boolean isVisible;
	public long fadeTime;

	/**
	 * Pointer to access relatives in QuadTree
	 */
	public final TileNode node;

	/**
	 * to avoid drawing a tile twice per frame
	 */
	int lastDraw = 0;

	public final static int PROXY_CHILD1 = 1 << 0;
	public final static int PROXY_CHILD2 = 1 << 1;
	public final static int PROXY_CHILD3 = 1 << 2;
	public final static int PROXY_CHILD4 = 1 << 3;
	public final static int PROXY_PARENT = 1 << 4;
	public final static int PROXY_GRAMPA = 1 << 5;
	public final static int PROXY_HOLDER = 1 << 6;

	/** keep track which tiles are locked as proxy for this tile */
	byte proxies;

	/** counting the tiles that use this tile as proxy */
	private byte refs;

	/** up to 255 Threads may lock a tile */
	private byte locked;

	/**
	 * only used GLRenderer when this tile sits in for another tile.
	 * e.g. x:-1,y:0,z:1 for x:1,y:0
	 */
	MapTile holder;

	/**
	 * @return true when tile might be referenced by render thread.
	 */
	boolean isLocked() {
		return locked > 0 || refs > 0;
	}

	/**
	 * Set this tile to be locked, i.e. to no be modified or cleared
	 * while rendering. Renderable parent, grand-parent and children
	 * will also be locked. Dont forget to unlock when tile is not longer
	 * used. This function should only be called through {@link TileManager}
	 */
	void lock() {
		if (locked++ > 0)
			return;

		/* lock all tiles that could serve as proxy */
		MapTile p = node.parent.item;
		if (p != null && (p.state != State.NONE)) {
			proxies |= PROXY_PARENT;
			p.refs++;
		}

		p = node.parent.parent.item;
		if (p != null && (p.state != State.NONE)) {
			proxies |= PROXY_GRAMPA;
			p.refs++;
		}

		for (int j = 0; j < 4; j++) {
			if ((p = node.child(j)) == null || p.state == 0)
				continue;

			proxies |= (1 << j);
			p.refs++;
		}
	}

	/**
	 * Unlocks this tile when it cannot be used by render-thread.
	 */
	void unlock() {
		if (--locked > 0 || proxies == 0)
			return;

		if ((proxies & PROXY_PARENT) != 0)
			node.parent.item.refs--;

		if ((proxies & PROXY_GRAMPA) != 0)
			node.parent.parent.item.refs--;

		for (int i = 0; i < 4; i++) {
			if ((proxies & (1 << i)) != 0)
				node.child(i).refs--;
		}
		proxies = 0;
	}

	/**
	 * @return true if tile is loading, has new data or is ready
	 *         for rendering
	 */
	public boolean isActive() {
		return state > State.NONE;
	}

	/**
	 * Test whether it is save to access a proxy item through
	 * this.rel.*
	 */
	public boolean hasProxy(int proxy) {
		return (proxies & proxy) != 0;
	}

	/**
	 * CAUTION: This function may only be called by {@link TileManager}
	 */
	protected void clear() {
		if (layers != null) {
			layers.clear();
			layers = null;
		}

		TextItem.pool.releaseAll(labels.clear());
		SymbolItem.pool.releaseAll(symbols.clear());

		// still needed?
		state = State.NONE;
	}
}
