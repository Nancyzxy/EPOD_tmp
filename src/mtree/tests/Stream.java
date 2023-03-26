package mtree.tests;

import mtree.utils.Constants;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Stream {

    PriorityQueue<Data> streams;

    public static Stream streamInstance;

    public static Stream getInstance(String type) {

        if (streamInstance != null) {
            return streamInstance;
        } else if (!Constants.dataFile.trim().equals("")) {
            streamInstance = new Stream();
//            streamInstance.getData(Constants.dataFile);
            return streamInstance;
        } else if ("ForestCover".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.forestCoverFileName);
            return streamInstance;
        } else if ("TAO".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.taoFileName);
            return streamInstance;
        } else if ("EM".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.emFileName);
            return streamInstance;
        }else if ("STOCK".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.stockFileName);
            return streamInstance;
        }else if ("Gauss".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.gaussFileName);
            return streamInstance;
        }else if ("HPC".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.hpcFileName);
            return streamInstance;
        } else {
            streamInstance = new Stream();
            streamInstance.getRandomInput(1000, 10);
            return streamInstance;

        }

    }

    public boolean hasNext() {
        return !streams.isEmpty();
    }

    /**
     *  return a list of data which is in the period [currentTime,currentTime+length]
     * @param currentTime
     * @param length
     * @param filename
     * @return
     */
    public ArrayList<Data> getIncomingData(int currentTime, int length, String filename) {

        ArrayList<Data> results = new ArrayList<>();
        try {
            BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

            String line = "";
            int time = 0;
            try {
                while ((line = bfr.readLine()) != null) {
                    time++;
                    if (time > currentTime && time <= currentTime + length) {
                        String[] atts = line.split(",");
                        double[] d = new double[atts.length];
                        for (int i = 0; i < d.length; i++) {
                            d[i] = Double.parseDouble(atts[i]) + (new Random()).nextDouble() / 10000000;
                        }
                        Data data = new Data(d);
                        data.arrivalTime = time;
                        results.add(data);
                    }
                }
                bfr.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return results;
    }

    public Date getFirstTimeStamp(String filename) throws FileNotFoundException, IOException, ParseException {
        BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

        String line = "";
        line = bfr.readLine();
        String[] atts = line.split(",");
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        return formatter.parse(atts[0].trim());
    }

    /**
     * return a list of data which is in the period [currentTime,currentTime+length] but each has a likely probability
     * to return
     * @param currentTime
     * @param length
     * @param filename
     * @param likely
     * @return
     */
    public ArrayList<Data> getRandomIncomingData(int currentTime, int length, String filename, double likely) {
        Random r = new Random();
        ArrayList<Data> results = new ArrayList<>();
        try {
            BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

            String line = "";
            int time = 0;
            try {
                while ((line = bfr.readLine()) != null) {
                    time++;
                    if (time > currentTime && time <= currentTime + length) {
                        String[] atts = line.split(",");
                        double[] d = new double[atts.length];
                        for (int i = 0; i < d.length; i++) {

                            d[i] = Double.parseDouble(atts[i]) + (new Random()).nextDouble() / 10000000;
                        }
                        Data data = new Data(d);
                        data.arrivalTime = time;

                        if (likely > r.nextDouble()) {
                            results.add(data);
                        }
                    }
                }
                bfr.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return results;

    }

    /**
     * return time based Incoming data, each data line begins with a timestamp like: [2022/8/6 data]
     * @param currentTime
     * @param lengthInSecond
     * @param filename
     * @return
     */
    public ArrayList<Data> getTimeBasedIncomingData(Date currentTime, int lengthInSecond, String filename) {
        ArrayList<Data> results = new ArrayList<>();
        try {
            BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

            String line = "";
            int time = 0;
            Date endTime = new Date();
            endTime.setTime(currentTime.getTime() + lengthInSecond * 1000L);
            try {
                while ((line = bfr.readLine()) != null) {
                    String[] atts = line.split(",");
                    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

                    try {
                        Date data_time = formatter.parse(atts[0].trim());
                        if (data_time.after(currentTime) && data_time.before(endTime)) {

                            double[] d = new double[atts.length - 1];
                            for (int i = 1; i < d.length; i++) {

                                d[i - 1] = Double.parseDouble(atts[i]) + (new Random()).nextDouble() / 10000000;
                            }
                            Data data = new Data(d);
                            data.arrivalTime = time;

                            results.add(data);

                        }
                    } catch (ParseException ex) {
                        Logger.getLogger(Stream.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
                bfr.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return results;

    }

    /**
     * get incoming data in the period [currentTime,currentTime+length]
     * @param currentTime
     * @param length
     * @return
     */
    public ArrayList<Data> getIncomingData(int currentTime, int length) {
        ArrayList<Data> results = new ArrayList<Data>();
        Data d = streams.peek();
        while (d != null && d.arrivalTime > currentTime
                && d.arrivalTime <= currentTime + length) {
            results.add(d);
            streams.poll();
            d = streams.peek();

        }
        return results;

    }

    /**
     * generate random data of the size length in the range [0,range]
     * @param length
     * @param range
     */
    public void getRandomInput(int length, int range) {

        Random r = new Random();
        for (int i = 1; i <= length; i++) {
            double d = r.nextInt(range);
            Data data = new Data(d);
            data.arrivalTime = i;
            streams.add(data);
            streams.add(data);

        }

    }

    /**
     * read data and store it in the streams
     * @param filename
     */
    public void getData(String filename) {

        try {
            BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

            String line = "";
            int time = 1;
            try {
                while ((line = bfr.readLine()) != null) {
                    String[] atts = line.split(",");
                    double[] d = new double[atts.length];
                    for (int i = 0; i < d.length; i++) {
                        d[i] = Double.parseDouble(atts[i]) + (new Random()).nextDouble() / 10000000;
                    }
                    Data data = new Data(d);
                    data.arrivalTime = time;
                    streams.add(data);
                    time++;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public Stream() {
        Comparator<Data> comparator = new DataComparator();
        streams = new PriorityQueue<Data>(comparator);
    }

}

class DataComparator implements Comparator<Data> {

    @Override
    public int compare(Data x, Data y) {
        if (x.arrivalTime < y.arrivalTime) {
            return -1;
        } else if (x.arrivalTime > y.arrivalTime) {
            return 1;
        } else {
            return 0;
        }

    }

}
