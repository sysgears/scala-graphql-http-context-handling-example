package controllers

import com.google.inject.{Inject, Singleton}
import graphql.GraphQL
import models.errors.TooComplexQueryError
import play.api.libs.json._
import play.api.mvc._
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.playJson._
import sangria.parser.QueryParser
import sangria.renderer.SchemaRenderer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class AppController @Inject()(cc: ControllerComponents,
                              graphQL: GraphQL) extends AbstractController(cc) {

  def graphiql: Action[AnyContent] = Action(Ok(views.html.graphiql()))

  def graphqlBody: Action[JsValue] = Action.async(parse.json) {
    implicit request: Request[JsValue] =>

      val extract: JsValue => (String, Option[String], Option[JsObject]) = query => (
        (query \ "query").as[String],
        (query \ "operationName").asOpt[String],
        (query \ "variables").toOption.flatMap {
          case JsString(vars) => Some(parseVariables(vars))
          case obj: JsObject => Some(obj)
          case _ => None
        }
      )

      val maybeQuery: Try[(String, Option[String], Option[JsObject])] = Try {
        request.body match {
          case arrayBody@JsArray(_) => extract(arrayBody.value(0))
          case objectBody@JsObject(_) => extract(objectBody)
          case otherType =>
            //TODO: Define custom type for this error
            throw new Error {
              s"/graphql endpoint does not support request body of type [${otherType.getClass.getSimpleName}]"
            }
        }
      }

      maybeQuery match {
        case Success((query, operationName, variables)) => executeQuery(query, variables, operationName)
        case Failure(error) => Future.successful {
          BadRequest(error.getMessage)
        }
      }
  }

  def renderSchema = Action {
    Ok(SchemaRenderer.renderSchema {
      graphQL.Schema
    })
  }

  def parseVariables(variables: String): JsObject = if (variables.trim.isEmpty || variables.trim == "null") Json.obj()
  else Json.parse(variables).as[JsObject]

  def executeQuery(query: String, variables: Option[JsObject] = None, operation: Option[String] = None): Future[Result] = QueryParser.parse(query) match {
    case Success(queryAst: Document) => Executor.execute(
      schema = graphQL.Schema,
      queryAst = queryAst,
      variables = variables.getOrElse(Json.obj()),
      exceptionHandler = graphQL.exceptionHandler,
      queryReducers = List(
        QueryReducer.rejectMaxDepth[Unit](graphQL.maxQueryDepth),
        QueryReducer.rejectComplexQueries[Unit](graphQL.maxQueryComplexity, (_, _) => TooComplexQueryError())
      )
    ).map(Ok(_)).recover {
      case error: QueryAnalysisError ⇒ BadRequest(error.resolveError)
      case error: ErrorWithResolver ⇒ InternalServerError(error.resolveError)
    }
    case Failure(ex) => Future(BadRequest(s"${ex.getMessage}"))
  }
}