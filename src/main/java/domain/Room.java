package domain;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.container.AsyncResponse;

/**
 * @author Kim Nguyen 
 */

public class Room {
	public static final int MAX_SIZE = 10;				// maximun number of messages in the list
	
	private String name;								// name of the chat room
	private Set<String> users;							// list of current users	  

	private LinkedHashMap<String, Message> messages;	// 10 last messages
	private AtomicLong counter;							// message ID counter
	private Message firstMessage;						// 1st message in the list of 10 last
	private Message lastMessage;						// last message in the list

	Map<String,AsyncResponse> listeners;				// list of current listeners to new message			

	public Room(String name) {		
		this.name = name;				
		users = new HashSet<String>();		
		listeners = new ConcurrentHashMap<String,AsyncResponse>();				
		messages = new LinkedHashMap<String, Message>(){	// this block is from Bill Burke's book				   			
		      		@Override
		      			protected boolean removeEldestEntry
		      							(Map.Entry<String, Message> eldest){
		      				boolean remove = size() > MAX_SIZE;
		      				if (remove) 						// remove oldest message when size is max
		      					firstMessage = eldest.getValue().getNext();
		      				return remove;
		      			}
		   			};
		counter = new AtomicLong(0);
	}

	// add a new user to the chat room
	// return list of updated users
	public Set<String> addUser(String userName) {
		synchronized (users) {
			users.add(userName);
		}	
		
		return users;
	}
	
	// remove a user from list
	public void removeUser(String userName) {
		synchronized (users) {
			if (users.contains(userName)) {
				users.remove(userName);				
			}
		}		
	}	
	
	public void printUsers() {
		System.out.print("New user list: ");
		synchronized (users) {
			for (String user: users)
				System.out.print(user+" ");
		}	
		System.out.println();
	}
	
	public void printListeners() {
		System.out.print("New listener list: ");
		synchronized (listeners) {
			for (String listener: listeners.keySet())
				System.out.print(listener+" ");
		}	
		System.out.println();
	}
	
	
	// add a new listener to listeners list	
	public void addListener(String userName, AsyncResponse async) {			
		listeners.put(userName,async);		
	}
	
	// remove a listener from list	
	public void removeListener(String userName) {			
		listeners.remove(userName);			
	}
	
	// return room size (= number of users)
	public int getSize() {	
		int size;
		synchronized (users) {
			size = users.size();
		}
		return size;		
	}
	
	// check if a user is in the chat room
	public boolean isUserInRoom(String userName) {				
		synchronized (users) {
				if (users.contains(userName))
					return true;
				return false;
		}				
	}
	
	// add a new message to message list
	// return the new created message
	public Message addMessage ( String text, String userName, boolean fromServer) {	
		Message message = new Message();			
		synchronized (messages){								// this block of code is from Buill Burke's book
			String id = Long.toString(counter.incrementAndGet());// get an ID for the new message
			message.setId(id);		    		
			message.setMessage(text);
			if ( fromServer) {
				message.setExcludedUser(userName);				
			}
			if (messages.size() == 0){							// if message list is empty
				firstMessage = message;							// set new message be the 1st message
			}
			else{												// if message list is not empty
				lastMessage.setNext(message);					// set 'next' field of the last message point to new msg
			}			
			messages.put(message.getId(), message);				// add new message to the list
			lastMessage = message;	    						// set new message as last message of the list	
			
		}
		return message;
	}
	
	// return the message following the one with 
	// id matching input 'next'
	public Message getNextMessage(String next, String userName) {		
		Message message = null;						
		synchronized (messages){			
			Message current = messages.get(next);	// get the message with id matched input				
			if (current == null) {					// if no message matched
				message = firstMessage;				// return the 1st message in the list				
			}
			else {				
				message = current.getNext();		// if there is a matched, get the next message
			}
			
			while ( (message != null) && isExclude(userName, message)) { // if user is excluded to the message				
				message = message.getNext();							// get the next one
			}
		}		
		return message;
	}
	
	// check if a user is excluded to receive a message
	public static boolean isExclude(String userName, Message message) {
	
		String excludedUser = message.getExcludedUser();	// get the excluded user name
		
		if ( excludedUser == null)	{					// if no excluded user specified			
			return false;								// return false
		}
		if ( excludedUser.equals(userName))	{			// if user matched the excluded user			
			return true;									// return true
		}		
		return false;	   
	}	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LinkedHashMap<String, Message> getMessages() {
		return messages;
	}

	public void setMessages(LinkedHashMap<String, Message> messages) {
		this.messages = messages;
	}

	public Map<String, AsyncResponse> getListeners() {
		return listeners;
	}	

	public void setListeners(Map<String, AsyncResponse> listeners) {
		this.listeners = listeners;
	}

	public void setUsers(Set<String> users) {
		this.users = users;
	}
}