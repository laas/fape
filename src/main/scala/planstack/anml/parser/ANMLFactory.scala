package planstack.anml.parser

import AnmlParser._
import java.io.FileReader
import planstack.anml.ANMLException

object ANMLFactory {

  def parseAnmlFromFile(file:String) : ParseResult = {
    parseAll(anml, new FileReader(file)) match {
      case Success(res, _) => new ParseResult(res)
      case err => throw new ANMLException("Unable to parse ANML:\n" + err.toString)
    }
  }

  def parseAnmlString(anmlString:String) : ParseResult = {
    parseAll(anml, anmlString) match {
      case Success(res, _) => new ParseResult(res)
      case err => throw new ANMLException("Unable to parse ANML:\n" + err.toString)
    }
  }
}
