# 0단계: Yocto를 위한 리눅스 기초

Yocto는 **리눅스 PC(호스트)** 에서 실행합니다. 보드가 아니라 PC에서요.
그래서 "보드에서 `ifconfig`, `top` 치는 것"과는 조금 다른, **PC 터미널에서 파일 다루고 프로그램 설치하는 능력**이 필요합니다.

여기 나온 것만 알면 Yocto 실습은 충분합니다. 전부 외울 필요는 없고, 직접 쳐 보면서 손에 익히세요.

---

## 1. 용어 정리

| 용어 | 의미 |
|---|---|
| **호스트(Host)** | Yocto를 실행하는 PC (보통 x86_64 Ubuntu) |
| **타겟(Target)** | 만든 이미지가 올라갈 보드 (ARM 등) |
| **셸(Shell)** | 명령어를 입력받는 프로그램. 보통 `bash` |
| **터미널** | 셸을 띄워주는 창 |
| **루트(root)** | 관리자 계정. `sudo` 를 앞에 붙이면 관리자 권한으로 실행 |
| **패키지 관리자** | 프로그램 설치 도구. Ubuntu는 `apt` |

---

## 2. 디렉터리(폴더) 이동과 파일 보기

```bash
pwd                 # 지금 내가 어느 폴더에 있는지 출력
ls                  # 현재 폴더 파일 목록
ls -l               # 자세히 (권한, 크기, 날짜)
ls -la              # 숨김 파일(.으로 시작)까지
cd /home/user       # 절대 경로로 이동
cd poky             # 현재 위치 기준 하위 폴더로 이동
cd ..               # 상위 폴더로
cd ~                # 내 홈 폴더로 (~ = /home/내아이디)
cd -                # 직전에 있던 폴더로
```

**리눅스 폴더 구조 (보드에서도 똑같음!)**

```
/            최상위 (윈도우의 C:\ 같은 것)
├── bin, usr/bin   실행 파일 (ls, cat ...)
├── lib, usr/lib   라이브러리 (.so 파일)  ← 내가 추가할 라이브러리가 여기 들어감
├── usr/include    헤더 파일 (.h)
├── etc            설정 파일
├── home           사용자 폴더
├── dev            장치 파일 (/dev/ttyS0 같은 시리얼 포트)
├── proc, sys      커널 정보 (가상 파일)
└── tmp            임시 파일
```

## 3. 파일 다루기

```bash
cat file.txt            # 파일 내용 전체 출력
less file.txt           # 긴 파일을 페이지 단위로 보기 (q로 종료, /검색어 로 검색)
head -n 20 file.txt     # 앞 20줄
tail -n 20 file.txt     # 뒤 20줄
tail -f log.txt         # 파일 끝을 계속 지켜보기 (빌드 로그 볼 때 유용)

cp a.txt b.txt          # 복사
cp -r dirA dirB         # 폴더 통째로 복사
mv a.txt b.txt          # 이동 / 이름 변경
rm a.txt                # 삭제 (휴지통 없음! 주의)
rm -r dirA              # 폴더 삭제
mkdir -p a/b/c          # 폴더 생성 (중간 폴더까지)
```

## 4. 텍스트 편집기

레시피 파일(.bb)이나 설정 파일(local.conf)을 수정해야 합니다.

- **추천: VS Code** — GUI가 편하면 이게 제일 쉬움. `code .` 로 현재 폴더 열기
  (WSL이면 Windows의 VS Code + "WSL" 확장으로 바로 연결됨)
- **nano** — 터미널용 쉬운 편집기. `nano local.conf` → 수정 → `Ctrl+O`(저장) `Enter` → `Ctrl+X`(종료)
- **vi / vim** — 보드에는 보통 vi만 있으므로 최소한만 알아두기:
  `i`(입력 모드) → 수정 → `Esc` → `:wq` `Enter` (저장 후 종료) / `:q!` (저장 안 하고 종료)

## 5. 검색 (Yocto에서 엄청 자주 씀)

```bash
find . -name "*.bb"                 # 현재 폴더 아래에서 .bb 파일 찾기
find . -name "lua*"                 # 이름이 lua로 시작하는 파일
grep -r "IMAGE_INSTALL" conf/       # conf 폴더 안에서 문자열 검색
grep -rn "SRC_URI" .                # 줄 번호와 함께 (-n)
grep -ri "error" log.do_compile     # 대소문자 무시 (-i)
```

