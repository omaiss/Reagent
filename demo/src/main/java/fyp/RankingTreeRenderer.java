package fyp;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class RankingTreeRenderer extends DefaultTreeCellRenderer {

    private final Color topRankColor = new Color(92, 184, 92);
    private final Color midRankColor = new Color(240, 173, 78);
    private final Color lowRankColor = new Color(217, 83, 79);

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        String text = node.getUserObject().toString();

        try {
            int score = extractScore(text);
            setForeground(score >= 8 ? topRankColor : score >= 5 ? midRankColor : lowRankColor);
            setFont(new Font("Consolas", Font.BOLD, 14));
        } catch (Exception e) {
            setForeground(Color.BLACK);
        }

        return component;
    }

    private int extractScore(String text) {
        try {
            String scorePart = text.substring(text.indexOf("Score:") + 7).trim();
            return Integer.parseInt(scorePart.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
