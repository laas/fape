package fape.drawing.gui

import java.awt.{BorderLayout, Dimension}

import fape.drawing.TimedCanvas
import org.apache.batik.anim.dom.SAXSVGDocumentFactory

import java.awt.event._
import java.io.StringReader
import javax.swing._

import org.apache.batik.swing._
import org.apache.batik.util.XMLResourceDescriptor

class ChartWindow(title: String) {

  val frame = new JFrame(title)
  val canvas = new JSVGCanvas()
  frame.getContentPane.add(canvas, BorderLayout.CENTER)

  private var isDisplayed = false

  frame.addWindowListener(new WindowAdapter {
    override def windowClosing(e:WindowEvent) { System.exit(0) }
  })

  def draw(chart: TimedCanvas): Unit = {

    val reader = new StringReader(chart.draw.toString())

    val parser = XMLResourceDescriptor.getXMLParserClassName
    val f:SAXSVGDocumentFactory = new SAXSVGDocumentFactory(parser)
    val svgDoc = f.createSVGDocument("file:///tmp/tmp.svg", reader)

    // Display the document.
    canvas.setSVGDocument(svgDoc)

    if(!isDisplayed) {
      frame.setSize(new Dimension(chart.totalWidth.toInt, chart.totalHeight.toInt))
      frame.setVisible(true)
      isDisplayed = true
    }
  }
}
