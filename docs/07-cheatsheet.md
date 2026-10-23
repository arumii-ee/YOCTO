# 부록: Yocto 치트시트

## 매일 쓰는 명령

```bash
source poky/oe-init-build-env build      # 새 터미널마다 필수

bitbake <이미지 또는 레시피>                # 빌드
bitbake -c <태스크> <레시피>                # 특정 태스크만 (fetch, compile, install, clean ...)
bitbake -c cleansstate <레시피>            # 캐시까지 지우고 처음부터 다시
bitbake -c listtasks <레시피>              # 실행 가능한 태스크 목록
bitbake -c devshell <레시피>               # 크로스 컴파일 환경이 설정된 셸 열기 (디버깅에 최고)
bitbake -c menuconfig virtual/kernel       # 커널 설정 메뉴
bitbake -e <레시피> | grep ^변수=           # 변수 최종값 확인 (★ 디버깅 1순위)
bitbake -g <이미지>                         # 의존성 그래프 생성 (task-depends.dot)
bitbake <이미지> -c populate_sdk           # SDK 생성
bitbake -k <이미지>                         # 에러 나도 가능한 건 계속 빌드

bitbake-layers show-layers                 # 레이어 목록
bitbake-layers add-layer <경로>            # 레이어 추가
bitbake-layers remove-layer <경로>         # 레이어 제거
bitbake-layers create-layer <경로>         # 새 레이어 뼈대 생성
bitbake-layers show-recipes "<패턴>"       # 레시피 검색
bitbake-layers show-appends                # 어떤 bbappend 가 적용되는지

oe-pkgdata-util list-pkgs -p <레시피>      # 레시피가 만든 패키지 목록
oe-pkgdata-util list-pkg-files <패키지>    # 패키지에 들어간 파일
oe-pkgdata-util find-path /usr/lib/libz.so.1   # 이 파일은 어느 패키지에서 왔나

devtool add <이름> <소스URL>               # 레시피 자동 생성
devtool modify <레시피>                    # 기존 레시피 소스 수정 모드
devtool build <레시피>
devtool finish <레시피> <레이어경로>

runqemu <MACHINE> [이미지] nographic       # QEMU 실행 (종료: Ctrl+A, X)
```

## 레시피 템플릿

```bitbake
SUMMARY = ""
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://..."                       # 또는 git://...;protocol=https;branch=main
# SRCREV = "<commit>"                        # git 일 때
S = "${WORKDIR}"                             # git 이면 "${WORKDIR}/git"  (5.1+ 로컬파일이면 "${UNPACKDIR}")

DEPENDS = ""                                 # 빌드 의존성
RDEPENDS:${PN} = ""                          # 실행 의존성 (스크립트 등 자동 탐지 안 될 때)

inherit cmake                                # 또는 autotools / meson / pkgconfig / 없음

EXTRA_OECMAKE = ""                           # cmake 옵션
EXTRA_OEMAKE = ""                            # make 옵션

do_install:append() {                        # 기본 install 뒤에 추가 작업
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/my.conf ${D}${sysconfdir}/
}

FILES:${PN} += "${datadir}/myapp"            # 기본 규칙에 안 걸리는 파일을 패키지에 포함
```

## bbappend 템플릿 (남의 레시피 수정)

```bitbake
# meta-study/recipes-xxx/busybox/busybox_%.bbappend   (% = 아무 버전)
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"     # 내 files/ 폴더도 찾게
SRC_URI += "file://my-fix.patch"
```

## 변수 연산자

| 문법 | 의미 |
|---|---|
| `A = "x"` | 대입 |
| `A ?= "x"` | 값 없을 때만 |
| `A ??= "x"` | 가장 약한 기본값 |
| `A := "x"` | 즉시 확장 |
| `A += "x"` | 공백 + 뒤에 추가 |
| `A =+ "x"` | 앞에 추가 + 공백 |
| `A:append = " x"` | 뒤에 추가 (공백 직접!) — 마지막에 적용됨 |
| `A:prepend = "x "` | 앞에 추가 |
| `A:remove = "x"` | 제거 |
| `A:${PN}` | 패키지별 변수 (`RDEPENDS:${PN}`, `FILES:${PN}`) |

## 경로 변수

| 변수 | 값 |
|---|---|
| `${PN}` / `${PV}` | 레시피 이름 / 버전 (`libhello_1.0.bb` → `libhello` / `1.0`) |
| `${WORKDIR}` `${S}` `${B}` `${D}` | 작업 / 소스 / 빌드 / 설치 폴더 |
| `${bindir}` | `/usr/bin` |
| `${sbindir}` | `/usr/sbin` |
| `${libdir}` | `/usr/lib` |
| `${includedir}` | `/usr/include` |
| `${sysconfdir}` | `/etc` |
| `${datadir}` | `/usr/share` |
| `${THISDIR}` | 현재 레시피 파일이 있는 폴더 |

## local.conf 에 자주 넣는 것

```bitbake
MACHINE = "qemuarm64"
INHERIT += "rm_work"                               # 디스크 절약
BB_NUMBER_THREADS = "8"                            # 동시 태스크 수
PARALLEL_MAKE = "-j 8"                             # make -j
DL_DIR = "${HOME}/yocto/downloads"                 # 다운로드를 여러 빌드에서 공유
SSTATE_DIR = "${HOME}/yocto/sstate-cache"          # 캐시를 여러 빌드에서 공유
IMAGE_INSTALL:append = " lua ldd"                  # 테스트용 패키지 추가
EXTRA_IMAGE_FEATURES += "tools-debug ssh-server-dropbear"   # gdb, ssh 서버
IMAGE_ROOTFS_EXTRA_SPACE = "524288"                # rootfs 여유 공간 (KB)
```

## 에러 대응 순서

1. 화면 끝의 `ERROR:` 줄에서 **레시피 이름 + 태스크** 확인
2. `Logfile of failure stored in:` 경로의 **로그 열기**
3. 로그에서 **첫 번째** `error:` 찾기 (마지막 거 말고)
4. 변수 문제 같으면 `bitbake -e <레시피> | grep ^변수=`
5. 소스/환경 문제 같으면 `bitbake -c devshell <레시피>` 로 들어가서 직접 make 해보기
6. 캐시 문제 같으면 `bitbake -c cleansstate <레시피>` 후 재빌드
7. 에러 메시지 그대로 검색 (+ "yocto" + 버전 이름)

## 용어집

| 용어 | 설명 |
|---|---|
| BSP | Board Support Package. 보드 지원 레이어 |
| sysroot | 크로스 컴파일 시 사용하는 타겟용 헤더/라이브러리 모음 |
| sstate | 태스크 결과 캐시. 재빌드를 빠르게 함 |
| native | 호스트(PC)용으로 빌드되는 도구 (`cmake-native` 등) |
| virtual/kernel | "이 MACHINE 이 쓰는 커널" 을 가리키는 별칭 |
| wic | 파티션까지 포함된 디스크 이미지 형식 (SD카드에 바로 씀) |
| ipk / rpm / deb | 패키지 형식. Poky 기본은 rpm (`PACKAGE_CLASSES` 로 변경) |
| FSBL | Xilinx 1차 부트로더 (First Stage Boot Loader) |
| XSA | Vivado 가 내보내는 하드웨어 설명 파일 |
