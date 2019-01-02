package com.yeespec.microscope.utils;

import java.io.File;
import java.util.Comparator;

/**
 * Created by Administrator on 2017/6/13.
 */

public class MyFileComparator  implements Comparator<File> {
    public int compare(File file1, File file2) {
        if (file1.lastModified() > file2.lastModified()) {
            return -1;
        } else {
            return 1;
        }
    }
}
