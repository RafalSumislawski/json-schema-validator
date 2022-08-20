package io.sumislawski.jsonvs

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import cats.syntax.all._
import fs2.io.file.Files
import io.circe._
import io.circe.literal._
import io.circe.parser._
import io.sumislawski.jsonvs.core.SchemaValidationService
import io.sumislawski.jsonvs.infrastructure.filestorage.LocalFileSystemSchemaStorage
import io.sumislawski.jsonvs.infrastructure.httpapi.SchemaValidationRoutes
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpApp, Request}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

class JsonSchemaValidationServiceSpec extends AsyncFunSuite with AsyncIOSpec with Matchers {

  private def jsonSchemaValidationService(): Resource[IO, HttpApp[IO]] = for {
    storageDirectory <- Files[IO].tempDirectory
    storage <- Resource.eval(LocalFileSystemSchemaStorage[IO](storageDirectory))
    service <- Resource.eval(SchemaValidationService(storage))
  } yield {
    new SchemaValidationRoutes[IO](service)
      .routes.orNotFound
  }

  test("Responding to a request to upload a schema") {
    jsonSchemaValidationService().use { jsvs =>
      for {
        response <- jsvs.run(Request(method = POST, uri = uri"/schema/config-schema").withEntity(TestData.configSchema))
        _ = response.status shouldEqual Created
        _ <- response.as[Json].asserting(_ shouldEqual json"""{"action": "uploadSchema", "id": "config-schema", "status": "success"}""")
      } yield ()
    }
  }

  test("Returning correct schema ID in response to a request to upload a schema") {
    jsonSchemaValidationService().use { jsvs =>
      val schemaName = "schemaName"
      for {
        response <- jsvs.run(Request(method = POST, uri = uri"/schema" / schemaName).withEntity(TestData.configSchema))
        _ = response.status shouldEqual Created
        _ <- response.as[Json].asserting(_ shouldEqual json"""{"action": "uploadSchema", "id": $schemaName, "status": "success"}""")
      } yield ()
    }
  }

  test("Rejecting a request to upload a schema with an already used ID") {
    jsonSchemaValidationService().use { jsvs =>
      val schemaName = "schemaName"
      for {
        response1 <- jsvs.run(Request(method = POST, uri = uri"/schema" / schemaName).withEntity(TestData.configSchema))
        response2 <- jsvs.run(Request(method = POST, uri = uri"/schema" / schemaName).withEntity(TestData.configSchema))
        _ = response2.status shouldEqual Conflict
        expectedMessage = s"Schema [$schemaName] already exists."
        _ <- response2.as[Json].asserting(_ shouldEqual json"""{"action": "uploadSchema", "id": $schemaName, "status": "error", "message": $expectedMessage}""")
      } yield ()
    }
  }

  test("Rejecting a request to upload a schema which is not a valid JSON") {
    jsonSchemaValidationService().use { jsvs =>
      for {
        response <- jsvs.run(Request(method = POST, uri = uri"/schema/config-schema"))
        _ = response.status shouldEqual BadRequest
        _ <- response.as[Json].asserting(_ shouldEqual json"""{"action": "uploadSchema", "id": "config-schema", "status": "error", "message": "Invalid JSON schema"}""")
      } yield ()
    }
  }

  test("Downloading a previously uploaded schema") {
    jsonSchemaValidationService().use { jsvs =>
      val schemaName = "schemaName"
      for {
        _ <- jsvs.run(Request(method = POST, uri = uri"/schema" / schemaName).withEntity(TestData.configSchema))
        response2 <- jsvs.run(Request(method = GET, uri = uri"/schema" / schemaName))
        _ = response2.status shouldEqual Ok
        expectedSchema <- parse(TestData.configSchema).liftTo[IO]
        actualSchema <- response2.as[Json]
        _ = actualSchema shouldEqual expectedSchema
      } yield ()
    }
  }

  test("Trying to download a nonexistent schema") {
    jsonSchemaValidationService().use { jsvs =>
      for {
        response <- jsvs.run(Request(method = GET, uri = uri"/schema/thisSchemaDoesntExist"))
        _ = response.status shouldEqual NotFound
        _ <- response.as[Json].asserting(_ shouldEqual json"""{"action": "downloadSchema", "id": "thisSchemaDoesntExist", "status": "error", "message": "Schema [thisSchemaDoesntExist] doesn't exist"}""")
      } yield ()
    }
  }

  test("Validating a valid JSON") {
    jsonSchemaValidationService().use { jsvs =>
      for {
        _ <- jsvs.run(Request(method = POST, uri = uri"/schema/config-schema").withEntity(TestData.configSchema))
        response <- jsvs.run(Request(method = POST, uri = uri"/validate/config-schema").withEntity(TestData.validConfig))
        _ = response.status shouldEqual Ok
        _ <- response.as[Json].asserting(_ shouldEqual json"""{"action": "validateDocument", "id": "config-schema", "status": "success"}""")
      } yield ()
    }
  }

  test("Validating an invalid JSON") {
    jsonSchemaValidationService().use { jsvs =>
      for {
        _ <- jsvs.run(Request(method = POST, uri = uri"/schema/config-schema").withEntity(TestData.configSchema))
        response <- jsvs.run(Request(method = POST, uri = uri"/validate/config-schema").withEntity(TestData.invalidConfig))
        _ = response.status shouldEqual UnprocessableEntity // The response code for this case was not specified in the requirements
        _ <- response.as[Json].asserting(_ shouldEqual json"""{"action": "validateDocument", "id": "config-schema", "status": "error", "message": "object has missing required properties ([\"destination\"])"}""")
      } yield ()
    }
  }

  test("Attempting to validate against a nonexistent schema") {
    jsonSchemaValidationService().use { jsvs =>
      for {
        response <- jsvs.run(Request(method = POST, uri = uri"/validate/thisSchemaDoesntExist").withEntity(TestData.validConfig))
        _ = response.status shouldEqual NotFound
        _ <- response.as[Json].asserting(_ shouldEqual json"""{"action": "validateDocument", "id": "thisSchemaDoesntExist", "status": "error", "message": "Schema [thisSchemaDoesntExist] doesn't exist"}""")
      } yield ()
    }
  }
}
