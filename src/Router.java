import java.io.*;
import java.util.*;

class Triple {
    String networkName;
    Router outgoing;
    int cost;

    public Triple(String networkName, Router outgoing, int cost) {
        this.networkName = networkName;
        this.outgoing = outgoing;
        this.cost = cost;
    }
}

public class Router {
    public static Map<Integer, Router> allRouters = new HashMap<>();
    public final static int infinity = 99999999;
    
    public int ID;
    public String networkName;  //The name of network, i.e., 192.168.0.1
    public Map<Integer, Integer> adjacentRouters;    //Directly connected routers, along with the costs
    //Key: ID of directly connected routers, Value: last received tick value
    public Map<Integer, Integer> lastTickVal;
    public boolean isOn;
    //Routing table
    
    static int tick = 0;
    //Key:LSP's originate router, Value: LSP's sequenceNumber
    public Map<Integer, Integer> receivedLSPs;
    public Map<Router, Triple> routingTable;
    
    public Router(int ID, String networkName) {
        this.ID = ID;
        this.networkName = networkName;
        this.adjacentRouters = new HashMap<>();
        this.lastTickVal = new HashMap();
        this.receivedLSPs = new HashMap<>();
        this.routingTable = new HashMap<>();
        this.isOn = true;
    }
    
    public static void readInfile() throws IOException {
        BufferedReader bd = new BufferedReader(new InputStreamReader(new FileInputStream("infile.dat")));
        String line;
        int currSource = 0;
        while((line = bd.readLine()) != null){
            String[] words = line.split("[ \t]+");
            //The title lines
            if (words[0].length() != 0) {
                currSource = Integer.valueOf(words[0]);
                Router newRouter = new Router(currSource, words[1]);
                allRouters.put(newRouter.ID, newRouter);
            }
            //The adjacent lines
            else {
                int cost = (words.length == 2 || words[2].length() == 0) ? 1 : Integer.valueOf(words[2]);
                int neigh = Integer.valueOf(words[1]);
                allRouters.get(currSource).adjacentRouters.put(neigh, cost);
            }
        }
    }
    
    static void initializeRoutingTable() {
        for (Router r : allRouters.values()) {
            for (Router object : allRouters.values()) {
                if (r.equals(object)) {
                    continue;
                }
                Triple tri = new Triple(object.networkName, null, infinity);
                r.routingTable.put(object, tri);
            }
        }
        
        for (Router r : allRouters.values()) {
            r.dijkstra();
        }
    }
    
    private static String promptUsers() {
        System.out.println("If you want to continue, enter \"C\"");
        System.out.println("If you want to quit, enter \"Q\"");
        System.out.println("If you want to print routing table, enter \"P\" followed by the router's id number");
        System.out.println("If you want to shut down a router, enter \"S\" followed by the router's id number");
        System.out.println("If you want to start up a router, enter \"T\" followed by the router's id number");
        Scanner sc = new Scanner(System.in);
        String res = sc.nextLine();
        return res;
    }
    
    private static void continueRouting() {
        tick++;
        if (tick > 1) {
            boolean changed = false;
            for (Router rt : allRouters.values()) {
                Set<Integer> tempAdj = new HashSet<>(rt.adjacentRouters.keySet());
                for (int adj : tempAdj) {
                    if (!rt.lastTickVal.containsKey(adj) || tick - rt.lastTickVal.get(adj) >= 2) {
                        System.out.println(adj);
                        rt.adjacentRouters.remove(adj);
                        changed = true;
                    }
                }
            }
            if (changed) {
                for (Router rt : allRouters.values()) {
                    rt.dijkstra();
                }
            }
        }
        
        for (Router r : allRouters.values()) {
            r.originatePacket();
        }
    }
    
    private void printRoutingTable() {
        if (!this.isOn) {
            System.out.println("This router is shut down");
        }
        for (Triple tri : this.routingTable.values()) {
            System.out.println(tri.networkName + ", " + tri.outgoing.ID + ", " + tri.cost);
        }
    }
    
