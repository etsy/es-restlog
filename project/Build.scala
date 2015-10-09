import java.io.File

import com.typesafe.sbt.S3Plugin._
import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  lazy val restlog = Project(
    id = "es-restlog",
    base = file(".")
  ).settings(
    organization := "es-restlog",
    version := s"0.1-es${V.elasticsearchMajorVersion}",
    description := "REST request logging for Elasticsearch",
    autoScalaLibrary := false,
    crossPaths := false,
    javacOptions ++= Seq("-source", V.java, "-target", V.java)
  ).settings(
    s3Settings ++ Seq(
      credentials += Credentials(Path.userHome / ".s3credentials"),
      S3.host in S3.upload := "es-restlog.s3.amazonaws.com",
      mappings in S3.upload <<= (name, version, target) map { (name, v, out) => Seq((out / s"$name-$v.zip", s"$name-$v.zip")) },
      S3.upload <<= S3.upload dependsOn pack
    ): _*
  ).settings(
    pack <<= packTask
  ).settings(
    libraryDependencies ++= Seq(
      "org.elasticsearch" % "elasticsearch" % V.elasticsearch % "provided"
    )
  )

  lazy val pack = TaskKey[File]("pack")

  def packTask = Def.task {
    val archive = target.value / s"${name.value}-${version.value}.zip"

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
           |java.version=${V.java}
           |elasticsearch.version=${V.elasticsearch}
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

  object V {

    val java = "1.8"
    val elasticsearch = "2.0.0"

    def elasticsearchMajorVersion = elasticsearch.split('.')(0)

  }

}