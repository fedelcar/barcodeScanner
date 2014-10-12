package com.lamppost.barcode;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacv.JavaCV;

import java.util.LinkedList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_imgproc.*;

public class StolenRectangleFinder
{
    public static List<Rect> findRectangles(Mat image, String tempDirPath) {
        List<Rect> result = new LinkedList<Rect>();

        Mat pyr = new Mat();
        Mat timg = new Mat();
        Mat gray0 = new Mat(image.size(), CvType.CV_8U);
        Mat gray = new Mat();

        // down-scale and upscale the image to filter out the noise
        pyrDown(image, pyr, new Size((image.cols() + 1) / 2, (image.rows() + 1) / 2), BORDER_DEFAULT);
        pyrUp(pyr, timg, image.size(), BORDER_DEFAULT);
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

                    Imgproc.threshold(gray0, gray, (l + 1) * 255 / N, 255,
                            Imgproc.THRESH_BINARY);
                }

                Mat hierarchy = new Mat();
                // find contours and store them all as a list
                Imgproc.findContours(gray, contours, hierarchy,
                        Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                MatOfPoint2f approx = new MatOfPoint2f();

                Iterator<MatOfPoint> each = contours.iterator();
                while (each.hasNext()) {
                    MatOfPoint p = each.next();
                    MatOfPoint2f wrapper = new MatOfPoint2f();
                    wrapper.fromArray(p.toArray());
                    Imgproc.approxPolyDP(wrapper, approx,
                            Imgproc.arcLength(wrapper, true) * 0.02, true);
                    MatOfPoint approxMat = new MatOfPoint();
                    approxMat.fromArray(approx.toArray());
                    if (approx.total() == 4
                            && Math.abs(Imgproc.contourArea(approx)) > 50
                            && Imgproc.isContourConvex(approxMat)) {
                        double maxCosine = 0;
                        Point[] pArray = approx.toArray();
                        for (int j = 2; j <= 5; j++) {
                            // find the maximum cosine of the angle between
                            // joint edges
                            double cosine = Math.abs(angle(pArray[j % 4],
                                    pArray[j - 2], pArray[(j - 1) % 4]));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        // if cosines of all angles are small
                        // (all angles are ~90 degree) then write quandrange
                        // vertices to resultant sequence
                        if (maxCosine < 0.1) {
                            Rect r = Imgproc.boundingRect(approxMat);
                            if (r.width >= minWidthOfDetectedRect
                                    && r.height >= minHieghtOfDetectedRect) {
                                if (!added(result, r)) {
                                    result.add(r);

                                }
                            }
                        }
                    }
                }
            }
        }
        drawRects(image.clone(), result, tempDirPath, "Detected_rectangles.png");
        return result;
    }
}
