package application.entity

import application.entity.User.UserId

case class Auth(
    email: Email,
    hashedPassword: HashedPassword,
    userId: UserId
)
