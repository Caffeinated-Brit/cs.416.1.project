package org.example;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//10.222.21.224
//10.222.21.224

public class Client {

    private static DatagramSocket socket = null;

    public Client(int myPort)throws SocketException {
        socket = new DatagramSocket(myPort);

    }
    public static void main(String[] args) {
        ExecutorService es = Executors.newFixedThreadPool(4);
        Parser parser = new Parser();
        parser.mapFromConfig(); // Load data from config.json
        String whoAmI = args[0];
        String whereAmI = parser.getMySubnet(whoAmI);
        String gateWayRouter = String.valueOf(parser.getSubnetRouter(whereAmI));

        ArrayList<String> ipAndPortsList = parser.getIpAndPort(whoAmI);
        int myPort = Integer.parseInt(ipAndPortsList.get(1));
        System.out.println("WhoAmI: " + whoAmI);
        System.out.println("WhereAmI: " + whereAmI);
        System.out.println("My Port: " + myPort);

        ArrayList<String> neighborsList = parser.getNeighbors(whoAmI);
        System.out.println("List of Neighbors");
        System.out.println(neighborsList);

        try {
            Client client = new Client(myPort);
                Receive receiveTask = new Receive(whoAmI);
                es.submit(receiveTask);

            while(true) {

                InetAddress serverIP = InetAddress.getByName(parser.getIpAndPort(neighborsList.get(0)).get(0));
                int serverPort = Integer.parseInt(parser.getIpAndPort(neighborsList.get(0)).get(1));

                client.service(serverIP,serverPort, whoAmI, whereAmI,gateWayRouter);
            }
        }catch(SocketException e){
            System.out.println("Socket Error");
        }catch(UnknownHostException e){
            System.out.println("Unknown Host");
        }catch (IOException e){
            System.out.println("IO Exception Error");
        }finally {
            System.out.println("Shutting down...");
            es.shutdown();
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

    DatagramPacket buildPacket(String userInputDesMac, String userInputDesSub, String srcSub, String srcMac, String desSub, String desMac, String message,InetAddress serverIP, int serverPort){
        String build = userInputDesMac + "." + userInputDesSub +"."+ srcSub + "." + srcMac + "." + desSub + "." + desMac + "." + message;
        //should look like this now pc4.n4.n1.pc1.n4.pc4.lalalalala
        byte [] buffer = build.getBytes();
        DatagramPacket request = new DatagramPacket(buffer,buffer.length,serverIP,serverPort);
        return request;
    }


    public static String getRightDes(String srcSub, String desSub, String gateWayRouter) {
        if (srcSub.equals(desSub)){
            return srcSub;
        }
        else{
            return gateWayRouter;
        }
    }

    public void service(InetAddress serverIP, int serverPort, String whoAmI, String whereAmI, String gateWayRouter) throws IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter recipient mac: ");
        String userInputDesMac = scanner.nextLine();
        System.out.println("Enter recipient subnet: ");
        String userInputDesSub = scanner.nextLine();

        String checkedDesSub = getRightDes(whereAmI,userInputDesSub,gateWayRouter);


        System.out.printf("Enter message you want to send to %s through %d\n",serverIP.toString(),serverPort);
        String message = scanner.nextLine();

        //byte [] buffer = message.getBytes();
        //DatagramPacket request = new DatagramPacket(buffer,buffer.length,serverIP,serverPort);

        DatagramPacket request = buildPacket(userInputDesMac,checkedDesSub,whereAmI, whoAmI, userInputDesMac, userInputDesSub, message, serverIP, serverPort);
        socket.send(request);

    }

    static class Receive implements Runnable {
        private final String whoAmI;

        public Receive( String whoAmI) {
            this.whoAmI = whoAmI;
        }

        @Override
        public void run() {
            System.out.println("Ready to Receive");
            while (true) {
                DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                try {
                    socket.receive(response);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                byte[] responseMessage = response.getData();
                //String message = new String((responseMessage));
                //String name = String.valueOf(message.charAt(1));
                String name = getDesMac(responseMessage);


                //System.out.println("Intended Message Recipient: " + name);
                //System.out.println("Who am I: " + whoAmI);
                if ( name.equals(whoAmI) ) {
                    //String stringMessage = new String(responseMessage);
                    //System.out.println(stringMessage.substring(2).trim());
                    System.out.println(getMessage(responseMessage));
                    System.out.println("Enter recipient: ");
                }

            }
        }
    }
}

