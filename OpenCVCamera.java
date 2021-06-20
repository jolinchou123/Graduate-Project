package com.example.jolin.afinal;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.opencv.core.Core.FONT_HERSHEY_PLAIN;
import static org.opencv.core.Core.determinant;

public class OpenCVCamera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = "OpenCVCamera";
    private SoundEffectPlayer      soundEffectPlayer ;
    private SoundEffectPlayer      soundEffectPlayer2 ;

    private Mat                    mRgba1;
    private Mat                    mRgba2;
    private Mat                    mRgba3;

    private Mat                    readMat; //存怪獸圖片
    private Mat                    readMatt; //存死掉怪獸圖片
    public Mat                    resultMat;

    private CameraBridgeViewBase mOpenCvCameraView;

    double bp = 0.0, wp = 0.0;
    double density = 0.0;
    String message="";
    float speedup = 4.0f;
    boolean status = true; //判斷是否已藏怪獸
    boolean m_status = true; //判斷怪獸homography準不
    boolean mm_status = true; //判斷怪獸homography準不
    int attack = 0; //是否進入攻擊
    boolean appear = true; //怪獸活著還死掉
    boolean visible = true; //怪獸消失出現

    /*----儲存藏怪物的畫面----*/
    private StoreValue storeValue;
    /*----儲存藏怪物的畫面----*/

    /*---------- ==========AKAZE FEATURE DETECT&COMPUTE ==========-----------*/
    //----準備工具
    FeatureDetector detector;
    DescriptorExtractor descriptorExtractor;
    DescriptorMatcher descriptorMatcher;

    //----相機影像變數
    MatOfKeyPoint keyPoints1;
    Mat                            descriptors1 ;
    //----比較影像變數
    MatOfKeyPoint                  keyPoints2;
    Mat                            descriptors2;
    //----下個畫面影像變數
    MatOfKeyPoint                  keyPoints3;
    Mat                            descriptors3;
    //輸出影像
    Mat                            drawInImg; //用來擷取鏡頭的影像
    Mat                            secondImg; //連拍兩張用來找special point
    Mat                            nextImg; //貼上怪獸後的下個畫面
    /*---------- ========== ==========-----------*/
    /*---------- ==========MUSCLE SENSOR ==========-----------*/
    private Button back;
    private Long startTime;
    private static Handler handler = new Handler();
    int scores =100;
    private TextView target;
    private ArrayList Logs = new ArrayList();
    private ProgressBar progressbar;
    private ImageView win;
    private ImageView lose;
    private Button startbutton;
    double[] logs = new double[16];
    double[] im = new double[16];
    double[] fft=new double[16];
    int count=0;
    int v=0;
    int o;
    /*---------- ========== ==========-----------*/
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public OpenCVCamera() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /*---------==========- Application Setting ==========----------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*----音效模組-----*/
        soundEffectPlayer = new SoundEffectPlayer( this ) ;
        soundEffectPlayer2 = new SoundEffectPlayer( this ) ;

        /*----設定當前頁面----*/
        setContentView(R.layout.activity_open_cvcamera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);//設定螢幕不隨手機旋轉
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//設定螢幕直向顯示

        /*----將java camera view 與 opencv camera view 綁上----*/
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        /*----sensor----*/
        progressbar = (ProgressBar) findViewById (R.id.progressBar);
        final Button startbutton = (Button)findViewById(R.id.start);
        win= (ImageView) findViewById (R.id.win);
        lose= (ImageView) findViewById (R.id.lose);
        progressbar.setProgress(100);
        progressbar.setMax(100);

        /*----Back to main menu----*/
        back = (Button)findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                Intent i = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(i);
            }
        });

        /*startbutton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                progressbar.setProgress(100);
                startTime = System.currentTimeMillis();
                count=0;
                scores=100;
                handler.post(updateTimer);
            }
        });*/
    }

    private Runnable updateTimer = new Runnable() {
        @Override
        public void run() {

            final TextView time = (TextView) findViewById(R.id.time);
            Long spentTime = System.currentTimeMillis() - startTime;

            //計算目前已過分鐘數
            Long minutes = (spentTime/1000)/60;
            //計算目前已過秒數
            Long seconds = (spentTime/1000) % 60;

            TextView target = (TextView) findViewById(R.id.target);
            // time.setText(""+minutes+":"+seconds+" ");

            /*從session拿connect抓到的數字*/
            Session session = Session.getSession();

            int i =0;
            i = Integer.parseInt((String) session.get("data"));

            if(i>100000) i=i/1000;
            else if(i<100){
                o=i;
            }
            else if((i>10000)&&(i<100000)){
                if(i%1000==v%1000)
                    o=i/1000;
                else if(i%100==v%100)
                    o=i/100;
            }
            else if((i>1000)&&(i<10000)){
                if(i%100==v%100)
                    o=i/100;
                else if((i%10==v%10)&&(i%100!=v%100))
                    o=i/10;
            }
            else if(i<1000){
                if(i%10==v%10)
                    o=i/10;
                else
                    o=i;
            }
            v=i;

            /*----打怪----*/
            target.setText("A wild monster appered!!");
            //target.setBackgroundColor(0xFFFFFFFF);
            time.setVisibility(View.VISIBLE);
            if(seconds<3){
                time.setText("Start in ... "+(4-seconds));
                target.setText("hold to decrease the monsters health!!");
            }

            else if(seconds>=3){
                progressbar.setVisibility(View.VISIBLE);
                time.setText("Time left :"+(18-seconds));
                if(o>900){
                    o=0;
                }
                if(o>300){
                    target.setText("good!");
                    scores--;
                    if(o>500){
                        target.setText("perfect!");
                        scores--;}
                }
                if(o<=100){
                    target.setText("...weak");
                }
                progressbar.setProgress(scores);
            }

            if(seconds>18){
                progressbar.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                if(scores>0) {
                    target.setText("You lose");
                    appear = false;
                    lose.setVisibility(View.VISIBLE);
                    soundEffectPlayer2.stop();
                    soundEffectPlayer.play(R.raw.losesong);
                    back.setVisibility(View.VISIBLE);
                }
            }

            handler.postDelayed(this, 150);

            if(scores<=0){
                target.setText("You win");
                appear = false;
                soundEffectPlayer2.stop();
                soundEffectPlayer.play(R.raw.winsong);
                win.setVisibility(View.VISIBLE);
                handler.removeCallbacks(updateTimer);
                back.setVisibility(View.VISIBLE);
            }

            if(seconds>18) {
                handler.removeCallbacks(updateTimer);
            }
            /*----打怪----*/

            /*----怪獸跑掉停止攻擊----*/
            if(visible==false){
                attack=0;
                progressbar.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                target.setText("monster escape");
                handler.removeCallbacks(updateTimer);
                soundEffectPlayer2.stop();
            }
            /*----怪獸跑掉停止攻擊----*/
        }
    };

    /*----------------------------------------------------------------------------------------------------*/

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        handler.removeCallbacks(updateTimer);
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        soundEffectPlayer2.stop();
    }

    /*----------========== OpenCv Camera ========== ----------*/
    @Override
    public void onCameraViewStarted(int width, int height) {
        //讀入怪獸圖片
        InputStream is = getResources().openRawResource(R.raw.monster);
        Bitmap footbm = BitmapFactory.decodeStream(is);
        readMat = new Mat();
        Utils.bitmapToMat(footbm,readMat);

        //讀入死掉怪獸圖片
        InputStream iss = getResources().openRawResource(R.raw.ddmonster);
        Bitmap footbmm = BitmapFactory.decodeStream(iss);
        readMatt = new Mat();
        Utils.bitmapToMat(footbmm,readMatt);

        //init AKAZE TOOLS
        detector= FeatureDetector.create(FeatureDetector.AKAZE);
        descriptorExtractor= DescriptorExtractor.create(DescriptorExtractor.AKAZE);
        descriptorMatcher=DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);

        //init 相機影像變數
        keyPoints1 = new MatOfKeyPoint();
        descriptors1 = new Mat();
        keyPoints2 = new MatOfKeyPoint();
        descriptors2 = new Mat();
        keyPoints3 = new MatOfKeyPoint();
        descriptors3 = new Mat();

        //init 其他變數
        mRgba1 = new Mat(height, width, CvType.CV_8UC4);
        mRgba2 = new Mat(height, width, CvType.CV_8UC4);
        mRgba3 = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba1.release();
        mRgba2.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Scalar redcolor = new Scalar(255, 0, 0);

        //----將相機畫面導直
        mRgba1 = inputFrame.rgba();
        Mat mRgbaT2 = mRgba1.t().clone();
        Core.flip(mRgbaT2, mRgba1.t(), 0);
        Core.flip(mRgbaT2, mRgbaT2, 1);
        Imgproc.resize(mRgbaT2, mRgbaT2, mRgba1.size());
        mRgba1 = mRgbaT2.clone();

        //初始化要拿來畫特徵點的Mat
        drawInImg = mRgba1.clone();
        //初始化要拿來貼怪獸的Mat
        resultMat = mRgba1.clone();

        //怪獸圖長寬
        float width = readMat.width();
        float height = readMat.height();
        //死掉怪獸圖長寬
        float mwidth = readMatt.width();
        float mheight = readMatt.height();

        /*----開始找藏怪獸的地方----*/
        if (status == true) {
            Mat temp1 = new Mat();
            Mat blurred = new Mat();
            Mat canny = new Mat();
            Mat blackThres = new Mat(drawInImg.rows(), drawInImg.cols(), CvType.CV_8UC1);//建立图像二值化存儲空間
            Mat whiteThres = new Mat(drawInImg.rows(), drawInImg.cols(), CvType.CV_8UC1);//建立图像二值化存儲空間

            Imgproc.cvtColor(drawInImg, temp1, Imgproc.COLOR_RGBA2GRAY);

            /*----計算密度----*/
            //去除雜訊
            Imgproc.GaussianBlur(temp1, blurred, new Size(5, 5), 1, 1);
            //用canny偵測邊緣
            Imgproc.Canny(blurred, canny, 30, 150, 5, true);
            //用threshold創造一個binary image
            Imgproc.threshold(canny, blackThres, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            Imgproc.threshold(canny, whiteThres, 0, 255, Imgproc.THRESH_BINARY_INV);
            //讀取pixels
            bp = Core.countNonZero(blackThres);
            wp = Core.countNonZero(whiteThres);
            //計算密度(numPixelsOn/totalPixels)
            density = bp / (bp + wp);
            /*----計算密度----*/
            if (density > 0.07) { //密度大於某門檻值
                message = "Density Enough!";
                Imgproc.putText(drawInImg, message, new Point(0, 50), FONT_HERSHEY_PLAIN, 3, new Scalar(255, 255, 255), 5);

                //----將比較畫面導直
                mRgba2 = inputFrame.rgba();
                Mat mRgbaT22 = mRgba2.t().clone();
                Core.flip(mRgbaT22, mRgba2.t(), 0);
                Core.flip(mRgbaT22, mRgbaT22, 1);
                Imgproc.resize(mRgbaT22, mRgbaT22, mRgba2.size());
                mRgba2 = mRgbaT22.clone();
                //初始化要拿來畫特徵點的Mat
                secondImg = mRgba2.clone();
                Mat temp2 = new Mat();
                Imgproc.cvtColor(secondImg, temp2, Imgproc.COLOR_RGBA2GRAY);

                /*----尋找special point----*/
                //使畫面加速
                //Imgproc.resize(temp1, temp1, new Size(temp1.width() / speedup, temp1.height() / speedup));//長寬縮小speedUp倍 加速
                //Imgproc.resize(temp2, temp2, new Size(temp2.width() / speedup, temp2.height() / speedup));//長寬縮小speedUp倍 加速

                //只取畫面中間
                Rect roi1 = new Rect((int)(temp1.width()*0.25), (int)(temp1.height()*0.25), temp1.width()/2, temp1.height()/2);
                Mat subTemp1 = new Mat(temp1, roi1);
                Rect roi2 = new Rect((int)(temp2.width()*0.25), (int)(temp2.height()*0.25), temp2.width()/2, temp2.height()/2);
                Mat subTemp2 = new Mat(temp2, roi2);

                /*detector.detect(temp1, keyPoints1);
                descriptorExtractor.compute(temp1, keyPoints1, descriptors1);
                detector.detect(temp2, keyPoints2);
                descriptorExtractor.compute(temp2, keyPoints2, descriptors2);*/
                detector.detect(subTemp1, keyPoints1);
                descriptorExtractor.compute(subTemp1, keyPoints1, descriptors1);
                detector.detect(subTemp2, keyPoints2);
                descriptorExtractor.compute(subTemp2, keyPoints2, descriptors2);
                //Features2d.drawKeypoints(drawInImg, keyPoints1, drawInImg, redcolor, 3);

                List<KeyPoint> orgKeyPoint = keyPoints1.toList();
                //將keypoint轉換成resize前的大小
                /*for (KeyPoint k : orgKeyPoint) {
                    k.pt.x *= speedup;
                    k.pt.y *= speedup;
                    k.size *= speedup;
                }*/

                //準備List來存specialmatch
                LinkedList<DMatch> special_matches = new LinkedList<DMatch>();
                //準備List來存條件符合的specialPoint
                LinkedList<Point> spKeyPoint1 = new LinkedList<>();

                //配對兩張圖的descriptors
                List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
                descriptorMatcher.knnMatch(descriptors1, descriptors2, matches, 2);

                //----依matches.size()來篩選specialmatch
                for (int i = 0; i < matches.size(); i++) {
                    MatOfDMatch matofDMatch = matches.get(i);
                    DMatch dmatches[] = matofDMatch.toArray();
                    DMatch dm1 = dmatches[0];
                    DMatch dm2 = dmatches[1];

                    if (dm1.distance < 0.01 * dm2.distance) {
                        special_matches.addLast(dm1);
                        spKeyPoint1.addLast(orgKeyPoint.get(dm1.queryIdx).pt);

                        if (special_matches.size() > 10) {
                            status = false;
                            message = "Can Hide Monster";
                            Imgproc.putText(drawInImg, message, new Point(0, 100), FONT_HERSHEY_PLAIN, 3, new Scalar(255, 255, 255), 5);
                            /*----貼怪獸上去----*/
                            /*Point a_src = new Point(0, 0);
                            Point b_src = new Point(width - 1, 0);
                            Point c_src = new Point(width - 1, height - 1);
                            Point d_src = new Point(0, height - 1);
                            Point a_dst = spKeyPoint1.getFirst();
                            Point b_dst = new Point(a_dst.x + width, a_dst.y);
                            Point c_dst = new Point(a_dst.x + width, a_dst.y + height);
                            Point d_dst = new Point(a_dst.x, a_dst.y + height);

                            Point[] pts_src = new Point[4];
                            pts_src[0] = a_src;
                            pts_src[1] = b_src;
                            pts_src[2] = c_src;
                            pts_src[3] = d_src;
                            LinkedList<Point> pts_dst = new LinkedList<Point>();
                            pts_dst.add(a_dst);
                            pts_dst.add(b_dst);
                            pts_dst.add(c_dst);
                            pts_dst.add(d_dst);

                            MatOfPoint2f src = new MatOfPoint2f();
                            src.fromArray(pts_src);
                            MatOfPoint2f dst = new MatOfPoint2f();
                            dst.fromList(pts_dst);

                            Mat H = Calib3d.findHomography(src, dst);
                            Imgproc.warpPerspective(readMat, resultMat, H, resultMat.size());*/
                            /*----貼怪獸上去----*/

                            Point a_object = spKeyPoint1.getFirst();
                            Point b_object = new Point(a_object.x + width, a_object.y);
                            Point c_object = new Point(a_object.x + width, a_object.y + height);
                            Point d_object = new Point(a_object.x, a_object.y + height);
                            LinkedList<Point> pts_object = new LinkedList<Point>();
                            pts_object.add(a_object);
                            pts_object.add(b_object);
                            pts_object.add(c_object);
                            pts_object.add(d_object);
                            MatOfPoint2f object = new MatOfPoint2f();
                            object.fromList(pts_object);
                            Point[] pts_scene = new Point[0];
                            MatOfPoint2f scene = new MatOfPoint2f(pts_scene);

                            Mat T2 = new Mat(3, 3, CvType.CV_32S);
                            T2.put(0, 0, 1);
                            T2.put(0, 1, 0);
                            T2.put(0, 2, 264);
                            T2.put(1, 0, 0);
                            T2.put(1, 1, 1);
                            T2.put(1, 2, 176);
                            T2.put(2, 0, 0);
                            T2.put(2, 1, 0);
                            T2.put(2, 2, 1);

                            Core.perspectiveTransform(object, scene, T2);

                            Point a_src = new Point(0, 0);
                            Point b_src = new Point(width - 1, 0);
                            Point c_src = new Point(width - 1, height - 1);
                            Point d_src = new Point(0, height - 1);
                            Point[] corner_dst = scene.toArray();
                            Point a_dst = new Point(corner_dst[0].x, corner_dst[0].y);
                            Point b_dst = new Point(corner_dst[1].x, corner_dst[1].y);
                            Point c_dst = new Point(corner_dst[2].x, corner_dst[2].y);
                            Point d_dst = new Point(corner_dst[3].x, corner_dst[3].y);
                            Point[] pts_src = new Point[4];
                            pts_src[0] = a_src;
                            pts_src[1] = b_src;
                            pts_src[2] = c_src;
                            pts_src[3] = d_src;
                            LinkedList<Point> pts_dst = new LinkedList<Point>();
                            pts_dst.add(a_dst);
                            pts_dst.add(b_dst);
                            pts_dst.add(c_dst);
                            pts_dst.add(d_dst);

                            MatOfPoint2f src = new MatOfPoint2f();
                            src.fromArray(pts_src);
                            MatOfPoint2f dst = new MatOfPoint2f();
                            dst.fromList(pts_dst);

                            Mat H = Calib3d.findHomography(src, dst);
                            Imgproc.warpPerspective(readMat, resultMat, H, resultMat.size());

                            //將藏怪獸的畫面存起來
                            storeValue = new StoreValue(keyPoints1, descriptors1, resultMat, spKeyPoint1);
                        }
                    }
                }



                message = "Special Point:" + spKeyPoint1.size();
                Imgproc.putText(drawInImg, message, new Point(0, 150), FONT_HERSHEY_PLAIN, 3, new Scalar(255, 255, 255), 5);
                Core.addWeighted(drawInImg, 0.7, resultMat, 0.3, 0.0, drawInImg);
            } else {
                message = "No Density!";
                Imgproc.putText(drawInImg, message, new Point(0, 50), FONT_HERSHEY_PLAIN, 3, new Scalar(255, 255, 255), 5);
            }
        }
        /*----開始找藏怪獸的地方----*/

        /*----找到藏怪獸的地方----*/
        else if (status == false) {
                //----將相機畫面導直
                mRgba3 = inputFrame.rgba();
                Mat mRgbaT222 = mRgba3.t().clone();
                Core.flip(mRgbaT222, mRgba3.t(), 0);
                Core.flip(mRgbaT222, mRgbaT222, 1);
                Imgproc.resize(mRgbaT222, mRgbaT222, mRgba3.size());
                mRgba3 = mRgbaT222.clone();
                //初始化要拿來畫特徵點的Mat
                nextImg = mRgba3.clone();
                Mat temp3 = new Mat();
                Imgproc.cvtColor(nextImg, temp3, Imgproc.COLOR_RGBA2GRAY);

                /*----判斷貼怪獸畫面與下個畫面是否相似----*/
                //使畫面加速
                //Imgproc.resize(temp3, temp3, new Size(temp3.width() / speedup, temp3.height() / speedup));//長寬縮小speedUp倍 加速

                Rect roi3 = new Rect((int)(temp3.width()*0.25), (int)(temp3.height()*0.25), temp3.width()/2, temp3.height()/2);
                Mat subTemp3 = new Mat(temp3, roi3);

                /*detector.detect(temp3, keyPoints3);
                descriptorExtractor.compute(temp3, keyPoints3, descriptors3);*/

                detector.detect(subTemp3, keyPoints3);
                descriptorExtractor.compute(subTemp3, keyPoints3, descriptors3);
                //Features2d.drawKeypoints(drawInImg, keyPoints3, drawInImg, redcolor, 3);

                List<KeyPoint> orgKeyPoint = storeValue.keyPoints1.toList();
                //將keypoint轉換成resize前的大小
                /*for (KeyPoint k : orgKeyPoint) {
                    k.pt.x *= speedup;
                    k.pt.y *= speedup;
                    k.size *= speedup;
                }*/
                List<KeyPoint> orgCompareKeyPoint = keyPoints3.toList();
                //將keypoint轉換成resize前的大小
                /*for (KeyPoint k : orgCompareKeyPoint) {
                    k.pt.x *= speedup;
                    k.pt.y *= speedup;
                    k.size *= speedup;
                }*/

                //準備兩個list來存goodmatch
                List<KeyPoint> gdKeyPoint = new ArrayList<KeyPoint>();
                List<KeyPoint> gdCompareKeyPoint = new ArrayList<KeyPoint>();
                //配對兩張圖的descriptors
                List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
                descriptorMatcher.knnMatch(storeValue.descriptors1, descriptors3, matches, 2);
                LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
                //----依matches.size()來篩選goodmatch
                for (int i = 0; i < matches.size(); i++) {
                    MatOfDMatch matofDMatch = matches.get(i);
                    DMatch dmatches[] = matofDMatch.toArray();
                    DMatch dm1 = dmatches[0];
                    DMatch dm2 = dmatches[1];
                    if (dm1.distance < 0.5 * dm2.distance) {
                        good_matches.addLast(matofDMatch.toArray()[0]);
                    }
                }
                for (int j = 0; j < good_matches.size(); j++) {
                    gdKeyPoint.add(orgKeyPoint.get(good_matches.get(j).queryIdx));
                    gdCompareKeyPoint.add(orgCompareKeyPoint.get(good_matches.get(j).trainIdx));
                }
                if (good_matches.size() > 10) {
                    message = "Same Scene" + good_matches.size();
                    Imgproc.putText(drawInImg, message, new Point(0, 200), FONT_HERSHEY_PLAIN, 3, new Scalar(255, 255, 255), 5);
                    List<Point> sceneTemp1 = new LinkedList<Point>();
                    MatOfPoint2f scene1 = new MatOfPoint2f();
                    for (KeyPoint k : gdKeyPoint) {
                        sceneTemp1.add(k.pt);
                    }
                    scene1.fromList(sceneTemp1);
                    List<Point> sceneTemp2 = new LinkedList<Point>();
                    MatOfPoint2f scene2 = new MatOfPoint2f();
                    for (KeyPoint k : gdCompareKeyPoint) {
                        sceneTemp2.add(k.pt);
                    }
                    scene2.fromList(sceneTemp2);
                    Mat H = Calib3d.findHomography(scene1, scene2);

                    Mat scene1_corners = new Mat(4, 1, CvType.CV_32FC2);
                    Mat scene2_corners = new Mat(4, 1, CvType.CV_32FC2);

                    //原本怪獸貼的區塊
                    Point point0 = storeValue.spKeyPoint1.getFirst();
                    Point point1 = new Point(point0.x + width, point0.y);
                    Point point2 = new Point(point0.x + width, point0.y + height);
                    Point point3 = new Point(point0.x, point0.y + height);
                    /*Point point0 = new Point(storeValue.corner_dst[0].x, storeValue.corner_dst[0].y);
                    Point point1 = new Point(storeValue.corner_dst[1].x, storeValue.corner_dst[1].y);
                    Point point2 = new Point(storeValue.corner_dst[2].x, storeValue.corner_dst[2].y);
                    Point point3 = new Point(storeValue.corner_dst[3].x, storeValue.corner_dst[3].y);*/

                    scene1_corners.put(0, 0, new double[]{point0.x, point0.y});
                    scene1_corners.put(1, 0, new double[]{point1.x, point1.y});
                    scene1_corners.put(2, 0, new double[]{point2.x, point2.y});
                    scene1_corners.put(3, 0, new double[]{point3.x, point3.y});
                    /*scene1_corners.put(0, 0, new double[]{point0.x, point0.y});
                    scene1_corners.put(1, 0, new double[]{point1.x, point1.y});
                    scene1_corners.put(2, 0, new double[]{point2.x, point2.y});
                    scene1_corners.put(3, 0, new double[]{point3.x, point3.y});*/
                    Core.perspectiveTransform(scene1_corners, scene2_corners, H);

                    /*----長寬窄到不行----*/
                    Point a = new Point(scene2_corners.get(0, 0));
                    Point b = new Point(scene2_corners.get(1, 0));
                    Point c = new Point(scene2_corners.get(2, 0));
                    Point d = new Point(scene2_corners.get(3, 0));
                    double m_width = 0.0, m_height = 0.0;
                    m_width = distance(a, b);
                    m_height = distance(b, c);
                    if (m_width < 20.0 || m_height < 20.0) {
                        m_status = false;
                    } else {
                        m_status = true;
                    }
                    /*----長寬窄到不行----*/

                    /*----計算homography行列式----*/
                    double det = determinant(H);
                    System.out.println(det);
                    if (det > 0) {
                        m_status = true;
                    } else {
                        m_status = false;
                    }
                    /*----計算homography行列式----*/

                    if (m_status == true) {
                        visible = true;
                        if(appear == true) {
                            /*----貼怪獸上去----*/
                            /*Point a_src = new Point(0, 0);
                            Point b_src = new Point(width - 1, 0);
                            Point c_src = new Point(width - 1, height - 1);
                            Point d_src = new Point(0, height - 1);
                            Point a_dst = new Point(scene2_corners.get(0, 0));
                            Point b_dst = new Point(scene2_corners.get(1, 0));
                            Point c_dst = new Point(scene2_corners.get(2, 0));
                            Point d_dst = new Point(scene2_corners.get(3, 0));
                            Point[] pts_src = new Point[4];
                            pts_src[0] = a_src;
                            pts_src[1] = b_src;
                            pts_src[2] = c_src;
                            pts_src[3] = d_src;
                            LinkedList<Point> pts_dst = new LinkedList<Point>();
                            pts_dst.add(a_dst);
                            pts_dst.add(b_dst);
                            pts_dst.add(c_dst);
                            pts_dst.add(d_dst);

                            MatOfPoint2f src = new MatOfPoint2f();
                            src.fromArray(pts_src);
                            MatOfPoint2f dst = new MatOfPoint2f();
                            dst.fromList(pts_dst);

                            Mat M = Calib3d.findHomography(src, dst);
                            Imgproc.warpPerspective(readMat, resultMat, M, resultMat.size());

                            Core.addWeighted(drawInImg, 0.7, resultMat, 0.3, 0.0, drawInImg);*/
                            /*----貼怪獸上去----*/

                            Point a_object = new Point(scene2_corners.get(0, 0));
                            Point b_object = new Point(scene2_corners.get(1, 0));
                            Point c_object = new Point(scene2_corners.get(2, 0));
                            Point d_object = new Point(scene2_corners.get(3, 0));
                            LinkedList<Point> pts_object = new LinkedList<Point>();
                            pts_object.add(a_object);
                            pts_object.add(b_object);
                            pts_object.add(c_object);
                            pts_object.add(d_object);
                            MatOfPoint2f object = new MatOfPoint2f();
                            object.fromList(pts_object);
                            Point[] pts_scene = new Point[0];
                            MatOfPoint2f scene = new MatOfPoint2f(pts_scene);

                            Mat T2 = new Mat(3, 3, CvType.CV_32S);
                            T2.put(0, 0, 1);
                            T2.put(0, 1, 0);
                            T2.put(0, 2, 264);
                            T2.put(1, 0, 0);
                            T2.put(1, 1, 1);
                            T2.put(1, 2, 176);
                            T2.put(2, 0, 0);
                            T2.put(2, 1, 0);
                            T2.put(2, 2, 1);

                            Core.perspectiveTransform(object, scene, T2);

                            Point a_src = new Point(0, 0);
                            Point b_src = new Point(width - 1, 0);
                            Point c_src = new Point(width - 1, height - 1);
                            Point d_src = new Point(0, height - 1);
                            Point[] corner_dst = scene.toArray();
                            Point a_dst = new Point(corner_dst[0].x, corner_dst[0].y);
                            Point b_dst = new Point(corner_dst[1].x, corner_dst[1].y);
                            Point c_dst = new Point(corner_dst[2].x, corner_dst[2].y);
                            Point d_dst = new Point(corner_dst[3].x, corner_dst[3].y);
                            Point[] pts_src = new Point[4];
                            pts_src[0] = a_src;
                            pts_src[1] = b_src;
                            pts_src[2] = c_src;
                            pts_src[3] = d_src;
                            LinkedList<Point> pts_dst = new LinkedList<Point>();
                            pts_dst.add(a_dst);
                            pts_dst.add(b_dst);
                            pts_dst.add(c_dst);
                            pts_dst.add(d_dst);

                            MatOfPoint2f src = new MatOfPoint2f();
                            src.fromArray(pts_src);
                            MatOfPoint2f dst = new MatOfPoint2f();
                            dst.fromList(pts_dst);

                            Mat M = Calib3d.findHomography(src, dst);

                            /*----長寬窄到不行----*/
                            double mm_width = 0.0, mm_height = 0.0;
                            mm_width = distance(a_dst, b_dst);
                            mm_height = distance(b_dst, c_dst);
                            if (mm_width < 30.0 || mm_height < 30.0) {
                                mm_status = false;
                            } else {
                                mm_status = true;
                            }
                            /*----長寬窄到不行----*/

                            /*----計算homography行列式----*/
                            double dett = determinant(M);
                            System.out.println(dett);
                            if (dett > 0.4) {
                                mm_status = true;
                            } else {
                                mm_status = false;
                            }
                            /*----計算homography行列式----*/

                            if(mm_status == true) {
                                Imgproc.warpPerspective(readMat, resultMat, M, resultMat.size());
                                Core.addWeighted(drawInImg, 0.7, resultMat, 0.3, 0.0, drawInImg);
                            }

                        }

                        /*----怪獸死掉----*/
                        else if(appear == false){
                            /*----貼死掉怪獸上去----*/
                            Point aa_src = new Point(0, 0);
                            Point bb_src = new Point(mwidth - 1, 0);
                            Point cc_src = new Point(mwidth - 1, mheight - 1);
                            Point dd_src = new Point(0, mheight - 1);
                            Point aa_dst = new Point(scene2_corners.get(0, 0));
                            Point bb_dst = new Point(scene2_corners.get(1, 0));
                            Point cc_dst = new Point(scene2_corners.get(2, 0));
                            Point dd_dst = new Point(scene2_corners.get(3, 0));
                            Point[] ppts_src = new Point[4];
                            ppts_src[0] = aa_src;
                            ppts_src[1] = bb_src;
                            ppts_src[2] = cc_src;
                            ppts_src[3] = dd_src;
                            LinkedList<Point> ppts_dst = new LinkedList<Point>();
                            ppts_dst.add(aa_dst);
                            ppts_dst.add(bb_dst);
                            ppts_dst.add(cc_dst);
                            ppts_dst.add(dd_dst);

                            MatOfPoint2f ssrc = new MatOfPoint2f();
                            ssrc.fromArray(ppts_src);
                            MatOfPoint2f ddst = new MatOfPoint2f();
                            ddst.fromList(ppts_dst);

                            Mat MM = Calib3d.findHomography(ssrc, ddst);
                            Imgproc.warpPerspective(readMatt, resultMat, MM, resultMat.size());
                            Core.addWeighted(drawInImg, 0.7, resultMat, 0.3, 0.0, drawInImg);
                            /*----貼死掉怪獸上去----*/
                        }
                        /*----怪獸死掉----*/

                    }

                    /*----開始攻擊----*/
                    attack++;
                    if ((attack > 0) && (attack < 2)) {
                        progressbar.setProgress(100);
                        startTime = System.currentTimeMillis();
                        count = 0;
                        scores = 100;
                        //win.setVisibility(View.INVISIBLE);
                        //lose.setVisibility(View.INVISIBLE);
                        handler.post(updateTimer);
                        soundEffectPlayer2.play(R.raw.flamingo);
                    }
                    /*----開始攻擊----*/

                    //將list清空
                    gdKeyPoint.clear();
                    gdCompareKeyPoint.clear();
                    matches.clear();
                    good_matches.clear();
                    sceneTemp1.clear();
                    sceneTemp2.clear();
                } else {
                    visible = false;
                    message = "Not Same Scene" + good_matches.size();
                    Imgproc.putText(drawInImg, message, new Point(0, 200), FONT_HERSHEY_PLAIN, 3, new Scalar(255, 255, 255), 5);
                }
            }
        /*----找到藏怪獸的地方----*/
        return drawInImg;
    }

    /*----計算怪獸四角的距離----*/
    double distance(Point x, Point y) {
        return Math.sqrt(Math.pow(x.x - y.x, 2) + Math.pow(x.y - y.y, 2));
    }
    /*----計算怪獸四角的距離----*/

}
