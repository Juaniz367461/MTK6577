/*
 * Copyright (C) 2009 The Android Open Source Project
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

#include <media/mediascanner.h>

#include <utils/StringArray.h>

#include "autodetect.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"
#ifndef ANDROID_DEFAULT_CODE
#undef LOG_TAG
#define LOG_TAG "MediaScannerClient"
#include "utils/Log.h"
#endif

namespace android {

MediaScannerClient::MediaScannerClient()
    :   mNames(NULL),
        mValues(NULL),
        mLocaleEncoding(kEncodingNone)
{
#ifndef ANDROID_DEFAULT_CODE 
    LOGI("MediaScannerClient Cons\n"); 
 #endif  
}

MediaScannerClient::~MediaScannerClient()
{
#ifndef ANDROID_DEFAULT_CODE
    LOGI("MediaScannerClient ~Decons\n"); 
#endif   
    delete mNames;
    delete mValues;
}

void MediaScannerClient::setLocale(const char* locale)
{
#ifndef ANDROID_DEFAULT_CODE
    LOGI("MediaScannerClient +setLocale locale:%s \n",locale);   
#endif 
    if (!locale) return;

    if (!strncmp(locale, "ja", 2))
        mLocaleEncoding = kEncodingShiftJIS;
    else if (!strncmp(locale, "ko", 2))
        mLocaleEncoding = kEncodingEUCKR;
    else if (!strncmp(locale, "zh", 2)) {
        if (!strcmp(locale, "zh_CN")) {
            // simplified chinese for mainland China
            mLocaleEncoding = kEncodingGBK;
        } else {
            // assume traditional for non-mainland Chinese locales (Taiwan, Hong Kong, Singapore)
            mLocaleEncoding = kEncodingBig5;
        }
    }
#ifndef ANDROID_DEFAULT_CODE
	else if(!strncmp(locale, "es", 2) || !strncmp(locale, "ru", 2) || !strncmp(locale, "pt", 2) \
		|| !strncmp(locale, "fr", 2) || !strncmp(locale, "de", 2) || !strncmp(locale, "tr", 2) \
		|| !strncmp(locale, "it", 2) || !strncmp(locale, "in", 2) || !strncmp(locale, "ms", 2) \
		|| !strncmp(locale, "ms", 2) || !strncmp(locale, "vi", 2) || !strncmp(locale, "ar", 2) \
		|| !strncmp(locale, "th", 2) || !strncmp(locale, "nl", 2) )
	{
		//all the lanuage is related to ISO-8859 charactor encoding
		mLocaleEncoding = kEncodingISO8859;
	}

    LOGI("MediaScannerClient -setLocale mLocaleEncoding:%x \n",mLocaleEncoding);
#endif    
}

void MediaScannerClient::beginFile()
{
    mNames = new StringArray;
    mValues = new StringArray;
}

status_t MediaScannerClient::addStringTag(const char* name, const char* value)
{
#ifndef ANDROID_DEFAULT_CODE
   LOGV("addStringTag mLocaleEncoding:%x \n",mLocaleEncoding);

   // validate to make sure it is legal utf8
   uint32_t valid_chars;
//   if (oscl_str_is_valid_utf8((const uint8 *)value, valid_chars))
   {   
//       if (mLocaleEncoding != kEncodingNone)
    { 
#else
    if (mLocaleEncoding != kEncodingNone) {
#endif 
       
        // don't bother caching strings that are all ASCII.
        // call handleStringTag directly instead.
        // check to see if value (which should be utf8) has any non-ASCII characters
#ifndef ANDROID_DEFAULT_CODE
           LOGV("addStringTag 1 name:%s, value:%s \n",name,value);
#endif
        bool nonAscii = false;
        const char* chp = value;
        char ch;
        while ((ch = *chp++)) {
            if (ch & 0x80) {
                nonAscii = true;
                break;
            }
        }

        if (nonAscii) {
#ifndef ANDROID_DEFAULT_CODE
              LOGV("addStringTag nonAscii \n"); 
#endif           
            // save the strings for later so they can be used for native encoding detection
            mNames->push_back(name);
            mValues->push_back(value);
            return OK;
        }
        // else fall through
    }

    // autodetection is not necessary, so no need to cache the values
    // pass directly to the client instead
#ifndef ANDROID_DEFAULT_CODE
       LOGV("+handleStringTag \n");
#endif
    return handleStringTag(name, value);
}
#ifndef ANDROID_DEFAULT_CODE
/*          
   else
   {
      LOGD( "value '%s' is not a legal UTF8 string\n",value );
      return true;
   } 
*/   

}
#endif

