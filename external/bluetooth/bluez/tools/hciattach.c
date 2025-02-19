/*
 *
 *  BlueZ - Bluetooth protocol stack for Linux
 *
 *  Copyright (C) 2000-2001  Qualcomm Incorporated
 *  Copyright (C) 2002-2003  Maxim Krasnyansky <maxk@qualcomm.com>
 *  Copyright (C) 2002-2010  Marcel Holtmann <marcel@holtmann.org>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#define _GNU_SOURCE
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <syslog.h>
#include <termios.h>
#include <time.h>
#include <sys/time.h>
#include <sys/poll.h>
#include <sys/param.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/uio.h>
#ifdef RDA_BT_SUPPORT
#include <linux/serial.h>
#endif
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

#include "hciattach.h"

#include "ppoll.h"
#ifdef RDA_BT_SUPPORT
#include <cutils/log.h>

#define TAG "[GPIOTST] "
#define LDO_POWER_PATH "/sys/mtuart/power"
#define NULL_FT {0,0}
#define GIO_RETERR(res, fmt, args...)                                       \
    do {                                                                    \
        printf(TAG "%s:%04d: " fmt"\n", __FUNCTION__, __LINE__, ##args);    \
        return res;                                                         \
    } while(0)

/* GPIO MODE CONTROL VALUE*/
typedef enum {
    GPIO_MODE_GPIO  = 0,
    GPIO_MODE_00    = 0,
    GPIO_MODE_01    = 1,
    GPIO_MODE_02    = 2,
    GPIO_MODE_03    = 3,

    GPIO_MODE_MAX,
    GPIO_MODE_DEFAULT = GPIO_MODE_01,
} GPIO_MODE;

/* GPIO PULL ENABLE*/
typedef enum {
    GPIO_PULL_DISABLE = 0,
    GPIO_PULL_ENABLE  = 1,

    GPIO_PULL_EN_MAX,
    GPIO_PULL_EN_DEFAULT = GPIO_PULL_ENABLE,
} GPIO_PULL_EN;

/* GPIO PULL-UP/PULL-DOWN*/
typedef enum {
    GPIO_PULL_DOWN  = 0,
    GPIO_PULL_UP    = 1,

    GPIO_PULL_MAX,
    GPIO_PULL_DEFAULT = GPIO_PULL_DOWN
} GPIO_PULL;

static int set_rda_power(int on) {
    int sz;
    int fd = -1;
    int ret = -1;
    const char buffer = (on ? '1' : '0');

    fd = open(LDO_POWER_PATH, O_WRONLY);
    if (fd < 0) {
        printf("open(%s) for write failed: %s (%d)", LDO_POWER_PATH,
             strerror(errno), errno);
        goto out;
    }
    sz = write(fd, &buffer, 1);
    if (sz < 0) {
        printf("write(%s) failed: %s (%d)", LDO_POWER_PATH, strerror(errno),
             errno);
        goto out;
    }
    ret = 0;

out:
    if (fd >= 0) close(fd);
    return ret;
}
#endif
struct uart_t {
	char *type;
	int  m_id;
	int  p_id;
	int  proto;
	int  init_speed;
	int  speed;
	int  flags;
	int  pm;
	char *bdaddr;
	int  (*init) (int fd, struct uart_t *u, struct termios *ti);
	int  (*post) (int fd, struct uart_t *u, struct termios *ti);
};

#define FLOW_CTL	0x0001
#define ENABLE_PM	1
#define DISABLE_PM	0

static volatile sig_atomic_t __io_canceled = 0;

static void sig_hup(int sig)
{
}

static void sig_term(int sig)
{
	__io_canceled = 1;
}

static void sig_alarm(int sig)
{
	fprintf(stderr, "Initialization timed out.\n");
	exit(1);
}

static int uart_speed(int s)
{
	switch (s) {
	case 9600:
		return B9600;
	case 19200:
		return B19200;
	case 38400:
		return B38400;
	case 57600:
		return B57600;
	case 115200:
		return B115200;
	case 230400:
		return B230400;
	case 460800:
		return B460800;
	case 500000:
		return B500000;
	case 576000:
		return B576000;
	case 921600:
		return B921600;
	case 1000000:
		return B1000000;
	case 1152000:
		return B1152000;
	case 1500000:
		return B1500000;
	case 2000000:
		return B2000000;
#ifdef B2500000
	case 2500000:
		return B2500000;
#endif
#ifdef B3000000
	case 3000000:
		return B3000000;
#endif
#ifdef B3500000
	case 3500000:
		return B3500000;
#endif
#ifdef B4000000
	case 4000000:
		return B4000000;
#endif
	default:
		return B57600;
	}
}

int set_speed(int fd, struct termios *ti, int speed)
{
	if (cfsetospeed(ti, uart_speed(speed)) < 0)
		return -errno;

	if (cfsetispeed(ti, uart_speed(speed)) < 0)
		return -errno;

	if (tcsetattr(fd, TCSANOW, ti) < 0)
		return -errno;

	return 0;
}

/*
 * Read an HCI event from the given file descriptor.
 */
int read_hci_event(int fd, unsigned char* buf, int size)
{
	int remain, r;
	int count = 0;

	if (size <= 0)
		return -1;

	/* The first byte identifies the packet type. For HCI event packets, it
	 * should be 0x04, so we read until we get to the 0x04. */
	while (1) {
		r = read(fd, buf, 1);
		if (r <= 0)
			return -1;
		if (buf[0] == 0x04)
			break;
	}
	count++;

	/* The next two bytes are the event code and parameter total length. */
	while (count < 3) {
		r = read(fd, buf + count, 3 - count);
		if (r <= 0)
			return -1;
		count += r;
	}

	/* Now we read the parameters. */
	if (buf[2] < (size - 3))
		remain = buf[2];
	else
		remain = size - 3;

	while ((count - 3) < remain) {
		r = read(fd, buf + count, remain - (count - 3));
		if (r <= 0)
			return -1;
		count += r;
	}

	return count;
}

/*
 * Ericsson specific initialization
 */
static int ericsson(int fd, struct uart_t *u, struct termios *ti)
{
	struct timespec tm = {0, 50000};
	char cmd[5];

	cmd[0] = HCI_COMMAND_PKT;
	cmd[1] = 0x09;
	cmd[2] = 0xfc;
	cmd[3] = 0x01;

	switch (u->speed) {
	case 57600:
		cmd[4] = 0x03;
		break;
	case 115200:
		cmd[4] = 0x02;
		break;
	case 230400:
		cmd[4] = 0x01;
		break;
	case 460800:
		cmd[4] = 0x00;
		break;
	case 921600:
		cmd[4] = 0x20;
		break;
	case 2000000:
		cmd[4] = 0x25;
		break;
	case 3000000:
		cmd[4] = 0x27;
		break;
	case 4000000:
		cmd[4] = 0x2B;
		break;
	default:
		cmd[4] = 0x03;
		u->speed = 57600;
		fprintf(stderr, "Invalid speed requested, using %d bps instead\n", u->speed);
		break;
	}

	/* Send initialization command */
	if (write(fd, cmd, 5) != 5) {
		perror("Failed to write init command");
		return -1;
	}

	nanosleep(&tm, NULL);
	return 0;
}

/*
 * Digianswer specific initialization
 */
