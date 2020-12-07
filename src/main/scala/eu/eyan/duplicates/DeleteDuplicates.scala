package eu.eyan.duplicates

import java.io.File

import eu.eyan.util.awt.MultiField
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.AbstractButtonPlus.AbstractButtonImplicit
import eu.eyan.util.swing.JButtonPlus.JButtonImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JTextAreaPlus.JTextAreaImplicit
import eu.eyan.util.swing.JTextFieldPlus.JTextFieldPlusImplicit
import eu.eyan.util.swing.SwingPlus.invokeLater
import eu.eyan.util.swing.{JPanelWithFrameLayout, SwingPlus}
import javax.swing.JFrame
import rx.lang.scala.subjects.BehaviorSubject

import scala.collection.mutable
import eu.eyan.util.scala.LongPlus.LongImplicit
import javax.swing.SwingUtilities

class TextFieldWithCheckBox(size: Int) extends JPanelWithFrameLayout {
  withSeparators
  newColumnFPG
  val textField = addTextField("", size)
  val include = newColumn.addCheckBox("Include")
  val checkBox = newColumn.addCheckBox("Allow to delete")

  def onEdited(action: => Unit) = {
    textField.onKeyReleased(action)
    include.onAction(action)
    checkBox.onAction(action)
    this
  }
}
//FIXME store in registry on delete line
class MultiFieldJTextFieldWithCheckbox(columnName: String, columns: Int = 0) extends MultiField[(String, Boolean, Boolean), TextFieldWithCheckBox](columnName) {
  def clearAllowDelete() = invokeLater(editors.foreach(_.editor.checkBox.setSelected(false)))

  protected def createEditor(fieldEdited: TextFieldWithCheckBox => Unit) = {
    val editor = new TextFieldWithCheckBox(columns)
    editor.onEdited(fieldEdited(editor))
  }

  protected def getValue(editor: TextFieldWithCheckBox) = {
    val text = editor.textField.getText.trim
    if (text.isEmpty) None else Some((text, editor.checkBox.isSelected, editor.include.isSelected))
  }

  protected def stringToValue(string: String): (String, Boolean, Boolean) = (string.substring(2), sToB(string.head), sToB(string.tail.head))
  protected def valueToString(value: (String, Boolean, Boolean)): String = bToS(value._2)+bToS(value._3)+ value._1

  protected def setValueInEditor(editor: TextFieldWithCheckBox)(value: (String, Boolean, Boolean)): Unit = {
    editor.textField.setText(value._1)
    editor.checkBox.setSelected(value._2)
    editor.include.setSelected(value._3)
  }

  private def bToS(b:Boolean)= if(b) "T" else "F"
  private def sToB(s:Char)= s == 'T'

  def getTexts = getValues.map(_._1)
}

object DeleteDuplicates {
  val TITLE = "Handle duplicates"
}
class DeleteDuplicates {
  val panel = new JPanelWithFrameLayout().withBorders.withSeparators
  panel.newColumn.newColumnFPG
  val N = "\n"
  panel.addSeparatorWithTitle("Directories to search")
  val dirs = new MultiFieldJTextFieldWithCheckbox("dirsToSearch", 50)
  dirs.setValues(List(("",false, false)))
  dirs.rememberValueInRegistry("dirsToSearch")
  
  panel.newRow.add(dirs)

  val findPanel = new JPanelWithFrameLayout().withSeparators
  val minSize = findPanel.addLabelFluent("Min bytes:").newColumn.addTextField("", 7).rememberValueInRegistry("minSize")
  val findPanelButton = findPanel.newColumnFPG.addButton("Find duplicates").onAction(findDuplicates(false))
  val fullHash = findPanel.newColumn.addCheckBox("Full hash")
  panel.newRow.add(findPanel)


  val deletePanel = new JPanelWithFrameLayout().withSeparators

  val allowDeleteButton = deletePanel.newRow.addCheckBox("Enable").tooltipText("Enable \"Delete duplicates\" button")
  val deleteDuplicatesButton = deletePanel.newColumnFPG.addButton("Delete duplicates").onAction(findDuplicates(true))
  panel.newRow.add(deletePanel)

  val progress = panel.newRow.addProgressBar(0, 1, "%dMB")

  val logs = panel.newRowFPGForTextArea.span(1).addTextArea()

  val hashFastCache = mutable.Map[File, String]()

  val isInProgress = BehaviorSubject(false)
  val isAllowDeleteSelected = BehaviorSubject(false)

  allowDeleteButton.onActionPerformed(isAllowDeleteSelected.onNext(allowDeleteButton.isSelected))
  isAllowDeleteSelected.subscribe(allowDeleteButton.setSelected _)

  isInProgress.subscribe(pr => invokeLater (findPanelButton.setEnabled(!pr)))
  isInProgress.combineLatest(isAllowDeleteSelected).subscribe(prAll => invokeLater (deleteDuplicatesButton.setEnabled(prAll._2 && !prAll._1)))

