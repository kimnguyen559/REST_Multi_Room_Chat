import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.TimeoutException;

/**
 * This is a chat client to test the Chat Server.
 * Client first connects to Server and enters a login name.
 * Client then signs up for one of the chat rooms and starts
 * to chat. Finally, client logs out of the chat room and exits
 * the system.
 * 
 * Client uses two threads: One thread reads console input then
 * sends request to Server.
 * 
 * The other thread reads Server response and display on the
 * console.
 * 
 * @author Kim Nguyen
 */
public class ChatClient{
	//public static final String SERVER_URL = "http://localhost:8080/";	// server URL
	//public static final String SERVER_URL = "http://xyzchat-env.us-west-2.elasticbeanstalk.com/";
	public static final String SERVER_URL = "http://localhost:8080";
	public static final String CMD_PROMT = "=> ";
	
	public static final int OK = 200;
	public static final int BAD_REQUEST = 400;	
	
	static boolean inChatRoom = false;		// flag if client is in a chat room	
	
	public static void main(String[] args) throws Exception   {		
		System.out.println();   		
		BufferedReader br = new BufferedReader
								(new InputStreamReader(System.in));

		final Client client = new ResteasyClientBuilder()		
                          		.connectionPoolSize(3)
                          		.build();
		WebTarget target = client.target(SERVER_URL);
		
		// 1st connection to Server   		
		Response response = target.request().get();        		// send get request 
		String responeMsg = response.readEntity(String.class);	
		System.out.println(responeMsg);  						// print server welcome response and prompt for login name			
		
        // read user input for login name to send to server
        do {        
        	System.out.print(CMD_PROMT);        
        	String input = br.readLine();  						// read console input for login name       	
        	response = target.request().post(Entity.entity 		// sent POST request with login name 
            				(input, MediaType.TEXT_PLAIN));        	
        	responeMsg = response.readEntity(String.class);		
        	System.out.println(responeMsg);       				// print server response message 	
        } while(response.getStatus() == BAD_REQUEST);			// loop back if login name is bad        
        
        Link nextLink = response.getLink("next");				// get link for the next request        

        // read input from console and send request to Server
        while (nextLink != null){								// loop to read user input
        	System.out.print(CMD_PROMT);         
        	String input = br.readLine().trim();				// read user input  
        	
        	int result = validateInput(input);					// validate input        	
        	
        	if (result != 4) {									// if valid input
        		URI uri = nextLink.getUri();        		
        		String newURI  ;	
        		
        		if (result == 0){								// if input is "/leave"
        			if (inChatRoom) {							// if in a chat room        				
        				newURI = uri.toString().replaceAll("/messages", "");        				 
        				response = client.target(newURI).request().delete();// send DELETE request to leave room
        			}
        			else {										
        				continue;
        			}
        		}
        		 
        		else if (result == 1) {							// if input is "/rooms"
        			if  (! inChatRoom) {						// if not in a chat room	        			
        			newURI = uri.toString() + input;	            		
        			response = client.target(newURI).request().get();	// send GET request for rooms list
        			}
        			else {
        				continue;
        			}
        		}
        		
        		else if (result == 2 ){							// if input is "/quit"
        			if (! inChatRoom){							// if not in a chat room       			
        			newURI = uri.toString();            		 
        			response = client.target(newURI).request().delete();// send DELETE request to exit
        			}
        			else {
        				continue;
        			}
        		} 
        		
        		else {	
        			if ( ! inChatRoom)		{					// if input is "/join roomName" & not in a chat room        				
        			int index = input.indexOf(' ');					// get index of space
        			newURI = uri.toString() + "/" + input.substring(index+1);	// calculate new URI            		             		 
        			response = client.target(newURI)				// send POST request to join roomName
        							.request()
        							.post(Entity.entity("", MediaType.TEXT_PLAIN));
        			}
        			else{
        				continue;
        			}
        		} 
        		responeMsg = response.readEntity(String.class);		// read response
        		nextLink = response.getLink("next");				// get link for the next request        		
        		
        		if (responeMsg!= null && responeMsg.length() > 0)	// if there is text in the response
        			System.out.println(responeMsg);       			// print server response message         		
        		
        		if ( (result==3) && (response.getStatus()==OK) ) { 	// request to join a chat room is OK      			
        			inChatRoom = true;								// set the flag        			
        			getNewMessage(client,target, nextLink);  		// start sending GET request for new messages      			
        		} 
        		
        		else if ( (result == 0) && (inChatRoom) 
        				&&(response.getStatus()==OK) ) { 			// if response to leave chat room is OK        			
        			inChatRoom = false;								// un-set the flag        			        			     			
        		} 
        	}
        	else if (inChatRoom && ( ! input.startsWith("/") )) {	// if in a chat room & text not started with '/'        		
        		client.target(nextLink)								// POST message to chat room
						.request()
						.post(Entity.entity(input, MediaType.TEXT_PLAIN));        		
        	}        	
        }        
        client.close();		// reach here when user type '/quit' to exist
	}
	
	// send GET request for new message
	private static void getNewMessage
			(final Client client, WebTarget target, Link link) {
		
		client.target(link).request().async().get(new InvocationCallback<Response>(){	// send async request
    		//@Override
    		public void completed(Response response){			// when response is back, this method runs
    			Link next = response.getLink("next");			// get link for the next request 
    			
    			String message = response.readEntity(String.class);
    			System.out.println();
    			System.out.print(message);// + "\r");
    			System.out.println();
    			System.out.print("=> ");
    			
    			if (inChatRoom) {								// if still in a chat room     				
    				client.target(next).request().async().get(this);// send a new GET request
    			}
    		}

    		//@Override
    		public void failed(Throwable throwable){
    			System.err.println("FAILURE!");
    		}
    	});
	}
	
	// receive user input string. return:
	// 0	: if input is "/leave"
	// 1	: if input is "/rooms"
	// 2	: if input is "/quit"
	// 3	: if input is "/join roomName"
	// 4	: if other
	private static int validateInput(String input) {	
		input = input.trim();
		
		if (input.equals("/leave")) {				
			return 0;
		}
		
		if (input.equals("/rooms")) {				
			return 1;
		}
		
		if (input.equals("/quit")) {			
			return 2;
		}
		
		if (input.matches("/join (.+)")) {					
			return 3;
		}
		
		return 4;			
	}
}
