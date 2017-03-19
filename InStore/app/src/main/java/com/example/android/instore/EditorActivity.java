package com.example.android.instore;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import android.widget.EditText;

import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.instore.data.TrackerContract.TrackerEntry;

import java.io.ByteArrayOutputStream;
import java.sql.Blob;

/**
 * Allows user to create a new tracker or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the tracker data loader
     */
    private static final int EXISTING_TRACKER_LOADER = 0;

    private static final int PICK_IMAGE_REQUEST = 1;


    /**
     * Content URI for the existing tracker (null if it's a new tracker)
     */
    private Uri mCurrentTrackerUri;

    /**
     * EditText field to enter the tracker's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the tracker's quantity
     */
    private EditText mQuantityEditText;

    /**
     * EditText field to enter the tracker's price
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the tracker's vendor info
     */
    private EditText mVendorEditText;

    /**
     * EditText field to enter the tracker's picture info
     */
    private ImageView mPictureImageView = null;


    /**
     * Boolean flag that keeps track of whether the tracker has been edited (true) or not (false)
     */
    private boolean mTrackerHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mPetHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mTrackerHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new tracker or editing an existing one.
        Intent intent = getIntent();
        mCurrentTrackerUri = intent.getData();

        // If the intent DOES NOT contain a tracker content URI, then we know that we are
        // creating a new tracker.
        if (mCurrentTrackerUri == null) {
            // This is a new tracker, so change the app bar to say "Add a Pet"
            setTitle(getString(R.string.editor_activity_title_new_tracker));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a tracker that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing tracker, so change app bar to say "Edit Pet"
            setTitle(getString(R.string.editor_activity_title_edit_tracker));

            // Initialize a loader to read the tracker data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_TRACKER_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mVendorEditText = (EditText) findViewById(R.id.edit_vendor);
        mPictureImageView = (ImageView) findViewById(R.id.image);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mVendorEditText.setOnTouchListener(mTouchListener);
        mPictureImageView.setOnTouchListener(mTouchListener);

    }


    /**
     * This method is called when the order button is clicked.
     */
    public void submitOrder(View view) {

        EditText vendorEditText = (EditText) findViewById(R.id.edit_vendor);
        String vendorEmail = vendorEditText.getText().toString();

        EditText trackerEditText = (EditText) findViewById(R.id.edit_name);
        String trackerName = trackerEditText.getText().toString();

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{vendorEmail}); //get email from db vendor id
        intent.putExtra(Intent.EXTRA_SUBJECT, "Ordering more of " + trackerName); // get name of the tracker
        intent.putExtra(Intent.EXTRA_TEXT, "Hello, We need more of" + trackerName); // body of the email

        if (intent.resolveActivity(getPackageManager()) != null) {

            startActivity(intent);
        }
    }

    public void addImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_photo)), PICK_IMAGE_REQUEST);
    }

    /**
     * Get user input from editor and save tracker into database.
     */
    private void saveTracker() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String vendorString = mVendorEditText.getText().toString().trim();


        // Check if this is supposed to be a new tracker
        // and check if all the fields in the editor are blank
        if (TextUtils.isEmpty(nameString) || Integer.valueOf(quantityString) < 0 || Double.valueOf(priceString) < 0.00 || TextUtils.isEmpty(vendorString) || mPictureImageView.getDrawable() == null) {
            // Since no fields were modified, we can return early without creating a new tracker.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            Toast.makeText(this, "Please add a valid entry", Toast.LENGTH_SHORT).show();

            return;
        }

        Bitmap imageBitMap = ((BitmapDrawable) mPictureImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitMap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] imageByteArray = outputStream.toByteArray();

        // Create a ContentValues object where column names are the keys,
        // and tracker attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(TrackerEntry.COLUMN_TRACKER_NAME, nameString);
        values.put(TrackerEntry.COLUMN_TRACKER_PRICE, priceString);
        values.put(TrackerEntry.COLUMN_TRACKER_QUANTITY, quantityString);
        values.put(TrackerEntry.COLUMN_TRACKER_VENDOR, vendorString);
        values.put(TrackerEntry.COLUMN_TRACKER_IMAGE, imageByteArray);


        // Determine if this is a new or existing tracker by checking if mCurrentTrackerUri is null or not
        if (mCurrentTrackerUri == null) {
            // This is a NEW tracker, so insert a new tracker into the provider,
            // returning the content URI for the new tracker.
            Uri newUri = getContentResolver().insert(TrackerEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_tracker_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_tracker_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING tracker, so update the tracker with content URI: mCurrentPetUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentPetUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentTrackerUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_tracker_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_tracker_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                try {
                    Uri imageUri = data.getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    mPictureImageView.setImageBitmap(bitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new tracker, hide the "Delete" menu item.
        if (mCurrentTrackerUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save tracker to database
                saveTracker();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the tracker hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mTrackerHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the tracker hasn't changed, continue with handling back button press
        if (!mTrackerHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all tracker attributes, define a projection that contains
        // all columns from the tracker table
        String[] projection = {
                TrackerEntry._ID,
                TrackerEntry.COLUMN_TRACKER_NAME,
                TrackerEntry.COLUMN_TRACKER_QUANTITY,
                TrackerEntry.COLUMN_TRACKER_VENDOR,
                TrackerEntry.COLUMN_TRACKER_PRICE,
                TrackerEntry.COLUMN_TRACKER_IMAGE
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentTrackerUri,         // Query the content URI for the current tracker
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of tracker attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(TrackerEntry.COLUMN_TRACKER_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(TrackerEntry.COLUMN_TRACKER_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(TrackerEntry.COLUMN_TRACKER_PRICE);
            int vendorColumnIndex = cursor.getColumnIndex(TrackerEntry.COLUMN_TRACKER_VENDOR);
            int pictureColumnIndex = cursor.getColumnIndex(TrackerEntry.COLUMN_TRACKER_IMAGE);


            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String quantity = cursor.getString(quantityColumnIndex);
            String price = cursor.getString(priceColumnIndex);
            String vendor = cursor.getString(vendorColumnIndex);
            byte[] pictureByte = cursor.getBlob(pictureColumnIndex);

            Bitmap pictureBitmap = BitmapFactory.decodeByteArray(pictureByte, 0, pictureByte.length);


            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mQuantityEditText.setText(quantity);
            mPriceEditText.setText(price);
            mVendorEditText.setText(vendor);
            mPictureImageView.setImageBitmap(pictureBitmap);

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
        mVendorEditText.setText("");
        mPictureImageView.setImageBitmap(null);

    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the tracker.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this tracker.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the tracker.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the tracker.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the tracker in the database.
     */
    private void deletePet() {
        // Only perform the delete if this is an existing tracker.
        if (mCurrentTrackerUri != null) {
            // Call the ContentResolver to delete the tracker at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentPetUri
            // content URI already identifies the tracker that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentTrackerUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_tracker_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_tracker_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }
}