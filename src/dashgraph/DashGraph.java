package dashgraph;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class DashGraph extends StaticWidget {

    JFreeChart chart;
    XYSeriesCollection data;

    NetworkTable table;

    List<String> graphedVars;

    JPanel holder;
    ChartPanel chartPanel;
    StringProperty timeVar;

    boolean enabled = true;

    @Override
    public void init() {
        data = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart("", "Time", "value", data, PlotOrientation.VERTICAL, true, true, true);

        JLabel addLabel = new JLabel("Add a variable: ");
        addLabel.setMinimumSize(new Dimension(40, 18));
        addLabel.setFocusable(false);

        JTextField addVarField = new JTextField();
        addVarField.setFocusable(true);
        addVarField.addActionListener((ActionEvent event) -> {
            synchronized (DashGraph.this) {
                String newVar = addVarField.getText();
                addVarField.setText("");

                if (!newVar.equals(timeVar.getValue()) && table.containsKey(newVar)) {

                    if (!graphedVars.contains(newVar)) {
                        graphedVars.add(newVar);
                        data.addSeries(new XYSeries(newVar));
                    }
                } else {
                    System.out.println("Tried to add invalid key!");
                }
            }
        });
        addVarField.setMinimumSize(new Dimension(25, 18));
        addVarField.setMaximumSize(new Dimension(100, 18));

        JLabel removeLabel = new JLabel("Remove a variable: ");
        removeLabel.setMinimumSize(new Dimension(40, 18));
        removeLabel.setFocusable(false);

        JTextField removeVarField = new JTextField();
        removeVarField.setFocusable(true);
        removeVarField.addActionListener((ActionEvent e) -> {
            synchronized (DashGraph.this) {
                String removedVar = removeVarField.getText();
                removeVarField.setText("");
                if (graphedVars.remove(removedVar)) {
                    System.out.println("Removed var");
                    data.removeSeries(data.getSeries(removedVar));
                } else {
                    System.out.println("Could not remove var");
                }
            }
        });
        removeVarField.setMinimumSize(addVarField.getMinimumSize());
        removeVarField.setMaximumSize(addVarField.getMaximumSize());

        JButton clearButton = new JButton("Clear");
        clearButton.setFocusable(false);
        clearButton.addActionListener((ActionEvent e) -> {
            clearGraph();
        });
        clearButton.setMinimumSize(new Dimension(70, 20));
        clearButton.setMaximumSize(new Dimension(80, 20));

        JRadioButton enableButton = new JRadioButton("Enabled");
        enableButton.setFocusable(false);
        enableButton.addActionListener((ActionEvent e) -> {
            synchronized (DashGraph.this) {
                DashGraph.this.enabled = enableButton.isSelected();
            }
        });
        enableButton.setSelected(true);
        enableButton.setMinimumSize(new Dimension(70, 20));
        enableButton.setMaximumSize(new Dimension(80, 20));

        chartPanel = new ChartPanel(chart);

        holder = new JPanel();

        super.setResizable(true);

        graphedVars = new ArrayList<>();

        timeVar = new StringProperty(this, "Time Variable", "Time");

        GroupLayout layout = new GroupLayout(holder);

        SequentialGroup bottomHorizontalGroup = layout.createSequentialGroup();
        bottomHorizontalGroup.addComponent(addLabel).addComponent(addVarField).addComponent(removeLabel).addComponent(removeVarField).addComponent(enableButton).addComponent(clearButton);
        ParallelGroup bottomVerticalGroup = layout.createParallelGroup();
        bottomVerticalGroup.addComponent(addLabel).addComponent(addVarField).addComponent(removeLabel).addComponent(removeVarField).addComponent(enableButton).addComponent(clearButton);

        layout.setHorizontalGroup(layout.createParallelGroup().addComponent(chartPanel).addGroup(GroupLayout.Alignment.CENTER, bottomHorizontalGroup));
        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(chartPanel).addGroup(bottomVerticalGroup));

        holder.setLayout(layout);

        setLayout(new BorderLayout());

        setMinimumSize(new Dimension(300, 200));
        setPreferredSize(new Dimension(600, 400));
        setMaximumSize(new Dimension(1000, 800));

        table = NetworkTableInstance.getDefault().getTable("SmartDashboard");
        table.addEntryListener((NetworkTable nt, String key, NetworkTableEntry nte, NetworkTableValue ntv, int i) -> {
            synchronized (DashGraph.this) {
                if (DashGraph.this.enabled && key.equals(timeVar.getValue())) {
                    graphedVars.stream().filter((s) -> (nt.getEntry(s).exists())).forEachOrdered((s) -> {
                        data.getSeries(s).add(ntv.getDouble(), nt.getEntry(s).getDouble(0));
                    });
                }
            }
        }, EntryListenerFlags.kLocal | EntryListenerFlags.kUpdate);

        add(holder, BorderLayout.CENTER);
    }

    @Override
    public void propertyChanged(Property prprt) {
        if (prprt == timeVar) {
            chart.getXYPlot().getDomainAxis().setLabel(timeVar.getValue());
            clearGraph();
        }
    }

    private synchronized void clearGraph() {
        graphedVars.forEach((s) -> {
            data.getSeries(s).clear();
        });
        System.out.println("Cleared graph");
    }

    public static void main(String[] args) {
        NetworkTable x = NetworkTableInstance.getDefault().getTable("SmartDashboard");

        DashGraph g = new DashGraph();
        g.init();

        JFrame f = new JFrame();
        f.add(g);
        f.setMinimumSize(g.getMinimumSize());
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while (true) {
            x.getEntry("Time").setDouble(System.currentTimeMillis());
            x.getEntry("shit").setDouble(3);
        }
    }
}
