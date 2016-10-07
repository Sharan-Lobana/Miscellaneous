import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.HashMap;


public class ChatServerWithRooms
{

	public static final int ROOM_CAPACITY = 3;
	//The listening port number
	private static final int PORT = 9001;

	private static HashMap<String, HashSet<String>> chatroomnames = new HashMap<String, HashSet<String>>();
	// private static HashSet<String> names = new HashSet<String>();

	private static HashMap<String, HashSet<PrintWriter>> chatroomwriters = new HashMap<String, HashSet<PrintWriter>>();

	public static void main(String [] args) throws Exception
	{
		System.out.println("The chat server is running");
		ServerSocket listener = new ServerSocket(PORT);

		try
		{
			while(true)
			{
				new Handler(listener.accept()).start();
				System.out.println("New connection to chat server");
			}
		}
		finally
		{
			listener.close();
		}
	} 


	private static class Handler extends Thread
	{
		private String chatroom;
		private String name;
		private Socket socket;
		private BufferedReader in;
		private PrintWriter out;

		public Handler(Socket socket)
		{
			this.socket = socket;
		}

		public void run()
		{
			try 
			{
				in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				HashSet<String> membernames;
				//Ask for the chat room the client wants to connect to
				while(true)
				{
					out.println("SUBMITCHATROOM");
					chatroom = in.readLine();
					if(chatroom == null)
						return;

					synchronized (chatroomnames)
					{
						membernames = chatroomnames.get(chatroom);
						if(membernames == null)
						{
							membernames = new HashSet<String>();
							System.out.println("NEW CHAT ROOM CREATED: "+ chatroom + "\n");
							break;
						}
						else if(membernames.size() >= ROOM_CAPACITY)
						{
							System.out.println("THE CHAT ROOM: " + chatroom + " is full. Connection refused.\n");
							//Don't break here, ask for another chat room name
						}
						else
						{
							System.out.println("NEW CONNECTION ATTEMPT FOR CHAT ROOM: " + chatroom + "\n" );
							break;
						}
					}
				}

				while(true)
				{
					out.println("SUBMITNAME");
					name = in.readLine();
					if(name == null)
						return;
					synchronized (chatroomnames)
					{
						//Check if the chat room was newly created
						if(membernames.size() == 0)
						{
							//Add the member to the newly created chat room;
							membernames.add(name);
							System.out.println(name + " joined chatroom " + chatroom + "\n");
							//Add the chat room to the HashMap
							chatroomnames.put(chatroom, membernames);
 
							break;
						}
						//Check if someone else has joined the room meanwhile
						//and the capacity is full
						else if(chatroomnames.get(chatroom).size() >= ROOM_CAPACITY)
						{
							//Disconnect the lazy user from the server
							//He doesn't deserve to use the services of 
							//the given chat room
							System.out.println("THE CHAT ROOM: " + chatroom + " is full. Name Submission refused.\n");
							return;
						}
						else
						{
							//Check if the chatroom already contains the username
							if(!chatroomnames.get(chatroom).contains(name))
							{
								//Add the user to the given chatroom
								membernames = chatroomnames.get(chatroom);
								membernames.add(name);
								chatroomnames.put(chatroom, membernames);
								break;
							}
						}	
					}
				}

				//Add the socket's print writer to the 
				//set of all writers so this client can 
				//receive broadcast messages
				out.println("NAMEACCEPTED");

				synchronized (chatroomwriters)
				{
					HashSet<PrintWriter> writers = chatroomwriters.get(chatroom);

					//Check if the chatroom has been newly created
					if(writers == null)
					{
						writers = new HashSet<PrintWriter>();
						
					}

					//Add the  member to output list of chatroom
					writers.add(out);
					//Add the output list to hashmap
					chatroomwriters.put(chatroom, writers);

					for(PrintWriter writer : writers)
					{
						writer.println("NOTIFICATION:(" + name + " has joined the chatroom " + chatroom + ")");
					}
				}
				// writers.add(out);

				while(true)
				{
					String input = in.readLine();
					if(input == null)
					{
						return;
					}
					HashSet<PrintWriter> writers = chatroomwriters.get(chatroom);
					for (PrintWriter writer : writers)
					{
						writer.println("MESSAGE " + name + ":" + input);
					}
				}

			}
			catch (IOException e)
			{
				System.out.println(e);
			}
			finally
			{

				if(name != null)
				{
					synchronized (chatroomnames)
					{
						HashSet<String> membernames = chatroomnames.get(chatroom);
						membernames.remove(name);
						System.out.println(name + " left the chatroom " + chatroom + "\n");
						//Check if the room is empty
						if(membernames.size() == 0)
						{
							//Remove the chatroom from the Hashmap
							chatroomnames.remove(chatroom);
							System.out.println("Chatroom " + chatroom + " removed due to zero members.\n");
						}
						else
						{
							chatroomnames.put(chatroom, membernames);	
						}	
					}
				}
				if(out != null)
				{
					synchronized (chatroomwriters)
					{
						HashSet<PrintWriter> writers = chatroomwriters.get(chatroom);
						writers.remove(out);

						//Check if the room is empty
						if(writers.size() == 0)
						{
							//Remove the chatroom from the Hashmap
							chatroomwriters.remove(chatroom);
						}
						else
						{
							chatroomwriters.put(chatroom, writers);
							for(PrintWriter writer : writers)
							{
								writer.println("NOTIFICATION:(" + name + " has left the chatroom " + chatroom + ")");
							}

						}
					}
					
				}
				try
				{
					socket.close();
				}
				catch(IOException e)
				{
					System.out.println("Exception occured while closing socket");
				}
			}
		}
	}
}