package pt.jigsawsolver;

import java.util.List;

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
	
	private class Edge {
		MatOfPoint curve;
		MatOfPoint curveNorm;
		Element parent;
		Edge connected;
		int no;
		
		Edge(MatOfPoint curve, Element parent, int no) {
			this.parent = parent;
			this.curve = curve;
			this.curveNorm = normalizedCurve(curve);
			this.no = no;
		}
		
		public Point getStartPoint() {
			return new Point(curve.get(0, 0));
		}
		
		private MatOfPoint normalizedCurve(MatOfPoint c) {
			// clone workaround
			Mat norm1 = c.clone();
			MatOfPoint norm = new MatOfPoint();
			norm1.convertTo(norm, CvType.CV_32S);
			
			Point p1 = new Point(norm.get(0, 0));
			Point p2 = new Point(norm.get(norm.rows()-1, 0));
			
			double dist = Math.sqrt(distSq(p1, p2));
			double cos = (p2.x - p1.x) / dist;
			double sin = (p2.y - p1.y) / dist;
			
			for(int i = 0; i<norm.rows(); i++) {
				double[] pt = norm.get(i, 0);
				double x = pt[0];
				double y = pt[1];
				
				pt[0] =  cos*(x-p1.x)+sin*(y-p1.y);
				pt[1] = -sin*(x-p1.x)+cos*(y-p1.y);
				
				norm.put(i, 0, pt);
			}
			return norm;
		}
		
		public boolean isFlat() {
			Rect rect = Imgproc.boundingRect(curveNorm);
			return ((double)rect.height/(double)rect.width) < 0.05;
		}
		
		public void merge(Edge e) {
			this.connected = e;
			e.connected = this;
		}
		
		public double distance(Edge e) {
			if(this.isFlat() || e.isFlat() || this == e)
				return Double.MAX_VALUE;
			
			MatOfPoint temp1 = new MatOfPoint();
			this.curveNorm.copyTo(temp1);

			MatOfPoint temp2 = new MatOfPoint();
			e.curveNorm.copyTo(temp2);
			
			Point end = new Point(temp1.get(temp1.rows()-1, 0));
			for(int i = 0; i < temp2.rows(); i++) {
				double[] t = temp2.get(i, 0);
				double x = -t[0]+end.x;
				double y = -t[1]+end.y;
				
				t[0] = x;
				t[1] = y;
				temp2.put(i, 0, t);
			}
			temp1.push_back(temp2);
			
			return Imgproc.contourArea(temp1);
		}
		
		Edge next() {
			return parent.edges.get((no+1)%4);
		}
		
		Edge prev() {
			return parent.edges.get(no==0? 3 : no-1);
		}
		
		
	}
	
	private class Element {
		int id;
		Mat img;
		List<Edge> edges;
		private boolean corner = false;
		private boolean border = false;
		
		Element(int id, Mat img, MatOfPoint contour) {
			this.id = id;
			this.img = img;
			this.edges = getEdges(contour);
			
			int flat = 0;
			for (Edge e : edges)
				if(e.isFlat()) flat++;
			
			if(flat==1)
				border = true;
			else if(flat==2)
				corner = true;
			
		}
		
		boolean isBorder() {
			return this.border;
		}
		
		boolean isCorner() {
			return this.corner;
		}
		
		Edge[] flatNeighbours() {
			if(!isBorder() && !isCorner())
				return null;
			
			Edge[] ret = new Edge[2];
			
			if(isBorder()) {
				Edge e = edges.get(0);
				while(!e.isFlat())
					e = e.next();
				ret[0] = e.prev();
				ret[1] = e.next();
			} else if(isCorner()) {
				Edge e = edges.get(0);

				while(!e.isFlat() || !e.next().isFlat())
					e = e.next();
				
				ret[0] = e.prev();
				ret[1] = e.next().next();
			}
			
			return ret;
		}
		
		public Mat represent() {
			Mat ret = this.img.clone();
			
			Imgproc.putText(ret, String.valueOf(id), new Point(0,23), Core.FONT_HERSHEY_DUPLEX, 1., Scalar.all(255));
			String type = "Inner";
			if (this.isBorder()) type = "Border";
			if (this.isCorner()) type = "Corner";
			Imgproc.putText(ret, type, new Point(0,48), Core.FONT_HERSHEY_DUPLEX, 1., Scalar.all(255));
			
			for(Edge e : edges) {
				List<MatOfPoint> l = new ArrayList<>(1);
				l.add(e.curve);
				Scalar color = e.isFlat()? new Scalar(0,255,0) : new Scalar(255,0,0);
				Imgproc.polylines(ret, l, false, color, 1);
				Imgproc.drawMarker(ret, e.getStartPoint(), new Scalar(0,0,255), Imgproc.MARKER_TILTED_CROSS, 9, 1, 8);
			}
			
			return ret;
		}
		
		private List<Edge> getEdges(MatOfPoint cont) {
			
			MatOfPoint2f points1 = new MatOfPoint2f();
			MatOfPoint2f points = new MatOfPoint2f();
			cont.convertTo(points1, CvType.CV_32F);
			Imgproc.approxPolyDP(points1, points, 0.15, true);
			int[] cornerIds = getCornerIds(points);

			List<Edge> l = new ArrayList<>(4);
			
			l.add( cut(points, cornerIds[0], cornerIds[1], 0) );
			l.add( cut(points, cornerIds[1], cornerIds[2], 1) );
			l.add( cut(points, cornerIds[2], cornerIds[3], 2) );
			l.add( cut(points, cornerIds[3], cornerIds[0], 3) );

			return l;
		}
		
		private Edge cut(MatOfPoint2f cont, int start, int end, int id) {
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
			return new Edge(ret1, this, id);
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
		
		
	}
	
	private static double distSq(Point a, Point b) {
		return (a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y);
	}
	
	public void loadImage(Mat scene) {
		// Contour extraction
		Mat gray1 = scene.clone();
		Imgproc.cvtColor(scene, gray1, Imgproc.COLOR_RGB2GRAY);
		Mat gray = scene.clone();
		Imgproc.blur(gray1, gray, new Size(2,2));
		
	    List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

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
	
	public void solve() {
		List<Element> corners = new ArrayList<>(4);
		List<Element> borders = new ArrayList<>();
		List<Element> inner = new ArrayList<>();
		
		for (Element e : this.elements) {
			if(e.isCorner())
				corners.add(e);
			else if(e.isBorder())
				borders.add(e);
			else
				inner.add(e);
		}
		
		List<Element> flats = new ArrayList<>(borders);
		flats.addAll(corners);
		
		List<Edge> l = new ArrayList<>();
		List<Edge> r = new ArrayList<>();
		
		for(Element el : flats) {
			Edge[] e = el.flatNeighbours();
			l.add(e[0]);
			r.add(e[1]);
		}
		
		double[][] dist = new double[l.size()][r.size()];
		
		for(int i = 0; i < l.size(); i++) {
			for(int j = 0; j < r.size(); j++) {
				dist[i][j] = l.get(i).distance(r.get(j));
			}
		}
	}
}