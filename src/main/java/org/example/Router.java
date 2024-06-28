package org.example;

import java.net.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
// TODO
// PART ONE
// Put table together; (check if connected to subnets)
// pull neighbors form config.

// PART 2
// Sent out table to attaching routers.

//PART 3
// Receiving back other tables from other routers, And changing its own table from this.
// for loop check and edit each part of table
//  -changing cost/distance
//  -changing next hop (possibly)

//(same as part 2)
// sending out newly created table again.



// udp @r2%n2#0<n2%n3#0<n3%n4#0<n4%@

public class Router {
    private DatagramSocket socket;
    Map routerTable = new HashMap<String, ArrayList<String>>();
    ArrayList<String> neighborsList = new ArrayList<>();
//    ArrayList<ArrayList<String>> links = new ArrayList<>();

    ArrayList<ArrayList> links = new ArrayList<ArrayList>();


    public Router(String port) throws SocketException {
        socket = new DatagramSocket(Integer.parseInt(port));
    }

    public static void main(String[] args) throws IOException {
        String whoAmI = args[0];
        Parser parser = new Parser();
        parser.mapFromConfig();



        System.out.println("Who Am I: " + whoAmI);
        System.out.println("My Ip and port: " + parser.getRouterIpPort(whoAmI));
        System.out.println("Linked Routers: " + parser.getLinks(whoAmI));
        System.out.println("getRouterNeighbors: " + parser.getRouterNeighbors(whoAmI));

        Router router = new Router(parser.getRouterIpPort(whoAmI).get(1));

        router.links = parser.getLinks(whoAmI);

        router.startUp(whoAmI);
        router.service(whoAmI);




    }

    private void startUp(String whoAmI) throws IOException {
        System.out.println("startup");
        Parser parser = new Parser();
        parser.mapFromConfig();

        this.neighborsList = parser.getRouterNeighbors(whoAmI);
        System.out.println(neighborsList);


        for (String subnet : neighborsList ){
            ArrayList<String> value = new ArrayList<>();
            value.add("0");
            value.add(subnet);
            this.routerTable.put(subnet, value);

        }
        System.out.println(routerTable);

        floodTable(whoAmI);

    }

    private Boolean checkIfFound(HashMap<String, ArrayList<String>> receivedRouterTable, String routerRecievedName){
        // param should be the entire table it is receiving
        // returns true if it needs update, false for no change

        Set<String> receivedKeys = receivedRouterTable.keySet();
        System.out.println("receivedKeys: "+ receivedKeys);
        boolean updated = false;
        for (String receivedKey : receivedKeys){
            if (!routerTable.containsKey(receivedKey)) {
                System.out.println(receivedKey);
                ArrayList<String> newValues = new ArrayList<>();
                int distance = Integer.parseInt(receivedRouterTable.get(receivedKey).get(0));
                newValues.add(String.valueOf(distance));
                newValues.add(routerRecievedName);
                //updateTable(receivedKey, receivedRouterTable.get(receivedKey));
                updateTable(receivedKey, newValues);
                System.out.println("updated table 1");
                updated = true;
            } else {
                ArrayList<String> localValues = (ArrayList<String>) routerTable.get(receivedKey);
                ArrayList<String> receivedValues = receivedRouterTable.get(receivedKey);
                if (Integer.parseInt(localValues.get(0)) > Integer.parseInt(receivedValues.get(0))) {
                    System.out.println("updated table 2 if statement");
                    routerTable.remove(receivedKey);
                    updateTable(receivedKey, receivedValues);
                    updated = true;
                } else {
                    System.out.println("No update necessary");
                }
            }
        }
            System.out.println("after updated table");
            return updated;
    }


    private void updateTable(String newKey, ArrayList<String> newValues){
        this.routerTable.put(newKey, newValues);
        System.out.println("Update table is called");
        System.out.println(this.routerTable.containsKey(newKey));
        System.out.println(newKey);
        System.out.println(this.routerTable.get(newKey));
    }

    private void send(DatagramPacket packetOfSendingTable) throws IOException {
        socket.send(packetOfSendingTable);
    }

    private DatagramPacket buildDataPacket(byte[] receivedPacketBuffer, String whoAmI) throws UnknownHostException {

        Parser parser = new Parser();
        parser.mapFromConfig();

        String userDesMac = getUserDesMac(receivedPacketBuffer);
        String userDesSubnet = getUserDesSubnet(receivedPacketBuffer);
        String srcSub = getSrcSubnet(receivedPacketBuffer);
        String srcMac = getSrcMac(receivedPacketBuffer);
        String srcDesMac = getDesMac(receivedPacketBuffer);
        String desSubnet = getDesSubnet(receivedPacketBuffer);
        String message = getMessage(receivedPacketBuffer);



        String nexthop = returnNextHop(routerTable, getDesSubnet(receivedPacketBuffer));

        DatagramPacket dataPacket = null;


        for (String neighbor : parser.getRouterNeighbors(whoAmI) ){
            if (neighbor.equals(nexthop)){
                System.out.println("Sending into connected subnet " + nexthop);

                ArrayList<String> switchIpPort = parser.getOuterSwitchIpAndPort(nexthop);

                String build = userDesMac + "." + userDesSubnet +"."+ srcSub + "." + srcMac + "." + srcDesMac + "." + desSubnet + "." + message;
                byte [] buffer = build.getBytes();

                System.out.println("switch ip and port: " + switchIpPort);
                dataPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(switchIpPort.get(0)), Integer.parseInt(switchIpPort.get(1)));
                return dataPacket;

            }
        }

