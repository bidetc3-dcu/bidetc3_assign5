package com.example.android.liondesk;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;


/*
    Class copied from  https://developer.android.com/guide/topics/ui/controls/pickers.html
 */

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    // Log TAG for debugging purposes TODO remove when testing is done
    private static final String TAG = DatePickerFragment.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {

        // Displays the selected date in the date view in a MMMM dd, yyyy format
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        CharSequence output = DateFormat.format("MMM dd, yyyy", cal);
      ((TextView) getActivity().findViewById(R.id.date_holder)).setText(output);

        Toast.makeText(getContext(), "The date has been picked by the user.",
                Toast.LENGTH_SHORT).show();
        //todo send a query to database to check desk availability on that date
        ((MainActivity)getActivity()).getHotDesks();
    }
}

