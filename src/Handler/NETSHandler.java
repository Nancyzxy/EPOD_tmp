package Handler;

import framework.EdgeNode;
import framework.EdgeNodeNetwork;
import framework.UnitInNode;
import utils.Constants;
import java.util.ArrayList;
import java.util.List;

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

    public boolean neighboringSet(ArrayList<?> c1, ArrayList<?> c2) {
        double ss = 0;
        double neighCellIdxDist = Math.sqrt(Constants.subDim)*2;
        double neighCellFullDimIdxDist = Math.sqrt(Constants.dim)*2;
        double cellIdxDist = (c1.size() == Constants.dim? neighCellFullDimIdxDist : neighCellIdxDist);
        double threshold =cellIdxDist*cellIdxDist;
        for(int k = 0; k<c1.size(); k++) {
            short x1 = (short) c1.get(k);
            short x2 = (short) c2.get(k);
            ss += Math.pow(x1-x2,2);
            if (ss >= threshold) return false;
        }
        return true;
    }

}
