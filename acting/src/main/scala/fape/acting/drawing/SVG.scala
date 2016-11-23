package fape.acting.drawing

import java.io.FileWriter

import fape.core.planning.states.{Printer, State}
import planstack.anml.model.concrete.ActionStatus

import scala.xml.{NodeSeq, Node, Attribute}
import scala.collection.JavaConverters._

abstract class Action(name: String, start: Int, minDur: Int, uncertainDur: Int, failedDur: Int, successDur: Int) {

  def toXml =
    <tr>
      <th>{name}</th>
      <td>{successDur}</td>
      <td>{failedDur}</td>
      <td>{uncertainDur}</td>
      <td>{minDur}</td>
      <td>{start}</td>
    </tr>

  def getStart = start
  def getName = name
}

case class Pending(name: String, start: Int, minDur: Int, uncertainDur: Int)
  extends Action(name, start, minDur, uncertainDur, 0, 0)

case class Failed(name: String, start: Int, failedDur: Int)
  extends Action(name, start, 0, 0, failedDur, 0)

case class Successful(name: String, start: Int, successDur: Int)
  extends Action(name, start, 0, 0, 0, successDur)


class SvgRect(x: Float, y:Float, width: Float, height: Float, clazz: String, attr: List[Attribute] = Nil) {
  implicit def ts(i:Int) : String = i.toString
  implicit def ts(f:Float) : String = f.toString

  def draw(c: Canvas): Node = {
    <rect x={c.xProj(x)} y={c.yProj(y)} width={width*c.xRatio} height={c.h} class={clazz}/>
  }
}

class SvgLine(x1:Float, y1:Float, x2:Float, y2:Float, clazz:String) {
  implicit def ts(i:Int) : String = i.toString
  implicit def ts(f:Float) : String = f.toString

  def draw(c: Canvas) : Node = {
    <line x1={c.xProj(x1)} y1={c.yProj(y1)} x2={c.xProj(x2)} y2={c.yProj(y2)} class={clazz}/>
  }
}

class Canvas(xOffset: Int, origWidth: Int, finalWidth: Int, yOffset: Int, numLines: Int, finalHeight: Int) {
  val xRatio : Float = finalWidth / origWidth
  val H : Float = finalHeight / (numLines + 0.5f)
  val h = H * 0.66f
  val space = H * 0.33f

  def xProj(x: Float) = xOffset + x * xRatio
  def yProj(y: Float) = yOffset +space + y * H
}

object SVG extends App {
  implicit def ts(i:Int) : String = i.toString
  implicit def ts(f:Float) : String = f.toString

  val acts = List(
    Pending("GoTo(operator2, wl2)", 1, 8, 2),
    Pending("GoTo(PR3, stock_table)", 1, 8, 2),
    Pending("GoTo(PR2, stock_table)", 1, 8, 2),
    //Pending("ProcessSurface(operator2, PR3, as2)", 10, 81, 0),
    Pending("Clean(operator2, as2)", 10, 19, 3),
    Pending("Pick(PR3, glue, stock_table)", 10, 5, 2)
  )


