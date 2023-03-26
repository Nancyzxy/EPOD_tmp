package Detector;

import dataStructure.Vector;
import framework.Device;
import java.util.HashSet;
import java.util.List;

public abstract class Detector {
    Device device;
    public Detector(Device device){
        this.device = device;
    }
    public abstract void detectOutlier(List<Vector> data);
}
