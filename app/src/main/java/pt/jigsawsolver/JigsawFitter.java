package pt.jigsawsolver;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
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
    public static Mat find(Mat img_object1, Mat img_scene1) {
        Mat img_object = new Mat();
        Mat img_scene = new Mat();

        Imgproc.cvtColor(img_object1, img_object, Imgproc.COLOR_RGBA2RGB, 1);
        Imgproc.cvtColor(img_scene1, img_scene, Imgproc.COLOR_RGBA2RGB, 1);

        FeatureDetector detector = FeatureDetector.create(FeatureDetector.DYNAMIC_ORB); //SURF i SIFT nie działa

        MatOfKeyPoint keypoints_object = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_scene  = new MatOfKeyPoint();

        detector.detect(img_object, keypoints_object);
        detector.detect(img_scene, keypoints_scene);

        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        Mat descriptor_object = new Mat();
        Mat descriptor_scene = new Mat();

        extractor.compute(img_object, keypoints_object, descriptor_object);
        extractor.compute(img_scene, keypoints_scene, descriptor_scene);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matches = new MatOfDMatch();

        matcher.match(descriptor_object, descriptor_scene, matches);
        List<DMatch> matchesList = matches.toList();

        Double max_dist = 0.0;
        Double min_dist = 100.0;

        for(int i = 0; i < descriptor_object.rows(); i++){
            Double dist = (double) matchesList.get(i).distance;
            if(dist < min_dist) min_dist = dist;
            if(dist > max_dist) max_dist = dist;
        }

        System.out.println("-- Max dist : " + max_dist);
        System.out.println("-- Min dist : " + min_dist);

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        MatOfDMatch gm = new MatOfDMatch();

        for(int i = 0; i < descriptor_object.rows(); i++){
            if(matchesList.get(i).distance <= 3*min_dist){
                good_matches.addLast(matchesList.get(i));
            }
        }

        gm.fromList(good_matches);

        Mat img_matches = new Mat();

        Features2d.drawMatches(
                img_object,
                keypoints_object,
                img_scene,
                keypoints_scene,
                gm,
                img_matches,
                Scalar.all(-1),
                Scalar.all(-1),
                new MatOfByte(),
                Features2d.NOT_DRAW_SINGLE_POINTS | Features2d.DRAW_RICH_KEYPOINTS);

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
            Mat homography = Calib3d.findHomography(obj, scene);

            Mat obj_corners = new Mat(4,1,CvType.CV_32FC2);
            Mat scene_corners = new Mat(4,1, CvType.CV_32FC2);

            obj_corners.put(0, 0, new double[] {0,0});
            obj_corners.put(1, 0, new double[] {img_object.cols(),0});
            obj_corners.put(2, 0, new double[] {img_object.cols(),img_object.rows()});
            obj_corners.put(3, 0, new double[] {0,img_object.rows()});


            Core.perspectiveTransform(obj_corners, scene_corners, homography);

            //offset the rectangle to the template
            int offset = img_object.cols();
            for (int i = 0; i < 4; i++) {
                double[] pt = scene_corners.get(i, 0);
                pt[0] += offset;
                scene_corners.put(i, 0, pt);
            }

            Scalar color = new Scalar(0, 255, 0);

            Imgproc.line(img_matches, new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)), color, 4);
            Imgproc.line(img_matches, new Point(scene_corners.get(1,0)), new Point(scene_corners.get(2,0)), color, 4);
            Imgproc.line(img_matches, new Point(scene_corners.get(2,0)), new Point(scene_corners.get(3,0)), color, 4);
            Imgproc.line(img_matches, new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)), color, 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return img_matches;
    }
}