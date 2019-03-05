/*Problem 1
 * This .java file reads data from the .xz file into data frames and creates two .txt file, one for all the stocks
 * and the other one for stocks that have total volumes greater than 10MM in dollars.
 *  *Please change the path of the data file and the printed file to your local path
 * */

package handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import helper.MeanCalculator;


public class DataHandler {

	//Hashmap links tickers to their total volume in dollars
	private HashMap<String,Double> tickersToTotal;
	//Hashmap links tickers to their price records
	private HashMap<String,ArrayList<Double>> tickersToPrices;
	//Hashmap links tickers to their price changes
	private HashMap<String,ArrayList<Double>> tickersToDeltaPrices;
	//Hashmap links tickers with total volume greater than 10MM to their spreads and total volumes
	private HashMap<String, ArrayList<Double>> subsetTotalSpread;
	//Hashmap links tickers to spreads
	private HashMap<String, Double> tickersToSpread;
	//Mean calculator that helps to keep tracking of ranning average
	private MeanCalculator mc1;
	private MeanCalculator mc2;
	
	
	public DataHandler() {
		this.tickersToTotal=new HashMap<String,Double>();
		this.tickersToPrices=new HashMap<String, ArrayList<Double>>();
		this.tickersToDeltaPrices=new HashMap<String,ArrayList<Double>>();
		this.tickersToSpread= new HashMap<String, Double>(); 
		this.subsetTotalSpread=new HashMap<String,ArrayList<Double>>();
		mc1=new MeanCalculator();
		mc2=new MeanCalculator();
		
	}
	
	public HashMap<String,Double> getTickersToTotal() {
		return this.tickersToTotal;
	}
	
	public HashMap<String,ArrayList<Double>> getTickersToPrices() {
		return this.tickersToPrices;
	}
	
	public HashMap<String,ArrayList<Double>> getTickersToDeltaPrices() {
		return this.tickersToDeltaPrices;
	}
	
	
	public  HashMap<String, Double> getTickersToSpread() {
		return this.tickersToSpread;
	}
	
	public HashMap<String,ArrayList<Double>> getSubset() {
		return this.subsetTotalSpread;
	}
	
	//Read the data into the data frame
	public void readData() {
		String path="/Users/meixisun/Documents/ctm_20171229.txt.xz";//Please change the path
		File f=new File(path);
		try(XZCompressorInputStream xz = new XZCompressorInputStream(new FileInputStream(f));
			    BufferedReader reader = new BufferedReader(new InputStreamReader(xz))) {
				//Skip the header line
			    String line = reader.readLine();
			    
			    while(null!=(line=reader.readLine())) {
			    	
			    	String[] tok=line.split(",");
		        	String time=tok[1];
		        	String ticker="";
		        	
		        	//Check if the suffix exists, and combine it with the root if it exists
		        	if(!tok[4].isEmpty()) {
		        		ticker=tok[3]+" "+tok[4];
		        	}
		        	else {
		        		ticker=tok[3];
		        	}
		        	
		        	double volume=Double.parseDouble(tok[6]);
		        	double price=Double.parseDouble(tok[7]);
		        	String corr=tok[9];
		        	String[] t=time.split(":");
		        	int hour=Integer.parseInt(t[0]);
		        	int minute=Integer.parseInt(t[1]);
		        	boolean include=true;
		        	
		        	//Time filter
		        	if((hour<9) || (hour>=16)) {
		        		include=false;
		        	}
		        	if((hour==9) && (minute<30)) {
		        		include=false;
		        	}
		        	//"00" filter
		        	if(!corr.equals("00")) {
		        		include=false;
		        	}
		        	if(include) {
		        		//Fill out the hashmaps
		        		if(this.tickersToPrices.containsKey(ticker)) {
		        			this.tickersToPrices.get(ticker).add(price);
		        			double newVolume=this.tickersToTotal.get(ticker)+(double)volume*price;
		        			this.tickersToTotal.put(ticker, newVolume);
		        		}
		        		else {
		        			this.tickersToPrices.put(ticker, new ArrayList<Double>());
		        			this.tickersToTotal.put(ticker, (double)volume*price);
		        			this.tickersToPrices.get(ticker).add(price);
		        			
		        		}
		        	}
			    }

			} catch (Exception e) {
			    e.printStackTrace();
			}
		
	}
	