    //Dijkstra to update this router's routing table
    private void dijkstra() {
        Set<Router> V_S = new HashSet<>(allRouters.values());
        Set<Router> S = new HashSet<>();
        Map<Router, Integer> D = new HashMap<>();  //D stores current minimum cost
        Map<Router, Router> outgoings = new HashMap<>();

        V_S.remove(this);
        S.add(this);    //S contains selected nodes whose shortest distance from the source is already known
        
        for (Router target : V_S) {
            outgoings.put(target, target);    //First, outgoing links are all themselves
        }
        
        for (Router r : V_S) {
            D.put(r, this.getCost(r.ID));
        }
        
        while (!V_S.isEmpty()) {
            Router min = Collections.min(V_S, (Router r1, Router r2) -> D.get(r1) - D.get(r2));
            V_S.remove(min);
            S.add(min);
            if (!min.isOn) {
                continue;
            }
            
            for (Router r : V_S) {
                //If a detour is less cost
                boolean shortcut = D.get(r) > (D.get(min) + min.getCost(r.ID));
                //Update the distance from source to r
                D.put(r, Math.min(D.get(r), D.get(min) + min.getCost(r.ID)));
                if (shortcut) {
                    //If there is a shortcut by detour, then we should allocate this router's outgoing to min's outgoing
                    outgoings.put(r, outgoings.get(min));
                }
            }
        }
        
        for (Router target : S) {
            if (target == this) {
                continue;
            }
            this.routingTable.get(target).outgoing = outgoings.get(target);
            this.routingTable.get(target).cost = D.get(target);
        }
    }
    
    //Get cost from this router to the neighbor router
    private int getCost(int neigh) {
        if (this.ID == neigh) {
            return 0;
        }
        if (this.adjacentRouters.containsKey(neigh)) {
            return this.adjacentRouters.get(neigh);
        }
        return infinity;
    }
    
    private static void shutDown(int routerID) {
        allRouters.get(routerID).isOn = false;
    }
    
    private static void startUp(int routerID) {
        allRouters.get(routerID).isOn = true;
    }
    
    public void receivePacket(LSP lsp, int senderID) {
        receivedLSPs.put(lsp.originRouter, lsp.sequenceNumber);
        LSP forwardedLSP = new LSP(lsp);
        //Discard the LSP
        if (forwardedLSP.TTL == 0 || (receivedLSPs.containsKey(senderID) && receivedLSPs.get(senderID) >= lsp.sequenceNumber)) {
            return;
        }
        //Forward the LSP
        for (int adj : adjacentRouters.keySet()) {
            if (allRouters.get(adj).ID != senderID && allRouters.get(adj).isOn) {
                allRouters.get(adj).lastTickVal.put(this.ID, tick); //update the last received tick value on the receiving router
                allRouters.get(adj).receivePacket(forwardedLSP, this.ID);
            }
        }
    }
    
    public void originatePacket() {
        for (int r : this.adjacentRouters.keySet()) {
            if (allRouters.get(r).isOn) {
                allRouters.get(r).lastTickVal.put(this.ID, tick); //update the last received tick value on the receiving router
                allRouters.get(r).receivePacket(new LSP(this), this.ID);
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        readInfile();
        initializeRoutingTable();
        String ans = "";
        /*allRouters.get(0).printRoutingTable();
        for (int r : allRouters.get(0).adjacentRouters.keySet()) {
            System.out.println("router" + r);
            System.out.println("cost" + allRouters.get(0).adjacentRouters.get(r));
        }*/
        while(true) {
            ans = promptUsers();
            if (ans.equals("Q")) {
                System.out.println("Program is ended.");
                break;
            }
            int router = 0;
            if (ans.length() > 1) {
                router = Integer.valueOf(ans.substring(1, ans.length()));
            }
            switch(ans.charAt(0)) {
                case 'C':
                    continueRouting();
                    break;
                case 'P':
                    allRouters.get(router).printRoutingTable();
                    break;
                case 'S':
                    shutDown(router);
                    break;
                case 'T':
                    startUp(router);
                    break;
                default:
                    System.out.println("Bad input");
                    break;
            }
        }
    }
}
