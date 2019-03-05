/*Problem 2
 * This class utilizes the data handler for problem 1 and conducts the tick test for all the top 30 common stocks
 * *Please change the path of the master file and the printed file to your local path
 * */

package handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;


public class TickTest {
	
	//Data handler
	private DataHandler dh;
	//Tree map links total volumes to tickers
	private TreeMap<Double,String> totalToTickers;
	//Linked list contains the top 30 tickers
	private LinkedList<String> top30;
	//Hashmap links tickers to their types
	private HashMap<String,String> tickersToType;
	//Hashmap links tickers to number of seller-initiated orders
	private HashMap<String,Integer> testSell;
	//Hashmap links tickers to number of buyer-initiated orders
	private HashMap<String,Integer> testBuy;
	//Hashmap links tickers to number of undetermined orders
	private HashMap<String,Integer> testUndetermined;
	
	public TickTest() throws FileNotFoundException {
		this.dh=new DataHandler();
		this.initiateDH();
		this.setTickersToType();
		this.setTotalToTickers();
		this.setTop30();
		this.testBuy=new HashMap<String,Integer>();
		this.testSell=new HashMap<String,Integer>();
		this.testUndetermined=new HashMap<String,Integer>();
	}
	
	//Initiate members in the data handler
	public void initiateDH() {
		this.dh.readData();
		this.dh.deltaPrices();
		this.dh.subsetting();
	}
	
	public TreeMap<Double,String> getTotalToTickers() {
		return this.totalToTickers;
	}
	
	public LinkedList<String> getTop30() {
		return this.top30;
	}
	
	public HashMap<String,Integer> getSell() {
		return this.testSell;
	}
	
	public HashMap<String,Integer> getBuy() {
		return this.testBuy;
	}
	
	public HashMap<String,Integer> getUndetermined() {
		return this.testUndetermined;
	}
	
	
	public void setTotalToTickers() {
		this.totalToTickers=new TreeMap<Double,String>(Collections.reverseOrder());
		for(String t:this.dh.getSubset().keySet()) {
			if(this.tickersToType.get(t).equals("A")) {
				this.totalToTickers.put(this.dh.getSubset().get(t).get(0), t);
			}
		}
	}
	
	//Picks the top 30 tickers with high total volume
	public void setTop30() {
		this.top30=new LinkedList<String>();
		Iterator<Entry<Double, String>> itr=this.totalToTickers.entrySet().iterator();
		for(int i=0;i<30;i++) {
			this.top30.add(itr.next().getValue());
		}
	}
	
	//Read the master file and map all the tickers in the subset with their types
	public void setTickersToType() throws FileNotFoundException {
		this.tickersToType=new HashMap<String,String>();
		String path="/Users/meixisun/Documents/mastm_20171229.txt";//Please change path here
		File f=new File(path);
		Scanner scan=new Scanner(f);
		String line=scan.nextLine();
		while(scan.hasNextLine()) {
			line=scan.nextLine();
			String[] tok=line.split("\\|");
			String ticker=tok[1];
			String type=tok[4];	
			this.tickersToType.put(ticker, type);			
		}
		scan.close();
	}
	
	//Conduct the tick test
	public void tickTest() {
		for(String t:this.top30) {
			this.testBuy.put(t, 0);
			this.testSell.put(t, 0);
			this.testUndetermined.put(t, 0);
		}
		
		for(String t:this.top30) {
			for(int i=0; i<this.dh.getTickersToDeltaPrices().get(t).size();i++) {
				if(this.dh.getTickersToDeltaPrices().get(t).get(i)<0) {
					this.testSell.put(t, this.testSell.get(t)+1);
				}
				else if(this.dh.getTickersToDeltaPrices().get(t).get(i)>0) {
					this.testBuy.put(t, this.testBuy.get(t)+1);
				}
				else {
					if(i>=1) {
						if(this.dh.getTickersToDeltaPrices().get(t).get(i-1)<0) {
							this.testSell.put(t, this.testSell.get(t)+1);
						}
						else if(this.dh.getTickersToDeltaPrices().get(t).get(i-1)>0) {
							this.testBuy.put(t, this.testBuy.get(t)+1);
						}
						else {
							this.testUndetermined.put(t, this.testUndetermined.get(t)+1);
						}
					}
					else {
						this.testUndetermined.put(t, this.testUndetermined.get(t)+1);
					}
					
				}
			}
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		long start=System.currentTimeMillis();
		File f=new File("/Users/meixisun/Documents/ticktest.txt");
		PrintWriter pw=new PrintWriter(f);
		
		TickTest tt=new TickTest();
		tt.tickTest();
		pw.println("Ticker,Sell,Buy,Undetermined");
		for(int i=0; i<tt.getTop30().size();i++) {
			String t=tt.getTop30().get(i);
			pw.println(t+","+tt.getSell().get(t)+","+tt.getBuy().get(t)+","+tt.getUndetermined().get(t));
		}
		pw.close();
		
		long end=System.currentTimeMillis();
		
		long duration=end-start;
		
		System.out.println("The runtime of this program is: "+duration/1000.0+" seconds");

	}

}
