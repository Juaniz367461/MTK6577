#ifndef __BT_NET_DEV_H__
#define __BT_NET_DEV_H__


#include <linux/types.h>
#include <linux/compiler.h>

#define BTNIOCNEWUNIT_IP	_IOWR('B', 1, int)	/* create new ip protocol btn unit */
#define BTNIOCNEWUNIT_ETH	_IOWR('B', 2, int)	/* create new ethernet btn unit */


#endif
