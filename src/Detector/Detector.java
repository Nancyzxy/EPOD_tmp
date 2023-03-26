package Detector;

import dataStructure.Vector;
import framework.Device;

import java.util.*;

public abstract class Detector {
    public Set<? extends Vector> outlierVector; // This field is only used to return to the global network
    Device device;
    public Detector(Device device){
        this.device = device;
    }
    public abstract void detectOutlier(List<Vector> data);

    //pruning + 后续处理
    public abstract void processOutliers();

    public abstract HashMap<ArrayList<?>,List<? extends Vector>> sendData(HashSet<ArrayList<?>> bucketIds);
}
