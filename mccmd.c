/* Copyright (c) 2013 Austin Adams

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE. */

#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <readline/readline.h>
#include <readline/history.h>

/* the response parser's current location.
   in a response, the header is first line. it consists the length of the body
   in the form of a series of ASCII digits terminated by a newline. 
   the body contains the output of the command included in the request. */
enum location {HEAD, BODY};
/* describes the state of a formatting style.
   NA is n/a or not applicable. used when a feature is not to be changed.
   OFF disables a feature (e.g. no bold), and ON enables it (e.g. italics). */
enum style {NA = -1, OFF, ON};
/* ANSI escape sequence colors. taken from
   https://wikipedia.org/wiki/ANSI_escape_code#Colors */
enum color {ANY = -1, BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE};
/* the next byte in a minecraft chat control sequence:
   don't look for the next byte at all, leading byte in § (\xc2), continuation
   byte in § (\xa7), or format letter (0-f) (respectively). */
enum seqbyte {IGNORE = -1, LEADING, CONTINUATION, LETTER};
/* what to do with minecraft chat control sequences
   convert to ansi escape sequences, remove them entirely, or leave them */
enum seqaction {CONVERT, REMOVE, INTACT};

/* describes the current state of or change in formatting.
   when used to represent the current state of formatting during parsing,
   all values are either OFF or ON. For formatting deltas, however, each
   value is set to NA (no change), OFF (disable the feature), or ON (enable) */
struct format {
    enum style random; 
    enum style bold;
    enum style strikethru; 
    enum style underline;
    enum style italic;
    enum color color; /* the current ansi color */
};
/* describes a minecraft chat formatting letter */
struct chatcode {
    const char letter; /* one of 0123456789abcdefklmnor */
    const struct format delta; /* the resulting change in formatting */
};

/* the maximum amount of a response to recv() at once */
const size_t recvsize = 1024;

/* size of each chunk of memory allocated while reading input lines from a 
   non-tty */
const size_t lineincr = 128;

const char *const program = "mccmd";
const char *const defaultport = "5454";
const char *const prompt = "> ";

/* symbol that starts a minecraft chat control sequence */
const char *const seqsymbol = "§";

/* map of minecraft formatting letters to their formatting changes
   gleaned from http://wiki.vg/Chat#Control_Sequences */
const struct chatcode chatcodes[] = {
    {'0', {OFF, OFF, OFF, OFF, OFF, BLACK}},
    {'1', {OFF, OFF, OFF, OFF, OFF, BLUE}}, 
    {'2', {OFF, OFF, OFF, OFF, OFF, GREEN}}, 
    {'3', {OFF, OFF, OFF, OFF, OFF, CYAN}}, 
    {'4', {OFF, OFF, OFF, OFF, OFF, RED}}, 
    {'5', {OFF, OFF, OFF, OFF, OFF, MAGENTA}}, 
    {'6', {OFF, OFF, OFF, OFF, OFF, YELLOW}}, 
    {'7', {OFF, OFF, OFF, OFF, OFF, WHITE}}, 
    {'8', {OFF, OFF, OFF, OFF, OFF, BLACK}}, 
    {'9', {OFF, OFF, OFF, OFF, OFF, BLUE}}, 
    {'a', {OFF, OFF, OFF, OFF, OFF, GREEN}}, 
    {'b', {OFF, OFF, OFF, OFF, OFF, CYAN}}, 
    {'c', {OFF, OFF, OFF, OFF, OFF, RED}}, 
    {'d', {OFF, OFF, OFF, OFF, OFF, MAGENTA}}, 
    {'e', {OFF, OFF, OFF, OFF, OFF, YELLOW}}, 
    {'f', {OFF, OFF, OFF, OFF, OFF, WHITE}}, 
    {'k', {ON,  NA,  NA,  NA,  NA,  ANY}}, /* randomness */
    {'l', {NA,  ON,  NA,  NA,  NA,  ANY}}, /* bold */
    {'m', {NA,  NA,  ON,  NA,  NA,  ANY}}, /* strikethrough */
    {'n', {NA,  NA,  NA,  ON,  NA,  ANY}}, /* underline */
    {'o', {NA,  NA,  NA,  NA,  ON,  ANY}}, /* italic */
    {'r', {OFF, OFF, OFF, OFF, OFF, WHITE}} /* reset */
};

