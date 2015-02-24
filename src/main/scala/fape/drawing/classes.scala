package fape.drawing




import scala.xml.{NodeSeq, Attribute}
import ImplicitConversions._

object ImplicitConversions {
  implicit def ts(i:Int) : String = i.toString
  implicit def ts(f:Float) : String = f.toString
}

class TimedCanvas(val lines: Iterable[ChartLine]) {
  val height = 800
  val width = 1000
  val labelWidth = 300
  val maxX = lines.flatMap(_.elements).map(_.maxX).max

  val lineHeight : Float = height / lines.size * 0.66f
  val spaceBetweenLines : Float = height /lines.size * 0.33f

  def yOfLine(l: Int) : Float = l * lineHeight + (l-1) * spaceBetweenLines
  def xProj(x: Float) : Float = labelWidth + x * width / maxX
  def widthProj(w: Float) : Float = w * width / maxX


  def draw : NodeSeq =
    <svg>
      { lines.zipWithIndex.map { case (line, index) => line.draw(this, index) }}
    </svg>
}

class ChartLine(val label:TextLabel, val elements: Iterable[CanvasElement]) {
  def draw(canvas: TimedCanvas, atLine: Int): NodeSeq = {
    <g>
      { label.draw(canvas, atLine) }
      { for(e <- elements) e.draw(canvas, atLine) }
    </g>
  }
}

class TextLabel(val txt:String, val clazz: String) {
  def draw(c: TimedCanvas, l: Int): NodeSeq =
    <text x="295" y={c.yOfLine(l)} class={clazz} style="text-anchor: end" font-size="12px">{txt}</text>
}

trait CanvasElement {

  def draw(canvas: TimedCanvas, atLine: Int) : NodeSeq
  def maxX : Float
}

class RectElem(x: Float, width: Float, clazz: String, attr: List[Attribute] = Nil)
  extends CanvasElement
{
  override def draw(c: TimedCanvas, l: Int): NodeSeq = {
    <rect x={c.xProj(x)} y={c.yOfLine(l)} width={c.widthProj(width)} height={c.lineHeight} class={clazz}/>
  }
  override def maxX = x+width
}