import java.io.File

import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  val javaVersion = "1.8"

  lazy val esVersion = SettingKey[String]("Elasticsearch version")

  lazy val restlog = Project(
    id = "es-restlog",
    base = file(".")
  ).settings(
    organization := "es-restlog",
    version := "0.3",
    esVersion := "2.1.1",
    description := "REST request logging for Elasticsearch",
    autoScalaLibrary := false,
    crossPaths := false,
    javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion)
  ).settings(
    pack <<= packTask
  ).settings(
    libraryDependencies ++= Seq(
      "org.elasticsearch" % "elasticsearch" % esVersion.value % "provided"
    )
  )

  lazy val pack = TaskKey[File]("pack")

  def packTask = Def.task {
    val esVersionQualifier = s"es_${esVersion.value.replace('.', '_')}"
    val archive = target.value / s"${name.value}-${version.value}-$esVersionQualifier.zip"

    println(archive)

    val pluginDescriptorFile = {
      val f = File.createTempFile(name.value, "tmp")
      IO.write(f,
        s"""name=${name.value}
            |version=${version.value}
            |description=${description.value}
            |site=false
            |jvm=true
            |classname=com.etsy.elasticsearch.restlog.RestlogPlugin
            |java.version=$javaVersion
            |elasticsearch.version=${esVersion.value}
            |""".stripMargin)
      f
    }

    val jar = (packageBin in Compile).value

    IO.zip(
      Seq(jar -> jar.getName, pluginDescriptorFile -> "plugin-descriptor.properties")
        ++ update.value.matching(configurationFilter("runtime")).map(f => f -> f.getName),
      archive
    )

    pluginDescriptorFile.delete()

    archive
  }

}
