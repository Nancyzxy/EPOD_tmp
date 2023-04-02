package Detector;


import dataStructure.MCO;

import java.util.*;

import dataStructure.Vector;
import framework.Device;
import mtree.utils.MTreeClass;
import mtree.utils.Utils;
import utils.Constants;

public class MCOD extends Detector {
    public static HashMap<ArrayList<?>, MCO> map_to_MCO;
    public static ArrayList<MCO> internal_dataList;

    //--------------------------------------------------------------------------------
    public static HashMap<MCO, ArrayList<MCO>> filled_clusters; //{d.center(d.arrivalTime), cluster}
    public static HashMap<MCO, ArrayList<MCO>> unfilled_clusters;
    // 全局的--------------------------------------------------------------
    public static MTreeClass mtree;
    public static HashSet<MCO> outliers;
    public static PriorityQueue<MCO> eventQueue;
    //---------------------------------------------------------------------------------------------
    //E2D
//    public static HashMap<MCO, Integer> rec_msg; //-1: outlier 0:not sure 1:inlier

    //-------------------------------------------------------------------------------------------------------
    public static HashMap<ArrayList<?>, Integer> external_info;

    public MCOD(Device device) {
        super(device);
        map_to_MCO = new HashMap<>();
        internal_dataList = new ArrayList<>();
        filled_clusters = new HashMap<>();
        unfilled_clusters = new HashMap<>();
        mtree = new MTreeClass();
        outliers = new HashSet<>();
        eventQueue = new PriorityQueue<>(new MCComparator());
        external_info = new HashMap<>();
        external_data = Collections.synchronizedMap(new HashMap<>());
    }

    // 预处理入口函数
    @Override
    public void detectOutlier(List<Vector> data) {
        // 1.去除过期点
        HashSet<Vector> result = new HashSet<>();
        if (Constants.S != Constants.W) {
            // 1.1 除去internal过期的点
            for (int i = internal_dataList.size() - 1; i >= 0; i--) {
                MCO d = internal_dataList.get(i);
                if (d.arrivalTime <= Constants.currentSlideID - Constants.W) {
                    //remove d from data List
                    internal_dataList.remove(i);

                    //if d is in filled cluster
                    if (d.isInFilledCluster) {
                        removeFromFilledCluster(d);
                    } else removeFromUnfilledCluster(d);

                    //process event queue
                    process_event_queue();
                }
            }

            // 1.2 除去external过期的点
            clean_expired_external_data();

        } else {
            internal_dataList.clear();
            external_info.clear();
            external_data.clear();
            filled_clusters.clear();
            unfilled_clusters.clear();
//            dataList_set.clear();
            eventQueue.clear();
            mtree = null;
            mtree = new MTreeClass();
            outliers.clear();
        }

        // 2.process new data
        data.forEach(this::processNewData);

        //add result
        result.addAll(outliers);
    }

    private void removeFromFilledCluster(MCO d) {
        //get the cluster
        ArrayList<MCO> cluster = filled_clusters.get(d.center);
        if (cluster != null) {
            cluster.remove(d);
            filled_clusters.put(d.center, cluster);
            check_shrink(d.center);
        }
        Integer origin = this.device.fullCellDelta.get(new ArrayList<>(Arrays.asList(d.center.values)));
        this.device.fullCellDelta.put(new ArrayList<>(Arrays.asList(d.center.values)), origin - 1);
    }

    private void removeFromUnfilledCluster(MCO d) {
        ArrayList<MCO> cluster = unfilled_clusters.get(d.center);
        if (cluster != null) {
            cluster.remove(d);
            if (cluster.size() == 0) {
                unfilled_clusters.remove(d.center);
                this.device.fullCellDelta.put(new ArrayList<>(Arrays.asList(d.center.values)), Integer.MIN_VALUE);
            } else {
                unfilled_clusters.put(d.center, cluster);
                Integer origin = this.device.fullCellDelta.get(new ArrayList<>(Arrays.asList(d.center.values)));
                this.device.fullCellDelta.put(new ArrayList<>(Arrays.asList(d.center.values)), origin - 1);
            }
        }
        if (d.numberOfSucceeding + d.exps.size() < Constants.K) {
            outliers.remove(d);
        }

        outliers.forEach((data) -> {
            while (data.exps.size() > 0 && data.exps.get(0) <= d.arrivalTime + Constants.W) {
                data.exps.remove(0);
                if (data.exps.isEmpty()) {
                    data.ev = 0;
                } else {
                    data.ev = data.exps.get(0);
                }
            }
        });
    }

