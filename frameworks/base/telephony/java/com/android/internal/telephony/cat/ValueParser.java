/*
 * Copyright (C) 2006-2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.internal.telephony.cat;

import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.cat.Duration.TimeUnit;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

//Add by Huibin Mao Mtk80229
//ICS Migration start
import java.net.UnknownHostException;
//ICS Migration end
import com.mediatek.featureoption.FeatureOption;

abstract class ValueParser {

    /**
     * Search for a Command Details object from a list.
     *
     * @param ctlvs List of ComprehensionTlv objects used for search
     * @return An CtlvCommandDetails object found from the objects. If no
     *         Command Details object is found, ResultException is thrown.
     * @throws ResultException
     */
    static CommandDetails retrieveCommandDetails(ComprehensionTlv ctlv)
            throws ResultException {

        CommandDetails cmdDet = new CommandDetails();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            cmdDet.compRequired = ctlv.isComprehensionRequired();
            cmdDet.commandNumber = rawValue[valueIndex] & 0xff;
            cmdDet.typeOfCommand = rawValue[valueIndex + 1] & 0xff;
            cmdDet.commandQualifier = rawValue[valueIndex + 2] & 0xff;
            return cmdDet;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    /**
     * Search for a Device Identities object from a list.
     *
     * @param ctlvs List of ComprehensionTlv objects used for search
     * @return An CtlvDeviceIdentities object found from the objects. If no
     *         Command Details object is found, ResultException is thrown.
     * @throws ResultException
     */
    static DeviceIdentities retrieveDeviceIdentities(ComprehensionTlv ctlv)
            throws ResultException {

        DeviceIdentities devIds = new DeviceIdentities();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            devIds.sourceId = rawValue[valueIndex] & 0xff;
            devIds.destinationId = rawValue[valueIndex + 1] & 0xff;
            return devIds;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
    }

    /**
     * Retrieves Duration information from the Duration COMPREHENSION-TLV
     * object.
     *
     * @param ctlv A Text Attribute COMPREHENSION-TLV object
     * @return A Duration object
     * @throws ResultException
     */
    static Duration retrieveDuration(ComprehensionTlv ctlv) throws ResultException {
        int timeInterval = 0;
        TimeUnit timeUnit = TimeUnit.SECOND;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();

        try {
            timeUnit = TimeUnit.values()[(rawValue[valueIndex] & 0xff)];
            timeInterval = rawValue[valueIndex + 1] & 0xff;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        return new Duration(timeInterval, timeUnit);
    }

    /**
     * Retrieves Item information from the COMPREHENSION-TLV object.
     *
     * @param ctlv A Text Attribute COMPREHENSION-TLV object
     * @return An Item
     * @throws ResultException
     */
    static Item retrieveItem(ComprehensionTlv ctlv) throws ResultException {
        Item item = null;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();

        if (length != 0) {
            int textLen = length - 1;

            try {
                int id = rawValue[valueIndex] & 0xff;
                String text = IccUtils.adnStringFieldToString(rawValue,
                        valueIndex + 1, textLen);
                item = new Item(id, text);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }

        return item;
    }

    /**
     * Retrieves Item id information from the COMPREHENSION-TLV object.
     *
     * @param ctlv A Text Attribute COMPREHENSION-TLV object
     * @return An Item id
     * @throws ResultException
     */
    static int retrieveItemId(ComprehensionTlv ctlv) throws ResultException {
        int id = 0;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();

        try {
            id = rawValue[valueIndex] & 0xff;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return id;
    }

    /**
     * Retrieves icon id from an Icon Identifier COMPREHENSION-TLV object
     *
     * @param ctlv An Icon Identifier COMPREHENSION-TLV object
     * @return IconId instance
     * @throws ResultException
     */
    static IconId retrieveIconId(ComprehensionTlv ctlv) throws ResultException {
        IconId id = new IconId();

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            id.selfExplanatory = (rawValue[valueIndex++] & 0xff) == 0x00;
            id.recordNumber = rawValue[valueIndex] & 0xff;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return id;
    }

    /**
     * Retrieves item icons id from an Icon Identifier List COMPREHENSION-TLV
     * object
     *
     * @param ctlv An Item Icon List Identifier COMPREHENSION-TLV object
     * @return ItemsIconId instance
     * @throws ResultException
     */
    static ItemsIconId retrieveItemsIconId(ComprehensionTlv ctlv)
            throws ResultException {
        CatLog.d("ValueParser", "retrieveItemsIconId:");
        ItemsIconId id = new ItemsIconId();

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int numOfItems = ctlv.getLength() - 1;
        id.recordNumbers = new int[numOfItems];

        try {
            // get icon self-explanatory
            id.selfExplanatory = (rawValue[valueIndex++] & 0xff) == 0x00;

            for (int index = 0; index < numOfItems;) {
                id.recordNumbers[index++] = rawValue[valueIndex++];
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        return id;
    }

    /**
     * Retrieves text attribute information from the Text Attribute
     * COMPREHENSION-TLV object.
     *
     * @param ctlv A Text Attribute COMPREHENSION-TLV object
     * @return A list of TextAttribute objects
     * @throws ResultException
     */
    static List<TextAttribute> retrieveTextAttribute(ComprehensionTlv ctlv)
            throws ResultException {
        ArrayList<TextAttribute> lst = new ArrayList<TextAttribute>();

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();

        if (length != 0) {
            // Each attribute is consisted of four bytes
            int itemCount = length / 4;

            try {
                for (int i = 0; i < itemCount; i++, valueIndex += 4) {
                    int start = rawValue[valueIndex] & 0xff;
                    int textLength = rawValue[valueIndex + 1] & 0xff;
                    int format = rawValue[valueIndex + 2] & 0xff;
                    int colorValue = rawValue[valueIndex + 3] & 0xff;

                    int alignValue = format & 0x03;
                    TextAlignment align = TextAlignment.fromInt(alignValue);

                    int sizeValue = (format >> 2) & 0x03;
                    FontSize size = FontSize.fromInt(sizeValue);
                    if (size == null) {
                        // Font size value is not defined. Use default.
                        size = FontSize.NORMAL;
                    }

                    boolean bold = (format & 0x10) != 0;
                    boolean italic = (format & 0x20) != 0;
                    boolean underlined = (format & 0x40) != 0;
                    boolean strikeThrough = (format & 0x80) != 0;

                    TextColor color = TextColor.fromInt(colorValue);

                    TextAttribute attr = new TextAttribute(start, textLength,
                            align, size, bold, italic, underlined,
                            strikeThrough, color);
                    lst.add(attr);
                }

                return lst;

            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return null;
    }


    /**
     * Retrieves alpha identifier from an Alpha Identifier COMPREHENSION-TLV
     * object.
     *
     * @param ctlv An Alpha Identifier COMPREHENSION-TLV object
     * @return String corresponding to the alpha identifier
     * @throws ResultException
     */
    static String retrieveAlphaId(ComprehensionTlv ctlv) throws ResultException {

        if (ctlv != null) {
            byte[] rawValue = ctlv.getRawValue();
            int valueIndex = ctlv.getValueIndex();
            int length = ctlv.getLength();
            if (length != 0) {
                try {
                //MTK-START [mtk80950][120810][ALPS00XXXXXX] add UTK
                    if (FeatureOption.EVDO_DT_VIA_SUPPORT) {
                        return utkStringFieldToString(rawValue, valueIndex,
                            length);
                    } else {
                    return IccUtils.adnStringFieldToString(rawValue, valueIndex,
                            length);
                    }
                //MTK-END [mtk80950][120810][ALPS00XXXXXX] add UTK
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            } else {
                return "";
            }
        } else {
            return "";
        }
    }


//MTK-START [mtk80950][120810][ALPS00XXXXXX] add UTK
    static String retrieveAdress(ComprehensionTlv ctlv) throws ResultException {

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length >= 2) {
            try {
                // Skip npi and ton
                return IccUtils.bcdToString(rawValue, valueIndex + 1,
                        length -1);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return null;
    }

//MTK-END [mtk80950][120810][ALPS00XXXXXX] add UTK

    /**
     * Retrieves text from the Text COMPREHENSION-TLV object, and decodes it
     * into a Java String.
     *
     * @param ctlv A Text COMPREHENSION-TLV object
     * @return A Java String object decoded from the Text object
     * @throws ResultException
     */
    static String retrieveTextString(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        byte codingScheme = 0x00;
        String text = null;
        int textLen = ctlv.getLength();

        // In case the text length is 0, return a null string.
        if (textLen == 0) {
            return text;
        } else {
            // one byte is coding scheme
            textLen -= 1;
        }

        try {
            codingScheme = (byte) (rawValue[valueIndex] & 0x0c);

            if (codingScheme == 0x00) { // GSM 7-bit packed
                text = GsmAlphabet.gsm7BitPackedToString(rawValue,
                        valueIndex + 1, (textLen * 8) / 7);
            } else if (codingScheme == 0x04) { // GSM 8-bit unpacked
                //MTK-START [mtk80950][120810][ALPS00XXXXXX] add UTK
                if (FeatureOption.EVDO_DT_VIA_SUPPORT) {
                    text = utkStringFieldToString(rawValue, valueIndex + 1, textLen);
                } else {
                text = GsmAlphabet.gsm8BitUnpackedToString(rawValue,
                        valueIndex + 1, textLen);
                }
                //MTK-END [mtk80950][120810][ALPS00XXXXXX] add UTK
            } else if (codingScheme == 0x08) { // UCS2
                text = new String(rawValue, valueIndex + 1, textLen, "UTF-16");
            } else {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }

            return text;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (UnsupportedEncodingException e) {
            // This should never happen.
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    //MTK-START [mtk80950][120810][ALPS00XXXXXX] add UTK
    static byte[] retrieveSmsPdu(ComprehensionTlv ctlv)
            throws ResultException {

        byte[] smsPdu = new byte[ctlv.getLength()];
        System.arraycopy(ctlv.getRawValue(), ctlv.getValueIndex(), smsPdu, 0, ctlv.getLength());
        return smsPdu;
    }

    private static String utkStringFieldToString(byte[] data, int offset, int length) {
        if (length >= 1) {
            if (data[offset] == (byte) 0x80) {
                int ucslen = (length - 1) / 2;
                String ret = null;

                try {
                    ret = new String(data, offset + 1, ucslen * 2, "utf-16be");
                } catch (UnsupportedEncodingException ex) {
                    ;
                }

                if (ret != null) {
                    // trim off trailing FFFF characters

                    ucslen = ret.length();
                    while (ucslen > 0 && ret.charAt(ucslen - 1) == '\uFFFF')
                        ucslen--;

                    return ret.substring(0, ucslen);
                }
            }
        }

        boolean isucs2 = false;
        char base = '\0';
        int len = 0;

        if (length >= 3 && data[offset] == (byte) 0x81) {
            len = data[offset + 1] & 0xFF;
            if (len > length - 3)
                len = length - 3;

            base = (char) ((data[offset + 2] & 0xFF) << 7);
            offset += 3;
            isucs2 = true;
        } else if (length >= 4 && data[offset] == (byte) 0x82) {
            len = data[offset + 1] & 0xFF;
            if (len > length - 4)
                len = length - 4;

            base = (char) (((data[offset + 2] & 0xFF) << 8) |
                            (data[offset + 3] & 0xFF));
            offset += 4;
            isucs2 = true;
        }

        if (isucs2) {
            StringBuilder ret = new StringBuilder();

            while (len > 0) {
                // UCS2 subset case

                if (data[offset] < 0) {
                    ret.append((char) (base + (data[offset] & 0x7F)));
                    offset++;
                    len--;
                }

                // GSM character set case

                int count = 0;
                while (count < len && data[offset + count] >= 0)
                    count++;

                ret.append(GsmAlphabet.gsm8BitUnpackedToString(data,
                           offset, count));

                offset += count;
                len -= count;
            }

            return ret.toString();
        }

        return utk8BitUnpackedToString(data, offset, length);
    }

    private static String
    utk8BitUnpackedToString(byte[] data, int offset, int length) {

        StringBuilder ret = new StringBuilder(length);

        for (int i = offset ; i < offset + length ; i++) {

            int c = data[i] & 0xff;

            if (c == 0x00)  /* @ */
              c = 0x40;
            else if (c == 0x02)  /* $ */
              c = 0x24;
            else if (c == 0x11)  /* _ */
              c = 0x5f;

            ret.append((char)c);
        }

        return ret.toString();
    }
    //MTK-END [mtk80950][120810][ALPS00XXXXXX] add UTK


	//Add by Huibin Mao Mtk80229
	//ICS Migration start
    
		static BearerDesc retrieveBearerDesc(ComprehensionTlv ctlv) throws ResultException {
	        byte[] rawValue = ctlv.getRawValue();
	        int valueIndex = ctlv.getValueIndex();
			BearerDesc bearerDesc = new BearerDesc();
	
	        try {
			    int bearerType = rawValue[valueIndex++] & 0xff;
			    bearerDesc.bearerType = bearerType;
				
			    if(BipUtils.BEARER_TYPE_GPRS == bearerType) {
				    bearerDesc.precedence = rawValue[valueIndex++] & 0xff;
				    bearerDesc.delay = rawValue[valueIndex++] & 0xff;
				    bearerDesc.reliability = rawValue[valueIndex++] & 0xff;
				    bearerDesc.peak = rawValue[valueIndex++] & 0xff;
				    bearerDesc.mean = rawValue[valueIndex++] & 0xff;
				    bearerDesc.pdpType = rawValue[valueIndex++] & 0xff;
			    } else if(BipUtils.BEARER_TYPE_CSD == bearerType) {
			    	  CatLog.d("CAT", "retrieveBearerDesc: unsupport CSD");
			        throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
			    } else {
			        CatLog.d("CAT", "retrieveBearerDesc: un-understood bearer type");
	                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
			    }
	        } catch (IndexOutOfBoundsException e) {
	            CatLog.d("CAT", "retrieveBearerDesc: out of bounds");
	            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
	        }
	
			return bearerDesc;
		}
	
		static int retrieveBufferSize(ComprehensionTlv ctlv) throws ResultException {
	    byte[] rawValue = ctlv.getRawValue();
	    int valueIndex = ctlv.getValueIndex();
			int size = 0;
	
			try {
	            size = ((rawValue[valueIndex] & 0xff) << 8) + (rawValue[valueIndex + 1] & 0xff);
			} catch (IndexOutOfBoundsException e) {
				      CatLog.d("CAT", "retrieveBufferSize: out of bounds");
	            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
	    }
	
			return size;
		}
	
		static String retrieveNetworkAccessName(ComprehensionTlv ctlv) throws ResultException {
	        byte[] rawValue = ctlv.getRawValue();
	        int valueIndex = ctlv.getValueIndex();
			String networkAccessName = null;
	
			try {
			// int len = ctlv.getLength() - ctlv.getValueIndex() + 1;
			int totalLen = ctlv.getLength();
			String stkNetworkAccessName = new String(rawValue, valueIndex, totalLen);
		    String stkNetworkIdentifier = null;
		    String stkOperatorIdentifier = null;
	        
	        if(stkNetworkAccessName != null && totalLen > 0){
	           //Get network identifier
	           int len = rawValue[valueIndex++];
	           if(totalLen > len){
                  stkNetworkIdentifier = new String(rawValue, valueIndex, len);
                  valueIndex+=len;
                  
	           }
	           CatLog.d("CAT", "totalLen:" + totalLen + ";" + valueIndex + ";" + len);
	           
	           //Get operator identififer
	           if(totalLen > (len+1)){
	              totalLen-=len;
	              len = rawValue[valueIndex++];
	              if(totalLen >= len){
                    stkOperatorIdentifier = new String(rawValue, valueIndex, len);
                  }
                  CatLog.d("CAT", "totalLen:" + totalLen + ";" + valueIndex + ";" +  len);
	           }
	           
	           if(stkNetworkIdentifier != null && stkOperatorIdentifier != null){
	                networkAccessName = stkNetworkIdentifier + "." + stkOperatorIdentifier;
	           }else if(stkNetworkIdentifier != null){
	                networkAccessName = stkNetworkIdentifier;
	           }
	           	           
	           CatLog.d("CAT", "nw:" + stkNetworkIdentifier + ";" + stkOperatorIdentifier);
	        }
	        
	        
			} catch (IndexOutOfBoundsException e) {
			    CatLog.d("CAT", "retrieveNetworkAccessName: out of bounds");
	        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
	    }
	
			return networkAccessName;
		}
	
		static TransportProtocol retrieveTransportProtocol(ComprehensionTlv ctlv) throws ResultException {
			byte[] rawValue = ctlv.getRawValue();
	        int valueIndex = ctlv.getValueIndex();
			int protocolType = 0;
			int portNumber = 0;
	
			try {
	            protocolType = rawValue[valueIndex++];
				portNumber = ((rawValue[valueIndex] & 0xff) << 8) + (rawValue[valueIndex + 1] & 0xff);
			} catch (IndexOutOfBoundsException e) {
			    CatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
	            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
	        }
	
			return new TransportProtocol(protocolType, portNumber);
		}
	
		static OtherAddress retrieveOtherAddress(ComprehensionTlv ctlv) throws ResultException {
	        byte[] rawValue = ctlv.getRawValue();
	        int valueIndex = ctlv.getValueIndex();
			int addressType = 0;
			OtherAddress otherAddress = null;
	
			try {
	            addressType = rawValue[valueIndex++];
				if(BipUtils.ADDRESS_TYPE_IPV4 == addressType) {
	                otherAddress = new OtherAddress(addressType, rawValue, valueIndex);
				} else if(BipUtils.ADDRESS_TYPE_IPV6 == addressType) {
					return null;
			        // throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
			    } else {
			    	return null;
	                // throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
			    }
			} catch (IndexOutOfBoundsException e) {
			    CatLog.d("CAT", "retrieveOtherAddress: out of bounds");
			    return null;
	            // throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
	        } catch (UnknownHostException e2) {
	            CatLog.d("CAT", "retrieveOtherAddress: unknown host");
	            return null;
	            // throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
	        }
	
			return otherAddress;
		}
	
		static int retrieveChannelDataLength(ComprehensionTlv ctlv) throws ResultException {
	        byte[] rawValue = ctlv.getRawValue();
	        int valueIndex = ctlv.getValueIndex(); 
			int length = 0;
	
	        CatLog.d("CAT", "valueIndex:" + valueIndex);
	        
			try {
				length = rawValue[valueIndex] & 0xFF;
			} catch(IndexOutOfBoundsException e) {
			    CatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
			    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
			}
	
			return length;
		}
	
		static byte[] retrieveChannelData(ComprehensionTlv ctlv) throws ResultException {
	        byte[] rawValue = ctlv.getRawValue();
	        int valueIndex = ctlv.getValueIndex(); 
			byte[] channelData = null;
	
			try {
				channelData = new byte[ctlv.getLength()];
				System.arraycopy(rawValue, valueIndex, channelData, 0, channelData.length);
			} catch(IndexOutOfBoundsException e) {
			    CatLog.d("CAT", "retrieveChannelData: out of bounds");
			    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
			}
	
			return channelData;
		}
    static byte[] retrieveNextActionIndicator(ComprehensionTlv ctlv) throws ResultException {
        byte[] nai;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();

        nai = new byte[length];
        try {
            for(int index = 0; index < length; )
            {
                nai[index++] = rawValue[valueIndex++];
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return nai;
    }
	//ICS Migration end

}
