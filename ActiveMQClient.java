package middleware;

import org.apache.activemq.ActiveMQConnectionFactory;

import path.Path;
import payout.AsianPayout;
import payout.EuropeanPayout;

import javax.jms.*;

/** This class is a client that can listen to messages sent by server and calculate and send out results. It could recognize the type of option
 * and informations related to that option. It utilize classes in the payout package to do the calculation.
 * The results and requests destinations must correspond to those of the server class to ensure the communication between the two classes.
 */
public class ActiveMQClient {
  

   private static String brokerURL = "tcp://localhost:61616";
   private static ConnectionFactory factory;
   private Connection connection;
   private Session session;
   private String ticker;
   private MessageProducer producer;

   public ActiveMQClient(String ticker) throws Exception {
      factory = new ActiveMQConnectionFactory(brokerURL);
      connection = factory.createConnection();
      connection.start();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      producer = session.createProducer(null);
      this.ticker = ticker;
   }

   public static void main(String[] args) throws Exception {
      ActiveMQClient client = new ActiveMQClient("Requests");//change the destination if necessary
      client.run();
   }
   
   //This class is designed to receive requests from the server and calculate and send back result
   public void run() throws Exception{
	  //Listen to requests
      Destination destination_1 = session.createQueue(ticker);
      MessageConsumer messageConsumer =session.createConsumer(destination_1);
      
      //Create destination for result
      Destination destination_2=session.createQueue("Results");//change the destination if necessary
      messageConsumer.setMessageListener(new MessageListener() {
	         public void onMessage(Message message) {
	            if (message instanceof TextMessage){
	               try{
	            	   //token the message sent by server to get information
	            	  String request=((TextMessage) message).getText();
	            	  String[] tok=request.split(",");
	            	  
	            	  //store all the information
	            	  String optionType=tok[0];
	            	  int id=Integer.parseInt(tok[1]);
	            	  int duration=Integer.parseInt(tok[2]);
	            	  double startPrice=Double.parseDouble(tok[3]);
	            	  double volatility=Double.parseDouble(tok[4]);
	            	  double interest=Double.parseDouble(tok[5]);
	            	  int interval=Integer.parseInt(tok[6]);
	            	  
	            	  //generate a path to calculate the payout
	            	  Path p=new Path();
	            	  p.generatePath(duration,startPrice,volatility,interest,interval);
	            	  
	            	  //check which kind of payout to return
	            	  double payout=0.0;
	            	  if(optionType.equals("European")) {
	            		  EuropeanPayout ep=new EuropeanPayout(165);
	            		  payout=ep.payout(p);
	            	  }
	            	  else {
	            		  AsianPayout ep=new AsianPayout(164);
	            		  payout=ep.payout(p);
	            	  }
	            	  
	            	  //Generate result message
	            	  TextMessage msg = session.createTextMessage(optionType+","+id+","+payout);
	            	  //send the message to the result destination
	            	  producer.send(destination_2,msg);
	               } catch (JMSException e ){
	                  e.printStackTrace();
	               }
	            }
	         }
	      });
   }

  public Session getSession() {
     return session;
  }

}