static int digi(int fd, struct uart_t *u, struct termios *ti)
{
	struct timespec tm = {0, 50000};
	char cmd[5];

	/* DigiAnswer set baud rate command */
	cmd[0] = HCI_COMMAND_PKT;
	cmd[1] = 0x07;
	cmd[2] = 0xfc;
	cmd[3] = 0x01;

	switch (u->speed) {
	case 57600:
		cmd[4] = 0x08;
		break;
	case 115200:
		cmd[4] = 0x09;
		break;
	default:
		cmd[4] = 0x09;
		u->speed = 115200;
		break;
	}

	/* Send initialization command */
	if (write(fd, cmd, 5) != 5) {
		perror("Failed to write init command");
		return -1;
	}

	nanosleep(&tm, NULL);
	return 0;
}

static int texas(int fd, struct uart_t *u, struct termios *ti)
{
	return texas_init(fd, ti);
}

static int texas2(int fd, struct uart_t *u, struct termios *ti)
{
	return texas_post(fd, ti);
}

static int texasalt(int fd, struct uart_t *u, struct termios *ti)
{
	return texasalt_init(fd, u->speed, ti);
}

static int ath3k_ps(int fd, struct uart_t *u, struct termios *ti)
{
	return ath3k_init(fd, u->speed, u->init_speed, u->bdaddr, ti);
}

static int ath3k_pm(int fd, struct uart_t *u, struct termios *ti)
{
	return ath3k_post(fd, u->pm);
}

static int qualcomm(int fd, struct uart_t *u, struct termios *ti)
{
	return qualcomm_init(fd, u->speed, ti, u->bdaddr);
}

static int read_check(int fd, void *buf, int count)
{
	int res;

	do {
		res = read(fd, buf, count);
		if (res != -1) {
			buf += res;
			count -= res;
		}
	} while (count && (errno == 0 || errno == EINTR));

	if (count)
		return -1;

	return 0;
}

/*
 * BCSP specific initialization
 */
static int serial_fd;
static int bcsp_max_retries = 10;

static void bcsp_tshy_sig_alarm(int sig)
{
	unsigned char bcsp_sync_pkt[10] = {0xc0,0x00,0x41,0x00,0xbe,0xda,0xdc,0xed,0xed,0xc0};
	static int retries = 0;

	if (retries < bcsp_max_retries) {
		retries++;
		if (write(serial_fd, &bcsp_sync_pkt, 10) < 0)
			return;
		alarm(1);
		return;
	}

	tcflush(serial_fd, TCIOFLUSH);
	fprintf(stderr, "BCSP initialization timed out\n");
	exit(1);
}

static void bcsp_tconf_sig_alarm(int sig)
{
	unsigned char bcsp_conf_pkt[10] = {0xc0,0x00,0x41,0x00,0xbe,0xad,0xef,0xac,0xed,0xc0};
	static int retries = 0;

	if (retries < bcsp_max_retries){
		retries++;
		if (write(serial_fd, &bcsp_conf_pkt, 10) < 0)
			return;
		alarm(1);
		return;
	}

	tcflush(serial_fd, TCIOFLUSH);
	fprintf(stderr, "BCSP initialization timed out\n");
	exit(1);
}

static int bcsp(int fd, struct uart_t *u, struct termios *ti)
{
	unsigned char byte, bcsph[4], bcspp[4],
		bcsp_sync_resp_pkt[10] = {0xc0,0x00,0x41,0x00,0xbe,0xac,0xaf,0xef,0xee,0xc0},
		bcsp_conf_resp_pkt[10] = {0xc0,0x00,0x41,0x00,0xbe,0xde,0xad,0xd0,0xd0,0xc0},
		bcspsync[4]     = {0xda, 0xdc, 0xed, 0xed},
		bcspsyncresp[4] = {0xac,0xaf,0xef,0xee},
		bcspconf[4]     = {0xad,0xef,0xac,0xed},
		bcspconfresp[4] = {0xde,0xad,0xd0,0xd0};
	struct sigaction sa;
	int len;

	if (set_speed(fd, ti, u->speed) < 0) {
		perror("Can't set default baud rate");
		return -1;
	}

	ti->c_cflag |= PARENB;
	ti->c_cflag &= ~(PARODD);

	if (tcsetattr(fd, TCSANOW, ti) < 0) {
		perror("Can't set port settings");
		return -1;
	}

	alarm(0);

	serial_fd = fd;
	memset(&sa, 0, sizeof(sa));
	sa.sa_flags = SA_NOCLDSTOP;
	sa.sa_handler = bcsp_tshy_sig_alarm;
	sigaction(SIGALRM, &sa, NULL);

	/* State = shy */

	bcsp_tshy_sig_alarm(0);
	while (1) {
		do {
			if (read_check(fd, &byte, 1) == -1){
				perror("Failed to read");
				return -1;
			}
		} while (byte != 0xC0);

		do {
			if ( read_check(fd, &bcsph[0], 1) == -1){
				perror("Failed to read");
				return -1;
			}
		} while (bcsph[0] == 0xC0);

		if ( read_check(fd, &bcsph[1], 3) == -1){
			perror("Failed to read");
			return -1;
		}

		if (((bcsph[0] + bcsph[1] + bcsph[2]) & 0xFF) != (unsigned char)~bcsph[3])
			continue;
		if (bcsph[1] != 0x41 || bcsph[2] != 0x00)
			continue;

		if (read_check(fd, &bcspp, 4) == -1){
			perror("Failed to read");
			return -1;
		}

		if (!memcmp(bcspp, bcspsync, 4)) {
			if (write(fd, &bcsp_sync_resp_pkt,10) < 0)
				return -1;
		} else if (!memcmp(bcspp, bcspsyncresp, 4))
			break;
	}

	/* State = curious */

	alarm(0);
	sa.sa_handler = bcsp_tconf_sig_alarm;
	sigaction(SIGALRM, &sa, NULL);
	alarm(1);

	while (1) {
		do {
			if (read_check(fd, &byte, 1) == -1){
				perror("Failed to read");
				return -1;
			}
		} while (byte != 0xC0);

		do {
			if (read_check(fd, &bcsph[0], 1) == -1){
				perror("Failed to read");
				return -1;
			}
		} while (bcsph[0] == 0xC0);

		if (read_check(fd, &bcsph[1], 3) == -1){
			perror("Failed to read");
			return -1;
		}

		if (((bcsph[0] + bcsph[1] + bcsph[2]) & 0xFF) != (unsigned char)~bcsph[3])
			continue;

		if (bcsph[1] != 0x41 || bcsph[2] != 0x00)
			continue;

		if (read_check(fd, &bcspp, 4) == -1){
			perror("Failed to read");
			return -1;
		}

		if (!memcmp(bcspp, bcspsync, 4))
			len = write(fd, &bcsp_sync_resp_pkt, 10);
		else if (!memcmp(bcspp, bcspconf, 4))
			len = write(fd, &bcsp_conf_resp_pkt, 10);
		else if (!memcmp(bcspp, bcspconfresp,  4))
			break;
		else
			continue;

		if (len < 0)
			return -errno;
	}

	/* State = garrulous */

	return 0;
}

/*
 * CSR specific initialization
 * Inspired strongly by code in OpenBT and experimentations with Brainboxes
 * Pcmcia card.
 * Jean Tourrilhes <jt@hpl.hp.com> - 14.11.01
 */
