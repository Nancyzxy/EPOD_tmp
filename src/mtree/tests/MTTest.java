package mtree.tests;

import be.tarsos.lsh.Vector;
import mtree.utils.Constants;
import mtree.utils.Utils;
import outlierdetection.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class MTTest {

    public static int currentTime = 0;

    public static boolean stop = false;

    public static HashSet<Integer> idOutliers = new HashSet<>();

    public static String algorithm;
    public static String dataset="";
    public static Date currentRealTime;

    public static void main(String[] args) throws IOException, ParseException {
        System.out.println("begin to run:)");
        readArguments(args);
        MesureMemoryThread mesureThread = new MesureMemoryThread();
        mesureThread.start();
        Stream s = Stream.getInstance(dataset);
        System.out.println("finish reading");
        ExactStorm estorm = new ExactStorm();
        ApproxStorm apStorm = new ApproxStorm(0.1);
        AbstractC abstractC = new AbstractC();
        Lazy_Update_Event lue = new Lazy_Update_Event();
        Direct_Update_Event due = new Direct_Update_Event();
        MicroCluster micro = new MicroCluster();
        MicroCluster_New mcnew = new MicroCluster_New();
        MESI mesi = new MESI();
        MESIWithHash mesiWithHash = new MESIWithHash();
        int numberWindows = 0;
        double totalTime = 0;
        while (!stop) {

            if (Constants.numberWindow != -1 && numberWindows > Constants.numberWindow) {
                break;
            }
            numberWindows++;

            ArrayList<Data> incomingData;

            if (currentTime != 0) {
                incomingData = s.getIncomingData(currentTime, Constants.slide);
                currentTime = currentTime + Constants.slide;
            } else {
                incomingData = s.getIncomingData(currentTime, Constants.W);
                currentTime = currentTime + Constants.W;
            }

            long start = Utils.getCPUTime(); // requires java 1.5

            double elapsedTimeInSec = 0;
            switch (algorithm) {
                /*
                case "exactStorm":
                    ArrayList<Data> outliers = estorm.detectOutlier(incomingData, currentTime, Constants.W, Constants.slide);
                    double elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers.forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);
                    });

                    break;
                case "approximateStorm":
                    ArrayList<Data> outliers2 = apStorm.detectOutlier(incomingData, currentTime, Constants.W,
                            Constants.slide);
                    elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers2.forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);
                    });
                    break;
                case "abstractC":
                    ArrayList<Data> outliers3 = abstractC.detectOutlier(incomingData, currentTime, Constants.W,
                            Constants.slide);
                    elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers3.forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);

                    });
                    break;
                case "lue":
                    HashSet<DataLUEObject> outliers4 = lue.detectOutlier(incomingData, currentTime, Constants.W,
                            Constants.slide);
                    elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers4.stream().forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);
                    });
                    break;
                case "due":
                    HashSet<DataLUEObject> outliers5 = due.detectOutlier(incomingData, currentTime, Constants.W,
                            Constants.slide);
                    elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers5.forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);
                    });
                    break;
                case "microCluster":
                    ArrayList<Data> outliers6 = micro.detectOutlier(incomingData, currentTime, Constants.W,
                            Constants.slide);
                    elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers6.forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);
                    });

                    break;*/
                case "microCluster_new":
                    ArrayList<Data> outliers9 = mcnew.detectOutlier(incomingData, currentTime, Constants.W,
                            Constants.slide);
                    elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers9.forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);

                    });
                    break;
                case "mesi":
                    ArrayList<Data> outliers7 = mesi.detectOutlier(incomingData, currentTime, Constants.W,
                            Constants.slide);
                    elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers7.forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);
                    });
                    break;
                case "mesiWithHash":
                    HashSet<Vector> outliers8 = mesiWithHash.detectOutlier(incomingData, currentTime, Constants.W,
                            Constants.slide);
                    elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                    totalTime += elapsedTimeInSec;
                    outliers8.forEach((outlier) -> {
                        idOutliers.add(outlier.arrivalTime);
                    });
                    break;

            }
            if (numberWindows == 1) {
                totalTime = 0;
                MesureMemoryThread.timeForIndexing = 0;
                MesureMemoryThread.timeForNewSlide = 0;
                MesureMemoryThread.timeForExpireSlide = 0;
            }
            System.out.println("#window: " + numberWindows);
            System.out.println("Total #outliers: " + idOutliers.size());
            System.out.println("Average Time: " + totalTime * 1.0 / numberWindows);
            System.out.println("Peak memory: " + MesureMemoryThread.maxMemory * 1.0 / 1024 / 1024);
            System.out.println("Time index, remove data from structure: " + MesureMemoryThread.timeForIndexing * 1.0 / 1000000000 / numberWindows);
            System.out.println("Time for querying: " + MesureMemoryThread.timeForQuerying * 1.0 / 1000000000 / numberWindows);
            System.out.println("Time for new slide: " + MesureMemoryThread.timeForNewSlide * 1.0 / 1000000000 / numberWindows);
            System.out.println("Time for expired slide: " + MesureMemoryThread.timeForExpireSlide * 1.0 / 1000000000 / numberWindows);
            System.out.println("------------------------------------");

            switch (algorithm) {
                case "exactStorm":

                    System.out.println("Avg neighbor list length = " + ExactStorm.avgAllWindowNeighbor * 1.0 / numberWindows);
                    break;
                case "mesi":

                    System.out.println("Avg trigger list = " + MESI.avgAllWindowTriggerList * 1.0 / numberWindows);
                    System.out.println("Avg neighbor list = " + MESI.avgAllWindowNeighborList * 1.0 / numberWindows);
                    break;
                case "microCluster":

                    System.out.println("Number clusters = " + MicroCluster.numberCluster * 1.0 / numberWindows);
                    System.out.println("Max  Number points in event queue = " + MicroCluster.numberPointsInEventQueue);

                    System.out.println("Avg number points in clusters= " + MicroCluster.numberPointsInClustersAllWindows * 1.0 / numberWindows);
                    System.out.println("Avg Rmc size = " + MicroCluster.avgPointsInRmcAllWindows * 1.0 / numberWindows);
                    System.out.println("Avg Length exps= " + MicroCluster.avgLengthExpsAllWindows * 1.0 / numberWindows);
                    break;
                case "due":
                    Direct_Update_Event.avgAllWindowNumberPoints = Direct_Update_Event.numberPointsInEventQueue;
                    System.out.println("max #points in event queue = " + Direct_Update_Event.avgAllWindowNumberPoints);
                    break;
                case "microCluster_new":
                    System.out.println("avg points in clusters = " + MicroCluster_New.avgNumPointsInClusters * 1.0 / numberWindows);
                    System.out.println("Avg points in event queue = " + MicroCluster_New.avgNumPointsInEventQueue * 1.0 / numberWindows);
                    System.out.println("avg neighbor list length = " + MicroCluster_New.avgNeighborListLength * 1.0 / numberWindows);
                    break;
            }
        }

        ExactStorm.avgAllWindowNeighbor = ExactStorm.avgAllWindowNeighbor * 1.0 / numberWindows;
        MESI.avgAllWindowTriggerList = MESI.avgAllWindowTriggerList * 1.0 / numberWindows;
        MicroCluster.numberCluster = MicroCluster.numberCluster * 1.0 / numberWindows;
        MicroCluster.avgPointsInRmcAllWindows = MicroCluster.avgPointsInRmcAllWindows * 1.0 / numberWindows;
        MicroCluster.avgLengthExpsAllWindows = MicroCluster.avgLengthExpsAllWindows * 1.0 / numberWindows;
        MicroCluster.numberPointsInClustersAllWindows = MicroCluster.numberPointsInClustersAllWindows * 1.0 / numberWindows;
        MicroCluster_New.avgNumPointsInClusters = MicroCluster_New.avgNumPointsInClusters * 1.0 / numberWindows;
        mesureThread.averageTime = totalTime * 1.0 / (numberWindows - 1);
        mesureThread.writeResult();
        mesureThread.stop();
        mesureThread.interrupt();

        /**
         * Write result to file
         */
        if (!"".equals(Constants.resultFile)) {
            writeResult();
        }
    }
    public static void readArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {

            //check if arg starts with --
            String arg = args[i];
            if (arg.indexOf("--") == 0) {
                switch (arg) {
                    case "--algorithm":
                        algorithm = args[i + 1];
                        break;
                    case "--dataset":
                        dataset = args[i + 1];
                        break;
                    case "--R":
                        Constants.R = Double.parseDouble(args[i + 1]);
                        break;
                    case "--W":
                        Constants.W = Integer.parseInt(args[i + 1]);
                        break;
                    case "--k":
                        Constants.k = Integer.parseInt(args[i + 1]);
                        break;
                    case "--datafile":
                        Constants.dataFile = args[i + 1];
                        break;
                    case "--output":
                        Constants.outputFile = args[i + 1];
                        break;
                    case "--numberWindow":
                        Constants.numberWindow = Integer.parseInt(args[i + 1]);
                        break;
                    case "--slide":
                        Constants.slide = Integer.parseInt(args[i + 1]);
                        break;
                    case "--resultFile":
                        Constants.resultFile = args[i + 1];
                        break;
                    case "--samplingTime":
                        Constants.samplingPeriod = Integer.parseInt(args[i + 1]);
                        break;
                    case "--likely":
                        Constants.likely = Double.parseDouble(args[i + 1]);
                        break;

                }
            }
        }
    }

    public static void writeResult() {
        String resultFile = "";
        switch (algorithm) {
            case "exactStorm":
                resultFile = Constants.outputStorm;
                break;
            case "approximateStorm":
                resultFile = Constants.outputapStorm;
                break;
            case "abstractC":
                resultFile = Constants.outputabstractC;
                break;
            case "lue":
                resultFile = Constants.outputLUE;
                break;
            case "due":
                resultFile = Constants.outputDUE;
                break;
            case "microCluster":
                resultFile = Constants.outputMicro;
                break;
            case "microCluster_new":
                resultFile = Constants.outputMicroNew;
                break;
            case "mesi":
                resultFile = Constants.outputMESI;
                break;
            case "mesiWithHash":
                resultFile = Constants.outputMESIHash;
                break;
        }


        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(resultFile, true)))) {

            for (Integer time : idOutliers) {
                out.println(time);
            }
            out.println("===========================================================================");
        } catch (IOException ignored) {
        }
    }

}