#ifndef ANDROID_DEFAULT_CODE
bool charMatchISO8859(uint8_t ch)
{
	if(((ch > 0x00 && ch < 0x1F)) || (ch == 0x7F) || ((ch > 0x80) && (ch < 0x9F)) )
		return false;
	return true;
	
}
#endif
static uint32_t possibleEncodings(const char* s)
{
#ifndef ANDROID_DEFAULT_CODE
   LOGI("+possibleEncodings %s \n",s);       
#endif   
    uint32_t result = kEncodingAll;
    // if s contains a native encoding, then it was mistakenly encoded in utf8 as if it were latin-1
    // so we need to reverse the latin-1 -> utf8 conversion to get the native chars back
    uint8_t ch1, ch2;
    uint8_t* chp = (uint8_t *)s;

#ifndef ANDROID_DEFAULT_CODE   
	uint32_t uiISO8859 = kEncodingISO8859;

    while ((ch1 = *chp++)) {
        if (ch1 & 0x80) {
        	if((ch1 >= 0xE0)) {
        	 	// if three or more bytes for one charactor,most of the time is UTF-8
        	 	result = kEncodingNone;
        	 	break;
        	}
        	else if((ch1 >= 0xC0) && (ch1 <= 0xDF)){
		        	//if two bytes for one charactor, most native mistaken is this case
		            ch2 = *chp++;
		            ch1 = ((ch1 << 6) & 0xC0) | (ch2 & 0x3F);
		            // ch1 is now the first byte of the potential native char 
                            	
                    //check ch1 whether is iso8859
					if(uiISO8859 && charMatchISO8859(ch1)){

						uiISO8859 &= kEncodingISO8859;	
					}
					else
						uiISO8859  = 0;
					

		            ch2 = *chp;
		            if (ch2 & 0x80){
						if((ch2 >= 0xC0) && (ch2 <= 0xDF)){ //if the second is also two bytes
							chp++;
                			ch2 = ((ch2 << 6) & 0xC0) | (*chp++ & 0x3F);
						}
            		 }

                    //check ch2 whether is iso8859
					if(uiISO8859 && charMatchISO8859(ch2)){

						uiISO8859 &= kEncodingISO8859;	
					}
					else
						uiISO8859  = 0;

		            // ch2 is now the second byte of the potential native char
		            int ch = (int)ch1 << 8 | (int)ch2;
		            result &= findPossibleEncodings(ch);
          }
        }
        // else ASCII character, which could be anything
    }

   result |= uiISO8859; //contain the iso8859 info in the result
   
   LOGI("-possibleEncodings %d \n",result); 
#else
    while ((ch1 = *chp++)) {
        if (ch1 & 0x80) {
            ch2 = *chp++;
            ch1 = ((ch1 << 6) & 0xC0) | (ch2 & 0x3F);
            // ch1 is now the first byte of the potential native char

            ch2 = *chp++;
            if (ch2 & 0x80)
                ch2 = ((ch2 << 6) & 0xC0) | (*chp++ & 0x3F);
            // ch2 is now the second byte of the potential native char
            int ch = (int)ch1 << 8 | (int)ch2;
            result &= findPossibleEncodings(ch);
        }
        // else ASCII character, which could be anything
    }

#endif

    return result;
}

