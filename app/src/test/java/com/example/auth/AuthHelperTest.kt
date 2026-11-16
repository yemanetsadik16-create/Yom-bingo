package com.example.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthHelperTest {

  @Test
  fun `username maps to internal yemebingo email`() {
    assertEquals("yemane@yemebingo.internal", usernameToInternalEmail("Yemane"))
    assertEquals("abel.12@yemebingo.internal", usernameToInternalEmail("  Abel 12  "))
    assertEquals("desbelebisrat12@gmail.com", usernameToInternalEmail("DesbeleBisrat12@gmail.com"))
  }

  @Test
  fun `registration username validation checks length and characters`() {
    assertTrue(isValidRegistrationUsername("Ab"))
    assertTrue(isValidRegistrationUsername("Yemane_99"))
    assertFalse(isValidRegistrationUsername("a"))
    assertFalse(isValidRegistrationUsername("user@gmail.com"))
    assertFalse(isValidRegistrationUsername("   "))
  }
}
