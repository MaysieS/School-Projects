package exchangeStructures;


import java.util.HashMap;

import fills.Fill;
import messages.Cancelled;
import messages.RestingOrderConfirmation;
import orderSpecs.Price;
import orderSpecs.Quantity;
import orderSpecs.Side;
import orderTypes.RestingOrder;
import orderTypes.SweepingOrder;


public class Book {
	
	Market _market;
	Side _side;
	HashMap<Price, PriceLevel> _priceLevel;
	Book otherSide;
	
	//Constructor
	public Book(Market market, Side side) {
		this._market=market;
		this._side=side;
		this._priceLevel=new HashMap<Price, PriceLevel>();
		this.otherSide=this;
	}
	
	//Getter for market
	public Market getMarket() {
		return this._market;
	}
	
	//Getter for side
	public Side getSide() {
		return this._side;
	}
	
	//Getter for otherside
	public Book getOtherSide() {
		return this.otherSide;
	}
	
	//Getter for pricelevel
	public HashMap<Price, PriceLevel> getPriceLevels() {
		return this._priceLevel;
	}
	
	//Setter for market
	public void setMarket(Market market) {
		this._market=market;
	}
	
	//Setter for side
	public void setSide(Side side) {
		this._side=side;
	}
	
	//Setter for pricelevel
	public void setPriceLevel(HashMap<Price, PriceLevel> pl) {
		this._priceLevel=pl;
	}
	
	//Setter for otherside
	public void setOtherSide(Book other) {
		this.otherSide=other;
	}
	
	public void sweep(SweepingOrder sweepingOrder) throws Exception {
		//Check if there is cancelled order in the book
		//If there is such order, remove it from the book
		for (Cancelled c:this.getMarket().getExchange().getComms().getCancelationConfirmations()) {
			for (Price price:this._priceLevel.keySet()) {
				for(int i=0; i< this._priceLevel.get(price).getOrders().size();i++) {
					if(c.getClientOrderId().hashCode()==this._priceLevel.get(price).getOrders().get(i).getClientOrderId().hashCode()) {
						this._priceLevel.get(price).getOrders().remove(i);
					}
				}
				if(this._priceLevel.get(price).getOrders().size()==0) {
					this._priceLevel.remove(price);
				}
				
			}
		}
		
		if (this._side==Side.SELL) {
			//If there is no resting order in this ask book, create resting order in the bid book
			if (this._priceLevel.size()==0) {
				this.conductSweepingOrder(sweepingOrder);
			}
			else {
				//Find the minimum ask price in this ask book to see if there is order could be filled by the sweeping order
				//Remove the corresponding price level if all of the orders on this price level were filled
				//Send fill messages to the exchange for both side of the trade
				//If the sweeping order is not filled, set the rest of it as a resting order
				while(compareMintoAsk(sweepingOrder) && (!sweepingOrder.isFilled()) && (this._priceLevel.get(this.findMinPrice()).getOrders().size()>0)) {
					Price min=this.findMinPrice();
					for(int i=0; i<this._priceLevel.get(min).getOrders().size(); i++) {
						
						Quantity quantity=new Quantity(sweepingOrder.getQuantity().getValue());
						if (this._priceLevel.get(min).getOrders().get(i).getQuantity().compareTo(sweepingOrder.getQuantity())>=0) {
							Fill fill=new Fill(this._priceLevel.get(min).getOrders().get(i).getClientId(),sweepingOrder.getClientId(),this._priceLevel.get(min).getOrders().get(i).getClientOrderId(),quantity);
							Fill fill2=new Fill(sweepingOrder.getClientId(),this._priceLevel.get(min).getOrders().get(i).getClientId(),sweepingOrder.getClientOrderId(),quantity);
							this._priceLevel.get(min).getOrders().get(i).reduceQtyBy(quantity);
							if(this._priceLevel.get(min).getOrders().get(i).getQuantity().hashCode()==0) {
								this.getMarket().getExchange().setNull(this._priceLevel.get(min).getOrders().get(i).getClientOrderId());
							}
							this.getMarket().getExchange().getComms().sendFill(fill);
							this.getMarket().getExchange().getComms().sendFill(fill2);
							sweepingOrder.reduceQtyBy(quantity);
						}
						else {
							Fill fill=new Fill(this._priceLevel.get(min).getOrders().get(i).getClientId(),sweepingOrder.getClientId(),this._priceLevel.get(min).getOrders().get(i).getClientOrderId(),this._priceLevel.get(min).getOrders().get(i).getQuantity());
							Fill fill2=new Fill(sweepingOrder.getClientId(),this._priceLevel.get(min).getOrders().get(i).getClientId(),sweepingOrder.getClientOrderId(),this._priceLevel.get(min).getOrders().get(i).getQuantity());
							this.getMarket().getExchange().getComms().sendFill(fill);
							this.getMarket().getExchange().getComms().sendFill(fill2);
							sweepingOrder.reduceQtyBy(this._priceLevel.get(min).getOrders().get(i).getQuantity());
							this.getMarket().getExchange().setNull(this._priceLevel.get(min).getOrders().get(i).getClientOrderId());
							this._priceLevel.get(min).getOrders().remove(i);
						}
					}
				}
				if(!sweepingOrder.isFilled()) {
					this.conductSweepingOrder(sweepingOrder);
					RestingOrder ro=new RestingOrder(sweepingOrder);
					this.getMarket().getExchange().getComms().sendRestingOrderConfirmation(new RestingOrderConfirmation(ro));
				}
			}
		}
		else {
			//If there is no resting order in this bid book, create resting order in the ask book
			if (this._priceLevel.size()==0) {
				this.conductSweepingOrder(sweepingOrder);
			}
			else {
				//Find the maximum ask price in this ask book to see if there is order could be filled by the sweeping order
				//Remove the corresponding price level if all of the orders on this price level were filled
				//Send fill messages to the exchange for both side of the trade
				//If the sweeping order is not filled, set the rest of it as a resting order
				while(compareMaxtoBid(sweepingOrder) && (!sweepingOrder.isFilled()) && (this._priceLevel.get(this.findMaxPrice()).getOrders().size()>0)) {
					Price max=this.findMaxPrice();
					for(int i=0; i<this._priceLevel.get(max).getOrders().size(); i++) {
						
						if (this._priceLevel.get(max).getOrders().get(i).getQuantity().compareTo(sweepingOrder.getQuantity())>=0) {
							Quantity quantity=new Quantity(sweepingOrder.getQuantity().getValue());
							this._priceLevel.get(max).getOrders().get(i).reduceQtyBy(quantity);
							Fill fill=new Fill(this._priceLevel.get(max).getOrders().get(i).getClientId(),sweepingOrder.getClientId(),this._priceLevel.get(max).getOrders().get(i).getClientOrderId(),quantity);
							Fill fill2=new Fill(sweepingOrder.getClientId(),this._priceLevel.get(max).getOrders().get(i).getClientId(),sweepingOrder.getClientOrderId(),quantity);
							this.getMarket().getExchange().getComms().sendFill(fill);
							this.getMarket().getExchange().getComms().sendFill(fill2);
							sweepingOrder.reduceQtyBy(quantity);
							if(this._priceLevel.get(max).getOrders().get(i).getQuantity().hashCode()==0) {
								this.getMarket().getExchange().setNull(this._priceLevel.get(max).getOrders().get(i).getClientOrderId());
							}
							
						}
						else {
							Fill fill=new Fill(this._priceLevel.get(max).getOrders().get(i).getClientId(),sweepingOrder.getClientId(),this._priceLevel.get(max).getOrders().get(i).getClientOrderId(),this._priceLevel.get(max).getOrders().get(i).getQuantity());
							Fill fill2=new Fill(sweepingOrder.getClientId(),this._priceLevel.get(max).getOrders().get(i).getClientId(),sweepingOrder.getClientOrderId(),this._priceLevel.get(max).getOrders().get(i).getQuantity());
							this.getMarket().getExchange().getComms().sendFill(fill);
							this.getMarket().getExchange().getComms().sendFill(fill2);
							sweepingOrder.reduceQtyBy(this._priceLevel.get(max).getOrders().get(i).getQuantity());
							this.getMarket().getExchange().setNull(this._priceLevel.get(max).getOrders().get(i).getClientOrderId());
							this._priceLevel.get(max).getOrders().remove(i);
							
						}
					}
					if(this._priceLevel.get(this.findMaxPrice()).getOrders().size()==0) {
						this._priceLevel.remove(max);
					}
				}
				if(!sweepingOrder.isFilled()) {
					this.conductSweepingOrder(sweepingOrder);
					RestingOrder ro=new RestingOrder(sweepingOrder);
					this.getMarket().getExchange().getComms().sendRestingOrderConfirmation(new RestingOrderConfirmation(ro));
				}
			}
		}
		
		
	}
	
