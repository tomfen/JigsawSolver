package pt.jigsawsolver;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class JigsawSolver {

    List<Element> elements = new ArrayList<>();

    private class Element {
        Mat img;
        Mat contour;

        Element(Mat img, Mat contour) {
            this.img = img;
            this.contour = contour;
        }
    }

    public Mat loadImage(Mat scene) {
        // Contour extraction
        Mat gray = scene.clone();
        Imgproc.cvtColor(scene, gray, Imgproc.COLOR_RGB2GRAY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_TREE , Imgproc.CHAIN_APPROX_SIMPLE);

        // Pomocnicze
        Mat ret = scene.clone();
        Imgproc.drawContours(ret, contours, -1, new Scalar(0,255,0), 1);

        // Crop each element
        for(MatOfPoint cont : contours) {

            Rect rect = Imgproc.boundingRect(cont);
            Point corner1 = new Point(rect.x, rect.y);
            Point corner2 = new Point(rect.x+rect.width, rect.y+rect.height);

            Imgproc.rectangle(ret, corner1, corner2, new Scalar(255,255,0));

            Mat croppedElement = new Mat(scene, rect);

            offsetContour(cont, rect.x, rect.y);

            Element el = this.new Element(croppedElement, cont);
            this.elements.add(el);
        }

        /*for(int i = 0; i < this.elements.size(); i++) {
            Highgui.imwrite("EL"+i+".png", this.elements.get(i).img);
        }*/


        return ret;
    }

    private void offsetContour(MatOfPoint cont, int x, int y) {
        for (int i = 0; i < cont.rows(); i++) {
            for(int j = 0; j <cont.cols(); j++) {
                double[] p = cont.get(i, j);
                p[0] -= x;
                p[1] -= y;
                cont.put(i, j, p);
            }
        }
    }
}

