# 5단계 실습: 실제 보드에 올리기

QEMU에서 되던 것을 실제 보드로 옮깁니다. 핵심은 딱 하나입니다:

> **보드용 BSP 레이어를 추가하고 `MACHINE` 을 바꾼다.**
> 내 레이어(meta-study)와 레시피는 **하나도 바꿀 필요가 없습니다.**
> (아키텍처가 바뀌어도 Yocto가 그 보드용 크로스 컴파일러로 다시 빌드해 줌)

---

## 1. 일반적인 순서

```
1. 보드의 BSP 레이어 찾기          (layers.openembedded.org 에서 MACHINE 검색, 또는 칩 벤더 GitHub)
2. poky 와 같은 브랜치로 clone
3. bitbake-layers add-layer
4. local.conf 에 MACHINE = "<보드이름>"
5. bitbake <이미지>
6. tmp/deploy/images/<보드이름>/ 의 이미지를 SD카드/eMMC/플래시에 기록
7. 시리얼 콘솔(USB-UART, 115200bps)로 부팅 로그 확인
```

BSP 레이어 안의 `conf/machine/*.conf` 파일 이름이 사용 가능한 `MACHINE` 이름입니다:
```bash
ls ../meta-raspberrypi/conf/machine/
```

시리얼 콘솔 프로그램 (PC):
```bash
sudo apt install picocom
sudo picocom -b 115200 /dev/ttyUSB0      # 종료: Ctrl+A, Ctrl+X
```
Windows 라면 PuTTY / Tera Term 사용.

---

## 2. 연습용 추천: 라즈베리파이 4

자료가 가장 많고, SD카드만 쓰면 돼서 실패해도 부담이 없습니다.

```bash
cd ~/yocto
git clone -b scarthgap https://git.yoctoproject.org/meta-raspberrypi
cd build
bitbake-layers add-layer ../meta-raspberrypi
```

`conf/local.conf`:
```bitbake
MACHINE = "raspberrypi4-64"
ENABLE_UART = "1"                    # 시리얼 콘솔 사용 (GPIO14/15)
```

```bash
bitbake study-image
ls tmp/deploy/images/raspberrypi4-64/*.wic.bz2
```

SD카드에 쓰기 (**`/dev/sdX` 를 반드시 정확히 확인! 잘못 쓰면 PC 디스크가 날아갑니다**):
```bash
lsblk                                   # SD카드 장치 이름 확인 (용량으로 구분)
sudo apt install bmap-tools
sudo bmaptool copy tmp/deploy/images/raspberrypi4-64/study-image-raspberrypi4-64.rootfs.wic.bz2 /dev/sdX
```
Windows 라면 `.wic.bz2` 를 풀어서 balenaEtcher / Raspberry Pi Imager 로 써도 됩니다.

부팅 후 `helloapp` 실행 → QEMU와 똑같이 나오면 성공!

---

## 3. Xilinx (AMD) Zynq / ZynqMP 보드

Xilinx 보드는 **FPGA 하드웨어 설계(Vivado)** 가 먼저 있어야 한다는 점이 다릅니다.

```
Vivado 하드웨어 설계
   → .xsa 파일 export (어떤 IP가 어떤 주소에 있는지 + 비트스트림)
       → Yocto(meta-xilinx)가 .xsa 를 읽어서 디바이스 트리 / MACHINE 설정 생성
           → 부트로더(FSBL, U-Boot) + 커널 + rootfs 빌드
               → BOOT.BIN + image.ub (또는 wic 이미지) 를 SD카드에
```

### 방법 1: PetaLinux 를 이미 쓰고 있다면 (기존 프로젝트에 라이브러리 추가)

PetaLinux 프로젝트의 `project-spec/meta-user/` 는 그냥 Yocto 레이어입니다.
**meta-study 의 레시피 폴더를 그대로 복사**하면 됩니다.

