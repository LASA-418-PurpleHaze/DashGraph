package dashgraph;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public abstract class DashGraph extends StaticWidget {

    XYSeriesCollection data;
    JFreeChart chart;
    JPanel holder;
    JButton clearButton;
    JRadioButton enableButton;
    ChartPanel chartPanel;
    NetworkTable dashTable;
    final Object lock;
    boolean enabled = true;

    public DashGraph() {
        lock = new Object();
        data = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart("", "", "", data, PlotOrientation.VERTICAL, true, true, true);
        chartPanel = new ChartPanel(chart);
        holder = new JPanel();

        dashTable = NetworkTableInstance.getDefault().getTable("SmartDashboard");

        clearButton = new JButton("Clear");
        clearButton.setFocusable(false);
        clearButton.addActionListener((ActionEvent e) -> {
            clearGraph();
        });
        clearButton.setMinimumSize(new Dimension(70, 20));
        clearButton.setMaximumSize(new Dimension(80, 20));

        enableButton = new JRadioButton("Enabled");
        enableButton.setFocusable(false);
        enableButton.addActionListener((ActionEvent e) -> {
            synchronized (lock) {
                DashGraph.this.enabled = enableButton.isSelected();
            }
        });
        enableButton.setSelected(true);
        enableButton.setMinimumSize(new Dimension(70, 20));
        enableButton.setMaximumSize(new Dimension(80, 20));
    }

    protected void clearGraph() {
        synchronized (lock) {
            data.getSeries().forEach((Object o) -> ((XYSeries) o).clear());
            System.out.println("Cleared graph");
        }
    }
}
