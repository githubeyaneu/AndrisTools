package eu.eyan.fritzbox

import eu.eyan.util.java.lang.ClassPlus.ClassPlusImplicit
import eu.eyan.util.jdbc.Database
import eu.eyan.util.jdbc.annotations.SqlTable

import scala.xml.Node


/*

Attribute von <device/group>:
• identifier: eindeutige ID, AIN, MAC-Adresse
Achtung: für HANFUN- oder Zigbee-Geräte siehe auch Kapitel 1.1 zum Identifier bei Units
• id: interne Geräteid
• fwversion: Firmwareversion des Gerätes
• manufacturer: "AVM"
• productname: Produktname des Gerätes, leer bei unbekanntem/undefiniertem Gerät
• functionbitmask: Bitmaske der Geräte-Funktionsklassen, beginnen mit Bit 0, es können mehrere Bits gesetzt sein
Bit 0: HAN-FUN Gerät
Bit 2: Licht/Lampe
Bit 4: Alarm-Sensor
Bit 5: AVM Button
Bit 6: AVM Heizkörperregler
Bit 7: AVM Energie Messgerät
Bit 8: Temperatursensor
Bit 9: AVM Schaltsteckdose
Bit 10: AVM DECT Repeater
Bit 11: AVM Mikrofon
Bit 13: HAN-FUN-Unit
Bit 15: an-/ausschaltbares Gerät/Steckdose/Lampe/Aktor
Bit 16: Gerät mit einstellbarem Dimm-, Höhen- bzw. Niveau-Level
Bit 17: Lampe mit einstellbarer Farbe/Farbtemperatur
Bit 18: Rollladen(Blind) - hoch, runter, stop und level 0% bis 100 %
Bit 20: Luftfeuchtigkeitssensor
Die Bits 5,6,7,9,10 und 11 werden nur von FRITZ!-Geräten verwendet und nicht von HANFUN- oder Zigbee-Geräten.
Beispiel FD300: binär 101000000(320), Bit6(HKR) und Bit8(Temperatursensor) sind gesetzt
Unterknoten von <device>/<group>
<present>0/1 - Gerät verbunden nein/ja
<txbusy>0/1 – das Senden eines Kommandos(wie Schaltbefehl oder Helligkeit ändern) läuft – ja(1) bzw. nein(0)
<name>Gerätename
optionale Unterknoten von <device>/<group> - wenn vom Gerät unterstützt
<batterylow>0 oder 1: Batterieladezustand niedrig - bitte Batterie wechseln
<battery>Batterieladezustand in Prozent
Es folgenden Knoten für die verschiedenen Funktionsgruppen der Geräte (siehe functionbitmask). Nur grundsätzlich
unterstützte Funktionsgruppen werden übermittelt.
Bspw. gibt es die <temperature>-Funktionsgruppe nur bei Geräten mit Temperatursensor.


Temperatursensor
<temperature>
<celsius>Wert in 0,1 °C, negative und positive Werte möglich
<offset>Wert in 0,1 °C, negative und positive Werte möglich

Heizkörperregler
<hkr>
<tist>Isttemperatur in 0,5 °C, Wertebereich: 0x0 – 0x64
0 <= 0°C, 1 = 0,5°C...... 120 = 60°C, 254 = ON , 253 = OFF
leer wenn die Isttemperatur unbekannt ist(bspw. wenn der HKR inaktiv ist, also present=0)
<tsoll>Solltemperatur in 0,5 °C, Wertebereich: 0x10 – 0x38
16 – 56 (8 bis 28°C), 16 <= 8°C, 17 = 8,5°C...... 56 >= 28°C, 254 = ON , 253 = OFF
leer wenn die Solltemperatur unbekannt ist(bspw. wenn der HKR inaktiv ist, also present=0)
<komfort>Komforttemperatur in 0,5 °C, Wertebereich: 0x10 – 0x38
16 – 56 (8 bis 28°C), 16 <= 8°C, 17 = 8,5°C...... 56 >= 28°C, 254 = ON , 253 = OFF
<absenk>Absenktemperatur in 0,5 °C, Wertebereich: 0x10 – 0x38
16 – 56 (8 bis 28°C), 16 <= 8°C, 17 = 8,5°C...... 56 >= 28°C, 254 = ON , 253 = OFF
<batterylow>0 oder 1: Batterieladezustand niedrig - bitte Batterie wechseln
<battery>Batterieladezustand in Prozent
<windowopenactiv> Fenster-offen Modus aktiviert: 0 oder 1
Der Fenster-offen Modus kann entweder durch einen Temperaturabfall vom HKR selbst erkannt,
durch einen externen Tür-/Fenstersensor ausgelöst oder über die API mit sethkrwindowopen aktiviert worden sein.
<windowopenactiveendtime> Fenster-offen End-Zeit, in Sekunden seit 1970
wenn ein externer Tür-/Fenstersensor zugeordnet und aktiv ist: windowopenactiveendtime = -1
in diesem Fall ist die End-Zeit unbekannt
Wenn der Fenster-offen Modus vom HKR selbst durch einen Temperaturabfall erkannt worden ist, wird die
End-Zeit durch den in der FRITZ!OS Benutzeroberfläche konfigurierten Wert bestimmt.
Wenn der Fenster-offen Modus über sethkrwindowopen aktiviert wurde, wird die End-Zeit durch den
entsprechenden Parameter dieses Kommandos bestimmt.
<boostactive> Boost Modus aktiviert: 0 oder 1
<boostactiveendtime> Boost End-Zeit, in Sekunden seit 1970
<adaptiveHeatingActive> adaptive Heizregelung aktiviert, 0 oder 1
<adaptiveHeatingRunning> 0 oder 1, heizt die adaptive Heizregelung aktuell
AHA-HTTP-API 13/20 © AVM GmbH
gegebenenfalls in der Zeit vor einem Komforttemperatur-Schaltpunkt aktiv
<holidayactive> befindet sich der HKR aktuell in einem Urlaubszeitraum, 0 oder 1
<summeractive> befindet sich der HKR aktuell im „Heizung aus“ Zeitraum, 0 oder 1
<lock>0/1 - Tastensperre über UI/API ein nein/ja(leer bei unbekannt oder Fehler), Achtung die Tastensperre wird
automatisch bei <summeractive>==1 oder <holidayactive>==1 aktiviert
<devicelock>0/1 - Tastensperre direkt am Gerät ein nein/ja(leer bei unbekannt oder Fehler)
<nextchange>nächste Temperaturänderung
<endperiod>timestamp in Sekunden seit 1970, 0 bei unbekannt
<tchange>Zieltemperatur, Wertebereich siehe tsoll(255/0xff ist unbekannt/undefiniert)
</nextchange>
<errorcode>Fehlercodes die der HKR liefert (bspw. wenn es bei der Installation des HKRs Problem gab):
0: kein Fehler
1: Keine Adaptierung möglich. Gerät korrekt am Heizkörper montiert?
2: Ventilhub zu kurz oder Batterieleistung zu schwach. Ventilstößel per Hand mehrmals öffnen und schließen oder
neue Batterien einsetzen.
3: Keine Ventilbewegung möglich. Ventilstößel frei?
4: Die Installation wird gerade vorbereitet.
5: Der Heizkörperregler ist im Installationsmodus und kann auf das Heizungsventil montiert werden.
6: Der Heizkörperregler passt sich nun an den Hub des Heizungsventils an.



bei Gruppe
<groupinfo>
<masterdeviceid>interne id des Master/Chef-Schalters, 0 bei "keiner gesetzt"
<members>interne ids der Gruppenmitglieder, kommasepariert
Hinweis: bei Fehlern oder unbekannten Werten sind die betreffenden Elemente leer. Beispiel für unbekannte Temperatur:
<celsius></celsius>


EXAMPLE:
<devicelist version="1" fwversion="7.57">
  <device identifier="09995 1000500" id="26" functionbitmask="320" fwversion="05.08" manufacturer="AVM" productname="FRITZ!DECT 301">
    <present>1</present>
    <txbusy>0</txbusy>
    <name>1 - Emelet - Háló</name>
    <battery>20</battery>
    <batterylow>0</batterylow>
    <temperature>
      <celsius>235</celsius>
      <offset>0</offset>
    </temperature>
    <hkr>
      <tist>47</tist>
      <tsoll>32</tsoll>
      <absenk>32</absenk>
      <komfort>40</komfort>
      <lock>0</lock>
      <devicelock>0</devicelock>
      <errorcode>0</errorcode>
      <windowopenactiv>0</windowopenactiv>
      <windowopenactiveendtime>0</windowopenactiveendtime>
      <boostactive>0</boostactive>
      <boostactiveendtime>0</boostactiveendtime>
      <batterylow>0</batterylow>
      <battery>20</battery>
      <nextchange>
        <endperiod>1697169600</endperiod>
        <tchange>40</tchange>
      </nextchange>
      <summeractive>0</summeractive>
      <holidayactive>0</holidayactive>
      <adaptiveHeatingActive>1</adaptiveHeatingActive>
      <adaptiveHeatingRunning>0</adaptiveHeatingRunning>
    </hkr>
  </device>
  <device.....

 */