```bash
cd <petalinux-project>
cp -r <yocto-study>/meta-study/recipes-example project-spec/meta-user/

# rootfs 메뉴에 항목 추가
echo "CONFIG_libhello" >> project-spec/meta-user/conf/user-rootfsconfig
echo "CONFIG_helloapp" >> project-spec/meta-user/conf/user-rootfsconfig

petalinux-config -c rootfs     # → user packages 에서 libhello, helloapp 선택 → 저장
petalinux-build
petalinux-package --boot --fsbl images/linux/zynq_fsbl.elf --fpga --u-boot   # (Zynq-7000 예시)
```

> 레시피의 `LAYERSERIES_COMPAT` 는 레시피가 아니라 layer.conf 에 있는 것이라서, 레시피 폴더만 복사할 땐 신경 쓸 필요 없습니다.
> 단, 해당 PetaLinux 버전의 Yocto 버전에 따라 `S = "${WORKDIR}"` 부분은 맞춰야 할 수 있습니다.

PetaLinux 버전마다 기반 Yocto 버전이 다릅니다. 내 PetaLinux 가 어떤 Yocto 버전인지는
해당 버전의 사용자 가이드(UG1144) 또는 프로젝트의 `components/yocto/layers/` 안의 레이어 브랜치/`LAYERSERIES_COMPAT` 로 확인하세요.

### 방법 2: 순수 Yocto + meta-xilinx

AMD는 레이어 조합을 맞춰주는 manifest 를 제공합니다 (`repo` 도구 사용).

```bash
# repo 도구 설치 (Google 의 다중 git 관리 도구)
sudo apt install repo

mkdir ~/xilinx-yocto && cd ~/xilinx-yocto
repo init -u https://github.com/Xilinx/yocto-manifests.git -b <릴리스 브랜치, 예: rel-v2024.2>
repo sync
source setupsdk          # (manifest 의 안내에 따름)
```

그 후 `.xsa` 로 MACHINE 설정을 생성하고 빌드하는 절차는 버전마다 조금씩 다르므로,
**사용하는 릴리스의 meta-xilinx README** 를 따라가세요:
https://github.com/Xilinx/meta-xilinx (브랜치를 사용 버전으로 바꿔서 볼 것)

> 💡 **권장 학습 순서:** QEMU → 라즈베리파이 → Xilinx.
> Xilinx 는 Vivado/FSBL/비트스트림까지 얽혀 있어서 Yocto 개념이 안 잡힌 상태로 시작하면 어디서 문제인지 찾기 어렵습니다.
> 하지만 이미 PetaLinux 프로젝트가 있다면 **방법 1** 로 바로 라이브러리 추가를 연습해도 좋습니다.

---

## 4. 회사 커스텀 보드라면?

벤더 평가보드(EVK)의 BSP 레이어를 기반으로 **내 BSP 레이어**를 만드는 것이 일반적입니다.

```
meta-mycompany-bsp/
├── conf/machine/myboard.conf                # require 로 평가보드 machine 설정을 가져와서 일부만 변경
├── recipes-kernel/linux/
│   ├── linux-xxx_%.bbappend                 # 커널 레시피에 덧붙이기
│   └── files/myboard.dts                    # 내 보드 디바이스 트리
└── recipes-bsp/u-boot/
    └── u-boot-xxx_%.bbappend
```

이 부분은 범위가 넓으니 1~4단계를 마친 다음 공부하세요. 키워드: *"Yocto custom machine"*, *"bbappend device tree"*, *"devtool modify linux"*.

---

## ✅ 5단계 자가 점검

1. 보드의 BSP 레이어를 찾아서 poky 와 같은 브랜치로 받았다.
2. `MACHINE` 만 바꾸고 meta-study 는 그대로 둔 채 빌드에 성공했다.
3. 이미지를 SD카드에 쓰고 시리얼 콘솔로 부팅 로그를 봤다.
4. 보드에서 `helloapp` 실행, `ldd` 로 libhello 연결 확인.
5. `file` 명령으로 QEMU 빌드와 보드 빌드의 helloapp 아키텍처 차이를 확인했다 (예: aarch64 vs ARM 32bit).
