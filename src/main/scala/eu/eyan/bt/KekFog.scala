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
import rx.lang.scala.subjects.BehaviorSubject
import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.Observable
import eu.eyan.util.rx.lang.scala.ObservablePlus.ObservableImplicitBoolean
import eu.eyan.util.rx.lang.scala.ObservablePlus.ObservableImplicit
import eu.eyan.util.text.Text
import java.awt.Color
import org.apache.log4j.BasicConfigurator
import scala.collection.mutable.ListBuffer
import javax.obex.ClientSession
import javax.microedition.io.Connector
import javax.obex.ResponseCodes
import javax.obex.HeaderSet

//  http://www.aviyehuda.com/blog/2010/01/08/connecting-to-bluetooth-devices-with-java/
//  http://snapshot.bluecove.org/distribution/download/2.1.1-SNAPSHOT/2.1.1-SNAPSHOT.63/
object KekFog extends App {
  BasicConfigurator.configure(Log.log4jAppender)
  //  BasicConfigurator.configure
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
      ("AttributeIDs", attributes(_)),
      ("ConnectionURL", _.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false)),
      ("AttributeValue", rec => String.valueOf(rec.getAttributeValue(0x0100))),
      ("HostDevice", _.getHostDevice.getBluetoothAddress))
      .rememberColumnWidhts(registry.registryValue("table"))

  private def attributes(serviceRecord: ServiceRecord) = {
    val ids = serviceRecord.getAttributeIDs
    val values = ids.map(serviceRecord.getAttributeValue(_))

    ids.zip(values).mkString(", ")
  }

  private val bluetoothDiscovery = new BluetoothDiscovery

  private val panel = new JPanelWithFrameLayout().withBorders.withSeparators
  private val inquiryButton = panel.newColumn("f:1000px").newRow.addButton("-").onActionPerformed(inquiryStart)
  panel.newRow.addFluentInScrollPane(btDevicesTable)
  private val serviceSearchButton = panel.newRow.addButton("-").onAction(startServiceSearch)
  panel.newRow.addFluentInScrollPane(btServicesTable)

  inquiryButton.enabled(bluetoothDiscovery.discoveryActive.negate)
  inquiryButton.text(bluetoothDiscovery.discoveryState.map("Discover (" + _ + ")"))
  serviceSearchButton.enabled(bluetoothDiscovery.discoveryActive.negate.and(btDevicesTable.isRowSelected))
  serviceSearchButton.text(bluetoothDiscovery.discoveryState.map("Search Service (" + _ + ")"))

  new JFrame()
    .iconFromChar('B', Color.blue.brighter.brighter.brighter)
    .title("Kékfog")
    .onCloseDispose
    .withComponent(panel)
    .menuItem("File", "Exit", System.exit(0))
    .menuItem("Debug", "Open log window", LogWindow.show(null))
    .menuItem("Debug", "Clear registry values", RegistryPlus.clear("Kekfog"))
    .packAndSetVisible
    .maximize

  bluetoothDiscovery.servicesDiscovered.subscribe(_.services.foreach(btServicesTable += _))
  bluetoothDiscovery.deviceDiscovered.subscribe(btDevicesTable += _)

  private def searchForService(bt: BluetoothDevice): Unit = bluetoothDiscovery.searchServices(bt)
  private def startServiceSearch: Unit = { btServicesTable.clear; btDevicesTable.selection.foreach(searchForService _) }

  private def inquiryStart: Unit = { btDevicesTable.clear; bluetoothDiscovery.startInquiry }

}

case class BluetoothDevice(btRemoteDevice: RemoteDevice, classOfDevice: DeviceClass)
case class BluetoothServices(transactionId: Int, services: Array[ServiceRecord])

trait DiscoveryState
case object DISCOVERY_NOT_STARTED extends DiscoveryState

case object INQUIRY_STARTED extends DiscoveryState
case object INQUIRY_COMPLETED extends DiscoveryState
case object INQUIRY_TERMINATED extends DiscoveryState
case object INQUIRY_ERROR extends DiscoveryState
case object INQUIRY_UNKNOWN extends DiscoveryState

