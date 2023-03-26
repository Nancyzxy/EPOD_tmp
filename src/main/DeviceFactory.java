package main;

import be.tarsos.lsh.Index;
import be.tarsos.lsh.families.EuclidianHashFamily;
import be.tarsos.lsh.families.HashFamily;
import utils.Constants;

import java.util.Objects;

public class DeviceFactory {

    /* used only for LSH method */
    public HashFamily hashFamily;
    public Index index;
    public int NumberOfHashTables;
    public int NumberOfHashes;


    public DeviceFactory(int numberOfHashes, int numberOfHashTables){
        EuclidianHashFamily hashFamily;
        if ((int) (1 * Constants.R) == 0) {
            hashFamily = new EuclidianHashFamily(4, Constants.dim);
        } else {
            hashFamily = new EuclidianHashFamily((int) (10*Constants.R), Constants.dim);
        }
        this.hashFamily = hashFamily;
        this.NumberOfHashes = numberOfHashes;
        this.NumberOfHashTables = numberOfHashTables;
        this.index = new Index(hashFamily,NumberOfHashes,NumberOfHashTables);
    }

    public DeviceFactory(){}

    public Device createEdgeDevice(int deviceId) throws Throwable {
        Device device  = new Device(deviceId);
        if (Objects.equals(Constants.methodToGenerateFingerprint, "LSH")){
            device.setNumberOfHashTables(NumberOfHashTables);
            device.setIndex(index);
        }
        return device;
    }




}
