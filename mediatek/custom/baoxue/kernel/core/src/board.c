/* system header files */
#include <linux/kernel.h>
#include <linux/mm.h>
#include <linux/interrupt.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/device.h>
#include <linux/delay.h>
#include <linux/pm.h>
#include <linux/mutex.h>
#include <linux/uaccess.h>
#include <linux/syscalls.h>
#include <linux/mtd/nand.h>

#include <asm/irq.h>
#include <asm/io.h>
#include <asm/mach-types.h>
#include <asm/mach/arch.h>
#include <asm/mach/irq.h>
#include <asm/mach/map.h>
#include <asm/mach/time.h>
#include <asm/setup.h>

#include <mach/system.h>
#include <mach/board.h>
#include <mach/hardware.h>
#include <mach/mt6577_gpio.h>
#include <mach/mt_bt.h>
#include <mach/eint.h>
#include <mach/mtk_rtc.h>
#include <mach/mt6577_typedefs.h>

#include <cust_gpio_usage.h>
#include <cust_eint.h>

#if defined(CONFIG_MTK_COMBO) || defined(CONFIG_MTK_COMBO_MODULE)
#include <mach/mt_combo.h>
#endif

#if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT)
static sdio_irq_handler_t mtk_wcn_cmb_sdio_eirq_handler = NULL;
static pm_callback_t mtk_wcn_cmb_sdio_pm_cb = NULL;
static void *mtk_wcn_cmb_sdio_pm_data = NULL;
static void *mtk_wcn_cmb_sdio_eirq_data = NULL;

const static u32 mtk_wcn_cmb_sdio_eint_pin = GPIO_WIFI_EINT_PIN;
const static u32 mtk_wcn_cmb_sdio_eint_num = CUST_EINT_WIFI_NUM;
const static u32 mtk_wcn_cmb_sdio_eint_m_eint = GPIO_WIFI_EINT_PIN_M_EINT;
const static u32 mtk_wcn_cmb_sdio_eint_m_gpio = GPIO_WIFI_EINT_PIN_M_GPIO;
/*
index: port number of combo chip (1:SDIO1, 2:SDIO2, no SDIO0)
value: slot power status of  (0:off, 1:on, 0xFF:invalid)
*/
    #if (CONFIG_MTK_WCN_CMB_SDIO_SLOT == 0)
        static unsigned char combo_port_pwr_map[4] = {0x0, 0xFF, 0xFF, 0xFF};
    #elif (CONFIG_MTK_WCN_CMB_SDIO_SLOT == 1)
        static unsigned char combo_port_pwr_map[4] = {0xFF, 0x0, 0xFF, 0xFF};
    #elif (CONFIG_MTK_WCN_CMB_SDIO_SLOT == 2)
        static unsigned char combo_port_pwr_map[4] = {0xFF, 0xFF, 0x0, 0xFF};
    #elif (CONFIG_MTK_WCN_CMB_SDIO_SLOT == 3)
        static unsigned char combo_port_pwr_map[4] = {0xFF, 0xFF, 0xFF, 0x0};
    #else
        #error "unsupported CONFIG_MTK_WCN_CMB_SDIO_SLOT" CONFIG_MTK_WCN_CMB_SDIO_SLOT
    #endif
#else
static sdio_irq_handler_t mt_wifi_irq_handler = NULL;
static pm_message_t mt_wifi_pm_state = { .event = PM_EVENT_HIBERNATE };
static pm_callback_t mt_wifi_pm_cb = NULL;
static void *mt_wifi_pm_data = NULL;
static void *mt_wifi_irq_data = NULL;
static int mt_wifi_pm_late_cb = 0;
#endif

/*=======================================================================*/
/* Board Specific Devices Power Management                               */
/*=======================================================================*/
extern kal_bool pmic_chrdet_status(void);


#ifdef RDA_BT_SUPPORT
extern void export_bt_hci_wakeup_chip(void);
extern void rda_5990_set_wake_lock(void);

