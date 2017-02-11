package com.ffm.portfoliocalculator;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import org.joda.time.LocalDate;
import java.util.ArrayList;

public class PortfolioCalculator {
	
	public static double calculatePercentReturn(double numerator, double denominator) {
		if (denominator==0.00) {
			return 0;
		} else {
			return (numerator/denominator*100);
		}
	}

	public double getCurrentPrice(double initialPrice,String tickerSymbol){
	    if (tickerSymbol.equals("MARKET")) {tickerSymbol="SPY";}
		String url ="http://finance.yahoo.com/d/quotes.csv?s="+tickerSymbol+"&f=l1";
		try {
			URL yahoofinance = new URL(url);
		    URLConnection yc = yahoofinance.openConnection();
		    BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			double inputLine = Double.parseDouble(in.readLine());
			in.close();
			return inputLine;
		} catch (IOException e) {
			return initialPrice;
		}
	}

	public HashMap<LocalDate,Double> getHistoricalPrice(String tickerSymbol, LocalDate endDate, LocalDate startDate, String dataType){
	    HashMap<LocalDate,Double> priceData = new HashMap<LocalDate,Double>();
	    String url = generateHistoricalUrl(tickerSymbol, endDate, startDate, dataType);
	    int priceIndex;
	    if (dataType.equals("d")) {
	    	priceIndex = 4;
	    } else {
	    	priceIndex = 1;
	    }
	    try {
			URL yahoofinance = new URL(url);
		    URLConnection yc = yahoofinance.openConnection();
		    BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
	    	in.readLine(); //Read the firstLine. This is the header.
		    String inputLine; 
		    while((inputLine = in.readLine()) != null) //While not in the header
		    	{
		    			String [] dataLine = inputLine.split("\\,");
		    			String dateText = dataLine[0];
		    			LocalDate date= new LocalDate(Integer.parseInt(dateText.substring(0,4)),Integer.parseInt(dateText.substring(5,7)),Integer.parseInt(dateText.substring(8,10)));
		    			double price = Double.parseDouble(dataLine[priceIndex]);
		    			priceData.put(date,price);
		    	}
		    in.close();
		    return priceData;
	    }catch (MalformedURLException e1) {
        }catch(IOException e1){
        }
	    HashMap<LocalDate, Double> fail = new HashMap<LocalDate, Double>();
	    fail.put(endDate, -1.00);
	    return fail;
	}
	
	private String generateHistoricalUrl(String tickerSymbol, LocalDate endDate, LocalDate startDate, String dataType) {
		if (tickerSymbol.equals("MARKET")) {tickerSymbol="SPY";}
		String endMonth=String.format("%d",(Integer.parseInt(endDate.toString().substring(5,7))-1));
	    String endDay=endDate.toString().substring(8,10);
	    String endYear=endDate.toString().substring(0,4);
	    String startMonth=String.format("%d",(Integer.parseInt(startDate.toString().substring(5,7))-1));
	    String startDay=startDate.toString().substring(8,10);
	    String startYear=startDate.toString().substring(0,4);
	    String url = "http://ichart.finance.yahoo.com/table.csv?s="+tickerSymbol+"&d="+endMonth+"&e="+endDay+"&f="+endYear+"&g="+dataType+"&a="+startMonth+"&b="+startDay+"&c="+startYear+"&ignore=.csv";
	    return url;
	}

	public static ArrayList<LocalDate> sortDateList(ArrayList<LocalDate> unOrderedList) {
		ArrayList<LocalDate> temporaryList = new ArrayList<LocalDate>();
		LocalDate minDate = LocalDate.now();
		while (temporaryList.size()<unOrderedList.size())	{
			for (LocalDate date: unOrderedList) {
				if (date.compareTo(minDate)<0 && !temporaryList.contains(date)) {
					minDate = date;
				}
			}
			temporaryList.add(minDate);
			minDate = LocalDate.now();
		}
		for (int i =0; i<temporaryList.size();i++) {
			unOrderedList.set(i, temporaryList.get(i));
		}
		return unOrderedList;
	}
	
}