    private void resetObject(MCO o, boolean isInFilledCluster) {
        o.ev = 0;
        o.exps.clear();
//        o.Rmc.clear();
        o.numberOfSucceeding = 0;
        o.isInFilledCluster = isInFilledCluster;
        //        o.isCenter = o.isCenter;
        //        o.lastCalculated =
    }

    // 对于Unfilled cluster里的一个点d，更新它自己的前继后继和unfilled clusters里的前后继
    private void update_info_unfilled(MCO d, boolean fromShrinkCluster) {
        for (MCO center : unfilled_clusters.keySet()) {
            //去除自己cluster,避免重复计算
            if (fromShrinkCluster && center == d.center) continue;

//            MCO center = dataList_set.get(center_id);
            // 如果center中心离d 3R/2以内，检查这个unfilled cluster里的所有点
            if (mtree.getDistanceFunction().calculate(center, d) <= 3 * Constants.R / 2) {
                ArrayList<MCO> unfilled_cluster = unfilled_clusters.get(center);
                for (MCO point : unfilled_cluster) {
                    if (point != d && mtree.getDistanceFunction().calculate(point, d) <= Constants.R) {
                        if (isSameSlide(point, d) == -1) {
                            //is preceeding neighbor
                            d.exps.add(point.arrivalTime + Constants.W);
                            if (!fromShrinkCluster) {
                                point.numberOfSucceeding++;
                            }
                        } else if (isSameSlide(point, d) == 0) {
                            d.numberOfSucceeding++;
                            if (!fromShrinkCluster) {
                                point.numberOfSucceeding++;
                            }
                        } else {
                            //对应一个filled_cluster边unfilled而言
                            d.numberOfSucceeding++;
                            if (!fromShrinkCluster) {
                                point.exps.add(d.arrivalTime + Constants.W);
                            }
                        }

                        //just keep k-numberofSucceedingNeighbor
                        if (!fromShrinkCluster) {
                            checkInlier(point);
                        }
                    }
                }
            }
        }
    }

    //d这个点在filled_cluster里找邻居更新信息
    private void update_info_filled(MCO d) {
        for (MCO center : filled_clusters.keySet()) {
//            MCO center = dataList_set.get(center_id);
            // 如果center中心离d 3R/2以内，检查这个filled cluster里的所有点
            if (mtree.getDistanceFunction().calculate(center, d) <= 3 * Constants.R / 2) {
                ArrayList<MCO> filled_cluster = filled_clusters.get(center);
                for (MCO point : filled_cluster) {
                    if (mtree.getDistanceFunction().calculate(point, d) <= Constants.R) {
                        if (isSameSlide(d, point) <= 0) {
                            d.numberOfSucceeding++;
                        } else {
                            //p is preceeding neighbor
                            d.exps.add(point.arrivalTime + Constants.W);
                        }
                    }
                }
            }
        }
        checkInlier(d);
    }

    private void addToUnfilledCluster(MCO nearest_center, MCO d) {
        //更新点的信息
        d.isCenter = false;
        d.isInFilledCluster = false;
        d.center = nearest_center;

        //更新cluster信息
        ArrayList<MCO> cluster = unfilled_clusters.get(nearest_center);
        cluster.add(d);
        unfilled_clusters.put(nearest_center, cluster);

        //update fullCellDelta
        Integer origin = this.device.fullCellDelta.get(new ArrayList<>(Arrays.asList(d.center.values)));
        this.device.fullCellDelta.put(new ArrayList<>(Arrays.asList(d.center.values)), origin + 1);

        //这两步顺序不能换，因为是在update_info_filled里checkInlier(d)
        //update self and others secceeding and preceeding in unfilled_cluster
        update_info_unfilled(d, false);
        //find neighbors in filled clusters (3R/2)
        update_info_filled(d);

        check_filled(nearest_center);
    }

    private void formUnfilledCluster(MCO d) {
        d.isCenter = true;
        d.isInFilledCluster = false;
        d.center = d;
        map_to_MCO.put(new ArrayList<>(Arrays.asList(d.values)), d);

        ArrayList<MCO> cluster = new ArrayList<>();
        cluster.add(d);
        unfilled_clusters.put(d, cluster);

        this.device.fullCellDelta.put(new ArrayList<>(Arrays.asList(d.center.values)), 1);

        update_info_unfilled(d, false);
        update_info_filled(d);

        check_filled(d); //以防K=1
    }

