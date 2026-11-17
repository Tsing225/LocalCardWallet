# 本地卡包 LocalCardWallet

一款完全离线的 Android 卡包应用：把银行卡收进手机本机，用 NFC 贴卡读取或手工录入，
按银行品牌色生成卡面，随时查看卡号、有效期等信息。数据只存在本机，绝不联网。

## 功能

- **NFC 读卡**：贴近带芯片的银行卡，通过 EMV 协议读取卡号与有效期，读到后弹窗请用户确认再入库。
- **手工录入**：卡号经 Luhn 校验、卡组织长度校验，并与本地 BIN 库复核发卡行是否一致。
- **本地 BIN 库**：341 家银行、9480 个号段，全部离线查询，可识别发卡行、品牌色与借记 / 贷记类型；支持手动指定发卡行，手动指定后不会被自动识别覆盖。
- **卡面**：8 套程序绘制的模板（按银行主色实时生成），或用自己的照片做封面；照片只存于 App 私有目录。
- **安全门禁**：访问密码（PBKDF2 加盐哈希）+ 生物识别解锁；查看完整卡号与 CVV 需再次生物识别；剪贴板复制会标记敏感并在 60 秒后自动清除。
- **其他**：中英双语、浅色 / 深色 / 动态取色、卡片上限 100 张。

## 如何构建

需要 Android Studio（或命令行 + JDK 17）：

```bash
./gradlew :app:assembleDebug     # 调试包
./gradlew :app:testDebugUnitTest # 单元测试
```

最低 Android 8.0（API 24），目标 API 37。需要带 NFC 的真机才能使用读卡功能；
`android.hardware.nfc` 声明为必需，因此应用不会安装到无 NFC 的设备上。

## 代码结构

```
app/src/main/java/com/tsing/localcardwallet/
  MainActivity.kt       单 Activity：导航（钱包 / 设置 / 添加 / 详情）、NFC 读卡、钱包首页叠卡布局
  LockScreen.kt         锁屏：建密码、解锁、生物识别、输错锁定
  AuthManager.kt        本地账号：PBKDF2 密码哈希、输错锁定计时
  AppDatabase.kt        Room 数据库定义与 1→4 迁移
  DbKeyManager.kt       数据库口令保管：Keystore 包裹的随机口令，含旧版明文口令迁移
  CardEntity.kt / CardDao.kt
  CardValidator.kt      Luhn、卡组织识别、长度规则、BIN 一致性复核
  BankBinRepository.kt  banks.json 加载与最长前缀匹配（绝不在主线程解析）
  CardFace.kt           卡面绘制（Canvas 模板、卡组织标识）与封面图加载
  CardDetailScreen.kt   详情页：遮罩/显示卡号与 CVV、复制、编辑
  AddCardScreen.kt      手工录入：实时校验与 BIN 锁定提示
  SettingsScreen.kt     语言、主题、生物识别开关、改密码、立即锁定、清空
  BiometricGate.kt      生物识别 / 设备凭据验证封装
  FacePickerSheet.kt / BankPickerDialog.kt
  CardMedia.kt          封面照片复制到私有目录
  AppSettings.kt / LocaleHelper.kt / BounceClick.kt
  ui/theme/             Material 3 配色与排版
app/src/main/assets/banks.json  BIN 数据库（见 THIRD_PARTY_NOTICES.txt）
```

## 安全模型（写给未来的自己）

- **数据库加密**：Room + SQLCipher，口令是 32 字节随机值，经 Android Keystore 的 AES-GCM
  密钥加密后存入 SharedPreferences；密钥本身不可导出。旧版本曾明文存放口令，
  升级后首次启动自动迁移（原样复用旧口令，已存数据不受影响）并删除明文。
- **访问密码只是门禁**：密码哈希（PBKDF2-HMAC-SHA256，12 万次迭代）只决定能否打开界面，
  不参与数据库加密。忘记密码只能清空数据重置。
- **无备份**：`allowBackup=false`，且备份 / 迁移规则明确排除数据库、配置与文件。
  原因：Keystore 密钥无法随备份迁移，备份出去的数据库在新设备上打不开；
  换机请先手工记下卡片信息（应用内暂不提供导出功能）。
- **防窥屏**：窗口设置 `FLAG_SECURE`，系统截屏与多任务缩略图看不到卡面；
  切到后台超过 60 秒再回来会自动上锁。
- **无网络**：Manifest 没有 `INTERNET` 权限，代码里没有任何网络调用；
  NFC 读取时不读交易记录。
- **剪贴板**：复制的完整卡号在 Android 13+ 标记为敏感内容，60 秒后自动清除。

## BIN 数据来源

`assets/banks.json` 派生自两个开源数据（仅保留银行名、国家、品牌色与 BIN 前缀等事实数据），
完整声明见 `app/src/main/assets/THIRD_PARTY_NOTICES.txt`：

- ramoona/banks-db（CC-BY-4.0）：全球银行数据
- hexindai/bcbc（MIT）：中国银联 BIN 数据，中国号段优先采用

应用不内置任何银行 logo 或卡面图片：卡组织标识与卡面均由代码绘制，照片封面由用户自选。

## 版本

当前版本 1.8.2。改动记录以 git 提交历史为准。
