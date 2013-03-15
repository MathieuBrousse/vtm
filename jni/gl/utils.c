#include <jni.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <android/log.h>
#include <stdlib.h>
#include <math.h>

#define JNI(X) JNIEXPORT Java_org_oscim_utils_GlUtils_##X

static const char TAG[] = "org.oscim.utils.GLUtils";

static void
nativeClassInit(JNIEnv *_env)
{

}

void JNI(init)(JNIEnv *env, jclass* clazz)
{
  nativeClassInit(env);
  __android_log_print(ANDROID_LOG_INFO, TAG, "all initialized");
}

#undef JNI
#define JNI(X) JNIEXPORT Java_org_oscim_utils_Matrix4_##X

#define CAST(x) (float *)(uintptr_t) x
#define MAT_SIZE 16 * sizeof(float)

static const float identity[] =
      { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };

static inline void
multiplyMM(float* r, const float* lhs, const float* rhs);

static inline void
setRotateM(float* rm, int rmOffset, float a, float x, float y, float z);

static inline void
transposeM(float* mTrans, int mTransOffset, float* m, int mOffset);

static inline void
matrix4_proj(float* mat, float* vec);


jlong JNI(alloc)(JNIEnv *env, jclass* clazz)
{
  return (long) calloc(16, sizeof(float));
}

void JNI(delete)(JNIEnv* env, jclass* clazz, jlong ptr)
{
  free(CAST(ptr));
}

void JNI(setAsUniform)(JNIEnv* env, jclass* clazz, jlong ptr, jint location)
{
  float* m = CAST(ptr);

  glUniformMatrix4fv((GLint) location, (GLsizei) 1, (GLboolean) 0, (GLfloat *) m);
}

void JNI(setValueAt)(JNIEnv* env, jclass* clazz, jlong ptr, jint pos, jfloat value)
{
  float* m = CAST(ptr);
  if (pos > -1 && pos < 16)
    m[pos] = value;
}

void JNI(identity)(JNIEnv* env, jclass* clazz, jlong ptr)
{
  float* m = CAST(ptr);
  memcpy(m, identity, MAT_SIZE);
}

void JNI(setScale)(JNIEnv* env, jclass* clazz, jlong ptr, jfloat sx, jfloat sy, jfloat sz)
{
  float* m = CAST(ptr);
  memcpy(m, identity, MAT_SIZE);
  m[0] = sx;
  m[5] = sy;
  m[10] = sz;
}

void JNI(setTranslation)(JNIEnv* env, jclass* clazz, jlong ptr, jfloat x, jfloat y, jfloat z)
{
  float* m = CAST(ptr);
  memcpy(m, identity, MAT_SIZE);
  m[12] = x;
  m[13] = y;
  m[14] = z;
}

void JNI(setRotation)(JNIEnv* env, jclass* clazz, jlong ptr, jfloat a, jfloat x, jfloat y, jfloat z)
{
  float* m = CAST(ptr);
  setRotateM(m, 0, a, x, y, z);
}

void JNI(setTransScale)(JNIEnv* env, jclass* clazz, jlong ptr, jfloat tx, jfloat ty, jfloat scale)
{
  float* m = CAST(ptr);
  memcpy(m, identity, MAT_SIZE);
  m[0] = scale;
  m[5] = scale;
  m[12] = tx;
  m[13] = ty;
}

void JNI(set)(JNIEnv* env, jclass* clazz, jlong ptr, jfloatArray obj_mat)
{
  float* m = CAST(ptr);
  float* mat = (float*) (*env)->GetPrimitiveArrayCritical(env, obj_mat, 0);

  memcpy(m, mat, MAT_SIZE);

  (*env)->ReleasePrimitiveArrayCritical(env, obj_mat, mat, 0);
}

void JNI(get)(JNIEnv* env, jclass* clazz, jlong ptr, jfloatArray obj_mat)
{
  float* m = CAST(ptr);
  float* mat = (float*) (*env)->GetPrimitiveArrayCritical(env, obj_mat, 0);

  memcpy(mat, m, MAT_SIZE);

  (*env)->ReleasePrimitiveArrayCritical(env, obj_mat, mat, 0);
}

void JNI(mul)(JNIEnv* env, jclass* clazz, jlong ptr_a, jlong ptr_b)
{
  float* mata = CAST(ptr_a);
  float* matb = CAST(ptr_b);

  multiplyMM(mata, mata, matb);
}

