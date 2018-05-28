package dashgraph;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.smartdashboard.properties.FileProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import static edu.wpi.first.wpiutil.RuntimeDetector.isWindows;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.Align;

public class PathGraph extends DashGraph {

    StringProperty robotCoord, pathCoord;
    FileProperty bgImage;
    double[] def = new double[]{0, 0};

    @Override
    public void init() {
        robotCoord = new StringProperty(this, "robotXY", "robotXY");
        pathCoord = new StringProperty(this, "pathXY", "pathXY");
        bgImage = new FileProperty(this, "image", (isWindows() ? System.getenv("USERPROFILE") : System.getProperty("user.home")).concat("/SmartDashboard/extensions/field_2018.png"));

        chart.getXYPlot().getDomainAxis().setLabel("X");
        chart.getXYPlot().getDomainAxis().setAutoRange(false);
        chart.getXYPlot().getDomainAxis().setRange(0, 54 * 12);

        chart.getXYPlot().getRangeAxis().setLabel("Y");
        chart.getXYPlot().getRangeAxis().setAutoRange(false);
        chart.getXYPlot().getRangeAxis().setRange(-27 * 12 / 2, 27 * 12 / 2);

        try {
            File f = new File(bgImage.getValue());
            Image fieldImage = ImageIO.read(f);

            chart.getPlot().setBackgroundImage(fieldImage);
            chart.getPlot().setBackgroundImageAlpha((float) 1.0);
            chart.getPlot().setBackgroundImageAlignment(Align.FIT);
            chart.getPlot().setBackgroundPaint(new Color(0, 0, 0, 0));
        } catch (Exception e) {
            System.err.println("error reading field image");
        }

        chartPanel.setDomainZoomable(false);
        chartPanel.setRangeZoomable(false);

        data.addSeries(new XYSeries("robotXY"));
        data.addSeries(new XYSeries("pathXY"));

        dashTable.addEntryListener((NetworkTable nt, String key, NetworkTableEntry nte, NetworkTableValue ntv, int i) -> {
            synchronized (lock) {
                if (PathGraph.this.enabled && (key.equals(robotCoord.getName()) || key.equals(pathCoord.getName()))) {
                    double[] newPoint = nte.getDoubleArray(def);
                    data.getSeries(key).add(newPoint[0], newPoint[1]);
                }
            }
        }, EntryListenerFlags.kLocal | EntryListenerFlags.kUpdate);

        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(500, 250));
        add(chartPanel, BorderLayout.CENTER);
    }

    @Override
    public void propertyChanged(Property prprt) {
        if (prprt == bgImage) {
            try {
                File f = new File(bgImage.getValue());
                Image fieldImage = ImageIO.read(f);

                SwingUtilities.invokeLater(() -> chart.getPlot().setBackgroundImage(fieldImage));
            } catch (IOException ex) {
            }
        }
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
            x.getEntry("robotXY").setDoubleArray(new double[]{System.currentTimeMillis(), 5});
            Thread.sleep(20);
        }
    }

}
