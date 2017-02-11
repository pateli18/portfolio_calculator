package com.ffm.portfoliocalculator;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class ReturnsDatabaseConnector {
	
	private static final String DATABASE_NAME = "Returns";
	private SQLiteDatabase database;
	private DatabaseOpenHelper databaseOpenHelper;
	
	/*
	 * Initializes the database connector
	 * @param context: context in which the database is contained, use getApplicationContext()
	 */
	public ReturnsDatabaseConnector(Context context) {
		this.databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 2);
	}
	
	/* 
	 * Opens the database
	 */
	public void open() throws SQLException {
		this.database = this.databaseOpenHelper.getWritableDatabase();
	}

	/*
	 * Closes the database if it is open
	 */
	public void close() {
		if (this.database !=null) {this.database.close();}
	}
	
	/*
	 * Inserts all of the return data into the database, passing in data from the user's portfolio, putting it into a ContentValues object,
	 * and then inputting it into the database
	 * @param date: date for which the returns are being input
	 * @param returnsData: all of the returns data in the form of <Ticker Symbol,<Name of Return, Return Amount>>
	 * @param update: determine whether to insert a new value (false) or update an existing value (true)
	 */
	public void insertReturns(String date, HashMap<String,HashMap<String,Double>> returnsData, boolean update) {
		ContentValues newReturn = new ContentValues(); //create object be put into the database
		this.open(); //open the database
		if (!update) { //inserting a new value
			for (String transactionItem : returnsData.keySet()) { //cycle through each ticker symbol
				newReturn.put("date", date);
				newReturn.put("item", transactionItem);
				for (String amount : returnsData.get(transactionItem).keySet()) { //cycle through each return
					newReturn.put(amount.replace(" ","").replace("-","_"), returnsData.get(transactionItem).get(amount)); //enters the return name, formatted for the database, with its corresponding amount
				}
			this.database.insert("returns", null, newReturn); //inserts values into database	
			}
		} else { //updating existing value
			for (String transactionItem : returnsData.keySet()) {
				newReturn.put("date", date);
				newReturn.put("item", transactionItem);
				for (String amount : returnsData.get(transactionItem).keySet()) {
					newReturn.put(amount.replace(" ","").replace("-","_"), returnsData.get(transactionItem).get(amount));
				}
				this.database.update("returns", newReturn, "date=? AND item=?", new String[]{date,transactionItem}); //changes return amount for corresponding ticker symbol / date
			}
		}
		this.close(); //close the database
	}

	/*
	 * Get returns needed for the graph view
	 * @param itemDate: initial date of the graph
	 * @param itemName: ticker symbol of the security to graph
	 * @param returnName: return name to graph
	 * @return cursor containing all of the data for each date
	 */
	public Cursor getGraphReturns(String itemDate, String itemName, String returnName) {
		return this.database.query("returns", new String[]{"date", returnName.replace(" ", "").replace("-", "_")}, "date>=? AND item=?", new String[] {itemDate, itemName}, null, null, "date");
	}
	
	/*
	 * Get a single return amount 
	 * @param itemDate: date of the return
	 * @param itemName: ticker symbol of the security
	 * @param return name: name of the return
	 * @return amount of the return
	 */
	public double getSingleReturn(String itemDate,String itemName, String returnName) {
		this.open(); //open the database
		Cursor data = this.database.query("returns", null, "date=? AND item=?", new String[]{itemDate, itemName}, null, null, null); //get data cursor filtered for requested parameters
		data.moveToNext(); //move to the first line of the data table
		double singleReturn= data.getDouble(data.getColumnIndex(returnName.replace(" ","").replace("-","_"))); //get data from the cursor and store the value to return
		this.close(); // close the database
		return singleReturn;
	}

	/*
	 * Gets the most recent date available in the database
	 * @return most recent date in the database
	 */
	public String getMostRecentDate() {
		String mostRecentDate;
		this.open(); //open the database
		Cursor returnsDatabase = this.database.query("returns", new String[]{"date"}, null, null, null,null, "date DESC"); //get cursor sorted in descending order by date
		if (returnsDatabase.moveToNext()) { //check if table is empty
			mostRecentDate = returnsDatabase.getString(0); //get most recent date from the cursor and store it in the return value
		} else {
			mostRecentDate = null; //table is empty
		}
		this.close(); //close the database
		return mostRecentDate;
	}
	
	/*
	 * Gets the most recent price of the security stored in the database
	 * @param date: date for which the price is needed
	 * @param itemName: ticker symbol of the security 
	 * @return the most recent price in the database
	 */
	public double getCurrentPrice(String date, String itemName) {
		double currentPrice;
		if (getMostRecentDate() != null && date.compareTo(getMostRecentDate())<=0) { //check that the price requested is for a date that exists in the database
			currentPrice = this.getSingleReturn(this.getMostRecentDate(),itemName, "Price"); //get price amount from the database
		} else {
			currentPrice = 0; //price requested for the date not available in the database
		}
		return currentPrice;
	}
	
	/*
	 * Delete all the returns starting from a specific date
	 * @param date: date to delete initial returns from
	 */
	public void deleteReturns(String date) 
	{
	   if (date != null) { //ensure date is not null
			open(); //open database
		    database.delete("returns", "date>=?", new String[]{date}); //delete all returns greater than or equal to the parameter date
		    close(); //delete database
	   }
	} 
	
	/*
	 * Needed to create additional database
	 */
	private class DatabaseOpenHelper extends SQLiteOpenHelper {
		public DatabaseOpenHelper(Context context, String name, CursorFactory factory, int version) {
			super(context,name,factory,version);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			String createQuery = "CREATE TABLE returns" + "(_id INTEGER PRIMARY KEY, date TEXT, item TEXT, Price REAL"
					+ ", GrossPre_TaxMarketCurrentHoldings REAL, GrossPre_TaxDividendCurrentHoldings REAL, GrossPre_TaxTotalCurrentHoldings REAL"
					+ ", GrossPre_TaxMarketSoldHoldings REAL, GrossPre_TaxDividendSoldHoldings REAL, GrossPre_TaxTotalSoldHoldings REAL"
					+ ", GrossPre_TaxTotalMarket REAL, GrossPre_TaxTotalDividend REAL, TotalGrossPre_Tax REAL"
					+ ", GrossPost_TaxMarketCurrentHoldings REAL, GrossPost_TaxDividendCurrentHoldings REAL, GrossPost_TaxTotalCurrentHoldings REAL"
					+ ", GrossPost_TaxMarketSoldHoldings REAL, GrossPost_TaxDividendSoldHoldings REAL, GrossPost_TaxTotalSoldHoldings REAL"
					+ ", GrossPost_TaxTotalMarket REAL, GrossPost_TaxTotalDividend REAL, TotalGrossPost_Tax REAL"
					+ ", TotalGrossPre_TaxAlpha REAL, TotalGrossPost_TaxAlpha REAL"
					+ ", PercentPre_TaxMarketCurrentHoldings REAL, PercentPre_TaxDividendCurrentHoldings REAL, PercentPre_TaxTotalCurrentHoldings REAL"
					+ ", PercentPre_TaxMarketSoldHoldings REAL, PercentPre_TaxDividendSoldHoldings REAL, PercentPre_TaxTotalSoldHoldings REAL"
					+ ", PercentPre_TaxTotalMarket REAL, PercentPre_TaxTotalDividend REAL, TotalPercentPre_Tax REAL"
					+ ", PercentPost_TaxMarketCurrentHoldings REAL, PercentPost_TaxDividendCurrentHoldings REAL, PercentPost_TaxTotalCurrentHoldings REAL"
					+ ", PercentPost_TaxMarketSoldHoldings REAL, PercentPost_TaxDividendSoldHoldings REAL, PercentPost_TaxTotalSoldHoldings REAL"
					+ ", PercentPost_TaxTotalMarket REAL, PercentPost_TaxTotalDividend REAL, TotalPercentPost_Tax REAL"
					+ ", TotalPercentPre_TaxAlpha REAL, TotalPercentPost_TaxAlpha REAL);";
			db.execSQL(createQuery);
		}
	
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{	
		}
	}

}