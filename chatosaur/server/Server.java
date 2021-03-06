package chatosaur.server;

import java.net.*;
import java.io.*;
import java.util.ArrayList;

// import my project's classes
import chatosaur.server.ServerInterface;
import chatosaur.server.OutgoingServerMessage;
import chatosaur.server.OutgoingServerList;
import chatosaur.server.IncomingServerList;
import chatosaur.server.Connection;
import chatosaur.server.ConnectionBroker;
import chatosaur.common.ConnectedServer;
import chatosaur.server.Log;

public class Server {

    private String id;
    private ArrayList<ConnectedServer> serverList;
    private ArrayList<Connection> connections;

    public boolean running = false;
    public Log log;
    public String host;
    public int port = -1;

    public Server(String host) {
        this.host = host;
    }

    public boolean start() {

        if (port > -1) {
            // set up our log based on the port that should have been set
            log = new Log("server_" + Integer.toString(port) + ".log");

            // establish ArrayList to hold connections
            connections = new ArrayList<Connection>();

            // establish ArrayList to hold servers
            serverList = new ArrayList<ConnectedServer>();

            // add ourseleves to the list
            serverList.add(new ConnectedServer(host, port));

            new Thread(new ConnectionBroker(this)).start();

            log.write("Server started on port " + Integer.toString(port) + ".");

            running = true;
        }

        return running;
    }

    // this lets us kill a client object and socket when they disconnect
    public void killSocket(Connection conn) {
        for (int i=0; i<connections.size(); i++) {
            // find the connection we want to get rid of in the list
            if (connections.get(i) == conn) {
                String goodbye = "<" + conn.clientName + "> left the room.";

                // let everyone know this user left
                sendToAll(conn, goodbye);
                log.write(goodbye);

                // remove the connection
                connections.remove(i);
                break;
            }
        }
    }

    // send to all connections on this server except for the sender
    public void sendToAll(Connection from, String message) {
        // loop through each connection on this server and send it the message
        // skip the person who sent the message. this might be changed in the future
        for (int i=0; i<connections.size(); i++) {
            Connection conn = connections.get(i);
            if (conn == from) {
                continue;
            }
            conn.sendMessage(message);
        }

        // now send to other servers so they can send to their clients
        propagateMessage(from, message);
    }

    // send to all connections on this server
    public void sendToAllFromOutside(String message) {
        // loop through and send an outside message to everyone on this server
        for (int i=0; i<connections.size(); i++) {
            Connection conn = connections.get(i);
            conn.sendMessage(message);
        }
    }

    // just a getter to get out list of current connections on this server
    public ArrayList<Connection> getConnections() {
        return connections;
    }

    // setter to set the servers port to listen on
    public void setPort(int port) {
        this.port = port;
    }

    // add a server to the network
    public void addServer(String host, int port) {
        ConnectedServer newServer = new ConnectedServer(host, port);

        // add an instance to our list
        serverList.add(newServer);

        // send out an updated list to all the servers
        propagateList();
    }

    // set the server list. This happens when another server sends us a new one
    public void setServerList(ArrayList<ConnectedServer> newList) {
        // set the new list
        serverList = newList;

        String list = serverList.get(0).getClientName();

        // build up a list of the servers so we can log this
        for (int i=1; i<serverList.size(); i++) {
            ConnectedServer s = serverList.get(i);
            list = list + ", " + s.getClientName();
        }

        // write the new list to the logs
        log.write("Updated List: " + list);
    }

    // read in a message
    public String readMessage(Socket incoming) {
        String message = null;

        try {
            BufferedReader sin = new BufferedReader(new InputStreamReader(incoming.getInputStream())); 
            message = sin.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return message;
    }

    // shutdown the server, the idea is for this to be a central location to perform tasks
    // before we kill a server
    public void gracefulShutdown() {
        if (running == true) {
            running = false;
            log.write("Server shutdown.");
        }
    }

    // gets a fresh copy of the server list
    public ArrayList<ConnectedServer> getServerList() {
        return serverList;
    }

    // removes a connected server from the list
    public void removeConnectedServer(ConnectedServer toRemove) {
        boolean removed = false;

        for (int i=0; i<serverList.size(); i++) {
            ConnectedServer s = serverList.get(i);

            // check to see if this is the one we want to remove
            if (s == toRemove) {
                serverList.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            log.write("Removed connected server: <" + toRemove.host + ":" + Integer.toString(toRemove.port) + ">");
        } else {
            log.write("Could not remove connected server: <" + toRemove.host + ":" + Integer.toString(toRemove.port) + ">");
        }
    }

    // private

    // send a message from one of this server's connections (clients) to the other server
    // so that they can propagate the message to all their clients
    private void propagateMessage(Connection from, String message) {

        for (int i=0; i<serverList.size(); i++) {
            ConnectedServer s = serverList.get(i);

            // make sure this isn't the current server
            if (host != s.host && port != s.port) {
                log.write("Sending message to: <" + s.host + ":" + Integer.toString(s.port) + ">");
                new OutgoingServerMessage(this, s, from, message);
            }
        }
    }

    // sends the current server list around to the other connected servers.
    // hopefully this doesn't happen too often
    private void propagateList() {

        for (int i=0; i<serverList.size(); i++) {
            ConnectedServer s = serverList.get(i);

            // make sure we don't send to ourselves
            if (host != s.host && port != s.port) {
                log.write("Sending list to: <" + s.host + ":" + Integer.toString(s.port) + ">");
                new OutgoingServerList(this, s);
            }
        }
    }

    // main method

    public static void main(String[] args) {

        // create new server bind to port and start
        Server server = new Server("localhost");

        // start up the user interface so someone can manage the server
        // pass in the new server so we can have access to it later
        ServerInterface sinterface = new ServerInterface(server);
        sinterface.start();
    }
}
