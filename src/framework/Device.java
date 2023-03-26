package framework;

import Detector.Detector;
import Detector.NewNETS;
import Detector.MCOD;
import RPC.RPCFrame;
import be.tarsos.lsh.Index;
import dataStructure.Vector;
import utils.Constants;
import utils.DataGenerator;
import java.util.*;

@SuppressWarnings("unchecked")
public class Device extends RPCFrame implements Runnable {
    public int deviceId;
    private int numberOfHashTables;
    public Index index;
    public List<Vector> rawData=new ArrayList<>();

    public HashMap<ArrayList<?>,Integer> fullCellDelta; //fingerprint 
    public Map<Long,List<Vector>> allRawDataList;
    public DataGenerator dataGenerator;
    public EdgeNode nearestNode;
    public Detector detector;
    public long itr;

    public HashMap<ArrayList<?>, Integer> status;

    /* used for LSH method */
    public Map<Long,List<Vector>> aggFingerprints;


    public Device(int deviceId) {
        this.port = new Random().nextInt(50000)+10000;
        this.deviceId = deviceId;
        this.dataGenerator = new DataGenerator(deviceId);
        if (Objects.equals(Constants.methodToGenerateFingerprint, "CELLID")){
            this.detector = new NewNETS(0,this);
        } else if (Objects.equals(Constants.methodToGenerateFingerprint, "MCOD")) {
            this.detector = new MCOD(this);
        }
        this.fullCellDelta = new HashMap<>();
        this.allRawDataList = Collections.synchronizedMap(new HashMap<>());
    }

    public Set<Vector> detectOutlier(int itr) throws Throwable {
        //get initial data
        Constants.currentSlideID = itr;
        Date currentRealTime = new Date();
        currentRealTime.setTime(dataGenerator.firstTimeStamp.getTime() + (long) Constants.S * 10 * 1000 * itr);
        this.rawData = dataGenerator.getTimeBasedIncomingData(currentRealTime, Constants.S*10);

        //step1: 产生指纹 + 本地先检测出outliers
        clearFingerprints();
        this.detector.detectOutlier(this.rawData);

        //step2: 上传指纹
        if(itr>Constants.nS-1) {
            sendAggFingerprints(fullCellDelta);
        }

        //本地获取数据 + 处理outliers
        ((NewNETS)this.detector).processOutliers();
        return ((NewNETS)this.detector).outliers;
    }


    public void sendAggFingerprints(HashMap<ArrayList<?>,Integer> aggFingerprints) throws Throwable {
        Object[] parameters = new Object[]{aggFingerprints,this.hashCode()};
        invoke("localhost",this.nearestNode.port,
                EdgeNode.class.getMethod("upload", HashMap.class, Integer.class),parameters);
    }

    public void clearFingerprints(){
        this.fullCellDelta = new HashMap<>();
    }
    public HashMap<ArrayList<?>,List<Vector>> sendData(HashSet<ArrayList<?>> bucketIds){
        HashMap<ArrayList<?>, List<Vector>> data = new HashMap<>();
        NewNETS newNETS = (NewNETS)this.detector;
        for (ArrayList<?> id: bucketIds){
            data.put(id, newNETS.localDataBucket.get(id));
        }
        return data;
    }

    public void getExternalData(HashMap<ArrayList<?>, Integer> status,HashMap<Integer,HashSet<ArrayList<?>>> result) throws InterruptedException {
        this.status = status;
        ArrayList<Thread> threads = new ArrayList<>();
        for (Integer edgeDeviceCode :EdgeNodeNetwork.deviceHashMap.keySet()){
            if (this.hashCode() == edgeDeviceCode) continue;
            if (result.containsKey(edgeDeviceCode)){
                Thread t =new Thread(()->{
                    Object[] parameters = new Object[]{result.get(edgeDeviceCode)};
                    try {
                        HashMap<ArrayList<?>, List<Vector>> data = (HashMap<ArrayList<?>, List<Vector>>)
                                invoke("localhost", EdgeNodeNetwork.deviceHashMap.get(edgeDeviceCode).port,
                                        Device.class.getMethod("sendData", HashSet.class), parameters);
                        NewNETS newNETS = (NewNETS) this.detector;
                        data.keySet().forEach(
                                x -> {
                                    if (!newNETS.externalData.containsKey(x)){
                                        newNETS.externalData.put(x,new ArrayList<>());
;                                    }
                                    newNETS.externalData.get(x).addAll(data.get(x));
                                }
                        );
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
                threads.add(t);
                t.start();
            }
        }
        for (Thread t:threads){
            t.join();
        }
    }

    public void setNearestNode(EdgeNode nearestNode) {
        this.nearestNode = nearestNode;
    }

    // Below methods are only used in LSH
    public void generateAggFingerprints(List<Vector> data) {

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
