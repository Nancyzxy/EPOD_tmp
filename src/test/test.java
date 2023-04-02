package test;

import java.util.ArrayList;

public class test {
    public static void main(String[] args) {
        //将double数组转化成Double的arraylist
        double[] a = {1,2,3,4,5};
        ArrayList<Double> b = new ArrayList<>();
        for (double d : a) {
            b.add(d);
        }
    }
}
