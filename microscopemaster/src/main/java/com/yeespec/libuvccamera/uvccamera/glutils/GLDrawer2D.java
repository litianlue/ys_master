package com.yeespec.libuvccamera.uvccamera.glutils;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: GLDrawer2D.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Helper class to draw to whole view using specific texture and texture matrix
 */
public class GLDrawer2D {
    /**
     * 两个SurfaceTexture的作用是不一样的。
     * 通过TextureView获取到的surfaceTexture
     * 是为了创建一个EGL native窗口
     * （当然用SurfaceHolder或Surface也是可以最为eglCreateWindowSurface的native_window参数），
     * 你可以把这个窗口看成手机显示屏，，，
     * 而 new 的这个surfaceTexture才是作为Mediaplayer不断生产的视频流的载体，
     * 在内部将视频流转换为一帧一帧的纹理texture，然后被绘制，最终显示在那个窗口上
     */

    /**
     * OpenGl绘制纹理三步走 :
     * 1.载入顶点 ;     loadVertex()
     * 2.初始化着色器 ;   initShader()
     * 3.载入纹理 ;     loadTexture()
     */

    /**
     * 1）加载纹理并显示到物体上；
     * 2）重新组织程序，管理多个着色器和顶点数据之间的切换；
     * 3）调整纹理以适应它们将要被绘制的形状，既可以调整纹理坐标，也可以通过拉伸或压扁纹理本身来实现；
     * 4）纹理不能直接传递，需要被绑定到纹理单元，然后将纹理单元传递给着色器；
     */

    private static final boolean DEBUG = false; // TODO set false on release
    private static final String TAG = "GLDrawer2D";

    /**
     * 至少需要一个vertex shader来绘制shape和
     * 一个fragment shader来绘制颜色和texture,
     * 这些shader必须要被编译然后再添加到一个OpenGL ES program中,
     * 然后这个progrem会被用来绘制shape
     */

    //渲染shape顶点的OpenGL ES代码 :   可编程着色器是一种脚本
    // TODO: 2016/9/9 :显示无变形拉伸配置
    private static final String vss
            = "uniform mat4 uMVPMatrix;\n"
            + "uniform mat4 uTexMatrix;\n"
            + "attribute highp vec4 aPosition;\n"
            + "attribute highp vec4 aTextureCoord;\n"
            + "varying highp vec2 vTextureCoord;\n"
            + "\n"
            + "void main() {\n"
            + "	gl_Position = uMVPMatrix * aPosition;\n"
            + "	vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n"
            + "}\n";

    // TODO: 2016/9/9 :兼容版本
    //渲染shape的face的颜色和texture的OpenGL ES代码 :     可编程着色器是一种脚本
    //    private static final String fss
    //            = "#extension GL_OES_EGL_image_external : require\n"
    //            + "precision mediump float;\n"
    //            + "uniform samplerExternalOES sTexture;\n"
    //            + "varying highp vec2 vTextureCoord;\n"
    //            + "void main() {\n"
    //            + "vec2 coord = vTextureCoord;\n"
    //            + "coord.s = coord.s * 0.9;\n"  //其实是去掉图像横向s的一半 , 向量缩小了;
    //            //            + "coord.t = coord.t * 1.1;\n"  //其实是去掉图像纵向t的一半 , 向量缩小了;
    //            + "  gl_FragColor = texture2D(sTexture, coord);\n"
    //            + "}";

    //        private static final String fss
    //                = "#extension GL_OES_EGL_image_external : require\n"
    //                + "precision mediump float;\n"
    //                + "uniform samplerExternalOES sTexture;\n"
    //                + "varying highp vec2 vTextureCoord;\n"
    //                + "void main() {\n"
    //                + "vec2 coord = vTextureCoord;\n"
    //                + "coord.s = coord.s * 0.9;\n"  //其实是去掉图像横向s的一半 , 向量缩小了;
    //                + "coord.t = coord.t * 1.1;\n"  //其实是去掉图像纵向t的一半 , 向量缩小了;
    //                + "  gl_FragColor = texture2D(sTexture, coord);\n"
    //                + "}";

    // TODO: 2016/9/9 :显示无变形拉伸配置
    private static final String fss
            = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "varying highp vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
            + "}";

