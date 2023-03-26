package rtree;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName RTree
 * @Description
 */
public class RTree {
    private RTNode root; // 根节点
    private int tree_type; // 树类型
    private int nodeCapacity = -1; // 结点容量
    private float fillFactor = -1; // 结点填充因子 ，用于计算每个结点最小条目个数
    private int dimension; // 维度

    public RTree(int capacity, float fillFactor, int type, int dimension) {
        this.fillFactor = fillFactor;
        tree_type = type;
        nodeCapacity = capacity;
        this.dimension = dimension;
        root = new RTDataNode(this, Constants.NULL); // 根节点的父节点为NULL
    }

    /**
     * @return RTree的维度
     */
    public int getDimension() {
        return dimension;
    }

    /** 设置跟节点 */
    public void setRoot(RTNode root) {
        this.root = root;
    }

    /**
     * @return 填充因子
     */
    public float getFillFactor() {
        return fillFactor;
    }

    /**
     * @return 返回结点容量
     */
    public int getNodeCapacity() {
        return nodeCapacity;
    }

    /**
     * @return 返回树的类型
     */
    public int getTreeType() {
        return tree_type;
    }

    /**
     * --> 向Rtree中插入Rectangle 1、先找到合适的叶节点 2、再向此叶节点中插入
     *
     * @param rectangle
     */
    public boolean insert(Rectangle rectangle) {
        if (rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getHigh().getDimension() != getDimension()) // 矩形维度与树的维度不一致
        {
            throw new IllegalArgumentException("Rectangle dimension different than RTree dimension.");
        }

        RTDataNode leaf = root.chooseLeaf(rectangle);

        return leaf.insert(rectangle);
    }

    public ArrayList<Point> search(Rectangle rectangle){
        if (rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getHigh().getDimension() != getDimension()) // 矩形维度与树的维度不一致
        {
            throw new IllegalArgumentException("Rectangle dimension different than RTree dimension.");
        }
        return root.search(rectangle);
    }

    /**
     * 从R树中删除Rectangle
     * <p>
     * 1、寻找包含记录的结点--调用算法findLeaf()来定位包含此记录的叶子结点L，如果没有找到则算法终止。<br>
     * 2、删除记录--将找到的叶子结点L中的此记录删除<br>
     * 3、调用算法condenseTree<br>
     *
     * @param rectangle
     * @return
     */
    public int delete(Rectangle rectangle) {
        if (rectangle == null) {
            throw new IllegalArgumentException("Rectangle cannot be null.");
        }

        if (rectangle.getHigh().getDimension() != getDimension()) {
            throw new IllegalArgumentException("Rectangle dimension different than RTree dimension.");
        }

        RTDataNode leaf = root.findLeaf(rectangle);

        if (leaf != null) {
            return leaf.delete(rectangle);
        }

        return -1;
    }

    /**
     * 从给定的结点root开始遍历所有的结点
     *
     * @param root
     * @return 所有遍历的结点集合
     */
    public List<RTNode> traversePostOrder(RTNode root) {
        if (root == null)
            throw new IllegalArgumentException("Node cannot be null.");

        List<RTNode> list = new ArrayList<RTNode>();
        list.add(root);

        if (!root.isLeaf()) {
            for (int i = 0; i < root.usedSpace; i++) {
                List<RTNode> a = traversePostOrder(((RTDirNode) root).getChild(i));
                for (int j = 0; j < a.size(); j++) {
                    list.add(a.get(j));
                }
            }
        }

        return list;
    }

    public static void main(String[] args) throws Exception {
        // 结点容量：4、填充因子：0.4、树类型：二维
        RTree tree = new RTree(4, 0.4f, Constants.RTREE_QUADRATIC, 2);
        // 每一行的四个数构成两个点（一个矩形）
        float[] f = {10,20,10,20,
                    10,30,10,30,
                    0,20,0,20,
                    20,20,20,20,
                    10,10,10,10,
                    0,30,0,30};

        // 插入结点
        for (int i = 0; i < f.length;) {
            Point p1 = new Point(new float[] { f[i++], f[i++] });
            Point p2 = new Point(new float[] { f[i++], f[i++] });
            final Rectangle rectangle = new Rectangle(p1, p2);
            tree.insert(rectangle);

            Rectangle[] rectangles = tree.root.datas;
            System.out.println("========================");
            System.out.println("level:" + tree.root.level);
        }
        System.out.println("---------------------------------");
        System.out.println("Insert finished.");

        ArrayList<Point>points = tree.search(new Rectangle(new Point(new float[]{10,20}),10));
        int a = 1;
        /*
        // 删除结点
        System.out.println("---------------------------------");
        System.out.println("Begin delete.");

        for (int i = 0; i < f.length;) {
            Point p1 = new Point(new float[] { f[i++], f[i++] });
            Point p2 = new Point(new float[] { f[i++], f[i++] });
            final Rectangle rectangle = new Rectangle(p1, p2);
            tree.delete(rectangle);                      111111

            Rectangle[] rectangles = tree.root.datas;
            System.out.println(tree.root.level);
            for (int j = 0; j < rectangles.length; j++)
                System.out.println(rectangles[j]);
        }

        System.out.println("---------------------------------");
        System.out.println("Delete finished.");

        Rectangle[] rectangles = tree.root.datas;
        for (int i = 0; i < rectangles.length; i++)
            System.out.println(rectangles[i]);
*/
    }
}