#ifndef ANDROID_DEFAULT_CODE   
void MediaScannerClient::convertValues(uint32_t encoding, int i)
{
    LOGV("+convertValues encoding:%d \n",encoding);   
   
    const char* enc = NULL;
    switch (encoding) {
        case kEncodingShiftJIS:
            enc = "shift-jis";
            break;
        case kEncodingGBK:
            enc = "gbk";
            break;
        case kEncodingBig5:
            enc = "Big5";
            break;
        case kEncodingEUCKR:
            enc = "EUC-KR";
            break;
		case kEncodingISO8859:
			enc = NULL; // ISO8859 no need to change
			break;
        default:
            // check if the mLocaleEncoding is GBK, use GBK as first priority
            // This code is used for encoding type is not clear.
            if(encoding > 0)
            {
                if((mLocaleEncoding == kEncodingGBK) || (mLocaleEncoding == kEncodingNone))
                {
                    if(encoding & 0x2){
                       enc = "gbk";
                    }
                    else if(encoding & 0x4){
                       enc = "Big5";
                    }
                    else if(encoding & 0x8){
                       enc = "EUC-KR";
                    }                                
                    else if(encoding & 0x1){
                       enc = "shift-jis";
                    }                    
                }
                else if(mLocaleEncoding == kEncodingBig5)
                {
                    if(encoding & 0x4){
                       enc = "Big5";
                    }                
                    else if(encoding & 0x2){
                       enc = "gbk";
                    }
                    else if(encoding & 0x8){
                       enc = "EUC-KR";
                    }
                    else if(encoding & 0x1){
                       enc = "shift-jis";
                    }                     
                }
				else if(mLocaleEncoding == kEncodingISO8859)
				{
					if(encoding & 0x10){ // if iso8859 no need to convert
						enc = NULL;
					}
					else if(encoding & 0x2){
                       enc = "gbk";
                    }
                    else if(encoding & 0x4){
                       enc = "Big5";
                    }
                    else if(encoding & 0x8){
                       enc = "EUC-KR";
                    }                                
                    else if(encoding & 0x1){
                       enc = "shift-jis";
                    }     
				}
            }
            LOGV("+convertValues mLocaleEncoding:%d, enc=%s \n",mLocaleEncoding,enc); 
    }

    if (enc) 
    {
        UErrorCode status = U_ZERO_ERROR;

        LOGV("+convertValues enc:%s \n",enc); 

        UConverter *conv = ucnv_open(enc, &status);
        if (U_FAILURE(status)) {
            LOGD("could not create UConverter for %s\n", enc);
            return;
        }
        UConverter *utf8Conv = ucnv_open("UTF-8", &status);
        if (U_FAILURE(status)) {
            LOGD("could not create UConverter for UTF-8\n");
            ucnv_close(conv);
            return;
        }

        // for each value string, convert from native encoding to UTF-8
        {
            // first we need to untangle the utf8 and convert it back to the original bytes
            // since we are reducing the length of the string, we can do this in place
            uint8_t* src = (uint8_t *)mValues->getEntry(i);
            int len = strlen((char *)src);
            uint8_t* dest = src;

            uint8_t uch;
            while ((uch = *src++)) {
                if (uch & 0x80)
                    *dest++ = ((uch << 6) & 0xC0) | (*src++ & 0x3F);
                else
                    *dest++ = uch;
            }
            *dest = 0;

            // now convert from native encoding to UTF-8
            const char* source = mValues->getEntry(i);
            int targetLength = len * 3 + 1;
            char* buffer = new char[targetLength];
            if (!buffer)
                goto _Fail_Case;
            
            char* target = buffer;

            ucnv_convertEx(utf8Conv, conv, &target, target + targetLength,
                    &source, (const char *)dest, NULL, NULL, NULL, NULL, TRUE, TRUE, &status);

            LOGV("+convertValues source:%s, target:%s, status=%d \n",source,target,status);

            if (U_FAILURE(status)) {
                LOGD("ucnv_convertEx failed: %d\n", status);
                mValues->setEntry(i, "???");
            } else {
                // zero terminate
                *target = 0;
                mValues->setEntry(i, buffer);
            }         

            delete[] buffer;
        }
        
_Fail_Case:
        ucnv_close(conv);
        ucnv_close(utf8Conv);
    }

    LOGV("-convertValues \n");
    
}
#else

