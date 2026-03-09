-MODULE DAY
*
* Modified to use real arithmetic to prevent 16-bit integer overflow
* with SNOBOL4+.  ALso adjusted pattern for DATE() function at DAY+2
*
	DEFINE('DAY(DATE)M,Y')
	YEAR_  =  365
	YEAR_4  =  4 * YEAR_ + 1
	CENT_  =  (25. * YEAR_4) - 1
	CENT_4  =  4 * CENT_ + 1
	DAY_ZERO  =  2
						:(DAY_END)
DAY	DATE      BREAK('/') . M   LEN(1)
+	(BREAK('/') . D  LEN(1)  REM . Y  |  REM . D)
	(IDENT(Y,'')  DATE())      '-' ARB '-'  LEN(2) . Y
	Y  =  EQ(SIZE(Y),2)   '19'  Y
	M  =  LE(M,2)  M + 12			:F(DAY_1)
	Y  =  Y - 1
DAY_1	M  =  M - 3
DAY_2	DAY = (Y / 400) * CENT_4 + (REMDR(Y,400) / 100) * CENT_
.	+  (REMDR(Y,100) / 4) * YEAR_4 +  REMDR(Y,4) * YEAR_
	DAY = DAY + ((153 * M) + 2) / 5 + D + DAY_ZERO
	D  =  CONVERT(REMDR(DAY,7),'INTEGER')
	'0SUN1MON2TUES3WEDNES4THURS5FRI6SATUR7'
+	D  BREAK('01234567') . DAY
	DAY  =  DAY   'DAY'			:(RETURN)
DAY_END
