package com.project.pervsys.picaround.localDatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import static com.project.pervsys.picaround.utility.Config.*;

public class DBManager {

    private static final String TAG = "MapsActivity";
    private DBHelper mDbHelper;

    public DBManager(Context context) {
        mDbHelper = new DBHelper(context);
    }

    public void insert(String username){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(USERNAME, username);

        db.insert(USERNAMES, null, cv);
    }

    public boolean delete(String username) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        if (db.delete(USERNAMES, USERNAME + "=?", new String[]{username}) > 0)
            return true;
        else return false;
    }

    public Cursor query() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                USERNAMES,
                new String[] { USERNAME },
                null, null, null, null, null, null);

        return cursor;
    }

    public Cursor queryLike(String queryString) {
//        if (queryString.isEmpty())
//            return null;
//        else {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                USERNAMES,
                new String[]{USERNAME},
                USERNAME + " LIKE '%" + queryString + "%'",
                null, null, null, null, null);

        return cursor;
//        }
    }

    public void createTable(){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.execSQL(mDbHelper.CREATE_TABLE_USERNAMES);
    }

    public void dropTable(){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.execSQL(mDbHelper.DELETE_TABLE_USERNAMES);
    }
}
