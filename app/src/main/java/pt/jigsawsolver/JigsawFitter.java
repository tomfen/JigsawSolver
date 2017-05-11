package pt.jigsawsolver;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.*;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

public class JigsawFitter {

    private static final int FEATURE_DETECTOR = FeatureDetector.BRISK;
    private static final int DESCRIPTOR_EXTRACTOR = DescriptorExtractor.BRISK;
    private static final int FEATURE_MATCHER = DescriptorMatcher.BRUTEFORCE_HAMMING;
    private static final double REPROJECT_ERROR = 8.;

    private static final Scalar OUTLINE_COLOR = new Scalar(0, 255, 0);
    private static final int OULINE_THICKNESS = 6;

    public static Mat find(Mat img_object1, Mat img_scene1) {

        Mat img_object = new Mat();
        Mat img_scene = new Mat();

        Imgproc.cvtColor(img_object1, img_object, Imgproc.COLOR_RGBA2RGB, 1);
        Imgproc.cvtColor(img_scene1, img_scene, Imgproc.COLOR_RGBA2RGB, 1);

        FeatureDetector detector = FeatureDetector.create(FEATURE_DETECTOR); //SURF i SIFT nie działa

        MatOfKeyPoint keypoints_object = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_scene  = new MatOfKeyPoint();

        detector.detect(img_object, keypoints_object);
        detector.detect(img_scene, keypoints_scene);

        DescriptorExtractor extractor = DescriptorExtractor.create(DESCRIPTOR_EXTRACTOR);

        Mat descriptor_object = new Mat();
        Mat descriptor_scene = new Mat();

        extractor.compute(img_object, keypoints_object, descriptor_object);
        extractor.compute(img_scene, keypoints_scene, descriptor_scene);

        DescriptorMatcher matcher = DescriptorMatcher.create(FEATURE_MATCHER);

        MatOfDMatch matches = new MatOfDMatch();

        matcher.match(descriptor_object, descriptor_scene, matches);

        List<DMatch> matchesList = matches.toList();

        Double max_dist = 0.0;
        Double min_dist = 100.0;

        for (int i = 0; i < matchesList.size(); i++)
        {
            Double dist = (double) matchesList.get(i).distance;
            if (dist < min_dist && dist != 0) min_dist = dist;
            if (dist > max_dist) max_dist = dist;
        }

        System.out.println("-- Max dist : " + max_dist);
        System.out.println("-- Min dist : " + min_dist);

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        MatOfDMatch matchesFiltered = new MatOfDMatch();

        if(min_dist > 50 )
        {
            System.out.println("No match found");
        }

        double threshold = 3 * min_dist;
        double threshold2 = 2 * min_dist;

        if (threshold > 75)
        {
            threshold  = 75;
        }
        else if (threshold2 >= max_dist)
        {
            threshold = min_dist * 1.1;
        }
        else if (threshold >= max_dist)
        {
            threshold = threshold2 * 1.4;
        }

        System.out.println("Threshold : "+threshold);

        for (int i = 0; i < matchesList.size(); i++)
        {
            Double dist = (double) matchesList.get(i).distance;

            if (dist < threshold)
            {
                good_matches.add(matches.toList().get(i));
            }
        }

        matchesFiltered.fromList(good_matches);

        System.out.println("matchesFiltered.size() : " + matchesFiltered.size());

        if(matchesFiltered.rows() >= 4)
        {
            System.out.println("match found");

            LinkedList<Point> objList = new LinkedList<Point>();
            LinkedList<Point> sceneList = new LinkedList<Point>();

            List<KeyPoint> keypoints_objectList = keypoints_object.toList();
            List<KeyPoint> keypoints_sceneList = keypoints_scene.toList();

            for(int i = 0; i<good_matches.size(); i++){
                objList.addLast(keypoints_objectList.get(good_matches.get(i).queryIdx).pt);
                sceneList.addLast(keypoints_sceneList.get(good_matches.get(i).trainIdx).pt);
            }

            MatOfPoint2f obj = new MatOfPoint2f();
            obj.fromList(objList);

            MatOfPoint2f scene = new MatOfPoint2f();
            scene.fromList(sceneList);

            try {
                Mat homography = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, REPROJECT_ERROR);

                Mat obj_corners = new Mat(4,1,CvType.CV_32FC2);
                Mat scene_corners = new Mat(4,1, CvType.CV_32FC2);

                obj_corners.put(0, 0, new double[] {0,0});
                obj_corners.put(1, 0, new double[] {img_object.cols(),0});
                obj_corners.put(2, 0, new double[] {img_object.cols(),img_object.rows()});
                obj_corners.put(3, 0, new double[] {0,img_object.rows()});


                Core.perspectiveTransform(obj_corners, scene_corners, homography);

                Imgproc.line(img_scene, new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)), OUTLINE_COLOR, OULINE_THICKNESS);
                Imgproc.line(img_scene, new Point(scene_corners.get(1,0)), new Point(scene_corners.get(2,0)), OUTLINE_COLOR, OULINE_THICKNESS);
                Imgproc.line(img_scene, new Point(scene_corners.get(2,0)), new Point(scene_corners.get(3,0)), OUTLINE_COLOR, OULINE_THICKNESS);
                Imgproc.line(img_scene, new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)), OUTLINE_COLOR, OULINE_THICKNESS);

            } catch (Exception e) {
                return null;
            }

        }
        else
        {
            System.out.println("match not found");
        }


        return img_scene;
    }
}
