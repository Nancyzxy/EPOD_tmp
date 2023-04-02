package Detector;

import dataStructure.MCO;
import dataStructure.Tuple;
import dataStructure.Vector;
import framework.Device;

import java.util.*;

public abstract class Detector {
    public Set<? extends Vector> outlierVector; // This field is only used to return to the global network
    public Map<Integer, Map<ArrayList<?>, List<Vector>>> external_data;
    public HashMap<ArrayList<?>, Integer> status;
    Device device;
    public Detector(Device device){
        this.device = device;
        this.external_data = Collections.synchronizedMap(new HashMap<>());//TODO:�������ͳһ
    }
    public abstract void detectOutlier(List<Vector> data);

    //pruning + ��������
    public abstract void processOutliers();

    public abstract Map<ArrayList<?>,List<Vector>> sendData(HashSet<ArrayList<?>> bucketIds, int edgeNodeHashCode, int lastSent);
}
