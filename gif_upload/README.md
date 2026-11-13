# CarGif

GIF / MP4 素材管理服务，提供网页上传管理和车机只读接口。

## 环境要求

- Go 1.27.4 或更高版本（以 `go.mod` 为准），通过 `go version` 检查。
- 已创建 Cloudflare R2 存储桶，并取得具有对象读写权限的访问凭证。
- SQLite 通过 Go 驱动集成，无需单独安装数据库；前端无需安装 Node.js。

仓库已包含 `go.mod`、`go.sum`，拉取代码后下载依赖即可，无需再次执行 `go mod init`。

## 运行配置

首次运行时，将 `.env.example` 复制为 `.env`，填写以下配置。如果已有 `.env`，直接检查并修改，不要覆盖。

| 配置项 | 说明 |
| --- | --- |
| `PORT` | 服务端口，默认 `8080` |
| `JWT_SECRET` | 用户登录令牌的签名密钥，替换示例值为随机密钥 |
| `CLIENT_API_KEY` | 车机请求头 `X-API-Key` 使用的共享密钥，替换示例值 |
| `DB_PATH` | SQLite 文件路径，默认 `data/app.db` |
| `MAX_UPLOAD_MB` | 单文件大小上限，默认 `200` MB |
| `R2_ACCOUNT_ID` | Cloudflare 账户 ID |
| `R2_ACCESS_KEY_ID` | R2 访问凭证的 Access Key ID |
| `R2_SECRET_ACCESS_KEY` | R2 访问凭证的 Secret Access Key |
| `R2_BUCKET` | R2 存储桶名称 |

也可以使用 `R2_ENDPOINT` 直接指定存储端点，此时不需要 `R2_ACCOUNT_ID`。

程序从启动时的工作目录读取 `.env`；相对数据库路径也基于该目录。以下命令都在gif_upload 目录执行。数据库目录会自动创建，需要有写入权限。`.env` 和 `data/` 不应提交到 Git，`data/` 需要持久保存。

## Windows 本地启动

在 PowerShell 中执行：

```powershell
cd E:\work\github\car-gif\gif_upload
go version
```

如果安装 Go 后提示找不到命令，先重新打开终端；也可以为当前终端临时设置 PATH：

```powershell
$env:Path = "C:\Program Files\Go\bin;$env:Path"
```

首次运行且尚无 `.env` 时复制配置模板：

```powershell
Copy-Item .env.example .env
notepad .env
```

配置完成后，下载依赖并直接启动：

```powershell
go mod download
go run .
```

也可以先编译，再启动生成的程序：

```powershell
go build -o car-gif.exe .
.\car-gif.exe
```

以上两种方式选择一种即可。默认访问 <http://localhost:8080/>，按 `Ctrl+C` 停止服务。若修改了 `PORT`，请使用实际端口。

## Linux 服务器启动

以下以项目目录 `/root/car-gif/gif_upload` 为例，服务器需已安装满足版本要求的 Go。

```bash
cd /root/car-gif/gif_upload
go version
```

首次运行且尚无 `.env` 时复制并编辑配置：

```bash
cp .env.example .env
nano .env
```

下载依赖并在 Linux 服务器上编译：

```bash
go mod download
go build -o car-gif .
```

先前台运行，确认启动日志正常；按 `Ctrl+C` 停止后再切换为后台运行：

```bash
./car-gif
```

后台启动前，确认没有其他实例占用端口：

```bash
nohup ./car-gif > car-gif.log 2>&1 & echo $! > car-gif.pid
```

该方式在退出 SSH 后继续运行，但服务器重启后不会自动启动；需要开机启动时可使用 systemd 托管。

查看日志：

```bash
tail -f /root/car-gif/gif_upload/car-gif.log
```

检查服务是否响应（若修改了 `PORT`，替换对应端口）：

```bash
curl http://127.0.0.1:8080/api/health
```

预期返回 `{"ok":true}`。这只验证 HTTP 服务响应，R2 配置还需通过实际上传和下载验证。

停止由上述后台命令启动的实例：

```bash
kill "$(cat /root/car-gif/gif_upload/car-gif.pid)"
```

更新代码时先停止旧实例，重新编译成功后再执行后台启动命令。网页资源嵌入在程序中，修改 `web/` 后也需要重新编译并重启。Windows 编译生成的 `.exe` 不能直接用于 Linux。

外部访问地址为 `http://服务器IP:8080/`，需要在服务器安全组和防火墙中放行相应端口。正式部署建议通过反向代理提供 HTTPS。

## 网页上传与车机访问

网页可注册、登录并上传 GIF / MP4。上传完成后会写入 SQLite 文件记录，车机接口才能查询到；直接在 R2 控制台上传文件不会自动生成记录。

网页直接上传到 R2，需要在存储桶 CORS 策略中允许实际网页来源。例如本地开发：

```json
[
  {
    "AllowedOrigins": ["http://localhost:8080"],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

服务器部署时，将实际访问来源（协议、域名或 IP、端口）加入 `AllowedOrigins`。

车机无需网页登录，携带 `X-API-Key` 请求头访问：

- `GET /api/client/files?page=1&page_size=40`：分页查询全部用户的素材。
- `GET /api/client/files/:id/download`：获取素材临时下载链接。
