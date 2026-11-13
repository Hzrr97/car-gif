const $ = (id) => document.getElementById(id);
const TOKEN_KEY = "gifapp_token";
const USER_KEY = "gifapp_user";
const PAGE_SIZE = 12;

let currentPage = 1;
let uploading = false;
let authMode = "login";

function token() { return localStorage.getItem(TOKEN_KEY); }

async function api(path, opts = {}) {
  const headers = Object.assign({}, opts.headers);
  if (opts.body) headers["Content-Type"] = "application/json";
  const t = token();
  if (t) headers["Authorization"] = "Bearer " + t;
  const res = await fetch(path, Object.assign({}, opts, { headers }));
  let data = null;
  try { data = await res.json(); } catch (e) { /* 非 JSON 响应 */ }
  if (res.status === 401 && !path.startsWith("/api/auth")) {
    logout();
    throw new Error("登录已过期，请重新登录");
  }
  if (!res.ok) throw new Error((data && data.error) || "请求失败 (" + res.status + ")");
  return data;
}

function showAuth() {
  $("main-view").classList.add("hidden");
  $("auth-view").classList.remove("hidden");
}

function showMain() {
  $("auth-view").classList.add("hidden");
  $("main-view").classList.remove("hidden");
  const user = JSON.parse(localStorage.getItem(USER_KEY) || "{}");
  $("who").textContent = user.username || "";
}

function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  showAuth();
}

function setAuthMode(mode) {
  authMode = mode;
  $("tab-login").classList.toggle("active", mode === "login");
  $("tab-register").classList.toggle("active", mode === "register");
  $("auth-submit").textContent = mode === "login" ? "登录" : "注册";
  $("password").autocomplete = mode === "login" ? "current-password" : "new-password";
  $("auth-error").textContent = "";
}

async function submitAuth(e) {
  e.preventDefault();
  $("auth-error").textContent = "";
  $("auth-submit").disabled = true;
  try {
    const data = await api("/api/auth/" + authMode, {
      method: "POST",
      body: JSON.stringify({ username: $("username").value.trim(), password: $("password").value }),
    });
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data.user));
    showMain();
    currentPage = 1;
    await loadFiles();
  } catch (err) {
    $("auth-error").textContent = err.message;
  } finally {
    $("auth-submit").disabled = false;
  }
}

async function loadFiles(page) {
  if (page) currentPage = page;
  const list = $("file-list");
  list.textContent = "加载中...";
  try {
    const data = await api("/api/files?page=" + currentPage + "&page_size=" + PAGE_SIZE);
    renderFiles(data);
  } catch (err) {
    list.textContent = err.message;
  }
}

function renderFiles(data) {
  const list = $("file-list");
  list.textContent = "";
  if (!data.items.length) {
    list.textContent = "还没有文件，先上传一个吧。";
    $("pager").textContent = "";
    return;
  }
  for (const f of data.items) {
    const card = document.createElement("div");
    card.className = "file-card";

    const preview = document.createElement("div");
    preview.className = "preview";
    if (f.content_type === "image/gif") {
      const img = document.createElement("img");
      img.src = f.url;
      img.alt = f.name;
      img.loading = "lazy";
      preview.appendChild(img);
    } else {
      const video = document.createElement("video");
      video.src = f.url;
      video.controls = true;
      video.preload = "metadata";
      preview.appendChild(video);
    }
    card.appendChild(preview);

    const info = document.createElement("div");
    info.className = "file-info";
    const name = document.createElement("p");
    name.className = "file-name";
    name.textContent = f.name;
    name.title = f.name;
    const meta = document.createElement("p");
    meta.className = "file-meta";
    meta.textContent = formatSize(f.size) + " · " + new Date(f.created_at).toLocaleString("zh-CN");
    info.appendChild(name);
    info.appendChild(meta);
    card.appendChild(info);

    const actions = document.createElement("div");
    actions.className = "file-actions";
    actions.appendChild(makeButton("复制链接", () => copyText(f.url)));
    actions.appendChild(makeButton("下载", () => window.open(f.url, "_blank")));
    actions.appendChild(makeButton("删除", async () => {
      if (!confirm("确定删除「" + f.name + "」？此操作不可恢复。")) return;
      try {
        await api("/api/files/" + f.id, { method: "DELETE" });
        await loadFiles();
      } catch (err) { alert(err.message); }
    }, "danger"));
    card.appendChild(actions);

    list.appendChild(card);
  }

  const totalPages = Math.max(1, Math.ceil(data.total / data.page_size));
  const pager = $("pager");
  pager.textContent = "";
  const prev = makeButton("上一页", () => loadFiles(currentPage - 1));
  prev.disabled = currentPage <= 1;
  const next = makeButton("下一页", () => loadFiles(currentPage + 1));
  next.disabled = currentPage >= totalPages;
  const label = document.createElement("span");
  label.textContent = "第 " + data.page + " / " + totalPages + " 页 · 共 " + data.total + " 个文件";
  pager.appendChild(prev);
  pager.appendChild(label);
  pager.appendChild(next);
}