static int csr(int fd, struct uart_t *u, struct termios *ti)
{
	struct timespec tm = {0, 10000000};	/* 10ms - be generous */
	unsigned char cmd[30];		/* Command */
	unsigned char resp[30];		/* Response */
	int  clen = 0;		/* Command len */
	static int csr_seq = 0;	/* Sequence number of command */
	int  divisor;

	/* It seems that if we set the CSR UART speed straight away, it
	 * won't work, the CSR UART gets into a state where we can't talk
	 * to it anymore.
	 * On the other hand, doing a read before setting the CSR speed
	 * seems to be ok.
	 * Therefore, the strategy is to read the build ID (useful for
	 * debugging) and only then set the CSR UART speed. Doing like
	 * this is more complex but at least it works ;-)
	 * The CSR UART control may be slow to wake up or something because
	 * every time I read its speed, its bogus...
	 * Jean II */

	/* Try to read the build ID of the CSR chip */
	clen = 5 + (5 + 6) * 2;
	/* HCI header */
	cmd[0] = HCI_COMMAND_PKT;
	cmd[1] = 0x00;		/* CSR command */
	cmd[2] = 0xfc;		/* MANUFACTURER_SPEC */
	cmd[3] = 1 + (5 + 6) * 2;	/* len */
	/* CSR MSG header */
	cmd[4] = 0xC2;		/* first+last+channel=BCC */
	/* CSR BCC header */
	cmd[5] = 0x00;		/* type = GET-REQ */
	cmd[6] = 0x00;		/* - msB */
	cmd[7] = 5 + 4;		/* len */
	cmd[8] = 0x00;		/* - msB */
	cmd[9] = csr_seq & 0xFF;/* seq num */
	cmd[10] = (csr_seq >> 8) & 0xFF;	/* - msB */
	csr_seq++;
	cmd[11] = 0x19;		/* var_id = CSR_CMD_BUILD_ID */
	cmd[12] = 0x28;		/* - msB */
	cmd[13] = 0x00;		/* status = STATUS_OK */
	cmd[14] = 0x00;		/* - msB */
	/* CSR BCC payload */
	memset(cmd + 15, 0, 6 * 2);

	/* Send command */
	do {
		if (write(fd, cmd, clen) != clen) {
			perror("Failed to write init command (GET_BUILD_ID)");
			return -1;
		}

		/* Read reply. */
		if (read_hci_event(fd, resp, 100) < 0) {
			perror("Failed to read init response (GET_BUILD_ID)");
			return -1;
		}

	/* Event code 0xFF is for vendor-specific events, which is
	 * what we're looking for. */
	} while (resp[1] != 0xFF);

#ifdef CSR_DEBUG
	{
	char temp[512];
	int i;
	for (i=0; i < rlen; i++)
		sprintf(temp + (i*3), "-%02X", resp[i]);
	fprintf(stderr, "Reading CSR build ID %d [%s]\n", rlen, temp + 1);
	// In theory, it should look like :
	// 04-FF-13-FF-01-00-09-00-00-00-19-28-00-00-73-00-00-00-00-00-00-00
	}
#endif
	/* Display that to user */
	fprintf(stderr, "CSR build ID 0x%02X-0x%02X\n",
		resp[15] & 0xFF, resp[14] & 0xFF);

	/* Try to read the current speed of the CSR chip */
	clen = 5 + (5 + 4)*2;
	/* -- HCI header */
	cmd[3] = 1 + (5 + 4)*2;	/* len */
	/* -- CSR BCC header -- */
	cmd[9] = csr_seq & 0xFF;	/* seq num */
	cmd[10] = (csr_seq >> 8) & 0xFF;	/* - msB */
	csr_seq++;
	cmd[11] = 0x02;		/* var_id = CONFIG_UART */
	cmd[12] = 0x68;		/* - msB */

#ifdef CSR_DEBUG
	/* Send command */
	do {
		if (write(fd, cmd, clen) != clen) {
			perror("Failed to write init command (GET_BUILD_ID)");
			return -1;
		}

		/* Read reply. */
		if (read_hci_event(fd, resp, 100) < 0) {
			perror("Failed to read init response (GET_BUILD_ID)");
			return -1;
		}

	/* Event code 0xFF is for vendor-specific events, which is
	 * what we're looking for. */
	} while (resp[1] != 0xFF);

	{
	char temp[512];
	int i;
	for (i=0; i < rlen; i++)
		sprintf(temp + (i*3), "-%02X", resp[i]);
	fprintf(stderr, "Reading CSR UART speed %d [%s]\n", rlen, temp+1);
	}
#endif

	if (u->speed > 1500000) {
		fprintf(stderr, "Speed %d too high. Remaining at %d baud\n",
			u->speed, u->init_speed);
		u->speed = u->init_speed;
	} else if (u->speed != 57600 && uart_speed(u->speed) == B57600) {
		/* Unknown speed. Why oh why can't we just pass an int to the kernel? */
		fprintf(stderr, "Speed %d unrecognised. Remaining at %d baud\n",
			u->speed, u->init_speed);
		u->speed = u->init_speed;
	}
	if (u->speed == u->init_speed)
		return 0;

	/* Now, create the command that will set the UART speed */
	/* CSR BCC header */
	cmd[5] = 0x02;			/* type = SET-REQ */
	cmd[6] = 0x00;			/* - msB */
	cmd[9] = csr_seq & 0xFF;	/* seq num */
	cmd[10] = (csr_seq >> 8) & 0xFF;/* - msB */
	csr_seq++;

	divisor = (u->speed*64+7812)/15625;

	/* No parity, one stop bit -> divisor |= 0x0000; */
	cmd[15] = (divisor) & 0xFF;		/* divider */
	cmd[16] = (divisor >> 8) & 0xFF;	/* - msB */
	/* The rest of the payload will be 0x00 */

#ifdef CSR_DEBUG
	{
	char temp[512];
	int i;
	for(i = 0; i < clen; i++)
		sprintf(temp + (i*3), "-%02X", cmd[i]);
	fprintf(stderr, "Writing CSR UART speed %d [%s]\n", clen, temp + 1);
	// In theory, it should look like :
	// 01-00-FC-13-C2-02-00-09-00-03-00-02-68-00-00-BF-0E-00-00-00-00-00-00
	// 01-00-FC-13-C2-02-00-09-00-01-00-02-68-00-00-D8-01-00-00-00-00-00-00
	}
#endif

	/* Send the command to set the CSR UART speed */
	if (write(fd, cmd, clen) != clen) {
		perror("Failed to write init command (SET_UART_SPEED)");
		return -1;
	}

	nanosleep(&tm, NULL);
	return 0;
}

/*
 * Silicon Wave specific initialization
 * Thomas Moser <thomas.moser@tmoser.ch>
 */