static void combo_bgf_eirq_handler(void)
{
    export_bt_hci_wakeup_chip(); 
    rda_5990_set_wake_lock();
    mt65xx_eint_unmask(CUST_EINT_COMBO_BGF_NUM);
}

static void mt_combo_bgf_request_irq(void *data)
{
    mt65xx_eint_set_sens(CUST_EINT_COMBO_BGF_NUM, CUST_EINT_EDGE_SENSITIVE);
    mt65xx_eint_set_hw_debounce(CUST_EINT_COMBO_BGF_NUM, CUST_EINT_COMBO_BGF_DEBOUNCE_CN);
    mt65xx_eint_registration(CUST_EINT_COMBO_BGF_NUM,
        CUST_EINT_COMBO_BGF_DEBOUNCE_EN,
        CUST_EINT_POLARITY_HIGH,
        combo_bgf_eirq_handler,
        0);
    mt65xx_eint_mask(CUST_EINT_COMBO_BGF_NUM); /*2*/
    return;
}

/* Combo chip shared interrupt (BGF_INT_B) */
void mt_combo_bgf_enable_irq(void)
{
    //mt_set_gpio_mode(GPIO_COMBO_BGF_EINT_PIN, GPIO_COMBO_BGF_EINT_PIN_M_GPIO);
    mt_set_gpio_pull_enable(GPIO_COMBO_BGF_EINT_PIN, GPIO_PULL_DISABLE);
    //mt_set_gpio_pull_select(GPIO_COMBO_BGF_EINT_PIN, GPIO_PULL_UP);
    mt_set_gpio_mode(GPIO_COMBO_BGF_EINT_PIN, GPIO_COMBO_BGF_EINT_PIN_M_EINT);

    mt_combo_bgf_request_irq(NULL);

    mt65xx_eint_unmask(CUST_EINT_COMBO_BGF_NUM);
    return;
}
EXPORT_SYMBOL(mt_combo_bgf_enable_irq);

void mt_combo_bgf_disable_irq(void)
{
    mt65xx_eint_mask(CUST_EINT_COMBO_BGF_NUM);
    mt_set_gpio_mode(GPIO_COMBO_BGF_EINT_PIN, GPIO_COMBO_BGF_EINT_PIN_M_GPIO);
    mt_set_gpio_dir(GPIO_COMBO_BGF_EINT_PIN, GPIO_DIR_IN);
    mt_set_gpio_pull_select(GPIO_COMBO_BGF_EINT_PIN, GPIO_PULL_DOWN);
    mt_set_gpio_pull_enable(GPIO_COMBO_BGF_EINT_PIN, GPIO_PULL_ENABLE);

    return;
}
EXPORT_SYMBOL(mt_combo_bgf_disable_irq);
#endif


#ifdef RDA_WLAN_SUPPORT
static int rda_wlan_status = 0;
extern void export_wifi_eirq_disable();
extern int rda_wifi_power_on();
extern int rda_wifi_power_off();
extern int rda_bt_power_off();
int get_wlan_status()
{
	return rda_wlan_status;
}
EXPORT_SYMBOL(get_wlan_status);
#endif
void mt6577_power_off(void)
{
	printk("mt6577_power_off\n");
#ifdef RDA_WLAN_SUPPORT
  rda_bt_power_off();
  //if(rda_wlan_status)
      rda_wifi_power_off();
  rda_wlan_status = 0;
#endif

	/* pull PWRBB low */
	rtc_bbpu_power_down();

	while (1) {
		printk("mt6577_power_off : check charger\n");
		if (pmic_chrdet_status() == KAL_TRUE)
			arch_reset(0, "charger");
	}
}

/*=======================================================================*/
/* Board Specific Devices                                                */
/*=======================================================================*/
/*GPS driver*/
/*FIXME: remove mt3326 notation */
struct mt3326_gps_hardware mt3326_gps_hw = {
    .ext_power_on =  NULL,
    .ext_power_off = NULL,
};

