package application.usecase

import scala.util.{Success, Failure}

import neko.jdbc.ConnectionIORunner

import application.entity.User.UserId
import application.entity.Token
import application.repository.TokenRepository

class FetchUserIdByToken(
    tokenRepository: TokenRepository,
    connectionIORunner: ConnectionIORunner
) {

  def execute(token: Token): Option[UserId] = {
    connectionIORunner.runTx(tokenRepository.fetchUserIdByToken(token)) match {
      case Success(Right(Some(user))) => Some(user)
      case _ => None
    }
  }

}
