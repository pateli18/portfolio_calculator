package com.ffm.portfoliocalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.joda.time.LocalDate;
import org.joda.time.Days;

public class Transaction implements GenerateReturns {

	public String tickerSymbol;
	public LocalDate purchaseDate;
	private LocalDate sellDate;
	public double pricePerShare;
	private double currentPrice;
	public double transactionCost;
	public int numOfShares;
	public int sharesAvailable; //refers to shares that have not yet been sold
	public double totalPurchaseAmount; //purchase amount for shares that have not been sold
	public double totalPurchaseAmountTotalShares;//purchase amount for both sold and un-sold shares
	public double totalPurchaseAmountSoldShares;//purchase amount for sold shares
	private double totalSellAmount;//sell amount for un-sold shares
	private double incomeTaxRate;
	private double capitalGainsRate;
	private double taxRate;//rate that can be set equal to incomeTax or CapitalGains rate
	public HashMap <String, Double> returns;//HashMap that holds various return info, with the name of the return as the key
	public HashMap <String, Double> taxCredit;//HashMap that holds tax credits that can then be allocated to various returns
	public HashMap <String,SellingTransaction> sellTransactionsList;//HashMap that holds all of the sell transactions associated with the Transaction
	private Portfolio portfolio;
	private String transactionType;
	public int id;

	//Intializes the class with no parameters for use by extensions of this class
	public Transaction() {}
	
	/* Intializes the Transaction Class for a Non-Market Equivalent Transactions
		@param Portfolio: the Profile of the user creating the transaction, neceessary to apply appropriate tax rate
		@param tickerSumbol: tickerSymbol of the security being purchased
		@param purchaseDate: the date the seecurity was purchased
		@param pricePerShare: price the security was purchased for
		@param transactionCost: amount charged by the brokerage to execute the transaction
		@param numOfShares: the number of shares purchased
	*/
	public Transaction(Portfolio portfolio,int id, String tickerSymbol, LocalDate purchaseDate, double pricePerShare, double transactionCost, int numOfShares) {
		this.tickerSymbol = tickerSymbol;
		this.purchaseDate = purchaseDate;
		this.pricePerShare = pricePerShare;
		this.transactionCost = transactionCost;
		this.numOfShares = numOfShares;
		this.sharesAvailable = this.numOfShares;
		this.totalPurchaseAmount = this.numOfShares*this.pricePerShare+this.transactionCost;
		this.totalPurchaseAmountTotalShares=this.numOfShares*this.pricePerShare+this.transactionCost;
		this.totalPurchaseAmountSoldShares = 0.00;
		this.incomeTaxRate = portfolio.incomeTaxRate;
		this.capitalGainsRate = portfolio.capitalGainsRate;
		this.portfolio = portfolio;
		this.sellTransactionsList = new HashMap<String,SellingTransaction>();
		this.transactionType = "Buy";
		this.id=id;
		
		//This initializes the returns hash table
		this.returns = new HashMap<String,Double>();
		this.returns.put("Gross Pre-Tax Market Sold Holdings",0.00);
		this.returns.put("Gross Post-Tax Market Sold Holdings",0.00);
		this.returns.put("Percent Pre-Tax Market Sold Holdings",0.00);
		this.returns.put("Percent Post-Tax Market Sold Holdings",0.00);
		this.returns.put("Gross Pre-Tax Dividend Sold Holdings",0.00);
		this.returns.put("Gross Post-Tax Dividend Sold Holdings",0.00);
		this.returns.put("Percent Pre-Tax Dividend Sold Holdings",0.00);
		this.returns.put("Percent Post-Tax Dividend Sold Holdings",0.00);
		this.returns.put("Gross Pre-Tax Total Sold Holdings",0.00);
		this.returns.put("Gross Post-Tax Total Sold Holdings",0.00);
		this.returns.put("Percent Pre-Tax Total Sold Holdings",0.00);
		this.returns.put("Percent Post-Tax Total Sold Holdings",0.00);

		//This initializes the tax credit hash table
		this.taxCredit = new HashMap<String,Double>();
		this.taxCredit.put("Current",0.00);
		this.taxCredit.put("Sold",0.00);
		this.taxCredit.put("Total",0.00);
	}

