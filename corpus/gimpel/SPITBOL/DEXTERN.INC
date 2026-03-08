* DEXTERN.inc - DEXTERN(PROTO,LBL) defines a function using PROTO
*		with entry point LBL.  The function is loaded on
*		demand when it is first called.
*
*		A library file is searched sequentially for the
*		function, which begins with LBL in column 1, and
*		ends with (LBL '_END') in column 1.
*
	DEFINE('DEXTERN(PROTO,LBL)NAME')
	DEFINE('LOADEX(LBL)PAT,X,CODE')
	INPUT(.LIB_FILE,.LIB_, "Library File Name") :S(DEXTERN_END)
	OUTPUT = "In DEXTERN, could not open library file"	:(END)
DEXTERN	PROTO  IDENT(LBL)  BREAK('(') . LBL
	CODE(LBL  "  LOADEX('" LBL "')  ; :("  LBL  ")" )
+						:F(DEXTERN1)
	DEFINE(PROTO, LBL)			:S(RETURN)
DEXTERN1 OUTPUT = "In DEXTERN, illegal prototype or function body"
+						:(ERROR)
LOADEX	REWIND(.LIB_)
	PAT  =  POS(0)  LBL  (' ' | CHAR(9) | RPOS(0))
LOADEX_1	CODE  =  LIB_FILE		:F(LOADEX_4)
	CODE   PAT				:F(LOADEX_1)
	PAT  =  POS(0)  LBL  '_END'  (' ' | CHAR(9) | RPOS(0))
LOADEX_2	X  =  LIB_FILE			:F(LOADEX_3)
	X   PAT					:S(LOADEX_3)
	X   POS(0)  ANY('*-')			:S(LOADEX_2)
	X  =  ';'  X
	X   POS(0)  ';'  ANY('.+')  =  ' '
	CODE  =  CODE  X			:(LOADEX2)
LOADEX_3	CODE(CODE)			:S(RETURN)F(LOADEX_4)
LOADEX_4 OUTPUT = "In LOADEX, function missing or uncodable" :(ERROR)
DEXTERN_END
