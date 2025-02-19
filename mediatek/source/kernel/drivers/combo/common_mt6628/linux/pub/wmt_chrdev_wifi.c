#include <linux/init.h>
#include <linux/module.h>
#include <linux/types.h>
#include <linux/kernel.h>
#include <linux/fs.h>
#include <linux/cdev.h>
#include <linux/sched.h>
#include <asm/current.h>
#include <asm/uaccess.h>
#include <linux/fcntl.h>
#include <linux/poll.h>
#include <linux/time.h>
#include <linux/delay.h>
#include "wmt_exp.h"
#include "stp_exp.h"

MODULE_LICENSE("Dual BSD/GPL");

#define WIFI_DRIVER_NAME "mtk_wmt_WIFI_chrdev"
#define WIFI_DEV_MAJOR 194 // never used number

#define PFX                         "[MTK-WIFI] "
#define WIFI_LOG_DBG                  3
#define WIFI_LOG_INFO                 2
#define WIFI_LOG_WARN                 1
#define WIFI_LOG_ERR                  0


unsigned int gDbgLevel = WIFI_LOG_INFO;

#define WIFI_DBG_FUNC(fmt, arg...)    if(gDbgLevel >= WIFI_LOG_DBG){ printk(PFX "%s: "  fmt, __FUNCTION__ ,##arg);}
#define WIFI_INFO_FUNC(fmt, arg...)   if(gDbgLevel >= WIFI_LOG_INFO){ printk(PFX "%s: "  fmt, __FUNCTION__ ,##arg);}
#define WIFI_WARN_FUNC(fmt, arg...)   if(gDbgLevel >= WIFI_LOG_WARN){ printk(PFX "%s: "  fmt, __FUNCTION__ ,##arg);}
#define WIFI_ERR_FUNC(fmt, arg...)    if(gDbgLevel >= WIFI_LOG_ERR){ printk(PFX "%s: "   fmt, __FUNCTION__ ,##arg);}
#define WIFI_TRC_FUNC(f)              if(gDbgLevel >= WIFI_LOG_DBG){printk(PFX "<%s> <%d>\n", __FUNCTION__, __LINE__);}

#define VERSION "1.0"

static int WIFI_devs = 1;        /* device count */
static int WIFI_major = WIFI_DEV_MAJOR;       /* dynamic allocation */
module_param(WIFI_major, uint, 0);
static struct cdev WIFI_cdev;
volatile int retflag = 0;

static int WIFI_open(struct inode *inode, struct file *file)
{
    WIFI_INFO_FUNC("%s: major %d minor %d (pid %d)\n", __func__,
        imajor(inode),
        iminor(inode),
        current->pid
        );

    //TODO
    //Disable EINT(external interrupt), and set the GPIO to EINT mode.        
    

#if 1 /* turn on WIFI */

    if (MTK_WCN_BOOL_FALSE == mtk_wcn_wmt_func_on(WMTDRV_TYPE_WIFI)) {
        WIFI_WARN_FUNC("WMT turn on WIFI fail!\n");
        return -ENODEV;
    }else{
    retflag = 0;
        WIFI_INFO_FUNC("WMT register WIFI rst cb!\n");
    }
#endif  

    return 0;
}

static int WIFI_close(struct inode *inode, struct file *file)
{
    WIFI_INFO_FUNC("%s: major %d minor %d (pid %d)\n", __func__,
        imajor(inode),
        iminor(inode),
        current->pid
        );
    retflag = 0; 
    
    //TODO
    //Configure the EINT pin to GPIO mode.    

    if (MTK_WCN_BOOL_FALSE == mtk_wcn_wmt_func_off(WMTDRV_TYPE_WIFI)) {
        WIFI_INFO_FUNC("WMT turn off WIFI fail!\n");
        return -EIO;    //mostly, native programmer will not check this return value.
    }
    else {
        WIFI_INFO_FUNC("WMT turn off WIFI OK!\n");
    }

    return 0;
}

struct file_operations WIFI_fops = {
    .open = WIFI_open,
    .release = WIFI_close,
};

static int WIFI_init(void)
{
    dev_t dev = MKDEV(WIFI_major, 0);
    int alloc_ret = 0;
    int cdev_err = 0;

    /*static allocate chrdev*/
    alloc_ret = register_chrdev_region(dev, 1, WIFI_DRIVER_NAME);
        if (alloc_ret) {
            WIFI_ERR_FUNC("fail to register chrdev\n");
            return alloc_ret;
        }

    cdev_init(&WIFI_cdev, &WIFI_fops);
    WIFI_cdev.owner = THIS_MODULE;

        cdev_err = cdev_add(&WIFI_cdev, dev, WIFI_devs);
        if (cdev_err)
            goto error;

        WIFI_INFO_FUNC("%s driver(major %d) installed.\n", WIFI_DRIVER_NAME, WIFI_major);
        retflag = 0;

    return 0;

error:
    if (cdev_err == 0)
        cdev_del(&WIFI_cdev);

    if (alloc_ret == 0)
        unregister_chrdev_region(dev, WIFI_devs);

    return -1;
}

static void WIFI_exit(void)
{
    dev_t dev = MKDEV(WIFI_major, 0);
        retflag = 0;

    cdev_del(&WIFI_cdev);
    unregister_chrdev_region(dev, WIFI_devs);

    WIFI_INFO_FUNC("%s driver removed.\n", WIFI_DRIVER_NAME);
}

module_init(WIFI_init);
module_exit(WIFI_exit);

