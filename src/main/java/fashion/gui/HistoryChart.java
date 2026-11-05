package fashion.gui;

import fashion.model.TrendSignal;
import javax.swing.JPanel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Date-spaced observations; no interpolation or invented values for missing dates. */
public final class HistoryChart extends JPanel {
    private List<TrendSignal> signals = List.of();

    public HistoryChart() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(700, 240));
        getAccessibleContext().setAccessibleName("Trend popularity history chart");
    }

    public void setSignals(List<TrendSignal> signals) {
        this.signals = List.copyOf(signals);
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(55, 65, 81));
            if (signals.isEmpty()) {
                g.drawString("Choose History to plot imported observations.", 24, 36);
                return;
            }
            if (signals.stream().map(TrendSignal::category).distinct().count() > 1) {
                g.drawString("Select one category to plot this trend. All observations are shown in the table.", 24, 36);
                return;
            }
            Map<LocalDate, Double> values = signals.stream().collect(Collectors.groupingBy(
                    TrendSignal::date, TreeMap::new, Collectors.averagingDouble(TrendSignal::popularity)));
            var dates = values.keySet().stream().toList();
            int left = 55, right = Math.max(left + 1, getWidth() - 65);
            int top = 42, bottom = Math.max(top + 1, getHeight() - 55);
            g.drawString("Popularity (0–100) · points are observed dates; same-date values are averaged", 16, 20);
            for (int value = 0; value <= 100; value += 25) {
                int y = bottom - (bottom - top) * value / 100;
                g.setColor(new Color(230, 233, 239));
                g.drawLine(left, y, right, y);
                g.setColor(new Color(55, 65, 81));
                g.drawString(Integer.toString(value), 20, y + 4);
            }
            long first = dates.get(0).toEpochDay(), last = dates.get(dates.size() - 1).toEpochDay();
            for (var entry : values.entrySet()) {
                int x = first == last ? (left + right) / 2
                        : left + (int) ((right - left) * ((double) (entry.getKey().toEpochDay() - first) / (last - first)));
                int y = bottom - (int) ((bottom - top) * entry.getValue() / 100);
                g.setColor(new Color(90, 61, 158));
                g.fillOval(x - 5, y - 5, 10, 10);
            }
            g.setColor(new Color(55, 65, 81));
            g.drawString(dates.get(0).toString(), left - 25, bottom + 22);
            if (dates.size() > 1) g.drawString(dates.get(dates.size() - 1).toString(), right - 50, bottom + 22);
            g.drawString("Unobserved dates have no points. See the table for exact dates and values.", 16, getHeight() - 10);
        } finally { g.dispose(); }
    }
}