        System.out.println("Sending to next router " + nexthop);
        ArrayList<String> nextHopIpPort = parser.getRouterIpPort(nexthop);
        String build = userDesMac + "." + userDesSubnet +"."+ srcSub + "." + srcMac + "." + srcDesMac + "." + desSubnet + "." + message;
        byte [] buffer = build.getBytes();

        dataPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(nextHopIpPort.get(0)), Integer.parseInt(nextHopIpPort.get(1)));

        return dataPacket;
    };

    private String receivedRouter(byte[] receivedPacketBuffer){
        String receivedRouterName = null;
        String packetData = new String(receivedPacketBuffer);
        String[] pairs = packetData.split("%");
        receivedRouterName = pairs[0].substring(1);
        return receivedRouterName;
    }



    private HashMap<String, ArrayList<String>> packetToTable(byte[] receivedPacketBuffer, String receivedRouter, String whoAmI){
        //TODO: Add link cost to each entry

        Parser parser = new Parser();
        HashMap<String, ArrayList<String>> table = new HashMap<String, ArrayList<String>>();
        String packetData = new String(receivedPacketBuffer);
        System.out.printf("/n Packet data before turing to table: %s",packetData);

        String[] pairs = packetData.split("%"); //SEPARATION PATTERN // udp @r2%n2#0<n2%n3#0<n3%n4#0<n4%@

        String foundCost = "";
        for (ArrayList<String> link : links) {
            if (link.get(0).equals(receivedRouter)) {
                foundCost = link.get(1);
            }
        }
        System.out.println("\nFound Cost:" + foundCost);

        for (String pair : pairs) {
            if (pair != "" || !pair.contains("#")) {
                int index = pair.indexOf('#');
                if (index != -1 || !pair.contains("@")) {
                    String key = pair.substring(0, index);
                    String[] valueArray = pair.substring(index + 1).split("<");
                    int updatedCost = Integer.parseInt(valueArray[0]) + Integer.parseInt(foundCost);
                    valueArray[0] = String.valueOf(updatedCost);
                    if (!table.containsKey(key)) {
                        ArrayList<String> newValues = new ArrayList<>();
                        newValues.add(valueArray[0]);
                        newValues.add(receivedRouter);
                        table.put(key, newValues);
                    }
                } else {
                    System.out.println("Removed from values: " + pair);
                }
            }
        }
        return table;
    }


    private DatagramPacket tableToPacket(HashMap<String, ArrayList<String>> routerTable, String ip, int port, String whoAmI) throws UnknownHostException {
        //TODO: add current router name to the packet, add start special character, add end special character
        Set<String> tableKeys = routerTable.keySet();
        String message = "";
        message = "@" + whoAmI;
        for (String key : tableKeys) {
            ArrayList<String> values = routerTable.get(key);
            message += "%" + key + "#" + values.get(0) + "<" + values.get(1);
        }
        message += "%@";
        System.out.println(message);
        byte[] buffer = message.getBytes();
        InetAddress formattedIp = InetAddress.getByName(ip);
        DatagramPacket tablePacket = new DatagramPacket(buffer, buffer.length, formattedIp, port);

//        tablePacket.setData();
        System.out.println(tablePacket);

        return tablePacket;
    }

    private void floodTable(String whoAmI) throws IOException {
        Parser parser = new Parser();
        parser.mapFromConfig();

        ArrayList<ArrayList> links = parser.getLinks(whoAmI);

        for (ArrayList link : links) {
            String routerName = (String) link.get(0);

            String[] ipPort = parser.getRouterIpPort(routerName).toArray(new String[0]);
            String serverIP = ipPort[0];
            int serverPort = Integer.parseInt(ipPort[1]);

            DatagramPacket packetOfSendingTable = tableToPacket((HashMap<String, ArrayList<String>>) routerTable, serverIP, serverPort, whoAmI);

            send(packetOfSendingTable);
            System.out.println(serverIP + ":" + serverPort);
        }
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


    public static String returnNextHop(Map<String, ArrayList<String>> routerTable, String input) {

        if (routerTable.containsKey(input)) {
            ArrayList<String> routes = routerTable.get(input);
            return routes.get(1);
        } else {
            return null;
        }
    }
    private void service(String whoAmI) throws IOException {
        Parser parser = new Parser();
        parser.mapFromConfig();

        while (true){
            System.out.println("In Service");
            System.out.println(routerTable);
            DatagramPacket receivedPacket = new DatagramPacket(new byte[1024], 1024);
            socket.receive(receivedPacket);
            byte[] receivedPacketBuffer = receivedPacket.getData();

            char specialChar = (char) receivedPacketBuffer[0];
            if (specialChar == '@') {
                String routerRecievedName = receivedRouter(receivedPacketBuffer);
                HashMap<String, ArrayList<String>> receivedRouterTable = packetToTable(receivedPacketBuffer, routerRecievedName, whoAmI);
                boolean resultOfCheck = checkIfFound(receivedRouterTable, routerRecievedName);
                if (resultOfCheck) {
                    System.out.println("Flooded");
                    floodTable(whoAmI);
                }
            }else{

                String message = new String((receivedPacketBuffer));
                System.out.println(message);
                System.out.println("Destination Subnet: " + getDesSubnet(receivedPacketBuffer));

                send(buildDataPacket(receivedPacketBuffer, whoAmI));


            }

        }
    }



}
