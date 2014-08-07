package com.limemobile.app.launcher.anim;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class Rotate3d extends Animation {
	/** 起始角度 **/
	private float mFromDegree;
	/** 结束角度 **/
	private float mToDegree;
	/** 中轴X坐标 **/
	private float mCenterX;
	/** 中轴Y坐标 **/
	private float mCenterY;
	// private float mLeft;
	// private float mTop;
	private float mSaveFromDegree;
	private float mSaveToDegree;

	private int type;

	public final static int ROTATE_X = 0;
	public final static int ROTATE_Y = 1;
	public final static int ROTATE_Z = 2;
	public final static int ROTATE_XY = 3;
	public final static int ROTATE_XZ = 4;
	public final static int ROTATE_YZ = 5;
	public final static int ROTATE_XYZ = 6;
	
	private Camera mCamera;

	// private static final String TAG = "Rotate3d";

//	public Rotate3d(float fromDegree, float toDegree, float left, float top, float centerX, float centerY) {
//		this.mFromDegree = fromDegree;
//		this.mToDegree = toDegree;
//		// this.mLeft = left;
//		// this.mTop = top;
//		this.mCenterX = centerX;
//		this.mCenterY = centerY;
//
//	}

	/**
	 * 3D旋转
	 * 
	 * @param fromDegree
	 *            起始角度
	 * @param toDegree
	 *            结束角度
	 * @param centerX
	 *            旋转中心轴X
	 * @param centerY
	 *            旋转中心轴Y
	 */
	public Rotate3d(float fromDegree, float toDegree, float centerX, float centerY) {
		this.mFromDegree = fromDegree;
		this.mToDegree = toDegree;
		this.mCenterX = centerX;
		this.mCenterY = centerY;
		mSaveFromDegree = fromDegree;
		mSaveToDegree = toDegree;
		type = ROTATE_Y;
	}
	
	/**
	 *  反转角度
	 * @param isReverse 是否反转
	 */
	public void reverseTransformation(boolean isReverse){
		if(isReverse){
			this.mFromDegree = -mSaveFromDegree;
			this.mToDegree = -mSaveToDegree;
		}else{
			this.mFromDegree = mSaveFromDegree;
			this.mToDegree = mSaveToDegree;
		}
	}
	
	public void rotate(float degrees){
		switch (type) {
		case ROTATE_X:
			mCamera.rotateX(degrees);
			break;
		case ROTATE_Y:
			mCamera.rotateY(degrees);
			break;
		case ROTATE_Z:
			mCamera.rotateZ(degrees);
			break;
		case ROTATE_XY:
			mCamera.rotateX(degrees);
			mCamera.rotateY(degrees);
			break;
		case ROTATE_XZ:
			mCamera.rotateX(degrees);
			mCamera.rotateZ(degrees);
			break;
		case ROTATE_YZ:
			mCamera.rotateY(degrees);
			mCamera.rotateZ(degrees);
			break;
		case ROTATE_XYZ:
			mCamera.rotateX(degrees);
			mCamera.rotateY(degrees);
			mCamera.rotateZ(degrees);
			break;
		}
	}
	
	@Override
	public void initialize(int width, int height, int parentWidth, int parentHeight) {
		super.initialize(width, height, parentWidth, parentHeight);
		mCamera = new Camera();
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {
		final float FromDegree = mFromDegree;
		float degrees = FromDegree + (mToDegree - mFromDegree) * interpolatedTime;
		final float centerX = mCenterX;
		final float centerY = mCenterY;
		final Matrix matrix = t.getMatrix();
//		Log.e("Rotate3d", "degrees=" + degrees);
		if (degrees <= -76.0f) {
			degrees = -90.0f;
			mCamera.save();
			rotate(degrees);
			mCamera.getMatrix(matrix);
			mCamera.restore();
		} else if (degrees >= 76.0f) {
			degrees = 90.0f;
			mCamera.save();
			rotate(degrees);
			mCamera.getMatrix(matrix);
			mCamera.restore();
		} else {
			mCamera.save();
			mCamera.translate(0, 0, centerX);
			rotate(degrees);
			mCamera.translate(0, 0, -centerX);
			mCamera.getMatrix(matrix);
			mCamera.restore();
		}

		matrix.preTranslate(-centerX, -centerY);
		matrix.postTranslate(centerX, centerY);
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
