package com.lamppost.barcode;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.pdf417.PDF417Reader;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BarcodeDecoder
{
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    /**
     * Aspect ratio of the barcode to analyze. In this case for Argentinian DNI.
     */
    private static final float ASPECT_RATIO = 3.23f;

    /**
     * Threshold for the angle between the sides of the found rectangles. Affects how perfect the rectangles need to be
     * to be found.
     */
    private static final float COSINE_THRESHOLD = 0.1f;

    private static final String FILEPATH = "/Users/Federico/Downloads/PackDeFotos/stream.jpg";
    private static final String FILESPATH = "/Users/Federico/Downloads/PackDeFotos/stream.jpg";

    static int iterations = 0;

    public static void main(String[] args)
    {
        long startMillis = 0;
        long executionTime = 0;

        try
        {
            startMillis = System.currentTimeMillis();

            Webcam webcam = Webcam.getDefault();
            webcam.close();
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcam.open();

            do {

                try {

                    //ImageInputStream stream = ImageIO.createImageInputStream(webcam.getImage());

                    BufferedImage stream = webcam.getImage();
                    ImageIO.write(stream, "JPG", new File(FILEPATH));

                    if (webcam.getImage() != null)
                        System.out.println(System.currentTimeMillis() + " - Imagen leida.");

                    File sourceFile = new File(FILEPATH);
                    File outputDir = new File(sourceFile.getParent() + File.separator + "Detected_Rectangles");
                    for (File outputFile : outputDir.listFiles()) {
                        outputFile.delete();
                    }

                    String response = decodeBarcode(stream);

                    if (response != null) {
                        System.out.println(System.currentTimeMillis() + " - " + response + " (ZXing)");
                    } else {
                        findRectangles(stream);
                        for (File outputFile : outputDir.listFiles()) {
                            response = decodeBarcode(stream);
                            if (response != null) {
                                System.out.println(System.currentTimeMillis() + " - " + response);
                                break;
                            }
                        }
                    }

                    stream.flush();
                }
                catch (CvException e) {
                    e.printStackTrace();
                }
            }
            while (1==1);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        executionTime = System.currentTimeMillis() - startMillis;
        System.out.println(System.currentTimeMillis() + ": end (executionTime: " + executionTime + " millis - decodeBarcode: " + iterations);
    }

    public static String decodeBarcode(BufferedImage image) throws IOException
    {

        try
        {
            //System.out.println(System.currentTimeMillis() + ": decodeBarcode");
            iterations++;
            //BufferedImage bufferedImage = ImageIO.read(image);

            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            PDF417Reader pdf417Reader = new PDF417Reader();
            return pdf417Reader.decode(binaryBitmap).toString();
        }
        catch (NullPointerException | FormatException | NotFoundException | ChecksumException e)
        {
            //e.printStackTrace();
            //throw new IOException();
        }
        return null;
    }

    private static Mat getMatFromBufferedImage(BufferedImage bufferedImage)
    {
        BufferedImage image = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(data);

        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, byteBuffer.array());
        return mat;
    }

    /**
     * Finds all rectangles in an image and writes them to disk
     * @param bufferedImage The buffered image
     * @throws IOException
     */
    public static void findRectangles(BufferedImage bufferedImage) throws IOException
    {
        /*File imageFile = new File(imagePath);
        if (!imageFile.exists())
        {
            throw new IOException();
        }
        String tempDirPath = imageFile.getParent();

        Mat image = Highgui.imread(imageFile.getPath(), Highgui.CV_LOAD_IMAGE_COLOR);
*/

        Mat image = getMatFromBufferedImage(bufferedImage);

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
                            double cosine = Math.abs(angle(pArray[(j - 1) % 4], pArray[j % 4], pArray[j - 2]));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        // if cosines of all angles are small
                        // (all angles are ~90 degree) then write quandrange
                        // vertices to resultant sequence

                        if (maxCosine < COSINE_THRESHOLD) {
                            Rect r = Imgproc.boundingRect(approxMat);
                            float ratio = (float) r.height/ (float) r.width;
                            if (ratio > ASPECT_RATIO - 0.5f && ratio < ASPECT_RATIO + 0.5f || ratio > 1f/ASPECT_RATIO - 0.5f && ratio < 1f/ASPECT_RATIO + 0.5f)
                            {
                                result.add(approxMat);
                            }
                        }
                    }
                }
            }
        }
        writeFoundRectangles(image.clone(), result, FILESPATH);
    }

    /**
     * Finds the angle between two segments given three points
     * @param pt0 The point of intersection between the two segments
     * @param pt1 Point belonging to first segment
     * @param pt2 Point belonging to second segment
     * @return
     **/
    private static double angle(Point pt0,Point pt1, Point pt2)
    {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;

        return (dx1*dx2 + dy1*dy2) / Math.sqrt((dx1*dx1 + dy1*dy1) * (dx2*dx2 + dy2*dy2) + 1e-10);
    }

    /**
     * Writes found rectangles to individual files
     * @param img {@link org.opencv.core.Mat} representing the original image
     * @param squares Location of the found rectangles in the image
     * @param imageDirPath Location directory of the original file
     */
    private static void writeFoundRectangles(Mat img, List<MatOfPoint> squares, String imageDirPath)
    {
        int i = 0;
        File detectedRectangleDir = new File(imageDirPath + File.separator + "Detected_Rectangles");
        if (!detectedRectangleDir.exists())
        {
            detectedRectangleDir.mkdir();
        }

        for (MatOfPoint square : squares)
        {

            Point bottom =  null;
            Point left = null;
            Point right = null;
            Point top = null;
            for (Point point : square.toArray())
            {
                if (top == null || point.y < top.y)
                {
                    top = point;
                }
                if (bottom == null || point.y > bottom.y)
                {
                    bottom = point;
                }
                if (left == null || point.x < left.x)
                {
                    left = point;
                }
                if (right == null || point.x > right.x)
                {
                    right = point;
                }
            }

            // Get differences top left minus bottom left

            //System.out.println(String.format("(%f, %f); (%f, %f)", top.x, top.y, left.x, left.y));

            // Get rotation in degrees
            double rotation = Math.atan((left.x - top.x) / (top.y - left.y));
            //System.out.println(rotation*180/Math.PI);

            Rect rect = Imgproc.boundingRect(square);

            int dif1 = Math.min(rect.x, 40);
            rect.x -= dif1;
            int dif2 = Math.min(img.cols() - rect.width, 40);
            rect.width += dif2;

            int dif3 = Math.min(rect.y,40);
            rect.y-=dif3;
            int dif4 = Math.min(img.rows()-rect.height,40);
            rect.height+=dif4;

            Mat croppedImage = new Mat(img, rect);

            try
            {
                Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(croppedImage.cols()/2, croppedImage.rows()/2),rotation*180/Math.PI, 1);

                Mat rotatedImage = new Mat();
                Imgproc.warpAffine(croppedImage, rotatedImage, rotationMatrix, croppedImage.size());

                Highgui.imwrite(imageDirPath + File.separator + "Detected_Rectangles" + File.separator + "Rotated_Detected_Rectangle_" + i++ + ".png", rotatedImage);
                Highgui.imwrite(imageDirPath + File.separator + "Detected_Rectangles" + File.separator + "Detected_Rectangle_" + i++ + ".png", croppedImage);
            }


            catch (IllegalArgumentException e)
            {
                System.out.println(e);
            }

        }
    }
}