/*=======================================================================*/
/* Board Specific Devices Init                                           */
/*=======================================================================*/

#ifdef MTK_BT_SUPPORT
void mt_bt_power_on(void)
{
    printk(KERN_INFO "+mt_bt_power_on\n");

#if defined(CONFIG_MTK_COMBO) || defined(CONFIG_MTK_COMBO_MODULE)
    /* combo chip product */
    /*
     * Ignore rfkill0/state call. Controll BT power on/off through device /dev/stpbt.
     */
#else 
    /* standalone product */
#endif

    printk(KERN_INFO "-mt_bt_power_on\n");
}
EXPORT_SYMBOL(mt_bt_power_on);

void mt_bt_power_off(void)
{
    printk(KERN_INFO "+mt_bt_power_off\n");

#if defined(CONFIG_MTK_COMBO) || defined(CONFIG_MTK_COMBO_MODULE)
    /* combo chip product */
    /*
     * Ignore rfkill0/state call. Controll BT power on/off through device /dev/stpbt.
     */
#else
    /* standalone product */
#endif

    printk(KERN_INFO "-mt_bt_power_off\n");
}
EXPORT_SYMBOL(mt_bt_power_off);

int mt_bt_suspend(pm_message_t state)
{
    printk(KERN_INFO "+mt_bt_suspend\n");
    printk(KERN_INFO "-mt_bt_suspend\n");
    return MT_BT_OK;
}

int mt_bt_resume(pm_message_t state)
{
    printk(KERN_INFO "+mt_bt_resume\n");
    printk(KERN_INFO "-mt_bt_resume\n");
    return MT_BT_OK;
}
#endif


#if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT)
static void mtk_wcn_cmb_sdio_enable_eirq(void)
{
    mt65xx_eint_unmask(mtk_wcn_cmb_sdio_eint_num);/* CUST_EINT_WIFI_NUM */
}

static void mtk_wcn_cmb_sdio_disable_eirq(void)
{
    mt65xx_eint_mask(mtk_wcn_cmb_sdio_eint_num); /* CUST_EINT_WIFI_NUM */
}

static void mtk_wcn_cmb_sdio_eirq_handler_stub(void)
{
    if (mtk_wcn_cmb_sdio_eirq_handler) {
        mtk_wcn_cmb_sdio_eirq_handler(mtk_wcn_cmb_sdio_eirq_data);
    }
}

static void mtk_wcn_cmb_sdio_request_eirq(sdio_irq_handler_t irq_handler, void *data)
{
    mt65xx_eint_set_sens(mtk_wcn_cmb_sdio_eint_num, CUST_EINT_WIFI_SENSITIVE); /*CUST_EINT_WIFI_NUM */
    mt65xx_eint_set_hw_debounce(mtk_wcn_cmb_sdio_eint_num, CUST_EINT_WIFI_DEBOUNCE_CN); /*CUST_EINT_WIFI_NUM */
    mt_set_gpio_pull_enable(mtk_wcn_cmb_sdio_eint_pin, GPIO_PULL_ENABLE);
    mt_set_gpio_pull_select(mtk_wcn_cmb_sdio_eint_pin, GPIO_PULL_UP);
    mt65xx_eint_registration(mtk_wcn_cmb_sdio_eint_num/*CUST_EINT_WIFI_NUM */,
        CUST_EINT_WIFI_DEBOUNCE_EN,
        CUST_EINT_WIFI_POLARITY,
        mtk_wcn_cmb_sdio_eirq_handler_stub,
        0);
    mt65xx_eint_mask(mtk_wcn_cmb_sdio_eint_num);/*CUST_EINT_WIFI_NUM */

    mtk_wcn_cmb_sdio_eirq_handler = irq_handler;
    mtk_wcn_cmb_sdio_eirq_data    = data;
}

