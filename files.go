package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"path"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

var allowedExts = map[string]string{
	".gif": "image/gif",
	".mp4": "video/mp4",
}

type FileHandler struct {
	db    *sql.DB
	store *Storage
	cfg   *Config
}

type File struct {
	ID          int64  `json:"id"`
	Name        string `json:"name"`
	Size        int64  `json:"size"`
	ContentType string `json:"content_type"`
	CreatedAt   string `json:"created_at"`
	URL         string `json:"url,omitempty"`
}

type presignRequest struct {
	Filename string `json:"filename" binding:"required"`
	Size     int64  `json:"size" binding:"required"`
}

func (h *FileHandler) PresignUpload(c *gin.Context) {
	var req presignRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求格式错误"})
		return
	}
	if req.Size <= 0 || req.Size > h.cfg.MaxUploadBytes {
		c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Sprintf("文件大小需在 1 字节 ~ %d MB 之间", h.cfg.MaxUploadBytes/1024/1024)})
		return
	}
	ext := strings.ToLower(filepath.Ext(req.Filename))
	contentType, ok := allowedExts[ext]
	if !ok {
		c.JSON(http.StatusBadRequest, gin.H{"error": "仅支持 .gif 和 .mp4 文件"})
		return
	}

	key := fmt.Sprintf("uploads/%d/%s%s", currentUserID(c), uuid.NewString(), ext)
	url, err := h.store.PresignPut(c.Request.Context(), key, contentType, h.cfg.UploadURLTTL)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "生成上传凭证失败"})
		return
	}
	c.JSON(http.StatusOK, gin.H{
		"upload_url":   url,
		"key":          key,
		"content_type": contentType,
		"expires_in":   int(h.cfg.UploadURLTTL.Seconds()),
	})
}

type completeRequest struct {
	Key  string `json:"key" binding:"required"`
	Name string `json:"name" binding:"required"`
}

func (h *FileHandler) CompleteUpload(c *gin.Context) {
	var req completeRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求格式错误"})
		return
	}
	uid := currentUserID(c)
	if !strings.HasPrefix(req.Key, fmt.Sprintf("uploads/%d/", uid)) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无效的文件 key"})
		return
	}
	ext := strings.ToLower(path.Ext(req.Key))
	if _, ok := allowedExts[ext]; !ok {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的文件类型"})
		return
	}

	head, err := h.store.Head(c.Request.Context(), req.Key)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "未在存储中找到该文件，请确认上传是否成功"})
		return
	}
	size := derefInt64(head.ContentLength)
	if size <= 0 || size > h.cfg.MaxUploadBytes {
		_ = h.store.Delete(c.Request.Context(), req.Key)
		c.JSON(http.StatusBadRequest, gin.H{"error": "文件为空或超出大小限制，已删除"})
		return
	}

	name := sanitizeName(req.Name, req.Key)
	contentType := allowedExts[ext]
	if head.ContentType != nil && *head.ContentType != "" {
		contentType = *head.ContentType
	}

	var f File
	res, err := h.db.Exec("INSERT INTO files(user_id, object_key, name, size, content_type) VALUES(?,?,?,?,?)",
		uid, req.Key, name, size, contentType)
	if err != nil {
		if !strings.Contains(err.Error(), "UNIQUE") {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
			return
		}
		// 重复调用 complete 时幂等返回已有记录
		if err := h.queryFile("WHERE object_key=? AND user_id=?", req.Key, uid).Scan(&f.ID, &f.Name, &f.Size, &f.ContentType, &f.CreatedAt); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
			return
		}
	} else {
		id, _ := res.LastInsertId()
		if err := h.queryFile("WHERE id=?", id).Scan(&f.ID, &f.Name, &f.Size, &f.ContentType, &f.CreatedAt); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
			return
		}
	}
	c.JSON(http.StatusOK, f)
}

