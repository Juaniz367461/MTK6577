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

package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Container class for CAT GET INPUT, GET IN KEY commands parameters.
 *
 */
public class Input implements Parcelable {
	public String text;
	public String defaultText;
	public Bitmap icon;
	public int minLen;
	public int maxLen;
	public boolean ucs2;
	public boolean packed;
	public boolean digitOnly;
	public boolean echo;
	public boolean yesNo;
	public boolean helpAvailable;
	public Duration duration;
	//Add by Huibin Mao Mtk80229
	//ICS Migration start
	public boolean iconSelfExplanatory;    
	//ICS Migration end

	Input() {
		text = "";
		defaultText = null;
		icon = null;
		minLen = 0;
		maxLen = 1;
		ucs2 = false;
		packed = false;
		digitOnly = false;
		echo = false;
		yesNo = false;
		helpAvailable = false;
		duration = null;
		//Add by Huibin Mao Mtk80229
		//ICS Migration start
		iconSelfExplanatory = false;        
		//ICS Migration end
	}

	private Input(Parcel in) {
		text = in.readString();
		defaultText = in.readString();
		icon = in.readParcelable(null);
		minLen = in.readInt();
		maxLen = in.readInt();
		ucs2 = in.readInt() == 1 ? true : false;
		packed = in.readInt() == 1 ? true : false;
		digitOnly = in.readInt() == 1 ? true : false;
		echo = in.readInt() == 1 ? true : false;
		yesNo = in.readInt() == 1 ? true : false;
		helpAvailable = in.readInt() == 1 ? true : false;
		duration = in.readParcelable(null);
		//Add by Huibin Mao Mtk80229
		//ICS Migration start
		iconSelfExplanatory = in.readInt() == 1 ? true : false;        
		//ICS Migration end
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(text);
		dest.writeString(defaultText);
		dest.writeParcelable(icon, 0);
		dest.writeInt(minLen);
		dest.writeInt(maxLen);
		dest.writeInt(ucs2 ? 1 : 0);
		dest.writeInt(packed ? 1 : 0);
		dest.writeInt(digitOnly ? 1 : 0);
		dest.writeInt(echo ? 1 : 0);
		dest.writeInt(yesNo ? 1 : 0);
		dest.writeInt(helpAvailable ? 1 : 0);
		dest.writeParcelable(duration, 0);
		//Add by Huibin Mao Mtk80229
		//ICS Migration start
		dest.writeInt(iconSelfExplanatory ? 1 : 0);	        
		//ICS Migration end
	}

	public static final Parcelable.Creator<Input> CREATOR = new Parcelable.Creator<Input>() {
		public Input createFromParcel(Parcel in) {
			return new Input(in);
		}

		public Input[] newArray(int size) {
			return new Input[size];
		}
	};

	boolean setIcon(Bitmap Icon) { return true; }
}