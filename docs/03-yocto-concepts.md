# 2단계: Yocto 핵심 개념

Yocto는 용어가 많아서 처음에 다들 헷갈립니다. 하지만 **핵심은 5개**뿐입니다.

> **BitBake(빌드 도구)가 레이어(폴더 묶음)에 들어있는 레시피(빌드 설명서)를 읽어서,
> 설정(MACHINE/DISTRO)에 맞게 이미지(결과물)를 만든다.**

---

## 1. 이름 정리: Yocto / Poky / OpenEmbedded / BitBake

| 이름 | 정체 | 비유 |
|---|---|---|
| **Yocto Project** | 프로젝트(단체) 이름. 도구가 아님 | "GNU 프로젝트" 같은 것 |
| **BitBake** | 실제 빌드 실행 도구 (Python으로 작성) | `make` |
| **OpenEmbedded-Core (OE-Core)** | 기본 레시피 모음 (gcc, glibc, busybox, 커널...) | 표준 라이브러리 |
| **Poky** | BitBake + OE-Core + 예제 설정을 묶은 **참조 배포판** | 벤더가 주는 "예제 프로젝트" |
| **meta-openembedded** | 추가 레시피 모음 (Lua, Python 패키지, 네트워크 툴 등 수천 개) | 확장 라이브러리 |

처음에는 **"Poky를 받아서 시작한다"** 고 기억하면 됩니다.

## 2. 레시피 (Recipe, `.bb` 파일) ★가장 중요

**레시피 = 소프트웨어 하나를 빌드하는 방법을 적은 설명서**입니다.
"라이브러리를 추가한다" = 레시피를 찾거나 작성한다.

```bitbake
# libhello_1.0.bb   (파일 이름 = 패키지이름_버전.bb)
SUMMARY = "내 예제 라이브러리"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://hello.c file://hello.h file://Makefile"   # 소스는 어디서? (로컬/git/http)
S = "${WORKDIR}"                                              # 소스가 풀리는 위치

do_compile() {             # 어떻게 빌드?
    oe_runmake
}

do_install() {             # 빌드 결과를 rootfs의 어디에 넣을지?
    oe_runmake install DESTDIR=${D} LIBDIR=${libdir} INCDIR=${includedir}
}
```

BitBake는 각 레시피에 대해 **태스크(task)** 를 순서대로 실행합니다:

```
do_fetch     소스 다운로드 (SRC_URI)
  ↓
do_unpack    압축 해제 → ${S}
  ↓
do_patch     패치 적용
  ↓
do_configure ./configure, cmake 등
  ↓
do_compile   컴파일 (크로스 컴파일러 자동 사용)
  ↓
do_install   ${D} (가짜 루트) 아래에 설치
  ↓
do_package   ${D}를 libhello / libhello-dev / libhello-dbg ... 로 쪼갬
  ↓
(이미지 빌드 시) rootfs에 패키지 설치
```

### 레시피의 종류와 확장자

| 파일 | 용도 |
|---|---|
| `xxx.bb` | 레시피 본체 |
| `xxx.bbappend` | **남의 레시피를 수정**할 때 (원본은 건드리지 않고 덧붙이기) |
| `xxx.inc` | 여러 레시피가 공유하는 내용 (C의 헤더처럼 `require`로 포함) |
| `xxx.bbclass` | 공통 빌드 로직. `inherit cmake` 하면 CMake 빌드 방식이 자동 적용 |
| `xxx.conf` | 설정 파일 (local.conf, layer.conf, machine.conf) |

### 자주 쓰는 클래스 (inherit)

빌드 시스템에 따라 `inherit` 한 줄이면 configure/compile/install 이 자동으로 됩니다.

```bitbake
inherit cmake        # CMakeLists.txt 가 있는 프로젝트
inherit autotools    # ./configure && make 프로젝트
inherit meson        # meson 프로젝트
inherit pkgconfig    # pkg-config(.pc) 를 쓰는 프로젝트 (보통 위와 같이 씀)
```

## 3. 레이어 (Layer)

**레이어 = 관련 레시피와 설정을 모아 놓은 폴더.** 이름은 관례상 `meta-` 로 시작합니다.

```
poky/
├── bitbake/           ← 빌드 도구
├── meta/              ← OE-Core (기본 레시피)
├── meta-poky/         ← Poky 배포판 설정
├── meta-yocto-bsp/    ← 예제 보드 지원
└── ...

(따로 받는 레이어들)
meta-openembedded/
├── meta-oe/           ← Lua 등 수많은 추가 패키지
├── meta-python/
└── meta-networking/
meta-xilinx/           ← Xilinx(AMD) 보드 지원 (BSP 레이어)
meta-raspberrypi/      ← 라즈베리파이 보드 지원
meta-study/            ← ★ 내가 만드는 레이어 (이 저장소에 예제 있음)
```

