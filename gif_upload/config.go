package main

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/joho/godotenv"
)

type Config struct {
	Port              string
	JWTSecret         string
	ClientAPIKey      string
	DBPath            string
	R2Endpoint        string
	R2AccessKeyID     string
	R2SecretAccessKey string
	R2Bucket          string
	MaxUploadBytes    int64
	UploadURLTTL      time.Duration
	DownloadURLTTL    time.Duration
}

func LoadConfig() (*Config, error) {
	_ = godotenv.Load()

	cfg := &Config{
		Port:              envOr("PORT", "8080"),
		JWTSecret:         os.Getenv("JWT_SECRET"),
		ClientAPIKey:      os.Getenv("CLIENT_API_KEY"),
		DBPath:            envOr("DB_PATH", "data/app.db"),
		R2AccessKeyID:     os.Getenv("R2_ACCESS_KEY_ID"),
		R2SecretAccessKey: os.Getenv("R2_SECRET_ACCESS_KEY"),
		R2Bucket:          os.Getenv("R2_BUCKET"),
		UploadURLTTL:      15 * time.Minute,
		DownloadURLTTL:    time.Hour,
	}

	cfg.R2Endpoint = os.Getenv("R2_ENDPOINT")
	if cfg.R2Endpoint == "" {
		if id := os.Getenv("R2_ACCOUNT_ID"); id != "" {
			cfg.R2Endpoint = fmt.Sprintf("https://%s.r2.cloudflarestorage.com", id)
		}
	}

	maxMB, err := strconv.ParseInt(envOr("MAX_UPLOAD_MB", "200"), 10, 64)
	if err != nil || maxMB <= 0 {
		return nil, fmt.Errorf("MAX_UPLOAD_MB 必须是正整数")
	}
	cfg.MaxUploadBytes = maxMB * 1024 * 1024

	var missing []string
	for k, v := range map[string]string{
		"JWT_SECRET":         cfg.JWTSecret,
		"R2_ENDPOINT":        cfg.R2Endpoint,
		"R2_ACCESS_KEY_ID":   cfg.R2AccessKeyID,
		"R2_SECRET_ACCESS_KEY": cfg.R2SecretAccessKey,
		"R2_BUCKET":          cfg.R2Bucket,
	} {
		if v == "" {
			missing = append(missing, k)
		}
	}
	if len(missing) > 0 {
		return nil, fmt.Errorf("缺少环境变量: %s（参考 .env.example 配置 .env）", strings.Join(missing, ", "))
	}
	return cfg, nil
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}