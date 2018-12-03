package eu.eyan.bt

import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import javax.swing.JFrame
import javax.bluetooth.LocalDevice
import eu.eyan.log.Log
import javax.bluetooth.DiscoveryAgent
import javax.bluetooth.DiscoveryListener
import javax.bluetooth.RemoteDevice
import javax.bluetooth.DeviceClass
import javax.bluetooth.ServiceRecord

object KekFog extends App {
  Log.activateInfoLevel
  //  new JFrame().withComponent(panel)
  //http://www.aviyehuda.com/blog/2010/01/08/connecting-to-bluetooth-devices-with-java/
  val localDevice = LocalDevice.getLocalDevice
  val agent = localDevice.getDiscoveryAgent()
  agent.startInquiry(DiscoveryAgent.GIAC, new MyDiscoveryListener());
  Thread.sleep(30000)
}

class MyDiscoveryListener extends DiscoveryListener {
  def inquiryCompleted(arg0: Int) = Log.info(arg0 + "")
  def serviceSearchCompleted(arg0: Int, arg1: Int) = Log.info(arg0 + " " + arg1)
  def servicesDiscovered(arg0: Int, arg1: Array[ServiceRecord]) = Log.info(arg0 + " " + arg1.mkString)
  def deviceDiscovered(btDevice: RemoteDevice, arg1: DeviceClass) = {
    var name: String = ""
    try name = btDevice.getFriendlyName(false)
    catch { case e: Exception => name = btDevice.getBluetoothAddress() }
    Log.info("device found: " + name)
    Log.info("btDevice" + btDevice)
    Log.info("deviceClass" + arg1)
  }
}