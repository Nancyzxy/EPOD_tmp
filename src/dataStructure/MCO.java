package dataStructure;

import java.util.ArrayList;

public class MCO extends Vector {
    //全部点
    public boolean isInFilledCluster;
    public boolean isCenter;
    public MCO center;
    public long ev; //记录min数量的最早preceeding的exp time, 如果safe了记为0
    public int last_calculate_time; //指的是slideID

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
        last_calculate_time = -1;
        exps = new ArrayList<>();
        numberOfSucceeding = 0;
    }
}
