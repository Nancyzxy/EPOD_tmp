package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class test {
    public static void main(String[] args) {
        //��double����ת����Double��arraylist
        double[] a = {1,2,3,4,5};
        ArrayList<Double> b = new ArrayList<>();
        for (double d : a) {
            b.add(d);
        }
    }
}
