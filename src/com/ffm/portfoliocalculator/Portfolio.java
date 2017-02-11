package com.ffm.portfoliocalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.joda.time.LocalDate;

import android.content.Context;

public class Portfolio extends TransactionsAggregate implements GenerateReturns{
	public HashMap <String,TransactionsAggregate> securitiesList;
	public HashMap <String,Double> returns;
	private String tickerSymbol;
	private double totalPurchaseAmount;
	private double totalPurchaseAmountTotalShares;
	private double totalPurchaseAmountSoldShares;
	public HashMap<String,SecurityDividendInfo> dividendDictionary;
	public double capitalGainsRate;
	public double incomeTaxRate;
	private HashMap <String, Double> taxCredit;
	public boolean connectionFail = false;
	public Context context;
	private HashMap<String,HashMap<LocalDate,Double>>priceData;

	public Portfolio() {
		this.securitiesList = new HashMap<String,TransactionsAggregate>();
		this.returns=new HashMap<String,Double>();
		this.tickerSymbol="PORTFOLIO";
		this.dividendDictionary= new HashMap<String, SecurityDividendInfo>();
		this.priceData = new HashMap<String, HashMap<LocalDate, Double>>();
	}
	
	public void setCapitalGainsRate(double taxRate) {
		this.capitalGainsRate = taxRate;
	}
	
	public void setIncomeTaxRate(double taxRate) {
		this.incomeTaxRate = taxRate;
	}
	
	public void setContext(Context context) {
		this.context = context;
	}
	
	private void updatePriceData(String tickerSymbol, LocalDate initialDate) {
		if (!priceData.containsKey(tickerSymbol) && !priceData.containsKey(initialDate) && !priceData.containsKey(LocalDate.now())) {
			priceData.remove(tickerSymbol);
			priceData.put(tickerSymbol, new PortfolioCalculator().getHistoricalPrice(tickerSymbol, LocalDate.now(), initialDate, "d"));
		}
	}
	
	public double getPrice(String tickerSymbol, LocalDate date) {
		Double price;
		price = priceData.get(tickerSymbol).get(date);
		if (price == null) {
			ReturnsDatabaseConnector databaseConnector = new ReturnsDatabaseConnector(context);
			price = databaseConnector.getSingleReturn(date.minusDays(1).toString(),tickerSymbol, "Price");
		}
		return price;
	}
	
	public void calculateReturns(LocalDate date) {
		this.securitiesList.get("MARKET").calculateReturns(this.securitiesList.get("MARKET"), date);
		this.aggregatePortfolioReturns(this.securitiesList,"MARKET",this.securitiesList.get("MARKET"), date);
		this.calculateAlphas(this.securitiesList.get("MARKET"));
	}

	public void createPurchaseTransaction(int id,String tickerSymbol, LocalDate purchaseDate, double pricePerShare, double transactionCost, int numOfShares, LocalDate minDate) {
		Transaction newPurchaseTransaction = new Transaction(this,id, tickerSymbol,purchaseDate,pricePerShare,transactionCost,numOfShares);
		Transaction newMarketTransaction = new Transaction(this,id,purchaseDate,transactionCost,newPurchaseTransaction.totalPurchaseAmount);
		Transaction[] newTransactionsList = new Transaction[]{newPurchaseTransaction,newMarketTransaction};
		for (Transaction newTransaction: newTransactionsList) {
			if (this.securitiesList.containsKey(newTransaction.tickerSymbol)) {
				this.securitiesList.get(newTransaction.tickerSymbol).addNewTransaction(newTransaction);
			} else {
				this.updatePriceData(newTransaction.tickerSymbol, minDate);
				TransactionsAggregate newSecurity = new TransactionsAggregate(newTransaction.tickerSymbol, this);
				newSecurity.addNewTransaction(newTransaction);
				this.securitiesList.put(newTransaction.tickerSymbol,newSecurity);
			}
			this.adjustDividendDictionary(newTransaction.tickerSymbol, purchaseDate);
			this.securitiesList.get(newTransaction.tickerSymbol).aggregateData();
		}
	}

