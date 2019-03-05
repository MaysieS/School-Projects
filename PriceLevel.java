package exchangeStructures;

import java.util.ArrayList;


import orderTypes.RestingOrder;

public class PriceLevel {
	private ArrayList<RestingOrder> _restingOrder;
	
	//Constructor
	public PriceLevel() {
		_restingOrder=new ArrayList<RestingOrder>();
	}
	
	//Getter for resting orders on this price level
	public ArrayList<RestingOrder> getOrders() {
		return this._restingOrder;
	}
	
	//Add new resting order to this price level
	public void addRestingOrder(RestingOrder ro) {
		this._restingOrder.add(ro);
	}
	
	

}