void parseargs (int, char **, char **, char **, enum seqaction *);
int getsock (char *, char *);
char *line (int);
void printresponse (int, enum seqaction);

int main (int argc, char **argv) {
    int sock, interactive;
    char *lineread, *host, *port; 
    enum seqaction seqaction;

    /* try to find out if we're being piped or used interactively */
    interactive = isatty(fileno(stdin)) && isatty(fileno(stdout));

    /* default to the loopback address */
    host = NULL;
    /* cast to get rid of warning about defaultport being a constant pointer
       to a constant character (whereas port is just a char *)  */
    port = (char *) defaultport;
    /* convert minecraft chat formatting to ansi escape sequences if we're
       running interactively, otherwise (if we're being piped) remove them */
    seqaction = interactive? CONVERT : REMOVE;

    /* update the args struct according to the arguments passed into the
       program (or exit if an invalid option or -h was specified) */
    parseargs(argc, argv, &host, &port, &seqaction);

    sock = getsock(host, port);
   
    while ((lineread = line(interactive)) != NULL) { 
        send(sock, lineread, strlen(lineread), 0); 
        free(lineread);

        /* send terminating newline */
        send(sock, (void *)"\n", 1, 0);

        printresponse(sock, seqaction);
    }

    /* clear out the prompt so that $PS1 will be on a new line */
    if (interactive)
        putchar('\n');

    close(sock);

    return EXIT_SUCCESS;
}

/* XXX transition towards function return values
       (this is sort of a hack) */
/* crash and print either a custom message or the error message associated
   with ERRNO */
void die (const char *msg) {
    if (msg == NULL)
        perror(program);
    else
        fprintf(stderr, "%s: %s\n", program, msg);

    exit(EXIT_FAILURE);
}

/* print usage to the specified output stream */
/* XXX find a way to integrate this into die() */
void usage (FILE *stream) {
    fprintf(stream, "usage: %s [-r|-i|-c|-h] [host [port]]\n"
                    "\n"
                    "send commands to a minecraft server.\n"
                    "(default host is localhost, default port is %s)\n"
                    "\n"
                    "-r  remove minecraft chat formatting\n"
                    "    (the default when not in a tty)\n"
                    "-i  leave minecraft chat formatting intact\n"
                    "-c  convert minecraft chat formatting into ansi\n"
                    "    escape sequences (the default in a tty)\n"
                    "-h  show this message\n"
                    "\n"
                    "https://github.com/ausbin/mccmd\n"
                    , program, defaultport);
}

/* change the arguments passed into the program into a more usable struct */
void parseargs (int argc, char **argv, char **host, char **port,
                enum seqaction *seqaction) {
    int c;

    while ((c = getopt(argc, argv, "rich")) != -1) {
        switch (c) {
            case 'r':
                *seqaction = REMOVE;
            break;
            case 'i':
                *seqaction = INTACT;
            break;
            case 'c':
                *seqaction = CONVERT;
            break;
            case 'h':
                /* XXX this is ugly */
                usage(stdout);
                exit(EXIT_SUCCESS);
            break;
            case '?':
                usage(stderr);
                exit(EXIT_FAILURE);
            break;
        }
    }

    /* there should not be more than 2 positional arguments */
    if (argc-optind > 2) { 
        usage(stderr);
        exit(EXIT_FAILURE);
    }

    while (optind < argc)
        if (*host == NULL)
            *host = argv[optind++];
        else
            *port = argv[optind++];
}

