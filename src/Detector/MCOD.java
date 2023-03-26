package Detector;

import java.util.*;
import be.tarsos.lsh.Vector;
import framework.Device;
import mtree.utils.MTreeClass;
import mtree.utils.Constants;
import mtree.utils.Utils;
import dataStructure.MCO;

public class MCOD extends Detector {
    public static ArrayList<MCO> internal_dataList = new ArrayList<>();

    //--------------------------------------------------------------------------------
    public static HashMap<MCO, ArrayList<MCO>> filled_clusters = new HashMap<>(); //{d.center(d.arrivalTime), cluster}
    public static HashMap<MCO, ArrayList<MCO>> unfilled_clusters = new HashMap<>();
    //    public static HashMap<Integer, MCO> dataList_set = new HashMap<>();
    // 全局的--------------------------------------------------------------
    public static MTreeClass mtree = new MTreeClass();
    public static ArrayList<MCO> outlierList = new ArrayList<>();
    public static PriorityQueue<MCO> eventQueue = new PriorityQueue<>(new MCComparator());
    //---------------------------------------------------------------------------------------------
    //D2E
    public static HashMap<MCO, Integer> send_msg = new HashMap<>();
    //E2D
    public static HashMap<MCO, Integer> rec_msg; //-1: outlier 0:not sure 1:inlier
    //    public static HashMap<device_id, ArrayList<cluster_id/MCO>> answer_list;
    //D2D
    public static HashMap<MCO, ArrayList<MCO>> rec_cluster;

    public MCOD(Device device) {
        super(device);
    }

    // 预处理入口函数
    @Override
    public HashSet<Vector> detectOutlier(List<Vector> data, long currentTime) {
        // 1.去除过期点
        HashSet<Vector> result = new HashSet<>();
        if (Constants.slide != Constants.W) {
            // 1.1 除去internal过期的点
            for (int i = internal_dataList.size() - 1; i >= 0; i--) {
                MCO d = internal_dataList.get(i);
                if (d.arrivalTime <= currentTime - Constants.W) {
                    //remove d from data List
                    internal_dataList.remove(i);

                    //想问个问题，不解散cluster，越积越多，没问题吗
                    //if d is in filled cluster
                    if (d.isInFilledCluster) {
                        removeFromFilledCluster(d);
                    } else removeFromUnfilledCluster(d);

                    //process event queue
                    process_event_queue(currentTime);
                }
            }

            // 1.2 除去external过期的点
            clean_expired_external_data(currentTime);

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
            outlierList.clear();
            last_calculate_time=currentTime;
        }

        // 2.process new data
        data.forEach(this::processNewData);

        //add result
        outlierList.forEach(result::add);
//        printStatistic();
        return result;
    }

    private void removeFromFilledCluster(MCO d) {
        //get the cluster
        ArrayList<MCO> cluster = filled_clusters.get(d.center);
        if (cluster != null) {
            cluster.remove(d);
            filled_clusters.put(d.center, cluster);
            check_shrink(d.center);
        }
    }

