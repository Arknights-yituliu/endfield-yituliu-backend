如果遇到需要修改.yml后缀的配置文件时，仅可读取我的application-test.yml，不要读取src/main/resources下其他的yml

git提交格式要求:
类型:功能描述，例如：feat:origin自动配置

数据库字符集与排序规则约定:

* 字符集统一使用 utf8mb4
* 排序规则默认 utf8mb4\_general\_ci，用于普通业务表（用户、日志、OAuth、配置等）
* 以下场景必须使用 utf8mb4\_bin：
  1. 游戏账号/角色 UID 相关表（ak\_*、ef\_*），避免大小写或重音不敏感导致不同账号被误合并
  2. 存放哈希摘要、令牌摘要等需精确比较的列（如 SHA-256 的 CHAR(64) 列），即使所在表为 general\_ci，列级也要显式指定 COLLATE utf8mb4\_bin
* 每张表必须显式声明 DEFAULT CHARSET 与 COLLATE，不依赖服务端默认继承
* 库级需显式对齐排序规则（CREATE DATABASE 与 ALTER DATABASE 两者都要写），避免已有库沿用历史排序规则导致字符串比较报 1267 非法混用

如果没有明确说明需要迁移，不需要出迁移脚本和sql
