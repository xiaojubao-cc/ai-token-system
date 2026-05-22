第一步：连接到服务器

在你的电脑上打开终端，输入：

ssh root@你的服务器IP

输入密码后登录成功。

  ---
第二步：安装 Docker

Ubuntu / Debian 系统：

# 1. 更新软件包列表
apt update

# 2. 安装必要的依赖
apt install -y ca-certificates curl

# 3. 添加 Docker 官方 GPG 密钥
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

# 4. 添加 Docker 软件源
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list >
/dev/null

# 5. 安装 Docker
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 6. 验证安装
docker --version
docker compose version

CentOS 系统：

# 1. 安装必要依赖
yum install -y yum-utils

# 2. 添加 Docker 软件源
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 3. 安装 Docker
yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 4. 启动 Docker 并设置开机自启
systemctl start docker
systemctl enable docker

# 5. 验证安装
docker --version
docker compose version

  ---
第三步：上传项目到服务器

方式一：通过 Git 拉取（推荐）

# 在服务器上找一个目录放项目，比如 /opt
cd /opt

# 克隆项目（把地址换成你的 Git 仓库地址）
git clone https://github.com/你的用户名/ai-token-system.git

cd ai-token-system

方式二：通过 SCP 上传（如果没有 Git 仓库）

# 在你本地电脑的终端执行（不是服务器上）：
scp -r D:\java_projects\ai-token-system root@你的服务器IP:/opt/

  ---
第四步：按需修改配置（可选）

在启动之前，你可以修改 docker-compose.yml 中的几个关键配置：

cd /opt/ai-token-system
vim docker-compose.yml

以下是你可以修改的地方：

┌─────────────────┬────────────────────────────┬────────────────────────────────────────────────┐
│     配置项      │            位置            │                      说明                      │
├─────────────────┼────────────────────────────┼────────────────────────────────────────────────┤
│ MySQL root 密码 │ MYSQL_ROOT_PASSWORD        │ 默认 123456，生产环境请改成复杂密码            │
├─────────────────┼────────────────────────────┼────────────────────────────────────────────────┤
│ 应用数据库密码  │ SPRING_DATASOURCE_PASSWORD │ 要和上面 MySQL 密码一致                        │
├─────────────────┼────────────────────────────┼────────────────────────────────────────────────┤
│ 应用端口        │ ports: "8080:8080"         │ 如果 8080 被占用，改前面的数字，如 "9090:8080" │
└─────────────────┴────────────────────────────┴────────────────────────────────────────────────┘

▎ 注意：如果改了 MySQL 密码，docker-compose.yml 里两个地方要一起改。

  ---
第五步：构建并启动

cd /opt/ai-token-system

# 一条命令启动所有服务（MySQL + Redis + 应用）
# -d 表示后台运行
docker compose up -d

这个命令执行过程中 Docker 会：
1. 下载 MySQL 8.0 镜像和 Redis 7 镜像
2. 下载 Maven 基础镜像，在容器里编译 Java 项目（约 3-10 分钟，取决于网速）
3. 构建应用镜像
4. 依次启动 MySQL → Redis → 应用

首次构建需要较长时间（下载依赖），后续重新构建会快很多。

  ---
第六步：验证是否成功

# 1. 查看三个容器是否都在运行（STATUS 列应该都是 Up）
docker compose ps

正常情况下会看到三个容器都是 Up 状态。

# 2. 查看应用日志，确认启动没有报错
docker compose logs app

看到类似 Started AiTokenSystemApplication 就说明成功。

# 3. 测试接口是否响应
curl http://localhost:8080

也可以从浏览器访问：http://你的服务器IP:8080

  ---
第七步：常用操作

# 查看所有容器运行状态
docker compose ps

# 实时查看应用日志（按 Ctrl+C 退出）
docker compose logs -f app

# 查看 MySQL 日志
docker compose logs -f mysql

# 重启应用（修改代码后重新构建并启动）
docker compose up -d --build app

# 停止所有服务
docker compose stop

# 启动已停止的服务
docker compose start

# 停止并删除所有容器（数据不会丢）
docker compose down

# 停止并删除所有容器 + 清空数据库数据（谨慎！）
docker compose down -v

  ---
常见问题排查

Q1：启动失败，日志显示 Communications link failure

说明应用连不上 MySQL。检查：
# 确认 MySQL 容器是否在运行
docker compose ps mysql

# 查看 MySQL 日志是否已经显示 "ready for connections"
docker compose logs mysql | grep "ready for connections"
MySQL 首次启动需要初始化，等待约 30 秒再试。

Q2：端口被占用

改 docker-compose.yml 里的端口映射，例如把 "8080:8080" 改成 "9090:8080"，然后 docker compose up -d 重新启动。

Q3：构建失败，显示 Maven 下载依赖出错

可能是服务器网络问题导致依赖下载失败。重试：
docker compose build --no-cache app
docker compose up -d

Q4：修改了代码后如何更新

cd /opt/ai-token-system
git pull                      # 拉取最新代码
docker compose up -d --build app  # 重新构建并重启应用

Q5：想进入容器内部排查问题

# 进入应用容器
docker exec -it ai-token-app sh

# 进入 MySQL 容器
docker exec -it ai-token-mysql mysql -uroot -p123456 ai_token

  ---
总结：核心命令速查

┌────────────┬──────────────────────────────────┐
│    操作    │               命令               │
├────────────┼──────────────────────────────────┤
│ 一键启动   │ docker compose up -d             │
├────────────┼──────────────────────────────────┤
│ 查看状态   │ docker compose ps                │
├────────────┼──────────────────────────────────┤
│ 查看日志   │ docker compose logs -f app       │
├────────────┼──────────────────────────────────┤
│ 重启应用   │ docker compose restart app       │
├────────────┼──────────────────────────────────┤
│ 更新并重启 │ docker compose up -d --build app │
├────────────┼──────────────────────────────────┤
│ 全部停止   │ docker compose down              │
├────────────┼──────────────────────────────────┤
│ 清空数据   │ docker compose down -v           │
└────────────┴──────────────────────────────────┘