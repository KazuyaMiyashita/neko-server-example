package infra.db

import java.util.UUID
import java.time.Clock
import java.sql.{ResultSet, Timestamp}
import java.sql.SQLIntegrityConstraintViolationException

import javax.crypto.{SecretKey, SecretKeyFactory}
import javax.crypto.spec.PBEKeySpec
import java.util.Base64

import scala.util.Try

import neko.jdbc.ConnectionIO
import neko.jdbc.query._

import application.repository.UserRepository
import application.entity.{User, Auth, Email, RawPassword, HashedPassword}
import application.entity.User.{UserId, UserName}

import com.mysql.cj.exceptions.MysqlErrorNumbers

class UserRepositoryImpl(
    clock: Clock,
    applicationSecret: String
) extends UserRepository {

  import UserRepositoryImpl._

  def insertUserIO(user: User): ConnectionIO[Nothing, Unit] = ConnectionIO.right { conn =>
    val query =
      """insert into users(id, name, created_at) values (?, ?, ?);"""
    val stmt = conn.prepareStatement(query)
    stmt.setString(1, user.id.value)
    stmt.setString(2, user.name.value)
    stmt.setTimestamp(3, Timestamp.from(user.createdAt))
    stmt.executeUpdate()
    ()
  }

  def insertAuthIO(auth: Auth): ConnectionIO[UserRepository.SaveNewUserError, Unit] = {
    ConnectionIO
      .right { conn =>
        val query = "insert into auths(email, hashed_password, user_id) values (?, ?, ?);"
        val pstmt = conn.prepareStatement(query)
        pstmt.setString(1, auth.email.value)
        pstmt.setString(2, auth.hashedPassword.value)
        pstmt.setString(3, auth.userId.value)
        pstmt.executeUpdate()
        ()
      }
      .recover {
        case e: SQLIntegrityConstraintViolationException if e.getErrorCode == MysqlErrorNumbers.ER_DUP_ENTRY =>
          UserRepository.SaveNewUserError.DuplicateEmail(e)
      }
  }

  override def saveNewUser(
      userName: UserName,
      email: Email,
      rawPassword: RawPassword
  ): ConnectionIO[UserRepository.SaveNewUserError, User] = {
    val user           = User(UserId(UUID.randomUUID()), userName, clock.instant())
    val hashedPassword = createHashedPassword(rawPassword)
    val auth           = Auth(email, hashedPassword, user.id)
    val io: ConnectionIO[UserRepository.SaveNewUserError, User] = for {
      _ <- insertUserIO(user)
      _ <- insertAuthIO(auth)
    } yield user
    io
  }

  override def createHashedPassword(rawPassword: RawPassword): HashedPassword = {
    val keySpec = new PBEKeySpec(
      rawPassword.value.toCharArray,
      applicationSecret.getBytes,
      /* iterationCount = */ 10000,
      /* keyLength = */ 512 /* bytes */
    )
    val secretKey: SecretKey = secretKeyFactory.generateSecret(keySpec)
    val value                = Base64.getEncoder.encodeToString(secretKey.getEncoded)
    HashedPassword(value)
  }

  override def fetchUserIdBy(email: Email, rawPassword: RawPassword): ConnectionIO[Nothing, Option[UserId]] = {
    val hashedPassword = createHashedPassword(rawPassword)
    ConnectionIO.right { conn =>
      val query                        = "select user_id from auths where email = ? and hashed_password = ?;"
      val mapping: ResultSet => UserId = row => UserId(UUID.fromString(row.getString("user_id")))
      val pstmt                        = conn.prepareStatement(query)
      pstmt.setString(1, email.value)
      pstmt.setString(2, hashedPassword.value)
      val resultSet = pstmt.executeQuery()
      select(resultSet, mapping)
    }
  }

  override def fetchBy(userId: UserId): ConnectionIO[Nothing, Option[User]] = ConnectionIO.right { conn =>
    val query = """select * from users where id = ?;"""
    val mapping: ResultSet => User = row =>
      User(
        id = UserId(UUID.fromString(row.getString("id"))),
        name = UserName(row.getString("name")),
        createdAt = row.getTimestamp("created_at").toInstant
      )
    val pstmt = conn.prepareStatement(query)
    pstmt.setString(1, userId.value)
    val resultSet = pstmt.executeQuery()
    select(resultSet, mapping)
  }

  override def updateUserName(userId: UserId, newUserName: UserName): ConnectionIO[Nothing, Unit] = ConnectionIO.right {
    conn =>
      val query = "update users set name = ? where id = ?;"
      val pstmt = conn.prepareStatement(query)
      pstmt.setString(1, newUserName.value)
      pstmt.setString(2, userId.value)
      val rows = pstmt.executeUpdate()
      if (rows != 1) throw new RuntimeException
      ()
  }

}

object UserRepositoryImpl {

  val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

}
