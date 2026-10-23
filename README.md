# Yocto 공부 노트 (완전 초보 → 내 라이브러리를 보드에 올리기)

## 이 저장소의 목표

> **"리눅스에 원하는 라이브러리를 추가해서, 내 기기(보드)에 올라가는 리눅스 이미지를 직접 만든다."**

이 목표를 기준으로, 꼭 필요한 것만 순서대로 정리했습니다.
MCU 펌웨어(C), Lua, Pascal 경험이 있는 임베디드 개발자를 기준으로, MCU 개발과 비교하면서 설명합니다.

## 한 줄 요약: Yocto가 뭔가?

**Yocto는 "내 보드 전용 리눅스 배포판을 만드는 빌드 시스템"입니다.**

MCU 개발로 비유하면:

| MCU 개발 | Yocto (임베디드 리눅스) |
|---|---|
| 소스 + Makefile → `firmware.bin` 한 개 | 수백 개 패키지 소스 + 레시피 → **부트로더 + 커널 + 루트파일시스템 이미지** |
| 벤더 SDK(STM32Cube 등)가 칩 지원 | **BSP 레이어**(meta-xilinx, meta-raspberrypi 등)가 보드 지원 |
| 라이브러리 추가 = 소스 복사 + Makefile 수정 | 라이브러리 추가 = **레시피(.bb) 작성 + 이미지에 포함** |
| 크로스 컴파일러(arm-none-eabi-gcc) 직접 설치 | Yocto가 **크로스 컴파일러까지 직접 만들어 줌** |

그리고 **PetaLinux = Xilinx(AMD)가 Yocto 위에 씌운 편의 도구(래퍼)** 입니다.
`petalinux-build` 명령 뒤에서는 결국 Yocto의 `bitbake`가 돌아갑니다. 그래서 Yocto를 이해하면 PetaLinux도 저절로 이해됩니다.
(자세한 내용: [docs/03-yocto-concepts.md](docs/03-yocto-concepts.md#petalinux와의-관계))

## 학습 로드맵

각 단계는 이전 단계를 전제로 합니다. **순서대로** 진행하세요.

| 단계 | 문서 | 내용 | 예상 기간 |
|---|---|---|---|
| 0 | [docs/01-linux-basics.md](docs/01-linux-basics.md) | Yocto를 돌리기 위한 리눅스 PC(호스트) 사용법 | 3~5일 |
| 1 | [docs/02-embedded-linux-concepts.md](docs/02-embedded-linux-concepts.md) | 임베디드 리눅스 구조 (부트로더/커널/디바이스트리/rootfs) | 2~3일 |
| 2 | [docs/03-yocto-concepts.md](docs/03-yocto-concepts.md) | Yocto 핵심 개념 (Poky, BitBake, 레시피, 레이어) | 2~3일 |
| 3 | [docs/04-first-build-qemu.md](docs/04-first-build-qemu.md) | **실습:** 첫 이미지 빌드 → QEMU(가상 보드)에서 부팅 | 1~2일 (빌드 대기 포함) |
| 4 | [docs/05-add-library.md](docs/05-add-library.md) | **실습:** 기존 라이브러리 추가 + 내 C 라이브러리 레시피 작성 | 1주 |
| 5 | [docs/06-porting-to-board.md](docs/06-porting-to-board.md) | **실습:** 실제 보드(라즈베리파이 / Xilinx Zynq)에 올리기 | 1~2주 |
| 부록 | [docs/07-cheatsheet.md](docs/07-cheatsheet.md) | 명령어/변수 치트시트, 자주 나는 에러 | 수시로 참고 |

> 💡 **팁:** 4단계까지는 실제 보드 없이 PC만으로 전부 가능합니다(QEMU 사용).
> 보드부터 잡으면 "Yocto 문제인지, 보드 문제인지" 구분이 안 돼서 훨씬 어렵습니다.

## 실습용 예제 레이어: `meta-study/`

5단계 문서에서 사용하는 예제가 들어 있습니다. 그대로 복사해서 빌드해 볼 수 있습니다.

```
meta-study/
├── conf/layer.conf                         # "이 폴더는 Yocto 레이어다" 선언
├── recipes-example/
│   ├── libhello/                           # 내가 만든 C 공유 라이브러리 (libhello.so)
│   │   ├── libhello_1.0.bb                 #   ← 레시피 (빌드 방법 설명서)
│   │   └── files/ (hello.c, hello.h, Makefile)
│   └── helloapp/                           # libhello를 사용하는 프로그램
│       ├── helloapp_1.0.bb
│       └── files/main.c
└── recipes-core/images/
    └── study-image.bb                      # 위 두 개 + Lua를 포함한 나만의 이미지
```

## 준비물

- **리눅스 PC** (Ubuntu 22.04 또는 24.04 LTS 권장)
  - Windows만 있다면: WSL2 + Ubuntu 로도 가능 (단, 디스크는 WSL 내부 경로 사용)
  - 가상머신(VirtualBox 등)도 가능하지만 느림
- **디스크 여유 공간 100GB 이상** (빌드 한 번에 50~90GB 사용)
- **RAM 16GB 이상 권장** (8GB면 가능하지만 매우 느리고 메모리 부족 에러 가능)
- **CPU 코어 많을수록 좋음** — 첫 빌드는 4코어 기준 3~6시간, 16코어면 1시간 내외
- 인터넷 (소스 수 GB 다운로드)

## 참고 자료 (공식)

- Yocto Project 공식 문서: https://docs.yoctoproject.org/
  - 처음이면: *Quick Build* → *Overview and Concepts Manual* → *Development Tasks Manual* 순서
- 릴리스 목록 (어떤 버전이 LTS인지): https://wiki.yoctoproject.org/wiki/Releases
- 레이어/레시피 검색: https://layers.openembedded.org/
- Bootlin 무료 교육 자료 (Yocto 슬라이드+실습, 강력 추천): https://bootlin.com/training/yocto/
- AMD/Xilinx Yocto 레이어: https://github.com/Xilinx/meta-xilinx
