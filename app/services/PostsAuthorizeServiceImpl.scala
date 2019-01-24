package services

import graphql.Context
import models.errors.Unauthorized

import scala.concurrent.Future

/**
  * Provides authorizing functions.
  */
class PostsAuthorizeServiceImpl extends PostsAuthorizeService {

  /** @inheritdoc*/
  override def withPostAuthorization[T](context: Context)(callback: String => Future[T]): Future[T] =
    context.requestHeaders.get("my-id") match {
      case Some(id) => callback(id)
      case None => Future.failed(Unauthorized("You are not authorized."))
    }
}