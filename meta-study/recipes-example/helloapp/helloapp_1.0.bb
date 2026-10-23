SUMMARY = "Example application using libhello"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://main.c"

# scarthgap(5.0) 기준. styhead(5.1) 이후 버전에서는 S = "${UNPACKDIR}" 로 바꿀 것
S = "${WORKDIR}"

# 빌드할 때 libhello 의 헤더와 .so 가 필요하다
# (보드에서 실행할 때 필요한 libhello 패키지는 Yocto가 자동으로 의존성 추가)
DEPENDS = "libhello"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} main.c -o helloapp -lhello
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 helloapp ${D}${bindir}/
}
