package eu.eyan.search

import java.awt.{ Color, Desktop }
import java.io.File
import java.net.{ URI, URLEncoder }
import java.nio.file.Files
import java.nio.file.attribute.FileTime

import eu.eyan.log.{ Log, LogWindow }
import eu.eyan.util.awt.clipboard.ClipboardPlus
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.java.time.InstantPlus.InstantImplicit
import eu.eyan.util.registry.RegistryPlus
import eu.eyan.util.scala.collection.TraversableOncePlus.TraversableOnceImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.JButtonPlus.JButtonImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JLabelPlus.JLabelImplicit
import eu.eyan.util.swing.JPanelWithFrameLayout
import eu.eyan.util.swing.JTextFieldPlus.JTextFieldPlusImplicit
import eu.eyan.util.swing.JToggleButtonPlus.JToggleButtonImplicit
import javax.swing.{ JFrame, JOptionPane, JScrollPane }
import java.io.FileWriter
import eu.eyan.util.io.FileLineStartOffsetReader
import eu.eyan.util.scala.TryCatch

object SearchTool extends App {
  Log.activateInfoLevel

  val TITLE = "Search texts in files"

  val panel = new JPanelWithFrameLayout().withBorders.withSeparators
    .newColumn.newColumnFPG

  panel.newRow("f:p").addLabel("Search dir: ").cursor_HAND_CURSOR
  val searchPathTextField = panel.nextColumn.addTextField("D:\\Collection1", 30).rememberValueInRegistry("searchPathTextField")

  panel.newRow("f:p").addLabel("Search texts: ").cursor_HAND_CURSOR
  val searchTextsTextFields = panel.nextColumn.addTextFieldMulti("searchTextsTextFields", 30, List()).rememberValueInRegistry("searchTextsTextFields")
  searchTextsTextFields.onChanged(() => frame.resizeAndBack)

  panel.newRow.addButton("Search").onAction_disableEnable(doSearch)

  panel.newRow.addLabel("Progress:")
  val progressBar = panel.nextColumn.addProgressBar()

  val logArea = panel.newRow("f:100px:g").span.addTextArea()

  val frame =
    new JFrame()
      .title(TITLE)
      .onCloseHide
      .iconFromChar('S', Color.RED)
      .addToSystemTray()
      .withComponent(new JScrollPane(panel))
      .menuItem("File", "Exit", System.exit(0))
      .menuItem("Debug", "Open log window", LogWindow.show(panel))
      .menuItem("Debug", "Copy logs to clipboard", ClipboardPlus.copyToClipboard(LogWindow.getAllLogs))
      .menuItem("Debug", "Clear registry values", RegistryPlus.clear(TITLE))
      .menuItem("Help", "About", alert("This is not an official tool, no responsibilities are taken. Use it at your own risk."))
      .packAndSetVisible
      .center

  def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  def doSearch = {
    val searchStrings = searchTextsTextFields.getValues
    val files = searchPathTextField.getText.asDir.fileTreeWithItself.filter(_.isFile).toList
    var byteCounter = 0L
    var foundCounter = 0L
    progressBar.valueChanged(0)
    val sumBytes = files.map(_.length).sum

    def now = System.currentTimeMillis
    val start = now
    val wr = new FileWriter("D:\\Collection1\\found.txt")

    files.foreach(file => eu.eyan.util.scala.TryCatchThrowable({
      val lines = file.lines()
      val foundLines = lines.filter(_.containsAny(searchStrings)).toList
      foundCounter += foundLines.size
      byteCounter += file.length
      progressBar.valueChanged((byteCounter * 100 / sumBytes).toInt)

      val seconds = ((now - start) / 1000)
      val mb = byteCounter / 1024 / 1024
      val mbps = if(seconds==0) 1L else mb / seconds
      println(byteCounter + " " + mbps + "MBps, found: " + foundCounter)
      wr.write(foundLines.mkString("\r\n") + "\r\n")
    }, (e: Throwable) => {
      println("Error " + file + ", " + e.getMessage)
    }))
    wr.close
  }

}