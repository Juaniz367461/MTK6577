#ifndef __MTK_SPC_REG_H__
#define __MTK_SPC_REG_H__

#include <asm/io.h>
#include <mach/mt6577_reg_base.h>

#define REG_SMI_SPC_BASE            0xF0008000
#define REG_SMI_SEN                 0x500
#define REG_SMI_SRAM_RANGE0         0x520
#define REG_SMI_SRAM_RANGE1         0x524
#define REG_SMI_SRAM_RANGE2         0x528
#define REG_SMI_SRAM_RANGE3         0x52C
#define REG_SMI_SRNG_ACTL0          0x530
#define REG_SMI_SRNG_ACTL1          0x534
#define REG_SMI_SRNG_ACTL2          0x538
#define REG_SMI_SRNG_ACTL3          0x53C
#define REG_SMI_SRNG_ACTL4          0x540
#define REG_SMI_D_VIO_CON0          0x550
#define REG_SMI_D_VIO_CON1          0x554
#define REG_SMI_D_VIO_CON2          0x558
#define REG_SMI_D_VIO_STA0          0x560
#define REG_SMI_D_VIO_STA1          0x564
#define REG_SMI_D_VIO_STA2          0x568
#define REG_SMI_VIO_DBG0            0x570
#define REG_SMI_VIO_DBG1            0x574
#define REG_SMI_SECUR_CON0          0x5C0
#define REG_SMI_SECUR_CON1          0x5C4
#define REG_SMI_SECUR_CON2          0x5C8
#define REG_SMI_SECUR_CON3          0x5CC
#define REG_SMI_SECUR_CON4          0x5D0
#define REG_SMI_SECUR_CON5          0x5D4
#define REG_SMI_SECUR_CON6          0x5D8
#define REG_SMI_SECUR_CON7          0x5DC
#define REG_SMI_SECUR_CON_MCU       0x5E0

static inline unsigned int SPC_ReadReg32(unsigned int Offset) 
{
  return ioread32(REG_SMI_SPC_BASE+Offset);
}
static inline void SPC_WriteReg32(unsigned int Offset, unsigned int Val)
{                   
  iowrite32(Val, REG_SMI_SPC_BASE+Offset);
}

#endif




