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
 *     TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
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
        0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x10000000,0x90409030,0x40001000,0x00420033,0x20202020,
    }},
    {set:{//01 Capture
        0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x10000000,0xF051F03C,0x40001950,0x0043003F,0x20202020,
    }},
    {set:{//02
        0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,0x00000000,
    }}
},
OB:{
    {set:{//00
        0xA8A8A8A8
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
        0x000006E7,	0xF000F000,	0x006403FF,	0x00000000,	0x000000B7
    }},
    {set:{//01
        0x000006E7,	0xF000F000,	0x006403FF,	0x00000000,	0x000000B7
    }},
    {set:{//02
        0x000006E7,	0xF000F000,	0x006403FF,	0x00000000,	0x000000B7
    }}, 
    {set:{//03 Disable (Do not modify me)
        0x00000000, 0x50285050, 0x006003A0, 0x00300050, 0x000008B7
    }}
},
NR1:{//NR1
    {set:{//00 Preview ISO100
        0x000006E7,	0x000011A0,	0x092328C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//01 Preview ISO200
        0x000006E7,	0x000011A0,	0x092428C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//02 Preview ISO400
        0x000006E7,	0x000011A0,	0x092428C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//03 Preview ISO800
    	0x000006E7,	0x000011A0,	0x093028C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//04 Preview ISO1600
        0x000006E7,	0x000011A0,	0x093028C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//05 Capture ISO100 
        0x000006E7,	0x000011A0,	0x092328C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//06 Capture ISO200
        0x000006E7,	0x000011A0,	0x092428C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }}, 
    {set:{//07 Capture ISO400
        0x000006E7,	0x000011A0,	0x092428C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//08 Capture ISO800        
        0x000006E7,	0x000011A0,	0x092C28C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//09 Capture ISO1600   		
        0x000006E7,	0x000011A0,	0x092D28C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//10
        0x000006E7,	0x000011A0,	0x092D28C8,	0x000008AC,	0x05050507,	0x090B0C0C,	0x05050507,	
        0x090B0C0C,	0x05050507,	0x090B0C0C,	0x05050507,	0x090B0C0C,	0x0206090D,	0x10101010
    }},
    {set:{//11 Disable (Do not modify me)
        0x000000C0, 0x000011A0, 0x094428A0, 0x000007AF, 0x03050709, 0x0B0D0F11, 0x03050709, 
        0x0B0D0F11, 0x03050709, 0x0B0D0F11, 0x03050709, 0x0B0D0F11, 0x0406090D, 0x10101010
    }}
},
NR2:{//NR2
    {set:{//00 Preview ISO100/ISO200/ISO400
        0x00200002,	0x00009414,	0x00927788,	0x02030405,	0x0408080F,	0x02105182,	0x08080808,	0x10101010,	0x01AF5B58,	0x0000056B,	0x01020303

    }},
    {set:{//01 Preview ISO800
        0x00200003,	0x00009414,	0x00927788,	0x1320365A,	0x1321303F,	0x02106295,	0x08080808,	0x10101010,	0x01555B58,	0x00000555,	0x01020303
    }},
    {set:{//02 Preview ISO1600
        0x00200003,	0x00009414,	0x00927788,	0x1320365A,	0x1E324664,	0x02106B59,	0x08080808,	0x10101010,	0x01555B58,	0x00000555,	0x0914233E
    }},
    {set:{//03 Capture Mode0 ISO100/ISO200
        0x00200002,	0x00009414,	0x00927788,	0x02030405,	0x0408080F,	0x02105182,	0x08080808,	0x10101010,	0x01AF5B58,	0x0000056B,	0x01020303
    }},
    {set:{//04 Capture Mode0 ISO400
        0x00200002,	0x00009414,	0x00927788,	0x02030405,	0x0F1E2D3C,	0x02105182,	0x08080808,	0x10101010,	0x01AF5B58,	0x0000056B,	0x01020303
    }},
    {set:{//05 Capture Mode0 ISO800
        0x00200003,	0x00009414,	0x00927788,	0x02030405,	0x1321303F,	0x02106295,	0x08080808,	0x10101010,	0x01555B58,	0x00000555,	0x01020303
    }},
    {set:{//06 Capture Mode0 ISO1600
        0x00200003,	0x00009414,	0x00927788,	0x1320365A,	0x1E324664,	0x02106B59,	0x08080808,	0x10101010,	0x01555B58,	0x00000555,	0x0914233E
    }}, 
    {set:{//07 Capture Mode1 ISO100/ISO200
        0x00210002,	0x00009414,	0x00927788,	0x02030405,	0x0408080F,	0x02105182,	0x08080808,	0x10101010,	0x01AF5B58,	0x0000056B,	0x01020303
    }},
    {set:{//08 Capture Mode1 ISO400
        0x00210002,	0x00009414,	0x00927788,	0x02030405,	0x0F1E2D3C,	0x02105182,	0x08080808,	0x10101010,	0x01AF5B58,	0x0000056B,	0x01020303
    }},
    {set:{//09 Capture Mode1 ISO800
        0x00210003,	0x00009414,	0x00927788,	0x02030405,	0x1321303F,	0x02106295,	0x08080808,	0x10101010,	0x01555B58,	0x00000555,	0x01020303
    }},
    {set:{//10 Capture Mode1 ISO1600
        0x00210003,	0x00009414,	0x00927788,	0x1320365A,	0x1E324664,	0x02106B59,	0x08080808,	0x10101010,	0x01555B58,	0x00000555,	0x0914233E
    }},
    {set:{//11 Disable (Do not modify me)
        0x00000000, 0x0001FF01, 0x00523264, 0x04090B0F, 0x050A0B10, 0x02107294, 0x08101820, 0x10080604, 0x01AF5B43, 0x0000056B, 0x0306070A
    }}
},
EE:{//EE
    {set:{//00 Preview                              // middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02030403
    }},
    {set:{//01 Capture ISO100                       // middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02030403
    }},
    {set:{//02 Capture ISO200                       // middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02030402
    }},
    {set:{//03 Capture ISO400                       // middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02030201
    }},
    {set:{//04 Capture ISO800                       // middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x01020201
    }},
    {set:{//05 Capture ISO1600                      // middle sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x00010101
    }},
    {set:{//06 no one uses this, this is Min EE     // low sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x01020201
    }},
    {set:{//07 no one uses this, this is Max EE     // high sharpness
        0x00000008, 0x4132322D, 0x00641909, 0x02030403
    }}
}, 
Saturation:{
    {set:{//00                                      // middle saturation
        0x00000709,	0x1020E0F0,	0x2E4C524C,	0x2E000000,	0xFF00FF00,	0x00000000,	0x00000000,	0x001E140A    	
    }},
    {set:{//01                                      // middle saturation
        0x00000709,	0x1020E0F0,	0x2A4A504A,	0x2A000000,	0xFF00FF00,	0x00000000,	0x00000000,	0x001E140A    	
    }},
    {set:{//02                                      // middle saturation
        0x00000709,	0x1020E0F0,	0x28484E48,	0x28000000,	0xFF00FF00,	0x00000000,	0x00000000,	0x001E140A    	
    }},
    {set:{//03                                      // middle saturation
        0x00000709,	0x1020E0F0,	0x26464C46,	0x26000000,	0xFF00FF00,	0x00000000,	0x00000000,	0x001E140A    	
    }},
    {set:{//04                                      // middle saturation
        0x00000709,	0x1020E0F0,	0x24444844,	0x24000000,	0xFF00FF00,	0x00000000,	0x00000000,	0x001E140A
    }},
    {set:{//05 no one uses this, this is Min Sat.   // low saturation
        0x00010709,	0x1020E0F0,	0x20404040,	0x20000000,	0xFF00FF00,	0x00000000,	0x00000000,	0x001E140A
    }},
    {set:{//06 no one uses this, this is Max Sat.   // high saturation
        0x00000709,	0x1020E0F0,	0x32505650,	0x32000000,	0xFF00FF00,	0x00000000,	0x00000000,	0x001E140A
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
        0x004AA585, 0x008F2D02, 0x00889038, 
    }},
    {set:{//01
        0x005ABA00, 0x008B2209, 0x00839437, 
    }},
    {set:{//02
        0x004AAB01, 0x00841F05, 0x00018F2E, 
    }}
},
Gamma:{
    {set:{//00
        0x4426140a, 0x8a7c6d5a, 0xc0b1a096, 0xe2dcd4cb, 0xfcf7f3ee
    }},
    {set:{//01
        0x4426140a, 0x8a7c6d5a, 0xc0b1a096, 0xe2dcd4cb, 0xfcf7f3ee
    }},
    {set:{//02
        0x4426140a, 0x8a7c6d5a, 0xc0b1a096, 0xe2dcd4cb, 0xfcf7f3ee
    }},
    {set:{//03
        0x4426140a, 0x8a7c6d5a, 0xc0b1a096, 0xe2dcd4cb, 0xfcf7f3ee
    }},
    {set:{//04
        0x4426140a, 0x8a7c6d5a, 0xc0b1a096, 0xe2dcd4cb, 0xfcf7f3ee
    }}, 
}

