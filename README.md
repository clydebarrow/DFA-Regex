# Regex-C

Convert regexes into C code suitable for use in real-time systems. It works with an 8 bit character set, i.e. bytes.
The regexes may contain embedded actions. Note that actions may be executed while a match is still in progress,
even if the ultimate result is failure.

The generated code uses a "push" model where a function is generated that expects a single character
per invocation. No lookahead is used, which restricts the scope of the regexes somewhat, but ensures
that each state change happens immediately without having to wait for further input.

Lack of lookahead means the regexes are inherently non-greedy - success will be signalled as soon as it matches
any valid expression.

Although the code can be used to process input from various sources, the expectation is that a file will
be provided with a single regex (potentially made up of multiple rules, but these are treated as alternates)
along with C code to be copied through. The general format of the file is:
````
%prefix <file_prefix>

// arbitrary C code to be copied
main()
{
    ...
}

%names
#comments start with #
      # white space is stripped
#you can define macros here to be expanded, e.g.
        digit =  [0-9\.]
        hexdigit =  [0-9A-F]

%rule
# this is a regular expression
# most common meta-characters are recognised. Literals must be enclosed in quotes (single or double)
# or you can e.g. use [x] for a single character. Hex literals can also be used

# this pattern always starts with a newline
\n
# when the newline is seen, perform an action. Actions must be enclosed in {} and start on a new line, but may extend across
# multiple lines.
    { ptr = buffer; }
#next we expect one or more digits followed by a comma. Copying of the data into the buffer must be done by user code, it is not automatic
(digit+ ','
    { ptr[-1] = 0; printf("got element %s\n", buffer); ptr = buffer; }
# we can have more than one of those groups
)+
# now we want 4 hex digits followed by a <CR>
hexdigit{4} \r
    { ptr[-1] = 0; printf("got checksum %s\n", buffer); ptr = buffer; }

%rule
# if another rule is defined here it will be treated as an alternate, i.e. the entire RE is (rule1)|(rule2)|(rule3) etc.
````

### Invocation

You can download a compiled jar from [here](https://github.com/clydebarrow/Regex-to-C/releases/download/v1.0/regex-c.jar). 
Run from the command line:

`java -jar regex-c.jar input.re`

The generated code will comprise a header file called `lex_<prefix>.h` and a C file `lex_<prefix>.c` where `<prefix>` is the value
specified in the input file via `%prefix`

### RE Syntax

TODO

### Use Case
A complete example:
```c
%prefix test

static test_state_t test_state;

#include    <stdio.h>
#include    <stdlib.h>

#define BUFLEN  16
static unsigned char * ptr;
static unsigned char buffer[BUFLEN];

static char * data = "\n1234,3456,ABDE\r\n5,6,AB12\r";

static test_action_t addChar(char c) {
    test_state_t prev = test_state;
    if(ptr >= buffer && ptr < buffer+BUFLEN)
        *ptr++ = c;
    test_action_t action = test_lex((unsigned char)c);
    if(c <= ' ')
        fprintf(stderr, "state %d: %02x - > state %d, action %d\n", prev, c, test_state, action);
    else
        fprintf(stderr, "state %d: '%c' - > state %d, action %d\n", prev, c, test_state, action);
    return action;}


int main(void) {
    test_reset();
    while(*data != 0)
        switch(addChar(*data++)) {
        case test_CONTINUE:
            continue;
        case test_FAIL:
            fprintf(stderr, "match failed at %s!\n", data);
            exit(1);
        case test_ACCEPT:
            fprintf(stderr, "match succeeded at %s - continuing\n", data);
            test_reset();
            continue;
    }
    exit(0);
}

%names
        digit =  [0-9\.]
        hexdigit =  [0-9A-F]

%rule

\n
    { ptr = buffer; }
(digit+ ','
    { ptr[-1] = 0; printf("got element %s\n", buffer); ptr = buffer; }
)+
hexdigit{4} \r
    { ptr[-1] = 0; printf("got checksum %s\n", buffer); ptr = buffer; }
```