static void mtk_wcn_cmb_sdio_register_pm(pm_callback_t pm_cb, void *data)
{
    printk( KERN_INFO "mtk_wcn_cmb_sdio_register_pm (0x%p, 0x%p)\n", pm_cb, data);
    /* register pm change callback */
    mtk_wcn_cmb_sdio_pm_cb = pm_cb;
    mtk_wcn_cmb_sdio_pm_data = data;
}

static void mtk_wcn_cmb_sdio_on (int sdio_port_num) {
    pm_message_t state = { .event = PM_EVENT_USER_RESUME };

    printk(KERN_INFO "mtk_wcn_cmb_sdio_on (%d) \n", sdio_port_num);

    /* 1. disable sdio eirq */
    mtk_wcn_cmb_sdio_disable_eirq();
    mt_set_gpio_pull_enable(mtk_wcn_cmb_sdio_eint_pin, GPIO_PULL_DISABLE); /* GPIO_WIFI_EINT_PIN */
    mt_set_gpio_mode(mtk_wcn_cmb_sdio_eint_pin, mtk_wcn_cmb_sdio_eint_m_eint); /* EINT mode */

    /* 2. call sd callback */
    if (mtk_wcn_cmb_sdio_pm_cb) {
        //printk(KERN_INFO "mtk_wcn_cmb_sdio_pm_cb(PM_EVENT_USER_RESUME, 0x%p, 0x%p) \n", mtk_wcn_cmb_sdio_pm_cb, mtk_wcn_cmb_sdio_pm_data);
        mtk_wcn_cmb_sdio_pm_cb(state, mtk_wcn_cmb_sdio_pm_data);
    }
    else {
        printk(KERN_WARNING "mtk_wcn_cmb_sdio_on no sd callback!!\n");
    }
}

static void mtk_wcn_cmb_sdio_off (int sdio_port_num) {
    pm_message_t state = { .event = PM_EVENT_USER_SUSPEND };

    printk(KERN_INFO "mtk_wcn_cmb_sdio_off (%d) \n", sdio_port_num);

    /* 1. call sd callback */
    if (mtk_wcn_cmb_sdio_pm_cb) {
        //printk(KERN_INFO "mtk_wcn_cmb_sdio_off(PM_EVENT_USER_SUSPEND, 0x%p, 0x%p) \n", mtk_wcn_cmb_sdio_pm_cb, mtk_wcn_cmb_sdio_pm_data);
        mtk_wcn_cmb_sdio_pm_cb(state, mtk_wcn_cmb_sdio_pm_data);
    }
    else {
        printk(KERN_WARNING "mtk_wcn_cmb_sdio_off no sd callback!!\n");
    }

    /* 2. disable sdio eirq */
    mtk_wcn_cmb_sdio_disable_eirq();
    /*printk(KERN_INFO "[mt6620] set WIFI_EINT input pull down\n");*/
    mt_set_gpio_mode(mtk_wcn_cmb_sdio_eint_pin, mtk_wcn_cmb_sdio_eint_m_gpio); /* GPIO mode */
    mt_set_gpio_dir(mtk_wcn_cmb_sdio_eint_pin, GPIO_DIR_IN);
    mt_set_gpio_pull_select(mtk_wcn_cmb_sdio_eint_pin, GPIO_PULL_UP);
    mt_set_gpio_pull_enable(mtk_wcn_cmb_sdio_eint_pin, GPIO_PULL_ENABLE);
}