static int swave(int fd, struct uart_t *u, struct termios *ti)
{
	struct timespec tm = { 0, 500000 };
	char cmd[10], rsp[100];
	int r;

	// Silicon Wave set baud rate command
	// see HCI Vendor Specific Interface from Silicon Wave
	// first send a "param access set" command to set the
	// appropriate data fields in RAM. Then send a "HCI Reset
	// Subcommand", e.g. "soft reset" to make the changes effective.

	cmd[0] = HCI_COMMAND_PKT;	// it's a command packet
	cmd[1] = 0x0B;			// OCF 0x0B	= param access set
	cmd[2] = 0xfc;			// OGF bx111111 = vendor specific
	cmd[3] = 0x06;			// 6 bytes of data following
	cmd[4] = 0x01;			// param sub command
	cmd[5] = 0x11;			// tag 17 = 0x11 = HCI Transport Params
	cmd[6] = 0x03;			// length of the parameter following
	cmd[7] = 0x01;			// HCI Transport flow control enable
	cmd[8] = 0x01;			// HCI Transport Type = UART

	switch (u->speed) {
	case 19200:
		cmd[9] = 0x03;
		break;
	case 38400:
		cmd[9] = 0x02;
		break;
	case 57600:
		cmd[9] = 0x01;
		break;
	case 115200:
		cmd[9] = 0x00;
		break;
	default:
		u->speed = 115200;
		cmd[9] = 0x00;
		break;
	}

	/* Send initialization command */
	if (write(fd, cmd, 10) != 10) {
		perror("Failed to write init command");
		return -1;
	}

	// We should wait for a "GET Event" to confirm the success of
	// the baud rate setting. Wait some time before reading. Better:
	// read with timeout, parse data
	// until correct answer, else error handling ... todo ...

	nanosleep(&tm, NULL);

	r = read(fd, rsp, sizeof(rsp));
	if (r > 0) {
		// guess it's okay, but we should parse the reply. But since
		// I don't react on an error anyway ... todo
		// Response packet format:
		//  04	Event
		//  FF	Vendor specific
		//  07	Parameter length
		//  0B	Subcommand
		//  01	Setevent
		//  11	Tag specifying HCI Transport Layer Parameter
		//  03	length
		//  01	flow on
		//  01 	Hci Transport type = Uart
		//  xx	Baud rate set (see above)
	} else {
		// ups, got error.
		return -1;
	}

	// we probably got the reply. Now we must send the "soft reset"
	// which is standard HCI RESET.

	cmd[0] = HCI_COMMAND_PKT;	// it's a command packet
	cmd[1] = 0x03;
	cmd[2] = 0x0c;
	cmd[3] = 0x00;

	/* Send reset command */
	if (write(fd, cmd, 4) != 4) {
		perror("Can't write Silicon Wave reset cmd.");
		return -1;
	}

	nanosleep(&tm, NULL);

	// now the uart baud rate on the silicon wave module is set and effective.
	// change our own baud rate as well. Then there is a reset event comming in
 	// on the *new* baud rate. This is *undocumented*! The packet looks like this:
	// 04 FF 01 0B (which would make that a confirmation of 0x0B = "Param
	// subcommand class". So: change to new baud rate, read with timeout, parse
	// data, error handling. BTW: all param access in Silicon Wave is done this way.
	// Maybe this code would belong in a seperate file, or at least code reuse...

	return 0;
}

/*
 * ST Microelectronics specific initialization
 * Marcel Holtmann <marcel@holtmann.org>
 */
static int st(int fd, struct uart_t *u, struct termios *ti)
{
	struct timespec tm = {0, 50000};
	char cmd[5];

	/* ST Microelectronics set baud rate command */
	cmd[0] = HCI_COMMAND_PKT;
	cmd[1] = 0x46;			// OCF = Hci_Cmd_ST_Set_Uart_Baud_Rate
	cmd[2] = 0xfc;			// OGF = Vendor specific
	cmd[3] = 0x01;

	switch (u->speed) {
	case 9600:
		cmd[4] = 0x09;
		break;
	case 19200:
		cmd[4] = 0x0b;
		break;
	case 38400:
		cmd[4] = 0x0d;
		break;
	case 57600:
		cmd[4] = 0x0e;
		break;
	case 115200:
		cmd[4] = 0x10;
		break;
	case 230400:
		cmd[4] = 0x12;
		break;
	case 460800:
		cmd[4] = 0x13;
		break;
	case 921600:
		cmd[4] = 0x14;
		break;
	default:
		cmd[4] = 0x10;
		u->speed = 115200;
		break;
	}

	/* Send initialization command */
	if (write(fd, cmd, 5) != 5) {
		perror("Failed to write init command");
		return -1;
	}

	nanosleep(&tm, NULL);
	return 0;
}

static int stlc2500(int fd, struct uart_t *u, struct termios *ti)
{
	bdaddr_t bdaddr;
	unsigned char resp[10];
	int n;
	int rvalue;

	/* STLC2500 has an ericsson core */
	rvalue = ericsson(fd, u, ti);
	if (rvalue != 0)
		return rvalue;

#ifdef STLC2500_DEBUG
	fprintf(stderr, "Setting speed\n");
#endif
	if (set_speed(fd, ti, u->speed) < 0) {
		perror("Can't set baud rate");
		return -1;
	}

#ifdef STLC2500_DEBUG
	fprintf(stderr, "Speed set...\n");
#endif

	/* Read reply */
	if ((n = read_hci_event(fd, resp, 10)) < 0) {
		fprintf(stderr, "Failed to set baud rate on chip\n");
		return -1;
	}

#ifdef STLC2500_DEBUG
	for (i = 0; i < n; i++) {
		fprintf(stderr, "resp[%d] = %02x\n", i, resp[i]);
	}
#endif

	str2ba(u->bdaddr, &bdaddr);
	return stlc2500_init(fd, &bdaddr);
}

static int bgb2xx(int fd, struct uart_t *u, struct termios *ti)
{
	bdaddr_t bdaddr;

	str2ba(u->bdaddr, &bdaddr);

	return bgb2xx_init(fd, &bdaddr);
}

/*
 * Broadcom specific initialization
 * Extracted from Jungo openrg
 */
