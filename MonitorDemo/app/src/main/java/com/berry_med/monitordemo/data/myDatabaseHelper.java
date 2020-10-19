package com.berry_med.monitordemo.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class myDatabaseHelper extends SQLiteOpenHelper {
    public static final String CREATE_INF="create table Information("
            + "id text,"
            +"number text,"
            +"time text)";
    public static final String CREATE_ECG="create table ECG("
            +"Iid text,"
            +"number text,"
            +"heartRate text,"
            +"restRate text,"
            +"status text)";
    public static final String CREATE_NIBP="create table NIBP ("
            +"Iid text,"
            +"number text,"
            +"data text)";
    public static final String CREATE_SPO2="create table SPO2("
            +"Iid text,"
            +"number text,"
            +"data text)";
    public static final String CREATE_SPO2WAVE="create table SPO2WAVE("
            +"Iid text,"
            +"number text,"
            +"data text)";

    public  myDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,int version)
    {
        super(context,name,factory,version);
    }
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(CREATE_INF);
        //db.execSQL(CREATE_ECG);
        //db.execSQL(CREATE_NIBP);
        db.execSQL(CREATE_SPO2);
        db.execSQL(CREATE_SPO2WAVE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
