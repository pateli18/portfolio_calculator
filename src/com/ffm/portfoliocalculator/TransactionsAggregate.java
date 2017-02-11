package com.ffm.portfoliocalculator;

import java.util.HashMap;
import org.joda.time.LocalDate;
import java.util.ArrayList;

public class TransactionsAggregate extends Transaction implements GenerateReturns {

	public String tickerSymbol;
	public double transactionCost;
	public double pricePerShare;
	private double currentPrice = 0;
	public int numOfShares;
	private int totalShares;
	public double totalPurchaseAmount;
	public double totalPurchaseAmountTotalShares;
	public double totalPurchaseAmountSoldShares;
	public HashMap <String, Double> returns;
	public HashMap <String, Double> taxCredit;
	public HashMap <String,Transaction> transactionsList; 
	public HashMap <String,SellingTransaction> sellTransactionsList;
	private ArrayList<LocalDate> orderedTransactionList;
	private String id;
	public double marketSharesRatio;
	private Portfolio portfolio;

	public TransactionsAggregate () {}
	
	public TransactionsAggregate(String tickerSymbol, Portfolio portfolio) {
		this.tickerSymbol=tickerSymbol;
		orderedTransactionList= new ArrayList<LocalDate>();
		this.returns=new HashMap<String,Double>();
		this.taxCredit=new HashMap<String,Double>();
		this.transactionsList= new HashMap<String,Transaction>();
		this.sellTransactionsList= new HashMap<String,SellingTransaction>();
		this.portfolio=portfolio;
	}

	public void addNewTransaction(Transaction singleTransaction) {
		this.transactionsList.put(singleTransaction.purchaseDate.toString(),singleTransaction);
		this.orderedTransactionList.add(singleTransaction.purchaseDate);
	}

	public void aggregateData() {
		this.pricePerShare=0.00;
		this.numOfShares=0;
		this.totalPurchaseAmount=0.00;
		this.totalPurchaseAmountTotalShares=0.00;
		this.totalPurchaseAmountSoldShares=0.00;
		this.totalShares = 0;
		this.id="T.";
		for (String transaction:this.transactionsList.keySet()) {
			Transaction individualTransaction = this.transactionsList.get(transaction);
			this.totalShares+=individualTransaction.numOfShares;
			this.numOfShares+=individualTransaction.sharesAvailable;
			this.totalPurchaseAmount+=individualTransaction.totalPurchaseAmount;
			this.pricePerShare+=(individualTransaction.numOfShares*individualTransaction.pricePerShare);
			this.totalPurchaseAmountTotalShares+=(individualTransaction.numOfShares*individualTransaction.pricePerShare+individualTransaction.transactionCost);
			this.totalPurchaseAmountSoldShares+=individualTransaction.totalPurchaseAmountSoldShares;
			this.id += String.format("%d.", individualTransaction.id);
		}
		this.pricePerShare = this.pricePerShare/this.totalShares;
		
		this.transactionCost = calculateTransactionCost();
		
		this.orderedTransactionList=PortfolioCalculator.sortDateList(this.orderedTransactionList);
	}
	
