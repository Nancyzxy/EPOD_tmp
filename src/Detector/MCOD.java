package Detector;

import be.tarsos.lsh.Vector;
import framework.Device;

import java.util.HashSet;
import java.util.List;

public class MCOD extends Detector{

    public MCOD(Device device) {
        super(device);
    }

    @Override
    public HashSet<Vector> detectOutlier(List<Vector> data, long currentTime) {
        return null;
    }
}
