package validators

import com.google.inject.Inject
import config.AccessConfig
import graphql.Context
import models.errors.Forbidden

import scala.concurrent.Future

/**
  * Provides functions to validate that user has administrator rights.
  */
class AdminAccessValidatorImpl @Inject()(config: AccessConfig) extends AdminAccessValidator {

  /** @inheritdoc*/
  override def withAdminAccessValidation[T](context: Context)(callback: => Future[T]): Future[T] = {
    context.requestHeaders.get("admin-access-token") match {
      case Some(value) =>
        if (config.adminAccessToken == value) callback else Future.failed(Forbidden("You dont have admin rights."))
      case None => Future.failed(Forbidden("You don't have admin rights."))
    }
  }
}