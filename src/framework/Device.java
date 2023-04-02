package framework;

import Detector.Detector;
import Detector.MCOD;
import Detector.NewNETS;
import RPC.RPCFrame;
import dataStructure.Vector;
import utils.Constants;
import utils.DataGenerator;
import java.util.*;

@SuppressWarnings("unchecked")
public class Device extends RPCFrame implements Runnable {
    public int deviceId;
    public List<Vector> rawData = new ArrayList<>();
    public HashMap<ArrayList<?>,Integer> fullCellDelta; //fingerprint
    public DataGenerator dataGenerator;
    public EdgeNode nearestNode;
    public Detector detector;
    public HashMap<ArrayList<?>, Integer> status;


    public Device(int deviceId) {
        this.port = new Random().nextInt(50000)+10000;
        this.deviceId = deviceId;
        this.dataGenerator = new DataGenerator(deviceId);
        if (Objects.equals(Constants.methodToGenerateFingerprint, "NETS")){
            this.detector = new NewNETS(0,this);
        } else if (Objects.equals(Constants.methodToGenerateFingerprint, "MCOD")) {
            this.detector = new MCOD(this);
        }
        this.fullCellDelta = new HashMap<>();
    }

    public Set<? extends Vector> detectOutlier(int itr) throws Throwable {
        //get initial data
        Constants.currentSlideID = itr;
        Date currentRealTime = new Date();
        currentRealTime.setTime(dataGenerator.firstTimeStamp.getTime() + (long) Constants.S * 10 * 1000 * itr);
        this.rawData = dataGenerator.getTimeBasedIncomingData(currentRealTime, Constants.S*10);

        //step1: ����ָ�� + �����ȼ���outliers
        clearFingerprints();
        this.detector.detectOutlier(this.rawData);

        //step2: �ϴ�ָ��
        if(itr>=Constants.nS-1) {
            sendAggFingerprints(fullCellDelta);
        }

        //���ػ�ȡ���� + ����outliers
        this.detector.processOutliers();
        return this.detector.outlierVector;
    }


    public void sendAggFingerprints(HashMap<ArrayList<?>,Integer> aggFingerprints) throws Throwable {
        Object[] parameters = new Object[]{aggFingerprints,this.hashCode()};
        invoke("localhost",this.nearestNode.port,
                EdgeNode.class.getMethod("upload", HashMap.class, Integer.class),parameters);
    }

    public void clearFingerprints(){
        this.fullCellDelta = new HashMap<>();
    }
    public Map<ArrayList<?>, List<Vector>> sendData(HashSet<ArrayList<?>> bucketIds, int edgeNodeHashCode){
        return this.detector.sendData(bucketIds, edgeNodeHashCode);
    }

    public void getExternalData(HashMap<ArrayList<?>, Integer> status,HashMap<Integer,HashSet<ArrayList<?>>> result) throws InterruptedException {
        this.status = status;
        ArrayList<Thread> threads = new ArrayList<>();
        for (Integer edgeDeviceCode :result.keySet()) {
            Thread t = new Thread(() -> {
                Object[] parameters = new Object[]{result.get(edgeDeviceCode)};
                try {
                    Map<ArrayList<?>, List<Vector>> data = (Map<ArrayList<?>, List<Vector>>)
                            invoke("localhost", EdgeNodeNetwork.deviceHashMap.get(edgeDeviceCode).port,
                                    Device.class.getMethod("sendData", HashSet.class), parameters);
                    if (this.detector.external_data.containsKey(Constants.currentSlideID)) {
                        this.detector.external_data.put(Constants.currentSlideID, Collections.synchronizedMap(new HashMap<>()));
                    }
                    data.keySet().forEach(
                            x -> {
                                Map<ArrayList<?>, List<Vector>> map = this.detector.external_data.get(Constants.currentSlideID);
                                if (!map.containsKey(x)) {
                                    map.put(x, Collections.synchronizedList(new ArrayList<>()));
                                    ;
                                }
                                map.get(x).addAll(data.get(x));
                            }
                    );
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t:threads){
            t.join();
        }
    }

    public void setNearestNode(EdgeNode nearestNode) {
        this.nearestNode = nearestNode;
    }
}
