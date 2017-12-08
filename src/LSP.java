import java.util.*;

public class LSP {
    public int originRouter;
    public int sequenceNumber;  //Start by 1, incremented as each new one generated
    public int TTL;     //Start by 10
    public Map<Router, Integer> dirConRouters;  //Directly connected routers, along with the cost.
    
    public LSP(Router rt) {
        originRouter = rt.ID;
        sequenceNumber = ++rt.sequenceNumber;
        TTL = 10;
    }
    
    public LSP(LSP lsp) {
        originRouter = lsp.originRouter;
        sequenceNumber = lsp.sequenceNumber;
        TTL = lsp.TTL - 1;
    }
}
