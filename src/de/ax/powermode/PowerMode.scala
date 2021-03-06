/*
 * Copyright 2015 Baptiste Mesta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ax.powermode

import java.io.File

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.{ApplicationComponent, PersistentStateComponent, State, Storage}
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.util.xmlb.XmlSerializerUtil
import de.ax.powermode.power.management.ElementOfPowerContainerManager
import org.apache.log4j._
import org.jetbrains.annotations.Nullable

import scala.util.Try

/**
  * @author Baptiste Mesta
  */
object PowerMode {

  val logger = Logger.getLogger(classOf[PowerMode])

  @Nullable def getInstance: PowerMode = {
    try {
      ApplicationManager.getApplication.getComponent(classOf[PowerMode])
    } catch {
      case e: Exception =>
        null
    }
  }

  def obtainColorEdges(pm: PowerMode): ColorEdges = {
    import pm._
    val edges = new ColorEdges()
    edges.setAlpha(getColorAlpha)
    edges.setRedFrom(getRedFrom)
    edges.setRedTo(getRedTo)
    edges.setGreenFrom(getGreenFrom)
    edges.setGreenTo(getGreenTo)
    edges.setBlueFrom(getBlueFrom)
    edges.setBlueTo(getBlueTo)
    edges
  }
}

@State(name = "PowerModeII", storages = Array(new Storage(file = "$APP_CONFIG$/power.mode.ii.xml")))
class PowerMode extends ApplicationComponent with PersistentStateComponent[PowerMode] {
  var bamLife: Long = 1000

  var isBamEnabled: Boolean = true

  var isSoundsPlaying = false

  var soundsFolder = Option.empty[File]


  var gravityFactor: Double = 21.21

  var sparkVelocityFactor: Double = 4.36

  var sparkSize = 3

  var sparksEnabled = true

  var frameRate: Int = 30

  def setFrameRate(f: Int) {
    frameRate = f
  }

  def getFrameRate() = frameRate

  var maxFlameSize = 100

  var maxFlameLife = 2000

  var heatupTime = 10000

  var lastKeys: List[Long] = List.empty[Long]

  var keyStrokesPerMinute = 300
  var heatupFactor = 1.0
  var sparkLife = 3000
  var sparkCount = 10
  var shakeRange = 4
  var flamesEnabled: Boolean = true
  var maybeElementOfPowerContainerManager = Option.empty[ElementOfPowerContainerManager]
  private var enabled: Boolean = true
  private var shakeEnabled: Boolean = true

  def increaseHeatup {
    val ct = System.currentTimeMillis()
    lastKeys = ct :: lastKeys.filter(_ >= ct - heatupTime)
  }

  def valueFactor = heatupFactor + ((1 - heatupFactor) * timeFactor)

  def timeFactor: Double = {
    val tf = Try {
      if (heatupTime < 1000) {
        1
      } else {
        val d = heatupTime.toDouble / (60000.0 / keyStrokesPerMinute)
        math.min(lastKeys.size, d) / d
      }
    }.getOrElse(0.0)
    tf
  }

  def reduceHeatup: Unit = {
    val ct = System.currentTimeMillis()
    lastKeys = lastKeys.filter(_ >= ct - heatupTime)
  }

  var caretAction: Boolean = true

  def initComponent {
    val editorFactory = EditorFactory.getInstance
    maybeElementOfPowerContainerManager = Some(new ElementOfPowerContainerManager)
    maybeElementOfPowerContainerManager.foreach(editorFactory.addEditorFactoryListener(_, new Disposable() {
      def dispose {
      }
    }))
    val editorActionManager = EditorActionManager.getInstance
    EditorFactory.getInstance().getEventMulticaster.addCaretListener(new MyCaretListener())
    maybeElementOfPowerContainerManager.map(cm =>
      editorActionManager.getTypedAction.setupRawHandler
      (new MyTypedActionHandler(editorActionManager.getTypedAction.getRawHandler)))
  }

  def disposeComponent {
    maybeElementOfPowerContainerManager.foreach(_.dispose)
  }

  def getComponentName: String = {
    return "PowerModeII"
  }

  def getState: PowerMode = {
    return this
  }

  def loadState(state: PowerMode) {
    XmlSerializerUtil.copyBean(state, this)
  }

  def isEnabled: Boolean = {
    enabled
  }

  def setEnabled(enabled: Boolean) {
    this.enabled = enabled
  }

  def isShakeEnabled: Boolean = {
    shakeEnabled
  }

