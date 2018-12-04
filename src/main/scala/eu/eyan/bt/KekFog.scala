package eu.eyan.bt

import scala.collection.mutable.MutableList

import eu.eyan.log.Log
import eu.eyan.log.LogWindow
import eu.eyan.util.registry.RegistryGroup
import eu.eyan.util.registry.RegistryPlus
import eu.eyan.util.swing.JButtonPlus.JButtonImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JPanelWithFrameLayout
import eu.eyan.util.swing.JTablePlus
import eu.eyan.util.swing.JTablePlus.JTableImplicit
import javax.bluetooth.DeviceClass
import javax.bluetooth.DiscoveryAgent
import javax.bluetooth.DiscoveryListener
import javax.bluetooth.LocalDevice
import javax.bluetooth.RemoteDevice
import javax.bluetooth.ServiceRecord
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JScrollPane
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import javax.bluetooth.UUID
import eu.eyan.util.swing.JTablePlus3

//  http://www.aviyehuda.com/blog/2010/01/08/connecting-to-bluetooth-devices-with-java/
//  http://snapshot.bluecove.org/distribution/download/2.1.1-SNAPSHOT/2.1.1-SNAPSHOT.63/
object KekFog extends App {
  Log.activateInfoLevel

  private val registry = RegistryGroup("Kekfog")
  
  private val btDevicesTable =
    new JTablePlus3[String, BluetoothDevice](
      ("Name", _.btRemoteDevice.getBluetoothAddress),
      ("Address", bt => try bt.btRemoteDevice.getFriendlyName(false) catch { case e: Exception => Log.error(e); "-" }),
      ("Class", _.classOfDevice.toString),
      ("Major class", _.classOfDevice.getMajorDeviceClass + ""),
      ("Minor class", _.classOfDevice.getMinorDeviceClass + ""),
      ("Service class", _.classOfDevice.getServiceClasses + ""))
      .rememberColumnWidhts(registry.registryValue("table"))

  private val btServicesTable =
    new JTablePlus3[String, ServiceRecord](
      ("AttributeIDs", _.getAttributeIDs.mkString(", ")),
      ("ConnectionURL", _.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false)),
      ("AttributeValue", rec => String.valueOf(rec.getAttributeValue(0x0100))),
      ("HostDevice", _.getHostDevice.getBluetoothAddress))
      .rememberColumnWidhts(registry.registryValue("table"))

  private def discoveredCallBack(bt: BluetoothDevice) = btDevicesTable += bt

  private def searchForService(bt: BluetoothDevice): Unit = discoveryAndListener.searchServices(bt)

  private def startServiceSearch: Unit = { btServicesTable.clear ; serviceSearchButton.disabled; serviceSearchButton.setText("Inquiry"); btDevicesTable.selection.foreach(searchForService _) }
  private def servicesDiscovered(services: Array[ServiceRecord]) = services.foreach(btServicesTable += _)
  private def serviceSearchCompletedCallback(result: InquiryResult) = { serviceSearchButton.enabled; serviceSearchButton.setText("Discover " + result) }

  private val discoveryAndListener = new MyDiscovery(discoveredCallBack, inquiryEnded, servicesDiscovered, serviceSearchCompletedCallback)
  private def inquiryStart: Unit = { btDevicesTable.clear; inquiryButton.disabled; inquiryButton.setText("Inquiry"); discoveryAndListener.startInquiry }
  private def inquiryEnded(result: InquiryResult): Unit = { inquiryButton.enabled; inquiryButton.setText("Discover " + result) }

  private val panel = new JPanelWithFrameLayout().withBorders.withSeparators
  private val inquiryButton = panel.newColumn("f:1000px").newRow.addButton("Discover").onActionPerformed(inquiryStart)

  panel.newRow.addFluentInScrollPane(btDevicesTable)
  private val serviceSearchButton = panel.newRow.addButton("Search Service").onAction(startServiceSearch)
  panel.newRow.addFluentInScrollPane(btServicesTable)

  new JFrame()
    .iconFromChar('B')
    .title("Kékfog")
    .onCloseHide
    .withComponent(panel)
    .menuItem("File", "Exit", System.exit(0))
    .menuItem("Debug", "Open log window", LogWindow.show(null))
    .menuItem("Debug", "Clear registry values", RegistryPlus.clear("Kekfog"))
    .packAndSetVisible
    .maximize
}

