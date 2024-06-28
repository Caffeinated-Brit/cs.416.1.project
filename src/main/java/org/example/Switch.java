package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;

public class Switch {
    //this is the base layout for what we need to do in the switch.

    //  needs to check config on start up,
    //  create table
    //  needs to receive packets,
    //  check table for that packets mac.address
    //  send to port associated with mac.address, or flood too all but the reception port if not found.

    private DatagramSocket socket;
    Map forwardingTable = new HashMap<String, ArrayList<String>>();

    ArrayList<String> neighborsList = new ArrayList<>();

    boolean amITheGateway = false;

    ArrayList<String> connectedRouterInfo = new ArrayList<>();


    public Switch(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }


    public static String getUserDesMac(byte[] responseMessage){
        String message = new String((responseMessage));
        String srcSubnet = null;
        String[] arrOfMessage = message.split("\\.");
        srcSubnet =  arrOfMessage[0];
        //System.out.printf("srcSubnet: %s\n", srcSubnet);
        return srcSubnet;
    };
    public static String getUserDesSubnet(byte[] responseMessage){
        String message = new String((responseMessage));
        String srcSubnet = null;
        String[] arrOfMessage = message.split("\\.");
        srcSubnet =  arrOfMessage[1];
        //System.out.printf("srcSubnet: %s\n", srcSubnet);
        return srcSubnet;
    };

    public static String getSrcSubnet(byte[] responseMessage){
        String message = new String((responseMessage));
        String srcSubnet = null;
        String[] arrOfMessage = message.split("\\.");
        srcSubnet =  arrOfMessage[2];
        //System.out.printf("srcSubnet: %s\n", srcSubnet);
        return srcSubnet;
    };
    public static String getSrcMac(byte[] responseMessage){
        String message = new String((responseMessage));
        String srcMac = null;
        String[] arrOfMessage = message.split("\\.");
        srcMac = arrOfMessage[3];
        //System.out.printf("srcMac: %s", srcMac);
        // should return pc
        return srcMac;
    };

    public static String getDesSubnet(byte[] responseMessage){
        String message = new String((responseMessage));
        String desSubnet = null;
        String[] arrOfMessage = message.split("\\.");
        desSubnet = arrOfMessage[5];
        //System.out.printf("desSubnet: %s", desSubnet);
        return desSubnet;
    };

    public static String getDesMac(byte[] responseMessage){
        String message = new String((responseMessage));
        String desMac = null;
        String[] arrOfMessage = message.split("\\.");
        desMac = arrOfMessage[4];
        //System.out.printf("desMac: %s", desMac);
        return desMac;
    };


    public static String getMessage(byte[] responseMessage){
        String message = new String((responseMessage));
        String srcMac = null;
        String[] arrOfMessage = message.split("\\.");
        srcMac = arrOfMessage[6];
        //System.out.printf("\nMessage: %s", srcMac);
        // should return pc
        return srcMac;
    };

    DatagramPacket buildPacket(String userInputDesMac, String finalDes, String srcSub, String srcMac, String desSub, String desMac, String message,InetAddress serverIP, int serverPort){
        String build = userInputDesMac + "." + finalDes +"."+ srcSub + "." + srcMac + "." + desSub + "." + desMac + "." + message;
        //should look like this now pc4.n4.n1.pc1.n4.pc4.lalalalala
        byte [] buffer = build.getBytes();
        DatagramPacket request = new DatagramPacket(buffer,buffer.length,serverIP,serverPort);
        return request;
    }


