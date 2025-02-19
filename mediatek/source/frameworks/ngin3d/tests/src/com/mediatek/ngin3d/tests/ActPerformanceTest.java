package com.mediatek.ngin3d.tests;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.StrictMode;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import com.mediatek.ngin3d.BitmapFont;
import com.mediatek.ngin3d.BitmapText;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Empty;
import com.mediatek.ngin3d.Glo3D;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Ngin3d;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.android.StageView;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.PropertyAnimation;
import com.mediatek.ngin3d.presentation.PresentationEngine;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class ActPerformanceTest extends ActivityInstrumentationTestCase2<PresentationStubActivity> {

    private static final String TAG = "ActPerformanceTest";
    protected Stage mStage;
    private StageView mStageView;
    protected PresentationEngine mPresentationEngine;

    /**
     * Causes the current thread to sleep for a specified period.
     * @param ms Sleep time in milliseconds
     */
    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            // Do nothing
        }
    }

    public ActPerformanceTest() {
        super(PresentationStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mStageView = getActivity().getStageView();
        mStage = new Stage();
        mPresentationEngine = Ngin3d.createPresentationEngine(mStage);

        mPresentationEngine.initialize(480, 800,
                getInstrumentation().getContext().getResources());

        if (ENGINE_INITIALIZATION_TIME_CRITERIA == 0) {
            setupCriteriaValue(getActivity().getResources());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        mPresentationEngine.uninitialize();

        mPresentationEngine = null;
        mStage = null;
        super.tearDown();
    }

    @SmallTest
    public void testEmptyImage() {
        mStage.realize(mPresentationEngine);
    }

    // Test criteria
    private static long ENGINE_INITIALIZATION_TIME_CRITERIA = 0;
    private static long IMAGE_LOADING_FROM_RESOURCE_TIME_CRITERIA = 0;
    private static long IMAGE_LOADING_FROM_BITMAP_TIME_CRITERIA = 0;
    private static long IMAGE_LOADING_FROM_FILE_TIME_CRITERIA = 0;
    private static long SYSTEM_TEXT_CONTENT_UPDATE_TIME_CRITERIA = 0;
    private static long BITMAP_FONT_TEXT_CONTENT_UPDATE_TIME_CRITERIA = 0;
    private static long RENDER_50_ACTORS_TIME_CRITERIA = 0;
    private static long RENDER_100_ACTORS_TIME_CRITERIA = 0;
    private static long START_50_ANIMATIONS_TIME_CRITERIA = 0;
    private static long START_100_ANIMATIONS_TIME_CRITERIA = 0;
    private static long SCREEN_SHOT_TIME_CRITERIA = 0;
    private static long RENDER_25_LANDSCAPES_FPS_CRITERIA = 0;

    /**
     * Measures the average frame rate over a given duration.
     * @param duration Duration over which to measure in milliseconds
     * @param interval Interval at which to sample frame rate in milliseconds
     * @return Average frame rate in frames per second
     */
    private float measureFps(int duration, int interval) {
        if (duration <= 0 || interval <= 0 || interval > duration) {
            return -1;
        }

        mStageView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        float fpsSum = 0;
        int intervalCount = duration / interval;

        for (int i = 0; i < intervalCount; ++i) {
            sleep(interval);
            fpsSum += mStageView.getFPS();
        }

        mStageView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        return fpsSum / intervalCount;
    }

    public void test01_EngineInitialTime() {
        mPresentationEngine.uninitialize();
        Log.v(TAG, "Start engine initialize");
        long t1 = SystemClock.uptimeMillis();
        mPresentationEngine.initialize(480, 800);
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "Engine initialize costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.initialize-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(ENGINE_INITIALIZATION_TIME_CRITERIA)));
    }

    public void test02_ImageLoadingFromResourceTime() {
        Log.v(TAG, "Start loading images from resource");
        long t1 = SystemClock.uptimeMillis();
        Image image = Image.createFromResource(getInstrumentation().getContext().getResources(), R.drawable.earth);
        image.realize(mPresentationEngine);
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "Loading images from resource costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.image_loading_from_resource-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(IMAGE_LOADING_FROM_RESOURCE_TIME_CRITERIA)));
    }

    public void test03_ImageLoadingFromBitmapTime() {
        Log.v(TAG, "Start loading images from bitmap");
        long t1 = SystemClock.uptimeMillis();
        Bitmap bitmap = BitmapFactory.decodeResource(getInstrumentation().getContext().getResources(), R.drawable.earth);
        Image image = Image.createFromBitmap(bitmap);
        image.realize(mPresentationEngine);
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "Loading images from bitmap costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.image_loading_from_bitmap-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(IMAGE_LOADING_FROM_BITMAP_TIME_CRITERIA)));
    }

    public void test04_ImageLoadingFromFile() {
        Log.v(TAG, "Start loading images fro file");
        long t1 = SystemClock.uptimeMillis();
        Image image = Image.createFromFile("/sdcard/a3d/earth.bmp");
        image.realize(mPresentationEngine);
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "Loading images fro file costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.image_loading_from_file-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(IMAGE_LOADING_FROM_FILE_TIME_CRITERIA)));
    }

    public void test05_UpdateSystemTextContent() {
        Log.v(TAG, "Start update system text");
        long t1 = SystemClock.uptimeMillis();
        Text sysText = new Text("MediaTek");
        sysText.realize(mPresentationEngine);
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "Update system text costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.update_system_text-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(SYSTEM_TEXT_CONTENT_UPDATE_TIME_CRITERIA)));
    }

    public void test06_UpdateBitmapTextContent() {
        Log.v(TAG, "Start update bitmap text");
        long t1 = SystemClock.uptimeMillis();
        BitmapFont font = new BitmapFont(getInstrumentation().getContext().getResources(), R.raw.bmfont1, R.drawable.bmfont1);
        BitmapText fontText = new BitmapText("MediaTek", font);
        fontText.realize(mPresentationEngine);
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "Update bitmap text costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.update_bitmap_text-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(BITMAP_FONT_TEXT_CONTENT_UPDATE_TIME_CRITERIA)));
    }

    public void test07_Render50Actor() {
        for (int i = 0; i < 50; i++) {
            Container c = new Container();
            mStage.add(c);
        }
        Log.v(TAG, "Start render 50 Actors");
        long t1 = SystemClock.uptimeMillis();
        mStage.realize(mPresentationEngine);
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "Render 50 Actors costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.render_50_actors-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(RENDER_50_ACTORS_TIME_CRITERIA)));
        mStage.removeAll();
        mStage.realize(mPresentationEngine);
    }

    public void test08_Render100Actor() {
        for (int i = 0; i < 100; i++) {
            Container c = new Container();
            mStage.add(c);
        }
        Log.v(TAG, "Start render 100 Actors");
        long t1 = SystemClock.uptimeMillis();
        mStage.realize(mPresentationEngine);
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "Render 100 Actors costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.render_100_actors-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(RENDER_100_ACTORS_TIME_CRITERIA)));
        mStage.removeAll();
        mStage.realize(mPresentationEngine);
    }

    public void test09_Start50Animation() {
        Empty empty1 = new Empty();
        AnimationGroup group = new AnimationGroup();
        Rotation start = new Rotation(0, 0, 0);
        Rotation end = new Rotation(0, 0, 360);

        for (int i = 0; i < 50; i++) {
            PropertyAnimation ani = new PropertyAnimation(empty1, "rotation", start, end);
            ani.setDuration(1000);
            group.add(ani);
        }
        Log.v(TAG, "start 50 animations");
        long t1 = SystemClock.uptimeMillis();
        group.start();
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "start 50 animations costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.start_50_animations-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(START_50_ANIMATIONS_TIME_CRITERIA)));
        group.stop();
    }

    public void test10_Start100Animation() {
        Empty empty1 = new Empty();
        AnimationGroup group = new AnimationGroup();
        Rotation start = new Rotation(0, 0, 0);
        Rotation end = new Rotation(0, 0, 360);

        for (int i = 0; i < 100; i++) {
            PropertyAnimation ani = new PropertyAnimation(empty1, "rotation", start, end);
            ani.setDuration(1000);
            group.add(ani);
        }
        Log.v(TAG, "start 100 animations");
        long t1 = SystemClock.uptimeMillis();
        group.start();
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "start 100 animations costs: " + t2);

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.start_100_animations-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(START_100_ANIMATIONS_TIME_CRITERIA)));
        group.stop();
    }

    public void test11_ScreenShot() {
        mStageView.pauseRendering();
        Log.v(TAG, "Start screenshot");
        long t1 = SystemClock.uptimeMillis();
        mStageView.getScreenShot();
        long t2 = SystemClock.uptimeMillis() - t1;
        mStageView.resumeRendering();
        Log.v(TAG, "screenshot costs: " + t2);
        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.screen_shot-time.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(SCREEN_SHOT_TIME_CRITERIA)));
    }

    public void test12_Render25Landscapes() {
        int width = mStageView.getWidth();
        int height = mStageView.getHeight();

        Container scene = new Container();
        mStageView.getStage().add(scene);
        scene.setPosition(new Point(0, height, 0));
        scene.setScale(new Scale(1, -1, 1));// UI is Y-down, model is Y-up
        scene.setAnchorPoint(new Point(0.f, 0.f, 0.f));

        int rows = 5;
        int columns = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Glo3D landscape = Glo3D.createFromAsset("landscape.glo");
                scene.add(landscape);
                landscape.setPosition(new Point(
                            width / columns * (i + 0.5f),
                            height / rows * (j + 0.5f)));
                landscape.setRotation(new Rotation(
                            360 / columns * i,
                            360 / rows * j,
                            0));
                landscape.setScale(new Scale(10, 10, 10));
            }
        }

        // Sleep for 5 seconds to give plenty of time for the render thread to
        // realize the Actors in the presentation layer.
        sleep(5000);
        Log.v(TAG, "Start render 25 landscapes");
        float fps = measureFps(5000, 1000);
        Log.v(TAG, "25 landscapes render at: " + fps + " fps");

        mStageView.getStage().removeAll();

        final PresentationStubActivity activity = getActivity();
        writePerformanceData(activity, "ngin3d.render_25_landscapes-fps.txt",
                fps);
        assertThat(fps, is(greaterThanOrEqualTo(
                        (float) RENDER_25_LANDSCAPES_FPS_CRITERIA)));
    }

    /**
     * To detect slow calls such as disk read/write and network access during engine
     * initialization and rendering.
     */
    public void test12_detectSlowCalls() {
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyDeath().build());

            mPresentationEngine.uninitialize();
            mPresentationEngine.initialize(480, 800);

            for (int i = 0; i < 100; i++) {
                Container c = new Container();
                mStage.add(c);
            }
            mStage.realize(mPresentationEngine);
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void writePerformanceData(Activity activity, String name, Object data) {
        File dataFile = new File(activity.getDir("perf", Context.MODE_PRIVATE), name);
        dataFile.delete();
        try {
            FileWriter writer = new FileWriter(dataFile);
            writer.write("YVALUE=" + data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupCriteriaValue(Resources res) {
        File dir = Environment.getExternalStorageDirectory();
        File file = new File(dir, "/data/ngin3d-performance.xml");
        if (file.exists()) {
            try{
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(file);
                NodeList nodeList = document.getElementsByTagName("integer");
                ENGINE_INITIALIZATION_TIME_CRITERIA = Integer.parseInt((nodeList.item(0)).getTextContent());
                IMAGE_LOADING_FROM_RESOURCE_TIME_CRITERIA = Integer.parseInt((nodeList.item(1)).getTextContent());
                IMAGE_LOADING_FROM_BITMAP_TIME_CRITERIA = Integer.parseInt((nodeList.item(2)).getTextContent());
                IMAGE_LOADING_FROM_FILE_TIME_CRITERIA = Integer.parseInt((nodeList.item(3)).getTextContent());
                SYSTEM_TEXT_CONTENT_UPDATE_TIME_CRITERIA = Integer.parseInt((nodeList.item(4)).getTextContent());
                BITMAP_FONT_TEXT_CONTENT_UPDATE_TIME_CRITERIA = Integer.parseInt((nodeList.item(5)).getTextContent());
                RENDER_50_ACTORS_TIME_CRITERIA = Integer.parseInt((nodeList.item(6)).getTextContent());
                RENDER_100_ACTORS_TIME_CRITERIA = Integer.parseInt((nodeList.item(7)).getTextContent());
                START_50_ANIMATIONS_TIME_CRITERIA = Integer.parseInt((nodeList.item(8)).getTextContent());
                START_100_ANIMATIONS_TIME_CRITERIA = Integer.parseInt((nodeList.item(9)).getTextContent());
                SCREEN_SHOT_TIME_CRITERIA = Integer.parseInt((nodeList.item(10)).getTextContent());
                RENDER_25_LANDSCAPES_FPS_CRITERIA = Integer.parseInt((nodeList.item(11)).getTextContent());
                return;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        } else {
            ENGINE_INITIALIZATION_TIME_CRITERIA = res.getInteger(R.integer.initializtion);
            IMAGE_LOADING_FROM_RESOURCE_TIME_CRITERIA = res.getInteger(R.integer.loading_from_resource);
            IMAGE_LOADING_FROM_BITMAP_TIME_CRITERIA = res.getInteger(R.integer.loading_from_bitmap);
            IMAGE_LOADING_FROM_FILE_TIME_CRITERIA = res.getInteger(R.integer.loading_from_file);
            SYSTEM_TEXT_CONTENT_UPDATE_TIME_CRITERIA = res.getInteger(R.integer.system_text_update);
            BITMAP_FONT_TEXT_CONTENT_UPDATE_TIME_CRITERIA = res.getInteger(R.integer.bitmap_font_text_update);
            RENDER_50_ACTORS_TIME_CRITERIA = res.getInteger(R.integer.render_50_actors);
            RENDER_100_ACTORS_TIME_CRITERIA = res.getInteger(R.integer.render_100_actors);
            START_50_ANIMATIONS_TIME_CRITERIA = res.getInteger(R.integer.start_50_animations);
            START_100_ANIMATIONS_TIME_CRITERIA = res.getInteger(R.integer.start_100_animations);
            SCREEN_SHOT_TIME_CRITERIA = res.getInteger(R.integer.screenshot);
            RENDER_25_LANDSCAPES_FPS_CRITERIA = res.getInteger(R.integer.render_25_landscapes);
        }
    }
}