case class BluetoothDevice(btRemoteDevice: RemoteDevice, classOfDevice: DeviceClass)

trait InquiryResult
case object INQUIRY_COMPLETED extends InquiryResult
case object INQUIRY_TERMINATED extends InquiryResult
case object INQUIRY_ERROR extends InquiryResult
case object INQUIRY_UNKNOWN extends InquiryResult
case object SERVICE_SEARCH_COMPLETED extends InquiryResult
case object SERVICE_SEARCH_TERMINATED extends InquiryResult
case object SERVICE_SEARCH_ERROR extends InquiryResult
case object SERVICE_SEARCH_NO_RECORDS extends InquiryResult
case object SERVICE_SEARCH_DEVICE_NOT_REACHABLE extends InquiryResult
case object SERVICE_SEARCH_UNKNOWN extends InquiryResult

class MyDiscovery(
  discoveredCallBack:             BluetoothDevice => Unit,
  inquiryCompletedCallback:       InquiryResult => Unit,
  servicesDiscoveredCallback:     Array[ServiceRecord] => Unit,
  serviceSearchCompletedCallback: InquiryResult => Unit) {
  def startInquiry = agent.startInquiry(DiscoveryAgent.GIAC, discoveryListener)
  def searchServices(bt: BluetoothDevice) = Log.info(agent.searchServices(null, Array(new UUID(0x1105)), bt.btRemoteDevice, discoveryListener))

  private val localDevice = LocalDevice.getLocalDevice
  private val agent = localDevice.getDiscoveryAgent()

  private val discoveryListenerCodes = Map(
    0x00 -> INQUIRY_COMPLETED,
    0x05 -> INQUIRY_TERMINATED,
    0x07 -> INQUIRY_ERROR,
    0x01 -> SERVICE_SEARCH_COMPLETED,
    0x02 -> SERVICE_SEARCH_TERMINATED,
    0x03 -> SERVICE_SEARCH_ERROR,
    0x04 -> SERVICE_SEARCH_NO_RECORDS,
    0x06 -> SERVICE_SEARCH_DEVICE_NOT_REACHABLE)

  private val discoveryListener = new DiscoveryListener {
    def deviceDiscovered(btRemoteDevice: RemoteDevice, classOfDevice: DeviceClass) = {
      discoveredCallBack(BluetoothDevice(btRemoteDevice, classOfDevice))
      var name: String = ""
      try name = btRemoteDevice.getFriendlyName(false)
      catch { case e: Exception => name = btRemoteDevice.getBluetoothAddress }
      Log.info(s"""
				  device found: $name
				  btDevice $btRemoteDevice
				  deviceClass $classOfDevice""")
    }

    def inquiryCompleted(result: Int) = {
      Log.info(result + "=" + discoveryListenerCodes.get(result))
      inquiryCompletedCallback(discoveryListenerCodes.get(result).getOrElse(INQUIRY_UNKNOWN))
    }

    def serviceSearchCompleted(transactionID: Int, responseCode: Int) = {
      Log.info(transactionID + " " + responseCode + "=" + discoveryListenerCodes.get(responseCode))
      serviceSearchCompletedCallback(discoveryListenerCodes.get(responseCode).getOrElse(SERVICE_SEARCH_UNKNOWN))
    }

    def servicesDiscovered(transactionID: Int, listOfServices: Array[ServiceRecord]) = {
      Log.info(transactionID + " " + listOfServices.mkString)
      servicesDiscoveredCallback(listOfServices)
    }
  }
}