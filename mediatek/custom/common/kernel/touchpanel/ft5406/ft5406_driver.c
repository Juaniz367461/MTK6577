/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

#define TPD_HAVE_BUTTON

#include "tpd.h"
#include <linux/interrupt.h>
#include <cust_eint.h>
#include <linux/i2c.h>
#include <linux/sched.h>
#include <linux/kthread.h>
#include <linux/rtpm_prio.h>
#include <linux/wait.h>
#include <linux/time.h>
#include <linux/delay.h>
#include <mach/mt6577_pm_ldo.h>
//#include <mach/mt6575_pll.h>

#ifdef TPD_GPT_TIMER_RESUME
#include <mach/hardware.h>
#include <mach/mt6577_gpt.h>
#include <linux/timer.h>
#endif
#include "tpd_custom_ft5406.h"

#include "cust_gpio_usage.h"

//#define AGOLD_TP_DEBUG
#if defined(AGOLD_TP_DEBUG)
#define TPD_DEBUG(fmt, arg...)  printk("[agold tpd:5406]" "[%s]" fmt "\r\n", __FUNCTION__ ,##arg)
#define TPD_DMESG(fmt, arg...)  printk("[agold tpd dmesg:5406]" "[%s]" fmt "\r\n", __FUNCTION__ ,##arg)
#else
#define TPD_DEBUG(fmt, arg...)
#define TPD_DMESG(fmt, arg...)
#endif


#define TPD_POWER_SOURCE		MT65XX_POWER_LDO_VGP2

extern struct tpd_device *tpd;
 
struct i2c_client *i2c_client = NULL;
struct task_struct *thread = NULL;
 
static DECLARE_WAIT_QUEUE_HEAD(waiter);
 
static void tpd_eint_interrupt_handler(void);
  
extern void mt65xx_eint_unmask(unsigned int line);
extern void mt65xx_eint_mask(unsigned int line);
extern void mt65xx_eint_set_hw_debounce(kal_uint8 eintno, kal_uint32 ms);
extern kal_uint32 mt65xx_eint_set_sens(kal_uint8 eintno, kal_bool sens);
extern void mt65xx_eint_registration(kal_uint8 eintno, kal_bool Dbounce_En,
								  kal_bool ACT_Polarity, void (EINT_FUNC_PTR)(void),
								  kal_bool auto_umask);

 
static int __devinit tpd_probe(struct i2c_client *client, const struct i2c_device_id *id);
static int tpd_detect(struct i2c_client *client, int kind, struct i2c_board_info *info);
static int __devexit tpd_remove(struct i2c_client *client);
static int touch_event_handler(void *unused);
 
static int tpd_flag = 0;
static int point_num = 0;
static int p_point_num = 0;

#define TPD_CLOSE_POWER_IN_SLEEP

#define TPD_OK 0
//register define

#define DEVICE_MODE 0x00
#define GEST_ID 0x01
#define TD_STATUS 0x02
#define FW_ID_ADDR	0xA6


//register define

#define AGOLD_FINGER_NUM_MAX	5 

struct touch_info {
    int y[AGOLD_FINGER_NUM_MAX];
    int x[AGOLD_FINGER_NUM_MAX];
    int p[AGOLD_FINGER_NUM_MAX];
    int count;
};
 
static const struct i2c_device_id tpd_id[] = {{"mtk-tpd",0},{}};

#if defined(E1910) && !defined(AGOLD_TP_CFG_FOR_E1910_SMT)
unsigned short force[] = {0,0x72,I2C_CLIENT_END,I2C_CLIENT_END}; 
static const unsigned short * const forces[] = { force, NULL };
//static struct i2c_client_address_data addr_data = { .forces = forces, };
static struct i2c_board_info __initdata i2c_tpd = { I2C_BOARD_INFO("mtk-tpd", (0x72>>1))};
#else
unsigned short force[] = {0,0x70,I2C_CLIENT_END,I2C_CLIENT_END}; 
static const unsigned short * const forces[] = { force, NULL };
//static struct i2c_client_address_data addr_data = { .forces = forces, };
static struct i2c_board_info __initdata i2c_tpd = { I2C_BOARD_INFO("mtk-tpd", (0x70>>1))};
#endif
 

