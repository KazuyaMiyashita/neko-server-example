val dottyVersion = "0.23.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dotty-simple",
    version := "0.1.0",

    scalaVersion := dottyVersion,

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",

    resolvers += "Github Repository" at "https://kazuyamiyashita.github.io/neko-framework/mvn-repo/",
    libraryDependencies += "com.kazmiy" %% "neko-server" % "1.0.0",
    libraryDependencies += "com.kazmiy" %% "neko-jdbc" % "1.0.0",
    libraryDependencies += "com.kazmiy" %% "neko-json" % "1.0.0",
    
  )