case object SERVICE_SEARCH_STARTED extends DiscoveryState
case object SERVICE_SEARCH_COMPLETED extends DiscoveryState
case object SERVICE_SEARCH_TERMINATED extends DiscoveryState
case object SERVICE_SEARCH_ERROR extends DiscoveryState
case object SERVICE_SEARCH_NO_RECORDS extends DiscoveryState
case object SERVICE_SEARCH_DEVICE_NOT_REACHABLE extends DiscoveryState
case object SERVICE_SEARCH_UNKNOWN extends DiscoveryState

case object UUID_A extends UUID(0x1105)

case object UUID_Base_UUID_Value extends UUID("0000000000001000800000805F9B34FB", false) // 128-bit  (Used in promoting 16-bit and 32-bit UUIDs to 128-bit UUIDs)
case object UUID_SDP extends UUID(0x0001)
case object UUID_RFCOMM extends UUID(0x0003)
case object UUID_OBEX extends UUID(0x0008)
case object UUID_HTTP extends UUID(0x000C)
case object UUID_L2CAP extends UUID(0x0100)
case object UUID_BNEP extends UUID(0x000F)
case object UUID_Serial_Port extends UUID(0x1101)
case object UUID_ServiceDiscoveryServerServiceClassID extends UUID(0x1000)
case object UUID_BrowseGroupDescriptorServiceClassID extends UUID(0x1001)
case object UUID_PublicBrowseGroup extends UUID(0x1002)
case object UUID_OBEX_Object_Push_Profile extends UUID(0x1105)
case object UUID_OBEX_File_Transfer_Profile extends UUID(0x1106)
case object UUID_Personal_Area_Networking_User extends UUID(0x1115)
case object UUID_Network_Access_Point extends UUID(0x1116)
case object UUID_Group_Network extends UUID(0x1117)

class BluetoothDiscovery() {
  private val ALL_UUIDS = Array[UUID](
    UUID_Base_UUID_Value,
    UUID_SDP,
    UUID_RFCOMM,
    UUID_OBEX,
    UUID_HTTP,
    UUID_L2CAP,
    UUID_BNEP,
    UUID_Serial_Port,
    UUID_ServiceDiscoveryServerServiceClassID,
    UUID_BrowseGroupDescriptorServiceClassID,
    UUID_PublicBrowseGroup,
    UUID_OBEX_Object_Push_Profile,
    UUID_OBEX_File_Transfer_Profile,
    UUID_Personal_Area_Networking_User,
    UUID_Network_Access_Point,
    UUID_Group_Network)

  case class ServiceToSearch(bt: BluetoothDevice, service: UUID)
  private val servicesToSearch = ListBuffer[ServiceToSearch]()

  def discoveryState = discoveryState_.distinctUntilChanged
  def startInquiry: Boolean = {
    if (discoveryActive.get) false
    else {
      discoveryState_ onNext INQUIRY_STARTED
      agent.startInquiry(DiscoveryAgent.GIAC, discoveryListener)
    }
  }
  def deviceDiscovered = deviceDiscovered_.asInstanceOf[Observable[BluetoothDevice]]

  def searchServices(bt: BluetoothDevice) = {
    if (discoveryActive.get) -2
    else {
      discoveryState_ onNext SERVICE_SEARCH_STARTED
      ALL_UUIDS.foreach(uuid => servicesToSearch += ServiceToSearch(bt, uuid))
      searchNextService
    }
  }

  private def searchNextService = if (servicesToSearch.nonEmpty) {
    Log.info(servicesToSearch)
    val serviceToSearch = servicesToSearch.head
    servicesToSearch.remove(0)
    agent.searchServices(null, Array(serviceToSearch.service), serviceToSearch.bt.btRemoteDevice, discoveryListener)
  }

  def servicesDiscovered = servicesDiscovered_.asInstanceOf[Observable[BluetoothServices]]