	private double calculateTransactionCost() {
		double transactionCost = 0.00;
		for (String transaction:this.transactionsList.keySet()) {
			transactionCost+=this.transactionsList.get(transaction).transactionCost;
		}
		for (String transaction:this.sellTransactionsList.keySet()) {
			transactionCost+=this.sellTransactionsList.get(transaction).transactionCost;
		}
		return transactionCost;
	}
	private void aggregateReturns(HashMap <String,Transaction> transactionsList, String exception, double price, TransactionsAggregate marketReturnObject, LocalDate date) {
		this.aggregateData();
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
				transactionsList.get(transaction).calculateReturns(price,marketReturnObject,date);
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
		for (String item: returnList) {
			this.returns.put("Gross Post-Tax " + item + " Holdings", this.returns.get("Gross Pre-Tax "+ item+ " Holdings")-this.aggregateTransactionAggregateTaxBill(transactionsList,item,exception));
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
		this.addAggregateReturns("Market Current Holdings","Dividend Current Holdings",this.totalPurchaseAmount,"Current Holdings");
		this.addAggregateReturns("Market Sold Holdings","Dividend Sold Holdings",this.totalPurchaseAmountSoldShares,"Sold Holdings");
		this.addAggregateReturns("Market Current Holdings","Market Sold Holdings",this.totalPurchaseAmountTotalShares,"Market");
		this.addAggregateReturns("Dividend Current Holdings","Dividend Sold Holdings",this.totalPurchaseAmountTotalShares,"Dividend");		
		this.addAggregateReturns("Total Market","Total Dividend",this.totalPurchaseAmountTotalShares,"Total");	
	}

	public void calculateReturns(TransactionsAggregate marketReturnObject, LocalDate date) {
		if (date.equals(LocalDate.now())) {
			this.currentPrice = new PortfolioCalculator().getCurrentPrice(this.currentPrice, this.tickerSymbol);
		} else {
			this.currentPrice =  portfolio.getPrice(this.tickerSymbol, date);
		}
		this.aggregateReturns(this.transactionsList,"No Exception", this.currentPrice,marketReturnObject, date);
		this.calculateAlphas(marketReturnObject);
	}

	public void sellStock(Portfolio portfolio, int id, int numOfShares, LocalDate sellDate, double transactionCost, double pricePerShare) {
		SellingTransaction sellTransaction = new SellingTransaction(portfolio,id,this.tickerSymbol,numOfShares,numOfShares,sellDate,transactionCost,pricePerShare,sellDate);
		int i = 0;
		int numOfSharesSold = sellTransaction.numOfShares;
		while (numOfSharesSold>0) {
			LocalDate transaction = this.orderedTransactionList.get(i);
			int sharesSold = Math.min(numOfSharesSold,this.transactionsList.get(transaction.toString()).sharesAvailable);
			this.transactionsList.get(transaction.toString()).getSellInformation(portfolio,sellTransaction.id,sellTransaction.numOfShares,sharesSold,sellTransaction.sellDate,sellTransaction.transactionCost,sellTransaction.currentPrice);
			numOfSharesSold-=sharesSold;
			i++;
		}
		sellTransaction.aggregateSellReturns(this.transactionsList);
		this.sellTransactionsList.put(sellTransaction.sellDate.toString(),sellTransaction);
	}
	
	private double aggregateTransactionAggregateTaxBill(HashMap <String, Transaction> transactionList ,String returnType,String exception) { //need to figure out what return list to input
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
			finalTaxBill=this.useAggregateTaxCredit(taxBill,returnType.substring(returnType.indexOf(" ")+1,returnType.length()),true);
		} else { 
			finalTaxBill=taxBill;
		}
		return finalTaxBill;
	}

