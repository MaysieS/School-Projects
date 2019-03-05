package middleware;

import org.apache.activemq.ActiveMQConnectionFactory;

import engine.StatsCollector;
import engine.StoppingCriteria;


import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/** This class is a server that can generate requests for option payout and send them to a client. After the client send back result, the 
 * server adds the result to a stats collector and keep tracking for the stopping criteria. The server sends 100 requests each time as a 
 * batch, and keep sending unless the stopping criteria is met.
 * The results and requests destinations must correspond to those of the server class to ensure the communication between the two classes.
 */
public class ActiveMQPublisher {
   protected int MAX_DELTA_PERCENT = 1;
   protected Map<String, Double> LAST_PRICES = new Hashtable<String, Double>();
   protected static int count = 10;
   protected static int total;

   protected static String brokerURL = "tcp://localhost:61616";
   protected static ConnectionFactory factory;
   protected Connection connection;
   protected Session session;
   protected MessageProducer producer;
   
   private StoppingCriteria _criteria;
   private int _numSimulation;
   
   private String ticker;
   

   public ActiveMQPublisher(String ticker) throws JMSException {
      factory = new ActiveMQConnectionFactory(brokerURL);
      connection = factory.createConnection();
      connection.start();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      producer = session.createProducer(null);
      
      this.ticker=ticker;
   }

   public void close() throws JMSException {
       if (connection != null) {
           connection.close();
       }
   }

   public static void main(String[] args) throws Exception {
	   //Pop up the choices of European and Asian options
	  System.out.println("Please choose an option style: "+"Type European or Asian");
	  //The user need to enter one type of option to run
	  Scanner scan=new Scanner(System.in);
	  String optionType=scan.nextLine();
      ActiveMQPublisher publisher = new ActiveMQPublisher("Results");//change the destination if necessary
      publisher.run(optionType);
      scan.close();

   }

   //This class calls multiple methods to complete the simulation process
   //Input: Option type, either 'European' or 'Asian'
   public void run(String optionType) throws Exception{
	 //Create queue for requests
	 Destination queue_1=session.createQueue("Requests");//change the destination if necessary
	      
	 //Create queue for results
	 Destination queue_2 = session.createQueue(ticker);
	 MessageConsumer messageConsumer =session.createConsumer(queue_2);
	      
	 //Stats collector
	 StatsCollector sc_1=new StatsCollector();
	      
	 //Keep tracking of the request id
	 TreeMap<Integer,Integer> idSet=new TreeMap<Integer,Integer>();
	      
	 //Warm up the simulation by 100 batches (10000 simulations) and return current id for further id generating
	 int id=this.warmUp(sc_1, optionType, queue_1, messageConsumer, idSet);
	    
	      
	 //Generate stopping criteria
	 this._criteria=new StoppingCriteria(0.98,sc_1.getSD(),0.1);
	 this._criteria.calculateNum();
	 this._numSimulation=this._criteria.getNumSimulation();
	 
	 //Run the simulation process, stop when meet the stopping criteria
	 this.runSimulation(id, queue_1, messageConsumer, optionType, sc_1, idSet);
	  
     
   }
   
