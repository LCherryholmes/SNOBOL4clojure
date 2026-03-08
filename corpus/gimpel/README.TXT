Programs from:

        A L G O R I T H M S   I N   S N O B O L 4
        -----------------------------------------
                    by James F. Gimpel

        ISBN 0-939793-01-6
        ISBN 0-939793-00-8 (paperback)

Version 1.06 -- 2/7/1999
Correctly error in PERM.INC.


(c) Copyright 1976 Bell Telephone Laboratories, Inc.

(c) Copyright 1986, 1998 Catspaw, Inc.

Program material provided courtesy of Dr. James F. Gimpel.

Permission to distribute these programs was obtained by
Catspaw, Inc. from James F. Gimpel and AT&T Bell Laboratories.

Catspaw, Inc. grants permission to make and distribute verbatim
copies of these programs provided the copyright notice and this
permission notice are preserved on all copies.

Catspaw, Inc. further grants permission to incorporate these 
programs and functions into programs you are developing for any
commercial or non-commercial purpose.

Catspaw, Inc.
P.O. Box 1123
Salida, CO 81201
719-539-3884
support@SNOBOL4.com
ftp://ftp.SNOBOL4.com
http://www.SNOBOL4.com



Notes on the programs:

Programs are provided in two forms -- for SNOBOL4+ and for
SPITBOL.  There are minor differences in some programs,
and the file name extension are either .SNO or .SPT.

All program sources are in directories \SNOBOL4 and \SPITBOL.
A Table of Contents file is in \CONTENTS.TXT

Minor changes have been made in several programs to accommodate
the 16-bit integers of Catspaw's SNOBOL4+.  For example, the RANDOM
function now uses real values to compute a value with a reasonable
period.

Several subprograms duplicate functions that are built into the
SNOBOL4+ and SPITBOL systems.  These files have been modified to
contain a GOTO around the defining code, so that the built-in
definition is not replaced.  Users of other SNOBOL4 systems must
remove these GOTOs.  The affected files are:

        ARC.INC                 Inverse trig functions (SPITBOL only)
        BREAKX.INC              BREAK with rematch
        LOG.INC                 Logarithms
        LPAD.INC                Left pad string
        RAISE.INC               Exponentiation
        REVERSE.INC             Reverse string
        RPAD.INC                Right pad string
        SQRT.INC                Square root
        SUBSTR.INC              Extract substring
        TRIG.INC                Trig functions (SPITBOL only)

SPITBOL does not allow the redefinition of built-in operators and
functions with OPSYN.  Programs that rely upon OPSYN have been
modified to use alternate strategies, and these programs will not
match the examples in the book exactly.  The affected programs
(SPITBOL version only) are:

        FTRACE.INC              Function call/return tracing
        INFINIP.INC             Infinite precision arithmetic
        INSULATE.INC            Insulate &ANCHOR in function calls
        MFREAD.INC              Multi-file read function
        PHYSICAL.INC            Arithmetic on physical quantities
        REDEFINE.INC            Redefine built-in operator/function
        VISIT.INC               Visit members of a structure

The OR function in section 8.10 is provided in several variants that
improve efficiency and flexibility.  See the comments in files
ORSORT.INC, ORBREAK.INC, and ORVISUAL.INC.

Finally, the trigonometric functions in files ARC.INC and TRIG.INC
can be made more efficient if you have an 8087 or 80287 and are
using SNOBOL4+.  The SNOBOL4+ diskette contains two assembly-
language functions, ATAN.COM and TAN.COM that use the numeric
processor to compute those functions.  The remaining trig functions
can be derived from the tangent and arctangent.  SPITBOL has
these functions built in, and will use the numeric co-processor
automatically.

Programs have been modified to use the features of SNOBOL4+'s
and SPITBOL's INCLUDE control statement.  Functions referenced will
be included automatically during compilation.  In addition,
redundant INCLUDE's are ignored.  Thus you can include two Gimpel
each of which includes a third file, such as DEXP.INC, with the
result that DEXP.INC will only be included once.

You may also take advantage of SNOBOL4+'s and SPITBOL's ability to
search a specified directory for INCLUDE files.  For example, you could
place all of the Gimpel files in directory C:\LIBRARY, and use:

        SET SNOLIB=C:\LIBRARY

to search that sub-directory for include files.
Under UNIX, use:
   setenv SNOLIB "(/u/name/Library)"    (Berkeley Unix)

   SNOLIB=/u/name/Library               (System V Unix)
   export SNOLIB

Current versions of programs CODE.SNO and CODE.SPT are included.
The SLOAD function has been modified to process INCLUDE control cards
in the same manner as the version 2.0 compiler.  Thus you can use
CODE.SNO to interactively load and exercise these programs.
For example, use the SET SNOLIB directive as described above, and
run CODE by typing SPITBOL CODE.  You can then
load the QUOTE.INC function by typing:

        SLOAD('QUOTE.INC')

Any INCLUDE files used by QUOTE.INC will be loaded as well.  You can
then exercise the QUOTE function interactively.

A word of caution:  All of these programs assume the space (or blank)
character is the only white space character.  They do not understand
the tab character used on personal computers.  Some rework will be
necessary to accept input containing the tab character.

A 500-page book, Algorithms in SNOBOL4, by James F. Gimpel, explains
these programs, as well as describing additional algorithms and
pattern theory.  The book has been reprinted by Catspaw, Inc., and
is available in both paperback and hardcover format.


