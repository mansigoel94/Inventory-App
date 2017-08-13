package com.example.mansi.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mansi.inventoryapp.data.InventoryContract.InventoryEntry;

/**
 * Created by Mansi on 09-08-2017.
 */

public class InventoryCursorAdapter extends CursorAdapter {

    protected InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);

    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView product_name = (TextView) view.findViewById(R.id.inventory_item_name_text);
        TextView product_quantity = (TextView) view.findViewById(R.id.inventory_item_current_quantity_text);
        TextView product_price = (TextView) view.findViewById(R.id.inventory_item_price_text);
        ImageView product_thumbnail = (ImageView) view.findViewById(R.id.product_thumbnail);
        TextView sale_button = (TextView) view.findViewById(R.id.sale_button);

        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
        int thumbnailColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PICTURE);
        int id = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));

        final String productName = cursor.getString(nameColumnIndex);

        final int quantity = cursor.getInt(quantityColumnIndex);
        String productQuantity = "Inventory " + String.valueOf(quantity);

        final Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);

        String productPrice = "Price $" + cursor.getString(priceColumnIndex);

        Uri thumbUri = Uri.parse(cursor.getString(thumbnailColumnIndex));

        product_name.setText(productName);
        product_quantity.setText(productQuantity);
        product_price.setText(productPrice);

        Glide.with(context).load(thumbUri)
                .placeholder(R.drawable.ic_photo_black_48dp)
                .centerCrop()
                .into(product_thumbnail);

        sale_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ContentResolver resolver = view.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                if (quantity > 0) {
                    int qq = quantity;
                    values.put(InventoryEntry.COLUMN_QUANTITY, --qq);
                    resolver.update(
                            currentProductUri,
                            values,
                            null,
                            null
                    );
                    context.getContentResolver().notifyChange(currentProductUri, null);
                } else {
                    Toast.makeText(context, "Item out of stock", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
