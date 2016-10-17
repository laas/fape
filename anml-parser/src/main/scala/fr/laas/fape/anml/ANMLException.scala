package fr.laas.fape.anml

class ANMLException(message:String, cause: Option[Throwable]) extends RuntimeException(message, cause.orNull) {
  def this(msg:String) = this(msg, None)
  def this(msg:String, cause:Throwable) = this(msg, Some(cause))
}

class VariableNotFound(varName:String) extends ANMLException(s"Unable to find variable: $varName")

class UnrecognizedExpression(e: parser.Expr, cause: Option[Throwable])
  extends ANMLException(s"Could not recognize expression: ${e.asANML}  --  $e", cause)

class FunctionNotFoundInType(funcName:String, typ:String) extends ANMLException(s"Could not find function \'$funcName\' in type: \'$typ\'")