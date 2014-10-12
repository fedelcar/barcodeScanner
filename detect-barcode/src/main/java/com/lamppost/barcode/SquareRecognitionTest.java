package com.lamppost.barcode;

public class SquareRecognitionTest
{
   /* private String imageDir = "3";
    private int thresh = 50, N = 11;

    // Returns sequence of squares detected on the image.
    // The sequence is stored in the specified memory storage.
    private void findSquares(IplImage image, MatVector squares)
    {
        IplImage pyr = new IplImage();
        IplImage newImage = new IplImage();
        IplImage thirdImage = new IplImage();
        IplImage gray = new IplImage();

        Point pointForDilation = new Point(-1, -1);

        // Down-scale and upscale the image to filter out the noise.
        pyrDown(new Mat(image), new Mat(pyr), new Size(image.cvSize().width()/2, image.cvSize().height()/2), BORDER_DEFAULT);
        pyrUp(new Mat(pyr), new Mat(newImage), new Mat(image).size(), BORDER_DEFAULT);
        MatVector contours = new MatVector();

        // try several threshold levels
        for (int l = 0; l < N; l++)
        {
            // hack: use Canny instead of zero threshold level.
            // Canny helps to catch squares with gradient shading
            if (l == 0)
            {
                // Apply Canny. Take the upper threshold from slider
                // and set the lower to 0 (which forces edges merging).
                cvCanny(newImage, thirdImage, 0, thresh, 5);

                // Dilate canny output to remove potential
                // holes between edge segments.
                dilate(new Mat(thirdImage), new Mat(thirdImage), new Mat());
            } else {
                // apply threshold if l!=0:
                //gray = gray0 >= (l+1)*255/N;
            };

            // find contours and store them all as a list
            findContours(new Mat(thirdImage), contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

            Mat approx = new Mat();

            // test each contour
            for (int i = 0; i < contours.size(); i++)
            {
                // approximate contour with accuracy proportional
                // to the contour perimeter
                approxPolyDP(contours.get(i), approx, arcLength(contours.get(i), true)*0.02, true);

                // square contours should have 4 vertices after approximation
                // relatively large area (to filter out noisy contours)
                // and be convex.
                // Note: absolute value of an area is used because
                // area may be positive or negative - in accordance with the
                // contour orientation
                if( approx.size().height() * approx.size().width() == 4
                        && fabs(contourArea(Mat(approx))) > 1000
                        && isContourConvex(Mat(approx)) )
                {
                    double maxCosine = 0;

                    for( int j = 2; j < 5; j++ )
                    {
                        // find the maximum cosine of the angle between joint edges
                        double cosine = Math.abs(angle(approx[j%4], approx[j-2], approx[j-1]));
                        maxCosine = MAX(maxCosine, cosine);
                    }

                    // if cosines of all angles are small
                    // (all angles are ~90 degree) then write quandrange
                    // vertices to resultant sequence
                    if( maxCosine < 0.3 )
                        squares.push_back(approx);
                }
            }
        }
    };

    public int main()
    {
        // Loads the image. 0 = greyscale.
        IplImage image = cvLoadImage(imageDir, 0);

        // Print error if image is empty
        //if (image.empty())
        //{
        //};

        findSquares(image, squares);
        drawSquares(image, squares);

        return 0;
    };*/
}
