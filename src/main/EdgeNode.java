package main;
import RPC.RPCFrame;
import utils.Constants;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unchecked")
public class EdgeNode extends RPCFrame implements Runnable {
    public ArrayList<Integer> edgeDevices;
    public Map<ArrayList<Short>, UnitInNode> unitsStatusMap; // used to maintain the status of the unit in a node
    public Map<Integer,ArrayList<ArrayList<Short>>> deviceUnitsMap; //used to store device has which units
    public Map<Integer,HashMap<Integer,ArrayList<ArrayList<Short>>>> deviceResultMap; // each device need to ask certain device for a set of units


    /* this two fields are used for judge whether the node has c*/
    public AtomicInteger count;
    volatile Boolean flag = false;


    //TODO handler;

    public EdgeNode(){
        this.port = new Random().nextInt(50000)+10000;
        this.unitsStatusMap = Collections.synchronizedMap(new HashMap<>());
        this.deviceUnitsMap = Collections.synchronizedMap(new HashMap<>());
        this.deviceResultMap = Collections.synchronizedMap(new HashMap<>());
        this.count= new AtomicInteger(0);
    }

    public void upload(HashMap<ArrayList<Short>,Integer> fingerprints,Integer edgeDeviceHashCode) throws Throwable {
        this.flag = false;
        ArrayList<ArrayList<Short>> unitsInDevice = new ArrayList<>();
        for (ArrayList<Short> id: fingerprints.keySet()){
            if (!unitsStatusMap.containsKey(id)){
                unitsStatusMap.put(id,new UnitInNode(id,0));
            }
            unitsStatusMap.get(id).pointCnt += fingerprints.get(id);
            unitsStatusMap.get(id).update();
            unitsStatusMap.get(id).belongedDevices.add(edgeDeviceHashCode);
            unitsInDevice.add(id);
        }

        ArrayList<Thread> threads = new ArrayList<>();
        /*
        this.reverseFingerprints.put(edgeDeviceHashCode, aggFingerprints);
        for (Long id : aggFingerprints) {
            if (!this.localAggFingerprints.containsKey(id)) {
                this.localAggFingerprints.put(id, Collections.synchronizedList(new ArrayList<Integer>()));
            }
            this.localAggFingerprints.get(id).add(edgeDeviceHashCode);
            if (!this.result.containsKey(id)) {
                this.result.put(id, Collections.synchronizedList(new ArrayList<Integer>()));
            }
            this.result.get(id).add(edgeDeviceHashCode);
        }*/
        count.incrementAndGet();
        boolean flag = count.compareAndSet(edgeDevices.size(), 0);
        if (flag) { //node has finished uploading data, entering into the N-N phase
            this.flag = true;
            for (EdgeNode node : EdgeNodeNetwork.edgeNodes) {
                if (node == this)
                    continue;
                while (!node.flag) {}
                Thread t = new Thread(() -> {
                    try {
                        Object[] parameters = new Object[]{this.localAggFingerprints};
                        Map<Long, ArrayList<Integer>> tmp = (Map<Long, ArrayList<Integer>>)
                                invoke("localhost", node.port, EdgeNode.class.getMethod
                                        ("compareAndSend", Map.class), parameters);
                        //TODO: java.lang.NullPointerException
                        if (tmp != null) {
                            for (Long x : tmp.keySet()) {
                                if (!result.containsKey(x)) {
                                    result.put(x, Collections.synchronizedList(new ArrayList<Integer>()));
                                }
                                result.get(x).addAll(tmp.get(x));
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });
                }
            }
//
//                threads.add(t);
//                t.start();
//            }
//            for (Thread t : threads) {
//                t.join();
//            }
//            //已经向网络中所有的node请求过数据，开始把数据发还给device
//            sendBackResult();
//            this.localAggFingerprints = Collections.synchronizedMap(new HashMap<>());
//            this.reverseFingerprints = Collections.synchronizedMap(new HashMap<>());
//            this.result = Collections.synchronizedMap(new HashMap<>());
        }
    }

    public HashMap<Long,List<Integer>> compareAndSend(Map<Long,ArrayList<Integer>> aggFingerprints) throws Throwable {
        HashMap<Long, List<Integer>> result = new HashMap<>();
        //cellID 比较的过程

        if (Objects.equals(Constants.methodToGenerateFingerprint, "CELLID")){
            for (Long id:aggFingerprints.keySet()){
                ArrayList<Short> dimId = Device.hashBucket.get(id);
                for (Long id0:localAggFingerprints.keySet()){
                    ArrayList<Short> dimId0 = Device.hashBucket.get(id0);
                    if (neighboringSet(dimId,dimId0))
                        result.put(id0, localAggFingerprints.get(id0));
                }
            }
        }
        else if (Objects.equals(Constants.methodToGenerateFingerprint, "LSH")){
            HashSet<Long> intersection = new HashSet<>(localAggFingerprints.keySet());
            intersection.retainAll(aggFingerprints.keySet());
            if (!intersection.isEmpty()) {
                for (Long x : intersection) {
                    result.put(x, localAggFingerprints.get(x));
                }
            }
        }
        return result;
    }

    public boolean neighboringSet(ArrayList<Short> c1, ArrayList<Short> c2) {
        double ss = 0;
        double cellIdxDist = Math.sqrt(Constants.dim)*2;
        double threshold =cellIdxDist*cellIdxDist;
        for(int k = 0; k<c1.size(); k++) {
            ss += Math.pow((c1.get(k) - c2.get(k)),2);
            if (ss >= threshold) return false;
        }
        return true;
    }

    public void sendBackResult() throws Throwable {
//        System.out.println(Thread.currentThread().getName() + " " + this + "  sendBackResult");
        ArrayList<Thread> threads = new ArrayList<>();
        for (Integer edgeDeviceCode : this.edgeDevices) {
            Thread t = new Thread(() -> {
                HashMap<Integer, ArrayList<Long>> dependent = new HashMap<>();
                for (Long x : this.reverseFingerprints.get(edgeDeviceCode)) {
                    //CellId
                    if (Objects.equals(Constants.methodToGenerateFingerprint, "CELLID")) {
                        for (Long id : deviceResultMap.keySet()) {
                            if (neighboringSet(Device.hashBucket.get(x), Device.hashBucket.get(id))) {
                                for (Integer y : deviceResultMap.get(id)) {
                                    if (y == edgeDeviceCode.hashCode()) continue;
                                    if (!dependent.containsKey(y)) {
                                        dependent.put(y, new ArrayList<Long>());
                                    }
                                    dependent.get(y).add(id);
                                }
                            }
                        }
                    }
                    else if (Objects.equals(Constants.methodToGenerateFingerprint, "LSH")) {
                        //LSH
                        if (!deviceResultMap.containsKey(x)) continue;
                        for (Integer y : deviceResultMap.get(x)) {
                            if (y == edgeDeviceCode.hashCode()) continue;
                            if (!dependent.containsKey(y)) {
                                dependent.put(y, new ArrayList<Long>());
                            }
                            dependent.get(y).add(x);
                        }
                    }
                }
                Object[] parameters = new Object[]{dependent};
                try {
                    invoke("localhost", edgeDeviceHashMap.get(edgeDeviceCode).port, Device.class.getMethod
                            ("setDependentDevice", HashMap.class), parameters);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    public void setEdgeDevices(ArrayList<Integer> edgeDevices) {
        this.edgeDevices = edgeDevices;
    }
}
class UnitInNode {
    ArrayList<Short> unitID; //unit中心点的坐标
    int pointCnt; //unit中点的数量
    boolean isSafe; //unit的安全状态
    HashMap<Integer, Integer> isUpdated; //unit的更新状态 DeviceHashCode -> 1/0

    List<Integer> belongedDevices;
    public UnitInNode(ArrayList<Short> unitID, int pointCnt) {
        this.unitID = unitID;
        this.pointCnt = pointCnt;
        this.isUpdated = new HashMap<>();
        for (Integer hashcode : EdgeNodeNetwork.edgeDeviceHashMap.keySet()) {
            // initially we put 1 into it, indicating the first time we need to get the data
            isUpdated.put(hashcode, 1);
        }
        this.belongedDevices = Collections.synchronizedList(new ArrayList<Integer>());
    }

    /* update safeness and updateness */
    public void update() {
        isSafe = (pointCnt > Constants.K);
        isUpdated.put(this.hashCode(), 1);
    }

}