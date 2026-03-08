* GPM.inc - GPM(S) will transform string S according to the
*	    rules of the general macro processor.
*
-INCLUDE "balx.inc"
-INCLUDE "push.inc"
	DEFINE('GPM(S)PREFIX,BOD,ARG,NAME,N,PUSH_POP')
	&ALPHABET  LEN(1) . FORB_CH
	MAC_TBL  =  TABLE()
	ITEM  =  '<' BALX('<>') '>'  |  '#' *GPM_BAL ';'
+	| NOTANY('<#') BREAK('<#>;,')
	GPM_BAL  =  ARBNO(ITEM)
	GET.PREFIX.BOD  =  POS(0) BREAK('<#') . PREFIX  FENCE
+	('<' BALX('<>') . BOD '>'      |
+	'#' GPM_BAL . NAME . *PROC('NAME')
+	ARBNO(',' GPM_BAL . ARG . *PROC('ARG'))
+	';' . *PROC('MEND')  )       |
+	REM . PREFIX    NULL . BOD		:(GPM_END)
GPM	IDENT(S)				:S(RETURN)
	S  GET.PREFIX.BOD  =			:F(FRETURN)
	GPM  =  GPM  PREFIX  BOD		:(GPM)
GPM_END

	DEFINE('PROC(TYPE)')			:(PROC_END)
PROC	PROC  =  .DUMMY				:($('P' TYPE))

PNAME	NAME  =  GPM(NAME)
	N  =  0
	PUSH_POP  =
	PUSH(NAME)				:(NRETURN)

PARG	PUSH(GPM(ARG))
	N  =  N + 1				:(NRETURN)

PMEND	BOD = IDENT(NAME,'DEF') POP()		:F(PMEND_2)
	MAC_TBL<POP()>  =  BOD
	BOD  =					:(NRETURN)
PMEND_2	BOD  =  REPLACE(MAC_TBL<NAME>,'&',FORB_CH)
PMEND_1	BOD   FORB_CH N  =  TOP()		:S(PMEND_1)
	N  =  N - 1
	POP()					:S(PMEND_1)
	BOD  =  GPM(BOD)			:(NRETURN)
PROC_END