	public void createSellTransaction(int id, String tickerSymbol, int numOfShares, LocalDate sellDate, double transactionCost, double pricePerShare) { 
		this.securitiesList.get(tickerSymbol).sellStock(this,id,numOfShares, sellDate,transactionCost, pricePerShare);
		
		TransactionsAggregate marketObject = this.securitiesList.get("MARKET");
		
		double marketPricePerShare; 
		if (LocalDate.now().equals(sellDate)) {
			marketPricePerShare = new PortfolioCalculator().getCurrentPrice(-1.00, "MARKET");
		} else {
			marketPricePerShare = new PortfolioCalculator().getHistoricalPrice("MARKET", sellDate, sellDate, "d").get(sellDate);
		}

		int marketNumOfShares = Math.min((int)(numOfShares/this.securitiesList.get(tickerSymbol).getMarketSharesRatio(marketObject)),marketObject.numOfShares);
		
		this.securitiesList.get("MARKET").sellStock(this,id,marketNumOfShares, sellDate,transactionCost, marketPricePerShare);
	}

	public void aggregateData() {
		this.totalPurchaseAmount=0.00;
		this.totalPurchaseAmountTotalShares=0.00;
		this.totalPurchaseAmountSoldShares=0.00;
		for (String security:this.securitiesList.keySet()) {
			if (!this.securitiesList.get(security).tickerSymbol.equals("MARKET")) {
				this.totalPurchaseAmount+=this.securitiesList.get(security).totalPurchaseAmount;
				this.totalPurchaseAmountTotalShares+=this.securitiesList.get(security).totalPurchaseAmountTotalShares;
				this.totalPurchaseAmountSoldShares+=this.securitiesList.get(security).totalPurchaseAmountSoldShares;
			}
		}
	}

