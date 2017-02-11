package com.ffm.portfoliocalculator;

import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.LocalDate;
import org.joda.time.Days;

public class SellingTransaction extends Transaction implements GenerateReturns {
	
	public String tickerSymbol;
	public int numOfShares;
	private int sharesAvailable;
	private LocalDate purchaseDate;
	public LocalDate sellDate;
	public double transactionCost;
	public double pricePerShare;
	public double currentPrice;
	public HashMap <String, Double> returns;
	public double totalPurchaseAmount;
	private double totalSellAmount;
	private double capitalGainsRate;
	private double incomeTaxRate;
	private double taxRate;
	public HashMap <String, Double> taxCredit;
	private HashMap <String, SellingTransaction> individualTransactionList;
	private Portfolio portfolio;
	private String transactionType;
	public int id;
	
	public SellingTransaction(Portfolio portfolio,int id,String tickerSymbol, int numOfShares, int sharesAvailable, LocalDate sellDate, double transactionCost, double pricePerShare, LocalDate purchaseDate) {
		this.tickerSymbol = tickerSymbol;
		this.numOfShares = numOfShares;
		this.sharesAvailable=sharesAvailable;
		this.purchaseDate = purchaseDate;
		this.sellDate = sellDate;
		this.transactionCost = transactionCost;
		this.currentPrice = pricePerShare;
		this.totalPurchaseAmount = 0.00;
		this.capitalGainsRate = portfolio.capitalGainsRate;
		this.incomeTaxRate = portfolio.incomeTaxRate;
		this.portfolio = portfolio;
		this.taxRate = 0.00;
		this.returns = new HashMap<String,Double>();
		this.transactionType = "Sell";
		this.id = id;
		
		//This initializes the returns hash table
		this.returns = new HashMap<String,Double>();
		this.returns.put("Gross Pre-Tax Market Current Holdings",0.00);
		this.returns.put("Gross Post-Tax Market Current Holdings",0.00);
		this.returns.put("Percent Pre-Tax Market Current Holdings",0.00);
		this.returns.put("Percent Post-Tax Market Current Holdings",0.00);
		this.returns.put("Gross Pre-Tax Dividend Current Holdings",0.00);
		this.returns.put("Gross Post-Tax Dividend Current Holdings",0.00);
		this.returns.put("Percent Pre-Tax Dividend Current Holdings",0.00);
		this.returns.put("Percent Post-Tax Dividend Current Holdings",0.00);
		this.returns.put("Gross Pre-Tax Total Current Holdings",0.00);
		this.returns.put("Gross Post-Tax Total Current Holdings",0.00);
		this.returns.put("Percent Pre-Tax Total Current Holdings",0.00);
		this.returns.put("Percent Post-Tax Total Current Holdings",0.00);
		this.returns.put("Total Gross Pre-Tax Alpha",0.00);
		this.returns.put("Total Gross Post-Tax Alpha",0.00);
		this.returns.put("Total Percent Pre-Tax Alpha",0.00);
		this.returns.put("Total Percent Post-Tax Alpha",0.00);
		/**
		* This initializes the tax credit hash table
		*/
		this.taxCredit = new HashMap<String,Double>();
		this.taxCredit.put("Current",0.00);
		this.taxCredit.put("Sold",0.00);
		this.taxCredit.put("Total",0.00);
	}		

