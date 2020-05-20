package eu.eyan.pvtools

import java.awt.Color
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

import eu.eyan.log.Log
import eu.eyan.log.LogWindow
import eu.eyan.util.awt.clipboard.ClipboardPlus
import eu.eyan.util.registry.RegistryPlus
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JScrollPane

object PVTools extends App {
  Log.activateInfoLevel
  LogWindow.redirectSystemOutAndErrToLogWindow

  val panel = new PVToolsContent().panel

  private val frame: JFrame =
    new JFrame()
      .title(PVToolsContent.TITLE)
      .onCloseHide
      .iconFromChar('I', Color.ORANGE)
      .addToSystemTray()
      .withComponent(new JScrollPane(panel))
      .menuItem("File", "Exit", System.exit(0))
      .menuItem("Debug", "Open log window", LogWindow.show(panel))
      .menuItem("Debug", "Copy logs to clipboard", ClipboardPlus.copyToClipboard(LogWindow.getAllLogs))
      .menuItem("Debug", "Clear registry values", RegistryPlus.clear(PVToolsContent.TITLE))
      .menuItem("Help", "Write email", writeEmail())
      .menuItem("Help", "About", alert("This is not an official tool, no responsibilities are taken. Use it at your own risk."))
      .packAndSetVisible
      .center

  private def writeEmail() =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=Photo%20and%20video%20import&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))

  private def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

}
