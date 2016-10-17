package fr.laas.fape.gui

import scala.collection.JavaConverters._
import scala.xml.NodeSeq

object ImplicitConversions {
  implicit def ts(i:Int) : String = i.toString
  implicit def ts(f:Float) : String = f.toString
}

import ImplicitConversions._

class TimedCanvas(val lines: Iterable[ChartLine], currentTime:Option[Float] = None) {

  def this(lines: java.util.Collection[ChartLine], currentTime: Int) = this(lines.asScala, Some(currentTime.toFloat))
  def this(lines: java.util.Collection[ChartLine]) = this(lines.asScala, None)

  def totalWidth = width + labelWidth
  def totalHeight = height
  val numLines = lines.size +1 // keep one for label on time grid
  val height = 650
  val width = 650

  val maxX =
    if(lines.nonEmpty) lines.flatMap(_.elements).map(_.maxX).max
    else 10 // no lines do display

  val lineHeight : Float = height / numLines * 0.66f
  val labelHeight : Int =
    if(lineHeight > 16) 14
    else if(lineHeight < 10) 10
    else lineHeight.toInt
  val spaceBetweenLines : Float = height / numLines * 0.33f

  val labelWidth =
    if(lines.nonEmpty) lines.map(_.label.length(this)).max /2
    else 10

  def yOfLine(l: Int) : Float = (l+0.5f) * (lineHeight + spaceBetweenLines)
  def xProj(x: Float) : Float = labelWidth + x * width / maxX
  def widthProj(w: Float) : Float =
    if(width >= 0) w * width / maxX
    else 0

  def draw : NodeSeq =
    <svg xmlns="http://www.w3.org/2000/svg" viewBox={s"0 0 $totalWidth $totalHeight"}>
      <style type="text/css" >
          <![CDATA[
          rect.pending { fill:   #000000; }
          rect.uncertain { fill:   #bbbbbb; }
          rect.successful { fill: #00AA00; }
          rect.failed { fill: #AA0000; }
          rect.sv-value { fill: #BBBBBB; }
          line.grid { stroke: #bbb; }
          line.current-time { stroke: #0000AA; }
          text.time-label { fill: #BBBBBB; }
          text.action-name { fill: #555555; }
          rect.unknown { fill: #DDDDDD; }
        ]]>
        </style>
      { // time grid: Below
        (0 to maxX.toInt+5).filter(_ % 5 == 0).map(x =>
          <line x1={xProj(x)} y1={0} x2={xProj(x)} y2={yOfLine(numLines-1)} class="grid"/>
          <text x={xProj(x)} y={yOfLine(numLines-1)+lineHeight} class="time-label" style="text-anchor: middle" font-size={"12px"}>{x}</text>
        )
      }
      { lines.zipWithIndex.map { case (line, index) => line.draw(this, index) }}
      { currentTime match {
        case Some(t) => <line x1={xProj(t)} y1={0} x2={xProj(t)} y2={yOfLine(numLines-1)} class="current-time"/>
        case None =>
      }}
    </svg>
}

class ChartLine(val label:TextLabel, val elements: Iterable[CanvasElement]) {
  def this(label:TextLabel, elements: java.util.Collection[CanvasElement]) = this(label, elements.asScala)
  def this(label:TextLabel, elem: CanvasElement) = this(label, List(elem))
  def this(label:TextLabel, elem1: CanvasElement, elem2: CanvasElement) = this(label, List(elem1, elem2))


  def draw(canvas: TimedCanvas, atLine: Int): NodeSeq = {
    <g>
      { label.draw(canvas, atLine) }
      { elements.map(_.draw(canvas, atLine)) }
    </g>
  }
}

case class TextLabel(txt:String, clazz: String) {

  def length(c: TimedCanvas) : Float = txt.size * c.labelHeight

  def draw(c: TimedCanvas, l: Int): NodeSeq =
    <text x={c.labelWidth-5} y={c.yOfLine(l)+c.lineHeight/2} class={clazz} style="text-anchor: end" font-size={c.labelHeight}>{txt}</text>
}

trait CanvasElement {

  def draw(canvas: TimedCanvas, atLine: Int) : NodeSeq
  def maxX : Float
}

case class RectElem(x: Float, width: Float, clazz: String)
  extends CanvasElement
{
  override def draw(c: TimedCanvas, l: Int): NodeSeq = {
    <rect x={c.xProj(x)} y={c.yOfLine(l)} width={c.widthProj(width)} height={c.lineHeight} class={clazz}/>
  }
  override def maxX = x+width
}

case class RectTextElem(x: Float, width: Float, txt: String, clazz: String)
  extends CanvasElement
{
  override def draw(c: TimedCanvas, l: Int): NodeSeq =
    <g>
      <rect x={c.xProj(x)} y={c.yOfLine(l)} width={c.widthProj(width)} height={c.lineHeight} class={clazz}/>
      <text x={c.xProj(x+width/2f)} y={c.yOfLine(l)+c.lineHeight/2} font-size={c.labelHeight}
            style="text-anchor: middle" class={"rect-text "+clazz} >{txt}</text>
    </g>

  override def maxX: Float = x+width
}