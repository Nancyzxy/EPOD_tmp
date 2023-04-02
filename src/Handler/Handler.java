package Handler;

import framework.EdgeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Handler {
    EdgeNode node;
    public Handler(EdgeNode node){
        this.node = node;
    }

    public abstract boolean neighboringSet(ArrayList<?> c1, ArrayList<?> c2);
}
