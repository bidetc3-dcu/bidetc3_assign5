package com.example.android.liondesk;
// code adapted from
// http://codetheory.in/using-asyncqueryhandler-to-access-content-providers-asynchronously-in-android/

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.EntityIterator;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.UserDictionary;
import android.util.Log;

public class MyQueryHandler extends AsyncQueryHandler {

    private static final String TAG = MyQueryHandler.class.getSimpleName();

    public MyQueryHandler(ContentResolver cr) {
        super(cr);
    }

        @Override
        protected void onQueryComplete ( int token, Object cookie, Cursor cursor){

        // query is complete
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            // get the event ID that is the last element in the Uri
            long eventID = Long.parseLong(uri.getLastPathSegment());
            Log.d(TAG, uri.toString());
        }


        @Override
        protected void onUpdateComplete ( int token, Object cookie,int result){
        // update() completed
    }

        @Override
        protected void onDeleteComplete ( int token, Object cookie,int result){
        // delete() completed
    }


}