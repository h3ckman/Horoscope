package com.heckmobile.horoscope;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.heckmobile.entities.Horoscope;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Fragment that appears in the "content_frame"
 */
public class HoroscopeFragment extends Fragment {
	public static final String ARG_HOROSCOPE = "horoscope";

	public HoroscopeFragment() {
		// Empty constructor required for fragment subclasses
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_horoscope, container, false);
		Horoscope horoscope = (Horoscope) getArguments().getSerializable(ARG_HOROSCOPE);

		String formatDate = new SimpleDateFormat("EEE, MMM dd yyyy").format(horoscope.date);
		
		((TextView) rootView.findViewById(R.id.horoscope_title)).setText(horoscope.sign);
		((TextView) rootView.findViewById(R.id.horoscope_date)).setText(formatDate);
		((TextView) rootView.findViewById(R.id.horoscope_description)).setText(horoscope.description);
		
		int imageId = getResources().getIdentifier("ic_"+horoscope.sign.toLowerCase(Locale.getDefault()),
				"drawable", getActivity().getPackageName());
		((ImageView) rootView.findViewById(R.id.image)).setImageResource(imageId);
		return rootView;
	}
}