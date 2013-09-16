package com.heckmobile.horoscope;

import com.heckmobile.entities.Horoscope;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// URL to pull data from
	static final String DAILYURL = "http://www.astrology.com/horoscopes/daily-extended.rss";
	static final String WEEKLYURL = "http://www.astrology.com/horoscopes/weekly-overview.rss";
	static final String MONTHLYURL = "http://www.astrology.com/horoscopes/monthly-overview.rss";

	// XML node keys
	static final String KEY_ITEM = "item"; // parent node
	static final String KEY_SIGN = "title";
	static final String KEY_DATE = "pubDate";
	static final String KEY_DESC = "description";
	static final String PREFS_NAME = "sign";

	private ListView mDrawerList;
	private DrawerLayout mDrawer;
	private CustomActionBarDrawerToggle mDrawerToggle;
	private MenuItem actionDate;
	private String[] menuItems;
	public static HashMap<String, Horoscope> horoscopeMap = new HashMap<String, Horoscope>();
	public static HashMap<String, HashMap<String, Horoscope>> entryList = new HashMap<String, HashMap<String, Horoscope>>();
	//public static ArrayList<HashMap<String, Horoscope>> entryList = new ArrayList<HashMap<String, Horoscope>>();
	public static Horoscope currentHoroscope;
	public static String currentSign = "Scorpio";
	public static int lastSign;
	public static String currentUrl = DAILYURL;
	public Spinner actionBarSpinner;
	ProgressDialog pDialog;
	SharedPreferences settings;
	String userSign;
	boolean firstLaunch;
	Date todaysDate = new Date();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.drawer_layout);

		// Enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// Set up the action bar to show a dropdown list.
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.menu_date, R.layout.actionbar_spinner);
		adapter.setDropDownViewResource(R.layout.actionbar_spinner_item);
		getActionBar().setListNavigationCallbacks(adapter, new onNavigationItemSelected());

		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

		// Set a custom shadow that overlays the main content when the drawer opens
		mDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		// Initialize drawer
		_initMenu();
		mDrawerToggle = new CustomActionBarDrawerToggle(this, mDrawer);
		mDrawer.setDrawerListener(mDrawerToggle);

		// Check if first run
		settings = getSharedPreferences(PREFS_NAME, 0);
		firstLaunch = settings.getBoolean("firstrun", true);

		if (firstLaunch) {
			// Show tutorial if first run
			onCoachMark();
		}	
	}

	// On click listener for action bar dropdown, loads first item immediately
	private class onNavigationItemSelected implements OnNavigationListener {

		@Override
		public boolean onNavigationItemSelected(int position, long itemId) {
			boolean hasData = false;
			DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy");
			switch(position) {
			case 0:
				currentUrl = DAILYURL;
				break;
			case 1:
				currentUrl = WEEKLYURL;
				break;
			case 2:
				currentUrl = MONTHLYURL;
				dateFormat = new SimpleDateFormat("MMM yyyy");
				break;
			}

			getHoroscopeData();

			/*
				if((dateFormat.format(horoscopeMap.get(currentSign).date).equals(dateFormat.format(todaysDate)))) {

				}
			 */
			return false;
		}
	}

	private void getHoroscopeData() {
		// Get user's sign from shared preferences
		userSign = settings.getString(PREFS_NAME, "");

		try {
			// Get horoscope entries by reading from internal storage
			File file = new File(this.getFilesDir(),"entryList");
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			entryList = (HashMap<String, HashMap<String, Horoscope>>) ois.readObject();
			ois.close();

			// Get the correct entry if it exists
			if(entryList.containsKey(currentUrl)) {
				horoscopeMap = entryList.get(currentUrl);
				currentHoroscope = horoscopeMap.get(currentSign);
				// TODO Add check for latest horoscope
				selectItem(currentHoroscope.sign, currentHoroscope.id);
			}
			
			// Otherwise, call loader
			else {
				LoadHoroscopes loadHoroscopes = new LoadHoroscopes(MainActivity.this);
				loadHoroscopes.execute();
			}	
		}
		
		// If no data to read from storage, call loader
		catch(Exception e) {
			Log.e("No Data", "No stored file to retrieve");
			LoadHoroscopes loadHoroscopes = new LoadHoroscopes(MainActivity.this);
			loadHoroscopes.execute();
		}
	}


	/**
	 * Background Async Task to load horoscopes
	 * */
	class LoadHoroscopes extends AsyncTask {

		Context context;

		public LoadHoroscopes(Context context) {
			this.context = context;
			pDialog = new ProgressDialog(this.context);
		}
		/**
		 * Before starting background thread show ProgressDialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog.setMessage(Html
					.fromHtml("<b>Loading Horoscopes</b><br/>Please Wait..."));
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		/**
		 * Get all Places as JSON file
		 * */
		@Override
		protected String doInBackground(Object... args) {
			try {
				XmlParser parser = new XmlParser();
				String xml = parser.getXmlFromUrl(currentUrl); // getting XML
				Document doc = parser.getDomElement(xml); // getting DOM element

				NodeList nl = doc.getElementsByTagName(KEY_ITEM);

				// Looping through all item nodes <item>
				for (int i = 0; i < nl.getLength(); i++) {

					Element e = (Element) nl.item(i);

					// Get each sign's information
					String sign = parser.getValue(e, KEY_SIGN); // title child value
					String date = parser.getValue(e, KEY_DATE); // date child value
					String description = parser.getValue(e, KEY_DESC); // description child value

					// Get the name of the sign (trim the rest)
					sign = sign.substring(0, sign.indexOf(" "));

					// Trim the description to emit links and remove html tags
					description = description.substring(0, description.indexOf("<p>More horoscopes!"));
					description = Html.fromHtml(description).toString();

					// Convert the String date to Date
					Date formatDate = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).parse(date);

					Horoscope horoscope = new Horoscope(i, sign, formatDate, description);

					// Store in HashMap
					horoscopeMap.put(sign, horoscope);
				}

				entryList.put(currentUrl, horoscopeMap);
				Log.w("List Size", "Entry list size = "+entryList.size());

				File file = new File(this.context.getFilesDir(),"entryList");   
				ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
				outputStream.writeObject(entryList);
				outputStream.flush();
				outputStream.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		/**
		 * After completing background task dismiss the progress dialog and show
		 * the data 
		 * **/
		@Override
		protected void onPostExecute(Object result) {
			pDialog.dismiss();
			if(!horoscopeMap.isEmpty()) {
				settings.edit().putBoolean("firstrun", false).commit();
				firstLaunch=false;
				currentHoroscope = horoscopeMap.get(currentSign);
				selectItem(currentHoroscope.sign, currentHoroscope.id);
			}
			else {
				showSettingsAlert();
			}
		}
	}

	private void _initMenu() {

		NsMenuAdapter mAdapter = new NsMenuAdapter(this);

		// Add Header
		mAdapter.addHeader(R.string.ns_menu_main_header);

		// Add first block

		menuItems = getResources().getStringArray(
				R.array.ns_menu_items);
		String[] menuItemsIcon = getResources().getStringArray(
				R.array.ns_menu_items_icon);

		int res = 0;
		for (String item : menuItems) {

			int id_title = getResources().getIdentifier(item, "string",
					this.getPackageName());
			int id_icon = getResources().getIdentifier(menuItemsIcon[res],
					"drawable", this.getPackageName());

			NsMenuItemModel mItem = new NsMenuItemModel(id_title, id_icon);
			//if (res==1) mItem.counter=12; //it is just an example...
			//if (res==3) mItem.counter=3; //it is just an example...
			mAdapter.addItem(mItem);
			res++;
		}
		lastSign = res;


		mAdapter.addHeader(R.string.ns_menu_main_header2);

		// Add second block

		menuItems = getResources().getStringArray(
				R.array.ns_menu_items_2);
		menuItemsIcon = getResources().getStringArray(
				R.array.ns_menu_items_icon_2);

		res = 0;
		for (String item : menuItems) {

			int id_title = getResources().getIdentifier(item, "string",
					this.getPackageName());
			int id_icon = getResources().getIdentifier(menuItemsIcon[res],
					"drawable", this.getPackageName());

			NsMenuItemModel mItem = new NsMenuItemModel(id_title, id_icon);
			mAdapter.addItem(mItem);
			res++;
		}

		mDrawerList = (ListView) findViewById(R.id.drawer);
		if (mDrawerList != null)
			mDrawerList.setAdapter(mAdapter);

		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);


		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = mDrawer.isDrawerOpen(mDrawerList);
		menu.findItem(R.id.action_refresh).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * The action bar home/up should open or close the drawer.
		 * ActionBarDrawerToggle will take care of this.
		 */
		if (mDrawerToggle.onOptionsItemSelected(item)) {

			return true;
		}

		switch (item.getItemId()) {
		case R.id.action_refresh:
			if(haveInternet(this)) {
				LoadHoroscopes loadHoroscopes = new LoadHoroscopes(this);
				loadHoroscopes.execute();
			}
			else {
				showSettingsAlert();
			}

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private class CustomActionBarDrawerToggle extends ActionBarDrawerToggle {

		public CustomActionBarDrawerToggle(Activity mActivity,DrawerLayout mDrawerLayout){
			super(
					mActivity,
					mDrawerLayout, 
					R.drawable.ic_drawer,
					R.string.ns_menu_open, 
					R.string.ns_menu_close);
		}

		@Override
		public void onDrawerClosed(View view) {
			getActionBar().setTitle(getString(R.string.ns_menu_close));
			invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
		}

		@Override
		public void onDrawerOpened(View drawerView) {
			getActionBar().setTitle(getString(R.string.ns_menu_open));
			invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
		}
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {

			// Get the name of the selected sign
			String item = ((TextView) view
					.findViewById(R.id.menurow_title)).getText().toString();

			selectItem(item, position);
		}
	}

	private void selectItem(String item, int position) {

		// Check if clicked item is a sign
		if(position <= lastSign) {
			if(!horoscopeMap.isEmpty()) {
				try
				{
					currentHoroscope = horoscopeMap.get(item);

					String arrayItem = getResources().getStringArray(R.array.ns_menu_items)[position-1]; // Throws out of bounds exception if not sign

					// Get the horoscope for the sign
					currentSign = item;
					Horoscope selected = horoscopeMap.get(item);

					Fragment fragment = new HoroscopeFragment();
					Bundle args = new Bundle();
					args.putSerializable(HoroscopeFragment.ARG_HOROSCOPE, selected);
					fragment.setArguments(args);

					FragmentManager fragmentManager = getFragmentManager();
					fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

					getActionBar().setSubtitle(item);
				}

				// If not a sign, launch settings
				catch(ArrayIndexOutOfBoundsException e) {

				}
			}
			else {
				showSettingsAlert();
			}
		}

		else {
			/*
			Log.w("Settings", "Settings clicked");
			Fragment fragment = new HelpFragment();
			Bundle args = new Bundle();
			args.putSerializable("horoscopeMap", horoscopeMap);
			fragment.setArguments(args);

			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

			getActionBar().setSubtitle(item);
			 */

			onCoachMark();
		}

		// Highlight the selected item and close the drawer
		mDrawerList.setItemChecked(position, true);

		// Close the drawer
		mDrawer.closeDrawer(mDrawerList);
	}

	public static boolean haveInternet(Context ctx) {

		NetworkInfo info = (NetworkInfo) ((ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

		if (info == null || !info.isConnected()) {
			return false;
		}
		if (info.isRoaming()) {
			// here is the roaming option you can change it if you want to
			// disable internet while roaming, just return false
			return false;
		}
		return true;
	}

	public void showSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		// Setting Dialog Title
		alertDialog.setTitle("Problem Loading Horoscopes");

		// Setting Dialog Message
		alertDialog
		.setMessage("There may be a problem with your Wi-Fi or data connection. Would you like to go to your settings?");

		// On pressing Settings button
		alertDialog.setPositiveButton("Settings",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(
						Settings.ACTION_WIFI_SETTINGS);
				startActivity(intent);
			}
		});

		// on pressing cancel button
		alertDialog.setNegativeButton("Exit",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				System.exit(0);
			}
		});

		// Showing Alert Message
		alertDialog.show();
	}

	public void onCoachMark(){

		//final Dialog dialog = new Dialog(this);
		final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);

		dialog.setContentView(R.layout.coach_mark);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));


		dialog.setCanceledOnTouchOutside(true);
		//for dismissing anywhere you touch
		View masterView = dialog.findViewById(R.id.coach_mark_master_view);
		masterView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
			}
		});
		dialog.show();
	}
}