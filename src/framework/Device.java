package framework;

import Detector.Detector;
import Detector.NewNETS;
import Detector.MCOD;
import RPC.RPCFrame;
import be.tarsos.lsh.Index;
import be.tarsos.lsh.Vector;
import be.tarsos.lsh.families.EuclideanDistance;
import utils.Constants;
import utils.DataGenerator;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unchecked")
public class Device extends RPCFrame implements Runnable {
    public int deviceId;
    private int numberOfHashTables;
    public Index index;
    public List<Vector> rawData=new ArrayList<>();

    public HashMap<ArrayList<Short>,Integer> fullCellDelta;
    public Map<Long,List<Vector>> allRawDataList;
    public Map<Integer,ArrayList<Integer>> dependentDevice;
    public DataGenerator dataGenerator;
    public EdgeNode nearestNode;
    public Detector detector;
    public long itr;
    public HashSet<Vector> outlier;
    public static Map<Long,ArrayList<Short>>hashBucket;

    /* used for LSH method */
    public Map<Long,List<Vector>> aggFingerprints;


    public Device(int deviceId) throws Throwable {
        this.port = new Random().nextInt(50000)+10000;
        this.deviceId = deviceId;
        this.dataGenerator = new DataGenerator(deviceId);
        if (Objects.equals(Constants.methodToGenerateFingerprint, "CELLID")){
            this.detector = new NewNETS(0,this);
        } else if (Objects.equals(Constants.methodToGenerateFingerprint, "MCOD")) {
            this.detector = new MCOD(this);
        }
        this.fullCellDelta = new HashMap<>();
        this.dependentDevice = Collections.synchronizedMap(new HashMap<>());
        this.allRawDataList = Collections.synchronizedMap(new HashMap<>());

        if (hashBucket==null){
            hashBucket = Collections.synchronizedMap(new HashMap<>());
        }
    }

    public Set<Vector> detectOutlier(long itr) throws Throwable {
        //get initial data
        this.itr = itr;
        Date currentRealTime = new Date();
        currentRealTime.setTime(dataGenerator.firstTimeStamp.getTime() + (long) Constants.S * 10 * 1000 * itr);
        Constants.currentTime = currentRealTime.getTime();
        this.rawData = dataGenerator.getTimeBasedIncomingData(currentRealTime, Constants.S*10);

        //step1: 产生指纹 + 本地先检测出outliers
        clearFingerprints();
        NewNETS newNETS = (NewNETS)(this.detector);
        this.detector.detectOutlier(this.rawData,itr);

        //step2: 上传指纹
        if(itr>Constants.nS-1) {
            sendAggFingerprints(fullCellDelta);
        }

        //本地获取数据 + 处理outliers

        return outlier;
    }


    public void sendAggFingerprints(HashMap<ArrayList<Short>,Integer> aggFingerprints) throws Throwable {
        Object[] parameters = new Object[]{aggFingerprints,this.hashCode()};
        invoke("localhost",this.nearestNode.port,
                EdgeNode.class.getMethod("upload", HashMap.class, Integer.class),parameters);
    }

