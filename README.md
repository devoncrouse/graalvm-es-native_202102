# graalvm-es-native

This project demonstrates a logged exception I'm seeing in a native image, using the Elasticsearch RestHighLevelClient when auth caching is not disabled on the underyling HTTP client. Nothing is generated for the reported class using the native-image-agent.

I've also tried relection config manually for BasicScheme with no luck.

#### Run an Elasticsearch instance locally (optional)
If you look past the ConnectException and timeouts without it, you still see the auth cache-related error logging first. Running this on the side demonstrates that the client is still working to some extent (maybe not caching auth) despite the logged error. Note that this was used when generating the checked-in native-image-agent configuration:
```bash
docker run --network=host -e "discovery.type=single-node" -e "opendistro_security.disabled=true" amazon/opendistro-for-elasticsearch:1.12.0
```

#### Build/re-generate native-image-agent configuration
The unit test run under the agent makes several calls to exercise the transport the same way it's used at runtime:
```bash
./gradlew clean build
```

#### Build the native image binary
```bash
./gradlew build -x test nativeImage
```

#### Run the binary
Loop 10 times making calls with/without auth caching enabled:
```bash
./build/graal/graalvm-es-native
```

#### Output
One iteration of log output:
```
Auth cache enabled; call will log exception when run as a native image: 
Feb 13, 2021 8:39:07 AM org.apache.http.impl.client.BasicAuthCache get
WARNING: Unexpected error while de-serializing auth scheme
java.lang.ClassNotFoundException: org.apache.http.impl.auth.BasicScheme
	at com.oracle.svm.core.hub.ClassForNameSupport.forName(ClassForNameSupport.java:60)
	at java.lang.Class.forName(DynamicHub.java:1260)
	at java.io.ObjectInputStream.resolveClass(ObjectInputStream.java:756)
	at java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1995)
	at java.io.ObjectInputStream.readClassDesc(ObjectInputStream.java:1862)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:2169)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1679)
	at java.io.ObjectInputStream.readObject(ObjectInputStream.java:493)
	at java.io.ObjectInputStream.readObject(ObjectInputStream.java:451)
	at org.apache.http.impl.client.BasicAuthCache.get(BasicAuthCache.java:130)
	at org.apache.http.client.protocol.RequestAuthCache.process(RequestAuthCache.java:107)
	at org.apache.http.protocol.ImmutableHttpProcessor.process(ImmutableHttpProcessor.java:133)
	at org.apache.http.impl.nio.client.MainClientExec.prepareRequest(MainClientExec.java:520)
	at org.apache.http.impl.nio.client.MainClientExec.prepare(MainClientExec.java:146)
	at org.apache.http.impl.nio.client.DefaultClientExchangeHandlerImpl.start(DefaultClientExchangeHandlerImpl.java:124)
	at org.apache.http.impl.nio.client.InternalHttpAsyncClient.execute(InternalHttpAsyncClient.java:141)
	at org.elasticsearch.client.RestClient.performRequestAsync(RestClient.java:537)
	at org.elasticsearch.client.RestClient.performRequestAsyncNoCatch(RestClient.java:520)
	at org.elasticsearch.client.RestClient.performRequest(RestClient.java:232)
	at org.elasticsearch.client.RestHighLevelClient.internalPerformRequest(RestHighLevelClient.java:1765)
	at org.elasticsearch.client.RestHighLevelClient.performRequest(RestHighLevelClient.java:1735)
	at org.elasticsearch.client.RestHighLevelClient.performRequestAndParseEntity(RestHighLevelClient.java:1697)
	at org.elasticsearch.client.ClusterClient.health(ClusterClient.java:146)
	at com.example.app.EntryPoint$.demoCallWithLoggedException(EntryPoint.scala:42)
	at com.example.app.EntryPoint$.$anonfun$main$3(EntryPoint.scala:24)
	at scala.collection.immutable.Range.foreach$mVc$sp(Range.scala:158)
	at com.example.app.EntryPoint$.main(EntryPoint.scala:23)
	at com.example.app.EntryPoint.main(EntryPoint.scala)

Auth cache enabled response: docker-cluster

Auth cache disabled; call will NOT log exception: 
Auth cache disabled response: docker-cluster
```