  def print(acts: Iterable[Action], time: Int, fileName: String) {

    val max = acts.foldLeft(0) {
      case (x, Pending(name, start, min, dur)) =>
        if(x > start+min+dur) x
        else start+min+dur
      case (x, Failed(_, start, dur)) =>
        if(x > start +dur) x
        else start+dur
      case (x, Successful(_, start, dur)) =>
        if(x > start+dur) x
        else start+dur
    }

    println(max)
    val c = new Canvas(300, max, 1000, 0, acts.size, 800)
    val fontSize =
      if(c.h < 10 ) c.H.toInt.toString+"px"
      else if(c.h >16) "16px"
      else ((c.h+c.H)/2).toInt.toString + "px"

    println(c.h)
    println(c.H)
    println(fontSize)

    def toSvg(act: Action, num: Int) : NodeSeq = {
      def nameNode(name: String, line:Int) =
        <text x="295" y={(c.H*(num+0.5)+c.space).toString} class="action-name" style="text-anchor: end" font-size={fontSize}>{name}</text>

      act match {
        case Pending(name, start, min, uncertain) =>
          List(
            new SvgRect(start, num, min, 10, "pending").draw(c),
            new SvgRect(start + min + 0.2f, num, uncertain - 0.2f, 10, "uncertain").draw(c),
            nameNode(name, num)
          )

        case Successful(name, start, successDur) => List(
          new SvgRect(start, num, successDur, 10, "successful").draw(c),
          nameNode(name, num)
        )

        case Failed(name, start, failedDur) => List(
          new SvgRect(start, num, failedDur, 10, "failed").draw(c),
          nameNode(name, num)
        )
      }
    }

    val svg =
      <svg xmlns="http://www.w3.org/2000/svg">
        <style type="text/css" >
          <![CDATA[

          rect.pending { fill:   #000000; }
          rect.uncertain { fill:   #bbbbbb; }
          rect.successful { fill: #00AA00; }
          rect.failed { fill: #AA0000; }
          line.grid { stroke: #bbb; }
          line.current-time { stroke: #0000AA; }
          text.time-label { fill: #BBBBBB; }
          text.action-name { fill: #555555; }

        ]]>
        </style>
        { // time grid: Below
        (0 to max+5).filter(_ % 5 == 0).map(x => List(
          new SvgLine(x, -0.5f, x, acts.size, "grid").draw(c),
          <text x={c.xProj(x)} y={(c.H*(acts.size+0.8)+c.space).toString} class="time-label" style="text-anchor: middle" font-size={fontSize}>{x.toString}</text>
        ))
        }

        { //Actions
        acts.zipWithIndex.map(p => toSvg(p._1, p._2))
        }

        {
        new SvgLine(time, -0.5f, time, acts.size, "current-time").draw(c)
        }

      </svg>

    val f = new FileWriter(fileName)
    f.write(svg.toString())
    f.close()
  }

  def printSvgToFile(st: State, filename:String): Unit = {
    val acts = for(a <- st.getAllActions.asScala) yield {
      val start = st.getEarliestStartTime(a.start)
      val earliestEnd = st.getEarliestStartTime(a.end)
      val name = Printer.action(st, a)
      a.status match {
        case ActionStatus.EXECUTED => Successful(name, start, earliestEnd-start)
        case ActionStatus.FAILED => Failed(name, start, earliestEnd-start)
        case ActionStatus.PENDING | ActionStatus.EXECUTING =>
          st.getDurationBounds(a) match {
            case Some((min, max)) => Pending(name, start, min, max-min)
            case None => Pending(name, start, earliestEnd-start, 0)
          }
      }
    }
    val filtered = acts.filterNot(a =>
      a.getName.contains("Get") || a.getName.contains("Process") || a.getName.contains("GlueAttach"))

    val sorted = filtered.sortWith((a,b) => {
      def priority(a:Action) = {
        var base = 0
        if(a.getName.contains("operator2") && !a.getName.contains("HandOver")) base += 20000
        else if(a.getName.contains("operator1") && !a.getName.contains("HandOver")) base += 55000
        else if(a.getName.contains("PR2")) base += 30000
        else if(a.getName.contains("PR3")) base += 10000
        base + a.getStart
      }
      priority(a) < priority(b)
    })
    print(sorted, st.getEarliestStartTime(st.pb.earliestExecution), filename)
  }

}

object MainSvg extends App {
  val acts = List(
    Successful("AAAAAAAAAAAa", 10, 50),
    Failed("BBBBBBBBBBBBB", 20, 15),
    Pending("CCCCCCCCCCCC", 60, 20, 8))
    .filterNot(a =>
      a.getName.contains("Get") || a.getName.contains("Process") || a.getName.contains("GlueAttach"))
    .sortWith((a,b) => {
    def priority(a:Action) = {
      var base = 0
      if(a.getName.contains("operator2") && !a.getName.contains("HandOver")) base += 20000
      else if(a.getName.contains("operator1") && !a.getName.contains("HandOver")) base += 15000
      else if(a.getName.contains("PR2")) base += 30000
      else if(a.getName.contains("PR3")) base += 10000
      base + a.getStart
    }
    priority(a) < priority(b)
  })

  SVG.print(acts, 40, "out/test.svg")
}