/* find and return a socket fd connect()'ed to host:port */
int getsock (char *host, char *port) {
    int status, sock;
    struct addrinfo gaiargs, *result, *rp;

    /* set all of the values in the struct to pass to getaddrinfo() to NULL
       (required according to the manpage) */
    memset(&gaiargs, 0, sizeof (struct addrinfo));
    /* allow either IPv4 or IPv6 sockets */
    gaiargs.ai_family = AF_UNSPEC; 
    gaiargs.ai_socktype = SOCK_STREAM; /* tcp */

    status = getaddrinfo(host, port, &gaiargs, &result);

    /* bail if there were any complaints from getaddrinfo() */
    if (status != 0)
        die(gai_strerror(status));

    /* look for a working address in the "list" from getaddrinfo() */
    for (rp = result; rp != NULL; rp = rp->ai_next) {
        /* try to make a socket with this address */
        sock = socket(rp->ai_family, rp->ai_socktype, rp->ai_protocol);

        /* if it didn't work, try the next one */
        if (sock == -1)
            continue;

        /* if connecting to the socket succeeds, stop trying other
           addresses */
        if (connect(sock, rp->ai_addr, rp->ai_addrlen) != -1)
            break;
        else
            /* otherwise, close the socket and try the next one */
            close(sock);
    }

    /* XXX what if (somehow) 'result' was a null pointer even though 
           getaddrinfo() returned a zero exit status? */ 
    /* if all of the addresses we tried failed, print the message associated
       with ERRNO (it probably has more information about the latest failure) */
    if (rp == NULL)
        die(NULL);

    freeaddrinfo(result);

    return sock;
}

/* read a line from stdin (used if the session in non-interactive) */
char *feedline (void) {
    char c, *result;
    int i, bufsize;

    bufsize = 0;
    result = NULL;

    for (i = 0; (c = getchar()) != EOF && c != '\n'; ++i) {
        /* if the amount of memory we've read is larger than the memory we've
           allocated, allocate LINEINCR more memory */
        if (i == bufsize)
            result = realloc(result, (bufsize += lineincr));

        result[i] = c;
    }

    /* mimic the way readline handles blank lines:
       - if the line was *just* EOF, return a NULL pointer
       - if the line had content followed by EOF, return the content
       - if the line had content followed by a newline, return the content
       - if the line was *just* a newline, return a pointer to a single \0 */
    if (i == 0) {
        if (c == EOF)
            return NULL;
        else if (c == '\n')
            result = malloc(sizeof (char));
    }

    result[i] = '\0';

    return result;
}

/* abstraction layer over readline() (for interactive sessions) and feedline()
   (for piping or whatever - no keyboard shortcuts or prompts or anything) */
char *line (int interactive) {
    if (interactive) {
        char *lineread;

        lineread = readline(prompt);

        /* if the line wasn't blank or EOF'd, add it to the history */
        if (lineread != NULL && lineread[0] != '\0')
            add_history(lineread);

        return lineread;
    } else {
        return feedline();
    }
}

/* return the change in formatting that would occur when §{letter} is
   specified */
const struct format *getdelta (const char letter) {
    int i;

    for (i = 0; i < (sizeof chatcodes / sizeof (struct chatcode)); i++)
        if (chatcodes[i].letter == letter)
            return &chatcodes[i].delta;

    return NULL;
}

/* output an ansi control sequence from an integer */
void putansi (int code) {
    printf("\033[%im", code);
}

/* print an ansi escape sequence to stdout for every effect that needs to be
   enabled or disabled. */
void adjuststyle (enum style *from, const enum style *to, int ansicodeon) {
    /* if *to has anything to change about the current formatting, update
       both what the user sees and our internal representation */
    if (*to != NA && *from != *to) {
        if (*from == OFF && *to == ON)
            putansi(ansicodeon);
        else if (*from == ON && *to == OFF)
            putansi(20 + ansicodeon); /* the code to disable an ansi effect is
                                         20 plus the code used to enable it */
        *from = *to;
    }
}

/* update the current color according to *to both internally and in 
   the terminal.*/
void adjustcolor (enum color *from, const enum color *to) {
    if (*to != ANY && *from != *to) {
        putansi(30 + *to);
        *from = *to;
    }
}

