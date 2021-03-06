package org.oscim.renderer;

import org.oscim.core.MapPosition;
import org.oscim.map.Viewport;

public class GLViewport extends Viewport {

	/** Do not modify! */
	public final GLMatrix viewproj = mViewProjMatrix;
	/** Do not modify! */
	public final GLMatrix proj = mProjMatrix;
	/** Do not modify! */
	public final GLMatrix view = mViewMatrix;
	/** Do not modify! */
	public final float[] plane = new float[8];

	/** For temporary use, to setup MVP-Matrix */
	public final GLMatrix mvp = new GLMatrix();

	public final MapPosition pos = mPos;

	/**
	 * Set MVP so that coordinates are in screen pixel coordinates with 0,0
	 * being center
	 */
	public void useScreenCoordinates(boolean center, float scale) {
		float ratio = (1f / (scale * mWidth));

		if (center)
			mvp.setScale(ratio, ratio, ratio);
		else
			mvp.setTransScale((-mWidth / 2) * ratio * scale,
			                  (-mHeight / 2) * ratio * scale,
			                  ratio);

		mvp.multiplyLhs(proj);
	}

	protected boolean changed;

	public boolean changed() {
		return changed;
	}

	void setFrom(Viewport viewport) {
		changed = super.copy(viewport);
		getMapExtents(plane, 0);
	}

	public float getWidth() {
		return mWidth;
	}

	public float getHeight() {
		return mHeight;
	}
}
