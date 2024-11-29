# MiniChatApp

**写在前面：代码结构部分参考了网络教程以及开源项目，由于本人是从零开始学app开发，可能存在一些bug敬请见谅**

开发工具：AndroidStudio+Copilot
项目版本：API26+GroovyDSL+gradle=8.5

主要依赖：
1. Hilt: 用于依赖注入
2. Retrofit + OkHttp: 用于网络请求
3. Room: 用于本地数据存储
4. Coil: 用于图片加载
5. 一个WebSocket服务器，SpringBoot源码在 `项目根目录/chatserver` 下

项目结构：

```
main
 └── java
      └── com.example.minichatapp
           ├── data
           │    ├── local
           │    │    ├── AppDatabase.kt
           │    │    ├── AppSettings.kt
           │    │    └── UserDao.kt
           │    ├── remote
           │    │    └── ChatService.kt
           │    └── repository
           │         └── UserRepository.kt
           ├── di
           │    └── DatabaseModule.kt
           ├── domain.model
           │    ├── ChatMessage.kt
           │    └── User.kt
           ├── ui
           │    ├── components
           │    │    └── CommonComponents.kt
           │    ├── screens
           │    │    ├── auth
           │    │    │    ├── AuthViewModel
           │    │    │    ├── LoginScreen.kt
           │    │    │    └── RegisterScreen.kt
           │    │    ├── chat
           │    │    │    ├── ChatScreen.kt
           │    │    │    └── ChatMessageItem.kt
           │    │    ├── contact
           │    │    ├── settings
           │    │    │    ├── SettingsScreen.kt
           │    │    │    └── SettingsViewModel.kt
           │    │    └── MainScreen.kt
           │    ├── theme
           │    │     ├── Color.kt
           │    │     ├── Theme.kt
           │    │     └── Type.kt
           │    └── Navigation.kt
           ├── MainActivity.kt
           └── ProjectStructure.txt
```

## TodoList

1. 基本功能已经实现，注册界面应该有注册成功提示并延迟返回
2. 给聊天界面添加一个返回按钮
3. 增加图片库和图片发送功能
4. 增加联系人功能
5. 将用户信息转移到服务器存储

## 应用示意图

1. 登录与注册界面
   ![image-20241129153323657](assets\image-20241129153323657.png)![image-20241129153326674](assets\image-20241129153326674.png)

2. 设置界面

   ![image-20241129153413289](assets\image-20241129153413289.png)

3. 聊天室界面

   ![image-20241129153431613](assets\image-20241129153431613.png)