	/* Intializes the Transaction Class for a Market Equivalent Transactions, taking information from the user transaction to create a market-equivalent
		@param Portfolio: the Profile of the user creating the transaction, neceessary to apply appropriate tax rate
		@param purchaseDate: purchase date of the non-market equivalent transaction, used to find the market price on the corresponding day
		@param transactionCost: amount charged by the brokerage to execute the transaction (equal to the non-market equivalent transaction)
		@param totalPurchaseAmount: the total purchase amount of the non-equivalent transaction (numOfShares * pricePerShare + transactionCost)
	*/
	public Transaction(Portfolio portfolio, int id , LocalDate purchaseDate, double transactionCost, double totalPurchaseAmount) {
		this.tickerSymbol="MARKET";
		this.purchaseDate=purchaseDate;
		this.transactionCost=transactionCost;
		
		if (!LocalDate.now().equals(purchaseDate)) {	
			this.pricePerShare=new PortfolioCalculator().getHistoricalPrice("SPY",this.purchaseDate,this.purchaseDate,"d").get(this.purchaseDate); //retrieves the market price on the purchaseDate
		} else {
			this.pricePerShare=new PortfolioCalculator().getCurrentPrice(-1.00, "SPY");
		}
		
		if (this.pricePerShare<0) {
			portfolio.connectionFail = true;
			this.pricePerShare = 0.00;
		} else {
			portfolio.connectionFail = false;
		}
		
		this.numOfShares=(int)((totalPurchaseAmount-this.transactionCost)/this.pricePerShare); //calculates the number of shares purchased, converted to integer
		this.sharesAvailable=this.numOfShares;
		this.totalPurchaseAmount=this.pricePerShare*this.numOfShares+this.transactionCost;
		this.totalPurchaseAmountTotalShares=this.pricePerShare*this.numOfShares+this.transactionCost;
		this.incomeTaxRate=portfolio.incomeTaxRate;
		this.capitalGainsRate=portfolio.capitalGainsRate;
		this.totalPurchaseAmountSoldShares=0.00;
		this.sellTransactionsList = new HashMap<String,SellingTransaction>();
		this.portfolio = portfolio;
		this.id=id;
		this.transactionType = "Buy";

		//This initializes the returns hash table
		this.returns = new HashMap<String,Double>();
		this.returns.put("Gross Pre-Tax Market Sold Holdings",0.00);
		this.returns.put("Gross Post-Tax Market Sold Holdings",0.00);
		this.returns.put("Percent Pre-Tax Market Sold Holdings",0.00);
		this.returns.put("Percent Post-Tax Market Sold Holdings",0.00);
		this.returns.put("Gross Pre-Tax Dividend Sold Holdings",0.00);
		this.returns.put("Gross Post-Tax Dividend Sold Holdings",0.00);
		this.returns.put("Percent Pre-Tax Dividend Sold Holdings",0.00);
		this.returns.put("Percent Post-Tax Dividend Sold Holdings",0.00);
		this.returns.put("Gross Pre-Tax Total Sold Holdings",0.00);
		this.returns.put("Gross Post-Tax Total Sold Holdings",0.00);
		this.returns.put("Percent Pre-Tax Total Sold Holdings",0.00);
		this.returns.put("Percent Post-Tax Total Sold Holdings",0.00);

		//This initializes the tax credit hash table
		this.taxCredit = new HashMap<String,Double>();
		this.taxCredit.put("Current",0.00);
		this.taxCredit.put("Sold",0.00);
		this.taxCredit.put("Total",0.00);
	}

