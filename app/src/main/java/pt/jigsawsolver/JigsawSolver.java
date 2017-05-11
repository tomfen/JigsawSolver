package pt.jigsawsolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import org.opencv.imgproc.Moments;

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
		
		public Point getEndPoint() {
			return new Point(curve.get(curve.rows()-1, 0));
		}
		
		public Point getMidPoint() {
			Point p1 = getStartPoint();
			Point p2 = getEndPoint();
			return new Point((p1.x +p2.x)/2, (p1.y+p2.y)/2);
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
			HashSet<Element> set = e.parent.connected;
			this.parent.connected.addAll(set);
			for (Element el : set) {
				el.connected = this.parent.connected;
			}
			
			this.connected = e;
			e.connected = this;
		}
		
		public double distance(Edge e) {
			if(this.isFlat() || e.isFlat() || this.parent == e.parent)
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
		
		Edge opposite() {
			return parent.edges.get((no+2)%4);
		}
		
		
	}
	
	private class Element {
		int id;
		Mat img;
		List<Edge> edges;
		double tilt;
		int rotate = -1;
		Point position;
		HashSet<Element> connected;
		private boolean corner = false;
		private boolean border = false;
		private MatOfPoint contour;
		
		Element(int id, Mat img, MatOfPoint contour) {
			this.id = id;
			this.img = img;
			this.contour = contour;
			this.edges = getEdges(contour);
			this.connected = new HashSet<>();
			this.connected.add(this);
			
			int flat = 0;
			for (Edge e : edges)
				if(e.isFlat()) flat++;
			
			if(flat==1)
				border = true;
			else if(flat==2)
				corner = true;
			
			tilt = getAngle(edges.get(2).getMidPoint(), edges.get(0).getMidPoint());
		}
		
		private double getAngle(Point origin, Point target) {
		    double angle = Math.toDegrees(Math.atan2(target.y - origin.y, target.x - origin.x));
		    
		    if(angle < 0)
		    	angle += 360;
		    
		    return angle;
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
				ret[1] = e.opposite();
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
				if(e.connected != null)
					Imgproc.putText(ret, String.valueOf(e.connected.parent.id), e.getMidPoint(), Core.FONT_HERSHEY_DUPLEX, .5, Scalar.all(255));
			}
			
			Imgproc.line(ret, getCenter(), edges.get(0).getMidPoint(), new Scalar(255,255,0));
			Imgproc.putText(ret, String.valueOf(rotate), new Point(0,70), Core.FONT_HERSHEY_DUPLEX, 1., Scalar.all(255));

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

		public Point getCenter() {
			Moments m = Imgproc.moments(contour);
			return new Point(m.m10/m.m00,m.m01/m.m00);
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
	    
	    
	 }
	
	public void saveElements(String dir) {
		for(int i = 0; i < this.elements.size(); i++) {
	    	Mat img = this.elements.get(i).represent();
	    	Imgcodecs.imwrite(dir+"\\"+i+".png", img);
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
		List<Element> borders = new ArrayList<>();
		
		for (Element e : this.elements) {
			if(e.isCorner() || e.isBorder())
				borders.add(e);
		}
		
		solveBorder(borders);
	}
	
	private void solveBorder(List<Element> elements) {
		Map<Edge, Integer> l = new HashMap<>();
		Map<Edge, Integer> r = new HashMap<>();
		Edge[] el = new Edge[elements.size()];
		Edge[] er = new Edge[elements.size()];
		double[][] dist = new double[elements.size()][elements.size()];
		
		int k = 0;
		for(Element e : elements) {
			Edge[] ed = e.flatNeighbours();
			l.put(ed[0], k);
			r.put(ed[1], k);
			el[k] = ed[0];
			er[k] = ed[1];
			k++;
		}
		
		for(int i = 0; i < l.size(); i++) {
			for(int j = 0; j < r.size(); j++) {
				dist[i][j] = el[i].distance(er[j]);
			}
		}
		while(!l.isEmpty()) {
			int[] match = bestMatch(l, r, dist, el, er);
			
			Edge e1 = el[match[0]];
			Edge e2 = er[match[1]];
			e1.merge(e2);
			
			l.remove(e1);
			r.remove(e2);
		}
	}
	
	private int[] bestMatch(Map<Edge, Integer> l, Map<Edge, Integer> r, double[][] dist, Edge[] el, Edge[] er) {
		int[] ret = new int[2];
		double min = Double.MAX_VALUE;
		
		for(Integer i : l.values()) {
			for(Integer j : r.values()) {
				double v = Double.MAX_VALUE;
				if(el[i].parent.connected != er[j].parent.connected);
					v = dist[i][j];
				if(v < min) {
					min = v;
					ret[0] = i;
					ret[1] = j;
				}
			}
		}
		return ret;
	}

	public void scramble() {
		for(Element el : this.elements) {
			for(Edge ed : el.edges) {
				ed.connected = null;
			}
		}
		el.connected = new HashSet<>();
		el.connected.add(el);
	}
	
	public List<Element> getCorners() {
		List<Element> ret = new ArrayList<>();
		for(Element el : this.elements) {
			if(el.isCorner())
				ret.add(el);
		}
		return ret;
	}
	
	public Mat getSolution() {
		Element anchor = this.elements.get(0);
		
		computePosConnected(anchor, 0, 0, 0, new HashSet<Element>());
		
		Point min = new Point(0,0);
		Point max = new Point(0,0);
		
		for(Element el : elements) {
			if(el.position != null) {
				double x = el.position.x;
				double y = el.position.y;
				if(x<min.x) min.x=x;
				if(x>max.x) max.x=x;
				if(y<min.y) min.y=y;
				if(y>max.y) max.y=y;
			}
		}
		
		int pad = 400;
		
		Size size = new Size((max.x-min.x)*pad+pad, (max.y-min.y)*pad+pad);
		
		Mat canvas = Mat.zeros(size, CvType.CV_8UC3);
		
		for(Element el : elements) {
			if(el.position != null) {
				Mat adjusted = new Mat();
				int maxDim = el.img.cols() > el.img.rows()? el.img.cols(): el.img.rows();
				Mat M = Imgproc.getRotationMatrix2D(el.getCenter(), -(el.rotate+1)*90+el.tilt, 1.);
				Imgproc.warpAffine(el.img, adjusted, M, new Size(maxDim,maxDim));
				
				Rect roi = new Rect((int)(-min.x+el.position.x)*pad, (int)(-min.y+el.position.y)*pad, adjusted.cols(), adjusted.rows());
				adjusted.copyTo(canvas.submat(roi));
			}
		}
		
		return canvas;
	}
	
	public void computePosConnected(Element e, int x, int y, int r, HashSet<Element> visited) {
		if(visited.contains(e)) return;
		visited.add(e);
		e.position = new Point(x,y);
		
		e.rotate = r;

		for(Edge ed : e.edges) {
			int x1=0, y1=0, r1=0;
			if(ed.connected != null) {
				int dir = (4 + ed.no - r + 2) % 4;
				r1 = (4+ ed.connected.no - dir) % 4;
				switch(dir) {
					case 0: x1=x; y1=y-1; break;
					case 1: x1=x-1; y1=y; break;
					case 2: x1=x; y1=y+1; break;
					case 3: x1=x+1; y1=y; break;
				}
				computePosConnected(ed.connected.parent, x1, y1, r1, visited);
			}
		}
	}
}