int board_sdio_ctrl (unsigned int sdio_port_num, unsigned int on) {
#if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT)
			sdio_port_num = CONFIG_MTK_WCN_CMB_SDIO_SLOT;
			printk(KERN_WARNING "mt_combo_sdio_ctrl: force set sdio port to (%d)\n", sdio_port_num);
#endif
    if ((sdio_port_num >= 4) || (combo_port_pwr_map[sdio_port_num] == 0xFF) ) {
        /* invalid sdio port number or slot mapping */
        printk(KERN_WARNING "mt_mtk_wcn_cmb_sdio_ctrl invalid port(%d, %d)\n", sdio_port_num, combo_port_pwr_map[sdio_port_num]);
        return -1;
    }
    /*printk(KERN_INFO "mt_mtk_wcn_cmb_sdio_ctrl (%d, %d)\n", sdio_port_num, on);*/

    if (!combo_port_pwr_map[sdio_port_num] && on) {
    	  printk(KERN_INFO  "board_sdio_ctrl force off before on\n");
        mtk_wcn_cmb_sdio_off(sdio_port_num);
        combo_port_pwr_map[sdio_port_num] = 0;
        /* off -> on */
        mtk_wcn_cmb_sdio_on(sdio_port_num);
        combo_port_pwr_map[sdio_port_num] = 1;
    }
    else if (combo_port_pwr_map[sdio_port_num] && !on) {
        /* on -> off */
        mtk_wcn_cmb_sdio_off(sdio_port_num);
        combo_port_pwr_map[sdio_port_num] = 0;
    }
    else {
        return -2;
    }
    return 0;
}
EXPORT_SYMBOL(board_sdio_ctrl);

#endif /* end of defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT) */

    
    

#if defined(CONFIG_WLAN)
#if !defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT)
static void mt_wifi_enable_irq(void)
{
    mt65xx_eint_unmask(CUST_EINT_WIFI_NUM);
}

static void mt_wifi_disable_irq(void)
{
    mt65xx_eint_mask(CUST_EINT_WIFI_NUM);
}

static void mt_wifi_eirq_handler(void)
{
    if (mt_wifi_irq_handler) {
        mt_wifi_irq_handler(mt_wifi_irq_data);
    }
}

static void mt_wifi_request_irq(sdio_irq_handler_t irq_handler, void *data)
{
    mt65xx_eint_set_sens(CUST_EINT_WIFI_NUM, CUST_EINT_WIFI_SENSITIVE);
    mt65xx_eint_set_hw_debounce(CUST_EINT_WIFI_NUM, CUST_EINT_WIFI_DEBOUNCE_CN);
    mt65xx_eint_registration(CUST_EINT_WIFI_NUM,
        CUST_EINT_WIFI_DEBOUNCE_EN,
        CUST_EINT_WIFI_POLARITY,
        mt_wifi_eirq_handler,
        0);
    mt65xx_eint_mask(CUST_EINT_WIFI_NUM);

    mt_wifi_irq_handler = irq_handler;
    mt_wifi_irq_data    = data;
}

static void mt_wifi_register_pm(pm_callback_t pm_cb, void *data)
{
    /* register pm change callback */
    mt_wifi_pm_cb = pm_cb;
    mt_wifi_pm_data = data;
}

#endif /* end of !defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT) */ 

int mt_wifi_resume(pm_message_t state)
{
    int evt = state.event;

    if (evt != PM_EVENT_USER_RESUME && evt != PM_EVENT_RESUME) {
        return -1;
    }

    /*printk(KERN_INFO "[WIFI] %s Resume\n", evt == PM_EVENT_RESUME ? "PM":"USR");*/
#ifdef RDA_WLAN_SUPPORT
    export_wifi_eirq_disable(); 
    
    if(rda_wifi_power_on() < 0)
    	return -1;
    
    //rtc_gpio_enable_32k(RTC_GPIO_USER_GPS);
    //rtc_gpio_enable_32k(RTC_GPIO_USER_WIFI);
    //msleep(200);
    board_sdio_ctrl(3, 1);
    rda_wlan_status = 1;
    return 0;
#endif
#if defined(CONFIG_MTK_COMBO) || defined(CONFIG_MTK_COMBO_MODULE)
    /* combo chip product: notify combo driver to turn on Wi-Fi */

    /* Use new mtk_wcn_cmb_stub APIs instead of old mt_combo ones */
    mtk_wcn_cmb_stub_func_ctrl(COMBO_FUNC_TYPE_WIFI, 1);
    /*mt_combo_func_ctrl(COMBO_FUNC_TYPE_WIFI, 1);*/

#endif

    return 0;
}

