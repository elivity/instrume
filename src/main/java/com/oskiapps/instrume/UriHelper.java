package com.oskiapps.instrume;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;
import androidx.loader.content.CursorLoader;

/**
 * Created by Oskar on 23.04.2018.
 */
/**
 * Get a file path from a Uri. This will get the the path for Storage Access
 * Framework Documents, as well as the _data field for the MediaStore and
 * other file-based ContentProviders.
 *
 * @author paulburke

 */
public class UriHelper {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        System.out.println("oski went in initial");
        // DocumentProvider
        System.out.println("oski what is this" + DocumentsContract.isDocumentUri(context, uri));
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            System.out.println("oski went in kitat and isdocumenturi");
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                System.out.println("oski went in external");

                if ("primary".equalsIgnoreCase(type)) {
                    System.out.println("oski went in external primary");
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else {

                    String storageDefinition = "";
                    if(Environment.isExternalStorageRemovable()){
                        storageDefinition = "EXTERNAL_STORAGE";

                    } else{
                        storageDefinition = "SECONDARY_STORAGE";
                    }
                    String retString = "/storage/" + type + "/" + split[1];
                    String prepareEnv = System.getenv(storageDefinition);
                    String retString2 = prepareEnv.split(":")[0] /*Environment.getExternalStorageDirectory()*/ + "/" + split[1];
                    System.out.println("oski went in external" + retString2);
                    return /*System.getenv(storageDefinition)Environment.getExternalStorageDirectory() + "/" + split[1]*/ retString2;
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri internalExternalCheck;
                if(Environment.isExternalStorageRemovable() && isExternalStorageDocument(uri)){
                    internalExternalCheck = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                } else{
                    internalExternalCheck = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = internalExternalCheck;
                } else if ("video".equals(type)) {
                    contentUri = internalExternalCheck;
                } else if ("audio".equals(type)) {
                    contentUri = internalExternalCheck;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };
                System.out.println("oski went in media");

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            String fileName="unknown";//default fileName
            Uri filePathUri = uri;
            if (uri.getScheme().toString().compareTo("content")==0)
            {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor.moveToFirst())
                {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);//Instead of "MediaStore.Images.Media.DATA" can be used "_data"
                    filePathUri = Uri.parse(cursor.getString(column_index));
                    fileName = filePathUri.getLastPathSegment().toString();
                }
            }
            else if (uri.getScheme().compareTo("file")==0)
            {
                fileName = filePathUri.getLastPathSegment().toString();
            }
            else
            {
                fileName = fileName+"_"+filePathUri.getLastPathSegment();
            }
            return fileName;
           // return getRealPathFromURI(context, uri);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            System.out.println("oski file");

            return uri.getPath();
        }
        System.out.println("oski end");

        return null;
    }

    private static String getRealPathFromURI(Context ctx, Uri contentUri) {
        String[] proj = { MediaStore.Audio.Media.DATA };
        CursorLoader loader = new CursorLoader(ctx, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {

        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
