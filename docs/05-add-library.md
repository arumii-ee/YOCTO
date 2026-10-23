# 4단계 실습: 이미지에 라이브러리 추가하기 ★목표 단계

이 단계가 원하시는 목표의 핵심입니다. 상황별로 4가지 방법을 다룹니다.

| 상황 | 방법 | 섹션 |
|---|---|---|
| 유명한 라이브러리 (Lua, zlib, libcurl, sqlite...) | 이미 있는 레시피를 찾아서 이미지에 추가만 | [A](#a-이미-레시피가-있는-라이브러리-추가-예-lua) |
| 내가 짠 C 라이브러리 (소스가 내 PC에) | 내 레이어 + 레시피 작성 | [B](#b-내가-만든-c-라이브러리-추가-meta-study-예제) |
| GitHub 등에 있는데 레시피가 없는 라이브러리 | git 소스 레시피 작성 (또는 devtool 자동 생성) | [C](#c-git에-있는-라이브러리-레시피-직접-작성) / [D](#d-devtool로-레시피-자동-생성-편한-방법) |
| 라이브러리를 쓰는 앱을 PC에서 개발 | SDK 생성 | [E](#e-sdk-만들기-pc에서-보드용-앱-개발) |

> 시작 전: 3단계에서 만든 `~/yocto/build` 가 있고, `source poky/oe-init-build-env build` 를 한 상태라고 가정합니다.

---

## A. 이미 레시피가 있는 라이브러리 추가 (예: Lua)

### A-1. 레시피가 어느 레이어에 있는지 찾기

1. https://layers.openembedded.org/ → **Recipes** 탭 → `lua` 검색
2. 결과: `lua` 레시피가 **meta-oe** 레이어(meta-openembedded 저장소 안)에 있음
3. 브랜치(버전)를 scarthgap으로 선택해서 있는지 확인

이미 등록된 레이어 안에 있는지는 이렇게도 확인 가능:
```bash
bitbake-layers show-recipes "lua*"
```

### A-2. 레이어 받기 (poky와 같은 브랜치!)

```bash
cd ~/yocto
git clone -b scarthgap https://git.openembedded.org/meta-openembedded
cd build
bitbake-layers add-layer ../meta-openembedded/meta-oe
bitbake-layers show-layers        # meta-oe 가 보이면 성공
```

### A-3. 이미지에 추가

`conf/local.conf` 끝에:
```bitbake
IMAGE_INSTALL:append = " lua"
```

```bash
bitbake core-image-minimal
runqemu qemuarm64 nographic
# 로그인 후
root@qemuarm64:~# lua -e 'print("hello from lua " .. _VERSION)'
```

> 💡 `local.conf` 에 넣는 건 **연습용**입니다. 실제 제품에서는 [B](#b-내가-만든-c-라이브러리-추가-meta-study-예제)처럼 **나만의 이미지 레시피**를 만들어서 관리하세요.
> (local.conf 는 개인 설정이라 보통 git 으로 공유하지 않습니다)

### 라이브러리 이름 ↔ 패키지 이름 팁

레시피 이름과 패키지 이름이 다를 수 있습니다. 예를 들어 `curl` 레시피는 `curl`(명령어) 과 `libcurl`(라이브러리) 패키지를 만듭니다.
```bash
# 레시피를 한 번 빌드한 후, 어떤 패키지들이 만들어졌는지 확인
bitbake curl
oe-pkgdata-util list-pkgs -p curl
# 특정 패키지에 어떤 파일이 들어있는지
oe-pkgdata-util list-pkg-files libcurl
```

---

## B. 내가 만든 C 라이브러리 추가 (meta-study 예제)

이 저장소의 [`meta-study/`](../meta-study) 레이어를 그대로 씁니다.

### B-1. 레이어 구조 이해

```
meta-study/
├── conf/layer.conf                              # 레이어 선언
├── recipes-example/
│   ├── libhello/
│   │   ├── libhello_1.0.bb                      # 라이브러리 레시피
│   │   └── files/hello.c, hello.h, Makefile     # 소스 (SRC_URI = file://...)
│   └── helloapp/
│       ├── helloapp_1.0.bb                      # 라이브러리를 쓰는 앱 레시피 (DEPENDS = "libhello")
│       └── files/main.c
└── recipes-core/images/study-image.bb           # 나만의 이미지
```

각 파일을 열어서 주석을 읽어보세요. 핵심 포인트:

**`libhello_1.0.bb`**
- `SRC_URI = "file://..."` → 레시피 옆 `files/` 폴더에서 소스를 가져옴
- `do_compile` 에서 `oe_runmake` → Yocto가 크로스 컴파일러를 `CC` 로 넘겨줌
- `do_install` 에서 `${D}${libdir}` 로 설치 → 나중에 보드의 `/usr/lib` 가 됨
- `.so.1.0.0` / `.so.1` 은 `libhello` 패키지로, `.so` / `.h` 는 `libhello-dev` 패키지로 **자동 분리**

**`Makefile`**
- `CC`, `CFLAGS`, `LDFLAGS` 를 **Makefile에서 덮어쓰지 않음** ← 매우 중요
  - `CC = gcc` 처럼 고정하면 PC용(x86)으로 빌드돼 버립니다
  - `LDFLAGS` 를 무시하면 `QA Issue: No GNU_HASH in the ELF binary` 에러가 납니다
- `DESTDIR` 지원 (설치 위치를 바꿀 수 있어야 함)

**`helloapp_1.0.bb`**
- `DEPENDS = "libhello"` → libhello 가 먼저 빌드되고, 헤더/so 가 크로스 sysroot 에 들어감
- 보드 실행용 의존성(RDEPENDS)은 Yocto가 `ldd` 처럼 분석해서 **자동 추가**

**`study-image.bb`**
- `require` 로 core-image-minimal 을 가져와서 `IMAGE_INSTALL:append` 로 추가

### B-2. 빌드해 보기

```bash
cd ~/yocto
git clone <이 저장소 주소> yocto-study      # 또는 meta-study 폴더만 복사
cd build
bitbake-layers add-layer ../yocto-study/meta-study

# 레시피 하나씩 빌드 (문제 생기면 범위가 좁아서 찾기 쉬움)
bitbake libhello
bitbake helloapp

# 이미지 빌드 (study-image 는 lua 때문에 meta-oe 가 필요 → A-2 먼저)
bitbake study-image
runqemu qemuarm64 study-image nographic
```

보드(QEMU) 안에서:
```bash
root@qemuarm64:~# helloapp
Hello, Yocto! (from libhello)
1 + 2 = 3
root@qemuarm64:~# ls -l /usr/lib/libhello*
root@qemuarm64:~# ldd /usr/bin/helloapp      # libhello.so.1 => /usr/lib/libhello.so.1
```

> `ldd` 가 없다고 나오면 `IMAGE_INSTALL:append = " ldd"` 를 추가하세요 (minimal 이미지에는 없음).

### B-3. 빌드 결과를 PC에서 확인하는 법 (보드 없이 디버깅)

```bash
# libhello 작업 폴더 위치
bitbake -e libhello | grep ^WORKDIR=

# 그 안에서:
#   temp/log.do_compile     컴파일 로그 (실제 실행된 gcc 명령이 보임)
#   image/                  do_install 결과 (= ${D})
#   packages-split/         패키지별로 나뉜 결과 (libhello/, libhello-dev/ ...)

file <WORKDIR>/image/usr/lib/libhello.so.1.0.0     # "ARM aarch64" 인지 확인
```

### B-4. 소스를 수정했을 때

`files/hello.c` 를 고친 뒤 그냥 `bitbake libhello` 하면 Yocto가 변경을 감지해서 다시 빌드합니다.
이상하게 반영이 안 되면:
```bash
bitbake -c cleansstate libhello    # 이 레시피 캐시 삭제
bitbake libhello
```

### B-5. 내 레이어를 처음부터 만들 때

```bash
cd ~/yocto
bitbake-layers create-layer meta-mycompany     # 뼈대 자동 생성 (conf/layer.conf + 예제 레시피)
cd build
bitbake-layers add-layer ../meta-mycompany
```

---

## C. git에 있는 라이브러리 레시피 직접 작성

예: GitHub 에 있는 CMake 기반 라이브러리.

```bitbake
# recipes-libs/mylib/mylib_git.bb
SUMMARY = "Some library from GitHub"
LICENSE = "MIT"
# 저장소 안의 라이선스 파일과 그 md5 (md5sum LICENSE 로 계산)
LIC_FILES_CHKSUM = "file://LICENSE;md5=<md5값>"

SRC_URI = "git://github.com/<owner>/<repo>.git;protocol=https;branch=main"
SRCREV = "<빌드할 커밋 해시 40자리>"    # 재현 가능한 빌드를 위해 커밋을 고정

PV = "1.0+git"

S = "${WORKDIR}/git"

inherit cmake          # CMake 프로젝트면 이 한 줄로 configure/compile/install 자동

# CMake 옵션이 필요하면
EXTRA_OECMAKE = "-DBUILD_SHARED_LIBS=ON -DBUILD_TESTS=OFF"
```

빌드 시스템별:
| 프로젝트에 있는 파일 | 레시피에 쓸 것 |
|---|---|
| `CMakeLists.txt` | `inherit cmake` |
| `configure.ac` / `configure` | `inherit autotools` |
| `meson.build` | `inherit meson` |
| `Makefile` 만 | B 예제처럼 `do_compile` / `do_install` 직접 작성 |

`LIC_FILES_CHKSUM` 의 md5를 모르겠으면 아무 값이나 넣고 빌드하면, 에러 메시지에 올바른 md5를 알려줍니다.

---

## D. devtool로 레시피 자동 생성 (편한 방법)

`devtool` 은 레시피 초안을 자동으로 만들어 주는 도구입니다. 라이선스, 빌드 시스템을 알아서 분석합니다.

```bash
# 1) 소스 주소를 주면 레시피 초안 생성 (build/workspace/ 임시 레이어에 들어감)
devtool add mylib https://github.com/<owner>/<repo>.git

# 2) 생성된 레시피 확인/수정
devtool edit-recipe mylib

# 3) 빌드 테스트
devtool build mylib

# 4) (선택) QEMU 나 보드에 바로 설치해서 테스트 (ssh 로 연결 가능한 경우)
devtool deploy-target mylib root@192.168.7.2

# 5) 완성되면 내 레이어로 옮기기
devtool finish mylib ../yocto-study/meta-study
```

> 기존 레시피의 소스를 수정해서 패치를 만들 때도 `devtool modify <레시피>` 를 씁니다. (나중에 공부)

---

## E. SDK 만들기 (PC에서 보드용 앱 개발)

매번 레시피를 만들지 않고, **PC에서 바로 크로스 컴파일**하고 싶을 때 씁니다.
(MCU 개발의 툴체인 설치와 같은 개념)

```bash
bitbake study-image -c populate_sdk
ls tmp/deploy/sdk/
# poky-glibc-x86_64-study-image-cortexa57-qemuarm64-toolchain-5.0.x.sh

# SDK 설치 (아무 PC에나 설치 가능 → 팀원에게 배포하기 좋음)
./tmp/deploy/sdk/poky-glibc-x86_64-study-image-*.sh -d ~/sdk-study

# 사용: 새 터미널에서
source ~/sdk-study/environment-setup-cortexa57-poky-linux
echo $CC                       # aarch64-poky-linux-gcc ... --sysroot=...
$CC main.c -o helloapp -lhello # libhello-dev 가 SDK에 들어있으므로 그냥 됨
file helloapp                  # ARM aarch64 확인
```

SDK에는 이미지에 들어간 라이브러리의 **헤더(-dev 패키지)** 가 자동으로 포함됩니다.

---

## 자주 만나는 에러

| 에러 | 원인 / 해결 |
|---|---|
| `Nothing PROVIDES 'xxx'` | 레시피가 없음 → 레이어 추가 안 했거나 이름 오타 |
| `Nothing RPROVIDES 'xxx'` | 패키지 이름이 틀림 (레시피 이름 ≠ 패키지 이름일 수 있음, A의 팁 참고) |
| `QA Issue: No GNU_HASH in the ELF binary` | Makefile 이 `LDFLAGS` 를 안 씀 → 링크 명령에 `$(LDFLAGS)` 추가 |
| `QA Issue: ... -dev package contains non-symlink .so` | 버전 없는 `.so` 실제 파일만 설치함 → soname 버전 붙이거나 `FILES` 조정 |
| `QA Issue: ... installed but not shipped in any package [installed-vs-shipped]` | do_install 로 설치한 파일이 어떤 패키지에도 안 들어감 → `FILES:${PN} += "경로"` |
| `fatal error: hello.h: No such file` | `DEPENDS` 누락 |
| `LIC_FILES_CHKSUM ... md5 mismatch` | 에러 메시지에 나온 md5 로 교체 |
| 레이어 추가 시 `not compatible with ...` | `LAYERSERIES_COMPAT` 가 poky 버전과 다름 / 브랜치 불일치 |

---

## ✅ 4단계 자가 점검

1. meta-oe 를 추가하고 Lua 를 이미지에 넣어 QEMU에서 실행했다.
2. meta-study 를 추가하고 `helloapp` 이 QEMU에서 실행되는 것을 확인했다.
3. `libhello` 와 `libhello-dev` 패키지에 각각 어떤 파일이 들어가는지 `packages-split/` 에서 확인했다.
4. hello.c 에 함수를 하나 추가하고, helloapp 에서 호출하도록 고쳐서 다시 빌드했다.
5. SDK 를 만들어서 레시피 없이 `$CC` 로 helloapp 을 크로스 컴파일했다.
6. (도전) GitHub 의 작은 C 라이브러리 하나를 골라 C 또는 D 방법으로 레시피를 만들어 이미지에 넣었다.
