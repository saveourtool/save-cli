package org.cqfn.save.plugin.warn.sarif

import io.github.detekt.sarif4k.SarifSchema210
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.cqfn.save.plugin.warn.utils.Warning

import kotlin.test.Test
import kotlin.test.assertEquals

class SarifWarningAdapterTest {
    @Test
    fun test() {
        val sarif = """
            {
              "version": "2.1.0",
              "${'$'}schema": "http://json.schemastore.org/sarif-2.1.0-rtm.4",
              "runs": [
                {
                  "tool": {
                    "driver": {
                      "name": "ESLint",
                      "informationUri": "https://eslint.org",
                      "rules": [
                        {
                          "id": "no-unused-vars",
                          "shortDescription": {
                            "text": "disallow unused variables"
                          },
                          "helpUri": "https://eslint.org/docs/rules/no-unused-vars"
                        }
                      ]
                    }
                  },
                  "artifacts": [
                    {
                      "location": {
                        "uri": "file:///C:/dev/sarif/sarif-tutorials/samples/Introduction/simple-example.js"
                      }
                    }
                  ],
                  "results": [
                    {
                      "level": "error",
                      "message": {
                        "text": "'x' is assigned a value but never used."
                      },
                      "locations": [
                        {
                          "physicalLocation": {
                            "artifactLocation": {
                              "uri": "file:///C:/dev/sarif/sarif-tutorials/samples/Introduction/simple-example.js",
                              "index": 0
                            },
                            "region": {
                              "startLine": 1,
                              "startColumn": 5
                            }
                          }
                        }
                      ],
                      "ruleId": "no-unused-vars",
                      "ruleIndex": 0
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val sarifSchema210 = Json.decodeFromString<SarifSchema210>(sarif)

        val warnings = sarifSchema210.toWarnings()

        println(warnings)
        assertEquals(1, warnings.size)
        assertEquals(
            Warning("'x' is assigned a value but never used.", 1, 5, "simple-example.js"),
            warnings.first()
        )
    }
}
