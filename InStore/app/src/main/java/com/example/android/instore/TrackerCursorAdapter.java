package com.example.android.instore;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.instore.data.TrackerContract;

import static android.R.attr.id;
import static android.R.id.message;
import static com.example.android.instore.R.id.quantity;
import static java.security.AccessController.getContext;

/**
 * Created by Preethi on 2/11/17.
 */

public class TrackerCursorAdapter extends CursorAdapter {

    //  static String trackerName;

    /**
     * Constructs a new {@link TrackerCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public TrackerCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the tracker data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current pet can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, final Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);

        TextView priceTextView = (TextView) view.findViewById(R.id.price);

        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        int quantityColumnIndex = cursor.getColumnIndex(TrackerContract.TrackerEntry.COLUMN_TRACKER_QUANTITY);
        final int quantityTable = cursor.getInt(quantityColumnIndex);
        int idColumnIndex = cursor.getColumnIndex(TrackerContract.TrackerEntry._ID);
        final long id = cursor.getLong(idColumnIndex);

        Button sellButton = (Button) view.findViewById(R.id.sell);
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (quantityTable > 0) {

                    ContentValues values = new ContentValues();

                    int newQuantity = quantityTable - 1;

                    Log.v("new quantity", "after click" + newQuantity);

                    values.put(TrackerContract.TrackerEntry.COLUMN_TRACKER_QUANTITY, newQuantity);

                    Uri uri = ContentUris.withAppendedId(TrackerContract.TrackerEntry.CONTENT_URI, id);
                    view.getContext().getContentResolver().update(uri, values, null, null);

                    view.getContext().getContentResolver().notifyChange(TrackerContract.TrackerEntry.CONTENT_URI, null);

                }
            }
        });

        // Find the columns of pet attributes that we're interested in
        int nameColumnIndex = cursor.getColumnIndex(TrackerContract.TrackerEntry.COLUMN_TRACKER_NAME);
        int priceColumnIndex = cursor.getColumnIndex(TrackerContract.TrackerEntry.COLUMN_TRACKER_PRICE);

        // Read the pet attributes from the Cursor for the current pet
        String trackerName = cursor.getString(nameColumnIndex);
        String trackerPrice = "Price:" + cursor.getString(priceColumnIndex);
        String trackerQuantity = "Quantity:" + cursor.getString(quantityColumnIndex);


// Update the TextViews with the attributes for the current pet
        nameTextView.setText(trackerName);
        priceTextView.setText(trackerPrice);
        quantityTextView.setText(trackerQuantity);
    }


}