#include <mach/mt6577_boot.h>
static int boot_mode = 0; 

#ifdef TPD_HAVE_BUTTON
extern void tpd_button(unsigned int x, unsigned int y, unsigned int down); 
#if 0
#if 1
#define TPD_KEYS {KEY_HOME,KEY_MENU,KEY_BACK,KEY_SEARCH}
#define TPD_KEYS_DIM {{30,850,60,100},{180,850,60,100},{320,850,60,100},{450,850,60,100}}
#define TPD_KEY_COUNT 4
#else
#define TPD_KEYS {KEY_HOME,KEY_MENU,KEY_BACK}
#define TPD_KEYS_DIM {{80,850,60,100},{240,850,60,100},{400,850,60,100}}
#define TPD_KEY_COUNT 3
#endif
#endif
static int tpd_keys_local[TPD_KEY_COUNT] = TPD_KEYS;
static int tpd_keys_dim_local[TPD_KEY_COUNT][4] = TPD_KEYS_DIM;
#endif
 
static struct i2c_driver tpd_i2c_driver = {
	.driver.name = "mtk-tpd",
	.probe = tpd_probe,
	.remove = __devexit_p(tpd_remove),
	.id_table = tpd_id,
	.detect = tpd_detect,
	.address_list = (const unsigned short*) forces,
};
 
#if 0 //[Agold][Talcon.Hu]
static unsigned short i2c_addr[] = {0x72}; 
#endif  

