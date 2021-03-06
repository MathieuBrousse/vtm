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
package org.oscim.theme.styles;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Paint;
import org.oscim.backend.canvas.Paint.Align;
import org.oscim.backend.canvas.Paint.FontFamily;
import org.oscim.backend.canvas.Paint.FontStyle;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.theme.IRenderTheme.Callback;

/**
 * Represents a text along a polyline on the map.
 */
public final class Text extends RenderStyle {

	public final String style;

	public final float fontSize;
	public final Paint paint;
	public final Paint stroke;
	public final String textKey;

	public final boolean caption;
	public final float dy;
	public final int priority;

	public float fontHeight;
	public float fontDescent;

	public final TextureRegion texture;

	public static Text createText(float fontSize, float strokeWidth, int fill, int outline,
	        boolean billboard) {

		return createText("", fontSize, strokeWidth, fill, outline, billboard);
	}

	public static Text createText(String textKey, float fontSize, float strokeWidth, int fill,
	        int outline,
	        boolean billboard) {

		Text t = new Text("",
		                  textKey,
		                  FontFamily.DEFAULT,
		                  FontStyle.NORMAL,
		                  fontSize,
		                  fill,
		                  outline,
		                  strokeWidth,
		                  0,
		                  billboard,
		                  null,
		                  Integer.MAX_VALUE);

		t.fontHeight = t.paint.getFontHeight();
		t.fontDescent = t.paint.getFontDescent();

		return t;
	}

	public Text(String style, String textKey, FontFamily fontFamily, FontStyle fontStyle,
	        float fontSize, int fill, int outline, float strokeWidth, float dy, boolean caption,
	        TextureRegion symbol, int priority) {

		this.style = style;
		this.textKey = textKey;
		this.caption = caption;
		this.dy = -dy;
		this.priority = priority;
		this.texture = symbol;

		paint = CanvasAdapter.g.getPaint();
		paint.setTextAlign(Align.CENTER);
		paint.setTypeface(fontFamily, fontStyle);

		paint.setColor(fill);
		paint.setTextSize(fontSize);

		if (strokeWidth > 0) {
			stroke = CanvasAdapter.g.getPaint();
			stroke.setStyle(Paint.Style.STROKE);
			stroke.setTextAlign(Align.CENTER);
			stroke.setTypeface(fontFamily, fontStyle);
			stroke.setColor(outline);
			stroke.setStrokeWidth(strokeWidth);
			stroke.setTextSize(fontSize);
		} else
			stroke = null;

		this.fontSize = fontSize;

		//fontHeight = paint.getFontHeight();
		//fontDescent = paint.getFontDescent();
	}

	@Override
	public void renderNode(Callback renderCallback) {
		if (caption)
			renderCallback.renderPointText(this);
	}

	@Override
	public void renderWay(Callback renderCallback) {
		if (caption)
			renderCallback.renderAreaText(this);
		else
			renderCallback.renderWayText(this);
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		paint.setTextSize(fontSize * scaleFactor);
		if (stroke != null)
			stroke.setTextSize(fontSize * scaleFactor);

		fontHeight = paint.getFontHeight();
		fontDescent = paint.getFontDescent();
	}
}