레이어 종류:
- **BSP 레이어**: 특정 보드 지원 (커널, 부트로더, 디바이스 트리, MACHINE 정의)
- **배포판 레이어**: 정책 (systemd 쓸지, 어떤 기능 켤지)
- **소프트웨어 레이어**: 애플리케이션/라이브러리 레시피

> 💡 **절대 규칙: poky나 남의 레이어를 직접 수정하지 마세요.**
> 수정이 필요하면 **내 레이어**를 만들고 `.bbappend` 로 덮어씁니다.
> 그래야 나중에 버전 업데이트가 가능합니다. (MCU에서 벤더 HAL 소스를 직접 고치면 안 되는 것과 같은 이유)

어떤 레이어를 쓸지는 `build/conf/bblayers.conf` 에 등록합니다:
```bash
bitbake-layers show-layers                  # 현재 등록된 레이어 목록
bitbake-layers add-layer ../meta-study      # 레이어 추가
```

## 4. 설정: MACHINE / DISTRO / IMAGE

`build/conf/local.conf` 에서 설정합니다.

| 변수 | 의미 | 예시 |
|---|---|---|
| `MACHINE` | **어떤 보드용으로** 빌드할지 (CPU, 커널, 부트로더 결정) | `qemuarm64`, `raspberrypi4-64`, `zynq-generic` |
| `DISTRO` | **어떤 정책으로** (init 시스템, 기능 등) | `poky` (기본값) |
| 이미지 | **무엇을 넣어서** (bitbake 대상) | `core-image-minimal`, `core-image-base` |

```bash
MACHINE = "qemuarm64"           # local.conf 에 적음
bitbake core-image-minimal      # 이미지 이름은 명령줄로 지정
```

기본 제공 이미지:
- `core-image-minimal` — 부팅만 되는 최소 이미지 (실습용으로 최고, 빌드 빠름)
- `core-image-base` — minimal + 하드웨어 지원 기본 도구
- `core-image-full-cmdline` — 일반적인 리눅스 명령어 다 들어있음

## 5. 변수 문법 (자주 헷갈리는 것)

```bitbake
A = "value"         # 대입
A ?= "value"        # 아직 값이 없을 때만 대입 (기본값)
A += "value"        # 뒤에 공백 포함해서 추가   → "old value"
A:append = " value" # 뒤에 그대로 추가 (공백 직접 넣어야 함!) → "old value"
A:remove = "value"  # 제거
```

> ⚠️ **`IMAGE_INSTALL:append = " lua"` 에서 `"` 다음 공백을 빼먹는 실수가 매우 흔합니다.**
> 공백이 없으면 앞 패키지 이름과 붙어 버려서 `...busyboxlua` 같은 이상한 이름이 됩니다.
>
> 옛날 문서(2021년 이전)는 `IMAGE_INSTALL_append` (언더바) 문법을 씁니다. 지금 버전에서는 **콜론(`:`)** 을 써야 합니다.

자주 나오는 경로 변수:

| 변수 | 의미 | 값 예시 |
|---|---|---|
| `${WORKDIR}` | 레시피 작업 폴더 | `build/tmp/work/cortexa57-poky-linux/libhello/1.0/` |
| `${S}` | 소스 폴더 | |
| `${B}` | 빌드 폴더 | |
| `${D}` | 설치 대상 가짜 루트 (do_install에서 사용) | |
| `${bindir}` | 실행 파일 위치 | `/usr/bin` |
| `${libdir}` | 라이브러리 위치 | `/usr/lib` (64bit는 배포판에 따라 `/usr/lib64`도 가능) |
| `${includedir}` | 헤더 위치 | `/usr/include` |
| `${sysconfdir}` | 설정 파일 위치 | `/etc` |

→ 경로를 `/usr/lib` 처럼 하드코딩하지 말고 항상 변수를 쓰세요.

## 6. 의존성: DEPENDS vs RDEPENDS

| 변수 | 의미 | 예 |
|---|---|---|
| `DEPENDS` | **빌드할 때** 필요 (헤더, .so 링크용) | `DEPENDS = "libhello"` |
| `RDEPENDS:${PN}` | **보드에서 실행할 때** 필요 | `RDEPENDS:${PN} = "bash"` |

공유 라이브러리를 링크하면 런타임 의존성은 Yocto가 **자동으로** 잡아줍니다 (shlibs 자동 탐지).
그래서 C 라이브러리는 보통 `DEPENDS` 만 쓰면 됩니다. 스크립트(Lua, Python, shell)는 자동 탐지가 안 되므로 `RDEPENDS` 를 직접 적어야 합니다.

