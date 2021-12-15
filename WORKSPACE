workspace(name = "restlog")

load("//:version.bzl", "elasticsearch_version")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "6a3bd6a2cf02e96fd5041500bafa3a35731d4981" # master as of 9 Mar 2021
RULES_JVM_EXTERNAL_SHA = "42712b494220629e3dbd3d0edee1fb675fb41f507695ca80284b53c291a76299"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    name = "jvm_deps",
    artifacts = [
        "org.elasticsearch:elasticsearch:%s" % elasticsearch_version,
        "org.elasticsearch.test:framework:%s" % elasticsearch_version,
        "org.elasticsearch.client:elasticsearch-rest-high-level-client:%s" % elasticsearch_version,
        'org.apache.logging.log4j:log4j-api:jar:2.16.0',
        'org.apache.logging.log4j:log4j-slf4j-impl:jar:2.16.0',
        'org.apache.logging.log4j:log4j-core:2.16.0',        
        'com.google.guava:guava:29.0-jre',
        'org.mockito:mockito-core:3.5.13',
        'org.mockito:mockito-inline:3.5.13',
        'junit:junit:4.12',
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
    maven_install_json = "@//:jvm_deps_install.json",
    # fails if maven_install_json was not regenerated after updating artifacts
    fail_if_repin_required = True,
)


load("@jvm_deps//:defs.bzl", pinned_jvm_deps_install = "pinned_maven_install")

pinned_jvm_deps_install()

