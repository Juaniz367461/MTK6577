package com.mediatek.ngin3d.tests;

import android.graphics.Canvas;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import com.mediatek.ngin3d.*;
import com.mediatek.ngin3d.android.StageView;
import com.mediatek.ngin3d.presentation.Graphics2d;
import com.mediatek.ngin3d.presentation.Graphics3d;
import com.mediatek.ngin3d.presentation.ImageDisplay;
import com.mediatek.ngin3d.presentation.ImageSource;
import com.mediatek.ngin3d.presentation.Model3d;
import com.mediatek.ngin3d.presentation.Presentation;
import com.mediatek.ngin3d.presentation.PresentationEngine;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

public class PresentationTest extends ActivityInstrumentationTestCase2<PresentationStubActivity> {

    public PresentationTest() {
        super("com.mediatek.ngin3d.tests", PresentationStubActivity.class);
    }

    private PresentationStubActivity mActivity;
    private StageView mStageView;
    private PresentationEngine mPE;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mStageView = mActivity.getStageView();
        mPE = mStageView.getPresentationEngine();
        mStageView.waitSurfaceReady();

    }

    private int waitForFrames(PresentationStubActivity activity, int frames) throws InterruptedException {

        mStageView.setRenderMode(StageView.RENDERMODE_CONTINUOUSLY);

        for (int i = 0 ; i < frames ; i++) {
            mPE.getFrameInterval();
        }
         mStageView.setRenderMode(StageView.RENDERMODE_WHEN_DIRTY);
        return 0;
    }

    @MediumTest
    public void testBasicEngineFunctionalities() throws ExecutionException, InterruptedException {

         FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {
            public Boolean call() {
                assertTrue("PresentationEngine should be ready", mPE.isReady());

                Presentation container = mPE.createContainer();
                assertNotNull(container);
                container.initialize(this);

                Presentation empty = mPE.createEmpty();
                assertNotNull(empty);
                empty.initialize(this);
                container.addChild(empty);

                Graphics2d g2d = mPE.createGraphics2d();
                assertNotNull(g2d);
                g2d.initialize(this);
                container.addChild(g2d);
                Canvas canvas = g2d.beginDraw(256, 256, 0);
                assertNotNull(canvas);
                canvas.drawRGB(0, 0, 0);
                g2d.endDraw();

                Graphics3d g3d = mPE.createGraphics3d();
                assertNotNull(g3d);
                g3d.initialize(this);
                container.addChild(g3d);

                ImageDisplay image = mPE.createImageDisplay();
                assertNotNull(image);
                image.initialize(this);
                container.addChild(image);

                Model3d m3d = mPE.createModel3d(Model3d.SPHERE);
                assertNotNull(m3d);
                m3d.initialize(this);
                container.addChild(m3d);

                mPE.dump();

                return true;
            }
        });

        mStageView.runInGLThread(task);
        boolean result = task.get().booleanValue();
        assertTrue("The test runs successfully", result);
    }

    @MediumTest
    public void testImageDisplay() throws ExecutionException, InterruptedException {

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {
            public Boolean call() {
                Presentation container = mPE.createContainer();
                assertNotNull(container);
                container.initialize(this);

                ImageDisplay image = mPE.createImageDisplay();
                image.initialize(this);
                container.addChild(image);

                for (int i = 0; i < 100; i++) {
                    image.setImageSource(
                        new ImageSource(ImageSource.RES_ID,
                            new ImageDisplay.Resource(mActivity.getResources(), R.drawable.android)));
                    image.setImageSource(
                        new ImageSource(ImageSource.RES_ID,
                            new ImageDisplay.Resource(mActivity.getResources(), R.drawable.danger)));
                }

                // TODO: check memory usage here to ensure no memory leak

                assertEquals(Color.WHITE, image.getColor());
                assertEquals(255, image.getOpacity());
                assertEquals(new Dimension(48, 48), image.getSize());
                assertEquals(new Dimension(48, 48), image.getSourceDimension());

                image.uninitialize();

                return true;
            }
        });

        mStageView.runInGLThread(task);
        boolean result = task.get().booleanValue();
        assertTrue("The test runs successfully", result);
    }

    @MediumTest
    public void testThreadSynchronization() {

        final Stage stage = mStageView.getStage();

        final Random rnd = new Random(System.currentTimeMillis());
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; ++i) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < 200; ++j) {
                        try {
                            Thread.sleep(rnd.nextInt(10));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                        Actor a = new Empty();
                        stage.add(a);
                        stage.getChildrenCount();
                        stage.getChild(0);
                        stage.remove(a);
                    }
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < threads.length; ++i) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @MediumTest
    public void testImageSrcRect() throws InterruptedException {

         final Stage stage = mStageView.getStage();

        Image photo = Image.createFromResource(mActivity.getResources(), R.drawable.photo_01);
        stage.add(photo);

        photo.getAlphaSource();
        photo.setAlphaSource(Plane.FROM_VERTEX_COLOR);
        waitForFrames(mActivity, 1);
        photo.setAlphaSource(Plane.FROM_TEXEL);
        waitForFrames(mActivity, 1);

        Box box;

        box = new Box(0, 0, 320, 214);
        photo.setSourceRect(box);
        assertEquals(box, photo.getSourceRect());
        waitForFrames(mActivity, 1);

        box = new Box(0, 0, 160, 100);
        photo.setSourceRect(box);
        assertEquals(box, photo.getSourceRect());
        waitForFrames(mActivity, 1);

        box = new Box(0, 0, 0, 0);
        photo.setSourceRect(box);
        assertEquals(box, photo.getSourceRect());
        waitForFrames(mActivity, 1);

        box = new Box(0, 0, 10000, 10000);
        photo.setSourceRect(box);
        assertEquals(box, photo.getSourceRect());
        waitForFrames(mActivity, 1);

        box = new Box(-1, -1, 0, 0);
        photo.setSourceRect(box);
        assertEquals(box, photo.getSourceRect());
        waitForFrames(mActivity, 1);

        Dimension dim;

        dim = new Dimension(10, 10);
        photo.setSize(dim);
        waitForFrames(mActivity, 1);

        dim = new Dimension(0, 0);
        photo.setSize(dim);
        waitForFrames(mActivity, 1);

        dim = new Dimension(100000, 100000);
        photo.setSize(dim);
        waitForFrames(mActivity, 1);

        try {
            dim = new Dimension(-1, -1);
            photo.setSize(dim);
            waitForFrames(mActivity, 1);
            fail("Should throw exception for negative value");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @MediumTest
    public void testHitTest() throws InterruptedException {

        final Stage stage = mStageView.getStage();

        Point pos = new Point(200, 120);
        Image photo = Image.createFromResource(mActivity.getResources(), R.drawable.photo_01);
        stage.add(photo);

        // By default, rects point away from the camera, so we must rotate it.
        photo.setPosition(pos);
        photo.setRotation(new Rotation(0, 180, 0));

        waitForFrames(mActivity, 10);

        assertEquals(photo, stage.hitTest(pos));
        assertEquals(photo, stage.hitTest(new Point(199, 119)));
        assertEquals(photo, stage.hitTest(new Point(41, 20)));
        assertEquals(photo, stage.hitTest(new Point(201, 121)));
        assertEquals(photo, stage.hitTest(new Point(359, 220)));
        assertEquals(null, stage.hitTest(new Point(0, 0)));

        // Test after rotation
        photo.setRotation(new Rotation(0, 180, 45));

        // Test after scale

        // Test after visible
    }

    @MediumTest
    public void testRebuildingPresentation() throws InterruptedException {

         final Stage stage = mStageView.getStage();

        stage.add(new Empty());
        stage.add(new Empty());
        stage.add(new Empty());

        waitForFrames(mActivity, 10);
        assertThat(stage.getPresentation().getChildrenCount(), is(greaterThanOrEqualTo(3)));

        stage.unrealize();
        waitForFrames(mActivity, 5);
        stage.realize(mPE);
        waitForFrames(mActivity, 5);
        assertThat(stage.getPresentation().getChildrenCount(), is(greaterThanOrEqualTo(3)));
    }

    @MediumTest
    public void testNormalizedPosition() throws InterruptedException {

        final Stage stage = mStageView.getStage();

        Empty actor = new Empty();
        actor.setPosition(new Point(0.5f, 0.5f, true));
        stage.add(actor);
        waitForFrames(mActivity, 3);

        Point actualPos = actor.getPresentation().getPosition(true);
        assertEquals(0.5f, actualPos.x);
        assertEquals(0.5f, actualPos.y);
    }

}
