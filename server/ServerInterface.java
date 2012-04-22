package chatosaur.server;

import java.net.*;
import java.io.*;
import java.util.concurrent.Semaphore;

// manages and shows the user the server administration
public class ServerInterface {

    private Server server;

    // constructor
    public ServerInterface(Server server) {
        this.server = server;
    }

    public void start() {

        System.out.println("\nWelcome to Chatosaur Server v0.0001\n");

        while (true) {

            // show the user/administrator the menu
            showMenu();

            System.out.print("\nChoose an option: ");

            switch(getUserInput()) {
                case 1:
                    promptStartServer();
                    break;
                case 2:
                    promptAddServer();
                    break;
                case 3:
                    showMenu();
                    break;
                case 0:
                    promptShutdownServer();
                    break;
                default:
                    System.out.println("command not recognized.");
            }
        }
    }

    // show user the options
    private void showMenu() {
        System.out.println("\n1. start server\n" +
                           "2. add new server to system\n" +
                           "3. show this menu\n" +
                           "0. Exit/Shutdown");
    }

    // let's the user name the server and starts it
    private void promptStartServer() {
        System.out.print("\nGive the server a name: ");

        server.setName(getUserInput());
        server.start();
    }

    // prompt the user for a server and port to add to list
    private void promptAddServer() {

        System.out.print("\nServer name: ");
        String name = getUserInput();

        System.out.print("\nServer host: ");
        String host = getUserInput();

        System.out.print("\nServer port: ");
        int port = Integer.parseInt(getUserInput());

        // test the connection
        if (testConnection(host, port)) {
            if (server.addServer(name, host, port)) {
                System.out.println("Server added to system.");
            }
        } else {
            System.out.println("Could not contact server.");
        }
    }

    private void promptShutdownServer() {
        System.out.print("\nAre you sure? (y/n): ");

        if (getUserInput() == "y") {
            server.gracefulShutdown();
            System.exit(0);
        }
    }

    // test a connection. returns true if connection successful
    private boolean testConnection(String host, int port) {
        Socket socket = null;
        boolean reachable = false;

        try {
            Socket = new Socket(host, port);
            reachable = true;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {}
            }
        }

        return reachable;
    }

    // nice compact method to take user input
    private String getUserInput() {
        String input = "";

        try {
            BufferedReader UserInput = new BufferedReader(new InputStreamReader(System.in));
            input = UserInput.readLine();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return input;
    }

}