	private void aggregatePortfolioReturns(HashMap <String,TransactionsAggregate> transactionsList, String exception, TransactionsAggregate marketReturnObject, LocalDate date) {
		String[] returnList = new String[]{"Market Current", "Market Sold", "Dividend Current", "Dividend Sold"};
		this.taxCredit = new HashMap<String, Double>();
		this.taxCredit.put("Current",0.00);
		this.taxCredit.put("Sold",0.00);
		this.taxCredit.put("Total",0.00);
		for (String item: returnList) {
			this.returns.put("Gross Pre-Tax " + item + " Holdings",0.00);
		}
		for (String transaction: transactionsList.keySet()) {
			if (!transactionsList.get(transaction).tickerSymbol.equals(exception)) {
				transactionsList.get(transaction).calculateReturns(marketReturnObject, date);
				for (String item: taxCredit.keySet()) {
					double addTaxCredit = transactionsList.get(transaction).taxCredit.get(item)+this.taxCredit.get(item);
					this.taxCredit.put(item,addTaxCredit);
				}
				for (String item: returnList) {
					double addReturn = transactionsList.get(transaction).returns.get("Gross Pre-Tax " + item + " Holdings") + this.returns.get("Gross Pre-Tax " + item+ " Holdings");
					this.returns.put("Gross Pre-Tax " + item + " Holdings",addReturn);
				}
			}		
		}
		this.aggregateData();
		for (String item: returnList) {
			this.returns.put("Gross Post-Tax " + item + " Holdings", this.returns.get("Gross Pre-Tax "+ item+ " Holdings")-this.aggregatePortfolioTaxBill(transactionsList,item,exception));
			double base;
			if (item.contains("Current")) {
				base = this.totalPurchaseAmount;
			} else {
				base = this.totalPurchaseAmountSoldShares;
			}	
			this.returns.put("Percent Pre-Tax " + item + " Holdings",PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Pre-Tax " + item + " Holdings"),base));
			this.returns.put("Percent Post-Tax " + item + " Holdings",PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Post-Tax " + item + " Holdings"),base));
		}
		this.taxCredit.put("Market",this.taxCredit.get("Current")+this.taxCredit.get("Sold"));
		this.addPortfolioReturns("Market Current Holdings","Dividend Current Holdings",this.totalPurchaseAmount,"Current Holdings");
		this.addPortfolioReturns("Market Sold Holdings","Dividend Sold Holdings",this.totalPurchaseAmountSoldShares,"Sold Holdings");
		this.addPortfolioReturns("Market Current Holdings","Market Sold Holdings",this.totalPurchaseAmountTotalShares,"Market");
		this.addPortfolioReturns("Dividend Current Holdings","Dividend Sold Holdings",this.totalPurchaseAmountTotalShares,"Dividend");		
		this.addPortfolioReturns("Total Market","Total Dividend",this.totalPurchaseAmountTotalShares,"Total");	
	}

	private double aggregatePortfolioTaxBill(HashMap <String, TransactionsAggregate> transactionList ,String returnType,String exception) { //need to figure out what return list to input
		double taxBill = 0.00;
		for (String transaction: transactionList.keySet()) {
			if (!transactionList.get(transaction).tickerSymbol.equals(exception)) {
				if (transactionList.get(transaction).returns.get("Gross Pre-Tax " + returnType + " Holdings")>0) {
					taxBill+=(transactionList.get(transaction).returns.get("Gross Pre-Tax " + returnType + " Holdings") - transactionList.get(transaction).returns.get("Gross Post-Tax " + returnType + " Holdings"));
				}
			}
		}
		double finalTaxBill;
		if (returnType.contains("Market")) {
			finalTaxBill=this.usePortfolioTaxCredit(taxBill,returnType.substring(returnType.indexOf(" ")+1,returnType.length()),true);
		} else { 
			finalTaxBill=taxBill;
		}
		return finalTaxBill;
	}

	private void adjustDividendDictionary(String tickerSymbol,LocalDate purchaseDate) {
		if (!dividendDictionary.containsKey(tickerSymbol)) {
			dividendDictionary.put(tickerSymbol,new SecurityDividendInfo(tickerSymbol));
			dividendDictionary.get(tickerSymbol).downloadData(purchaseDate);
		} else {
			ArrayList<LocalDate> dateList = dividendDictionary.get(tickerSymbol).orderedDateList;
			if (dateList.size() != 0 && purchaseDate.compareTo(dateList.get(0))<1) {
				dividendDictionary.get(tickerSymbol).downloadData(purchaseDate);
			}
		}
	}

	private double usePortfolioTaxCredit (double taxBill, String holdingType, boolean aggregate) {
		this.taxCredit.put("Total",this.taxCredit.get("Current")+this.taxCredit.get("Sold"));
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
	
	private double calculatePortfolioTaxBill(double returnOnePre,double returnTwoPre, double returnOnePost, double returnTwoPost, String returnName) {
		double taxBill=(returnOnePre-returnOnePost)+(returnTwoPre-returnTwoPost);
		String holdingType;
		if (returnName.contains("Current")||returnName.contains("Sold")) {
			holdingType = returnName.substring(0,returnName.indexOf(" "));
		} else if (returnName.contains("Market")||returnName.contains("Total")) {
			holdingType = "Market";
		} else {
			holdingType = "Dividend";
		}
		taxBill = this.usePortfolioTaxCredit(taxBill,holdingType,false);
		return taxBill;
	}

	private void addPortfolioReturns(String returnOne, String returnTwo, double totalAmount, String returnName) {
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
				taxBill=this.calculatePortfolioTaxBill(returnOnePre,returnTwoPre,returnOnePost,returnTwoPost,returnName);
			} else {
				taxBill = 0.00;
			}
			this.returns.put(nameGross,returnOnePre+returnTwoPre-taxBill);
			this.returns.put(namePercent,PortfolioCalculator.calculatePercentReturn(this.returns.get(nameGross),totalAmount));
		}
	}

	private void calculateAlphas(TransactionsAggregate marketReturnObject) {
		String[] returnList = new String[]{"Total Gross Pre-Tax","Total Gross Post-Tax","Total Percent Pre-Tax","Total Percent Post-Tax"};
		for (String item: returnList) {
			this.returns.put(item + " Alpha", this.returns.get(item)-marketReturnObject.returns.get(item));
		}
	}

	public String getName() {
		return this.tickerSymbol;
	}

	public HashMap<String,Double> getReturns() {
		return this.returns;
	}
	
	public HashMap<String,? extends Transaction> getTransactionList() {
		return this.securitiesList;
	}

	public HashMap<String,HashMap<String,Double>> getReturnsDatabase() {
		HashMap<String,HashMap<String,Double>> returnsDatabase =  new HashMap<String,HashMap<String,Double>>();
		returnsDatabase.put(generateReturnDatabaseId(),this.getAllReturns());
		for (String transactionAggregate: this.getTransactionList().keySet()) {
			for (String transaction: this.getTransactionList().get(transactionAggregate).getTransactionList().keySet()) {
				GenerateReturns transactionItem = this.getTransactionList().get(transactionAggregate).getTransactionList().get(transaction);
				returnsDatabase.put(transactionItem.generateReturnDatabaseId(), transactionItem.getAllReturns());
			}
			GenerateReturns transactionAggregateItem = this.getTransactionList().get(transactionAggregate);
			returnsDatabase.put(transactionAggregateItem.generateReturnDatabaseId(), transactionAggregateItem.getAllReturns());
		}
		return returnsDatabase;
	}
	
	public HashMap<String, Double> getAllReturns() {
		HashMap<String, Double> allReturns = new HashMap<String, Double>();
		allReturns.put("Price", 100.00);
		allReturns.putAll(this.returns);
		return allReturns;
	}
	
	public String getPurchaseDate() {
		return this.securitiesList.get("MARKET").getPurchaseDate();
	}
	
	public ArrayList<String> getOrderedTransactionList() {
		ArrayList<String> orderedList = new ArrayList<String>();
		for (String transaction: getTransactionList().keySet()) {
			if (!transaction.equals("MARKET")) {
				orderedList.add(transaction);
			}
		}
		Collections.sort(orderedList);
		return orderedList;
	}
	
	public String generateReturnDatabaseId() {
		return this.tickerSymbol;
	}
	
	public ArrayList<HashMap<String,String>> getTransactionInfo() {
		ArrayList<HashMap<String,String>> transactionInfoItems = new ArrayList<HashMap<String,String>>();
		String[] infoItemsList = new String[]{"Purchase Amount",String.format("$%,.2f", this.totalPurchaseAmountTotalShares), "Transaction Costs", String.format("$%,.2f", this.securitiesList.get("MARKET").transactionCost),
				"Buy Transactions", String.format("%d", this.securitiesList.get("MARKET").transactionsList.size()),"Sell Transactions", String.format("%d", this.securitiesList.get("MARKET").sellTransactionsList.size())};
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
		if (!this.tickerSymbol.equals("MARKET")) {
			HashMap<String,String> returnItemsData2 = new HashMap<String,String>();
			returnItemsData2.put("Label", "Alpha");
			returnItemsData2.put("Gross Return", String.format("%,.0f",this.returns.get(String.format("Total Gross %s Alpha", preOrPost))));
			returnItemsData2.put("Percent Return", String.format("%,.1f",this.returns.get(String.format("Total Percent %s Alpha", preOrPost))));
			returnInfoItems.add(returnItemsData2);
		}
		return returnInfoItems;
	}
	
	public String getCurrentPrice() {
		return "NONE";
	}
}