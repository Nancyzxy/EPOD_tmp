package test;

import framework.DeviceFactory;
import framework.EdgeNodeNetwork;
import utils.Constants;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Objects;

public class testNetwork {

    public static void main(String[] args) throws Throwable {
        PrintStream ps ;
        DeviceFactory edgeDeviceFactory;
        if (Objects.equals(Constants.methodToGenerateFingerprint,"LSH")){
            double p1=0.8;
            double p2=0.4;
            double k = Math.log(Constants.W)/Math.log(1/p2);
            double L = Math.pow(Constants.W,Math.log(1/p1)/Math.log(1/p2));
            int numberOfHashes = 30;
            int numberOfHashTables = 5;
            ps = new PrintStream(new FileOutputStream("src/Result/" + Constants.dataset + "_" +
                    Constants.methodToGenerateFingerprint + "_"+numberOfHashes+"_"+numberOfHashTables +".txt"));
            edgeDeviceFactory = new DeviceFactory(numberOfHashes, numberOfHashTables);
        }else {
            ps = new PrintStream(new FileOutputStream("src/Result/" + Constants.dataset + "_" +
                    Constants.methodToGenerateFingerprint + ".txt"));
            edgeDeviceFactory = new DeviceFactory();

        }
        System.setOut(ps);
        EdgeNodeNetwork.createNetwork(Constants.nn,Constants.dn, edgeDeviceFactory);
        System.out.println("started!");
        EdgeNodeNetwork.startNetwork();
    }
}

