package Go;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Client extends JFrame implements ActionListener, Runnable {

    private static final int SERVER_PORT = 5000;
    private static BufferedReader fromServer = null;
    private static PrintWriter toServer = null;

    private static String playerColor;

    private JButton[][] buttons = new JButton[6][10];
    private JLabel messageLabel = new JLabel("...");

    private boolean doubleMoveCardUsed = false;
    private boolean replacementCardUsed = false;
    private boolean freedomCardUsed = false;

    private JButton doubleMoveCardButton = new JButton("Use Double Move Card");
    private JButton replacementCardButton = new JButton("Use Replacement Card");
    private JButton freedomCardButton = new JButton("Freedom Card Button");

    private boolean doubleMoveApplied = false;
    private boolean replacementCardApplied = false;
    private boolean freedomCardApplied = false;

    private boolean cardInUse = false;

    private static boolean isBot = true;
    private static Random random = new Random();

    // Intiialize the user interface
    public Client() {
        // Set window layout and properties
        setTitle("Player " + playerColor);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Add cards for the player
        JPanel cardsPanel = new JPanel(new FlowLayout());
        add(BorderLayout.NORTH, cardsPanel);

        cardsPanel.add(messageLabel);

        doubleMoveCardButton.addActionListener(this);
        replacementCardButton.addActionListener(this);
        freedomCardButton.addActionListener(this);;

        cardsPanel.add(doubleMoveCardButton);
        cardsPanel.add(replacementCardButton);
        cardsPanel.add(freedomCardButton);

        JPanel buttonsPanel = new JPanel(new GridLayout(buttons.length, buttons[0].length));
        add(BorderLayout.CENTER, buttonsPanel);

        // Set the buttons
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                buttons[row][col] = new JButton();
                buttons[row][col].addActionListener(this);
                buttonsPanel.add(buttons[row][col]);
            }
        }

        disableAllButtons();

        // Start a thread that listens to server messages
        new Thread(this).start();
    }

    // Disable all buttons, not allowing players to click
    private void disableAllButtons() {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                buttons[row][col].setEnabled(false);
            }
        }

        doubleMoveCardButton.setEnabled(false);
        replacementCardButton.setEnabled(false);
        freedomCardButton.setEnabled(false);
    }

    // Color a button, making it disabled
    private void colorButton(int row, int col, String colorName) {
        if (colorName.equals("RED")) {
            buttons[row][col].setBackground(Color.RED);
        } else if (colorName.equals("GREEN")) {
            buttons[row][col].setBackground(Color.GREEN);
        } else if (colorName.equals("BLUE")) {
            buttons[row][col].setBackground(Color.BLUE);
        } else if (colorName.equals("YELLOW")) {
            buttons[row][col].setBackground(Color.YELLOW);
        } else if (colorName.equals("ORANGE")) {
            buttons[row][col].setBackground(Color.ORANGE);
        }

        buttons[row][col].setText(colorName.charAt(0) + "");
        buttons[row][col].setEnabled(false);
    }

    // Enable only buttons that aren't colored
    private void enableButtons() {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                if (buttons[row][col].getText().isEmpty()) {
                    buttons[row][col].setEnabled(true);
                }
            }
        }

        doubleMoveCardButton.setEnabled(!doubleMoveCardUsed);
        replacementCardButton.setEnabled(!replacementCardUsed);
        freedomCardButton.setEnabled(!freedomCardUsed);
    }

    // Given a row and column, check its sorroundings if it has a free unused button
    private boolean hasFreeAdjacentButton(int row, int col) {
        int[] adjacentRows = {-1, -1, +0, +1, +1, +1, +0, -1};
        int[] adjacentCols = {+0, +1, +1, +1, +0, -1, -1, -1};

        for (int i = 0; i < adjacentRows.length; i++) {
            int adjacentRow = row + adjacentRows[i];
            int adjacentCol = col + adjacentCols[i];

            if (adjacentRow >= 0 && adjacentRow < buttons.length
                    && adjacentCol >= 0 && adjacentCol < buttons[row].length
                    && buttons[adjacentRow][adjacentCol].getText().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    // Check if player is blocked
    private boolean isBlocked() {
        // Find a free adjacent cell
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                if (!buttons[row][col].getText().isEmpty()
                        && buttons[row][col].getText().charAt(0) == playerColor.charAt(0)) {
                    if (hasFreeAdjacentButton(row, col)) {
                        return false;
                    }
                }
            }
        }

        if (!replacementCardUsed) {
            return false;
        }

        if (!freedomCardUsed) {
            // A freedom card can only be used on non-empty cell
            for (int row = 0; row < buttons.length; row++) {
                for (int col = 0; col < buttons[row].length; col++) {
                    if (buttons[row][col].getText().isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // Handle button clicks
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == doubleMoveCardButton || e.getSource() == replacementCardButton || e.getSource() == freedomCardButton) {
            if (cardInUse) {
                JOptionPane.showMessageDialog(this, "You still have a card in use to be applied.");
                return;
            }
        }

        if (e.getSource() == doubleMoveCardButton) {
            doubleMoveCardUsed = true;
            doubleMoveApplied = true;
            messageLabel.setText("Double move card applied.");
            doubleMoveCardButton.setEnabled(false);
            return;
        }

        if (e.getSource() == freedomCardButton) {
            freedomCardUsed = true;
            freedomCardApplied = true;
            messageLabel.setText("Freedom card applied.");
            freedomCardButton.setEnabled(false);
            return;
        }

        if (e.getSource() == replacementCardButton) {
            replacementCardUsed = true;
            replacementCardApplied = true;
            messageLabel.setText("Replacement card applied.");
            replacementCardButton.setEnabled(false);

            // Enable all buttons that isn't the same color as this player
            // Which they can be replaced
            for (int row = 0; row < buttons.length; row++) {
                for (int col = 0; col < buttons[row].length; col++) {
                    if (!buttons[row][col].getText().equals(playerColor.charAt(0) + "")) {
                        buttons[row][col].setEnabled(true);
                    }
                }
            }

            return;
        }

        // It's a regular button pressed, handle it
        JButton button = (JButton) e.getSource();

        // Find the row and column of the entered button and send it to server
        int rowMove = -1;
        int colMove = -1;

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                if (buttons[row][col] == button) {
                    rowMove = row;
                    colMove = col;
                    break;
                }
            }
        }

        // Validate the move, check if it has an adjacent color that is the same as the player's color
        boolean validMove = false;

        if (freedomCardApplied && buttons[rowMove][colMove].getText().isEmpty()) {
            validMove = true;
        } else {
            int[] adjacentRows = {-1, -1, +0, +1, +1, +1, +0, -1};
            int[] adjacentCols = {+0, +1, +1, +1, +0, -1, -1, -1};

            for (int i = 0; i < adjacentRows.length; i++) {
                int adjacentRow = rowMove + adjacentRows[i];
                int adjacentCol = colMove + adjacentCols[i];

                if (adjacentRow >= 0 && adjacentRow < buttons.length
                        && adjacentCol >= 0 && adjacentCol < buttons[rowMove].length) {
                    if ((!buttons[adjacentRow][adjacentCol].getText().isEmpty() || replacementCardApplied)
                            && buttons[adjacentRow][adjacentCol].getText().equals(playerColor.charAt(0) + "")) {
                        validMove = true;
                        break;
                    }
                }
            }
        }

        // Player is done, move is assigned to next player (or to same player if double move)
        if (validMove) {
            if (doubleMoveApplied) {
                toServer.println(rowMove + " " + colMove + " DOUBLEMOVE");
            } else {
                toServer.println(rowMove + " " + colMove);
            }

            doubleMoveApplied = false;
            replacementCardApplied = false;
            freedomCardApplied = false;
            cardInUse = false;
            disableAllButtons();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid move.");
        }
    }

    // Called only when the bot is enabled
    private void makeBotMove() throws Exception {
        // Simulate thinking
        Thread.sleep(1000);

        // Get all available clickable buttons
        List<JButton> validButtons = new ArrayList<>();
        int[] adjacentRows = {-1, -1, +0, +1, +1, +1, +0, -1};
        int[] adjacentCols = {+0, +1, +1, +1, +0, -1, -1, -1};

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                if (buttons[row][col].getText().equals(playerColor.charAt(0) + "")) {
                    for (int i = 0; i < adjacentRows.length; i++) {
                        int adjacentRow = row + adjacentRows[i];
                        int adjacentCol = col + adjacentCols[i];

                        if (adjacentRow >= 0 && adjacentRow < buttons.length
                                && adjacentCol >= 0 && adjacentCol < buttons[row].length
                                && buttons[adjacentRow][adjacentCol].getText().isEmpty()) {
                            validButtons.add(buttons[adjacentRow][adjacentCol]);
                        }
                    }
                }
            }
        }

        // If there are available buttons, randomly choose one
        if (!validButtons.isEmpty()) {
            // Attempt to do a double move if we can
            if (validButtons.size() >= 2 && !cardInUse && !doubleMoveCardUsed && random.nextBoolean()) {
                System.out.println("DOUBLE MOVE CARD USED!");
                doubleMoveCardButton.doClick();
            }

            // Send the move to the server
            JButton button = validButtons.get(random.nextInt(validButtons.size()));
            button.doClick();
            return;
        }

        // If there are no valid buttons, try a replacement card
        if (!replacementCardUsed) {
            // Get all replaceable buttons adjacent to this player
            validButtons.clear();

            for (int row = 0; row < buttons.length; row++) {
                for (int col = 0; col < buttons[row].length; col++) {
                    if (buttons[row][col].getText().equals(playerColor.charAt(0) + "")) {
                        for (int i = 0; i < adjacentRows.length; i++) {
                            int adjacentRow = row + adjacentRows[i];
                            int adjacentCol = col + adjacentCols[i];

                            if (adjacentRow >= 0 && adjacentRow < buttons.length
                                    && adjacentCol >= 0 && adjacentCol < buttons[row].length
                                    && !buttons[adjacentRow][adjacentCol].getText().equals(buttons[row][col].getText())) {
                                validButtons.add(buttons[adjacentRow][adjacentCol]);
                            }
                        }
                    }
                }
            }

            if (!validButtons.isEmpty()) {
                System.out.println("REPLACEMENT CARD USED");
                replacementCardButton.doClick();

                // Send the move to the server
                JButton button = validButtons.get(random.nextInt(validButtons.size()));
                button.doClick();
                return;
            }
        }

        // If there are no valid buttons, try a freedom card
        if (!freedomCardUsed) {
            // Get all clickable free buttons
            validButtons.clear();

            for (int row = 0; row < buttons.length; row++) {
                for (int col = 0; col < buttons[row].length; col++) {
                    if (buttons[row][col].getText().isEmpty()) {
                        validButtons.add(buttons[row][col]);
                    }
                }
            }

            if (!validButtons.isEmpty()) {
                System.out.println("FREEDOM CARD USED!");
                freedomCardButton.doClick();

                // Send the move to the server
                JButton button = validButtons.get(random.nextInt(validButtons.size()));
                button.doClick();

                return;
            }
        }

        System.out.println("CRAP!");
    }

    // Listen for server 
    @Override
    public void run() {
        try {
            while (true) {
                String message = fromServer.readLine();
                System.out.println(message);

                Scanner scanner = new Scanner(message);
                String command = scanner.next();

                // Respond to the UI the server's message
                if (command.equals("DISPLAYMOVE")) {
                    int row = scanner.nextInt();
                    int col = scanner.nextInt();
                    String color = scanner.next();
                    colorButton(row, col, color);
                    messageLabel.setText(color + " placed!");
                } else if (command.equals("MAKEMOVE")) {
                    if (isBlocked()) {
                        // Tell server we're blocked so we skip
                        toServer.println("BLOCKED");
                        messageLabel.setText("You are blocked!");
                    } else {
                        enableButtons();
                        messageLabel.setText("Your Turn!");

                        if (isBot) {
                            makeBotMove();
                        }
                    }
                } else if (command.equals("GAMEOVER")) {
                    messageLabel.setText("Game over!");
                    disableAllButtons();
                    JOptionPane.showMessageDialog(this, "Winner: " + scanner.nextLine());
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    // Join the server 
    public static void main(String[] args) throws Exception {
        // Connect to the server first

        String serverIP = JOptionPane.showInputDialog("Enter server's IP Address: ");

        if (serverIP == null) {
            return;
        }

        Socket s = new Socket(serverIP, SERVER_PORT);
        fromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
        toServer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);

        // Ask client whether to host or join a game
        String[] options = {"Host Game", "Join Game", "Cancel"};
        int option = JOptionPane.showOptionDialog(null, "What would you like to do?", "Options", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        isBot = JOptionPane.showConfirmDialog(null, "Would you like this client to be a bot?") == JOptionPane.YES_OPTION;

        if (option == 0) {
            // Host a game
            String response = JOptionPane.showInputDialog("Enter the number of player (2 to 5): ");

            if (response == null) {
                return;
            }

            // Set the number of players
            int numPlayers;

            try {
                numPlayers = Integer.parseInt(response);

                if (numPlayers < 2 || numPlayers > 5) {
                    throw new Exception();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Invalid number of players.");
                return;
            }

            // Tell server to host a game, then get our starting color
            toServer.println("HOSTGAME " + numPlayers);
        } else if (option == 1) {
            // Joining a game, try and look for availability
            toServer.println("JOINGAME");
        } else {
            return;
        }

        // Get starting color assigned
        playerColor = fromServer.readLine();

        if (playerColor.equals("false")) {
            // False is returned to players who join games but there are no hosted games available
            JOptionPane.showMessageDialog(null, "No games available.");
            s.close();
            return;
        }

        JOptionPane.showMessageDialog(null, "Your assigned color is " + playerColor);

        // Start the client's user interface
        Client client = new Client();
        client.setVisible(true);
    }
}
