To run this project, we will use a command line for both client and server:

To run the server:

1.	Open a terminal and change directory to where the Server.class is located.

2.	Compile the Java file if not yet compiled:

a.	javac Server.java

3.	Run the server:

a.	java Server

4.	The server assumes that port 5000 is available in the computer and is not being used by any other program otherwise the server will crash.

To run a client:

1.	Open a terminal and change directory to where the Client.class is located.

2.	Compile the Java file if not yet compiled:

a.	javac Client.java

3.	Run the client:

a.	Java Client

4.	The client will ask for the IP address of the server in which it will try to connect. If the server is not online, the client will terminate.

5.	If the client successfully connects to the server, the client is provided an option whether it will be controlled by a bot or a human player.

6.	After which, the client has the option to either host or join a game.

7.	Hosting a game requires the client to enter the number of players and the game will wait until the number of client who joins the game is met.

Program Architecture

Both the client and the server is made through Java utilizing the sockets. The server and the client is tightly configured to work only between the two programs. Meaning to say, any other client program cannot use the server and any other client cannot be used by the server. Both the client and the server are programmed to communicate strictly within their protocol.
The server accepts every new client as a separate thread. The connected client will request to either host or join a game. If the client requests to host a game, a separate game thread is created in which other clients will join at a later time. When a client requests to join a game, the server will find an available game and joins the client. 
The game thread is initially in a wait state until the required number of players are met and then the game thread controls the flow on which player goes next and decides whether the game is over or not. The game thread sends a command to a client whether it’s their time to make a move and wait for a response before moving on making other clients move. The cycle goes on until all responses received from a client are in “blocked” states. The game thread dies when the game is over.
A client starts with either hosting or joining a game. Either way, when the game starts the client is handled with 2 threads. The main thread to send messages to the server and a listener thread to listen for messages from the server. These threads are coordinated to conform to the rules of the game. The main thread handles user actions (click of buttons) and send the details to the server. The listener thread will wait for messages coming from server and respond to it such as when there is a new move from other clients and needs to put it colors to the buttons.
In order to control race conditions and concurrency problems in the server, a semaphore is used. The semaphore makes sure that the exact number of players joins the game. Moreover, the Game thread controls the coordination of cycle of player moves forcing other players to wait and only make the current player move.
To ensure the correctness of the program, it has been played a multiple times and observed that all responses are correct. (Black box testing)
