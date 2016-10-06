package services;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import domain.Message;
import domain.Room;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kim Nguyen 
 */

@Path("/")
public class RoomResource{
	ExecutorService writer = Executors.newSingleThreadExecutor();// a single thread to send new message to async clients
	private Room room;
	private UriBuilder base;

	@Context
	protected UriInfo uriInfo;
	
	public RoomResource(Room room, UriBuilder base) {
		this.room = room;
		this.base = base;
	}

	@POST
	@Path("/messages")
	@Consumes("text/plain")
	// client posts a new message to chat room
	public void post(@PathParam("userName") final String userName,
								@PathParam("roomName") final String roomName,
								final String text)   {		
		
		postNewMessage(userName,roomName,text, false);
	}
	
	// calling writer thread to create new message and send to listeners list
	public void postNewMessage(final String userName,final String roomName,
								final String text, final boolean fromServer)   {	
		
		writer.submit(new Runnable(){								// run writer thread
						//@Override
						public void run()	{							
							String newText = text;
							
							if ( ! fromServer )						// if message not from Server
								newText = "<= "+userName+": " + text; // add sender name in the message
							
							Message message = room.addMessage(newText,userName,fromServer);// add new message														
							Map<String, AsyncResponse> listeners = room.getListeners();	// get list of listeners	
							
							for (Map.Entry<String, AsyncResponse> entry : listeners.entrySet())  {// send message to each listener in the list									
								try{ 
									String key = entry.getKey();
									AsyncResponse value = entry.getValue();
									
									if (! Room.isExclude(key, message)) {		// if user is not excluded	
										
										send(base, value, key, roomName,message);	// then send message to the user
										listeners.remove(entry.getKey());			// remove user from listener list
									}									
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}														
						}         		
		});
	}

   @GET   
   @Path("/messages")
   // get a new posted message 
   public void getNewMessage(@PathParam("userName") String userName,
		   						@PathParam("roomName") final String roomName,
		   						@QueryParam("current") String next, 
		   						@Suspended AsyncResponse async)   {	   
	   
	   
	   Message message = room.getNextMessage(next,userName);		// get the next message
	   
	   if (message == null) {		// if no new message		   
		   queue(userName,async);	// queue the request
	   }
	   else {  						// if there is new message			   
		   send(base, async, userName, roomName, message);	// send to client
	   }    
   }
   
   @DELETE   
   // remove a user from chat room 
   public Response deleteUser(@PathParam("userName") String userName,
		   						@PathParam("roomName") final String roomName)   {		   
	   
	   room.removeUser(userName);						// remove user from chat room
	   String text = "<= * user has left " + roomName + ": " + userName;	   
	   postNewMessage(userName, roomName, text, true);	// announce to other users about the leave
	   	  
	   URI nextUri = base.clone().path(userName).build();
	   Link next = Link.fromUri(nextUri).rel("next").build();
	   
	   return Response.ok("", MediaType.TEXT_PLAIN_TYPE).links(next).build();
   }

   // add new listener to listeners list
   protected void queue(String userName,AsyncResponse async){		   
	   room.addListener(userName, async);	   
   }

   // send message to client
   protected void send(UriBuilder base, AsyncResponse async, 
		   				String userName, String roomName, Message message){	   	  
	   
	   URI nextUri = base.clone().path(userName+"/"+roomName+"/messages")
			   						.queryParam("current", message.getId()).build();
	   Link next = Link.fromUri(nextUri).rel("next").build();	   	   
	   
	   Response response = Response.ok(message.getMessage(), MediaType.TEXT_PLAIN_TYPE).links(next).build();
	   async.resume(response);	   
   }   
}
