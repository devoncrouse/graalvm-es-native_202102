package com.example.app

import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

class EntryPointTest extends JUnitSuite {
  @Test
  def runMain() {
    EntryPoint.main(Array("5"))
  }
}