	public void aggregateSellReturns(HashMap<String,Transaction> aggregateTransactionList) {
		individualTransactionList = new HashMap<String,SellingTransaction>();;
		String[] returnList = new String[]{"Market","Dividend"};
		for (String item: returnList) {
			this.returns.put("Gross Pre-Tax " + item + " Sold Holdings",0.00);
		}
		for (String transaction: aggregateTransactionList.keySet()) {
			if (aggregateTransactionList.get(transaction).sellTransactionsList.containsKey(this.sellDate.toString())) {
				for (String item: returnList) {
					double addReturn = this.returns.get("Gross Pre-Tax "+item+" Sold Holdings")+aggregateTransactionList.get(transaction).sellTransactionsList.get(this.sellDate.toString()).returns.get("Gross Pre-Tax "+item+" Sold Holdings");
					this.returns.put("Gross Pre-Tax "+item+" Sold Holdings",addReturn);
				}
				this.totalPurchaseAmount+=aggregateTransactionList.get(transaction).sellTransactionsList.get(this.sellDate.toString()).totalPurchaseAmount;
				this.individualTransactionList.put(aggregateTransactionList.get(transaction).purchaseDate.toString(),aggregateTransactionList.get(transaction).sellTransactionsList.get(this.sellDate.toString()));
			}
		}
		for (String item: returnList) {
			this.returns.put("Gross Post-Tax "+item+" Sold Holdings",this.returns.get("Gross Pre-Tax "+item+" Sold Holdings") - this.aggregateSellTransactionTaxBill(this.individualTransactionList,(item + " Sold"),"No Exception"));
			this.returns.put("Percent Pre-Tax "+item+" Sold Holdings",PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Pre-Tax "+item+" Sold Holdings"),this.totalPurchaseAmount));
			this.returns.put("Percent Post-Tax "+item+" Sold Holdings",PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Post-Tax "+item+" Sold Holdings"),this.totalPurchaseAmount));
		}
		this.pricePerShare =(this.totalPurchaseAmount - this.transactionCost)/this.numOfShares;
		this.addReturns("Market Sold Holdings","Dividend Sold Holdings",this.totalPurchaseAmount,"Sold Holdings");
		this.copySoldToTotalReturns();
	}

	public void calculateReturns() {
		this.calculatePreTaxReturn("Sold");
		this.calculatePostTaxReturn("Sold");
		this.copySoldToTotalReturns();
	}
	
	private void copySoldToTotalReturns() {
		String[] preOrPostTax = new String[]{"Pre-Tax","Post-Tax"};
		for (String tax: preOrPostTax) {
			String[] marketOrDividend = new String[]{"Market","Dividend"};
			for (String type: marketOrDividend) {
				this.returns.put(String.format("Gross %s Total %s", tax,type),this.returns.get(String.format("Gross %s %s Sold Holdings", tax,type)));
				this.returns.put(String.format("Percent %s Total %s", tax,type),this.returns.get(String.format("Percent %s %s Sold Holdings", tax,type)));
			}
			this.returns.put(String.format("Total Gross %s",tax),this.returns.get(String.format("Gross %s Total Sold Holdings", tax)));
			this.returns.put(String.format("Total Percent %s",tax),this.returns.get(String.format("Percent %s Total Sold Holdings", tax)));
		}
	}

	private void calculatePreTaxReturn(String holdingType) {
		double transactionCost;
		if (holdingType.equals("Current")&&this.sharesAvailable>0) {
			transactionCost=this.transactionCost;
		} else {
			transactionCost=(double)this.sharesAvailable/this.numOfShares*this.transactionCost;
		}
		this.totalSellAmount=this.sharesAvailable*this.currentPrice - transactionCost;
		this.returns.put("Gross Pre-Tax Market "+ holdingType + " Holdings",this.totalSellAmount-this.totalPurchaseAmount);
		this.returns.put("Percent Pre-Tax Market " + holdingType + " Holdings",PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Pre-Tax Market " + holdingType+ " Holdings"),this.totalPurchaseAmount));
		this.calculateDividendReturns(this.sharesAvailable,holdingType,this.totalPurchaseAmount);
		this.correctTaxRate(holdingType);
	}

	private void calculatePostTaxReturn(String holdingType) {
		this.returns.put("Gross Post-Tax Market " + holdingType + " Holdings",this.returns.get("Gross Pre-Tax Market " + holdingType + " Holdings")*(1-this.taxRate));
		this.returns.put("Percent Post-Tax Market " + holdingType + "Holdings", PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Post-Tax Market " + holdingType + " Holdings"),this.totalPurchaseAmount));
		this.addReturns("Market " + holdingType + " Holdings","Dividend "+holdingType+" Holdings",this.totalPurchaseAmount,holdingType+ " Holdings");
	}

		//calculates the tax rate to use for the post-tax return
	private void correctTaxRate(String holdingType) {
		int daysHeld = Days.daysBetween(purchaseDate,sellDate).getDays();
		if (this.returns.get("Gross Pre-Tax Market " + holdingType + " Holdings")<0) {
			this.taxRate = 0;
			this.taxCredit.put(holdingType, this.returns.get("Gross Pre-Tax Market " + holdingType + " Holdings"));
		} else if (daysHeld>364) {
			this.taxRate=this.capitalGainsRate;
		} else {
			this.taxRate=this.incomeTaxRate;
		} 
	}

	private void calculateDividendReturns(int shares,String returnType, double totalPurchaseAmount) { //need to figure out how to access dividend dictionary
		LocalDate lastDate = new LocalDate();
		double preTaxDivReturn = 0.00;
		double postTaxDivReturn=0.00;
		if (this.portfolio.dividendDictionary.containsKey(this.tickerSymbol)) {
			for (LocalDate dividendDate: this.portfolio.dividendDictionary.get(this.tickerSymbol).dividendHistoryList.keySet()) {
				if (dividendDate.compareTo(this.purchaseDate)>0&&dividendDate.compareTo(this.sellDate)<0) {
					preTaxDivReturn +=(shares*this.portfolio.dividendDictionary.get(this.tickerSymbol).dividendHistoryList.get(dividendDate));
					lastDate = dividendDate;
				}
			}
			if (Days.daysBetween(this.purchaseDate,lastDate).getDays()<60&&Days.daysBetween(lastDate,this.sellDate).getDays()<60) {
				postTaxDivReturn=preTaxDivReturn*(1-this.incomeTaxRate);
			} else {
				postTaxDivReturn=preTaxDivReturn*(1-this.capitalGainsRate);
			}	
			this.returns.put("Gross Pre-Tax Dividend " + returnType + " Holdings",preTaxDivReturn);
			this.returns.put("Gross Post-Tax Dividend " + returnType + " Holdings",postTaxDivReturn);
			this.returns.put("Percent Pre-Tax Dividend " + returnType + " Holdings",PortfolioCalculator.calculatePercentReturn(preTaxDivReturn,totalPurchaseAmount));
			this.returns.put("Percent Post-Tax Dividend " + returnType + " Holdings",PortfolioCalculator.calculatePercentReturn(postTaxDivReturn,totalPurchaseAmount));
		}
	}

	private void addReturns(String returnOne, String returnTwo, double totalAmount, String returnName) {
		String[] preOrPost = new String[] {"Pre","Post"};
		double returnOnePre = this.returns.get("Gross Pre-Tax " + returnOne);
		double returnTwoPre = this.returns.get("Gross Pre-Tax " + returnTwo);
		double returnOnePost = this.returns.get("Gross Post-Tax " + returnOne);
		double returnTwoPost = this.returns.get("Gross Post-Tax " + returnTwo);
		for (String item: preOrPost) {
			String nameGross;
			String namePercent;
			if (!returnName.equals("Total")) {
				nameGross="Gross " + item + "-Tax Total " + returnName;
				namePercent="Percent " + item + "-Tax Total " + returnName;
			} else {
				nameGross="Total Gross " + item + "-Tax";
				namePercent="Total Percent " + item + "-Tax";
			}
			double taxBill;
			if (item.equals("Post")) {
				taxBill=this.calculateTaxBill(returnOnePre,returnTwoPre,returnOnePost,returnTwoPost,returnName);
			} else {
				taxBill = 0.00;
			}
			this.returns.put(nameGross,returnOnePre+returnTwoPre-taxBill);
			this.returns.put(namePercent,PortfolioCalculator.calculatePercentReturn(this.returns.get(nameGross),totalAmount));
		}
	}

	private double calculateTaxBill(double returnOnePre,double returnTwoPre, double returnOnePost, double returnTwoPost, String returnName) {
		double taxBill=(returnOnePre-returnOnePost)+(returnTwoPre-returnTwoPost);
		String holdingType;
		if (returnName.contains("Current")||returnName.contains("Sold")) {
			holdingType = returnName.substring(0,returnName.indexOf(" "));
		} else if (returnName.contains("Market")||returnName.contains("Total")) {
			holdingType = "Market";
		} else {
			holdingType = "Dividend";
		}
			taxBill = this.useTaxCredit(taxBill,holdingType,false);
		return taxBill;
	}

	private double useTaxCredit (double taxBill, String holdingType, boolean aggregate) {
		this.taxCredit.put("Total",this.taxCredit.get("Current")+this.taxCredit.get("Sold"));
		this.taxCredit.put("Market",this.taxCredit.get("Current")+this.taxCredit.get("Sold"));
		if (holdingType.equals("Market")) { 
			aggregate = true;
		}
		if (!this.taxCredit.containsKey(holdingType)) {
			return taxBill;
		}
		if (taxBill + this.taxCredit.get(holdingType) < 0) {
			if (aggregate) {
				double aggTaxCredit = this.taxCredit.get(holdingType)+taxBill;
				this.taxCredit.put(holdingType, aggTaxCredit);
			}
			taxBill=0.00;
		} else {
			if (aggregate) {
				taxBill+=this.taxCredit.get(holdingType);
			}
			this.taxCredit.put(holdingType,0.00);
		}
		return taxBill;
	}
	
	private double aggregateSellTransactionTaxBill(HashMap <String, SellingTransaction> transactionList ,String returnType,String exception) { //need to figure out what return list to input
		double taxBill = 0.00;
		double taxCredit = 0.00;
		for (String transaction: transactionList.keySet()) {
			if (!transactionList.get(transaction).tickerSymbol.equals(exception)) {
				if (transactionList.get(transaction).returns.get("Gross Pre-Tax " + returnType + " Holdings")>0) {
					taxBill+=(transactionList.get(transaction).returns.get("Gross Pre-Tax " + returnType + " Holdings") - transactionList.get(transaction).returns.get("Gross Post-Tax " + returnType + " Holdings"));
				} else {
					taxCredit+=transactionList.get(transaction).returns.get("Gross Pre-Tax " + returnType + " Holdings");
				}
			}
		}
		this.taxCredit.put("Sold", this.taxCredit.get("Sold") + taxCredit);
		double finalTaxBill;
		if (returnType.contains("Market")) {
			finalTaxBill=this.useTaxCredit(taxBill,"Sold",true);
		} else { 
			finalTaxBill=taxBill;
		}
		return finalTaxBill;
	}

	public String getName() {
		return this.sellDate.toString();
	}

	public HashMap<String,Double> getReturns() {
		return this.returns;
	}

	public HashMap<String,? extends Transaction> getTransactionList() {
		return this.individualTransactionList;
	}
	
	public String[] getTransactionSummary() {
		String[] infoButtons = new String[]{this.tickerSymbol,this.sellDate.toString(),String.format("%d",this.id)};
		return infoButtons;	
	}
	
	public ArrayList<HashMap<String,String>> getTransactionInfo() {
		ArrayList<HashMap<String,String>> transactionInfoItems = new ArrayList<HashMap<String,String>>();
		String[] infoItemsList = new String[]{"Transaction Type", this.transactionType, "Purchase Price",String.format("$%,.2f", this.pricePerShare),"Sell Price", String.format("$%,.2f", this.currentPrice),
				"Total Shares",String.format("%d", this.numOfShares),"Transaction Cost", String.format("$%,.2f", this.transactionCost) };
		for (int i=0; i<infoItemsList.length; i+=2) {
			HashMap<String,String> infoItemsData = new HashMap<String,String>();
			infoItemsData.put("Label",infoItemsList[i]);
			infoItemsData.put("Data",infoItemsList[i+1]);
			transactionInfoItems.add(infoItemsData);
		}
		return transactionInfoItems;
	}
	
	public ArrayList<HashMap<String,String>> getReturnInfo(String preOrPost) {
		ArrayList<HashMap<String,String>> returnInfoItems = new ArrayList<HashMap<String,String>>();
		String[] returnItems = new String[]{"Market","Dividend"};
		for (String item: returnItems) {
			HashMap<String,String> returnItemsData = new HashMap<String,String>();
			returnItemsData.put("Label",item);
			returnItemsData.put("Gross Return", String.format("%,.0f",this.returns.get(String.format("Gross %s Total %s", preOrPost,item))));
			returnItemsData.put("Percent Return", String.format("%,.1f",this.returns.get(String.format("Percent %s Total %s", preOrPost,item))));
			returnInfoItems.add(returnItemsData);
		}
		HashMap<String,String> returnItemsData = new HashMap<String,String>();
		returnItemsData.put("Label", "Total");
		returnItemsData.put("Gross Return", String.format("%,.0f",this.returns.get(String.format("Total Gross %s", preOrPost))));
		returnItemsData.put("Percent Return", String.format("%,.1f",this.returns.get(String.format("Total Percent %s", preOrPost))));
		returnInfoItems.add(returnItemsData);
		return returnInfoItems;
	}

	public String generateReturnDatabaseId(String date) {
		return this.tickerSymbol;
	}
	
	public HashMap<String, Double> getAllReturns() {
		HashMap<String, Double> allReturns = new HashMap<String, Double>();
		allReturns.put("Price", this.currentPrice);
		allReturns.putAll(this.returns);
		return allReturns;
	}
	
	public boolean isSell() {
		return true;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getPurchaseDate() {
		return this.sellDate.toString();
	}
	
	public String getCurrentPrice() {
		return String.format("%.2f",this.currentPrice);
	}
	
	public String getTransactionType() {
		return "SELL";
	}
}