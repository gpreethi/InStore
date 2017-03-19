package com.example.android.instore.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static com.example.android.instore.R.id.price;
import static com.example.android.instore.R.id.quantity;

/**
 * Created by Preethi on 2/5/17.
 */

public class TrackerProvider extends ContentProvider {


    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = TrackerProvider.class.getSimpleName();

    /**
     * URI matcher code for the content URI for the trackers table
     */
    private static final int TRACKERS = 100;

    /**
     * URI matcher code for the content URI for a single tracker in the trackers table
     */
    private static final int TRACKER_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.trackers/trackers" will map to the
        // integer code {@link #TRACKERS}. This URI is used to provide access to MULTIPLE rows
        // of the trackers table.
        sUriMatcher.addURI(TrackerContract.CONTENT_AUTHORITY, TrackerContract.PATH_TRACKERS, TRACKERS);

        // The content URI of the form "content://com.example.android.trackers/trackers/#" will map to the
        // integer code {@link #TRACKER_ID}. This URI is used to provide access to ONE single row
        // of the trackers table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.trackers/trackers/3" matches, but
        // "content://com.example.android.trackers/trackers" (without a number at the end) doesn't match.
        sUriMatcher.addURI(TrackerContract.CONTENT_AUTHORITY, TrackerContract.PATH_TRACKERS + "/#", TRACKER_ID);
    }

    /**
     * Database helper object
     */
    private TrackerDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new TrackerDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACKERS:
                // For the TRACKERS code, query the trackers table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the trackers table.
                cursor = database.query(TrackerContract.TrackerEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case TRACKER_ID:
                // For the TRACKER_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.trackers/trackers/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = TrackerContract.TrackerEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the trackers table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(TrackerContract.TrackerEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACKERS:
                return insertTracker(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a tracker into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertTracker(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(TrackerContract.TrackerEntry.COLUMN_TRACKER_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Tracker requires a name");
        }

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new tracker with the given values
        long id = database.insert(TrackerContract.TrackerEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the tracker content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACKERS:
                return updateTracker(uri, contentValues, selection, selectionArgs);
            case TRACKER_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = TrackerContract.TrackerEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateTracker(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update trackers in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more trackers).
     * Return the number of rows that were successfully updated.
     */
    private int updateTracker(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the {@link TrackerEntry#COLUMN_PET_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(TrackerContract.TrackerEntry.COLUMN_TRACKER_NAME)) {
            String name = values.getAsString(TrackerContract.TrackerEntry.COLUMN_TRACKER_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Tracker requires a name");
            }
        }

        // If the {@link TrackerEntry#COLUMN_PET_GENDER} key is present,
        // check that the gender value is valid.
        if (values.containsKey(TrackerContract.TrackerEntry.COLUMN_TRACKER_QUANTITY)) {
            Integer quantity = values.getAsInteger(TrackerContract.TrackerEntry.COLUMN_TRACKER_QUANTITY);
            if (quantity == null && quantity < 0) {
                throw new IllegalArgumentException("Tracker requires valid quantity");
            }
        }

        // If the {@link TrackerEntry#COLUMN_TRACKER_WEIGHT} key is present,

        if (values.containsKey(TrackerContract.TrackerEntry.COLUMN_TRACKER_PRICE)) {
            // Check that the weight is greater than or equal to 0 kg
            Integer price = values.getAsInteger(TrackerContract.TrackerEntry.COLUMN_TRACKER_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Tracker requires valid price");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(TrackerContract.TrackerEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACKERS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(TrackerContract.TrackerEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case TRACKER_ID:
                // Delete a single row given by the ID in the URI
                selection = TrackerContract.TrackerEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(TrackerContract.TrackerEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACKERS:
                return TrackerContract.TrackerEntry.CONTENT_LIST_TYPE;
            case TRACKER_ID:
                return TrackerContract.TrackerEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
