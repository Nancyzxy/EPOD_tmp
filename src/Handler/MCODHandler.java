package Handler;

import framework.EdgeNode;
import java.util.ArrayList;
import utils.Constants;

public class MCODHandler extends Handler {
    public MCODHandler(EdgeNode node) {
        super(node);
    }

    @Override
    public boolean neighboringSet(ArrayList<?> c1, ArrayList<?> c2) {
    // 1.��������
        double sum = 0;
        for (int i = 0; i < c1.size(); i++) {
            sum += (Math.pow((double) c1.get(i) - (double) c2.get(i), 2));
        }
        return sum <= 4 * Constants.R * Constants.R;
    // 2.��ʹ�û����������map_to_MCO�еģ�����ӳ���ԭ����distance.calculate
    // 3.new�������� ��2R֮��
    // MCO a = new MCO(new Vector(c1.stream().collect(Collectors.toList())));
    }
}
