package com.example.android.liondesk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
    private List<HotDesk> hotDeskList;
    private LinearLayoutManager mLayoutManager;
    private String mUsername; // todo can I remove this?
    int VERTICAL = 1;
    private TextView mDateDisplay;
    private FloatingActionButton mFAB;
    private String mSelectedDeskID;

    //Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private FirebaseDatabase mFirebaseDatabase;
    //only access a portion of the database, here "Bookings" and "Users"
    private DatabaseReference mBookingsDatabaseReference; // WARNING! changed Message to Bookings
    private DatabaseReference mUsersDatabaseReference;
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
        mDateDisplay = findViewById(R.id.date_holder);
        mFAB = findViewById(R.id.fab);

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
        mFAB.setOnClickListener(new View.OnClickListener(){

            // When the button is clicked, book the desk at the specified time for the current user
            @Override
            public void onClick(View view){
                //todo book the desk at the specified time for the current user
                //get date and user,check they are not null
                // if not null, and if online, send write request to database
                mBookingsDatabaseReference.child(getDisplayedDate())
                        .child(mSelectedDeskID).child(mUsername).setValue("true");
                //do a code that sends a confirmation (intent with something?)
                //so that the confirmation snackbar get it
                //todo on results - display confirmation snackbar
            }

        });

        recyclerView = findViewById(R.id.recycler_view);

        hotDeskList = new ArrayList<>();
        mAdapter = new HotDesksAdapter(this, hotDeskList);

        // Interface implementation.
        mAdapter.setOnRecyclerViewItemClickListener(new HotDesksAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClicked(String text) {
                Log.d(TAG, "Text is = " + text);
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
                    onSignedOutCleanup();
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

        // Read from the database reference Hotdesks
        mHotDesksDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
               // Object value = dataSnapshot.getValue();
              //  Log.d(TAG, "Value is: " + value);
                getHotDesks(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        // Read from the database reference Hotdesks
    //    mBookingsDatabaseReference.addValueEventListener(new ValueEventListener() {
   //         @Override
    //        public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                // Object value = dataSnapshot.getValue();
                //  Log.d(TAG, "Value is: " + value);
    //            getBookedDeskIDs(dataSnapshot);
    //        }

    //        @Override
    //        public void onCancelled(DatabaseError error) {
                // Failed to read value
    //            Log.w(TAG, "Failed to read value.", error.toException());
   //         }
    //    });

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
    public void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);

    }

    // Methods related to authentication flow

    private void onSignedInInitialize(String username) {
        mUsername = username;
        // the user is signed in
                    /* code from friendly chat app - todo put your own code here
                     mUsername = mFirebaseUser.getDisplayName();
                     if (mFirebaseUser.getPhotoUrl() != null) {
                       mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
                     } */
        //todo add the code about attach Database readlistener - see udacity Saturday 35
        Toast.makeText(MainActivity.this, "You are signed in.",
                Toast.LENGTH_SHORT).show();
        // If the user email is not null and the email preference is not already set
        if ((mFirebaseAuth.getCurrentUser().getEmail() != null)
                && (readFromPreferences("email", null) == null)) {
            // Set the current email used to sign in as the email preference value
            // This is done only when the preference for email is not set yet
            saveToPreferences("email", mFirebaseAuth.getCurrentUser().getEmail());
        }
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        //todo add the code about detach Database readlistener - see udacity Saturday 35
        //todo do the same in onPause()

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

        String displayedDate =  mDateDisplay.getText().toString();
        Log.d(TAG, "The date is "+ displayedDate);
        return displayedDate;
    }

    // from https://codingwithmitch.com/android/32/
    // Get the IDs of the booked desks from the database
    /*
    private void getBookedDeskIDs(DataSnapshot dataSnapshot) {
        if (dataSnapshot != null) {

            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                HotDesk hDesk = new HotDesk();
                hDesk.setID(ds.child(getDisplayedDate()).getValue(HotDesk.class).getID()); //set the ID
                hDesk.setFloorNumber(ds.child(getDisplayedDate()).getValue(HotDesk.class).getFloorNumber()); //set the floor#
             //   hDesk.setThumbnail(ds.child(getDisplayedDate()).getValue(HotDesk.class).getThumbnail()); //set the pic
                hDesk.setThumbnail(R.drawable.hotdesk1); // dummy as I dont know how to get R.drawable.hotdesk1 at the moment
                hDesk.setStatus("FREE");

                hotDeskList.remove(hDesk);
            }
            mAdapter.notifyDataSetChanged();

        }
    }
*/
    //////

    /* from https://codingwithmitch.com/android/32/
    private void showData(DataSnapshot dataSnapshot) {
        for(DataSnapshot ds : dataSnapshot.getChildren()){
            UserInformation uInfo = new UserInformation();
            uInfo.setName(ds.child(userID).getValue(UserInformation.class).getName()); //set the name
            uInfo.setEmail(ds.child(userID).getValue(UserInformation.class).getEmail()); //set the email
            uInfo.setPhone_num(ds.child(userID).getValue(UserInformation.class).getPhone_num()); //set the phone_num

            //display all the information
            Log.d(TAG, "showData: name: " + uInfo.getName());
            Log.d(TAG, "showData: email: " + uInfo.getEmail());
            Log.d(TAG, "showData: phone_num: " + uInfo.getPhone_num());

            ArrayList<String> array  = new ArrayList<>();
            array.add(uInfo.getName());
            array.add(uInfo.getEmail());
            array.add(uInfo.getPhone_num());
            ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,array);
            mListView.setAdapter(adapter);
        }
    }

  *  /

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

    //todo display desks from hotdesk dbref
    // from https://codingwithmitch.com/android/32/
    // Add hotdesk object to the list which is added to the adapter
    private void getHotDesks(DataSnapshot dataSnapshot) {
         if (dataSnapshot != null) {

            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                HotDesk hDesk = new HotDesk();
                hDesk.setID(ds.getKey()); // set the ID
                // get the floor value as a string
                String floor = ds.child("floor").getValue(String.class);
                //set the floor# as an integer
                hDesk.setFloorNumber(Integer.valueOf(floor));
                // for testing the value todo remove when robust
                Toast.makeText(getApplicationContext(), "Floor : "+ floor,
                        Toast.LENGTH_SHORT).show();

                // get the thumbnail value as a string
                String thumbnail = ds.child("picture").getValue(String.class);
                // set the thumbnail reference as an integer
                // solution to get from a string to an int from
                // https://stackoverflow.com/questions/4427608/android-getting-resource-id-from-string#19093447
                int thumbnailIdentifier = getResources()
                        .getIdentifier(thumbnail, "drawable", getPackageName());
                hDesk.setThumbnail(thumbnailIdentifier);

                // for testing the value todo remove when robust
                Toast.makeText(getApplicationContext(), "Thumbnail : "+ thumbnail,
                        Toast.LENGTH_SHORT).show();

              //  hDesk.setFloorNumber(0);
               // hDesk.setFloorNumber(Integer.valueOf(floor));
             //   hDesk.setThumbnail(ds.child().getValue(HotDesk.class).getThumbnail()); //set the pic
               // hDesk.setThumbnail(R.drawable.hotdesk1); // dummy as I dont know how to get R.drawable.hotdesk1 at the moment
                hDesk.setStatus("FREE");

                hotDeskList.add(hDesk);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

}
