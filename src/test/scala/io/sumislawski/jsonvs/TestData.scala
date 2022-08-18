package io.sumislawski.jsonvs

object TestData {

  val configSchema =
    """
    {
      "$schema": "http://json-schema.org/draft-04/schema#",
      "type": "object",
      "properties": {
        "source": {
          "type": "string"
        },
        "destination": {
          "type": "string"
        },
        "timeout": {
          "type": "integer",
          "minimum": 0,
          "maximum": 32767
        },
        "chunks": {
          "type": "object",
          "properties": {
            "size": {
              "type": "integer"
            },
            "number": {
              "type": "integer"
            }
          },
          "required": ["size"]
        }
      },
      "required": ["source", "destination"]
    }"""

}
