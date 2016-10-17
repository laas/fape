package fr.laas.fape.anml.parser

import fr.laas.fape.anml.ANMLException
import AnmlParser._

import scala.util.matching.Regex.Match

object ANMLFactory {

//  val MultiLineCommentRegExp = """/\*(?:.|[\n\r])*?\*/""".r
//  val SingleLineCommentRegExp = """(//[^\n\r]*)[\n\r]""".r
  val commentRegEx = """(/\*(?:.|[\n\r])*?\*/)|(//[^\n\r]*)[\n\r]""".r

  def withoutComments(anmlString :String) = {
    // given a match, this function produces is a string with the same layout (spaces for chars
    // and newlines for new lines)
    val replace = (m :Match) => {
      var i=m.start
      var replacement = ""
      while(i < m.end) {
        if(anmlString.charAt(i) == '\n')
          replacement += "\n"
        else
          replacement += " "
        i += 1
      }
      replacement
    }

    commentRegEx.replaceAllIn(anmlString, replace)
  }

  def lines(in:String) = in.replaceAll("""//[^\n]*""", "")

  def parseAnmlFromFiles(files: List[String]) = {
    val anmlString =
      files.foldLeft("")((acc,curFile) => {
        val reader = scala.io.Source.fromFile(curFile)
        val fileContent = reader.mkString
        reader.close()
        acc+"\n"+fileContent
      })
    parseAnmlString(anmlString)
  }

  def parseAnmlFromFile(file:String) : ParseResult = {
    val reader = scala.io.Source.fromFile(file)
    val anmlString = reader.mkString
    reader.close()

    parseAnmlString(anmlString)
  }

  def parseAnmlString(anmlString:String) : ParseResult = {
    val commentFree = withoutComments(anmlString+"\n")

    parseAll(anml, commentFree) match {
      case Success(res, _) => new ParseResult(res)
      case err => throw new ANMLException("Unable to parse ANML:\n" + err.toString)
    }
  }
}
