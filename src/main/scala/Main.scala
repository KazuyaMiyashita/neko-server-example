
import neko.server._
import neko.server.http._
import neko.jdbc.{DBPool, DefaultConnectionIORunner}

import java.time.Clock
import java.sql.{DriverManager, Connection}
import scala.util.Try

import application.repository.{TokenRepository, UserRepository}
import application.usecase.{
  CreateUser,
  EditUserInfo,
  FetchUserIdByToken,
  Login,
  Logout
}
import infra.db.{TokenRepositoryImpl, UserRepositoryImpl}
import controller.{ControllerComponent, Routing, AuthController, UserController}

object Main extends App {

  val config = Config.fromEnv()
  val clock  = Clock.systemUTC()
  val dbPool: DBPool = new DBPool {
    Class.forName("com.mysql.cj.jdbc.Driver")
    override def getConnection(): Try[Connection] = Try {
      DriverManager.getConnection(
        config.db.url,
        config.db.user,
        config.db.password
      )
    }
  }
  val connectionIORunner = new DefaultConnectionIORunner(dbPool)

  val tokenRepository: TokenRepository     = new TokenRepositoryImpl(clock, config.applicationSecret)
  val userRepository: UserRepository       = new UserRepositoryImpl(clock, config.applicationSecret)

  val createUser         = new CreateUser(userRepository, connectionIORunner)
  val editUserInfo       = new EditUserInfo(userRepository, connectionIORunner)
  val fetchUserIdByToken = new FetchUserIdByToken(tokenRepository, connectionIORunner)
  val login              = new Login(userRepository, tokenRepository, connectionIORunner)
  val logout             = new Logout(tokenRepository, connectionIORunner)

  val controllerConponent: ControllerComponent = ControllerComponent.create(config.server.origin)
  val authController                           = new AuthController(fetchUserIdByToken, login, logout, controllerConponent)
  val userController                           = new UserController(fetchUserIdByToken, createUser, editUserInfo, controllerConponent)

  val application: HttpApplication   = new Routing(userController, authController, controllerConponent)
  val requestHandler: RequestHandler = new HttpRequestHandler(application)

  val serverSocketHandler = new ServerSocketHandler(requestHandler, config.server.port)

  serverSocketHandler.start()

  println("press enter to terminate")
  io.StdIn.readLine()
  println("closing...")
  serverSocketHandler.terminate()

}