  private val discoveryListener = new MyDiscoveryListener
  private val discoveryState_ = BehaviorSubject[DiscoveryState](DISCOVERY_NOT_STARTED)
  private val deviceDiscovered_ = PublishSubject[BluetoothDevice]
  private val servicesDiscovered_ = PublishSubject[BluetoothServices]

  private lazy val agent = localDevice.getDiscoveryAgent
  private lazy val localDevice = LocalDevice.getLocalDevice

  val discoveryActive = discoveryState.map(state => state == INQUIRY_STARTED || state == SERVICE_SEARCH_STARTED)

  private val discoveryListenerCodes = Map(
    0x00 -> INQUIRY_COMPLETED,
    0x01 -> SERVICE_SEARCH_COMPLETED,
    0x02 -> SERVICE_SEARCH_TERMINATED,
    0x03 -> SERVICE_SEARCH_ERROR,
    0x04 -> SERVICE_SEARCH_NO_RECORDS,
    0x05 -> INQUIRY_TERMINATED,
    0x06 -> SERVICE_SEARCH_DEVICE_NOT_REACHABLE,
    0x07 -> INQUIRY_ERROR)

  private class MyDiscoveryListener extends DiscoveryListener {
    def deviceDiscovered(btRemoteDevice: RemoteDevice, classOfDevice: DeviceClass) = {
      Log.debug(s"""btDevice $btRemoteDevice, deviceClass $classOfDevice""")
      deviceDiscovered_ onNext BluetoothDevice(btRemoteDevice, classOfDevice)
    }

    def inquiryCompleted(result: Int) = {
      Log.debug(result + "=" + discoveryListenerCodes.get(result))
      discoveryState_ onNext discoveryListenerCodes.get(result).getOrElse(INQUIRY_UNKNOWN)
    }

    def servicesDiscovered(transactionID: Int, listOfServices: Array[ServiceRecord]) = {
      Log.debug(transactionID + " " + listOfServices.mkString)
      servicesDiscovered_ onNext BluetoothServices(transactionID, listOfServices)
      sendMessage(listOfServices)
      logServices(listOfServices)
    }

    def serviceSearchCompleted(transactionID: Int, responseCode: Int) = {
      Log.debug(transactionID + " " + responseCode + "=" + discoveryListenerCodes.get(responseCode))
      if (servicesToSearch.isEmpty) discoveryState_ onNext discoveryListenerCodes.get(responseCode).getOrElse(SERVICE_SEARCH_UNKNOWN)
      else searchNextService
    }
  }

  def logServices(services: Array[ServiceRecord]) = {
    for (service <- services; attributeID <- service.getAttributeIDs; data = service.getAttributeValue(attributeID) if data != null) {
//      Log.info(serviceName.getDataType + s", $attributeID")//, $serviceName, " + service.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false))
      Log.info(data)
    }
  }

  def sendMessage(services: Array[ServiceRecord]) = {
    for (service <- services) {
      val url = service.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false)
      if (url != null) {
        val serviceName = service.getAttributeValue(0x0100)
        if (serviceName == null)
          Log.debug("serviceNotFound " + url)
        else {
          Log.info("service " + serviceName.getValue() + " found " + url)

          if (serviceName.getValue().equals("OBEX Object Push")) {
            sendMessageToDevice(url)
          }
        }
      }
    }
  }

  def sendMessageToDevice(serverURL: String) = {
    Log.info("Connecting to " + serverURL);

    val clientSession = Connector.open(serverURL).asInstanceOf[ClientSession]
    val hsConnectReply = clientSession.connect(null)
    if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
      Log.warn("Failed to connect")
    } else {
      val hsOperation = clientSession.createHeaderSet()
      hsOperation.setHeader(HeaderSet.NAME, "Hello.txt")
      hsOperation.setHeader(HeaderSet.TYPE, "text")

      //Create PUT Operation
      val putOperation = clientSession.put(hsOperation)

      // Sending the message
      val data = "Hello World !!!".getBytes("iso-8859-1")
      val os = putOperation.openOutputStream()
      os.write(data)
      os.close()

      putOperation.close()
      clientSession.disconnect(null)
      clientSession.close()
    }
  }
}