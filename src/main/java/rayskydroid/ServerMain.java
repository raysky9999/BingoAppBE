package rayskydroid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;
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
	private static ArrayList<String> blockIP;
    private static int PORT = 88;
    public static String myIP="";
    static ArrayList<String> myIPAddressesList;
    //static ArrayList<String> myStringArray1;
    static ArrayList<String> myMessageQueue;
    static ArrayList<String> myNameList;
    static HashMap<String,Date> lastResponded;
    
    static ServerMain thisObj=new ServerMain();
    /**
     * get local ip address
     */
    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
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
        Logger.addRecordToLog("The server is running...");
        myIPAddressesList = new ArrayList<String>();
        lastResponded = new HashMap<String, Date>();
        blockIP = new ArrayList<String>();
        //myStringArray1 = new ArrayList();
        myNameList = new ArrayList<String>();
        myMessageQueue = new ArrayList<String>();
        myIP = getLocalHostLANAddress().toString().substring(1);
        System.out.println("Host IP..." + myIP);
        if( args.length > 0 )
        {
        	PORT = Integer.parseInt(args[0]);
        	Logger.isEnabled = Boolean.parseBoolean(args[1]);
        	for ( int i = 2; i < args.length; i++){
        		blockIP.add(args[i]);
        		System.out.println("Blocking IP: " + args[i]);
        		Logger.addRecordToLog("Blocking IP: " + args[i]);
        	}
        }
        System.out.println("Port..." + PORT);
        ServerSocket listener = new ServerSocket(PORT);
        Logger.addRecordToLog("Server started!");
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
    public String processMsgFromHost(ArrayList<String> a) {
        //server processing messages received from host
    	String response = "";
    	if( a.get(2).split(":").length > 1 && a.get(2).split(":")[0].equals("IamHost")){            
            String value = a.get(2).split(":")[1];           
            //add host to list
            try{
	            if( !myIPAddressesList.contains(a.get(1)) ) {
	            	//myStringArray1.add("P" + (myStringArray1.size() + 1) + ":" + a.get(1) + ":" + value);
	                myIPAddressesList.add(a.get(1));
	                lastResponded.put(a.get(1), new Date());
	                if( value.length() == 0 )
	                	value = "no name entered";
	                myNameList.add(value.trim().replace("\n", ""));               
	                System.out.println("Host added:" + a.get(1) + "-" + value);
	                Logger.addRecordToLog("Host added:" + a.get(1) + "-" + value);
	            }
	            try {
	                Thread.sleep(500);//wait for client to start listening
	            }
	            catch( Exception e )
	            {
	                e.printStackTrace();
	            }   
            }
            catch( Exception e ){
            	StringWriter sw = new StringWriter();
        		PrintWriter pw = new PrintWriter(sw);
        		e.printStackTrace(pw);
            	Logger.addRecordToLog("ERROR during add host to list");
            	Logger.addRecordToLog(e.getMessage() + " " + e.toString() + " " + sw.toString());
            }
        }   
    	else if (a.get(2).split(":").length > 1 && a.get(2).split(":")[0].equals("StopHosting")){    
    		String value = a.get(2).split(":")[1]; 
    		try{
	    		if( myIPAddressesList.contains(a.get(1)) ) {
	    			//String str = "P" + (myStringArray1.size() + 1) + ":" + a.get(1) + ":" + value;
	            	//myStringArray1.remove(str);
	                myIPAddressesList.remove(a.get(1));
	                lastResponded.remove(a.get(1));
	                if( value.length() == 0 )
	                	value = "no name entered";
	                boolean removeSuccess = myNameList.remove(value.trim().replace("\n", ""));
	                if( !removeSuccess ){
	                	Logger.addRecordToLog("Unable to remove myNameList item:" + value.trim().replace("\n", ""));
	                	clearHosts();
	                }                
	                System.out.println("Host removed:" + a.get(1) + "-" + value);
	                Logger.addRecordToLog("Host removed:" + a.get(1) + "-" + value);
	                Logger.addRecordToLog("Sizes of remaining arrays: " 
	                					+ "-myIPAddressesList:" + myIPAddressesList.size()
	                					+ "-myNameList:" + myNameList.size());
	            }
    		}
    		catch( Exception e ){
    			StringWriter sw = new StringWriter();
        		PrintWriter pw = new PrintWriter(sw);
        		e.printStackTrace(pw);
            	Logger.addRecordToLog("ERROR during remove host from list");
            	Logger.addRecordToLog(e.getMessage() + " " + e.toString() + " " + sw.toString());
            }
    	}
    	else if( a.get(2).equals("GetMessages")){
            response = thisObj.getSendersMessage(a);
    	}
    	else{
    		System.out.println("message: " + a.get(0) + "~" + a.get(1) + "~" + a.get(2) + "~" + a.get(3) +", queued ");
    		Logger.addRecordToLog("message: " + a.get(0) + "~" + a.get(1) + "~" + a.get(2) + "~" + a.get(3) +", queued ");
    		myMessageQueue.add(a.get(0) + "~" + a.get(1) + "~" + a.get(2) + "~" + a.get(3));  
    	}    
    	return response;
    };

    public String processMsgFromClient(ArrayList<String> a) {
    	String response = "";
    	try{
	    	if( a.get(2).split(":").length > 1 && a.get(2).split(":")[0].equals("GetHost")){  
	    		//remove outdate hosts:
	    		ArrayList<Integer> toBeRemoved = new ArrayList<Integer>();
	    		for(int i=0;i<myIPAddressesList.size();i++){
	    			Date now = new Date();
	    			if( lastResponded.get(myIPAddressesList.get(i)) != null ){
		    			if( now.getTime() - lastResponded.get(myIPAddressesList.get(i)).getTime() >= 300000 ){
		    				toBeRemoved.add(i);
		    				Logger.addRecordToLog("lastResponded: " + lastResponded.get(myIPAddressesList.get(i)) );
		    				Logger.addRecordToLog("now: " + now );
		    			}
	    			}
	    			else{
	    				Logger.addRecordToLog("lastResponded mismatch with myIPAddressesList");
	    				clearHosts();
	    			}
	    		}
	    		for( Integer toBeRemovedIndex: toBeRemoved){
	    			String removedHost = myNameList.get(toBeRemovedIndex.intValue());
	    			myNameList.remove(toBeRemovedIndex.intValue());
	    			lastResponded.remove(myIPAddressesList.get(toBeRemovedIndex.intValue()));
	    	    	myIPAddressesList.remove(toBeRemovedIndex.intValue());    	
	    	    	Logger.addRecordToLog("removed host:" + removedHost);
	    		}
	            //get host lists
	            System.out.println(myIPAddressesList.size() + " hosts found...");
	            Logger.addRecordToLog(myIPAddressesList.size() + " hosts found...");
	            for(int i=0;i<myIPAddressesList.size();i++){
	            	if( i==0 )
	            		response = "host~" + myIPAddressesList.get(i) + "~HostResponse:" + myIPAddressesList.get(i) + "-" + myNameList.get(i);
	            	else
	            		response = response + "@host~" + myIPAddressesList.get(i) + "~HostResponse:" + myIPAddressesList.get(i) + "-" + myNameList.get(i);                            
	            	//myMessageQueue.add(response);
	            }            
	            System.out.println("Hosts list sent: " + response);
	            Logger.addRecordToLog("Hosts list sent: " + response);
	            try {
	                Thread.sleep(500);//wait for client to start listening
	            }
	            catch( Exception e )
	            {
	                e.printStackTrace();
	            }                
	        } 
	    	else if( a.get(2).equals("GetMessages")){
	            response = thisObj.getSendersMessage(a);
	    	}
	    	else{
	    		//server processing message received from clients
	    		System.out.println("message: " + a.get(0) + "~" + a.get(1) + "~" + a.get(2)  + "~" + a.get(3) +", queued ");
	    		Logger.addRecordToLog("message: " + a.get(0) + "~" + a.get(1) + "~" + a.get(2)  + "~" + a.get(3) +", queued ");
	        	myMessageQueue.add(a.get(0) + "~" + a.get(1) + "~" + a.get(2)  + "~" + a.get(3)); 
	    	}  
    	}
    	catch( Exception e ){
    		StringWriter sw = new StringWriter();
    		PrintWriter pw = new PrintWriter(sw);
    		e.printStackTrace(pw);
        	Logger.addRecordToLog("ERROR during processMsgFromClient");
        	Logger.addRecordToLog(e.getMessage() + " " + e.toString() + " " + sw.toString());
        }
    	return response;
    };

    private void clearHosts(){
    	Logger.addRecordToLog("Clearing everything");
    	Logger.addRecordToLog("Sizes of remaining arrays: " 
				+ "-myIPAddressesList:" + myIPAddressesList.size()
				+ "-myNameList:" + myNameList.size()
				+ "-lastResponded:" + lastResponded.size());
    	myNameList.clear();
    	myIPAddressesList.clear();
    	lastResponded.clear();
    };
    
    public String prepareResponseMessage(String textRecvIn){
        StringTokenizer tokenizer = new StringTokenizer(textRecvIn, "~");
    	
        ArrayList<String> a = new ArrayList<String>();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            a.add(token);
        }
        String message = "acknowledged by server";
        if( a.size() < 4 )
        	message = "failed to be stored in server, argument insufficient: " + a.size();     
        else{
        	String type = a.get(0);
	        if (type.compareTo("host") == 0 )
	            message = thisObj.processMsgFromHost(a);
	        else if (type.compareTo("client") == 0 )
	        	message = thisObj.processMsgFromClient(a);
	        else if (type.compareTo("clearHosts") == 0 ){
	        	message = "clearing all hosts...";
	        	clearHosts();
	        }
        }
        
        return message;
    };
    
    public String getSendersMessage(ArrayList<String> a){
    	String address = a.get(1);
    	String response = "no messages pending";
    	if( address.equals("192.168.43.1"))
    	{
    		response = "this is an invalid address, you are using tethering host IP!";
    	}
    	else
    	{	    	
	    	for(int i = 0; i < myMessageQueue.size(); i++){
	    		StringTokenizer tokenizer = new StringTokenizer(myMessageQueue.get(i), "~");
	        	
	            ArrayList<String> arrStr = new ArrayList<String>();
	
	            while (tokenizer.hasMoreTokens()) {
	                String token = tokenizer.nextToken();
	                arrStr.add(token);
	            }
	            if( arrStr.get(3).equals(a.get(1))){//address matches
	            	response = myMessageQueue.get(i);
	            	myMessageQueue.remove(i);
	            	break;
	            }
	    	}
	    	//updates lastResponded if host IP
	    	Date lastRespondedDateTime = lastResponded.get(address);
	    	if( lastRespondedDateTime != null ){
	    		lastResponded.remove(address);
	    		lastResponded.put(address, new Date());
	    	}
    	}
    	System.out.println("responding string: " + response + ", to: " + address);
    	Logger.addRecordToLog("responding string: " + response + ", to: " + address);
    	return response;
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
            	String message="";
            	dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                
                String textRecvIn = dataInputStream.readUTF();  
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                String senderAddr = socket.getInetAddress().toString();
                
                boolean IPAllowed = true;
                for ( int i = 0; i< blockIP.size();i++){
                	if( senderAddr.equals(blockIP.get(i)) )
                    {
    	                IPAllowed = false;
                    }
                }
                if( IPAllowed )
                { 
                	System.out.println(senderAddr);
	                System.out.println(timestamp.toString() + "|" + textRecvIn); 
	                Logger.addRecordToLog(textRecvIn); 
	                message = prepareResponseMessage(textRecvIn);              
	                dataOutputStream.writeUTF(message);
	                dataOutputStream.flush();
                }
            } catch (IOException e) {
                System.out.println(e);
                Logger.addRecordToLog(e.toString());
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