# CarGif

## 项目结构

- `gif_app/`：Android 车机客户端工程，使用 Android Studio 打开该目录。
- `gif_upload/`：Go 素材服务及网页上传管理，包含运行配置和 SQLite 数据目录。

Go 服务的本地和服务器启动步骤见 [gif_upload/README.md](gif_upload/README.md)。请先进入 `gif_upload/`，再执行依赖下载、编译和启动命令；程序从该目录读取 `.env`。
