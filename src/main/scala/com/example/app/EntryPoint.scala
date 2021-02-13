package com.example.app

import org.apache.http.HttpHost
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest
import org.elasticsearch.client.{ RequestOptions, RestClient }
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import org.elasticsearch.client.RestHighLevelClient
import scala.util.Try

/**
  * This demonstrates a logged exception I'm seeing with the Elasticsearch RestHighLevel client
  * when auth caching is not disabled on the underyling HTTP client. Nothing is generated for the
  * class using the native-image-agent even when transport is exercised like this while running it.
  *
  * I've also tried adding manual relection config for BasicScheme with no luck.
  */
object EntryPoint {
  val host = "localhost"
  val port = 9200

  def main(args: Array[String]): Unit = {
    for (i <- 0 to Try(args(0).toInt).getOrElse(10)) {
      demoCallWithLoggedException()
      Thread.sleep(1000L)
      demoCallWithoutLoggedException()
      Thread.sleep(1000L)
    }
  }

  /**
    * Exception logged when auth caching is enabled
    *
    * Feb 13, 2021 7:35:15 AM org.apache.http.impl.client.BasicAuthCache get
    * WARNING: Unexpected I/O error while de-serializing auth scheme
    * java.io.InvalidClassException: org.apache.http.impl.auth.BasicScheme; no valid constructor
    */
  def demoCallWithLoggedException() = {
    System.out.println("\nAuth cache enabled; call will log exception when run as a native image: ")
    val builder = RestClient.builder(new HttpHost(host, port, "http"))
    val client = new RestHighLevelClient(builder)
    val response = client.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT)
    client.close()
    System.out.println("Auth cache enabled response: " + response.getClusterName)
  }

  /**
    * No logged exception when auth caching is disabled
    */
  def demoCallWithoutLoggedException() = {
    System.out.println("\nAuth cache disabled; call will NOT log exception: ")
    val builder = RestClient.builder(new HttpHost(host, port, "http"))
    val client = new RestHighLevelClient(builder.setHttpClientConfigCallback(NoAuthCache()))
    val response = client.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT)
    client.close()
    System.out.println("Auth cache disabled response: " + response.getClusterName)
  }
}

/**
  *  Prevents logged exception when used to build HTTP client
  */
case class NoAuthCache() extends HttpClientConfigCallback {
  override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
    httpClientBuilder.disableAuthCaching()
  }
}