void JNI(copy)(JNIEnv* env, jclass* clazz, jlong ptr_dst, jlong ptr_src)
{
  float* dst = CAST(ptr_dst);
  float* src = CAST(ptr_src);

  memcpy(dst, src, MAT_SIZE);
}

void JNI(smul)(JNIEnv* env, jclass* clazz, jlong ptr_r, jlong ptr_a, jlong ptr_b)
{
  float* matr = CAST(ptr_r);
  float* mata = CAST(ptr_a);
  float* matb = CAST(ptr_b);

  multiplyMM(matr, mata, matb);
}

void JNI(strans)(JNIEnv* env, jclass* clazz, jlong ptr_r, jlong ptr_a)
{
  float* matr = CAST(ptr_r);
  float* mata = CAST(ptr_a);

  transposeM(matr, 0, mata, 0);
}

void JNI(prj)(JNIEnv* env, jclass* clazz, jlong ptr, jfloatArray obj_vec)
{
  float* m = CAST(ptr);
  float* vec = (float*) (*env)->GetPrimitiveArrayCritical(env, obj_vec, 0);

  matrix4_proj(m, vec);

  (*env)->ReleasePrimitiveArrayCritical(env, obj_vec, vec, 0);
}

static float someRandomEpsilon = 1.0f / (1 << 11);

void JNI(addDepthOffset)(JNIEnv* env, jclass* clazz, jlong ptr, jint delta)
{
  float* m = CAST(ptr);

  // from http://www.mathfor3dgameprogramming.com/code/Listing9.1.cpp
  //		float n = MapViewPosition.VIEW_NEAR;
  //		float f = MapViewPosition.VIEW_FAR;
  //		float pz = 1;
  //		float epsilon = -2.0f * f * n * delta / ((f + n) * pz * (pz + delta));

  m[10] *= 1.0f + someRandomEpsilon * delta;
}

/*
 * Copyright 2007, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// from android/platform_frameworks_base/blob/master/core/jni/android/opengl/util.cpp
#define I(_i, _j) ((_j)+ 4*(_i))

static inline void
multiplyMM(float* r, const float* lhs, const float* rhs)
{
  for (int i = 0; i < 4; i++)
    {
      register const float rhs_i0 = rhs[I(i,0)];
      register float ri0 = lhs[I(0,0)] * rhs_i0;
      register float ri1 = lhs[I(0,1)] * rhs_i0;
      register float ri2 = lhs[I(0,2)] * rhs_i0;
      register float ri3 = lhs[I(0,3)] * rhs_i0;
      for (int j = 1; j < 4; j++)
        {
          register const float rhs_ij = rhs[I(i,j)];
          ri0 += lhs[I(j,0)] * rhs_ij;
          ri1 += lhs[I(j,1)] * rhs_ij;
          ri2 += lhs[I(j,2)] * rhs_ij;
          ri3 += lhs[I(j,3)] * rhs_ij;
        }
      r[I(i,0)] = ri0;
      r[I(i,1)] = ri1;
      r[I(i,2)] = ri2;
      r[I(i,3)] = ri3;
    }
}

//static inline
//void
//mx4transform(float x, float y, float z, float w, const float* pM, float* pDest)
//{
//  pDest[0] = pM[0 + 4 * 0] * x + pM[0 + 4 * 1] * y + pM[0 + 4 * 2] * z + pM[0 + 4 * 3] * w;
//  pDest[1] = pM[1 + 4 * 0] * x + pM[1 + 4 * 1] * y + pM[1 + 4 * 2] * z + pM[1 + 4 * 3] * w;
//  pDest[2] = pM[2 + 4 * 0] * x + pM[2 + 4 * 1] * y + pM[2 + 4 * 2] * z + pM[2 + 4 * 3] * w;
//
//  pDest[3] = pM[3 + 4 * 0] * x + pM[3 + 4 * 1] * y + pM[3 + 4 * 2] * z + pM[3 + 4 * 3] * w;
//}

/**
 * Computes the length of a vector
 *
 * @param x x coordinate of a vector
 * @param y y coordinate of a vector
 * @param z z coordinate of a vector
 * @return the length of a vector
 */
static inline float
length(float x, float y, float z)
{
  return (float) sqrt(x * x + y * y + z * z);
}
/**
 * Rotates matrix m by angle a (in degrees) around the axis (x, y, z)
 * @param rm returns the result
 * @param rmOffset index into rm where the result matrix starts
 * @param a angle to rotate in degrees
 * @param x scale factor x
 * @param y scale factor y
 * @param z scale factor z
 */

