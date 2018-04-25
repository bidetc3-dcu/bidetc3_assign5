package com.example.android.liondesk;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;



/*
The Firebase code (Google authentification and realtime database)is adapted from the
Udacity course "Firebase in a weekend" [get real reference]
 */

public class MainActivity extends AppCompatActivity {

    // Log TAG for debugging purposes
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 1;
    public static final String ANONYMOUS = "anonymous";
    public static final int AVAILABILITY_FREE = 1;
    private static final int MY_REQUEST_CALENDAR_PERMISSIONS = 1;


    private RecyclerView recyclerView;
    private HotDesksAdapter mAdapter;
    private List<HotDesk> mHotDeskList;
    private LinearLayoutManager mLayoutManager;
    private String mUsername;
    private String mOwner;
    int VERTICAL = 1;
    private TextView mDateDisplay;
    private FloatingActionButton mFAB;
    private String mSelectedDeskID;
    private CoordinatorLayout mCoordinatorLayout;
    private String mAlertNoInternet, mAlertNoDeskSelected, mConfirmationBooking, mBookingCancelled;
    private boolean mIsCancelled; // Keep track whether the booking has been cancelled or not


    //Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private FirebaseDatabase mFirebaseDatabase;
    //only access a portion of the database, here "Bookings" and "Hotdesks"
    private DatabaseReference mBookingsDatabaseReference;
    private DatabaseReference mHotDesksDatabaseReference;

    private static MyQueryHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsername = ANONYMOUS;

        /* Initialize Firebase objects */

        //Get an instance of the Database. Main access point to the database.
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        // get the root node (getReference) then get the child "bookings" portion
        mBookingsDatabaseReference = mFirebaseDatabase.getReference().child("bookings");
        // get the root node (getReference) then get the child "hotdesks" portion
        mHotDesksDatabaseReference = mFirebaseDatabase.getReference().child("hotdesks");
        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();

        // Initialise references to Views
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        mDateDisplay = findViewById(R.id.date_holder);
        mFAB = findViewById(R.id.fab);
        mAlertNoInternet = this.getResources().getString(R.string.alert_no_internet);
        mAlertNoDeskSelected = this.getResources().getString(R.string.alert_no_desk_selected);
        mConfirmationBooking = this.getResources().getString(R.string.confirmation_booking);
        mBookingCancelled = this.getResources().getString(R.string.booking_cancelled);
        mIsCancelled = false;

        mHandler = new MyQueryHandler(this.getContentResolver());

