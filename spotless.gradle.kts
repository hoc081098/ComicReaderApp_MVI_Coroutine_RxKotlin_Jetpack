import com.diffplug.gradle.spotless.SpotlessExtension

apply(plugin = "com.diffplug.spotless")

configure<SpotlessExtension> {
  kotlin {
    target("**/*.kt")
    ktlint(versions.ktLint)
      .setUseExperimental(true)
      .userData(
        mapOf(
          "continuation_indent_size" to "4",
          "indent_size" to "2",
          "ij_kotlin_imports_layout" to "*",
          "end_of_line" to "lf",
          "charset" to "utf-8",
          "disabled_rules" to "experimental:package-name,experimental:trailing-comma",
        )
      )

    trimTrailingWhitespace()
    indentWithSpaces()
    endWithNewline()
  }

  format("xml") {
    target("**/res/**/*.xml")

    trimTrailingWhitespace()
    indentWithSpaces()
    endWithNewline()
  }

  kotlinGradle {
    target("**/*.gradle.kts", "*.gradle.kts")

    ktlint(versions.ktLint)
      .setUseExperimental(true)
      .userData(
        mapOf(
          "continuation_indent_size" to "4",
          "indent_size" to "2",
          "ij_kotlin_imports_layout" to "*",
          "end_of_line" to "lf",
          "charset" to "utf-8",
          "disabled_rules" to "no-wildcard-imports",
        )
      )

    trimTrailingWhitespace()
    indentWithSpaces()
    endWithNewline()
  }
}
