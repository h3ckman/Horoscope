package com.heckmobile.horoscope;

import java.util.Calendar;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class HelpFragment extends Fragment implements OnClickListener{
	public static final String ARG_HOROSCOPE = "horoscope";

	private static final String PREFS_NAME = "sign";

	Button saveSign;
	Spinner pickSign;
	SharedPreferences settings;
	
	public HelpFragment() {
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_help, container, false);
		
		saveSign = (Button) rootView.findViewById(R.id.help_button);
		pickSign = (Spinner) rootView.findViewById(R.id.help_dropdown);

		saveSign.setOnClickListener(this);
		
		return rootView;
	}

	@Override
	public void onClick(View v) {
		String item = pickSign.getSelectedItem().toString();
		String mySign = item.substring(0, item.indexOf(" "));
		settings = v.getContext().getSharedPreferences(PREFS_NAME, 0);
		settings.edit().putString(PREFS_NAME, mySign).commit();
	}
}