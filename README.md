# MiniChatApp

**写在前面：代码结构部分参考了网络教程以及开源项目，由于本人真的是从零开始学app开发，可能存在一些bug敬请见谅**

开发工具：AndroidStudio+Copilot
项目版本：API26+GroovyDSL+gradle=8.5

主要依赖：
1. Hilt: 用于依赖注入
2. Retrofit + OkHttp: 用于网络请求
3. Room: 用于本地数据存储
4. Coil: 用于图片加载

项目结构：



## TodoList:

1. 基本功能已经实现，注册界面应该有注册成功提示并延迟返回
2. 需要默认注册admin用户
3. 记得给Claude更新项目文件

## 登录&注册模块

1. 登录界面：包含用户名和密码输入框，以及登录和注册按钮
   <img src="E:\SLife\Codes\MiniChatAPP\assets\image-20241125170652798.png" alt="image-20241125170652798"  />

2. 注册界面：包含用户名、密码和确认密码，以及一些输入验证：

   用户名至少3个字符

   密码至少6个字符

   确认两次密码是否一致
   ![image-20241125170722071](E:\SLife\Codes\MiniChatAPP\assets\image-20241125170722071.png)

3. 改进了加载状态显示，添加了错误信息提示，改进了视觉样式，有简单的输入验证

4. 本地存储功能：

   使用 Room 数据库存储用户信息 -> DatabaseModule.kt

   实现用户注册和登录功能

   使用 ViewModel 处理业务逻辑

   使用协程处理异步操作

   使用 Hilt 进行依赖注入

   添加了admin作为默认用户

   密码输入隐藏（使用OutlinedTextField的visualTransformation）

## 聊天列表界面

以后可能添加聊天列表+联系人功能

1. ChatScreen.kt实现消息界面
2. 显示用户名和消息发送时间

![image-20241125181830516](E:\SLife\Codes\MiniChatAPP\assets\image-20241125181830516.png)

## 消息同步功能

WebSocket连接框架

### WebSocket服务器端

1. WebSocket连接管理：使用ConcurrentHashMap管理所有活动的WebSocket会话，通过URL参数识别用户身份
2. 消息处理：支持文本消息的接收和广播
3. 系统消息：在用户加入/离开时发送系统通知，支持消息类型区分（文本/图片）





### WebSocket客户端

1. ChatService.lt：WebSocket客户端服务管理
2. ChatViewModel：管理聊天状态
3. NetworkModule.kt：网络模块

