package config

import com.google.inject.Singleton

@Singleton
class AccessConfig {
  def getAdminAccessToken = "some_token"
}