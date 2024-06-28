package org.example;

import java.util.ArrayList;

public class test {
    public static void main(String[] args) {
        Parser parser = new Parser();
        parser.mapFromConfig(); // Load data from config.json

        // Call getNeighbors and getIpAndPort methods
        ArrayList<String> neighborsList = parser.getNeighbors("S1");
        System.out.println(neighborsList);
        //for (String neighbor : neighborsList) {
        //    System.out.println("Neighbor: " + neighbor);
        //}

        ArrayList<String> ipAndPortsList = parser.getIpAndPort("S1");
        System.out.println(ipAndPortsList);
        //for (String item : ipAndPortsList) {
        //   System.out.println("IP and Port: " + item);
        //}
    }
}