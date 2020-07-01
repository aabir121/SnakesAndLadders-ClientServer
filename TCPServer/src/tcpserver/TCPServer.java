package tcpserver;

import java.net.*;
import java.io.*;

public class TCPServer implements Runnable {

    private ChatServerThread clients[] = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread thread = null;
    private int clientCount = 0;

    public TCPServer(int port) {
        try {
            System.out.println("Binding to port " + port + ", please wait  ...");
            server = new ServerSocket(port);
            System.out.println("Server started: " + server);
            start();
        } catch (IOException ioe) {
            System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
        }
    }

    @Override
    public void run() {
        while (thread != null) {
            try {
                System.out.println("Waiting for a client ...");
                addThread(server.accept());
            } catch (IOException ioe) {
                System.out.println("Server accept error: " + ioe);
                stop();
            }
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            thread.stop();
            thread = null;
        }
    }

    private int findClient(int ID) {
        for (int i = 0; i < clientCount; i++) {
            if (clients[i].getID() == ID) {
                return i;
            }
        }
        return -1;
    }

    /*
        The main logic of server is to switch between players.
        The server co-ordinates the state of both the players accordingly.
    */
    public synchronized void handle(int ID, String input) {
        if (input.equals(".bye")) {
            clients[findClient(ID)].send(".bye");
            remove(ID);
        }
        //a player rolled the dice and send the value with the format val 4 (any value)
        else if (input.startsWith("val")) {

            String[] parse = input.split(" "); //split the input

            int next = findClient(ID) + 1; //switching to next player
            if (next >= clientCount) { 
                //if next player number is greater than the number of players
                //then go to the first player like a circular system.
                next = 0;
            }
            //send the rolled value of a player to the next player
            clients[next].send("value "+parse[1]);
            for (int i = 0; i < clientCount; i++) {
                if (i != next) {
                    //notify the player other than the next player to stop playing and it is the turn of next player
                    clients[i].send("Turn of Client " + next);
                }
            }
        }
        System.out.println(ID + ": " + input);
    }

    public synchronized void remove(int ID) {
        int pos = findClient(ID);
        if (pos >= 0) {
            ChatServerThread toTerminate = clients[pos];
            System.out.println("Removing client thread " + ID + " at " + pos);
            if (pos < clientCount - 1) {
                for (int i = pos + 1; i < clientCount; i++) {
                    clients[i - 1] = clients[i];
                }
            }
            clientCount--;
            try {
                toTerminate.close();
            } catch (IOException ioe) {
                System.out.println("Error closing thread: " + ioe);
            }
            toTerminate.stop();
        }
    }

    
    //as soon as any player joins notify the other player and also change the window title
    private void addThread(Socket socket) {
        if (clientCount < clients.length) {
            System.out.println("Client accepted: " + socket);
            clients[clientCount] = new ChatServerThread(this, socket);
            
            try {
                clients[clientCount].open();
                clients[clientCount].start();
                if(clientCount==1)
                {
                    for(int i=0;i<clientCount;i++)
                    {
                        clients[i].send("ready"); //always start with the  first client
                    }
                }
                clients[clientCount].send("Player "+clientCount); //provide the player info to change the window title
                clientCount++;
                
            } catch (IOException ioe) {
                System.out.println("Error opening thread: " + ioe);
            }
        } else {
            System.out.println("Client refused: maximum " + clients.length + " reached.");
        }
    }

    public static void main(String args[]) {
        TCPServer server = null;
        server = new TCPServer(2000);

    }
}