int mt_wifi_suspend(pm_message_t state)
{
    int evt = state.event;
#if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT)
    static int is_1st_suspend_from_boot = 1;
#endif

    if (evt != PM_EVENT_USER_SUSPEND && evt != PM_EVENT_SUSPEND) {
        return -1;
    }

#if defined(CONFIG_MTK_COMBO) || defined(CONFIG_MTK_COMBO_MODULE)
    #if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT)
    /* combo chip product: notify combo driver to turn on Wi-Fi */
    if (is_1st_suspend_from_boot) {
        pm_message_t state = { .event = PM_EVENT_USER_SUSPEND };

        if (mtk_wcn_cmb_sdio_pm_cb) {
            is_1st_suspend_from_boot = 0;
            /*              *** IMPORTANT DEPENDENDY***
            RFKILL: set wifi and bt suspend by default in probe()
            MT6573-SD: sd host is added to MMC stack and suspend is ZERO by default
            (which means NOT suspended).

            When boot up, RFKILL will set wifi off and this function gets
            called. In order to successfully resume wifi at 1st time, pm_cb here
            shall be called once to let MT6573-SD do sd host suspend and remove
            sd host from MMC. Then wifi can be turned on successfully.

            Boot->SD host added to MMC (suspend=0)->RFKILL set wifi off
            ->SD host removed from MMC (suspend=1)->RFKILL set wifi on
            */
            printk(KERN_INFO "1st mt_wifi_suspend (PM_EVENT_USER_SUSPEND) \n");
            (*mtk_wcn_cmb_sdio_pm_cb)(state, mtk_wcn_cmb_sdio_pm_data);
        }
        else {
            printk(KERN_WARNING "1st mt_wifi_suspend but no sd callback!!\n");
        }
    }
    else {
#ifdef RDA_WLAN_SUPPORT
        board_sdio_ctrl(3, 0);
        rda_wifi_power_off();
        rda_wlan_status = 0;
        return 0;
#endif        
        /* combo chip product, notify combo driver */

        /* Use new mtk_wcn_cmb_stub APIs instead of old mt_combo ones */
        mtk_wcn_cmb_stub_func_ctrl(COMBO_FUNC_TYPE_WIFI, 0);
        /*mt_combo_func_ctrl(COMBO_FUNC_TYPE_WIFI, 0);*/
    }
    #endif
#endif
    return 0;
}

void mt_wifi_power_on(void)
{
    pm_message_t state = { .event = PM_EVENT_USER_RESUME };

    (void)mt_wifi_resume(state);
}
EXPORT_SYMBOL(mt_wifi_power_on);

void mt_wifi_power_off(void)
{
    pm_message_t state = { .event = PM_EVENT_USER_SUSPEND };

    (void)mt_wifi_suspend(state);
}
EXPORT_SYMBOL(mt_wifi_power_off);

#endif /* end of defined(CONFIG_WLAN) */

/* Board Specific Devices                                                */
/*=======================================================================*/

/*=======================================================================*/
/* Board Specific Devices Init                                           */
/*=======================================================================*/

/*=======================================================================*/
/* Board Devices Capability                                              */
/*=======================================================================*/
#define MSDC_SDCARD_FLAG  (MSDC_SYS_SUSPEND | MSDC_WP_PIN_EN | MSDC_HIGHSPEED)  //| MSDC_CD_PIN_EN | MSDC_REMOVABLE 
#define MSDC_SDIO_FLAG    (MSDC_EXT_SDIO_IRQ | MSDC_HIGHSPEED)

