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
    """.stripMargin,
    """
      |type X;
      |variable X sv;
      |instance X x1, x2;
      |
      |[start] sv := x1;
    """.stripMargin,
    """
      |type X;
      |variable X sv;
      |instance X x1, x2;
      |
      |[start] sv := x1;
      |[10]  sv == x1;
    """.stripMargin,
    """
      |type X;
      |variable X sv;
      |instance X x1, x2;
      |
      |[start] sv := x1;
    """.stripMargin,
    """
      |type X;
      |variable X sv;
      |instance X x1, x2;
      |
      |[start] sv := x1;
      |[10,20] sv == x1 :-> x2;
      |[all] contains sv == x1;
    """.stripMargin,
    """
      |type X;
      |variable X sv;
      |instance X x1, x2;
      |
      |[start] sv := x1;
      |[10,20] sv == x1 :-> x2;
      |[all] contains sv == x2;
    """.stripMargin,
    """
      |type X;
      |variable X sv;
      |instance X x1, x2;
      |
      |[start] sv := x1;
      |[10,20] sv == x1 :-> x2;
      |[all] contains sv == x1;
      |[all] contains sv == x2;
    """.stripMargin,
    """
      |end = start + 30;
      |
    """.stripMargin,
    """
      |type X;
      |type Y;
      |fluent X sv(Y y);
      |instance X x1, x2;
      |instance Y y1, y2, y3;
      |
      |action A(X x, Y y) {
      |  [start] sv(y) := x;
      |};
    """.stripMargin,
    """
      |type X;
      |type Y;
      |fluent X sv(Y y);
      |fluent boolean sv2;
      |instance X x1, x2;
      |instance Y y1, y2, y3;
      |
      |action A(X x, Y y) {
      |  [start] sv(y2) := x;
      |  [end] sv2 := true;
      |};
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
    """.stripMargin,
    """
      |end = start + 30;
      |end > start + 40;
      |
    """.stripMargin,
    """
      |type X;
      |variable X sv;
      |instance X x1, x2;
      |
      |[start] sv := x1;
      |[10,20] sv == x1 :-> x2;
      |[19,30] sv == x2;
    """.stripMargin,
    """
      |type X;
      |variable X sv;
      |instance X x1, x2;
      |
      |[start] sv := x1;
      |[all] contains sv == x2;
    """.stripMargin
  )

}
