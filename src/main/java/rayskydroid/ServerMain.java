package rayskydroid;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.logging.Handler;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ServerMain {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 8888;
    public static String myIP="";
    static ArrayList<String> myIPAddressesList;
    static ArrayList<String> myStringArray1;
    static ArrayList<String> myNameList;
    String textRecvIn;
    static ServerMain thisObj=new ServerMain();
    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    /**
     * get local ip address
     */
    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        }
                        else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        }
        catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }    
    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The server is running...");
        myIP = getLocalHostLANAddress().toString().substring(1);
        System.out.println("Host IP..." + myIP);
        System.out.println("Port..." + PORT);
        ServerSocket listener = new ServerSocket(PORT);
        
        try {
            while (true) {
            	thisObj.new ThreadListener(listener.accept()).start();                
            }
        } finally {
            listener.close();
        }
    }

    /**
     * Sender handler thread
     */
    public void processMsgFromHost(ArrayList<String> a) {
        //server processing messages received from host
        
    };

    public void processMsgFromClient(ArrayList<String> a) {
    	//server processing message received from clients
        if( a.get(2).split(":").length > 1 ){
            String message = a.get(2).split(":")[0];
            String value = a.get(2).split(":")[1];
            if( message.compareTo("ping") == 0 )
            {
                //host processing messages received from clients
                if( !myIPAddressesList.contains(a.get(1)) ) {
                    myStringArray1.add("P" + (myStringArray1.size() + 1) + ":" + a.get(1) + ":" + value);
                    myIPAddressesList.add(a.get(1));
                    myNameList.add(value);
                }
                try {
                    Thread.sleep(1000);//wait for client to start listening
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
                broadCast(myStringArray1.toString());
                System.out.println( "Update sent to:" + myStringArray1.toString());                    
            }
            else if( message.compareTo("PingHost") == 0 )
            {
                sendMessage(value,myIP,PORT,"HostResponse:" + myIP + "-servergame");
                System.out.println("Response sent to:" + value);                    
            }
        }
    };
    public void sendMessage( String destination, String source, int port, String message ){
        String type = "host";            
        message = type + "~" + source + "~" + message;
        DataOutputStream senderDataOutputStream=null;
        DataInputStream senderDataInputStream=null;
        Socket senderSocket=null;
        try{
        	System.out.println("attempting connection to :" + destination + "port:" + port);                
            senderSocket = new Socket(destination,port);
            senderDataOutputStream = new DataOutputStream(senderSocket.getOutputStream());
            senderDataInputStream = new DataInputStream(senderSocket.getInputStream());
            senderDataOutputStream.writeUTF(message);
            senderDataOutputStream.flush();
            String ackMessage = senderDataInputStream.readUTF();
            System.out.println("sent message:" + message);
            System.out.println("to: " + destination + "port:" + port);
            System.out.println("ack: " + ackMessage);                
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        finally{
        	if (senderDataOutputStream != null) {
                try {
                	senderDataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (senderDataInputStream != null) {
                try {
                	senderDataInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                senderSocket.close();
            } catch (IOException e) {
            }
        }
    };
    private void broadCast( String message ){
        for (int i = 0; i < myIPAddressesList.size(); i++) {
            //if (myIPAddressesList.get(i).compareTo(gHostIP) != 0) {
            sendMessage(myIPAddressesList.get(i), myIP, PORT, message);
            //}
        }
    };
    public class ThreadSender extends Thread {
    	public void run() {
	    	StringTokenizer tokenizer = new StringTokenizer(thisObj.textRecvIn, "~");
	
	        ArrayList<String> a = new ArrayList();
	
	        while (tokenizer.hasMoreTokens()) {
	            String token = tokenizer.nextToken();
	            a.add(token);
	        }
	        String type = a.get(0);
	        if (type.compareTo("host") == 0 )
	            thisObj.processMsgFromHost(a);
	        else if (type.compareTo("client") == 0 )
	        	thisObj.processMsgFromClient(a);
    	}
    };
    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    public class ThreadListener extends Thread {
        private Socket socket;
        private DataOutputStream dataOutputStream;
        private DataInputStream dataInputStream;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public ThreadListener(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
            	String message="acknowledged by server:" + myIP;
            	dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
                thisObj.textRecvIn = dataInputStream.readUTF();   
                System.out.println(thisObj.textRecvIn); 
                thisObj.new ThreadSender().start();
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
            	if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }        
    };
}