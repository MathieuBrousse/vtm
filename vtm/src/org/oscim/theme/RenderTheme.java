/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.TagSet;
import org.oscim.theme.rule.Element;
import org.oscim.theme.rule.Rule;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.utils.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RenderTheme defines how map elements are drawn.
 */
public class RenderTheme implements IRenderTheme {
	static final Logger log = LoggerFactory.getLogger(RenderTheme.class);

	private static final int MATCHING_CACHE_SIZE = 512;

	private final float mBaseTextSize;
	private final int mMapBackground;

	private int mLevels;
	private Rule[] mRules;

	class RenderStyleCache {
		final int matchType;
		final LRUCache<MatchingCacheKey, RenderStyleItem> cache;
		final MatchingCacheKey cacheKey;

		/* temporary matching instructions list */
		final ArrayList<RenderStyle> instructionList;

		RenderStyleItem prevItem;

		public RenderStyleCache(int type) {
			cache = new LRUCache<MatchingCacheKey, RenderStyleItem>(MATCHING_CACHE_SIZE);
			instructionList = new ArrayList<RenderStyle>(4);
			cacheKey = new MatchingCacheKey();
			matchType = type;
		}

		RenderStyleItem getRenderInstructions() {
			return cache.get(cacheKey);
		}
	}

	class RenderStyleItem {
		RenderStyleItem next;
		int zoom;
		RenderStyle[] list;
		MatchingCacheKey key;
	}

	private final RenderStyleCache[] mStyleCache;

	public RenderTheme(int mapBackground, float baseStrokeWidth, float baseTextSize) {
		mMapBackground = mapBackground;
		mBaseTextSize = baseTextSize;

		mStyleCache = new RenderStyleCache[3];
		mStyleCache[0] = new RenderStyleCache(Element.NODE);
		mStyleCache[1] = new RenderStyleCache(Element.LINE);
		mStyleCache[2] = new RenderStyleCache(Element.POLY);
	}

	@Override
	public void destroy() {

		for (int i = 0; i < 3; i++)
			mStyleCache[i].cache.clear();

		if (mRules != null) {
			for (int i = 0, n = mRules.length; i < n; i++)
				mRules[i].onDestroy();
		}
	}

	@Override
	public int getLevels() {
		return mLevels;
	}

	@Override
	public int getMapBackground() {
		return mMapBackground;
	}

	@Override
	public RenderStyle[] matchElement(GeometryType geometryType, TagSet tags, int zoomLevel) {

		// list of renderinsctruction items in cache
		RenderStyleItem ris = null;

		// the item matching tags and zoomlevel
		RenderStyleItem ri = null;

		int type = geometryType.nativeInt;
		if (type < 1 || type > 3) {
			log.debug("invalid geometry type for RenderTheme " + geometryType.name());
			return null;
		}

		RenderStyleCache cache = mStyleCache[type - 1];

		// NOTE: maximum zoom level supported is 32
		int zoomMask = 1 << zoomLevel;

		synchronized (cache) {

			if ((cache.prevItem == null) || (cache.prevItem.zoom & zoomMask) == 0) {
				// previous instructions zoom does not match
				cache.cacheKey.set(tags, null);
			} else {
				// compare if tags match previous instructions
				if (cache.cacheKey.set(tags, cache.prevItem.key)) {
					//log.debug("same as previous " + Arrays.deepToString(tags));
					ri = cache.prevItem;
				}
			}

			if (ri == null) {
				// get instruction for current cacheKey
				ris = cache.getRenderInstructions();

				for (ri = ris; ri != null; ri = ri.next)
					if ((ri.zoom & zoomMask) != 0)
						// cache hit
						break;
			}

			if (ri == null) {
				// cache miss
				//log.debug(missCnt++ + " / " + hitCnt + " Cache Miss");

				List<RenderStyle> matches = cache.instructionList;
				matches.clear();

				for (Rule rule : mRules)
					rule.matchElement(cache.matchType, cache.cacheKey.mTags, zoomMask, matches);

				int size = matches.size();
				if (size > 1) {
					for (int i = 0; i < size - 1; i++) {
						RenderStyle r = matches.get(i);
						for (int j = i + 1; j < size; j++) {
							if (matches.get(j) == r) {
								log.debug("fix duplicate instruction! "
								        + Arrays.deepToString(cache.cacheKey.mTags)
								        + " zoom:" + zoomLevel + " "
								        + r.getClass().getName());
								matches.remove(j--);
								size--;
							}
						}
					}
				}
				// check if same instructions are used in another level
				for (ri = ris; ri != null; ri = ri.next) {
					if (size == 0) {
						if (ri.list != null)
							continue;

						// both matchinglists are empty
						break;
					}

					if (ri.list == null)
						continue;

					if (ri.list.length != size)
						continue;

					int i = 0;
					for (RenderStyle style : ri.list) {
						if (style != matches.get(i))
							break;
						i++;
					}
					if (i == size)
						// both matching lists contain the same items
						break;
				}

				if (ri != null) {
					// we found a same matchting list on another zoomlevel add
					// this zoom level to the existing RenderInstructionItem.
					ri.zoom |= zoomMask;

					//log.debug(zoomLevel + " same instructions " + size + " "
					//				+ Arrays.deepToString(tags));
				} else {
					//log.debug(zoomLevel + " new instructions " + size + " "
					//				+ Arrays.deepToString(tags));

					ri = new RenderStyleItem();
					ri.zoom = zoomMask;

					if (size > 0) {
						ri.list = new RenderStyle[size];
						matches.toArray(ri.list);
					}

					// attach this list to the one found for MatchingKey
					if (ris != null) {
						ri.next = ris.next;
						ri.key = ris.key;
						ris.next = ri;
					} else {
						ri.key = new MatchingCacheKey(cache.cacheKey);
						cache.cache.put(ri.key, ri);
					}
				}
			}

			cache.prevItem = ri;
		}

		return ri.list;
	}

	void complete(List<Rule> rulesList, int levels) {
		mLevels = levels;

		mRules = new Rule[rulesList.size()];
		rulesList.toArray(mRules);

		for (int i = 0, n = mRules.length; i < n; i++) {
			mRules[i].onComplete();
		}
	}

	@Override
	public void scaleTextSize(float scaleFactor) {

		for (int i = 0, n = mRules.length; i < n; i++)
			mRules[i].scaleTextSize(scaleFactor * mBaseTextSize);
	}
}
