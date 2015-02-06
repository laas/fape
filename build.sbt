name := "fape-all"

lazy val fape = Project(id = "fape-planning",
                            base = file("FAPE")) dependsOn(anml, constraints, graphs)

lazy val graphs = Project("graphs", file("planstack/graph"))

lazy val constraints = Project("constraints", file("planstack/constraints")) dependsOn(graphs)

lazy val anml = Project("anml", file("planstack/anml"))



