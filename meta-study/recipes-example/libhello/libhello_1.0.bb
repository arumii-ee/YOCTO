SUMMARY = "Example shared library for Yocto study"
DESCRIPTION = "hello_say(), hello_add() 를 제공하는 예제 공유 라이브러리"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# 소스: 이 레시피 옆 files/ 폴더에서 가져온다
SRC_URI = "file://hello.c \
           file://hello.h \
           file://Makefile \
          "

# scarthgap(5.0) 기준. styhead(5.1) 이후 버전에서는 S = "${UNPACKDIR}" 로 바꿀 것
S = "${WORKDIR}"

do_compile() {
    oe_runmake
}

do_install() {
    oe_runmake install DESTDIR=${D} LIBDIR=${libdir} INCDIR=${includedir}
}

# 결과 패키지 (자동 분할):
#   libhello      -> /usr/lib/libhello.so.1.0.0, /usr/lib/libhello.so.1   (보드 실행용)
#   libhello-dev  -> /usr/lib/libhello.so, /usr/include/hello.h          (개발/SDK용)
