package eu.eyan.bt

import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import javax.swing.JFrame
import javax.bluetooth.LocalDevice

object KekFog extends App {
//  new JFrame().withComponent(panel)
  val localDevice = LocalDevice.getLocalDevice
}