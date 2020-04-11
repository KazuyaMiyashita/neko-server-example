package application.repository

import scala.util.Try

import neko.jdbc.ConnectionIO

import application.entity.Token
import application.entity.User.UserId

trait TokenRepository {

  def createToken(userId: UserId): Token
  def saveToken(userId: UserId, token: Token): ConnectionIO[Nothing, Unit]
  def deleteToken(token: Token): ConnectionIO[Nothing, Boolean]
  def fetchUserIdByToken(token: Token): ConnectionIO[Nothing, Option[UserId]]

}