static int bcm2035(int fd, struct uart_t *u, struct termios *ti)
{
	int n;
	unsigned char cmd[30], resp[30];

	/* Reset the BT Chip */
	memset(cmd, 0, sizeof(cmd));
	memset(resp, 0, sizeof(resp));
	cmd[0] = HCI_COMMAND_PKT;
	cmd[1] = 0x03;
	cmd[2] = 0x0c;
	cmd[3] = 0x00;

	/* Send command */
	if (write(fd, cmd, 4) != 4) {
		fprintf(stderr, "Failed to write reset command\n");
		return -1;
	}

	/* Read reply */
	if ((n = read_hci_event(fd, resp, 4)) < 0) {
		fprintf(stderr, "Failed to reset chip\n");
		return -1;
	}

	if (u->bdaddr != NULL) {
		/* Set BD_ADDR */
		memset(cmd, 0, sizeof(cmd));
		memset(resp, 0, sizeof(resp));
		cmd[0] = HCI_COMMAND_PKT;
		cmd[1] = 0x01;
		cmd[2] = 0xfc;
		cmd[3] = 0x06;
		str2ba(u->bdaddr, (bdaddr_t *) (cmd + 4));

		/* Send command */
		if (write(fd, cmd, 10) != 10) {
			fprintf(stderr, "Failed to write BD_ADDR command\n");
			return -1;
		}

		/* Read reply */
		if ((n = read_hci_event(fd, resp, 10)) < 0) {
			fprintf(stderr, "Failed to set BD_ADDR\n");
			return -1;
		}
	}

	/* Read the local version info */
	memset(cmd, 0, sizeof(cmd));
	memset(resp, 0, sizeof(resp));
	cmd[0] = HCI_COMMAND_PKT;
	cmd[1] = 0x01;
	cmd[2] = 0x10;
	cmd[3] = 0x00;

	/* Send command */
	if (write(fd, cmd, 4) != 4) {
		fprintf(stderr, "Failed to write \"read local version\" "
			"command\n");
		return -1;
	}

	/* Read reply */
	if ((n = read_hci_event(fd, resp, 4)) < 0) {
		fprintf(stderr, "Failed to read local version\n");
		return -1;
	}

	/* Read the local supported commands info */
	memset(cmd, 0, sizeof(cmd));
	memset(resp, 0, sizeof(resp));
	cmd[0] = HCI_COMMAND_PKT;
	cmd[1] = 0x02;
	cmd[2] = 0x10;
	cmd[3] = 0x00;

	/* Send command */
	if (write(fd, cmd, 4) != 4) {
		fprintf(stderr, "Failed to write \"read local supported "
						"commands\" command\n");
		return -1;
	}

	/* Read reply */
	if ((n = read_hci_event(fd, resp, 4)) < 0) {
		fprintf(stderr, "Failed to read local supported commands\n");
		return -1;
	}

	/* Set the baud rate */
	memset(cmd, 0, sizeof(cmd));
	memset(resp, 0, sizeof(resp));
	cmd[0] = HCI_COMMAND_PKT;
	cmd[1] = 0x18;
	cmd[2] = 0xfc;
	cmd[3] = 0x02;
	switch (u->speed) {
	case 57600:
		cmd[4] = 0x00;
		cmd[5] = 0xe6;
		break;
	case 230400:
		cmd[4] = 0x22;
		cmd[5] = 0xfa;
		break;
	case 460800:
		cmd[4] = 0x22;
		cmd[5] = 0xfd;
		break;
	case 921600:
		cmd[4] = 0x55;
		cmd[5] = 0xff;
		break;
	default:
		/* Default is 115200 */
		cmd[4] = 0x00;
		cmd[5] = 0xf3;
		break;
	}
	fprintf(stderr, "Baud rate parameters: DHBR=0x%2x,DLBR=0x%2x\n",
		cmd[4], cmd[5]);

	/* Send command */
	if (write(fd, cmd, 6) != 6) {
		fprintf(stderr, "Failed to write \"set baud rate\" command\n");
		return -1;
	}

	if ((n = read_hci_event(fd, resp, 6)) < 0) {
		fprintf(stderr, "Failed to set baud rate\n");
		return -1;
	}

	return 0;
}
#ifdef RDA_BT_SUPPORT
#if 0
static int rda_setup_flow_ctl(int fd, struct uart_t *u, struct termios *ti)
{
 unsigned int i, num_send;
 
 unsigned char rda_flow_ctl_10[][14] =
 {
  {0x01,0x02,0xfd,0x0a,0x00,0x01,0x10,0x00,0x00,0x50,0x22,0x01,0x00,0x00},// flow control
 };
 
 if (u->flags & FLOW_CTL) {
  /*Setup flow control */
  for (i = 0; i < sizeof(rda_flow_ctl_10)/sizeof(rda_flow_ctl_10[0]); i++) {
   num_send = write(fd, rda_flow_ctl_10[i], sizeof(rda_flow_ctl_10[i]));
   if (num_send != sizeof(rda_flow_ctl_10[i])) {
    perror("");
    printf("num_send = %d (%d)\n", num_send, sizeof(rda_flow_ctl_10[i]));
    return -1;
   }
   usleep(5000);
  }
 }
 
 usleep(50000);
 
 return 0;
}
#endif
 
void rdabt_write_memory(int fd,__u32 addr,__u32 *data,__u8 len,__u8 memory_type)
{
   __u16 num_to_send; 
   __u16 i,j;
   __u8 data_to_send[256]={0};
   __u32 address_convert;
   
   data_to_send[0] = 0x01;
   data_to_send[1] = 0x02;
   data_to_send[2] = 0xfd;
   data_to_send[3] = (__u8)(len*4+6);
   data_to_send[4] = (memory_type+0x00);  // add the event display 0x80 no event callback 0x00
   data_to_send[5] = len;
   if(memory_type == 0x01)
   {
      address_convert = addr*4+0x200;
      data_to_send[6] = (__u8)address_convert;
      data_to_send[7] = (__u8)(address_convert>>8);
      data_to_send[8] = (__u8)(address_convert>>16);
      data_to_send[9] = (__u8)(address_convert>>24);   
   }
   else
   {
      data_to_send[6] = (__u8)addr;
      data_to_send[7] = (__u8)(addr>>8);
      data_to_send[8] = (__u8)(addr>>16);
      data_to_send[9] = (__u8)(addr>>24);
   }
   for(i=0;i<len;i++,data++)
   {
       j=10+i*4;
       data_to_send[j] =  (__u8)(*data);
       data_to_send[j+1] = (__u8)((*data)>>8);
       data_to_send[j+2] = (__u8)((*data)>>16);
       data_to_send[j+3] = (__u8)((*data)>>24);
   }
   num_to_send = 4+data_to_send[3];
 
   write(fd,&(data_to_send[0]),num_to_send);
   
  // for (i =0; i < num_to_send; i++)
  //   { printf ("%02x ", data_to_send[i]);} 
}
 
 
 
void RDA_uart_write_simple(int fd,__u8* buf,__u16 len)
{
    __u16 num_send; 
    write(fd,buf,len);
    usleep(10000);//10ms?
}
 
void RDA_uart_write_array(int fd,__u32 buf[][2],__u16 len,__u8 type)
{
   __u32 i;
   for(i=0;i<len;i++)
   {
      rdabt_write_memory(fd,buf[i][0],&buf[i][1],1,type);
      usleep(12000);//12ms?
   } 
}


