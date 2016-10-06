# REST Multi-Room Chat Web Service

This project, used Java JAX-RS API and RESTEasy framewok, implements a multiple-room chat REST server that allows users to sign in a chosen room and exchange conversation with other users through instant messages.

A REST client app is also implemented to test the functionalities of the chat server.

Users will be prompted for a login name when first connecting to the server.  A name could be rejected if it is a duplicate or if it contains letters that are not in the permitted set, [a-Za-z0-9.-_].

When a user enters a chat room, the Server announces them to other users in the room.

While a user is in a chat room, as soon as the Server receives a message from them, it echoes back to others in the room.

When a user quits a chat room, the Server announces the user’s departure.

By: Kim Nguyen - kimnguyen559@gmail.com

## User Stories

The following functionality is completed:

SERVER SIDE:
* Client can connect to the Server by sending a HTTP request
* Client can sign in and join one of two chat rooms, named “room-1” and “room-2”
* Client can receives instant messages from other Clients in the chat room
* Client can send instant messages to other Clients in the same chat room
* Client can quit the current chat room to join another one
* Client can exit and close the connection

CLIENT SIDE:
* User can get all the services provided by the Server throught the Client app

The following features are implemented:

* SERVER SIDE:
  Allow scalability using Asynchronous processing feature of JAX-RS: This mechanism allows to keep the thread pool at a minimal size while serving a large number of clients.  For request that takes little time to complete, such as signing in or joining a chat room, Server assigns one thread to one Client. For Client's poll request, there is only one single thread handling all waiting requests. When a new message is coming, the worker thread sends it to all clients queued in the waiting list.

* CLIENT SIDE:
  Allow users to send and receive messages concurrently by using two different threads in the same process. 

The Interface:

* GET / 						        : connect to server
* POST /						        : log in with a login name

* GET /{userName}/rooms			        : get list of chat rooms
* DELETE /{userName}				    : exit the system

* POST /{userName}/{roomName}		    : join a room
* DELETE /{userName}/{roomName}		    : leave the chat room

* GET /{userName}/{roomName}/messages	: get new message posted to chat room
* POST /{userName}/{roomName}/messages	: post a new message to chat room


## Video Walkthrough 

Here's a walkthrough of implemented user stories:

[Video Walkthrough](https://www.giphy.com/gifs/l2SqauWgUal9yDA9W/).

## Notes

This project was based on an example in the book "RESTful Java with JAX-RS 2.0" by Bill Burke, the founder of the RESTEasy project.  

Some of the author's code was also included in my program.  When this happened, a comment stating the origin of the code was inserted.

The sample code in the above book is an implementation of a simple chat server where a user’s message will be delivered to all users instantly.

My project extends the service to multiple rooms.  Additionally, beside delivering Client messages, Server can also send messages to Client.





