package com.yeespec.microscope.utils.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/7/4.
 */

public class MSQLUtil {

    private static DBOpneHelper dbOpenHelper;

    private static SQLiteDatabase sqLiteDb;

    public static void mPut(Context context, ConfigurationParameter param) {
        dbOpenHelper = new DBOpneHelper(context, DBOpneHelper.DB_NAME, 1);
        sqLiteDb = dbOpenHelper.getReadableDatabase();
        ConfigurationParameter parameter;
        parameter = findByUserandLight(param.getUser(), String.valueOf(param.getStimulatedLight()),param.getMultiple()+"");
        if (parameter == null) {
            add(param);
        }else {
            updata(param);
        }
        dbOpenHelper.close();
        sqLiteDb.close();
    }

    public static ConfigurationParameter mGet(Context context, String userName, String light,String contration) {
        dbOpenHelper = new DBOpneHelper(context,  DBOpneHelper.DB_NAME, 1);
        sqLiteDb = dbOpenHelper.getReadableDatabase();

        ConfigurationParameter parameter = null;

        parameter = findByUserandLight(userName, light,contration);
        dbOpenHelper.close();
        sqLiteDb.close();
        return parameter;
    }

    /**
     * 增，用insert向数据库中插入数据
     */

    public static void add(ConfigurationParameter p) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
       // values.put(DBOpneHelper._ID, p.getId());
        values.put(DBOpneHelper.USERNAME, p.getUser());
        values.put(DBOpneHelper.MULTIPLE, p.getMultiple());
        values.put(DBOpneHelper.LIGHT, p.getStimulatedLight());
        values.put(DBOpneHelper.TINTING, p.getTinting());
        values.put(DBOpneHelper.SENSITIVITY, p.getSensitivity());
        values.put(DBOpneHelper.BRIGHTNESS, p.getBrightness());
        values.put(DBOpneHelper.TINTING_STRING, p.getTintingString());
        values.put(DBOpneHelper.GAMMA, p.getGamma());
        if(db.isOpen())
        db.insert(DBOpneHelper.CP_TABLE, null, values);
        db.close();
    }

    /**
     * 删，通过用户名删除数据
     */
    public static void delete(String username) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        if(db.isOpen())
        db.delete(DBOpneHelper.CP_TABLE, DBOpneHelper.USERNAME + "=?", new String[]{username});
    }

    /**
     * 改，修改指定用户名下的激发块数据
     */
    public static void updata(ConfigurationParameter p) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        //values.put(DBOpneHelper._ID, p.getId());
        values.put(DBOpneHelper.USERNAME, p.getUser());
        values.put(DBOpneHelper.MULTIPLE, p.getMultiple());
        values.put(DBOpneHelper.LIGHT, p.getStimulatedLight());
        values.put(DBOpneHelper.TINTING, p.getTinting());
        values.put(DBOpneHelper.SENSITIVITY, p.getSensitivity());
        values.put(DBOpneHelper.BRIGHTNESS, p.getBrightness());
        values.put(DBOpneHelper.TINTING_STRING, p.getTintingString());
        values.put(DBOpneHelper.GAMMA, p.getGamma());

        if(db.isOpen())
        db.update(DBOpneHelper.CP_TABLE, values, DBOpneHelper.USERNAME + " = ? and " + DBOpneHelper.LIGHT + " = ? and "+DBOpneHelper.MULTIPLE+" = ?", new String[]{p.getUser(),String.valueOf(p.getStimulatedLight()),p.getMultiple()+""});
        db.close();
    }

    /**
     * 查，查询表中所有的数据
     */
    public static List<ConfigurationParameter> find() {
        List<ConfigurationParameter> ps = null;
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DBOpneHelper.CP_TABLE, null, null, null, null, null, null);
        if (cursor != null) {
            ps = new ArrayList<ConfigurationParameter>();
            while (cursor.moveToNext()) {

                ConfigurationParameter p = new ConfigurationParameter();
               // int _id = cursor.getInt(cursor.getColumnIndex(DBOpneHelper._ID));
                String name = cursor.getString(cursor.getColumnIndex(DBOpneHelper.USERNAME));
                String multiple = cursor.getString(cursor.getColumnIndex(DBOpneHelper.MULTIPLE));
                int stimulatedLight = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.LIGHT));
                int tinting = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.TINTING));
                int sensitivity = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.SENSITIVITY));
                int brightness = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.BRIGHTNESS));
                String tintingString = cursor.getString(cursor.getColumnIndex(DBOpneHelper.TINTING_STRING));
                int gamma = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.GAMMA));

               // p.setId(_id);
                p.setUser(name);
                p.setMultiple(multiple);
                p.setStimulatedLight(stimulatedLight);
                p.setTinting(tinting);
                p.setSensitivity(sensitivity);
                p.setBrightness(brightness);
                p.setTintingString(tintingString);
                p.setGamma(gamma);
                ps.add(p);
            }
        }
        cursor.close();
        return ps;
    }

    /**
     * 查询指定用户名下的激发块数据
     */
    public static ConfigurationParameter findByUserandLight(String username, String light,String contrion) {

        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DBOpneHelper.CP_TABLE,null, DBOpneHelper.USERNAME + " = ? and " + DBOpneHelper.LIGHT + " = ? and "+DBOpneHelper.MULTIPLE+" = ?", new String[]{username, light,contrion}, null, null, null);
       // Cursor cursor = db.query(DBOpneHelper.CP_TABLE, new String[]{DBOpneHelper.LIGHT}, DBOpneHelper.LIGHT +"=?", new String[]{light}, null, null,null);
         if(cursor==null) {
             cursor.close();
             return null;
         }
        ConfigurationParameter p = new ConfigurationParameter();


        if (cursor != null&&cursor.moveToFirst()) {

            int _id = cursor.getInt(cursor.getColumnIndex(DBOpneHelper._ID));

            String name = cursor.getString(cursor.getColumnIndex(DBOpneHelper.USERNAME));
            String multiple = cursor.getString(cursor.getColumnIndex(DBOpneHelper.MULTIPLE));

            int stimulatedLight = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.LIGHT));
            int tinting = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.TINTING));
            int sensitivity = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.SENSITIVITY));
            int brightness = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.BRIGHTNESS));
            String tintingString = cursor.getString(cursor.getColumnIndex(DBOpneHelper.TINTING_STRING));
            int gamma = cursor.getInt(cursor.getColumnIndex(DBOpneHelper.GAMMA));

            p.setId(_id);
            p.setUser(name);
            p.setMultiple(multiple);
            p.setStimulatedLight(stimulatedLight);
            p.setTinting(tinting);
            p.setSensitivity(sensitivity);
            p.setBrightness(brightness);
            p.setTintingString(tintingString);
            p.setGamma(gamma);
            cursor.close();
            db.close();
            return p;
        }else {
            cursor.close();
            db.close();
            return null;
        }


    }
}