__u32 RDA5876_PSKEY_RF[][2] =
{
    //{0x40240000,0x0004f39c}, //; houzhen 2010.02.07 for rda5990 bt
    {0x800000C0,0x00000021}, //; CHIP_PS PSKEY: Total number -----------------
    {0x800000C4,0x003F0000},
    {0x800000C8,0x00414003},
    {0x800000CC,0x004225BD},
    {0x800000D0,0x004908E4},
    {0x800000D4,0x0043B074},
    {0x800000D8,0x0044D01A},
    {0x800000DC,0x004A0800},
    {0x800000E0,0x0054A020},
    {0x800000E4,0x0055A020},
    {0x800000E8,0x0056A542},
    {0x800000EC,0x00574C18},
    {0x800000F0,0x003F0001},
    {0x800000F4,0x00410900},
    {0x800000F8,0x0046033F},
    {0x800000FC,0x004C0000},
    {0x80000100,0x004D0015},
    {0x80000104,0x004E002B},
    {0x80000108,0x004F0042},
    {0x8000010C,0x0050005A},
    {0x80000110,0x00510073},
    {0x80000114,0x0052008D},
    {0x80000118,0x005300A7},
    {0x8000011C,0x005400C4},
    {0x80000120,0x005500E3},
    {0x80000124,0x00560103},
    {0x80000128,0x00570127},
    {0x8000012C,0x0058014E},
    {0x80000130,0x00590178},
    {0x80000134,0x005A01A1},
    {0x80000138,0x005B01CE},
    {0x8000013C,0x005C01FF},
    {0x80000140,0x003F0000},
    {0x80000144,0x00000000}, //;         PSKEY: Page 0
    {0x80000040,0x10000000},
    //{0x40240000,0x0000f29c}, //; SPI2_CLK_EN PCLK_SPI2_EN 

};
 
 
__u32 RDA5876_PSKEY_MISC[][2] =
{
    // open sleep
 //   {0x40240000,0x2000f29c},
    {0x80000070,0x00022000},  //fix esco parameter
    {0x80000074,0xa5025010},
		{0x80000078,0x0f054001}, //sniff interval 2 0
		
		{0x80000040,0x00007000},

    {0x80002bec,0x00010a02},  //sleep enable
	   
    {0x40180004,0x0001a218},
	{0x40180024,0x0001a1e0},


	{0x40180004,0x0001a218},
	{0x40180024,0x0001a1e0},
    {0x40180008,0x0001a234},
    {0x40180028,0x00000014},
    
// {0x40180000,0x00000003}, //add by gongyu
       /*houzhen update  Mar 22 2012 */
    {0x8024002c,0x00d80500},
    {0x800004f4,0x83701898}, ///rda5990 disable 3m esco ev4 ev5  ssp
    //{0x800004f0,0xf88dffff}, ///rda5990 disable edr 
    {0x4020004C,0x1500FFFF},  //acl package as lo pri houzhen update 2012_04_15
    {0x40200050,0x2eb20000},  //rda5990 pta config
    {0x40200054,0xaaaaaaaa},  //rda5990 pta config 0xffffffff > 0xaaaaaaaa
    {0x40240000,0x0000f29c},  //config 32k
    {0x80000000,0xea00003e},//
    {0x80000100,0xe3a00020},//   mov r0,0x10   //  wifi frame=last 8 bits  houzhen Mar 29 2012 0x80
    {0x80000104,0xe5c50020},//   strb r0,[r5,0x20]
    {0x80000108,0xe3a00000},// mov r0  0
    {0x8000010c,0xe3a01a31},//       mov  r1,0x31000
    {0x80000110,0xe281fe29},//add pc,r1,0x290
    {0x4018000c,0x0003128c},//
    {0x4018002c,0x00032cb4},//
    {0x80000120,0x13a0f112},//
    {0x80000004,0xea000046},//
    {0x80000124,0xe5c5000c},// STRB   r0,[r5,#0xc]
    {0x80000128,0xe3a00080},//     mov r0,0x06    //bt frame=last 8 bits     houzhen Mar 29 2012 0x30
    {0x8000012c,0xe5c50020},// miss patch word
    {0x80000130,0xe3a00a31},// mov r0,0x31000
    {0x80000134,0xe280ffad},// add pc r0,0x2b4
    {0x40180010,0x000312b0},//
    {0x40180030,0x80000120},//
    {0x40180014,0x00031234},//
    {0x40180034,0x00008bac},//
 
    {0x40180018,0x0003123c},//
    {0x40180038,0x00031f74},//
     
		{0x80000008,0xea000050},
		{0x80000150,0xe284102c},
		{0x80000154,0xe3a02000},
		{0x80000158,0xe3a0eca0},
		{0x8000015c,0xe28ef0f8},
		{0x4018001c,0x0000a0f4},
		{0x4018003c,0x00032cbc},
		
    {0x40180000,0x0000007f},//

};

__u32 RDA5876_SWITCH_BAUDRATE[][2] =
{
//3200000
 //    {0x80000060,0x0030d400},
//3000000
       {0x80000060,0x002dc6c0},
//1500000
//     {0x80000060,0x0016e360},
//1152000
//     {0x80000060,0x00119400},
//baud rate 921600
//     {0x80000060,0x000e1000},
//     {0x80000064,0x000e1000},
     {0x80000040,0x00000100}
};
 
__u8 RDA_AUTOACCEPT_CONNECT[] = 
{
    0x01,0x05, 0x0c, 0x03, 0x02, 0x00, 0x02
};

 
void RDA5876_Pskey_RfInit(int fd)
{
    RDA_uart_write_array(fd,RDA5876_PSKEY_RF,sizeof(RDA5876_PSKEY_RF)/sizeof(RDA5876_PSKEY_RF[0]),0);
}
 
void RDA5876_Pskey_Misc(int fd)
{
    RDA_uart_write_array(fd,RDA5876_PSKEY_MISC,sizeof(RDA5876_PSKEY_MISC)/sizeof(RDA5876_PSKEY_MISC[0]),0);
    usleep(50000);
    RDA_uart_write_array(fd, RDA5876_SWITCH_BAUDRATE ,sizeof(RDA5876_SWITCH_BAUDRATE)/sizeof(RDA5876_SWITCH_BAUDRATE[0]),0);
}
 
#if 0
void RDA5876_DUT_Test(int fd)
{
  printf ("%s\n", __func__);
 RDA_pin_to_low();
 RDA_pin_to_high();
 
 RDA5876_RfInit(fd);
 RDA5876_Pskey_RfInit(fd);
  
 RDA_pin_to_low();
 RDA_pin_to_high(); 
 usleep(50000);
 
 RDA5876_RfInit(fd);   
 RDA5876_Pskey_RfInit(fd);
 
 RDA5876_Dccal(fd); 
 

 
 RDA_uart_write_array(fd,RDA5876_DUT_PSKEY,sizeof(RDA5876_DUT_PSKEY)/sizeof(RDA5876_DUT_PSKEY[0]),0);
 
 RDA_uart_write_array(fd,RDA5876_DUT,sizeof(RDA5876_DUT)/sizeof(RDA5876_DUT[0]),0);
 
 
 
 
 write(fd,&(RDA_ENABLE_ALLSCAN[0]),sizeof(RDA_ENABLE_ALLSCAN)/sizeof(RDA_ENABLE_ALLSCAN[0]));
 
 write(fd,&(RDA_AUTOACCEPT_CONNECT[0]),sizeof(RDA_AUTOACCEPT_CONNECT)/sizeof(RDA_AUTOACCEPT_CONNECT[0])); 
 
 write(fd,&(RDA_ENABLE_DUT[0]),sizeof(RDA_ENABLE_DUT)/sizeof(RDA_ENABLE_DUT[0]));
}
 
#endif

#define RDA_BT_IOCTL_MAGIC 'u'

#define RDA_BT_POWER_ON_IOCTL _IO(RDA_BT_IOCTL_MAGIC ,0x01)
#define RD_BT_RF_INIT_IOCTL   _IO(RDA_BT_IOCTL_MAGIC ,0x02)
#define RD_BT_DC_CAL_IOCTL    _IO(RDA_BT_IOCTL_MAGIC ,0x03)
#define RD_BT_SET_RF_SWITCH_IOCTL _IO(RDA_BT_IOCTL_MAGIC ,0x04)
#define RDA_BT_POWER_OFF_IOCTL _IO(RDA_BT_IOCTL_MAGIC ,0x05)
#define RDA_BT_EN_CLK _IO(RDA_BT_IOCTL_MAGIC ,0x06)
#define RD_BT_DC_DIG_RESET_IOCTL    _IO(RDA_BT_IOCTL_MAGIC ,0x07)


#define RDABT_DRV_NAME "/dev/rdacombo"


int rdabt_send_cmd_to_drv(int cmd, unsigned char shutdown) 
{
	static int fd = -1;
	
	if(fd <  0)
	    fd = open(RDABT_DRV_NAME, O_RDWR);
		
	if (fd < 0) {
		perror("Can't open rdabt device");
		return -1;
	}
	
	if(ioctl(fd, cmd) == -1)
	{
		perror("rdabt_send_cmd_to_drv failed \n");
	}
		
	if(shutdown)
	{
		close(fd);
		fd = -1;
	}
	
	return 0;
}

