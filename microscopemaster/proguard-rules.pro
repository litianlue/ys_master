# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/virgilyan/Documents/workspace/DevelopKit/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

#指定代码的压缩级别 :
-optimizationpasses 5

#是否使用大小写混合 :
-dontusemixedcaseclassnames

#是否混淆第三方jar :
-dontskipnonpubliclibraryclasses

#混淆是否做预校验 :
-dontpreverify

#混淆是否记录日志 :
-verbose
-printmapping priguardMapping.txt

#混淆时所采用的算法 :
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#保护给定的可选属性，例如LineNumberTable, LocalVariableTable, SourceFile, Deprecated, Synthetic, Signature, and InnerClasses.
-keepattributes Exceptions,InnerClasses,Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

-dontshrink
-dontoptimize



#去掉警告 :
#-dontwarn
#-dontskipnonpubliclibraryclassmembers

#死活编译不了,最后 只能在 混淆规则中 加入
#抑制警告 :
#-ignorewarnings

#导入第三方的类库,防止混淆时候读取包内容出错;
-libraryjars /src/main/libs/armeabi-v7a/libjpeg-turbo1400.so
-libraryjars /src/main/libs/armeabi-v7a/libusb100.so
-libraryjars /src/main/libs/armeabi-v7a/libuvc.so
-libraryjars /src/main/libs/armeabi-v7a/libUVCCamera.so

-libraryjars /src/main/jniLibs/armeabi-v7a/libvinit.so
-libraryjars /src/main/jniLibs/armeabi-v7a/libopencv_java3.so

#-libraryjars /src/main/libs/vitamio.jar

#过滤R文件的混淆 :
-keep class **.R$* {*;}
-keep public class **.R$* {*;}

#-------------系统类不需要混淆-------------
#AndroidMainfest中的类不混淆，四大组件和Application的子类和Framework层下所有的类默认不会进行混淆
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.app.IntentService
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.support.v4.**
-keep public class com.android.vending.licensing.ILicensingService
-keep public class android.webkit.**
-keep public class android.** {*;}
-keep public class javax.**

#jni方法不可混淆 :
#jni调用的java方法
#js调用java的方法
#java的native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep  class * implements com.yeespec.libuvccamera.usb.IButtonCallback
-keepnames class * implements com.yeespec.libuvccamera.usb.IButtonCallback
-keep  class * implements com.yeespec.libuvccamera.usb.IButtonCallback {*;}

-keep  class * implements com.yeespec.libuvccamera.usb.IFrameCallback
-keepnames class * implements com.yeespec.libuvccamera.usb.IFrameCallback
-keep  class * implements com.yeespec.libuvccamera.usb.IFrameCallback {*;}

-keep  class * implements com.yeespec.libuvccamera.usb.IStatusCallback
-keepnames class * implements com.yeespec.libuvccamera.usb.IStatusCallback
-keep  class * implements com.yeespec.libuvccamera.usb.IStatusCallback {*;}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context,android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context,android.util.AttributeSet,int);
}

-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}


#Parcelable的子类和Creator静态成员变量不混淆，否则会产生android.os.BadParcelableException异常
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepnames class * implements java.io.Serializable
-keep public class * implements java.io.Serializable {
    public *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保持自定义控件类不被混淆
-keepclassmembers class * extends android.app.Activity {
 public void *(android.view.View);
}


#--------------------eventbus避免混淆------------
-keepclassmembers class ** {
    public void onEvent*(**);
    void onEvent*(**);
}

-keepclassmembers class * {
    void *(**On*Event);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.facebook.** {*;}

#第三方包,不要混淆Annotation :
#-keep class * extends Java.lang.annotation.Annotation{*;}

#------------------不要混淆第三方类库 :-----------------
#过滤android.support.v4
-keep class android.support.v4.** { *;}
-keep interface android.support.v4.app.** {*;}
#过滤commons-httpclient-3.1.jar
-keep class org.apache.** {*;}
#过滤jackson-core-2.1.4.jar等:
-keep class com.fasterxml.jackson.** {*;}

#过滤xUtils-2.6.14.jar
-keep class com.lidroid.xutils.** {*;}


#反射用到的类不混淆(否则反射可能出现问题):
-keep class com.sun.** {*;}
-keep class io.netty.** {*;}
-keep class com.alibaba.** {*;}
-keep class com.alibaba.fastjson.** {*;}
#-libraryjars libs/fastjson-1.2.4.jar
-keep class com.aliyun.** {*;}
-keep class org.springframework.** {*;}
-keep class org.** {*;}
-keep class org.apache.** {*;}
-keep class org.slf4j.** {*;}
-keep class java.** {*;}
-keep class javax.** {*;}


#------------忽略异常提示------------------
#-dontwarn com.koushikdutta.**
#-dontwarn com.lidroid.xutils.**
#-dontwarn com.sun.**
-dontwarn io.netty.**
-dontwarn com.alibaba.**
-dontwarn org.apache.**
-dontwarn org.slf4j.**
#-dontwarn com.aliyun.**
#-dontwarn org.springframework.**
#-dontwarn org.**
#-dontwarn java.**
#-dontwarn javax.**
#-dontwarn android.support.v4.**
-dontwarn com.facebook.**

#keep是保持的意思,不混淆这些类:
#-keep public class * extends android.app.Activity

#keepclassmembers 保护指定类的成员,如果此类受到保护他们会保护的更好:
#-keepclassmembers class * extends android.app.Activity {
#                        public void * (android.view.View);
#                    }

#keepclasseswithmembers 不混淆类及其成员 :
#-keepclasseswithmembers class * {
#                            public <init>(android.content.Context, android.util.AttributeSet, int);
#                        }

#keepnames 不混淆类及其成员 :


# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}




