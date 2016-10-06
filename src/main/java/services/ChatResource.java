package services;

import domain.Room;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is a Chat Server.   
 * Server has two chat rooms: room-1 and room-2.
 * After a client entered one of the chat rooms,
 * Server announces to other users about the new comer.
 * 
 * While a client is in chat room, as soon as Server 
 * received a message from the client, Server echos
 * it back to all chat room users.
 * 
 * When a client drops out of a chat room, Server also informs
 * other members about the leave.
 * 
 * Some of the code is taken from the book "RESTful Java with JAX-RS 2.0" 
 * by Bill Burke.
 * 
 * @author Kim Nguyen 
 */

@Path("/")
public class ChatResource{      
   	public static final String CMD_PROMT = "<= ";
	public static final String WELCOME_MSG = "<= Welcome to the XYZ chat server";
	public static final String NAME_PROMT = "<= Login Name?";
	public static final String NAME_TAKEN = "<= Sorry, name taken.";
	public static final String EMPTY_NAME = "<= Sorry, name cannot be empty.";
	public static final String ALLOWED_LETTERS = "<= Sorry, allowed letters are [a-zA-Z0-1.-_]";
	public static final String LIST_END = "<= end of list.";	
	
	public static final String ROOM_1 = "room-1";
	public static final String ROOM_2 = "room-2";
	
	public static final int OK = 200;
	public static final int BAD_REQUEST = 400;
   
	private Set<String> users = new HashSet<String>();			// list of users
	private Map<String,Room> rooms = new HashMap<String,Room>();// list of rooms
	private Map<String,RoomResource> roomResources = null;// list of rooms

	@Context
	protected UriInfo uriInfo;
	
	// constructor
	public ChatResource() {		
		Room room1 = new Room(ROOM_1);			// create 2 chat rooms and add to list
		Room room2 = new Room(ROOM_2);
		rooms.put(room1.getName(), room1);
		rooms.put(room2.getName(), room2);		
	}
	
	// create RoomResources to access room resources
	private void createRoomResources() {		
		roomResources = new HashMap<String,RoomResource>();
		final UriBuilder base = uriInfo.getBaseUriBuilder();		
		for (Map.Entry<String, Room> entry: rooms.entrySet()) {
			RoomResource roomResource = new RoomResource(entry.getValue(),base);
			roomResources.put(entry.getKey(),roomResource);
		}		
		System.out.println("Server starting...");
	}

	@GET    
	@Produces("text/plain")
	// send welcome message to client and prompt for a login name 
	public Response welcome(){
		String message = WELCOME_MSG + "\n" + NAME_PROMT;		// send welcome msg and ask for login name
		if ( roomResources == null) {							// if 1st client, create room resources
			createRoomResources();
		}	   
		return Response.ok(message, MediaType.TEXT_PLAIN_TYPE).build();
	}
   
	@POST   
	@Produces("text/plain")
	@Consumes("text/plain")
	// read a login name from client and 
	// response with an Ok code if name is good
	// response with BAD_REQUEST code if name is duplicate, empty or contains illegal char
	public Response addUser(String userName) {	   
		String response = "";
		int result = validateUserName(userName);	// validate input name	   
		
		if ( result == 0) {							// if name is good
			synchronized (users) {													
				users.add(userName);				// add user
			}
			response = "<= Welcome " + userName + "!";
			
			final UriBuilder base = uriInfo.getBaseUriBuilder();		// get base URI
			URI nextUri = base.clone().path(userName).build();			// add user name as path	 	
			Link next = Link.fromUri(nextUri).rel("next").build();		// build link and send with response				
			
			return Response.status(OK).entity(response).links(next).build();// send OK response
		} 
		
		else {													// if name is bad			   
			if ( result == 1) {									// if name is empty
				response = EMPTY_NAME + "\n" + NAME_PROMT;
			}
			
			else if ( result == 2) {							// if name containing letter not allowed
				response = ALLOWED_LETTERS + "\n" + NAME_PROMT;
			}
		
			else  {												// if name is duplicate
				response = NAME_TAKEN + "\n" + NAME_PROMT;			
			}					
			return Response.status(BAD_REQUEST).entity(response).build();	// send BAD REQUEST response
		}		
   }
   
	// validate a user name, return:
	// - 1 for duplicate name
	// - 2 for empty name
	// - 3 for name with illegal char
	// - 0 for valid name
	private int validateUserName(String userName) {	   
		userName = userName.trim();
	   	if (userName.length() == 0)	{					// empty name	   		
	   		return 1;
	   	}
	   	
	   	if ( ! userName.matches("^[a-zA-Z0-9_.-]+$")) { // contains illegal char	   		
	   		return 2;
	   	}	   	
	   	
	   	synchronized (users){							// check existing user names
	   		if (users.contains(userName)) {				// duplicate name	   			
	   			return 3;
	   		}
	   	}	   	 	
	   	return 0;										// name is good, return 0
	}
	
	/*
	private void printUsers() {	   
		System.out.print("..New user list: ");			
		synchronized (users){							
		   		for (String user: users)
		   			System.out.print(user + " ");
		}	
		System.out.println();		  
	}
	*/
	
