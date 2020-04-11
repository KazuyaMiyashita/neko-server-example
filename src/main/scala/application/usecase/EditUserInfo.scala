package application.usecase

import scala.util.{Success, Failure}

import neko.jdbc.ConnectionIORunner

import application.entity.User.{UserId, UserName}
import application.repository.UserRepository

class EditUserInfo(
  userRepository: UserRepository,
  connectionIORunner: ConnectionIORunner
) {
  def execute(request: EditUserInfo.Request): Either[EditUserInfo.Error, Unit] = {
    for {
      newUserName <- request.validate
      _ <- connectionIORunner.runTx(userRepository.updateUserName(request.userId, newUserName)) match {
        case Success(_) => Right(())
        case Failure(e) => Left(EditUserInfo.Error.Unknown(e))
      }
    } yield ()
  }
}

object EditUserInfo {
  case class Request(
      userId: UserId,
      newUserName: String
  ) {
    def validate: Either[Error.ValidateError, UserName] = {
      for {
        nun <- UserName.of(newUserName).left.map {
          case UserName.Error.TooLong => Error.UserNameTooLong
        }
      } yield nun
    }
  }

  sealed trait Error
  object Error {
    sealed trait ValidateError  extends Error
    case object UserNameTooLong extends ValidateError
    case class Unknown(e: Throwable) extends Error
  }
}