    private static float squareSize = 1.0f;
    // TODO: 2016/9/9 :兼容版本
    //绘制的区域尺寸(顶点) 值为1.0f时 ,square的面积就是整个手机屏幕 , 若为0.5f则每个边长都为屏幕的一半 :
    // TODO: 2016/9/9 :显示无变形拉伸配置
    private static final float[] VERTICES = {
            squareSize, squareSize,     //右上
            -squareSize, squareSize,    //左上
            squareSize, -squareSize,    //右下
            -squareSize, -squareSize    //左下
    };

    //    private static final float[] VERTICES = {
    //            1.0f, 1.0f,     //右上
    //            -1.0f, 1.0f,    //左上
    //            1.0f, -1.0f,    //右下
    //            -1.0f, -1.0f    //左下
    //    };

    //纹理坐标数组 :
    /**
     * 在该组数据中，x=0,y=0对应纹理S=0.5,T=0.5，x=-0.5,y=-0.8对应纹理S=0,T=0.9，
     * 之所以有这种对应关系，看下前面讲到的OpenGL纹理坐标与计算机图像坐标的对比就清楚啦。
     * 至于纹理部分的数据使用了0.1和0.9作为T坐标，是为了避免把纹理压扁，
     * 而对纹理进行了裁剪，截取了0.1到0.9的部分。
     */
    // TODO: 2016/9/9 :兼容版本
    //    private static final float[] TEXCOORD = {   //T-轴翻转 :
    //            // Order of coordinates: X, Y, S, T
    //            1.0f, 1.0f,    //右上
    //            0.0f, 1.0f,    //左上
    //            1.0f, 0.0f,    //右下
    //            0.0f, 0.0f     //左下
    //    };


    //下面的参数配置可以实现全幅预览显示不变形 , 但是因为拉伸填充了边界 ,导致录像录制的图像是有边框填充且变形 :
    // TODO: 2016/9/9 :显示无变形拉伸配置
    //    private static final float[] TEXCOORD = {   //T-轴翻转 :
    //            // Order of coordinates: X, Y, S, T
    //            1.0f, 1.2f,    //右上
    //            0.0f, 1.2f,    //左上
    //            1.0f, -0.2f,    //右下
    //            0.0f, -0.2f     //左下
    //    };

    private static final float[] TEXCOORD = {
            1.0f, 1.0f,     //右上
            0.0f, 1.0f,     //左上
            1.0f, 0.0f,     //右下
            0.0f, 0.0f      //左下
    };

    /**
     * 用来缓存纹理坐标，因为纹理都是要在后台被绘制好，然
     * 后不断的替换最前面显示的纹理图像
     * <p/>
     * FloatBuffer，ShortBuffer是封装了本地数据结构的封装对象。
     * 是 的，这个2个对象里面的数据不被java虚拟机管理，相当于C语言的存储方式。
     */
    private final FloatBuffer pVertex;
    private final FloatBuffer pTexCoord;

    //着色参数值 :
    //着色器脚本程序的handle(句柄)
    //包含绘制一个或多个shape的shader的OpenGL ES对象 ;
    private int hProgram;

    int maPositionLoc;
    int maTextureCoordLoc;
    int muMVPMatrixLoc;
    int muTexMatrixLoc;
    //mMvpMatrix是一种简称为“模型视图投影矩阵”
    private final float[] mMvpMatrix = new float[16];

    private static final int FLOAT_SZ = Float.SIZE / 8;
    private static final int VERTEX_NUM = 4;
    private static final int VERTEX_SZ = VERTEX_NUM * 2;

    /**
     * Constructor
     * this should be called in GL context
     * 这应该在GL的上下文中调用 ;
     * 设置顶点缓存 :
     */
    public GLDrawer2D() {   //设置顶点缓存 :
        //创建顺序缓冲器
        pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)   //初始化ByteBuffer ,长度为数组的长度 * float的长度 ;
                .order(ByteOrder.nativeOrder())     //数组排列用nativeOrder
                .asFloatBuffer();    //创建此缓冲区的视图，作为一个float缓冲区.
        pVertex.put(VERTICES);
        pVertex.flip();     //将缓存字节数组的指针设置为数组的开始序列即数组下标0

