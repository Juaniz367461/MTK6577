/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/

/*******************************************************************************
*   ISP_NVRAM_REGISTER_STRUCT
********************************************************************************/
Idx:{//ISP_NVRAM_REG_INDEX_STRUCT
    Shading     :0, 
    OB          :0,
    DM          :0,
    DP          :0, 
    NR1         :0,
    NR2         :0,
    EE          :0,
    Saturation  :0,
    Contrast    :4,
    Hue         :1,
    CCM         :0,
    Gamma       :0
},
Shading:{
    {set:{//00 Preview
	    0,0,0,0,0,0,0x10000000, 0x903f902f, 0x40001950, 0x00480036, 0x20202020
    }},
    {set:{//01 Capture
	    0,0,0,0,0,0,0x10000000, 0xf04ff03b, 0x40001950, 0x00590043, 0x20202020
    }},
    {set:{//02
	    0,0,0,0,0,0,0,0,0,0,0
    }}
},
OB:{
    {set:{//00
        0x90909090
    }}
}, 
DM:{
    {set:{//00 Preview ISO100/ISO200/ISO400
        0x00000100,	0x0A800810,	0x0020083F
    }}, 
    {set:{//01 Preview ISO800
        0x00000100,	0x0A800810,	0x0020083F
    }}, 
    {set:{//02 Preview ISO1600
        0x00000100,	0x0A800810,	0x0020083F
    }}, 
    {set:{//03 Capture ISO100/ISO200
        0x00000100,	0x0A800810,	0x0020083F
    }}, 
    {set:{//04 Capture ISO400
        0x00000100,	0x0A800810,	0x0020083F
    }}, 
    {set:{//05 Capture ISO800
        0x00000100,	0x0A800810,	0x0020083F
    }}, 
    {set:{//06 Capture ISO1600
        0x00000100,	0x0A800810,	0x0020083F
    }}
}, 
DP:{//Auto Defect
    {set:{//00
        0x000006E7, 0x50285050,	0x006003A0,	0x00300050,	0x000009B7
    }},
    {set:{//01
        0x000006E7, 0x50285050,	0x006003A0,	0x00300050,	0x000009B7
    }},
    {set:{//02
        0x000006E7, 0x50285050,	0x006003A0,	0x00300050,	0x000009B7
    }}, 
    {set:{//03 Disable (Do not modify me)
        0x00000000, 0x50285050, 0x006003A0, 0x00300050, 0x000008B7
    }}
},
NR1:{//NR1
    {set:{//00 Preview ISO100
        0x000006E7, 0x000011A0, 0x092328C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }},
    {set:{//01 Preview ISO200
        0x000006E7, 0x000011A0, 0x092628C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }},
    {set:{//02 Preview ISO400
        0x000006E7, 0x000011A0, 0x092D28C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }},
    {set:{//03 Preview ISO800
        0x000006E7, 0x000011A0, 0x092F28C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }},
    {set:{//04 Preview ISO1600
        0x000006E7, 0x000011A0, 0x095028C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }},
    {set:{//05 Capture ISO100
        0x000006E7, 0x000011A0, 0x092328C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }},
    {set:{//06 Capture ISO200
        0x000006E7, 0x000011A0, 0x092628C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }}, 
    {set:{//07 Capture ISO400
        0x000006E7, 0x000011A0, 0x092D28C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }},
    {set:{//08 Capture ISO800
        0x000006E7, 0x000011A0, 0x092F28C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010, 
    }},
    {set:{//09 Capture ISO1600
        0x000006E7, 0x000011A0, 0x095028C8, 0x000008AC, 0x05050507, 0x090B0C0C, 0x05050507, 
        0x090B0C0C, 0x05050507, 0x090B0C0C, 0x05050507, 0x090B0C0C, 0x0206090D, 0x10101010,  
    }},
    {set:{//10 No Use
        0x000006E7, 0x000011A0, 0x094428A0, 0x000007AF, 0x03050709, 0x0B0D0F11, 0x03050709, 
        0x0B0D0F11, 0x03050709, 0x0B0D0F11, 0x03050709, 0x0B0D0F11, 0x0406090D, 0x10101010
    }},
    {set:{//11 Disable (Do not modify me)
        0x000000C0, 0x000011A0, 0x094428A0, 0x000007AF, 0x03050709, 0x0B0D0F11, 0x03050709, 
        0x0B0D0F11, 0x03050709, 0x0B0D0F11, 0x03050709, 0x0B0D0F11, 0x0406090D, 0x10101010
    }}
},
NR2:{//NR2
    {set:{//00 Preview ISO100/ISO200/ISO400
        0x00200003, 0x0001F414, 0x00927788, 0x02030405, 0x14282864, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303,  
    }},
    {set:{//01 Preview ISO800
        0x00200003, 0x0001F414, 0x00927788, 0x02030405, 0x1E3C3CA0, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303,
    }},
    {set:{//02 Preview ISO1600
        0x00200003, 0x0001F414, 0x00927788, 0x02030405, 0x1E3C3CA0, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303,
    }},
    {set:{//03 Capture Mode0 ISO100/ISO200      0 800
        0x00200003, 0x0001F414, 0x00927788, 0x02030405, 0x0A141428, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303,
    }},
    {set:{//04 Capture Mode0 ISO400                 0 1600
        0x00200003, 0x0001F414, 0x00927788, 0x02030405, 0x14282864, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303, 
    }},
    {set:{//05 Capture Mode0 ISO800                 1 100
        0x00200003, 0x0001F414, 0x00927788, 0x02030405, 0x1E3C3CA0, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303,
    }},
    {set:{//06 Capture Mode0 ISO1600               1 200
        0x00200003, 0x0001F414, 0x00922288, 0x02030405, 0x1E3C3CA0, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303,
    }}, 
    {set:{//07 Capture Mode1 ISO100/ISO200      1 400
        0x00210003, 0x0001F414, 0x00927788, 0x02030405, 0x0A141428, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303,
    }},
    {set:{//08 Capture Mode1 ISO400                1 800
        0x00210003, 0x0001F414, 0x00927788, 0x02030405, 0x14282864, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303, 
    }},
    {set:{//09 Capture Mode1 ISO800               1 1600
        0x00210003, 0x0001F414, 0x00927788, 0x02030405, 0x1E3C3CA0, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303,
    }},
    {set:{//10 Capture Mode1 ISO1600
        0x00210003, 0x0001F414, 0x00922288, 0x02030405, 0x1E3C3CA0, 0x02105182, 0x08101820, 0x10080604, 0x01AF5B58, 0x0000056B, 0x01020303, 
    }},
    {set:{//11 Disable (Do not modify me)
        0x00000000, 0x0001FF01, 0x00523264, 0x04090B0F, 0x050A0B10, 0x02107294, 0x08101820, 0x10080604, 0x01AF5B43, 0x0000056B, 0x0306070A
    }}
},
EE:{//EE
    {set:{//00 Preview                             // middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02030403, 
    }},
    {set:{//01 Capture ISO100					                     // middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02030403, 
    }},
    {set:{//02 Capture ISO200                       				// middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02020302, 
    }},
    {set:{//03 Capture ISO400                       				// middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x01020302, 
    }},
    {set:{//04 Capture ISO800                       				// middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x01020201, 
    }},
    {set:{//05 Capture ISO1600
        0x00000008, 0x4132322D, 0x00641909, 0x01020101, 
    }},
    {set:{//06 no one uses this, this is Min EE     // low sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x01020101, 
    }},
    {set:{//07 no one uses this, this is Max EE     // high sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02030403, 
    }}
}, 
Saturation:{
    {set:{//00  ISO100                                    // middle saturation
        0x00000709, 0x1020E0F0, 0x4C4C4C4C, 0x4C000000, 0xFF00FF00, 0x40404040, 0x00000000, 0x001E140A, 
    }},
    {set:{//01  ISO200                                    // middle saturation
        0x00000709, 0x1020E0F0, 0x4C4C4C4C, 0x4C000000, 0xFF00FF00, 0x40404040, 0x00000000, 0x001E140A, 
    }},
    {set:{//02  ISO400                                    // middle saturation
        0x00000709, 0x1020E0F0, 0x40464646, 0x40000000, 0xFF00FF00, 0x40404040, 0x00000000, 0x001E140A,
    }},
    {set:{//03  ISO800                                    // middle saturation
        0x00000709, 0x1020E0F0, 0x40404040, 0x40000000, 0xFF00FF00, 0x40404040, 0x00000000, 0x001E140A,
    }},
    {set:{//04  ISO1600                                    // middle saturation
        0x00000709, 0x1020E0F0, 0x3A404040, 0x3A000000, 0xFF00FF00, 0x40404040, 0x00000000, 0x001E140A,
    }},
    {set:{//05 no one uses this, this is Min Sat.   // low saturation
        0x00000709, 0x1020E0F0, 0x3A404040, 0x3A000000, 0xFF00FF00, 0x40404040, 0x00000000, 0x001E140A,
    }},
    {set:{//06 no one uses this, this is Max Sat.   // high saturation
        0x00000709, 0x1020E0F0, 0x4C4C4C4C, 0x4C000000, 0xFF00FF00, 0x40404040, 0x00000000, 0x001E140A, 
    }}
}, 
Contrast:{
    //..........................................................................
    //  low brightness
    {set:{//00  //  low contrast
        0x00000008, 0x00F00000, 0xFF00003B
    }},
    {set:{//01  //  middle contrast
        0x00000008, 0x00F00000, 0xFF000040
    }},
    {set:{//02  //  high contrast
        0x00000008, 0x00F00000, 0xFF000045
    }}, 
    //..........................................................................
    //  middle brightness
    {set:{//03  //  low contrast
        0x00000008, 0x00000000, 0xFF00003B
    }},
    {set:{//04  //  middle contrast
        0x00000008, 0x00000000, 0xFF000040
    }},
    {set:{//05  //  high contrast
        0x00000008, 0x00000000, 0xFF000045
    }}, 
    //..........................................................................
    //  high brightness
    {set:{//06  //  low contrast
        0x00000008, 0x000A0000, 0xFF00003B
    }},
    {set:{//07  //  middle contrast
        0x00000008, 0x000A0000, 0xFF000040
    }},
    {set:{//08  //  high contrast
        0x00000008, 0x000A0000, 0xFF000045
    }}, 
}, 
Hue:{
    {set:{//00  //  low hue
        0x00000002, 0x808062AE
    }},
    {set:{//01  //  middle hue
        0x00000002, 0x00007F01
    }},
    {set:{//02  //  high hue
        0x00000002, 0x80806252
    }}
}, 
CCM:{
    {set:{//00
        0x00309080, 0x00882107, 0x00029331,
    }},
    {set:{//01
        0x00399980, 0x00871E09, 0x00049733,
    }},
    {set:{//02
        0x00359283, 0x00862501, 0x00038F2C, 
    }}
},
Gamma:{
    {set:{//00
        0x42251309, 0x897C6C59, 0xBFB1A095, 0xE2DBD3CA, 0xFBF7F2ED
    }},
    {set:{//01
        0x42251309, 0x897C6C59, 0xBFB1A095, 0xE2DBD3CA, 0xFBF7F2ED
    }},
    {set:{//02
        0x42251309, 0x897C6C59, 0xBFB1A095, 0xE2DBD3CA, 0xFBF7F2ED
    }},
    {set:{//03
        0x42251309, 0x897C6C59, 0xBFB1A095, 0xE2DBD3CA, 0xFBF7F2ED
    }},
    {set:{//04
        0x42251309, 0x897C6C59, 0xBFB1A095, 0xE2DBD3CA, 0xFBF7F2ED
    }}, 
}