    private void check_filled(MCO center) {
        ArrayList<MCO> cluster = unfilled_clusters.get(center);
        if (cluster.size() >= Constants.K) {
//            MCO center = dataList_set.get(center_id);
            unfilled_clusters.remove(center);
            filled_clusters.put(center, cluster);
            cluster.forEach(p -> {
                outliers.remove(p);
                eventQueue.remove(p);
                resetObject(p, true);
            });
        }
    }

    private void check_shrink(MCO center) {
        ArrayList<MCO> cluster = filled_clusters.get(center);
        if (cluster.size() < Constants.K) {
            //更新cluster的状态
            filled_clusters.remove(center);
            unfilled_clusters.put(center, cluster);

            //更新cluster里点的状态
            cluster.sort(new MCComparatorArrivalTime());
            for (int i = 0; i < cluster.size(); i++) {
                MCO o = cluster.get(i);
                resetObject(o, false);
                //先处理下自己cluster里的,算preceeding和secceeding
                for (int j = 0; j < i; j++) {
                    MCO n = cluster.get(j);
                    o.exps.add(n.arrivalTime + Constants.W);
                }
                o.numberOfSucceeding = cluster.size() - 1 - i;

                update_info_unfilled(o, true);
                update_info_filled(o);
            }
        }
    }

    private void addToFilledCluster(MCO nearest_center, MCO d) {
        //更新点的信息
        d.isCenter = false;
        d.isInFilledCluster = true;
        d.center = nearest_center;

        //更新cluster的信息
        ArrayList<MCO> cluster = filled_clusters.get(nearest_center);
        cluster.add(d);
        filled_clusters.put(nearest_center, cluster);

        // update finger print
        Integer origin = this.device.fullCellDelta.get(new ArrayList<>(Arrays.asList(d.center.values)));
        this.device.fullCellDelta.put(new ArrayList<>(Arrays.asList(d.center.values)), origin + 1);

        //update for points in PD that has Rmc list contains center
        // filled 里的自己不用存succeeding和preceeding，只用更新别人
        for (MCO center : unfilled_clusters.keySet()) {
//            MCO center = dataList_set.get(center_id);
            // 如果center中心离d 3R/2以内，检查这个unfilled cluster里的所有点
            if (mtree.getDistanceFunction().calculate(center, d) <= 3 * Constants.R / 2) {
                ArrayList<MCO> unfilled_cluster = unfilled_clusters.get(center);
                for (MCO point : unfilled_cluster) {
                    if (mtree.getDistanceFunction().calculate(point, d) <= Constants.R) {
                        //好像没有这种情况了？
                        if (isSameSlide(d, point) == -1) {
                            point.exps.add(d.arrivalTime + Constants.W);
                        } else if (isSameSlide(d, point) >= 0) {
                            point.numberOfSucceeding++;
                        }
                        //check if point become inlier
                        checkInlier(point);
                    }
                }
            }
        }
    }

    public int isSameSlide(Vector o1, Vector o2) {
        if ((o1.arrivalTime - 1) / Constants.S == (o2.arrivalTime - 1) / Constants.S) {
            return 0;
        } else if ((o1.arrivalTime - 1) / Constants.S < (o2.arrivalTime - 1) / Constants.S) {
            return -1;
        } else {
            return 1;
        }
    }

    public MCO findNearestCenter(MCO d, boolean filled) {
        HashMap<MCO, ArrayList<MCO>> cluster;
        if (filled) cluster = filled_clusters;
        else cluster = unfilled_clusters;

        double min_distance = Double.MAX_VALUE;
        MCO min_center_id = null;
        for (MCO center : cluster.keySet()) {
            //compute the distance
            double distance = mtree.getDistanceFunction().calculate(center, d);

            if (distance < min_distance) {
                min_distance = distance;
                min_center_id = center;
            }
        }
        return min_center_id;
    }

