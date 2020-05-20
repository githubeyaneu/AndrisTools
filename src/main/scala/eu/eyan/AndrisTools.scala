package eu.eyan

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

import scala.io.Codec
import eu.eyan.util.java.lang.IntPlus.IntImplicit
import eu.eyan.util.scala.Try
import eu.eyan.util.scala.TryCatch
import rx.lang.scala.subjects.BehaviorSubject
import eu.eyan.util.rx.lang.scala.ObservablePlus
import eu.eyan.util.rx.lang.scala.ObservablePlus.ObservableImplicitBoolean
import eu.eyan.util.swing.SwingPlus
import java.awt.Dimension
import eu.eyan.util.scala.LongPlus.LongImplicit
import eu.eyan.util.swing.panelbuilder.JPanelBuilder
import javax.swing.JPanel
import com.jgoodies.forms.layout.FormLayout
import com.jgoodies.forms.factories.CC
import javax.swing.JLabel
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicitT
import rx.lang.scala.Observable
import eu.eyan.util.rx.lang.scala.ObservablePlus.ObservableImplicitT
import eu.eyan.pvtools.FFMPegPlus._
import eu.eyan.util.rx.lang.scala.ObserverPlus.ObserverImplicit
import eu.eyan.util.swing.panelbuilder.Click
import eu.eyan.pvtools.FFMpegParam._
import eu.eyan.pvtools.FFMpegVideoParam._
import eu.eyan.util.java.lang.RuntimePlus
import eu.eyan.pvtools.PVToolsContent
import javax.swing.JTabbedPane
import eu.eyan.duplicates.DeleteDuplicates
import eu.eyan.util.swing.JTabbedPanePlus
import eu.eyan.duplicates.DeleteDuplicatesDirs


object AndrisTools extends App {
  Log.activateInfoLevel
  LogWindow.redirectSystemOutAndErrToLogWindow

  val TITLE = "AndrisTools"
  
  val tabbedPane = new JTabbedPanePlus().rememberValueInRegistry("AndrisToolsSelectedTab")
  tabbedPane.addTab(PVToolsContent.TITLE, new JScrollPane(new PVToolsContent().panel))
  tabbedPane.addTab(DeleteDuplicates.TITLE, new JScrollPane(new DeleteDuplicates().panel))
  tabbedPane.addTab(DeleteDuplicatesDirs.TITLE, new JScrollPane(new DeleteDuplicatesDirs().panel))

  private val frame: JFrame =
    new JFrame()
      .title(TITLE)
      .onCloseHide
      .iconFromChar('A', Color.GREEN.darker().darker())
      .addToSystemTray()
      .withComponent(tabbedPane)
      .menuItem("File", "Exit", System.exit(0))
      .menuItem("Debug", "Open log window", LogWindow.show(tabbedPane))
      .menuItem("Debug", "Copy logs to clipboard", ClipboardPlus.copyToClipboard(LogWindow.getAllLogs))
      //.menuItem("Debug", "Clear registry values", RegistryPlus.clear(TITLE))
      .menuItem("Help", "Write email", writeEmail())
      .menuItem("Help", "About", alert("This is not an official tool, no responsibilities are taken. Use it at your own risk."))
      .packAndSetVisible
      .center

  private def writeEmail() =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=AndrisTools&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))

  private def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)
}