package dashgraph;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.networktables.TableEntryListener;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    JLabel addLabel;
    JTextField addVarField;
    JLabel removeLabel;
    JTextField removeVarField;

    JButton clearButton;
    JRadioButton enableButton;

    NetworkTable table;

    List<String> graphedVars;

    JPanel holder;
    ChartPanel chartPanel;
    StringProperty timeVar;

    boolean enabled = true;

    public DashGraph() {
        data = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart("", "Time", "value", data, PlotOrientation.VERTICAL, true, true, true);

        addLabel = new JLabel("Add a variable: ");
        addLabel.setMinimumSize(new Dimension(40, 18));
        addLabel.setFocusable(false);

        addVarField = new JTextField();
        addVarField.setFocusable(true);
        addVarField.addActionListener(new AddVarListener());
        addVarField.setMinimumSize(new Dimension(25, 18));
        addVarField.setMaximumSize(new Dimension(100, 18));

        removeLabel = new JLabel("Remove a variable: ");
        removeLabel.setMinimumSize(new Dimension(40, 18));
        removeLabel.setFocusable(false);

        removeVarField = new JTextField();
        removeVarField.setFocusable(true);
        removeVarField.addActionListener(new RemoveVarListener());
        removeVarField.setMinimumSize(addVarField.getMinimumSize());
        removeVarField.setMaximumSize(addVarField.getMaximumSize());

        clearButton = new JButton("Clear");
        clearButton.setFocusable(false);
        clearButton.addActionListener(new ClearListener());
        clearButton.setMinimumSize(new Dimension(70, 20));
        clearButton.setMaximumSize(new Dimension(80, 20));

        enableButton = new JRadioButton("Enabled");
        enableButton.setFocusable(false);
        enableButton.addActionListener(new EnableListener());
        enableButton.setSelected(true);
        enableButton.setMinimumSize(new Dimension(70, 20));
        enableButton.setMaximumSize(new Dimension(80, 20));

        chartPanel = new ChartPanel(chart);

        holder = new JPanel();
        holder.setMinimumSize(new Dimension(300, 200));
        holder.setPreferredSize(new Dimension(600, 400));
        holder.setMaximumSize(new Dimension(1000, 800));

        super.setResizable(true);

        graphedVars = new ArrayList<>();

        timeVar = new StringProperty(this, "Time Variable", "Time");
    }

    @Override
    public void init() {
        holder.setLayout(buildLayout(holder));
        setLayout(new BorderLayout());

        setMinimumSize(holder.getMinimumSize());
        setPreferredSize(holder.getPreferredSize());
        setMaximumSize(holder.getMaximumSize());

        table = NetworkTableInstance.getDefault().getTable("SmartDashboard");
        table.addEntryListener(new TimeListener(), 2 + 16);

        add(holder, BorderLayout.CENTER);
    }

    public GroupLayout buildLayout(JPanel p) {
        GroupLayout layout = new GroupLayout(p);

        SequentialGroup bottomHorizontalGroup = layout.createSequentialGroup();
        bottomHorizontalGroup.addComponent(addLabel).addComponent(addVarField).addComponent(removeLabel).addComponent(removeVarField).addComponent(enableButton).addComponent(clearButton);
        ParallelGroup bottomVerticalGroup = layout.createParallelGroup();
        bottomVerticalGroup.addComponent(addLabel).addComponent(addVarField).addComponent(removeLabel).addComponent(removeVarField).addComponent(enableButton).addComponent(clearButton);

        layout.setHorizontalGroup(layout.createParallelGroup().addComponent(chartPanel).addGroup(GroupLayout.Alignment.CENTER, bottomHorizontalGroup));
        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(chartPanel).addGroup(bottomVerticalGroup));

        return layout;
    }

    @Override
    public void propertyChanged(Property prprt) {
        if (prprt == timeVar) {
            chart.getXYPlot().getDomainAxis().setLabel(timeVar.getValue());
        }
    }

    private class ClearListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (DashGraph.this) {
                graphedVars.forEach((s) -> {
                    DashGraph.this.data.getSeries(s).clear();
                });
                System.out.println("Cleared graph");
            }
        }
    }

    private class EnableListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (DashGraph.this) {
                DashGraph.this.enabled = DashGraph.this.enableButton.isSelected();
            }
        }
    }

    private class AddVarListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
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
        }
    }

    private class RemoveVarListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
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
        }
    }

    private class TimeListener implements TableEntryListener {

        @Override
        public void valueChanged(NetworkTable nt, String key, NetworkTableEntry nte, NetworkTableValue ntv, int i) {
            synchronized (DashGraph.this) {
                if (DashGraph.this.enabled && key.equals("Time")) {
                    for (String s : graphedVars) {
                        if (nt.getEntry(s).exists()) {
                            data.getSeries(s).add(ntv.getDouble(), nt.getEntry(s).getDouble(0));
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        NetworkTableInstance.getDefault().startServer();
        NetworkTable x = NetworkTableInstance.getDefault().getTable("SmartDashboard");

        DashGraph g = new DashGraph();
        g.init();
        g.showInWindow();

        while (true) {
            x.getEntry("Time").setDouble(System.currentTimeMillis());
            x.getEntry("shit").setDouble(3);
        }
        /*
        Robot.setHost("localhost");

        DashGraph g = new DashGraph();
        g.init();
        g.showInWindow();

        int x = 0;
        while (x < 50000) {
            Robot.getTable().putNumber("Time", x);
            Robot.getTable().putNumber("test", x);
            x = x + 1;
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(DashGraph.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("done");
         */
    }

    public void showInWindow() {
        JFrame f = new JFrame();
        f.add(holder);
        f.setMinimumSize(holder.getMinimumSize());
        f.setVisible(true);
    }
}
