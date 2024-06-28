package org.example;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.Key;
import java.util.*;


public class Parser {
    Map switchPcNeighbors;

    Map routerNeighbors;
    Map ipAndPorts;
    JSONObject links;
    Map routers;

    Map subnetOuterSwitches;

    Map switchOrPcSubnets;

    Map subnetRouters;

    Map pcSubnets;
    public static void main(String[] args) {
        Parser parser = new Parser();
        try {
            Object obj = new JSONParser().parse(new FileReader("config.json"));
            JSONObject jsonObject = (JSONObject) obj;
            parser.switchPcNeighbors = ((Map) jsonObject.get("switch_pc_neighbors"));
            parser.ipAndPorts = ((Map) jsonObject.get("ip_and_port"));
            parser.routerNeighbors = ((Map) jsonObject.get("router_neighbors"));
            parser.links = (JSONObject) jsonObject.get("links");
            parser.routers = ((Map) jsonObject.get("routers"));
            parser.subnetOuterSwitches = ((Map) jsonObject.get("subnet_outer_switches"));
            parser.switchOrPcSubnets = ((Map) jsonObject.get("switch_or_pc_subnets"));
            parser.subnetRouters = ((Map) jsonObject.get("subnet_routers"));


            //example for how to loop through each, can delete before submitting
            ArrayList<String> neighborsList = parser.getNeighbors("s1");
            for (String neighbor : neighborsList) {
                System.out.println(neighbor);
            }

            ArrayList<String> ipAndPortsList = parser.getIpAndPort("s1");
            for (String item : ipAndPortsList) {
                System.out.println(item);
            }

            ArrayList<String> rNeighbors = parser.getRouterNeighbors("r1");
            for (String item : rNeighbors) {
                System.out.println(item);
            }

            ArrayList<ArrayList> _links = parser.getLinks("r2");
            for (ArrayList link : _links) {
                System.out.println(link);
            }

            ArrayList<String> _routers = parser.getRouterIpPort("r1");
            for (String item : _routers) {
                System.out.println(item);
            }

            ArrayList<String> outerSwitchIpAndPort = parser.getOuterSwitchIpAndPort("n1");
            for (String item : outerSwitchIpAndPort) {
                System.out.println(item);
            }

            System.out.println("n1's router is " + parser.getSubnetRouter("n1"));

            System.out.println("s1's subnet is " + parser.getMySubnet("s1"));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void mapFromConfig() {
        try {
            Object obj = new JSONParser().parse(new FileReader("config.json"));
            JSONObject jsonObject = (JSONObject) obj;
            switchPcNeighbors = ((Map) jsonObject.get("switch_pc_neighbors"));
            ipAndPorts = ((Map) jsonObject.get("ip_and_port"));
            routerNeighbors = ((Map) jsonObject.get("router_neighbors"));
            links = (JSONObject) jsonObject.get("links");
            routers = ((Map) jsonObject.get("routers"));
            subnetOuterSwitches = ((Map) jsonObject.get("subnet_outer_switches"));
            switchOrPcSubnets = ((Map) jsonObject.get("switch_or_pc_subnets"));
            subnetRouters = ((Map) jsonObject.get("subnet_routers"));


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getNeighbors(String realMac) {
        return (ArrayList) switchPcNeighbors.get(realMac);
    }

    public ArrayList<String> getRouterNeighbors(String name) {return (ArrayList) routerNeighbors.get(name);}

    public ArrayList<String> getIpAndPort(String realMac) {
        return (ArrayList) ipAndPorts.get(realMac);
    }
    public ArrayList<ArrayList> getLinks(String name) {
        ArrayList<ArrayList> finalList = new ArrayList<>();

        JSONArray retrievedLinks = (JSONArray) links.get(name);
        for (Object object : retrievedLinks) {
            Map mappedObject = (Map) object;
            for (Object key : mappedObject.keySet()) {
                ArrayList<String> innerArrayList = new ArrayList<>();
                innerArrayList.add(key.toString());
                innerArrayList.add(mappedObject.get(key).toString());
                finalList.add(innerArrayList);
            }
        }
        return finalList;
     }

    public ArrayList<String> getRouterIpPort(String name) {return (ArrayList) routers.get(name);}


    public String getSubnetRouter(String subnetName) {
        return (String) subnetRouters.get(subnetName);
    }

    public ArrayList<String> getOuterSwitchIpAndPort(String subnetName) {
        ArrayList<String> returned = (ArrayList) subnetOuterSwitches.get(subnetName);
        return getIpAndPort(returned.get(0));
    }

    public String getOuterSwitchName(String subnetName) {
        ArrayList<String> returned = (ArrayList) subnetOuterSwitches.get(subnetName);
        return returned.get(0);
    }


    public String getMySubnet(String pcOrSwitchName) {
        return (String) switchOrPcSubnets.get(pcOrSwitchName);
    }
}