	@GET    
	@Path("/{userName}/rooms")
	@Produces("text/plain")
	// send response with list of chat rooms
	public Response getRooms(@PathParam("userName") String userName){
		String response = "<= Rooms:"+ "\n";				// send welcome msg and ask for login name	
		for (Map.Entry<String, Room> entry : rooms.entrySet()) {
			response += "<= * " + entry.getKey() + " (" + entry.getValue().getSize() + ")\n";
		}
		response += LIST_END;		
		
		final UriBuilder base = uriInfo.getBaseUriBuilder();		// get base URI
		URI nextUri = base.clone().path(userName).build();			// add user name as path	 	
		Link next = Link.fromUri(nextUri).rel("next").build();		// build link and send with response
		
		return Response.status(OK).entity(response).links(next).build();// send OK response		
	}
	
	@DELETE    
	@Path("/{userName}")
	@Produces("text/plain")
	// delete user from user list
	public Response deleteUser(@PathParam("userName") String userName){		
		if ( ! isGoodUser(userName)) {		// if user is not in the list		
			return badUserName(userName);	// send bad request response			
		}
		synchronized (users) {				
			users.remove(userName);				// remove user from list
		}
		String response = "<= BYE\n";					
		
		return Response.ok(response, MediaType.TEXT_PLAIN_TYPE).build();		
	}
	
	// check if a user is in the list
	private boolean isGoodUser(String userName) {
		synchronized (users) {			
			if (users.contains(userName)) {	// if user is in the list
				return true;				// return true
			}
			return false;					// return false;
		}		
	}
	
	// response with a bad request code when a user name not exits in the system
	private Response badUserName(String userName){
			String response = "user does not exist\n";						
			return Response.status(BAD_REQUEST).entity(response).build();	// send BAD REQUEST response			
	}
	
	// check if a user and a room exits and user is in the room
	private boolean isGoodUserAndRoom(String userName, String roomName) {
			if (! isGoodUser(userName)	) 				// if user not exits
				return false;
			if (! rooms.containsKey(roomName)) 			// if room not exits
				return false;
			if (! rooms.get(roomName).isUserInRoom(userName)) // if user not in the room
				return false;
			
			return  true;
	}
	
	// response with a bad request code when :
	// - a user name not exits or
	// - room does not exits or 
	// - user is not in the room
	private Response badUserOrRoom(String userName, String roomName){
		String response = "user does not exist OR\n";
		response += "room does not exist OR\n";
		response += "user is not in chat room\n";
		return Response.status(BAD_REQUEST).entity(response).build();	// send BAD REQUEST response			
	}	
	
	@POST    
	@Path("/{userName}/{roomName}")
	@Produces("text/plain")
	// add user to a chat room
	public Response joinChatRoom(@PathParam("userName") String userName,
								@PathParam("roomName") String roomName){		
		
		final UriBuilder base = uriInfo.getBaseUriBuilder();		// get base URI
		URI nextUri;	 	
		Link next;	
		String response = "";		
		
		if ( ! isGoodUser(userName)) {		// if user not exist		
			return badUserName(userName);	// send bad request response			
		}
		
		if ( ! rooms.containsKey(roomName) ) {						// if room not exist			
			response = "<= room not exists";
			nextUri = base.clone().path(userName).build();			// add user name as path	 	
			next = Link.fromUri(nextUri).rel("next").build();		// build link and send with response
					
			return Response.status(BAD_REQUEST).entity(response).links(next).build();// send BAD_REQUEST response		
		}		
		
		Room room = rooms.get(roomName);							// get the requested room
		Set<String> roomUsers = room.addUser(userName);				// add user to the room		
		
		String text = "<= * new user joined " + roomName + ": " + userName;		
		sendServerMessage(userName,roomName,text);						// announcing new comer 
		
		response = "<= entering room: " + roomName + "\n";	
		for (String roomUser : roomUsers) {							// print list of users in chat room
			response += "<= * " + roomUser +"\n";
		}		
		response += LIST_END;
		
		nextUri = base.clone().path(userName+"/"+roomName+"/messages").build();		
		next = Link.fromUri(nextUri).rel("next").build();		// build link and send with response
		
		return Response.status(OK).entity(response).links(next).build();// send OK response		
	}
	
	// send announcement of new users to other room members
	private void sendServerMessage(String userName,String roomName, String text) {			
		RoomResource rs = roomResources.get(roomName);		
		rs.postNewMessage(userName, roomName, text, true);		
	}
	
	@DELETE   
	@Path("/{userName}/{roomName}")
	// remove a user from chat room 
	public Response deleteUser(@PathParam("userName") String userName,
			   						@PathParam("roomName") String roomName)   {			
		if ( ! isGoodUserAndRoom(userName, roomName) )		// if userName and roomName not valid
			return badUserOrRoom(userName,roomName);		
		
		Room room = rooms.get(roomName);		
		room.removeUser(userName);			// remove user from chat room users list
		room.removeListener(userName);		// remove user from chat room listeners list
		String text = "<= * user has left " + roomName + ": " + userName;
		
		sendServerMessage(userName, roomName, text);		// announce to other users about the leave
		
		final UriBuilder base = uriInfo.getBaseUriBuilder();		// get base URI
		URI nextUri = base.clone().path(userName).build();		
		Link next = Link.fromUri(nextUri).rel("next").build();		// build link and send with response
		
		
				
		return Response.ok("", MediaType.TEXT_PLAIN_TYPE).links(next).build();
	}	
	
	@Path("/{userName}/{roomName}")
	// calling RoomResource for the service
	public RoomResource getRoomResource(@PathParam("userName") String userName,
										@PathParam("roomName") String roomName) {	
		if ( ! isGoodUserAndRoom(userName, roomName) )		// if userName and roomName not valid
			return null;									// do nothing		
				
		return roomResources.get(roomName);
	}	
}
