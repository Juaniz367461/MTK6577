/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.util.MtkLog;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;

public class ClusterAlbumSet extends MediaSet implements ContentListener {
    private static final String TAG = "ClusterAlbumSet";
    private GalleryApp mApplication;
    private MediaSet mBaseSet;
    private int mKind;
    private ArrayList<ClusterAlbum> mAlbums = new ArrayList<ClusterAlbum>();
    private boolean mFirstReloadDone;
    // when deleting cluster albums, unnecessary reload might cause
    // the content of ClusterAlbums to be cleared during deletion,
    // and finally lead to unsuccessful deletion. We use this flag
    // to temporarily (partly) disable the reload of ClusterAlbum.
    private static boolean sSkipReload = false;

    public ClusterAlbumSet(Path path, GalleryApp application,
            MediaSet baseSet, int kind) {
        super(path, INVALID_DATA_VERSION);
        mApplication = application;
        mBaseSet = baseSet;
        mKind = kind;
        baseSet.addContentListener(this);
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public String getName() {
        return mBaseSet.getName();
    }

    @Override
    public long reload() {
        MtkLog.d(TAG, "reload");
        if (mBaseSet.reload() > mDataVersion) {
            // we mark this out and use updateClusters() directly to make sure everything is refreshed from here.
            /*if (mFirstReloadDone) {
                updateClustersContents();
            } else {
                updateClusters();
                mFirstReloadDone = true;
            }*/
            // do NOT skip base set reload; this will make sure that the dirty flag in ChangeNotifier is cleared normally
            if (!sSkipReload) {
                updateClusters();
                mDataVersion = nextVersionNumber();
            }
        }
        return mDataVersion;
    }

    public void onContentDirty() {
        notifyContentChanged();
    }

    private void updateClusters() {
        //we clear all last results, backwardly
        for (int i = mAlbums.size() - 1; i >= 0; i--) {
            mAlbums.get(i).setMediaItems(new ArrayList<Path>());
        }

        mAlbums.clear();
        Clustering clustering;
        Context context = mApplication.getAndroidContext();
        switch (mKind) {
            case ClusterSource.CLUSTER_ALBUMSET_TIME:
                clustering = new TimeClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_LOCATION:
                clustering = new LocationClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_TAG:
                clustering = new TagClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_FACE:
                clustering = new FaceClustering(context);
                break;
            default: /* CLUSTER_ALBUMSET_SIZE */
                clustering = new SizeClustering(context);
                break;
        }

        clustering.run(mBaseSet);
        int n = clustering.getNumberOfClusters();
        DataManager dataManager = mApplication.getDataManager();
        for (int i = 0; i < n; i++) {
            Path childPath;
            String childName = clustering.getClusterName(i);
            if (mKind == ClusterSource.CLUSTER_ALBUMSET_TAG) {
                //childPath = mPath.getChild(Uri.encode(childName));
                //This may be a very bad solution, but temporily,
                //as there is no picasa supported in Gallery2, this
                //solution is acceptable.
                //Be very careful when Picasa feature comes in
                //And check if there are better solution for DrmInclusion
                childPath = mPath.getChild(i);
            } else if (mKind == ClusterSource.CLUSTER_ALBUMSET_SIZE) {
                long minSize = ((SizeClustering) clustering).getMinSize(i);
                childPath = mPath.getChild(minSize);
            } else {
                childPath = mPath.getChild(i);
            }
            ClusterAlbum album = (ClusterAlbum) dataManager.peekMediaObject(
                        childPath);
            if (album == null) {
                album = new ClusterAlbum(childPath, dataManager, this);
            } else {
            	album.updateDataVersion();
            }
            album.setMediaItems(clustering.getCluster(i));
            album.setName(childName);
            album.setCoverMediaItem(clustering.getClusterCover(i));
            mAlbums.add(album);
            ArrayList<Path> items = album.getMediaItems();
            MtkLog.d(TAG, "updateClusters: album [" + album.getPath() + "][" + album.getName() + "] with " + (items == null ? "0" : items.size()) + " items added");
            
        }
    }

    private void updateClustersContents() {
        final HashSet<Path> existing = new HashSet<Path>();
        mBaseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            public void consume(int index, MediaItem item) {
                existing.add(item.getPath());
            }
        });

        int n = mAlbums.size();

        // The loop goes backwards because we may remove empty albums from
        // mAlbums.
        for (int i = n - 1; i >= 0; i--) {
            ArrayList<Path> oldPaths = mAlbums.get(i).getMediaItems();
            ArrayList<Path> newPaths = new ArrayList<Path>();
            int m = oldPaths.size();
            for (int j = 0; j < m; j++) {
                Path p = oldPaths.get(j);
                if (existing.contains(p)) {
                    newPaths.add(p);
                }
            }
            mAlbums.get(i).setMediaItems(newPaths);
            if (newPaths.isEmpty()) {
                mAlbums.remove(i);
            }
        }
    }
    
    public static void setSkipReload(boolean skip) {
        MtkLog.d(TAG, "setSkipReload: " + skip);
        sSkipReload = skip;
    }
    
    public static boolean getSkipReload() {
        MtkLog.d(TAG, "getSkipReload: " + sSkipReload);
        return sSkipReload;
    }
}