	/* calculates the tax rate to use for the post-tax return for a single transaction
		@param holdingType: Current or Sold
	*/
	private void correctTaxRate(String holdingType, LocalDate date) {
		this.sellDate = date; //sets sell date to current date/time, assuming that any currentshares would be sold on the current date to calculate the appropriate return
		int daysHeld = Days.daysBetween(purchaseDate,sellDate).getDays(); //calculates the number of days the security has been held
		if (this.returns.get("Gross Pre-Tax Market " + holdingType + " Holdings")<0) { //if the pre-tax return was negative, than the taxRate is automaticcaly zero
			this.taxRate = 0;
			this.taxCredit.put(holdingType, this.returns.get("Gross Pre-Tax Market " + holdingType + " Holdings")); //increase the tax credit available by the negative pre-tax return
		} else if (daysHeld>364) { //if held for more than one year, the capital gains rate is applied, otherwise it is the normal income tax rate
			this.taxRate=this.capitalGainsRate;
		} else {
			this.taxRate=this.incomeTaxRate;
		} 
	}

	/* aggregates the tax bill from any sell transactions associated with the purchase transaction by adding up the taxes from each sell transaction and subtracting any tax credits
		@param transactionList: HashMap containing all sell transactions
		@param returnType: Market or Dividend, Current or Sold (i.e. Market Current)
		@param exception: NEEDED?
		@return the final aggregate tax bill after all of the calculations finish
	*/
	private double aggregateTaxBill(HashMap <String, SellingTransaction> transactionList ,String returnType,String exception) {
		double taxBill = 0.00; //initialize tax bill used in aggregation
		for (String transaction: transactionList.keySet()) { //cycle through all transactions in the transaction list
			if (!transactionList.get(transaction).tickerSymbol.equals(exception)) { //don't think this is needed
				if (transactionList.get(transaction).returns.get("Gross Pre-Tax " + returnType + " Holdings")>0) { //checks if the pre-tax return is greater than 0 (if not, than there is no tax applied to the transaction)
					taxBill+=(transactionList.get(transaction).returns.get("Gross Pre-Tax " + returnType + " Holdings") - transactionList.get(transaction).returns.get("Gross Post-Tax " + returnType + " Holdings")); //calculates the sell transaction tax bill by subtracting post from pre, adds it to the aggregate bill
				}
			}
		}
		double finalTaxBill; //intializes final tax bill which will account for any necessary credits
		if (returnType.contains("Market")) { //if return type is not Market (i.e. Dividend), there will be no negative returns and therefore no credits
			finalTaxBill=this.useTaxCredit(taxBill,returnType.substring(returnType.indexOf(" ")+1,returnType.length()),true); //uses tax credit on tax bill, passing through the Current or Sold part of the return type
		} else { 
			finalTaxBill=taxBill;
		}
		return finalTaxBill;
	}