    private void processNewData(Vector vector) {

        MCO d = new MCO(vector);

        //add to datalist
        internal_dataList.add(d);

        // 1.是否可加入filled_cluster
        MCO nearest_filled_center = findNearestCenter(d, true);
        double min_distance = Double.MAX_VALUE;
        if (nearest_filled_center != null) { //found neareast cluster
            min_distance = mtree.getDistanceFunction().calculate(nearest_filled_center, d);
        }
        //assign to cluster if min distance <= R/2
        if (min_distance <= Constants.R / 2) {
            addToFilledCluster(nearest_filled_center, d);
        } else {
            // 2.是否可加入unfilled_cluster
            MCO nearest_unfilled_center_id = findNearestCenter(d, false);
            min_distance = Double.MAX_VALUE;
            if (nearest_unfilled_center_id != null) { //found neareast cluster
                min_distance = mtree.getDistanceFunction().calculate(nearest_unfilled_center_id, d);
            }
            if (min_distance <= Constants.R / 2) {
                addToUnfilledCluster(nearest_filled_center, d);
            } else {
                // 3.自己成一个unfilled_cluster
                formUnfilledCluster(d);
            }
        }
    }

    // 保留最少数目proceeding
    // 检查是否为safe | 剩余inlier（queue） | outlier
    private void checkInlier(MCO inPD) {
        Collections.sort(inPD.exps);

        while (inPD.exps.size() > Constants.K - inPD.numberOfSucceeding && inPD.exps.size() > 0) {
            inPD.exps.remove(0);
        }
        if (inPD.exps.size() > 0) {
            inPD.ev = inPD.exps.get(0);
        } else {
            inPD.ev = 0;
        }

        if (inPD.exps.size() + inPD.numberOfSucceeding >= Constants.K) {
            if (inPD.numberOfSucceeding >= Constants.K) {
                eventQueue.remove(inPD);
                outliers.remove(inPD);
            } else {
                outliers.remove(inPD);
                if (!eventQueue.contains(inPD)) {
                    // 还是没懂这个计时
                    long startTime3 = Utils.getCPUTime();
                    eventQueue.add(inPD);
                }
            }

        } else {
            eventQueue.remove(inPD);
            if (!outliers.contains(inPD)) {
                outliers.add(inPD);
            }
        }
    }

    private void process_event_queue() {
        MCO x = eventQueue.peek();

        while (x != null && x.ev <= Constants.currentSlideID) {

            x = eventQueue.poll();
            while (x.exps.get(0) <= Constants.currentSlideID) {
                x.exps.remove(0);
                if (x.exps.isEmpty()) {
                    x.ev = 0;
                    break;
                } else {
                    x.ev = x.exps.get(0);

                }
            }
            if (x.exps.size() + x.numberOfSucceeding < Constants.K) {
                outliers.add(x);

            } else if (x.numberOfSucceeding < Constants.K && x.exps.size() + x.numberOfSucceeding >= Constants.K) {
                eventQueue.add(x);
            }

            x = eventQueue.peek();

        }
    }

    public void processOutliers() {
        //pruning + 后续处理
        update_external_info();
        check_local_outliers();
        this.outlierVector = outliers;
    }