static int rdabt_poweron_init(int fd, struct uart_t *u, struct termios *ti)
{

    rdabt_send_cmd_to_drv(RDA_BT_POWER_OFF_IOCTL, 0);
    rdabt_send_cmd_to_drv(RDA_BT_POWER_ON_IOCTL, 0);   	//power on
    rdabt_send_cmd_to_drv(RDA_BT_EN_CLK, 0); 
    printf("bf RD_BT_RF_INIT_IOCTL \n");
    usleep(200000); 
    rdabt_send_cmd_to_drv(RD_BT_DC_DIG_RESET_IOCTL, 0);    //houzhen add to ensure bt powe up safely
    usleep(200000); 
    rdabt_send_cmd_to_drv(RD_BT_RF_INIT_IOCTL, 0);
    rdabt_send_cmd_to_drv(RD_BT_SET_RF_SWITCH_IOCTL, 0);

  /*houzhen update 2012 03 06*/
   RDA5876_Pskey_RfInit(fd);  
   usleep(100000); 
   rdabt_send_cmd_to_drv(RD_BT_DC_CAL_IOCTL, 1);
   usleep(10000);                                     
   RDA5876_Pskey_Misc(fd);
   usleep(200000);
 
   return 0;
 
}

#endif

struct uart_t uart[] = {
	{ "any",        0x0000, 0x0000, HCI_UART_H4,   115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, NULL     },

	{ "ericsson",   0x0000, 0x0000, HCI_UART_H4,   57600,  115200,
				FLOW_CTL, DISABLE_PM, NULL, ericsson },

	{ "digi",       0x0000, 0x0000, HCI_UART_H4,   9600,   115200,
				FLOW_CTL, DISABLE_PM, NULL, digi     },

	{ "bcsp",       0x0000, 0x0000, HCI_UART_BCSP, 115200, 115200,
				0, DISABLE_PM, NULL, bcsp     },

#ifdef RDA_BT_SUPPORT
	/* RDA 5876 */
	{ "rda",     0x0000, 0x0000, HCI_UART_H4,   115200, 921600, 
				0, DISABLE_PM, NULL, rdabt_poweron_init },
#endif
	/* Xircom PCMCIA cards: Credit Card Adapter and Real Port Adapter */
	{ "xircom",     0x0105, 0x080a, HCI_UART_H4,   115200, 115200,
				FLOW_CTL, DISABLE_PM,  NULL, NULL     },

	/* CSR Casira serial adapter or BrainBoxes serial dongle (BL642) */
	{ "csr",        0x0000, 0x0000, HCI_UART_H4,   115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, csr      },

	/* BrainBoxes PCMCIA card (BL620) */
	{ "bboxes",     0x0160, 0x0002, HCI_UART_H4,   115200, 460800,
				FLOW_CTL, DISABLE_PM, NULL, csr      },

	/* Silicon Wave kits */
	{ "swave",      0x0000, 0x0000, HCI_UART_H4,   115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, swave    },

	/* Texas Instruments Bluelink (BRF) modules */
	{ "texas",      0x0000, 0x0000, HCI_UART_LL,   115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, texas,    texas2 },

	{ "texasalt",   0x0000, 0x0000, HCI_UART_LL,   115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, texasalt, NULL   },

	/* ST Microelectronics minikits based on STLC2410/STLC2415 */
	{ "st",         0x0000, 0x0000, HCI_UART_H4,    57600, 115200,
				FLOW_CTL, DISABLE_PM,  NULL, st       },

	/* ST Microelectronics minikits based on STLC2500 */
	{ "stlc2500",   0x0000, 0x0000, HCI_UART_H4, 115200, 115200,
			FLOW_CTL, DISABLE_PM, "00:80:E1:00:AB:BA", stlc2500 },

	/* Philips generic Ericsson IP core based */
	{ "philips",    0x0000, 0x0000, HCI_UART_H4,   115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, NULL     },

	/* Philips BGB2xx Module */
	{ "bgb2xx",    0x0000, 0x0000, HCI_UART_H4,   115200, 115200,
			FLOW_CTL, DISABLE_PM, "BD:B2:10:00:AB:BA", bgb2xx },

	/* Sphinx Electronics PICO Card */
	{ "picocard",   0x025e, 0x1000, HCI_UART_H4, 115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, NULL     },

	/* Inventel BlueBird Module */
	{ "inventel",   0x0000, 0x0000, HCI_UART_H4, 115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, NULL     },

	/* COM One Platinium Bluetooth PC Card */
	{ "comone",     0xffff, 0x0101, HCI_UART_BCSP, 115200, 115200,
				0, DISABLE_PM,  NULL, bcsp     },

	/* TDK Bluetooth PC Card and IBM Bluetooth PC Card II */
	{ "tdk",        0x0105, 0x4254, HCI_UART_BCSP, 115200, 115200,
				0, DISABLE_PM, NULL, bcsp     },

	/* Socket Bluetooth CF Card (Rev G) */
	{ "socket",     0x0104, 0x0096, HCI_UART_BCSP, 230400, 230400,
				0, DISABLE_PM, NULL, bcsp     },

	/* 3Com Bluetooth Card (Version 3.0) */
	{ "3com",       0x0101, 0x0041, HCI_UART_H4,   115200, 115200,
				FLOW_CTL, DISABLE_PM, NULL, csr      },

	/* AmbiCom BT2000C Bluetooth PC/CF Card */
	{ "bt2000c",    0x022d, 0x2000, HCI_UART_H4,    57600, 460800,
				FLOW_CTL, DISABLE_PM, NULL, csr      },

	/* Zoom Bluetooth PCMCIA Card */
	{ "zoom",       0x0279, 0x950b, HCI_UART_BCSP, 115200, 115200,
				0, DISABLE_PM, NULL, bcsp     },

	/* Sitecom CN-504 PCMCIA Card */
	{ "sitecom",    0x0279, 0x950b, HCI_UART_BCSP, 115200, 115200,
				0, DISABLE_PM, NULL, bcsp     },

	/* Billionton PCBTC1 PCMCIA Card */
	{ "billionton", 0x0279, 0x950b, HCI_UART_BCSP, 115200, 115200,
				0, DISABLE_PM, NULL, bcsp     },

	/* Broadcom BCM2035 */
	{ "bcm2035",    0x0A5C, 0x2035, HCI_UART_H4,   115200, 460800,
				FLOW_CTL, DISABLE_PM, NULL, bcm2035  },

	{ "ath3k",    0x0000, 0x0000, HCI_UART_ATH3K, 115200, 115200,
			FLOW_CTL, DISABLE_PM, NULL, ath3k_ps, ath3k_pm  },

	/* QUALCOMM BTS */
	{ "qualcomm",   0x0000, 0x0000, HCI_UART_H4,   115200, 115200,
			FLOW_CTL, DISABLE_PM, NULL, qualcomm, NULL },

	{ NULL, 0 }
};

static struct uart_t * get_by_id(int m_id, int p_id)
{
	int i;
	for (i = 0; uart[i].type; i++) {
		if (uart[i].m_id == m_id && uart[i].p_id == p_id)
			return &uart[i];
	}
	return NULL;
}

static struct uart_t * get_by_type(char *type)
{
	int i;
	for (i = 0; uart[i].type; i++) {
		if (!strcmp(uart[i].type, type))
			return &uart[i];
	}
	return NULL;
}

