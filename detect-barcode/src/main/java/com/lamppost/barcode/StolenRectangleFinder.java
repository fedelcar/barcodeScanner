package com.lamppost.barcode;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class StolenRectangleFinder
{
    public static void main(String[] args) {

        Mat image = new Mat();
        String tempDirPath = "";

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        List<Rect> result = new LinkedList<Rect>();

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
                            double cosine = Math.abs(angle(pArray[j % 4], pArray[j - 2], pArray[(j - 1) % 4]));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        // if cosines of all angles are small
                        // (all angles are ~90 degree) then write quandrange
                        // vertices to resultant sequence

                        // Check if we need this shit

                        /*if (maxCosine < 0.1) {
                            Rect r = Imgproc.boundingRect(approxMat);
                            if (r.width >= minWidthOfDetectedRect && r.height >= minHieghtOfDetectedRect)
                            {
                                if (!added(result, r))
                                {
                                    result.add(r);
                                }
                            }
                        } */
                    }
                }
            }
        }
        //drawRects(image.clone(), result, tempDirPath, "Detected_rectangles.png");
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
    /*void drawSquares(IplImage img, CvSeq squares) {

        //      Java translation: Here the code is somewhat different from the C version.
        //      I was unable to get straight forward CvPoint[] arrays
        //      working with "reader" and the "CV_READ_SEQ_ELEM".

//        CvSeqReader reader = new CvSeqReader();

        IplImage cpy = cvCloneImage(img);
        int i = 0;

        // Used by attempt 3
        // Create a "super"-slice, consisting of the entire sequence of squares
        CvSlice slice = new CvSlice(squares);

        // initialize reader of the sequence
//        cvStartReadSeq(squares, reader, 0);

        // read 4 sequence elements at a time (all vertices of a square)
        for(i = 0; i < squares.total(); i += 4) {     int count[] = new int[]{4};
            // Attempt 3:
            // This works, may be the "cleanest" solution, does not use the "reader"
            CvPoint rect = new CvPoint(4);
            int count[] = new int[]{4};
            // get the 4 corner slice from the "super"-slice
            cvCvtSeqToArray(squares, rect, slice.start_index(i).end_index(i + 4));

            // draw the square as a closed polyline
            // Java translation: gotcha (re-)setting the opening "position" of the CvPoint sequence thing
            cvPolyLine(cpy, rect.position(0), count, 1, 1, CV_RGB(0,255,0), 3, CV_AA, 0);
        }

        // show the resultant image
        // cvShowImage(wndname, cpy);
        canvas.showImage(cpy);
        cvReleaseImage(cpy);
    }*/
}
