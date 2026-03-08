* BALX - Was BAL in Algorithms in SNOBOL4.
*	 Since SPITBOL does not permit the redefinition of
*	 built-in patterns, the name was changed to BALX.
*
*	BALX(PARENS,QTS) will return a pattern which will match
*	all nonnull balanced strings where PARENS is used to
*	specify paired brackets in nested fashion and QTS specifies
*	characters to be used as quotes.  Bracket characters within
*	quotes are ignored for balancing purposes.
*	BALX('(<[]>)',"'" '"') will match a balanced string in
*	SPITBOL source.
*
*	See also:   FASTBAL
*
	DEFINE('BALX(PARENS,QTS)Q,GBAL,NAME,STAR,LP,RP')
						:(BALX_END)
BALX	NAME  =  'BAL_.'  &STCOUNT
	STAR  =  CONVERT(NAME, 'EXPRESSION')
	GBAL  =  NOTANY(PARENS QTS)
BAL_1	QTS   LEN(1) . Q  =			:F(BAL_2)
	GBAL  =  Q BREAK(Q) Q  |  GBAL		:(BAL_1)
BAL_2	PARENS   LEN(1) . LP  RTAB(1) . PARENS  LEN(1) . RP  :F(BAL_3)
	GBAL  =  LP  (STAR | NULL) RP  |  GBAL	:(BAL_2)
BAL_3	BALX  =  GBAL  ARBNO(GBAL)
	$NAME  =  BALX				:(RETURN)
BALX_END
