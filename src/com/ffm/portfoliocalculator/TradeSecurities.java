package com.ffm.portfoliocalculator;

import java.util.HashMap;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

import org.joda.time.LocalDate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;

public class TradeSecurities extends Activity {
	
	private TextView tradeSecuritiesHeading;
	private Spinner buyOrSellSpinner;
	private EditText tickerSymbol;
	private EditText transactionDate;
	private EditText numberOfShares;
	private EditText pricePerShare;
	private EditText transactionCost;
	private HashMap<String,Integer> currentPortfolio;
	private boolean edit;
	private int editTransactionId;
	private String today;
	private Button submitTradeButton;
	private String errorMessage;
	private ProgressDialog progressDialog;
	
	public void onCreate(Bundle savedInstanceState) 
	   {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.trade_security);
			
			tradeSecuritiesHeading = (TextView) findViewById(R.id.tradeSecuritiesHeading);
			tickerSymbol = (EditText) findViewById(R.id.editTickerSymbol);
			transactionDate = (EditText) findViewById(R.id.editTransactionDate);
			numberOfShares = (EditText) findViewById(R.id.editNumberOfShares);
			pricePerShare = (EditText) findViewById(R.id.editPricePerShare);
			transactionCost = (EditText) findViewById(R.id.editTransactionCost);
			
			buyOrSellSpinner = (Spinner) findViewById(R.id.buyOrSellSpinner);
			ArrayAdapter<CharSequence> buyOrSellSpinnerAdapter = ArrayAdapter.createFromResource(TradeSecurities.this, R.array.buyOrSell_array, android.R.layout.simple_spinner_item);
			buyOrSellSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			buyOrSellSpinner.setAdapter(buyOrSellSpinnerAdapter);
			buyOrSellSpinner.setSelection(0, false);
			
			today = LocalDate.now().toString();
			transactionDate.setText(today);
			
			transactionDate.setOnClickListener(chooseDateButtonListener);
			
			submitTradeButton = (Button) findViewById(R.id.submitTradeButton);
			submitTradeButton.setOnClickListener(submitTradeButtonListener);
			
			currentPortfolio = new HashMap<String,Integer>();
			
			getActionBar().hide();
			
			Intent editData = getIntent();
			
			if (editData.getBooleanExtra("edit", false)) {
				edit = true;
				tradeSecuritiesHeading.setText("Edit Transaction");
				editTransactionId = editData.getIntExtra("id", 1);
				new LoadTransactionData().execute();
			}
			