## 6. 프로그램 설치 (apt)

```bash
sudo apt update                     # 설치 가능한 목록 갱신 (설치 전에 항상)
sudo apt install git                # 설치
sudo apt install -y git make gcc    # 여러 개 한 번에, -y = 묻지 않고 예
apt search lua                      # 패키지 검색
```

> 이건 **호스트 PC에 설치**하는 것입니다. 보드에 라이브러리를 넣는 건 Yocto 레시피로 합니다. 헷갈리지 마세요!

## 7. 권한과 sudo

```bash
ls -l
# -rwxr-xr-x 1 user user 1234 Jan 1 12:00 build.sh
#  ^^^ ^^^ ^^^
#  소유자 그룹 기타     r=읽기 w=쓰기 x=실행

chmod +x build.sh       # 실행 권한 주기
./build.sh              # 현재 폴더의 스크립트 실행 (./ 필수)
sudo 명령               # 관리자 권한으로 실행
```

> ⚠️ **Yocto 빌드(bitbake)는 절대 sudo로 실행하지 마세요.** 일반 사용자로 실행해야 하며, root로 하면 에러가 납니다.

## 8. 환경 변수와 `source`

Yocto에서 반드시 이해해야 하는 개념입니다.

```bash
echo $PATH              # 실행 파일을 찾는 경로 목록
echo $HOME              # 홈 폴더
export MYVAR=hello      # 환경 변수 설정 (현재 터미널에서만 유효)
```

- `./script.sh` → 스크립트가 **새 셸**에서 실행되고 끝나면 설정이 사라짐
- `source script.sh` → **현재 셸**에서 실행되어 설정(PATH, 현재 폴더 등)이 남음

Yocto는 `source oe-init-build-env` 로 시작합니다. 이게 PATH에 `bitbake` 명령을 추가하고 build 폴더로 이동시켜 줍니다.
**터미널을 새로 열 때마다 다시 해야 합니다.** (안 하면 `bitbake: command not found`)

## 9. 파이프와 리다이렉션

```bash
ls -l | grep ".bb"              # 앞 명령 출력을 뒤 명령 입력으로 (|)
bitbake ... 2>&1 | tee build.log    # 화면에도 보이고 파일에도 저장
echo "hello" > a.txt            # 파일에 쓰기 (덮어쓰기)
echo "world" >> a.txt           # 파일 끝에 추가
```

## 10. 압축 파일

```bash
tar xf file.tar.gz              # 압축 풀기 (.tar.gz, .tar.xz, .tar.bz2 모두)
tar czf out.tar.gz folder/      # 압축하기
```

## 11. Git 최소한

Yocto의 모든 레이어는 git으로 받습니다.

```bash
git clone https://git.yoctoproject.org/poky      # 저장소 복제
git clone -b scarthgap https://...               # 특정 브랜치(버전)로 복제
git branch -a                                    # 브랜치 목록
git checkout scarthgap                           # 브랜치 변경
git status                                       # 변경된 파일 확인
git log --oneline -10                            # 최근 커밋 10개
```

## 12. 디스크 / 프로세스

```bash
df -h               # 디스크 남은 용량 (Yocto 빌드 전 꼭 확인!)
du -sh build/       # 폴더 용량
free -h             # 메모리
nproc               # CPU 코어 수
top / htop          # 이미 아시는 것
Ctrl+C              # 실행 중인 명령 중단
```

---

## ✅ 0단계 자가 점검

아래를 막힘없이 할 수 있으면 다음 단계로 넘어가세요.

1. 홈 폴더에 `test/a/b` 폴더를 한 번에 만들고 그 안으로 이동한다.
2. `nano` 또는 VS Code로 `hello.c` 를 작성하고 `gcc hello.c -o hello && ./hello` 로 실행한다.
3. `/usr/include` 에서 `stdio.h` 를 `find` 로 찾는다.
4. `grep -rn "printf" /usr/include/stdio.h | head` 를 실행해 본다.
5. `df -h` 로 남은 디스크 용량이 100GB 이상인지 확인한다.
6. `source` 와 `./` 실행의 차이를 설명할 수 있다.
