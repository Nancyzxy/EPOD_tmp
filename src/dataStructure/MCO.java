package dataStructure;
import be.tarsos.lsh.Vector;
import mtree.tests.Data;

import java.util.ArrayList;

public class MCO extends Data {
    //全部点
    public boolean isInFilledCluster;
    public boolean isCenter;
    public MCO center;
    public long ev; //记录min数量的最早preceeding的exp time, 如果safe了记为0
    public long last_calculate_time;

    public ArrayList<Integer> exps;
    public int numberOfSucceeding;
//        public ArrayList<Integer> Rmc;


    public MCO(Data d) {
        super();
        this.arrivalTime = d.arrivalTime;
        this.values = d.values;
        center = null;
//            Rmc = new ArrayList<>();
        isInFilledCluster = false;
        isCenter = false;
        ev = 0;
        last_calculate_time = -1;
        exps = new ArrayList<>();
        numberOfSucceeding = 0;
    }
}