static  void tpd_down(int x, int y, int p) {

#ifdef TPD_HAVE_BUTTON
#if defined(AGOLD_TP_CFG_FOR_E1910_SMT) || defined(ROTATION_FOR_E1910_CQ)
	if(MTK_LCM_PHYSICAL_ROTATION == 270 || MTK_LCM_PHYSICAL_ROTATION == 90)
        {
		#if defined(AGOLD_TP_HEIGHT)
		if(boot_mode!=NORMAL_BOOT && x>=AGOLD_TP_HEIGHT) { 
			tpd_button(x, y, 1);
		#else
		if(boot_mode!=NORMAL_BOOT && x>=TPD_RES_Y) { 
			tpd_button(x, y, 1);
		#endif

			return;
		}
	}
        else
#endif
	{
		#if defined(AGOLD_TP_HEIGHT)
		if(boot_mode!=NORMAL_BOOT && y>=AGOLD_TP_HEIGHT) { 
			tpd_button(x, y, 1);
		#else
		if(boot_mode!=NORMAL_BOOT && y>=TPD_RES_Y) { 
			tpd_button(x, y, 1);
		#endif

			return;
		}
	}

#endif 

	// input_report_abs(tpd->dev, ABS_PRESSURE, p);
	 input_report_key(tpd->dev, BTN_TOUCH, 1);
	 input_report_abs(tpd->dev, ABS_MT_TOUCH_MAJOR, 1);

#if defined(AGOLD_TP_CFG_FOR_E1910_SMT) || defined(ROTATION_FOR_E1910_CQ)
	if(boot_mode!=NORMAL_BOOT && (MTK_LCM_PHYSICAL_ROTATION == 270 || MTK_LCM_PHYSICAL_ROTATION == 90) )
	{
		int temp;

		temp = y;
		y = x;
		x = TPD_RES_X-temp;
	}
#endif

	 input_report_abs(tpd->dev, ABS_MT_POSITION_X, x);
	 input_report_abs(tpd->dev, ABS_MT_POSITION_Y, y);
	 //printk("D[%4d %4d %4d] ", x, y, p);
	 input_mt_sync(tpd->dev);
	 TPD_DOWN_DEBUG_TRACK(x,y);
 }
 
 static  int tpd_up(int x, int y,int p) {
	 //input_report_abs(tpd->dev, ABS_PRESSURE, 0);
		 input_report_key(tpd->dev, BTN_TOUCH, 0);
		 //input_report_abs(tpd->dev, ABS_MT_TOUCH_MAJOR, 0);
		 //input_report_abs(tpd->dev, ABS_MT_POSITION_X, x);
		 //input_report_abs(tpd->dev, ABS_MT_POSITION_Y, y);
		 //printk("U[%4d %4d %4d] ", x, y, 0);
		 input_mt_sync(tpd->dev);
		 TPD_UP_DEBUG_TRACK(x,y);
 }

static int tpd_touchinfo(struct touch_info *cinfo, struct touch_info *pinfo)
{
	int i = 0;
	char data[32] = {0};

	u16 high_byte,low_byte;

	p_point_num = point_num;
        memcpy(pinfo, cinfo, sizeof(struct touch_info));
        memset(cinfo, 0, sizeof(struct touch_info));

	i2c_smbus_read_i2c_block_data(i2c_client, 0x00, 8, &(data[0]));
	i2c_smbus_read_i2c_block_data(i2c_client, 0x08, 8, &(data[8]));
	i2c_smbus_read_i2c_block_data(i2c_client, 0x10, 8, &(data[16]));
	i2c_smbus_read_i2c_block_data(i2c_client, 0x18, 8, &(data[24]));
	//TPD_DEBUG("FW version=%x]\n",data[24]);

	TPD_DEBUG("received raw data from touch panel as following:\n");
	TPD_DEBUG("[data[0]=%x,data[1]= %x ,data[2]=%x ,data[3]=%x ,data[4]=%x ,data[5]=%x]\n",data[0],data[1],data[2],data[3],data[4],data[5]);
	TPD_DEBUG("[data[9]=%x,data[10]= %x ,data[11]=%x ,data[12]=%x]\n",data[9],data[10],data[11],data[12]);
	TPD_DEBUG("[data[15]=%x,data[16]= %x ,data[17]=%x ,data[18]=%x]\n",data[15],data[16],data[17],data[18]);

	/* Device Mode[2:0] == 0 :Normal operating Mode*/
	if(data[0] & 0x70 != 0) return false; 

	/*get the number of the touch points*/
	point_num= data[2] & 0x0f;
	
	TPD_DEBUG("point_num =%d\n",point_num);

	if(AGOLD_FINGER_NUM_MAX < point_num)
	{
		TPD_DEBUG("point_num is error\n");
		return false;
	}
	
//	if(point_num == 0) return false;

	TPD_DEBUG("Procss raw data...\n");

		
	for(i = 0; i < point_num; i++)
	{
		cinfo->p[i] = data[3+6*i] >> 6; //event flag 

       		/*get the X coordinate, 2 bytes*/
		high_byte = data[3+6*i];
		high_byte <<= 8;
		high_byte &= 0x0f00;
		low_byte = data[3+6*i + 1];
		cinfo->x[i] = high_byte |low_byte;

		//cinfo->x[i] =  cinfo->x[i] * 480 >> 11; //calibra
		/*get the Y coordinate, 2 bytes*/		
		high_byte = data[3+6*i+2];
		high_byte <<= 8;
		high_byte &= 0x0f00;
		low_byte = data[3+6*i+3];
		cinfo->y[i] = high_byte |low_byte;

		//cinfo->y[i]=  cinfo->y[i] * 800 >> 11;	
		cinfo->count++;
	
#if defined(TPD_RES_X) && defined(AGOLD_TP_WIDTH) /*[Agold][Talcon.Hu]*/
		cinfo->x[i] = cinfo->x[i]*AGOLD_TP_WIDTH/TPD_RES_X;
#endif
#if defined(TPD_RES_Y) && defined(AGOLD_TP_HEIGHT) /*[Agold][Talcon.Hu]*/
		cinfo->y[i] = cinfo->y[i]*AGOLD_TP_HEIGHT/TPD_RES_Y;
#endif

		TPD_DEBUG(" cinfo->x[i=%d] = %d, cinfo->y[i] = %d, cinfo->p[i] = %d\n", i,cinfo->x[i], cinfo->y[i], cinfo->p[i]);
	}

	//TPD_DEBUG(" cinfo->x[0] = %d, cinfo->y[0] = %d, cinfo->p[0] = %d\n", cinfo->x[0], cinfo->y[0], cinfo->p[0]);	
	//TPD_DEBUG(" cinfo->x[1] = %d, cinfo->y[1] = %d, cinfo->p[1] = %d\n", cinfo->x[1], cinfo->y[1], cinfo->p[1]);		
	//TPD_DEBUG(" cinfo->x[2]= %d, cinfo->y[2]= %d, cinfo->p[2] = %d\n", cinfo->x[2], cinfo->y[2], cinfo->p[2]);	
	  
#if defined(AGOLD_SMT_TP_CONFIG)
	for(i = 0; i < point_num; i++)
	{
		cinfo->x[i] = cinfo->x[i] *36/51;//5.1CM/5.4CM
		cinfo->y[i] = cinfo->y[i] *27/37;//7.4CM/9.0CM
	}
#endif

#if !defined(AGOLD_TP_CFG_FOR_E1910_SMT) && !defined(ROTATION_FOR_E1910_CQ)
	if(MTK_LCM_PHYSICAL_ROTATION == 270 || MTK_LCM_PHYSICAL_ROTATION == 90)
	{
		for(i = 0; i < point_num; i++)
		{
			int temp;

			temp = cinfo->x[i];
			cinfo->x[i] = TPD_RES_X-cinfo->y[i];
			cinfo->y[i] = temp;
		}

		TPD_DEBUG("rot cinfo->x[0] = %d, cinfo->y[0] = %d, cinfo->p[0] = %d\n", cinfo->x[0], cinfo->y[0], cinfo->p[0]);	
		TPD_DEBUG("rot cinfo->x[1] = %d, cinfo->y[1] = %d, cinfo->p[1] = %d\n", cinfo->x[1], cinfo->y[1], cinfo->p[1]);
	}
#endif
	  
	return true;
};

static int touch_event_handler(void *unused)
{
	struct touch_info cinfo, pinfo;
	int i = 0;

	struct sched_param param = { .sched_priority = RTPM_PRIO_TPD };
	sched_setscheduler(current, SCHED_RR, &param);
 
	 do
	 {
		mt65xx_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM); 
		set_current_state(TASK_INTERRUPTIBLE); 
		wait_event_interruptible(waiter,tpd_flag!=0);
		 
		tpd_flag = 0;

		set_current_state(TASK_RUNNING);

		if (tpd_touchinfo(&cinfo, &pinfo))
		{
			TPD_DEBUG("point_num = %d\n",point_num);
			  
			if(point_num >0) 
			{
				#if 0
				tpd_down(cinfo.x[0], cinfo.y[0], 1);
				if(point_num>1)
				{
					tpd_down(cinfo.x[1], cinfo.y[1], 1);
					if(point_num >2) 
						tpd_down(cinfo.x[2], cinfo.y[2], 1);
				}
				#else
				while(i<point_num)
				{
					tpd_down(cinfo.x[i], cinfo.y[i], 1);
					i++;
				}
				i = 0;
				#endif
				TPD_DEBUG("press --->\n");
				
		    	} 
			else  
			{
				TPD_DEBUG("release --->\n"); 

				if(p_point_num >1)
				{
					i = 0;
					while(i<p_point_num){
						tpd_up(pinfo.x[i], pinfo.y[i], 1);
						i++;
					}
				}
				else
				{
					tpd_up(pinfo.x[0], pinfo.y[0], 1);
				}
				i = 0;

			#ifdef TPD_HAVE_BUTTON
				if(boot_mode!=NORMAL_BOOT && tpd->btn_state) 
				{ 
					tpd_button(pinfo.x[0], pinfo.y[0], 0);
				}
			#endif

            		}

			input_sync(tpd->dev);
        	}

 	}while(!kthread_should_stop());

	return 0;
}
 
static int tpd_detect (struct i2c_client *client, int kind, struct i2c_board_info *info) 
{
	printk("Eric trace: tpd_detect\n");
	strcpy(info->type, TPD_DEVICE);	
	return 0;
}

static void tpd_eint_interrupt_handler(void)
{
	TPD_DEBUG("TPD interrupt has been triggered\n");
	tpd_flag = 1;
	wake_up_interruptible(&waiter);
}

static int __devinit tpd_probe(struct i2c_client *client, const struct i2c_device_id *id)
{	 
	int retval = TPD_OK;
	char data;
	i2c_client = client;
	int i;

	printk("Eric trace: tpd_probe\n");

	#ifdef TPD_CLOSE_POWER_IN_SLEEP	 
	mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
	TPD_DEBUG("TPD_CLOSE_POWER_IN_SLEEP\n");
        for(i = 0; i < 2; i++) /*Do Power on again to avoid tp bug*/
	{
		hwPowerDown(TPD_POWER_SOURCE,"TP");
		mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ZERO);
		mdelay(10);
		mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
		mdelay(50);
		hwPowerOn(TPD_POWER_SOURCE,VOL_2800,"TP"); 

		msleep(100);
	}

	#else
#if 1 //[Agold][huyl][e1109 V1.1和v1.2兼容]
	hwPowerDown(TPD_POWER_SOURCE,"TP");

	printk("Eric trace: tpd power on!\n");
	hwPowerOn(TPD_POWER_SOURCE,VOL_2800,"TP"); 
	msleep(100);
#endif
	/*
	mt_set_gpio_mode(GPIO_CTP_EN_PIN, GPIO_CTP_EN_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_EN_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_EN_PIN, GPIO_OUT_ONE);
	msleep(100);
	*/
	mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
	#endif

	mt_set_gpio_mode(GPIO_CTP_EINT_PIN, GPIO_CTP_EINT_PIN_M_EINT);
	mt_set_gpio_dir(GPIO_CTP_EINT_PIN, GPIO_DIR_IN);
	mt_set_gpio_pull_enable(GPIO_CTP_EINT_PIN, GPIO_PULL_ENABLE);
	mt_set_gpio_pull_select(GPIO_CTP_EINT_PIN, GPIO_PULL_UP);
	msleep(50);
	
	printk("Eric trace addr:0x%02x",i2c_client->addr);

	if((i2c_smbus_read_i2c_block_data(i2c_client, FW_ID_ADDR, 1, &data))< 0)
	{
#if 0 //[Agold][Talcon.Hu]
		for(i = 0; i < sizeof(i2c_addr)/ sizeof(i2c_addr[0]); i++)
		{
			i2c_client->addr = i2c_addr[i];
			TPD_DEBUG("addr:0x%02x",i2c_client->addr);
			if((i2c_smbus_read_i2c_block_data(i2c_client, 0x00, 1, &data))>= 0)
			{
				goto i2c_transfer_sucess;
			}
		}
#endif  

		printk("Eric trace: I2C transfer error, line: %d\n", __LINE__);
		return -1; 
	}
#if 0 //[Agold][Talcon.Hu]
i2c_transfer_sucess:
#endif
	tpd_load_status = 1;

	thread = kthread_run(touch_event_handler, 0, TPD_DEVICE);
	if (IS_ERR(thread))
	{ 
		retval = PTR_ERR(thread);
		TPD_DMESG(TPD_DEVICE " failed to create kernel thread: %d\n", retval);
	}

	mt65xx_eint_set_sens(CUST_EINT_TOUCH_PANEL_NUM, CUST_EINT_TOUCH_PANEL_SENSITIVE);
	mt65xx_eint_set_hw_debounce(CUST_EINT_TOUCH_PANEL_NUM, CUST_EINT_TOUCH_PANEL_DEBOUNCE_CN);
	mt65xx_eint_registration(CUST_EINT_TOUCH_PANEL_NUM, CUST_EINT_TOUCH_PANEL_DEBOUNCE_EN, CUST_EINT_TOUCH_PANEL_POLARITY, tpd_eint_interrupt_handler, 1); 
	mt65xx_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);
	msleep(50);

	TPD_DMESG("Touch Panel Device Probe %s\n", (retval < TPD_OK) ? "FAIL" : "PASS");
	return 0;
}

