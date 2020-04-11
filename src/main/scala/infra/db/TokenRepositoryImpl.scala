package infra.db

import java.util.UUID
import java.time.{Clock, Instant}
import java.sql.{ResultSet, Timestamp}

import scala.util.Try
import scala.util.Random

import neko.jdbc.ConnectionIO
import neko.jdbc.query._

import application.entity.Token
import application.entity.User.UserId
import application.repository.TokenRepository

class TokenRepositoryImpl(
    clock: Clock,
    applicationSecret: String
) extends TokenRepository {

  import TokenRepositoryImpl._

  override def createToken(userId: UserId): Token = {
    val rnd = new Random
    rnd.setSeed(clock.instant().toEpochMilli + applicationSecret.## + applicationSecret.##)

    val tsLen  = ts.length
    val length = 64

    Token(List.fill(length)(ts(rnd.nextInt(tsLen))).mkString)
  }

  override def saveToken(userId: UserId, token: Token): ConnectionIO[Nothing, Unit] = ConnectionIO.right {
    conn =>
      val query =
        """insert into tokens(token, user_id, expires_at) values (?, ?, ?);"""
      val pstmt = conn.prepareStatement(query)
      pstmt.setString(1, token.value)
      pstmt.setString(2, userId.value)
      pstmt.setTimestamp(3, Timestamp.from(clock.instant().plusSeconds(60 * 60 * 24)))
      pstmt.executeUpdate()
  }

  override def deleteToken(token: Token): ConnectionIO[Nothing, Boolean] = ConnectionIO.right { conn =>
    val query = "delete from tokens where token = ?;"
    val pstmt = conn.prepareStatement(query)
    pstmt.setString(1, token.value)
    val rows: Int = pstmt.executeUpdate()
    if (rows > 1) throw new RuntimeException
    if (rows == 1) true else false
  }

  override def fetchUserIdByToken(token: Token): ConnectionIO[Nothing, Option[UserId]] = ConnectionIO.right { conn =>
    val query = "select user_id from tokens where token = ?;"
    val pstmt = conn.prepareStatement(query)
    pstmt.setString(1, token.value)
    val mapping: ResultSet => UserId = row => UserId(UUID.fromString(row.getString("user_id")))
    val resultSet = pstmt.executeQuery()
    select(resultSet, mapping)
  }

}

object TokenRepositoryImpl {

  private val ts: Array[Char] = (('A' to 'Z').toList :::
    ('a' to 'z').toList :::
    ('0' to '9').toList :::
    List('-', '.', '_', '~', '+', '/')).toArray

}
