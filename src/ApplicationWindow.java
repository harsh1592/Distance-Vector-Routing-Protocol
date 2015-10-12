import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;
import javax.swing.*;

/**
 * Application window to run each router in
 * 
 * @author Harsh Patil
 *
 */
public class ApplicationWindow {
	JTextArea myArea;
	JTextArea myInput;

	// set window title
	ApplicationWindow(String title) {

	// Creating and setting the window for each router
	JFrame frame = new JFrame(title);
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	// add a text area to the window for display
	myArea = new JTextArea(20, 50);
	myArea.setEditable(false);
	JScrollPane scrollPane = new JScrollPane(myArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

	frame.setLayout(new GridLayout(2, 1));
	frame.getContentPane().add(scrollPane);

	//add another text area for input
	myInput = new JTextArea(20, 20);
	frame.getContentPane().add(myInput);
	// Display the window.
	frame.pack();
	frame.setVisible(true);
}	

	/**
	 * gets the input from the input text area for the router
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public String getInput() throws InterruptedException {
		final CountDownLatch CDlatch = new CountDownLatch(1);
		KeyEventDispatcher kEdispatcher = new KeyEventDispatcher() {
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					CDlatch.countDown();
				return false;
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(kEdispatcher);
		CDlatch.await();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(kEdispatcher);
		String input = myInput.getText();
		myInput.setCaretPosition(0);
		myInput.setText("");
		return input;
	}

	// Methods to print string on to the text area
	public void print(String s) {
		myArea.append(s);
		myArea.setCaretPosition(myArea.getDocument().getLength());
	}

	public void println(String s) {
		print(s + "\n");
	}

	public void println() {
		print("\n");
	}

}
