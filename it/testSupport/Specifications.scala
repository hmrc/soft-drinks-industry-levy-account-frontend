package testSupport

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import testSupport.actions.ActionsBuilder
import testSupport.preConditions.PreconditionBuilder

trait Specifications extends AnyFreeSpec with ScalaFutures {
  this: TestConfiguration =>

  implicit val `given` = new PreconditionBuilder
  lazy val user = new ActionsBuilder(baseUrl)

}
