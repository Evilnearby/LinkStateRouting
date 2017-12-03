import java.io.*;
import java.util.*;

public class Router {
    public static Map<Integer, Router> allRouters;
    public int ID;
    public String networkName;  //The name of network, i.e., 192.168.0.1
    public Map<Router, Integer> adjacentRouters;    //Directly connected routers, along with the costs
    //Key: ID of directly connected routers, Value: tick value
    public Map<Integer, Integer> ticks;
    public boolean isOn;
    //Routing table
    
    //Key:LSP's originate router, Value: LSP's sequenceNumber
    public Map<Integer, Integer> receivedLSPs;

    public final static int infinity = 99999999;
    
    public static void readInfile() throws IOException {
        BufferedReader bd = new BufferedReader(new InputStreamReader(new FileInputStream("infile.dat")));
        String line;
        while((line = bd.readLine()) != null){
            String[] words = line.split("[ \t]+");
            //The title lines
            if (words[0].length() != 0) {
                
            }
            //The adjacent lines
            else {
                
            }
        }
    }
    
    private static String promptUsers() {
        System.out.println("If you want to continue, enter \"C\"");
        System.out.println("If you want to quit, enter \"Q\"");
        System.out.println("If you want to print routing table, enter \"P\" followed by the router's id number");
        System.out.println("If you want to shut down a router, enter \"S\" followed by the router's id number");
        System.out.println("If you want to start up a router, enter \"T\" followed by the router's id number");
        Scanner sc = new Scanner(System.in);
        String st = sc.nextLine();
        return st;
    }
    
    private static void cmdContinue() {
        
    }
    
    private static void printRT(int routerID) {
        
    }
    
    private static void shutDown(int routerID) {
        
    }
    
    private static void startUp(int routerID) {
        
    }
    
    public void receivePacket(LSP lsp, int senderID) {
        receivedLSPs.put(lsp.originRouter, lsp.sequenceNumber);
        LSP forwardedLSP = new LSP(this, lsp);
        //Discard the LSP
        if (forwardedLSP.TTL == 0 || (receivedLSPs.containsKey(senderID) && receivedLSPs.get(senderID) >= lsp.sequenceNumber)) {
            return;
        }
        //Distribute the LSP
        for (Router adj : adjacentRouters.keySet()) {
            if (adj.ID != senderID && adj.isOn) {
                adj.receivePacket(forwardedLSP, this.ID);
            }
        }
    }
    
    public void originatePacket() {
        for (Router r : this.adjacentRouters.keySet()) {
            if (r.isOn) {
                this.ticks.put(r.ID, this.ticks.get(r.ID) + 1); //tick
                if (this.ticks.get(r.ID) >= 2) {
                    this.adjacentRouters.put(r, infinity);
                    return;
                }
                r.receivePacket(new LSP(this), this.ID);
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        //readInfile();
        
    }
}