#if defined(CFG_DEV_MSDC0)
#if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT) && (CONFIG_MTK_WCN_CMB_SDIO_SLOT == 0)
    /* MSDC0 settings for MT66xx combo connectivity chip */
	struct msdc_hw msdc0_hw = {
	    .clk_src        = 1,
	    .cmd_edge       = MSDC_SMPL_FALLING,
	    .data_edge      = MSDC_SMPL_FALLING,
	    .clk_drv        = 4,
	    .cmd_drv        = 4,
	    .dat_drv        = 4,
	    .data_pins      = 4,
	    .data_offset    = 0,
	    //MT6620 use External IRQ, wifi uses high speed. here wifi manage his own suspend and resume, does not support hot plug
	    .flags          = MSDC_EXT_SDIO_IRQ | MSDC_HIGHSPEED,//MSDC_SYS_SUSPEND | MSDC_WP_PIN_EN | MSDC_CD_PIN_EN | MSDC_REMOVABLE,
	    .request_sdio_eirq = mtk_wcn_cmb_sdio_request_eirq,
	    .enable_sdio_eirq  = mtk_wcn_cmb_sdio_enable_eirq,
	    .disable_sdio_eirq = mtk_wcn_cmb_sdio_disable_eirq,
	    .register_pm       = mtk_wcn_cmb_sdio_register_pm,
	};
    #else
	struct msdc_hw msdc0_hw = {
	    .clk_src        = 1,
	    .cmd_edge       = MSDC_SMPL_FALLING,
	    .data_edge      = MSDC_SMPL_FALLING,
	    .clk_drv        = 0,
	    .cmd_drv        = 0,
	    .dat_drv        = 0,
	    .data_pins      = 8,
	    .data_offset    = 0,
#ifdef MTK_EMMC_SUPPORT
	    .flags          = MSDC_SYS_SUSPEND | MSDC_HIGHSPEED,
#else
	    .flags          = MSDC_SDCARD_FLAG,
#endif
	};
	#endif
#endif
#if defined(CFG_DEV_MSDC1)
    #if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT) && (CONFIG_MTK_WCN_CMB_SDIO_SLOT == 1)
    	struct msdc_hw msdc1_hw = {
	    .clk_src        = 1,
	    .cmd_edge       = MSDC_SMPL_FALLING,
	    .data_edge      = MSDC_SMPL_FALLING,
	    .clk_drv        = 4,
	    .cmd_drv        = 4,
	    .dat_drv        = 4,
	    .data_pins      = 4,
	    .data_offset    = 0,
	    //MT6620 use External IRQ, wifi uses high speed. here wifi manage his own suspend and resume, does not support hot plug
	    .flags          = MSDC_EXT_SDIO_IRQ | MSDC_HIGHSPEED,//MSDC_SYS_SUSPEND | MSDC_WP_PIN_EN | MSDC_CD_PIN_EN | MSDC_REMOVABLE,
	    .request_sdio_eirq = mtk_wcn_cmb_sdio_request_eirq,
	    .enable_sdio_eirq  = mtk_wcn_cmb_sdio_enable_eirq,
	    .disable_sdio_eirq = mtk_wcn_cmb_sdio_disable_eirq,
	    .register_pm       = mtk_wcn_cmb_sdio_register_pm,
	};
    #else
struct msdc_hw msdc1_hw = {
    .clk_src        = 1,
    .cmd_edge       = MSDC_SMPL_FALLING,
    .data_edge      = MSDC_SMPL_FALLING,
    .clk_drv        = 0,
    .cmd_drv        = 0,
    .dat_drv        = 0,
    .data_pins      = 4,
    .data_offset    = 0,
    .flags          = MSDC_SYS_SUSPEND | MSDC_WP_PIN_EN | MSDC_HIGHSPEED|MSDC_SPE,
};
    #endif
