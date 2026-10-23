#include <stdio.h>
#include "hello.h"

void hello_say(const char *name)
{
    printf("Hello, %s! (from libhello)\n", name);
}

int hello_add(int a, int b)
{
    return a + b;
}
