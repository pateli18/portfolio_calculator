package com.ffm.portfoliocalculator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class TransactionDatabaseConnector {
	
	private static final String DATABASE_NAME = "Transactions";
	private SQLiteDatabase database;
	private DatabaseOpenHelper databaseOpenHelper;
	
	public TransactionDatabaseConnector(Context context) {
		databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 2);
	}
	
	public void open() throws SQLException {
		database = databaseOpenHelper.getWritableDatabase();
	}

	public void close() {
		if (database !=null) {database.close();}
	}
	
	public void insertTransaction(String type, String tickerSymbol, String transactionDate, int numOfShares, double pricePerShare, double transactionCost) {
		ContentValues newTransaction = new ContentValues();
		newTransaction.put("type", type);
		newTransaction.put("tickerSymbol", tickerSymbol);
		newTransaction.put("transactionDate", transactionDate);
		newTransaction.put("numOfShares", numOfShares);
		newTransaction.put("pricePerShare", pricePerShare);
		newTransaction.put("transactionCost", transactionCost);
		
		open();
		database.insert("transactions", null, newTransaction);
		close();
	}
	
	public boolean isEmpty(){
		boolean empty;
		this.open();
		Cursor transactionDatabase = this.database.query("transactions", new String[]{"transactionDate"}, null, null, null,null, "transactionDate DESC");
		if (transactionDatabase.moveToNext()) {
			empty = false;
		} else {
			empty = true;
		}
		this.close();
		return empty;
	}
	
	public int countRows() {
		return (int)DatabaseUtils.queryNumEntries(this.database, "transactions");
	}
	
	public void updateTransaction(int id, String type, String tickerSymbol, String transactionDate, int numOfShares, double pricePerShare, double transactionCost)
	{
		ContentValues editTransaction = new ContentValues();
		editTransaction.put("type", type);
		editTransaction.put("tickerSymbol", tickerSymbol);
		editTransaction.put("transactionDate", transactionDate);
		editTransaction.put("numOfShares", numOfShares);
		editTransaction.put("pricePerShare", pricePerShare);
		editTransaction.put("transactionCost", transactionCost);
		
		open();
		database.update("transactions", editTransaction, "_id=" + id, null);
		close();
	} // end method updateContact
	
	public Cursor getAllTransactions() {
		return database.query("transactions", null, null, null, null,null, "transactionDate");
	}
	
	public Cursor getOneTransaction(int id) {
	      return database.query(
	         "transactions", null, "_id=" + id, null, null, null, null);
	}
	
	public void deleteTransaction(int id) 
	{
	      open(); // open the database
	      database.delete("transactions", "_id=" + id, null);
	      close(); // close the database
	} // end method deleteTransaction

	private class DatabaseOpenHelper extends SQLiteOpenHelper {
		public DatabaseOpenHelper(Context context, String name, CursorFactory factory, int version) {
			super(context,name,factory,version);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			String createQuery = "CREATE TABLE transactions" + "(_id INTEGER PRIMARY KEY, type TEXT,"
					+ "tickerSymbol TEXT, transactionDate TEXT, numOfShares INTEGER, pricePerShare REAL, transactionCost REAL);";
			
			db.execSQL(createQuery);
		}
	
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{	
		}
	}

}
