package com.ffm.portfoliocalculator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class PortfolioViewer extends Activity {
		
		private Portfolio userPortfolio; //custom portfolio object used to calculate returns and organize data
		
		// ListView Items
		private ListView listItem; //the object that comprises the listview
		private String[] from = new String[] {"tickerSymbol","price","grossReturn","percentReturn"}; //column headings for the listview to be matched with the relevant xml objects
		private int[] to = new int[]{R.id.tickerSymbol,R.id.price,R.id.grossReturn,R.id.percentReturn}; //xml items in the listview to match with the column headings
		private List<HashMap<String,String>> fillList; //object that holds data which fills the listview

		// View controls
		private String previousView; //name of the view the user just navigated away from, utilized when the user wants to go back
		private String currentView; //name of the view currently displayed on the screen, used when user navigates back to the main view
		private GenerateReturns currentReturnItem; //name of the current return mother object, used to call the portfolio data displayed on the screen
		
		// Return name controls
		private Button returnSelectButton; //header button used to display/hide return bar
		private boolean displayingReturnSelectButton;
		private String grossReturnName; //name of the return currently displayed on the screen
		private Spinner taxSpinner; //spinner to choose between Pre-Tax and Post-Tax
		private Spinner marketOrDividendSpinner; //spinner to choose between Market or Dividend or Total Return
		private Spinner currentOrSoldSpinner; // spinner to choose between Current or Sold or Total Holdings
		private int taxSpinnerStart;
		private int marketOrDividendSpinnerStart;
		private int currentOrSoldSpinnerStart;
		private LinearLayout spinnerList; //xml item that holds all of the above return spinners

		// Date controls
		private Button dateButton; //header button used to display/hide date spinner
		private Spinner dateSpinner; //spinner used to select time period for returns
		private int dateSpinnerStart; //prevents date spinner from being called before the user has input transaction data
		private String beginningDate; //initial date of the displayed time period
		private boolean displayingDateBar;
		
		// Gesture controls
		private GestureDetector gestureDetector; // detects gestures made on the screen, so that the app can respond accordingly
		private SwipeListener swipeListener; // detects swipes made by the user
		
		// Graph controls
		private XYMultipleSeriesDataset graphDataset; //dataset containing all of the single datasets displayed in the graph
		private XYMultipleSeriesRenderer graphRenderer; //holds the formatting options for the graph
		private GraphicalView graphView; //xml item holding the graph object
		private int[] colors; //colors for the various items displayed in the graph
		private int currentColor = 0; //initialize current color
		
		// Refresh/Data Loading controls
		private ProgressDialog progressDialog; //dialog box called when transaction data is loaded
		private PullToRefreshLayout mPullToRefreshLayout; //api for the pull to refresh function
		private double progressAmount;
		private boolean restart = false;
		
		// Shared preferences controls
		private SharedPreferences savedPreferences; //various preferences shared between activities in the application
		
		//Header items
		private TextView listHeader; //title displayed on the header, displays the value of the variable currentView
		private Button tradeButton; //button to initialize new transaction activity
		private Button preferencesButton; //button to initialize user preferences activity		
		
		// Program start controls
		private boolean spinnersStart = false; //prevents the spinners from changing and calling methods before the portfolio object has been initialized
		private boolean transactionDatabaseExists;
		private boolean portfolioCreated;

		// Closer look controls
		private LinearLayout closerLookLayout; //xml item holding the graph, transaction info, and transaction return summary objects
		private ArrayList<String> activeItems; //holds items whose radio buttons have been clicked by the user
		private int activeCloserLook; //determines which view to display in the close look item
		private RadioButton[] radioButtonGroup = new RadioButton[3]; //radio button group displayed at the bottom of the screen, used to select view to display in closer look
		
		// Home view names
		private static final String HOME_SCREEN_NAME = "Home";
		private static final String PORTFOLIO_NAME = "PORTFOLIO";
		private static final String MARKET_NAME = "MARKET";
		private static final String ALPHA_NAME = "ALPHA";
		
		// Database names
		private static final String TRANSACTIONS_DATABASE_NAME = "Transactions";
		private static final String RETURNS_DATABASE_NAME = "Returns";

		
		@Override
		public void onCreate(Bundle savedInstanceState) 
		{
			super.onCreate(savedInstanceState);
			
			setContentView(R.layout.activity_main);
			
			// List view items
			listItem = (ListView) findViewById(R.id.returnList);
			
			// Saved preferences
			savedPreferences = getSharedPreferences("preferences",MODE_PRIVATE);
			
			// Pull to Refresh
			mPullToRefreshLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);
			
			// Trade Button
			tradeButton = (Button) findViewById(R.id.addNewButton);
			tradeButton.setOnClickListener(tradeButtonListener);
			
			// Preferences Button
			preferencesButton = (Button) findViewById(R.id.preferencesButton);
			preferencesButton.setOnClickListener(preferencesButtonListener);
			
			// Date Button
			dateButton = (Button) findViewById(R.id.timeButton);
			dateButton.setOnClickListener(dateButtonListener);			
			
			//Date Spinner
			dateSpinner = (Spinner) findViewById(R.id.dateSelectSpinner);
			ArrayAdapter<CharSequence> dateSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.date_array, android.R.layout.simple_spinner_item);
			dateSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			dateSpinner.setAdapter(dateSpinnerAdapter);
			
			// Return Select Button
			returnSelectButton = (Button) findViewById(R.id.returnSelectButton);
			returnSelectButton.setOnClickListener(returnSelectButtonListener);
			spinnerList = (LinearLayout) findViewById(R.id.spinnerList);
			
			// Tax Spinner
			taxSpinner = (Spinner) findViewById(R.id.taxSpinner);
			ArrayAdapter<CharSequence> taxSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.tax_array, android.R.layout.simple_spinner_item);
			taxSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			taxSpinner.setAdapter(taxSpinnerAdapter);
			
			// MarketOrDividend spinner
			marketOrDividendSpinner = (Spinner) findViewById(R.id.marketOrDividendSpinner);
			ArrayAdapter<CharSequence> marketOrDividendSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.marketOrDividend_array, android.R.layout.simple_spinner_item);
			marketOrDividendSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			marketOrDividendSpinner.setAdapter(marketOrDividendSpinnerAdapter);
			
			// CurrentOrSold spinner
			currentOrSoldSpinner = (Spinner) findViewById(R.id.currentOrSoldSpinner);
			ArrayAdapter<CharSequence> currentOrSoldSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.currentOrSold_array, android.R.layout.simple_spinner_item);
			currentOrSoldSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			currentOrSoldSpinner.setAdapter(currentOrSoldSpinnerAdapter);
			
			// Graph Line Colors
			colors = new int[]{getResources().getColor(R.color.graph_green)
					,getResources().getColor(R.color.graph_red), getResources().getColor(R.color.graph_yellow)
					, getResources().getColor(R.color.graph_blue), getResources().getColor(R.color.graph_orange)};

			// Current Return Item
			currentReturnItem = null;
			
			// List Header
			listHeader = (TextView) findViewById(R.id.listHeader);
			
			// Gesture Controls
			swipeListener = new SwipeListener();
			gestureDetector = new GestureDetector(PortfolioViewer.this,swipeListener);
			
			// Closer Look View
			closerLookLayout = (LinearLayout) findViewById(R.id.chart);
			radioButtonGroup[0] = (RadioButton) findViewById(R.id.closerLook1);
			radioButtonGroup[1] = (RadioButton) findViewById(R.id.closerLook2);
			radioButtonGroup[2] = (RadioButton) findViewById(R.id.closerLook3);
			
			// Transaction Database Checker
			transactionDatabaseExists = doesDatabaseExist(getApplicationContext(),TRANSACTIONS_DATABASE_NAME) ;
			portfolioCreated = false;
			
			initializePreferences(); 
			
			/*
			 * Checks if the user has input any transaction information yet, and if so enables various buttons and spinners  
			 */
			if (transactionDatabaseExists) { //checks if the "Transactions" database exists
			
				// List Items
				listItem.setClickable(true); 
				listItem.setOnItemClickListener(listItemNextListener);
				
				// Date Spinner
				dateSpinner.setOnItemSelectedListener(dateSpinnerListener);
				
				// Return Spinners
				taxSpinner.setOnItemSelectedListener(returnSpinnerListener);
				marketOrDividendSpinner.setOnItemSelectedListener(returnSpinnerListener);
				currentOrSoldSpinner.setOnItemSelectedListener(returnSpinnerListener);
				
				// Pull to Refresh
				ActionBarPullToRefresh.from(this)
				.theseChildrenArePullable(R.id.returnList)
				.listener(onRefreshListener)
				.setup(mPullToRefreshLayout);
			}

			//Start Program			
			initializePortfolio();
			
		}
		
		protected void onResume() {
			super.onResume();
			
			//Start Program
			initializePreferences();
			if (portfolioCreated) { //checks if user has input transaction data
				initializeView();
			}

		}
		
		/*
		 * Initializes various user preferences
		 */
		private void initializePreferences() {
			
			// View controls
			previousView = savedPreferences.getString("previous view", HOME_SCREEN_NAME);
			currentView = savedPreferences.getString("current view", HOME_SCREEN_NAME);
			
			// Return controls
			grossReturnName= savedPreferences.getString("return name", "Total Gross Pre-Tax");
			displayingReturnSelectButton = false;
			returnSelectButton.setBackgroundResource(R.drawable.ic_action_expand);
			
			taxSpinnerStart = savedPreferences.getInt("tax spinner start", 0);
			marketOrDividendSpinnerStart = savedPreferences.getInt("market or dividend spinner start", 2);
			currentOrSoldSpinnerStart = savedPreferences.getInt("current or sold spinner start", 2);
			
			taxSpinner.setSelection(taxSpinnerStart, false);
			marketOrDividendSpinner.setSelection(marketOrDividendSpinnerStart, false);
			currentOrSoldSpinner.setSelection(currentOrSoldSpinnerStart, false);
			
			spinnerList.setVisibility(View.GONE);
			
			// Date Controls
			displayingDateBar = false;
			dateSpinnerStart = savedPreferences.getInt("date spinner start", 5);
			
			if (dateSpinnerStart<6) { // prevents spinner from calling MAX or Custom method calls on opening
				selectBeginningDate(dateSpinnerStart);
			} else {
				beginningDate = savedPreferences.getString("beginning date", LocalDate.now().minusYears(1).toString());
			}
			
			dateSpinner.setSelection(dateSpinnerStart, false);
			dateSpinner.setVisibility(View.GONE);
			
			// Closer Look Controls 
			activeCloserLook = 0;
			radioButtonGroup[activeCloserLook].setChecked(true);
			
			// Header Controls
			listHeader.setText(currentView);
			tradeButton.setBackgroundResource(R.drawable.add_transaction);
			preferencesButton.setBackgroundResource(R.drawable.settings_button);

			// HIDE ACTION BAR
			getActionBar().hide();
		}
		
		/*
		 * Initializes the actual view that the user sees, populating the list view, closer look view, and enabling the spinners
		 */
		private void initializeView() {
			refreshList(currentView); //set list to the currentView
			generateNewActiveItem(); //create the return items initially selected
			new UpdateView().execute(); //update the view
			spinnersStart = true; // allow the spinners to be changed
		}
		
		/*
		 * Creates the portfolio object used to populate the view
		 */
		private void initializePortfolio() {
			//begin loading data to populate layout
			if (transactionDatabaseExists) 
			{
				// create progress bar dialog box
				progressDialog = new ProgressDialog(PortfolioViewer.this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog.setProgressNumberFormat(null);
				progressDialog.setTitle("Updating Returns. Please Wait...");
				progressDialog.setCancelable(false);
				progressDialog.show();
				
				// initialize portfolio object, setting tax rates and context
				userPortfolio = new Portfolio();
				userPortfolio.setCapitalGainsRate(Double.parseDouble(savedPreferences.getString("capital gains rate", "15"))/100);
				userPortfolio.setIncomeTaxRate(Double.parseDouble(savedPreferences.getString("income tax rate", "25"))/100);
				userPortfolio.setContext(getApplicationContext());
				
				new LoadData().execute(); // loads returns data to present to user
			}	
			//populate the layout with 0's because the user has not entered a transaction yet
			else 
			{
				fillList = new ArrayList<HashMap<String,String>>();
				String[] startTickerList = new String[]{PORTFOLIO_NAME,MARKET_NAME,ALPHA_NAME};
				for(int i=0; i<3; i++) 
				{
					HashMap<String,String> item = new HashMap<String,String>();
					item.put("tickerSymbol", startTickerList[i]);
					item.put("grossReturn", "0.00");
					item.put("percentReturn", "0%");
					fillList.add(item);
				}
				
				activeItems = new ArrayList<String>();
				
				CustomListAdapter listAdapter = new CustomListAdapter(this, fillList, R.layout.return_list_item, from, to);
				listItem.setAdapter(listAdapter);
			
			}
		}

		/*
		 * Loads transactional data from the database and puts it into a portfolio object
		 */
		private class LoadData extends AsyncTask<Void,Void,Boolean> {
			
			protected Boolean doInBackground(Void...voids) {
				if (isNetworkAvailable()) { // ensure that the phone can connect to the yahoo server
					populateHistoricalData(); // input returns data into the returns database
					return true;
				} else {
					return false;
				}
				
			}
		
			@Override
			protected void onPostExecute(Boolean connected) {
				if (connected) { // able to connect to the yahoo server
					initializeView(); // populate the view the user sees
					progressDialog.dismiss(); 
					mPullToRefreshLayout.setRefreshComplete(); // turn flashing blue refresh bar off, needed for pull to refresh functionality
					restart = false; // no need to call on create again when user pulls to refresh
					portfolioCreated = true;
				} else { // unable to connect to the yahoo server
					progressDialog.dismiss();
					mPullToRefreshLayout.setRefreshComplete(); // turn flashing blue refresh bar off, needed for pull to refresh functionality
					restart = true; // need to call on create again when user pulls to refresh
					createDialogBox("Could not connect to the internet"); // inform the user that program could not connect to yahoo server
				}
			}
		}
		
		/* 
		 * Creates the information to populate the listView xml objects
		 * @param returnMother: transaction object that provides the transaction list for the list view
		 * @param exception: MARKET or None, used so that the market equivalent return is not grouped in with the rest of the portfolio
		 * @return: a List of HashMaps that is used as the new fillList for the listView adapter
		 */
		private List<HashMap<String,String>> createReturnItems(GenerateReturns returnMother) {
			
			List<HashMap<String,String>> returnListItems = new ArrayList<HashMap<String,String>>(); //initializes the list to be returned
			
			if (returnMother == null) { //if the user is on or returning to the home screen, then the home screen is created
				
				//creates list items for the Portfolio and Market Equivalent
				GenerateReturns[] initialGenerator = new GenerateReturns[]{userPortfolio,userPortfolio.securitiesList.get(MARKET_NAME)};
				HashMap<String, double[]> allReturns = new HashMap<String, double[]>();
				
				// cycle through the initial generator list
				for (int i=0; i<initialGenerator.length; i++) {
					HashMap<String,String> item = new HashMap<String,String>();
					item.put("tickerSymbol", initialGenerator[i].getName());
					item.put("price", initialGenerator[i].getCurrentPrice());
					String[] returns = calculateReturnAmounts(initialGenerator[i],grossReturnName,false);
					item.put("grossReturn", returns[0]);
					item.put("percentReturn", returns[1]);
					returnListItems.add(item);
					
					//used to calculate Alpha displayed on screen
					double[] returnAmounts = new double[]{Double.parseDouble(returns[0].substring(1,returns[0].length()).replace(",","")),Double.parseDouble(returns[1].substring(0,returns[1].length()-1).replace(",",""))};
					allReturns.put(initialGenerator[i].getName(), returnAmounts);
				}
				
				//creates list item for the alpha object
				HashMap<String,String> alphaItem = new HashMap<String,String>();
				alphaItem.put("tickerSymbol", ALPHA_NAME);
				alphaItem.put("price", "NONE");
				alphaItem.put("grossReturn", String.format("$%,.0f", allReturns.get(PORTFOLIO_NAME)[0]-allReturns.get(MARKET_NAME)[0]));
				alphaItem.put("percentReturn", String.format("%,.1f", allReturns.get(PORTFOLIO_NAME)[1]-allReturns.get(MARKET_NAME)[1]) + "%");
				returnListItems.add(alphaItem);
				
			} else {
				
				//creates list items for the returnMother's transaction list
				for (String transaction: returnMother.getOrderedTransactionList()) {
						GenerateReturns listItem = returnMother.getTransactionList().get(transaction);
						HashMap<String,String> item = new HashMap<String,String>();
						item.put("tickerSymbol", listItem.getName());
						item.put("price", listItem.getCurrentPrice());
						String[] returns = calculateReturnAmounts(listItem,grossReturnName,listItem.isSell());
						item.put("grossReturn", returns[0]);
						item.put("percentReturn", returns[1]);
						returnListItems.add(item);
					}
				}
			return returnListItems;
		}
		
		
		/* 
		 * Determines the returnMother for the createREturnMethodItems, then creates a new fillList and refreshes the view
		 * @itemName = Home/PORTFOLIO/the ticker symbol of any security in the Portfolio, used to determine returnMother
		 */
		private void refreshList(String itemName) {
			GenerateReturns returnMother;
			//Determines returnMother and exception based on itemName
			if (itemName.equals(PORTFOLIO_NAME)) {
				returnMother = userPortfolio;
				previousView = "Home";
			} else if (userPortfolio.securitiesList.containsKey(itemName)) {
				returnMother = userPortfolio.securitiesList.get(itemName);
				//Unlike other TransacationsAggregateObjects, MARKET returns to the home screen as that is where it is displayed
				if (itemName.equals(MARKET_NAME)) {
					previousView = "Home";
				} else {previousView = PORTFOLIO_NAME;}
			} else {
				returnMother = null;
				previousView = "Home";
			}
			currentView = itemName; //sets the currentView to the new one as this determines what the listView displays
			
			listHeader.setText(currentView);
			listHeader.setTextColor(getResources().getColor(R.color.white_text));
			
			//saves previousView and currentView to preferences so that the user sees the same screen when they return from another activity
			SharedPreferences.Editor preferencesEditor = savedPreferences.edit();
			preferencesEditor.putString("previous view", previousView);
			preferencesEditor.putString("current view", currentView);
			preferencesEditor.apply();
			
			currentReturnItem = returnMother; //used to get necessary data to send to the SummaryView activity
		}
		
		/*
		 * Selects the active item when a new list is generated, either PORTFOLIO or the most recent transaction in a list
		 */
		private void generateNewActiveItem() {
			activeItems = new ArrayList<String>(); // initialize list that holds active items
			if (currentReturnItem == null) { // on the home screen
				activeItems.add(PORTFOLIO_NAME); // add the PORTFOLIO object to the list
			} else { // not on the home screen
				int listSize = currentReturnItem.getOrderedTransactionList().size(); // get the length of the return item list for the currentView
				activeItems.add(currentReturnItem.getOrderedTransactionList().get(listSize-1)); // choose the most recent transaction in that list 
			}
		}
		
		/*
		 * Populate the return item list objects and generate the 'closer look' view
		 */
		private class UpdateView extends AsyncTask<Void,Void, Void> {
			
			@Override
			protected Void doInBackground(Void...voids) {
				fillList = createReturnItems(currentReturnItem); // retrieve the information to populate the return item list objects
				return null;
			}
			
			@Override
			protected void onPostExecute(Void v) {
				CustomListAdapter listAdapter = new CustomListAdapter(PortfolioViewer.this, fillList, R.layout.return_list_item, from, to); // create adapter to map information to xml objects
				listItem.setAdapter(listAdapter); // apply adapter to the list that the user views 
				customViewSelected(); // generate the 'closer look view'
			}
			
		}
		   
		private void customViewSelected() {
		
			closerLookLayout.removeAllViews();
			
			if (activeCloserLook>0) {
				if (activeItems.isEmpty()) {
					generateNewActiveItem();
				} else if (activeItems.size()>1) {
					String showItem = activeItems.get(0);
					activeItems = new ArrayList<String>();
					activeItems.add(showItem);
				}
			}
			
			switch(activeCloserLook) {
				case 0:
				{
					new UpdateGraph().execute();
					break;
				}
				case 1:
				{
					generateSecurityInfoView();
					break;
				}
				case 2: 
				{
					generateSecurityReturnsView();
					break;
				}
			}
		}
		
		private class UpdateGraph extends AsyncTask<Void, Void, Void> {
			
			@Override
			protected Void doInBackground(Void...voids) {
				aggregateGraphSeries();
				return null;
			}
			
			@Override 
			protected void onPostExecute(Void v) {
				createGraphView();
			}
		}
		
		private void clearGraphDataAndRenderers() {
			graphDataset = new XYMultipleSeriesDataset();
			graphRenderer = new XYMultipleSeriesRenderer();
			graphRenderer.setApplyBackgroundColor(true);
			graphRenderer.setBackgroundColor(getResources().getColor(R.color.listItemNormal));
			graphRenderer.setMarginsColor(getResources().getColor(R.color.listItemNormal));
			graphRenderer.setMargins(new int []{5, 50, 20, 45}); //TLBR
			graphRenderer.setLabelsColor(Color.BLACK);
			graphRenderer.setXLabelsColor(Color.BLACK);
			graphRenderer.setYLabelsColor(0, Color.BLACK);
			graphRenderer.setShowGrid(true);
			graphRenderer.setGridColor(Color.GRAY);			
			graphRenderer.setXLabels(0);
			graphRenderer.setXLabelsAngle(0);
			graphRenderer.setYLabels(4);
			graphRenderer.setYLabelsAlign(Align.RIGHT);
			graphRenderer.setLabelsTextSize(18);
			graphRenderer.setYTitle("INDEX");
			graphRenderer.setAxisTitleTextSize(20);
			graphRenderer.setLegendTextSize(20);
			graphRenderer.setShowAxes(false);
			graphRenderer.setPanEnabled(false);
			graphRenderer.setZoomEnabled(false);
		}
		
		private void createGraphView() {
				graphView = ChartFactory.getLineChartView(getApplicationContext(), graphDataset, graphRenderer);
				closerLookLayout.removeAllViews();
				closerLookLayout.addView(graphView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		}
		
		
		private void aggregateGraphSeries() {
			clearGraphDataAndRenderers();
			for (int i=0; i<activeItems.size(); i++) {
				if (currentReturnItem == null) {
					if (activeItems.get(i).equals(PORTFOLIO_NAME)) getLineGraph(userPortfolio,i);
					else if (activeItems.get(i).equals(MARKET_NAME)) getLineGraph(userPortfolio.securitiesList.get(MARKET_NAME), i);
				} else getLineGraph(currentReturnItem.getTransactionList().get(activeItems.get(i)),i);
			}
		}
		
		private void getLineGraph(GenerateReturns returnItem, int color) {
			ReturnsDatabaseConnector returnsDatabaseConnector = new ReturnsDatabaseConnector(getApplicationContext());
			
			String itemName = returnItem.generateReturnDatabaseId();
			String returnName = grossReturnName.replace("Gross ", "Percent");
			
			XYSeries newGraphDataSeries = new XYSeries(returnItem.getName());
			XYSeriesRenderer newGraphRenderer = new XYSeriesRenderer();

			newGraphRenderer.setColor(colors[color]);
			newGraphRenderer.setLineWidth(3);
			
			double x=0;
			
			if (beginningDate.compareTo(returnItem.getPurchaseDate())<0) {
				x = (double)Days.daysBetween(convertStringToLocalDate(beginningDate), convertStringToLocalDate(returnItem.getPurchaseDate())).getDays();
			}
			
			returnsDatabaseConnector.open();
			
			Cursor graphData = returnsDatabaseConnector.getGraphReturns(beginningDate,itemName,returnName);
			
			while (graphData.moveToNext()) {
				double y = graphData.getDouble(1);
				newGraphDataSeries.add(x, y);
				x++;
			}
			
			returnsDatabaseConnector.close();
			
			graphDataset.addSeries(newGraphDataSeries);
			graphRenderer.addSeriesRenderer(newGraphRenderer);
			
			double start = 0;
			double stop = Days.daysBetween(convertStringToLocalDate(beginningDate), LocalDate.now()).getDays();
			double step = Math.max((int)(stop-start)/4,1);
			
			String dateFormat;
			if (stop < 93) {
				dateFormat = "MMM-dd";
			} else if (stop < 1098) {
				dateFormat = "MMM-yyyy";
			} else {
				dateFormat = "yyyy";
			}
			
			for (double i = start; i<=stop; i+=step) {
				graphRenderer.addXTextLabel(i, convertStringToLocalDate(beginningDate).plusDays((int)i).toString(dateFormat));
			}
			
			currentColor ++;
			
			if (currentColor == 5) {
				currentColor = 0;
			}
		}
		
		
		
		/* Adds a new transasction (either buy or sell) to userPortfolio
		 * @param id: transaction id from the database
		 * @param type: Buy or Sell
		 * @param tickerSymvol: ticker symbol of the security
		 * @param transactionDate: date of the tranasction in the LocalDate object format
		 * @param pricePerShare: purchase or sell price of the securities individual shares
		 * @param transaction cost: brokerage cost to execute the transaction
		 * @param numOfShares: number of shares bought or sold
		*/
		private void newTransaction(int id, String type, String tickerSymbol, LocalDate transactionDate, double pricePerShare, double transactionCost, int numOfShares, LocalDate minDate) {
			if (type.equals("Buy")) {
				//Buy transaction
				userPortfolio.createPurchaseTransaction(id, tickerSymbol, transactionDate, pricePerShare, transactionCost, numOfShares, minDate);
			} else {
				//Sell transaction
				userPortfolio.createSellTransaction(id, tickerSymbol, numOfShares, transactionDate, transactionCost, pricePerShare);
			}
		}
		
		private void goBack() {
	    	  listHeader.setTextColor(getResources().getColor(R.color.headerBackground));
	    	  refreshList(previousView);
	    	  generateNewActiveItem();
	    	  new UpdateView().execute();
		}
			   
		//initializes the TradeSecurities activity after the TRADE button is clicked
		private OnClickListener tradeButtonListener = new OnClickListener()
			   {
			      @Override
			      public void onClick(View v)
			      {
			    	  tradeButton.setBackgroundResource(R.drawable.ic_action_dark_new);
			    	  Intent sendData = new Intent (PortfolioViewer.this,TradeSecurities.class);
			    	  startActivity(sendData);
			      } 
			   };
		
		//initialize the UserPreferences activity after the PREFERENCES button is clicked	   
		private OnClickListener preferencesButtonListener = new OnClickListener()
				   {
				      @Override
				      public void onClick(View v)
				      {
				    	  preferencesButton.setBackgroundResource(R.drawable.ic_action_dark_settings);
				    	  Intent openPreferences = new Intent (PortfolioViewer.this, UserPreferences.class);
				    	  startActivity(openPreferences);
				      } 
				   };

		//initialize the UserPreferences activity after the PREFERENCES button is clicked	   
		private OnClickListener dateButtonListener = new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (displayingDateBar) {
						dateSpinner.setVisibility(View.GONE);
						displayingDateBar = false;
					} else {
						dateSpinner.setVisibility(View.VISIBLE);
						displayingDateBar = true;
					}
				}
			};
			
			//initialize the UserPreferences activity after the PREFERENCES button is clicked	   
			private OnClickListener returnSelectButtonListener = new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (displayingReturnSelectButton) {
							returnSelectButton.setBackgroundResource(R.drawable.ic_action_expand);
							spinnerList.setVisibility(View.GONE);
							displayingReturnSelectButton = false;
						} else {
							returnSelectButton.setBackgroundResource(R.drawable.ic_action_collapse);
							spinnerList.setVisibility(View.VISIBLE);
							displayingReturnSelectButton = true;
						}
					}
				};
				   
		
		
		//refreshes the listview after a spinner item has changed
		private OnItemSelectedListener returnSpinnerListener = new OnItemSelectedListener(){
			
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (spinnersStart) {
					if (String.valueOf(currentOrSoldSpinner.getSelectedItem()).equals("Alpha")) {
						marketOrDividendSpinner.setSelection(2,false);
					}
					editReturnName(String.valueOf(taxSpinner.getSelectedItem()),String.valueOf(marketOrDividendSpinner.getSelectedItem()),String.valueOf(currentOrSoldSpinner.getSelectedItem()));
					refreshList(currentView);
					new UpdateView().execute();
				}
			}
		
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		};
		
		//refreshes the listview after a spinner item has changed
		private OnItemSelectedListener dateSpinnerListener = new OnItemSelectedListener(){
			
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (spinnersStart) {
					chooseBeginningDate(dateSpinner.getLastVisiblePosition());
					refreshList(currentView);
					new UpdateView().execute();
				}
			}
		
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		};
		
		private void chooseBeginningDate(int dateSpinnerValue) {
			selectBeginningDate(dateSpinnerValue);
			SharedPreferences.Editor preferencesEditor = savedPreferences.edit();
			preferencesEditor.putInt("date spinner start", dateSpinner.getLastVisiblePosition());
			preferencesEditor.putString("beginning date", this.beginningDate);
			preferencesEditor.apply();
		}
		
		@Override
		protected Dialog onCreateDialog(int id) {
			LocalDate initialDate=convertStringToLocalDate(beginningDate);
			return new DatePickerDialog(this, chooseDateListener,initialDate.getYear(),initialDate.getMonthOfYear()-1, initialDate.getDayOfMonth());
		}
		
		DatePickerDialog.OnDateSetListener chooseDateListener = new DatePickerDialog.OnDateSetListener() {
			
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				beginningDate = new LocalDate(year, monthOfYear +1, dayOfMonth).toString();
			}
		};
		
		private void selectBeginningDate (int userPreference) {
			LocalDate today = LocalDate.now();
			switch (userPreference) {
				case 0: 
				{
					beginningDate = today.minusDays(1).toString();
					break;
				}
				case 1:
				{
					beginningDate = today.minusWeeks(1).toString();
					break;
				}
				case 2:
				{
					beginningDate = today.minusMonths(1).toString();
					break;
				}
				case 3:
				{
					beginningDate = today.minusMonths(3).toString();
					break;
				}
				case 4:
				{
					beginningDate = today.minusMonths(6).toString();
					break;
				}
				case 5:
				{
					beginningDate = today.minusYears(1).toString();
					break;
				}
				case 6:
				{
					if (currentReturnItem == null) beginningDate = userPortfolio.getPurchaseDate();
					else beginningDate = currentReturnItem.getPurchaseDate();
					break;
				}
				default:
				{
					showDialog(1);
				}
			}
		}
		
		//run after an action has been performed on an individual list item, either creates a new list view or generates a SummaryView
		private AdapterView.OnItemClickListener listItemNextListener = new AdapterView.OnItemClickListener(){
			
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						//initiates new list view of current item's getTransactionLsit()
						String itemName = fillList.get((int)id).get("tickerSymbol");
						if (!itemName.contains("-") && !itemName.equals(ALPHA_NAME)) {
							refreshList(itemName);
							generateNewActiveItem();
							new UpdateView().execute();
						}
				}	
			};
		
		private String[] calculateReturnAmounts(GenerateReturns returnItem, String returnName, boolean typeSell) {
			ReturnsDatabaseConnector returnsDatabaseConnector = new ReturnsDatabaseConnector(getApplicationContext());
			String[] returnList = new String[2];
			double endGrossReturn = returnItem.getReturns().get(returnName);
			if (returnItem.getPurchaseDate().compareTo(beginningDate)>=0 || typeSell) {
				 double endPercentReturn = returnItem.getReturns().get(returnName.replace("Gross", "Percent"));
				 returnList[0]=String.format("$%,.0f", endGrossReturn);
				 returnList[1]=String.format("%,.1f", endPercentReturn) + "%";
			} else {
				double begGrossReturn = returnsDatabaseConnector.getSingleReturn(beginningDate, returnItem.generateReturnDatabaseId(), returnName);
				returnList[0]=String.format("$%,.0f", endGrossReturn-begGrossReturn);
				returnList[1]=String.format("%,.1f", (endGrossReturn/begGrossReturn)-1) + "%";
			}
			return returnList;
		}
		
		//checks if a touch event has occured
		public boolean dispatchTouchEvent(MotionEvent event) {
			super.dispatchTouchEvent(event);
			return gestureDetector.onTouchEvent(event);
		}
		
		//checks if the user has swiped to the right to return to the previous screen
		private class SwipeListener extends SimpleOnGestureListener {
			
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				float sensitivity = 200;
				float screenHeight = 640;
				if ((e2.getX()-e1.getX())>sensitivity) {
					if (e2.getY()<screenHeight) {
						goBack();
					} else {
						swiped("LEFT");
					}
					
				} else if ((e1.getX()-e2.getX())>sensitivity) {
					if (e2.getY()<screenHeight) {

					} else {
						swiped("RIGHT");
					}
				}
				return false;
			}
		}
		
		private void swiped(String direction) {
			
			if (transactionDatabaseExists) {
				if (direction.equals("LEFT")) {
					if (activeCloserLook == 0) activeCloserLook = 2;
					else activeCloserLook--;
				} else {
					if (activeCloserLook == 2) activeCloserLook = 0;
					else activeCloserLook++;
				}
				
				for (int i = 0; i<radioButtonGroup.length; i++) {
					if (i==activeCloserLook) {
						radioButtonGroup[i].setChecked(true);
					} else {
						radioButtonGroup[i].setChecked(false);
					}
				}
				
				new UpdateView().execute();
			}
		}
		
		private void editReturnName(String tax, String marketOrDividend, String currentOrSold) {
			String grossReturnNameEdit;
			if (currentOrSold.equals("Alpha")) {
				grossReturnNameEdit = String.format("Total Gross %s %s",tax,currentOrSold);
			}	else if (marketOrDividend.equals("Total")&&currentOrSold.equals("Total")) {
				grossReturnNameEdit = String.format("Total Gross %s",tax);
			} else {
				if (currentOrSold.equals("Total")) {
					grossReturnNameEdit = String.format("Gross %s Total %s", tax, marketOrDividend);
				} else {grossReturnNameEdit = String.format("Gross %s %s %s Holdings", tax, marketOrDividend, currentOrSold);}
			}
			this.grossReturnName = grossReturnNameEdit;
			SharedPreferences.Editor preferencesEditor = savedPreferences.edit();
			preferencesEditor.putString("return name", grossReturnNameEdit);
			preferencesEditor.putInt("tax spinner start", taxSpinner.getLastVisiblePosition());
			preferencesEditor.putInt("market or dividend spinner start", marketOrDividendSpinner.getLastVisiblePosition());
			preferencesEditor.putInt("current or sold spinner start", currentOrSoldSpinner.getLastVisiblePosition());
			preferencesEditor.apply();
		}
		

		private class CustomListAdapter extends SimpleAdapter {
			
			public CustomListAdapter(Context context, List<? extends Map<String,?>> data, int resource, String[] from, int[]to) {
				super(context, data, resource, from, to);
			}
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				
				TextView tickerSymbol = (TextView) view.findViewById(R.id.tickerSymbol);
				TextView price = (TextView) view.findViewById(R.id.price);
				TextView grossReturn = (TextView) view.findViewById(R.id.grossReturn);
				TextView percentReturn = (TextView) view.findViewById(R.id.percentReturn);
				
				String itemName = tickerSymbol.getText().toString();
				
				if (!currentView.equals(PORTFOLIO_NAME)) {
					price.setVisibility(View.GONE);
				}
				
				if (itemName.contains("-")) {
					view.setClickable(false);
					price.setVisibility(View.VISIBLE);
					price.setText(currentReturnItem.getTransactionList().get(itemName).getTransactionType());
					tickerSymbol.setTextSize(16);
				}
				
				RadioButton activeButton = (RadioButton) view.findViewById(R.id.activeButton);
				
				if (activeItems.contains(itemName)) {
					activeButton.setChecked(true);
				} else if (!itemName.equals(ALPHA_NAME)) {
					activeButton.setChecked(false);
				} else {
					activeButton.setVisibility(View.GONE);
				}
				
				activeButton.setOnClickListener(new OnClickListener() {
					
					@Override
			        public void onClick(View v) {
						
							String clickedItemName = fillList.get(listItem.getPositionForView(v)).get("tickerSymbol");
				            if (activeItems.contains(clickedItemName)) {
				            	activeItems.remove(clickedItemName);
				            	((RadioButton)v).setChecked(false);
				            } else if (activeCloserLook>0) {
				            	activeItems.clear();
				            	activeItems.add(clickedItemName);
				            	((RadioButton)v).setChecked(true);
				            } else {
				            	activeItems.add(clickedItemName);
				            	((RadioButton)v).setChecked(true);
				            }
				        if (transactionDatabaseExists) {
				            new UpdateView().execute();
						}
					}
				});
				
				String grossReturnValue = grossReturn.getText().toString();
				
				if (!grossReturnValue.equals("$")) {	
					if (Double.parseDouble(grossReturnValue.substring(1,grossReturnValue.length()).replaceAll(",",""))<0) {
						grossReturn.setBackgroundResource(R.drawable.return_negative);
						percentReturn.setBackgroundResource(R.drawable.return_negative);
					}
				}
				
				return view;
			}
		}
		
		private OnRefreshListener onRefreshListener = new OnRefreshListener() {
			
			@Override
			public void onRefreshStarted(View view) {
					
				progressDialog = new ProgressDialog(PortfolioViewer.this);
				progressDialog.setCancelable(false);
				progressDialog.setProgressNumberFormat(null);
				progressDialog.setTitle("Updating Returns. Please Wait...");
				
					if (historicalDataNeedsUpdating() || restart) {
						progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						progressDialog.show();
						
						userPortfolio = new Portfolio();
						userPortfolio.setCapitalGainsRate(Double.parseDouble(savedPreferences.getString("capital gains rate", "15"))/100);
						userPortfolio.setIncomeTaxRate(Double.parseDouble(savedPreferences.getString("income tax rate", "25"))/100);
						userPortfolio.setContext(getApplicationContext());
						
						new LoadData().execute();
					} else {	
						progressDialog.show();
						
						new RefreshData().execute();
					}
				} 
		};
		
		private class RefreshData extends AsyncTask<Void,Void,Boolean> {
		
			protected Boolean doInBackground(Void...voids) {
				if (isNetworkAvailable()) {
					userPortfolio.calculateReturns(LocalDate.now());
					return true;
				} else {
					return false;
				}

			}
		
			@Override
			protected void onPostExecute(Boolean connected) {
				mPullToRefreshLayout.setRefreshComplete();
				progressDialog.dismiss();
				if (connected) {
					initializeView();
				} else {
					createDialogBox("Could not connect to the internet");
				}
				
			}
		}
		
		private boolean isNetworkAvailable() {
		    ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		    return activeNetworkInfo != null && activeNetworkInfo.isConnected() && yahooServerConnectCheck();
		}
		
		private boolean yahooServerConnectCheck() {
			return new PortfolioCalculator().getCurrentPrice(-1, "SPY") >= 0;
		}
		
		private boolean historicalDataNeedsUpdating() {
			ReturnsDatabaseConnector returnsDatabaseConnector = new ReturnsDatabaseConnector(getApplicationContext());
			return !convertStringToLocalDate(returnsDatabaseConnector.getMostRecentDate()).equals(LocalDate.now());
		}
		
		private void createDialogBox(String message) {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PortfolioViewer.this);
			alertDialogBuilder.setTitle("Oops");
			alertDialogBuilder.setMessage(message);
			alertDialogBuilder.setPositiveButton("OK", null);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}

	private void populateHistoricalData() {

			TransactionDatabaseConnector transactionDatabaseConnector = new TransactionDatabaseConnector(PortfolioViewer.this);
			
			transactionDatabaseConnector.open();
			int numOfTransactions = transactionDatabaseConnector.countRows();
			
			Cursor transactionData = transactionDatabaseConnector.getAllTransactions();
			
			LocalDate minDate;
			
			transactionData.moveToFirst();
			
			ReturnsDatabaseConnector returnsDatabaseConnector;
			
			boolean update;
			
			if (doesDatabaseExist(getApplicationContext(),RETURNS_DATABASE_NAME)) {
				
				returnsDatabaseConnector = new ReturnsDatabaseConnector(getApplicationContext());

				if (returnsDatabaseConnector.getMostRecentDate() == null) {
					minDate = convertStringToLocalDate(transactionData.getString(3));
					update = false;
				} else {
					minDate = convertStringToLocalDate(returnsDatabaseConnector.getMostRecentDate());
					update = true;
				}
				
			} else {

				minDate = convertStringToLocalDate(transactionData.getString(3));
				
				update = false;
				
				returnsDatabaseConnector = new ReturnsDatabaseConnector(getApplicationContext());
			}
			
			progressAmount = 0;
				for (LocalDate date = minDate; date.compareTo(LocalDate.now())<=0; date = date.plusDays(1)) {
					while (!transactionData.isAfterLast() && convertStringToLocalDate(transactionData.getString(3)).compareTo(date)<=0) {
						LocalDate transactionDate = convertStringToLocalDate(transactionData.getString(3));
						newTransaction(transactionData.getInt(0),transactionData.getString(1),transactionData.getString(2),transactionDate, Double.parseDouble(transactionData.getString(5)), Double.parseDouble(transactionData.getString(6)), Integer.parseInt(transactionData.getString(4)), minDate.minusWeeks(1));
						progressAmount = progressAmount + (20 / numOfTransactions);
						progressDialog.setProgress((int)progressAmount);
						transactionData.moveToNext();
					}
					userPortfolio.calculateReturns(date);
					returnsDatabaseConnector.insertReturns(date.toString(),userPortfolio.getReturnsDatabase(),date.equals(minDate)&&update);
					progressAmount = progressAmount + (1.0/Days.daysBetween(minDate, LocalDate.now().plusDays(1)).getDays()) * 80;
					progressDialog.setProgress((int)progressAmount);
				}
			transactionDatabaseConnector.close();
		}

	private static boolean doesDatabaseExist(Context context, String databaseName) {
    	File dbFile= context.getDatabasePath(databaseName);
    	if (dbFile.exists()) {
    		TransactionDatabaseConnector transactionDatabaseConnector = new TransactionDatabaseConnector(context);
    		if (transactionDatabaseConnector.isEmpty()) {
    			context.deleteDatabase(TRANSACTIONS_DATABASE_NAME);
    			context.deleteDatabase(RETURNS_DATABASE_NAME);
    			return false;
    		} else {
    			return true;
    		}
    	} else {
    		return false;
    	}
	}

	private LocalDate convertStringToLocalDate(String date) {
		return new LocalDate(Integer.parseInt(date.substring(0,4)), Integer.parseInt(date.substring(5,7)), Integer.parseInt(date.substring(8,10)));
	}
		   
	public void onRadioButtonClicked(View view) {
	    // Is the button now checked?
	    boolean checked = ((RadioButton) view).isChecked();
	    
	    // Check which radio button was clicked
	    switch(view.getId()) {
	        case R.id.closerLook1:
	            if (checked)
	                activeCloserLook = 0;
	            break;
	        case R.id.closerLook2:
	            if (checked)
	                activeCloserLook = 1;
	            break;
	        case R.id.closerLook3:
	            if (checked)
	                activeCloserLook = 2;
	            break;
	    }
	    
	    if (transactionDatabaseExists) {
	    	new UpdateView().execute();
	    }
	}
	
	private void generateSecurityInfoView() {
		View securityInfoLayout = getLayoutInflater().inflate(R.layout.security_info, null);
		closerLookLayout.addView(securityInfoLayout);
		
		Button editButton = (Button) securityInfoLayout.findViewById(R.id.editButton);
		Button deleteButton = (Button) securityInfoLayout.findViewById(R.id.deleteButton);
		LinearLayout buttonHolder = (LinearLayout) securityInfoLayout.findViewById(R.id.buttonHolder);
		
		if (activeItems.get(0).contains("-") && !currentView.equals(MARKET_NAME)) {
			editButton.setVisibility(View.VISIBLE);
			deleteButton.setVisibility(View.VISIBLE);
			buttonHolder.setVisibility(View.VISIBLE);
			editButton.setOnClickListener(editButtonListener);
			deleteButton.setOnClickListener(deleteButtonListener);
		} else {
			editButton.setVisibility(View.GONE);
			deleteButton.setVisibility(View.GONE);
			buttonHolder.setVisibility(View.GONE);
		}
		
		ArrayList<HashMap<String,String>> securityInfo;
		
		if (currentReturnItem == null) {
			if (activeItems.get(0).equals(PORTFOLIO_NAME)) securityInfo = userPortfolio.getTransactionInfo();
			else securityInfo = userPortfolio.securitiesList.get(MARKET_NAME).getTransactionInfo();
		} else securityInfo = currentReturnItem.getTransactionList().get(activeItems.get(0)).getTransactionInfo();
		
		ListView infoListItem = (ListView) findViewById(R.id.securityInfoList);
		String[] fromInfoList = new String[]{"Label","Data"};
		int[] toInfoList = new int[]{R.id.securityInfoLabel, R.id.securityInfoValue};
		SimpleAdapter infoListAdapter = new SimpleAdapter(this, securityInfo, R.layout.security_info_list_item, fromInfoList, toInfoList);
		infoListItem.setAdapter(infoListAdapter);
	}
	
	private void generateSecurityReturnsView() {
		View securityReturnsLayout = getLayoutInflater().inflate(R.layout.security_return, null);
		closerLookLayout.addView(securityReturnsLayout);
		
		HashMap<String, ArrayList<HashMap<String,String>>> returnInfo = new HashMap<String, ArrayList<HashMap<String,String>>>();
		
		for (String taxType : new String[]{"Pre-Tax", "Post-Tax"}) {
			if (currentReturnItem == null) {
				if (activeItems.get(0).equals(PORTFOLIO_NAME)) returnInfo.put(taxType, userPortfolio.getReturnInfo(taxType));
				else returnInfo.put(taxType, userPortfolio.securitiesList.get(MARKET_NAME).getReturnInfo(taxType));
			} else returnInfo.put(taxType, currentReturnItem.getTransactionList().get(activeItems.get(0)).getReturnInfo(taxType));
		}
		
		String[] fromReturnList = new String[]{"Label","Gross Return", "Percent Return"};
		int[] toReturnList = new int[]{R.id.returnNameValue, R.id.grossAmountValue, R.id.percentAmountValue};
		
		ListView preTaxReturnListItem = (ListView) findViewById(R.id.preTaxReturnList);
		SimpleAdapter preTaxReturnListAdapter = new CustomReturnListAdapter(this, returnInfo.get("Pre-Tax"), R.layout.security_return_list_item, fromReturnList, toReturnList);
		preTaxReturnListItem.setAdapter(preTaxReturnListAdapter);
		
		ListView postTaxReturnListItem = (ListView) findViewById(R.id.postTaxReturnList);
		SimpleAdapter postTaxReturnListAdapter = new CustomReturnListAdapter(this, returnInfo.get("Post-Tax"), R.layout.security_return_list_item, fromReturnList, toReturnList);
		postTaxReturnListItem.setAdapter(postTaxReturnListAdapter);
	}
	
	private class CustomReturnListAdapter extends SimpleAdapter {
		
		public CustomReturnListAdapter(Context context, List<? extends Map<String,?>> data, int resource, String[] from, int[]to) {
			super(context, data, resource, from, to);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			
			TextView grossReturn = (TextView) view.findViewById(R.id.grossAmountValue);
			TextView percentReturn = (TextView) view.findViewById(R.id.percentAmountValue);
			
			String grossReturnValue = grossReturn.getText().toString();
			if (Double.parseDouble(grossReturnValue.replaceAll(",",""))<0) {
				grossReturn.setTextColor(Color.RED);
				percentReturn.setTextColor(Color.RED);
			}
			
			return view;
		}
	}
	
	private OnClickListener editButtonListener = new OnClickListener()
	   {
	      @Override
	      public void onClick(View v)
	      {
	  		Intent sendData = new Intent(PortfolioViewer.this, TradeSecurities.class);
			sendData.putExtra("edit",true);
			sendData.putExtra("id", currentReturnItem.getTransactionList().get(activeItems.get(0)).getId());
			startActivity(sendData);
	      } 
	   };
	   
		private OnClickListener deleteButtonListener = new OnClickListener()
		   {
		      @Override
		      public void onClick(View v)
		      {
		  		      // create a new AlertDialog Builder
		  		      AlertDialog.Builder builder = 
		  		         new AlertDialog.Builder(PortfolioViewer.this);

		  		      builder.setTitle("Confirm"); // title bar string
		  		      builder.setMessage(String.format("Are you sure you want to delete the transaction:\n"
		  		      		+ "Ticker Symbol: %s\n"
		  		      		+ "Transaction Date: %s", currentView, activeItems.get(0))); // message to display

		  		      // provide an OK button that simply dismisses the dialog
		  		      builder.setPositiveButton("Yes",
		  		         new DialogInterface.OnClickListener()
		  		         {
		  		            @Override
		  		            public void onClick(DialogInterface dialog, int button)
		  		            {
		  		               final TransactionDatabaseConnector transactionDatabaseConnector = 
		  		                  new TransactionDatabaseConnector(PortfolioViewer.this);

		  		             final ReturnsDatabaseConnector returnsDatabaseConnector = new ReturnsDatabaseConnector(PortfolioViewer.this);
		  		               // create an AsyncTask that deletes the contact in another 
		  		               // thread, then calls finish after the deletion
		  		               AsyncTask<Void, Void, Void> deleteTask =
		  		                  new AsyncTask<Void, Void, Void>()
		  		                  {
		  		                     @Override
		  		                     protected Void doInBackground(Void...voids)
		  		                     {
		  		                    	int transactionId = currentReturnItem.getTransactionList().get(activeItems.get(0)).getId();
		  		                    	String date = currentReturnItem.getTransactionList().get(activeItems.get(0)).getPurchaseDate();
		  		                    	
		  		                        transactionDatabaseConnector.deleteTransaction(transactionId);
		  		                        returnsDatabaseConnector.deleteReturns(date);
		  		                        return null;
		  		                     } // end method doInBackground

		  		                     @Override
		  		                     protected void onPostExecute(Void v)
		  		                     {
		  					  			SharedPreferences.Editor preferencesEditor = savedPreferences.edit();
		  								preferencesEditor.putString("previous view", "Home");
		  								preferencesEditor.putString("current view", "Home");
		  								preferencesEditor.putInt("date spinner start", 5);
		  								preferencesEditor.putString("beginning date", LocalDate.now().minusYears(1).toString());
		  								preferencesEditor.apply();
		  								onCreate(null);
		  		                     } // end method onPostExecute
		  		                  }; // end new AsyncTask

		  		               // execute the AsyncTask to delete contact at rowID
		  		               deleteTask.execute();               
		  		            } // end method onClick
		  		         } // end anonymous inner class
		  		      ); // end call to method setPositiveButton
						builder.setNegativeButton("No",new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,int id) {
								dialog.cancel();
							}
						});
					  builder.create();
				      builder.show(); // display the Dialog
		      } 
		   };   
		   
		   @Override
		   public void onBackPressed() {
			   if (transactionDatabaseExists) {
				   goBack();
			   } else {
				   super.onBackPressed();
			   }
			   
		   }
}
