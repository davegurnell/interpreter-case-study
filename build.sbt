name := "interpreter-case-study"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",    // source files are in UTF-8
  "-deprecation",          // warn about use of deprecated APIs
  "-unchecked",            // warn about unchecked type parameters
  "-feature",              // warn about misused language features
  "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
  "-Ypartial-unification", // allow the compiler to unify type constructors of different arities
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.4.0",
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")
