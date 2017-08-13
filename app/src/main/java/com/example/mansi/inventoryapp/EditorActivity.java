package com.example.mansi.inventoryapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mansi.inventoryapp.data.InventoryContract.InventoryEntry;

import java.io.File;


public class EditorActivity extends AppCompatActivity
        implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final int PICK_PHOTO_REQUEST = 20;
    public static final int EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE = 21;
    //identifier for the inventory data loader
    private static final int EXISTING_INVENTORY_LOADER = 0;
    //General Product QUERY PROJECTION
    public final String[] PRODUCT_PROJECTIONS = {
            InventoryEntry._ID,
            InventoryEntry.COLUMN_NAME,
            InventoryEntry.COLUMN_QUANTITY,
            InventoryEntry.COLUMN_PRICE,
            InventoryEntry.COLUMN_PICTURE
    };

    private Uri mCurrentUri; //current product _ID in edit mode null in insert mode

    //Product UI elements
    private ImageView mProductPhoto;
    private EditText mProductName;
    private EditText mProductPrice;
    private EditText quantityPicker;
    private TextView mQuantityDisplay;

    //Product Action Elements
    private Button deleteButton;
    private Button orderButton;
    private Button updateButton;
    private Button incrementButton;
    private Button decrementButton;

    private String mCurrentPhotoUri = "No Images";
    private String productForSending;
    private int quantityForSending = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Cast UI
        mProductPhoto = (ImageView) findViewById(R.id.image_product_photo);
        mProductName = (EditText) findViewById(R.id.inventory_item_name_edittext);
        mQuantityDisplay = (TextView) findViewById(R.id.quantity_value_display);
        mProductPrice = (EditText) findViewById(R.id.inventory_item_price_edittext);

        quantityPicker = (EditText) findViewById(R.id.quantity_editext);
        deleteButton = (Button) findViewById(R.id.delete_product_button);
        orderButton = (Button) findViewById(R.id.order_supplier_button);
        updateButton = (Button) findViewById(R.id.save_product_button);
        incrementButton = (Button) findViewById(R.id.quantity_increment);
        decrementButton = (Button) findViewById(R.id.quantity_decrement);

        mProductPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPhotoUpdate(view);
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProduct();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                orderSupplier();
            }
        });

        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                incrementQuantity();
            }
        });

        decrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decrementQuantity();
            }
        });
        //Check where we came from
        Intent intent = getIntent();
        mCurrentUri = intent.getData();

        if (mCurrentUri == null) {
            //User click new product
            setTitle(getString(R.string.add_product));
            //We can't order and delete new product for new ones
            orderButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        } else {
            //User want to update a specific product
            setTitle(getString(R.string.edit_product));
            orderButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);

            //Read database for selected Product
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }
    }

    private void decrementQuantity() {
        int quantity_displayed = Integer.parseInt(mQuantityDisplay.getText().toString());
        int quantity_picker = Integer.parseInt(quantityPicker.getText().toString());
        if (quantity_displayed - quantity_picker < 0) {
            Toast.makeText(getBaseContext(), "Minimum quantity value is 0", Toast.LENGTH_SHORT).show();
        } else {
            int updatedQuantity = quantity_displayed - quantity_picker;
            mQuantityDisplay.setText(String.valueOf(updatedQuantity));
        }
    }

    private void incrementQuantity() {
        int quantity_displayed = Integer.parseInt(mQuantityDisplay.getText().toString());
        int quantity_picker = Integer.parseInt(quantityPicker.getText().toString());
        int updatedQuantity = quantity_displayed + quantity_picker;
        mQuantityDisplay.setText(String.valueOf(updatedQuantity));
    }

    public void onPhotoUpdate(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //If We are on Marshmallow or above so we need to ask for runtime permissions
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                setPhoto();
            } else {
                // we are here if we do not all ready have permissions
                String[] permisionRequest = {Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permisionRequest, EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE);
            }
        } else {
            //We are on an older devices so we dont have to ask for runtime permissions
            setPhoto();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //We got permission from user
            setPhoto();
        } else {
            Toast.makeText(this, R.string.need_permission, Toast.LENGTH_LONG).show();
        }
    }

    private void setPhoto() {
        // invoke the image gallery using an implict intent.
        Intent intent = new Intent(Intent.ACTION_PICK);

        // where do we want to find the data?
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        // finally, get a URI representation
        Uri data = Uri.parse(pictureDirectoryPath);

        // set the data and type.  Get all image types.
        intent.setDataAndType(data, "image/*");

        // we will invoke this activity, and get something back from it.
        startActivityForResult(intent, PICK_PHOTO_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_PHOTO_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                //If we are here, everything processed successfully and we have an Uri data
                Uri mProductPhotoUri = data.getData();
                mCurrentPhotoUri = mProductPhotoUri.toString();

                Glide.with(this).load(mProductPhotoUri)
                        .placeholder(R.drawable.ic_photo_black_48dp)
                        .fitCenter()
                        .into(mProductPhoto);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(this,
                mCurrentUri,
                PRODUCT_PROJECTIONS,
                null,
                null,
                null);
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Return early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {

            // Extract values from current cursor
            String name = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_NAME));
            int quantity = cursor.getInt(cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY));
            float price = cursor.getFloat(cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE));
            mCurrentPhotoUri = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PICTURE));

            productForSending = name;

            //We updates fields to values on DB
            mProductName.setText(name);
            mProductPrice.setText(String.valueOf(price));
            mQuantityDisplay.setText(String.valueOf(quantity));

            Glide.with(this).load(mCurrentPhotoUri)
                    .placeholder(R.drawable.ic_photo_black_48dp)
                    .fitCenter()
                    .into(mProductPhoto);
        }
    }


    /**
     * Get user input from editor and save/update product into database.
     */
    private void saveProduct() {
        String nameString = mProductName.getText().toString().trim();
        String quantityString = mQuantityDisplay.getText().toString().toString();
        String priceString = mProductPrice.getText().toString().trim();

        //validating name and price
        if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, "Fields cannot be left blank", Toast.LENGTH_SHORT).show();
            // No change has been made so we can return
            return;
        }

        //validating quantity
        if (quantityString.equals("0")) {
            Toast.makeText(this, "Please add atleast one item quantity to be added to inventory", Toast.LENGTH_SHORT).show();
            return;
        }

        //validating image
        if (mCurrentPhotoUri.equalsIgnoreCase("No images")) {
            Toast.makeText(this, "Please add photo", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();

        values.put(InventoryEntry.COLUMN_NAME, nameString);
        values.put(InventoryEntry.COLUMN_QUANTITY, quantityString);
        values.put(InventoryEntry.COLUMN_PRICE, priceString);
        values.put(InventoryEntry.COLUMN_PICTURE, mCurrentPhotoUri);

        if (mCurrentUri == null) {

            Uri insertedRow = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (insertedRow == null) {
                Toast.makeText(this, R.string.error_insertion, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.record_updated, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        } else {
            // We are Updating previous stored data
            int rowUpdated = getContentResolver().update(mCurrentUri, values, null, null);

            if (rowUpdated == 0) {
                Toast.makeText(this, R.string.error_updation, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.record_updated, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProductName.setText("");
        mProductPrice.setText("");
        mQuantityDisplay.setText("");
    }

    /**
     * Prompt the user to confirm that they want to delete this pet.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this product");
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog and continue editing the product.
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
     * Perform the deletion of the pet in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing pet.
        if (mCurrentUri != null) {

            int rowsDeleted = getContentResolver().delete(mCurrentUri, null, null);

            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, R.string.error_deletion, Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, "Product Deleted", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }


    /**
     * Order from supplier
     */
    private void orderSupplier() {
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setData(Uri.parse("mailto:"));
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Order " + productForSending);
        intent.putExtra(Intent.EXTRA_TEXT, "Please ship " + productForSending +
                " in quantities " + quantityForSending);
        startActivity(intent);
    }
}