	private void calculateDividendReturns(int shares,String returnType, double totalPurchaseAmount, LocalDate date) { //need to figure out how to access dividend dictionary
		LocalDate lastDate = new LocalDate();
		this.sellDate = date;
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

	public void getSellInformation(Portfolio portfolio, int id, int numOfShares, int sharesAvailable, LocalDate sellDate, double transactionCost, double pricePerShare) {
		SellingTransaction sellTransaction = new SellingTransaction(portfolio,id,this.tickerSymbol,numOfShares,sharesAvailable,sellDate,transactionCost,pricePerShare,purchaseDate);
		sellTransaction.totalPurchaseAmount=sharesAvailable*this.pricePerShare+((double)sharesAvailable/this.numOfShares*this.transactionCost);
		sellTransaction.pricePerShare=pricePerShare;
		this.sellDate=sellDate;
		sellTransaction.calculateReturns();
		this.sellTransactionsList.put(sellDate.toString(),sellTransaction);
		this.sellDate= new LocalDate();
		this.sharesAvailable-=sharesAvailable;
		this.aggregateSellReturns();
		this.totalPurchaseAmount=this.sharesAvailable*this.pricePerShare+((double)this.sharesAvailable/this.numOfShares*this.transactionCost);
	}

	private void aggregateSellReturns() {
		String[] returnList = new String[] {"Market","Dividend"};
		for (String item: returnList) {
			this.returns.put("Gross Pre-Tax " + item + " Sold Holdings",0.00);
		}
		for (String transaction: this.sellTransactionsList.keySet()) {
			for (String item: returnList) {
				double aggregateSellTransactionReturns = this.sellTransactionsList.get(transaction).returns.get("Gross Pre-Tax "+ item + " Sold Holdings") + this.returns.get("Gross Pre-Tax " + item + " Sold Holdings");
				this.returns.put("Gross Pre-Tax " + item + " Sold Holdings",aggregateSellTransactionReturns);
			}
			double aggregateSoldTaxCredit = this.sellTransactionsList.get(transaction).taxCredit.get("Sold")+this.taxCredit.get("Sold");
			this.taxCredit.put("Sold", aggregateSoldTaxCredit);
		}
		this.totalPurchaseAmountSoldShares = (this.numOfShares-this.sharesAvailable)*this.pricePerShare+((double)(this.numOfShares-this.sharesAvailable)/this.numOfShares*this.transactionCost);
		for (String item: returnList) {
			this.returns.put("Gross Post-Tax " + item + " Sold Holdings", this.returns.get("Gross Pre-Tax " + item + " Sold Holdings")-this.aggregateTaxBill(this.sellTransactionsList,item + " Sold", "No Exception"));
			this.returns.put("Percent Pre-Tax " + item + " Sold Holdings", PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Pre-Tax " + item + " Sold Holdings"),this.totalPurchaseAmountSoldShares));
			this.returns.put("Percent Post-Tax " + item + " Sold Holdings", PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Post-Tax " + item + " Sold Holdings"),this.totalPurchaseAmountSoldShares));
		}
		this.addReturns("Market Sold Holdings", "Dividend Sold Holdings",this.totalPurchaseAmountSoldShares,"Sold Holdings");
	}

	private void getCurrentPrice(double stockPrice) {
		this.currentPrice=stockPrice;
	}

	private void calculatePreTaxReturn(String holdingType, LocalDate date) {
		double transactionCost;
		if (holdingType.equals("Current")&&this.sharesAvailable>0) {
			transactionCost=this.transactionCost;
		} else {
			transactionCost=(double)this.sharesAvailable/this.numOfShares*this.transactionCost;
		}
		this.totalSellAmount=this.sharesAvailable*this.currentPrice - transactionCost;
		this.returns.put("Gross Pre-Tax Market "+ holdingType + " Holdings",this.totalSellAmount-this.totalPurchaseAmount);
		this.returns.put("Percent Pre-Tax Market " + holdingType + " Holdings",PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Pre-Tax Market " + holdingType+ " Holdings"),this.totalPurchaseAmount));
		
		if (holdingType.equals("Current")) {
			this.calculateDividendReturns(this.sharesAvailable,holdingType,this.totalPurchaseAmount, date);
		} else {
			this.calculateDividendReturns(this.sharesAvailable,holdingType,this.totalPurchaseAmountSoldShares, date);
		}

		this.correctTaxRate(holdingType, date);
	}

	private void calculatePostTaxReturn(String holdingType) {
		this.returns.put("Gross Post-Tax Market " + holdingType + " Holdings",this.returns.get("Gross Pre-Tax Market " + holdingType + " Holdings")*(1-this.taxRate));
		this.returns.put("Percent Post-Tax Market " + holdingType + " Holdings", PortfolioCalculator.calculatePercentReturn(this.returns.get("Gross Post-Tax Market " + holdingType + " Holdings"),this.totalPurchaseAmount));
		this.addReturns("Market " + holdingType + " Holdings","Dividend "+holdingType+" Holdings",this.totalPurchaseAmount,holdingType+ " Holdings");
	}

	private void calculateTotalReturns(){
		this.addReturns("Market Current Holdings","Market Sold Holdings",this.totalPurchaseAmountTotalShares,"Market");
		this.addReturns("Dividend Current Holdings","Dividend Sold Holdings",this.totalPurchaseAmountTotalShares,"Dividend");
		this.addReturns("Total Market","Total Dividend",this.totalPurchaseAmountTotalShares,"Total");		
	}

	public void calculateReturns(double stockPrice, TransactionsAggregate marketReturnObject, LocalDate date){
		this.getCurrentPrice(stockPrice);
		this.calculatePreTaxReturn("Current", date);
		this.calculatePostTaxReturn("Current");
		this.calculateTotalReturns();
		this.calculateAlphas(marketReturnObject);
	}

	private void calculateAlphas(TransactionsAggregate marketReturnObject) {
		String[] returnList = new String[]{"Total Gross Pre-Tax","Total Gross Post-Tax","Total Percent Pre-Tax","Total Percent Post-Tax"};
		if (this.tickerSymbol.equals("MARKET")) {
			for (String item: returnList) {
				this.returns.put(item + " Alpha", 0.00);
			}
		} else {
			for (String item: returnList) {
				this.returns.put(item + " Alpha", this.returns.get(item)-marketReturnObject.getAlphaReturn(String.format("%d",this.id), item));
			}
		}
	}

	public String getName() {
		return this.purchaseDate.toString();
	}

	public HashMap<String,Double> getReturns() {
		return this.returns;
	}
	
	public HashMap<String,? extends Transaction> getTransactionList() {
		return this.sellTransactionsList;
	}
	
	public String[] getTransactionSummary() {
		String[] infoButtons = new String[]{this.tickerSymbol,this.purchaseDate.toString(),String.format("%d",this.id)};
		return infoButtons;	
	}
	
	
	public ArrayList<HashMap<String,String>> getTransactionInfo() {
		ArrayList<HashMap<String,String>> transactionInfoItems = new ArrayList<HashMap<String,String>>();
		String[] infoItemsList = new String[]{"Transaction Type", this.transactionType, "Purchase Price",String.format("$%,.2f", this.pricePerShare),"Current Price", String.format("$%,.2f", this.currentPrice),
				"Unsold Shares",String.format("%d", this.sharesAvailable), "Sold Shares",String.format("%d", this.numOfShares - this.sharesAvailable),
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
		HashMap<String,String> returnItemsData1 = new HashMap<String,String>();
		returnItemsData1.put("Label", "Total");
		returnItemsData1.put("Gross Return", String.format("%,.0f",this.returns.get(String.format("Total Gross %s", preOrPost))));
		returnItemsData1.put("Percent Return", String.format("%,.1f",this.returns.get(String.format("Total Percent %s", preOrPost))));
		returnInfoItems.add(returnItemsData1);
		if (!this.tickerSymbol.equals("MARKET")) {
			HashMap<String,String> returnItemsData2 = new HashMap<String,String>();
			returnItemsData2.put("Label", "Alpha");
			returnItemsData2.put("Gross Return", String.format("%,.0f",this.returns.get(String.format("Total Gross %s Alpha", preOrPost))));
			returnItemsData2.put("Percent Return", String.format("%,.1f",this.returns.get(String.format("Total Percent %s Alpha", preOrPost))));
			returnInfoItems.add(returnItemsData2);
		}
		return returnInfoItems;
	}
		
	public ArrayList<String> getOrderedTransactionList() {
		ArrayList<String> orderedList = new ArrayList<String>();
		for (String transaction: getTransactionList().keySet()) {
			orderedList.add(transaction);
		}
		Collections.sort(orderedList);
		Collections.reverse(orderedList);
		return orderedList;
	}
	
	public String generateReturnDatabaseId() {
		return this.tickerSymbol + this.id;
	}
	
	public HashMap<String, Double> getAllReturns() {
		HashMap<String, Double> allReturns = new HashMap<String, Double>();
		allReturns.put("Price", this.currentPrice);
		allReturns.putAll(this.returns);
		return allReturns;
	}
	
	public String getPurchaseDate() {
		return this.purchaseDate.toString();
	}
	
	public boolean isSell() {
		return false;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getCurrentPrice() {
		return String.format("%.2f",this.currentPrice);
	}
	
	public String getTransactionType() {
		return "BUY";
	}
}