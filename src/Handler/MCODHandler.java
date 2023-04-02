package Handler;

import framework.EdgeNode;
import java.util.ArrayList;
import java.util.List;

public class MCODHandler extends Handler{
    public MCODHandler(EdgeNode node) {
        super(node);
    }

    @Override
    public void handle(List<ArrayList<Short>> unSateUnits, int nodeHashcode) {

    }

    @Override
    public boolean neighboringSet(ArrayList<?> c1, ArrayList<?> c2) {
        return false;
    }
}
