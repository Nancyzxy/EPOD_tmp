package dataStructure;
import be.tarsos.lsh.Vector;
import mtree.tests.Data;

import java.util.ArrayList;

public class MCO extends Data {
    //全部点
    public boolean isInFilledCluster;
    public boolean isCenter;
    public MCO center;
    public int ev; //记录min数量的最早preceeding的exp time, 如果safe了记为0
    public int lastCalculated; //用于外部点更新

    public ArrayList<Integer> exps;
    public int numberOfSucceeding;
//        public ArrayList<Integer> Rmc;


    public MCO(Vector d) {
        super();
        this.arrivalTime = d.arrivalTime;
        this.values = d.values;
        center = null;
//            Rmc = new ArrayList<>();
        isInFilledCluster = false;
        isCenter = false;
        ev = 0;
//            lastCalculated =
        exps = new ArrayList<>();
        numberOfSucceeding = 0;
    }
}