static int __devexit tpd_remove(struct i2c_client *client)
{
	TPD_DEBUG("TPD removed\n");

	return 0;
}
 
 
static int tpd_local_init(void)
{
	TPD_DMESG("Focaltech FT5406 I2C Touchscreen Driver (Built %s @ %s)\n", __DATE__, __TIME__);

	boot_mode = get_boot_mode();
	if(boot_mode==3) boot_mode = NORMAL_BOOT;
#ifdef TPD_HAVE_BUTTON  
	tpd_button_setting(TPD_KEY_COUNT, tpd_keys_local, tpd_keys_dim_local);// initialize tpd button data
#endif

	if(i2c_add_driver(&tpd_i2c_driver)!=0)
	{
		TPD_DMESG("unable to add i2c driver.\n");
		return -1;
	}



	TPD_DMESG("end %s, %d\n", __FUNCTION__, __LINE__);  
	tpd_type_cap = 1;

	return 0; 
}

#ifdef TPD_GPT_TIMER_RESUME
// GPTimer
void ctp_thread_wakeup(UINT16 i)
{
	//printk("[Agold spl]**** ctp_thread_wakeup****\n" );
	GPT_NUM  gpt_num = GPT6;	
	mt65xx_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);  
	GPT_Stop(gpt_num); 
}

