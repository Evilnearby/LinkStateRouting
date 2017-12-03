import java.util.*;

public class LSP {
    public int originRouter;
    public int sequenceNumber;  //Start by 1, incremented as each new one generated
    public int TTL;     //Start by 10
    public Map<Router, Integer> dirConRouters;  //Directly connected routers, along with the cost.
    
    public LSP(Router rt) {
        originRouter = rt.ID;
        sequenceNumber = 1;
        TTL = 10;
    }
    
    public LSP(Router rt, LSP lsp) {
        originRouter = rt.ID;
        sequenceNumber = lsp.sequenceNumber + 1;
        TTL = lsp.TTL - 1;
    }
}
