package com.example.android.liondesk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/*
The Firebase code (Google authentification and realtime database)is adapted from the
Udacity course "Firebase in a weekend" [get real reference]
 */

public class MainActivity extends AppCompatActivity {

    // Log TAG for debugging purposes TODO remove when testing is done
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 1;
    public static final String ANONYMOUS = "anonymous";


    private RecyclerView recyclerView;
    private HotDesksAdapter mAdapter;
    private List<HotDesk> mHotDeskList;
    private LinearLayoutManager mLayoutManager;
    private String mUsername;
    int VERTICAL = 1;
    private TextView mDateDisplay;
    private FloatingActionButton mFAB;
    private String mSelectedDeskID;
    private CoordinatorLayout mCoordinatorLayout;
    private String mAlertNoInternet, mAlertNoDeskSelected, mConfirmationBooking, mBookingCancelled;

    //Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private FirebaseDatabase mFirebaseDatabase;
    //only access a portion of the database, here "Bookings", "Hotdesks" and "Users"
    private DatabaseReference mBookingsDatabaseReference;
    private DatabaseReference mUsersDatabaseReference;  // you can remove this if you dont implement user booking screens
    private DatabaseReference mHotDesksDatabaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsername = ANONYMOUS;

        /* Initialize Firebase objects */

        //Get an instance of the Database. Main access point to the database.
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        // get the root node (getReference) then get the child "bookings" portion
        mBookingsDatabaseReference = mFirebaseDatabase.getReference().child("bookings");
        // get the root node (getReference) then get the child "users" portion
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child("users");
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
                }

                else {
                    //todo do I need to check that date and user and dbref are not null first?
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
                            {showConfirmationMessage(getDisplayedDate(), mSelectedDeskID, mUsername);}
                            else
                            {
                                Toast.makeText(MainActivity.this, "ERROR - PLEASE TRY AGAIN", Toast.LENGTH_SHORT).show();
                                Log.d(TAG,"ERROR - BOOKING NOT RECORDED");
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

        // prepareDesks() – Adds dummy hotdesk data required for the recycler view.
        //TODO remove once data is coming from database
        //    prepareDesks();

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
        //todo add the code about attach Database readlistener - see udacity Saturday 35
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


    // code adapted from https://github.com/udacity/and-nd-firebase/compare/
    // 1.04-firebase-auth-firebaseui-signin...1.05-firebase-auth-signin-signout-setup
/*
    private void detachDatabaseReadListener(DatabaseReference databaseReference, ValueEventListener databaseListener) {
        if (databaseListener != null) {
            databaseReference.removeEventListener(databaseListener);
            databaseListener = null;
        }
    }
*/
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
        //adapterClear();
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

    //todo am I using that? if not, remove
    public void adapterClear() {


        final int size = mHotDeskList.size();
        mHotDeskList.clear();
        mAdapter.notifyItemRangeRemoved(0, size);
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
                .setAction("CANCEL", new View.OnClickListener() { //todo avoid hard coded string "CANCEL"
                    @Override
                    public void onClick(View view) {
                        // Delete the booking just made
                        mBookingsDatabaseReference.child(dateToCancel)
                                .child(deskIDToCancel).child(usernameToCancel).setValue(null);
                        Snackbar snackbar1 = Snackbar.make(mCoordinatorLayout, mBookingCancelled, Snackbar.LENGTH_SHORT);
                        snackbar1.show();
                    }
                });

        // code adapted from
        // https://stackoverflow.com/questions/30926380/how-can-i-be-notified-when-a-snackbar-has-dismissed-itself
        snackbar.addCallback(new Snackbar.Callback() {

            // When the snack bar does not show anymore,
            // the highglight on the selected desk is removed
            // and the list of desk displayed is cancelled
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
               // Refresh the list of hotdesks displayed
                getHotDesks();
            }
        });

        snackbar.show();


    }

    /**
     * Adding a few hotdesks for testing
     * TODO remove once data is coming from database
     */
 /*   private void prepareDesks() {
        int[] pictures = new int[]{
                R.drawable.hotdesk1,
                R.drawable.hotdesk2_bigger,
                R.drawable.hotdesk3_round,
                R.drawable.hotdesk4,
                R.drawable.hotdesk5,
                R.drawable.hotdesk6,
                R.drawable.hotdesk7,
                R.drawable.hotdesk8,
                R.drawable.hotdesk9,
                R.drawable.hotdesk10,
                R.drawable.hotdesk11};

        HotDesk h = new HotDesk("GA1", 0, "FREE", pictures[0]);
        hotDeskList.add(h);

        h = new HotDesk("GA2", 0, "FREE", pictures[1]);
        hotDeskList.add(h);

        h = new HotDesk("GB1", 0, "FREE", pictures[2]);
        hotDeskList.add(h);

        h = new HotDesk("GB2", 0, "FREE", pictures[3]);
        hotDeskList.add(h);

        h = new HotDesk("GB3", 0, "FREE", pictures[4]);
        hotDeskList.add(h);

        h = new HotDesk("GC1", 0, "FREE", pictures[5]);
        hotDeskList.add(h);

        h = new HotDesk("3A1", 3, "FREE", pictures[6]);
        hotDeskList.add(h);

        h = new HotDesk("3B1", 3, "FREE", pictures[7]);
        hotDeskList.add(h);

        h = new HotDesk("3B2", 3, "FREE", pictures[8]);
        hotDeskList.add(h);

        h = new HotDesk("4A1", 4, "FREE", pictures[9]);
        hotDeskList.add(h);

        h = new HotDesk("4A2", 4, "FREE", pictures[10]);
        hotDeskList.add(h);

        mAdapter.notifyDataSetChanged();
    }
    */
}


