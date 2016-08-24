/*
 * Copyright (c) 2012, David Erosa
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following  conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice, 
 *      this list of conditions and the following disclaimer.
 *   Redistributions in binary form must reproduce the above copyright notice, 
 *      this list of conditions and the following  disclaimer in the 
 *      documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,  BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT  SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR  BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDIN G NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH  DAMAGE
 *
 * Code modified by Andrew Stephan for Sync OnSet
 *
 */

package com.synconset;

import java.lang.Integer;
import java.lang.Override;
import java.net.URI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.synconset.*;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import dk.ridr.app.Manifest;

public class MultiImageChooserActivity extends Activity implements OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ImagePicker";

    public static final int NOLIMIT = -1;
    public static final String MAX_IMAGES_KEY = "MAX_IMAGES";
    public static final String WIDTH_KEY = "WIDTH";
    public static final String HEIGHT_KEY = "HEIGHT";
    public static final String QUALITY_KEY = "QUALITY";

    private ImageAdapter ia;

    private Cursor imagecursor, actualimagecursor;
    private int image_column_index, image_column_orientation, actual_image_column_index, orientation_column_index;
    private int colWidth;

    private static final int READ_EXTERNAL_STORAGE_PERMISSION = 0xC001;
    private static final int CURSORLOADER_THUMBS = 0;
    private static final int CURSORLOADER_REAL = 1;

    private ArrayList<BzImage> fileNames = new ArrayList<BzImage>();

    private SparseBooleanArray checkStatus = new SparseBooleanArray();

    //private int positionCounter;
    private int maxImages;
    private int maxImageCount;

    private int desiredWidth;
    private int desiredHeight;
    private int quality;

    private GridView gridView;

    private final ImageFetcher fetcher = new ImageFetcher();

    private int selectedColor = 0xff4285f4;
    private boolean shouldRequestThumb = true;

    private FakeR fakeR;

    private ProgressDialog progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //https://developer.android.com/training/permissions/requesting.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_STORAGE_PERMISSION);
            }
            else {
                InitializePlugin();
            }
        }
        else {
            InitializePlugin();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    InitializePlugin();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void InitializePlugin() {
        fakeR = new FakeR(this);
        setContentView(fakeR.getId("layout", "multiselectorgrid"));
        fileNames.clear();
        //positionCounter = 0;

        maxImages = getIntent().getIntExtra(MAX_IMAGES_KEY, NOLIMIT);
        desiredWidth = getIntent().getIntExtra(WIDTH_KEY, 0);
        desiredHeight = getIntent().getIntExtra(HEIGHT_KEY, 0);
        quality = getIntent().getIntExtra(QUALITY_KEY, 0);
        maxImageCount = maxImages;

        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();

        colWidth = width / 3;

        gridView = (GridView) findViewById(fakeR.getId("id", "gridview"));
        gridView.setOnItemClickListener(this);
        gridView.setOnScrollListener(new OnScrollListener() {
            private int lastFirstItem = 0;
            private long timestamp = System.currentTimeMillis();

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    shouldRequestThumb = true;
                    ia.notifyDataSetChanged();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                float dt = System.currentTimeMillis() - timestamp;
                if (firstVisibleItem != lastFirstItem) {
                    double speed = 1 / dt * 1000;
                    lastFirstItem = firstVisibleItem;
                    timestamp = System.currentTimeMillis();

                    // Limit if we go faster than a page a second
                    shouldRequestThumb = speed < visibleItemCount;
                }
            }
        });

        ia = new ImageAdapter(this);
        gridView.setAdapter(ia);

        LoaderManager.enableDebugLogging(false);
        getLoaderManager().initLoader(CURSORLOADER_THUMBS, null, this);
        getLoaderManager().initLoader(CURSORLOADER_REAL, null, this);
        setupHeader();
        updateAcceptButton();
        progress = new ProgressDialog(this);
        progress.setTitle("Henter billeder");
        progress.setMessage("Det kan tage et par sekunder..");
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
        String name = getImageName(position);
        int rotation = getImageRotation(position);

        if (name == null) {
            return;
        }
        boolean isChecked = !isChecked(position);
        if (maxImages == 0 && isChecked) {
            isChecked = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Højst " + maxImageCount + " billeder");
            builder.setMessage("Du må højst vælge " + maxImageCount + " billeder.");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else if (isChecked) {
            GridCell cell = (GridCell) view.getTag();
            //TODO: Save view with BzImage
            fileNames.add(new BzImage(name, new Integer(rotation), fileNames.size()));
            if (maxImageCount == 1) {
                this.selectClicked(null);
            } else {
                maxImages--;

                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    cell.thumbImageView.setImageAlpha(128);
                } else {
                    cell.thumbImageView.setAlpha(128);
                }
                cell.thumbImageView.setBackgroundColor(selectedColor);
            }
        } else {
            fileNames.remove(BzImage.getByName(fileNames, name));
            Collections.sort(fileNames, new Comparator<BzImage>() {
                @Override
                public int compare(BzImage i1, BzImage i2) {
                    return i1.getPosition() - i2.getPosition(); // Ascending
                }
            });
            for (Integer index = 0; index < fileNames.size(); index++) {
                fileNames.get(index).setPosition(index);
            }

            //TODO: Re-number fileNames (done)
            //TODO: Redraw numbers
            //TODO: Update positionCounter (done)
            //positionCounter = fileNames.size() - 1;
            maxImages++;
            GridCell cell = (GridCell) view.getTag();
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                cell.thumbImageView.setImageAlpha(255);
            } else {
                cell.thumbImageView.setAlpha(255);
            }
            cell.thumbImageView.setBackgroundColor(Color.TRANSPARENT);
            cell.positionTextView.setText("");
        }

        checkStatus.put(position, isChecked);
        updateAcceptButton();
        //TODO: Refresh af view?? nhaaa...
        ia.notifyDataSetChanged();
        //gridView.invalidateViews();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int cursorID, Bundle arg1) {
        CursorLoader cl = null;

        ArrayList<String> img = new ArrayList<String>();
        switch (cursorID) {

            case CURSORLOADER_THUMBS:
                img.add(MediaStore.Images.Media._ID);
                img.add(MediaStore.Images.Media.ORIENTATION);
                break;
            case CURSORLOADER_REAL:
                img.add(MediaStore.Images.Thumbnails.DATA);
                img.add(MediaStore.Images.Media.ORIENTATION);
                break;
            default:
                break;
        }

        cl = new CursorLoader(MultiImageChooserActivity.this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                img.toArray(new String[img.size()]), null, null, "DATE_MODIFIED DESC");
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            // NULL cursor. This usually means there's no image database yet....
            return;
        }

        switch (loader.getId()) {
            case CURSORLOADER_THUMBS:
                imagecursor = cursor;
                image_column_index = imagecursor.getColumnIndex(MediaStore.Images.Media._ID);
                image_column_orientation = imagecursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
                ia.notifyDataSetChanged();
                break;
            case CURSORLOADER_REAL:
                actualimagecursor = cursor;
                String[] columns = actualimagecursor.getColumnNames();
                actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                orientation_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION);
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == CURSORLOADER_THUMBS) {
            imagecursor = null;
        } else if (loader.getId() == CURSORLOADER_REAL) {
            actualimagecursor = null;
        }
    }

    public void cancelClicked(View ignored) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void selectClicked(View ignored) {
        ((TextView) getActionBar().getCustomView().findViewById(fakeR.getId("id", "actionbar_done_textview"))).setEnabled(false);
        getActionBar().getCustomView().findViewById(fakeR.getId("id", "actionbar_done")).setEnabled(false);
        progress.show();
        Intent data = new Intent();
        if (fileNames.isEmpty()) {
            this.setResult(RESULT_CANCELED);
            progress.dismiss();
            finish();
        } else {
            new ResizeImagesTask().execute(fileNames);
        }
    }


    /*********************
     * Helper Methods
     ********************/
    private void updateAcceptButton() {
        ((TextView) getActionBar().getCustomView().findViewById(fakeR.getId("id", "actionbar_done_textview")))
                .setEnabled(fileNames.size() != 0);
        getActionBar().getCustomView().findViewById(fakeR.getId("id", "actionbar_done")).setEnabled(fileNames.size() != 0);
    }

    private void setupHeader() {
        // From Roman Nkk's code
        // https://plus.google.com/113735310430199015092/posts/R49wVvcDoEW
        // Inflate a "Done/Discard" custom action bar view
        /*
         * Copyright 2013 The Android Open Source Project
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */
        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(fakeR.getId("layout", "actionbar_custom_view_done_discard"), null);
        customActionBarView.findViewById(fakeR.getId("id", "actionbar_done")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // "Done"
                selectClicked(null);
            }
        });
        customActionBarView.findViewById(fakeR.getId("id", "actionbar_discard")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Show the custom action bar view and hide the normal Home icon and title.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private String getImageName(int position) {
        actualimagecursor.moveToPosition(position);
        String name = null;

        try {
            name = actualimagecursor.getString(actual_image_column_index);
        } catch (Exception e) {
            return null;
        }
        return name;
    }

    private int getImageRotation(int position) {
        actualimagecursor.moveToPosition(position);
        int rotation = 0;

        try {
            rotation = actualimagecursor.getInt(orientation_column_index);
        } catch (Exception e) {
            return rotation;
        }
        return rotation;
    }

    public boolean isChecked(int position) {
        boolean ret = checkStatus.get(position);
        return ret;
    }



    private class GridCell {
        public TextView positionTextView;
        public ImageView thumbImageView;

        public GridCell(View container) {
            thumbImageView = (ImageView) container.findViewById(fakeR.getId("id", "thumbImageView"));// R.id.thumbImageView);
            positionTextView = (TextView) container.findViewById(fakeR.getId("id", "positionTextView")); // R.id.positionTextView);

        }
    }

    private class ImageAdapter extends BaseAdapter {
        //private final Bitmap mPlaceHolderBitmap;
        private final LayoutInflater inflater;

        public ImageAdapter(Context context) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            if (imagecursor != null) {
                return imagecursor.getCount();
            } else {
                return 0;
            }
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int pos, View convertView, ViewGroup parent) {
            GridCell viewHolder = null;
            if (convertView == null) {
                convertView = inflater.inflate(fakeR.getId("layout", "grid_cell"), null);
                viewHolder = new GridCell(convertView);

                ViewGroup.LayoutParams layoutParams = viewHolder.thumbImageView.getLayoutParams();
                layoutParams.height = colWidth;
                layoutParams.width = colWidth;
                viewHolder.thumbImageView.setLayoutParams(layoutParams);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (GridCell)convertView.getTag();
            }
            viewHolder.thumbImageView.setImageBitmap(null);

            final int position = pos;

            if (!imagecursor.moveToPosition(position)) {
                return convertView;
            }

            if (image_column_index == -1) {
                return convertView;
            }

            final int id = imagecursor.getInt(image_column_index);
            final int rotate = imagecursor.getInt(image_column_orientation);
            if (isChecked(pos)) {
                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    viewHolder.thumbImageView.setImageAlpha(128);
                } else {
                    viewHolder.thumbImageView.setAlpha(128);
                }
                viewHolder.thumbImageView.setBackgroundColor(selectedColor);

                String name = getImageName(position);
                BzImage bzImage = BzImage.getByName(fileNames, name);

                viewHolder.positionTextView.setText(String.format("%d", bzImage.getPosition() + 1));

            } else {
                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    viewHolder.thumbImageView.setImageAlpha(255);
                } else {
                    viewHolder.thumbImageView.setAlpha(255);
                }
                viewHolder.thumbImageView.setBackgroundColor(Color.TRANSPARENT);


                viewHolder.positionTextView.setText("");
            }
            if (shouldRequestThumb) {
                fetcher.fetch(Integer.valueOf(id), viewHolder.thumbImageView, colWidth, rotate);
            }

            return convertView;
        }
    }


    private class ResizeImagesTask extends AsyncTask<ArrayList<BzImage>, Void, ArrayList<String>> {
        private Exception asyncTaskError = null;

        @Override
        protected ArrayList<String> doInBackground(ArrayList<BzImage>... fileNamesArray) {
            ArrayList<BzImage> fileNames = fileNamesArray[0];
            ArrayList<String> al = new ArrayList<String>();
            for(BzImage bzImage : fileNames) {
                al.add(bzImage.toJSON());
            }
            return al;
        }

        @Override
        protected void onPostExecute(ArrayList<String> al) {
            Intent data = new Intent();

            if (asyncTaskError != null) {
                Bundle res = new Bundle();
                res.putString("ERRORMESSAGE", asyncTaskError.getMessage());
                data.putExtras(res);
                setResult(RESULT_CANCELED, data);
            } else if (al.size() > 0) {
                Bundle res = new Bundle();
                res.putStringArrayList("MULTIPLEFILENAMES", al);
                if (imagecursor != null) {
                    res.putInt("TOTALFILES", imagecursor.getCount());
                }
                data.putExtras(res);
                setResult(RESULT_OK, data);
            } else {
                setResult(RESULT_CANCELED, data);
            }

            progress.dismiss();
            finish();
        }
    }
}