void CTP_Thread_XGPTConfig(void)
{
	GPT_CONFIG config;	
	GPT_NUM  gpt_num = GPT6;    
	GPT_CLK_SRC clkSrc = GPT_CLK_SRC_RTC;
	//GPT_CLK_DIV clkDiv = GPT_CLK_DIV_128;
	GPT_CLK_DIV clkDiv = GPT_CLK_DIV_64;

	//printk("[Agold spl]***CTP_Thread_XGPTConfig***\n" );

    GPT_Init (gpt_num, ctp_thread_wakeup);
    config.num = gpt_num;
    config.mode = GPT_REPEAT;
	config.clkSrc = clkSrc;
    config.clkDiv = clkDiv;
    //config.u4Timeout = 10*128;
    config.u4CompareL = 256; // 10s : 512*64=32768
    config.u4CompareH = 0;
	config.bIrqEnable = TRUE;
    
    if (GPT_Config(config) == FALSE )
        return;                       
        
    GPT_Start(gpt_num);  

    return ;  
}
#endif

static int tpd_resume(struct i2c_client *client)
{
	int retval = TPD_OK;
	char data = 0;
	int retry_num = 0,ret = 0;

	TPD_DEBUG("TPD wake up\n");

#ifdef TPD_GPT_TIMER_RESUME
	mt65xx_eint_mask(CUST_EINT_TOUCH_PANEL_NUM);  	
	
	mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
	
	msleep(10);
	hwPowerDown(TPD_POWER_SOURCE,"TP");
	hwPowerOn(TPD_POWER_SOURCE,VOL_2800,"TP"); 
	// Run GPT timer
	CTP_Thread_XGPTConfig();
#else
	#ifdef TPD_CLOSE_POWER_IN_SLEEP	
	mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
	
	do{
		msleep(10);
		hwPowerOn(TPD_POWER_SOURCE,VOL_2800,"TP"); 
		msleep(300);

		if((ret = i2c_smbus_read_i2c_block_data(i2c_client, FW_ID_ADDR, 1, &data))< 0)
		{
			TPD_DEBUG("i2c transf error before reset :ret=%d,retry_num == %d\n",ret,retry_num);

			hwPowerDown(TPD_POWER_SOURCE,"TP");
		}	
		else
		{
			TPD_DEBUG("i2c transfer success after reset :ret=%d,retry_num == %d\n",ret,retry_num);
			break;
		}
		retry_num++;
	}while(retry_num < 10);

	if((ret = i2c_smbus_read_i2c_block_data(i2c_client, FW_ID_ADDR, 1, &data))< 0)
	{
		TPD_DEBUG("i2c transf error before reset :ret=%d,retry_num == %d\n",ret,retry_num);

		mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
		mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
		mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
		msleep(100);

		mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
		mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
		mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ZERO);  
		msleep(50);  
		mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
		mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
		mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
		msleep(400); 

		if((ret = i2c_smbus_read_i2c_block_data(i2c_client, FW_ID_ADDR, 1, &data))< 0)
		{
			TPD_DEBUG("i2c transf error after reset :ret = %d,retry_num == %d\n",ret,retry_num);
		}
		else
		{
			TPD_DEBUG("i2c transfer success after reset :ret = %d,retry_num == %d\n",ret,retry_num);
		}
	}
	TPD_DEBUG("retry_num == %d\n",retry_num);

	#else
	mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
	msleep(100);
        
	mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ZERO);  
	msleep(50);  
	mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
	msleep(400);  
	#endif
	mt65xx_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);  
