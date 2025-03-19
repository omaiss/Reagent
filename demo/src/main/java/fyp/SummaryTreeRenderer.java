package fyp;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SummaryTreeRenderer extends DefaultTreeCellRenderer {
    private Color hoverBackgroundColor = new JBColor(new Color(220, 235, 245), Gray._0);
    private final Color selectedGradientStart = new JBColor(new Color(100, 149, 237), Gray._0);
    private final Color selectedGradientEnd = new JBColor(new Color(70, 130, 180), Gray._0);

    public SummaryTreeRenderer() {
        super();
    }

    public void attachToTree(JTree tree) {
        tree.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                tree.repaint();
            }
        });
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        String text = node.getUserObject().toString();

        // Font & Padding Enhancements
        component.setFont(new Font("Consolas", Font.PLAIN, 14));
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        // Background Gradient for Selection
        if (sel) {
            setOpaque(false);
            setForeground(JBColor.WHITE);
        } else {
            setOpaque(true);
            setBackground(JBColor.WHITE);
        }

        // Dynamic Foreground Colors
        if (text.startsWith("📋")) {
            setForeground(new JBColor(new Color(0, 102, 204), Gray._0)); // Blue for Summary
            setFont(new Font("Consolas", Font.BOLD, 16));
        } else if (text.startsWith("🔴")) {
            setForeground(new JBColor(new Color(217, 83, 79), Gray._0)); // Red for Critical Issues
        } else if (text.startsWith("🟠")) {
            setForeground(new JBColor(new Color(240, 173, 78), Gray._0)); // Orange for Warnings
        } else if (text.startsWith("🟢")) {
            setForeground(new JBColor(new Color(92, 184, 92), Gray._0)); // Green for Fixes
        } else if (text.startsWith("📜")) {
            setForeground(new JBColor(new Color(102, 16, 242), Gray._0)); // Purple for Code Snippets
        }

        // Custom Icons for Better Visualization
        if (leaf) {
            setIcon(new ImageIcon("icons/document.png"));
        } else {
            setIcon(new ImageIcon("icons/folder.png"));
        }

        return new GradientPanel(sel ? selectedGradientStart : getBackground(), sel ? selectedGradientEnd : getBackground(), this);
    }

    public Color getHoverBackgroundColor() {
        return hoverBackgroundColor;
    }

    public void setHoverBackgroundColor(Color hoverBackgroundColor) {
        this.hoverBackgroundColor = hoverBackgroundColor;
    }

    // Custom Gradient Background Panel
    static class GradientPanel extends JPanel {
        private final Color colorStart, colorEnd;

        public GradientPanel(Color start, Color end, Component child) {
            this.colorStart = start;
            this.colorEnd = end;
            setLayout(new BorderLayout());
            add(child, BorderLayout.CENTER);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(0, 0, colorStart, getWidth(), getHeight(), colorEnd);
            g2d.setPaint(paint);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }
}
