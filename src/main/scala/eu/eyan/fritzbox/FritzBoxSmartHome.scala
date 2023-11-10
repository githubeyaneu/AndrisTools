package eu.eyan.fritzbox

import eu.eyan.util.http.HttpPlusServer
import eu.eyan.util.java.crypto.Pbkdf2Login
import eu.eyan.util.scala.xml.XmlPlus.{StringToXml, Xml}
import eu.eyan.util.string.StringPlus.StringPlusImplicit

import scala.xml.Node

object FritzBoxSmartHome {
  def stringToType(node: Node, paramName: String, toType: Class[_]): Object = {
    val value = paramValueFromXml(node, paramName)
    (toType.getName match {
      case "int" => value.toInt
      case "long" => value.toLong
      case "boolean" => value.equals("1")
      case _ => value
    }).asInstanceOf[Object]
  }

  private def paramValueFromXml(node: Node, paramName: String): String =
    node.childText(paramName)
      .orElse(node.firstChildDeepText(paramName))
      .orElse(node.attributeText(paramName))
}

class FritzBoxSmartHome(fritzSettings: FritzSettings) {

  private val fritzBox = HttpPlusServer.https(fritzSettings.fritzboxUrl)
  private val fritzBoxLoginPage = fritzBox.page("login_sid.lua?version=2")
  private val fritzBoxDeviceListInfo = fritzBox.page("webservices/homeautoswitch.lua?switchcmd=getdevicelistinfos")

  private val challenge = fritzBoxLoginPage.post().xPathText("Challenge")
  println("challenge", challenge)

  private val response = Pbkdf2Login.calculatePbkdf2Response(challenge, fritzSettings.fritzboxPassword)
  println("response", response)

  private val sid = fritzBoxLoginPage.post("username=" + fritzSettings.fritzboxUserName + "&response=" + response).xPathText("SID")
  println("sid", sid)

  def deviceListInfo = fritzBoxDeviceListInfo.post("sid=" + sid).asXml
}
