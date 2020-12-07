package eu.eyan

import java.awt.{Color, Desktop}
import java.net.{URI, URLEncoder}

import eu.eyan.duplicates.{DeleteDuplicates, DeleteDuplicatesDirs}
import eu.eyan.log.{Log, LogWindow}
import eu.eyan.pvtools.PVToolsContent
import eu.eyan.util.awt.clipboard.ClipboardPlus
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JTabbedPanePlus
import javax.swing.{JFrame, JOptionPane, JScrollPane}


object AndrisTools extends App {
  //jcef to java: https://medium.com/@daniel.bischoff/integrating-chromium-as-a-web-renderer-in-a-java-application-with-jcef-72f67a677db6
  /*
<html>
<video id="vid" src="file:///C:/Users/NemAdmin/Desktop/K%C3%A9pek_Asztal/20200503%20091652%20VID_.mp4" width=100%  height=100% controls>
Dies Video kann in Ihrem Browser nicht wiedergegeben werden.<br>
Eine Download-Version steht unter <a href="URL">Link-Addresse</a> zum Abruf bereit.
</video>
<script>
var vid=document.getElementById('vid');

document.body.onkeypress = function(e){
  console.log(e);
  if(e.which == 32){
    // stops default behaviour of space bar. Stop page scrolling down
    e.preventDefault();
    if (vid.paused) vid.play(); else vid.pause();
  }
}

function skip(value) {
  var video = document.getElementById("vid");
  video.currentTime += value;
}
</script>
</html>
   */
  Log.activateInfoLevel
  LogWindow.redirectSystemOutAndErrToLogWindow

  val TITLE = "AndrisTools"

  val tabbedPane = new JTabbedPanePlus().rememberValueInRegistry("AndrisToolsSelectedTab")
  tabbedPane.addTab(PVToolsContent.TITLE, new JScrollPane(new PVToolsContent().panel))
  tabbedPane.addTab(DeleteDuplicates.TITLE, new JScrollPane(new DeleteDuplicates().panel))
  tabbedPane.addTab(DeleteDuplicatesDirs.TITLE, new JScrollPane(new DeleteDuplicatesDirs().panel))

  new JFrame()
    .title(TITLE)
    .onCloseHide
    .iconFromChar('A', Color.GREEN.darker.darker)
    .addToSystemTray()
    .withComponent(tabbedPane)
    .menuItem("File", "Exit", System.exit(0))
    .menuItem("Debug", "Open log window", LogWindow.show(tabbedPane))
    .menuItem("Debug", "Copy logs to clipboard", ClipboardPlus.copyToClipboard(LogWindow.getAllLogs))
    //.menuItem("Debug", "Clear registry values", RegistryPlus.clear(TITLE))
    .menuItem("Help", "Write email", writeEmail())
    .menuItem("Help", "About", JOptionPane.showMessageDialog(null, "This is not an official tool, no responsibilities are taken. Use it at your own risk."))
    .packAndSetVisible
    .center

  private def writeEmail(): Unit =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=AndrisTools&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))
}