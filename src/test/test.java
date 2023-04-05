package test;

import java.util.ArrayList;
import java.util.Iterator;

public class test {
    public static void main(String[] args) {
        //将double数组转化成Double的arraylist
        ArrayList<Integer> list = new ArrayList<>();
        for(int i=0;i<10;i++){
            list.add(i);
        }
        Iterator<Integer> iterator = list.iterator();
        Loop:
        while (iterator.hasNext()){
            int i = iterator.next();

            if (i==5){
                System.out.println(i);
                continue Loop;
            }
            System.out.println("!!!!!");

        }

    }
}
