package models

/**
  * The Post entity.
  *
  * @param id       an id of the post
  * @param authorId an id of the post's author
  * @param title    a title of the post
  * @param content  a content of the post
  */
case class Post(id: Option[Long] = None,
                authorId: String,
                title: String,
                content: String)