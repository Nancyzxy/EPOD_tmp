package framework;
import Handler.*;
import RPC.RPCFrame;
import utils.Constants;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EdgeNode extends RPCFrame implements Runnable {
    public ArrayList<Integer> edgeDevices;
    public Map<ArrayList<?>,List<UnitInNode>> unitResultInfo; //primly used for pruning
    public Map<ArrayList<?>, UnitInNode> unitsStatusMap; // used to maintain the status of the unit in a node
    public Handler handler;
    public EdgeNode(){
        this.port = new Random().nextInt(50000)+10000;
        this.unitsStatusMap = Collections.synchronizedMap(new HashMap<>());
        this.unitResultInfo = Collections.synchronizedMap(new HashMap<>());
        this.count= new AtomicInteger(0);
        if (Objects.equals(Constants.methodToGenerateFingerprint, "NETS")) {
            this.handler = new NETSHandler(this);
        }else if (Objects.equals(Constants.methodToGenerateFingerprint,"MCOD")){
            this.handler = new MCODHandler(this);
        }
    }
    /* this two fields are used for judge whether the node has complete uploading*/
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
        if (flag) {
            //node has finished collecting data, entering into the N-N phase, only one thread go into this loop
            this.flag = true; //indicate to other nodes I am ready
            for (UnitInNode unitInNode : unitsStatusMap.values()) {
                unitInNode.updateSafeness();
            }
            //���ؽ�� TODO
            List<ArrayList<?>> unSafeUnits =
                    unitsStatusMap.keySet().stream().filter(key -> unitsStatusMap.get(key).isSafe != 2).toList();
            for (ArrayList<?> unsafeUnit:unSafeUnits){
                List<UnitInNode> unitInNodeList = unitsStatusMap.values().stream().filter(x -> x.isUpdated.get(this.hashCode())==1)
                        .filter(x -> this.handler.neighboringSet(unsafeUnit,x.unitID)).toList();
                unitResultInfo.put(unsafeUnit,unitInNodeList);
            }

            for (EdgeNode node : EdgeNodeNetwork.nodeHashMap.values()) {
                if (node == this)
                    continue;
                while (!node.flag) {
                }
                Thread t = new Thread(() -> {
                    try {
                        Object[] parameters = new Object[]{unSafeUnits, this.hashCode()};
                        invoke("localhost", node.port, EdgeNode.class.getMethod("compareAndSend", List.class, int.class),parameters);
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
            //�Ѿ������������е�node��������ݣ���ʼ�����ݷ�����device
            //Pruning Phase
            pruning();

            //send result back to the belonged device;
            sendBackResult();
        }
    }

    public void pruning(){
        //update UnitInNode update
        for (ArrayList<?> UnitID: unitResultInfo.keySet()){
            //add up all point count
            List<UnitInNode> list = unitResultInfo.get(UnitID);
            Optional<UnitInNode> exist = list.stream().filter(x->x.unitID.equals(UnitID) && (x.pointCnt > Constants.K)).findAny();
            if (exist.isPresent()){
                if (exist.get().pointCnt>Constants.K){
                    unitsStatusMap.get(UnitID).isSafe = 2;
                }
            }
            int totalCnt = list.stream().mapToInt(x->x.pointCnt).sum();
            if (totalCnt < Constants.K) {
                unitsStatusMap.get(UnitID).isSafe = 0;
            }
        }
    }

    public void compareAndSend(List<ArrayList<Short>> unSateUnits,int edgeNodeHash){
        ArrayList<Thread> threads = new ArrayList<>();
        for (ArrayList<Short> unit:unSateUnits){
            Thread t = new Thread(()->{
                List<UnitInNode> unitInNodeList = unitsStatusMap.values().stream()
                        .filter(x -> x.isUpdated.get(edgeNodeHash)==1)
                        .filter(x -> this.handler.neighboringSet(unit,x.unitID)).toList();
                unitInNodeList.forEach(x -> x.isUpdated.put(edgeNodeHash,0)); // TODO: CHECK whether is right
                Object[] parameters = new Object[]{unit, unitInNodeList};
                try {
                    invoke("localhost", EdgeNodeNetwork.nodeHashMap.get(edgeNodeHash).port,
                            EdgeNode.class.getMethod("sendFromNode", ArrayList.class, List.class), parameters);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                };
            });
            threads.add(t);
        }

        for (Thread t:threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendFromNode(ArrayList<?> unitID, List<UnitInNode> unitInNodeList) {
        if (!unitResultInfo.containsKey(unitID)) {
            unitResultInfo.put(unitID, unitInNodeList);
            return;
        }
        unitInNodeList.forEach(
                x -> {
                    for (UnitInNode unitInNode : unitResultInfo.get(unitID)) {
                        if (unitInNode.equals(x)) {
                            unitInNode.updateCount(x.pointCnt);
                            unitInNode.belongedDevices.addAll(x.belongedDevices);
                            return;
                        }
                    }
                    unitResultInfo.get(unitID).add(x);
                }
        );
    }


    public void sendBackResult() throws Throwable {
        ArrayList<Thread> threads = new ArrayList<>();
        for (Integer edgeDeviceCode : this.edgeDevices) {
            Thread t = new Thread(() -> {
                //Ϊÿ���豸������
                // 1 ��ȫ״̬
                List<UnitInNode> list = unitsStatusMap.values().stream().filter(
                        x->x.belongedDevices.contains(edgeDeviceCode)).
                toList();
                HashMap<ArrayList<Short>, Integer> status = new HashMap<>();
                for (UnitInNode i:list){
                    status.put(i.unitID,i.isSafe);
                }

                // 2 �����ĸ�deviceҪʲô����
                list = unitsStatusMap.values().stream().filter(
                                x->(x.belongedDevices.contains(edgeDeviceCode) && (x.isSafe == 1))).toList();
                HashMap<Integer,HashSet<ArrayList<Short>>> result = new HashMap<>();
                for (UnitInNode unitInNode:list){
                    unitResultInfo.get(unitInNode.unitID).forEach(
                            x -> {
                                Set<Integer> deviceList = x.belongedDevices;
                                for (Integer y:deviceList){
                                    if (!result.containsKey(y)){
                                        result.put(y,new HashSet<>());
                                    }
                                    result.get(y).add(x.unitID);
                                }
                            }
                    );
                }
                Object[] parameters = new Object[]{status,result};
                try {
                    invoke("localhost", EdgeNodeNetwork.deviceHashMap.get(edgeDeviceCode).port, Device.class.getMethod
                            ("getExternalData", HashMap.class, HashMap.class), parameters);
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
