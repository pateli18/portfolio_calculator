package com.ffm.portfoliocalculator;

import java.util.HashMap;
import java.util.ArrayList;

public interface GenerateReturns {
	public String getName();
	public HashMap<String,Double> getReturns();
	public HashMap<String,? extends Transaction> getTransactionList();
	public ArrayList<HashMap<String,String>> getTransactionInfo();
	public ArrayList<HashMap<String,String>> getReturnInfo(String preOrPost);
	public String[] getTransactionSummary();
	public ArrayList<String> getOrderedTransactionList();
	public String generateReturnDatabaseId();
	public HashMap<String, Double> getAllReturns();
	public String getPurchaseDate();
	public boolean isSell();
	public String getCurrentPrice();
	public String getTransactionType();
}
