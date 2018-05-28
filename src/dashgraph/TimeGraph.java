package dashgraph;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.jfree.data.xy.XYSeries;

public class TimeGraph extends DashGraph {

    StringProperty timeVar;

    @Override
    public void init() {
        timeVar = new StringProperty(this, "Time Variable", "Time");
        chart.getXYPlot().getDomainAxis().setLabel(timeVar.getValue());
        chart.getXYPlot().getRangeAxis().setLabel("value");

        dashTable.addEntryListener((NetworkTable nt, String key, NetworkTableEntry nte, NetworkTableValue ntv, int i) -> {
            synchronized (lock) {
                ((List<XYSeries>) data.getSeries()).stream().forEachOrdered((XYSeries xy)
                        -> xy.add(ntv.getDouble(), nt.getEntry((String) xy.getKey()).getDouble(0)));
            }
        }, EntryListenerFlags.kLocal | EntryListenerFlags.kUpdate);

        JLabel addLabel = new JLabel("Add a variable: ");
        addLabel.setMinimumSize(new Dimension(40, 18));
        addLabel.setFocusable(false);

        JTextField addVarField = new JTextField();
        addVarField.addActionListener((ActionEvent event) -> {
            synchronized (lock) {
                String newVar = addVarField.getText();
                addVarField.setText("");
                if (!newVar.equals(timeVar.getValue()) && dashTable.containsKey(newVar)) {
                    try {
                        data.getSeries(newVar);
                        System.out.println("Tried to add key already being graphed!");
                    } catch (Exception e) {
                        data.addSeries(new XYSeries(newVar));
                        System.out.println("Added " + newVar);
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
        removeVarField.addActionListener((ActionEvent ev) -> {
            synchronized (lock) {
                String removedVar = removeVarField.getText();
                removeVarField.setText("");
                try {
                    data.removeSeries(data.getSeries(removedVar));
                    System.out.println("Removed var");
                } catch (Exception e) {
                    System.out.println("Could not remove var");
                }
            }
        });
        removeVarField.setMinimumSize(addVarField.getMinimumSize());
        removeVarField.setMaximumSize(addVarField.getMaximumSize());

        GroupLayout layout = new GroupLayout(holder);
        SequentialGroup bottomHorizontalGroup = layout.createSequentialGroup();
        bottomHorizontalGroup.addComponent(addLabel).addComponent(addVarField).addComponent(removeLabel).addComponent(removeVarField).addComponent(enableButton).addComponent(clearButton);
        ParallelGroup bottomVerticalGroup = layout.createParallelGroup();
        bottomVerticalGroup.addComponent(addLabel).addComponent(addVarField).addComponent(removeLabel).addComponent(removeVarField).addComponent(enableButton).addComponent(clearButton);
        layout.setHorizontalGroup(layout.createParallelGroup().addComponent(chartPanel).addGroup(GroupLayout.Alignment.CENTER, bottomHorizontalGroup));
        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(chartPanel).addGroup(bottomVerticalGroup));
        holder.setLayout(layout);

        //super.setResizable(true);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(300, 200));
        setPreferredSize(new Dimension(600, 400));
        setMaximumSize(new Dimension(1000, 800));
        add(holder, BorderLayout.CENTER);
    }

    @Override
    public void propertyChanged(Property prprt) {
        synchronized (lock) {
            if (prprt == timeVar) {
                chart.getXYPlot().getDomainAxis().setLabel(timeVar.getValue());
                clearGraph();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        NetworkTable x = NetworkTableInstance.getDefault().getTable("SmartDashboard");

        TimeGraph g = new TimeGraph();
        g.init();

        JFrame f = new JFrame();
        f.add(g);
        f.setMinimumSize(g.getMinimumSize());
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while (true) {
            x.getEntry("Time").setDouble(System.currentTimeMillis());
            x.getEntry("shit").setDouble(3);
            Thread.sleep(20);
        }
    }
}
