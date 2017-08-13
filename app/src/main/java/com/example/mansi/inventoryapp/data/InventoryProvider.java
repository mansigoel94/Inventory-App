package com.example.mansi.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.mansi.inventoryapp.R;
import com.example.mansi.inventoryapp.data.InventoryContract.InventoryEntry;

import static android.content.ContentValues.TAG;

/**
 * Created by Mansi on 09-08-2017.
 */

public class InventoryProvider extends ContentProvider {

    /**
     * URI matcher code for the content URI for the inventory table
     */
    private static final int INVENTORY = 1;

    /**
     * URI matcher code for the content URI for single item in the inventory table
     */
    private static final int INVENTORY_ID = 2;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {

        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, INVENTORY);

        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", INVENTORY_ID);
    }

    private InventoryDbHelper mDbHelper;


    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                cursor = db.query(InventoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            case INVENTORY_ID:

                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = db.query(InventoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI match " + match);
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException(R.string.insert_error + uri.toString());
        }
    }

    public Uri insertProduct(Uri uri, ContentValues values) {

        if (values.getAsString(InventoryEntry.COLUMN_NAME) == null) {
            throw new IllegalArgumentException("Product requires a name");
        }

        if (values.getAsInteger(InventoryEntry.COLUMN_QUANTITY) == -1) {
            throw new IllegalArgumentException("Quantity cannot be left empty");
        }


        Float price = values.getAsFloat(InventoryEntry.COLUMN_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("product requires valid price");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the inventory content URI
        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, id);
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case INVENTORY:
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case INVENTORY_ID:
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int rowsUpdated;

        if (values == null) {
            throw new IllegalArgumentException("Cannot update empty values");
        }

        //validations of field
        if (values.containsKey(InventoryEntry.COLUMN_NAME) && values.getAsString(InventoryEntry.COLUMN_NAME) == null) {
            throw new IllegalArgumentException("Product name cannot be set to null");
        }

        if (values.containsKey(InventoryEntry.COLUMN_QUANTITY) && values.getAsInteger(InventoryEntry.COLUMN_QUANTITY) == -1) {
            throw new IllegalArgumentException("Quantity cannot be updated to empty");
        }


        Float price = values.getAsFloat(InventoryEntry.COLUMN_PRICE);
        if (values.containsKey(InventoryEntry.COLUMN_PRICE) && price != null && price < 0) {
            throw new IllegalArgumentException("product requires valid price");
        }

        switch (match) {
            case INVENTORY:
                rowsUpdated = database.update(InventoryEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case INVENTORY_ID:
                rowsUpdated = database.update(InventoryEntry.TABLE_NAME,
                        values,
                        InventoryEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            default:
                throw new IllegalArgumentException("URI is not valid");
        }
        return rowsUpdated;
    }
}