/* change the current color and effects of text in stdout as stated in delta */
void adjustformat (struct format *current, const struct format *delta) {
    adjuststyle(&current->random, &delta->random, 5);
    adjuststyle(&current->bold, &delta->bold, 1);
    adjuststyle(&current->strikethru, &delta->strikethru, 9);
    adjuststyle(&current->underline, &delta->underline, 4);
    adjuststyle(&current->italic, &delta->italic, 3);

    adjustcolor(&current->color, &delta->color);
}

/* de-absorb the parts of § we haven't printed.
   called whenever an attempt at parsing a minecraft chat control sequence
   is truncated or fails (e.g. §x shouldn't print x, §x -> §x is more chill) */
void deabsorb (int nextseqbyte) {
    int i;

    for (i = LEADING; i < nextseqbyte; ++i)
        putchar(seqsymbol[i]);
}

/* recv response and print output to sent command */
void printresponse (int sock, enum seqaction seqaction) {
    char *readed; /* the way all cool people say 'read' */
    int location, nextseqbyte, i, bodylen, bodylenread, msglen; 
    struct format current;
    const struct format *delta;

    /* allocate memory for the response */
    readed = malloc(recvsize);
    /* assume the current state of the terminal is peaceful and happy */
    memcpy(&current, getdelta('r'), sizeof (struct format));
    /* our grand parsing quest will start in the response header */
    location = HEAD;
    bodylen = 0;
    bodylenread = 0;

    if (seqaction == INTACT)
        nextseqbyte = IGNORE;
    else
        nextseqbyte = LEADING;
   
    /* recieve RECVSIZE until we're in the body and we've read the amount of
       the body specified in the header */ 
    for (; !(location == BODY && bodylenread >= bodylen); i++) {
        msglen = recv(sock, readed, recvsize, 0);

        /* the server closed the connection peacefully, but before we could
           read the header and the length of body as specified in the header */
        if (msglen == 0)
            die("connection terminated prematurely");
        else if (msglen < 0)
            die(NULL); /* print the errno message */

        /* read the response header */
        for (i = 0; location == HEAD && i < msglen; i++) {
            /* start reading the body when we encounter a newline. 
               if a newline was the first thing we encountered, complain */
            if (readed[i] == '\n' && i > 0)
                location = BODY;
            else {
                if (!(readed[i] >= '0' && readed[i] <= '9'))
                    die("invalid character in response header");

                bodylen *= 10; /* 45 -> 450 */
                bodylen += (readed[i]-'0'); /* 450 -> 454 */
            }
        }
        
        bodylenread += msglen-i;

        /* read the body (pick up where the header parser left off */
        for (; i < msglen; i++) {
            /* if we're looking for the leading byte and we encounter it,
               "absorb" it and start looking for the continuation byte */ 
            if (nextseqbyte == LEADING && readed[i] == seqsymbol[nextseqbyte])
                nextseqbyte = CONTINUATION;
            /* if, on the other hand, we're looking for the continuation byte
               and we find it, start looking for the letter (0-f) */
            else if (nextseqbyte == CONTINUATION && readed[i] == seqsymbol[nextseqbyte])
                nextseqbyte = LETTER;
            /* finally, if we're looking for a letter and we find a valid one,
               start looking for the leading byte again and adjust formatting
               accordingly if we're supposed to */
            else if (nextseqbyte == LETTER && (delta = getdelta(readed[i])) != NULL) {
                nextseqbyte = LEADING;

                if (seqaction == CONVERT)
                    adjustformat(&current, delta);
            /* if all of those tests failed, spit out any of the parts of §
               we've absorbed so far (if any), output the character in
               question and start looking for the leading byte again */
            } else {
                deabsorb(nextseqbyte);

                if (nextseqbyte > LEADING)
                    nextseqbyte = LEADING;

                putchar(readed[i]);
            }
        }
    }

    free(readed);

    /* again, choke out any parts of § we've absorbed */
    deabsorb(nextseqbyte);

    /* return the terminal to its happy peaceful state */
    adjustformat(&current, getdelta('r'));
}
