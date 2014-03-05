package planstack.anml.model

class Type(val name:String, val parent:String) {

  var methods = List[String]()

  def addMethod(methodName:String) { methods = methodName :: methods}

}
