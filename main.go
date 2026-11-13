package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
)

func main() {
	cfg, err := LoadConfig()
	if err != nil {
		log.Fatalf("配置错误: %v", err)
	}
	db, err := OpenDB(cfg.DBPath)
	if err != nil {
		log.Fatalf("数据库错误: %v", err)
	}
	defer db.Close()
	store, err := NewStorage(cfg)
	if err != nil {
		log.Fatalf("存储客户端初始化失败: %v", err)
	}

	gin.SetMode(gin.ReleaseMode)
	r := gin.New()
	r.Use(gin.Logger(), gin.Recovery())

	auth := &AuthHandler{db: db, cfg: cfg}
	files := &FileHandler{db: db, store: store, cfg: cfg}

	api := r.Group("/api")
	api.GET("/health", func(c *gin.Context) { c.JSON(http.StatusOK, gin.H{"ok": true}) })
	api.POST("/auth/register", auth.Register)
	api.POST("/auth/login", auth.Login)

	authorized := api.Group("", auth.AuthMiddleware())
	authorized.POST("/files/presign", files.PresignUpload)
	authorized.POST("/files/complete", files.CompleteUpload)
	authorized.GET("/files", files.List)
	authorized.GET("/files/:id/download", files.DownloadURL)
	authorized.DELETE("/files/:id", files.Delete)

	// 车机客户端只读接口：共享密钥鉴权，列出全部文件，无需用户登录
	client := api.Group("/client", ClientKeyMiddleware(cfg.ClientAPIKey))
	client.GET("/files", files.ClientList)
	client.GET("/files/:id/download", files.ClientDownloadURL)

	// sub, err := fs.Sub(webFS, "web")
	// if err != nil {
	// 	log.Fatal(err)
	// }
	// serveFile := func(name, contentType string) gin.HandlerFunc {
	// 	data, err := fs.ReadFile(sub, name)
	// 	if err != nil {
	// 		log.Fatalf("读取嵌入文件 %s 失败: %v", name, err)
	// 	}
	// 	return func(c *gin.Context) { c.Data(http.StatusOK, contentType, data) }
	// }
	// index := serveFile("index.html", "text/html; charset=utf-8")
	// r.GET("/", index)
	// r.GET("/index.html", index)
	// r.GET("/app.js", serveFile("app.js", "application/javascript; charset=utf-8"))
	// r.GET("/style.css", serveFile("style.css", "text/css; charset=utf-8"))

	srv := &http.Server{Addr: ":" + cfg.Port, Handler: r}
	go func() {
		log.Printf("服务已启动: http://localhost:%s", cfg.Port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("服务启动失败: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("正在关闭服务...")
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_ = srv.Shutdown(ctx)
}