package exchangeStructures;

import java.util.HashMap;

import messages.Cancel;
import messages.CancelRejected;
import messages.Cancelled;
import messages.RestingOrderConfirmation;
import orderSpecs.ClientOrderId;
import orderSpecs.MarketId;
import orderTypes.RestingOrder;
import orderTypes.SweepingOrder;

public class Exchange {
	
	private HashMap<MarketId,Market> _market;
	private HashMap<ClientOrderId, RestingOrder> _restingOrder;
	private Comms _comms;
	
	//Constructor
	public Exchange() {
		this._market=new HashMap<MarketId,Market>();
		this._restingOrder=new HashMap<ClientOrderId, RestingOrder>();
		this._comms=new Comms();
	}
	
	//Add new market to the exchange
	public void addMarket(Market market) {
		this._market.put(market.getMarketId(), market);
	}
	
	//Getter for market Id
	public Market getMarket(MarketId id) {
		return this._market.get(id);
	}
	
	//Getter for a specific resting order
	//Input: ClientOrderId
	public RestingOrder getOrder(ClientOrderId cId) {
		return this._restingOrder.get(cId);
	}
	
	//Getter for all resting orders
	public HashMap<ClientOrderId, RestingOrder> getRestingOrder() {
		return this._restingOrder;
	}
	
	//Getter for communications
	public Comms getComms() {
		return this._comms;
	}
	
	//Add a new resting order
	public void addRestingOrder(ClientOrderId cId, RestingOrder ro) {
		this._restingOrder.put(cId, ro);
	}
	
	//Set a certain resting order null
	public void setNull(ClientOrderId cId) {
		this._restingOrder.put(cId, null);
	}
	
	public void sweep(SweepingOrder sweepingOrder) throws Exception {
		this._market.get(sweepingOrder.getMarketId()).sweep(sweepingOrder);
		
		//If the sweeping order is not filled, add it to the list
		//Send resting order confirmation
		if (!sweepingOrder.isFilled()) {
			RestingOrder ro=new RestingOrder(sweepingOrder);
			this._restingOrder.put(sweepingOrder.getClientOrderId(), ro);
			this._comms.getRestingOrderConfirmations().add(new RestingOrderConfirmation(ro));
		}	
	}
	
	//Cancel a certain order
	public void cancel(Cancel cancel) {
		if(this._restingOrder.get(cancel.getClientOrderId())==null) {
			CancelRejected cr=new CancelRejected(cancel.getClientId(),cancel.getClientOrderId());
			this._comms.sendCancelRejected(cr);
		}
		else {
			this.setNull(cancel.getClientOrderId());
			Cancelled cancelled=new Cancelled(cancel.getClientId(),cancel.getClientOrderId());
			this._comms.getCancelationConfirmations().addLast(cancelled);
			this._comms.cancelled(cancelled);
		}
		
	}
	

}
