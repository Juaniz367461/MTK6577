/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_SURFACE_FLINGER_H
#define ANDROID_SURFACE_FLINGER_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Atomic.h>
#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <utils/RefBase.h>
#include <utils/SortedVector.h>
#include <utils/threads.h>

#include <binder/BinderService.h>
#include <binder/IMemory.h>

#include <ui/PixelFormat.h>
#include <surfaceflinger/IGraphicBufferAlloc.h>
#include <surfaceflinger/ISurfaceComposer.h>
#include <surfaceflinger/ISurfaceComposerClient.h>

#include "Barrier.h"
#include "Layer.h"

#include "MessageQueue.h"

#include "SurfaceFlingerWatchDog.h"

namespace android {

// ---------------------------------------------------------------------------

class Client;
class DisplayHardware;
class Layer;
class LayerDim;
class LayerScreenshot;
class SFWatchDog;
struct surface_flinger_cblk_t;

#define LIKELY( exp )       (__builtin_expect( (exp) != 0, true  ))
#define UNLIKELY( exp )     (__builtin_expect( (exp) != 0, false ))

// ---------------------------------------------------------------------------

class Client : public BnSurfaceComposerClient
{
public:
        Client(const sp<SurfaceFlinger>& flinger);
        ~Client();

    status_t initCheck() const;

    // protected by SurfaceFlinger::mStateLock
    size_t attachLayer(const sp<LayerBaseClient>& layer);
    void detachLayer(const LayerBaseClient* layer);
    sp<LayerBaseClient> getLayerUser(int32_t i) const;

private:
    // ISurfaceComposerClient interface
    virtual sp<ISurface> createSurface(
            surface_data_t* params, const String8& name,
            DisplayID display, uint32_t w, uint32_t h,PixelFormat format,
            uint32_t flags);
    virtual status_t destroySurface(SurfaceID surfaceId);
    virtual status_t onTransact(
        uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags);

    // constant
    sp<SurfaceFlinger> mFlinger;

    // protected by mLock
    DefaultKeyedVector< size_t, wp<LayerBaseClient> > mLayers;
    size_t mNameGenerator;

    // thread-safe
    mutable Mutex mLock;
};

class GraphicBufferAlloc : public BnGraphicBufferAlloc
{
public:
    GraphicBufferAlloc();
    virtual ~GraphicBufferAlloc();
    virtual sp<GraphicBuffer> createGraphicBuffer(uint32_t w, uint32_t h,
        PixelFormat format, uint32_t usage, status_t* error);
};

// ---------------------------------------------------------------------------

class GraphicPlane
{
public:
    static status_t orientationToTransfrom(int orientation, int w, int h,
            Transform* tr);

                                GraphicPlane();
                                ~GraphicPlane();

        bool                    initialized() const;

        void                    setDisplayHardware(DisplayHardware *);
        status_t                setOrientation(int orientation);
        int                     getOrientation() const { return mOrientation; }
        int                     getWidth() const;
        int                     getHeight() const;
        int                     getDisplayOrientation() const { return mDisplayOrientation; }
        float                  getDisplayWidth() const { return mDisplayWidth; }
        float                  getDisplayHeight() const { return mDisplayHeight; }

        const DisplayHardware&  displayHardware() const;
        DisplayHardware&        editDisplayHardware();
        const Transform&        transform() const;
        EGLDisplay              getEGLDisplay() const;

private:
                                GraphicPlane(const GraphicPlane&);
        GraphicPlane            operator = (const GraphicPlane&);