    private void removeFromUnfilledCluster(MCO d) {
        ArrayList<MCO> cluster = unfilled_clusters.get(d.center);
        if (cluster != null) {
            cluster.remove(d);
            if (cluster.size() ==0){
                unfilled_clusters.remove(d.center);
            }
            else unfilled_clusters.put(d.center, cluster);

        }
        if (d.numberOfSucceeding + d.exps.size() < Constants.k) {
            outlierList.remove(d);
        }

        outlierList.forEach((data) -> {
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

        ArrayList<MCO> cluster = new ArrayList<>();
        cluster.add(d);
        unfilled_clusters.put(d, cluster);
//        dataList_set.put(d.arrivalTime, d);

        update_info_unfilled(d, false);
        update_info_filled(d);

//        check_filled(d.arrivalTime);
    }

    private void check_filled(MCO center) {
        ArrayList<MCO> cluster = unfilled_clusters.get(center);
        if (cluster.size() >= Constants.k) {
//            MCO center = dataList_set.get(center_id);
            unfilled_clusters.remove(center);
            filled_clusters.put(center, cluster);
            cluster.forEach(p -> {
                outlierList.remove(p);
                eventQueue.remove(p);
                resetObject(p, true);
            });
        }
    }

    private void check_shrink(MCO center) {
        ArrayList<MCO> cluster = filled_clusters.get(center);
        if (cluster.size() < Constants.k) {
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

                update_info_unfilled(o, true); //怎么不放进去,放进去可以，那前面可以不用提前处理了，其实都行
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

    public int isSameSlide(MCO o1, MCO o2) {
        if ((o1.arrivalTime - 1) / Constants.slide == (o2.arrivalTime - 1) / Constants.slide) {
            return 0;
        } else if ((o1.arrivalTime - 1) / Constants.slide < (o2.arrivalTime - 1) / Constants.slide) {
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
            //get the center object
//            MCO center = dataList_set.get(center_id);
            //compute the distance
            double distance = mtree.getDistanceFunction().calculate(center, d);

            if (distance < min_distance) {
                min_distance = distance;
                min_center_id = center;
            }
        }
        return min_center_id;
    }

    private void processNewData(Vector data) {

        MCO d = new MCO(data);

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

        while (inPD.exps.size() > Constants.k - inPD.numberOfSucceeding && inPD.exps.size() > 0) {
            inPD.exps.remove(0);
        }
        if (inPD.exps.size() > 0) {
            inPD.ev = inPD.exps.get(0);
        } else {
            inPD.ev = 0;
        }

        if (inPD.exps.size() + inPD.numberOfSucceeding >= Constants.k) {
            if (inPD.numberOfSucceeding >= Constants.k) {
                eventQueue.remove(inPD);
                outlierList.remove(inPD);
            } else {
                outlierList.remove(inPD);
                if (!eventQueue.contains(inPD)) {
                    // 还是没懂这个计时
                    long startTime3 = Utils.getCPUTime();
                    eventQueue.add(inPD);
                }
            }

        } else {
            eventQueue.remove(inPD);
            if (!outlierList.contains(inPD)) {
                outlierList.add(inPD);
            }
        }
    }

    private void process_event_queue(long currentTime) {
        MCO x = eventQueue.peek();

        while (x != null && x.ev <= currentTime) {

            x = eventQueue.poll();
            while (x.exps.get(0) <= currentTime) {
                x.exps.remove(0);
                if (x.exps.isEmpty()) {
                    x.ev = 0;
                    break;
                } else {
                    x.ev = x.exps.get(0);

                }
            }
            if (x.exps.size() + x.numberOfSucceeding < Constants.k) {
                outlierList.add(x);

            } else if (x.numberOfSucceeding < Constants.k && x.exps.size() + x.numberOfSucceeding >= Constants.k) {
                eventQueue.add(x);
            }

            x = eventQueue.peek();

        }
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

    //-------------------------------------------------------------------------------------------------------
    public static HashMap<ArrayList<Short>, Integer> external_info = new HashMap<>();
    //HashMap<时间戳, HashMap<外部cluster中心点坐标,ArrayList<点>>>
    public static HashMap<Long, HashMap<ArrayList<Short>, ArrayList<MCO>>> external_data = new HashMap<>();
    long last_calculate_time;

    public static void clean_expired_external_data(long currentTime) {
        for (HashMap<ArrayList<Short>, ArrayList<MCO>> time_value : external_data.values()) {
            for (ArrayList<MCO> time_cluster_value : time_value.values()) {
                for (MCO mco : time_cluster_value) {
                    if (mco.arrivalTime <= currentTime - Constants.W) {
                        time_cluster_value.remove(mco);
                        external_info.get(mco.center.values)--;
                    }
                }
            }
        }
    }

    //更新external_info至最新状态
    public static void update_external_info(long current_time) {
        HashMap<ArrayList<Short>, ArrayList<MCO>> last_arrive_data = external_data.get(current_time);
        for (ArrayList<MCO> data : last_arrive_data.values()) {
            for (MCO mco : data) {
                external_info.get(mco.center)++;
            }
        }
    }


    //先写着后面再提取公共函数
    public void check_local_outliers() {
        ArrayList<MCO> inliers = new ArrayList<>();
        outlierList.forEach(o -> {
            int reply = rec_msg.get(o.center);
            if (reply == 1) {
                inliers.add(o);
                // 是在device端就确定为inlier的情况,没有精确的最早的neighbor过期的时间 更新不了相应proceding和succeeding
                o.ev = 现在时间 + 1;
                eventQueue.add(o);
            } else if (reply == -1) {
                // 好像不用做什么
            } else {
                int sum = 0;
                ArrayList<MCO> cluster3R_2 = new ArrayList<>();
                rec_cluster.forEach((key, value) -> {
                    if (mtree.getDistanceFunction().calculate(key, o) < Constants.R / 2) {
                        // 感觉得讨论一下下， 就是这里如果想算proceed和succeed的话，就涉及到了具体的距离计算，感觉跟不prun没啥区别
                        // 所以我先写根据size来剪枝的，然后把ev设成了下一个时间
//                        value.forEach();
                        sum += value.size();
                        if (sum >= Constants.k) {
                            inliers.add(o);
                            o.ev = 现在时间 + 1;
                            eventQueue.add(o);
                            break;
                        }
                    } else if (mtree.getDistanceFunction().calculate(key, o) < Constants.R * 3 / 2) {
                        cluster3R_2.add(key);
                    }
                });
                if (sum < Constants.k) {
                    cluster3R_2.forEach(c -> sum += rec_cluster.get(c).size());
                }
                if (sum < Constants.k) {
                    //outlier
                } else {

                }
            }
            inliers.forEach(i -> outlierList.remove(i));
        });
    }
}
