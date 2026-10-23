# 3단계 실습: 첫 이미지 빌드 → QEMU에서 부팅

보드 없이 PC에서 **가상 ARM64 보드(QEMU)** 용 리눅스를 빌드하고 부팅해 봅니다.
여기까지 성공하면 Yocto의 50%는 끝난 것입니다.

> 기준: **Ubuntu 22.04 / 24.04 + Yocto 5.0 (scarthgap)**

---

## 1. 호스트 패키지 설치

```bash
sudo apt update
sudo apt install -y build-essential chrpath cpio debianutils diffstat file gawk gcc \
    git iputils-ping libacl1 liblz4-tool locales python3 python3-git python3-jinja2 \
    python3-pexpect python3-pip python3-subunit socat texinfo unzip wget xz-utils zstd

# 로케일 설정 (없으면 빌드 시작 시 에러)
sudo locale-gen en_US.UTF-8
```

### Ubuntu 24.04 사용자만: AppArmor 설정

24.04에서는 아래 에러가 날 수 있습니다:
```
ERROR: User namespaces are not usable by BitBake, possibly due to AppArmor.
```
해결 (재부팅하면 원복되므로, 영구 적용은 `/etc/sysctl.d/` 에 파일로 저장):
```bash
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
# 영구 적용
echo 'kernel.apparmor_restrict_unprivileged_userns = 0' | sudo tee /etc/sysctl.d/60-yocto.conf
```

## 2. 작업 폴더 만들고 Poky 받기

```bash
mkdir -p ~/yocto && cd ~/yocto
git clone -b scarthgap https://git.yoctoproject.org/poky
```

> 경로에 **한글이나 공백이 없게** 하세요. (`~/바탕화면/yocto` ❌)
> WSL2라면 `/mnt/c/...` 가 아니라 `~/yocto` 같은 리눅스 내부 경로를 써야 합니다 (훨씬 빠르고 에러 없음).

## 3. 빌드 환경 초기화

```bash
cd ~/yocto
source poky/oe-init-build-env build
```

- `build` 폴더가 생기고 그 안으로 이동합니다.
- `bitbake` 명령을 쓸 수 있게 됩니다.
- **터미널을 새로 열 때마다 이 명령을 다시 실행해야 합니다.** (이미 있는 build 폴더는 설정이 유지됨)

## 4. local.conf 수정

`~/yocto/build/conf/local.conf` 를 편집기로 열어서:

```bitbake
# 1) MACHINE 변경: 기본값은 qemux86-64. 실제 보드와 비슷하게 ARM64로 연습
MACHINE ?= "qemuarm64"

# 2) (권장) 디스크 절약: 빌드 끝난 작업 폴더를 지움
INHERIT += "rm_work"

# 3) (메모리 16GB 미만일 때) 동시 작업 수 제한 — 메모리 부족으로 컴파일러가 죽는 걸 방지
# BB_NUMBER_THREADS = "4"
# PARALLEL_MAKE = "-j 4"
```

> 파일 안에 `MACHINE ?= "qemux86-64"` 줄이 이미 있습니다. 그 줄을 고치거나 파일 맨 끝에 추가하세요.

## 5. 빌드!

```bash
bitbake core-image-minimal
```

- 첫 빌드는 **몇 시간** 걸립니다 (컴파일러부터 전부 소스로 빌드하기 때문).
- 화면에 `Currently N running tasks (1234 of 4567)` 처럼 진행 상황이 나옵니다.
- 중간에 멈춰도(Ctrl+C) 다시 같은 명령을 치면 **이어서** 빌드됩니다.
- 두 번째부터는 `sstate-cache` 덕분에 몇 분 만에 끝납니다.

빌드가 끝나면 결과물 확인:
```bash
ls tmp/deploy/images/qemuarm64/
# core-image-minimal-qemuarm64.rootfs.ext4  ← 루트파일시스템
# Image                                     ← 커널
# ...
```

## 6. QEMU로 부팅

```bash
runqemu qemuarm64 nographic
```

- `nographic` = 별도 창 없이 현재 터미널을 시리얼 콘솔처럼 사용
- 부팅 로그가 쭉 나오고 `qemuarm64 login:` 이 뜨면 **`root`** 입력 (비밀번호 없음)

```bash
root@qemuarm64:~# uname -a       # aarch64 확인!
root@qemuarm64:~# cat /etc/os-release
root@qemuarm64:~# ls /usr/lib
root@qemuarm64:~# poweroff       # 종료
```

QEMU가 응답이 없으면 **`Ctrl+A` 누르고 뗀 다음 `X`** 를 누르면 강제 종료됩니다.

> 🎉 축하합니다! 소스에서 직접 만든 ARM64 리눅스를 부팅했습니다.

## 7. 빌드 중 에러가 났다면

1. **에러 메시지에서 레시피 이름과 태스크를 찾습니다.**
   ```
   ERROR: libfoo-1.0-r0 do_compile: oe_runmake failed
   ERROR: Logfile of failure stored in: /home/.../tmp/work/.../libfoo/1.0/temp/log.do_compile.12345
   ```
2. **Logfile 경로의 로그를 엽니다.** 진짜 원인은 거기 있습니다.
   ```bash
   less /home/.../temp/log.do_compile.12345
   ```
3. 네트워크 에러(do_fetch 실패)는 그냥 다시 `bitbake` 하면 되는 경우가 많습니다.
4. 컴파일러가 갑자기 죽음(`Killed`, `signal 9`) → 메모리 부족. 위 4번의 `BB_NUMBER_THREADS` 를 줄이세요.
5. 디스크 부족 → `df -h` 확인. `rm_work` 사용.

## 8. 유용한 확인 명령 (지금 한번씩 쳐보기)

```bash
bitbake-layers show-layers                     # 사용 중인 레이어
bitbake-layers show-recipes | less             # 사용 가능한 모든 레시피
bitbake-layers show-recipes "busybox*"         # 특정 레시피 검색
bitbake -e core-image-minimal | grep ^IMAGE_INSTALL=   # 최종 IMAGE_INSTALL 값
bitbake -e busybox | grep ^WORKDIR=            # busybox 작업 폴더 위치
```

---

## ✅ 3단계 자가 점검

1. `core-image-minimal` 을 빌드하고 `runqemu` 로 로그인했다.
2. 보드 안에서 `uname -m` 결과가 `aarch64` 인 것을 확인했다.
3. 새 터미널을 열고 `source poky/oe-init-build-env build` 후 `bitbake` 가 다시 되는 것을 확인했다.
4. `tmp/deploy/images/qemuarm64/` 안의 파일들이 무엇인지 대략 안다.
5. 빌드 에러가 나면 어떤 로그 파일을 열어야 하는지 안다.
