package com.example.android.instore.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Preethi on 2/4/17.
 */

public class TrackerDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = TrackerDbHelper.class.getSimpleName();

    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "trackers.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructs a new instance of {@link TrackerDbHelper}.
     *
     * @param context of the app
     */
    public TrackerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the trackers table
        String SQL_CREATE_TRACKERS_TABLE = "CREATE TABLE " + TrackerContract.TrackerEntry.TABLE_NAME + " ("
                + TrackerContract.TrackerEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TrackerContract.TrackerEntry.COLUMN_TRACKER_NAME + " TEXT NOT NULL, "
                + TrackerContract.TrackerEntry.COLUMN_TRACKER_QUANTITY + " INTEGER NOT NULL, "
                + TrackerContract.TrackerEntry.COLUMN_TRACKER_PRICE + " INTEGER NOT NULL, "
                + TrackerContract.TrackerEntry.COLUMN_TRACKER_IMAGE + " BLOB, "
                + TrackerContract.TrackerEntry.COLUMN_TRACKER_VENDOR + " TEXT NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_TRACKERS_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The database is still at version 1, so there's nothing to do be done here.
    }


}
