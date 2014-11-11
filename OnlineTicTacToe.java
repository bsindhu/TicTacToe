/** OnlineTicTacToe.java
 * 
 *  This is an online Tic-Tac-Toe game played between two users.
 *  The game uses the same window to display the ongoing game.
 *  Displays another window if the users would like to continue 
 *  playing after the current game is over.
 * @ author Sindhuri Bolisetty 
 * @ version 1/30/2014
 */


import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class OnlineTicTacToe implements ActionListener {
	private final int INTERVAL = 1000; // 1 second
	private final int NBUTTONS = 9; // #buttons
	private ObjectInputStream input = null; // input from my counterpart
	private ObjectOutputStream output = null; // output from my counterpart
	private JFrame window = null; // the tic-tac-toe window
	private JButton[] button = new JButton[NBUTTONS]; // button[0] - button[9]
	private boolean[] myTurn = new boolean[1]; // T: my turn, F: your turn
	private String myMark = null; // "O" or "X"
	private String yourMark = null; // "X" or "O"


	/**
	 * Prints out the usage.
	 */
	private static void usage( ) {
		System.err.println( "Usage: java OnlineTicTacToe ipAddr ipPort(>=5000)" );
		System.exit( -1 );
	}

	/**
	 * Prints out the stack trace upon a given error and quits the application.
	 * @param an exception
	 */
	private static void error( Exception e ) {
		e.printStackTrace();
		System.exit(-1);
	}

	/**
	 * Starts the online tic-tac-toe game.
	 * @param args[0]: my counterpart's ip address, args[0]: his/her port
	 */
	public static void main( String[] args ) {
		// verify the number of arguments
		if ( args.length != 2 ) {
			usage( );
		}

		// verify the correctness of my counterpart address
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName( args[0] );
		} catch ( UnknownHostException e ) {
			error( e );
		}

		// verify the correctness of my counterpart port
		int port = 0;
		try {
			port = Integer.parseInt( args[1] );
		} catch (NumberFormatException e) {
			error( e );
		}
		if ( port < 5000 ) {
			usage( );
		}

		// now start the application
		OnlineTicTacToe game = new OnlineTicTacToe( addr, port );
	}

	/**
	 * Is the constructor that sets up a TCP connection with my counterpart,
	 * brings up a game window, and starts a slave thread for listening to
	 * my counterpart.
	 * @param my counterpart's ip address
	 * @param my counterpart's port
	 */
	public OnlineTicTacToe( InetAddress addr, int port ) {
		
		// set up a TCP connection 
		boolean amServer = connection( addr, port );

		// set up a window
		makeWindow( !amServer ); // or makeWindow( false );

		// start my counterpart thread
		Counterpart counterpart = new Counterpart( );
		counterpart.start();
	}

	//establish TCP connection between
	private boolean connection( InetAddress addr, int port ) {
		ServerSocket serverSocket = null;
		boolean amServer = false;
		try {
			serverSocket = new ServerSocket( port );

			// Disable blocking I/O operations, by specifying a timeout
			serverSocket.setSoTimeout( INTERVAL );
		} catch (Exception e) {
			// exception if ServerSocket already established, which means I am
			// the client
		}
		Socket socket = null;
		while (true) {
			try {
				socket = serverSocket.accept();
			}  catch (SocketTimeoutException ste) {
				// non-blocking accept 
			}catch (IOException ioe) {
				error(ioe);
			}
			if (socket != null) {
				
				// peer has connected
				amServer = true;
				break;
			}

			// continue while loop until peer has been accepted
			try {
				if (socket == null)
					socket = new Socket( addr, port );
			} catch (IOException ioe) {}
			if (socket == null)
				continue;
			amServer = false;
			break;
		}
		System.out.println("amServer = " + amServer); 
		try {
			if (amServer) {

				// if you are the server then read the input stream
				input = new ObjectInputStream(socket.getInputStream());
				output = new ObjectOutputStream(socket.getOutputStream());
				System.out.println((String)input.readObject());
			} else {

				// if you are the client then write to output stream
				output = new ObjectOutputStream(socket.getOutputStream());
				input = new ObjectInputStream(socket.getInputStream());
				output.writeObject("Hello!"); 
			}
		} catch (Exception e) {
			error(e);
		}     
		return amServer;
	}

	/**
	 * Creates a 3x3 window for the tic-tac-toe game
	 * @param true if this window is created by the 1st player, false by
	 * the 2nd player
	 */
	private void makeWindow( boolean amFormer ) {
		myTurn[0] = amFormer;
		myMark = ( amFormer ) ? "O" : "X"; // 1st person uses "O"
		yourMark = ( amFormer ) ? "X" : "O"; // 2nd person uses "X"

		// create a window
		window = new JFrame("OnlineTicTacToe(" +
				((amFormer) ? "former)" : "latter)" ) + myMark );

		window.setSize(300, 300);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setLayout(new GridLayout(3, 3));

		// initialize all nine cells.
		for (int i = 0; i < NBUTTONS; i++) {
			button[i] = new JButton();
			window.add(button[i]);
			button[i].addActionListener(this);
		}

		// make it visible
		window.setVisible(true);
	}

	/**
	 * Marks the i-th button with mark ("O" or "X")
	 * @param the i-th button
	 * @param a mark ( "O" or "X" )
	 * @param true if it has been marked in success
	 */
	private boolean markButton( int i, String mark ) {
		if ( button[i].getText( ).equals( "" ) ) {
			button[i].setText( mark );
			button[i].setEnabled( false );
			return true;
		}
		return false;
	}

	/**
	 * Checks which button has been clicked
	 * @param an event passed from AWT
	 * @return an integer (0 through to 8) that shows which button has been
	 * clicked. -1 upon an error.
	 */
	private int whichButtonClicked( ActionEvent event ) {
		for ( int i = 0; i < NBUTTONS; i++ ) {
			if ( event.getSource( ) == button[i] )
				return i;
		}
		return -1;
	}

	/**
	 * Checks if the i-th button has been marked with mark( "O" or "X" ).
	 * @param the i-th button
	 * @param a mark ( "O" or "X" )
	 * @return true if the i-th button has been marked with mark.
	 */
	private boolean buttonMarkedWith( int i, String mark ) {
		return button[i].getText( ).equals( mark );
	}

	/**
	 * Pops out another small window indicating that mark("O" or "X") Won!
	 * @param a mark ( "O" or "X" )
	 */
	private void showWon( String mark ) {
		JOptionPane.showMessageDialog( null, mark + " Won!" );
	}

	/**
	 * Is called by AWT whenever any button has been clicked. You have to:
	 * <ol>
	 * <li> check if it is my turn,
	 * <li> check which button was clicked with whichButtonClicked( event ),
	 * <li> mark the corresponding button with markButton( buttonId, mark ),
	 * <li> check which button was clicked with whichButtonClicked( event ),
	 * <li> mark the corresponding button with markButton( buttonId, mark ),
	 * <li> send this information to my counterpart,
	 * <li> checks if the game was completed with
	 * buttonMarkedWith( buttonId, mark )
	 * <li> shows a winning message with showWon( )
	 */
	@Override
	public void actionPerformed( ActionEvent event ) {
		int i = whichButtonClicked( event );
		if ( markButton( i, myMark ) ) {
			try {
				output.writeInt(i);
				output.flush();
				System.out.println("wrote " + i + " to counterpart");
			} catch (Exception e) {
				System.exit(0);
			} 
		} 
		boolean isGameover = checkGame(myMark);

		// notify counterpart after my move
		synchronized (myTurn) {
			if(isGameover){
				playAgain();
			}
			myTurn[0] = false;
			myTurn.notify();
		}
	}

	//checks if any of the player has won the game.
	private boolean checkGame(String player) {

		// diagonal 0,4,8
		if (buttonMarkedWith(0, player) && buttonMarkedWith(4, player) && buttonMarkedWith(8, player)) {
			showWon(player);
			return true;
		}
		//diagonal 2,4,6
		if (buttonMarkedWith(2, player) && buttonMarkedWith(4, player) && buttonMarkedWith(6, player)) {
			showWon(player);
			return true;
		}

		//horizontal 0,1,2 or 3,4,5 or 6,7,8
		for (int i = 0; i < 7; i = i + 3) {
			if (buttonMarkedWith(i, player) && buttonMarkedWith(i + 1, player) && buttonMarkedWith(i + 2, player)) {
				showWon(player);
				return true;
			}
		}

		//vertical 0,3,6 or 1,4,7 or 2,5,8
		for (int i = 0; i < 3; i++) {
			if (buttonMarkedWith(i, player) && buttonMarkedWith(i + 3, player) && buttonMarkedWith(i + 6, player)) {
				showWon(player);
				return true;
			}
		}

		// checks the case for tie between the players
		for (int i = 0; i < NBUTTONS; i++) {
			if (button[i].getText().equals("")) break;
			if (i == NBUTTONS - 1) {
				JOptionPane.showMessageDialog( null, "Tie!!");
				return true;
			}
		}
		return false;
	}

	/*
	 * Shows a dialog box to user if they want to continue playing the game.
	 */
	
	private void playAgain()
	{
		int input = JOptionPane.showConfirmDialog(null, "Want to Play again?",
				"Game Over", JOptionPane.YES_NO_OPTION);
		
		//if clicked yes option play the game else exit the system.
		if (input == JOptionPane.YES_OPTION)
		{
			for(int i = 0; i < NBUTTONS; i++){
				button[i].setText( "" );
				button[i].setEnabled( true );
			}
		}
		else if(input == JOptionPane.NO_OPTION)
		{
			JOptionPane.showMessageDialog(null, "Thanks for playing");
			System.exit(0);
		}
	}
	
	/**
	 * This is a reader thread that keeps reading from and behaving as my
	 * counterpart.
	 */

	private class Counterpart extends Thread {

		@Override
		public void run( ) {
			while (true)
				try {
					synchronized (myTurn) {

						// spins until I make the move.
						if (myTurn[0]) continue;

						//disables counterpart buttons.
						for (int i = 0; i < NBUTTONS; i++) {
							if (button[i].getText().equals(""))
								button[i].setEnabled(false);
						}
						System.out.println("waiting for counterpart...");
						int i = input.readInt();

						// blocked until counterpart writes to input stream
						System.out.println("counterpart's position = " + i);
						markButton(i, yourMark);
						boolean isGameOver = checkGame(yourMark);
						try {
							if (!isGameOver) {

								//enables my counterpart buttons.
								for (int j = 0; j < NBUTTONS; j++) {
									if (button[j].getText().equals(""))
										button[j].setEnabled(true);
								}
							}
							if(isGameOver){
								playAgain();
							}
							myTurn.wait();
						} catch (Exception e) {
							error(e);
						}
					}
				} catch(EOFException eof){
					JOptionPane.showMessageDialog(null, "counterpart denied from playing again");
					System.out.println("counterpart disconnected.");
					System.exit(0);
				}
			catch (Exception e) {
				error(e);
			}
		}
	}
}