static inline void
setRotateM(float* rm, int rmOffset, float a, float x, float y, float z)
{
  rm[rmOffset + 3] = 0;
  rm[rmOffset + 7] = 0;
  rm[rmOffset + 11] = 0;
  rm[rmOffset + 12] = 0;
  rm[rmOffset + 13] = 0;
  rm[rmOffset + 14] = 0;
  rm[rmOffset + 15] = 1;
  a *= (float) (M_PI / 180.0f);
  float s = (float) sin(a);
  float c = (float) cos(a);
  if (1.0f == x && 0.0f == y && 0.0f == z)
    {
      rm[rmOffset + 5] = c;
      rm[rmOffset + 10] = c;
      rm[rmOffset + 6] = s;
      rm[rmOffset + 9] = -s;
      rm[rmOffset + 1] = 0;
      rm[rmOffset + 2] = 0;
      rm[rmOffset + 4] = 0;
      rm[rmOffset + 8] = 0;
      rm[rmOffset + 0] = 1;
    }
  else if (0.0f == x && 1.0f == y && 0.0f == z)
    {
      rm[rmOffset + 0] = c;
      rm[rmOffset + 10] = c;
      rm[rmOffset + 8] = s;
      rm[rmOffset + 2] = -s;
      rm[rmOffset + 1] = 0;
      rm[rmOffset + 4] = 0;
      rm[rmOffset + 6] = 0;
      rm[rmOffset + 9] = 0;
      rm[rmOffset + 5] = 1;
    }
  else if (0.0f == x && 0.0f == y && 1.0f == z)
    {
      rm[rmOffset + 0] = c;
      rm[rmOffset + 5] = c;
      rm[rmOffset + 1] = s;
      rm[rmOffset + 4] = -s;
      rm[rmOffset + 2] = 0;
      rm[rmOffset + 6] = 0;
      rm[rmOffset + 8] = 0;
      rm[rmOffset + 9] = 0;
      rm[rmOffset + 10] = 1;
    }
  else
    {
      float len = length(x, y, z);
      if (1.0f != len)
        {
          float recipLen = 1.0f / len;
          x *= recipLen;
          y *= recipLen;
          z *= recipLen;
        }
      float nc = 1.0f - c;
      float xy = x * y;
      float yz = y * z;
      float zx = z * x;
      float xs = x * s;
      float ys = y * s;
      float zs = z * s;
      rm[rmOffset + 0] = x * x * nc + c;
      rm[rmOffset + 4] = xy * nc - zs;
      rm[rmOffset + 8] = zx * nc + ys;
      rm[rmOffset + 1] = xy * nc + zs;
      rm[rmOffset + 5] = y * y * nc + c;
      rm[rmOffset + 9] = yz * nc - xs;
      rm[rmOffset + 2] = zx * nc - ys;
      rm[rmOffset + 6] = yz * nc + xs;
      rm[rmOffset + 10] = z * z * nc + c;
    }
}

/**
 * Transposes a 4 x 4 matrix.
 *
 * @param mTrans the array that holds the output inverted matrix
 * @param mTransOffset an offset into mInv where the inverted matrix is
 *        stored.
 * @param m the input array
 * @param mOffset an offset into m where the matrix is stored.
 */
static inline void
transposeM(float* mTrans, int mTransOffset, float* m, int mOffset)
{
  for (int i = 0; i < 4; i++)
    {
      int mBase = i * 4 + mOffset;
      mTrans[i + mTransOffset] = m[mBase];
      mTrans[i + 4 + mTransOffset] = m[mBase + 1];
      mTrans[i + 8 + mTransOffset] = m[mBase + 2];
      mTrans[i + 12 + mTransOffset] = m[mBase + 3];
    }
}
/*******************************************************************************
 * Copyright 2011 See libgdx AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
// from /gdx/src/com/badlogic/gdx/math/Matrix4.java
#define M00 0
#define M01 4
#define M02 8
#define M03 12
#define M10 1
#define M11 5
#define M12 9
#define M13 13
#define M20 2
#define M21 6
#define M22 10
#define M23 14
#define M30 3
#define M31 7
#define M32 11
#define M33 15

static inline void
matrix4_proj(float* mat, float* vec)
{
  float inv_w = 1.0f / (vec[0] * mat[M30] + vec[1] * mat[M31] + vec[2] * mat[M32] + mat[M33]);
  float x = (vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03]) * inv_w;
  float y = (vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13]) * inv_w;
  float z = (vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23]) * inv_w;
  vec[0] = x;
  vec[1] = y;
  vec[2] = z;
}
