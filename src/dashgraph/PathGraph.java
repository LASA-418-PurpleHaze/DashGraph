package dashgraph;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.smartdashboard.properties.DoubleProperty;
import edu.wpi.first.smartdashboard.properties.FileProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import static edu.wpi.first.wpiutil.RuntimeDetector.isWindows;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.Align;

public class PathGraph extends DashGraph {

    StringProperty robotCoord, pathCoord;
    FileProperty bgImage;
    DoubleProperty distanceUnitsPerInch;
    DoubleProperty fieldLength, fieldWidth, centerFieldX, centerFieldY;
    DoubleProperty gridLineInterval;

    XYSeries robotCoordSeries, pathCoordSeries;
    double[] def = new double[]{0, 0};

    public PathGraph() {
        robotCoord = new StringProperty(this, "Robot XY Dashboard key", "robotXY");
        pathCoord = new StringProperty(this, "Path XY Dashboard key", "pathXY");
        bgImage = new FileProperty(this, "Background image file", (isWindows() ? System.getenv("USERPROFILE") : System.getProperty("user.home")).concat("/SmartDashboard/extensions/field_2018.png"));
        fieldLength = new DoubleProperty(this, "Field length (ft.)", 54);
        fieldWidth = new DoubleProperty(this, "Field width (ft.)", 27);
        centerFieldX = new DoubleProperty(this, "X coordinate of centerfield (ft.)", 27);
        centerFieldY = new DoubleProperty(this, "Y coordinate of centerfield (ft.)", 0);
        distanceUnitsPerInch = new DoubleProperty(this, "Distance units per inch", 1.0);
        gridLineInterval = new DoubleProperty(this, "Gridline interval (distance units)", 10);
    }

    @Override
    public void init() {
        chart.getXYPlot().getDomainAxis().setLabel("X");
        chart.getXYPlot().getDomainAxis().setAutoRange(false);
        chart.getXYPlot().getDomainAxis().setRange(
                12 * distanceUnitsPerInch.getValue() * (centerFieldX.getValue() - fieldLength.getValue() / 2),
                12 * distanceUnitsPerInch.getValue() * (centerFieldX.getValue() + fieldLength.getValue() / 2));

        chart.getXYPlot().getRangeAxis().setLabel("Y");
        chart.getXYPlot().getRangeAxis().setAutoRange(false);
        chart.getXYPlot().getRangeAxis().setRange(
                12 * distanceUnitsPerInch.getValue() * (centerFieldY.getValue() - fieldWidth.getValue() / 2),
                12 * distanceUnitsPerInch.getValue() * (centerFieldY.getValue() + fieldWidth.getValue() / 2));

        ((NumberAxis) chart.getXYPlot().getDomainAxis()).setTickUnit(new NumberTickUnit(gridLineInterval.getValue()));
        ((NumberAxis) chart.getXYPlot().getRangeAxis()).setTickUnit(new NumberTickUnit(gridLineInterval.getValue()));
        ((NumberAxis) chart.getXYPlot().getDomainAxis()).setTickLabelFont(new Font(Font.DIALOG, Font.PLAIN, 6));
        ((NumberAxis) chart.getXYPlot().getRangeAxis()).setTickLabelFont(new Font(Font.DIALOG, Font.PLAIN, 6));

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

        robotCoordSeries = new XYSeries(robotCoord.getValue(), false, true);
        pathCoordSeries = new XYSeries(pathCoord.getValue(), false, true);
        data.addSeries(robotCoordSeries);
        data.addSeries(pathCoordSeries);

        dashTable.addEntryListener((NetworkTable nt, String key, NetworkTableEntry nte, NetworkTableValue ntv, int i) -> {
            synchronized (lock) {
                if (enabled && (key.equals(robotCoord.getName()) || key.equals(pathCoord.getName()))) {
                    double[] newPoint = nte.getDoubleArray(def);
                    data.getSeries(key).add(newPoint[0], newPoint[1]);
                }
            }
        }, EntryListenerFlags.kLocal | EntryListenerFlags.kUpdate);

        GroupLayout layout = new GroupLayout(holder);
        GroupLayout.SequentialGroup bottomHorizontalGroup = layout.createSequentialGroup();
        bottomHorizontalGroup.addComponent(enableButton).addComponent(clearButton);
        GroupLayout.ParallelGroup bottomVerticalGroup = layout.createParallelGroup();
        bottomVerticalGroup.addComponent(enableButton).addComponent(clearButton);
        layout.setHorizontalGroup(layout.createParallelGroup().addComponent(chartPanel).addGroup(GroupLayout.Alignment.CENTER, bottomHorizontalGroup));
        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(chartPanel).addGroup(bottomVerticalGroup));
        holder.setLayout(layout);

        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(500, 250));
        add(holder, BorderLayout.CENTER);
    }

    @Override
    public void propertyChanged(Property prprt) {
        synchronized (lock) {
            if (prprt == bgImage) {
                try {
                    File f = new File(bgImage.getValue());
                    Image fieldImage = ImageIO.read(f);

                    SwingUtilities.invokeLater(() -> chart.getPlot().setBackgroundImage(fieldImage));
                } catch (IOException ex) {
                }
            } else if (prprt == gridLineInterval) {
                ((NumberAxis) chart.getXYPlot().getDomainAxis()).setTickUnit(new NumberTickUnit(gridLineInterval.getValue()));
                ((NumberAxis) chart.getXYPlot().getRangeAxis()).setTickUnit(new NumberTickUnit(gridLineInterval.getValue()));
            } else if (prprt == distanceUnitsPerInch || prprt == centerFieldX
                    || prprt == centerFieldY || prprt == fieldLength || prprt == fieldWidth) {
                chart.getXYPlot().getDomainAxis().setRange(
                        12 * distanceUnitsPerInch.getValue() * (centerFieldX.getValue() - fieldLength.getValue() / 2),
                        12 * distanceUnitsPerInch.getValue() * (centerFieldX.getValue() + fieldLength.getValue() / 2));
                chart.getXYPlot().getRangeAxis().setRange(
                        12 * distanceUnitsPerInch.getValue() * (centerFieldY.getValue() - fieldWidth.getValue() / 2),
                        12 * distanceUnitsPerInch.getValue() * (centerFieldY.getValue() + fieldWidth.getValue() / 2));
            } else if (prprt == robotCoord) {
                data.removeSeries(robotCoordSeries);
                robotCoordSeries = new XYSeries(robotCoord.getValue(), false, true);
                data.addSeries(robotCoordSeries);
            } else if (prprt == pathCoord) {
                data.removeSeries(pathCoordSeries);
                pathCoordSeries = new XYSeries(pathCoord.getValue(), false, true);
                data.addSeries(pathCoordSeries);
            } else {
                System.out.println("Unhandled property change!");
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