    public static void main(String[] args) {
        Parser parser = new Parser();
        parser.mapFromConfig(); // Load data from config.json
        String whoAmI = args[0];
        String whereAmI = parser.getMySubnet(whoAmI);
        ArrayList<String> ipAndPortsList = parser.getIpAndPort(whoAmI);

        try {

            System.out.println("Who am I: " + whoAmI);
            System.out.println("WhereAmI: " + whereAmI);
            System.out.println("My Port: " + ipAndPortsList.get(1));
            Switch aSwitch = new Switch(Integer.parseInt(ipAndPortsList.get(1))); //needs to be connected to PC A
            aSwitch.neighborsList = parser.getNeighbors(whoAmI);
            if (parser.getOuterSwitchName(whereAmI).equals(whoAmI)) {
                aSwitch.amITheGateway = true;
                aSwitch.connectedRouterInfo = parser.getRouterIpPort(parser.getSubnetRouter(whereAmI));
            }

            aSwitch.service(whereAmI);


        } catch (SocketException ex) {
            System.out.println("Socket Error: " + ex.getMessage());
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }finally {
            System.out.println("Shutting down...");
        }
    }
    private void service(String whereAmI) throws IOException{
        while (true){
            System.out.println("In Service");
            DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
            socket.receive(request);
            byte[] buffer = request.getData();

            //
            String message = new String((buffer));
            System.out.println("Received Message: " + message);
            //


            System.out.println(request.getPort());
            // make this the port of the last hop
            int LastHopPort = request.getPort();

            //String stringSourceMac = String.valueOf(sourceMAC);
            //String stringDestinationMac = String.valueOf(destinationMAC);

            String stringSourceMac = getSrcMac(buffer);

            String stringDestinationMac = getDesMac(buffer);

            ///THESE BOTH TECHNICALLY NEED TO  PULL FROM THE FIRST 2 APPENDED THINGS
            //pc4.n4.n1.pc1.n4.pc4.lalalalala
            // ^  ^
            // This one

            updateForwardingTable(stringSourceMac, request);


            if (checkIfFound(stringDestinationMac) && Objects.equals(whereAmI, getDesSubnet(buffer))) {
                System.out.println("Found");
                ArrayList<String> foundIpAndPort = (ArrayList<String>) forwardingTable.get(stringDestinationMac);
                send(request, InetAddress.getByName(foundIpAndPort.get(0)), Integer.parseInt(foundIpAndPort.get(1)));
            } else if (amITheGateway && !Objects.equals(whereAmI, getDesSubnet(buffer))) {
                System.out.println("I am the gateway");
                send(request, InetAddress.getByName(connectedRouterInfo.get(0)), Integer.parseInt(connectedRouterInfo.get(1)));
            }
            else {
                System.out.println("Destination Not Found In Table");
                flood(LastHopPort, request);
            }
        }
    }

    private void updateForwardingTable(String sourceMac, DatagramPacket request) {
        String lastHopAddress = (String.valueOf(request.getAddress()).substring(1));
        String lastHopPort = String.valueOf(request.getPort());
        ArrayList<String> ipAndPortsList = new ArrayList<>(Arrays.asList(lastHopAddress, lastHopPort));
        ArrayList<String> ipAndPort = new ArrayList<>(ipAndPortsList);
        forwardingTable.put(sourceMac, ipAndPort);
    }

    private boolean checkIfFound(String destinationMac) {
        Parser parser = new Parser();
        parser.mapFromConfig(); // Load data from config.json


        if (forwardingTable.containsKey(destinationMac)) {
            System.out.println("Destination MAC: " + destinationMac);
            System.out.println("forwardingTable: " + forwardingTable);
            return true;
        } else {
            System.out.println("forwardingTable: " + forwardingTable);
            return false;
        }
    }

    private void flood(int LastHopPort, DatagramPacket request) throws IOException {
        System.out.println("Flood");
        String stringLastHopPort = String.valueOf(LastHopPort);

        for (String neighbor : neighborsList  ) {
            Parser parser = null;
            parser = new Parser();
            parser.mapFromConfig();

            if (!(stringLastHopPort.equals(parser.getIpAndPort(neighbor).get(1)))) {
                ArrayList<String> ipAndPortsList = parser.getIpAndPort(neighbor);
                send(request, InetAddress.getByName(ipAndPortsList.get(0)), Integer.parseInt(ipAndPortsList.get(1)));
            }
        }
    }

    private void send(DatagramPacket request, InetAddress serverIP, int serverPort) throws IOException {
        //TODO
        byte[] responseMessage = request.getData();
        String message = new String((responseMessage));
        //System.out.println("Sending: " + message);

        byte [] buffer = message.getBytes();

        DatagramPacket newRequest = new DatagramPacket(buffer,buffer.length,serverIP,serverPort);
        socket.send((newRequest));
    }
}