   //Warm up the simulation by 100 batches (10000 simulations in total)
   //Input: stats collector, optionType, destination, message consumer
   //Return: Current id
   public int warmUp(StatsCollector sc, String optionType, Destination queue_1, MessageConsumer messageConsumer, TreeMap<Integer,Integer> idSet) throws Exception {
	   // A flag that checks if the client is running and send result
	   boolean received=false;
	   //Initiate the first id as 0
	   int id=0;
	   //Count number of batches sent
	   int count=0;
	   while(count<100) {
		      //Sent request in first loop or when the client is running and responding
	    	  if ((count==0) || (received)) {
	    		  for(int i=0; i<100; i++) {
	    	    		 TextMessage msg = session.createTextMessage(optionType+","+id+",252,152.35,0.01,0.0001,1");
	    	    		 producer.send(queue_1,msg);
	    	    		 //put the new id as a new key in the id set for later checking
	    	    		 idSet.put(id, 1);
	    	    		 //update the id for next message
	    	    		 id++;
	    	    	 }
	    		  count++;
	    	  }
	    	 
	    	  //wait for results from the client
	    	 Thread.sleep(500);
	    	 
	    	 //Listen to results sent from client
	    	 messageConsumer.setMessageListener(new MessageListener() {
		         public void onMessage(Message message) {
		            if (message instanceof TextMessage){
		               try{
		            	   //Store message in a string
		            	   String result=((TextMessage) message).getText();
		            	   //token the string for information
		            	   String[] tok=result.split(",");
		            	   int resultId=Integer.parseInt(tok[1]);
		            	   double payout=Double.parseDouble(tok[2]);
		            	   //add the data is the message was sent by this server
		            	   if(idSet.containsKey(resultId)) {
		   	            		sc.addData(payout);
		   	            	}   
		            	   
		               } catch (JMSException e ){
		                  e.printStackTrace();
		               }
		            }
		         }
		      });
	    	 
	    	 //as long as there is data in the stats collector, the client is working, set flag true
	    	 if(sc.getData().size()!=0) {
	    		 received=true; 
	    		 System.out.println("The running estimated payout for number "+count+ " batch of warm up is currently: "+sc.getAverage());
	    	 }
	    	 
	      }
	      System.out.println("Warm up is done!");
	      System.out.println("==========================================================================================");
	      //return the last id for further id generating
	      return(id);
   }
   
   //Run additional simulations beside the warm up
   //Input: current id, destination, message consumer, option type, stats collector, id set
   public void runSimulation(int id, Destination queue_1, MessageConsumer messageConsumer, String optionType, StatsCollector sc_1, TreeMap<Integer, Integer> idSet) throws Exception {
	   // A flag that checks if the client is running and send result
	   boolean received=false;
	   //Count for the number of simulations had ran
	   int count_2=0;
	   
	   //Check if the number of simulation needed is greater than 10000, if it is, further simulations are needed
	   if(this._numSimulation>10000) {
		   System.out.println("More simulations besides warm up simulations are needed!");
		   System.out.println("==========================================================================================");
		   //Count for the number of batches sent
		   int count_batch=0;
		   //check if another batch is needed
		   while (count_2<this._numSimulation-100){
			   if ((count_2==0) || (received)) {
				   for(int i=0; i<100; i++) {
					   TextMessage msg = session.createTextMessage(optionType+","+id+",252,152.35,0.01,0.0001,1");
					   producer.send(queue_1,msg);
					   idSet.put(id, 1);
					   id++;
					   count_2++;
				   }
				   count_batch++;
	       		}
	       	 	//Wait for responses from the client
	       	 	 Thread.sleep(500);
	       	 	 messageConsumer.setMessageListener(new MessageListener() {
	   	         public void onMessage(Message message) {
	   	            if (message instanceof TextMessage){
	   	               try{
	   	            	   String result=((TextMessage) message).getText();
	   	            	   String[] tok=result.split(",");
	   	            	   int resultId=Integer.parseInt(tok[1]);
	   	            	   double payout=Double.parseDouble(tok[2]);
	   	            	   //Check if the message was sent by this server
	   	            	   if(idSet.containsKey(resultId)) {
	   	            		sc_1.addData(payout);
	   	            	   }   
	   	               } catch (JMSException e ){
	   	                  e.printStackTrace();
	   	               }
	   	            }
	   	         }
	   	      });
	       	 
	       	 if(sc_1.getData().size()!=0) {
	       		 received=true; 
	       		System.out.println("The running estimated payout for number "+count_batch+ " batch AFTER warm up is currently: "+sc_1.getAverage());
	       	 }
	       	//update the stopping criteria after one batch was simulated
	       	this._criteria=new StoppingCriteria(0.98,sc_1.getSD(),0.1);
	        this._criteria.calculateNum();
	        //update the number of simulation needed
	        this._numSimulation=this._criteria.getNumSimulation();
	       }
	     }
	     else {
	    	 this._numSimulation=10000;
	     }
	      connection.close();
	      System.out.println("==========================================================================================");
	      System.out.println("Total number of simulation is: "+this._numSimulation);
	      System.out.println("==========================================================================================");
	      System.out.println("The final estimated payout for the option is: "+sc_1.getAverage());
   }

}
