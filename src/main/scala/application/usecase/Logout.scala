package application.usecase

import scala.util.{Success, Failure}

import neko.jdbc.ConnectionIORunner

import application.entity.Token
import application.repository.TokenRepository

class Logout(
  tokenRepository: TokenRepository,
  connectionIORunner: ConnectionIORunner
) {

  def execute(token: Token): Boolean = {
    connectionIORunner.runTx(tokenRepository.deleteToken(token)) match {
      case Success(Right(true)) => true
      case _ => false
    }
  }

}
