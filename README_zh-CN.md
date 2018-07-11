# HTTP高速下载器
[English](https://github.com/proxyee-down-org/pdown-core/blob/master/README.md)  
使用JAVA NIO(netty)编写，速度快且易定制。 
## 指南
### 创建一个下载任务  
在创建下载任务之前，需要先构造好下载的请求，以及下载的相关设置。
### 请求  
请求属性基本和HTTP协议一致，一般只需要定义一个请求URI，剩下的都使用默认值就好。
#### 请求方法
请求方法默认为GET，也可以设置为POST,PUT等等。
#### 请求头
默认的请求头参见下表
  
key | value
---|---
Host | {host}
Connection | keep-alive
User-Agent | Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.75 Safari/537.36
Accept | text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
Referer | {host}

可以自定义请求头，当与默认请求头重复时将覆盖默认请求头 
*{host}：从请求地址中解析出来的域名*
#### 请求体
可以自定义请求体，目前只支持文本格式。
### 响应
默认情况下，会发起一次请求来获得响应内容,例如判断响应是否支持HTTP 206，响应大小，文件名称。
如果已经知道响应体的大小和名称的话，可以直接创建并开始一个下载任务，可以节省一次请求去解析响应相关信息。
### 选项
下载选项用于配置下载相关的属性，具体参见下表

属性名 | 默认值 | 描述 
---|---|---
filePath | {root} | 下载目录
fileName | {name} | 文件名
totalSize | 0(B) | 下载文件的总大小
supportRange | false | 是否支持206 Accept-Ranges响应(即断点下载)
connections | 16 | 下载连接数
timeout | 30(S) | 响应超时时间，若超时会自动重连.
retryCount | 5 | 重试次数，当所有连接请求异常都超过这个值时，任务会停止下载.
autoRename | false | 是否自动重命名，当检测到下载目录有同名文件时自动重命名,否则会停止下载.
speedLimit | 0(B/S) | 下载速度限制.

## 示例
```
//通过url下载一个文件
HttpDownBootstrap.builder("http://127.0.0.1/static/test.zip")
          .build()
          .start();
//自定义请求和响应来下载一个文件
HttpDownBootstrap.builder()
          .request(new HttpRequestInfo(HttpMethod.GET, "http://127.0.0.1/static/test.zip"))
          .response(new HttpResponseInfo(2048,true))
          .build()
          .start();
//设置下载选项
HttpDownBootstrap.builder("http://127.0.0.1/static/test.zip")
          .downConfig(new HttpDownConfigInfo().setConnections(32).setAutoRename(true).setSpeedLimit(1024*1024*5L))
          .build()
          .start();
//设置下载的二级代理
HttpDownBootstrap.builder("http://127.0.0.1/static/test.zip")
          .proxyConfig(new ProxyConfig(ProxyType.HTTP,"127.0.0.1",8888))
          .build()
          .start();
//设置下载的回调
HttpDownBootstrap.builder("http://127.0.0.1/static/test.zip")
          .callback(new ConsoleHttpDownCallback())
          .build()
          .start();
```
## 编译
```
git clone git@github.com:proxyee-down-org/pdown-core.git
cd pdown-core
mvn clean package -Dmaven.test.skip=true -Pexec
```
## 运行
```
#查看帮助
java -jar pdown.jar -h
#用默认选项下载
java -jar pdown.jar "http://127.0.0.1/static/test.zip"
```