/* Initialize UART driver */
static int init_uart(char *dev, struct uart_t *u, int send_break, int raw)
{
	struct termios ti;
	int fd, i;
	unsigned long flags = 0;
#ifdef RDA_BT_SUPPORT
        struct serial_struct ss;

        tcflush(fd, TCIOFLUSH);
#endif
	if (raw)
		flags |= 1 << HCI_UART_RAW_DEVICE;

	fd = open(dev, O_RDWR | O_NOCTTY);
	if (fd < 0) {
		perror("Can't open serial port");
		return -1;
	}

	tcflush(fd, TCIOFLUSH);

	if (tcgetattr(fd, &ti) < 0) {
		perror("Can't get port settings");
		return -1;
	}

	cfmakeraw(&ti);

	ti.c_cflag |= CLOCAL;
	if (u->flags & FLOW_CTL)
		ti.c_cflag |= CRTSCTS;
	else
		ti.c_cflag &= ~CRTSCTS;

	if (tcsetattr(fd, TCSANOW, &ti) < 0) {
		perror("Can't set port settings");
		return -1;
	}

	/* Set initial baudrate */
	if (set_speed(fd, &ti, u->init_speed) < 0) {
		perror("Can't set initial baud rate");
		return -1;
	}

	tcflush(fd, TCIOFLUSH);

	if (send_break) {
		tcsendbreak(fd, 0);
		usleep(500000);
	}

	if (u->init && u->init(fd, u, &ti) < 0)
		return -1;

	tcflush(fd, TCIOFLUSH);

	/* Set actual baudrate */
	if (set_speed(fd, &ti, u->speed) < 0) {
		perror("Can't set baud rate");
		return -1;
	}

	/* Set TTY to N_HCI line discipline */
	i = N_HCI;
	if (ioctl(fd, TIOCSETD, &i) < 0) {
		perror("Can't set line discipline");
		return -1;
	}

	if (flags && ioctl(fd, HCIUARTSETFLAGS, flags) < 0) {
		perror("Can't set UART flags");
		return -1;
	}

	if (ioctl(fd, HCIUARTSETPROTO, u->proto) < 0) {
		perror("Can't set device");
		return -1;
	}

	if (u->post && u->post(fd, u, &ti) < 0)
		return -1;

	return fd;
}

static void usage(void)
{
	printf("hciattach - HCI UART driver initialization utility\n");
	printf("Usage:\n");
	printf("\thciattach [-n] [-p] [-b] [-r] [-t timeout] [-s initial_speed] <tty> <type | id> [speed] [flow|noflow] [bdaddr]\n");
	printf("\thciattach -l\n");
}

int main(int argc, char *argv[])
{
	struct uart_t *u = NULL;
	int detach, printpid, raw, opt, i, n, ld, err;
	int to = 10;
	int init_speed = 0;
	int send_break = 0;
	pid_t pid;
	struct sigaction sa;
	struct pollfd p;
	sigset_t sigs;
	char dev[PATH_MAX];

	detach = 1;
	printpid = 0;
	raw = 0;

	while ((opt=getopt(argc, argv, "bnpt:s:lr")) != EOF) {
		switch(opt) {
		case 'b':
			send_break = 1;
			break;

		case 'n':
			detach = 0;
			break;

		case 'p':
			printpid = 1;
			break;

		case 't':
			to = atoi(optarg);
			break;

		case 's':
			init_speed = atoi(optarg);
			break;

		case 'l':
			for (i = 0; uart[i].type; i++) {
				printf("%-10s0x%04x,0x%04x\n", uart[i].type,
							uart[i].m_id, uart[i].p_id);
			}
			exit(0);

		case 'r':
			raw = 1;
			break;

		default:
			usage();
			exit(1);
		}
	}

	n = argc - optind;
	if (n < 2) {
		usage();
		exit(1);
	}

	for (n = 0; optind < argc; n++, optind++) {
		char *opt;

		opt = argv[optind];

		switch(n) {
		case 0:
			dev[0] = 0;
			if (!strchr(opt, '/'))
				strcpy(dev, "/dev/");
			strcat(dev, opt);
			break;

		case 1:
			if (strchr(argv[optind], ',')) {
				int m_id, p_id;
				sscanf(argv[optind], "%x,%x", &m_id, &p_id);
				u = get_by_id(m_id, p_id);
			} else {
				u = get_by_type(opt);
			}

			if (!u) {
				fprintf(stderr, "Unknown device type or id\n");
				exit(1);
			}

			break;

		case 2:
			u->speed = atoi(argv[optind]);
			break;

		case 3:
			if (!strcmp("flow", argv[optind]))
				u->flags |=  FLOW_CTL;
			else
				u->flags &= ~FLOW_CTL;
			break;

		case 4:
			if (!strcmp("sleep", argv[optind]))
				u->pm = ENABLE_PM;
			else
				u->pm = DISABLE_PM;
			break;

		case 5:
			u->bdaddr = argv[optind];
			break;
		}
	}

	if (!u) {
		fprintf(stderr, "Unknown device type or id\n");
		exit(1);
	}

	/* If user specified a initial speed, use that instead of
	   the hardware's default */
	if (init_speed)
		u->init_speed = init_speed;

	memset(&sa, 0, sizeof(sa));
	sa.sa_flags   = SA_NOCLDSTOP;
	sa.sa_handler = sig_alarm;
	sigaction(SIGALRM, &sa, NULL);

	/* 10 seconds should be enough for initialization */
	alarm(to);
	bcsp_max_retries = to;

	n = init_uart(dev, u, send_break, raw);
	if (n < 0) {
		perror("Can't initialize device");
		exit(1);
	}

	printf("Device setup complete\n");

	alarm(0);

	memset(&sa, 0, sizeof(sa));
	sa.sa_flags   = SA_NOCLDSTOP;
	sa.sa_handler = SIG_IGN;
	sigaction(SIGCHLD, &sa, NULL);
	sigaction(SIGPIPE, &sa, NULL);

	sa.sa_handler = sig_term;
	sigaction(SIGTERM, &sa, NULL);
	sigaction(SIGINT,  &sa, NULL);

	sa.sa_handler = sig_hup;
	sigaction(SIGHUP, &sa, NULL);

	if (detach) {
		if ((pid = fork())) {
			if (printpid)
				printf("%d\n", pid);
			return 0;
		}

		for (i = 0; i < 20; i++)
			if (i != n)
				close(i);
	}

	p.fd = n;
	p.events = POLLERR | POLLHUP;

	sigfillset(&sigs);
	sigdelset(&sigs, SIGCHLD);
	sigdelset(&sigs, SIGPIPE);
	sigdelset(&sigs, SIGTERM);
	sigdelset(&sigs, SIGINT);
	sigdelset(&sigs, SIGHUP);

	while (!__io_canceled) {
		p.revents = 0;
		err = ppoll(&p, 1, NULL, &sigs);
		if (err < 0 && errno == EINTR)
			continue;
		if (err)
			break;
	}

	/* Restore TTY line discipline */
	ld = N_TTY;
	if (ioctl(n, TIOCSETD, &ld) < 0) {
		perror("Can't restore line discipline");
		exit(1);
	}

#ifdef RDA_BT_SUPPORT
  rdabt_send_cmd_to_drv(RDA_BT_POWER_OFF_IOCTL, 1);
#endif
	return 0;
}
