package fr.laas.fape.planning

object Instances {

  val satisfiables = List(
    """
      |type X;
      |instance X x1, x2, x3;
      |
      |constant X v1;
      |constant X v2;
      |constant X v3;
      |
      |v1 != v2;
      |v2 != v3;
      |v3 != v1;
    """.stripMargin
  )

  val unsatisfiables = List(
    """
      |type X;
      |instance X x1, x2;
      |
      |constant X v1;
      |constant X v2;
      |constant X v3;
      |
      |v1 != v2;
      |v2 != v3;
      |v3 != v1;
    """.stripMargin,
    """
      |type X;
      |instance X x1, x2;
      |
      |constant X v1;
      |constant X v2;
      |
      |v1 == v2;
      |v2 != x1;
      |v1 != x2;
    """.stripMargin
  )

}
