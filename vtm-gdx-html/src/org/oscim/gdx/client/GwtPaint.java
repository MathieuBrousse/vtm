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

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;

import com.badlogic.gdx.graphics.Pixmap;

public class GwtPaint implements Paint {

	String color;
	boolean stroke;

	float strokeWidth;
	Align mAlign;

	float fontSize = 12;

	private FontStyle fontStyle = FontStyle.NORMAL;
	//private FontFamily fontFamily = FontFamily.DEFAULT;

	//String font = "12px sans-serif";
	String font = "13px Helvetica";

	//private int cap;

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public int getTextHeight(String text) {
		return 0;
	}

	@Override
	public int getTextWidth(String text) {
		return 0;
	}

	@Override
	public void setBitmapShader(Bitmap bitmap) {
	}

	@Override
	public void setColor(int color) {
		float a = ((color >>> 24) & 0xff) / 255f;
		int r = (color >>> 16) & 0xff;
		int g = (color >>> 8) & 0xff;
		int b = (color & 0xff);

		this.color = Pixmap.make(r, g, b, a);
	}

	@Override
	public void setDashPathEffect(float[] strokeDasharray) {
	}

	@Override
	public void setStrokeCap(Cap cap) {
		stroke = true;
	}

	@Override
	public void setStrokeWidth(float width) {
		stroke = true;
		strokeWidth = width;
	}

	@Override
	public void setStyle(Style style) {
	}

	@Override
	public void setTextAlign(Align align) {
		mAlign = align;
	}

	@Override
	public void setTextSize(float size) {
		fontSize = size;
		buildFont();
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		this.fontStyle = fontStyle;
		//this.fontFamily = fontFamily;
		buildFont();
	}

	@Override
	public float measureText(String text) {
		return GwtCanvasAdapter.getTextWidth(text, font);
	}

	// FIXME all estimates. no idea how to properly measure canvas text..
	@Override
	public float getFontHeight() {
		return 2 + fontSize + strokeWidth * 2;
	}

	@Override
	public float getFontDescent() {
		return 4 + strokeWidth;
	}

	void buildFont() {
		StringBuilder sb = new StringBuilder();

		if (this.fontStyle == FontStyle.BOLD)
			sb.append("bold ");
		else if (this.fontStyle == FontStyle.ITALIC)
			sb.append("italic ");

		sb.append(Math.round(this.fontSize));
		sb.append("px ");

		sb.append("Helvetica");

		this.font = sb.toString();

	}
}