#endif
#if defined(CFG_DEV_MSDC2)
    #if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT) && (CONFIG_MTK_WCN_CMB_SDIO_SLOT == 2)
    /* MSDC2 settings for MT66xx combo connectivity chip */
	struct msdc_hw msdc2_hw = {
	    .clk_src        = 1,
	    .cmd_edge       = MSDC_SMPL_FALLING,
	    .data_edge      = MSDC_SMPL_FALLING,
	    .clk_drv        = 4,
	    .cmd_drv        = 4,
	    .dat_drv        = 4,
	    .data_pins      = 4,
	    .data_offset    = 0,
	    //MT6620 use External IRQ, wifi uses high speed. here wifi manage his own suspend and resume, does not support hot plug
	    .flags          = MSDC_EXT_SDIO_IRQ | MSDC_HIGHSPEED,//MSDC_SYS_SUSPEND | MSDC_WP_PIN_EN | MSDC_CD_PIN_EN | MSDC_REMOVABLE,
	    .request_sdio_eirq = mtk_wcn_cmb_sdio_request_eirq,
	    .enable_sdio_eirq  = mtk_wcn_cmb_sdio_enable_eirq,
	    .disable_sdio_eirq = mtk_wcn_cmb_sdio_disable_eirq,
	    .register_pm       = mtk_wcn_cmb_sdio_register_pm,
	};
    #else
	struct msdc_hw msdc2_hw = {
	    .clk_src        = 1,
	    .cmd_edge       = MSDC_SMPL_FALLING,
	    .data_edge      = MSDC_SMPL_FALLING,
	    .clk_drv        = 0,
	    .cmd_drv        = 0,
	    .dat_drv        = 0,
	    .data_pins      = 4,
	    .data_offset    = 0,
	    .flags          = MSDC_SYS_SUSPEND | MSDC_WP_PIN_EN,
	};
	#endif
#endif

#if defined(CFG_DEV_MSDC3)
	#if defined(CONFIG_MTK_WCN_CMB_SDIO_SLOT) && (CONFIG_MTK_WCN_CMB_SDIO_SLOT == 3)
    /* MSDC3 settings for MT66xx combo connectivity chip */
	struct msdc_hw msdc3_hw = {
	    .clk_src        = 1,
	    .cmd_edge       = MSDC_SMPL_FALLING,
	    .data_edge      = MSDC_SMPL_FALLING,
	    .clk_drv        = 4,
	    .cmd_drv        = 4,
	    .dat_drv        = 4,
	    .data_pins      = 4,
	    .data_offset    = 0,
#ifdef RDA_WLAN_SUPPORT
    	.flags          = MSDC_EXT_SDIO_IRQ | MSDC_HIGHSPEED,
#else  
	    .flags          = MSDC_EXT_SDIO_IRQ | MSDC_HIGHSPEED,//| MSDC_HIGHSPEED
#endif
	    .request_sdio_eirq = mtk_wcn_cmb_sdio_request_eirq,
	    .enable_sdio_eirq  = mtk_wcn_cmb_sdio_enable_eirq,
	    .disable_sdio_eirq = mtk_wcn_cmb_sdio_disable_eirq,
	    .register_pm       = mtk_wcn_cmb_sdio_register_pm,
	};
    #else
    struct msdc_hw msdc3_hw = {
	    .clk_src        = 1,
	    .cmd_edge       = MSDC_SMPL_FALLING,
	    .data_edge      = MSDC_SMPL_FALLING,
	    .clk_drv        = 0,
	    .cmd_drv        = 0,
	    .dat_drv        = 0,
	    .data_pins      = 4,
	    .data_offset    = 0,
	    .flags          = MSDC_SYS_SUSPEND | MSDC_WP_PIN_EN | MSDC_HIGHSPEED,
	};
	#endif
#endif

/* MT6577 NAND Driver */
#if defined(CONFIG_MTK_MTD_NAND)
struct mt6577_nand_host_hw mt6577_nand_hw = {
    .nfi_bus_width          = 8,
	.nfi_access_timing		= NFI_DEFAULT_ACCESS_TIMING,
	.nfi_cs_num				= NFI_CS_NUM,
	.nand_sec_size			= 512,
	.nand_sec_shift			= 9,
	.nand_ecc_size			= 2048,
	.nand_ecc_bytes			= 32,
	.nand_ecc_mode			= NAND_ECC_HW,
};
#endif
