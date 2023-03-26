package Detector;

import be.tarsos.lsh.Vector;
import framework.Device;

import java.util.HashSet;
import java.util.List;

public abstract class Detector {
    Device device;
    public Detector(Device device){
        this.device = device;
    }
    public abstract HashSet<Vector> detectOutlier(List<Vector> data, long currentTime);
}
