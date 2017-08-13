package com.example.mansi.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.mansi.inventoryapp.data.InventoryContract.InventoryEntry;

/**
 * DataBase Helper for Inventory app
 */

public class InventoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory.db";

    private static final int DATABASE_VERSION = 1;

    private static final String CREATE_QUERY = "CREATE TABLE " + InventoryEntry.TABLE_NAME + " ("
            + InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + InventoryEntry.COLUMN_NAME + " TEXT NOT NULL, "
            + InventoryEntry.COLUMN_QUANTITY + " INTEGER DEFAULT 0, "
            + InventoryEntry.COLUMN_PRICE + " REAL NOT NULL DEFAULT 0.0, "
            + InventoryEntry.COLUMN_PICTURE + " TEXT DEFAULT 'No images')";

    private static final String DELETE_QUERY = "DROP TABLE IF EXISTS " + InventoryEntry.TABLE_NAME;

    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(DELETE_QUERY);
        onCreate(sqLiteDatabase);
    }
}