	//Calculate price difference and calculate the difference mean at the same time
	//Also for the first-order serial time difference
	public void deltaPrices() {
		for(String t:this.getTickersToPrices().keySet()) {
			this.getTickersToDeltaPrices().put(t, new ArrayList<Double>());
			//this.getTickersToDeltaLag().put(t, new ArrayList<Double>());
			for(int i=0; i<this.getTickersToPrices().get(t).size()-2; i++) {
				double deltaPt=this.getTickersToPrices().get(t).get(i+1)-this.getTickersToPrices().get(t).get(i);
				double deltaPtLag=this.getTickersToPrices().get(t).get(i+2)-this.getTickersToPrices().get(t).get(i+1);
				this.getTickersToDeltaPrices().get(t).add(deltaPt);
				//this.getTickersToDeltaLag().get(t).add(deltaPt);
				this.mc1.addData(deltaPt);
				this.mc2.addData(deltaPtLag);
			}
		}
	}
	
	//Calculate the roll spreads for each ticker using the autocovariance
	public void calculateRollSpread() {
		for(String t:this.getTickersToDeltaPrices().keySet()) {
			MeanCalculator mc=new MeanCalculator();
			for(int i=0; i<this.getTickersToDeltaPrices().get(t).size()-1; i++) {
				double diff1=this.getTickersToDeltaPrices().get(t).get(i)-this.mc1.getAverage();
				double diff2=this.getTickersToDeltaPrices().get(t).get(i+1)-this.mc2.getAverage();
				mc.addData(diff1*diff2);
			}
			this.tickersToSpread.put(t, 2*Math.sqrt(-mc.getAverage()));
		}
	}
	
	public void subsetting() {
		for(String t:this.tickersToTotal.keySet()) {
			if(this.tickersToTotal.get(t)>=10000000.0) {
				this.subsetTotalSpread.put(t, new ArrayList<Double>());
				this.subsetTotalSpread.get(t).add(this.tickersToTotal.get(t));
				this.subsetTotalSpread.get(t).add(this.tickersToSpread.get(t));
			}
			
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		long start=System.currentTimeMillis();
		//Print writer for all tickers
		File total_spread=new File("/Users/meixisun/Documents/total_spread.txt");//Please change the file
		PrintWriter pw= new PrintWriter(total_spread);
		DataHandler dh=new DataHandler();
		
		//Perform the data manipulation
		dh.readData();
		dh.deltaPrices();
		dh.calculateRollSpread();
		pw.println("Ticker,Total_Volume,Spread");
		for(String t:dh.getTickersToSpread().keySet()) {
			pw.println(t+","+dh.getTickersToTotal().get(t)+","+dh.getTickersToSpread().get(t));
		}
		pw.close();
		
		//Print writer for tickers has volume greater than 10MM
		File subset_total_spread=new File("/Users/meixisun/Documents/subset_total_spread.txt");//Please change the path
		PrintWriter pw1= new PrintWriter(subset_total_spread);
		
		//Subset the data
		dh.subsetting();
		
		pw1.println("Ticker,Total_Volume,Spread");
		
		for(String t:dh.getSubset().keySet()) {
			pw1.println(t+","+dh.getSubset().get(t).get(0)+","+dh.getSubset().get(t).get(1));
		}
		pw1.close();
		
		long end=System.currentTimeMillis();
		
		long duration=end-start;
		
		System.out.println("The runtime of this program is: "+duration/1000+" seconds");

	}

}
