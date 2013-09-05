package com.heckmobile.horoscope;

import com.heckmobile.entities.Horoscope;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// URL to pull data from
	static final String URL = "http://www.astrology.com/horoscopes/daily-extended.rss";
	
	// XML node keys
	static final String KEY_ITEM = "item"; // parent node
	static final String KEY_SIGN = "title";
	static final String KEY_DATE = "pubDate";
	static final String KEY_DESC = "description";

	private ListView mDrawerList;
	private DrawerLayout mDrawer;
	private CustomActionBarDrawerToggle mDrawerToggle;
	private String[] menuItems;
	ProgressDialog pDialog;
	HashMap<String, Horoscope> horoscopeMap = new HashMap<String, Horoscope>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.drawer_layout);

		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

		// set a custom shadow that overlays the main content when the drawer
		// opens

		mDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		_initMenu();
		mDrawerToggle = new CustomActionBarDrawerToggle(this, mDrawer);
		mDrawer.setDrawerListener(mDrawerToggle);

		/****************************************************/

		LoadHoroscopes loadHoroscopes = new LoadHoroscopes(this);
		loadHoroscopes.execute();

		/****************************************************/
	}

	/**
	 * Background Async Task to load Google places
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
					.fromHtml("<b>Loading</b><br/>Please Wait..."));
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		/**
		 * Get all Places as JSON file
		 * */
		protected String doInBackground(Object... args) {

			try {
				XmlParser parser = new XmlParser();
				String xml = parser.getXmlFromUrl(URL); // getting XML
				Document doc = parser.getDomElement(xml); // getting DOM element

				NodeList nl = doc.getElementsByTagName(KEY_ITEM);

				// Looping through all item nodes <item>
				for (int i = 0; i < nl.getLength(); i++) {

					// Creating new HashMap
					
					Element e = (Element) nl.item(i);

					// Get each sign's information
					String sign = parser.getValue(e, KEY_SIGN); // title child value
					String date = parser.getValue(e, KEY_DATE); // date child value
					String description = parser.getValue(e, KEY_DESC); // description child value
					
					// Get the name of the sign (trim the rest)
					sign = sign.substring(0, sign.indexOf(" "));
					
					// Trim the description to emit links
					description = description.substring(0, description.indexOf("<p>More horoscopes!"));
					
					// Convert the String date to Date
					Date formatDate = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).parse(date);
					
					Horoscope horoscope = new Horoscope(sign, formatDate, description);
					
					// Store in HashMap
					horoscopeMap.put(sign, horoscope);
				}
				
				

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * After completing background task dismiss the progress dialog and show
		 * the data 
		 * **/

		protected void onPostExecute(Object result) {
			pDialog.dismiss();
			String temp = horoscopeMap.get("Scorpio").description;
			 Log.w("Pisces", temp);
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
			if (res==1) mItem.counter=12; //it is just an example...
			if (res==3) mItem.counter=3; //it is just an example...
			mAdapter.addItem(mItem);
			res++;
		}

		mAdapter.addHeader(R.string.ns_menu_main_header2);

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
		menu.findItem(R.id.action_save).setVisible(!drawerOpen);
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

		// Handle your other action bar items...
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
			
			// Highlight the selected item and close the drawer
			mDrawerList.setItemChecked(position, true);
			
			// Get the name of the selected sign
			String sign = ((TextView) view
					.findViewById(R.id.menurow_title)).getText().toString();
			String text= "You clicked on " + sign;
			Toast.makeText(MainActivity.this, text , Toast.LENGTH_LONG).show();
			
			// Close the drawer
			mDrawer.closeDrawer(mDrawerList);
			
			// Get the horoscope for the sign
			Horoscope selected = horoscopeMap.get(sign);
			
			// Starting new intent
			Intent in = new Intent(view.getContext(), HoroscopeActivity.class);

			// Sending place reference id to single place activity
			in.putExtra("Horoscope", selected);
			startActivity(in);
		}

	}

}