  dirs.onChanged(() => Option(SwingUtilities.windowForComponent(panel)).map(_.asInstanceOf[JFrame]).foreach(_.resizeAndBack))

  private def findDuplicates(withDelete: Boolean) = SwingPlus.runInWorker ({
    invokeLater (logs.setText(""))
    if(withDelete) dirs.clearAllowDelete()
    isInProgress.onNext(true)
    isAllowDeleteSelected.onNext(false)

    val deletablePaths = dirs.getValues.filter(_._2).map(_._1)
    val dirPaths = dirs.getValues.filter(_._3).map(_._1).toStream
    val minSizeBytes = try{minSize.getText.toLong} catch {case _:Throwable => -1L}
    val filesStreams = dirPaths.flatMap(_.asDir.fileTreeWithItself.filter(_.isFile)).filter(_.length > minSizeBytes)
    var fileCt = 0L
    progress.setFormat("%d files")
    progress.setMaximum(Int.MaxValue)
    val files = filesStreams.collect { case file =>
      fileCt += 1
      if (fileCt % 100 == 0) progress.valueChanged(fileCt.toInt)
      file
    }.toList.distinct
    progress.valueChanged(fileCt.toInt)

    val fileGroupsByLength = files.groupBy(_.length())

    val filesSingle = fileGroupsByLength.filter(_._2.size == 1).values.flatten.toList

    val filesMultiGroups = fileGroupsByLength.filter(_._2.size != 1).values.toList.sortWith((l1, l2) => l1.head.length < l2.head.length)

    val filesSum = files.map(_.length).sum
    val filesSingleSum = filesSingle.map(_.length).sum
    val filesMultiGroupsSum = filesMultiGroups.flatten.map(_.length).sum

    logs.appendLater("Files found: " + files.size + ", Size: " + filesSum.toSize + N)
    logs.appendLater("Single: " + filesSingle.size + ", Size single: " + filesSingleSum.toSize +  N)
    logs.appendLater("Multi : " + filesMultiGroups.flatten.size + " (" + filesMultiGroups.size + " groups), Size multi:" + filesMultiGroupsSum.toSize +  N)

    progress.setFormat("")
    progress.valueChanged(0)

    var sum = 0L

    def updateProgress(readBytes: Long) = {
      sum += readBytes
      progress.setString(sum.toSize)
    }

    var remainingFilesCt = 0
    var deleteCt = 0

    val dirsContainingDuplicates = mutable.Set[File]()

    filesMultiGroups.foreach { group =>
      val hashGroups = group.groupBy(f =>
        if (fullHash.isSelected) f.hashFull(updateProgress)
        else if (hashFastCache.contains(f)) hashFastCache({updateProgress(f.length); f})
        else hashFastCache.getOrElseUpdate(f, f.hashFast(updateProgress))
      )


      hashGroups.foreach { hashFiles =>
        if (hashFiles._2.size < 2) logs.appendLater(".")
        else {
          logs.appendLater(N + N)
          val hash = hashFiles._1
          logs.appendLater(hash + N)

          val files = hashFiles._2.sortBy(f => f.getName.length)
          files.map(_.getParentFile).foreach(dirsContainingDuplicates.add)
          //          logs.append(files.map(file => ("File", file.length, file.getAbsolutePath)).mkString(N)+N)

          val filesToDeleteCandidates = files.filter(fileToDelete => deletablePaths.exists(fileToDelete.getAbsolutePath.contains(_)))
          //          logs.append(filesToDeleteCandidates.map(file => ("filesToDeleteCandidates", file.length, file.getAbsolutePath)).mkString(N)+N)
          val filesToKeepCandidates = files.filter(file => !filesToDeleteCandidates.contains(file))
          //          logs.append(filesToKeepCandidates.map(file => ("filesToKeepCandidates", file.length, file.getAbsolutePath)).mkString(N)+N)

          val filesToKeep = if (filesToKeepCandidates.isEmpty) List(filesToDeleteCandidates.head) else filesToKeepCandidates
          remainingFilesCt += filesToKeep.size
          val filesToDelete = if (filesToKeepCandidates.isEmpty) filesToDeleteCandidates.tail else filesToDeleteCandidates


          logs.appendLater(filesToKeep.map(file => ("Keep", file.length.toSize, file.getAbsolutePath)).mkString(N) + N)
          logs.appendLater(filesToDelete.map(file => ("Delete", file.length.toSize, file.getAbsolutePath)).mkString(N) + N)
          deleteCt += filesToDelete.size

          if (withDelete) filesToDelete.foreach(_.delete)
        }
      }
    }

    logs.appendLater(N + N + "dirsContainingDuplicates: " + N + dirsContainingDuplicates.toList.sorted.mkString(N))
    logs.appendLater(N + N + "DeleteCt: " + deleteCt)
    logs.appendLater(N + N + "Expected: " + (filesSingle.size + remainingFilesCt))


  },isInProgress.onNext(false))
}