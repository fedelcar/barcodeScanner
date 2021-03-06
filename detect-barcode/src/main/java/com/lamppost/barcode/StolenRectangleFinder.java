package com.lamppost.barcode;

import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.security.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class StolenRectangleFinder
{
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args)
    {
        long startTime = System.currentTimeMillis();

        String tempDirPath = "/Users/santiagoramirez/Downloads/";
        Mat image = Highgui.imread(tempDirPath + "detect-simple-shapes-feat-img1.png", Highgui.CV_LOAD_IMAGE_COLOR);

        Highgui.imwrite(tempDirPath + "output.png", image);

        List<MatOfPoint> result = new LinkedList<MatOfPoint>();

        Mat pyr = new Mat();
        Mat timg = new Mat();
        Mat gray0 = new Mat(image.size(), CvType.CV_8U);
        Mat gray = new Mat();

        // down-scale and upscale the image to filter out the noise
        Imgproc.pyrDown(image, pyr, new Size((image.cols() + 1) / 2, (image.rows() + 1) / 2));
        Imgproc.pyrUp(pyr, timg, image.size());
        List<MatOfPoint> contours = new LinkedList<MatOfPoint>();

        // find squares in every color plane of the image
        for (int c = 0; c < 3; c++) {
            int chArr[] = { c, 0 };
            MatOfInt ch = new MatOfInt();
            ch.fromArray(chArr);
            List<Mat> src = new LinkedList<Mat>();
            src.add(timg);
            List<Mat> dst = new LinkedList<Mat>();
            dst.add(gray0);
            Core.mixChannels(src, dst, ch);
            int N = 4;
            double threshLow = 1;
            double threshHigh = 1;

            // try several threshold levels
            for (int l = 0; l < N; l++) {
                // hack: use Canny instead of zero threshold level.
                // Canny helps to catch squares with gradient shading
                if (l == 0) {
                    // apply Canny. Take the upper threshold from slider
                    // and set the lower to 0 (which forces edges merging)
                    Imgproc.Canny(gray0, gray, threshLow, threshHigh);
                    // dilate canny output to remove potential
                    // holes between edge segments
                    Imgproc.dilate(gray, gray, new Mat());
                } else {
                    // apply threshold if l!=0:
                    // tgray(x,y) = gray(x,y) < (l+1)*255/N ? 255 : 0

                    Imgproc.threshold(gray0, gray, (l + 1) * 255 / N, 255, Imgproc.THRESH_BINARY);
                }

                Mat hierarchy = new Mat();
                // find contours and store them all as a list
                Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                MatOfPoint2f approx = new MatOfPoint2f();

                Iterator<MatOfPoint> each = contours.iterator();
                while (each.hasNext()) {
                    MatOfPoint p = each.next();
                    MatOfPoint2f wrapper = new MatOfPoint2f();
                    wrapper.fromArray(p.toArray());
                    Imgproc.approxPolyDP(wrapper, approx, Imgproc.arcLength(wrapper, true) * 0.02, true);
                    MatOfPoint approxMat = new MatOfPoint();
                    approxMat.fromArray(approx.toArray());
                    if (approx.total() == 4 && Math.abs(Imgproc.contourArea(approx)) > 50 && Imgproc.isContourConvex(approxMat))
                    {
                        double maxCosine = 0;
                        Point[] pArray = approx.toArray();
                        for (int j = 2; j <= 5; j++) {
                            // find the maximum cosine of the angle between
                            // joint edges
                            double cosine = Math.abs(angle(pArray[j % 4], pArray[j - 2], pArray[(j - 1) % 4]));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        // if cosines of all angles are small
                        // (all angles are ~90 degree) then write quandrange
                        // vertices to resultant sequence

                        // Check if we need this shit

                        int minWidthOfDetectedRect = 100;
                        int minHeightOfDetectedRect = 100;

                        if (maxCosine < 0.1) {
                            Rect r = Imgproc.boundingRect(approxMat);
                            if (approxMat.size().width >= minWidthOfDetectedRect && approxMat.size().height >= minHeightOfDetectedRect)
                            {
                                result.add(approxMat);
                            }
                        }
                    }
                }
            }
        }
        drawRects(image.clone(), result, tempDirPath, "Detected_rectangles.png");
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println(elapsedTime);

        //return result;
    }

    private static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;

        return (dx1*dx2 + dy1*dy2) / Math.sqrt((dx1*dx1 + dy1*dy1) * (dx2*dx2 + dy2*dy2) + 1e-10);
    }

    // Maybe we can adapt this shit
    private static void drawRects(Mat img, List<MatOfPoint> squares, String tempDirPath, String outputImage)
    {
        for (MatOfPoint square : squares)
        {
            Point[] asArray = square.toArray();

            Core.polylines(img, squares, true, new Scalar(2000));
        }

        Highgui.imwrite(tempDirPath + outputImage, img);
    }
}
