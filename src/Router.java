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
    //The graph this router understands, using adjacent list
    //Key, Source, Value, the router&cost map from this source.
    public Map<Integer, Map<Integer, Integer>> adjacentRouters;
    //Key: ID of directly connected routers, Value: last received tick value
    public Map<Integer, Integer> lastTickVal;
    public boolean isOn;
    //Routing table
    public int sequenceNumber;
    public Set<Router> offRouters;
    
    static int tick = 0;
    //Key:LSP's originate router, Value: LSP's sequenceNumber
    public Map<Integer, Integer> receivedLSPs;
    //Key: Target router value: the triple value
    public Map<Router, Triple> routingTable;
    
    public Router(int ID, String networkName) {
        this.ID = ID;
        this.networkName = networkName;
        this.adjacentRouters = new HashMap<>();
        this.adjacentRouters.put(this.ID, new HashMap<Integer, Integer>());
        this.lastTickVal = new HashMap<>();
        this.receivedLSPs = new HashMap<>();
        this.routingTable = new HashMap<>();
        this.isOn = true;
        this.sequenceNumber = 1;
        this.offRouters = new HashSet<>();
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
                //Initially, in each router's graph, there is only one entry which is its own network
                Router currRouter = allRouters.get(currSource);
                currRouter.adjacentRouters.get(currSource).put(neigh, cost);
                currRouter.adjacentRouters.putIfAbsent(neigh, new HashMap<>());
                currRouter.adjacentRouters.get(neigh).put(currSource, cost);
            }
        }
    }
    
    static void initializeRoutingTable() {
        for (Router r : allRouters.values()) {
            for (int routerID : r.adjacentRouters.get(r.ID).keySet()) {
                //Object is one of the router r's directly connected routers
                Router object = allRouters.get(routerID);
                if (r.equals(object)) {
                    continue;
                }
                Triple tri = new Triple(object.networkName, object, r.adjacentRouters.get(r.ID).get(object.ID));
                r.routingTable.put(object, tri);
            }
        }
    }
    
    private static String promptUsers() {
        System.out.println("If you want to continue, enter \"C\"");
        System.out.println("If you want to quit, enter \"Q\"");
        System.out.println("If you want to print routing table, enter \"P\" followed by the router's id number, without space");
        System.out.println("If you want to shut down a router, enter \"S\" followed by the router's id number, without space");
        System.out.println("If you want to start up a router, enter \"T\" followed by the router's id number, without space");
        Scanner sc = new Scanner(System.in);
        String res = sc.nextLine();
        return res;
    }
    
    private static void continueRouting() {
        tick++;
        if (tick > 1) {
            for (Router rt : allRouters.values()) {
                if (!rt.isOn) {
                    continue;
                }
                Set<Integer> tempAdj = new HashSet<>(rt.adjacentRouters.get(rt.ID).keySet());
                for (int adj : tempAdj) {
                    if (tick - rt.lastTickVal.get(adj) >= 2) {
                        //System.out.println(rt.ID + " has disconnected " + adj);
                        rt.offRouters.add(allRouters.get(adj));
                        rt.dijkstra();
                        //todo:check if this removal is correct, regarding ticking
                    }
                }
            }
        }
        
        for (Router r : allRouters.values()) {
            if (r.isOn) {
                r.originatePacket();
            }
        }
    }
    
    private void printRoutingTable() {
        if (!this.isOn) {
            System.out.println("This router is shut down");
        }
        System.out.println("Network\t\t\tOutgoing\tCost\t\t");
        System.out.println("--------------------------------");
        for (Triple tri : this.routingTable.values()) {
            String cost = tri.cost < infinity ? String.valueOf(tri.cost) : "infinity";
            System.out.println(tri.networkName + "\t\t" + tri.outgoing.ID + "\t\t\t" + cost);
        }
        System.out.println();
    }
    
    //Dijkstra to update this router's routing table
    private void dijkstra() {
        Set<Router> V_S = new HashSet<>();
        for (int rt : this.adjacentRouters.keySet()) {
            V_S.add(allRouters.get(rt));
        }
        Set<Router> S = new HashSet<>();
        Map<Router, Integer> D = new HashMap<>();  //D stores current minimum cost
        Map<Router, Router> outgoings = new HashMap<>();

        V_S.remove(this);
        S.add(this);    //S contains selected nodes whose shortest distance from the source is already known
        
        for (Router target : V_S) {
            outgoings.put(target, target);    //First, outgoing links are all themselves
        }
        
        for (Router r : V_S) {
            D.put(r, this.getCost(r.ID, this));
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
                boolean shortcut = D.get(r) > (D.get(min) + min.getCost(r.ID, this));
                //Update the distance from source to r
                D.put(r, Math.min(D.get(r), D.get(min) + min.getCost(r.ID, this)));
                if (shortcut) {
                    //If there is a shortcut by detour, then we should allocate this router's outgoing to min's outgoing
                    outgoings.put(r, outgoings.get(min));
                }
            }
        }
        
        S.remove(this);
        
        for (Router target : S) {
            if (this.routingTable.containsKey(target)) {
                this.routingTable.get(target).outgoing = outgoings.get(target);
                this.routingTable.get(target).cost = D.get(target);
            } else {
                Triple triple = new Triple(target.networkName, outgoings.get(target), D.get(target));
                this.routingTable.put(target, triple);
            }
        }
    }
    
    //Get cost from this router to the neighbor router
    private int getCost(int target, Router source) {
        if (this.ID == target) {
            return 0;
        }
        if (source.offRouters.contains(allRouters.get(target))) {
            return infinity;
        }
        if (this.adjacentRouters.get(this.ID).containsKey(target)) {
            return this.adjacentRouters.get(this.ID).get(target);
        }
        return infinity;
    }
    
    private static void shutDown(int routerID) {
        allRouters.get(routerID).isOn = false;
    }
    
    private static void startUp(int routerID) {
        allRouters.get(routerID).isOn = true;
        Router thisRouter = allRouters.get(routerID);
        for (int adj : thisRouter.adjacentRouters.get(thisRouter.ID).keySet()) {
            Router adjRouter = allRouters.get(adj);
            //Update the started up's tick vals
            thisRouter.lastTickVal.put(adj, tick);
            //Update the started up's neighbors' tick vals
            adjRouter.lastTickVal.put(routerID, tick);
        }
    }
    
    public void receivePacket(LSP receivedLsp, int senderID) {
        LSP lsp = new LSP(receivedLsp);
        //Discard the LSP ?????
        if (lsp.TTL == 0 || (receivedLSPs.containsKey(lsp.originRouter) && receivedLSPs.get(lsp.originRouter) >= lsp.sequenceNumber) || lsp.originRouter == this.ID) {
            //System.out.println("origin:" + lsp.originRouter + "ttl:" + lsp.TTL);
            return;
        }
        receivedLSPs.put(lsp.originRouter, lsp.sequenceNumber);
        //Update
        boolean changed = false;
        
        for (Router offRouter : lsp.offRouters) {
            changed = true;
            this.offRouters.add(offRouter);
        }
        
        if (this.offRouters.contains(allRouters.get(senderID))) {
            changed = true;
            this.offRouters.remove(allRouters.get(senderID));
        }
        if (this.offRouters.contains(allRouters.get(lsp.originRouter))) {
            changed = true;
            this.offRouters.remove(allRouters.get(lsp.originRouter));
        }
        
        Set<Router> tempOffRouters = new HashSet<>(this.offRouters);
        for (Router offRouter : tempOffRouters) {
            if (this.adjacentRouters.get(lsp.originRouter).containsKey(offRouter.ID) && !lsp.offRouters.contains(offRouter)) {
                changed = true;
                this.offRouters.remove(offRouter);
            }
        }
        
        //Merge the graph of the LSP with its own graph
        changed = changed || lsp.updateAdjacentList(this);
        
        if (changed) {
            this.dijkstra();
        }
        //Forward the LSP
        for (int adj : this.adjacentRouters.get(this.ID).keySet()) {
            if (allRouters.get(adj).ID != senderID && allRouters.get(adj).isOn) {
                allRouters.get(adj).lastTickVal.put(this.ID, tick); //update the last received tick value on the receiving router
                allRouters.get(adj).receivePacket(lsp, this.ID);
            }
        }
    }
    
    public void originatePacket() {
        for (int r : this.adjacentRouters.get(this.ID).keySet()) {
            if (allRouters.get(r).isOn) {
                allRouters.get(r).lastTickVal.put(this.ID, tick); //update the last received tick value on the receiving router
                allRouters.get(r).receivePacket(new LSP(this), this.ID);
            }
        }
        this.sequenceNumber++;
    }
    
    public static void main(String[] args) throws IOException {
        readInfile();
        initializeRoutingTable();
        String ans = "";
        /*continueRouting();
        shutDown(5);
        continueRouting();
        continueRouting();
        continueRouting();
        allRouters.get(0).printRoutingTable();
        for (int r : allRouters.get(0).adjacentRouters.get(0).keySet()) {
            System.out.println("router" + r);
            System.out.println("cost" + allRouters.get(0).adjacentRouters.get(0).get(r));
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
