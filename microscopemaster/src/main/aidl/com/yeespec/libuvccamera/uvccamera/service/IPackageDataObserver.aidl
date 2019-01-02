// IPackageDataObserver.aidl
package com.yeespec.libuvccamera.uvccamera.service;

// Declare any non-default types here with import statements

  interface IPackageDataObserver {
     void onRemoveCompleted(in String packageName, boolean succeeded);

}
