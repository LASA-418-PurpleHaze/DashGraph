package dashgraph;

import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.robot.Robot;
import javax.swing.JTextField;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;
import edu.wpi.first.wpilibj.tables.TableKeyNotDefinedException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;

public class DashGraph extends StaticWidget {

    JFreeChart chart;
    XYSeriesCollection data;

    JLabel addLabel;
    JTextField addVarField;
    JLabel removeLabel;
    JTextField removeVarField;

    JButton clearButton;
    JRadioButton enableButton;

    ITable robotTable;

    LinkedList<String> graphedVars;

    JPanel holder;
    ChartPanel chartPanel;

    boolean enabled = true;

    public DashGraph() {
        data = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart("Grapher", "category", "value", data, PlotOrientation.VERTICAL, true, true, true);

        addLabel = new JLabel("Add a variable: ");
        addVarField = new JTextField();
        addVarField.addActionListener(new AddVarListener());

        removeLabel = new JLabel("Remove a variable: ");
        removeVarField = new JTextField();
        removeVarField.addActionListener(new RemoveVarListener());
        
        robotTable = Robot.getTable();
        

        clearButton = new JButton("Clear Graph");
        clearButton.addActionListener(new ClearListener());

        enableButton = new JRadioButton("Graph Enabled");
        enableButton.addActionListener(new EnableListener());
        enableButton.setSelected(true);

        graphedVars = new LinkedList();
    }

    @Override
    public void init() {
        chartPanel = new ChartPanel(chart);
        chartPanel.setSize(600, 600);
        chartPanel.setVisible(true);

        holder = new JPanel();
        holder.setLayout(buildLayout(holder));
        
        robotTable.addTableListener(new TimeListener());
        
        add(holder);
    }

    public GroupLayout buildLayout(JPanel p) {
        GroupLayout layout = new GroupLayout(p);

        SequentialGroup bottomHorizontalGroup = layout.createSequentialGroup();
        bottomHorizontalGroup.addComponent(addLabel).addComponent(addVarField, 90, 90, 90).addComponent(removeLabel).addComponent(removeVarField, 90, 90, 90).addComponent(enableButton).addComponent(clearButton);
        ParallelGroup bottomVerticalGroup = layout.createParallelGroup();
        bottomVerticalGroup.addComponent(addLabel).addComponent(addVarField, 18, 18, 18).addComponent(removeLabel).addComponent(removeVarField, 18, 18, 18).addComponent(enableButton).addComponent(clearButton);
        layout.setHorizontalGroup(layout.createParallelGroup().addComponent(chartPanel, 600, 600, 600).addGroup(bottomHorizontalGroup));
        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(chartPanel, 400, 400, 400).addGroup(bottomVerticalGroup));

        return layout;
    }

    @Override
    public void propertyChanged(Property prprt) {
    }

    private class ClearListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            for (String s : graphedVars) {
                DashGraph.this.data.getSeries(s).clear();
            }
            System.out.println("Cleared graph");
        }
    }

    private class EnableListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println(DashGraph.this.enableButton.isSelected());
            DashGraph.this.enabled = DashGraph.this.enableButton.isSelected();
        }
    }

    private class AddVarListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            String newVar = addVarField.getText();
            addVarField.setText("");

            try {
                robotTable.getNumber(newVar);

                if (!graphedVars.contains(newVar)) {
                    graphedVars.add(newVar);
                    data.addSeries(new XYSeries(newVar));
                }
            } catch (TableKeyNotDefinedException exception) {
                System.out.println("Tried to add nonexistant key");
            }
        }
    }

    private class RemoveVarListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
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

    private class TimeListener implements ITableListener {
        
        @Override
        public void valueChanged(ITable source, String key, Object value, boolean isNew) {
            if (DashGraph.this.enabled && key.equals("Time")) {
                for (String s : graphedVars) {
                    try {
                        data.getSeries(s).add((double) value, robotTable.getNumber(s));
                    } catch (TableKeyNotDefinedException e) {
                        System.out.println("Attempted to graph nonexistant key");
                    }
                }
            }
        }
    }
}
