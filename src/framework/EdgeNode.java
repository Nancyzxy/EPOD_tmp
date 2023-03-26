package framework;
import Handler.*;
import RPC.RPCFrame;
import utils.Constants;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EdgeNode extends RPCFrame implements Runnable {
    public ArrayList<Integer> edgeDevices;
    public Map<ArrayList<Integer>,ArrayList<Integer>> unitDevicesMap;
    public Map<ArrayList<Short>,List<UnitInNode>> unitResultInfo; //primly used for pruning
    public Map<ArrayList<Short>, UnitInNode> unitsStatusMap; // used to maintain the status of the unit in a node
    public Map<Integer,HashMap<Integer,ArrayList<ArrayList<Short>>>> deviceResultMap; // each device need to ask certain device for a set of units
    public Handler handler;




    public EdgeNode(){
        this.port = new Random().nextInt(50000)+10000;
        this.unitsStatusMap = Collections.synchronizedMap(new HashMap<>());
        this.unitDevicesMap = Collections.synchronizedMap(new HashMap<>());
        this.unitResultInfo = Collections.synchronizedMap(new HashMap<>());
        this.deviceResultMap = Collections.synchronizedMap(new HashMap<>());
        this.count= new AtomicInteger(0);
        if (Objects.equals(Constants.methodToGenerateFingerprint, "CELLID")) {
            this.handler = new NETSHandler(this);
        }else if (Objects.equals(Constants.methodToGenerateFingerprint,"MCOD")){
            this.handler = new MCODHandler(this);
        }
    }

    /* this two fields are used for judge whether the node has c*/
    public AtomicInteger count;
    volatile Boolean flag = false;
    public void upload(HashMap<ArrayList<Short>,Integer> fingerprints,Integer edgeDeviceHashCode) throws Throwable {
        this.flag = false;
        for (ArrayList<Short> id : fingerprints.keySet()) {
            if (fingerprints.get(id) == Integer.MIN_VALUE) {
                unitsStatusMap.get(id).belongedDevices.remove(edgeDeviceHashCode);
                if (unitsStatusMap.get(id).belongedDevices.isEmpty()) {
                    unitsStatusMap.remove(id);
                }
            }
            if (!unitsStatusMap.containsKey(id)) {
                unitsStatusMap.put(id, new UnitInNode(id, 0));
            }
            unitsStatusMap.get(id).updateCount(fingerprints.get(id));
            unitsStatusMap.get(id).update();
            unitsStatusMap.get(id).belongedDevices.add(edgeDeviceHashCode);
        }

        ArrayList<Thread> threads = new ArrayList<>();

        count.incrementAndGet();
        boolean flag = count.compareAndSet(edgeDevices.size(), 0);
        if (flag) { //node has finished uploading data, entering into the N-N phase
            this.flag = true;
            for (UnitInNode unitInNode : unitsStatusMap.values()) {
                unitInNode.updateSafeness();
            }
            List<ArrayList<Short>> unSafeUnits =
                    unitsStatusMap.keySet().stream().filter(key -> unitsStatusMap.get(key).isSafe != 2).toList();

            for (EdgeNode node : EdgeNodeNetwork.nodeHashMap.values()) {
                if (node == this)
                    continue;
                while (!node.flag) {
                }
                Thread t = new Thread(() -> {
                    try {
                        Object[] parameters = new Object[]{unSafeUnits, this.hashCode()};
                        invoke("localhost", node.port, EdgeNode.class.getMethod("handle", List.class, int.class),parameters);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });
                threads.add(t);
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
            //已经向网络中所有的node请求过数据，开始把数据发还给device
            //Pruning Phase


//            sendBackResult();
//            this.localAggFingerprints = Collections.synchronizedMap(new HashMap<>());
//            this.reverseFingerprints = Collections.synchronizedMap(new HashMap<>());
//            this.result = Collections.synchronizedMap(new HashMap<>());
        }
    }


    public void handle(List<ArrayList<Short>> unSateUnits,int edgeNodeHash){
        this.handler.handle(unSateUnits, edgeNodeHash);
    }

    public void collectFromNode(ArrayList<Short> unitID, List<UnitInNode> unitInNodeList){
        unitInNodeList.forEach(
            x ->{
                for (UnitInNode unitInNode : unitResultInfo.get(unitID)){
                    if (unitInNode.equals(x)){
                        unitInNode.updateCount(x.pointCnt);
                        unitInNode.belongedDevices.addAll(x.belongedDevices);
                        return;
                    }
                }
                unitResultInfo.put(unitID,unitInNodeList);
            }
        );
    }


    public void sendBackResult() throws Throwable {
//        System.out.println(Thread.currentThread().getName() + " " + this + "  sendBackResult");
//        ArrayList<Thread> threads = new ArrayList<>();
//        for (Integer edgeDeviceCode : this.edgeDevices) {
//            Thread t = new Thread(() -> {
//                HashMap<Integer, ArrayList<Long>> dependent = new HashMap<>();
//                for (Long x : this.reverseFingerprints.get(edgeDeviceCode)) {
//                    //CellId
//                    if (Objects.equals(Constants.methodToGenerateFingerprint, "CELLID")) {
//                        for (Long id : deviceResultMap.keySet()) {
//                            if (neighboringSet(Device.hashBucket.get(x), Device.hashBucket.get(id))) {
//                                for (Integer y : deviceResultMap.get(id)) {
//                                    if (y == edgeDeviceCode.hashCode()) continue;
//                                    if (!dependent.containsKey(y)) {
//                                        dependent.put(y, new ArrayList<Long>());
//                                    }
//                                    dependent.get(y).add(id);
//                                }
//                            }
//                        }
//                    }
//                    else if (Objects.equals(Constants.methodToGenerateFingerprint, "LSH")) {
//                        //LSH
//                        if (!deviceResultMap.containsKey(x)) continue;
//                        for (Integer y : deviceResultMap.get(x)) {
//                            if (y == edgeDeviceCode.hashCode()) continue;
//                            if (!dependent.containsKey(y)) {
//                                dependent.put(y, new ArrayList<Long>());
//                            }
//                            dependent.get(y).add(x);
//                        }
//                    }
//                }
//                Object[] parameters = new Object[]{dependent};
//                try {
//                    invoke("localhost", EdgeNodeNetwork.nodeDeviceHashMap.get(edgeDeviceCode).port, Device.class.getMethod
//                            ("setDependentDevice", HashMap.class), parameters);
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            });
//            threads.add(t);
//            t.start();
//        }
//        for (Thread t : threads) {
//            t.join();
//        }
    }

    public void setEdgeDevices(ArrayList<Integer> edgeDevices) {
        this.edgeDevices = edgeDevices;
    }
}
