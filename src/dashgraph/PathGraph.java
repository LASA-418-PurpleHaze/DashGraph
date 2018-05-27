package dashgraph;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;

public class PathGraph extends DashGraph {

    StringProperty robotCoord, pathCoord;
    double[] def = new double[]{0, 0};

    @Override
    public void init() {
        robotCoord = new StringProperty(this, "robotXY", "robotXY");
        pathCoord = new StringProperty(this, "pathXY", "pathXY");

        chart.getXYPlot().getDomainAxis().setLabel("X");
        chart.getXYPlot().getRangeAxis().setLabel("Y");

        data.addSeries(new XYSeries("robotXY"));
        data.addSeries(new XYSeries("pathXY"));

        dashTable.addEntryListener((NetworkTable nt, String key, NetworkTableEntry nte, NetworkTableValue ntv, int i) -> {
            synchronized (PathGraph.this) {
                if (PathGraph.this.enabled && (key.equals(robotCoord.getName()) || key.equals(pathCoord.getName()))) {
                    double[] newPoint = nte.getDoubleArray(def);
                    data.getSeries(key).add(newPoint[0], newPoint[1]);
                }
            }
        }, EntryListenerFlags.kLocal | EntryListenerFlags.kUpdate);

        add(chartPanel);
    }

    @Override
    public void propertyChanged(Property prprt) {
    }

    public static void main(String[] args) throws InterruptedException {
        NetworkTable x = NetworkTableInstance.getDefault().getTable("SmartDashboard");

        PathGraph g = new PathGraph();
        g.init();

        JFrame f = new JFrame();
        f.add(g);
        f.setMinimumSize(g.getMinimumSize());
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while (true) {
            x.getEntry("robotXY").setDoubleArray(new double[]{System.currentTimeMillis(), System.nanoTime()});
            Thread.sleep(20);
        }
    }

}
