/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.filemanager;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.drm.DrmManagerClient;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class FileInfoAdapter extends BaseAdapter {
    private static final String TAG = "NavigationViewAdapter";
    private static final int DEFAULT_SECONDARY_SIZE_TEXT_COLOR = 0xff414141;
    private static final int DEFAULT_PRIMARY_TEXT_COLOR = Color.BLACK;

    public static final int MODE_NORMAL = 0;
    public static final int MODE_EDIT = 1;

    private int mMode = MODE_NORMAL;

    private Resources mResources = null;
    private LayoutInflater mInflater = null;
    private DrmManagerClient mDrmManagerClient = null;
    private String mCurrentPath = null;
    private List<FileInfo> mFileInfoList = null;

    /**
     * The constructor to construct a navigation view adapter
     * @param context the context of FileManagerBaseActivity
     * @param fileInfoList a list of file information
     * @param greyOut a list of grey out files (grey out when cut operation is performed)
     * @param drmManagerClient DrmManagerClient
     * @param currentDirPath the path of current directory
     * @param listItemMinHeight the minimum list item height
     */
    public FileInfoAdapter(Context context, List<FileInfo> fileInfoList,
            DrmManagerClient drmManagerClient, String currentDirPath) {
        mResources = context.getResources();
        mInflater = LayoutInflater.from(context);
        mFileInfoList = fileInfoList;
        mDrmManagerClient = drmManagerClient;
        mCurrentPath = currentDirPath;
    }

    /**
     * This method gets the count of the items in the name list
     * @return the number of the items
     */
    @Override
    public int getCount() {
        return mFileInfoList.size();
    }

    public void updateCurrentPath(String currentPath, List<FileInfo> fileInfos) {
        mCurrentPath = currentPath;
        mFileInfoList = fileInfos;
        notifyDataSetChanged();
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    /**
     * This method gets the name of the item at the specified position
     * @param pos the position of item
     * @return the FileInfo of the item
     */
    @Override
    public FileInfo getItem(int pos) {
        return mFileInfoList.get(pos);
    }

    /**
     * This method gets the item id at the specified position
     * @param pos the position of item
     * @return the id of the item
     */
    @Override
    public long getItemId(int pos) {
        return pos;
    }

    public void setMode(int mode) {
        if (mMode != mode) {
            mMode = mode;
            notifyDataSetChanged();
        }
    }

    public int getMode() {
        return mMode;
    }

    /**
     * This method sets the item's check boxes
     * @param id the id of the item
     * @param checked the checked state
     */
    public void setChecked(int position, boolean checked) {
        FileInfo checkInfo = getItem(position);
        if (checkInfo != null) {
            FileManagerLog.e(TAG, checkInfo.getFileName() + "position=" + checked);
            checkInfo.setChecked(checked);
        }
    }

    public void setAllItemsChecked(boolean checked) {
        for (FileInfo info : mFileInfoList) {
            info.setChecked(checked);
        }
        notifyDataSetChanged();
    }

    /**
     * This method gets the list of the checked items
     * @return the list of the checked items
     */
    protected ArrayList<String> getCheckedItemsList() {
        ArrayList<String> checkedItemsList = new ArrayList<String>();
        for (FileInfo fileInfo : mFileInfoList) {
            if (fileInfo.isChecked()) {
                checkedItemsList.add(fileInfo.getFileName());
            }
        }
        return checkedItemsList;
    }

    /**
     * This method gets the list of the checked items
     * @return the list of the checked items
     */
    protected ArrayList<FileInfo> getCheckedFileInfos() {
        ArrayList<FileInfo> checkedItemsList = new ArrayList<FileInfo>();
        for (FileInfo fileInfo : mFileInfoList) {
            if (fileInfo.isChecked()) {
                checkedItemsList.add(fileInfo);
            }
        }
        return checkedItemsList;
    }

    /**
     * This method gets the list of the checked items
     * @return the list of the checked items
     */
    protected int getCheckedItemsCount() {
        int count = 0;
        for (FileInfo fileInfo : mFileInfoList) {
            if (fileInfo.isChecked()) {
                count++;
            }
        }
        return count;
    }

    /**
     * This method gets the view for each item to be displayed in the list view
     * @param pos the position of the item
     * @param convertView the view to be shown
     * @param parent the parent view
     * @return the view to be shown
     */
    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        FileViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.adapter_fileinfos, null);
            viewHolder = new FileViewHolder((TextView) convertView
                    .findViewById(R.id.edit_adapter_name), (TextView) convertView
                    .findViewById(R.id.edit_adapter_size), (ImageView) convertView
                    .findViewById(R.id.edit_adapter_img), (CheckBox) convertView
                    .findViewById(R.id.edit_checkbox));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FileViewHolder) convertView.getTag();
        }
        FileInfo currentItem = getItem(pos);
        viewHolder.mName.setText(currentItem.getFileDescription());
        viewHolder.mName.setTextColor(DEFAULT_PRIMARY_TEXT_COLOR); // default
        viewHolder.mSize.setTextColor(DEFAULT_SECONDARY_SIZE_TEXT_COLOR);

        // set size
        if (currentItem.isDirectory()) { // it is a directory
            viewHolder.mSize.setVisibility(View.GONE);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(mResources.getString(R.string.size)).append(" ").append(
                    currentItem.getFileSizeStr());
            viewHolder.mSize.setText(sb.toString());
            viewHolder.mSize.setVisibility(View.VISIBLE);
        }
        // set icon
        setIcon(viewHolder, currentItem);

        switch (mMode) {
        case MODE_NORMAL:
            viewHolder.mCheckBox.setVisibility(View.INVISIBLE);
            break;
        case MODE_EDIT:
            viewHolder.mCheckBox.setVisibility(View.VISIBLE);
            viewHolder.mCheckBox.setChecked(currentItem.isChecked());
            break;
        default:
            break;
        }
        if (currentItem.isCut()) {
            convertView.setAlpha(0.5f);
        } else {
            convertView.setAlpha(1.0f);
        }

        return convertView;
    }

    private void setIcon(FileViewHolder navViewTag, FileInfo info) {
        MountPointHelper mph = MountPointHelper.getInstance();
        final boolean isMountPoint = mph.isMountPointPath(info.getFilePath());
        final boolean isExternalFile = isMountPoint ? false : mph.isExternalFile(info);
        String fileName = info.getFileName();

        int iconId = info.getFileIconResId();
        Bitmap bgdBmp = null;
        if (OptionsUtil.isDrmSupported() && info.isDrmFile()) {
            int actionId = info.getFileDrmActionId();
            FileManagerLog.d(TAG, "A drm file; actionId: " + actionId);
            if (actionId == FileManagerOperationActivity.NOT_DRM_FILE) {
                bgdBmp = BitmapFactory.decodeResource(mResources, iconId);
            } else {
                FileManagerLog.d(TAG, "Set icon for a drm file; actionId: " + actionId);
                bgdBmp = mDrmManagerClient.overlayDrmIconSkew(mResources, mCurrentPath + "/"
                        + fileName, actionId, iconId);

                if (bgdBmp == null) {
                    FileManagerLog.d(TAG, "bgbBmp is null");
                    // drm getMethod() failed, bgdBmp is null because it is not a correct .dcf file
                    bgdBmp = BitmapFactory.decodeResource(mResources, iconId);
                }
            }
        } else {
            bgdBmp = BitmapFactory.decodeResource(mResources, iconId);
        }
        if (isExternalFile) {
            bgdBmp = EditUtility.createSDCardIcon(mResources, bgdBmp);
        }
        navViewTag.mIcon.setImageBitmap(bgdBmp);
    }

    static class FileViewHolder {
        protected TextView mName;
        protected TextView mSize;
        protected ImageView mIcon;
        protected CheckBox mCheckBox;

        /**
         * The constructor to construct an edit view tag
         * @param name the name view of the item
         * @param size the size view of the item
         * @param icon the icon view of the item
         * @param box the check box view of the item
         */
        public FileViewHolder(TextView name, TextView size, ImageView icon, CheckBox box) {
            this.mName = name;
            this.mSize = size;
            this.mIcon = icon;
            this.mCheckBox = box;
        }
    }
}