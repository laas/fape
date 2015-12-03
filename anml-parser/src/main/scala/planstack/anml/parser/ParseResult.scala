package planstack.anml.parser

class ParseResult(val blocks:List[AnmlBlock]) {

  override def toString = blocks.mkString("\n")
}