  def setShakeEnabled(shakeEnabled: Boolean) {
    this.shakeEnabled = shakeEnabled
  }

  def getSparkCount = sparkCount

  def setSparkCount(sparkCount: Int) {
    this.sparkCount = sparkCount
  }

  def getSparkLife = sparkLife

  def setSparkLife(sparkRange: Int) {
    this.sparkLife = sparkRange
  }

  def getShakeRange = shakeRange

  def setShakeRange(shakeRange: Int) {
    this.shakeRange = shakeRange
  }

  def getHeatup = (heatupFactor * 100).toInt

  def setHeatup(heatup: Int) {
    this.heatupFactor = heatup / 100.0
  }

  def getHeatupTime = heatupTime

  def setHeatupTime(heatupTime: Int) {
    this.heatupTime = math.max(0, heatupTime)
  }

  def getFlameLife: Int = {
    return maxFlameLife
  }

  def setFlameLife(flameLife: Int): Unit = {
    maxFlameLife = flameLife
  }

  def getmaxFlameSize: Int = {
    return maxFlameSize
  }

  def setmaxFlameSize(maxFlameSize: Int): Unit = {
    this.maxFlameSize = maxFlameSize
  }

  def getKeyStrokesPerMinute: Int = {
    return keyStrokesPerMinute
  }

  def setKeyStrokesPerMinute(keyStrokesPerMinute: Int) {
    this.keyStrokesPerMinute = keyStrokesPerMinute
  }

  def isFlamesEnabled: Boolean = {
    return flamesEnabled
  }

  def setFlamesEnabled(flamesEnabled: Boolean) {
    this.flamesEnabled = flamesEnabled
  }

  def isSparksEnabled: Boolean = {
    return sparksEnabled
  }

  def setSparksEnabled(sparksEnabled: Boolean) {
    this.sparksEnabled = sparksEnabled
  }

  def getSparkSize: Int = {
    return sparkSize
  }

  def setSparkSize(sparkSize: Int) {
    this.sparkSize = sparkSize
  }

  def getGravityFactor(): Double = gravityFactor

  def setGravityFactor(f: Double) {
    gravityFactor = f
  }

  def getSparkVelocityFactor(): Double = sparkVelocityFactor

  def setSparkVelocityFactor(f: Double) {
    sparkVelocityFactor = f
  }

  var redFrom: Int = 200

  def getRedFrom: Int = {
    redFrom
  }

  var redTo: Int = 255

  def getRedTo: Int = {
    return redTo
  }

  var greenTo: Int = 255

  def getGreenTo: Int = {
    return greenTo
  }

  var greenFrom: Int = 0

  def getGreenFrom: Int = {
    return greenFrom
  }

  var blueFrom: Int = 0

  def getBlueFrom: Int = {
    return blueFrom
  }

  var blueTo: Int = 103

  def getBlueTo: Int = {
    return blueTo
  }

  var colorAlpha: Int = 164

  def getColorAlpha: Int = {
    return colorAlpha
  }

  def setRedFrom(redFrom: Int) {
    if (redFrom <= redTo)
      this.redFrom = redFrom
  }

  def setRedTo(redTo: Int) {
    if (redTo >= redFrom)
      this.redTo = redTo
  }

  def setGreenFrom(gf: Int) {
    if (gf <= greenTo)
      greenFrom = gf
  }

  def setGreenTo(greenTo: Int) {
    if (greenTo >= greenFrom)
      this.greenTo = greenTo
  }

  def setBlueFrom(bf: Int) {
    if (bf <= blueTo)
      blueFrom = bf
  }

  def setBlueTo(blueTo: Int) {
    if (blueTo >= getBlueFrom)
      this.blueTo = blueTo
  }

  def setColorAlpha(alpha: Int) {
    colorAlpha = alpha
  }

  def setSoundsFolder(file: String) {
    soundsFolder = Option(new File(file))
  }

  def getSoundsFolder = soundsFolder.map(_.getAbsolutePath).getOrElse("")

  def isCaretAction: Boolean = {
    caretAction
  }

  def setIsCaretAction(isCaretAction: Boolean) {
    this.caretAction = isCaretAction
  }

  def setIsSoundsPlaying(isSoundsPlaying: Boolean) {
    this.isSoundsPlaying = isSoundsPlaying
  }

  def getIsSoundsPlaying = isSoundsPlaying

  def getBamLife = bamLife

  def setBamLife(l: Long) {
    bamLife = l
  }

  def setIsBamEnabled(b: Boolean) {
    isBamEnabled = b
  }

  def getIsBamEnabled = isBamEnabled

}