@SqlTable(name = "DEVICE")
case class Device(
                   // attributes
                   identifier: String,
                   functionbitmask: Int,
                   fwversion: String,
                   manufacturer: String,
                   productname: String,

                   // device
                   present: Boolean,
                   txbusy: Boolean,
                   name: String,
                   battery: Int,
                   batterylow: Boolean,
                   celsius: Int,
                   offset: Int,

                   // hkr
                   tist: Int,
                   tsoll: Int,
                   absenk: Int,
                   komfort: Int,
                   // SQL cannot have column as lock... -> @SqlColumn(colType="VARCHAR(25)", colName="lock_") lock: Boolean,
                   devicelock: Boolean,
                   errorcode: Int,
                   windowopenactiv: Boolean,
                   windowopenactiveendtime: Int,
                   boostactive: Boolean,
                   boostactiveendtime: Int,
                   endperiod: Long,
                   tchange: Int,
                   summeractive: Boolean,
                   holidayactive: Boolean,
                   adaptiveHeatingActive: Boolean,
                   adaptiveHeatingRunning: Boolean) {
  override def toString: String = classOf[Device].classToString(this)
}

object Smart extends App {

  println("Smart start")

  private val fritzSettings = new FritzSettings()
  val deviceListXml = new FritzBoxSmartHome(fritzSettings).deviceListInfo
  val db = new Database(fritzSettings.dbHostAndPort, fritzSettings.dbDatabaseName, fritzSettings.dbUserName, fritzSettings.dbPassword)
  db.insert(classOf[Device], deviceListXml.child /*ren*/ .map(deviceXmlToDevice))

  println("Smart DONE!")

  private def deviceXmlToDevice(node: Node): Device = {
    val deviceClass = classOf[Device]
    val paramValuesFromXml = deviceClass.firstConstructorParameters.map(constructorParam => FritzBoxSmartHome.stringToType(node, constructorParam.getName, constructorParam.getType))
    val device = deviceClass.createNewWithParams(paramValuesFromXml: _*)
    device
  }


}