        DisplayHardware*        mHw;
        Transform               mGlobalTransform;
        Transform               mDisplayTransform;
        int                     mOrientation;
        int                     mDisplayOrientation;
        float                   mDisplayWidth;
        float                   mDisplayHeight;
        int                     mWidth;
        int                     mHeight;
};

// ---------------------------------------------------------------------------

enum {
    eTransactionNeeded      = 0x01,
    eTraversalNeeded        = 0x02
};

class SurfaceFlinger :
        public BinderService<SurfaceFlinger>,
        public BnSurfaceComposer,
        public IBinder::DeathRecipient,
        protected Thread
{
public:
    static char const* getServiceName() { return "SurfaceFlinger"; }

                    SurfaceFlinger();
    virtual         ~SurfaceFlinger();
            void    init();

    virtual status_t onTransact(
        uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags);

    virtual status_t dump(int fd, const Vector<String16>& args);

    // ISurfaceComposer interface
    virtual sp<ISurfaceComposerClient>  createConnection();
    virtual sp<IGraphicBufferAlloc>     createGraphicBufferAlloc();
    virtual sp<IMemoryHeap>             getCblk() const;
    virtual void                        bootFinished();
    virtual status_t                    freezeDisplay(DisplayID dpy, uint32_t flags);
    virtual status_t                    unfreezeDisplay(DisplayID dpy, uint32_t flags);
    virtual void                        setTransactionState(const Vector<ComposerState>& state,
                                                            int orientation, uint32_t flags);
    virtual int                         setOrientation(DisplayID dpy, int orientation, uint32_t flags);
    virtual bool                        authenticateSurfaceTexture(const sp<ISurfaceTexture>& surface) const;

    virtual status_t captureScreen(DisplayID dpy,
            sp<IMemoryHeap>* heap,
            uint32_t* width, uint32_t* height,
            PixelFormat* format, uint32_t reqWidth, uint32_t reqHeight,
            uint32_t minLayerZ, uint32_t maxLayerZ);

    virtual status_t                    turnElectronBeamOff(int32_t mode);
    virtual status_t                    turnElectronBeamOn(int32_t mode);

            void                        screenReleased(DisplayID dpy);
            void                        screenAcquired(DisplayID dpy);

            status_t renderScreenToTexture(DisplayID dpy,
                    GLuint* textureName, GLfloat* uOut, GLfloat* vOut);
            status_t renderScreenToTextureLocked(DisplayID dpy,
                    GLuint* textureName, GLfloat* uOut, GLfloat* vOut);

            status_t postMessageAsync(const sp<MessageBase>& msg,
                    nsecs_t reltime=0, uint32_t flags = 0);

            status_t postMessageSync(const sp<MessageBase>& msg,
                    nsecs_t reltime=0, uint32_t flags = 0);

    status_t removeLayer(const sp<LayerBase>& layer);
    status_t addLayer(const sp<LayerBase>& layer);
    status_t invalidateLayerVisibility(const sp<LayerBase>& layer);
    void invalidateHwcGeometry();

    sp<Layer> getLayer(const sp<ISurface>& sur) const;

    GLuint getProtectedTexName() const { return mProtectedTexName; }


    class MessageDestroyGLTexture : public MessageBase {
        GLuint texture;
    public:
        MessageDestroyGLTexture(GLuint texture) : texture(texture) { }
        virtual bool handler() {
            glDeleteTextures(1, &texture);
            return true;
        }
    };


private:
    // DeathRecipient interface
    virtual void binderDied(const wp<IBinder>& who);

private:
    friend class Client;
    friend class LayerBase;
    friend class LayerBaseClient;
    friend class Layer;
    friend class DisplayHardware;
    friend class SFWatchDog;
    friend class HWComposer;

    sp<ISurface> createSurface(
            ISurfaceComposerClient::surface_data_t* params,
            const String8& name,
            const sp<Client>& client,
            DisplayID display, uint32_t w, uint32_t h, PixelFormat format,
            uint32_t flags);

    sp<Layer> createNormalSurface(
            const sp<Client>& client, DisplayID display,
            uint32_t w, uint32_t h, uint32_t flags,
            PixelFormat& format);

    sp<LayerDim> createDimSurface(
            const sp<Client>& client, DisplayID display,
            uint32_t w, uint32_t h, uint32_t flags);

    sp<LayerScreenshot> createScreenshotSurface(
            const sp<Client>& client, DisplayID display,
            uint32_t w, uint32_t h, uint32_t flags);

    status_t removeSurface(const sp<Client>& client, SurfaceID sid);
    status_t destroySurface(const wp<LayerBaseClient>& layer);
    uint32_t setClientStateLocked(const sp<Client>& client, const layer_state_t& s);

    class LayerVector : public SortedVector< sp<LayerBase> > {
    public:
        LayerVector() { }
        LayerVector(const LayerVector& rhs) : SortedVector< sp<LayerBase> >(rhs) { }
        virtual int do_compare(const void* lhs, const void* rhs) const {
            const sp<LayerBase>& l(*reinterpret_cast<const sp<LayerBase>*>(lhs));
            const sp<LayerBase>& r(*reinterpret_cast<const sp<LayerBase>*>(rhs));
            // sort layers by Z order
            uint32_t lz = l->currentState().z;
            uint32_t rz = r->currentState().z;
            // then by sequence, so we get a stable ordering
            return (lz != rz) ? (lz - rz) : (l->sequence - r->sequence);
        }
    };

    struct State {

        // [mtk03712] for S3D composing phase
        //----------------------------------------------------------------------------
        enum {
            eComposing2D        = 0x00,
            eComposingS3D       = 0xFF,
            eComposingS3DLeft   = 0x01,
            eComposingS3DRight  = 0x02,
            eComposingS3DTop    = 0x03,
            eComposingS3DBottom = 0x04,
        };
        //----------------------------------------------------------------------------

        State() {
            orientation = ISurfaceComposer::eOrientationDefault;
            composingOrientation = ISurfaceComposer::eOrientationDefault;
            composingPhase = eComposing2D;
        }

        LayerVector     layersSortedByZ;
        uint8_t         orientation;
        uint8_t         orientationFlags;
        uint8_t         freezeDisplay;

        // [mtk03712] composing for S3D
        //-----------------------------------------------------------------
        uint8_t         composingPhase;
        uint8_t         composingOrientation;
    };

    virtual bool        threadLoop();
    virtual status_t    readyToRun();
    virtual void        onFirstRef();

public:     // hack to work around gcc 4.0.3 bug
    const GraphicPlane&     graphicPlane(int dpy) const;
          GraphicPlane&     graphicPlane(int dpy);
          void              signalEvent();
          void              repaintEverything();
          bool              hasFreezeDisplay(void) { return mFreezeDisplay;}
          bool              isLayerScreenShotVisible(void) { return mLayerScreenShotVisible;}
          void              checkLayerScreenShotVisibility(void) const;

private:
            void        waitForEvent();
            void        handleConsoleEvents();
            void        handleTransaction(uint32_t transactionFlags);
            void        handleTransactionLocked(uint32_t transactionFlags);

            void        computeVisibleRegions(
                            const LayerVector& currentLayers,
                            Region& dirtyRegion,
                            Region& wormholeRegion);

            void        handlePageFlip();
            bool        lockPageFlip(const LayerVector& currentLayers);
            void        unlockPageFlip(const LayerVector& currentLayers);
            void        handleWorkList();
            void        handleRepaint();
            void        postFramebuffer();
            void        setupHardwareComposer(Region& dirtyInOut);
            void        composeSurfaces(const Region& dirty);


            void        setInvalidateRegion(const Region& reg);
            Region      getAndClearInvalidateRegion();

            ssize_t     addClientLayer(const sp<Client>& client,
                    const sp<LayerBaseClient>& lbc);
            status_t    addLayer_l(const sp<LayerBase>& layer);
            status_t    removeLayer_l(const sp<LayerBase>& layer);
            status_t    purgatorizeLayer_l(const sp<LayerBase>& layer);

            uint32_t    getTransactionFlags(uint32_t flags);
            uint32_t    peekTransactionFlags(uint32_t flags);
            uint32_t    setTransactionFlags(uint32_t flags);
            void        commitTransaction();


            status_t captureScreenImplLocked(DisplayID dpy,
                    sp<IMemoryHeap>* heap,
                    uint32_t* width, uint32_t* height, PixelFormat* format,
                    uint32_t reqWidth, uint32_t reqHeight,
                    uint32_t minLayerZ, uint32_t maxLayerZ);

            status_t turnElectronBeamOffImplLocked(int32_t mode);
            status_t turnElectronBeamOnImplLocked(int32_t mode);
            status_t electronBeamOffAnimationImplLocked();
            status_t electronBeamOnAnimationImplLocked();

            void        debugFlashRegions();
            void        debugShowFPS() const;
            void        drawWormhole() const;


    mutable     MessageQueue    mEventQueue;

                // access must be protected by mStateLock
    mutable     Mutex                   mStateLock;
                State                   mCurrentState;
    volatile    int32_t                 mTransactionFlags;
                Condition               mTransactionCV;
                SortedVector< sp<LayerBase> > mLayerPurgatory;
                bool                    mTransationPending;
                Vector< sp<LayerBase> > mLayersPendingRemoval;

                // protected by mStateLock (but we could use another lock)
                GraphicPlane                mGraphicPlanes[1];
                bool                        mLayersRemoved;
                DefaultKeyedVector< wp<IBinder>, wp<Layer> > mLayerMap;

                // access must be protected by mInvalidateLock
    mutable     Mutex                       mInvalidateLock;
                Region                      mInvalidateRegion;

                // constant members (no synchronization needed for access)
                sp<IMemoryHeap>             mServerHeap;
                surface_flinger_cblk_t*     mServerCblk;
                GLuint                      mWormholeTexName;
                GLuint                      mProtectedTexName;
                nsecs_t                     mBootTime;

                // Can only accessed from the main thread, these members
                // don't need synchronization
                State                       mDrawingState;
                Region                      mDirtyRegion;
                Region                      mDirtyRegionRemovedLayer;
                Region                      mSwapRegion;
                Region                      mWormholeRegion;
                bool                        mVisibleRegionsDirty;
                bool                        mHwWorkListDirty;
                bool                        mFreezeDisplay;
                bool                        mNeedUpdateFreezeDisplayState;
                int32_t                     mElectronBeamAnimationMode;
                Vector< sp<LayerBase> >     mVisibleLayersSortedByZ;
                mutable bool                mLayerScreenShotVisible;


                // don't use a lock for these, we don't care
                int                         mDebugRegion;
                int                         mDebugBackground;
                int                         mDebugDDMS;
                int                         mDebugDisableHWC;
                int                         mDebugDisableTransformHint;
                volatile nsecs_t            mDebugInSwapBuffers;
                nsecs_t                     mLastSwapBufferTime;
                volatile nsecs_t            mDebugInTransaction;
                nsecs_t                     mLastTransactionTime;
                bool                        mBootFinished;
                bool                        mBootAnimationEnabled;

                // these are thread safe
    mutable     Barrier                     mReadyToRunBarrier;


                // protected by mDestroyedLayerLock;
    mutable     Mutex                       mDestroyedLayerLock;
                Vector<LayerBase const *>   mDestroyedLayers;

                // atomic variables
                enum {
                    eConsoleReleased = 1,
                    eConsoleAcquired = 2
                };
   volatile     int32_t                     mConsoleSignals;

   // only written in the main thread, only read in other threads
   volatile     int32_t                     mSecureFrameBuffer;

// [mtk04189] necessary data members and APIs used for optimization
//------------------------------------------------------------------------------------------
private:
                void        checkLayersSwapRequired(int fbLayerCount);

                bool        mLayersSwapRequired;
                bool        mBusySwap;
                bool        mLogRepaint;

                // added by Ryan
                // for run-time enable property
                void        triggerPropertySet();


                // added by Ryan
                // for enabling slow motion
                uint32_t    mDelayTime;

                // for drawing debug line
                bool        mDrawLine_G3D;
                bool        mDrawLine_Aux;
                bool        mDrawLine_ScreenShot;
                bool        mDrawLine_Overlay;
                bool        mDebugOEX;

public:
                bool        getAndClearLayersSwapRequired();

private:
                sp<SFWatchDog> mWatchDog;
//------------------------------------------------------------------------------------------

// [mtk03712] for S3D composing phase
//------------------------------------------------------------------------------------------
private:
                bool        mDebugS3D;
                void        handleComposingS3DSetting();
                void        clearScreenRegion(const Region& r);
                void        clearFB(const Region& r);
                void        composeSurfacesS3D(const Region& dirty);

public:
                status_t    setScissor(const Rect& r) const;
//------------------------------------------------------------------------------------------
// [mtk04189] Project Configuration
public:
    struct projectConfig_t {
        bool mtk_tvout_support;
        bool mtk_hdmi_support;
        bool mtk_s3d_support;
    };

    static projectConfig_t sMtkConfig;
};

// ---------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_SURFACE_FLINGER_H
