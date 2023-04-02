package framework;

import be.tarsos.lsh.Index;
import be.tarsos.lsh.families.EuclidianHashFamily;
import be.tarsos.lsh.families.HashFamily;
import utils.Constants;

import java.util.Objects;

public class DeviceFactory {

    public Device createEdgeDevice(int deviceId) {
        return new Device(deviceId);
    }




}