        pTexCoord = ByteBuffer.allocateDirect(TEXCOORD.length * FLOAT_SZ)     //初始化ByteBuffer ,长度为数组的长度 * float的长度 ;
                .order(ByteOrder.nativeOrder())     //数组排列用nativeOrder
                .asFloatBuffer();    //创建此缓冲区的视图，作为一个float缓冲区.
        //        pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)     //初始化ByteBuffer ,长度为数组的长度 * float的长度 ;
        //                .order(ByteOrder.nativeOrder())     //数组排列用nativeOrder
        //                .asFloatBuffer();    //创建此缓冲区的视图，作为一个float缓冲区.
        pTexCoord.put(TEXCOORD);
        pTexCoord.flip();   //将缓存字节数组的指针设置为数组的开始序列即数组下标0

        //真正的绘制纹理程序 :
        hProgram = loadShader(vss, fss);    //加载顶点/片段着色器
        GLES20.glUseProgram(hProgram);      //绘制时使用着色程序 , hProgram为着色参数 ;
        maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");      //位置索引 ;
        maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");  //texture绝对坐标索引 ;
        muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");       //返回一个于着色器程序中变量名为"uMVPMatrix"相关联的索引 ;
        muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");       //矩阵转换索引 ;

        Matrix.setIdentityM(mMvpMatrix, 0);
        //指定一个当前的textureParamHandle对象为一个全局的uniform 变量 :
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);     //通过投影和视图转换到着色
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0);     //通过投影和视图转换到着色

        //指定maPositionLoc和maTextureCoordLoc的数据值可以在什么地方访问.pVertex和pTexCoord在内部(NDK)是个指针,指向数组的第一组值的内存 ;
        GLES20.glVertexAttribPointer(maPositionLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);

        //在使用VertexAttribArray前必须先激活它 ;
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
    }

    /**
     * terminatinng, this should be called in GL context
     */
    public void release() {
        if (hProgram >= 0)
            GLES20.glDeleteProgram(hProgram);   //释放删除着色器脚本程序的handle(句柄)
        hProgram = -1;
    }

    /**
     * draw specific texture with specific texture matrix
     * 用特定的纹理矩阵绘制特定的纹理  (绘制Surface)
     *
     * @param tex_id     texture ID
     * @param tex_matrix texture matrix、if this is null, the last one use(we don't check size of this array and needs at least 16 of float)
     */
    public void draw(final int tex_id, final float[] tex_matrix) {
        GLES20.glUseProgram(hProgram);      //绘制时使用着色程序 , hProgram为着色参数 ;
        if (tex_matrix != null)
            //通过投影和视图转换到着色
            GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, tex_matrix, 0);
        //通过投影和视图转换到着色
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
        //启动纹理 :
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);     //设置的活动纹理单元为GPU中的纹理单元0
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex_id);     //绑定纹理到纹理tex[0]表示的纹理单元 ;
        //绘制三角形 :
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);       //GL_TRIANGLE_STRIP为绘制平滑三角形,
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);      //0是一个特殊的ID，他告诉OpenGL ES解除当前绑定对象。

        GLES20.glUseProgram(0);     //绘制时使用着色程序 , hProgram为着色参数 ;
    }

    /**
     * create external texture
     * 创建外部纹理 :
     * 接着初始化纹理
     *
     * @return texture ID
     */
    public static int initTex() {
        if (DEBUG)
            Log.v(TAG, "initTex:");
        final int[] tex = new int[1];
        //启用纹理 :
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);      //设置的活动纹理单元为GPU中的纹理单元0
        //生成纹理对象tex(用于存储纹理数据)
        /**
         * 指定生成n个纹理(第一个参数指定生成一个纹理)
         * tex数组将负责存储所有纹理的代号 ;
         *
         * 产生一个纹理Id,可以认为是纹理句柄，后面的操作将书用这个纹理id
         */
        GLES20.glGenTextures(1, tex, 0);
        //将绑定纹理(tex[0]表示指针指向纹理数据的初始位置)
        //通知OpenGL将texture纹理绑定到GL_TEXTURE_EXTERNAL_OES目标中 ;
        //绑定纹理到纹理tex[0]表示的纹理单元 ;
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);    //获取tex纹理数组中的第一个纹理;
        //设置在横向/纵向上都是 边缘纹理 方式 :
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        //设置纹理被缩小(距离视点很远时被缩小)时的滤波方式 :
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置纹理被放大(距离视点很近时被放大)时的滤波方式 :
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        return tex[0];
    }

    /**
     * delete specific texture
     */
    public static void deleteTex(final int hTex) {
        if (DEBUG)
            Log.v(TAG, "deleteTex:");
        final int[] tex = new int[]{hTex};
        GLES20.glDeleteTextures(1, tex, 0);
    }

    /**
     * load, compile and link shader
     * 加载顶点与片段着色器 ;
     *
     * @param vss source of vertex shader
     * @param fss source of fragment shader
     * @return
     */
    public static int loadShader(final String vss, final String fss) {  //可编程着色器
        if (DEBUG)
            Log.v(TAG, "loadShader:");
        //创建着色对象 它创建一个空的shader对象，它用于维护用来定义shader的源码字符串。支持以下两种shader : GL_VERTEX_SHADER / GL_FRAGMENT_SHADER
        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);    //GL_VERTEX_SHADER: 它运行在可编程的“顶点处理器”上，用于代替固定功能的顶点处理；
        //加载着色源     shader对象中原来的源码全部被新的源码所代替。
        GLES20.glShaderSource(vs, vss);
        //编译存储在shader对象中的源码字符串，编译结果被当作shader对象状态的一部分被保存起来，可通过glGetShaderiv函数获取编译状态。
        GLES20.glCompileShader(vs);
        final int[] compiled = new int[1];
        //检查编译状态 获取shader对象参数，参数包括：GL_SHADER_TYPE, GL_DELETE_STATUS, GL_COMPILE_STATUS, GL_INFO_LOG_LENGTH, GL_SHADER_SOURCE_LENGTH.
        GLES20.glGetShaderiv(vs, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {     //如果编译出错,则记录出错信息 ;
            if (DEBUG)
                Log.e(TAG, "Failed to compile vertex shader:"
                        + GLES20.glGetShaderInfoLog(vs));   //输出着色器的日志信息
            GLES20.glDeleteShader(vs);      //释放删除着色器句柄
            vs = 0;
        }

        //创建着色对象    它创建一个空的shader对象，它用于维护用来定义shader的源码字符串。支持以下两种shader : GL_VERTEX_SHADER / GL_FRAGMENT_SHADER
        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);      //GL_FRAGMENT_SHADER: 它运行在可编程的“片断处理器”上，用于代替固定功能的片段处理；
        //加载着色源     shader对象中原来的源码全部被新的源码所代替。
        GLES20.glShaderSource(fs, fss);
        //编译存储在shader对象中的源码字符串，编译结果被当作shader对象状态的一部分被保存起来，可通过glGetShaderiv函数获取编译状态。
        GLES20.glCompileShader(fs);
        //检查编译状态    获取shader对象参数，参数包括：GL_SHADER_TYPE, GL_DELETE_STATUS, GL_COMPILE_STATUS, GL_INFO_LOG_LENGTH, GL_SHADER_SOURCE_LENGTH.
        GLES20.glGetShaderiv(fs, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {     //如果编译出错,则记录出错信息 ;
            if (DEBUG)
                Log.w(TAG, "Failed to compile fragment shader:"
                        + GLES20.glGetShaderInfoLog(fs));   //输出着色器的日志信息
            GLES20.glDeleteShader(fs);      //释放删除着色器句柄
            fs = 0;
        }

        //创建一个渲染程序    建立一个空的program对象，shader对象可以被连接到program对像
        final int program = GLES20.glCreateProgram();
        //添加顶点着色程序  program对象提供了把需要做的事连接在一起的机制。在一个program中，在shader对象被连接在一起之前，必须先把shader连接到program上。
        GLES20.glAttachShader(program, vs);
        //添加片段着色程序  program对象提供了把需要做的事连接在一起的机制。在一个program中，在shader对象被连接在一起之前，必须先把shader连接到program上。
        GLES20.glAttachShader(program, fs);
        //链接计划  连接程序对象。
        //如果任何类型为GL_VERTEX_SHADER的shader对象连接到program,它将产生在“可编程顶点处理器”上可执行的程 序；
        //如果任何类型为GL_FRAGMENT_SHADER的shader对象连接到program,它将产生在“可编程片断处理器”上可执行的程序。
        GLES20.glLinkProgram(program);

        return program;
    }

    public float[] getMvpMatrxi() {
        return mMvpMatrix;
    }
}