void MediaScannerClient::convertValues(uint32_t encoding)
{  
    const char* enc = NULL;
    switch (encoding) {
        case kEncodingShiftJIS:
            enc = "shift-jis";
            break;
        case kEncodingGBK:
            enc = "gbk";
            break;
        case kEncodingBig5:
            enc = "Big5";
            break;
        case kEncodingEUCKR:
            enc = "EUC-KR";
            break;
    }

    if (enc) {
        UErrorCode status = U_ZERO_ERROR;

        UConverter *conv = ucnv_open(enc, &status);
        if (U_FAILURE(status)) {
            LOGE("could not create UConverter for %s\n", enc);
            return;
        }
        UConverter *utf8Conv = ucnv_open("UTF-8", &status);
        if (U_FAILURE(status)) {
            LOGE("could not create UConverter for UTF-8\n");
            ucnv_close(conv);
            return;
        }

        // for each value string, convert from native encoding to UTF-8
        for (int i = 0; i < mNames->size(); i++) {
            // first we need to untangle the utf8 and convert it back to the original bytes
            // since we are reducing the length of the string, we can do this in place
            uint8_t* src = (uint8_t *)mValues->getEntry(i);
            int len = strlen((char *)src);
            uint8_t* dest = src;

            uint8_t uch;
            while ((uch = *src++)) {
                if (uch & 0x80)
                    *dest++ = ((uch << 6) & 0xC0) | (*src++ & 0x3F);
                else
                    *dest++ = uch;
            }
            *dest = 0;

            // now convert from native encoding to UTF-8
            const char* source = mValues->getEntry(i);
            int targetLength = len * 3 + 1;
            char* buffer = new char[targetLength];
            if (!buffer)
                break;
            char* target = buffer;

            ucnv_convertEx(utf8Conv, conv, &target, target + targetLength,
                    &source, (const char *)dest, NULL, NULL, NULL, NULL, TRUE, TRUE, &status);
            if (U_FAILURE(status)) {
                LOGE("ucnv_convertEx failed: %d\n", status);
                mValues->setEntry(i, "???");
            } else {
                // zero terminate
                *target = 0;
                mValues->setEntry(i, buffer);
            }

            delete[] buffer;
        }

        ucnv_close(conv);
        ucnv_close(utf8Conv);
    }
}
#endif

void MediaScannerClient::endFile()
{
#ifndef ANDROID_DEFAULT_CODE   
   LOGV("endFile mLocaleEncoding:%d \n",mLocaleEncoding);   
    {      
#else
    if (mLocaleEncoding != kEncodingNone) {
#endif         
        int size = mNames->size();
        uint32_t encoding = kEncodingAll;
 
#ifndef ANDROID_DEFAULT_CODE   
        
        LOGV("endFile +possibleEncodings size: %d \n",size);     
        //// compute a bit mask containing all possible encodings
        for (int i = 0; i < mNames->size(); i++)
        {
            encoding = possibleEncodings(mValues->getEntry(i));
            LOGV("endFile +possibleEncodings: %d \n",encoding);        
            convertValues(encoding,i);            
        }

#else
        // compute a bit mask containing all possible encodings
        for (int i = 0; i < mNames->size(); i++)
            encoding &= possibleEncodings(mValues->getEntry(i));

        // if the locale encoding matches, then assume we have a native encoding.
        if (encoding & mLocaleEncoding)
            convertValues(mLocaleEncoding);
#endif
        
        // finally, push all name/value pairs to the client
        for (int i = 0; i < mNames->size(); i++) {
            status_t status = handleStringTag(mNames->getEntry(i), mValues->getEntry(i));
            if (status) {
                break;
            }
        }
    }
    // else addStringTag() has done all the work so we have nothing to do

    delete mNames;
    delete mValues;
    mNames = NULL;
    mValues = NULL;
}

}  // namespace android