	private double useAggregateTaxCredit (double taxBill, String holdingType, boolean aggregate) {
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
	
	private void addAggregateReturns(String returnOne, String returnTwo, double totalAmount, String returnName) {
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
				taxBill=this.calculateAggregateTaxBill(returnOnePre,returnTwoPre,returnOnePost,returnTwoPost,returnName);
			} else {
				taxBill = 0.00;
			}
			this.returns.put(nameGross,returnOnePre+returnTwoPre-taxBill);
			this.returns.put(namePercent,PortfolioCalculator.calculatePercentReturn(this.returns.get(nameGross),totalAmount));
		}
	}
	
	private double calculateAggregateTaxBill(double returnOnePre,double returnTwoPre, double returnOnePost, double returnTwoPost, String returnName) {
		double taxBill=(returnOnePre-returnOnePost)+(returnTwoPre-returnTwoPost);
		String holdingType;
		if (returnName.contains("Current")||returnName.contains("Sold")) {
			holdingType = returnName.substring(0,returnName.indexOf(" "));
		} else if (returnName.contains("Market")||returnName.contains("Total")) {
			holdingType = "Market";
		} else {
			holdingType = "Dividend";
		}
		taxBill = this.useAggregateTaxCredit(taxBill,holdingType,false);
		return taxBill;
	}
	
	private void calculateAlphas(TransactionsAggregate marketReturnObject) {
		String[] returnList = new String[]{"Total Gross Pre-Tax","Total Gross Post-Tax","Total Percent Pre-Tax","Total Percent Post-Tax"};
		if (this.tickerSymbol.equals("MARKET")) {
			for (String item: returnList) {
				this.returns.put(item + " Alpha", 0.00);
			}
		} else {
			for (String item: returnList) {
				this.returns.put(item + " Alpha", this.returns.get(item)-marketReturnObject.getAlphaReturn(this.id, item));
			}
		}

	}

	public String getName() {
		return this.tickerSymbol;
	}

	public HashMap<String,Double> getReturns() {
		return this.returns;
	}
	
	public HashMap<String,? extends Transaction> getTransactionList() {
		HashMap<String,Transaction> combinedTransactionList=new HashMap<String,Transaction>();
		combinedTransactionList.putAll(this.transactionsList);
		if (this.sellTransactionsList.size()>0) {combinedTransactionList.putAll(this.sellTransactionsList);}
		return combinedTransactionList;
	}
	
	public String[] getTransactionSummary() {
		String[] infoButtons = new String[]{this.tickerSymbol,"Total"};
		return infoButtons;	
	}
	
	public ArrayList<HashMap<String,String>> getTransactionInfo() {
		ArrayList<HashMap<String,String>> transactionInfoItems = new ArrayList<HashMap<String,String>>();
		String[] infoItemsList = new String[]{"Purchase Price",String.format("$%,.2f", this.pricePerShare),"Current Price", String.format("$%,.2f", this.currentPrice),
				"Unsold Shares",String.format("%d", this.numOfShares), "Sold Shares",String.format("%d", this.totalShares - this.numOfShares),
				"Total Shares",String.format("%d", this.totalShares),"Transaction Costs", String.format("$%,.2f", this.transactionCost), "Buy Transactions", String.format("%d", this.transactionsList.size()),
				"Sell Transactions", String.format("%d", this.sellTransactionsList.size()) };
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
	
	public double getAlphaReturn(String id, String returnName) {
		double returnValue = 0;
		double returnValueGross = 0;
		double returnPurchaseAmount=0;
		if (id.contains("T")) {
			for (String individualTransaction: this.transactionsList.keySet()) {
				Transaction marketReturnObject = this.transactionsList.get(individualTransaction);
				if (id.contains(String.format(".%d.",marketReturnObject.id))) {
					if (returnName.contains("Percent")) {
						returnValueGross += marketReturnObject.returns.get(returnName.replace("Percent", "Gross"));
						returnPurchaseAmount += marketReturnObject.totalPurchaseAmountTotalShares;
						returnValue = PortfolioCalculator.calculatePercentReturn(returnValueGross,returnPurchaseAmount);
					} else {
						returnValue += marketReturnObject.returns.get(returnName);
					}
				}
			}
		} else {
			for (String individualTransaction: this.transactionsList.keySet()) {
				Transaction marketReturnObject = this.transactionsList.get(individualTransaction);
				if (marketReturnObject.id == Integer.parseInt(id)) {
					returnValue = marketReturnObject.returns.get(returnName);
				}
			}
		}
		return returnValue;
	}
	
	public double getMarketSharesRatio(TransactionsAggregate marketAggregateObject) {
		int marketNumOfShares = 0;
		for (String individualTransaction:  marketAggregateObject.transactionsList.keySet()) {
			Transaction marketObject = marketAggregateObject.transactionsList.get(individualTransaction);
			if (this.id.contains(String.format(".%d.",marketObject.id))) {
				marketNumOfShares += marketObject.numOfShares;
			}
		}
		return (double)this.totalShares/marketNumOfShares;
	}
	
	public String generateReturnDatabaseId() {
		return this.tickerSymbol;
	}
	
	public HashMap<String, Double> getAllReturns() {
		HashMap<String, Double> allReturns = new HashMap<String, Double>();
		allReturns.put("Price", this.currentPrice);
		allReturns.putAll(this.returns);
		return allReturns;
	}
	
	public String getPurchaseDate() {
		return this.orderedTransactionList.get(0).toString();
	}
	
	public String getCurrentPrice() {
		return String.format("%.2f",this.currentPrice);
	}
}