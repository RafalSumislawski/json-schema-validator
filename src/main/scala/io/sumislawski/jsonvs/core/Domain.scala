package io.sumislawski.jsonvs.core

case class SchemaId private(id: String) extends AnyVal {
  override def toString: String = id
}

object SchemaId {
  private val regex = raw"[a-zA-Z_0-9\-]+".r

  def apply(s: String): Either[IllegalArgumentException, SchemaId] =
    if (regex.matches(s)) Right(new SchemaId(s))
    else Left(new IllegalArgumentException(s"SchemaId [$s] contains illegal characters."))

}

case class Schema(s: String) {
  override def toString: String = s
}

case class Document(s: String) {
  override def toString: String = s
}

sealed trait ValidationResult

object ValidationResult {
  case object Valid extends ValidationResult

  case class Invalid(message: String) extends ValidationResult
}
