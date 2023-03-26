package framework;

import utils.Constants;

import java.util.*;

public class UnitInNode {
    public ArrayList<Short> unitID; //unit���ĵ������
    public int pointCnt; //unit�е������
    public int isSafe; //unit�İ�ȫ״̬ 0-outlier 1-undetermined 2-safe
    public HashMap<Integer, Integer> isUpdated; //unit�ĸ���״̬ DeviceHashCode -> 1/0
    public Set<Integer> belongedDevices;

    public UnitInNode(ArrayList<Short> unitID, int pointCnt) {
        this.unitID = unitID;
        this.pointCnt = pointCnt;
        this.isUpdated = new HashMap<>();
        for (Integer hashcode : EdgeNodeNetwork.deviceHashMap.keySet()) {
            // initially we put 1 into it, indicating the first time we need to get the data
            isUpdated.put(hashcode, 1);
        }
        this.belongedDevices = Collections.synchronizedSet(new HashSet<>());
    }

    /* update safeness */
    public void updateSafeness() {
        if (pointCnt > Constants.K) {
            this.isSafe = 2;
        } else {
            this.isSafe = 1;
        }
    }

    public synchronized void update() {
        isUpdated.replaceAll((k, v) -> 1);
    }

    public synchronized void updateCount(int cnt) {
        this.pointCnt += cnt;
    }

    @Override
    public boolean equals(Object obj) {
        UnitInNode unitInNode = (UnitInNode) obj;
        return this.unitID == unitInNode.unitID;
    }
}
