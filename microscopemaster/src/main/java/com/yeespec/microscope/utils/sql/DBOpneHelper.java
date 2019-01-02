package com.yeespec.microscope.utils.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2017/7/4.
 */

public class DBOpneHelper extends SQLiteOpenHelper {

    private static final int VERSION = 1;//版本
    public static final String DB_NAME = "MPreferencesParameter.db";//数据库名
    public static final String CP_TABLE = "mcptable";//表名
    public static final String _ID = "_id";//表中的列名
    public static final String USERNAME = "name";//用户名
    public static final String MULTIPLE = "multiple";//物镜倍数
    public static final String LIGHT = "lights";//灯光
    public static final String TINTING ="tinting";
    public static final String SENSITIVITY ="sensitivity";
    public static final String  BRIGHTNESS="brightness";
    public static final String TINTING_STRING ="tinting_string";
    public static final String  GAMMA="gamma";

    //创建数据库语句，STUDENT_TABLE，_ID ，NAME的前后都要加空格
    private static final String CREATE_TABLE = "create table " + CP_TABLE + " ( " + _ID + " Integer primary key autoincrement,"+
            USERNAME+" varchar(50) NOT NULL , " +
            MULTIPLE+" varchar(20) NOT NULL , " +
            LIGHT+" stimulated_light integer, " +
            TINTING+" integer , " +
            SENSITIVITY+" integer , " +
            BRIGHTNESS+" integer  ," +
            TINTING_STRING+" varchar(20) ," +
            GAMMA+" integer  " +
            " ); ";

    public DBOpneHelper(Context context, String name, int version) {
        super(context, name, null, version);
        //super(context, DB_NAME, null, version);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + CP_TABLE;
        db.execSQL(sql);
        onCreate(db);
    }
}
