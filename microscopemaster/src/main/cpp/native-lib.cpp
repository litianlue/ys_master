#include <jni.h>
#include <cmath>
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>
#include <android/bitmap.h>//
#define LOG_TAG "libibmphotophun"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define NN 2
#define MM NN/2

#define RGB565_R(p) ((((p) & 0xF800) >> 11) << 3)
#define RGB565_G(p) ((((p) & 0x7E0 ) >> 5)  << 2)
#define RGB565_B(p) ( ((p) & 0x1F  )        << 3)
#define MAKE_RGB565(r,g,b) ((((r) >> 3) << 11) | (((g) >> 2) << 5) | ((b) >> 3))

#define RGBA_A(p) (((p) & 0xFF000000) >> 24)
#define RGBA_R(p) (((p) & 0x00FF0000) >> 16)
#define RGBA_G(p) (((p) & 0x0000FF00) >>  8)
#define RGBA_B(p)  ((p) & 0x000000FF)
#define MAKE_RGB565(r,g,b) ((((r) >> 3) << 11) | (((g) >> 2) << 5) | ((b) >> 3))

#define RGBA_A(p) (((p) & 0xFF000000) >> 24)
#define RGBA_R(p) (((p) & 0x00FF0000) >> 16)
#define RGBA_G(p) (((p) & 0x0000FF00) >>  8)
#define RGBA_B(p)  ((p) & 0x000000FF)
#define MAKE_RGBA(r,g,b,a) (((a) << 24) | ((r) << 16) | ((g) << 8) | (b))

extern "C"


