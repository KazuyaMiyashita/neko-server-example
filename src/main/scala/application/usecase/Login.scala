package application.usecase

import scala.util.{Success, Failure}

import neko.jdbc.ConnectionIORunner

import application.entity.{Email, Token, RawPassword}
import application.repository.{UserRepository, TokenRepository}

class Login(
    userRepository: UserRepository,
    tokenRepositoty: TokenRepository,
    connectionIORunner: ConnectionIORunner
) {

  def execute(request: Login.Request): Either[Login.Error, Token] = {
    for {
      t <- request.validate
      (email, rawPassword) = (t._1, t._2)
      userId <- connectionIORunner.runReadOnly(userRepository.fetchUserIdBy(email, rawPassword)) match {
        case Failure(e)                   => Left(Login.Error.Unknown(e))
        case Success(Right(Some(userId))) => Right(userId)
        case _                            => Left(Login.Error.UserNotExist)
      }
      token = tokenRepositoty.createToken(userId)
      _ <- connectionIORunner.runReadOnly(tokenRepositoty.saveToken(userId, token)) match {
        case Failure(e) => Left(Login.Error.Unknown(e))
        case Success(_) => Right(())
      }
    } yield token
  }

}

object Login {
  case class Request(
      email: String,
      rawPassword: String
  ) {
    def validate: Either[Error.ValidateError, (Email, RawPassword)] = {
      for {
        e <- Email.of(email).left.map {
          case Email.Error.WrongFormat => Error.EmailWrongFormat
        }
        rp <- RawPassword.of(rawPassword).left.map {
          case RawPassword.Error.TooShort => Error.RawPasswordTooShort
        }
      } yield (e, rp)
    }
  }

  sealed trait Error
  object Error {
    sealed trait ValidateError       extends Error
    case object EmailWrongFormat     extends ValidateError
    case object RawPasswordTooShort  extends ValidateError
    case object UserNotExist         extends Error
    case class Unknown(e: Throwable) extends Error
  }
}