        // Get today's date
        // Code from https://stackoverflow.com/questions/8654990/how-can-i-get-current-date-in-android
        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date());
        // Set today's date as default in the date view
        mDateDisplay.setText(currentDate);

        // Set a click listener on the view which displays the date
        mDateDisplay.setOnClickListener(new View.OnClickListener() {

            // The code in this method will be executed when the view "mDateDisplay" is clicked on.
            @Override
            public void onClick(View view) {
                Log.v(TAG, "The view to Select a date has been clicked.");

                //Display the Date picker
                showDatePickerDialog();
            }
        });

        // Set a click listener on the FAB
        mFAB.setOnClickListener(new View.OnClickListener() {

            // When the button is clicked, book the desk at the specified time for the current user
            @Override
            public void onClick(View view) {
                // Display "No Internet Connection" if no internet connection
                if (!checkIfConnectedToInternet()) {
                    alertOfNoInternetConnection();
                }
                // Display an alert if no desk has been selected yet
                else if (mSelectedDeskID == null) {
                    alertOfNoDeskSelected();
                } else {
                    // record a booking in the database for the selected date, selected desk, and
                    // current user
                    mBookingsDatabaseReference.child(getDisplayedDate())
                            .child(mSelectedDeskID).child(mUsername)
                            // set a completion listener
                            // code copied from
                            // https://stackoverflow.com/questions/41403085/how-to-check-if-writing-task-was-successful-in-firebase
                            .setValue("true", new DatabaseReference.CompletionListener() {
                                // the completion listener will return a null error if the write action completes successfully
                                public void onComplete(DatabaseError error, DatabaseReference ref) {
                                    // if the booking was recorded successfully
                                    if (error == null)
                                    // Display confirmation + option to CANCEL + refreshed list of desks
                                    {
                                        showConfirmationMessage(getDisplayedDate(), mSelectedDeskID, mUsername);
                                    } else {
                                        Toast.makeText(MainActivity.this, "ERROR - PLEASE TRY AGAIN", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "ERROR - BOOKING NOT RECORDED");
                                        getHotDesks(); // refreshes the list of hotdesks
                                    }
                                }
                            });

                }
            }

        });

        recyclerView = findViewById(R.id.recycler_view);

        mHotDeskList = new ArrayList<>();
        mAdapter = new HotDesksAdapter(this, mHotDeskList);

        // Interface implementation - Capture the desk ID when the desk card is clicked
        mAdapter.setOnRecyclerViewItemClickListener(new HotDesksAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClicked(String text) {
                mSelectedDeskID = text;
            }
        });

        mLayoutManager = new LinearLayoutManager(this, VERTICAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(),
                mLayoutManager.getOrientation()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = mFirebaseAuth.getCurrentUser();

                if (user != null) {
                    onSignedInInitialize(user.getDisplayName());

                } else {
                    // Not signed in, shows the Sign In UI
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        if (mAuthStateListener != null) {
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // If we check if connected to the Internet and find out that we are not connected
        if (!checkIfConnectedToInternet()) {
            // display "No Internet Connection" in a snackbar
            alertOfNoInternetConnection();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_account: // Launch the account activity
                Intent intent = new Intent(this, AccountActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_signout:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }

    }
    // Methods related to authentication flow

    private void onSignedInInitialize(String username) {

        mUsername = username;
        // If the user email is not null and the email preference is not already set
        if ((mFirebaseAuth.getCurrentUser().getEmail() != null)
                && (readFromPreferences("email", null) == null)) {
            // Set the current email used to sign in as the email preference value
            // This is done only when the preference for email is not set yet
            saveToPreferences("email", mFirebaseAuth.getCurrentUser().getEmail());
        }
        // Populate the adapter with data
        getHotDesks();
    }

    // Create the date Picker fragment and display the Date Picker
    // code copied from https://developer.android.com/guide/topics/ui/controls/pickers.html

    private void showDatePickerDialog() {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    // Capture information in Preferences
    // 2 methods below: code from https://stackoverflow.com/questions/552070/android-how-do-i-set-a-preference-in-code
    private void saveToPreferences(String valueKey, String value) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(valueKey, value);
        edit.apply();
    }

    public String readFromPreferences(String valueKey, String valueDefault) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        return prefs.getString(valueKey, valueDefault);
    }

    /*
    Utility method to get the current displayed date in the date view
    */
    public String getDisplayedDate() {

        String displayedDate = mDateDisplay.getText().toString();
        Log.d(TAG, "The date is " + displayedDate);
        return displayedDate;
    }


    // Compose the list of hotdesks which are not booked on a given day and which is added to the adapter
    public void getHotDesks() {
        // Reset the variable that drives the highlight on desk card to -1 so no desk is highlighted
        mAdapter.setLastCheckedPosition(-1);
        fillHotDeskList();
        // If there is a booking at the selected date (bookings/date)
        if (mBookingsDatabaseReference.child(mDateDisplay.getText().toString()) != null) {

            // Read data from the database, at the path Bookings / child
            // where the key = the date selected in the date picker

            mBookingsDatabaseReference.child(mDateDisplay.getText().toString())
                    .addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // if there are desks booked at this date
                            if (dataSnapshot.hasChildren()) {
                                // remove the desks already booked from the list which will be displayed
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    String deskID = ds.getKey();
                                    // Code to remove an object from the list is taken from
                                    //https://stackoverflow.com/questions/10502164/
                                    // how-do-i-remove-an-object-from-an-arraylist-in-java#10502214
                                    Iterator<HotDesk> it = mHotDeskList.iterator();
                                    while (it.hasNext()) {
                                        HotDesk hotDesk = it.next();
                                        if (hotDesk.getID().equals(deskID)) {
                                            it.remove();
                                            mAdapter.notifyDataSetChanged();

                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            //   Failed to read value
                            Log.w(TAG, "Failed to read value.", error.toException());
                            Toast.makeText(MainActivity.this, "reading ERROR", Toast.LENGTH_SHORT).show();

                        }
                    });
        }
    }

    // from https://codingwithmitch.com/android/32/
    // Create and return the full list of hotdesks

    // recyclerview update comes from
    // https://stackoverflow.com/questions/31367599/how-to-update-recyclerview-adapter-data#48959184
    private void fillHotDeskList() {

        mHotDesksDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    // clear old list
                    mHotDeskList.clear();
                    // add new list
                    final ArrayList<HotDesk> newList = new ArrayList<>();

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        HotDesk hDesk = new HotDesk();
                        hDesk.setID(ds.getKey()); // set the ID
                        // get the floor value as a string
                        String floor = ds.child("floor").getValue(String.class);
                        //set the floor# as an integer
                        hDesk.setFloorNumber(Integer.valueOf(floor));

                        // get the thumbnail value as a string
                        String thumbnail = ds.child("picture").getValue(String.class);
                        // set the thumbnail reference as an integer
                        // solution to get from a string to an int from
                        // https://stackoverflow.com/questions/4427608/android-getting-resource-id-from-string#19093447
                        int thumbnailIdentifier = getResources()
                                .getIdentifier(thumbnail, "drawable", getPackageName());
                        hDesk.setThumbnail(thumbnailIdentifier);

                        hDesk.setStatus("FREE");

                        // add items to newlist
                        newList.add(hDesk);
                    }
                    // add new list
                    mHotDeskList.addAll(newList);
                    // notify adapter
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

    }

    // Display "No Internet Connection" alert in a snackbar
    // All snackbar Code adapted from https://www.androidhive.info/2015/09/android-material-design-snackbar-example/
    public void alertOfNoInternetConnection() {

        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, mAlertNoInternet, Snackbar.LENGTH_LONG);

        snackbar.show();
    }

    //Method to check if there is connection to the Internet or not
    // copied from
    // https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html

    public boolean checkIfConnectedToInternet() {
        ConnectivityManager cm =
                (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    // Display "Please select a desk" alert in a Toast
    private void alertOfNoDeskSelected() {
        Toast.makeText(MainActivity.this, mAlertNoDeskSelected, Toast.LENGTH_SHORT).show();
    }

    // Display a confirmation message once the booking has been done
    private void showConfirmationMessage(String date, String deskID, String username) {
        final String dateToCancel = date;
        final String deskIDToCancel = deskID;
        final String usernameToCancel = username;

        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, mConfirmationBooking, Snackbar.LENGTH_LONG)
                .setAction("CANCEL", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Delete the booking just made
                        mBookingsDatabaseReference.child(dateToCancel)
                                .child(deskIDToCancel).child(usernameToCancel).setValue(null);
                        Snackbar snackbar1 = Snackbar.make(mCoordinatorLayout, mBookingCancelled, Snackbar.LENGTH_SHORT);
                        snackbar1.show();
                        mIsCancelled = true;
                    }
                });

        // code adapted from
        // https://stackoverflow.com/questions/30926380/how-can-i-be-notified-when-a-snackbar-has-dismissed-itself
        snackbar.addCallback(new Snackbar.Callback() {

            // When the snack bar does not show anymore,
            // the highglight on the selected desk is removed,
            // the list of desk displayed is cancelled,
            // and if the booking has not been cancelled,
            // the confirmation by email or addition to user's calendar are applied, if selected in preferences
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                // Refresh the list of hotdesks displayed and remove the highlight on the selected desk
                getHotDesks();
                if (!mIsCancelled) // If the booking was not cancelled
                {
                    // If the user ticked the option to receive a confirmation email
                    if (getEmailConfirmationPreference()) {
                        // Send confirmation email
                        sendConfirmationEmail();
                    }

                    // If the user selected the option to get the booking inserted in their calendar
                    if (getCalendarInsertionPreference()) {
                        // Start the process to Insert the booking as an event in the user's calendar
                        CheckPermissionInsertInUsersCalendar();
                    }
                }
            }
        });
        snackbar.show();
    }


    // Get the email confirmation preference (true if ticked, false if unticked)
    public boolean getEmailConfirmationPreference() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean emailConfirmation = preferences.getBoolean("checkbox_email_confirmation", true);
        Log.d(TAG, "email notif = " + emailConfirmation);
        return emailConfirmation;
    }

    // Get the calendar insertion preference (true if ticked, false if unticked)
    public boolean getCalendarInsertionPreference() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean calendarInsertion = preferences.getBoolean("checkbox_calendar_insertion", true);
        Log.d(TAG, "calendar insert = " + calendarInsertion);
        return calendarInsertion;
    }

    // Get the preferred email address stored in the Account information
    public String getEmailPreference() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String emailAddress = preferences.getString("email", "");
        Log.d(TAG, "email = " + emailAddress);
        return emailAddress;
    }

    // Send a confirmation email to the current user's email
    public void sendConfirmationEmail() {
        //todo Build an intent to send the email confirmation - for version 2
        String email = getEmailPreference();
    }

    // Check for Permission to write to Calendar
    //code copied from https://developer.android.com/training/permissions/requesting.html#java
    private void CheckPermissionInsertInUsersCalendar() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR)
                        != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.WRITE_CALENDAR,
                            Manifest.permission.READ_CALENDAR},
                    MY_REQUEST_CALENDAR_PERMISSIONS);
        } else {
            insertInUsersCalendar();
        }
    }

    // code adapted from
    // http://codetheory.in/using-asyncqueryhandler-to-access-content-providers-asynchronously-in-android/

    public void insertInUsersCalendar() {

        // Get the primary calendar ID
        long calendarID = getCalendarID();

        // Gather the event data
        long startMillis = 0;
        long endMillis = 0;
        String date = getDisplayedDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

        Calendar selectedDay = Calendar.getInstance();
        try {
            selectedDay.setTime(sdf.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(selectedDay.get(YEAR), selectedDay.get(MONTH), selectedDay.get(Calendar.DAY_OF_MONTH), 8, 30);
        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.set(selectedDay.get(YEAR), selectedDay.get(MONTH), selectedDay.get(Calendar.DAY_OF_MONTH), 18, 00);
        endMillis = endTime.getTimeInMillis();
        // code from https://stackoverflow.com/questions/11934465/eventtimezone-value
        TimeZone tz = TimeZone.getDefault();
        String timeZone = tz.getID();

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, "WORKING FROM THE OFFICE");
        values.put(CalendarContract.Events.EVENT_LOCATION, "LIONBRIDGE DESK # " + mSelectedDeskID);
        values.put(CalendarContract.Events.ALL_DAY, 1);
        values.put(CalendarContract.Events.AVAILABILITY, AVAILABILITY_FREE);
        values.put(CalendarContract.Events.CALENDAR_ID, calendarID);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone);

        //Perform the insert
        mHandler.startInsert(1, null, CalendarContract.Events.CONTENT_URI, values);
    }

    // Get the primary calendar ID of the first Google account detected for the user
    // Code from
    // https://stackoverflow.com/questions/16242472/retrieve-the-default-calendar-id-in-android
    public long getCalendarID() {
        // the calendar ID that will be returned
        long calendarID = 1;    //value instanciated with dummy value
        //projection
        String[] projection = {
                CalendarContract.Calendars._ID
        };

        // selection, Defines WHERE clause columns and placeholders
        String selection = CalendarContract.Calendars.VISIBLE + " = 1 AND "
                + CalendarContract.Calendars.IS_PRIMARY + "= 1 AND "
                + CalendarContract.Calendars.OWNER_ACCOUNT + "=  \"" + mFirebaseAuth.getCurrentUser().getEmail() + "\"";

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED) {
            Cursor cursor = getContentResolver().query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    selection,
                    null,
                    CalendarContract.Calendars._ID + " ASC");

            int idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID);

            if (cursor == null) {
                // Some providers return null if an error occurs whereas others throw an exception
                Toast.makeText(MainActivity.this, "Cursor is null", Toast.LENGTH_SHORT).show();

            } else if (cursor.getCount() < 1) {
                // No matches found
                Toast.makeText(MainActivity.this, "No Calendar found", Toast.LENGTH_SHORT).show();

            } else {
                // Return the primary calendar ID of the user currently logged in
                while (cursor.moveToNext()) {
                    int i = cursor.getInt(idIndex);
                    Log.d(TAG, "idIndexInt: " + i);
                    calendarID = (long) i;
                }
            }
            cursor.close();

        } else {
            CheckPermissionInsertInUsersCalendar();
        }
        return calendarID;
    }

    // adapted from https://github.com/googlesamples/android-RuntimePermissions/blob/master/
    // Application/src/main/java/com/example/android/system/runtimepermissions/MainActivity.java
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        // We have requested multiple permissions for calendar, so all of them need to be checked.
        if (requestCode == MY_REQUEST_CALENDAR_PERMISSIONS) {
            if (com.example.android.liondesk.PermissionUtil.verifyPermissions(grantResults)) {
                // All required permissions have been granted, insert the booking in the calendar
                insertInUsersCalendar();

            } else {
                Log.i(TAG, "Contacts permissions were NOT granted.");
                Snackbar.make(mCoordinatorLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