    static class MCComparator implements Comparator<MCO> {
        @Override
        public int compare(MCO o1, MCO o2) {
            if (o1.ev < o2.ev) {
                return -1;
            } else if (o1.ev == o2.ev) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    static class MCComparatorArrivalTime implements Comparator<MCO> {
        @Override
        public int compare(MCO o1, MCO o2) {
            if (o1.arrivalTime < o2.arrivalTime) {
                return -1;
            } else if (o1.arrivalTime == o2.arrivalTime) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    //public Map<Integer, Map<ArrayList<?>, List<Vector>>> external_data;
    public void clean_expired_external_data() {
        for (Map<ArrayList<?>, List<Vector>> time_value : external_data.values()) {
            for (ArrayList<?> key : time_value.keySet()) {

                Iterator<Vector> iterator = time_value.get(key).iterator();//实例化迭代器
                while (iterator.hasNext()) {
                    Vector v = iterator.next();//读取当前集合数据元素
                    if (v.arrivalTime <= Constants.currentSlideID - Constants.W) {
                        //all.remove(str);//使用集合当中的remove方法对当前迭代器当中的数据元素值进行删除操作(注:此操作将会破坏整个迭代器结构)使得迭代器在接下来将不会起作用
                        int cnt = external_info.get(key);
                        if (cnt == 1) {
                            external_info.remove(key);
                        } else {
                            external_info.put(key, cnt - 1);
                        }
                        iterator.remove();
                    }
                }
            }
        }
    }

    //更新external_info至最新状态
    public void update_external_info() {
        Map<ArrayList<?>, List<Vector>> last_arrive_data = external_data.get(Constants.currentSlideID);
        for (ArrayList<?> key : last_arrive_data.keySet()) {
            if (!external_info.containsKey(key)) {
                external_info.put(key, 0);
            }
            int cnt = external_info.get(key);
            external_info.put(key, cnt + last_arrive_data.get(key).size());
        }
    }

    //todo: need checking
    public void check_local_outliers() {
        ArrayList<MCO> inliers = new ArrayList<>();
        for (MCO o : outliers) {
            int reply = this.status.get(o.center);
            //首先我们需要prunning掉被判断为安全的以及被判断成outlier的点，加入event queue，event time 设为下一个时间点
            if (reply == 2) {
                inliers.add(o);
                // 是在device端就确定为inlier的情况,没有精确的最早的neighbor过期的时间 更新不了相应proceding和succeeding
                o.ev = Constants.currentSlideID + 1;
                eventQueue.add(o);
            }
            //确定为outlier的点不用做操作
            //不确定的点：
            else if (reply == 1) {
                int sum = 0;
                boolean flag = false;
                ArrayList<ArrayList<?>> cluster3R_2 = new ArrayList<>();
                //HashMap<ArrayList<?>, Integer> external_info
                for (Map.Entry<ArrayList<?>, Integer> entry : external_info.entrySet()) {
                    ArrayList<?> key = entry.getKey();
                    Integer value = entry.getValue();
//                    MCO c = map_to_MCO.get(key); //不知道有没有问题 直接算距离 不要mtree
                    double distance = distance(key, o.center.values);
                    if (distance <= Constants.R / 2) {
                        sum += value;
                        // 只是先对外部的点进行pruning,未加上内部数据
                        if (sum >= Constants.K) {
                            inliers.add(o);
                            o.ev = Constants.currentSlideID + 1;
                            eventQueue.add(o);
                            flag = true;
                            break;
                        }
                    } else if (distance <= Constants.R * 3 / 2) {
                        cluster3R_2.add(key);
                    }
                }
                inliers.forEach(i -> outliers.remove(i));

                if (!flag) {
                    for (ArrayList<?> c : cluster3R_2) {
                        sum += external_info.get(c);
                    }
                    //否则，在所有3R/2内cluster（外部）的点，若和本地相加小于k，则判断成为Outlier,
                    if (sum < Constants.K) {
                        continue;
                    }
                    //注意，在本地存储外来点时按照arrivalTime存，本地点的pre和succ包括本地邻居和部分外来邻居。并且需要记录上次计算的arrivaltime信息（last_calculated），在下次需要寻找邻居时，在arrivaltime+1及以后继续寻找并更新pre，succ
                    else {
                        if (o.last_calculate_time == -1 || o.last_calculate_time < Constants.K - Constants.W) {
                            o.last_calculate_time = Constants.K - Constants.W;
                        }
                        while (o.last_calculate_time <= Constants.currentSlideID) {
                            Map<ArrayList<?>, List<Vector>> cur_data = external_data.get(o.last_calculate_time);
                            if (cur_data != null) {
                                // 对每一个3R/2内的邻居
                                for (ArrayList<?> c : cluster3R_2) {
                                    // 在当前的时间点看是否有此cluster
                                    List<Vector> cur_cluster_data = cur_data.get(c);
                                    // 如果有
                                    for (Vector v : cur_cluster_data) {
                                        if (distance(v.values, o.values) <= Constants.R) {
                                            if (isSameSlide(o, v) <= 0) {
                                                o.numberOfSucceeding++;
                                            } else {
                                                //p is preceeding neighbor
                                                o.exps.add(v.arrivalTime + Constants.W);
                                            }
                                        }
                                    }
                                }
                            }
                            o.last_calculate_time++;
                            checkInlier(o);
                        }
                    }
                }
            }
        }
    }

    //todo: 根据历史记录来发送数据
    @Override
    public Map<ArrayList<?>, List<Vector>> sendData(HashSet<ArrayList<?>> bucketIds, int edgeNodeHashCode) {
        Map<ArrayList<?>, List<Vector>> result = new HashMap<>();
        for (ArrayList<?> bucketId : bucketIds) {
            MCO center = map_to_MCO.get(bucketId);
            if (filled_clusters.get(center) == null) {
                List<Vector> list = unfilled_clusters.get(center).stream().map(c -> (Vector) c).toList();
                result.put(bucketId, list);
            } else {
                List<Vector> list = filled_clusters.get(center).stream().map(c -> (Vector) c).toList();
                result.put(bucketId, list);
            }
        }
        return result;
    }

    public double distance(ArrayList<?> a, Double[] b) {
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            sum += Math.pow((double) a.get(i) - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    public double distance(Double[] a, Double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }
}
