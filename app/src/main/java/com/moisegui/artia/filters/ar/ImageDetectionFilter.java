package com.moisegui.artia.filters.ar;


import android.content.Context;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.moisegui.artia.MotifContrat;
import com.moisegui.artia.MotifDbHelper;
import com.moisegui.artia.data.model.Motif;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ImageDetectionFilter implements ARFilter {
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    private static final String TAG = "HOPE";
    // The reference image (this detector's target).
    private Mat mReferenceImage;
    List<Mat> mReferencesImage = new ArrayList<Mat>();
    List<Motif> motifList = new ArrayList<>();
    List<Mat> dbReferencesImages = new ArrayList<Mat>();
    // Features of the reference image.
    private final MatOfKeyPoint mReferenceKeypoints =
            new MatOfKeyPoint();
    List<MatOfKeyPoint> mReferencesKeypoints = new ArrayList<MatOfKeyPoint>();

    // Descriptors of the reference image's features.
    private final Mat mReferenceDescriptors = new Mat();
    List<Mat> mReferencesDescriptors = new ArrayList<Mat>();
    // The corner coordinates of the reference image, in pixels.
    // CVType defines the color depth, number of channels, and
    // channel layout in the image. Here, each point is represented
    // by two 32-bit floats.
    private final Mat mReferenceCorners =
            new Mat(4, 1, CvType.CV_32FC2);
    List<Mat> mReferencesCorners = new ArrayList<Mat>();
    // The reference image's corner coordinates, in 3D, in real
    // units.
    private final MatOfPoint3f mReferenceCorners3D =
            new MatOfPoint3f();
    List<MatOfPoint3f> mReferencesCorners3D = new ArrayList<MatOfPoint3f>();

    // Features of the scene (the current frame).
    private final MatOfKeyPoint mSceneKeypoints =
            new MatOfKeyPoint();
    // Descriptors of the scene's features.
    private final Mat mSceneDescriptors = new Mat();
    // Tentative corner coordinates detected in the scene, in
    // pixels.
    private final Mat mCandidateSceneCorners =
            new Mat(4, 1, CvType.CV_32FC2);
    // Good corner coordinates detected in the scene, in pixels.
    private final MatOfPoint2f mSceneCorners2D =
            new MatOfPoint2f();
    // The good detected corner coordinates, in pixels, as integers.
    private final MatOfPoint mIntSceneCorners = new MatOfPoint();

    // A grayscale version of the scene.
    private final Mat mGraySrc = new Mat();
    // Tentative matches of scene features and reference features.
    private final MatOfDMatch mMatches = new MatOfDMatch();

    // A feature detector, which finds features in images.
    private final FeatureDetector mFeatureDetector =
            FeatureDetector.create(FeatureDetector.ORB);
    // A descriptor extractor, which creates descriptors of
    // features.
    private final DescriptorExtractor mDescriptorExtractor =
            DescriptorExtractor.create(DescriptorExtractor.ORB);
    // A descriptor matcher, which matches features based on their
    // descriptors.
    private final DescriptorMatcher mDescriptorMatcher =
            DescriptorMatcher.create(
                    DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

    // Distortion coefficients of the camera's lens.
    // Assume no distortion.
    private final MatOfDouble mDistCoeffs = new MatOfDouble(
            0.0, 0.0, 0.0, 0.0);

    // An adaptor that provides the camera's projection matrix.
//    private final CameraProjectionAdapter mCameraProjectionAdapter;
    // The Euler angles of the detected target.
    private final MatOfDouble mRVec = new MatOfDouble();
    // The XYZ coordinates of the detected target.
    private final MatOfDouble mTVec = new MatOfDouble();
    // The rotation matrix of the detected target.
    private final MatOfDouble mRotation = new MatOfDouble();
    // The OpenGL pose matrix of the detected target.
    private final float[] mGLPose = new float[16];
    private double realSize;
    // Whether the target is currently detected.
    private boolean mTargetFound = false;

    @RequiresApi(api = VERSION_CODES.P)
    public ImageDetectionFilter(final Context context,
                                final double realSize)
            throws IOException {

        this.realSize = realSize;
        // Load the reference image from the app's resources.
        // It is loaded in BGR (blue, green, red) format.


        MotifDbHelper sql = new MotifDbHelper(context);

        Cursor cursor = sql.dbget();
        CursorWindow cw = new CursorWindow("test", 100 * 1024 * 1024);
        AbstractWindowedCursor ac = (AbstractWindowedCursor) cursor;
        ac.setWindow(cw);


        Log.i(TAG, "Count..." + cursor.getCount());
        if (cursor != null)
            cursor.moveToFirst();

        while (cursor.isAfterLast() == false) {
            int t = cursor.getInt(cursor.getColumnIndex(MotifContrat.MotifTable.t));
            int w = cursor.getInt(cursor.getColumnIndex(MotifContrat.MotifTable.w));
            int h = cursor.getInt(cursor.getColumnIndex(MotifContrat.MotifTable.h));
            byte[] p = cursor.getBlob(cursor.getColumnIndex(MotifContrat.MotifTable.pix));
            Motif motif = new Motif(
                    cursor.getString(cursor.getColumnIndex(MotifContrat.MotifTable.motifID)),
                    cursor.getString(cursor.getColumnIndex(MotifContrat.MotifTable.motifName)),
                    cursor.getString(cursor.getColumnIndex(MotifContrat.MotifTable.motifDescription)),
                    cursor.getString(cursor.getColumnIndex(MotifContrat.MotifTable.motifImageSrc))
            );

            Mat m = new Mat(h, w, t);
            m.put(0, 0, p);

            dbReferencesImages.add(m);
            motifList.add(motif);
            Log.i(TAG, "Motif" + m.toString());
            Log.i(TAG, "Images Mat Blobs" + m.toString());
            cursor.moveToNext();
        }

        Log.i(TAG, "TOus les motifs" + motifList.toString());
    }

    public static void saveMotif(Context context, Motif motif, File file) throws IOException {
        MotifDbHelper sql = new MotifDbHelper(context);


        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Uri uri = Uri.fromFile(file);
        Bitmap bmp = getBitmap(context, uri);

        Mat image = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bmp, image);

        sql.dbput(motif, image);
        Log.i("TAG", "That Works");
    }

    public static Bitmap getBitmap(final Context context, Uri uri) {
        Bitmap bmp = null;
        try {
            bmp = MediaStore.Images.Media.getBitmap(
                    context.getContentResolver(),
                    uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmp;
    }

    @Override
    public float[] getGLPose() {
        return (mTargetFound ? mGLPose : null);
    }

    Mat destinationMat = null;
    Mat sourceMat = null;

    @Override
    public Map<String, Object> recherche(final Mat src, final Mat dst) {
        destinationMat = dst;
        sourceMat = src;

        // Convert the scene to grayscale.
        Imgproc.cvtColor(src, mGraySrc, Imgproc.COLOR_RGBA2GRAY);

        // Detect the scene features, compute their descriptors,
        // and match the scene descriptors to reference descriptors.
        mFeatureDetector.detect(mGraySrc, mSceneKeypoints);
        mDescriptorExtractor.compute(mGraySrc, mSceneKeypoints,
                mSceneDescriptors);


        Log.v("TAILLE DE LA DB", String.valueOf(dbReferencesImages.size()));
//////////////Check in local
        for (int i = 0; i < dbReferencesImages.size(); i++) {

            mReferenceImage = dbReferencesImages.get(i);
            // Create grayscale and RGBA versions of the reference image.
            final Mat referenceImageGray = new Mat();
            Imgproc.cvtColor(mReferenceImage, referenceImageGray,
                    Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(mReferenceImage, mReferenceImage,
                    Imgproc.COLOR_BGR2RGBA);


            // Store the reference image's corner coordinates, in pixels.
            mReferenceCorners.put(0, 0,
                    new double[]{0.0, 0.0});
            mReferenceCorners.put(1, 0,
                    new double[]{referenceImageGray.cols(), 0.0});
            mReferenceCorners.put(2, 0,
                    new double[]{referenceImageGray.cols(),
                            referenceImageGray.rows()});
            mReferenceCorners.put(3, 0,
                    new double[]{0.0, referenceImageGray.rows()});
            Log.i(TAG, "mReferenceCorners  " + mReferenceCorners);


            // Compute the image's width and height in real units, based
            // on the specified real size of the image's smaller
            // dimension.
            final double aspectRatio =
                    (double) referenceImageGray.cols() /
                            (double) referenceImageGray.rows();
            final double halfRealWidth;
            final double halfRealHeight;
            if (referenceImageGray.cols() > referenceImageGray.rows()) {
                halfRealHeight = 0.5f * realSize;
                halfRealWidth = halfRealHeight * aspectRatio;
            } else {
                halfRealWidth = 0.5f * realSize;
                halfRealHeight = halfRealWidth / aspectRatio;
            }


            // Define the real corner coordinates of the printed image
            // so that it normally lies in the xy plane (like a painting
            // or poster on a wall).
            // That is, +z normally points out of the page toward the
            // viewer.
            mReferenceCorners3D.fromArray(
                    new Point3(-halfRealWidth, -halfRealHeight, 0.0),
                    new Point3(halfRealWidth, -halfRealHeight, 0.0),
                    new Point3(halfRealWidth, halfRealHeight, 0.0),
                    new Point3(-halfRealWidth, halfRealHeight, 0.0));
            Log.i(TAG, "mReferenceCorners3D  " + mReferenceCorners3D);

            // Detect the reference features and compute their
            // descriptors.
            mFeatureDetector.detect(referenceImageGray,
                    mReferenceKeypoints);
            Log.i(TAG, "mReferenceKeypoints  " + mReferenceKeypoints);

            mDescriptorExtractor.compute(referenceImageGray,
                    mReferenceKeypoints, mReferenceDescriptors);
            Log.i(TAG, "mReferenceDescriptors  " + mReferenceDescriptors);
            //  mReferencesImage.add(mReferenceImage);
            //  mReferencesCorners.add(mReferenceCorners);
            // mReferencesCorners3D.add(mReferenceCorners3D);
            //mReferencesKeypoints.add(mReferenceKeypoints);
            // mReferencesDescriptors.add(mReferenceDescriptors);

            Log.i(TAG, "mReferencesImage " + mReferencesImage);
            Log.i(TAG, "mReferencesCorners " + mReferencesCorners);

            Log.i(TAG, "mReferencesCorners3D " + mReferencesCorners3D);

            Log.i(TAG, "mReferencesKeypoints " + mReferencesKeypoints);

            Log.i(TAG, "mReferencesDescriptors " + mReferencesDescriptors);


            mDescriptorMatcher.match(mSceneDescriptors,
                    mReferenceDescriptors, mMatches);

            // Attempt to find the target image's 3D pose in the scene.
            Mat res = findPose();

            if (mTargetFound) {
                Log.v("FOUND MATCH", "Trouvé");
                Map<String, Object> result = new HashMap<>();
                result.put("SUCCESS", res);
                result.put("motif", motifList.get(i));
                return result;
            }

        }


        Log.v("FAIL", "FAIL");
        Map<String, Object> result = new HashMap<>();
        result.put("FAIL", src);
        return result;
    }

    private Mat findPose() {

        final List<DMatch> matchesList = mMatches.toList();
        if (matchesList.size() < 4) {
            // There are too few matches to find the pose.
            return sourceMat;
        }

        final List<KeyPoint> referenceKeypointsList =
                mReferenceKeypoints.toList();
        final List<KeyPoint> sceneKeypointsList =
                mSceneKeypoints.toList();

        // Calculate the max and min distances between keypoints.
        double maxDist = 0.0;
        double minDist = Double.MAX_VALUE;
        for (final DMatch match : matchesList) {
            final double dist = match.distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }

        // The thresholds for minDist are chosen subjectively
        // based on testing. The unit is not related to pixel
        // distances; it is related to the number of failed tests
        // for similarity between the matched descriptors.
        if (minDist > 50.0) {
            // The target is completely lost.
            mTargetFound = false;
            return sourceMat;
        } else if (minDist > 25.0) {
            // The target is lost but maybe it is still close.
            // Keep using any previously found pose.
            return sourceMat;
        }

        // Identify "good" keypoints based on match distance.
        final List<Point> goodReferencePointsList =
                new ArrayList<Point>();
        final ArrayList<Point> goodScenePointsList =
                new ArrayList<Point>();
        final double maxGoodMatchDist = 1.75 * minDist;
        for (final DMatch match : matchesList) {
            if (match.distance < maxGoodMatchDist) {
                goodReferencePointsList.add(
                        referenceKeypointsList.get(match.trainIdx).pt);
                goodScenePointsList.add(
                        sceneKeypointsList.get(match.queryIdx).pt);
            }
        }

        if (goodReferencePointsList.size() < 4 ||
                goodScenePointsList.size() < 4) {
            // There are too few good points to find the pose.
            return sourceMat;
        }

        // There are enough good points to find the pose.
        // (Otherwise, the method would have already returned.)

        // Convert the matched points to MatOfPoint2f format, as
        // required by the Calib3d.findHomography function.
        final MatOfPoint2f goodReferencePoints = new MatOfPoint2f();
        goodReferencePoints.fromList(goodReferencePointsList);
        final MatOfPoint2f goodScenePoints = new MatOfPoint2f();
        goodScenePoints.fromList(goodScenePointsList);

        // Find the homography.
        final Mat homography = Calib3d.findHomography(
                goodReferencePoints, goodScenePoints);

        // Use the homography to project the reference corner
        // coordinates into scene coordinates.
        Core.perspectiveTransform(mReferenceCorners,
                mCandidateSceneCorners, homography);

        // Convert the scene corners to integer format, as required
        // by the Imgproc.isContourConvex function.
        mCandidateSceneCorners.convertTo(mIntSceneCorners,
                CvType.CV_32S);

        // Check whether the corners form a convex polygon. If not,
        // (that is, if the corners form a concave polygon), the
        // detection result is invalid because no real perspective can
        // make the corners of a rectangular image look like a concave
        // polygon!
        if (!Imgproc.isContourConvex(mIntSceneCorners)) {
            return sourceMat;
        }


        mTargetFound = true;
        return destinationMat;
    }
}