func (h *FileHandler) List(c *gin.Context) {
	h.listFiles(c, currentUserID(c))
}

// ClientList 列出全部用户的文件，供车机客户端（密钥鉴权）使用。
func (h *FileHandler) ClientList(c *gin.Context) {
	h.listFiles(c, 0)
}

// listFiles 按 uid 过滤；uid=0 表示不限用户（全局）。
func (h *FileHandler) listFiles(c *gin.Context, uid int64) {
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))
	if page < 1 {
		page = 1
	}
	if pageSize < 1 || pageSize > 100 {
		pageSize = 20
	}

	where := ""
	args := make([]any, 0, 3)
	if uid > 0 {
		where = "WHERE user_id=?"
		args = append(args, uid)
	}

	var total int64
	if err := h.db.QueryRow("SELECT COUNT(*) FROM files "+where, args...).Scan(&total); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
		return
	}
	listArgs := append(args, pageSize, (page-1)*pageSize)
	rows, err := h.db.Query(
		"SELECT id, name, size, content_type, created_at, object_key FROM files "+where+" ORDER BY id DESC LIMIT ? OFFSET ?",
		listArgs...)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
		return
	}
	defer rows.Close()

	items := make([]File, 0)
	for rows.Next() {
		var f File
		var key string
		if err := rows.Scan(&f.ID, &f.Name, &f.Size, &f.ContentType, &f.CreatedAt, &key); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
			return
		}
		url, err := h.store.PresignGet(c.Request.Context(), key, h.cfg.DownloadURLTTL)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "生成下载链接失败"})
			return
		}
		f.URL = url
		items = append(items, f)
	}
	c.JSON(http.StatusOK, gin.H{"items": items, "total": total, "page": page, "page_size": pageSize})
}

func (h *FileHandler) DownloadURL(c *gin.Context) {
	h.downloadURL(c, currentUserID(c))
}

// ClientDownloadURL 供车机客户端按 id 获取下载链接（密钥鉴权，不限用户）。
func (h *FileHandler) ClientDownloadURL(c *gin.Context) {
	h.downloadURL(c, 0)
}

func (h *FileHandler) downloadURL(c *gin.Context, uid int64) {
	var key string
	var err error
	if uid > 0 {
		err = h.db.QueryRow("SELECT object_key FROM files WHERE id=? AND user_id=?", c.Param("id"), uid).Scan(&key)
	} else {
		err = h.db.QueryRow("SELECT object_key FROM files WHERE id=?", c.Param("id")).Scan(&key)
	}
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "文件不存在"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
		return
	}
	url, err := h.store.PresignGet(c.Request.Context(), key, h.cfg.DownloadURLTTL)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "生成下载链接失败"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"url": url, "expires_in": int(h.cfg.DownloadURLTTL.Seconds())})
}

func (h *FileHandler) Delete(c *gin.Context) {
	uid := currentUserID(c)
	var key string
	err := h.db.QueryRow("SELECT object_key FROM files WHERE id=? AND user_id=?", c.Param("id"), uid).Scan(&key)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "文件不存在"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
		return
	}
	if err := h.store.Delete(c.Request.Context(), key); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "删除存储文件失败"})
		return
	}
	if _, err := h.db.Exec("DELETE FROM files WHERE id=? AND user_id=?", c.Param("id"), uid); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "服务器错误"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"ok": true})
}

func (h *FileHandler) queryFile(where string, args ...any) *sql.Row {
	return h.db.QueryRow("SELECT id, name, size, content_type, created_at FROM files "+where, args...)
}

func sanitizeName(name, key string) string {
	name = strings.TrimSpace(filepath.Base(name))
	if name == "" || name == "." || name == string(filepath.Separator) {
		name = path.Base(key)
	}
	if r := []rune(name); len(r) > 255 {
		name = string(r[:255])
	}
	return name
}

func derefInt64(p *int64) int64 {
	if p == nil {
		return 0
	}
	return *p
}