    public void getData() throws InterruptedException {
        ArrayList<Thread> threads = new ArrayList<>();
        for (Integer edgeDeviceCode :EdgeNodeNetwork.deviceHashMap.keySet()){
            if (this.hashCode() ==edgeDeviceCode) continue;
            /*use for measurement*/
            AtomicReference<Double> recall = new AtomicReference<>((double) 0); // 正样本中被预测正确的
            AtomicReference<Double> precious = new AtomicReference<>((double) 0); // 预测为正的中真实也为正的
            HashSet<Integer> dataSet = new HashSet<>();
            HashSet<Integer> dataSet1 = new HashSet<>();
            for (Vector a: this.rawData){
                for (Vector b: EdgeNodeNetwork.deviceHashMap.get(edgeDeviceCode).rawData){
                    if (new EuclideanDistance().distance(a,b)<=Constants.R){
                        dataSet.add(b.arrivalTime);
                    }
                }
            }
            if (dependentDevice.containsKey(edgeDeviceCode)){
                Thread t =new Thread(()->{
                    Object[] parameters = new Object[]{dependentDevice.get(edgeDeviceCode)};
                    try {
                        HashMap<Long, List<Vector>> data = (HashMap<Long, List<Vector>>)
                                invoke("localhost",
                                        EdgeNodeNetwork.deviceHashMap.get(edgeDeviceCode).port,
                                        Device.class.getMethod("sendData", ArrayList.class), parameters);

                        //cellID<坐标: arraylist> arraylist<point>
                        /*use for measurement*/
                        for (Long x : data.keySet()) {
                            try {
                                for (Vector v: data.get(x)){
                                    dataSet1.add(v.arrivalTime);
                                }
                                this.allRawDataList.get(x).addAll(data.get(x));
                            } catch (NullPointerException ignored) {
                            }
                        }
                        if (dataSet.size() == 0) {
                            System.out.println(this.hashCode() + " from " + edgeDeviceCode + " transferred # is " + dataSet.size());
                        } else {
                            HashSet<Integer> intersection = new HashSet<>(dataSet);
                            intersection.retainAll(dataSet1);
                            recall.set(intersection.size()* 1.0 / (dataSet.size()));
                            precious.set(intersection.size()* 1.0 / (dataSet1.size()) );
                            System.out.printf(this.hashCode() + " from " + edgeDeviceCode +
                                    ": recall = %f, precious = %f, intersection# is %d,neighbor# is %d, transfer# is %d\n",
                                    recall.get(), precious.get(),intersection.size(),dataSet.size(),dataSet1.size());
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
                threads.add(t);
                t.start();
                continue;
//                HashMap<Long, HashMap<ArrayList<Short>, ArrayList<Vector>>> data;
//                HashMap<ArrayList<Short>, int> allexternaldatalist;
//                int last_calculated; keyset.sort
            }
            System.out.println(this.hashCode()+" from "+edgeDeviceCode+" neighbor # is "+dataSet.size());
            /*end*/
        }
        for (Thread t:threads){
            t.join();
        }
        HashSet<Vector> tmp = new HashSet<>();
        for (List<Vector> x: this.allRawDataList.values()) {
            tmp.addAll(x);
        }
        outlier = detector.detectOutlier(new ArrayList<>(tmp),itr);
    }

    public void clearFingerprints(){
        this.fullCellDelta = new HashMap<>();
    }
    public HashMap<Long,List<Vector>> sendData(ArrayList<Long> bucketIds){
        HashMap<Long,List<Vector>> data = new HashMap<>();
        for (Long x:bucketIds){
            data.put(x, aggFingerprints.get(x));
        }
        return data;
    }

    public void setDependentDevice(HashMap<Integer, ArrayList<Integer>> dependentDevice) throws InterruptedException {
        this.dependentDevice = Collections.synchronizedMap(dependentDevice);
        getData();
    }

    public void setNearestNode(EdgeNode nearestNode) {
        this.nearestNode = nearestNode;
    }



    // The method is only used in LSH
    public void generateAggFingerprints(List<Vector> data) {
//        clearFingerprints();

        if (Objects.equals(Constants.methodToGenerateFingerprint, "LSH")) {
            for (Vector datum : data) {
                for (int j = 0; j < this.numberOfHashTables; j++) {
                    long bucketId = index.getHashTable().get(j).getHashValue(datum);
                    if (!aggFingerprints.containsKey(bucketId)) {
                        aggFingerprints.put(bucketId, Collections.synchronizedList(new ArrayList<Vector>()));
                        allRawDataList.put(bucketId, Collections.synchronizedList(new ArrayList<Vector>()));
                    }
                    aggFingerprints.get(bucketId).add(datum);
                    allRawDataList.get(bucketId).add(datum);
                }
            }
        }
    }

    public int getNumberOfHashTables() {
        return numberOfHashTables;
    }

    public void setNumberOfHashTables(int numberOfHashTables) {
        this.numberOfHashTables = numberOfHashTables;
    }

    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }
}
