package com.mediatek.ngin3d.demo;

import android.os.Bundle;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.Cube;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.android.StageActivity;
import com.mediatek.ngin3d.demo.R;

import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple demo comprising one static textured 3D cuboid.
 * This code is used to verify rendering of 3D objects when using
 * conventional coordinates and perspective projection.
 *
 * The aim with this demo is to compare / verify the A3M
 * rendering against a 3rd-party 3D renderer^ using the
 * same geometry definition.
 *
 * ^We used POVray - a freeware 3D raytracing renderer.
 */
public class CuboidDemo extends StageActivity {

    // Clipping distances for camera
    private static final float Z_NEAR = 2.f;
    private static final float Z_FAR = 2000.f;

    // Note app uses PERSPECTIVE mode so coord system is Right-handed
    private static final float CAM_Z = 1111.f;
    private static final float CAM_X = 240.f;
    private static final float CAM_Y = 400.f;

    // Narrow FOV required because camera is very far away
    private static final float CAM_FOV = 25.0f;

    private static final float SLABX = 0.f;
    private static final float SLABY = 0.f;
    private static final float SLABZ = 0.f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Container myWorld = new Container();

        // The unit cuboid is sized to match the size of the weather field.
        final Cube slab = makeCube(new Scale(292.3f, 55.82f, 185.6f));

        // 3 long thin cuboids are used to create crude visual axes.
        final Cube xaxis = makeCube(new Scale(2000.f, 1f, 1f));
        final Cube yaxis = makeCube(new Scale(1f, 2000.f, 1f));
        final Cube zaxis = makeCube(new Scale(1f, 1f, 2000.f));

        slab.setPosition(new Point(SLABX, SLABY, SLABZ));

        myWorld.add(xaxis, yaxis, zaxis, slab);

        //mStage.setStereo3D( true, 1200.0f/30.0f ); // focus on ~centre of slab

        mStage.setProjection( Stage.PERSPECTIVE, Z_NEAR, Z_FAR, CAM_Z );
        mStage.setCameraFov(CAM_FOV);

        mStage.setCamera(
            new Point(CAM_X, CAM_Y, CAM_Z),
            // Aim the camera at the centre of the cuboid
            new Point(SLABX, SLABY, SLABZ) );

        mStage.add( myWorld );

    }

    private Cube makeCube( Scale size ) {
        Cube newCube = Cube.createFromResource(getResources(),
            R.drawable.fourcolour);
        newCube.setPosition(new Point(0.f, 0.f, 0.f));
        newCube.setScale( size );
        newCube.setAnchorPoint( new Point(0.f, 0.f, 0.f) );

        return newCube;
    }
}