	//Add new pricelevel
	//Input: An object of Price as the key
	public void addPriceLevel(Price price) {
		PriceLevel pl=new PriceLevel();
		this._priceLevel.put(price, pl);
	}
	
	//Return true if he minimum price of the ask book is less than the price of the sweeping order
	//Return false otherwise
	public boolean compareMintoAsk(SweepingOrder sweepingOrder) {
		Price min=this.findMinPrice();
		if(min.compareTo(sweepingOrder.getPrice())>0) {
			return false;
		}
		else {
			return true;
		}
	}
	
	//Return true if he maximum price of the bid book is greater than the price of the sweeping order
	//Return false otherwise
	public boolean compareMaxtoBid(SweepingOrder sweepingOrder) {
		Price max=this.findMaxPrice();
		if(max.compareTo(sweepingOrder.getPrice())<0) {
			return false;
		}
		else {
			return true;
		}
	}
	
	//Find the minimum price in the resting orders of the book
	public Price findMinPrice(){
		Price min=this._priceLevel.keySet().iterator().next();
		for (Price price:this._priceLevel.keySet()) {
			if (min.compareTo(price)>0) {
				min=price;
			}
		}
		return min;
	}
	
	//Find the minimum price in the resting orders of the book
	public Price findMaxPrice() {
		Price max=this._priceLevel.keySet().iterator().next();
		for (Price price:this._priceLevel.keySet()) {
			if (max.compareTo(price)<0) {
				max=price;
			}
		}
		return max;
	}
	
	//Add the unfilled sweeping order as resting order to the book and exchange
	public void conductSweepingOrder(SweepingOrder sweepingOrder) {
		RestingOrder resting=new RestingOrder(sweepingOrder);
		Price price=sweepingOrder.getPrice();
		if (this.otherSide._priceLevel.containsKey(price)) {
			this.otherSide._priceLevel.get(price).addRestingOrder(resting);
			this.getMarket().getExchange().addRestingOrder(sweepingOrder.getClientOrderId(), resting);
		}
		else{
			this.otherSide.addPriceLevel(price);
			this.otherSide._priceLevel.get(price).addRestingOrder(resting);
			this.getMarket().getExchange().addRestingOrder(sweepingOrder.getClientOrderId(), resting);
		}
	}

}