function makeButton(text, onClick, cls) {
  const b = document.createElement("button");
  b.type = "button";
  b.textContent = text;
  if (cls) b.className = cls;
  b.addEventListener("click", onClick);
  return b;
}

function uploadFile(file) {
  if (!file || uploading) return;
  const ext = (file.name.split(".").pop() || "").toLowerCase();
  if (ext !== "gif" && ext !== "mp4") {
    alert("仅支持 GIF / MP4 文件");
    return;
  }
  uploading = true;
  $("upload-progress").classList.remove("hidden");
  $("bar-fill").style.width = "0%";
  const status = $("upload-status");
  status.className = "";
  status.textContent = "正在获取上传凭证...";

  (async () => {
    try {
      const presign = await api("/api/files/presign", {
        method: "POST",
        body: JSON.stringify({ filename: file.name, size: file.size }),
      });
      status.textContent = "正在上传 " + file.name + " ...";
      await putWithProgress(presign.upload_url, file, presign.content_type, (pct) => {
        $("bar-fill").style.width = Math.round(pct * 100) + "%";
      });
      status.textContent = "上传完成，正在校验...";
      await api("/api/files/complete", {
        method: "POST",
        body: JSON.stringify({ key: presign.key, name: file.name }),
      });
      status.textContent = "上传成功";
      await loadFiles(1);
    } catch (err) {
      status.textContent = "上传失败：" + err.message;
      status.className = "error";
    } finally {
      uploading = false;
      $("file-input").value = "";
    }
  })();
}

function putWithProgress(url, file, contentType, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", url);
    xhr.setRequestHeader("Content-Type", contentType);
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress(e.loaded / e.total);
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) resolve();
      else reject(new Error("存储返回状态 " + xhr.status));
    };
    xhr.onerror = () => reject(new Error("网络错误，直传失败（请检查存储桶 CORS 配置）"));
    xhr.send(file);
  });
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    alert("链接已复制（1 小时内有效）");
  } catch (e) {
    const ta = document.createElement("textarea");
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand("copy");
    ta.remove();
    alert("链接已复制（1 小时内有效）");
  }
}

function formatSize(n) {
  if (n < 1024) return n + " B";
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + " KB";
  if (n < 1024 * 1024 * 1024) return (n / 1024 / 1024).toFixed(1) + " MB";
  return (n / 1024 / 1024 / 1024).toFixed(2) + " GB";
}

$("tab-login").addEventListener("click", () => setAuthMode("login"));
$("tab-register").addEventListener("click", () => setAuthMode("register"));
$("auth-form").addEventListener("submit", submitAuth);
$("logout").addEventListener("click", logout);
$("refresh").addEventListener("click", () => loadFiles());

const dz = $("dropzone");
dz.addEventListener("click", () => $("file-input").click());
$("file-input").addEventListener("change", (e) => uploadFile(e.target.files[0]));
dz.addEventListener("dragover", (e) => { e.preventDefault(); dz.classList.add("dragover"); });
dz.addEventListener("dragleave", () => dz.classList.remove("dragover"));
dz.addEventListener("drop", (e) => {
  e.preventDefault();
  dz.classList.remove("dragover");
  uploadFile(e.dataTransfer.files[0]);
});

(async function init() {
  if (!token()) { showAuth(); return; }
  showMain();
  await loadFiles();
})();