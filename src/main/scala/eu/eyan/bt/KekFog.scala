package eu.eyan.bt

import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import javax.swing.JFrame
import javax.bluetooth.LocalDevice

object KekFog extends App {
//  new JFrame().withComponent(panel)
  //http://www.aviyehuda.com/blog/2010/01/08/connecting-to-bluetooth-devices-with-java/
  println(LocalDevice.isPowerOn)
  val localDevice = LocalDevice.getLocalDevice
}