jdouble Java_com_yeespec_libuvccamera_uvccamera_glutils_JniUtils_getFreShnesFormC( JNIEnv *env, jobject /* this */, unsigned char *pBuffer, int height, int width, int bpp)
{


    unsigned char *buf=(unsigned char *)pBuffer;
    ///////////////////////////////////将传入图像均分9等分
    int temph=height/9;
    int tempw=width/9;
    int x,y,tempx,tempy;
    int i,j,k;

    int value=0;
    float *gray;
    float graythreshold=200,*gray1,*gray2,*gray3,*gray4,*gray5,*gray6,*gray7,*gray8,*gray9;
    float avg1=0,avg2=0,avg3=0,avg4=0,avg5=0,avg6=0,avg7=0,avg8=0,avg9=0;
    float temp[3];
    int lineByte=(width*bpp/8+3)/4*4;//width*3;

    gray1=new float[NN*NN];
    gray2=new float[NN*NN];
    gray3=new float[NN*NN];
    gray4=new float[NN*NN];
    gray5=new float[NN*NN];
    gray6=new float[NN*NN];
    gray7=new float[NN*NN];
    gray8=new float[NN*NN];
    gray9=new float[NN*NN];
    int xxx=height/2;
    int yyy=width/2;

    int graycount=0;/////////////计数器

    tempx=2*temph;
    y=4*tempw;
    //return 2;
    for( i=0;i<NN;i++)
    {
        for( j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(tempx+MM-i)*lineByte+(y-MM+j)*3+k++);
            }
            gray1[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg1+=gray1[i];
    }
    avg1=avg1/graycount;
    graycount=0;
    tempx=4*temph;
    y=4*tempw;
    for( i=0;i<NN;i++)
    {
        for( j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(tempx+MM-i)*lineByte+(y-MM+j)*3+k++);
            }
            gray2[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg2+=gray2[i];
    }
    avg2=avg2/graycount;
    graycount=0;
    tempx=6*temph;
    y=4*tempw;
    for( i=0;i<NN;i++)
    {
        for( j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(tempx+MM-i)*lineByte+(y-MM+j)*3+k++);
            }
            gray3[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg3+=gray3[i];
    }
    avg3=avg3/graycount;

    graycount=0;
    tempx=3*temph;
    y=3*tempw;
    for(i=0;i<NN;i++)
    {
        for(j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(tempx+MM-i)*lineByte+(y-MM+j)*3+k++);
            }
            gray4[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg4+=gray4[i];
    }
    avg4=avg4/graycount;

    graycount=0;
    tempx=5*temph;
    y=3*tempw;
    for(i=0;i<NN;i++)
    {
        for(j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(tempx+MM-i)*lineByte+(y-MM+j)*3+k++);
            }
            gray5[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg5+=gray5[i];
    }
    avg5=avg5/graycount;

    graycount=0;
    tempx=3*temph;
    y=5*tempw;
    for(i=0;i<NN;i++)
    {
        for(j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(tempx+MM-i)*lineByte+(y-MM+j)*3+k++);
            }
            gray6[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg6+=gray6[i];
    }
    avg6=avg6/graycount;

    graycount=0;
    tempx=5*temph;
    y=5*tempw;
    for(i=0;i<NN;i++)
    {
        for(j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(tempx+MM-i)*lineByte+(y-MM+j)*3+k++);
            }
            gray7[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg7+=gray7[i];
    }
    avg7=avg7/graycount;

    graycount=0;
    tempy=2*tempw;
    x=4*temph;
    for(i=0;i<NN;i++)
    {
        for(j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(x+MM-i)*lineByte+(tempy-MM+j)*3+k++);
            }
            gray8[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg8+=gray8[i];
    }
    avg8=avg8/graycount;

    graycount=0;
    tempy=6*tempw;
    x=4*temph;
    for(i=0;i<NN;i++)
    {
        for(j=0;j<NN;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+(x+MM-i)*lineByte+(tempy-MM+j)*3+k++);
            }
            gray9[graycount]=(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            graycount++;
            value=0;
        }
    }
    for (i=0;i<graycount;i++)
    {
        avg9+=gray9[i];
    }
    avg9=avg9/graycount;

    float S1=0,S2=0,S3=0,S4=0,S5=0,S6=0,S7=0,S8=0,S9=0;

    if (avg1<graythreshold)
    {
        float xx=avg1;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray1[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S1=S1+b;//E(xi-x~)^2
        }
        S1=S1/(graycount);

        S1=sqrt(S1);
    }
    if (avg2<graythreshold)
    {
        float xx=avg2;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray2[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S2=S2+b;//E(xi-x~)^2
        }
        S2=S2/(graycount);

        S2=sqrt(S2);
    }
    if (avg3<graythreshold)
    {
        float xx=avg3;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray3[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S3=S3+b;//E(xi-x~)^2
        }
        S3=S3/(graycount);

        S3=sqrt(S3);
    }
    if (avg4<graythreshold)
    {
        float xx=avg4;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray4[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S4=S4+b;//E(xi-x~)^2
        }
        S4=S4/(graycount);

        S4=sqrt(S4);
    }
    if (avg5<graythreshold)
    {
        float xx=avg5;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray5[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S5=S5+b;//E(xi-x~)^2
        }
        S5=S5/(graycount);

        S5=sqrt(S5);
    }
    if (avg6<graythreshold)
    {
        float xx=avg6;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray6[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S6=S6+b;//E(xi-x~)^2
        }
        S6=S6/(graycount);

        S6=sqrt(S6);
    }
    if (avg7<graythreshold)
    {
        float xx=avg7;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray7[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S7=S7+b;//E(xi-x~)^2
        }
        S7=S7/(graycount);

        S7=sqrt(S7);
    }
    if (avg8<graythreshold)
    {
        float xx=avg8;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray8[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S8=S8+b;//E(xi-x~)^2
        }
        S8=S8/(graycount);

        S8=sqrt(S8);
    }
    if (avg9<graythreshold)
    {
        float xx=avg9;//    x~=Egi/n
        for(j=0;j<graycount;j++)
        {
            float b=gray9[j]-xx;//xi-x~
            b=b*b;//(xi-x~)^2
            S9=S9+b;//E(xi-x~)^2
        }
        S9=S9/(graycount);

        S9=sqrt(S9);
    }
    float SUM=S1+S2+S3+S4+S5+S6+S7+S8+S9;
    SUM=sqrt(SUM);

    float Average = (avg1+avg2+avg3+avg4+avg5+avg6+avg7+avg8+avg9)/9;
    SUM = SUM / Average;

    delete []gray1;
    delete []gray2;
    delete []gray3;
    delete []gray4;
    delete []gray5;
    delete []gray6;
    delete []gray7;
    delete []gray8;
    delete []gray9;
    return SUM;
}
extern "C"
jdouble Java_com_yeespec_libuvccamera_uvccamera_glutils_JniUtils_getBitmapGamma(JNIEnv *env, jobject obj,
                                                                                jbyte *pBuffer, int height, int width, int ts, int row)
{
    // unsigned char *buf=(unsigned char *)pBuffer;
    int i,j,k;
    float temp[3];
    int value = 0;
    int lineByte = (width*4/8+3)/4*4;//width*3;
    float black=0,white=0;
//    int len=env->GetArrayLength(pBuffer);
//
//    jbyte *buf = env->GetByteArrayElements(pBuffer,NULL);


    //  env->SetByteArrayRegion(pBuffer,0,len,buf);

    for( i=0;i<height;i++)
    {
        for( j=0;j<width;j++)
        {

//             temp[0] = *(buf+i*height/3);
//            for(k=0;k<3;)
//            {
//                temp[value++]=*(buf+ i*lineByte + j*3 + k++);
//            }
            float grayCell =(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            if(grayCell>ts){
                white++;
            } else{
                black++;
            }
            value=0;
        }
    }

    //float percentage = (black/(white+black))*100;
    return *(pBuffer+5000);
}
extern "C"
jdouble Java_com_yeespec_libuvccamera_uvccamera_glutils_JniUtils_getOperationBrignes(JNIEnv *env, jobject obj, jobject bitmap)
{
    AndroidBitmapInfo srcInfo;
    int graycount = 0;
    float Whitegray = 0;
    int result = 0;
    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_getInfo(env, bitmap, &srcInfo)) {
        LOGE("get bitmap info failed");
        return 0;
    }

    void *srcBuf, *dstBuf;
    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_lockPixels(env, bitmap, &srcBuf)) {
        LOGE("lock src bitmap failed");
        return 0;
    }

    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_lockPixels(env, bitmap, &dstBuf)) {
        LOGE("lock dst bitmap failed");
        return 0;
    }

    int w = srcInfo.width;
    int h = srcInfo.height;
    float *grayCell;
    grayCell = new float[w * h];
    int32_t *srcPixs = (int32_t *) srcBuf;
    int32_t *desPixs = (int32_t *) dstBuf;
    int alpha = 0xFF << 24;
    int i, j;
    int color;
    int red;
    int green;
    int blue;
    for (i = 0; i < h; i++) {
        for (j = 0; j < w; j++) {
            color = srcPixs[w * i + j];
            red = ((color & 0x00FF0000) >> 16);
            green = ((color & 0x0000FF00) >> 8);
            blue = color & 0x000000FF;
            int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
            grayCell[graycount]= gray;
            graycount++;
            j += 8;
        }
        i += 8;
    }



    for (int i = 0; i < graycount; i++) {
        Whitegray += grayCell[i];
    }
    if (graycount != 0)
        result = (int) (Whitegray / graycount);
    return result;
}
extern "C"
jdouble Java_com_yeespec_libuvccamera_uvccamera_glutils_JniUtils_getFreshnes_AVG(JNIEnv *env, jobject /* this */, unsigned char *pBuffer, int height, int width)
{
    unsigned char *buf=(unsigned char *)pBuffer;
    int i,j,k;
    float temp[3];
    int value = 0;
    int lineByte = (width*4/8+3)/4*4;//width*3;
    float *grayCell;
    grayCell = new float[height * width];
    int graycount = 0;
    float Whitegray = 0;
    float result = 0;
    for( i=0;i<height;i++)
    {
        for( j=0;j<width;j++)
        {
            for(k=0;k<3;)
            {
                temp[value++]=*(buf+ i*lineByte + j*3 + k++);
            }
            float grayCell2 =(float)(0.11*temp[2]+0.59*temp[1]+0.3*temp[0]);
            grayCell[graycount]= grayCell2;
            graycount++;
            value=0;
            j = j + 8;
        }
        i = i + 8;
    }
    for (int i = 0; i < graycount; i++) {
        Whitegray += grayCell[i];
    }
    if (graycount != 0)
        result = (int) (Whitegray / graycount);
    return result;
}
extern "C"
jdouble Java_com_yeespec_libuvccamera_uvccamera_glutils_JniUtils_changeBrightness(JNIEnv *env, jobject obj,  int direction,jobject bitmap) {
    //https://blog.csdn.net/xjwangliang/article/details/7065670

    AndroidBitmapInfo srcInfo;
    float black=0,white=0;
    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_getInfo(env, bitmap, &srcInfo)) {
        LOGE("get bitmap info failed");
        return 0;
    }

    void *srcBuf, *dstBuf;
    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_lockPixels(env, bitmap, &srcBuf)) {
        LOGE("lock src bitmap failed");
        return 0;
    }

    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_lockPixels(env, bitmap, &dstBuf)) {
        LOGE("lock dst bitmap failed");
        return 0;
    }
    int w = srcInfo.width;
    int h = srcInfo.height;
    int32_t *srcPixs = (int32_t *) srcBuf;
    int32_t *desPixs = (int32_t *) dstBuf;
     int alpha = 0xFF << 24;
    int i, j;
    int color;
    int red;
    int green;
    int blue;
    for (i = 0; i < h; i++) {
        for (j = 0; j < w; j++) {
            color = srcPixs[w * i + j];
            red = ((color & 0x00FF0000) >> 16);
            green = ((color & 0x0000FF00) >> 8);
            blue = color & 0x000000FF;
            int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
            if(gray>=direction){
                white++;
            } else{
                black++;
            }

        }
    }

    float percentage = (black/(white+black))*100;
    AndroidBitmap_unlockPixels(env,bitmap);
    return percentage;
}
extern "C"
jdouble Java_com_yeespec_libuvccamera_uvccamera_glutils_JniUtils_fillOnePixelBitmap(JNIEnv *env, jobject obj, jobject bitmap)
{
    AndroidBitmapInfo srcInfo;
    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_getInfo(env, bitmap, &srcInfo)) {
        LOGE("get bitmap info failed");
        return 0;
    }

    void *srcBuf, *dstBuf;
    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_lockPixels(env, bitmap, &srcBuf)) {
        LOGE("lock src bitmap failed");
        return 0;
    }

    if (ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_lockPixels(env, bitmap, &dstBuf)) {
        LOGE("lock dst bitmap failed");
        return 0;
    }
    int w = srcInfo.width;
    int h = srcInfo.height;
    int32_t *srcPixs = (int32_t *) srcBuf;
    int32_t *desPixs = (int32_t *) dstBuf;
   // int alpha = 0xFF << 24;
    int i, j;
    int color;
//    int red;
//    int green;
//    int blue;
    //https://blog.csdn.net/dreamInTheWorld/article/details/78717205
    for (i = 0; i < h; i++) {
        for (j = 0; j < w; j++) {

            if(j==(w-1)){
                color = srcPixs[w * i + (j-1)];
                //int pixel = origin.getPixel(i-1,j);
                //origin.setPixel(i,j,pixel);
                desPixs[w * i + j] = color;
            }
            if(i==0){ //横
                color = srcPixs[w * (i+1) + j];
                //int pixel2 = origin.getPixel(i,j+1);
                //origin.setPixel(i,j,pixel2);
                desPixs[w * i + j] = color;

            }


//            red = ((color & 0x00FF0000) >> 16);
//            green = ((color & 0x0000FF00) >> 8);
//            blue = color & 0x000000FF;
//            color = (red + green + blue) / 3;
//            color = alpha | (color << 16) | (color << 8) | color;
//            desPixs[w * i + j] = color;
        }
    }
    AndroidBitmap_unlockPixels(env, bitmap);

    return 1;
}