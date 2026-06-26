package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EnumsTest {
    @Test
    fun `test enum mapping ACTIVE`() {
        val activeAccountDto =
            Network.AccountDto(
                id = "account-1",
                status = Network.StatusDto.ACTIVE,
                balance = 1000.0,
            )
        val activeAccount = mapAccountDtoToAccount(activeAccountDto)

        assertNotNull(activeAccount)
        assertEquals("account-1", activeAccount?.id)
        assertEquals(Domain.Status.ACTIVE, activeAccount?.status)
        assertEquals(1000.0, activeAccount?.balance ?: 0.0, 0.001)
    }

    @Test
    fun `test enum mapping INACTIVE`() {
        val inactiveAccountDto =
            Network.AccountDto(
                id = "account-2",
                status = Network.StatusDto.INACTIVE,
                balance = 500.0,
            )
        val inactiveAccount = mapAccountDtoToAccount(inactiveAccountDto)

        assertNotNull(inactiveAccount)
        assertEquals("account-2", inactiveAccount?.id)
        assertEquals(Domain.Status.INACTIVE, inactiveAccount?.status)
        assertEquals(500.0, inactiveAccount?.balance ?: 0.0, 0.001)
    }

    @Test
    fun `test enum mapping PENDING`() {
        val pendingAccountDto =
            Network.AccountDto(
                id = "account-3",
                status = Network.StatusDto.PENDING,
                balance = 250.0,
            )
        val pendingAccount = mapAccountDtoToAccount(pendingAccountDto)

        assertNotNull(pendingAccount)
        assertEquals("account-3", pendingAccount?.id)
        assertEquals(Domain.Status.PENDING, pendingAccount?.status)
        assertEquals(250.0, pendingAccount?.balance ?: 0.0, 0.001)
    }

    @Test
    fun `test null enum defaults to first enum value`() {
        val nullStatusDto =
            Network.AccountDto(
                id = "account-4",
                status = null,
                balance = 0.0,
            )
        val nullStatusAccount = mapAccountDtoToAccount(nullStatusDto)

        assertNotNull(nullStatusAccount)
        assertEquals("account-4", nullStatusAccount?.id)
        assertEquals(Domain.Status.ACTIVE, nullStatusAccount?.status)
        assertEquals(0.0, nullStatusAccount?.balance ?: 0.0, 0.001)
    }
}
