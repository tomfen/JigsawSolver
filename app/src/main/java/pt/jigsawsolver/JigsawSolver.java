package pt.jigsawsolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class JigsawSolver {
	
	List<Element> elements = new ArrayList<>();
	
	private class Element {
		int id;
		Mat img;
		List<MatOfPoint> edges;
		
		Element(int id, Mat img, MatOfPoint contour) {
			this.id = id;
			this.img = img;
			this.edges = getEdges(contour);
		}
		
		boolean isBorder() {
			for(MatOfPoint edge : edges) {
				if(isFlat(edge)) return true;
			}
			return false;
		}
		
		boolean isCorner() {
			int flatEdges = 0;
			for(MatOfPoint edge : edges) {
				if(isFlat(edge)) {
					flatEdges++;
					if(flatEdges>=2) return true;
				}
			}
			return false;
		}
		
		public Mat represent() {
			Mat ret = this.img.clone();
			
			Imgproc.putText(ret, String.valueOf(id), new Point(0,23), Core.FONT_HERSHEY_DUPLEX, 1., Scalar.all(255));
			String type = "Inside";
			if (this.isBorder()) type = "Border";
			if (this.isCorner()) type = "Corner";
			Imgproc.putText(ret, type, new Point(0,46), Core.FONT_HERSHEY_DUPLEX, 1., Scalar.all(255));
			
			Imgproc.polylines(ret, edges.subList(0, 1), false, new Scalar(255,0,0), 2, 4, 0);
			Imgproc.polylines(ret, edges.subList(1, 2), false, new Scalar(0,0,255), 2, 4, 0);
			Imgproc.polylines(ret, edges.subList(2, 3), false, new Scalar(0,255,0), 2, 4, 0);
			Imgproc.polylines(ret, edges.subList(3, 4), false, new Scalar(0,255,255), 2, 8, 0);
			
			return ret;
		}
		
		private boolean isFlat(MatOfPoint edge) {
			double area = Imgproc.contourArea(edge);
			Point p1 = new Point(edge.get(0, 0));
			Point p2 = new Point(edge.get(edge.rows()-1, 0));
			double dist = Math.sqrt(distSq(p1, p2));
			return area/dist < 1.;
		}
		
		private List<MatOfPoint> getEdges(MatOfPoint cont) {
			
			MatOfPoint2f points = new MatOfPoint2f();
			cont.convertTo(points, CvType.CV_32F);
			int[] cornerIds = getCornerIds(points);

			List<MatOfPoint> l = new ArrayList<>(4);
			
			l.add( cut(points, cornerIds[0], cornerIds[1]) );
			l.add( cut(points, cornerIds[1], cornerIds[2]) );
			l.add( cut(points, cornerIds[2], cornerIds[3]) );
			l.add( cut(points, cornerIds[3], cornerIds[0]) );
			
			return l;
		}
		
		private MatOfPoint cut(MatOfPoint2f cont, int start, int end) {
			Mat ret;
			if (start < end) {
				ret = cont.submat(start, end+1, 0, 1);
			} else {
				ret = cont.submat(start, cont.rows(), 0, 1);
				Mat app = cont.submat(0, end+1, 0, 1);
				ret.push_back(app);
			}
			
			MatOfPoint ret1 = new MatOfPoint();
			ret.convertTo(ret1, CvType.CV_32S);
			return ret1;
		}
		
		private int[] getCornerIds(MatOfPoint2f cont) {
			int[] corners = new int[4];
			
			RotatedRect rect = Imgproc.minAreaRect(cont);
			Point[] rectPts = new Point[4];
			rect.points(rectPts);
			
			double mindist[] = new double[4];
			Arrays.fill(mindist, Double.MAX_VALUE);

			for(int i = 0; i < cont.rows(); i++) {
				Point p = new Point(cont.get(i, 0));
				for(int j = 0; j < 4; j++) {
					double dist = distSq(rectPts[j], p);
					if(mindist[j] > dist) {
						corners[j] = i;
						mindist[j] = dist;
					}
				}
			}
			
			Arrays.sort(corners);
			return corners;
		}
		
		private double distSq(Point a, Point b) {
			return (a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y);
		}
	}
	
	public void loadImage(Mat scene) {
		// Contour extraction
		Mat gray1 = scene.clone();
		Imgproc.cvtColor(scene, gray1, Imgproc.COLOR_RGB2GRAY);
		Mat gray = scene.clone();
		Imgproc.blur(gray1, gray, new Size(2,2));
		
	    List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_SIMPLE);

	    // Crop each element
		int id = 0;
	    for(MatOfPoint cont : contours) {
	    	
			Rect rect = Imgproc.boundingRect(cont);
			Mat croppedElement = new Mat(scene, rect);
			
			offsetContour(cont, -rect.x, -rect.y);

			Element el = this.new Element(id++, croppedElement, cont);
			this.elements.add(el);
	    }
	    
	    for(int i = 0; i < this.elements.size(); i++) {
	    	Mat img = this.elements.get(i).represent();
	    	Imgcodecs.imwrite("EL\\"+i+".png", img);
	    }
	 }
	
	private void offsetContour(MatOfPoint cont, int x, int y) {
		for (int i = 0; i < cont.rows(); i++) {
			for(int j = 0; j <cont.cols(); j++) {
				double[] p = cont.get(i, j);
				p[0] += x;
				p[1] += y;
				cont.put(i, j, p);
			}
		}
	}
	
}
