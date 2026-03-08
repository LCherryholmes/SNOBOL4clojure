* MFREAD.inc - MFREAD(P,U,L) will behave like READ(P) except that an
*	       optional second argument (U) can be used to specify
*	       a unit other than the normal reader.  And optional
*	       third argument can specify a file name and logical
*	       record length (for the first call associated with
*	       a given unit).
*
*   CAUTION:  This function attempts to redefine the built-in
*	      REWIND function, which is not allowed in SPITBOL.
*             Instead, the user should use REWIND.(N).
*
	DEFINE('MFREAD(P,U,L)BUF,NF,NM,DATA')

	DATA('RDATA(RNM,RBUF,RNF)')
	READ_TBL  =  TABLE()
	READ_TBL<>  =  RDATA(.INPUT)
* Cannot redefine built-in functions in SPITBOL.
*	OPSYN('REWIND.','REWIND')
	DEFINE('REWIND.(N)')			:(MFREAD_END)
REWIND.	READ_TBL<N>  =
	REWIND(N)				:(RETURN)
MFREAD	DATA  =  READ_TBL<U>
	IDENT(DATA,NULL)			:F(MFREAD_1)
	NM  =  'READ:' U
	DATA  =  RDATA(NM)
	READ_TBL<U>  =  DATA
	INPUT(NM,U,L)				:S(MFREAD_1)
	TERMINAL = "Failure in MFREAD INPUT function" :(END)
MFREAD_1 NM  =  RNM(DATA)
	BUF  =  RBUF(DATA)
	NF  =  RNF(DATA)
	LT(NF,0)				:S(FRETURN)
	IDENT(BUF,NULL)				:F(MFREAD_2)
	BUF  =  $NM				:F(MFREAD_3)
	RBUF(DATA)  =  BUF
MFREAD_2 BUF  P					:F(FRETURN)
	MFREAD  =  BUF
	RBUF(DATA)  =				:(RETURN)
MFREAD_3 RNF(DATA)  =  NF - 1			:(MFREAD_1)
MFREAD_END
