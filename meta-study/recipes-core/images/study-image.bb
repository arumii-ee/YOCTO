SUMMARY = "Study image: core-image-minimal + libhello + helloapp + lua"

# core-image-minimal 을 그대로 가져와서
require recipes-core/images/core-image-minimal.bb

# 원하는 패키지를 추가 (앞에 공백 필수!)
# lua 는 meta-openembedded/meta-oe 레이어가 필요하다
IMAGE_INSTALL:append = " libhello helloapp lua"
