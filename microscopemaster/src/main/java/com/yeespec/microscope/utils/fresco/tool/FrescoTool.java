package com.yeespec.microscope.utils.fresco.tool;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import com.yeespec.microscope.utils.fresco.FrescoConfig;

/**
 * Created by mac on 15/5/4.
 */
public class FrescoTool {
    private static FrescoTool frescoTool;
    private FrescoConfig frescoConfig;
    private Context context;
    private ImagePipelineConfig imagePipelineConfig;

    private FrescoTool(Context context) {
        this.context = context;
        this.frescoConfig = FrescoConfig.getInstance(this.context);
    }

    private FrescoTool(Context context, FrescoConfig frescoConfig) {
        this.context = context;
        this.frescoConfig = frescoConfig;
    }

    public FrescoConfig getFrescoConfig() {
        return frescoConfig;
    }

    public void setFrescoConfig(FrescoConfig frescoConfig) {
        this.frescoConfig = frescoConfig;
    }

    /**
     * 获取单例实体对象（默认配置）
     *
     * @param context
     * @return
     */
    public static FrescoTool getInstance(Context context) {
        if (frescoTool == null) {
            frescoTool = new FrescoTool(context);
        }
        return frescoTool;
    }
    public static void frescoToolNull() {
        frescoTool = null;
    }

    /**
     * 获取单例实体对象
     *
     * @param context
     * @param frescoConfig Fresco 自定义的配置
     * @return
     */
    public static FrescoTool getInstance(Context context, FrescoConfig frescoConfig) {
        if (frescoTool == null) {
            frescoTool = new FrescoTool(context, frescoConfig);
        }
        return frescoTool;
    }

    /**
     * 初始化fresco
     */
    public void init() {
        imagePipelineConfig = frescoConfig.init();
        Fresco.initialize(context, imagePipelineConfig);
    }


    /**
     * 显示图片
     *
     * @param lowUrl           低质量图片URL
     * @param url              图片URL
     * @param simpleDraweeView 显示图片的view
     * @param height           高度
     * @param width            宽度
     * @param listener         监听器
     * @param postprocessor    处理器
     */
    public static void displayImage(String lowUrl, String url, DraweeView simpleDraweeView, int height, int width, ControllerListener listener, Postprocessor postprocessor) {
        Uri uri = Uri.parse(url);
        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
                .setLocalThumbnailPreviewsEnabled(true)
                .setAutoRotateEnabled(true)
                .setProgressiveRenderingEnabled(true);

        if (height != -1 && width != -1) {
            imageRequestBuilder.setResizeOptions(new ResizeOptions(width, height));
        }

        if (postprocessor != null) {
            imageRequestBuilder.setPostprocessor(postprocessor);
        }

        ImageRequest imageRequest = imageRequestBuilder.build();

        PipelineDraweeControllerBuilder pipelineDraweeControllerBuilder = Fresco.newDraweeControllerBuilder()
                .setAutoPlayAnimations(true)//自动播放图片动画
                .setImageRequest(imageRequest)//设置单个图片请求～～～不可与setFirstAvailableImageRequests共用，配合setLowResImageRequest为高分辨率的图
                .setOldController(simpleDraweeView.getController())//DraweeController复用
                .setTapToRetryEnabled(true);

        if (lowUrl != null) {
            Uri lowUri = Uri.parse(lowUrl);
            ImageRequest lowImageRequest = ImageRequestBuilder.newBuilderWithSource(lowUri)
                    .build();
            pipelineDraweeControllerBuilder.setLowResImageRequest(lowImageRequest);
        }

        if (listener != null) {
            pipelineDraweeControllerBuilder.setControllerListener(listener);//监听图片下载完毕等
        }

        DraweeController draweeController = pipelineDraweeControllerBuilder.build();

        simpleDraweeView.setController(draweeController);
    }

    /**
     * 显示图片
     *
     * @param url              url地址
     * @param simpleDraweeView 显示图片View
     * @param listener         监听器
     */
    public static void displayImage(String url, DraweeView simpleDraweeView, ControllerListener listener) {
        displayImage(null, url, simpleDraweeView, -1, -1, listener, null);
    }

    /**
     * 显示图片
     *
     * @param url              url地址
     * @param simpleDraweeView 显示图片View
     * @param height           高度
     * @param width            宽度
     * @param listener         监听器
     */
    public static void displayImage(String url, DraweeView simpleDraweeView, int height, int width, ControllerListener listener) {

        displayImage(null, url, simpleDraweeView, height, width, listener, null);
    }

    /**
     * 显示图片
     *
     * @param url              url地址
     * @param simpleDraweeView 显示图片View
     */
    public static void displayImage(String url, DraweeView simpleDraweeView) {
        displayImage(null, url, simpleDraweeView, -1, -1, null, null);
    }


}
