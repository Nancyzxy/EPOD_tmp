package Handler;

import framework.EdgeNode;
import framework.EdgeNodeNetwork;
import framework.UnitInNode;
import java.util.ArrayList;
import java.util.List;
import RPC.RPCFrame;
import utils.Constants;

public class NETSHandler extends Handler{
    public NETSHandler(EdgeNode node) {
        super(node);
    }

    @Override
    public void handle(List<ArrayList<Short>> unSateUnits, int nodeHashCode) {
        ArrayList<Thread> threads = new ArrayList<>();
        for (ArrayList<Short> unit:unSateUnits){
            Thread t = new Thread(()->{
                List<UnitInNode> unitInNodeList = this.node.unitsStatusMap.values().stream()
                        .filter(x -> x.isUpdated.get(node.hashCode())==1)
                        .filter(x -> neighboringSet(unit,x.unitID)).toList();
                unitInNodeList.forEach(x -> x.isUpdated.put(node.hashCode(),0)); // TODO: CHECK whether is right
                Object[] parameters = new Object[]{unit, unitInNodeList};
                try {
                    node.invoke("localhost", EdgeNodeNetwork.nodeHashMap.get(nodeHashCode).port,
                            EdgeNode.class.getMethod("collectFromNode", ArrayList.class, List.class), parameters);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                };
            });
            threads.add(t);
        }

        for (Thread t:threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    public boolean neighboringSet(ArrayList<Short> c1, ArrayList<Short> c2) {
        double ss = 0;
        double cellIdxDist = Math.sqrt(Constants.dim)*2;
        double threshold =cellIdxDist*cellIdxDist;
        for(int k = 0; k<c1.size(); k++) {
            ss += Math.pow((c1.get(k) - c2.get(k)),2);
            if (ss >= threshold) return false;
        }
        return true;
    }

}