#endif
	return retval;
}
 
static int tpd_suspend(struct i2c_client *client, pm_message_t message)
{
	int retval = TPD_OK;
	static char data = 0x3;

	TPD_DEBUG("TPD enter sleep\n");
	mt65xx_eint_mask(CUST_EINT_TOUCH_PANEL_NUM);
	#ifdef TPD_CLOSE_POWER_IN_SLEEP	
	hwPowerDown(TPD_POWER_SOURCE,"TP");

	mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ZERO);  
	#else
	i2c_smbus_write_i2c_block_data(i2c_client, 0xA5, 1, &data);  //TP enter sleep mode
	/*
	mt_set_gpio_mode(GPIO_CTP_EN_PIN, GPIO_CTP_EN_PIN_M_GPIO);
	mt_set_gpio_dir(GPIO_CTP_EN_PIN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_CTP_EN_PIN, GPIO_OUT_ZERO);
        */
	#endif
	return retval;
} 


static struct tpd_driver_t tpd_device_driver = {
	.tpd_device_name = "FT5406",
	.tpd_local_init = tpd_local_init,
	.suspend = tpd_suspend,
	.resume = tpd_resume,
	#ifdef TPD_HAVE_BUTTON
	.tpd_have_button = 1,
	#else
	.tpd_have_button = 0,
	#endif		
};

/* called when loaded into kernel */
static int __init tpd_driver_init(void) {
	printk("MediaTek FT5406 touch panel driver init\n");
	i2c_register_board_info(0, &i2c_tpd, 1);
	if(tpd_driver_add(&tpd_device_driver) < 0)
		TPD_DMESG("add FT5406 driver failed\n");

	return 0;
}
 
/* should never be called */
static void __exit tpd_driver_exit(void) {
	TPD_DMESG("MediaTek FT5406 touch panel driver exit\n");
	//input_unregister_device(tpd->dev);
	tpd_driver_remove(&tpd_device_driver);
}
 
 module_init(tpd_driver_init);
 module_exit(tpd_driver_exit);
