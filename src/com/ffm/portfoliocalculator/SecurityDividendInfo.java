package com.ffm.portfoliocalculator;
import java.util.HashMap;
import org.joda.time.LocalDate;
import java.util.ArrayList;

public class SecurityDividendInfo {
	public HashMap<LocalDate,Double> dividendHistoryList;
	public ArrayList<LocalDate> orderedDateList;
	private String tickerSymbol;

	public SecurityDividendInfo(String tickerSymbol) {
		this.dividendHistoryList=new HashMap<LocalDate,Double>();
		this.orderedDateList=new ArrayList<LocalDate>();
		this.tickerSymbol=tickerSymbol;
	}

	public void downloadData (LocalDate startDate) {
		startDate=startDate.minusMonths(1);
		HashMap<LocalDate,Double> dividendList = new PortfolioCalculator().getHistoricalPrice(this.tickerSymbol, LocalDate.now(), startDate, "v");
		for (LocalDate date: dividendList.keySet()) {
			if (!this.dividendHistoryList.containsKey(date)) {
				this.dividendHistoryList.put(date,dividendList.get(date));
				this.orderedDateList.add(date);
			}
		}
		this.orderedDateList=PortfolioCalculator.sortDateList(this.orderedDateList);
	}
}