## 7. 버전(릴리스) 이름

Yocto 릴리스는 코드네임으로 부릅니다. **레이어끼리 버전(브랜치)을 반드시 맞춰야 합니다.**
(poky가 scarthgap이면 meta-openembedded, meta-xilinx 도 scarthgap 브랜치)

| 버전 | 코드네임 | 비고 |
|---|---|---|
| 4.0 | kirkstone | LTS (구버전, 기존 제품에서 많이 사용) |
| 5.0 | **scarthgap** | **LTS (2024.04 ~ 2028.04)** — 자료가 가장 많아서 이 노트의 기준 |
| 5.1 / 5.2 / 5.3 | styhead / walnascar / whinlatter | 단기 지원 버전 |
| 6.0 | wrynose | 차기 LTS (2026년 4월 예정이었음 — 상태는 공식 페이지 확인) |

👉 최신 상태는 항상 https://wiki.yoctoproject.org/wiki/Releases 에서 확인하세요.
👉 **보드 BSP 레이어(meta-xilinx 등)가 지원하는 버전**에 맞추는 게 가장 중요합니다.

## 8. 빌드 결과물 위치

```
build/
├── conf/
│   ├── local.conf          ← MACHINE 등 내 설정
│   └── bblayers.conf       ← 사용할 레이어 목록
├── downloads/              ← 받은 소스 (다른 빌드와 공유 가능)
├── sstate-cache/           ← 빌드 캐시 (두 번째 빌드부터 빨라지는 이유)
└── tmp/
    ├── work/<아키텍처>/<레시피>/<버전>/   ← 레시피별 작업 폴더 (에러 로그는 여기 temp/ 에)
    └── deploy/
        ├── images/<MACHINE>/          ★ 최종 이미지 (보드에 쓸 파일)
        ├── ipk/ 또는 rpm/             패키지 파일
        └── sdk/                       SDK 설치 파일
```

---

## PetaLinux와의 관계

```
┌──────────────────────────────────────┐
│ PetaLinux 명령어                      │  petalinux-create / -config / -build / -package
│ (Xilinx가 만든 편의 래퍼 스크립트)       │  + 메뉴 기반 설정 (menuconfig)
├──────────────────────────────────────┤
│ meta-xilinx, meta-xilinx-tools,       │  Xilinx 칩/보드 지원 레이어
│ meta-petalinux 등                     │
├──────────────────────────────────────┤
│ Yocto (Poky / OE-Core / BitBake)      │  ← 실제로 빌드하는 엔진
└──────────────────────────────────────┘
```

| PetaLinux 명령 | 뒤에서 실제로 하는 일 (Yocto) |
|---|---|
| `petalinux-create -t project` | 빌드 폴더 + 레이어 구성 생성 |
| `petalinux-config --get-hw-description` | Vivado의 `.xsa` 를 읽어 디바이스 트리/MACHINE 설정 생성 |
| `petalinux-config -c rootfs` | `IMAGE_INSTALL` 에 패키지 추가 (메뉴로) |
| `petalinux-create -t apps` | `project-spec/meta-user/` 레이어에 레시피 생성 |
| `petalinux-build` | `bitbake petalinux-image-minimal` |
| `petalinux-package --boot` | `BOOT.BIN` 생성 (FSBL + 비트스트림 + U-Boot) |

즉, PetaLinux 프로젝트의 `project-spec/meta-user/` 는 **그냥 Yocto 레이어**이고, 거기에 레시피를 넣는 방법은 이 노트에서 배우는 것과 똑같습니다.

> 📌 **참고:** AMD(Xilinx)는 최근 PetaLinux 툴을 단계적으로 정리하고, 표준 Yocto 방식(meta-xilinx + `gen-machine-conf` 등)과
> 새 개발 프레임워크(AMD Embedded Development Framework, EDF)로 옮겨가는 방향을 발표했습니다.
> 구체적인 지원 일정은 AMD 공식 문서/릴리스 노트에서 확인하세요.
> 어느 쪽이든 **Yocto 자체를 아는 것이 가장 오래 쓸 수 있는 지식**입니다.

---

## ✅ 2단계 자가 점검

1. Poky, BitBake, OE-Core 의 관계를 한 문장으로 설명할 수 있다.
2. 레시피의 do_fetch → do_install 흐름을 말할 수 있다.
3. `.bb` 와 `.bbappend` 의 차이, 그리고 왜 원본을 고치면 안 되는지 안다.
4. `MACHINE` 과 이미지 이름이 각각 무엇을 결정하는지 안다.
5. `DEPENDS` 와 `RDEPENDS` 의 차이를 안다.
6. `IMAGE_INSTALL:append = " lua"` 에서 공백이 왜 필요한지 안다.
