import java.util.*;

public class LSP {
    public int originRouter;
    public int sequenceNumber;  //Start by 1, incremented as each new one generated
    public int TTL;     //Start by 10
    public Map<Integer, Map<Integer, Integer>> reachableRouters;  //Directly connected routers, along with the cost.
    public Set<Router> offRouters;
    
    public LSP(Router rt) {
        originRouter = rt.ID;
        sequenceNumber = ++rt.sequenceNumber;
        TTL = 10;
        reachableRouters = new HashMap<>(rt.adjacentRouters);
        this.offRouters = new HashSet<>(rt.offRouters);
    }
    
    public LSP(LSP lsp) {
        originRouter = lsp.originRouter;
        sequenceNumber = lsp.sequenceNumber;
        TTL = lsp.TTL - 1;
        reachableRouters = lsp.reachableRouters;
    }
    
    public boolean updateAdjacentList(Router receivingRouter) {
        boolean changed = false;
        for (int source : this.reachableRouters.keySet()) {
            if (source == receivingRouter.ID) {
                continue;
            }
            Map<Integer, Integer> oldCostMap = receivingRouter.adjacentRouters.get(source);
            Map<Integer, Integer> newCostMap = this.reachableRouters.get(source);
            //If the receiving router has know about this source, let's see if its data is up to date
            if (receivingRouter.adjacentRouters.containsKey(source)) {
                //Source: the co-existing source router
                for (int adj : newCostMap.keySet()) {
                    if (oldCostMap.containsKey(adj)) {
                        if (oldCostMap.get(adj) != newCostMap.get(adj)) {
                            changed = true;
                            oldCostMap.put(adj, newCostMap.get(adj));
                        }
                    } else {
                        changed = true;
                        oldCostMap.put(adj, newCostMap.get(adj));
                    }
                }
                /*//Case 1: New map's size is greater than old one, means the newMap is a complete one
                if (oldCostMap.keySet().size() < newCostMap.keySet().size()) {
                    changed = true;
                    receivingRouter.adjacentRouters.put(source, new HashMap<>(newCostMap));
                } 
                //Cae 2: Sizes are equal. Check if there is cost change
                else if (oldCostMap.keySet().size() == newCostMap.keySet().size()) {
                    for (int routerID : newCostMap.keySet()) {
                        if (newCostMap.get(routerID) != oldCostMap.get(routerID)) {
                            changed = true;
                            oldCostMap.put(routerID, newCostMap.get(routerID));
                        }
                    }
                }*/
            }
            //If the receiving router has not known this source, we add the source and its cost map to the router
            else {
                changed = true;
                receivingRouter.adjacentRouters.put(source, new HashMap<>(newCostMap));
            }
        }
        return changed;
    }
}
