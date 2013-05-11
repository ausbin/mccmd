#!/usr/bin/env python3
#
# Copyright (c) 2013 Austin Adams
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

import re
import sys
from argparse import ArgumentParser

def ansify (*codez) :
    result = ""
    for c in codez :
        result += "\x1b[{0}m".format(c)
    return result

ctlseq = re.compile(r"§([0123456789abcdefklmnor])")
mapping = {
    # no bold, no italic, no underline, no blink, not crossed-out, color
    "0": ansify(22, 23, 24, 25, 29, 30), # black        -> black
    "1": ansify(22, 23, 24, 25, 29, 34), # dark blue    -> blue
    "2": ansify(22, 23, 24, 25, 29, 32), # dark green   -> green
    "3": ansify(22, 23, 24, 25, 29, 36), # dark cyan    -> cyan
    "4": ansify(22, 23, 24, 25, 29, 31), # dark red     -> red
    "5": ansify(22, 23, 24, 25, 29, 35), # purple       -> magenta
    "6": ansify(22, 23, 24, 25, 29, 33), # gold         -> yellow
    "7": ansify(22, 23, 24, 25, 29, 37), # gray         -> white
    "8": ansify(22, 23, 24, 25, 29, 30), # dark gray    -> black
    "9": ansify(22, 23, 24, 25, 29, 34), # blue         -> blue
    "a": ansify(22, 23, 24, 25, 29, 32), # bright green -> green
    "b": ansify(22, 23, 24, 25, 29, 36), # cyan         -> cyan
    "c": ansify(22, 23, 24, 25, 29, 31), # red          -> red
    "d": ansify(22, 23, 24, 25, 29, 35), # pink         -> magenta
    "e": ansify(22, 23, 24, 25, 29, 33), # yellow       -> yellow
    "f": ansify(22, 23, 24, 25, 29, 37), # white        -> white
    "k": ansify(6),                      # randomness   -> fast blinking
    "l": ansify(1),                      # bold         -> bold
    "m": ansify(9),                      # strike-thru  -> crossed-out (rare)
    "n": ansify(4),                      # underline    -> underline
    "o": ansify(3),                      # italic       -> italic (semi-rare)
    "r": ansify(22, 23, 24, 25, 29, 37), # reset        -> reset
}
parser = ArgumentParser(description="send commands to a craftbukkit server "
                                    "via the mccmd plugin and/or convert "
                                    "or remove minecraft chat control "
                                    "characters.",
                        epilog="https://github.com/ausbin/mccmd") 

parser.add_argument("-p", "--process", action="store_true",
                    help="convert or remove minecraft chat control "
                         "characters in stdin instead of connecting to a "
                         "server (nullifies all other options except for "
                         "-c/--nocolor)")
parser.add_argument("host", nargs="?", default="localhost",
                    help="host to connect to (defaults to localhost)")
parser.add_argument("port", nargs="?", type=int, default=5454,
                    help="port to connect to on 'host' (defaults to 5454)")
parser.add_argument("-c", "--nocolor", action="store_false", dest="color",
                    help="discard minecraft chat control sequences "
                         "instead of converting them directly to "
                         "ansi control sequences")

def convert (chat, toansi=True, cleanending=False) :
    if toansi:
        # if specified, add a "reset" minecraft control character, which
        # turns off underlines, italics, colors, and blinking. 
        # (this could be used to prevent the "styling" in one command from
        #  leaking into another or even the rest of the shell)
        if cleanending:
            chat += "§r"

        repl = lambda m: mapping[m.group(1)]
    else:
        repl = ""

    return ctlseq.sub(repl, chat)

if __name__ == "__main__":
    args = parser.parse_args(); 

    if args.process: 
        for line in sys.stdin:
            sys.stdout.write(convert(line, args.color))

        if args.color:
            # clean up after all the random control characters we just dumped to stdout
            sys.stdout.write(convert("", cleanending=True))
    else :
        interactive = sys.stdin.isatty() and sys.stdout.isatty()

        if interactive:
            # integrate readline features into input() if we can
            try:
                import readline
            except ImportError :
                pass

        import socket

        server = socket.create_connection((args.host, args.port))

        while True:
            if interactive:
                try:
                    line = input("> ")+"\n"
                except EOFError:
                    line = ""
            else:
                line = sys.stdin.readline()

            if not line:
                # clear out the prompt
                if interactive:
                    print()
                break
            elif line.strip():
                # send the command as a utf-8-encoded string
                server.send(line.encode("utf-8"))
         
                # number of bytes left to read (taken from the response header,
                # defaults to unknown because we don't know yet)
                remaining = None
                # raw contents of the response header containing the length of
                # the body
                head = bytes() 
                body = bytes()

                # read reply from the socket
                while True:
                    # read a maximum of one kibibyte from the socket
                    resp = server.recv(1024)

                    if remaining is None:
                        try:
                            endofhead = resp.index(b"\n")+1
                        except ValueError:
                            head += resp
                            continue

                        head += resp[:endofhead]
                        remaining = int(head.decode("utf-8"))

                        # if the length specified in the header was 0, stop
                        # because there's nothing else to read from the socket
                        if not remaining:
                            break
                        # otherwise, chop off the 
                        else :
                            resp = resp[endofhead:]

                    body += resp
                    remaining -= len(resp)

                    # if there's no more to read, stop
                    if remaining <= 0:
                        break

                sys.stdout.write(convert(body.decode("utf-8"), args.color, True))

        server.close()



