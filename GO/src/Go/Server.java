package Go;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Server {

    private static final int PORT = 5000;

    private static List<Game> games = new ArrayList<>();

    // Entry point of the program
    public static void main(String[] args) throws Exception {
        // Start the server
        System.out.println("Starting server on port " + PORT + "...");
        ServerSocket ss = new ServerSocket(PORT);

        while (true) {
            try {
                System.out.println("Listening to clients...");
                Socket s = ss.accept();

                // Handle the client as a separate thread
                new ClientHandler(s).start();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Handle an individual client (1 thread is to 1 client)
    private static class ClientHandler extends Thread {

        private Socket s;
        private BufferedReader fromClient;
        private PrintWriter toClient;

        // Create the client handler
        public ClientHandler(Socket s) {
            this.s = s;
        }

        // Read a message from this client
        public String readMessage() throws Exception {
            return fromClient.readLine();
        }

        // Send a message to this client
        public void sendMessage(String message) throws Exception {
            toClient.println(message);
        }

        // Start processing a client request
        @Override
        public void run() {
            try {
                // Initialize communication objects to allow send and receive messages
                fromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
                toClient = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);

                // A client either joins a game or hosts a game
                String message = readMessage();
                Scanner scanner = new Scanner(message);
                String command = scanner.next();

                if (command.equals("HOSTGAME")) {
                    // Start a new game setting the number of players
                    int numPlayers = scanner.nextInt();
                    Game game = new Game(numPlayers);
                    games.add(game);
                    System.out.println("A client hosted a new game for " + numPlayers + " players.");
                    game.joinGame(this);

                    // Start the game as a separate thread
                    game.start();
                } else if (command.equals("JOINGAME")) {
                    // Find a game to join
                    for (Game someGame : games) {
                        if (someGame.joinGame(this)) {
                            return;
                        }
                    }

                    // Disconnect client if there is no game found
                    sendMessage("false");
                    s.close();
                }

                // We're done, the game thread will handle the flow of the game
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    // A game represents a play between 2 to 5 players
    // This game contains only the state of the board, it does not
    // perform any validation. The validation of the game happens on the
    // client.
    private static class Game extends Thread {

        private static final String COLORS[] = {"RED", "GREEN", "BLUE", "YELLOW", "ORANGE"};
        private static final int MAX_PLAYERS = 5;

        private int maxPlayers;
        private int numPlayers;
        private String[][] board = new String[6][10];
        private ClientHandler[] players = new ClientHandler[MAX_PLAYERS];
        private int[] scores = new int[MAX_PLAYERS];
        private boolean[] blocked = new boolean[MAX_PLAYERS];

        private Semaphore semaphore = new Semaphore(0);

        // Create a new game initializing the number of players
        public Game(int maxPlayers) {
            this.maxPlayers = maxPlayers;

            for (int row = 0; row < board.length; row++) {
                for (int col = 0; col < board[row].length; col++) {
                    board[row][col] = "";
                }
            }

            numPlayers = 0;
        }

        // Check if all playere are blocked
        private boolean areAllBlocked() {
            for (int i = 0; i < numPlayers; i++) {
                if (!blocked[i]) {
                    return false;
                }
            }

            return true;
        }

        // Handles the flow of the game
        @Override
        public void run() {
            // Wait until all players are filled
            try {
                semaphore.acquire(maxPlayers);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }

            System.out.println("A game has players filled, starting new game...");
            int currentPlayer = 0;

            try {
                Random random = new Random();

                // Place the initial random letters to each player in the board
                for (int i = 0; i < numPlayers; i++) {
                    int row = random.nextInt(board.length);
                    int col = random.nextInt(board[row].length);

                    while (!board[row][col].isEmpty()) {
                        row = random.nextInt(board.length);
                        col = random.nextInt(board[row].length);
                    }

                    board[row][col] = COLORS[i];

                    for (int j = 0; j < numPlayers; j++) {
                        players[j].sendMessage("DISPLAYMOVE " + row + " " + col + " " + COLORS[i]);
                    }
                }

                for (int i = 0; i < blocked.length; i++) {
                    blocked[i] = false;
                }

                while (!areAllBlocked()) {
                    // Signal the next player to make a move
                    players[currentPlayer].sendMessage("MAKEMOVE");

                    // Receive a row and target column where to place the letter
                    String message = players[currentPlayer].readMessage();

                    if (!message.equals("BLOCKED")) {
                        blocked[currentPlayer] = false;

                        // Regular move
                        Scanner scanner = new Scanner(message);
                        int row = scanner.nextInt();
                        int col = scanner.nextInt();

                        board[row][col] = COLORS[currentPlayer];
                        scores[currentPlayer]++;

                        // Broadcast the move to all clients to get an update of the move
                        for (int i = 0; i < numPlayers; i++) {
                            players[i].sendMessage("DISPLAYMOVE " + row + " " + col + " " + COLORS[currentPlayer]);
                        }

                        if (scanner.hasNext() && scanner.next().equals("DOUBLEMOVE")) {
                            // Make the player move again if it is double card
                            currentPlayer--;
                        }
                    } else {
                        blocked[currentPlayer] = true;
                    }

                    // Move to the next player
                    currentPlayer++;
                    currentPlayer %= numPlayers;
                }

                // Find the highest scoring color
                int highestScore = -1;

                for (int i = 0; i < numPlayers; i++) {
                    if (scores[i] > highestScore) {
                        highestScore = scores[i];
                    }
                }

                // Respond a game over and the winners
                String winners = "";

                for (int i = 0; i < numPlayers; i++) {
                    if (scores[i] == highestScore) {
                        winners = COLORS[i];
                    }
                }

                for (int i = 0; i < numPlayers; i++) {
                    players[i].sendMessage("GAMEOVER " + winners);
                }
            } catch (Exception e) {
            }
        }

        // Return the assigned color of a client
        public String getPlayerColor(ClientHandler client) {
            for (int i = 0; i < numPlayers; i++) {
                if (players[i] == client) {
                    return COLORS[i];
                }
            }

            return null;
        }

        // Make a client join this game
        public synchronized boolean joinGame(ClientHandler client) throws Exception {
            if (numPlayers >= maxPlayers) {
                return false;
            }

            // Assign a color for the client
            players[numPlayers] = client;
            numPlayers++;

            // Send the color of the client
            client.sendMessage(COLORS[numPlayers - 1]);
            semaphore.release();

            return true;
        }
    }
}
