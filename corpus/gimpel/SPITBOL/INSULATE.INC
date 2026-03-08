* INSULATE.inc - INSULATE(PROTO,LABEL) will cause each function
*		 following it to trap to INS_CALL() when called
*		 and to INS_RET() on return.  This requires
*		 redefining DEFINE to point to INSULATE.
*
*  NOTE:  This does not work under SPITBOL, because the built-in
*	  function DEFINE cannot be redefined.
*
*	  Instead, we create a function INSULATE which must
*	  be used explicitly instead of DEFINE on each function
*	  that is to be "insulated."

-INCLUDE "push.inc"
	DEFINE('INSULATE(PROTO,LABEL)NAME')
	DEFINE('INS_CALL()')
	DEFINE('INS_RET()')
*	OPSYN('DEFINE.', 'DEFINE')
*	OPSYN('DEFINE',  'INSULATE')
	&TRACE  =  100000			:(INSULATE_END)
INSULATE PROTO  BREAK('(') . NAME
	DEFINE(PROTO, LABEL)
	TRACE(NAME, 'CALL',, 'INS_CALL')
	TRACE(NAME, 'RETURN' ,, 'INS_RET')	:(RETURN)
INS_CALL PUSH(&ANCHOR)  ;  &ANCHOR  =  0	:(RETURN)
INS_RET	 &ANCHOR  =  POP()			:(RETURN)
INSULATE_END
