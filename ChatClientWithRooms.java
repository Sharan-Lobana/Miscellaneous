import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatClientWithRooms
{
	BufferedReader in;
	PrintWriter out;

	JFrame frame = new JFrame("ChatApplication");
	JTextField textField = new JTextField(40);
	JTextArea messageArea = new JTextArea(8, 40);

	public ChatClientWithRooms()
	{
		textField.setEditable(false);
		messageArea.setEditable(false);
		frame.getContentPane().add(textField, "North");
		frame.getContentPane().add(new JScrollPane(messageArea), "Center");
		frame.pack();

		textField.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				out.println(textField.getText());
				textField.setText("");
			}
		});
	}

	private String getServerAddress()
	{
		return JOptionPane.showInputDialog(
			frame,
			"Enter IP Address of the server:",
			"Welcome to the ChatApplication",
			JOptionPane.QUESTION_MESSAGE);
	}

	private String getChatRoomName()
	{
		return JOptionPane.showInputDialog(
			frame,
			"Choose the name of chat room:",
			"Chat room name selection",
			JOptionPane.PLAIN_MESSAGE);
	}

	private String getName()
	{
		return JOptionPane.showInputDialog(
			frame,
			"Choose a screen name:",
			"Screen name selection",
			JOptionPane.PLAIN_MESSAGE);
	}

	private void run() throws IOException, SocketException
	{
		String serverAddress;
		Socket socket;
		while(true)
		{
			try
			{
				serverAddress = getServerAddress();
				socket = new Socket(serverAddress, 9001);
				break;
			}
			catch(SocketException e)
			{
				System.out.println(e);
				System.out.println("The host name entered may be invalid");
				// return;
			}
			catch(Exception e2)
			{
				System.out.println(e2);
				// return;
			}
		}
		
		
		in = new BufferedReader(new InputStreamReader(
			socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);

		while(true)
		{
			String line = in.readLine();

			if(line.startsWith("SUBMITCHATROOM"))
			{
				out.println(getChatRoomName());
			}
			if(line.startsWith("SUBMITNAME"))
			{
				out.println(getName());
			}
			else if(line.startsWith("NAMEACCEPTED"))
			{
				textField.setEditable(true);
			}
			else if(line.startsWith("MESSAGE"))
			{
				messageArea.append(line.substring(8) + "\n");
			}
			else if(line.startsWith("NOTIFICATION"))
			{
				messageArea.append(line.substring(13) + "\n");
			}
		}
	}

	public static void main(String [] args) throws Exception
	{
		ChatClientWithRooms client = new ChatClientWithRooms();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}
}


