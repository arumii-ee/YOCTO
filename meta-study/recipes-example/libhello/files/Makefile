# libhello 공유 라이브러리 빌드용 Makefile
# Yocto가 CC, CFLAGS, LDFLAGS 를 크로스 컴파일용으로 환경 변수로 넘겨준다.
# 그래서 여기서 CC를 강제로 지정하면 안 된다.

PREFIX ?= /usr
LIBDIR ?= $(PREFIX)/lib
INCDIR ?= $(PREFIX)/include

NAME   = libhello.so
SONAME = $(NAME).1
REAL   = $(NAME).1.0.0

all: $(REAL)

$(REAL): hello.o
	$(CC) $(LDFLAGS) -shared -Wl,-soname,$(SONAME) -o $@ $^

hello.o: hello.c hello.h
	$(CC) $(CFLAGS) -fPIC -c -o $@ hello.c

install: $(REAL)
	install -d $(DESTDIR)$(LIBDIR) $(DESTDIR)$(INCDIR)
	install -m 0755 $(REAL) $(DESTDIR)$(LIBDIR)/
	ln -sf $(REAL) $(DESTDIR)$(LIBDIR)/$(SONAME)
	ln -sf $(SONAME) $(DESTDIR)$(LIBDIR)/$(NAME)
	install -m 0644 hello.h $(DESTDIR)$(INCDIR)/

clean:
	rm -f *.o $(NAME)*

.PHONY: all install clean