			new LoadCheckData().execute();

	   } 
		
	    private OnClickListener chooseDateButtonListener = new OnClickListener()
		   {
		      @Override
		      public void onClick(View v)
		      {
		    	  showDialog(1);
		      } 
		   };
		
		@Override
		protected Dialog onCreateDialog(int id) {
			String date = transactionDate.getText().toString();
			return new DatePickerDialog(this, chooseDateListener,Integer.parseInt(date.substring(0,4)),Integer.parseInt(date.substring(5, 7))-1, Integer.parseInt(date.substring(8,10)));
			
		}
		
		DatePickerDialog.OnDateSetListener chooseDateListener = new DatePickerDialog.OnDateSetListener() {
			
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				transactionDate.setText(new LocalDate(year, monthOfYear+1, dayOfMonth).toString());
			}
		};
		
		private boolean ensureEntries() {
			HashMap<EditText, String> textFields = new HashMap<EditText, String>();
			textFields.put(tickerSymbol, "ticker symbol");
			textFields.put(numberOfShares, "number of shares");
			textFields.put(pricePerShare, "price per share");
			textFields.put(transactionCost, "transaction cost");
			
			for (EditText field: textFields.keySet()) {
				if (field.getText().toString().equals("")) {
					errorMessage = String.format("Please enter the %s", textFields.get(field));
					return false;
				}
			}

			return true;
		}
		   
		private boolean validateTickerSymbol(String tickSym) {
			if (String.valueOf(buyOrSellSpinner.getSelectedItem()).equals("Sell")) {
				if(!currentPortfolio.containsKey(tickSym)) {
					errorMessage = "You cannot sell this security because you do not own it! Please select another security";
					return false;
				} else {
					return true;
				}
			} else {
				Double inputReturn = new PortfolioCalculator().getHistoricalPrice(tickSym, LocalDate.now(), LocalDate.now().minusWeeks(1), "d").get(LocalDate.now());
				if (inputReturn!=null && inputReturn <0) {
					errorMessage = "Please enter a valid ticker symbol";
					return false;
				} else {
					return true;
				}
			}
		}
		
		private boolean validateDate(String date) {
			if (date.compareTo(LocalDate.now().toString())>0) {
					errorMessage = "Please do not select a date in the future";
					return false;
			}else if (date.equals(LocalDate.now().toString())) {
				if (LocalDate.now().getDayOfWeek()<6) {
					return true;
				} else {
					errorMessage = "Please enter a trading day for the transaction date (a non-weekend/holiday)";
					return false;
				}
			} else {
				LocalDate localDate = new LocalDate(Integer.parseInt(date.substring(0,4)), Integer.parseInt(date.substring(5,7)), Integer.parseInt(date.substring(8,10)));
				Double inputReturn = new PortfolioCalculator().getHistoricalPrice("SPY", localDate,localDate, "d").get(localDate);
				if (inputReturn!= null && inputReturn > 0) {
					return true;
				} else {
					errorMessage = "Please enter a trading day for the transaction date (a non-weekend/holiday)";
					return false;
				}
			}
		}
		
		private boolean validateNumberOfShares(int shares) {
			if (String.valueOf(buyOrSellSpinner.getSelectedItem()).equals("Sell")) {
				String currentTickerSymbol = tickerSymbol.getText().toString(); 
				if(currentPortfolio.get(currentTickerSymbol)<shares) {
					errorMessage = String.format("You do not have this many shares to sell. The maximum amount of %s shares you may sell is %d", currentTickerSymbol,currentPortfolio.get(currentTickerSymbol));
					return false;
				}
			}
			return true;
		}
		
		private boolean validateNumber(double number, String field) {
			if(number>0) {
				return true;
			} else {
				errorMessage = String.format("Please choose a positive number for %s", field);
				return false;
			}
		}
		
		private void createDialogBox(String message) {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(TradeSecurities.this);
			alertDialogBuilder.setTitle("WARNING");
			alertDialogBuilder.setMessage(message);
			alertDialogBuilder.setPositiveButton("OK", null);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}

		private OnClickListener submitTradeButtonListener = new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				progressDialog = new ProgressDialog(TradeSecurities.this);
				progressDialog.setTitle("Checking Inputs. Please Wait...");
				progressDialog.setCancelable(false);
				progressDialog.show();
				new CheckInputData().execute();
			}
		};
		
		private void confirmTrade () {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(TradeSecurities.this);
			alertDialogBuilder.setTitle("Confirm Trade?");
			alertDialogBuilder.setMessage(String.format("Transaction Type: %s \n"
					+ "Ticker Symbol: %s \n"
					+ "Transaction Date: %s \n"
					+ "Number of Shares: %s \n"
					+ "Price per Share: $%.2f \n"
					+ "Transaction cost: $%.2f",
					String.valueOf(buyOrSellSpinner.getSelectedItem()),tickerSymbol.getText().toString(), transactionDate.getText().toString(),
					numberOfShares.getText().toString(), Double.parseDouble(pricePerShare.getText().toString()), Double.parseDouble(transactionCost.getText().toString())));
			alertDialogBuilder.setCancelable(false);
			alertDialogBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					
					final ReturnsDatabaseConnector returnsDatabaseConnector = new ReturnsDatabaseConnector(TradeSecurities.this);
					
		            AsyncTask<Void, Void, Void> saveTransactionTask = 
		                    new AsyncTask<Void, Void, Void>() 
		                    {
		                       @Override
		                       protected Void doInBackground(Void... voids) 
		                       {
		                          saveTransaction(); // save transaction to the database
		                          returnsDatabaseConnector.deleteReturns(transactionDate.getText().toString());
		                          return null;
		                       } // end method doInBackground
		           
		                       @Override
		                       protected void onPostExecute(Void v) 
		                       {
		                    	  Intent sendData = new Intent (TradeSecurities.this, PortfolioViewer.class);
		                          sendData.putExtra("transactions submitted", true);
		                          startActivity(sendData);
		                       } // end method onPostExecute
		                    }; // end AsyncTask
		                    
		                 // save the contact to the database using a separate thread
		            saveTransactionTask.execute();
				}
			  });
			alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					dialog.cancel();
				}
			});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
		
		private void saveTransaction() {
			TransactionDatabaseConnector databaseConnector = new TransactionDatabaseConnector(getApplicationContext());
			if (edit) {
				databaseConnector.updateTransaction(editTransactionId,
						String.valueOf(buyOrSellSpinner.getSelectedItem()),
						tickerSymbol.getText().toString(),
						transactionDate.getText().toString(),
						Integer.parseInt(numberOfShares.getText().toString()),
						Double.parseDouble(pricePerShare.getText().toString()),
						Double.parseDouble(transactionCost.getText().toString()));
			} else {
				databaseConnector.insertTransaction(
					String.valueOf(buyOrSellSpinner.getSelectedItem()),
					tickerSymbol.getText().toString(),
					transactionDate.getText().toString(),
					Integer.parseInt(numberOfShares.getText().toString()),
					Double.parseDouble(pricePerShare.getText().toString()),
					Double.parseDouble(transactionCost.getText().toString()));
			}
		}
		
		private class LoadCheckData extends AsyncTask<Void,Void,Void> {
			
			TransactionDatabaseConnector databaseConnector = new TransactionDatabaseConnector(TradeSecurities.this);
			
			protected Void doInBackground(Void...voids) {
				databaseConnector.open();
				Cursor transactionData = databaseConnector.getAllTransactions(); //change this to just get necessary data
				while (transactionData.moveToNext()) {
					String transactionTickerSymbol = transactionData.getString(2);
					if (!currentPortfolio.keySet().toString().contains(transactionTickerSymbol)) {
						currentPortfolio.put(transactionTickerSymbol,transactionData.getInt(4));
					} else {
						int currentShares = currentPortfolio.get(transactionTickerSymbol);
						currentPortfolio.put(transactionTickerSymbol, currentShares + transactionData.getInt(4));
					}
				}
				return null;
			}
		
			@Override
			protected void onPostExecute(Void v) {
				databaseConnector.close();
			}
		}
		
		private class LoadTransactionData extends AsyncTask<Void,Void,Cursor> {
			
			TransactionDatabaseConnector databaseConnector = new TransactionDatabaseConnector(TradeSecurities.this);
			
			protected Cursor doInBackground(Void...voids) {
				databaseConnector.open();
				Cursor transactionData = databaseConnector.getOneTransaction(editTransactionId); //change this to just get necessary data
				return transactionData;
			}
		
			@Override
			protected void onPostExecute(Cursor transactionData) {
				while (transactionData.moveToNext()) {
					if (transactionData.getString(1).equals("Buy")) {
						buyOrSellSpinner.setSelection(0);
					} else {buyOrSellSpinner.setSelection(1);}

					tickerSymbol.setText(transactionData.getString(2));

					String date = transactionData.getString(3);
					transactionDate.setText(date);

					numberOfShares.setText(transactionData.getString(4));
					pricePerShare.setText(String.format("%.2f", Double.parseDouble(transactionData.getString(5))));
					transactionCost.setText(String.format("%.2f", Double.parseDouble(transactionData.getString(6))));
				}
				databaseConnector.close();
			}
		}
		
		private class CheckInputData extends AsyncTask<Void, Void, Boolean> {
			
			@Override
			protected Boolean doInBackground(Void...voids) {
				if (ensureEntries() && connectedToInternet() &&
						validateTickerSymbol(tickerSymbol.getText().toString()) &&
						validateNumberOfShares(Integer.parseInt(numberOfShares.getText().toString())) &&
								validateNumber(Double.parseDouble(pricePerShare.getText().toString()),"Price Per Share") &&
								validateNumber(Double.parseDouble(transactionCost.getText().toString()),"Transaction Cost") &&
								validateDate(transactionDate.getText().toString())) {
					return true;
				} else {return false;}
			}
			
			@Override
			protected void onPostExecute(Boolean allDataValid) {
				progressDialog.dismiss();
				if (!allDataValid) {
					createDialogBox(errorMessage);
				} else {
					confirmTrade();
				}
			}
		}
		
		private boolean connectedToInternet() {
			if (isNetworkAvailable() && yahooServerConnectCheck()) {
				return true;
			} else {
				errorMessage = "Not connected to the Internet. Cannot validate data";
				return false;
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
}
