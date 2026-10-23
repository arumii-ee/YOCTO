#include <stdio.h>
#include <hello.h>

int main(void)
{
    hello_say("Yocto");
    printf("1 + 2 = %d\n", hello_add(1, 2));
    return 0;
}
